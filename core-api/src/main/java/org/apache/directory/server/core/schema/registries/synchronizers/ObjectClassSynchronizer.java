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
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;


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


    public boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        ObjectClass oc = factory.getObjectClass( targetEntry, registries, getSchemaName( name ) );
        String schemaName = getSchemaName( entry.getDn() );

        if ( isSchemaEnabled( schemaName ) )
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

        if ( isSchemaEnabled( schemaName ) )
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

        if ( isSchemaEnabled( schemaName ) )
        {
            // Check that the entry has no descendant
            if ( objectClassRegistry.hasDescendants( oc.getOid() ) )
            {
                String msg = "Cannot delete " + entry.getDn().getUpName() + ", as there are some " +
                    " dependant ObjectClasses";
                
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            objectClassRegistry.unregister( oc.getOid() );
        }
        else
        {
            unregisterOids( oc );
        }
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
        
        // Inject the new DN
        LdapDN newDn = new LdapDN( targetEntry.getDn() );
        newDn.remove( newDn.size() - 1 );
        newDn.add( newRdn );
        
        checkOidIsUnique( newOid );
        ObjectClass oc = factory.getObjectClass( targetEntry, registries, schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            // Check that the entry has no descendant
            if ( objectClassRegistry.hasDescendants( oldOc.getOid() ) )
            {
                String msg = "Cannot rename " + entry.getDn().getUpName() + " to " + newDn + 
                    " as the later has descendants' ObjectClasses";
                
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            objectClassRegistry.unregister( oldOc.getOid() );
            objectClassRegistry.register( oc );
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

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            objectClassRegistry.unregister( oldOc.getOid() );
        }
        else
        {
            unregisterOids( oldOc );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
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
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            objectClassRegistry.unregister( oldAt.getOid() );
        }
        else
        {
            unregisterOids( oldAt );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            objectClassRegistry.register( oc );
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
