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
package org.apache.directory.server.core.filtering;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.CursorIterator;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.shared.ldap.exception.OperationAbandonedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor which uses a list of filters to selectively return entries and/or
 * modify the contents of entries.  Uses lazy pre-fetching on positioning 
 * operations which means adding filters after creation will not miss candidate
 * entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EntryFilteringCursor implements Cursor<ClonedServerEntry>
{
    /** the logger used by this class */
    private static final Logger log = LoggerFactory.getLogger( EntryFilteringCursor.class );

    /** the underlying wrapped search results Cursor */
    private final Cursor<ServerEntry> wrapped;
    
    /** the parameters associated with the search operation */
    private final SearchingOperationContext operationContext;
    
    /** the list of filters to be applied */
    private final List<EntryFilter> filters;
    
    /** the first accepted search result that is pre fetched */
    private ClonedServerEntry prefetched;
    
    /** whether or not this search has been abandoned */
    private boolean abandoned = false;

    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    
    /**
     * Creates a new entry filtering Cursor over an existing Cursor using a 
     * single filter initially: more can be added later after creation.
     * 
     * @param wrapped the underlying wrapped Cursor whose entries are filtered
     * @param searchControls the controls of search that created this Cursor
     * @param invocation the search operation invocation creating this Cursor
     * @param filter a single filter to be used
     */
    public EntryFilteringCursor( Cursor<ServerEntry> wrapped, 
        SearchingOperationContext operationContext, EntryFilter filter )
    {
        this( wrapped, operationContext, Collections.singletonList( filter ) );
    }

    
    /**
     * Creates a new entry filtering Cursor over an existing Cursor using a 
     * no filter initially: more can be added later after creation.
     * 
     * @param wrapped the underlying wrapped Cursor whose entries are filtered
     * @param searchControls the controls of search that created this Cursor
     * @param invocation the search operation invocation creating this Cursor
     * @param filter a single filter to be used
     */
    public EntryFilteringCursor( Cursor<ServerEntry> wrapped, SearchingOperationContext operationContext )
    {
        this.wrapped = wrapped;
        this.operationContext = operationContext;
        this.filters = new ArrayList<EntryFilter>();
    }

    
    /**
     * Creates a new entry filtering Cursor over an existing Cursor using a 
     * list of filters initially: more can be added later after creation.
     * 
     * @param wrapped the underlying wrapped Cursor whose entries are filtered
     * @param operationContext the operation context that created this Cursor
     * @param invocation the search operation invocation creating this Cursor
     * @param filters a list of filters to be used
     */
    public EntryFilteringCursor( Cursor<ServerEntry> wrapped, 
        SearchingOperationContext operationContext, List<EntryFilter> filters )
    {
        this.wrapped = wrapped;
        this.operationContext = operationContext;
        this.filters = new ArrayList<EntryFilter>();
        this.filters.addAll( filters );
    }

    
    // ------------------------------------------------------------------------
    // Class Specific Methods
    // ------------------------------------------------------------------------

    
    /**
     * Gets whether or not this EntryFilteringCursor has been abandoned.
     *
     * @return true if abandoned, false if not
     */
    public boolean isAbandoned()
    {
        return abandoned;
    }
    
    
    /**
     * Sets whether this EntryFilteringCursor has been abandoned.
     *
     * @param abandoned true if abandoned, false if not
     */
    public void setAbandoned( boolean abandoned )
    {
        this.abandoned = abandoned;
        
        if ( abandoned )
        {
            log.info( "Cursor has been abandoned." );
        }
    }
    
    
    /**
     * Adds an entry filter to this EntryFilteringCursor at the very end of 
     * the filter list.  EntryFilters are applied in the order of addition.
     * 
     * @param filter a filter to apply to the entries
     * @return the result of {@link List#add(Object)}
     */
    public boolean addEntryFilter( EntryFilter filter )
    {
        return filters.add( filter );
    }
    
    
    /**
     * Removes an entry filter to this EntryFilteringCursor at the very end of 
     * the filter list.  
     * 
     * @param filter a filter to remove from the filter list
     * @return the result of {@link List#remove(Object)}
     */
    public boolean removeEntryFilter( EntryFilter filter )
    {
        return filters.remove( filter );
    }
    
    
    /**
     * Gets an unmodifiable list of EntryFilters applied.
     *
     * @return an unmodifiable list of EntryFilters applied
     */
    public List<EntryFilter> getEntryFilters()
    {
        return Collections.unmodifiableList( filters );
    }
    
    
    /**
     * @return the operationContext
     */
    public SearchingOperationContext getOperationContext()
    {
        return operationContext;
    }

    
    // ------------------------------------------------------------------------
    // Cursor Interface Methods
    // ------------------------------------------------------------------------

    
    /* 
     * @see Cursor#after(Object)
     */
    public void after( ClonedServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /* 
     * @see Cursor#afterLast()
     */
    public void afterLast() throws Exception
    {
        wrapped.afterLast();
        prefetched = null;
    }


    /* 
     * @see Cursor#available()
     */
    public boolean available()
    {
        return prefetched != null;
    }


    /* 
     * @see Cursor#before(java.lang.Object)
     */
    public void before( ClonedServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /* 
     * @see Cursor#beforeFirst()
     */
    public void beforeFirst() throws Exception
    {
        wrapped.beforeFirst();
        prefetched = null;
    }


    /* 
     * @see Cursor#close()
     */
    public void close() throws Exception
    {
        wrapped.close();
        prefetched = null;
    }


    /* 
     * @see Cursor#first()
     */
    public boolean first() throws Exception
    {
        if ( abandoned )
        {
            log.info( "Cursor has been abandoned." );
            close();
            throw new OperationAbandonedException();
        }
        
        beforeFirst();
        return next();
    }


    /* 
     * @see Cursor#get()
     */
    public ClonedServerEntry get() throws Exception
    {
        if ( available() )
        {
            return prefetched;
        }
        
        throw new InvalidCursorPositionException();
    }


    /* 
     * @see Cursor#isClosed()
     */
    public boolean isClosed() throws Exception
    {
        return wrapped.isClosed();
    }


    /* 
     * @see Cursor#isElementReused()
     */
    public boolean isElementReused()
    {
        return true;
    }


    /* 
     * @see Cursor#last()
     */
    public boolean last() throws Exception
    {
        if ( abandoned )
        {
            log.info( "Cursor has been abandoned." );
            close();
            throw new OperationAbandonedException();
        }

        afterLast();
        return previous();
    }


    /* 
     * @see Cursor#next()
     */
    public boolean next() throws Exception
    {
        if ( abandoned )
        {
            log.info( "Cursor has been abandoned." );
            close();
            throw new OperationAbandonedException();
        }
        
        ClonedServerEntry tempResult = null;
        outer: while ( wrapped.next() )
        {
            boolean accepted = true;
            tempResult = new ClonedServerEntry( wrapped.get() );
            
            /*
             * O P T I M I Z A T I O N
             * -----------------------
             * 
             * Don't want to waste cycles on enabling a loop for processing 
             * filters if we have zero or one filter.
             */
            
            if ( filters.isEmpty() )
            {
                prefetched = tempResult;
                return true;
            }
            
            if ( filters.size() == 1 )
            {
                if ( filters.get( 0 ).accept( operationContext, tempResult ) )
                {
                    prefetched = tempResult;
                    return true;
                }
            }
            
            /* E N D   O P T I M I Z A T I O N */
            
            for ( EntryFilter filter : filters )
            {
                // if a filter rejects then short and continue with outer loop
                if ( ! ( accepted &= filter.accept( operationContext, tempResult ) ) )
                {
                    continue outer;
                }
            }
            
            /*
             * Here the entry has been accepted by all filters.
             */
            
            prefetched = tempResult;
            return true;
        }
        
        prefetched = null;
        return false;
    }


    /* 
     * @see Cursor#previous()
     */
    public boolean previous() throws Exception
    {
        if ( abandoned )
        {
            log.info( "Cursor has been abandoned." );
            close();
            throw new OperationAbandonedException();
        }
        
        ClonedServerEntry tempResult = null;
        outer: while ( wrapped.previous() )
        {
            boolean accepted = true;
            tempResult = new ClonedServerEntry( wrapped.get() );
            
            /*
             * O P T I M I Z A T I O N
             * -----------------------
             * 
             * Don't want to waste cycles on enabling a loop for processing 
             * filters if we have zero or one filter.
             */
            
            if ( filters.isEmpty() )
            {
                prefetched = tempResult;
                return true;
            }
            
            if ( filters.size() == 1 )
            {
                if ( filters.get( 0 ).accept( operationContext, tempResult ) )
                {
                    prefetched = tempResult;
                    return true;
                }
            }
            
            /* E N D   O P T I M I Z A T I O N */
            
            for ( EntryFilter filter : filters )
            {
                // if a filter rejects then short and continue with outer loop
                if ( ! ( accepted &= filter.accept( operationContext, tempResult ) ) )
                {
                    continue outer;
                }
            }
            
            /*
             * Here the entry has been accepted by all filters.
             */
            
            prefetched = tempResult;
            return true;
        }
        
        prefetched = null;
        return false;
    }


    /* 
     * @see Iterable#iterator()
     */
    public Iterator<ClonedServerEntry> iterator()
    {
        return new CursorIterator<ClonedServerEntry>( this );
    }
}
