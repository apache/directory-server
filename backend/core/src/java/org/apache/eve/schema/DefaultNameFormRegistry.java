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


import org.apache.ldap.common.schema.NameForm;

import java.util.Map;
import java.util.HashMap;
import javax.naming.NamingException;


/**
 * A plain old java object implementation of an NameFormRegistry.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultNameFormRegistry implements NameFormRegistry
{
    /** maps an OID to an NameForm */
    private final Map byOid;
    /** monitor notified via callback events */
    private NameFormRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty DefaultNameFormRegistry.
     */
    public DefaultNameFormRegistry()
    {
        byOid = new HashMap();
        monitor = new NameFormRegistryMonitorAdapter();
    }


    /**
     * Sets the monitor that is to be notified via callback events.
     *
     * @param monitor the new monitor to notify of notable events
     */
    public void setMonitor( NameFormRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( NameForm nameForm ) throws NamingException
    {
        if ( byOid.containsKey( nameForm.getOid() ) )
        {
            NamingException e = new NamingException( "nameForm w/ OID " +
                nameForm.getOid() + " has already been registered!" );
            monitor.registerFailed( nameForm, e );
            throw e;
        }

        byOid.put( nameForm.getOid(), nameForm );
        monitor.registered( nameForm );
    }


    public NameForm lookup( String oid ) throws NamingException
    {
        if ( ! byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "nameForm w/ OID "
                + oid + " not registered!" );
            monitor.lookupFailed( oid, e );
            throw e;
        }

        NameForm nameForm = ( NameForm ) byOid.get( oid );
        monitor.lookedUp( nameForm );
        return nameForm;
    }


    public boolean hasNameForm( String oid )
    {
        return byOid.containsKey( oid );
    }
}
