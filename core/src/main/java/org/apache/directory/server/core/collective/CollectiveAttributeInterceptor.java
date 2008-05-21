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


import org.apache.commons.lang.NotImplementedException;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerSearchResult;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import java.util.HashSet;
import java.util.Set;


/**
 * An interceptor based service dealing with collective attribute
 * management.  This service intercepts read operations on entries to
 * inject collective attribute value pairs into the response based on
 * the entires inclusion within collectiveAttributeSpecificAreas and
 * collectiveAttributeInnerAreas.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CollectiveAttributeInterceptor extends BaseInterceptor
{
    /** The global registries */
    private Registries registries;
    
    /** The attributeType registry */
    private AttributeTypeRegistry atRegistry;
    
    private PartitionNexus nexus;
    
    private CollectiveAttributesSchemaChecker collectiveAttributesSchemaChecker;


    /**
     * the search result filter to use for collective attribute injection
     */
    private final SearchResultFilter SEARCH_FILTER = new SearchResultFilter()
    {
        public boolean accept( Invocation invocation, ServerSearchResult result, SearchControls controls )
            throws Exception
        {
            LdapDN name = ((ServerSearchResult)result).getDn();
            
            if ( name.isNormalized() == false )
            {
            	name = LdapDN.normalize( name, atRegistry.getNormalizerMapping() );
            }
            
            ServerEntry entry = result.getServerEntry();
            String[] retAttrs = controls.getReturningAttributes();
            addCollectiveAttributes( name, entry, retAttrs );
            result.setServerEntry( entry );
            return true;
        }
    };

    public void init( DirectoryService directoryService ) throws Exception
    {
        super.init( directoryService );
        nexus = directoryService.getPartitionNexus();
        atRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
        collectiveAttributesSchemaChecker = new CollectiveAttributesSchemaChecker( nexus, atRegistry );
        registries = directoryService.getRegistries();
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
    private void addCollectiveAttributes( LdapDN normName, ServerEntry entry, String[] retAttrs ) throws Exception
    {
        EntryAttribute caSubentries;

        //noinspection StringEquality
        if ( ( retAttrs == null ) || ( retAttrs.length != 1 ) || ( retAttrs[0] != SchemaConstants.ALL_USER_ATTRIBUTES ) )
        {
            ServerEntry entryWithCAS = nexus.lookup( new LookupOperationContext( registries, normName, new String[] { 
            	SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT_OID } ) );
            caSubentries = entryWithCAS.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        }
        else
        {
            caSubentries = entry.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        }
        
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
        EntryAttribute collectiveExclusions = entry.get( SchemaConstants.COLLECTIVE_EXCLUSIONS_AT );
        Set<String> exclusions = new HashSet<String>();
        
        if ( collectiveExclusions != null )
        {
            if ( collectiveExclusions.contains( SchemaConstants.EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT_OID )
                || collectiveExclusions.contains( SchemaConstants.EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT  ) )
            {
                /*
                 * This entry does not allow any collective attributes
                 * to be injected into itself.
                 */
                return;
            }

            exclusions = new HashSet<String>();
            
            for ( Value<?> value:collectiveExclusions )
            {
                AttributeType attrType = atRegistry.lookup( ( String ) value.get() );
                exclusions.add( attrType.getOid() );
            }
        }
        
        /*
         * If no attributes are requested specifically
         * then it means all user attributes are requested.
         * So populate the array with all user attributes indicator: "*".
         */
        if ( retAttrs == null )
        {
            retAttrs = SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY;
        }
        
        /*
         * Construct a set of requested attributes for easier tracking.
         */ 
        Set<String> retIdsSet = new HashSet<String>( retAttrs.length );
        
        for ( String retAttr:retAttrs )
        {
            if ( retAttr.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) ||
                retAttr.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
            {
                retIdsSet.add( retAttr );
            }
            else
            {
                retIdsSet.add( atRegistry.lookup( retAttr ).getOid() );
            }
        }

        /*
         * For each collective subentry referenced by the entry we lookup the
         * attributes of the subentry and copy collective attributes from the
         * subentry into the entry.
         */
        for ( Value<?> value:caSubentries )
        {
            String subentryDnStr = ( String ) value.get();
            LdapDN subentryDn = new LdapDN( subentryDnStr );
            ServerEntry subentry = nexus.lookup( new LookupOperationContext( registries, subentryDn ) );
            
            for ( AttributeType attributeType:subentry.getAttributeTypes() )
            {
                String attrId = attributeType.getName();
                
                if ( !attributeType.isCollective() )
                {
                    continue;
                }
                
                /*
                 * Skip the addition of this collective attribute if it is excluded
                 * in the 'collectiveAttributes' attribute.
                 */
                if ( exclusions.contains( attributeType.getOid() ) )
                {
                    continue;
                }
                
                Set<AttributeType> allSuperTypes = getAllSuperTypes( attributeType );

                for ( String retId : retIdsSet )
                {
                    if ( retId.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) || retId.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
                    {
                        continue;
                    }

                    AttributeType retType = atRegistry.lookup( retId );

                    if ( allSuperTypes.contains( retType ) )
                    {
                        retIdsSet.add( atRegistry.lookup( attrId ).getOid() );
                        break;
                    }
                }

                /*
                 * If not all attributes or this collective attribute requested specifically
                 * then bypass the inclusion process.
                 */
                if ( !( retIdsSet.contains( SchemaConstants.ALL_USER_ATTRIBUTES ) || 
                    retIdsSet.contains( atRegistry.lookup( attrId ).getOid() ) ) )
                {
                    continue;
                }
                
                EntryAttribute subentryColAttr = subentry.get( attrId );
                EntryAttribute entryColAttr = entry.get( attrId );

                /*
                 * If entry does not have attribute for collective attribute then create it.
                 */
                if ( entryColAttr == null )
                {
                    entryColAttr = new DefaultServerAttribute( attrId, atRegistry.lookup( attrId ) );
                    entry.put( entryColAttr );
                }

                /*
                 *  Add all the collective attribute values in the subentry
                 *  to the currently processed collective attribute in the entry.
                 */
                for ( Value<?> subentryColVal:subentryColAttr )
                {
                    entryColAttr.add( (String)subentryColVal.get() );
                }
            }
        }
    }
    
    
    private Set<AttributeType> getAllSuperTypes( AttributeType id ) throws NamingException
    {
        Set<AttributeType> allSuperTypes = new HashSet<AttributeType>();
        AttributeType superType = id;
        
        while ( superType != null )
        {
            superType = superType.getSuperior();
            
            if ( superType != null )
            {
                allSuperTypes.add( superType );
            }
        }
        
        return allSuperTypes;
    }


    // ------------------------------------------------------------------------
    // Interceptor Method Overrides
    // ------------------------------------------------------------------------
    public ServerEntry lookup( NextInterceptor nextInterceptor, LookupOperationContext opContext ) throws Exception
    {
        ServerEntry result = nextInterceptor.lookup( opContext );
        
        if ( result == null )
        {
            return null;
        }
        
        if ( ( opContext.getAttrsId() == null ) || ( opContext.getAttrsId().size() == 0 ) ) 
        {
            addCollectiveAttributes( opContext.getDn(), result, SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        }
        else
        {
            addCollectiveAttributes( opContext.getDn(), result, opContext.getAttrsIdArray() );
        }

        return result;
    }


    public Cursor<ServerEntry> list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws Exception
    {
        Cursor<ServerEntry> result = nextInterceptor.list( opContext );
        Invocation invocation = InvocationStack.getInstance().peek();
        
//        return new SearchResultFilteringEnumeration( result, new SearchControls(), invocation, SEARCH_FILTER, "List collective Filter" );
        
        // TODO not implemented
        throw new NotImplementedException();
    }


    public Cursor<ServerEntry> search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) throws Exception
    {
        Cursor<ServerEntry> result = nextInterceptor.search( opContext );
        Invocation invocation = InvocationStack.getInstance().peek();
        
//        return new SearchResultFilteringEnumeration( 
//            result, opContext.getSearchControls(), invocation, SEARCH_FILTER, "Search collective Filter" );
        
        // TODO not implemented
        throw new NotImplementedException();
    }
    
    // ------------------------------------------------------------------------
    // Partial Schema Checking
    // ------------------------------------------------------------------------
    
    public void add( NextInterceptor next, AddOperationContext opContext ) throws Exception
    {
        collectiveAttributesSchemaChecker.checkAdd( opContext.getDn(), opContext.getEntry() );
        
        next.add( opContext );
    }


    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        collectiveAttributesSchemaChecker.checkModify( opContext.getRegistries(),opContext.getDn(), opContext.getModItems() );

        next.modify( opContext );
    }
}
