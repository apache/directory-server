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
package org.apache.directory.server.schema.registries;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.registries.NormalizerRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The POJO implementation for the NormalizerRegistry service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultNormalizerRegistry implements NormalizerRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultNormalizerRegistry.class );
    
    /** A speedup for debug */
    private static final boolean DEBUG = LOG.isDebugEnabled();
    
    /** a map of Normalizers looked up by OID */
    private final Map<String,Normalizer> byOidNormalizer;
    
    /** maps an OID to a normalizerDescription */
    private final Map<String,NormalizerDescription> oidToDescription;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a new default DefaultNormalizerRegistry.
     */
    public DefaultNormalizerRegistry()
    {
        byOidNormalizer = new ConcurrentHashMap<String, Normalizer>();
        oidToDescription = new ConcurrentHashMap<String, NormalizerDescription>();
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void register( NormalizerDescription description, Normalizer normalizer ) throws NamingException
    {
        String oid = description.getOid();
        
        if ( byOidNormalizer.containsKey( oid ) )
        {
            String msg = "Normalizer already registered for OID " + oid;
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        oidToDescription.put( oid, description );
        byOidNormalizer.put( oid, normalizer );
        
        if ( DEBUG )
        {
            LOG.debug( "registered normalizer with oid: {}", oid );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void register( Normalizer normalizer ) throws NamingException
    {
        String oid = normalizer.getOid();
        
        if ( byOidNormalizer.containsKey( oid ) )
        {
            String msg = "Normalizer already registered for OID " + oid;
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        byOidNormalizer.put( oid, normalizer );
        
        if ( DEBUG )
        {
            LOG.debug( "registered normalizer with oid: {}", oid );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public Normalizer lookup( String oid ) throws NamingException
    {
        if ( !byOidNormalizer.containsKey( oid ) )
        {
            String msg = "Normalizer for OID " + oid + " does not exist!";
            LOG.debug( msg );
            throw new NamingException( msg );
        }

        Normalizer normalizer = byOidNormalizer.get( oid );
        
        if ( DEBUG )
        {
            LOG.debug( "registered normalizer with oid: {}", oid );
        }
        
        return normalizer;
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        return byOidNormalizer.containsKey( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws NamingException
    {
        if ( !OID.isOID( oid ) )
        {
            String msg = "OID " + oid + " is not a numeric OID";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        NormalizerDescription description = oidToDescription.get( oid );
        
        if ( description != null )
        {
            return getSchema( description );
        }

        String msg = "OID " + oid + " not found in oid to schema name map!";
        LOG.error( msg );
        throw new NamingException( msg );
    }


    private static String getSchema( NormalizerDescription desc )
    {
        List<String> values = desc.getExtensions().get( "X-SCHEMA" );
        
        if ( ( values == null ) || ( values.size() == 0 ) )
        {
            return "other";
        }
        
        return values.get( 0 );
    }
    

    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return byOidNormalizer.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Normalizer> iterator()
    {
        return byOidNormalizer.values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public void unregister( String oid ) throws NamingException
    {
        if ( !OID.isOID( oid ) )
        {
            String msg = "OID " + oid + " is not a numeric OID";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        byOidNormalizer.remove( oid );
        oidToDescription.remove( oid );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName )
    {
        List<String> oids = new ArrayList<String>( byOidNormalizer.keySet() );
        
        for ( String oid : oids )
        {
            NormalizerDescription description = oidToDescription.get( oid );
            String schemaNameForOid = getSchema( description );
            
            if ( schemaNameForOid.equalsIgnoreCase( schemaName ) )
            {
                byOidNormalizer.remove( oid );
                oidToDescription.remove( oid );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
        List<String> oids = new ArrayList<String>( byOidNormalizer.keySet() );
        
        for ( String oid : oids )
        {
            NormalizerDescription description = oidToDescription.get( oid );
            String schemaNameForOid = getSchema( description );
            
            if ( schemaNameForOid.equalsIgnoreCase( originalSchemaName ) )
            {
                List<String> schemaExt = description.getExtensions().get( "X-SCHEMA" );
                schemaExt.clear();
                schemaExt.add( newSchemaName );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<NormalizerDescription> normalizerDescriptionIterator()
    {
        return oidToDescription.values().iterator();
    }
}
