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


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaWrapper;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.schema.loader.ldif.SchemaEntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract registry synchronizer with some reused functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractRegistrySynchronizer implements RegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractRegistrySynchronizer.class );

    /** The global registries */
    protected final Registries registries;
    
    /** The OID registry */
    protected final OidRegistry oidRegistry;
    
    /** The m-oid AttrributeType */
    protected final AttributeType m_oidAT;
    
    /** The Schema objetc factory */
    protected final SchemaEntityFactory factory;

    
    protected AbstractRegistrySynchronizer( Registries targetRegistries ) throws Exception
    {
        registries = targetRegistries;
        m_oidAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
        factory = new SchemaEntityFactory();
        oidRegistry = registries.getOidRegistry();
    }
    
    
    /**
     * Tells if the schema the DN references is loaded or not
     *
     * @param dn The SchemaObject's DN 
     * @return true if the schema is loaded
     * @throws Exception If The DN is not a SchemaObject DN
     */
    protected boolean isSchemaLoaded( LdapDN dn ) throws Exception
    {
        return registries.isSchemaLoaded( getSchemaName( dn ) );
    }
    
    
    /**
     * Tells if the schemaName is loaded or not
     *
     * @param schemaName The schema we want to check
     * @return true if the schema is loaded
     */
    protected boolean isSchemaLoaded( String schemaName )
    {
        return registries.isSchemaLoaded( schemaName );
    }
    
    
    /**
     * Tells if a schema is loaded and enabled 
     *
     * @param schemaName The schema we want to check
     * @return true if the schema is loaded and enabled, false otherwise
     */
    protected boolean isSchemaEnabled( String schemaName )
    {
        Schema schema = registries.getLoadedSchema( schemaName );
        
        return ( ( schema != null ) && schema.isEnabled() );
    }
    
    
    /**
     * Exctract the schema name from the DN. It is supposed to be the 
     * second RDN in the dn :
     * <pre>
     * ou=schema, cn=MySchema, ...
     * </pre>
     * Here, the schemaName is MySchema
     *
     * @param dn The DN we want to get the schema name from
     * @return The schema name
     * @throws NamingException If we got an error
     */
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

        if ( oidRegistry.hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }


    protected void checkOidIsUnique( SchemaObject schemaObject ) throws Exception
    {
        String oid = schemaObject.getOid();

        if ( oidRegistry.hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }


    protected void checkOidIsUnique( String oid ) throws Exception
    {
        if ( oidRegistry.hasOid( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }

    
    /**
     * Add a new SchemaObject to the schema registry, assuming that
     * it has an associated schema and that this schema is loaded
     */
    protected void addToSchema( SchemaObject schemaObject, String schemaName ) throws Exception
    {
        if ( isSchemaLoaded( schemaName ) )
        {
            Set<SchemaWrapper> schemaObjects = registries.getObjectBySchemaname().get( schemaName );
            
            if ( schemaObjects == null )
            {
                schemaObjects = registries.addSchema( schemaName );
            }
            
            SchemaWrapper schemaWrapper = new SchemaWrapper( schemaObject );
            
            if ( schemaObjects.contains( schemaWrapper ) )
            {
                String msg = "Cannot inject " + schemaObject.getName() + " into " + schemaName + 
                " as this schema already contains this element";
                LOG.warn( msg );
            
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            schemaObjects.add( schemaWrapper );
            LOG.debug( "The SchemaObject {} has been added to the schema {}", schemaObject, schemaName   );
        }
        else
        {
            String msg = "Cannot inject " + schemaObject.getName() + " into " + schemaName + 
            " as this schema is not loaded";
            LOG.warn( msg );
        
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
    }

    
    
    
    /**
     * Delete a SchemaObject from the schema registry, assuming that
     * it has an associated schema and that this schema is loaded
     */
    protected void deleteFromSchema( SchemaObject schemaObject, String schemaName ) throws Exception
    {
        if ( isSchemaLoaded( schemaName ) )
        {
            Set<SchemaWrapper> schemaObjects = registries.getObjectBySchemaname().get( schemaName );

            SchemaWrapper schemaWrapper = new SchemaWrapper( schemaObject );
            
            if ( !schemaObjects.contains( schemaWrapper ) )
            {
                String msg = "Cannot remove " + schemaObject.getName() + " from " + schemaName + 
                " as this schema does not contain this element";
                LOG.warn( msg );
            
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            schemaObjects.remove( schemaWrapper );
            LOG.debug(  "The SchemaObject {} has been removed from the schema {}", schemaObject, schemaName );
        }
        else
        {
            String msg = "Cannot inject " + schemaObject.getName() + " into " + schemaName + 
            " as this schema is not loaded";
            LOG.warn( msg );
        
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
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
    
    
    protected void unregisterOids( SchemaObject obj ) throws Exception
    {
        oidRegistry.unregister( obj.getOid() );
    }
    
    
    protected void registerOids( SchemaObject obj ) throws Exception
    {
        oidRegistry.register( obj );
    }
}
