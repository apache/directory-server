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


import java.util.Comparator;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * A handler for operations peformed to add, delete, modify, rename and 
 * move schema comparators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaComparatorHandler implements SchemaChangeHandler
{
    private final SchemaEntityFactory factory;
    private final RegistryModifier registryModifier;
    private final Registries targetRegistries;
    private final AttributeType m_oidAT;


    public MetaComparatorHandler( Registries targetRegistries ) throws NamingException
    {
        this.targetRegistries = targetRegistries;
        this.factory = new SchemaEntityFactory( targetRegistries );
        this.registryModifier = new RegistryModifier();
        this.m_oidAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
    }


    private String getOid( Attributes entry ) throws NamingException
    {
        Attribute oid = ServerUtils.getAttribute( m_oidAT, entry );
        if ( oid == null )
        {
            return null;
        }
        return ( String ) oid.get();
    }
    
    
    private void modify( LdapDN name, Attributes entry, Attributes targetEntry ) throws NamingException
    {
        String oid = getOid( entry );
        Comparator replacement = factory.getComparator( entry, targetRegistries );
        Schema targetSchema = targetRegistries.getSchema( MetaSchemaUtils.getSchemaName( name ) );
        registryModifier.modify( oid, replacement, targetSchema );
    }


    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, Attributes targetEntry )
        throws NamingException
    {
        modify( name, entry, targetEntry );
    }


    public void modify( LdapDN name, ModificationItem[] mods, Attributes entry, Attributes targetEntry )
        throws NamingException
    {
        modify( name, entry, targetEntry );
    }


    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        Comparator comparator = factory.getComparator( entry, targetRegistries );
        Schema schema = targetRegistries.getSchema( MetaSchemaUtils.getSchemaName( name ) );
        registryModifier.add( getOid( entry ), comparator, schema );
    }


    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        Comparator comparator = factory.getComparator( entry, targetRegistries );
        Schema schema = targetRegistries.getSchema( MetaSchemaUtils.getSchemaName( name ) );
        registryModifier.remove( getOid( entry ), comparator, schema );
    }


    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException
    {
        Schema schema = targetRegistries.getSchema( MetaSchemaUtils.getSchemaName( name ) );
        String oid = NamespaceTools.getRdnValue( newRdn );
        registryModifier.renameComparator( getOid( entry ), oid, schema );
    }
}
