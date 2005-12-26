/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.schema.bootstrap;


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.asn1new.primitives.OID;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.server.schema.NormalizerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The POJO implementation for the NormalizerRegistry service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapNormalizerRegistry implements NormalizerRegistry
{
    /** The LoggerFactory used by this class */
    private static Logger log = LoggerFactory.getLogger( BootstrapNormalizerRegistry.class );

    /** a map of Normalizers looked up by OID */
    private final Map byOid;
    
    /** maps an OID to a schema name*/
    private final Map oidToSchema;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    /**
     * Creates a default normalizer registry.
     */
    public BootstrapNormalizerRegistry()
    {
        byOid = new HashMap();
        oidToSchema = new HashMap();
    }

    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( String schema, String oid, Normalizer normalizer )
        throws NamingException
    {
        if ( byOid.containsKey( oid ) )
        {
        	String message = "Normalizer already registered for OID " + oid;
        	
        	log.error( message);
            throw new NamingException( message );
        }

        oidToSchema.put( oid, schema );
        byOid.put( oid, normalizer );
    }


    public Normalizer lookup( String oid ) throws NamingException
    {
        if ( ! byOid.containsKey( oid ) )
        {
        	String message = "Normalizer for OID "
                + oid + " does not exist!";
        	
        	log.error( message );
            throw new NamingException( message );
        }

        return ( Normalizer ) byOid.get( oid );
    }


    public boolean hasNormalizer( String oid )
    {
        return byOid.containsKey( oid );
    }


    public String getSchemaName( String oid ) throws NamingException
    {
        if ( OID.isOID( oid )== false )
        {
        	log.error( "Invalid oid :m '" + oid + "'" );
            throw new NamingException( "Looks like the arg is not a numeric OID" );
        }

        if ( oidToSchema.containsKey( oid ) )
        {
            return ( String ) oidToSchema.get( oid );
        }

        String message = "OID " + oid + " not found in oid to schema name map!";
        log.error( message );
        throw new NamingException( message );
    }
    
    /**
     * A String representation of this class
     */
    public String toString( String tabs )
    {
    	StringBuffer sb = new StringBuffer();
    	
    	sb.append( tabs ).append( "BootstrapNormalizerRegistry : {\n" );
    	
    	sb.append( tabs ).append( "  By oid : \n" );
    	
    	sb.append( StringTools.mapToString( byOid, tabs + "    " ) ) .append( '\n' );
    	
    	sb.append( tabs ).append( "  By schema : \n" );

    	sb.append( StringTools.mapToString( oidToSchema, tabs + "    " ) ) .append( '\n' );
    	
    	sb.append( tabs ).append( "}\n" );
    	
    	return sb.toString();
    }

    /**
     * A String representation of this class
     */
    public String toString()
    {
    	return toString( "" );
    }
}
