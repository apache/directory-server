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


import org.apache.ldap.common.schema.DITStructureRule;

import java.util.Map;
import java.util.HashMap;
import javax.naming.NamingException;


/**
 * A plain old java object implementation of an DITStructureRuleRegistry.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultDITStructureRuleRegistry implements DITStructureRuleRegistry
{
    /** maps an OID to an DITStructureRule */
    private final Map byOid;
    /** monitor notified via callback events */
    private DITStructureRuleRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty DefaultDITStructureRuleRegistry.
     */
    public DefaultDITStructureRuleRegistry()
    {
        byOid = new HashMap();
        monitor = new DITStructureRuleRegistryMonitorAdapter();
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


    public void register( DITStructureRule dITStructureRule ) throws NamingException
    {
        if ( byOid.containsKey( dITStructureRule.getOid() ) )
        {
            NamingException e = new NamingException( "dITStructureRule w/ OID " +
                dITStructureRule.getOid() + " has already been registered!" );
            monitor.registerFailed( dITStructureRule, e );
            throw e;
        }

        byOid.put( dITStructureRule.getOid(), dITStructureRule );
        monitor.registered( dITStructureRule );
    }


    public DITStructureRule lookup( String oid ) throws NamingException
    {
        if ( ! byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "dITStructureRule w/ OID "
                + oid + " not registered!" );
            monitor.lookupFailed( oid, e );
            throw e;
        }

        DITStructureRule dITStructureRule = ( DITStructureRule ) byOid.get( oid );
        monitor.lookedUp( dITStructureRule );
        return dITStructureRule;
    }


    public boolean hasDITStructureRule( String oid )
    {
        return byOid.containsKey( oid );
    }
}
