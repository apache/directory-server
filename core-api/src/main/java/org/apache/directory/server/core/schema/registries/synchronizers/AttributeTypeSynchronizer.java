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
package org.apache.directory.server.core.schema.registries.synchronizers;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A handler for operations performed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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
    public void add( ServerEntry entry ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );

        // The parent DN must be ou=attributetypes,cn=<schemaName>,ou=schema
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
        List<Throwable> errors = new ArrayList<Throwable>();

        if ( schema.isEnabled() && attributeType.isEnabled() )
        {
            // As we may break the registries, work on a cloned registries
            Registries clonedRegistries = schemaManager.getRegistries().clone();

            // Inject the newly created AttributeType in the cloned registries
            clonedRegistries.add( errors, attributeType );

            // Remove the cloned registries
            clonedRegistries.clear();

            // If we didn't get any error, apply the addition to the real retistries
            if ( errors.isEmpty() )
            {
                // Apply the addition to the real registries
                schemaManager.getRegistries().add( errors, attributeType );

                LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
            }
            else
            {
                // We have some error : reject the addition and get out
                String msg = "Cannot add the AttributeType " + entry.getDn().getUpName() + " into the registries, "
                    + "the resulting registries would be inconsistent :" + StringTools.listToString( errors );
                LOG.info( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }
        else
        {
            // At least, we register the OID in the globalOidRegistry, and associates it with the
            // schema
            schemaManager.getRegistries().associateWithSchema( errors, attributeType );

            if ( !errors.isEmpty() )
            {
                String msg = "Cannot add the AttributeType " + entry.getDn().getUpName() + " into the registries, "
                    + "we have got some errors :" + StringTools.listToString( errors );

                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }

            LOG.debug( "Added {} into the disabled schema {}", dn.getUpName(), schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, boolean cascade )
        throws Exception
    {
        LdapDN name = opContext.getDn();
        ServerEntry entry = opContext.getEntry();
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
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );

        // The parent DN must be ou=attributetypes,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.ATTRIBUTE_TYPE );

        // Test that the Oid exists
        AttributeType attributeType = ( AttributeType ) checkOidExists( entry );

        // Get the SchemaName
        String schemaName = getSchemaName( entry.getDn() );

        // Get the schema 
        Schema schema = schemaManager.getLoadedSchema( schemaName );
        List<Throwable> errors = new ArrayList<Throwable>();

        if ( schema.isEnabled() && attributeType.isEnabled() )
        {
            // As we may break the registries, work on a cloned registries
            Registries clonedRegistries = schemaManager.getRegistries().clone();

            // Relax the cloned registries
            clonedRegistries.setRelaxed();

            // Remove this AttributeType from the Registries
            clonedRegistries.delete( errors, attributeType );

            // Remove the AttributeType from the schema/SchemaObject Map
            clonedRegistries.dissociateFromSchema( attributeType );

            // Update the cross references for AT
            clonedRegistries.delCrossReferences( attributeType );

            // Check the registries now
            errors = clonedRegistries.checkRefInteg();

            // If we didn't get any error, swap the registries
            if ( errors.isEmpty() )
            {
                clonedRegistries.setStrict();
                schemaManager.swapRegistries( clonedRegistries );
            }
            else
            {
                // We have some error : reject the deletion and get out
                // Destroy the cloned registries
                clonedRegistries.clear();

                // The schema is disabled. We still have to update the backend
                String msg = "Cannot delete the AttributeType " + entry.getDn().getUpName() + " into the registries, "
                    + "the resulting registries would be inconsistent :" + StringTools.listToString( errors );
                LOG.info( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }
        else
        {
            unregisterOids( attributeType );

            // Remove the AttributeType from the schema/SchemaObject Map
            schemaManager.getRegistries().dissociateFromSchema( attributeType );

            LOG.debug( "Removed {} from the disabled schema {}", attributeType, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        AttributeType oldAt = factory
            .getAttributeType( schemaManager, entry, schemaManager.getRegistries(), schemaName );

        // Inject the new OID
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );

        // Inject the new DN
        LdapDN newDn = new LdapDN( targetEntry.getDn() );
        newDn.remove( newDn.size() - 1 );
        newDn.add( newRdn );
        targetEntry.setDn( newDn );

        AttributeType at = factory.getAttributeType( schemaManager, targetEntry, schemaManager.getRegistries(),
            schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            // Check that the entry has no descendant
            if ( schemaManager.getAttributeTypeRegistry().hasDescendants( oldAt.getOid() ) )
            {
                String msg = "Cannot rename " + entry.getDn().getUpName() + " to " + newDn
                    + " as the later has descendants' AttributeTypes";

                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
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


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkParent( newParentName, schemaManager, SchemaConstants.ATTRIBUTE_TYPE );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        AttributeType oldAt = factory.getAttributeType( schemaManager, entry, schemaManager.getRegistries(),
            oldSchemaName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRn.getValue();
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        checkOidIsUnique( newOid );
        AttributeType newAt = factory.getAttributeType( schemaManager, targetEntry, schemaManager.getRegistries(),
            newSchemaName );

        if ( !isSchemaLoaded( oldSchemaName ) )
        {
            String msg = "Cannot move a schemaObject from a not loaded schema " + oldSchemaName;
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        if ( !isSchemaLoaded( newSchemaName ) )
        {
            String msg = "Cannot move a schemaObject to a not loaded schema " + newSchemaName;
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
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


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) throws Exception
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
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        if ( !isSchemaLoaded( newSchemaName ) )
        {
            String msg = "Cannot move a schemaObject to a not loaded schema " + newSchemaName;
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
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
