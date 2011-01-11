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
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.AVA;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.util.DateUtils;
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

    private final EntryFilter DENORMALIZING_SEARCH_FILTER = new EntryFilter()
    {
        public boolean accept( SearchingOperationContext operation, ClonedServerEntry serverEntry ) throws Exception
        {
            if ( operation.getSearchControls().getReturningAttributes() == null )
            {
                return true;
            }

            return filterDenormalized( serverEntry );
        }
    };

    /**
     * the database search result filter to register with filter service
     */
    private final EntryFilter SEARCH_FILTER = new EntryFilter()
    {
        public boolean accept( SearchingOperationContext operation, ClonedServerEntry entry ) throws Exception
        {
            return operation.getSearchControls().getReturningAttributes() != null
                || filterOperationalAttributes( entry );
        }
    };

    private DirectoryService service;

    /** The subschemasubentry DN */
    private DN subschemaSubentryDn;

    /** The admin DN */
    private DN adminDn;

    /** The schemaManager */
    private SchemaManager schemaManager;

    private static AttributeType MODIFIERS_NAME_AT;
    private static AttributeType MODIFY_TIMESTAMP_AT;
    private static AttributeType ENTRY_CSN_AT;

    /**
     * Creates the operational attribute management service interceptor.
     */
    public OperationalAttributeInterceptor()
    {
    }


    public void init( DirectoryService directoryService ) throws LdapException
    {
        service = directoryService;
        schemaManager = directoryService.getSchemaManager();

        // stuff for dealing with subentries (garbage for now)
        Value<?> subschemaSubentry = service.getPartitionNexus().getRootDSE( null ).get(
            SchemaConstants.SUB_SCHEMA_SUBENTRY_AT ).get();
        subschemaSubentryDn = service.getDNFactory().create( subschemaSubentry.getString() );

        // Create the Admin DN 
        adminDn = service.getDNFactory().create( ServerDNConstants.ADMIN_SYSTEM_DN );

        // Init the At we use locally
        MODIFIERS_NAME_AT = schemaManager.getAttributeType( SchemaConstants.MODIFIERS_NAME_AT );
        MODIFY_TIMESTAMP_AT = schemaManager.getAttributeType( SchemaConstants.MODIFY_TIMESTAMP_AT );
        ENTRY_CSN_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        
        // Init the ApSeqNuber ATs and SubentriesUUID AT
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.ACCESS_CONTROL_SEQ_NUMBER_AT );
        AP_SEQUENCE_NUMBER_ATTRIBUTE_TYPES.add( attributeType );
        attributeType = schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.ACCESS_CONTROL_SUBENTRIES_UUID_AT );
        SUBENTRIES_UUID_ATTRIBUTE_TYPES.add( attributeType );

        attributeType = schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT );
        AP_SEQUENCE_NUMBER_ATTRIBUTE_TYPES.add( attributeType );
        attributeType = schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_UUID_AT );
        SUBENTRIES_UUID_ATTRIBUTE_TYPES.add( attributeType );
        
        attributeType = schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.SUB_SCHEMA_SEQ_NUMBER_AT );
        AP_SEQUENCE_NUMBER_ATTRIBUTE_TYPES.add( attributeType );
        attributeType = schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.SUB_SCHEMA_SUBENTRY_UUID_AT );
        SUBENTRIES_UUID_ATTRIBUTE_TYPES.add( attributeType );

        attributeType = schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.TRIGGER_EXECUTION_SEQ_NUMBER_AT );
        AP_SEQUENCE_NUMBER_ATTRIBUTE_TYPES.add( attributeType );
        attributeType = schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_UUID_AT );
        SUBENTRIES_UUID_ATTRIBUTE_TYPES.add( attributeType );
    }


    public void destroy()
    {
    }
    
    
    private void addOpAttr( boolean isAdmin, Entry entry, String attributeType, String value ) throws LdapNoPermissionException
    {
        if ( entry.containsAttribute( attributeType ) )
        {
            if ( !isAdmin )
            {
                // Wrong !
                String message = I18n.err( I18n.ERR_30, attributeType );
                LOG.error( message );
                throw new LdapNoPermissionException( message );
            }
        }
        else
        {
            entry.put( attributeType, value );
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
    public void add( NextInterceptor nextInterceptor, AddOperationContext addContext ) throws LdapException
    {
        String principal = getPrincipal().getName();

        Entry entry = addContext.getEntry();

        // If we are using replication, the below four OAs may already be present and we retain
        // those values if the user is admin.
        boolean isAdmin = addContext.getSession().getAuthenticatedPrincipal().getName().equals(
            ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );

        // The EntryUUID
        addOpAttr( isAdmin, entry, SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
        
        // The EntryCSN
        addOpAttr( isAdmin, entry, SchemaConstants.ENTRY_CSN_AT, service.getCSN().toString() );
        
        // The CreatorsName
        addOpAttr( isAdmin, entry, SchemaConstants.CREATORS_NAME_AT, principal );
        
        // The CreateTimestamp
        addOpAttr( isAdmin, entry, SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        
        nextInterceptor.add( addContext );
    }

    
    private boolean checkCanModify( boolean isAdmin, Modification modification, AttributeType attributeType ) throws LdapException
    {
        AttributeType modifiedAT = modification.getAttribute().getAttributeType();

        if ( attributeType.equals( modifiedAT ) )
        {
            if ( !isAdmin )
            {
                String message = I18n.err( I18n.ERR_31 );
                LOG.error( message );
                throw new LdapSchemaViolationException( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, message );
            }
            else
            {
                return true;
            }
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void modify( NextInterceptor nextInterceptor, ModifyOperationContext modifyContext ) throws LdapException
    {
        // We must check that the user hasn't injected either the modifiersName
        // or the modifyTimestamp operational attributes : they are not supposed to be
        // added at this point EXCEPT in cases of replication by a admin user.
        // If so, remove them, and if there are no more attributes, simply return.
        // otherwise, inject those values into the list of modifications
        List<Modification> mods = modifyContext.getModItems();

        boolean isAdmin = modifyContext.getSession().getAuthenticatedPrincipal().getDN().equals( adminDn );

        boolean modifierAtPresent = false;
        boolean modifiedTimeAtPresent = false;
        boolean entryCsnAtPresent = false;
        
        for ( Modification modification : mods )
        {
            modifierAtPresent = checkCanModify( isAdmin, modification, MODIFIERS_NAME_AT );
            modifiedTimeAtPresent = checkCanModify( isAdmin, modification, MODIFY_TIMESTAMP_AT );
            entryCsnAtPresent = checkCanModify( isAdmin, modification, ENTRY_CSN_AT );
            
            if ( AP_SEQUENCE_NUMBER_ATTRIBUTE_TYPES.contains( modification.getAttribute().getAttributeType() ) && !isAdmin )
            {
                String message = I18n.err( I18n.ERR_32 );
                LOG.error( message );
                throw new LdapSchemaViolationException( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, message );
            }

            if ( SUBENTRIES_UUID_ATTRIBUTE_TYPES.contains( modification.getAttribute().getAttributeType() ) && !isAdmin )
            {
                String message = I18n.err( I18n.ERR_32 );
                LOG.error( message );
                throw new LdapSchemaViolationException( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, message );
            }

            if ( PWD_POLICY_STATE_ATTRIBUTE_TYPES.contains( modification.getAttribute().getAttributeType() ) && !isAdmin )
            {
                String message = I18n.err( I18n.ERR_32 );
                LOG.error( message );
                throw new LdapSchemaViolationException( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, message );
            }
        }

        if ( !modifierAtPresent )
        {
            // Inject the ModifiersName AT if it's not present
            EntryAttribute attribute = new DefaultEntryAttribute( MODIFIERS_NAME_AT, getPrincipal()
                .getName() );

            Modification modifiersName = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );

            mods.add( modifiersName );
        }

        if ( !modifiedTimeAtPresent )
        {
            // Inject the ModifyTimestamp AT if it's not present
            EntryAttribute attribute = new DefaultEntryAttribute( MODIFY_TIMESTAMP_AT, DateUtils
                .getGeneralizedTime() );

            Modification timestamp = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );

            mods.add( timestamp );
        }

        if ( !entryCsnAtPresent )
        {
            String csn = service.getCSN().toString();
            EntryAttribute attribute = new DefaultEntryAttribute( ENTRY_CSN_AT, csn );
            Modification updatedCsn = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );
            mods.add( updatedCsn );
        }
        
        // Go down in the chain
        nextInterceptor.modify( modifyContext );
    }


    public void rename( NextInterceptor nextInterceptor, RenameOperationContext renameContext ) throws LdapException
    {
        Entry entry = ( ( ClonedServerEntry ) renameContext.getEntry() ).getClonedEntry();
        entry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal().getName() );
        entry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        Entry modifiedEntry = renameContext.getOriginalEntry().clone();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal().getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        //modifiedEntry.setDn( renameContext.getNewDn() );
        renameContext.setModifiedEntry( modifiedEntry );

        nextInterceptor.rename( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor nextInterceptor, MoveOperationContext moveContext ) throws LdapException
    {
        Entry modifiedEntry = moveContext.getOriginalEntry().clone();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal().getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        modifiedEntry.setDn( moveContext.getNewDn() );
        moveContext.setModifiedEntry( modifiedEntry );

        nextInterceptor.move( moveContext );
    }


    public void moveAndRename( NextInterceptor nextInterceptor, MoveAndRenameOperationContext moveAndRenameContext )
        throws LdapException
    {
        Entry modifiedEntry = moveAndRenameContext.getOriginalEntry().clone();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal().getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        modifiedEntry.setDn( moveAndRenameContext.getNewDn() );
        moveAndRenameContext.setModifiedEntry( modifiedEntry );

        nextInterceptor.moveAndRename( moveAndRenameContext );
    }


    public Entry lookup( NextInterceptor nextInterceptor, LookupOperationContext lookupContext ) throws LdapException
    {
        Entry result = nextInterceptor.lookup( lookupContext );

        if ( lookupContext.getAttrsId() == null )
        {
            filterOperationalAttributes( result );
        }
        else if ( !lookupContext.hasAllOperational() )
        {
            filter( lookupContext, result );
        }

        denormalizeEntryOpAttrs( result );
        return result;
    }


    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext listContext )
        throws LdapException
    {
        EntryFilteringCursor cursor = nextInterceptor.list( listContext );
        cursor.addEntryFilter( SEARCH_FILTER );
        return cursor;
    }


    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext searchContext )
        throws LdapException
    {
        EntryFilteringCursor cursor = nextInterceptor.search( searchContext );

        if ( searchContext.isAllOperationalAttributes()
            || ( searchContext.getReturningAttributes() != null && !searchContext.getReturningAttributes().isEmpty() ) )
        {
            if ( service.isDenormalizeOpAttrsEnabled() )
            {
                cursor.addEntryFilter( DENORMALIZING_SEARCH_FILTER );
            }

            return cursor;
        }

        cursor.addEntryFilter( SEARCH_FILTER );
        return cursor;
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
        for ( AttributeType attributeType : attributes.getAttributeTypes() )
        {
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


    private void filter( LookupOperationContext lookupContext, Entry entry ) throws LdapException
    {
        DN dn = lookupContext.getDn();
        List<String> ids = lookupContext.getAttrsId();

        // still need to protect against returning op attrs when ids is null
        if ( ids == null || ids.isEmpty() )
        {
            filterOperationalAttributes( entry );
            return;
        }

        Set<AttributeType> attributeTypes = entry.getAttributeTypes();

        if ( dn.size() == 0 )
        {
            for ( AttributeType attributeType : attributeTypes )
            {
                if ( !ids.contains( attributeType.getOid() ) )
                {
                    entry.removeAttributes( attributeType );
                }
            }
        }

        denormalizeEntryOpAttrs( entry );

        // do nothing past here since this explicity specifies which
        // attributes to include - backends will automatically populate
        // with right set of attributes using ids array
    }


    public void denormalizeEntryOpAttrs( Entry entry ) throws LdapException
    {
        if ( service.isDenormalizeOpAttrsEnabled() )
        {
            EntryAttribute attr = entry.get( SchemaConstants.CREATORS_NAME_AT );

            if ( attr != null )
            {
                DN creatorsName = service.getDNFactory().create( attr.getString() );

                attr.clear();
                attr.add( denormalizeTypes( creatorsName ).getName() );
            }

            attr = entry.get( SchemaConstants.MODIFIERS_NAME_AT );

            if ( attr != null )
            {
                DN modifiersName = service.getDNFactory().create( attr.getString() );

                attr.clear();
                attr.add( denormalizeTypes( modifiersName ).getName() );
            }

            attr = entry.get( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT );

            if ( attr != null )
            {
                DN modifiersName = service.getDNFactory().create( attr.getString() );

                attr.clear();
                attr.add( denormalizeTypes( modifiersName ).getName() );
            }
        }
    }


    /**
     * Does not create a new DN but alters existing DN by using the first
     * short name for an attributeType definition.
     * 
     * @param dn the normalized distinguished name
     * @return the distinuished name denormalized
     * @throws Exception if there are problems denormalizing
     */
    public DN denormalizeTypes( DN dn ) throws LdapException
    {
        DN newDn = new DN();

        for ( int ii = 0; ii < dn.size(); ii++ )
        {
            RDN rdn = dn.getRdn( ii );
            if ( rdn.size() == 0 )
            {
                newDn = newDn.add( new RDN() );
                continue;
            }
            else if ( rdn.size() == 1 )
            {
                String name = schemaManager.lookupAttributeTypeRegistry( rdn.getNormType() ).getName();
                String value = rdn.getAVA().getNormValue().getString();
                newDn = newDn.add( new RDN( name, name, value, value ) );
                continue;
            }

            // below we only process multi-valued rdns
            StringBuffer buf = new StringBuffer();

            for ( Iterator<AVA> atavs = rdn.iterator(); atavs.hasNext(); /**/)
            {
                AVA atav = atavs.next();
                String type = schemaManager.lookupAttributeTypeRegistry( rdn.getNormType() ).getName();
                buf.append( type ).append( '=' ).append( atav.getNormValue() );

                if ( atavs.hasNext() )
                {
                    buf.append( '+' );
                }
            }

            newDn = newDn.add( new RDN( buf.toString() ) );
        }

        return newDn;
    }


    private boolean filterDenormalized( Entry entry ) throws Exception
    {
        denormalizeEntryOpAttrs( entry );
        return true;
    }
}