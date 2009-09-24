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
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;


/**
 * A handler for operations performed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AttributeTypeSynchronizer extends AbstractRegistrySynchronizer
{
    private final AttributeTypeRegistry atRegistry;

    
    public AttributeTypeSynchronizer( Registries registries ) throws Exception
    {
        super( registries );
        this.atRegistry = registries.getAttributeTypeRegistry();
    }


    protected boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) 
        throws Exception
    {
        String schemaName = getSchemaName( name );
        String oid = getOid( entry );
        AttributeType at = factory.getAttributeType( targetEntry, registries, schemaName );
        
        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            if ( atRegistry.contains( oid ) )
            {
                atRegistry.unregister( oid );
            }
            
            atRegistry.register( at );
            
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
        AttributeType at = factory.getAttributeType( entry, registries, schemaName );
        
        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            // Don't inject the modified element if the schema is disabled
            atRegistry.register( at );
        }
        else
        {
            registerOids( at );
        }
    }


    /**
     * Delete the attributeType, if it has no descendant.
     * 
     * @param entry the AttributeType entry to delete
     * @param cascade unused
     * @exception Exception if the deletion failed
     */
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        AttributeType at = factory.getAttributeType( entry, registries, schemaName );
        
        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            // Check that the entry has no descendant
            if ( atRegistry.hasDescendants( at.getOid() ) )
            {
                String msg = "Cannot delete " + entry.getDn().getUpName() + ", as there are some " +
                    " dependant AttributeTypes";
                
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            // Don't inject the modified element if the schema is disabled
            atRegistry.unregister( at.getOid() );
        }
        
        unregisterOids( at.getOid() );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        AttributeType oldAt = factory.getAttributeType( entry, registries, schemaName );

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
        
        AttributeType at = factory.getAttributeType( targetEntry, registries, schemaName );

        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            // Check that the entry has no descendant
            if ( atRegistry.hasDescendants( oldAt.getOid() ) )
            {
                String msg = "Cannot rename " + entry.getDn().getUpName() + " to " + newDn + 
                    " as the later has descendants' AttributeTypes";
                
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            atRegistry.unregister( oldAt.getOid() );
            atRegistry.register( at );
        }
        
        unregisterOids( oldAt.getOid() );
        registerOids( at );
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        AttributeType oldAt = factory.getAttributeType( entry, registries, oldSchemaName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRn.getValue();
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        checkOidIsUnique( newOid );
        AttributeType at = factory.getAttributeType( targetEntry, registries, newSchemaName );

        Schema oldSchema = registries.getLoadedSchema( oldSchemaName );
        
        if ( ( oldSchema != null ) && oldSchema.isEnabled() )
        {
            atRegistry.unregister( oldAt.getOid() );
        }

        unregisterOids( oldAt.getOid() );

        Schema newSchema = registries.getLoadedSchema( newSchemaName );
        
        if ( ( newSchema != null ) && newSchema.isEnabled() )
        {
            atRegistry.register( at );
        }
        else
        {
            registerOids( at );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        AttributeType oldAt = factory.getAttributeType( entry, registries, oldSchemaName );
        AttributeType at = factory.getAttributeType( entry, registries, newSchemaName );
        
        Schema oldSchema = registries.getLoadedSchema( oldSchemaName );
        
        if ( ( oldSchema != null ) && oldSchema.isEnabled() )
        {
            atRegistry.unregister( oldAt.getOid() );
        }
        
        Schema newSchema = registries.getLoadedSchema( newSchemaName );
        
        if ( ( newSchema != null ) && newSchema.isEnabled() )
        {
            atRegistry.register( at );
        }
    }
    
    
    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a attributeType should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        
        if ( ! registries.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a attributeType should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.ATTRIBUTE_TYPES_AT ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a attributeType should have a relative name of ou=attributeTypes.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
