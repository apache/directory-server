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


import org.apache.ldap.common.schema.AttributeType;

import java.util.Map;
import java.util.HashMap;
import javax.naming.NamingException;


/**
 * A plain old java object implementation of an AttributeTypeRegistry.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultAttributeTypeRegistry implements AttributeTypeRegistry
{
    /** maps an OID to an AttributeType */
    private final Map byOid;
    /** monitor notified via callback events */
    private AttributeTypeRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty DefaultAttributeTypeRegistry.
     */
    public DefaultAttributeTypeRegistry()
    {
        byOid = new HashMap();
        monitor = new AttributeTypeRegistryMonitorAdapter();
    }


    /**
     * Sets the monitor that is to be notified via callback events.
     *
     * @param monitor the new monitor to notify of notable events
     */
    public void setMonitor( AttributeTypeRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( AttributeType attributeType ) throws NamingException
    {
        if ( byOid.containsKey( attributeType.getOid() ) )
        {
            NamingException e = new NamingException( "attributeType w/ OID " +
                attributeType.getOid() + " has already been registered!" );
            monitor.registerFailed( attributeType, e );
            throw e;
        }

        byOid.put( attributeType.getOid(), attributeType );
        monitor.registered( attributeType );
    }


    public AttributeType lookup( String oid ) throws NamingException
    {
        if ( ! byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "attributeType w/ OID "
                + oid + " not registered!" );
            monitor.lookupFailed( oid, e );
            throw e;
        }

        AttributeType attributeType = ( AttributeType ) byOid.get( oid );
        monitor.lookedUp( attributeType );
        return attributeType;
    }


    public boolean hasAttributeType( String oid )
    {
        return byOid.containsKey( oid );
    }
}
