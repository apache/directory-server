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
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncmodifydn.SyncModifyDnControl;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.message.AbandonListener;
import org.apache.directory.shared.ldap.message.SearchResponseEntryImpl;
import org.apache.directory.shared.ldap.message.control.replication.SyncModifyDnType;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.message.internal.InternalAbandonableRequest;
import org.apache.directory.shared.ldap.message.internal.InternalSearchRequest;
import org.apache.directory.shared.ldap.message.internal.InternalSearchResponseEntry;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.future.WriteFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * modeled after PersistentSearchListener
 *  NOTE: doco is missing at many parts. Will be added once the functionality is satisfactory
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncReplSearchListener implements DirectoryListener, AbandonListener
{
    private static final Logger LOG = LoggerFactory.getLogger( SyncReplSearchListener.class );

    private LdapSession session;
    private InternalSearchRequest req;

    private volatile boolean pushInRealTime;

    private final ReplicaEventLog clientMsgLog;


    SyncReplSearchListener( LdapSession session, InternalSearchRequest req, ReplicaEventLog clientMsgLog,
        boolean pushInRealTime )
    {
        this.pushInRealTime = pushInRealTime;
        setSession( session );
        setReq( req );

        this.clientMsgLog = clientMsgLog;
    }


    public void setSession( LdapSession session )
    {
        this.session = session;
    }


    public void setReq( InternalSearchRequest req )
    {
        this.req = req;
        if ( req != null )
        {
            req.addAbandonListener( this );
        }
    }


    public void abandon() throws Exception
    {
        if ( session != null )
        {
            // must abandon the operation 
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


    public void requestAbandoned( InternalAbandonableRequest req )
    {
        try
        {
            abandon();
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_164 ), e );
        }
    }


    public void entryAdded( AddOperationContext opContext )
    {
        Entry entry = opContext.getEntry();

        LOG.debug( "sending added entry {}", entry.getDn() );
        
        try
        {
            if ( pushInRealTime )
            {
                
                InternalSearchResponseEntry respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                respEntry.setObjectName( entry.getDn() );
                respEntry.setEntry( entry );

                SyncStateValueControl syncAdd = new SyncStateValueControl();
                syncAdd.setSyncStateType( SyncStateTypeEnum.ADD );
                syncAdd.setEntryUUID( StringTools.uuidToBytes( entry.get( SchemaConstants.ENTRY_UUID_AT )
                    .getString() ) );
                syncAdd.setCookie( getCookie( entry ) );
                respEntry.add( syncAdd );

                WriteFuture future = session.getIoSession().write( respEntry );
                handleWriteFuture( future, entry, EventType.ADD, null );
            }
            else
            {
                clientMsgLog.log( EventType.ADD, opContext.getEntry() );
            }
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // shouldn't happen
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryDeleted( DeleteOperationContext opContext )
    {
        sendDeletedEntry( opContext.getEntry() );
    }


    private void sendDeletedEntry( Entry entry )
    {
        LOG.debug( "sending deleted entry {}", entry.getDn() );

        try
        {
            if ( pushInRealTime )
            {
                InternalSearchResponseEntry respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                respEntry.setObjectName( entry.getDn() );
                respEntry.setEntry( entry );

                SyncStateValueControl syncDelete = new SyncStateValueControl();
                syncDelete.setSyncStateType( SyncStateTypeEnum.DELETE );
                syncDelete.setEntryUUID( StringTools.uuidToBytes( entry.get(
                    SchemaConstants.ENTRY_UUID_AT ).getString() ) );
                syncDelete.setCookie( getCookie( entry ) );
                respEntry.add( syncDelete );

                WriteFuture future = session.getIoSession().write( respEntry );
                
                handleWriteFuture( future, entry, EventType.DELETE, null );
            }
            else
            {
                clientMsgLog.log( EventType.DELETE, entry );
            }
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // shouldn't happen
            LOG.error( e.getMessage(), e );
        }
    }
    
    
    public void entryModified( ModifyOperationContext opContext )
    {
        Entry alteredEntry = opContext.getAlteredEntry();

        LOG.debug( "sending modified entry {}", alteredEntry.getDn() );

        try
        {
            if ( pushInRealTime )
            {
                
                InternalSearchResponseEntry respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                respEntry.setObjectName( opContext.getDn() );
                respEntry.setEntry( alteredEntry );

                SyncStateValueControl syncModify = new SyncStateValueControl();
                syncModify.setSyncStateType( SyncStateTypeEnum.MODIFY );
                syncModify.setEntryUUID( StringTools.uuidToBytes( alteredEntry.get(
                    SchemaConstants.ENTRY_UUID_AT ).getString() ) );
                syncModify.setCookie( getCookie( alteredEntry ) );
                respEntry.add( syncModify );

                WriteFuture future = session.getIoSession().write( respEntry );
                
                // store altered entry cause that holds the updated CSN
                handleWriteFuture( future, alteredEntry, EventType.MODIFY, null );
            }
            else
            {
                clientMsgLog.log( EventType.MODIFY, opContext.getAlteredEntry() );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryMoved( MoveOperationContext opContext )
    {
        Entry entry = opContext.getEntry();

        LOG.debug( "sending moved entry {}", entry.getDn() );

        try
        {
            if( ! opContext.getParent().isChildOf( clientMsgLog.getSearchCriteria().getBase() ) )
            {
                sendDeletedEntry( opContext.getEntry() );
                return;
            }
            
            SyncModifyDnControl modDnControl = new SyncModifyDnControl( SyncModifyDnType.MOVE );
            modDnControl.setEntryDn( opContext.getDn().getNormName() );
            modDnControl.setNewSuperiorDn( opContext.getParent().getNormName() );

            if ( pushInRealTime )
            {
                InternalSearchResponseEntry respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                respEntry.setObjectName( opContext.getDn() );
                respEntry.setEntry( entry );

                SyncStateValueControl syncModify = new SyncStateValueControl();
                syncModify.setSyncStateType( SyncStateTypeEnum.MODDN );
                syncModify.setEntryUUID( StringTools.uuidToBytes( entry.get(
                    SchemaConstants.ENTRY_UUID_AT ).getString() ) );
                syncModify.setCookie( getCookie( entry ) );
                respEntry.add( syncModify );
                respEntry.add( modDnControl );
                
                WriteFuture future = session.getIoSession().write( respEntry );
                
                handleWriteFuture( future, entry, null, modDnControl );
            }
            else
            {
                clientMsgLog.log( new ReplicaEventMessage( modDnControl, opContext.getEntry() ) );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryMovedAndRenamed( MoveAndRenameOperationContext opContext )
    {
        
        LOG.debug( "sending moveAndRenamed entry {}", opContext.getDn() );

        try
        {
            if( ! opContext.getParent().isChildOf( clientMsgLog.getSearchCriteria().getBase() ) )
            {
                sendDeletedEntry( opContext.getEntry() );
                return;
            }

            SyncModifyDnControl modDnControl = new SyncModifyDnControl( SyncModifyDnType.MOVEANDRENAME );
            modDnControl.setEntryDn( opContext.getDn().getNormName() );
            modDnControl.setNewSuperiorDn( opContext.getParent().getNormName() );
            modDnControl.setNewRdn( opContext.getNewRdn().getNormName() );
            modDnControl.setDeleteOldRdn( opContext.getDelOldDn() );

            if ( pushInRealTime )
            {
                Entry alteredEntry = opContext.getAlteredEntry();
                
                InternalSearchResponseEntry respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                respEntry.setObjectName( opContext.getAlteredEntry().getDn() );
                respEntry.setEntry( alteredEntry );

                SyncStateValueControl syncModify = new SyncStateValueControl();
                syncModify.setSyncStateType( SyncStateTypeEnum.MODDN );
                syncModify.setEntryUUID( StringTools.uuidToBytes( alteredEntry.get(
                    SchemaConstants.ENTRY_UUID_AT ).getString() ) );
                syncModify.setCookie( getCookie( alteredEntry ) );
                respEntry.add( syncModify );
                respEntry.add( modDnControl );

                WriteFuture future = session.getIoSession().write( respEntry );
                
                handleWriteFuture( future, alteredEntry, null, modDnControl );
            }
            else
            {
                clientMsgLog.log( new ReplicaEventMessage( modDnControl, opContext.getEntry() ) );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    public void entryRenamed( RenameOperationContext opContext )
    {
        Entry entry = opContext.getEntry();
        
        LOG.debug( "sending renamed entry {}", entry.getDn() );
        
        try
        {
            SyncModifyDnControl modDnControl = new SyncModifyDnControl();
            modDnControl.setModDnType( SyncModifyDnType.RENAME );
            modDnControl.setEntryDn( opContext.getDn().getName() );
            modDnControl.setNewRdn( opContext.getNewRdn().getName() );
            modDnControl.setDeleteOldRdn( opContext.getDelOldDn() );

            if ( pushInRealTime )
            {
                InternalSearchResponseEntry respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                respEntry.setObjectName( entry.getDn() );
                respEntry.setEntry( entry );

                SyncStateValueControl syncModify = new SyncStateValueControl();
                syncModify.setSyncStateType( SyncStateTypeEnum.MODDN );
                syncModify.setEntryUUID( StringTools.uuidToBytes( entry.get(
                    SchemaConstants.ENTRY_UUID_AT ).getString() ) );
                syncModify.setCookie( getCookie( opContext.getAlteredEntry() ) );
                respEntry.add( syncModify );
                respEntry.add( modDnControl );
                
                WriteFuture future = session.getIoSession().write( respEntry );
                
                handleWriteFuture( future, opContext.getAlteredEntry(), null, modDnControl );
            }
            else
            {
                clientMsgLog.log( new ReplicaEventMessage( modDnControl, opContext.getEntry() ) );
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
        return StringTools.getBytesUtf8( clientMsgLog.getId() + SyncReplProvider.REPLICA_ID_DELIM + csn );
    }
    
    
    private void handleWriteFuture( WriteFuture future, Entry entry, EventType event, SyncModifyDnControl modDnControl )
    {
        future.awaitUninterruptibly();
        if( !future.isWritten() )
        {
            LOG.error( "Failed to write to the consumer {}", clientMsgLog.getId() );
            LOG.error( "", future.getException() );
            
            // set realtime push to false, will be set back to true when the client
            // comes back and sends another request this flag will be set to true
            pushInRealTime = false;
            
            if( modDnControl != null )
            {
                clientMsgLog.log( new ReplicaEventMessage( modDnControl, entry ) );
            }
            else
            {
                clientMsgLog.log( event, entry );
            }
        }
    }
}