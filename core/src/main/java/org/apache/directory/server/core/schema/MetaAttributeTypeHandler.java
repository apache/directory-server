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
package org.apache.directory.server.core.schema;


import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A handler for operations peformed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaAttributeTypeHandler extends AbstractSchemaChangeHandler
{
    private final SchemaPartitionDao dao;
    private final AttributeTypeRegistry atRegistry;

    

    public MetaAttributeTypeHandler( Registries targetRegistries, PartitionSchemaLoader loader, SchemaPartitionDao dao ) 
        throws Exception
    {
        super( targetRegistries, loader );
        
        this.dao = dao;
        this.atRegistry = targetRegistries.getAttributeTypeRegistry();
    }


    protected boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) 
        throws Exception
    {
        String oid = getOid( entry );
        Schema schema = getSchema( name );
        AttributeType at = factory.getAttributeType( targetEntry, targetRegistries, schema.getSchemaName() );
        
        if ( ! schema.isDisabled() )
        {
            atRegistry.unregister( oid );
            atRegistry.register( at );
            
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }
    
    
    public void add( AttributeType at ) throws Exception
    {
        Schema schema = dao.getSchema( at.getSchemaName() );
        if ( ! schema.isDisabled() )
        {
            atRegistry.register( at );
        }
        else
        {
            registerOids( at );
        }
    }


    public void add( LdapDN name, ServerEntry entry ) throws Exception
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        checkOidIsUnique( entry );
        
        Schema schema = getSchema( name );
        AttributeType at = factory.getAttributeType( entry, targetRegistries, schema.getSchemaName() );
        add( at );
    }


    public void delete( LdapDN name, ServerEntry entry, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( name );
        AttributeType at = factory.getAttributeType( entry, targetRegistries, schemaName );
        Set<ServerEntry> dependees = dao.listAttributeTypeDependents( at );
        
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The attributeType with OID " + at.getOid() 
                + " cannot be deleted until all entities" 
                + " using this attributeType have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        delete( at, cascade );
    }


    public void delete( AttributeType at, boolean cascade ) throws Exception
    {
        Schema schema = loader.getSchema( at.getSchemaName() );
        if ( ! schema.isDisabled() )
        {
            atRegistry.unregister( at.getOid() );
        }
        unregisterOids( at.getOid() );
    }


    public void rename( LdapDN name, ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        Schema schema = getSchema( name );
        AttributeType oldAt = factory.getAttributeType( entry, targetRegistries, schema.getSchemaName() );
        Set<ServerEntry> dependees = dao.listAttributeTypeDependents( oldAt );
        
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The attributeType with OID " + oldAt.getOid()
                + " cannot be deleted until all entities" 
                + " using this attributeType have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        AttributeType at = factory.getAttributeType( targetEntry, targetRegistries, schema.getSchemaName() );

        if ( ! schema.isDisabled() )
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
        Schema oldSchema = getSchema( oriChildName );
        AttributeType oldAt = factory.getAttributeType( entry, targetRegistries, oldSchema.getSchemaName() );
        Set<ServerEntry> dependees = dao.listAttributeTypeDependents( oldAt );
        
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The attributeType with OID " + oldAt.getOid()
                + " cannot be deleted until all entities" 
                + " using this attributeType have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema newSchema = getSchema( newParentName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRn.getValue();
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        checkOidIsUnique( newOid );
        AttributeType at = factory.getAttributeType( targetEntry, targetRegistries, newSchema.getSchemaName() );

        if ( ! oldSchema.isDisabled() )
        {
            atRegistry.unregister( oldAt.getOid() );
        }
        unregisterOids( oldAt.getOid() );

        if ( ! newSchema.isDisabled() )
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
        Schema oldSchema = getSchema( oriChildName );
        AttributeType oldAt = factory.getAttributeType( entry, targetRegistries, oldSchema.getSchemaName() );
        Set<ServerEntry> dependees = dao.listAttributeTypeDependents( oldAt );
        
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The attributeType with OID " + oldAt.getOid() 
                + " cannot be deleted until all entities" 
                + " using this attributeType have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema newSchema = getSchema( newParentName );
        AttributeType at = factory.getAttributeType( entry, targetRegistries, newSchema.getSchemaName() );
        
        if ( ! oldSchema.isDisabled() )
        {
            atRegistry.unregister( oldAt.getOid() );
        }
        
        if ( ! newSchema.isDisabled() )
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
        if ( ! targetRegistries.getAttributeTypeRegistry().getOid( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
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
