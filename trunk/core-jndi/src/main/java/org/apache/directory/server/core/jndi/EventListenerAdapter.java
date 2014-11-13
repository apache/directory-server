/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.jndi;


import javax.naming.Binding;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;

import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.entry.ServerEntryUtils;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A DirectoryListener implementation which adapts call back to methods 
 * notifying of changes to the DIT into NamingEvents for use with the ApacheDS
 * DirectoryService JNDI provider.
 * 
 * TODO for the time being bindings in NamingEvents generated are not relative 
 * to the source context which they should be.
 * 
 * TODO presume correctly manipulated entry values in opContext.getEntry() 
 * objects to function properly - at this point this is not handled in the
 * Interceptors and needs to be added for this adapter to populate the event
 * bindings.
 * 
 * TODO - Should we factor in the attributes to be returned in bindings? 
 * Perhaps this should be privided as search controls along with the info
 * we need to handle aliases, and referals?
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EventListenerAdapter implements DirectoryListener
{
    private static final Logger LOG = LoggerFactory.getLogger( EventListenerAdapter.class );
    private final NamingListener listener;
    private final ServerLdapContext source;


    public EventListenerAdapter( ServerLdapContext source, NamingListener listener )
    {
        this( source, listener, new SearchControls() );
    }


    public EventListenerAdapter( ServerLdapContext source, NamingListener listener, SearchControls controls )
    {
        this.source = source;
        this.listener = listener;
    }
    
    
    @Override
    public boolean isSynchronous()
    {
        return false; // always asynchronous
    }


    private void deliverNamingExceptionEvent( Exception e )
    {
        LOG.error( I18n.err( I18n.ERR_118 ), e );
        NamingExceptionEvent evt = null;

        if ( e instanceof NamingException )
        {
            evt = new NamingExceptionEvent( source, ( NamingException ) e );
        }
        else
        {
            NamingException ne = new NamingException( I18n.err( I18n.ERR_119 ) );
            ne.setRootCause( e );
            evt = new NamingExceptionEvent( source, ne );
        }

        listener.namingExceptionThrown( evt );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.event.DirectoryListener#entryAdded(org.apache.directory.server.core.interceptor.context.AddOperationContext)
     */
    public void entryAdded( AddOperationContext addContext )
    {
        try
        {
            Binding binding = new Binding( addContext.getDn().getName(),
                ServerEntryUtils.toBasicAttributes( addContext.getEntry() ), false );
            NamingEvent evt = new NamingEvent( source, NamingEvent.OBJECT_ADDED,
                binding, null, addContext );

            if ( listener instanceof NamespaceChangeListener )
            {
                ( ( NamespaceChangeListener ) listener ).objectAdded( evt );
            }
        }
        catch ( Exception e )
        {
            deliverNamingExceptionEvent( e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.event.DirectoryListener#entryDeleted(org.apache.directory.server.core.interceptor.context.DeleteOperationContext)
     */
    public void entryDeleted( DeleteOperationContext deleteContext )
    {
        try
        {
            if ( listener instanceof NamespaceChangeListener )
            {
                Binding binding = new Binding( deleteContext.getDn().getName(),
                    ServerEntryUtils.toBasicAttributes( deleteContext.getEntry() ), false );
                NamingEvent evt = new NamingEvent( source, NamingEvent.OBJECT_REMOVED, null,
                    binding, deleteContext );
                ( ( NamespaceChangeListener ) listener ).objectAdded( evt );
            }
        }
        catch ( Exception e )
        {
            deliverNamingExceptionEvent( e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.event.DirectoryListener#entryModified(org.apache.directory.server.core.interceptor.context.ModifyOperationContext)
     */
    public void entryModified( ModifyOperationContext modifyContext )
    {
        try
        {
            Binding newBinding = new Binding( modifyContext.getDn().getName(),
                ServerEntryUtils.toBasicAttributes( modifyContext.getEntry() ), false );
            Binding oldBinding = new Binding( modifyContext.getDn().getName(),
                ServerEntryUtils.toBasicAttributes( ( ( ClonedServerEntry ) modifyContext.getEntry() )
                    .getOriginalEntry() ), false );
            NamingEvent evt = new NamingEvent( source, NamingEvent.OBJECT_CHANGED,
                newBinding, oldBinding, modifyContext );

            if ( listener instanceof ObjectChangeListener )
            {
                ( ( ObjectChangeListener ) listener ).objectChanged( evt );
            }
        }
        catch ( Exception e )
        {
            deliverNamingExceptionEvent( e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.event.DirectoryListener#entryMoved(org.apache.directory.server.core.interceptor.context.MoveOperationContext)
     */
    public void entryMoved( MoveOperationContext moveContext )
    {
        try
        {
            if ( listener instanceof NamespaceChangeListener )
            {
                Binding newBinding = new Binding( moveContext.getDn().getName(),
                    ServerEntryUtils.toBasicAttributes( moveContext.getEntry() ), false );
                Binding oldBinding = new Binding( moveContext.getDn().getName(),
                    ServerEntryUtils.toBasicAttributes( ( ( ClonedServerEntry ) moveContext.getEntry() )
                        .getOriginalEntry() ), false );
                NamingEvent evt = new NamingEvent( source, NamingEvent.OBJECT_RENAMED,
                    newBinding, oldBinding, moveContext );
                ( ( NamespaceChangeListener ) listener ).objectRenamed( evt );
            }
        }
        catch ( Exception e )
        {
            deliverNamingExceptionEvent( e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.event.DirectoryListener#entryMovedAndRenamed(org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext)
     */
    public void entryMovedAndRenamed( MoveAndRenameOperationContext moveAndRenameContext )
    {
        try
        {
            if ( listener instanceof NamespaceChangeListener )
            {
                Binding newBinding = new Binding( moveAndRenameContext.getDn().getName(),
                    ServerEntryUtils.toBasicAttributes( moveAndRenameContext.getEntry() ), false );
                Binding oldBinding = new Binding( moveAndRenameContext.getDn().getName(),
                    ServerEntryUtils.toBasicAttributes( ( ( ClonedServerEntry ) moveAndRenameContext.getEntry() )
                        .getOriginalEntry() ), false );
                NamingEvent evt = new NamingEvent( source, NamingEvent.OBJECT_RENAMED,
                    newBinding, oldBinding, moveAndRenameContext );
                ( ( NamespaceChangeListener ) listener ).objectRenamed( evt );
            }
        }
        catch ( Exception e )
        {
            deliverNamingExceptionEvent( e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.event.DirectoryListener#entryRenamed(org.apache.directory.server.core.interceptor.context.RenameOperationContext)
     */
    public void entryRenamed( RenameOperationContext renameContext )
    {
        try
        {
            if ( listener instanceof NamespaceChangeListener )
            {
                Binding newBinding = new Binding( renameContext.getDn().getName(),
                    ServerEntryUtils.toBasicAttributes( renameContext.getEntry() ), false );
                Binding oldBinding = new Binding( renameContext.getDn().getName(),
                    ServerEntryUtils.toBasicAttributes( ( ( ClonedServerEntry ) renameContext.getEntry() )
                        .getOriginalEntry() ), false );
                NamingEvent evt = new NamingEvent( source, NamingEvent.OBJECT_RENAMED,
                    newBinding, oldBinding, null );
                ( ( NamespaceChangeListener ) listener ).objectRenamed( evt );
            }
        }
        catch ( Exception e )
        {
            deliverNamingExceptionEvent( e );
        }
    }
}
