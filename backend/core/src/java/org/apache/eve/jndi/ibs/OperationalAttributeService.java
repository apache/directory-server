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


import javax.naming.Name;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.*;

import org.apache.eve.RootNexus;
import org.apache.eve.db.DbSearchResult;
import org.apache.eve.db.SearchResultFilter;
import org.apache.eve.jndi.Invocation;
import org.apache.eve.jndi.BaseInterceptor;
import org.apache.eve.jndi.InvocationStateEnum;
import org.apache.eve.schema.GlobalRegistries;

import org.apache.ldap.common.util.DateUtils;


/**
 * An interceptor based service which manages the creation and modification of
 * operational attributes as operations are performed.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OperationalAttributeService extends BaseInterceptor
{
    /** the default user principal or DN */
    private final String DEFAULT_PRINCIPAL = "cn=admin,ou=system";
    /** the database search result filter to register with filter service */
    private final SearchResultFilter SEARCH_FILTER = new SearchResultFilter()
    {
        public boolean accept( DbSearchResult result, SearchControls controls )
        {
            if ( controls.getReturningAttributes() == null )
            {
                return filter( result.getAttributes() );
            }

            return true;
        }
    };
    /** the lookup filter to register with filter service */
    private final LookupFilter LOOKUP_FILTER = new LookupFilter()
    {
        public void filter( Name dn, Attributes entry )
        {
            OperationalAttributeService.this.filter( entry );
        }

        public void filter( Name dn, Attributes entry, String[] ids )
        {
            // do nothing since this explicity specifies which attributes
            // to include - backends will automatically populate with right
            // set of attributes
        }
    };

    /** the root nexus of the system */
    private final RootNexus nexus;
    /** a service used to filter search and lookup operations */
    private ResultFilteringService filteringService;
    /** the global schema object registries */
    private final GlobalRegistries globalRegistries;


    /**
     * Creates the operational attribute management service interceptor.
     *
     * @param nexus the root nexus of the system
     * @param globalRegistries the global schema object registries
     */
    public OperationalAttributeService( RootNexus nexus,
                                        GlobalRegistries globalRegistries,
                                        ResultFilteringService filteringService )
    {
        this.nexus = nexus;
        if ( this.nexus == null )
        {
            throw new NullPointerException( "the nexus cannot be null" );
        }

        this.globalRegistries = globalRegistries;
        if ( this.globalRegistries == null )
        {
            throw new NullPointerException( "the global registries cannot be null" );
        }

        this.filteringService = filteringService;
        if ( this.filteringService == null )
        {
            throw new NullPointerException( "the filter service cannot be null" );
        }

        this.filteringService.addLookupFilter( LOOKUP_FILTER );
        this.filteringService.addSearchResultFilter( SEARCH_FILTER );
    }


    /**
     * Adds extra operational attributes to the entry before it is added.
     *
     * @see BaseInterceptor#add(String, Name, Attributes)
     */
    protected void add( String upName, Name normName, Attributes entry ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            BasicAttribute attribute = new BasicAttribute( "creatorsName" );
            attribute.add( getPrincipal( invocation ) );
            entry.put( attribute );

            attribute = new BasicAttribute( "createTimestamp" );
            attribute.add( DateUtils.getGeneralizedTime( System.currentTimeMillis() ) );
            entry.put( attribute );
        }
    }


    protected void modify( Name dn, int modOp, Attributes mods ) throws NamingException
    {
        Invocation invocation = getInvocation();

        // add operational attributes after call in case the operation fails
        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            Attributes attributes = new BasicAttributes();
            BasicAttribute attribute = new BasicAttribute( "modifiersName" );
            attribute.add( getPrincipal( invocation ) );
            attributes.put( attribute );

            attribute = new BasicAttribute( "modifyTimestamp" );
            attribute.add( DateUtils.getGeneralizedTime( System.currentTimeMillis() ) );
            attributes.put( attribute );

            nexus.modify( dn, DirContext.REPLACE_ATTRIBUTE, attributes );
        }
    }


    protected void modify( Name dn, ModificationItem[] mods ) throws NamingException
    {
        super.modify( dn, mods );

        Invocation invocation = getInvocation();

        // add operational attributes after call in case the operation fails
        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            Attributes attributes = new BasicAttributes();
            BasicAttribute attribute = new BasicAttribute( "modifiersName" );
            attribute.add( getPrincipal( invocation ) );
            attributes.put( attribute );

            attribute = new BasicAttribute( "modifyTimestamp" );
            attribute.add( DateUtils.getGeneralizedTime( System.currentTimeMillis() ) );
            attributes.put( attribute );

            nexus.modify( dn, DirContext.REPLACE_ATTRIBUTE, attributes );
        }
    }


    protected void modifyRdn( Name dn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        Invocation invocation = getInvocation();

        // add operational attributes after call in case the operation fails
        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            Attributes attributes = new BasicAttributes();
            BasicAttribute attribute = new BasicAttribute( "modifiersName" );
            attribute.add( getPrincipal( invocation ) );
            attributes.put( attribute );

            attribute = new BasicAttribute( "modifyTimestamp" );
            attribute.add( DateUtils.getGeneralizedTime( System.currentTimeMillis() ) );
            attributes.put( attribute );

            Name newDn = ( Name ) dn.clone();
            newDn.remove( newDn.size() - 1 );
            newDn.add( newRdn );
            nexus.modify( newDn, DirContext.REPLACE_ATTRIBUTE, attributes );
        }
    }


    protected void move( Name oriChildName, Name newParentName ) throws NamingException
    {
        Invocation invocation = getInvocation();

        // add operational attributes after call in case the operation fails
        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            Attributes attributes = new BasicAttributes();
            BasicAttribute attribute = new BasicAttribute( "modifiersName" );
            attribute.add( getPrincipal( invocation ) );
            attributes.put( attribute );

            attribute = new BasicAttribute( "modifyTimestamp" );
            attribute.add( DateUtils.getGeneralizedTime( System.currentTimeMillis() ) );
            attributes.put( attribute );

            nexus.modify( newParentName, DirContext.REPLACE_ATTRIBUTE, attributes );
        }
    }


    protected void move( Name oriChildName, Name newParentName, String newRdn,
                         boolean deleteOldRdn ) throws NamingException
    {
        Invocation invocation = getInvocation();

        // add operational attributes after call in case the operation fails
        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            Attributes attributes = new BasicAttributes();
            BasicAttribute attribute = new BasicAttribute( "modifiersName" );
            attribute.add( getPrincipal( invocation ) );
            attributes.put( attribute );

            attribute = new BasicAttribute( "modifyTimestamp" );
            attribute.add( DateUtils.getGeneralizedTime( System.currentTimeMillis() ) );
            attributes.put( attribute );

            nexus.modify( newParentName, DirContext.REPLACE_ATTRIBUTE, attributes );
        }
    }


    /**
     * Filters out the operational attributes within a search results
     * attributes.  The attributes are directly modified.
     *
     * @param attributes the resultant attributes to filter
     * @return true always
     */
    private boolean filter( Attributes attributes )
    {
        attributes.remove( "creatorsName" );
        attributes.remove( "modifiersName" );
        attributes.remove( "createTimestamp" );
        attributes.remove( "modifyTimestamp" );
        return true;
    }


    /**
     * Gets the DN of the principal associated with this operation.
     *
     * @param invocation the invocation to get the principal for
     * @return the principal as a String
     * @throws NamingException if there are problems
     */
    private String getPrincipal( Invocation invocation ) throws NamingException
    {
        String principal;
        Context ctx = ( ( Context ) invocation.getContextStack().peek() );
        principal = ( String ) ctx.getEnvironment().get( Context.SECURITY_PRINCIPAL );
        return principal == null ? DEFAULT_PRINCIPAL : principal;
    }
}
