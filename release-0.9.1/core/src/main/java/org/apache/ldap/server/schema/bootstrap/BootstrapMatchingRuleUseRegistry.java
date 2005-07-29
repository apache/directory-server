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

import org.apache.ldap.common.schema.MatchingRuleUse;
import org.apache.ldap.server.schema.MatchingRuleUseRegistry;
import org.apache.ldap.server.schema.MatchingRuleUseRegistryMonitor;
import org.apache.ldap.server.schema.MatchingRuleUseRegistryMonitorAdapter;


/**
 * A plain old java object implementation of an MatchingRuleUseRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapMatchingRuleUseRegistry implements MatchingRuleUseRegistry
{
    /** maps a name to an MatchingRuleUse */
    private final Map byName;
    /** maps a MatchingRuleUse name to a schema name*/
    private final Map nameToSchema;
    /** monitor notified via callback events */
    private MatchingRuleUseRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty BootstrapMatchingRuleUseRegistry.
     */
    public BootstrapMatchingRuleUseRegistry()
    {
        this.byName = new HashMap();
        this.nameToSchema = new HashMap();
        this.monitor = new MatchingRuleUseRegistryMonitorAdapter();
    }


    /**
     * Sets the monitor that is to be notified via callback events.
     *
     * @param monitor the new monitor to notify of notable events
     */
    public void setMonitor( MatchingRuleUseRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( String schema, MatchingRuleUse matchingRuleUse )
        throws NamingException
    {
        if ( byName.containsKey( matchingRuleUse.getName() ) )
        {
            NamingException e = new NamingException( "matchingRuleUse w/ name "
                + matchingRuleUse.getName() + " has already been registered!" );
            monitor.registerFailed( matchingRuleUse, e );
            throw e;
        }

        nameToSchema.put( matchingRuleUse.getName(), schema );
        byName.put( matchingRuleUse.getName(), matchingRuleUse );
        monitor.registered( matchingRuleUse );
    }


    public MatchingRuleUse lookup( String name ) throws NamingException
    {
        if ( ! byName.containsKey( name ) )
        {
            NamingException e = new NamingException( "matchingRuleUse w/ name "
                + name + " not registered!" );
            monitor.lookupFailed( name, e );
            throw e;
        }

        MatchingRuleUse matchingRuleUse = ( MatchingRuleUse ) byName.get( name );
        monitor.lookedUp( matchingRuleUse );
        return matchingRuleUse;
    }


    public boolean hasMatchingRuleUse( String name )
    {
        return byName.containsKey( name );
    }


    public String getSchemaName( String id ) throws NamingException
    {
        if ( nameToSchema.containsKey( id ) )
        {
            return ( String ) nameToSchema.get( id );
        }

        throw new NamingException( "Name " + id + " not found in name to " +
            "schema name map!" );
    }


    public Iterator list()
    {
        return byName.values().iterator();
    }
}
