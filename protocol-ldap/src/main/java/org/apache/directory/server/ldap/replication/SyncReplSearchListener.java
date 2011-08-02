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
import org.apache.directory.shared.util.Strings;
import org.apache.mina.core.future.WriteFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * modeled after PersistentSearchListener
 *  NOTE: doco is missing at many parts. Will be added once the functionality is satisfactory
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
    
    
    private void sendResult( SearchResultEntry searchResultEntry, Entry entry, EventType eventType, 
        SyncStateValue syncStateValue, SyncModifyDn syncModifyDn )
    {
        searchResultEntry.addControl( syncStateValue );

        WriteFuture future = session.getIoSession().write( searchResultEntry );

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

        LOG.debug( "sending added entry {}", entry.getDn() );

        try
        {
            // We send the added entry directly to the consumer if it's connected
            if ( pushInRealTime )
            {
                // Construct a new SearchResultEntry
                SearchResultEntry respEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                respEntry.setObjectName( entry.getDn() );
                respEntry.setEntry( entry );

                // Create the control which will be added to the response.
                SyncStateValue syncAdd = createControl( directoryService, SyncStateTypeEnum.ADD, entry );
                
                sendResult( respEntry, entry, EventType.ADD, syncAdd, null );
            }
            else
            {
                // We are not connected, store the entry into the consumer's queue
                consumerMsgLog.log( EventType.ADD, addContext.getEntry() );
            }
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // shouldn't happen
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryDeleted( DeleteOperationContext deleteContext )
    {
        Entry entry = deleteContext.getEntry();
        sendDeletedEntry( entry );
    }
    
    
    private void sendDeletedEntry( Entry entry )
    {
        LOG.debug( "sending deleted entry {}", entry.getDn() );

        try
        {
            if ( pushInRealTime )
            {
                SearchResultEntry respEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                respEntry.setObjectName( entry.getDn() );
                respEntry.setEntry( entry );

                SyncStateValue syncDelete = createControl( directoryService, SyncStateTypeEnum.DELETE, entry );

                sendResult( respEntry, entry, EventType.DELETE, syncDelete, null );
            }
            else
            {
                consumerMsgLog.log( EventType.DELETE, entry );
            }
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // shouldn't happen
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryModified( ModifyOperationContext modifyContext )
    {
        Entry alteredEntry = modifyContext.getAlteredEntry();

        LOG.debug( "sending modified entry {}", alteredEntry.getDn() );

        try
        {
            if ( pushInRealTime )
            {

                SearchResultEntry respEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                respEntry.setObjectName( modifyContext.getDn() );
                respEntry.setEntry( alteredEntry );

                SyncStateValue syncModify = createControl( directoryService, SyncStateTypeEnum.MODIFY, alteredEntry );

                sendResult( respEntry, alteredEntry, EventType.MODIFY, syncModify, null );
            }
            else
            {
                consumerMsgLog.log( EventType.MODIFY, modifyContext.getAlteredEntry() );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryMoved( MoveOperationContext moveContext )
    {
        Entry entry = moveContext.getOriginalEntry();

        LOG.debug( "sending moved entry {}", entry.getDn() );

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

            if ( pushInRealTime )
            {
                SearchResultEntry respEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                respEntry.setObjectName( moveContext.getDn() );
                respEntry.setEntry( entry );
                respEntry.addControl( modDnControl );

                SyncStateValue syncModify = createControl( directoryService, SyncStateTypeEnum.MODDN, entry );

                sendResult( respEntry, entry, null, syncModify, modDnControl );
            }
            else
            {
                consumerMsgLog.log( new ReplicaEventMessage( modDnControl, entry ) );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryMovedAndRenamed( MoveAndRenameOperationContext moveAndRenameContext )
    {

        LOG.debug( "sending moveAndRenamed entry {}", moveAndRenameContext.getDn() );

        try
        {
            if ( !moveAndRenameContext.getNewSuperiorDn().isDescendantOf( consumerMsgLog.getSearchCriteria().getBase() ) )
            {
                sendDeletedEntry( moveAndRenameContext.getEntry() );
                return;
            }

            SyncModifyDnDecorator modDnControl = 
                new SyncModifyDnDecorator( directoryService.getLdapCodecService(), SyncModifyDnType.MOVEANDRENAME );
            modDnControl.setEntryDn( moveAndRenameContext.getDn().getNormName() );
            modDnControl.setNewSuperiorDn( moveAndRenameContext.getNewSuperiorDn().getNormName() );
            modDnControl.setNewRdn( moveAndRenameContext.getNewRdn().getNormName() );
            modDnControl.setDeleteOldRdn( moveAndRenameContext.getDeleteOldRdn() );

            if ( pushInRealTime )
            {
                Entry alteredEntry = moveAndRenameContext.getModifiedEntry();

                SearchResultEntry respEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                respEntry.setObjectName( moveAndRenameContext.getModifiedEntry().getDn() );
                respEntry.setEntry( alteredEntry );
                respEntry.addControl( modDnControl );

                SyncStateValue syncModify = createControl( directoryService, SyncStateTypeEnum.MODDN, alteredEntry );

                sendResult( respEntry, alteredEntry, null, syncModify, modDnControl );
            }
            else
            {
                consumerMsgLog.log( new ReplicaEventMessage( modDnControl, moveAndRenameContext.getEntry() ) );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryRenamed( RenameOperationContext renameContext )
    {
        Entry entry = renameContext.getEntry();

        LOG.debug( "sending renamed entry {}", entry.getDn() );

        try
        {
            SyncModifyDnDecorator modDnControl = new SyncModifyDnDecorator( directoryService.getLdapCodecService() );
            modDnControl.setModDnType( SyncModifyDnType.RENAME );
            modDnControl.setEntryDn( renameContext.getDn().getName() );
            modDnControl.setNewRdn( renameContext.getNewRdn().getName() );
            modDnControl.setDeleteOldRdn( renameContext.getDeleteOldRdn() );

            if ( pushInRealTime )
            {
                SearchResultEntry respEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                respEntry.setObjectName( entry.getDn() );
                respEntry.setEntry( entry );
                respEntry.addControl( modDnControl );

                SyncStateValue syncModify = createControl( directoryService, SyncStateTypeEnum.MODDN, entry );
                
                Entry modifiedEntry = renameContext.getModifiedEntry();
                
                // In this case, the cookie is different
                syncModify.setCookie( getCookie( modifiedEntry ) );

                sendResult( respEntry, modifiedEntry, null, syncModify, modDnControl );
            }
            else
            {
                consumerMsgLog.log( new ReplicaEventMessage( modDnControl, renameContext.getEntry() ) );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    public boolean isPushInRealTime()
    {
        return pushInRealTime;
    }


    public void setPushInRealTime( boolean pushInRealTime )
    {
        this.pushInRealTime = pushInRealTime;
    }


    private byte[] getCookie( Entry entry ) throws LdapInvalidAttributeValueException
    {
        String csn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();
        return Strings.getBytesUtf8( consumerMsgLog.getId() + SyncReplRequestHandler.REPLICA_ID_DELIM + csn );
    }


    private void handleWriteFuture( WriteFuture future, Entry entry, EventType event, SyncModifyDn modDnControl )
    {
        future.awaitUninterruptibly();
        
        if ( !future.isWritten() )
        {
            LOG.error( "Failed to write to the consumer {} during the event {} on entry {}", new Object[] { 
                           consumerMsgLog.getId(), event, entry.getDn() } );
            LOG.error( "", future.getException() );

            // set realtime push to false, will be set back to true when the client
            // comes back and sends another request this flag will be set to true
            pushInRealTime = false;

            if ( modDnControl != null )
            {
                consumerMsgLog.log( new ReplicaEventMessage( modDnControl, entry ) );
            }
            else
            {
                consumerMsgLog.log( event, entry );
            }
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