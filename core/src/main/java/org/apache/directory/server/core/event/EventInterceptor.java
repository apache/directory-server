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
import org.apache.directory.server.core.entry.ClonedServerEntry;
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
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} based service for notifying {@link 
 * DirectoryListener}s of changes to the DIT.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 666516 $
 */
public class EventInterceptor extends BaseInterceptor
{
    private final static Logger LOG = LoggerFactory.getLogger( EventInterceptor.class );

    
    private List<RegistrationEntry> registrations = new CopyOnWriteArrayList<RegistrationEntry>();
    private DirectoryService ds;
    private FilterNormalizingVisitor filterNormalizer;
    private Evaluator evaluator;
    private ExecutorService executor;
    
    
    @Override
    public void init( DirectoryService ds ) throws Exception
    {
        LOG.info( "Initializing ..." );
        super.init( ds );
        
        this.ds = ds;
        OidRegistry oidRegistry = ds.getSchemaManager().getGlobalOidRegistry();
        SchemaManager schemaManager = ds.getSchemaManager();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        filterNormalizer = new FilterNormalizingVisitor( ncn, schemaManager );
        evaluator = new ExpressionEvaluator( oidRegistry, schemaManager );
        executor = new ThreadPoolExecutor( 1, 10, 1000, TimeUnit.MILLISECONDS, 
            new ArrayBlockingQueue<Runnable>( 100 ) );
        
        this.ds.setEventService( new DefaultEventService() );
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
                });
                break;
            case DELETE:
                executor.execute( new Runnable() 
                {
                    public void run()
                    {
                        listener.entryDeleted( ( DeleteOperationContext ) opContext );
                    }
                });
                break;
            case MODIFY:
                executor.execute( new Runnable() 
                {
                    public void run()
                    {
                        listener.entryModified( ( ModifyOperationContext ) opContext );
                    }
                });
                break;
            case MOVE:
                executor.execute( new Runnable() 
                {
                    public void run()
                    {
                        listener.entryMoved( ( MoveOperationContext ) opContext );
                    }
                });
                break;
            case RENAME:
                executor.execute( new Runnable() 
                {
                    public void run()
                    {
                        listener.entryRenamed( ( RenameOperationContext ) opContext );
                    }
                });
                break;
        }
    }
    
    
    public void add( NextInterceptor next, final AddOperationContext opContext ) throws Exception
    {
        next.add( opContext );
        List<RegistrationEntry> selecting = getSelectingRegistrations( opContext.getDn(), opContext.getEntry() );

        if ( selecting.isEmpty() )
        {
            return;
        }

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isAdd( registration.getCriteria().getEventMask() ) )
            {
                fire( opContext, EventType.ADD, registration.getListener() );
            }
        }
    }


    public void delete( NextInterceptor next, final DeleteOperationContext opContext ) throws Exception
    {
        List<RegistrationEntry> selecting = getSelectingRegistrations( opContext.getDn(), opContext.getEntry() );
        next.delete( opContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isDelete( registration.getCriteria().getEventMask() ) )
            {
                fire( opContext, EventType.DELETE, registration.getListener() );
            }
        }
    }


    public void modify( NextInterceptor next, final ModifyOperationContext opContext ) throws Exception
    {
        ClonedServerEntry oriEntry = opContext.lookup( opContext.getDn(), ByPassConstants.LOOKUP_BYPASS );
        List<RegistrationEntry> selecting = getSelectingRegistrations( opContext.getDn(), oriEntry );
        
        next.modify( opContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        // Get the modified entry
        ClonedServerEntry alteredEntry = opContext.lookup( opContext.getDn(), ByPassConstants.LOOKUP_BYPASS );
        opContext.setAlteredEntry( alteredEntry );

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isModify( registration.getCriteria().getEventMask() ) )
            {
                fire( opContext, EventType.MODIFY, registration.getListener() );
            }
        }
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws Exception
    {
        ClonedServerEntry oriEntry = opContext.lookup( opContext.getDn(), ByPassConstants.LOOKUP_BYPASS );
        List<RegistrationEntry> selecting = getSelectingRegistrations( opContext.getDn(), oriEntry );
        
        next.rename( opContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        // Get the modifed entry
        ClonedServerEntry alteredEntry = opContext.lookup( opContext.getNewDn(), ByPassConstants.LOOKUP_BYPASS );
        opContext.setAlteredEntry( alteredEntry );
        
        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isRename( registration.getCriteria().getEventMask() ) )
            {
                fire( opContext, EventType.RENAME, registration.getListener() );
            }
        }
    }


    public void moveAndRename( NextInterceptor next, final MoveAndRenameOperationContext opContext ) throws Exception
    {
        ClonedServerEntry oriEntry = opContext.lookup( opContext.getDn(), ByPassConstants.LOOKUP_BYPASS );
        List<RegistrationEntry> selecting = getSelectingRegistrations( opContext.getDn(), oriEntry );
        next.moveAndRename( opContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        opContext.setAlteredEntry( opContext.lookup( opContext.getNewDn(), ByPassConstants.LOOKUP_BYPASS ) );
        
        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isMoveAndRename( registration.getCriteria().getEventMask() ) )
            {
                executor.execute( new Runnable() 
                {
                    public void run()
                    {
                        registration.getListener().entryMovedAndRenamed( opContext );
                    }
                });
            }
        }
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws Exception
    {
        ClonedServerEntry oriEntry = opContext.lookup( opContext.getDn(), ByPassConstants.LOOKUP_BYPASS );
        List<RegistrationEntry> selecting = getSelectingRegistrations( opContext.getDn(), oriEntry );
        
        next.move( opContext );

        if ( selecting.isEmpty() )
        {
            return;
        }

        for ( final RegistrationEntry registration : selecting )
        {
            if ( EventType.isMove( registration.getCriteria().getEventMask() ) )
            {
                fire( opContext, EventType.MOVE, registration.getListener() );
            }
        }
    }
    
    
    List<RegistrationEntry> getSelectingRegistrations( DN name, ServerEntry entry ) throws Exception
    {
        if ( registrations.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<RegistrationEntry> selecting = new ArrayList<RegistrationEntry>();
        
        for ( RegistrationEntry registration : registrations )
        {
            NotificationCriteria criteria = registration.getCriteria();
            
            if ( evaluator.evaluate( criteria.getFilter(), criteria.getBase().getNormName(), entry ) )
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
            criteria.getBase().normalize( ds.getSchemaManager().getNormalizerMapping() );
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
