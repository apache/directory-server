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
package org.apache.ldap.server.schema;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.ldap.common.schema.DITStructureRule;
import org.apache.ldap.common.util.JoinIterator;
import org.apache.ldap.server.schema.bootstrap.BootstrapDitStructureRuleRegistry;


/**
 * A plain old java object implementation of an DITStructureRuleRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GlobalDitStructureRuleRegistry implements DITStructureRuleRegistry
{
    /** maps an OID to an DITStructureRule */
    private final Map byOid;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;
    /** monitor notified via callback events */
    private DITStructureRuleRegistryMonitor monitor;
    /** the underlying bootstrap registry to delegate on misses to */
    private BootstrapDitStructureRuleRegistry bootstrap;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty BootstrapDitStructureRuleRegistry.
     */
    public GlobalDitStructureRuleRegistry( BootstrapDitStructureRuleRegistry bootstrap, OidRegistry oidRegistry )
    {
        this.byOid = new HashMap();
        this.oidToSchema = new HashMap();
        this.oidRegistry = oidRegistry;
        this.monitor = new DITStructureRuleRegistryMonitorAdapter();

        this.bootstrap = bootstrap;
        if ( this.bootstrap == null )
        {
            throw new NullPointerException( "the bootstrap registry cannot be null" ) ;
        }
    }


    /**
     * Sets the monitor that is to be notified via callback events.
     *
     * @param monitor the new monitor to notify of notable events
     */
    public void setMonitor( DITStructureRuleRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( String schema, DITStructureRule dITStructureRule ) throws NamingException
    {
        if ( byOid.containsKey( dITStructureRule.getOid() ) ||
             bootstrap.hasDITStructureRule( dITStructureRule.getOid() ) )
        {
            NamingException e = new NamingException( "dITStructureRule w/ OID " +
                dITStructureRule.getOid() + " has already been registered!" );
            monitor.registerFailed( dITStructureRule, e );
            throw e;
        }

        oidRegistry.register( dITStructureRule.getName(), dITStructureRule.getOid() ) ;
        byOid.put( dITStructureRule.getOid(), dITStructureRule );
        oidToSchema.put( dITStructureRule.getOid(), schema );
        monitor.registered( dITStructureRule );
    }


    public DITStructureRule lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( byOid.containsKey( id ) )
        {
            DITStructureRule dITStructureRule = ( DITStructureRule ) byOid.get( id );
            monitor.lookedUp( dITStructureRule );
            return dITStructureRule;
        }

        if ( bootstrap.hasDITStructureRule( id ) )
        {
            DITStructureRule dITStructureRule = bootstrap.lookup( id );
            monitor.lookedUp( dITStructureRule );
            return dITStructureRule;
        }

        NamingException e = new NamingException( "dITStructureRule w/ OID "
            + id + " not registered!" );
        monitor.lookupFailed( id, e );
        throw e;
    }


    public boolean hasDITStructureRule( String id )
    {
        if ( oidRegistry.hasOid( id ) )
        {
            try
            {
                return byOid.containsKey( oidRegistry.getOid( id ) ) ||
                       bootstrap.hasDITStructureRule( id );
            }
            catch ( NamingException e )
            {
                return false;
            }
        }

        return false;
    }


    public String getSchemaName( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( oidToSchema.containsKey( id ) )
        {
            return ( String ) oidToSchema.get( id );
        }

        if ( bootstrap.hasDITStructureRule( id ) )
        {
            return bootstrap.getSchemaName( id );
        }

        throw new NamingException( "OID " + id + " not found in oid to " +
            "schema name map!" );
    }


    public Iterator list()
    {
        return new JoinIterator( new Iterator[]
            { byOid.values().iterator(),bootstrap.list() } );
    }
}
