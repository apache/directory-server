/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 * 
 */

package org.apache.directory.server.dhcp.messages;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum MessageType
{
    DHCPUNRECOGNIZED(( byte ) -1, "unrecognized"),

    DHCPDISCOVER(( byte ) 1, "DHCP Discover"),

    DHCPOFFER(( byte ) 2, "DHCP Offer"),

    DHCPREQUEST(( byte ) 3, "DHCP Request"),

    DHCPDECLINE(( byte ) 4, "DHCP Decline"),

    DHCPACK(( byte ) 5, "DHCP Acknowledge"),

    DHCPNAK(( byte ) 6, "DHCP Not Acknowledge"),

    DHCPRELEASE(( byte ) 7, "DHCP Release"),

    DHCPINFORM(( byte ) 8, "DHCP Inform");

    private String name;
    private byte ordinal;


    public static MessageType getTypeByCode( byte type )
    {
        for ( MessageType mt : MessageType.values() )
        {
            if ( type == mt.getCode() )
            {
                return mt;
            }
        }
        return DHCPUNRECOGNIZED;
    }


    public byte getCode()
    {
        return ordinal;
    }


    /**
     * Private constructor prevents construction outside of this class.
     */
    MessageType( byte ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    @Override
    public String toString()
    {
        return name;
    }
}
