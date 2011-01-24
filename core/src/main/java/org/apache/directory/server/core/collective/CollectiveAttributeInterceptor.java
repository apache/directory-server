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
import java.util.List;
import java.util.Set;

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
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.*;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interceptor based service dealing with collective attribute
 * management.  This service intercepts read operations on entries to
 * inject collective attribute value pairs into the response based on
 * the entires inclusion within collectiveAttributeSpecificAreas and
 * collectiveAttributeInnerAreas.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CollectiveAttributeInterceptor extends BaseInterceptor
{
    /** The LoggerFactory used by this Interceptor */
    private static Logger LOG = LoggerFactory.getLogger( CollectiveAttributeInterceptor.class );

    /**
     * the search result filter to use for collective attribute injection
     */
    private final EntryFilter SEARCH_FILTER = new EntryFilter()
    {
        public boolean accept( SearchingOperationContext operation, ClonedServerEntry result ) throws Exception
        {
            String[] retAttrs = operation.getSearchControls().getReturningAttributes();
            addCollectiveAttributes( operation, result, retAttrs );
            
            return true;
        }
    };
    //-------------------------------------------------------------------------------------
    // Initialization
    //-------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );
        
        LOG.debug( "CollectiveAttribute interceptor initilaized" );
    }


    // ------------------------------------------------------------------------
    // Interceptor Method Overrides
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        checkAdd( addContext.getDn(), addContext.getEntry() );

        next.add( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext listContext )
        throws LdapException
    {
        EntryFilteringCursor cursor = nextInterceptor.list( listContext );
        
        cursor.addEntryFilter( SEARCH_FILTER );
        
        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( NextInterceptor nextInterceptor, LookupOperationContext lookupContext ) throws LdapException
    {
        Entry result = nextInterceptor.lookup( lookupContext );

        // Adding the collective attributes if any
        if ( ( lookupContext.getAttrsId() == null ) || ( lookupContext.getAttrsId().size() == 0 ) )
        {
            addCollectiveAttributes( lookupContext, result, SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        }
        else
        {
            addCollectiveAttributes( lookupContext, result, lookupContext.getAttrsIdArray() );
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        checkModify( modifyContext );

        next.modify( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext searchContext )
        throws LdapException
    {
        EntryFilteringCursor cursor = nextInterceptor.search( searchContext );
        
        cursor.addEntryFilter( SEARCH_FILTER );

        return cursor;
    }


    //-------------------------------------------------------------------------------------
    // Helper methods
    //-------------------------------------------------------------------------------------
    /**
     * Check if we can add an entry. There are two cases : <br>
     * <ul>
     * <li>The entry is a normal entry : it should not contain any 'c-XXX' attributeType</li>
     * <li>The entry is a collectiveAttributeSubentry
     * </ul>
     */
    private void checkAdd( Dn normName, Entry entry ) throws LdapException
    {
        if ( entry.hasObjectClass( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            // This is a collectiveAttribute subentry. It must have at least one collective
            // attribute
            for ( EntryAttribute attribute : entry )
            {
                if ( attribute.getAttributeType().isCollective() )
                {
                    return;
                }
            }
            
            LOG.info( "A CollectiveAttribute subentry *should* have at least one collectiveAttribute" );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, I18n.err( I18n.ERR_257_COLLECTIVE_SUBENTRY_WITHOUT_COLLECTIVE_AT ) );
        }

        if ( containsAnyCollectiveAttributes( entry ) )
        {
            /*
             * TODO: Replace the Exception and the ResultCodeEnum with the correct ones.
             */
            LOG.info( "Cannot add the entry {} : it contains some CollectiveAttributes and is not a collective subentry",
                entry );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, I18n.err( I18n.ERR_241_CANNOT_STORE_COLLECTIVE_ATT_IN_ENTRY ) );
        }
    }

    
    /**
     * Check that we can modify an entry
     */
    private void checkModify( ModifyOperationContext modifyContext ) throws LdapException
    {
        List<Modification> mods = modifyContext.getModItems();
        Entry originalEntry = modifyContext.getEntry();
        Entry targetEntry = ( Entry ) SchemaUtils.getTargetEntry( mods, originalEntry );

        // If the modified entry contains the CollectiveAttributeSubentry, then the modification
        // is accepted, no matter what
        if ( targetEntry.contains( OBJECT_CLASS_AT, SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            return;
        }

        // Check that we don't add any collectve attribute, this is not allowed on normal entries
        if ( hasCollectiveAttributes( mods ) )
        {
            /*
             * TODO: Replace the Exception and the ResultCodeEnum with the correct ones.
             */
            LOG.info( "Cannot modify the entry {} : it contains some CollectiveAttributes and is not a collective subentry",
                targetEntry );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, I18n.err( I18n.ERR_242 ) );
        }
    }

    
    /**
     * Check that we have a CollectiveAttribute in the modifications. (CollectiveAttributes
     * are those with a name starting with 'c-').
     */
    private boolean hasCollectiveAttributes( List<Modification> mods ) throws LdapException
    {
        for ( Modification mod : mods )
        {
            // TODO: handle http://issues.apache.org/jira/browse/DIRSERVER-1198
            EntryAttribute attr = mod.getAttribute();
            AttributeType attrType = attr.getAttributeType();

            // Defensive programming. Very unlikely to happen here...
            if ( attrType == null )
            {
                try
                {
                    attrType = schemaManager.lookupAttributeTypeRegistry( attr.getUpId() );
                }
                catch ( LdapException le )
                {
                    throw new LdapInvalidAttributeTypeException();
                }
            }

            ModificationOperation modOp = mod.getOperation();

            // If the AT is collective and we don't try to remove it, then we can return.
            if ( attrType.isCollective() && ( modOp != ModificationOperation.REMOVE_ATTRIBUTE ) )
            {
                return true;
            }
        }

        // No collective attrbute found
        return false;
    }

    
    /**
     * Check if the entry contains any collective AttributeType (those starting with 'c-')
     */
    private boolean containsAnyCollectiveAttributes( Entry entry ) throws LdapException
    {
        Set<AttributeType> attributeTypes = entry.getAttributeTypes();

        for ( AttributeType attributeType : attributeTypes )
        {
            if ( attributeType.isCollective() )
            {
                return true;
            }
        }

        return false;
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
     * @throws LdapException if there are problems accessing subentries
     */
    private void addCollectiveAttributes( OperationContext opContext, Entry entry, String[] retAttrs ) throws LdapException
    {
        EntryAttribute collectiveAttributeSubentries = ( ( ClonedServerEntry ) entry ).getOriginalEntry().get(
            COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );

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
        EntryAttribute collectiveExclusions = ( ( ClonedServerEntry ) entry ).getOriginalEntry().get(
            COLLECTIVE_EXCLUSIONS_AT );
        Set<String> exclusions = new HashSet<String>();

        if ( collectiveExclusions != null )
        {
            if ( collectiveExclusions.contains( SchemaConstants.EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT_OID )
                || collectiveExclusions.contains( SchemaConstants.EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT ) )
            {
                /*
                 * This entry does not allow any collective attributes
                 * to be injected into itself.
                 */
                return;
            }

            exclusions = new HashSet<String>();

            for ( Value<?> value : collectiveExclusions )
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

        for ( String retAttr : retAttrs )
        {
            if ( retAttr.equals( SchemaConstants.ALL_USER_ATTRIBUTES )
                || retAttr.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
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
        for ( Value<?> value : collectiveAttributeSubentries )
        {
            String subentryDnStr = value.getString();
            Dn subentryDn = opContext.getSession().getDirectoryService().getDNFactory().create( subentryDnStr );

            /*
             * TODO - Instead of hitting disk here can't we leverage the 
             * SubentryService to get us cached sub-entries so we're not
             * wasting time with a lookup here? It is ridiculous to waste
             * time looking up this sub-entry. 
             */

            Entry subentry = opContext.lookup( subentryDn, ByPassConstants.LOOKUP_COLLECTIVE_BYPASS );

            for ( AttributeType attributeType : subentry.getAttributeTypes() )
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
                    if ( retId.equals( SchemaConstants.ALL_USER_ATTRIBUTES )
                        || retId.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
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
                if ( !( retIdsSet.contains( SchemaConstants.ALL_USER_ATTRIBUTES ) || retIdsSet.contains( schemaManager
                    .lookupAttributeTypeRegistry( attrId ).getOid() ) ) )
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
                    entryColAttr = new DefaultEntryAttribute( attrId, schemaManager
                        .lookupAttributeTypeRegistry( attrId ) );
                    entry.put( entryColAttr );
                }

                /*
                 *  Add all the collective attribute values in the subentry
                 *  to the currently processed collective attribute in the entry.
                 */
                for ( Value<?> subentryColVal : subentryColAttr )
                {
                    entryColAttr.add( subentryColVal.getString() );
                }
            }
        }
    }


    private Set<AttributeType> getAllSuperTypes( AttributeType id ) throws LdapException
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
}
