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

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.FilteringOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaUtils;
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
     * Creates a new instance of a CollectiveAttributeInterceptor.
     */
    public CollectiveAttributeInterceptor()
    {
        super( InterceptorEnum.COLLECTIVE_ATTRIBUTE_INTERCEPTOR );
    }

    /**
     * the search result filter to use for collective attribute injection
     */
    private class CollectiveAttributeFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        public boolean accept( SearchOperationContext operation, Entry entry ) throws LdapException
        {
            addCollectiveAttributes( operation, entry );

            return true;
        }
        
        
        /**
         * {@inheritDoc}
         */
        public String toString( String tabs )
        {
            return tabs + "CollectiveAttributeFilter";
        }
    }

    /** The CollectiveAttribute search filter */
    private final EntryFilter SEARCH_FILTER = new CollectiveAttributeFilter();


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
    public void add( AddOperationContext addContext ) throws LdapException
    {
        checkAdd( addContext.getDn(), addContext.getEntry() );

        next( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        Entry result = next( lookupContext );

        // do not add collective attributes
        if ( lookupContext.isSyncreplLookup() )
        {
            return result;
        }
        
        // Adding the collective attributes if any
        addCollectiveAttributes( lookupContext, result );

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        checkModify( modifyContext );

        next( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        EntryFilteringCursor cursor = next( searchContext );

        // only add collective attributes for non-syncrepl search
        if( !searchContext.isSyncreplSearch() )
        {
            cursor.addEntryFilter( SEARCH_FILTER );
        }

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
            for ( Attribute attribute : entry )
            {
                if ( attribute.getAttributeType().isCollective() )
                {
                    return;
                }
            }

            LOG.info( "A CollectiveAttribute subentry *should* have at least one collectiveAttribute" );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION,
                I18n.err( I18n.ERR_257_COLLECTIVE_SUBENTRY_WITHOUT_COLLECTIVE_AT ) );
        }

        if ( containsAnyCollectiveAttributes( entry ) )
        {
            /*
             * TODO: Replace the Exception and the ResultCodeEnum with the correct ones.
             */
            LOG.info(
                "Cannot add the entry {} : it contains some CollectiveAttributes and is not a collective subentry",
                entry );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION,
                I18n.err( I18n.ERR_241_CANNOT_STORE_COLLECTIVE_ATT_IN_ENTRY ) );
        }
    }


    /**
     * Check that we can modify an entry
     */
    private void checkModify( ModifyOperationContext modifyContext ) throws LdapException
    {
        List<Modification> mods = modifyContext.getModItems();
        Entry originalEntry = modifyContext.getEntry();
        Entry targetEntry = SchemaUtils.getTargetEntry( mods, originalEntry );

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
            LOG.info(
                "Cannot modify the entry {} : it contains some CollectiveAttributes and is not a collective subentry",
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
            Attribute attr = mod.getAttribute();
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
        for ( Attribute attribute : entry.getAttributes() )
        {
            AttributeType attributeType = attribute.getAttributeType();

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
     * @throws LdapException if there are problems accessing subentries
     */
    private void addCollectiveAttributes( FilteringOperationContext opContext, Entry entry )
        throws LdapException
    {
        CoreSession session = opContext.getSession();

        Attribute collectiveAttributeSubentries = ( ( ClonedServerEntry ) entry ).getOriginalEntry().get(
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
        Attribute collectiveExclusions = ( ( ClonedServerEntry ) entry ).getOriginalEntry().get(
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
         * For each collective subentry referenced by the entry we lookup the
         * attributes of the subentry and copy collective attributes from the
         * subentry into the entry.
         */
        for ( Value<?> value : collectiveAttributeSubentries )
        {
            String subentryDnStr = value.getString();
            Dn subentryDn = session.getDirectoryService().getDnFactory().create( subentryDnStr );

            /*
             * TODO - Instead of hitting disk here can't we leverage the
             * SubentryService to get us cached sub-entries so we're not
             * wasting time with a lookup here? It is ridiculous to waste
             * time looking up this sub-entry.
             */

            LookupOperationContext lookupContext = new LookupOperationContext( session, subentryDn,
                SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            Entry subentry = session.getDirectoryService().getPartitionNexus().lookup( lookupContext );

            for ( Attribute attribute : subentry.getAttributes() )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attrId = attributeType.getName();

                // Skip the attributes which are not collectve
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

                /*
                 * If not all attributes or this collective attribute requested specifically
                 * then bypass the inclusion process.
                 */
                if ( !opContext.isAllUserAttributes() && !opContext.contains( schemaManager, attributeType ) )
                {
                    continue;
                }

                Attribute subentryColAttr = subentry.get( attrId );
                Attribute entryColAttr = entry.get( attrId );

                /*
                 * If entry does not have attribute for collective attribute then create it.
                 */
                if ( entryColAttr == null )
                {
                    entryColAttr = new DefaultAttribute( schemaManager.lookupAttributeTypeRegistry( attrId ) );
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
}
