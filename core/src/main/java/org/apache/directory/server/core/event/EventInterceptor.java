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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.normalization.FilterNormalizingVisitor;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link org.apache.directory.server.core.interceptor.Interceptor} based service for notifying {@link
 * DirectoryListener}s of changes to the DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EventInterceptor extends BaseInterceptor
{
    private final static Logger LOG = LoggerFactory.getLogger( EventInterceptor.class );

    private List<RegistrationEntry> registrations = new CopyOnWriteArrayList<RegistrationEntry>();
    private FilterNormalizingVisitor filterNormalizer;
    private Evaluator evaluator;
    private ExecutorService executor;


    @Override
    public void init( DirectoryService directpryService ) throws LdapException
    {
        LOG.info( "Initializing ..." );
        super.init( directpryService );

        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        filterNormalizer = new FilterNormalizingVisitor( ncn, schemaManager );
        evaluator = new ExpressionEvaluator( schemaManager );
        executor = new ThreadPoolExecutor( 1, 10, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>( 100 ) );

        this.directoryService.setEventService( new DefaultEventService() );
        LOG.info( "Initialization complete." );
    }


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


    public void add( NextInterceptor next, final AddOperationContext addContext ) throws LdapException
    {
        next.add( addContext );
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


    public void delete( NextInterceptor next, final DeleteOperationContext deleteContext ) throws LdapException
    {
        List<RegistrationEntry> selecting = getSelectingRegistrations( deleteContext.getDn(), deleteContext.getEntry() );
        next.delete( deleteContext );

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


    public void modify( NextInterceptor next, final ModifyOperationContext modifyContext ) throws LdapException
    {
        Entry oriEntry = modifyContext.getEntry();

        List<RegistrationEntry> selecting = getSelectingRegistrations( modifyContext.getDn(), oriEntry );

        next.modify( modifyContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        // Get the modified entry
        Entry alteredEntry = modifyContext.lookup( modifyContext.getDn(), ByPassConstants.LOOKUP_BYPASS );
        modifyContext.setAlteredEntry( alteredEntry );

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isModify( registration.getCriteria().getEventMask() ) )
            {
                fire( modifyContext, EventType.MODIFY, registration.getListener() );
            }
        }
    }


    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        Entry oriEntry = renameContext.getEntry().getOriginalEntry();
        List<RegistrationEntry> selecting = getSelectingRegistrations( renameContext.getDn(), oriEntry );

        next.rename( renameContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        // Get the modifed entry
        Entry alteredEntry = renameContext.lookup( renameContext.getNewDn(), ByPassConstants.LOOKUP_BYPASS );
        renameContext.setModifiedEntry( alteredEntry );

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isRename( registration.getCriteria().getEventMask() ) )
            {
                fire( renameContext, EventType.RENAME, registration.getListener() );
            }
        }
    }


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
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        Entry oriEntry = moveContext.getOriginalEntry();
        List<RegistrationEntry> selecting = getSelectingRegistrations( moveContext.getDn(), oriEntry );

        next.move( moveContext );

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


    List<RegistrationEntry> getSelectingRegistrations( Dn name, Entry entry ) throws LdapException
    {
        if ( registrations.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<RegistrationEntry> selecting = new ArrayList<RegistrationEntry>();

        for ( RegistrationEntry registration : registrations )
        {
            NotificationCriteria criteria = registration.getCriteria();

            Dn base = criteria.getBase();

            if ( !criteria.getFilter().isSchemaAware() )
            {
                criteria.getFilter().accept( filterNormalizer );
            }

            // fix for DIRSERVER-1502
            if ( ( name.equals( base ) || name.isChildOf( base ) )
                && evaluator.evaluate( criteria.getFilter(), base, entry ) )
            {
                selecting.add( registration );
            }
        }

        return selecting;
    }

    // -----------------------------------------------------------------------
    // EventService Inner Class
    // -----------------------------------------------------------------------

    class DefaultEventService implements EventService
    {
        /*
         * Does not need normalization since default values in criteria is used.
         */
        public void addListener( DirectoryListener listener )
        {
            registrations.add( new RegistrationEntry( listener ) );
        }


        /*
         * Normalizes the criteria filter and the base.
         */
        public void addListener( DirectoryListener listener, NotificationCriteria criteria ) throws Exception
        {
            criteria.getBase().normalize( directoryService.getSchemaManager() );
            ExprNode result = ( ExprNode ) criteria.getFilter().accept( filterNormalizer );
            criteria.setFilter( result );
            registrations.add( new RegistrationEntry( listener, criteria ) );
        }


        public void removeListener( DirectoryListener listener )
        {
            for ( RegistrationEntry entry : registrations )
            {
                if ( entry.getListener() == listener )
                {
                    registrations.remove( entry );
                }
            }
        }


        public List<RegistrationEntry> getRegistrationEntries()
        {
            return Collections.unmodifiableList( registrations );
        }
    }
}
