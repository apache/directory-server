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
package org.apache.directory.server.core.schema.registries.synchronizers;


import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.schema.loader.ldif.SchemaEntityFactory;

//import javax.naming.NamingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;


/**
 * An abstract registry synchronizer with some reused functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractRegistrySynchronizer implements RegistrySynchronizer
{
    protected final Registries registries;
    protected final AttributeType m_oidAT;
    protected final SchemaEntityFactory factory;

    
    protected AbstractRegistrySynchronizer( Registries targetRegistries ) throws Exception
    {
        this.registries = targetRegistries;
        this.m_oidAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
        this.factory = new SchemaEntityFactory();
    }
    
    
    protected boolean isSchemaLoaded( LdapDN dn ) throws Exception
    {
        return registries.isSchemaLoaded( getSchemaName( dn ) );
    }
    
    
    protected String getSchemaName( LdapDN dn ) throws NamingException
    {
        if ( dn.size() < 2 )
        {
            throw new NamingException( "At least two name components are expected for the dn" );
        }
        
        Rdn rdn = dn.getRdn( 1 );
        return ( String ) rdn.getValue();
    }


    protected void checkOidIsUnique( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );

        if ( registries.getOidRegistry().hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }


    protected void checkOidIsUnique( SchemaObject schemaObject ) throws Exception
    {
        String oid = schemaObject.getOid();

        if ( registries.getOidRegistry().hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }


    protected void checkOidIsUnique( String oid ) throws Exception
    {
        if ( registries.getOidRegistry().hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }
    
    
    protected abstract boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) 
        throws Exception;
    
    
    public final boolean modify( LdapDN name, ModificationOperation modOp, ServerEntry mods, ServerEntry entry, ServerEntry targetEntry, 
        boolean cascade ) throws Exception
    {
        return modify( name, entry, targetEntry, cascade );
    }


    public final boolean modify( LdapDN name, List<Modification> mods, ServerEntry entry,
        ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        return modify( name, entry, targetEntry, cascade );
    }

    
    protected Set<String> getOids( Set<ServerEntry> results ) throws Exception
    {
        Set<String> oids = new HashSet<String>( results.size() );
        
        for ( ServerEntry result : results )
        {
            LdapDN dn = result.getDn();
            dn.normalize( this.registries.getAttributeTypeRegistry().getNormalizerMapping() );
            oids.add( ( String ) dn.getRdn().getValue() );
        }
        
        return oids;
    }
    
    
    protected String getOid( ServerEntry entry ) throws Exception
    {
        EntryAttribute oid = entry.get( m_oidAT );
        
        if ( oid == null )
        {
            return null;
        }
        
        return oid.getString();
    }
    
    
    protected void unregisterOids( String oid ) throws Exception
    {
        registries.getOidRegistry().unregister( oid );
    }
    
    
    protected void registerOids( SchemaObject obj ) throws Exception
    {
        registries.getOidRegistry().register( obj );
    }
}
