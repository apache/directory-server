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
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;


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
        
        if ( registries.isSchemaLoaded( schemaName ) )
        {
            atRegistry.unregister( oid );
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
        
        if ( registries.isSchemaLoaded( schemaName ) )
        {
            atRegistry.register( at );
        }
        else
        {
            registerOids( at );
        }
    }


    public void delete( LdapDN name, ServerEntry entry, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( name );
        AttributeType at = factory.getAttributeType( entry, registries, schemaName );
        
        if ( registries.isSchemaLoaded( schemaName ) )
        {
            atRegistry.unregister( at.getOid() );
        }
        unregisterOids( at.getOid() );
    }


    public void rename( LdapDN name, ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( name );
        AttributeType oldAt = factory.getAttributeType( entry, registries, schemaName );

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        AttributeType at = factory.getAttributeType( targetEntry, registries, schemaName );

        if ( registries.isSchemaLoaded( schemaName ) )
        {
            atRegistry.unregister( oldAt.getOid() );
            atRegistry.register( at );
        }
        else
        {
            registerOids( at );
        }
        
        unregisterOids( oldAt.getOid() );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn,
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

        if ( registries.isSchemaLoaded( oldSchemaName ) )
        {
            atRegistry.unregister( oldAt.getOid() );
        }
        unregisterOids( oldAt.getOid() );

        if ( registries.isSchemaLoaded( newSchemaName ) )
        {
            atRegistry.register( at );
        }
        else
        {
            registerOids( at );
        }
    }


    public void replace( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        AttributeType oldAt = factory.getAttributeType( entry, registries, oldSchemaName );
        AttributeType at = factory.getAttributeType( entry, registries, newSchemaName );
        
        if ( registries.isSchemaLoaded( oldSchemaName ) )
        {
            atRegistry.unregister( oldAt.getOid() );
        }
        
        if ( registries.isSchemaLoaded( newSchemaName ) )
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
