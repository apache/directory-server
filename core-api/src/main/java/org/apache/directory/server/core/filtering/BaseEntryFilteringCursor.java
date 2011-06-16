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

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ClonedServerEntrySearch;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.CursorIterator;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.OperationAbandonedException;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.model.schema.UsageEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor which uses a list of filters to selectively return entries and/or
 * modify the contents of entries.  Uses lazy pre-fetching on positioning 
 * operations which means adding filters after creation will not miss candidate
 * entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BaseEntryFilteringCursor implements EntryFilteringCursor
{
    /** the logger used by this class */
    private static final Logger log = LoggerFactory.getLogger( BaseEntryFilteringCursor.class );

    /** the underlying wrapped search results Cursor */
    private final Cursor<Entry> wrapped;
    
    /** the parameters associated with the search operation */
    private final SearchingOperationContext operationContext;
    
    /** the list of filters to be applied */
    private final List<EntryFilter> filters;
    
    /** the first accepted search result that is pre fetched */
    private ClonedServerEntry prefetched;

    
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
    public BaseEntryFilteringCursor( Cursor<Entry> wrapped, 
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
    public BaseEntryFilteringCursor( Cursor<Entry> wrapped, SearchingOperationContext operationContext )
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
    public BaseEntryFilteringCursor( Cursor<Entry> wrapped, 
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

    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#isAbandoned()
     */
    public boolean isAbandoned()
    {
        return getOperationContext().isAbandoned();
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#setAbandoned(boolean)
     */
    public void setAbandoned( boolean abandoned )
    {
        getOperationContext().setAbandoned( abandoned );
        
        if ( abandoned )
        {
            log.info( "Cursor has been abandoned." );
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#addEntryFilter(org.apache.directory.server.core.filtering.EntryFilter)
     */
    public boolean addEntryFilter( EntryFilter filter )
    {
        return filters.add( filter );
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#removeEntryFilter(org.apache.directory.server.core.filtering.EntryFilter)
     */
    public boolean removeEntryFilter( EntryFilter filter )
    {
        return filters.remove( filter );
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#getEntryFilters()
     */
    public List<EntryFilter> getEntryFilters()
    {
        return Collections.unmodifiableList( filters );
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#getOperationContext()
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
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#after(org.apache.directory.server.core.entry.ClonedServerEntry)
     */
    public void after( ClonedServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /* 
     * @see Cursor#afterLast()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#afterLast()
     */
    public void afterLast() throws Exception
    {
        wrapped.afterLast();
        prefetched = null;
    }


    /* 
     * @see Cursor#available()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#available()
     */
    public boolean available()
    {
        return prefetched != null;
    }


    /* 
     * @see Cursor#before(java.lang.Object)
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#before(org.apache.directory.server.core.entry.ClonedServerEntry)
     */
    public void before( ClonedServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /* 
     * @see Cursor#beforeFirst()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#beforeFirst()
     */
    public void beforeFirst() throws Exception
    {
        wrapped.beforeFirst();
        prefetched = null;
    }


    /* 
     * @see Cursor#close()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#close()
     */
    public void close() throws Exception
    {
        wrapped.close();
        prefetched = null;
    }


    /* 
     * @see Cursor#close()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#close()
     */
    public void close( Exception reason ) throws Exception
    {
        wrapped.close( reason );
        prefetched = null;
    }
    
    
    public final void setClosureMonitor( ClosureMonitor monitor )
    {
        wrapped.setClosureMonitor( monitor );
    }


    /* 
     * @see Cursor#first()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#first()
     */
    public boolean first() throws Exception
    {
        if ( getOperationContext().isAbandoned() )
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
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#get()
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
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#isClosed()
     */
    public boolean isClosed() throws Exception
    {
        return wrapped.isClosed();
    }


    /* 
     * @see Cursor#last()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#last()
     */
    public boolean last() throws Exception
    {
        if ( getOperationContext().isAbandoned() )
        {
            log.info( "Cursor has been abandoned." );
            close();
            throw new OperationAbandonedException();
        }

        afterLast();
        return previous();
    }
    
    
    private void filterContents( ClonedServerEntry entry ) throws Exception
    {
        boolean typesOnly = getOperationContext().isTypesOnly();

        boolean returnAll = ( getOperationContext().getReturningAttributes() == null ||
            ( getOperationContext().isAllOperationalAttributes() && getOperationContext().isAllUserAttributes() ) ) && ( ! typesOnly );
        
        if ( returnAll )
        {
            return;
        }

        if ( getOperationContext().isNoAttributes() )
        {
            for ( AttributeType at : entry.getOriginalEntry().getAttributeTypes() )
            {
                entry.remove( entry.get( at ) );
            }
            
            return;
        }
        
        
        if ( getOperationContext().isAllUserAttributes() )
        {
            for ( AttributeType at : entry.getOriginalEntry().getAttributeTypes() )
            {
                boolean isNotRequested = true;
                
                for ( AttributeTypeOptions attrOptions:getOperationContext().getReturningAttributes() )
                {
                    if ( attrOptions.getAttributeType().equals( at ) || attrOptions.getAttributeType().isAncestorOf( at ) )
                    {
                        isNotRequested = false;
                        break;
                    }
                }
                
                boolean isNotUserAttribute = at.getUsage() != UsageEnum.USER_APPLICATIONS;
                
                if (  isNotRequested && isNotUserAttribute )
                {
                    entry.removeAttributes( at );
                }
                else if( typesOnly )
                {
                    entry.get( at ).clear();
                }
            }
            
            return;
        }
        
        if ( getOperationContext().isAllOperationalAttributes() )
        {
            for ( AttributeType at : entry.getOriginalEntry().getAttributeTypes() )
            {
                boolean isNotRequested = true;
                
                for ( AttributeTypeOptions attrOptions:getOperationContext().getReturningAttributes() )
                {
                    if ( attrOptions.getAttributeType().equals( at ) || attrOptions.getAttributeType().isAncestorOf( at ) )
                    {
                        isNotRequested = false;
                        break;
                    }
                }

                boolean isUserAttribute = at.getUsage() == UsageEnum.USER_APPLICATIONS;
                
                if ( isNotRequested && isUserAttribute )
                {
                    entry.removeAttributes( at );
                }
                else if( typesOnly )
                {
                    entry.get( at ).clear();
                }
            }
            
            return;
        }
        
        if ( getOperationContext().getReturningAttributes() != null )
        {
            for ( AttributeType at : entry.getOriginalEntry().getAttributeTypes() )
            {
                boolean isNotRequested = true;
                
                for ( AttributeTypeOptions attrOptions:getOperationContext().getReturningAttributes() )
                {
                    if ( attrOptions.getAttributeType().equals( at ) || attrOptions.getAttributeType().isAncestorOf( at ) )
                    {
                        isNotRequested = false;
                        break;
                    }
                }
    
                if ( isNotRequested )
                {
                    entry.removeAttributes( at );
                }
                else if( typesOnly )
                {
                    entry.get( at ).clear();
                }
            }
        }
    }
    
    
    /* 
     * @see Cursor#next()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#next()
     */
    public boolean next() throws Exception
    {
        if ( getOperationContext().isAbandoned() )
        {
            log.info( "Cursor has been abandoned." );
            close();
            throw new OperationAbandonedException();
        }
        
        ClonedServerEntry tempResult = null;
        
        outer: while ( wrapped.next() )
        {
            boolean accepted = true;
            
            Entry tempEntry = wrapped.get();
            
            if ( tempEntry instanceof ClonedServerEntry )
            {
                tempResult = ( ClonedServerEntry ) tempEntry;
            }
            else
            {
                tempResult = new ClonedServerEntrySearch( tempEntry );
            }
            
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
                filterContents( prefetched );
                return true;
            }
            
            if ( ( filters.size() == 1 ) &&  filters.get( 0 ).accept( getOperationContext(), tempResult ) )
            {
                prefetched = tempResult;
                filterContents( prefetched );
                return true;
            }
            
            /* E N D   O P T I M I Z A T I O N */
            for ( EntryFilter filter : filters )
            {
                // if a filter rejects then short and continue with outer loop
                if ( ! ( accepted &= filter.accept( getOperationContext(), tempResult ) ) )
                {
                    continue outer;
                }
            }
            
            /*
             * Here the entry has been accepted by all filters.
             */
            prefetched = tempResult;
            filterContents( prefetched );
            
            return true;
        }
        
        prefetched = null;
        return false;
    }


    /* 
     * @see Cursor#previous()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#previous()
     */
    public boolean previous() throws Exception
    {
        if ( getOperationContext().isAbandoned() )
        {
            log.info( "Cursor has been abandoned." );
            close();
            throw new OperationAbandonedException();
        }
        
        ClonedServerEntry tempResult = null;
        
        outer: while ( wrapped.previous() )
        {
            boolean accepted = true;
            tempResult = new ClonedServerEntrySearch( wrapped.get() );
            
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
                filterContents( prefetched );
                return true;
            }
            
            if ( ( filters.size() == 1 ) && filters.get( 0 ).accept( getOperationContext(), tempResult ) )
            {
                prefetched = tempResult;
                filterContents( prefetched );
                return true;
            }
            
            /* E N D   O P T I M I Z A T I O N */
            
            for ( EntryFilter filter : filters )
            {
                // if a filter rejects then short and continue with outer loop
                if ( ! ( accepted &= filter.accept( getOperationContext(), tempResult ) ) )
                {
                    continue outer;
                }
            }
            
            /*
             * Here the entry has been accepted by all filters.
             */
            prefetched = tempResult;
            filterContents( prefetched );
            return true;
        }
        
        prefetched = null;
        
        return false;
    }


    /* 
     * @see Iterable#iterator()
     */
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#iterator()
     */
    public Iterator<ClonedServerEntry> iterator()
    {
        return new CursorIterator<ClonedServerEntry>( this );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAfterLast() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isAfterLast()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isBeforeFirst() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isBeforeFirst()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isFirst() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isFirst()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLast() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isLast()" ) ) );
    }
}
