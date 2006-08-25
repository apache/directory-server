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
package org.apache.directory.server.core.schema.global;


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.NormalizerRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapNormalizerRegistry;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple POJO implementation of the NormalizerRegistry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GlobalNormalizerRegistry implements NormalizerRegistry
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( GlobalNormalizerRegistry.class );
    /** the normalizers in this registry */
    private final Map normalizers;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** the underlying bootstrap registry to delegate on misses to */
    private BootstrapNormalizerRegistry bootstrap;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a default NormalizerRegistry by initializing the map and the
     * montior.
     */
    public GlobalNormalizerRegistry(BootstrapNormalizerRegistry bootstrap)
    {
        this.oidToSchema = new HashMap();
        this.normalizers = new HashMap();
        this.bootstrap = bootstrap;
        if ( this.bootstrap == null )
        {
            throw new NullPointerException( "the bootstrap registry cannot be null" );
        }
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    public void register( String schema, String oid, Normalizer normalizer ) throws NamingException
    {
        if ( normalizers.containsKey( oid ) || bootstrap.hasNormalizer( oid ) )
        {
            NamingException e = new NamingException( "Normalizer with OID " + oid + " already registered!" );
            throw e;
        }

        oidToSchema.put( oid, schema );
        normalizers.put( oid, normalizer );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registered normalizer for OID: " + oid );
        }
    }


    public Normalizer lookup( String oid ) throws NamingException
    {
        Normalizer n;
        NamingException e;

        if ( normalizers.containsKey( oid ) )
        {
            n = ( Normalizer ) normalizers.get( oid );
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up normalizer for OID: " + oid );
            }
            return n;
        }

        if ( bootstrap.hasNormalizer( oid ) )
        {
            n = bootstrap.lookup( oid );
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up normalizer for OID: " + oid );
            }
            return n;
        }

        e = new NamingException( "Normalizer not found for OID: " + oid );
        throw e;
    }


    public boolean hasNormalizer( String oid )
    {
        return normalizers.containsKey( oid ) || bootstrap.hasNormalizer( oid );
    }


    public String getSchemaName( String oid ) throws NamingException
    {
        if ( !Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new NamingException( "OID " + oid + " is not a numeric OID" );
        }

        if ( oidToSchema.containsKey( oid ) )
        {
            return ( String ) oidToSchema.get( oid );
        }

        if ( bootstrap.hasNormalizer( oid ) )
        {
            return bootstrap.getSchemaName( oid );
        }

        throw new NamingException( "OID " + oid + " not found in oid to " + "schema name map!" );
    }
}
