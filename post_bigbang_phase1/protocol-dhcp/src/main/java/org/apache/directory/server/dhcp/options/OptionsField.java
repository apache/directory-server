/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
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


import java.util.HashSet;
import java.util.Set;


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
    private Set<DhcpOption> options = new HashSet<DhcpOption>();


    /**
     * Adds the provided {@link DhcpOption} to this {@link OptionsField}.
     *
     * @param option
     */
    public void add( DhcpOption option )
    {
        options.add( option );
    }


    /**
     * Returns whether this {@link OptionsField} is empty.
     *
     * @return true if this {@link OptionsField} is empty.
     */
    public boolean isEmpty()
    {
        return options.isEmpty();
    }


    /**
     * Returns this {@link OptionsField} as an array of {@link DhcpOption}s.
     *
     * @return The array of {@link DhcpOption}s.
     */
    public DhcpOption[] toArray()
    {
        return options.toArray( new DhcpOption[options.size()] );
    }
}
