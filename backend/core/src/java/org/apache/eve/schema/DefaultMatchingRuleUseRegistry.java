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


import org.apache.ldap.common.schema.MatchingRuleUse;

import java.util.Map;
import java.util.HashMap;
import javax.naming.NamingException;


/**
 * A plain old java object implementation of an MatchingRuleUseRegistry.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultMatchingRuleUseRegistry implements MatchingRuleUseRegistry
{
    /** maps a name to an MatchingRuleUse */
    private final Map byName;
    /** monitor notified via callback events */
    private MatchingRuleUseRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty DefaultMatchingRuleUseRegistry.
     */
    public DefaultMatchingRuleUseRegistry()
    {
        byName = new HashMap();
        monitor = new MatchingRuleUseRegistryMonitorAdapter();
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


    public void register( String name, MatchingRuleUse matchingRuleUse )
        throws NamingException
    {
        if ( byName.containsKey( name ) )
        {
            NamingException e = new NamingException( "matchingRuleUse w/ name "
                + name + " has already been registered!" );
            monitor.registerFailed( matchingRuleUse, e );
            throw e;
        }

        byName.put( name, matchingRuleUse );
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
}
