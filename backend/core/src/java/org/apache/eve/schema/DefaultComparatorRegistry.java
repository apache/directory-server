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


import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import javax.naming.NamingException;


/**
 * A simple POJO implementation of the ComparatorRegistry service interface.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultComparatorRegistry implements ComparatorRegistry
{
    /** the comparators in this registry */
    private final Map comparators;
    /** the monitor for delivering callback events */
    private ComparatorRegistryMonitor monitor;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a default ComparatorRegistry by initializing the map and the
     * montior.
     */
    public DefaultComparatorRegistry()
    {
        comparators = new HashMap();
        monitor = new ComparatorRegistryMonitorAdapter();
    }


    /**
     * Sets the monitor used by this registry.
     *
     * @param monitor the monitor to set for registry event callbacks
     */
    public void setMonitor( ComparatorRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( String oid, Comparator comparator ) throws NamingException
    {
        if ( comparators.containsKey( oid ) )
        {
            NamingException e = new NamingException( "Comparator with OID "
                + oid + " already registered!" );
            monitor.registerFailed( oid, comparator, e );
            throw e;
        }

        monitor.registered( oid, comparator );
        comparators.put( oid, comparator );
    }


    public Comparator lookup( String oid ) throws NamingException
    {
        if ( ! comparators.containsKey( oid ) )
        {
            Comparator c = ( Comparator ) comparators.get( oid );
            monitor.lookedUp( oid, c );
            return c;
        }


        NamingException e = new NamingException( "Comparator not found for OID" );
        monitor.lookupFailed( oid, e );
        throw e;
    }


    public boolean hasComparator( String oid )
    {
        return comparators.containsKey( oid );
    }
}
