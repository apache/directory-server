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


import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.Subordinates;
import org.apache.directory.server.core.shared.SchemaService;
import org.apache.directory.server.i18n.I18n;
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
    private static final Logger LOG = LoggerFactory.getLogger( OperationalAttributeInterceptor.class );

    /** The denormalizer filter */
    private final EntryFilter denormalizingSearchFilter = new OperationalAttributeDenormalizingSearchFilter();
    
    /** The filter that add the mandatory operational attributes */
    private final EntryFilter operationalAttributeSearchFilter = new OperationalAttributeSearchFilter();
    
    /** The filter that add the subordinates operational attributes */
    private final EntryFilter subordinatesSearchFilter = new SubordinatesSearchFilter();

    /** The subschemasubentry Dn */
    private Dn subschemaSubentryDn;

    /** The admin Dn */
    private Dn adminDn;

    /**
     * the search result filter to use for collective attribute injection
     */
    private class OperationalAttributeDenormalizingSearchFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept( SearchOperationContext operation, Entry entry ) throws LdapException
        {
            if ( operation.getReturningAttributesString() == null )
            {
                return true;
            }

            // Denormalize the operational Attributes
            denormalizeEntryOpAttrs( entry );
            
            return true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( String tabs )
        {
            return tabs + "OperationalAttributeDenormalizingSearchFilter";
        }
    }

    
    /**
     * the search result filter to use for the addition of mandatory operational attributes
     */
    private class OperationalAttributeSearchFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept( SearchOperationContext operation, Entry entry ) throws LdapException
        {
            if ( operation.getReturningAttributesString() == null )
            {
                return true;
            }

            // Add the SubschemaSubentry AttributeType if it's requested
            SchemaManager schemaManager = operation.getSession().getDirectoryService().getSchemaManager();
            
            if ( operation.isAllOperationalAttributes()
                || operation.getReturningAttributes().contains( 
                    new AttributeTypeOptions( schemaManager.getAttributeType( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ) ) ) )
            {
                AttributeType subschemaSubentryAt = schemaManager.getAttributeType( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
                entry.add( new DefaultAttribute( subschemaSubentryAt, 
                    directoryService.getPartitionNexus().getRootDseValue( subschemaSubentryAt ) ) );
            }

            return true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( String tabs )
        {
            return tabs + "OperationalAttributeSearchFilter";
        }
    }

    
    /**
     * The search result filter to use for the addition of the subordinates attributes, if requested
     */
    private class SubordinatesSearchFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept( SearchOperationContext operation, Entry entry ) throws LdapException
        {
            // Add the nbChildren/nbSubordinates attributes if required
            processSubordinates( operation.getReturningAttributes(), operation.isAllOperationalAttributes(), entry );

            return true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( String tabs )
        {
            return tabs + "SubordinatesSearchFilter";
        }
    }

    
    /**
     * Creates the operational attribute management service interceptor.
     */
    public OperationalAttributeInterceptor()
    {
        super( InterceptorEnum.OPERATIONAL_ATTRIBUTE_INTERCEPTOR );
    }


    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        // stuff for dealing with subentries (garbage for now)
        Value subschemaSubentry = directoryService.getPartitionNexus().getRootDseValue(
            directoryService.getAtProvider().getSubschemaSubentry() );
        subschemaSubentryDn = dnFactory.create( subschemaSubentry.getValue() );

        // Create the Admin Dn
        adminDn = dnFactory.create( ServerDNConstants.ADMIN_SYSTEM_DN );
    }


    @Override
    public void destroy()
    {
    }


    /**
     * Check if we have to add an operational attribute, or if the admin has injected one
     */
    private boolean checkAddOperationalAttribute( boolean isAdmin, Entry entry, AttributeType attribute )
        throws LdapException
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
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        String principal = getPrincipal( addContext ).getName();

        Entry entry = addContext.getEntry();

        // If we are using replication, the below four OAs may already be present and we retain
        // those values if the user is admin.
        boolean isAdmin = addContext.getSession().getAuthenticatedPrincipal().getDn().equals( adminDn );

        // The EntryUUID attribute
        if ( !checkAddOperationalAttribute( isAdmin, entry, directoryService.getAtProvider().getEntryUUID() ) )
        {
            entry.put( directoryService.getAtProvider().getEntryUUID(), UUID.randomUUID().toString() );
        }

        // The EntryCSN attribute
        if ( !checkAddOperationalAttribute( isAdmin, entry, directoryService.getAtProvider().getEntryCSN() ) )
        {
            entry.put( directoryService.getAtProvider().getEntryCSN(), directoryService.getCSN().toString() );
        }

        // The CreatorsName attribute
        if ( !checkAddOperationalAttribute( isAdmin, entry, directoryService.getAtProvider().getCreatorsName() ) )
        {
            entry.put( directoryService.getAtProvider().getCreatorsName(), principal );
        }

        // The CreateTimeStamp attribute
        if ( !checkAddOperationalAttribute( isAdmin, entry, directoryService.getAtProvider().getCreateTimestamp() ) )
        {
            entry.put( directoryService.getAtProvider().getCreateTimestamp(), DateUtils.getGeneralizedTime() );
        }

        // Now, check that the user does not add operational attributes
        // The accessControlSubentries attribute
        checkAddOperationalAttribute( isAdmin, entry, directoryService.getAtProvider().getAccessControlSubentries() );

        // The CollectiveAttributeSubentries attribute
        checkAddOperationalAttribute( isAdmin, entry, directoryService.getAtProvider()
            .getCollectiveAttributeSubentries() );

        // The TriggerExecutionSubentries attribute
        checkAddOperationalAttribute( isAdmin, entry, directoryService.getAtProvider().getTriggerExecutionSubentries() );

        // The SubSchemaSybentry attribute
        checkAddOperationalAttribute( isAdmin, entry, directoryService.getAtProvider().getSubschemaSubentry() );

        next( addContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        Dn dn = lookupContext.getDn();

        if ( dn.getNormName().equals( subschemaSubentryDn.getNormName() ) )
        {
            Entry serverEntry = SchemaService.getSubschemaEntry( directoryService, lookupContext );
            serverEntry.setDn( dn );

            return serverEntry;
        }

        Entry entry = next( lookupContext );

        denormalizeEntryOpAttrs( entry );
        
        // Add the nbChildren/nbSubordinates attributes if required
        processSubordinates( lookupContext.getReturningAttributes(), lookupContext.isAllOperationalAttributes(), entry );

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
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

            if ( attributeType.equals( directoryService.getAtProvider().getModifiersName() ) )
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

            if ( attributeType.equals( directoryService.getAtProvider().getModifyTimestamp() ) )
            {
                if ( !isAdmin )
                {
                    String message = I18n.err( I18n.ERR_30, attributeType );
                    LOG.error( message );
                    throw new LdapNoPermissionException( message );
                }
                else
                {
                    modifiedTimeAtPresent = true;
                }
            }

            if ( attributeType.equals( directoryService.getAtProvider().getEntryCSN() ) )
            {
                if ( !isAdmin )
                {
                    String message = I18n.err( I18n.ERR_30, attributeType );
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
                String message = I18n.err( I18n.ERR_30, attributeType );
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
                Attribute attribute = new DefaultAttribute( directoryService.getAtProvider().getModifiersName(),
                    getPrincipal( modifyContext ).getName() );

                Modification modifiersName = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                    attribute );

                mods.add( modifiersName );
            }

            if ( !modifiedTimeAtPresent )
            {
                // Inject the ModifyTimestamp AT if it's not present
                Attribute attribute = new DefaultAttribute( directoryService.getAtProvider().getModifyTimestamp(),
                    DateUtils.getGeneralizedTime() );

                Modification timestamp = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );

                mods.add( timestamp );
            }

            if ( !entryCsnAtPresent )
            {
                String csn = directoryService.getCSN().toString();
                Attribute attribute = new DefaultAttribute( directoryService.getAtProvider().getEntryCSN(), csn );
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
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        Entry modifiedEntry = moveContext.getOriginalEntry().clone();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal( moveContext ).getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        Attribute csnAt = new DefaultAttribute( directoryService.getAtProvider().getEntryCSN(), directoryService
            .getCSN().toString() );
        modifiedEntry.put( csnAt );

        modifiedEntry.setDn( moveContext.getNewDn() );
        moveContext.setModifiedEntry( modifiedEntry );

        next( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Entry modifiedEntry = moveAndRenameContext.getModifiedEntry();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal( moveAndRenameContext ).getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        modifiedEntry.setDn( moveAndRenameContext.getNewDn() );

        Attribute csnAt = new DefaultAttribute( directoryService.getAtProvider().getEntryCSN(), directoryService
            .getCSN().toString() );
        modifiedEntry.put( csnAt );

        moveAndRenameContext.setModifiedEntry( modifiedEntry );

        next( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Entry entry = ( ( ClonedServerEntry ) renameContext.getEntry() ).getClonedEntry();
        entry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal( renameContext ).getName() );
        entry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        Entry modifiedEntry = renameContext.getOriginalEntry().clone();
        modifiedEntry.put( SchemaConstants.MODIFIERS_NAME_AT, getPrincipal( renameContext ).getName() );
        modifiedEntry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        Attribute csnAt = new DefaultAttribute( directoryService.getAtProvider().getEntryCSN(), directoryService
            .getCSN().toString() );
        modifiedEntry.put( csnAt );

        renameContext.setModifiedEntry( modifiedEntry );

        next( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        EntryFilteringCursor cursor = next( searchContext );

        if ( searchContext.isAllOperationalAttributes()
            || ( ( searchContext.getReturningAttributes() != null ) && !searchContext.getReturningAttributes().isEmpty() ) )
        {
            if ( directoryService.isDenormalizeOpAttrsEnabled() )
            {
                cursor.addEntryFilter( denormalizingSearchFilter );
            }

            cursor.addEntryFilter( operationalAttributeSearchFilter );
            cursor.addEntryFilter( subordinatesSearchFilter );
            
            return cursor;
        }

        return cursor;
    }


    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        // insert a new CSN into the entry, this is for replication
        Entry entry = deleteContext.getEntry();
        Attribute csnAt = new DefaultAttribute( directoryService.getAtProvider().getEntryCSN(), directoryService
            .getCSN().toString() );
        entry.put( csnAt );

        next( deleteContext );
    }


    private void denormalizeEntryOpAttrs( Entry entry ) throws LdapException
    {
        if ( directoryService.isDenormalizeOpAttrsEnabled() )
        {
            Attribute attr = entry.get( SchemaConstants.CREATORS_NAME_AT );

            if ( attr != null )
            {
                Dn creatorsName = dnFactory.create( attr.getString() );

                attr.clear();
                attr.add( denormalizeTypes( creatorsName ).getName() );
            }

            attr = entry.get( SchemaConstants.MODIFIERS_NAME_AT );

            if ( attr != null )
            {
                Dn modifiersName = dnFactory.create( attr.getString() );

                attr.clear();
                attr.add( denormalizeTypes( modifiersName ).getName() );
            }

            attr = entry.get( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT );

            if ( attr != null )
            {
                Dn modifiersName = dnFactory.create( attr.getString() );

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
                String value = rdn.getValue();
                newDn = newDn.add( new Rdn( name, value ) );
                continue;
            }

            // below we only process multi-valued rdns
            StringBuilder buf = new StringBuilder();

            for ( Iterator<Ava> atavs = rdn.iterator(); atavs.hasNext(); /**/)
            {
                Ava atav = atavs.next();
                String type = schemaManager.lookupAttributeTypeRegistry( rdn.getNormType() ).getName();
                buf.append( type ).append( '=' ).append( atav.getValue().getValue() );

                if ( atavs.hasNext() )
                {
                    buf.append( '+' );
                }
            }

            newDn = newDn.add( new Rdn( buf.toString() ) );
        }

        return newDn;
    }
    
    
    private void processSubordinates( Set<AttributeTypeOptions> returningAttributes, boolean allAttributes, Entry entry ) 
        throws LdapException
    {
        // Bypass the rootDSE : we won't get the nbChildren and nbSubordiantes for this special entry
        if ( Dn.isNullOrEmpty( entry.getDn() ) )
        {
            return;
        }

        // Add the Subordinates AttributeType if it's requested
        AttributeType nbChildrenAt = directoryService.getAtProvider().getNbChildren();
        AttributeTypeOptions nbChildrenAto = new AttributeTypeOptions( nbChildrenAt );
        AttributeType nbSubordinatesAt = directoryService.getAtProvider().getNbSubordinates();
        AttributeTypeOptions nbSubordinatesAto = new AttributeTypeOptions( nbSubordinatesAt );
        
        if ( returningAttributes != null )
        {
            boolean nbChildrenRequested = returningAttributes.contains( nbChildrenAto ) || allAttributes;
            boolean nbSubordinatesRequested = returningAttributes.contains( nbSubordinatesAto ) || allAttributes;

            if ( nbChildrenRequested || nbSubordinatesRequested )
            {
                Partition partition = directoryService.getPartitionNexus().getPartition( entry.getDn() );
                Subordinates subordinates = partition.getSubordinates( entry );
                
                long nbChildren = subordinates.getNbChildren();
                long nbSubordinates = subordinates.getNbSubordinates();
                
                if ( nbChildrenRequested )
                {
                    entry.add( new DefaultAttribute( nbChildrenAt, 
                        Long.toString( nbChildren ) ) );
                }
    
                if ( nbSubordinatesRequested )
                { 
                    entry.add( new DefaultAttribute( nbSubordinatesAt,
                        Long.toString( nbSubordinates ) ) );
                }
            }
        }
    }
}