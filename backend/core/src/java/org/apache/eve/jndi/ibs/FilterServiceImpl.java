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
package org.apache.eve.jndi.ibs;


import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.naming.directory.*;

import org.apache.eve.db.SearchResultFilter;
import org.apache.eve.db.DbSearchResult;
import org.apache.eve.db.SearchResultEnumeration;
import org.apache.eve.db.ResultFilteringEnumeration;
import org.apache.eve.jndi.Invocation;
import org.apache.eve.jndi.BaseInterceptor;
import org.apache.eve.jndi.InvocationStateEnum;

import org.apache.ldap.common.filter.ExprNode;


/**
 * An interceptor based service which manages the filtering of result responses
 * back to callers.  This service is strictly post invocation based and
 * operates upon Attributes and SearchResults obtained through
 * {@link org.apache.eve.BackingStore#lookup(Name)},
 * {@link org.apache.eve.BackingStore#lookup(Name,String[])} and
 * {@link org.apache.eve.BackingStore#search(Name, Map, ExprNode, SearchControls)}
 * operations.
 * <p>
 * We try to limit the amount of filtering decorators used on search results and
 * lookup values to prevent inefficiencies such as the unecessary cloning of
 * Attributes.  Several other services may depend upon this service to modify
 * search and lookup results as callers access them.
 * </p><p>
 * This service should be registered last within after chain of the interceptor
 * framework.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class FilterServiceImpl extends BaseInterceptor
        implements FilterService
{
    /** the set of registered result filters for search */
    private final List resultFilters = new ArrayList();
    /** the set of registered entry filters for lookup operations */
    private final List lookupFilters = new ArrayList();


    // ------------------------------------------------------------------------
    // BaseInterceptor Overrides
    // ------------------------------------------------------------------------


    /**
     * @see FilterService#addLookupFilter(LookupFilter)
     */
    public boolean addLookupFilter( LookupFilter filter )
    {
        return lookupFilters.add( filter );
    }


    /**
     * @see FilterService#addSearchResultFilter(SearchResultFilter)
     */
    public boolean addSearchResultFilter( SearchResultFilter filter )
    {
        return resultFilters.add( filter );
    }


    // ------------------------------------------------------------------------
    // BaseInterceptor Overrides
    // ------------------------------------------------------------------------


    /**
     * @see BaseInterceptor#lookup(javax.naming.Name)
     */
    protected void lookup( Name dn ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            Attributes attributes = ( Attributes ) invocation.getReturnValue();
            Attributes retval = ( Attributes ) attributes.clone();
            LdapContext ctx = ( LdapContext ) invocation.getContextStack().peek();
            filter( ctx, dn, retval );
            invocation.setReturnValue( retval );
        }
    }


    /**
     * @see BaseInterceptor#lookup(javax.naming.Name, String[])
     */
    protected void lookup( Name dn, String[] ids ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            LdapContext ctx = ( LdapContext ) invocation.getContextStack().peek();
            Attributes attributes = ( Attributes ) invocation.getReturnValue();
            Attributes retval = ( Attributes ) attributes.clone();
            filter( ctx, dn, retval, ids );
            invocation.setReturnValue( retval );
        }
    }


    /**
     * @see BaseInterceptor#search(Name, Map, ExprNode, SearchControls)
     */
    protected void search( Name base, Map env, ExprNode filter,
                           SearchControls searchControls )
            throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            if ( searchControls.getReturningAttributes() != null )
            {
                return;
            }

            SearchResultEnumeration enum ;
            ResultFilteringEnumeration retval;
            LdapContext ctx = ( LdapContext ) invocation.getContextStack().peek();
            enum = ( SearchResultEnumeration ) invocation.getReturnValue();
            retval = new ResultFilteringEnumeration( enum, searchControls, ctx );
            retval.addResultFilter( new SearchResultFilter()
            {
                public boolean accept( LdapContext ctx, DbSearchResult result,
                                       SearchControls controls )
                        throws NamingException
                {
                    return FilterServiceImpl.this.accept( ctx, result, controls );
                }
            } );
            invocation.setReturnValue( retval );
        }
    }


    // ------------------------------------------------------------------------
    // Private methods used to apply filters
    // ------------------------------------------------------------------------


    /**
     * Applies the linear stack of result filters to the search result to be
     * returned to the user.
     *
     * @param result the copy of the database search result to accep, modify,
     * or reject before being returned
     * @param controls the search controls associated with the invocation
     * @param ctx the LDAP context that made the search call
     * @return true if this result should not be returned to the callers of a
     * search result enumeration
     * @throws NamingException if there are errors while applying the linear
     * composition of filters
     */
    private boolean accept( LdapContext ctx, DbSearchResult result, SearchControls controls )
            throws NamingException
    {
        boolean isAccepted = true;

        for ( int ii = 0; ii < resultFilters.size(); ii++ )
        {
            SearchResultFilter filter = ( SearchResultFilter ) resultFilters.get( ii );

            if ( ! ( isAccepted &= filter.accept( ctx, result, controls ) ) )
            {
                break;
            }
        }

        return isAccepted;
    }


    /**
     * Applies the linear stack of entry filters to the entry looked up and
     * eventually returned a caller of the lookup methods.
     *
     * @param ctx the LDAP context that made the lookup call
     * @param dn the distinguished name of the lookup entry being filtered
     * @param entry the attributes of the entry being filtered
     * @throws NamingException if there are errors while applying the linear
     * composition of filters
     */
    private void filter( LdapContext ctx, Name dn, Attributes entry ) throws NamingException
    {
        for ( int ii = 0; ii < lookupFilters.size(); ii++ )
        {
            ( ( LookupFilter ) lookupFilters.get( ii ) ).filter( ctx, dn, entry );
        }
    }


    /**
     * Applies the linear stack of entry filters to the entry looked up and
     * eventually returned a caller of the lookup methods.
     *
     * @param ctx the LDAP context that made the lookup call
     * @param dn the distinguished name of the lookup entry being filtered
     * @param entry the attributes of the entry being filtered
     * @param ids the attributes of the the lookup operation is supposed to
     * return
     * @throws NamingException if there are errors while applying the linear
     * composition of filters
     */
    private void filter( LdapContext ctx, Name dn, Attributes entry, String[] ids )
            throws NamingException
    {
        for ( int ii = 0; ii < lookupFilters.size(); ii++ )
        {
            ( ( LookupFilter ) lookupFilters.get( ii ) ).filter( ctx, dn, entry, ids );
        }
    }
}
