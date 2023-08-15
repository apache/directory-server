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
package org.apache.directory.server.ldap.replication.provider;


import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateTypeEnum;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateValueImpl;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.AbandonListener;
import org.apache.directory.api.ldap.model.message.AbandonableRequest;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchResultEntryImpl;
import org.apache.directory.api.ldap.model.message.controls.ChangeType;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.event.EventType;
import org.apache.directory.server.core.api.interceptor.context.AbstractChangeOperationContext;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapProtocolUtils;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.replication.ReplicaEventMessage;
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
    
    private static String replConsumerConfigDn = Strings.toLowerCaseAscii( ServerDNConstants.REPL_CONSUMER_CONFIG_DN );
    private static String schemaDn = Strings.toLowerCaseAscii( SchemaConstants.OU_SCHEMA );
    private static String replConsumerDn = Strings.toLowerCaseAscii( ServerDNConstants.REPL_CONSUMER_DN_STR );
    
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
        setSession( session );
        setSearchRequest( searchRequest );
        this.consumerMsgLog = consumerMsgLog;
    }


    /**
     * Store the Ldap session to use
     * @param session The Ldap Session to use
     */
    public void setSession( LdapSession session )
    {
        this.session = session;
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


    @Override
    public boolean isSynchronous()
    {
            return true; // always synchronous
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
                session.getCoreSession().getDirectoryService().getEventService().removeListener( this );
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
        SyncStateValue syncStateValue = new SyncStateValueImpl();

        syncStateValue.setSyncStateType( operation );
        String uuidStr = entry.get( SchemaConstants.ENTRY_UUID_AT ).getString();
        syncStateValue.setEntryUUID( Strings.uuidToBytes( uuidStr ) );
        syncStateValue.setCookie( getCookie( entry ) );
        
        return syncStateValue;
    }
    
    
    /**
     * Send the result to the consumer. If the consumer has disconnected, we fail back to the queue.
     */
    private void sendResult( SearchResultEntry searchResultEntry, Entry entry, EventType eventType, 
        SyncStateValue syncStateValue )
    {
        searchResultEntry.addControl( syncStateValue );

        LOG.debug( "sending event {} of entry {}", eventType, entry.getDn() );
        WriteFuture future = session.getIoSession().write( searchResultEntry );

        // Now, send the entry to the consumer
        handleWriteFuture( future, entry, eventType );
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
        
        if ( isConfigEntry( entry ) || isNotValidForReplication( addContext ) )
        {
            return;
        }

        try
        {
            //System.out.println( "ADD Listener : log " + entry.getDn() );
            // we log it first
            consumerMsgLog.log( new ReplicaEventMessage( ChangeType.ADD, entry ) );

            // We send the added entry directly to the consumer if it's connected
            if ( pushInRealTime )
            {
                // Construct a new SearchResultEntry
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( entry.getDn() );
                resultEntry.setEntry( entry );

                // Create the control which will be added to the response.
                SyncStateValue syncAdd = createControl( session.getCoreSession().getDirectoryService(), SyncStateTypeEnum.ADD, entry );
                
                sendResult( resultEntry, entry, EventType.ADD, syncAdd );
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
        
        if ( isConfigEntry( entry ) || isNotValidForReplication( deleteContext ) )
        {
            return;
        }
        
        sendDeletedEntry( ( ( ClonedServerEntry ) entry ).getClonedEntry() );
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

                SyncStateValue syncDelete = createControl( session.getCoreSession().getDirectoryService(), SyncStateTypeEnum.DELETE, entry );

                sendResult( resultEntry, entry, EventType.DELETE, syncDelete );
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

        if ( isConfigEntry( alteredEntry ) || isNotValidForReplication( modifyContext ) )
        {
            return;
        }

        try
        {
            //System.out.println( "MODIFY Listener : log " + alteredEntry.getDn() );
            consumerMsgLog.log( new ReplicaEventMessage( ChangeType.MODIFY, alteredEntry ) );
            
            if ( pushInRealTime )
            {

                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( modifyContext.getDn() );
                resultEntry.setEntry( alteredEntry );

                SyncStateValue syncModify = createControl( session.getCoreSession().getDirectoryService(), SyncStateTypeEnum.MODIFY, alteredEntry );

                sendResult( resultEntry, alteredEntry, EventType.MODIFY, syncModify );
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
        // should always send the modified entry cause the consumer perform the modDn operation locally
        Entry entry = moveContext.getModifiedEntry();

        if ( isConfigEntry( entry ) || isNotValidForReplication( moveContext ) )
        {
            return;
        }

        try
        {
            if ( !moveContext.getNewSuperior().isDescendantOf( consumerMsgLog.getSearchCriteria().getBase() ) )
            {
                sendDeletedEntry( moveContext.getOriginalEntry() );
                return;
            }

            //System.out.println( "MOVE Listener : log " + moveContext.getDn() + " moved to " + moveContext.getNewSuperior() );
            consumerMsgLog.log( new ReplicaEventMessage( ChangeType.MODDN, entry ) );
            
            if ( pushInRealTime )
            {
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( moveContext.getDn() );
                resultEntry.setEntry( entry );

                SyncStateValue syncModify = createControl( session.getCoreSession().getDirectoryService(), SyncStateTypeEnum.MODDN, entry );

                sendResult( resultEntry, entry, EventType.MOVE, syncModify );
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
        // should always send the modified entry cause the consumer perform the modDn operation locally
        Entry entry = moveAndRenameContext.getModifiedEntry();

        if ( isConfigEntry( entry ) || isNotValidForReplication( moveAndRenameContext ) )
        {
            return;
        }

        try
        {
            if ( !moveAndRenameContext.getNewSuperiorDn().isDescendantOf( consumerMsgLog.getSearchCriteria().getBase() ) )
            {
                sendDeletedEntry( entry );
                return;
            }


            //System.out.println( "MOVE AND RENAME Listener : log " + moveAndRenameContext.getDn() + 
            //    " moved to " + moveAndRenameContext.getNewSuperiorDn() + " renamed to " + moveAndRenameContext.getNewRdn() );
            consumerMsgLog.log( new ReplicaEventMessage( ChangeType.MODDN, entry ) );
            
            if ( pushInRealTime )
            {
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( entry.getDn() );
                resultEntry.setEntry( entry );

                SyncStateValue syncModify = createControl( session.getCoreSession().getDirectoryService(), SyncStateTypeEnum.MODDN, entry );

                sendResult( resultEntry, entry, EventType.MOVE_AND_RENAME, syncModify );
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
        // should always send the modified entry cause the consumer perform the modDn operation locally
        Entry entry = renameContext.getModifiedEntry();

        if ( isConfigEntry( entry ) || isNotValidForReplication( renameContext ) )
        {
            return;
        }

        try
        {
            // should always send the original entry cause the consumer perform the modDn operation there
            //System.out.println( "RENAME Listener : log " + renameContext.getDn() + " renamed to " + renameContext.getNewRdn() );
            consumerMsgLog.log( new ReplicaEventMessage( ChangeType.MODDN, entry ) );
            
            if ( pushInRealTime )
            {
                SearchResultEntry resultEntry = new SearchResultEntryImpl( searchRequest.getMessageId() );
                resultEntry.setObjectName( entry.getDn() );
                resultEntry.setEntry( entry );

                SyncStateValue syncModify = createControl( session.getCoreSession().getDirectoryService(), SyncStateTypeEnum.MODDN, entry );
                
                // In this case, the cookie is different
                syncModify.setCookie( getCookie( entry ) );

                sendResult( resultEntry, entry, EventType.RENAME, syncModify );
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

        return LdapProtocolUtils.createCookie( consumerMsgLog.getId(), csn );
    }


    /**
     * Process the writing of the replicated entry to the consumer
     */
    private void handleWriteFuture( WriteFuture future, Entry entry, EventType event )
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
        else
        {
            try
            {
                // if successful update the last sent CSN
                consumerMsgLog.setLastSentCsn( entry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );
            }
            catch ( Exception e )
            {
                //should never happen
                LOG.error( "No entry CSN attribute found", e );
            }
        }
    }
    
    
    /**
     * checks if the given entry belongs to the ou=config or ou=schema partition
     * We don't replicate those two partitions
     * @param entry the entry
     * @return true if the entry belongs to ou=config partition, false otherwise
     */
    private boolean isConfigEntry( Entry entry )
    {
        // we can do Dn.isDescendantOf but in this part of the
        // server the DNs are all normalized and a simple string compare should
        // do the trick
        
        String name = Strings.toLowerCase( entry.getDn().getName() );
        
        if ( name.endsWith( replConsumerConfigDn )
            || name.endsWith( schemaDn )
            || name.endsWith( replConsumerDn ) )
        {
            return true;
        }
        
        // do not replicate the changes made to transport config entries
        return name.startsWith( "ads-transportid" ) && name.endsWith( ServerDNConstants.CONFIG_DN );
    }
    
    
    private boolean isNotValidForReplication( AbstractChangeOperationContext ctx )
    {
        if ( ctx.isGenerateNoReplEvt() )
        {
            return true;
        }
        
        return isMmrConfiguredToReceiver( ctx );
    }
    

    /**
     * checks if the sender of this replication event is setup with MMR
     * (Note: this method is used to prevent sending a replicated event back to the sender after 
     *  performing local update)
     * @param ctx the operation's context
     * @return true if the rid present in operation context is same as the event log's ID, false otherwise
     */
    private boolean isMmrConfiguredToReceiver( AbstractChangeOperationContext ctx )
    {
        if ( ctx.isReplEvent() )
        {
            boolean skip = ( ctx.getRid() == consumerMsgLog.getId() );
            
            if ( skip )
            {
                LOG.debug( "RID in operation context matches with the ID of replication event log {} for host {}", consumerMsgLog.getName(), consumerMsgLog.getHostName() );
            }
            
            return skip;
        }
        
        return false;
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
