/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.ldap.replication;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.event.DirectoryListener;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.extras.controls.SyncModifyDn;
import org.apache.directory.shared.ldap.extras.controls.SyncModifyDnType;
import org.apache.directory.shared.ldap.extras.controls.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.extras.controls.SyncStateValue;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncModifyDnDecorator;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncStateValueDecorator;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.model.message.AbandonListener;
import org.apache.directory.shared.ldap.model.message.AbandonableRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultEntryImpl;
import org.apache.directory.shared.ldap.model.message.controls.ChangeType;
import org.apache.directory.shared.util.Strings;
import org.apache.mina.core.future.WriteFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A listener associated with the replication system. It does send the modifications to the 
 * consumer, if it's connected, or store the data into a queue for a later transmission.
 * 
 * Note: we always log the entry irrespective of the client's connection status for guaranteed delivery
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncReplSearchListener implements DirectoryListener, AbandonListener
{
    /** Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyncReplSearchListener.class );

    /** The ldap session */
    private LdapSession session;
    
    /** The search request we are processing */
    private SearchRequest searchRequest;

    /** A flag telling if we push the response to the consumer or if we store them in a queue */
    private volatile boolean pushInRealTime;

    /** The consumer configuration */
    private final ReplicaEventLog consumerMsgLog;
    
    private DirectoryService directoryService;


    /**
     * Create a new instance of a consumer listener
     * 
     * @param session The LDAP session to use for this listener
     * @param searchRequest The searchRequest to process
     * @param consumerMsgLog The consumer configuration
     * @param pushInRealTime Tells if we push the results to the consumer in real time
     */
    SyncReplSearchListener( LdapSession session, SearchRequest searchRequest, ReplicaEventLog consumerMsgLog,
        boolean pushInRealTime )
    {
        this.pushInRealTime = pushInRealTime;
        this.session = session;
        setSearchRequest( searchRequest );
        this.consumerMsgLog = consumerMsgLog;
        directoryService = session.getLdapServer().getDirectoryService();
    }


    /**
     * Store the Ldap session to use
     * @param session The Ldap Session to use
     */
    public void setSession( LdapSession session )
    {
        this.session = session;
        directoryService = session.getLdapServer().getDirectoryService();
    }


    /**
     * Stores the SearchRequest, and associate a AbandonListener to it
     * 
     * @param searchRequest The SearchRequest instance to store
     */
    public void setSearchRequest( SearchRequest searchRequest )
    {
        this.searchRequest = searchRequest;
        
        if ( searchRequest != null )
        {
            searchRequest.addAbandonListener( this );
        }
    }


    /**
     * Abandon a SearchRequest
     * 
     * @param searchRequest The SearchRequest to abandon
     */
    public void requestAbandoned( AbandonableRequest searchRequest )
    {
        try
        {
            if ( session != null )
            {
                // We first remove the Listener from the session's chain
                directoryService.getEventService().removeListener( this );
            }

            /*
             * From RFC 2251 Section 4.11:
             * 
             * In the event that a server receives an Abandon Request on a Search  
             * operation in the midst of transmitting responses to the Search, that
             * server MUST cease transmitting entry responses to the abandoned
             * request immediately, and MUST NOT send the SearchResultDone. Of
             * course, the server MUST ensure that only properly encoded LDAPMessage
             * PDUs are transmitted. 
             * 
             * SO DON'T SEND BACK ANYTHING!!!!!
             */
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_164 ), e );
        }
    }

    
    /**
     * Create the SyncStateValue control
     */
    private SyncStateValue createControl( DirectoryService directoryService, SyncStateTypeEnum operation, Entry entry ) 
        throws LdapInvalidAttributeValueException
    {
        SyncStateValue syncStateValue = new SyncStateValueDecorator( directoryService.getLdapCodecService() );

        syncStateValue.setSyncStateType( operation );
        syncStateValue.setEntryUUID( 
            Strings.uuidToBytes( entry.get( SchemaConstants.ENTRY_UUID_AT ).getString() ) );
        syncStateValue.setCookie( getCookie( entry ) );
        
        return syncStateValue;
    }
    
    
    /**
     * Send the result to the consumer. If the consumer has disconnected, we fail back to the queue.
     */
    private void sendResult( SearchResultEntry searchResultEntry, Entry entry, EventType eventType, 
        SyncStateValue syncStateValue, SyncModifyDn syncModifyDn )
    {
        searchResultEntry.addControl( syncStateValue );

        LOG.debug( "sending event {} of entry {}", eventType, entry.getDn() );
        WriteFuture future = session.getIoSession().write( searchResultEntry );

        // Now, send the entry to the consumer
        handleWriteFuture( future, entry, eventType, syncModifyDn );
    }
    

    /**
     * Process a ADD operation. The added entry is pushed to the consumer if it's connected,
     * or stored in the consumer's queue if it's not.
     * 
     * @param addContext The Addition operation context
     */
    public void entryAdded( AddOperationContext addContext )
    {
        Entry entry = addContext.getEntry();

        try
        {
            //System.out.println( "ADD Listener : log " + entry.getDn() );
            // we log it first
            consumerMsgLog.log( new ReplicaEventMessage( ChangeType.ADD, ((ClonedServerEntry)entry).getClonedEntry() ) );

            // We send the added entry directly to the consumer if it's connected
            if ( pushInRealTime )
            {
                // Construct a new SearchResultEntry
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( entry.getDn() );
                resultEntry.setEntry( entry );

                // Create the control which will be added to the response.
                SyncStateValue syncAdd = createControl( directoryService, SyncStateTypeEnum.ADD, entry );
                
                sendResult( resultEntry, entry, EventType.ADD, syncAdd, null );
            }
            
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // shouldn't happen
            LOG.error( e.getMessage(), e );
        }
    }


    /**
     * Process a Delete operation. A delete event is send to the consumer, or stored in its 
     * queue if the consumer is not connected.
     * 
     * @param deleteContext The delete operation context
     */
    public void entryDeleted( DeleteOperationContext deleteContext )
    {
        Entry entry = deleteContext.getEntry();
        sendDeletedEntry( entry );
    }
    

    /**
     * A helper method, as the delete opertaionis used by the ModDN operations.
     */
    private void sendDeletedEntry( Entry entry )
    {
        try
        {
            //System.out.println( "DELETE Listener : log " + entry.getDn() );
            consumerMsgLog.log( new ReplicaEventMessage( ChangeType.DELETE, entry ) );
            
            if ( pushInRealTime )
            {
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( entry.getDn() );
                resultEntry.setEntry( entry );

                SyncStateValue syncDelete = createControl( directoryService, SyncStateTypeEnum.DELETE, entry );

                sendResult( resultEntry, entry, EventType.DELETE, syncDelete, null );
            }
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // shouldn't happen
            LOG.error( e.getMessage(), e );
        }
    }


    /**
     * Process a Modify operation. A modify event is send to the consumer, or stored in its 
     * queue if the consumer is not connected.
     * 
     * @param modifyContext The modify operation context
     */
    public void entryModified( ModifyOperationContext modifyContext )
    {
        Entry alteredEntry = modifyContext.getAlteredEntry();

        try
        {
            //System.out.println( "MODIFY Listener : log " + alteredEntry.getDn() );
            consumerMsgLog.log( new ReplicaEventMessage( ChangeType.MODIFY, alteredEntry ) );
            
            if ( pushInRealTime )
            {

                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( modifyContext.getDn() );
                resultEntry.setEntry( alteredEntry );

                SyncStateValue syncModify = createControl( directoryService, SyncStateTypeEnum.MODIFY, alteredEntry );

                sendResult( resultEntry, alteredEntry, EventType.MODIFY, syncModify, null );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    /**
     * Process a Move operation. A MODDN event is send to the consumer, or stored in its 
     * queue if the consumer is not connected.
     * 
     * @param moveContext The move operation context
     */
    public void entryMoved( MoveOperationContext moveContext )
    {
        Entry entry = moveContext.getOriginalEntry();

        try
        {
            if ( !moveContext.getNewSuperior().isDescendantOf( consumerMsgLog.getSearchCriteria().getBase() ) )
            {
                sendDeletedEntry( entry );
                return;
            }

            SyncModifyDn modDnControl = 
                new SyncModifyDnDecorator( directoryService.getLdapCodecService(), SyncModifyDnType.MOVE );
            modDnControl.setEntryDn( moveContext.getDn().getNormName() );
            modDnControl.setNewSuperiorDn( moveContext.getNewSuperior().getNormName() );

            //System.out.println( "MOVE Listener : log " + moveContext.getDn() + " moved to " + moveContext.getNewSuperior() );
            consumerMsgLog.log( new ReplicaEventMessage( modDnControl, entry ) );
            
            if ( pushInRealTime )
            {
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( moveContext.getDn() );
                resultEntry.setEntry( entry );
                resultEntry.addControl( modDnControl );

                SyncStateValue syncModify = createControl( directoryService, SyncStateTypeEnum.MODDN, entry );

                sendResult( resultEntry, entry, null, syncModify, modDnControl );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    /**
     * Process a MoveAndRename operation. A MODDN event is send to the consumer, or stored in its 
     * queue if the consumer is not connected.
     * 
     * @param moveAndRenameContext The move and rename operation context
     */
    public void entryMovedAndRenamed( MoveAndRenameOperationContext moveAndRenameContext )
    {
        try
        {
            if ( !moveAndRenameContext.getNewSuperiorDn().isDescendantOf( consumerMsgLog.getSearchCriteria().getBase() ) )
            {
                sendDeletedEntry( moveAndRenameContext.getEntry() );
                return;
            }

            SyncModifyDnDecorator modDnControl = 
                new SyncModifyDnDecorator( directoryService.getLdapCodecService(), SyncModifyDnType.MOVE_AND_RENAME );
            modDnControl.setEntryDn( moveAndRenameContext.getDn().getNormName() );
            modDnControl.setNewSuperiorDn( moveAndRenameContext.getNewSuperiorDn().getNormName() );
            modDnControl.setNewRdn( moveAndRenameContext.getNewRdn().getNormName() );
            modDnControl.setDeleteOldRdn( moveAndRenameContext.getDeleteOldRdn() );

            // should always send the original entry cause the consumer perform the modDn operation there
            Entry entry = moveAndRenameContext.getOriginalEntry();

            //System.out.println( "MOVE AND RENAME Listener : log " + moveAndRenameContext.getDn() + 
            //    " moved to " + moveAndRenameContext.getNewSuperiorDn() + " renamed to " + moveAndRenameContext.getNewRdn() );
            consumerMsgLog.log( new ReplicaEventMessage( modDnControl, entry ) );
            
            if ( pushInRealTime )
            {
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( entry.getDn() );
                resultEntry.setEntry( entry );
                resultEntry.addControl( modDnControl );

                SyncStateValue syncModify = createControl( directoryService, SyncStateTypeEnum.MODDN, entry );

                sendResult( resultEntry, entry, null, syncModify, modDnControl );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    /**
     * Process a Rename operation. A MODDN event is send to the consumer, or stored in its 
     * queue if the consumer is not connected.
     * 
     * @param renameContext The rename operation context
     */
    public void entryRenamed( RenameOperationContext renameContext )
    {
        Entry entry = renameContext.getOriginalEntry();

        try
        {
            SyncModifyDnDecorator modDnControl = new SyncModifyDnDecorator( directoryService.getLdapCodecService() );
            modDnControl.setModDnType( SyncModifyDnType.RENAME );
            modDnControl.setEntryDn( renameContext.getDn().getName() );
            modDnControl.setNewRdn( renameContext.getNewRdn().getName() );
            modDnControl.setDeleteOldRdn( renameContext.getDeleteOldRdn() );

            // should always send the original entry cause the consumer perform the modDn operation there
            //System.out.println( "RENAME Listener : log " + renameContext.getDn() + " renamed to " + renameContext.getNewRdn() );
            consumerMsgLog.log( new ReplicaEventMessage( modDnControl, entry ) );
            
            if ( pushInRealTime )
            {
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( entry.getDn() );
                resultEntry.setEntry( entry );
                resultEntry.addControl( modDnControl );

                SyncStateValue syncModify = createControl( directoryService, SyncStateTypeEnum.MODDN, entry );
                
                // In this case, the cookie is different
                syncModify.setCookie( getCookie( entry ) );

                sendResult( resultEntry, entry, null, syncModify, modDnControl );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    /**
     * @return true if the entries are sent to the consumer in real time
     */
    public boolean isPushInRealTime()
    {
        return pushInRealTime;
    }


    /**
     * Set the pushInRealTime parameter
     * @param pushInRealTime true if the entries must be push to the consumer directly
     */
    public void setPushInRealTime( boolean pushInRealTime )
    {
        this.pushInRealTime = pushInRealTime;
    }


    /**
     * Get the cookie from the entry
     */
    private byte[] getCookie( Entry entry ) throws LdapInvalidAttributeValueException
    {
        String csn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();

        return Strings.getBytesUtf8( consumerMsgLog.getId() + SyncReplRequestHandler.REPLICA_ID_DELIM + csn );
    }


    /**
     * Process the writing of the replicated entry to the consumer
     */
    private void handleWriteFuture( WriteFuture future, Entry entry, EventType event, SyncModifyDn modDnControl )
    {
        // Let the operation be executed.
        // Note : we wait 10 seconds max
        future.awaitUninterruptibly( 10000L );
        
        if ( !future.isWritten() )
        {
            LOG.error( "Failed to write to the consumer {} during the event {} on entry {}", new Object[] { 
                           consumerMsgLog.getId(), event, entry.getDn() } );
            LOG.error( "", future.getException() );

            // set realtime push to false, will be set back to true when the client
            // comes back and sends another request this flag will be set to true
            pushInRealTime = false;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "SyncReplSearchListener : \n" );
        sb.append( '\'' ).append( searchRequest ).append( "', " );
        sb.append( '\'' ).append( pushInRealTime ).append( "', \n" );
        sb.append( consumerMsgLog );
        sb.append( '\n' );
        
        return sb.toString();
    }
}
