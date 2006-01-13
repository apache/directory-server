/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.enumeration;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.DirectoryManager;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.common.exception.OperationAbandonedException;
import org.apache.ldap.common.message.AbandonListener;
import org.apache.ldap.common.message.AbandonableRequest;
import org.apache.ldap.common.name.LdapName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A enumeration decorator which filters database search results as they are
 * being enumerated back to the client caller.
 *
 * @see SearchResultFilter
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchResultFilteringEnumeration implements NamingEnumeration, AbandonListener
{
    /** the logger used by this class */
    private static final Logger log = LoggerFactory.getLogger( SearchResultFilteringEnumeration.class );

    /** the list of filters to be applied */
    private final List filters;
    /** the underlying decorated enumeration */
    private final NamingEnumeration decorated;

    /** the first accepted search result that is prefetched */
    private SearchResult prefetched;
    /** flag storing closed state of this naming enumeration */
    private boolean isClosed = false;
    /** the controls associated with the search operation */
    private final SearchControls searchControls;
    /** the Invocation that representing the search creating this enumeration */
    private final Invocation invocation;
    /** whether or not the caller context has object factories which need to be applied to the results */
    private final boolean applyObjectFactories;
    /** whether or not this search has been abandoned */
    private boolean abandoned = false;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a new database result filtering enumeration to decorate an
     * underlying enumeration.
     *
     * @param decorated the underlying decorated enumeration
     * @param searchControls the search controls associated with the search
     * creating this enumeration
     * @param invocation the invocation representing the seach that created this enumeration
     */
    public SearchResultFilteringEnumeration( NamingEnumeration decorated,
                                             SearchControls searchControls,
                                             Invocation invocation,
                                             SearchResultFilter filter )
            throws NamingException
    {
        this.searchControls = searchControls;
        this.invocation = invocation;
        this.filters = new ArrayList();
        this.filters.add( filter );
        this.decorated = decorated;
        this.applyObjectFactories = invocation.getCaller().getEnvironment().containsKey( Context.OBJECT_FACTORIES );

        if ( ! decorated.hasMore() )
        {
            close();
            return;
        }

        prefetch();
    }


    /**
     * Creates a new database result filtering enumeration to decorate an
     * underlying enumeration.
     *
     * @param decorated the underlying decorated enumeration
     * @param searchControls the search controls associated with the search
     * creating this enumeration
     * @param invocation the invocation representing the seach that created this enumeration
     */
    public SearchResultFilteringEnumeration( NamingEnumeration decorated,
                                             SearchControls searchControls,
                                             Invocation invocation,
                                             List filters )
            throws NamingException
    {
        this.searchControls = searchControls;
        this.invocation = invocation;
        this.filters = new ArrayList();
        this.filters.addAll( filters );
        this.decorated = decorated;
        this.applyObjectFactories = invocation.getCaller().getEnvironment().containsKey( Context.OBJECT_FACTORIES );

        if ( ! decorated.hasMore() )
        {
            close();
            return;
        }

        prefetch();
    }


    // ------------------------------------------------------------------------
    // New SearchResultFilter management methods
    // ------------------------------------------------------------------------


    /**
     * Adds a database search result filter to this filtering enumeration at
     * the very end of the filter list.  Filters are applied in the order of
     * addition.
     *
     * @param filter a filter to apply to the results
     * @return the result of {@link List#add(Object)}
     */
    public boolean addResultFilter( SearchResultFilter filter )
    {
        return filters.add( filter );
    }


    /**
     * Removes a database search result filter from the filter list of this
     * filtering enumeration.
     *
     * @param filter a filter to remove from the filter list
     * @return the result of {@link List#remove(Object)}
     */
    public boolean removeResultFilter( SearchResultFilter filter )
    {
        return filters.remove( filter );
    }


    /**
     * Gets an unmodifiable list of filters.
     *
     * @return the result of {@link Collections#unmodifiableList(List)}
     */
    public List getFilters()
    {
        return Collections.unmodifiableList( filters );
    }


    // ------------------------------------------------------------------------
    // NamingEnumeration Methods
    // ------------------------------------------------------------------------


    public void close() throws NamingException
    {
        isClosed = true;
        decorated.close();
    }


    public boolean hasMore()
    {
        return !isClosed;
    }


    public Object next() throws NamingException
    {
        SearchResult retVal = this.prefetched;
        prefetch();
        return retVal;
    }


    // ------------------------------------------------------------------------
    // Enumeration Methods
    // ------------------------------------------------------------------------


    public boolean hasMoreElements()
    {
        return !isClosed;
    }


    public Object nextElement()
    {
        SearchResult retVal = this.prefetched;

        try
        {
            prefetch();
        }
        catch ( NamingException e )
        {
            throw new RuntimeException( "Failed to prefetch.", e );
        }

        return retVal;
    }


    // ------------------------------------------------------------------------
    // Private utility methods
    // ------------------------------------------------------------------------


    private void applyObjectFactories( SearchResult result ) throws NamingException
    {
        // if already populated or no factories are available just return
        if ( result.getObject() != null || ! applyObjectFactories )
        {
            return;
        }

        DirContext ctx = ( DirContext ) invocation.getCaller();
        Hashtable env = ctx.getEnvironment();
        Attributes attrs = result.getAttributes();
        Name name = new LdapName( result.getName() );
        try
        {
            Object obj = DirectoryManager.getObjectInstance( null, name, ctx, env, attrs );
            result.setObject( obj );
        }
        catch ( Exception e )
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "ObjectFactories threw exception while attempting to generate an object for " );
            buf.append( result.getName() );
            buf.append( ". Call on SearchResult.getObject() will return null." );
            log.warn( buf.toString(), e );
        }
    }


    /**
     * Keeps getting results from the underlying decorated filter and applying
     * the filters until a result is accepted by all and set as the prefetced
     * result to return on the next() result request.  If no prefetched value
     * can be found before exhausting the decorated enumeration, then this and
     * the underlying enumeration is closed.
     *
     * @throws NamingException if there are problems getting results from the
     * underlying enumeration
     */
    private void prefetch() throws NamingException
    {
        SearchResult tmp;
        if ( abandoned )
        {
            this.close();
            throw new OperationAbandonedException();
        }

        outer:
        while( decorated.hasMore() )
        {
            boolean accepted = true;
            tmp = ( SearchResult ) decorated.next();

            // don't waste using a for loop if we got 0 or 1 element
            if ( filters.isEmpty() )
            {
                this.prefetched = tmp;
                applyObjectFactories( this.prefetched );
                return;
            }
            else if ( filters.size() == 1 )
            {
                accepted = ( ( SearchResultFilter ) filters.get( 0 ) )
                        .accept( invocation, tmp, searchControls );
                if ( accepted )
                {
                    this.prefetched = tmp;
                    applyObjectFactories( this.prefetched );
                    return;
                }

                continue;
            }

            // apply all filters shorting their application on result denials
            for ( int ii = 0; ii < filters.size(); ii ++ )
            {
                SearchResultFilter filter = ( SearchResultFilter ) filters.get( ii );
                accepted &= filter.accept( invocation, tmp, searchControls );

                if ( ! accepted )
                {
                    continue outer;
                }
            }

            /*
             * If we get here then a result has been accepted by all the
             * filters so we set the result as the prefetched value to return
             * on the following call to the next() or nextElement() methods
             */
            this.prefetched = tmp;
            applyObjectFactories( this.prefetched );
            return;
        }

        /*
         * If we get here then no result was found to be accepted by all
         * filters before we exhausted the decorated enumeration so we close
         */
        close();
    }


    public void requestAbandoned( AbandonableRequest req )
    {
        this.abandoned = true;
    }
}
