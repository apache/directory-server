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
package org.apache.directory.server.core.event;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.event.Evaluator;
import org.apache.directory.server.core.api.event.EventType;
import org.apache.directory.server.core.api.event.ExpressionEvaluator;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.event.RegistrationEntry;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.NextInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link org.apache.directory.server.core.api.interceptor.Interceptor} based service for notifying {@link
 * DirectoryListener}s of changes to the DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EventInterceptor extends BaseInterceptor
{
    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger( EventInterceptor.class );

    private Evaluator evaluator;
    private ExecutorService executor;


    /**
     * Initialize the event interceptor. It creates a pool of executor which will be used
     * to call the listeners in separate threads.
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        LOG.info( "Initializing ..." );
        super.init( directoryService );

        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        evaluator = new ExpressionEvaluator( schemaManager );
        executor = new ThreadPoolExecutor( 1, 10, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>( 100 ) );

        this.directoryService.setEventService( new DefaultEventService( directoryService ) );
        LOG.info( "Initialization complete." );
    }


    /**
     * Call the listener passing it the context.
     */
    private void fire( final OperationContext opContext, EventType type, final DirectoryListener listener )
    {
        switch ( type )
        {
            case ADD:
                executor.execute( new Runnable()
                {
                    public void run()
                    {
                        listener.entryAdded( ( AddOperationContext ) opContext );
                    }
                } );

                break;

            case DELETE:
                executor.execute( new Runnable()
                {
                    public void run()
                    {
                        listener.entryDeleted( ( DeleteOperationContext ) opContext );
                    }
                } );

                break;

            case MODIFY:
                executor.execute( new Runnable()
                {
                    public void run()
                    {
                        listener.entryModified( ( ModifyOperationContext ) opContext );
                    }
                } );

                break;

            case MOVE:
                executor.execute( new Runnable()
                {
                    public void run()
                    {
                        listener.entryMoved( ( MoveOperationContext ) opContext );
                    }
                } );

                break;

            case RENAME:
                executor.execute( new Runnable()
                {
                    public void run()
                    {
                        listener.entryRenamed( ( RenameOperationContext ) opContext );
                    }
                } );

                break;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( final AddOperationContext addContext ) throws LdapException
    {
        next( addContext );

        List<RegistrationEntry> selecting = getSelectingRegistrations( addContext.getDn(), addContext.getEntry() );

        if ( selecting.isEmpty() )
        {
            return;
        }

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isAdd( registration.getCriteria().getEventMask() ) )
            {
                fire( addContext, EventType.ADD, registration.getListener() );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( final DeleteOperationContext deleteContext ) throws LdapException
    {
        List<RegistrationEntry> selecting = getSelectingRegistrations( deleteContext.getDn(), deleteContext.getEntry() );
        next( deleteContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isDelete( registration.getCriteria().getEventMask() ) )
            {
                fire( deleteContext, EventType.DELETE, registration.getListener() );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void modify( final ModifyOperationContext modifyContext ) throws LdapException
    {
        Entry oriEntry = modifyContext.getEntry();

        List<RegistrationEntry> selecting = getSelectingRegistrations( modifyContext.getDn(), oriEntry );

        next( modifyContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        // Get the modified entry
        CoreSession session = modifyContext.getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, modifyContext.getDn() );
        lookupContext.setAttrsId( SchemaConstants.ALL_ATTRIBUTES_ARRAY );

        Entry alteredEntry = directoryService.getPartitionNexus().lookup( lookupContext );
        modifyContext.setAlteredEntry( alteredEntry );

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isModify( registration.getCriteria().getEventMask() ) )
            {
                fire( modifyContext, EventType.MODIFY, registration.getListener() );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Entry oriEntry = ((ClonedServerEntry)renameContext.getEntry()).getOriginalEntry();
        List<RegistrationEntry> selecting = getSelectingRegistrations( renameContext.getDn(), oriEntry );

        next( renameContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        // Get the modifed entry
        CoreSession session = renameContext.getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, renameContext.getNewDn() );
        lookupContext.setAttrsId( SchemaConstants.ALL_ATTRIBUTES_ARRAY );

        Entry alteredEntry = directoryService.getPartitionNexus().lookup( lookupContext );
        renameContext.setModifiedEntry( alteredEntry );

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isRename( registration.getCriteria().getEventMask() ) )
            {
                fire( renameContext, EventType.RENAME, registration.getListener() );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( NextInterceptor next, final MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Entry oriEntry = moveAndRenameContext.getOriginalEntry();
        List<RegistrationEntry> selecting = getSelectingRegistrations( moveAndRenameContext.getDn(), oriEntry );
        next.moveAndRename( moveAndRenameContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isMoveAndRename( registration.getCriteria().getEventMask() ) )
            {
                executor.execute( new Runnable()
                {
                    public void run()
                    {
                        registration.getListener().entryMovedAndRenamed( moveAndRenameContext );
                    }
                } );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        Entry oriEntry = moveContext.getOriginalEntry();
        List<RegistrationEntry> selecting = getSelectingRegistrations( moveContext.getDn(), oriEntry );

        next( moveContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isMove( registration.getCriteria().getEventMask() ) )
            {
                fire( moveContext, EventType.MOVE, registration.getListener() );
            }
        }
    }


    /**
     * Find a list of registrationEntries given an entry and a name. We check against
     * the criteria for each registrationEntry
     */
    private List<RegistrationEntry> getSelectingRegistrations( Dn name, Entry entry ) throws LdapException
    {
        if ( directoryService.getEventService().getRegistrationEntries().isEmpty() )
        {
            return Collections.emptyList();
        }

        List<RegistrationEntry> selecting = new ArrayList<RegistrationEntry>();

        for ( RegistrationEntry registration : directoryService.getEventService().getRegistrationEntries() )
        {
            NotificationCriteria criteria = registration.getCriteria();

            Dn base = criteria.getBase();

            // fix for DIRSERVER-1502
            if ( ( name.equals( base ) || name.isDescendantOf( base ) )
                && evaluator.evaluate( criteria.getFilter(), base, entry ) )
            {
                selecting.add( registration );
            }
        }

        return selecting;
    }
}
