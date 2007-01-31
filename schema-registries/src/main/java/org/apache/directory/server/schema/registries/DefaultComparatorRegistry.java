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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple POJO implementation of the ComparatorRegistry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultComparatorRegistry implements ComparatorRegistry
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( DefaultComparatorRegistry.class );
    /** the comparators in this registry */
    private final Map<String,Comparator> byOid;
    /** maps an OID to a schema name*/
    private final Map<String,String> oidToSchema;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a default ComparatorRegistry by initializing the map and the
     * montior.
     */
    public DefaultComparatorRegistry()
    {
        this.oidToSchema = new HashMap<String,String>();
        this.byOid = new HashMap<String, Comparator>();
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    
    public void register( String schema, String oid, Comparator comparator ) throws NamingException
    {
        if ( byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "Comparator with OID " + oid + " already registered!" );
            throw e;
        }

        oidToSchema.put( oid, schema );
        byOid.put( oid, comparator );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registed comparator with OID: " + oid );
        }
    }


    public Comparator lookup( String oid ) throws NamingException
    {
        if ( byOid.containsKey( oid ) )
        {
            Comparator c = ( Comparator ) byOid.get( oid );
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up comparator with OID: " + oid );
            }
            return c;
        }

        NamingException e = new NamingException( "Comparator not found for OID: " + oid );
        throw e;
    }


    public boolean hasComparator( String oid )
    {
        return byOid.containsKey( oid );
    }


    public String getSchemaName( String oid ) throws NamingException
    {
        if ( ! Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new NamingException( "OID " + oid + " is not a numeric OID" );
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


    public void unregister( String oid ) throws NamingException
    {
        if ( ! Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new NamingException( "OID " + oid + " is not a numeric OID" );
        }

        this.byOid.remove( oid );
        this.oidToSchema.remove( oid );
    }
    
    
    public void unregisterSchemaElements( String schemaName )
    {
        List<String> oids = new ArrayList<String>( byOid.keySet() );
        for ( String oid : oids )
        {
            String schemaNameForOid = oidToSchema.get( oid );
            if ( schemaNameForOid.equalsIgnoreCase( schemaName ) )
            {
                byOid.remove( oid );
                oidToSchema.remove( oid );
            }
        }
    }


    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
        List<String> oids = new ArrayList<String>( byOid.keySet() );
        for ( String oid : oids )
        {
            String schemaNameForOid = oidToSchema.get( oid );
            if ( schemaNameForOid.equalsIgnoreCase( originalSchemaName ) )
            {
                oidToSchema.remove( oid );
                oidToSchema.put( oid, newSchemaName );
            }
        }
    }
}
