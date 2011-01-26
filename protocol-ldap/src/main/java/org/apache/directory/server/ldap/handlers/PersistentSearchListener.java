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
package org.apache.directory.server.ldap.handlers;


import org.apache.directory.server.core.event.DirectoryListener;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.ChangeOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.codec.search.controls.entryChange.EntryChange;
import org.apache.directory.shared.ldap.codec.search.controls.persistentSearch.PersistentSearch;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.AbandonListener;
import org.apache.directory.shared.ldap.model.message.AbandonableRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultEntryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A DirectoryListener implementation which sends back added, deleted, modified or 
 * renamed entries to a client that created this listener.  This class is part of the
 * persistent search implementation which uses the event notification scheme built into
 * the server core.  
 * 
 * This listener is disabled only when a session closes or when an abandon request 
 * cancels it.  Hence time and size limits in normal search operations do not apply
 * here.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PersistentSearchListener implements DirectoryListener, AbandonListener
{
    private static final Logger LOG = LoggerFactory.getLogger( PersistentSearchListener.class );
    final LdapSession session;
    final SearchRequest req;
    final PersistentSearch psearchControl;


    PersistentSearchListener( LdapSession session, SearchRequest req )
    {
        this.session = session;
        this.req = req;
        req.addAbandonListener( this );
        this.psearchControl = ( PersistentSearch ) req.getControls().get( PersistentSearch.OID );
    }


    public void abandon() throws LdapException
    {
        // must abandon the operation 
        session.getCoreSession().getDirectoryService().getEventService().removeListener( this );

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


    public void requestAbandoned( AbandonableRequest req )
    {
        try
        {
            abandon();
        }
        catch ( LdapException e )
        {
            LOG.error( I18n.err( I18n.ERR_164 ), e );
        }
    }


    private void setECResponseControl( SearchResultEntry response, ChangeOperationContext opContext, ChangeType type )
    {
        if ( psearchControl.isReturnECs() )
        {
            EntryChange ecControl = new EntryChange();
            ecControl.setChangeType( type );

            if ( opContext.getChangeLogEvent() != null )
            {
                ecControl.setChangeNumber( opContext.getChangeLogEvent().getRevision() );
            }

            if ( opContext instanceof RenameOperationContext || opContext instanceof MoveOperationContext )
            {
                ecControl.setPreviousDn( opContext.getDn() );
            }

            response.addControl( ecControl );
        }
    }


    public void entryAdded( AddOperationContext addContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.ADD ) )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( addContext.getDn() );
        respEntry.setEntry( addContext.getEntry() );
        setECResponseControl( respEntry, addContext, ChangeType.ADD );
        session.getIoSession().write( respEntry );
    }


    public void entryDeleted( DeleteOperationContext deleteContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.DELETE ) )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( deleteContext.getDn() );
        respEntry.setEntry( deleteContext.getEntry() );
        setECResponseControl( respEntry, deleteContext, ChangeType.DELETE );
        session.getIoSession().write( respEntry );
    }


    public void entryModified( ModifyOperationContext modifyContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.MODIFY ) )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( modifyContext.getDn() );
        respEntry.setEntry( modifyContext.getAlteredEntry() );
        setECResponseControl( respEntry, modifyContext, ChangeType.MODIFY );
        session.getIoSession().write( respEntry );
    }


    public void entryMoved( MoveOperationContext moveContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.MODDN ) )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( moveContext.getDn() );
        respEntry.setEntry( moveContext.getEntry() );
        setECResponseControl( respEntry, moveContext, ChangeType.MODDN );
        session.getIoSession().write( respEntry );
    }


    public void entryMovedAndRenamed( MoveAndRenameOperationContext moveAndRenameContext )
    {
        entryRenamed( moveAndRenameContext );
    }


    public void entryRenamed( RenameOperationContext renameContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.MODDN ) )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( renameContext.getModifiedEntry().getDn() );
        respEntry.setEntry( renameContext.getModifiedEntry() );
        setECResponseControl( respEntry, renameContext, ChangeType.MODDN );
        session.getIoSession().write( respEntry );
    }
}