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


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.Normalizer;
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
    private final static Logger log = LoggerFactory.getLogger( DefaultNormalizerRegistry.class );
    /** a map of Normalizers looked up by OID */
    private final Map<String, Normalizer> byOid;
    /** maps an OID to a schema name*/
    private final Map<String, String> oidToSchema;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a default normalizer registry.
     */
    public DefaultNormalizerRegistry()
    {
        this.byOid = new HashMap<String, Normalizer>();
        this.oidToSchema = new HashMap<String, String>();
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    
    public void register( String schema, String oid, Normalizer normalizer ) throws NamingException
    {
        if ( byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "Normalizer already " + "registered for OID " + oid );
            throw e;
        }

        oidToSchema.put( oid, schema );
        byOid.put( oid, normalizer );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registered normalizer with oid: " + oid );
        }
    }


    public Normalizer lookup( String oid ) throws NamingException
    {
        if ( !byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "Normalizer for OID " + oid + " does not exist!" );
            throw e;
        }

        Normalizer normalizer = ( Normalizer ) byOid.get( oid );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registered normalizer with oid: " + oid );
        }
        return normalizer;
    }


    public boolean hasNormalizer( String oid )
    {
        return byOid.containsKey( oid );
    }


    public String getSchemaName( String oid ) throws NamingException
    {
        if ( ! Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new NamingException( "Looks like the arg is not a numeric OID" );
        }

        if ( oidToSchema.containsKey( oid ) )
        {
            return ( String ) oidToSchema.get( oid );
        }

        throw new NamingException( "OID " + oid + " not found in oid to " + "schema name map!" );
    }


    public Iterator<String> oidIterator()
    {
        return byOid.keySet().iterator();
    }
}
