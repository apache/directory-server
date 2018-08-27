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

package org.apache.directory.server.dhcp.options.dhcp;


import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * This option is used to convey the type of the DHCP message.  The code
 * for this option is 53, and its length is 1.  Legal values for this
 * option are:
 * 
 *         Value   Message Type
 *         -----   ------------
 *           1     DHCPDISCOVER
 *           2     DHCPOFFER
 *           3     DHCPREQUEST
 *           4     DHCPDECLINE
 *           5     DHCPACK
 *           6     DHCPNAK
 *           7     DHCPRELEASE
 *           8     DHCPINFORM
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DhcpMessageType extends DhcpOption
{
    private MessageType type;


    public DhcpMessageType()
    {
    }


    public DhcpMessageType( MessageType type )
    {
        this.type = type;
    }


    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getTag()
     */
    public byte getTag()
    {
        return 53;
    }


    @Override
    public void setData( byte[] messageType )
    {
        type = MessageType.getTypeByCode( messageType[0] );
    }


    @Override
    public byte[] getData()
    {
        return new byte[]
            { type.getCode() };
    }


    public MessageType getType()
    {
        return type;
    }
}
