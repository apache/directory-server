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
package org.apache.eve.schema;


import org.apache.ldap.common.schema.MatchingRule;

import java.util.Map;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;


/**
 * A MatchingRuleRegistry service used to lookup matching rules by OID.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapMatchingRuleRegistry implements MatchingRuleRegistry
{
    /** a map using an OID for the key and a MatchingRule for the value */
    private final Map matchingRules;
    /** a monitor used to track noteable registry events */
    private MatchingRuleRegistryMonitor monitor = null;
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a BootstrapMatchingRuleRegistry using existing MatchingRulees 
     * for lookups.
     * 
     * @param matchingRules a map of OIDs to their respective MatchingRule
     *      objects
     */
    public BootstrapMatchingRuleRegistry( MatchingRule[] matchingRules,
                                          OidRegistry registry )
    {
        this ( matchingRules, registry,
            new MatchingRuleRegistryMonitorAdapter() );
    }

        
    /**
     * Creates a BootstrapMatchingRuleRegistry using existing MatchingRulees 
     * for lookups.
     * 
     * @param matchingRules a map of OIDs to their respective MatchingRule
     *      objects
     */
    public BootstrapMatchingRuleRegistry( MatchingRule[] matchingRules,
                                          OidRegistry registry,
                                          MatchingRuleRegistryMonitor monitor )
    {
        this.monitor = monitor;
        this.matchingRules = new HashMap();
        
        for ( int ii = 0; ii < matchingRules.length; ii++ )
        {
            this.matchingRules.put( matchingRules[ii].getOid(),
                matchingRules[ii] );
            registry.register( matchingRules[ii].getOid(),
                matchingRules[ii].getOid() );

            if ( matchingRules[ii].getName() != null )
            {    
                registry.register( matchingRules[ii].getName(),
                    matchingRules[ii].getOid() );
            }
            
            monitor.registered( matchingRules[ii] );
        }
    }
    

    // ------------------------------------------------------------------------
    // MatchingRuleRegistry interface methods
    // ------------------------------------------------------------------------
    
    
    /**
     * @see org.apache.eve.schema.MatchingRuleRegistry#lookup(java.lang.String)
     */
    public MatchingRule lookup( String oid ) throws NamingException
    {
        if ( matchingRules.containsKey( oid ) )
        {
            MatchingRule MatchingRule = ( MatchingRule )
                matchingRules.get( oid );
            monitor.lookedUp( MatchingRule );
            return MatchingRule;
        }
        
        NamingException fault = new NamingException( "Unknown MatchingRule OID "
            + oid );
        monitor.lookupFailed( oid, fault );
        throw fault;
    }
    

    /**
     * @see org.apache.eve.schema.MatchingRuleRegistry#register(
     * org.apache.ldap.common.schema.MatchingRule)
     */
    public void register( MatchingRule MatchingRule ) throws NamingException
    {
        NamingException fault = new OperationNotSupportedException(
                "MatchingRule registration on read-only bootstrap " +
                "MatchingRuleRegistry not supported." );
        monitor.registerFailed( MatchingRule, fault );
        throw fault;
    }

    
    /**
     * @see org.apache.eve.schema.MatchingRuleRegistry#hasMatchingRule(
     * java.lang.String)
     */
    public boolean hasMatchingRule( String oid )
    {
        return matchingRules.containsKey( oid );
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
}
