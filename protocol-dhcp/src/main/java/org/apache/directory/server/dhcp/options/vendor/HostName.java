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

package org.apache.directory.server.dhcp.options.vendor;


import org.apache.directory.server.dhcp.options.StringOption;


/**
 * This option specifies the name of the client.  The name may or may
 * not be qualified with the local domain name (see section 3.17 for the
 * preferred way to retrieve the domain name).  See RFC 1035 for
 * character set restrictions.
 * 
 * The code for this option is 12, and its minimum length is 1.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HostName extends StringOption
{
    public HostName()
    {
    }


    /**
     * @param name
     */
    public HostName( String name )
    {
        setString( name );
    }


    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getTag()
     */
    public byte getTag()
    {
        return 12;
    }
}
