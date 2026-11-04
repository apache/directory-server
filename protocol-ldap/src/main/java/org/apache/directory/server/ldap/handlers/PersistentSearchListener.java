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


import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.AbandonListener;
import org.apache.directory.api.ldap.model.message.AbandonableRequest;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchResultEntryImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.ChangeType;
import org.apache.directory.api.ldap.model.message.controls.EntryChange;
import org.apache.directory.api.ldap.model.message.controls.EntryChangeImpl;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ChangeOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.ldap.LdapSession;
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
 * The changed entries are never sent as they are found in the operation context:
 * each of them is read again through the session of the client which created the
 * persistent search, so that the authentication and authorization checks of a
 * normal search are applied before anything is sent back.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PersistentSearchListener implements DirectoryListener, AbandonListener
{
    private static final Logger LOG = LoggerFactory.getLogger( PersistentSearchListener.class );
    final LdapSession session;
    final SearchRequest req;
    final PersistentSearch psearchControl;

    /** The attributes requested by the client */
    private final String[] attributes;

    /** The entries about to be deleted, as visible by the client */
    private final Map<DeleteOperationContext, Entry> deletedEntries = Collections.synchronizedMap( new WeakHashMap<>() );

    public PersistentSearchListener( LdapSession session, SearchRequest req )
    {
        this.session = session;
        this.req = req;
        req.addAbandonListener( this );
        this.psearchControl = ( PersistentSearch ) req.getControls().get( PersistentSearch.OID );
        attributes = req.getAttributes().toArray( Strings.EMPTY_STRING_ARRAY );
    }

    
    @Override
    public boolean isSynchronous()
    {
        return false; // always asynchronous
    }


    public void abandon()
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
        abandon();
    }


    private void setECResponseControl( SearchResultEntry response, ChangeOperationContext opContext, ChangeType type )
    {
        if ( psearchControl.isReturnECs() )
        {
            EntryChange ecControl = new EntryChangeImpl();
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

        Entry entry = getVisibleEntry( addContext.getDn() );

        if ( entry == null )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( addContext.getDn() );
        respEntry.setEntry( entry );
        
        setECResponseControl( respEntry, addContext, ChangeType.ADD );
        session.getIoSession().write( respEntry );
    }


    /**
     * {@inheritDoc}
     * 
     * The entry can't be read anymore once it has been deleted, so we read it now,
     * and keep it until we are notified of the deletion.
     */
    @Override
    public void beforeEntryDeleted( DeleteOperationContext deleteContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.DELETE ) )
        {
            return;
        }

        Entry entry = getVisibleEntry( deleteContext.getDn() );

        if ( entry != null )
        {
            deletedEntries.put( deleteContext, entry );
        }
    }


    public void entryDeleted( DeleteOperationContext deleteContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.DELETE ) )
        {
            return;
        }

        Entry entry = deletedEntries.remove( deleteContext );

        if ( entry == null )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( deleteContext.getDn() );
        respEntry.setEntry( entry );
        setECResponseControl( respEntry, deleteContext, ChangeType.DELETE );
        session.getIoSession().write( respEntry );
    }


    public void entryModified( ModifyOperationContext modifyContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.MODIFY ) )
        {
            return;
        }

        Entry entry = getVisibleEntry( modifyContext.getDn() );

        if ( entry == null )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( modifyContext.getDn() );
        respEntry.setEntry( entry );

        setECResponseControl( respEntry, modifyContext, ChangeType.MODIFY );
        session.getIoSession().write( respEntry );
    }


    public void entryMoved( MoveOperationContext moveContext )
    {
        if ( !psearchControl.isNotificationEnabled( ChangeType.MODDN ) )
        {
            return;
        }

        Entry entry = getVisibleEntry( moveContext.getNewDn() );

        if ( entry == null )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( moveContext.getNewDn() );
        respEntry.setEntry( entry );
        
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

        Entry entry = getVisibleEntry( renameContext.getNewDn() );

        if ( entry == null )
        {
            return;
        }

        SearchResultEntry respEntry = new SearchResultEntryImpl( req.getMessageId() );
        respEntry.setObjectName( entry.getDn() );
        respEntry.setEntry( entry );
        
        setECResponseControl( respEntry, renameContext, ChangeType.MODDN );
        session.getIoSession().write( respEntry );
    }
    
    
    /**
     * Reads an entry the way a base level search done by the client would: the
     * search goes through the client's session, so the entry is only returned if
     * the client is authenticated and allowed to see it, and only with the attributes
     * (and values) it is allowed to read. The userPassword attribute is also removed
     * if the server is configured to hide it.
     *
     * @param dn The entry's Dn
     * @return The entry as visible by the client, or null if it can't see it
     */
    private Entry getVisibleEntry( Dn dn )
    {
        try ( Cursor<Entry> cursor = session.getCoreSession().search( dn, SearchScope.OBJECT,
            new PresenceNode( SchemaConstants.OBJECT_CLASS_AT ), AliasDerefMode.NEVER_DEREF_ALIASES, attributes ) )
        {
            if ( !cursor.next() )
            {
                return null;
            }

            Entry entry = cursor.get();

            if ( session.getCoreSession().getDirectoryService().isPasswordHidden() )
            {
                entry.removeAttributes( SchemaConstants.USER_PASSWORD_AT );
            }

            return entry;
        }
        catch ( Exception e )
        {
            // The client is not allowed to see the entry, or the entry does not exist anymore
            LOG.debug( "Entry {} not sent to the persistent search client: {}", dn, e.getMessage() );

            return null;
        }
    }
}
