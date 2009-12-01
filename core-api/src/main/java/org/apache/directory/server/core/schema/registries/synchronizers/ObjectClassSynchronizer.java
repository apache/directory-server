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

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ObjectClassSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ObjectClassSynchronizer.class );


    /**
     * Creates a new instance of ObjectClassSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    public ObjectClassSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        LdapDN name = opContext.getDn();
        ServerEntry entry = opContext.getEntry();
        String oid = getOid( entry );
        ObjectClass oc = factory.getObjectClass( schemaManager, targetEntry, schemaManager.getRegistries(), getSchemaName( name ) );
        String schemaName = getSchemaName( entry.getDn() );

        if ( isSchemaEnabled( schemaName ) )
        {
            schemaManager.unregisterObjectClass( oid );
            schemaManager.add( oc );
            
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }


    /**
     * {@inheritDoc}
     */
    public void add( ServerEntry entry ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );
        
        // The parent DN must be ou=objectclasses,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.OBJECT_CLASS );
        
        // The new schemaObject's OID must not already exist
        checkOidIsUnique( entry );
        
        // Build the new ObjectClass from the given entry
        String schemaName = getSchemaName( dn );
        
        ObjectClass objectClass = factory.getObjectClass( schemaManager, entry, schemaManager.getRegistries(), schemaName );
        List<Throwable> errors = new ArrayList<Throwable>();

        // At this point, the constructed ObjectClass has not been checked against the 
        // existing Registries. It may be broken (missing SUP, or such), it will be checked
        // there, if the schema and the ObjectClass are both enabled.
        Schema schema = schemaManager.getLoadedSchema( schemaName );
        
        if ( schema.isEnabled() && objectClass.isEnabled() )
        {
            // As we may break the registries, work on a cloned registries
            Registries clonedRegistries = schemaManager.getRegistries().clone();

            add( errors, clonedRegistries, objectClass );
            
            // Remove the cloned registries
            clonedRegistries.clear();
            
            // If we didn't get any error, swap the registries
            if ( errors.isEmpty() )
            {
                // Apply the addition to the real registries
                add( errors, schemaManager.getRegistries(), objectClass );

                LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
            }
            else
            {
                // We have some error : reject the addition and get out
                String msg = "Cannot add the ObjectClass " + entry.getDn().getUpName() + " into the registries, "+
                    "the resulting registries would be inconsistent :" + StringTools.listToString( errors );
                LOG.info( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }
        else
        {
            // At least, we register the OID in the globalOidRegistry, and associates it with the
            // schema
            schemaManager.getRegistries().associateWithSchema( objectClass );

            LOG.debug( "Added {} into the disabled schema {}", dn.getUpName(), schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );
        
        // The parent DN must be ou=objectclasses,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.OBJECT_CLASS );
        
        // Get the ObjectClass from the given entry ( it has been grabbed from the server earlier)
        String schemaName = getSchemaName( entry.getDn() );
        ObjectClass objectClass = factory.getObjectClass( schemaManager, entry, schemaManager.getRegistries(), schemaName );

        // Applies the Registries to this ObjectClass 
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isEnabled() && objectClass.isEnabled() )
        {
            objectClass.applyRegistries( null, schemaManager.getRegistries() );
        }
        
        String oid = objectClass.getOid();

        if ( isSchemaEnabled( schemaName ) )
        {
            if ( schemaManager.getRegistries().isReferenced( objectClass ) )
            {
                String msg = "Cannot delete " + entry.getDn().getUpName() + ", as there are some " +
                    " dependant SchemaObjects :\n" + getReferenced( objectClass );
                LOG.warn( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }

        // Remove the ObjectClass from the schema content
        deleteFromSchema( objectClass, schemaName );

        if ( schemaManager.getObjectClassRegistry().contains( oid ) )
        {
            // Update the referenced and referencing objects
            // The MAY AttributeTypes
            for ( AttributeType may : objectClass.getMayAttributeTypes() )
            {
                schemaManager.getRegistries().delReference( objectClass, may );
            }
            
            // The MUST AttributeTypes
            for ( AttributeType must : objectClass.getMayAttributeTypes() )
            {
                schemaManager.getRegistries().delReference( objectClass, must );
            }
            
            // The superiors
            for ( ObjectClass superior : objectClass.getSuperiors() )
            {
                schemaManager.getRegistries().delReference( objectClass, superior );
            }
            
            // Update the Registry
            schemaManager.unregisterObjectClass( objectClass.getOid() );
            
            LOG.debug( "Removed {} from the enabled schema {}", objectClass, schemaName );
        }
        else
        {
            unregisterOids( objectClass );
            LOG.debug( "Removed {} from the disabled schema {}", objectClass, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        ObjectClass oldOc = factory.getObjectClass( schemaManager, entry, schemaManager.getRegistries(), schemaName );

        // Dependency constraints are not managed by this class
//        Set<ServerEntry> dependees = dao.listObjectClassDependents( oldOc );
//        
//        if ( dependees != null && dependees.size() > 0 )
//        {
//            throw new LdapOperationNotSupportedException( "The objectClass with OID " + oldOc.getOid()
//                + " cannot be deleted until all entities" 
//                + " using this objectClass have also been deleted.  The following dependees exist: " 
//                + getOids( dependees ), 
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        
        // Inject the new DN
        LdapDN newDn = new LdapDN( targetEntry.getDn() );
        newDn.remove( newDn.size() - 1 );
        newDn.add( newRdn );
        
        checkOidIsUnique( newOid );
        ObjectClass oc = factory.getObjectClass( schemaManager, targetEntry, schemaManager.getRegistries(), schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            // Check that the entry has no descendant
            if ( schemaManager.getObjectClassRegistry().hasDescendants( oldOc.getOid() ) )
            {
                String msg = "Cannot rename " + entry.getDn().getUpName() + " to " + newDn + 
                    " as the later has descendants' ObjectClasses";
                
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            schemaManager.unregisterObjectClass( oldOc.getOid() );
            schemaManager.add( oc );
        }
        else
        {
            unregisterOids( oldOc );
            registerOids( oc );
        }
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn, ServerEntry entry, 
        boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        ObjectClass oldOc = factory.getObjectClass( schemaManager, entry, schemaManager.getRegistries(), oldSchemaName );

        // this class does not handle dependencies
//        Set<ServerEntry> dependees = dao.listObjectClassDependents( oldOc );
//        if ( dependees != null && dependees.size() > 0 )
//        {
//            throw new LdapOperationNotSupportedException( "The objectClass with OID " + oldOc.getOid()
//                + " cannot be deleted until all entities" 
//                + " using this objectClass have also been deleted.  The following dependees exist: " 
//                + getOids( dependees ), 
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }

        String newSchemaName = getSchemaName( newParentName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        ObjectClass oc = factory.getObjectClass( schemaManager, targetEntry, schemaManager.getRegistries(), newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterObjectClass( oldOc.getOid() );
        }
        else
        {
            unregisterOids( oldOc );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( oc );
        }
        else
        {
            registerOids( oc );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        ObjectClass oldAt = factory.getObjectClass( schemaManager, entry, schemaManager.getRegistries(), oldSchemaName );

        // dependencies are not managed by this class
//        Set<ServerEntry> dependees = dao.listObjectClassDependents( oldAt );
//        if ( dependees != null && dependees.size() > 0 )
//        {s
//            throw new LdapOperationNotSupportedException( "The objectClass with OID " + oldAt.getOid() 
//                + " cannot be deleted until all entities" 
//                + " using this objectClass have also been deleted.  The following dependees exist: " 
//                + getOids( dependees ), 
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }

        ObjectClass oc = factory.getObjectClass( schemaManager, entry, schemaManager.getRegistries(), newSchemaName );
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterObjectClass( oldAt.getOid() );
        }
        else
        {
            unregisterOids( oldAt );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( oc );
        }
        else
        {
            registerOids( oc );
        }
    }


    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a objectClass should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        
        if ( ! schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a objectClass should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.OBJECT_CLASSES_AT ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a attributeType should have a relative name of ou=objectClasses.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
