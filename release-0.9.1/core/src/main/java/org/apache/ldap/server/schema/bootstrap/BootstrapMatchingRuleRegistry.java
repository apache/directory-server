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
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.ldap.common.schema.MatchingRule;
import org.apache.ldap.server.schema.MatchingRuleRegistry;
import org.apache.ldap.server.schema.MatchingRuleRegistryMonitor;
import org.apache.ldap.server.schema.MatchingRuleRegistryMonitorAdapter;
import org.apache.ldap.server.schema.OidRegistry;


/**
 * A MatchingRuleRegistry service used to lookup matching rules by OID.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapMatchingRuleRegistry implements MatchingRuleRegistry
{
    /** a map using an OID for the key and a MatchingRule for the value */
    private final Map byOid;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;
    /** a monitor used to track noteable registry events */
    private MatchingRuleRegistryMonitor monitor = null;
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a BootstrapMatchingRuleRegistry using existing MatchingRulees
     * for lookups.
     * 
     */
    public BootstrapMatchingRuleRegistry( OidRegistry oidRegistry )
    {
        this.oidToSchema = new HashMap();
        this.oidRegistry = oidRegistry;
        this.byOid = new HashMap();
        this.monitor = new MatchingRuleRegistryMonitorAdapter();
    }
    

    // ------------------------------------------------------------------------
    // MatchingRuleRegistry interface methods
    // ------------------------------------------------------------------------
    
    
    /**
     * @see org.apache.ldap.server.schema.MatchingRuleRegistry#lookup(String)
     */
    public MatchingRule lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( byOid.containsKey( id ) )
        {
            MatchingRule MatchingRule = ( MatchingRule ) byOid.get( id );
            monitor.lookedUp( MatchingRule );
            return MatchingRule;
        }
        
        NamingException fault = new NamingException( "Unknown MatchingRule OID " + id );
        monitor.lookupFailed( id, fault );
        throw fault;
    }
    

    /**
     * @see MatchingRuleRegistry#register(String, MatchingRule)
     */
    public void register( String schema, MatchingRule matchingRule ) throws NamingException
    {
        if ( byOid.containsKey( matchingRule.getOid() ) )
        {
            NamingException e = new NamingException( "matchingRule w/ OID " +
                matchingRule.getOid() + " has already been registered!" );
            monitor.registerFailed( matchingRule, e );
            throw e;
        }

        oidToSchema.put( matchingRule.getOid(), schema );

        String[] names = matchingRule.getNames();
        for ( int ii = 0; ii < names.length; ii++ )
        {
            oidRegistry.register( names[ii], matchingRule.getOid() );
        }

        byOid.put( matchingRule.getOid(), matchingRule );
        monitor.registered( matchingRule );
    }

    
    /**
     * @see org.apache.ldap.server.schema.MatchingRuleRegistry#hasMatchingRule(String)
     */
    public boolean hasMatchingRule( String id )
    {
        if ( oidRegistry.hasOid( id ) )
        {
            try
            {
                return byOid.containsKey( oidRegistry.getOid( id ) );
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

        throw new NamingException( "OID " + id + " not found in oid to " +
            "schema name map!" );
    }


    // ------------------------------------------------------------------------
    // package friendly monitor methods
    // ------------------------------------------------------------------------
    
    
    /**
     * Gets the monitor for this registry.
     * 
     * @return the monitor
     */
    MatchingRuleRegistryMonitor getMonitor()
    {
        return monitor;
    }

    
    /**
     * Sets the monitor for this registry.
     * 
     * @param monitor the monitor to set
     */
    void setMonitor( MatchingRuleRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    public Iterator list()
    {
        return byOid.values().iterator();
    }
}
