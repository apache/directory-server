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


import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;


/**
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ObjectClassSynchronizer extends AbstractRegistrySynchronizer
{
    private final ObjectClassRegistry objectClassRegistry;


    public ObjectClassSynchronizer( Registries registries ) throws Exception
    {
        super( registries );
        this.objectClassRegistry = registries.getObjectClassRegistry();
    }


    protected boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        ObjectClass oc = factory.getObjectClass( targetEntry, registries, getSchemaName( name ) );

        if ( isSchemaLoaded( name ) )
        {
            objectClassRegistry.unregister( oid );
            objectClassRegistry.register( oc );
            
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }


    public void add( LdapDN name, ServerEntry entry ) throws Exception
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        checkOidIsUnique( entry );
        
        String schemaName = getSchemaName( name );
        ObjectClass oc = factory.getObjectClass( entry, registries, schemaName );

        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            objectClassRegistry.register( oc );
        }
        else
        {
            registerOids( oc );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        ObjectClass oc = factory.getObjectClass( entry, registries, schemaName );

        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            objectClassRegistry.unregister( oc.getOid() );
        }
        
        unregisterOids( oc.getOid() );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        ObjectClass oldOc = factory.getObjectClass( entry, registries, schemaName );

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
        checkOidIsUnique( newOid );
        ObjectClass oc = factory.getObjectClass( targetEntry, registries, schemaName );

        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            objectClassRegistry.unregister( oldOc.getOid() );
            objectClassRegistry.register( oc );
        }
        else
        {
            registerOids( oc );
        }
        
        unregisterOids( oldOc.getOid() );
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn, ServerEntry entry, 
        boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        ObjectClass oldOc = factory.getObjectClass( entry, registries, oldSchemaName );

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
        ObjectClass oc = factory.getObjectClass( targetEntry, registries, newSchemaName );

        Schema oldSchema = registries.getLoadedSchema( oldSchemaName );
        
        if ( ( oldSchema != null ) && oldSchema.isEnabled() )
        {
            objectClassRegistry.unregister( oldOc.getOid() );
        }
        
        unregisterOids( oldOc.getOid() );
        
        Schema newSchema = registries.getLoadedSchema( newSchemaName );
        
        if ( ( newSchema != null ) && newSchema.isEnabled() )
        {
            objectClassRegistry.register( oc );
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
        ObjectClass oldAt = factory.getObjectClass( entry, registries, oldSchemaName );

        // dependencies are not managed by this class
//        Set<ServerEntry> dependees = dao.listObjectClassDependents( oldAt );
//        if ( dependees != null && dependees.size() > 0 )
//        {
//            throw new LdapOperationNotSupportedException( "The objectClass with OID " + oldAt.getOid() 
//                + " cannot be deleted until all entities" 
//                + " using this objectClass have also been deleted.  The following dependees exist: " 
//                + getOids( dependees ), 
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }

        ObjectClass oc = factory.getObjectClass( entry, registries, newSchemaName );
        
        if ( isSchemaLoaded( oriChildName ) )
        {
            objectClassRegistry.unregister( oldAt.getOid() );
        }
        
        if ( isSchemaLoaded( newParentName ) )
        {
            objectClassRegistry.register( oc );
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
        
        if ( ! registries.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
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
