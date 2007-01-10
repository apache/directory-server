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

import javax.naming.Name;
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
import org.apache.directory.server.core.subtree.SubentryService;
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
    /**
     * the search result filter to use for collective attribute injection
     */
    private final SearchResultFilter SEARCH_FILTER = new SearchResultFilter()
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
            throws NamingException
        {
            LdapDN name = new LdapDN(result.getName());
            filter( name, result.getAttributes() );
            searchFilter( name, result.getAttributes(), controls.getReturningAttributes() );
            return true;
        }
    };

    private AttributeTypeRegistry registry = null;
    private PartitionNexus nexus = null;


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );
        nexus = factoryCfg.getPartitionNexus();
        registry = factoryCfg.getRegistries().getAttributeTypeRegistry();
    }


    /**
     * Adds the set of collective attributes contained in subentries referenced
     * by the entry.  All collective attributes that are not exclused are added
     * to the entry from all subentries.
     *
     * @param name name of the entry being processed
     * @param entry the entry to have the collective attributes injected
     * @throws NamingException if there are problems accessing subentries
     */
    private void addCollectiveAttributes( Name name, Attributes entry ) throws NamingException
    {
        LdapDN normName = LdapDN.normalize( ( LdapDN ) name, registry.getNormalizerMapping() );
        Attributes entryWithCAS = nexus.lookup( normName, new String[] { "collectiveAttributeSubentries" } );
        Attribute subentries = entryWithCAS.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );

        if ( subentries == null )
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
                return;
            }

            exclusions = new HashSet();
            for ( int ii = 0; ii < collectiveExclusions.size(); ii++ )
            {
                AttributeType attrType = registry.lookup( ( String ) collectiveExclusions.get( ii ) );
                exclusions.add( attrType.getOid() );
            }
        }
        else
        {
            exclusions = Collections.EMPTY_SET;
        }

        /*
         * For each collective subentry referenced by the entry we lookup the
         * attributes of the subentry and copy collective attributes from the
         * subentry into the entry.
         */
        for ( int ii = 0; ii < subentries.size(); ii++ )
        {
            String subentryDnStr = ( String ) subentries.get( ii );
            LdapDN subentryDn = new LdapDN( subentryDnStr );
            Attributes subentry = nexus.lookup( subentryDn );
            NamingEnumeration attrIds = subentry.getIDs();
            while ( attrIds.hasMore() )
            {
                String attrId = ( String ) attrIds.next();
                AttributeType attrType = registry.lookup( attrId );

                // skip the addition of this collective attribute if it is excluded
                if ( exclusions.contains( attrType.getOid() ) )
                {
                    continue;
                }

                /*
                 * If the attribute type of the subentry attribute is collective
                 * then we need to add all the values of the collective attribute
                 * to the entry making sure we do not overwrite values already
                 * existing for the collective attribute in case multiple
                 * subentries add the same collective attributes to this entry.
                 */

                if ( attrType.isCollective() )
                {
                    Attribute subentryColAttr = subentry.get( attrId );
                    Attribute entryColAttr = entry.get( attrId );

                    // if entry does not have attribute for colattr then create it
                    if ( entryColAttr == null )
                    {
                        entryColAttr = new AttributeImpl( attrId );
                        entry.put( entryColAttr );
                    }

                    // add all the collective attribute values in the subentry to entry
                    for ( int jj = 0; jj < subentryColAttr.size(); jj++ )
                    {
                        entryColAttr.add( subentryColAttr.get( jj ) );
                    }
                }
            }
        }
    }


    /**
     * Filter that injects collective attributes into the entry.
     *
     * @param name name of the entry being filtered
     * @param attributes the resultant attributes with added collective attributes
     * @return true always
     */
    private boolean filter( Name name, Attributes attributes ) throws NamingException
    {
        addCollectiveAttributes( name, attributes );
        return true;
    }


    private void filter( Name dn, Attributes entry, String[] ids ) throws NamingException
    {
        // still need to return collective attrs when ids is null
        if ( ids == null )
        {
            return;
        }

        // now we can filter out even collective attributes from the requested return ids
        if ( dn.size() == 0 )
        {
            HashSet idsSet = new HashSet( ids.length );

            for ( int ii = 0; ii < ids.length; ii++ )
            {
                idsSet.add( ids[ii].toLowerCase() );
            }

            NamingEnumeration list = entry.getIDs();

            while ( list.hasMore() )
            {
                String attrId = ( ( String ) list.nextElement() ).toLowerCase();

                if ( !idsSet.contains( attrId ) )
                {
                    entry.remove( attrId );
                }
            }
        }

        // do nothing past here since this explicity specifies which
        // attributes to include - backends will automatically populate
        // with right set of attributes using ids array
    }
    
    private void searchFilter( Name dn, Attributes entry, String[] ids ) throws NamingException
    {
        // still need to return collective attrs when ids is null
        if ( ids == null )
        {
            return;
        }
        
        HashSet idsSet = new HashSet( ids.length );

        for ( int ii = 0; ii < ids.length; ii++ )
        {
            idsSet.add( ids[ii].toLowerCase() );
        }
        
        if ( idsSet.contains( "*" ) )
        {
            return;
        }

        NamingEnumeration list = entry.getIDs();

        while ( list.hasMore() )
        {
            String attrId = ( ( String ) list.nextElement() ).toLowerCase();
            
            AttributeType attrType = registry.lookup( attrId );
            if ( !attrType.isCollective() )
            {
                continue;
            }

            if ( !idsSet.contains( attrId ) )
            {
                entry.remove( attrId );
            }
        }

        // do nothing past here since this explicity specifies which
        // attributes to include - backends will automatically populate
        // with right set of attributes using ids array
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
        filter( name, result );
        return result;
    }
    

    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name, String[] attrIds ) throws NamingException
    {
        Attributes result = nextInterceptor.lookup( name, attrIds );
        if ( result == null )
        {
            return null;
        }
        filter( name, result );
        filter( name, result, attrIds );
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
}
