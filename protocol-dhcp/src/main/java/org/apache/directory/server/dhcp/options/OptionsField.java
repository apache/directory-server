/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *
 */

package org.apache.directory.server.dhcp.options;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * The Dynamic Host Configuration Protocol (DHCP) provides a framework
 * for passing configuration information to hosts on a TCP/IP network.  
 * Configuration parameters and other control information are carried in
 * tagged data items that are stored in the 'options' field of the DHCP
 * message.  The data items themselves are also called "options."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class OptionsField
{
    /**
     * A map of option code (Integer)->DhcpOption. FIXME: use IntHashtable from
     * commons collections
     */
    private Map options = new HashMap();


    public void add( DhcpOption option )
    {
        options.put( new Integer( option.getTag() ), option );
    }


    public boolean isEmpty()
    {
        return options.isEmpty();
    }


    public Iterator iterator()
    {
        return options.values().iterator();
    }


    /**
     * Return the (first) DHCP option matching a given option class or
     * <code>null</code> of the option isn't set.
     * 
     * @param class1
     */
    public DhcpOption get( Class optionClass )
    {
        Integer key = new Integer( DhcpOption.getTagByClass( optionClass ) );
        return ( DhcpOption ) options.get( key );
    }


    /**
     * Return the (first) DHCP option matching a given tag or <code>null</code>
     * of the option isn't set.
     * 
     * @param class1
     */
    public DhcpOption get( int tag )
    {
        Integer key = new Integer( tag );
        return ( DhcpOption ) options.get( key );
    }


    /**
     * Merge the options from the given options field into my options. Existing
     * options are replaced by the ones from the supplied options field.
     * 
     * @param options2
     */
    public void merge( OptionsField options )
    {
        if ( null == options )
            return;

        for ( Iterator i = options.iterator(); i.hasNext(); )
        {
            DhcpOption option = ( DhcpOption ) i.next();
            this.options.put( new Integer( option.getTag() ), option );
        }
    }


    /**
     * Remove instances of the given option class.
     * 
     * @param class1
     */
    public void remove( Class c )
    {
        Integer key = new Integer( DhcpOption.getTagByClass( c ) );
        options.remove( key );
    }


    /**
     * Remove options matching the given tag
     * 
     * @param class1
     */
    public void remove( int tag )
    {
        Integer key = new Integer( tag );
        options.remove( key );
    }


    /**
     * @see Map#clear()
     */
    public void clear()
    {
        options.clear();
    }
}
