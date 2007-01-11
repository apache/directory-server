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
package org.apache.directory.server.core.collective;


import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * An interceptor based service dealing with collective attribute
 * management.  This service intercepts read operations on entries to
 * inject collective attribute value pairs into the response based on
 * the entires inclusion within collectiveAttributeSpecificAreas and
 * collectiveAttributeInnerAreas.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CollectiveAttributeService extends BaseInterceptor
{
    public static final String COLLECTIVE_ATTRIBUTE_SUBENTRIES = "collectiveAttributeSubentries";
    
    /**
     * the search result filter to use for collective attribute injection
     */
    private final SearchResultFilter SEARCH_FILTER = new SearchResultFilter()
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
            throws NamingException
        {
            LdapDN name = new LdapDN( result.getName() );
            name = LdapDN.normalize( name, attrTypeRegistry.getNormalizerMapping() );
            Attributes entry = result.getAttributes();
            String[] retAttrs = controls.getReturningAttributes();
            addCollectiveAttributes( name, entry, retAttrs );
            return true;
        }
    };

    private AttributeTypeRegistry attrTypeRegistry = null;
    private PartitionNexus nexus = null;


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );
        nexus = factoryCfg.getPartitionNexus();
        attrTypeRegistry = factoryCfg.getRegistries().getAttributeTypeRegistry();
    }


    /**
     * Adds the set of collective attributes requested in the returning attribute list
     * and contained in subentries referenced by the entry. Excludes collective
     * attributes that are specified to be excluded via the 'collectiveExclusions'
     * attribute in the entry.
     *
     * @param normName name of the entry being processed
     * @param entry the entry to have the collective attributes injected
     * @param retAttrs array or attribute type to be specifically included in the result entry(s)
     * @throws NamingException if there are problems accessing subentries
     */
    private void addCollectiveAttributes( LdapDN normName, Attributes entry, String[] retAttrs ) throws NamingException
    {
        Attributes entryWithCAS = nexus.lookup( normName, new String[] { COLLECTIVE_ATTRIBUTE_SUBENTRIES } );
        Attribute caSubentries = entryWithCAS.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES );

        /*
         * If there are no collective attribute subentries referenced
         * then we have no collective attributes to inject to this entry.
         */
        if ( caSubentries == null )
        {
            return;
        }
        
        /*
         * Before we proceed we need to lookup the exclusions within the
         * entry and build a set of exclusions for rapid lookup.  We use
         * OID values in the exclusions set instead of regular names that
         * may have case variance.
         */
        Attribute collectiveExclusions = entry.get( "collectiveExclusions" );
        Set exclusions;
        if ( collectiveExclusions != null )
        {
            if ( collectiveExclusions.contains( "2.5.18.0" )
                || collectiveExclusions.contains( "excludeAllCollectiveAttributes" ) )
            {
                /*
                 * This entry does not allow any collective attributes
                 * to be injected into itself.
                 */
                return;
            }

            exclusions = new HashSet();
            for ( int ii = 0; ii < collectiveExclusions.size(); ii++ )
            {
                AttributeType attrType = attrTypeRegistry.lookup( ( String ) collectiveExclusions.get( ii ) );
                exclusions.add( attrType.getOid() );
            }
        }
        else
        {
            exclusions = Collections.EMPTY_SET;
        }
        
        /*
         * If no attributes are requested specifically
         * then it means all user attributes are requested.
         * So populate the array with all user attributes indicator: "*".
         */
        if ( retAttrs == null )
        {
            retAttrs = new String[] { "*" };
        }
        
        /*
         * Construct a set of requested attributes for easier tracking.
         */ 
        HashSet retIdsSet = new HashSet( retAttrs.length );
        for ( int ii = 0; ii < retAttrs.length; ii++ )
        {
            retIdsSet.add( retAttrs[ii].toLowerCase() );
        }

        /*
         * For each collective subentry referenced by the entry we lookup the
         * attributes of the subentry and copy collective attributes from the
         * subentry into the entry.
         */
        for ( int ii = 0; ii < caSubentries.size(); ii++ )
        {
            String subentryDnStr = ( String ) caSubentries.get( ii );
            LdapDN subentryDn = new LdapDN( subentryDnStr );
            Attributes subentry = nexus.lookup( subentryDn );
            NamingEnumeration attrIds = subentry.getIDs();
            while ( attrIds.hasMore() )
            {
                String attrId = ( String ) attrIds.next();
                AttributeType attrType = attrTypeRegistry.lookup( attrId );

                if ( !attrType.isCollective() )
                {
                    continue;
                }
                
                /*
                 * Skip the addition of this collective attribute if it is excluded
                 * in the 'collectiveAttributes' attribute.
                 */
                if ( exclusions.contains( attrType.getOid() ) )
                {
                    continue;
                }

                /*
                 * If not all attributes or this collective attribute requested specifically
                 * then bypass the inclusion process.
                 */
                if ( !( retIdsSet.contains( "*" ) || retIdsSet.contains( attrId ) ) )
                {
                    /*
                     * TODO: Check if the requested attribute types list includes any type
                     *       that is a supertype of any collective attribute that applies
                     *       to this entry.
                     *       
                     * See: http://issues.apache.org/jira/browse/DIRSERVER-820
                     */
                    continue;
                }
                
                Attribute subentryColAttr = subentry.get( attrId );
                Attribute entryColAttr = entry.get( attrId );

                /*
                 * If entry does not have attribute for collective attribute then create it.
                 */
                if ( entryColAttr == null )
                {
                    entryColAttr = new AttributeImpl( attrId );
                    entry.put( entryColAttr );
                }

                /*
                 *  Add all the collective attribute values in the subentry
                 *  to the currently processed collective attribute in the entry.
                 */
                for ( int jj = 0; jj < subentryColAttr.size(); jj++ )
                {
                    entryColAttr.add( subentryColAttr.get( jj ) );
                }
            }
        }
    }


    // ------------------------------------------------------------------------
    // Interceptor Method Overrides
    // ------------------------------------------------------------------------

    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        Attributes result = nextInterceptor.lookup( name );
        if ( result == null )
        {
            return null;
        }
        addCollectiveAttributes( name, result, new String[] { "*" } );
        return result;
    }
    

    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name, String[] attrIds ) throws NamingException
    {
        Attributes result = nextInterceptor.lookup( name, attrIds );
        if ( result == null )
        {
            return null;
        }
        addCollectiveAttributes( name, result, attrIds );
        return result;
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, LdapDN base ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( base );
        Invocation invocation = InvocationStack.getInstance().peek();
        return new SearchResultFilteringEnumeration( e, new SearchControls(), invocation, SEARCH_FILTER );
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor, LdapDN base, Map env, ExprNode filter,
        SearchControls searchCtls ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.search( base, env, filter, searchCtls );
        Invocation invocation = InvocationStack.getInstance().peek();
        return new SearchResultFilteringEnumeration( e, searchCtls, invocation, SEARCH_FILTER );
    }
    
    
    /*
     * TODO: Add change inducing Interceptor methods to track and prevent
     *       modification of collective attributes over entries/subentries
     *       which are not of type collectiveAttributeSubentry.
     * 
     * See: http://issues.apache.org/jira/browse/DIRSERVER-821
     * See: http://issues.apache.org/jira/browse/DIRSERVER-822
     */
    
}
