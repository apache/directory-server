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
package org.apache.directory.server.core.operational;


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.model.name.Ava;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.UsageEnum;
import org.apache.directory.shared.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that adds or modifies the default attributes
 * of entries. There are six default attributes for now;
 * <tt>'creatorsName'</tt>, <tt>'createTimestamp'</tt>, <tt>'modifiersName'</tt>,
 * <tt>'modifyTimestamp'</tt>, <tt>entryUUID</tt> and <tt>entryCSN</tt>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OperationalAttributeInterceptor extends BaseInterceptor
{
    /** The LoggerFactory used by this Interceptor */
    private static Logger LOG = LoggerFactory.getLogger( OperationalAttributeInterceptor.class );

    private final EntryFilter DENORMALIZING_SEARCH_FILTER = new OperationalAttributeDenormalizingSearchFilter();

    private final EntryFilter SEARCH_FILTER = new OperationalAttributeSearchFilter();

    /** The subschemasubentry Dn */
    private Dn subschemaSubentryDn;

    /** The admin Dn */
    private Dn adminDn;
    
    /** Some attributeTypes we use locally */
    private static AttributeType entryUuidAT;
    private static AttributeType entryCsnAT;
    private static AttributeType creatorsNameAT;
    private static AttributeType createTimeStampAT;
    private static AttributeType accessControlSubentriesAT;
    private static AttributeType collectiveAttributeSubentriesAT;
    private static AttributeType triggerExecutionSubentriesAT;
    private static AttributeType subschemaSubentryAT;

    /**
     * the search result filter to use for collective attribute injection
     */
    private class OperationalAttributeDenormalizingSearchFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        public boolean accept( SearchingOperationContext operation, Entry entry ) throws Exception
        {
            if ( operation.getReturningAttributesString() == null )
            {
                return true;
            }

            return filterDenormalized( entry );
        }
        
        
        /**
         * {@inheritDoc}
         */
        public String toString( String tabs )
        {
            return tabs + "OperationalAttributeDenormalizingSearchFilter";
        }
    }

    /**
     * the database search result filter to register with filter service
     */
    private class OperationalAttributeSearchFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        public boolean accept( SearchingOperationContext operation, Entry entry ) throws Exception
        {
            return operation.getReturningAttributesString() != null
                || filterOperationalAttributes( entry );
        }
        
        
        /**
         * {@inheritDoc}
         */
        public String toString( String tabs )
        {
            return tabs + "OperationalAttributeSearchFilter";
        }
    }


    /**
     * Creates the operational attribute management service interceptor.
     */
    public OperationalAttributeInterceptor()
    {
        super( InterceptorEnum.OPERATIONAL_ATTRIBUTE_INTERCEPTOR );
    }


    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        // stuff for dealing with subentries (garbage for now)
        Value<?> subschemaSubentry = directoryService.getPartitionNexus().getRootDse( null ).get(
            SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).get();
        subschemaSubentryDn = directoryService.getDnFactory().create( subschemaSubentry.getString() );

        // Create the Admin Dn
        adminDn = directoryService.getDnFactory().create( ServerDNConstants.ADMIN_SYSTEM_DN );
        
        // Initialize the AttributeType we use locally
        entryUuidAT = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT_OID );
        entryCsnAT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT_OID );
        creatorsNameAT = schemaManager.getAttributeType( SchemaConstants.CREATORS_NAME_AT );
        createTimeStampAT = schemaManager.getAttributeType( SchemaConstants.CREATE_TIMESTAMP_AT_OID );
        accessControlSubentriesAT = schemaManager.getAttributeType( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT_OID );
        collectiveAttributeSubentriesAT = schemaManager.getAttributeType( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT_OID );
        triggerExecutionSubentriesAT = schemaManager.getAttributeType( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        subschemaSubentryAT = schemaManager.getAttributeType( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
    }


    public void destroy()
    {
    }


    /**
     * Check if we have to add an operational attribute, or if the admin has injected one
     */
    private boolean checkAddOperationalAttribute( boolean isAdmin, Entry entry, AttributeType attribute ) throws LdapException
    {
        if ( entry.containsAttribute( attribute ) )
        {
            if ( !isAdmin )
            {
                // Wrong !
                String message = I18n.err( I18n.ERR_30, attribute );
                LOG.error( message );
                throw new LdapNoPermissionException( message );
            }
            else
            {
                return true;
            }
        }
        else
        {
            return false;
        }
    }


    /**
     * Adds extra operational attributes to the entry before it is added.
     * 
     * We add those attributes :
     * - creatorsName
     * - createTimestamp
     * - entryCSN
     * - entryUUID
     */
    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        String principal = getPrincipal( addContext ).getName();

        Entry entry = addContext.getEntry();

        // If we are using replication, the below four OAs may already be present and we retain
        // those values if the user is admin.
        boolean isAdmin = addContext.getSession().getAuthenticatedPrincipal().getName().equals(
            ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );

        // The EntryUUID attribute
        if ( !checkAddOperationalAttribute( isAdmin, entry, entryUuidAT ) )
        {
            entry.put( entryUuidAT, UUID.randomUUID().toString() );
        }

        // The EntryCSN attribute
        if ( !checkAddOperationalAttribute( isAdmin, entry, entryCsnAT ) )
        {
            entry.put( entryCsnAT, directoryService.getCSN().toString() );
        }

        // The CreatorsName attribute
        if ( !checkAddOperationalAttribute( isAdmin, entry, creatorsNameAT ) )
        {
            entry.put( creatorsNameAT, principal );
        }

        // The CreateTimeStamp attribute
        if ( !checkAddOperationalAttribute( isAdmin, entry, createTimeStampAT ) )
        {
            entry.put( createTimeStampAT, DateUtils.getGeneralizedTime() );
        }

        // Now, check that the user does not add operational attributes
        // The accessControlSubentries attribute
        checkAddOperationalAttribute( isAdmin, entry, accessControlSubentriesAT );

        // The CollectiveAttributeSubentries attribute
        checkAddOperationalAttribute( isAdmin, entry, collectiveAttributeSubentriesAT );

        // The TriggerExecutionSubentries attribute
        checkAddOperationalAttribute( isAdmin, entry, triggerExecutionSubentriesAT );

        // The SubSchemaSybentry attribute
        checkAddOperationalAttribute( isAdmin, entry, subschemaSubentryAT );

        next( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        EntryFilteringCursor cursor = next( listContext );
        cursor.addEntryFilter( SEARCH_FILTER );

        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        Entry result = next( lookupContext );

        if ( lookupContext.isAllUserAttributes() )
        {
            if ( lookupContext.isAllOperationalAttributes() )
            {
                // The user has requested '+' and '*', return everything.
                return result;
            }
            else
            {
                filter( lookupContext, result );
            }
        }
        else
        {
            if ( lookupContext.isAllOperationalAttributes() )
            {
                // Select the user attrinbutes from the result, depending on the returning attributes list
                filterUserAttributes( lookupContext, result );
            }
            else if ( ( lookupContext.getReturningAttributes() == null ) || ( lookupContext.getReturningAttributes().size() == 0 ) )
            {
                // No returning attributes, return all the user attributes
                // unless the user has requested no attributes
                if ( lookupContext.isNoAttributes() )
                {
                    result.clear();
                }
                else
                {
                    filterOperationalAttributes( result );
                }
            }
            else
            {
                // Deal with the returning attributes
                filterList( lookupContext, result );
            }
        }

        denormalizeEntryOpAttrs( result );

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // We must check that the user hasn't injected either the modifiersName
        // or the modifyTimestamp operational attributes : they are not supposed to be
        // added at this point EXCEPT in cases of replication by a admin user.
        // If so, remove them, and if there are no more attributes, simply return.
        // otherwise, inject those values into the list of modifications
        List<Modification> mods = modifyContext.getModItems();

        boolean isAdmin = modifyContext.getSession().getAuthenticatedPrincipal().getDn().equals( adminDn );

        boolean modifierAtPresent = false;
        boolean modifiedTimeAtPresent = false;
        boolean entryCsnAtPresent = false;
        Dn dn = modifyContext.getDn();

        for ( Modification modification : mods )
        {
            AttributeType attributeType = modification.getAttribute().getAttributeType();

            if ( attributeType.equals( MODIFIERS_NAME_AT ) )
            {
                if ( !isAdmin )
                {
                    String message = I18n.err( I18n.ERR_31 );
                    LOG.error( message );
                    throw new LdapNoPermissionException( message );
                }
                else
                {
                    modifierAtPresent = true;
                }
            }

            if ( attributeType.equals( MODIFY_TIMESTAMP_AT ) )
            {
                if ( !isAdmin )
                {
                    String message = I18n.err( I18n.ERR_32 );
                    LOG.error( message );
                    throw new LdapNoPermissionException( message );
                }
                else
                {
                    modifiedTimeAtPresent = true;
                }
            }

            if ( attributeType.equals( ENTRY_CSN_AT ) )
            {
                if ( !isAdmin )
                {
                    String message = I18n.err( I18n.ERR_32 );
                    LOG.error( message );
                    throw new LdapNoPermissionException( message );
                }
                else
                {
                    entryCsnAtPresent = true;
                }
            }

            if ( PWD_POLICY_STATE_ATTRIBUTE_TYPES.contains( attributeType ) && !isAdmin )
            {
                String message = I18n.err( I18n.ERR_32 );
                LOG.error( message );
                throw new LdapNoPermissionException( message );
            }
        }

        // Add the modification AT only if we are not trying to modify the SubentrySubschema
        if ( !dn.equals( subschemaSubentryDn ) )
        {
            if ( !modifierAtPresent )
            {
                // Inject the ModifiersName AT if it's not present
                Attribute attribute = new DefaultAttribute( MODIFIERS_NAME_AT, getPrincipal( modifyContext )
                    .getName() );

                Modification modifiersName = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                    attribute );

                mods.add( modifiersName );
            }

            if ( !modifiedTimeAtPresent )
            {
                // Inject the ModifyTimestamp AT if it's not present
                Attribute attribute = new DefaultAttribute( MODIFY_TIMESTAMP_AT, DateUtils
                    .getGeneralizedTime() );

                Modification timestamp = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );

                mods.add( timestamp );
            }

            if ( !entryCsnAtPresent )
            {
                String csn = directoryService.getCSN().toString();
                Attribute attribute = new DefaultAttribute( ENTRY_CSN_AT, csn );
                Modification updatedCsn = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );
                mods.add( updatedCsn );
            }
        }

        // Go down in the chain
        next( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        Entry modifiedEntry = moveContext.getOriginalEntry().clone();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal( moveContext ).getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        
        Attribute csnAt = new DefaultAttribute( ENTRY_CSN_AT, directoryService.getCSN().toString() );
        modifiedEntry.put( csnAt );

        modifiedEntry.setDn( moveContext.getNewDn() );
        moveContext.setModifiedEntry( modifiedEntry );

        next( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Entry modifiedEntry = moveAndRenameContext.getOriginalEntry().clone();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal( moveAndRenameContext ).getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        modifiedEntry.setDn( moveAndRenameContext.getNewDn() );
        
        Attribute csnAt = new DefaultAttribute( ENTRY_CSN_AT, directoryService.getCSN().toString() );
        modifiedEntry.put( csnAt );

        moveAndRenameContext.setModifiedEntry( modifiedEntry );

        next( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Entry entry = ( ( ClonedServerEntry ) renameContext.getEntry() ).getClonedEntry();
        entry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal( renameContext ).getName() );
        entry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        Entry modifiedEntry = renameContext.getOriginalEntry().clone();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal( renameContext ).getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        
        Attribute csnAt = new DefaultAttribute( ENTRY_CSN_AT, directoryService.getCSN().toString() );
        modifiedEntry.put( csnAt );

        renameContext.setModifiedEntry( modifiedEntry );

        next( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        EntryFilteringCursor cursor = next( searchContext );

        if ( searchContext.isAllOperationalAttributes()
            || ( searchContext.getReturningAttributes() != null && !searchContext.getReturningAttributes().isEmpty() ) )
        {
            if ( directoryService.isDenormalizeOpAttrsEnabled() )
            {
                cursor.addEntryFilter( DENORMALIZING_SEARCH_FILTER );
            }

            return cursor;
        }

        return cursor;
    }


    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        // insert a new CSN into the entry, this is for replication
        Entry entry = deleteContext.getEntry();        
        Attribute csnAt = new DefaultAttribute( ENTRY_CSN_AT, directoryService.getCSN().toString() );
        entry.put( csnAt );

        next( deleteContext );
    }


    /**
     * Filters out the operational attributes within a search results attributes.  The attributes are directly
     * modified.
     *
     * @param attributes the resultant attributes to filter
     * @return true always
     * @throws Exception if there are failures in evaluation
     */
    private boolean filterOperationalAttributes( Entry attributes ) throws LdapException
    {
        Set<AttributeType> removedAttributes = new HashSet<AttributeType>();

        // Build a list of attributeType to remove
        for ( Attribute attribute : attributes.getAttributes() )
        {
            AttributeType attributeType = attribute.getAttributeType();

            if ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS )
            {
                removedAttributes.add( attributeType );
            }
        }

        // Now remove the attributes which are not USERs
        for ( AttributeType attributeType : removedAttributes )
        {
            attributes.removeAttributes( attributeType );
        }

        return true;
    }


    /**
     * Filters out the user attributes within a search results attributes. The attributes are directly
     * modified.
     *
     * @param attributes the resultant attributes to filter
     * @return true always
     * @throws Exception if there are failures in evaluation
     */
    private boolean filterUserAttributes( LookupOperationContext lookupContext, Entry attributes ) throws LdapException
    {
        Set<String> removedAttributes = new HashSet<String>();

        // Build a list of attributeType to remove
        for ( Attribute attribute : attributes.getAttributes() )
        {
            AttributeType attributeType = attribute.getAttributeType();

            if ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS )
            {
                removedAttributes.add( attributeType.getOid() );
            }
        }

        // Now remove the attributes which are not in the list to be returned
        for ( String returningAttribute : lookupContext.getReturningAttributesString() )
        {
            removedAttributes.remove( returningAttribute );
        }

        // Now, remove the attributes from the result
        for ( String attribute : removedAttributes )
        {
            attributes.removeAttributes( attribute );
        }

        return true;
    }


    private void filter( LookupOperationContext lookupContext, Entry entry ) throws LdapException
    {
        Dn dn = lookupContext.getDn();
        String[] ids = lookupContext.getReturningAttributesString();

        // still need to protect against returning op attrs when ids is null
        if ( ( ids == null ) || ( ids.length == 0 ) )
        {
            filterOperationalAttributes( entry );
            return;
        }

        if ( dn.size() == 0 )
        {
            Set<AttributeType> removedAttributes = new HashSet<AttributeType>();

            for ( Attribute attribute : entry.getAttributes() )
            {
                AttributeType attributeType = attribute.getAttributeType();

                if ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS )
                {
                    // If it's not in the list of returning attribute, remove it
                    if ( !lookupContext.contains( schemaManager, attributeType.getOid() ) )
                    {
                        removedAttributes.add( attributeType );
                    }
                }
            }

            for ( AttributeType attributeType : removedAttributes )
            {
                entry.removeAttributes( attributeType );
            }
        }

        denormalizeEntryOpAttrs( entry );

        // do nothing past here since this explicity specifies which
        // attributes to include - backends will automatically populate
        // with right set of attributes using ids array
    }


    private void filterList( LookupOperationContext lookupContext, Entry entry ) throws LdapException
    {
        Dn dn = lookupContext.getDn();
        String[] ids = lookupContext.getReturningAttributesString();

        // still need to protect against returning op attrs when ids is null
        if ( ( ids == null ) || ( ids.length == 0 ) )
        {
            filterOperationalAttributes( entry );
            
            return;
        }

        if ( dn.size() == 0 )
        {
            Set<AttributeType> removedAttributes = new HashSet<AttributeType>();

            for ( Attribute attribute : entry.getAttributes() )
            {
                AttributeType attributeType = attribute.getAttributeType();

                // If it's not in the list of returning attribute, remove it
                if ( !lookupContext.contains( schemaManager, attributeType.getOid() ) )
                {
                    removedAttributes.add( attributeType );
                }
            }

            for ( AttributeType attributeType : removedAttributes )
            {
                entry.removeAttributes( attributeType );
            }
        }

        denormalizeEntryOpAttrs( entry );

        // do nothing past here since this explicity specifies which
        // attributes to include - backends will automatically populate
        // with right set of attributes using ids array
    }


    private void denormalizeEntryOpAttrs( Entry entry ) throws LdapException
    {
        if ( directoryService.isDenormalizeOpAttrsEnabled() )
        {
            Attribute attr = entry.get( SchemaConstants.CREATORS_NAME_AT );

            if ( attr != null )
            {
                Dn creatorsName = directoryService.getDnFactory().create( attr.getString() );

                attr.clear();
                attr.add( denormalizeTypes( creatorsName ).getName() );
            }

            attr = entry.get( SchemaConstants.MODIFIERS_NAME_AT );

            if ( attr != null )
            {
                Dn modifiersName = directoryService.getDnFactory().create( attr.getString() );

                attr.clear();
                attr.add( denormalizeTypes( modifiersName ).getName() );
            }

            attr = entry.get( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT );

            if ( attr != null )
            {
                Dn modifiersName = directoryService.getDnFactory().create( attr.getString() );

                attr.clear();
                attr.add( denormalizeTypes( modifiersName ).getName() );
            }
        }
    }


    /**
     * Does not create a new Dn but alters existing Dn by using the first
     * short name for an attributeType definition.
     * 
     * @param dn the normalized distinguished name
     * @return the distinuished name denormalized
     * @throws Exception if there are problems denormalizing
     */
    private Dn denormalizeTypes( Dn dn ) throws LdapException
    {
        Dn newDn = new Dn( schemaManager );
        int size = dn.size();

        for ( int pos = 0; pos < size; pos++ )
        {
            Rdn rdn = dn.getRdn( size - 1 - pos );

            if ( rdn.size() == 0 )
            {
                newDn = newDn.add( new Rdn() );
                continue;
            }
            else if ( rdn.size() == 1 )
            {
                String name = schemaManager.lookupAttributeTypeRegistry( rdn.getNormType() ).getName();
                String value = rdn.getNormValue().getString();
                newDn = newDn.add( new Rdn( name, value ) );
                continue;
            }

            // below we only process multi-valued rdns
            StringBuffer buf = new StringBuffer();

            for ( Iterator<Ava> atavs = rdn.iterator(); atavs.hasNext(); /**/)
            {
                Ava atav = atavs.next();
                String type = schemaManager.lookupAttributeTypeRegistry( rdn.getNormType() ).getName();
                buf.append( type ).append( '=' ).append( atav.getNormValue() );

                if ( atavs.hasNext() )
                {
                    buf.append( '+' );
                }
            }

            newDn = newDn.add( new Rdn( buf.toString() ) );
        }

        return newDn;
    }


    private boolean filterDenormalized( Entry entry ) throws Exception
    {
        denormalizeEntryOpAttrs( entry );
        return true;
    }
}