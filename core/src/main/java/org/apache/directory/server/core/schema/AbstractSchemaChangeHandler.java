/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.schema;


import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.util.AttributeUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * An abstract schema change handler with some reused functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractSchemaChangeHandler implements SchemaChangeHandler
{
    protected final Registries targetRegistries;
    protected final PartitionSchemaLoader loader;
    protected final AttributeType m_oidAT;
    protected final SchemaEntityFactory factory;

    
    protected AbstractSchemaChangeHandler( Registries targetRegistries, PartitionSchemaLoader loader ) throws NamingException
    {
        this.targetRegistries = targetRegistries;
        this.loader = loader;
        this.m_oidAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
        this.factory = new SchemaEntityFactory( targetRegistries );
    }
    
    
    protected void checkOidIsUnique( Attributes entry ) throws NamingException
    {
        String oid = getOid( entry );
        
        if ( targetRegistries.getOidRegistry().hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }

    
    protected void checkOidIsUnique( SchemaObject schemaObject ) throws NamingException
    {
        String oid = schemaObject.getOid();
        
        if ( targetRegistries.getOidRegistry().hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }
    
    
    protected void checkOidIsUnique( String oid ) throws NamingException
    {
        if ( targetRegistries.getOidRegistry().hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }
    
    
    protected abstract void modify( LdapDN name, Attributes entry, Attributes targetEntry, boolean cascade ) 
        throws NamingException;
    
    
    public final void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, Attributes targetEntry, 
        boolean cascade ) throws NamingException
    {
        modify( name, entry, targetEntry, cascade );
    }


    public final void modify( LdapDN name, List<ModificationItemImpl> mods, Attributes entry,
        Attributes targetEntry, boolean cascade ) throws NamingException
    {
        modify( name, entry, targetEntry, cascade );
    }

    
    protected Set<String> getOids( Set<SearchResult> results ) throws NamingException
    {
        Set<String> oids = new HashSet<String>( results.size() );
        
        for ( SearchResult result : results )
        {
            LdapDN dn = new LdapDN( result.getName() );
            dn.normalize( this.targetRegistries.getAttributeTypeRegistry().getNormalizerMapping() );
            oids.add( ( String ) dn.getRdn().getValue() );
        }
        
        return oids;
    }
    
    
    protected String getOid( Attributes entry ) throws NamingException
    {
        Attribute oid = AttributeUtils.getAttribute( entry, m_oidAT );
        if ( oid == null )
        {
            return null;
        }
        return ( String ) oid.get();
    }
    
    
    protected String getSchemaName( LdapDN name ) throws NamingException
    {
        return MetaSchemaUtils.getSchemaName( name );
    }
    
    
    protected Schema getSchema( LdapDN name ) throws NamingException
    {
        return loader.getSchema( MetaSchemaUtils.getSchemaName( name ) );
    }
    
    
    protected void unregisterOids( String oid ) throws NamingException
    {
        targetRegistries.getOidRegistry().unregister( oid );
    }
    
    
    protected void registerOids( SchemaObject obj ) throws NamingException
    {
        String[] names = obj.getNames();
        
        if ( names != null )
        {
            for ( String name: names )
            {
                targetRegistries.getOidRegistry().register( name, obj.getOid() );
            }
        }
        
        targetRegistries.getOidRegistry().register( obj.getOid(), obj.getOid() );
    }
}
