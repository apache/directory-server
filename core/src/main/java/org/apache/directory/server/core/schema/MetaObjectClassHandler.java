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
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * A handler for operations peformed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaObjectClassHandler extends AbstractSchemaChangeHandler
{
    private final SchemaPartitionDao dao;
    private final ObjectClassRegistry objectClassRegistry;


    public MetaObjectClassHandler( Registries targetRegistries, PartitionSchemaLoader loader, SchemaPartitionDao dao ) 
        throws NamingException
    {
        super( targetRegistries, loader );
        
        this.dao = dao;
        this.objectClassRegistry = targetRegistries.getObjectClassRegistry();
    }


    protected void modify( LdapDN name, Attributes entry, Attributes targetEntry ) throws NamingException
    {
        String oldOid = getOid( entry );
        ObjectClass oc = factory.getObjectClass( targetEntry, targetRegistries );
        Schema schema = getSchema( name );

        if ( ! schema.isDisabled() )
        {
            objectClassRegistry.unregister( oldOid );
            objectClassRegistry.register( schema.getSchemaName(), oc );
        }
    }


    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        
        ObjectClass oc = factory.getObjectClass( entry, targetRegistries );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            objectClassRegistry.register( schema.getSchemaName(), oc );
        }
        else
        {
            registerOids( oc );
        }
    }


    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        ObjectClass oc = factory.getObjectClass( entry, targetRegistries );
        Set<SearchResult> dependees = dao.listObjectClassDependees( oc );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The objectClass with OID " + oc.getOid() 
                + " cannot be deleted until all entities" 
                + " using this objectClass have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            objectClassRegistry.unregister( oc.getOid() );
        }
        
        unregisterOids( oc.getOid() );
    }


    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException
    {
        ObjectClass oldOc = factory.getObjectClass( entry, targetRegistries );
        Set<SearchResult> dependees = dao.listObjectClassDependees( oldOc );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The objectClass with OID " + oldOc.getOid()
                + " cannot be deleted until all entities" 
                + " using this objectClass have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( name );
        Attributes targetEntry = ( Attributes ) entry.clone();
        String newOid = NamespaceTools.getRdnValue( newRdn );
        targetEntry.put( new AttributeImpl( MetaSchemaConstants.M_OID_AT, newOid ) );
        ObjectClass oc = factory.getObjectClass( targetEntry, targetRegistries );

        if ( ! schema.isDisabled() )
        {
            objectClassRegistry.unregister( oldOc.getOid() );
            objectClassRegistry.register( schema.getSchemaName(), oc );
        }
        else
        {
            registerOids( oc );
        }
        
        unregisterOids( oldOc.getOid() );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        ObjectClass oldOc = factory.getObjectClass( entry, targetRegistries );
        Set<SearchResult> dependees = dao.listObjectClassDependees( oldOc );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The objectClass with OID " + oldOc.getOid()
                + " cannot be deleted until all entities" 
                + " using this objectClass have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        Attributes targetEntry = ( Attributes ) entry.clone();
        String newOid = NamespaceTools.getRdnValue( newRn );
        targetEntry.put( new AttributeImpl( MetaSchemaConstants.M_OID_AT, newOid ) );
        ObjectClass oc = factory.getObjectClass( targetEntry, targetRegistries );

        if ( ! oldSchema.isDisabled() )
        {
            objectClassRegistry.unregister( oldOc.getOid() );
        }
        unregisterOids( oldOc.getOid() );
        
        if ( ! newSchema.isDisabled() )
        {
            objectClassRegistry.register( newSchema.getSchemaName(), oc );
        }
        else
        {
            registerOids( oc );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        ObjectClass oldAt = factory.getObjectClass( entry, targetRegistries );
        Set<SearchResult> dependees = dao.listObjectClassDependees( oldAt );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The objectClass with OID " + oldAt.getOid() 
                + " cannot be deleted until all entities" 
                + " using this objectClass have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        
        ObjectClass oc = factory.getObjectClass( entry, targetRegistries );
        
        if ( ! oldSchema.isDisabled() )
        {
            objectClassRegistry.unregister( oldAt.getOid() );
        }
        
        if ( ! newSchema.isDisabled() )
        {
            objectClassRegistry.register( newSchema.getSchemaName(), oc );
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
        if ( ! targetRegistries.getOidRegistry().getOid( rdn.getType() ).equals( OU_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a objectClass should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( "objectClasses" ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a attributeType should have a relative name of ou=objectClasses.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
