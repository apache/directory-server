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


import org.apache.ldap.common.schema.DITContentRule;

import java.util.Map;
import java.util.HashMap;
import javax.naming.NamingException;


/**
 * A plain old java object implementation of an DITContentRuleRegistry.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultDITContentRuleRegistry implements DITContentRuleRegistry
{
    /** maps an OID to an DITContentRule */
    private final Map byOid;
    /** monitor notified via callback events */
    private DITContentRuleRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty DefaultDITContentRuleRegistry.
     */
    public DefaultDITContentRuleRegistry()
    {
        byOid = new HashMap();
        monitor = new DITContentRuleRegistryMonitorAdapter();
    }


    /**
     * Sets the monitor that is to be notified via callback events.
     *
     * @param monitor the new monitor to notify of notable events
     */
    public void setMonitor( DITContentRuleRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( DITContentRule dITContentRule ) throws NamingException
    {
        if ( byOid.containsKey( dITContentRule.getOid() ) )
        {
            NamingException e = new NamingException( "dITContentRule w/ OID " +
                dITContentRule.getOid() + " has already been registered!" );
            monitor.registerFailed( dITContentRule, e );
            throw e;
        }

        byOid.put( dITContentRule.getOid(), dITContentRule );
        monitor.registered( dITContentRule );
    }


    public DITContentRule lookup( String oid ) throws NamingException
    {
        if ( ! byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "dITContentRule w/ OID "
                + oid + " not registered!" );
            monitor.lookupFailed( oid, e );
            throw e;
        }

        DITContentRule dITContentRule = ( DITContentRule ) byOid.get( oid );
        monitor.lookedUp( dITContentRule );
        return dITContentRule;
    }


    public boolean hasDITContentRule( String oid )
    {
        return byOid.containsKey( oid );
    }
}
