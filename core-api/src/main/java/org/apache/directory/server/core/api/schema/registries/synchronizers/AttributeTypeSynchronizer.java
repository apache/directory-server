/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.api.schema.registries.synchronizers;


import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.registries.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A handler for operations performed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AttributeTypeSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AttributeTypeSynchronizer.class );


    /**
     * Creates a new instance of AttributeTypeSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    public AttributeTypeSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public void add( Entry entry ) throws LdapException
    {
        Dn dn = entry.getDn();
        Dn parentDn = dn.getParent();

        // The parent Dn must be ou=attributetypes,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.ATTRIBUTE_TYPE );

        // The new schemaObject's OID must not already exist
        checkOidIsUnique( entry );

        // Build the new AttributeType from the given entry
        String schemaName = getSchemaName( dn );

        AttributeType attributeType = factory.getAttributeType( schemaManager, entry, schemaManager.getRegistries(),
            schemaName );

        // At this point, the constructed AttributeType has not been checked against the 
        // existing Registries. It may be broken (missing SUP, or such), it will be checked
        // there, if the schema and the AttributeType are both enabled.
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isEnabled() && attributeType.isEnabled() )
        {
            if ( schemaManager.add( attributeType ) )
            {
                LOG.debug( "Added {} into the enabled schema {}", dn.getName(), schemaName );
            }
            else
            {
                // We have some error : reject the addition and get out
                String msg = I18n.err( I18n.ERR_345, entry.getDn().getName(),
                    Strings.listToString( schemaManager.getErrors() ) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            LOG.debug( "The AttributeType {} cannot be added in the disabled schema {}.", attributeType, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean modify( ModifyOperationContext modifyContext, Entry targetEntry, boolean cascade )
        throws LdapException
    {
        Dn name = modifyContext.getDn();
        Entry entry = modifyContext.getEntry();
        String schemaName = getSchemaName( name );
        String oid = getOid( entry );
        AttributeType at = factory.getAttributeType( schemaManager, targetEntry, schemaManager.getRegistries(),
            schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            if ( schemaManager.getAttributeTypeRegistry().contains( oid ) )
            {
                schemaManager.unregisterAttributeType( oid );
            }

            schemaManager.add( at );

            return SCHEMA_MODIFIED;
        }

        return SCHEMA_UNCHANGED;
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Entry entry, boolean cascade ) throws LdapException
    {
        Dn dn = entry.getDn();
        Dn parentDn = dn.getParent();

        // The parent Dn must be ou=attributetypes,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.ATTRIBUTE_TYPE );

        // Get the SchemaName
        String schemaName = getSchemaName( entry.getDn() );

        // Get the schema 
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isDisabled() )
        {
            // The schema is disabled, nothing to do.
            LOG.debug( "The AttributeType {} cannot be removed from the disabled schema {}.",
                dn.getName(), schemaName );

            return;
        }

        // Test that the Oid exists
        AttributeType attributeType = ( AttributeType ) checkOidExists( entry );

        if ( schema.isEnabled() && attributeType.isEnabled() )
        {
            if ( schemaManager.delete( attributeType ) )
            {
                LOG.debug( "Removed {} from the schema {}", attributeType, schemaName );
            }
            else
            {
                // We have some error : reject the deletion and get out
                String msg = I18n.err( I18n.ERR_346, entry.getDn().getName(),
                    Strings.listToString( schemaManager.getErrors() ) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            LOG.debug( "Removed {} from the disabled schema {}", attributeType, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Entry entry, Rdn newRdn, boolean cascade ) throws LdapException
    {
        String schemaName = getSchemaName( entry.getDn() );
        AttributeType oldAt = factory
            .getAttributeType( schemaManager, entry, schemaManager.getRegistries(), schemaName );

        // Inject the new OID
        Entry targetEntry = ( Entry ) entry.clone();
        String newOid = newRdn.getNormValue().getString();
        checkOidIsUnique( newOid );
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );

        // Inject the new Dn
        Dn newDn = targetEntry.getDn().getParent();
        newDn = newDn.add( newRdn );
        targetEntry.setDn( newDn );

        AttributeType at = factory.getAttributeType( schemaManager, targetEntry, schemaManager.getRegistries(),
            schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            // Check that the entry has no descendant
            if ( schemaManager.getAttributeTypeRegistry().hasDescendants( oldAt.getOid() ) )
            {
                String msg = I18n.err( I18n.ERR_347, entry.getDn().getName(), newDn );

                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }

            schemaManager.unregisterAttributeType( oldAt.getOid() );
            schemaManager.add( at );
        }
        else
        {
            unregisterOids( oldAt );
            registerOids( at );
        }
    }


    public void moveAndRename( Dn oriChildName, Dn newParentName, Rdn newRn, boolean deleteOldRn,
        Entry entry, boolean cascade ) throws LdapException
    {
        checkParent( newParentName, schemaManager, SchemaConstants.ATTRIBUTE_TYPE );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        AttributeType oldAt = factory.getAttributeType( schemaManager, entry, schemaManager.getRegistries(),
            oldSchemaName );
        Entry targetEntry = ( Entry ) entry.clone();
        String newOid = newRn.getNormValue().getString();
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        checkOidIsUnique( newOid );
        AttributeType newAt = factory.getAttributeType( schemaManager, targetEntry, schemaManager.getRegistries(),
            newSchemaName );

        if ( !isSchemaLoaded( oldSchemaName ) )
        {
            String msg = I18n.err( I18n.ERR_348, oldSchemaName );
            LOG.warn( msg );
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
        }

        if ( !isSchemaLoaded( newSchemaName ) )
        {
            String msg = I18n.err( I18n.ERR_349, newSchemaName );
            LOG.warn( msg );
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
        }

        deleteFromSchema( oldAt, oldSchemaName );
        addToSchema( newAt, newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterAttributeType( oldAt.getOid() );
        }
        else
        {
            unregisterOids( oldAt );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( newAt );
        }
        else
        {
            registerOids( newAt );
        }
    }


    public void move( Dn oriChildName, Dn newParentName, Entry entry, boolean cascade ) throws LdapException
    {
        checkParent( newParentName, schemaManager, SchemaConstants.ATTRIBUTE_TYPE );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        AttributeType oldAt = factory.getAttributeType( schemaManager, entry, schemaManager.getRegistries(),
            oldSchemaName );
        AttributeType newAt = factory.getAttributeType( schemaManager, entry, schemaManager.getRegistries(),
            newSchemaName );

        if ( !isSchemaLoaded( oldSchemaName ) )
        {
            String msg = "Cannot move a schemaObject from a not loaded schema " + oldSchemaName;
            LOG.warn( msg );
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
        }

        if ( !isSchemaLoaded( newSchemaName ) )
        {
            String msg = I18n.err( I18n.ERR_349, newSchemaName );
            LOG.warn( msg );
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
        }

        deleteFromSchema( oldAt, oldSchemaName );
        addToSchema( newAt, newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterAttributeType( oldAt.getOid() );
        }
        else
        {
            unregisterOids( oldAt );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( newAt );
        }
        else
        {
            registerOids( newAt );
        }
    }
}
