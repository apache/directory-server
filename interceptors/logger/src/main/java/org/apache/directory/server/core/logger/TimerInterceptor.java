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
package org.apache.directory.server.core.logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.NextInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interceptor used to log times to process each operation.
 * 
 * The way it works is that it gathers the time to process an operation 
 * into a global counter, which is logged every 1000 operations (when 
 * using the OPERATION_STATS logger). It's also possible to get the time for
 * each single operation if activating the OPERATION_TIME logger.
 * 
 * Thos two loggers must be set to DEBUG.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TimerInterceptor extends BaseInterceptor
{
    /** A aggregating logger */
    private static final Logger OPERATION_STATS = LoggerFactory.getLogger( "OPERATION_STATS" );

    /** An operation logger */
    private static final Logger OPERATION_TIME = LoggerFactory.getLogger( "OPERATION_TIME" );

    /** Speedup for logs */
    private static final boolean IS_DEBUG_STATS = OPERATION_STATS.isDebugEnabled();
    private static final boolean IS_DEBUG_TIME = OPERATION_TIME.isDebugEnabled();

    /** The Logger's name */
    private String name;
    
    /** Stats for the add operation */
    private static AtomicLong totalAdd = new AtomicLong( 0 );
    private static AtomicInteger nbAddCalls = new AtomicInteger( 0 );

    /** Stats for the bind operation */
    private static AtomicLong totalBind = new AtomicLong( 0 );
    private static AtomicInteger nbBindCalls = new AtomicInteger( 0 );

    /** Stats for the compare operation */
    private static AtomicLong totalCompare = new AtomicLong( 0 );
    private static AtomicInteger nbCompareCalls = new AtomicInteger( 0 );

    /** Stats for the delete operation */
    private static AtomicLong totalDelete = new AtomicLong( 0 );
    private static AtomicInteger nbDeleteCalls = new AtomicInteger( 0 );
    
    /** Stats for the GetRootDSE operation */
    private static AtomicLong totalGetRootDSE = new AtomicLong( 0 );
    private static AtomicInteger nbGetRootDSECalls = new AtomicInteger( 0 );
    
    /** Stats for the HasEntry operation */
    private static AtomicLong totalHasEntry = new AtomicLong( 0 );
    private static AtomicInteger nbHasEntryCalls = new AtomicInteger( 0 );

    /** Stats for the list operation */
    private static AtomicLong totalList = new AtomicLong( 0 );
    private static AtomicInteger nbListCalls = new AtomicInteger( 0 );
    
    /** Stats for the lookup operation */
    private static AtomicLong totalLookup = new AtomicLong( 0 );
    private static AtomicInteger nbLookupCalls = new AtomicInteger( 0 );
    
    /** Stats for the modify operation */
    private static AtomicLong totalModify = new AtomicLong( 0 );
    private static AtomicInteger nbModifyCalls = new AtomicInteger( 0 );
    
    /** Stats for the move operation */
    private static AtomicLong totalMove = new AtomicLong( 0 );
    private static AtomicInteger nbMoveCalls = new AtomicInteger( 0 );
    
    /** Stats for the moveAndRename operation */
    private static AtomicLong totalMoveAndRename = new AtomicLong( 0 );
    private static AtomicInteger nbMoveAndRenameCalls = new AtomicInteger( 0 );
    
    /** Stats for the rename operation */
    private static AtomicLong totalRename = new AtomicLong( 0 );
    private static AtomicInteger nbRenameCalls = new AtomicInteger( 0 );
    
    /** Stats for the search operation */
    private static AtomicLong totalSearch = new AtomicLong( 0 );
    private static AtomicInteger nbSearchCalls = new AtomicInteger( 0 );
    
    /** Stats for the unbind operation */
    private static AtomicLong totalUnbind = new AtomicLong( 0 );
    private static AtomicInteger nbUnbindCalls = new AtomicInteger( 0 );
    
    /**
     * 
     * Creates a new instance of TimerInterceptor.
     *
     * @param name This interceptor's name
     */
    public TimerInterceptor( String name )
    {
        this.name = name;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next.add( addContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbAddCalls.incrementAndGet();
            totalAdd.getAndAdd( delta );
    
            if ( nbAddCalls.get() % 1000 == 0 )
            {
                long average = totalAdd.get()/(nbAddCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average add = {} microseconds, nb adds = {}", average, nbAddCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta add = {}", name, delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void bind( NextInterceptor next, BindOperationContext bindContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( bindContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbBindCalls.incrementAndGet();
            totalBind.getAndAdd( delta );
    
            if ( nbBindCalls.get() % 1000 == 0 )
            {
                long average = totalBind.get()/(nbBindCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average bind = {} microseconds, nb binds = {}", average, nbBindCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta bind = {}", name, delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( NextInterceptor next, CompareOperationContext compareContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        boolean compare = next.compare( compareContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbCompareCalls.incrementAndGet();
            totalCompare.getAndAdd( delta );
    
            if ( nbCompareCalls.get() % 1000 == 0 )
            {
                long average = totalCompare.get()/(nbCompareCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average compare = {} microseconds, nb compares = {}", average, nbCompareCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta compare = {}", name, delta );
        }
        
        return compare;
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( deleteContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbDeleteCalls.incrementAndGet();
            totalDelete.getAndAdd( delta );
    
            if ( nbDeleteCalls.get() % 1000 == 0 )
            {
                long average = totalDelete.get()/(nbDeleteCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average delete = {} microseconds, nb deletes = {}", average, nbDeleteCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta delete = {}", name, delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void destroy()
    {
    }

    
    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }


    /**
     * {@inheritDoc}
     */
    public Entry getRootDSE( GetRootDSEOperationContext getRootDseContext )
        throws LdapException
    {
        long t0 = System.nanoTime();
        Entry rootDSE = next( getRootDseContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbGetRootDSECalls.incrementAndGet();
            totalGetRootDSE.getAndAdd( delta );
    
            if ( nbGetRootDSECalls.get() % 1000 == 0 )
            {
                long average = totalGetRootDSE.get()/(nbGetRootDSECalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average getRootDSE = {} microseconds, nb getRootDSEs = {}", average, nbGetRootDSECalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta getRootDSE = {}", name, delta );
        }
        
        return rootDSE;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( NextInterceptor next, EntryOperationContext hasEntryContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        boolean hasEntry = next.hasEntry( hasEntryContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbHasEntryCalls.incrementAndGet();
            totalHasEntry.getAndAdd( delta );
    
            if ( nbHasEntryCalls.get() % 1000 == 0 )
            {
                long average = totalHasEntry.get()/(nbHasEntryCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average hasEntry = {} microseconds, nb hasEntrys = {}", average, nbHasEntryCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta hasEntry = {}", name, delta );
        }
        
        return hasEntry;
    }

    
    /**
     * {@inheritDoc}
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext listContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        EntryFilteringCursor cursor = next.list( listContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbListCalls.incrementAndGet();
            totalList.getAndAdd( delta );
    
            if ( nbListCalls.get() % 1000 == 0 )
            {
                long average = totalList.get()/(nbListCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average list = {} microseconds, nb lists = {}", average, nbListCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta list = {}", name, delta );
        }
        
        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( NextInterceptor next, LookupOperationContext lookupContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        Entry entry = next.lookup( lookupContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbLookupCalls.incrementAndGet();
            totalLookup.getAndAdd( delta );
    
            if ( nbLookupCalls.get() % 1000 == 0 )
            {
                long average = totalLookup.get()/(nbLookupCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average lookup = {} microseconds, nb lookups = {}", average, nbLookupCalls.get() );
            }
        }
        
        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta lookup = {}", name, delta );
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next.modify( modifyContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbModifyCalls.incrementAndGet();
            totalModify.getAndAdd( delta );
    
            if ( nbModifyCalls.get() % 1000 == 0 )
            {
                long average = totalModify.get()/(nbModifyCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average modify = {} microseconds, nb modifys = {}", average, nbModifyCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta modify = {}", name, delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next.move( moveContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbMoveCalls.incrementAndGet();
            totalMove.getAndAdd( delta );
    
            if ( nbMoveCalls.get() % 1000 == 0 )
            {
                long average = totalMove.get()/(nbMoveCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average move = {} microseconds, nb moves = {}", average, nbMoveCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta move = {}", name, delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next.moveAndRename( moveAndRenameContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbMoveAndRenameCalls.incrementAndGet();
            totalMoveAndRename.getAndAdd( delta );
    
            if ( nbMoveAndRenameCalls.get() % 1000 == 0 )
            {
                long average = totalMoveAndRename.get()/(nbMoveAndRenameCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average moveAndRename = {} microseconds, nb moveAndRenames = {}", average, nbMoveAndRenameCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta moveAndRename = {}", name, delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next.rename( renameContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbRenameCalls.incrementAndGet();
            totalRename.getAndAdd( delta );
    
            if ( nbRenameCalls.get() % 1000 == 0 )
            {
                long average = totalRename.get()/(nbRenameCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average rename = {} microseconds, nb renames = {}", average, nbRenameCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta rename = {}", name, delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext searchContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        EntryFilteringCursor cursor = next.search( searchContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbSearchCalls.incrementAndGet();
            totalSearch.getAndAdd( delta );
    
            if ( nbSearchCalls.get() % 1000 == 0 )
            {
                long average = totalSearch.get()/(nbSearchCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average search = {} microseconds, nb searches = {}", average, nbSearchCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta search = {}", name, delta );
        }
        
        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( unbindContext );
        long delta = System.nanoTime() - t0;
        
        if ( IS_DEBUG_STATS )
        {
            nbUnbindCalls.incrementAndGet();
            totalUnbind.getAndAdd( delta );
    
            if ( nbUnbindCalls.get() % 1000 == 0 )
            {
                long average = totalUnbind.get()/(nbUnbindCalls.get() * 1000);
                OPERATION_STATS.debug( name + " : Average unbind = {} microseconds, nb unbinds = {}", average, nbUnbindCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta unbind = {}", name, delta );
        }
    }
}
