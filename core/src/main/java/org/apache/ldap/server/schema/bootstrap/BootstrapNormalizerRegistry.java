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

import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.server.schema.NormalizerRegistry;
import org.apache.ldap.server.schema.NormalizerRegistryMonitor;
import org.apache.ldap.server.schema.NormalizerRegistryMonitorAdapter;


/**
 * The POJO implementation for the NormalizerRegistry service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapNormalizerRegistry implements NormalizerRegistry
{
    /** a map of Normalizers looked up by OID */
    private final Map byOid;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** the monitor used to deliver callback notification events */
    private NormalizerRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a default normalizer registry.
     */
    public BootstrapNormalizerRegistry()
    {
        this.byOid = new HashMap();
        this.oidToSchema = new HashMap();
        this.monitor = new NormalizerRegistryMonitorAdapter();
    }


    /**
     * Sets the monitor used to deliver notification events to via callbacks.
     *
     * @param monitor the monitor to recieve callback events
     */
    public void setMonitor( NormalizerRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( String schema, String oid, Normalizer normalizer )
        throws NamingException
    {
        if ( byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "Normalizer already " +
                "registered for OID " + oid );
            monitor.registerFailed( oid, normalizer, e );
            throw e;
        }

        oidToSchema.put( oid, schema );
        byOid.put( oid, normalizer );
        monitor.registered( oid, normalizer );
    }


    public Normalizer lookup( String oid ) throws NamingException
    {
        if ( ! byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "Normalizer for OID "
                + oid + " does not exist!" );
            monitor.lookupFailed( oid, e );
            throw e;
        }

        Normalizer normalizer = ( Normalizer ) byOid.get( oid );
        monitor.lookedUp( oid, normalizer );
        return normalizer;
    }


    public boolean hasNormalizer( String oid )
    {
        return byOid.containsKey( oid );
    }


    public String getSchemaName( String oid ) throws NamingException
    {
        if ( Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new NamingException( "Looks like the arg is not a numeric OID" );
        }

        if ( oidToSchema.containsKey( oid ) )
        {
            return ( String ) oidToSchema.get( oid );
        }

        throw new NamingException( "OID " + oid + " not found in oid to " +
            "schema name map!" );
    }
}
