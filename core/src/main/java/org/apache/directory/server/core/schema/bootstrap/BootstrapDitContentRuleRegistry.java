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
package org.apache.directory.server.core.schema.bootstrap;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.DITContentRuleRegistry;
import org.apache.directory.server.core.schema.DITContentRuleRegistryMonitor;
import org.apache.directory.server.core.schema.DITContentRuleRegistryMonitorAdapter;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.shared.ldap.schema.DITContentRule;


/**
 * A plain old java object implementation of an DITContentRuleRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapDitContentRuleRegistry implements DITContentRuleRegistry
{
    /** maps an OID to an DITContentRule */
    private final Map byOid;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;
    /** monitor notified via callback events */
    private DITContentRuleRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty BootstrapDitContentRuleRegistry.
     */
    public BootstrapDitContentRuleRegistry( OidRegistry oidRegistry )
    {
        this.byOid = new HashMap();
        this.oidToSchema = new HashMap();
        this.oidRegistry = oidRegistry;
        this.monitor = new DITContentRuleRegistryMonitorAdapter();
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


    public void register( String schema, DITContentRule dITContentRule ) throws NamingException
    {
        if ( byOid.containsKey( dITContentRule.getOid() ) )
        {
            NamingException e = new NamingException( "dITContentRule w/ OID " +
                dITContentRule.getOid() + " has already been registered!" );
            monitor.registerFailed( dITContentRule, e );
            throw e;
        }

        oidRegistry.register( dITContentRule.getName(), dITContentRule.getOid() ) ;
        byOid.put( dITContentRule.getOid(), dITContentRule );
        oidToSchema.put( dITContentRule.getOid(), schema );
        monitor.registered( dITContentRule );
    }


    public DITContentRule lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( ! byOid.containsKey( id ) )
        {
            NamingException e = new NamingException( "dITContentRule w/ OID "
                + id + " not registered!" );
            monitor.lookupFailed( id, e );
            throw e;
        }

        DITContentRule dITContentRule = ( DITContentRule ) byOid.get( id );
        monitor.lookedUp( dITContentRule );
        return dITContentRule;
    }


    public boolean hasDITContentRule( String id )
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


    public Iterator list()
    {
        return byOid.values().iterator();
    }
}
