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


import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;


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
    /** The SchemaManager */
    private SchemaManager schemaManager;
    
    private PartitionNexus nexus;
    
    private CollectiveAttributesSchemaChecker collectiveAttributesSchemaChecker;


    /**
     * the search result filter to use for collective attribute injection
     */
    private final EntryFilter SEARCH_FILTER = new EntryFilter()
    {
        public boolean accept( SearchingOperationContext operation, ClonedServerEntry result )
            throws Exception
        {
            DN name = result.getDn();
            
            if ( name.isNormalized() == false )
            {
                name = DN.normalize( name, schemaManager.getNormalizerMapping() );
            }
            
            String[] retAttrs = operation.getSearchControls().getReturningAttributes();
            addCollectiveAttributes( operation, result, retAttrs );
            return true;
        }
    };

    public void init( DirectoryService directoryService ) throws Exception
    {
        super.init( directoryService );
        schemaManager = directoryService.getSchemaManager();
        nexus = directoryService.getPartitionNexus();
        collectiveAttributesSchemaChecker = new CollectiveAttributesSchemaChecker( nexus, schemaManager );
    }


    /**
     * Adds the set of collective attributes requested in the returning attribute list
     * and contained in subentries referenced by the entry. Excludes collective
     * attributes that are specified to be excluded via the 'collectiveExclusions'
     * attribute in the entry.
     *
     * @param opContext the context of the operation collective attributes 
     * are added to
     * @param entry the entry to have the collective attributes injected
     * @param retAttrs array or attribute type to be specifically included in the result entry(s)
     * @throws NamingException if there are problems accessing subentries
     */
    private void addCollectiveAttributes( OperationContext opContext, Entry entry, 
        String[] retAttrs ) throws Exception
    {
        EntryAttribute collectiveAttributeSubentries = 
            ((ClonedServerEntry)entry).getOriginalEntry().get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        
        /*
         * If there are no collective attribute subentries referenced then we 
         * have no collective attributes to inject to this entry.
         */
        if ( collectiveAttributeSubentries == null )
        {
            return;
        }
    
        /*
         * Before we proceed we need to lookup the exclusions within the entry 
         * and build a set of exclusions for rapid lookup.  We use OID values 
         * in the exclusions set instead of regular names that may have case 
         * variance.
         */
        EntryAttribute collectiveExclusions = 
            ((ClonedServerEntry)entry).getOriginalEntry().get( SchemaConstants.COLLECTIVE_EXCLUSIONS_AT );
        Set<String> exclusions = new HashSet<String>();
        
        if ( collectiveExclusions != null )
        {
            if ( collectiveExclusions.contains( SchemaConstants.EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT_OID )
                 || 
                 collectiveExclusions.contains( SchemaConstants.EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT  ) )
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
                AttributeType attrType = schemaManager.lookupAttributeTypeRegistry( value.getString() );
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
                retIdsSet.add( schemaManager.lookupAttributeTypeRegistry( retAttr ).getOid() );
            }
        }

        /*
         * For each collective subentry referenced by the entry we lookup the
         * attributes of the subentry and copy collective attributes from the
         * subentry into the entry.
         */
        for ( Value<?> value:collectiveAttributeSubentries )
        {
            String subentryDnStr = value.getString();
            DN subentryDn = new DN( subentryDnStr );
            
            /*
             * TODO - Instead of hitting disk here can't we leverage the 
             * SubentryService to get us cached sub-entries so we're not
             * wasting time with a lookup here? It is ridiculous to waste
             * time looking up this sub-entry. 
             */
            
            Entry subentry = opContext.lookup( subentryDn, ByPassConstants.LOOKUP_COLLECTIVE_BYPASS );
            
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

                    AttributeType retType = schemaManager.lookupAttributeTypeRegistry( retId );

                    if ( allSuperTypes.contains( retType ) )
                    {
                        retIdsSet.add( schemaManager.lookupAttributeTypeRegistry( attrId ).getOid() );
                        break;
                    }
                }

                /*
                 * If not all attributes or this collective attribute requested specifically
                 * then bypass the inclusion process.
                 */
                if ( !( retIdsSet.contains( SchemaConstants.ALL_USER_ATTRIBUTES ) || 
                    retIdsSet.contains( schemaManager.lookupAttributeTypeRegistry( attrId ).getOid() ) ) )
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
                    entryColAttr = new DefaultEntryAttribute( attrId, schemaManager.lookupAttributeTypeRegistry( attrId ) );
                    entry.put( entryColAttr );
                }

                /*
                 *  Add all the collective attribute values in the subentry
                 *  to the currently processed collective attribute in the entry.
                 */
                for ( Value<?> subentryColVal:subentryColAttr )
                {
                    entryColAttr.add( subentryColVal.getString() );
                }
            }
        }
    }
    
    
    private Set<AttributeType> getAllSuperTypes( AttributeType id ) throws Exception
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

    
    public Entry lookup( NextInterceptor nextInterceptor, LookupOperationContext opContext ) 
        throws Exception
    {
        Entry result = nextInterceptor.lookup( opContext );
        
        if ( result == null )
        {
            return null;
        }
        
        if ( ( opContext.getAttrsId() == null ) || ( opContext.getAttrsId().size() == 0 ) ) 
        {
            addCollectiveAttributes( opContext, result, SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        }
        else
        {
            addCollectiveAttributes( opContext, result, opContext.getAttrsIdArray() );
        }

        return result;
    }


    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws Exception
    {
        EntryFilteringCursor cursor = nextInterceptor.list( opContext );
        cursor.addEntryFilter( SEARCH_FILTER );
        return cursor;
    }


    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) throws Exception
    {
        EntryFilteringCursor cursor = nextInterceptor.search( opContext );
        cursor.addEntryFilter( SEARCH_FILTER );
        return cursor;
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
        collectiveAttributesSchemaChecker.checkModify( opContext,opContext.getDn(), opContext.getModItems() );

        next.modify( opContext );
    }
}
