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

package org.apache.directory.server.dhcp.service;


import java.net.InetSocketAddress;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;


/**
 * DHCP Protocol (RFC 2131, RFC 2132). Implementations of the DHCP service must
 * be thread-safe with respect to concurrent calls to getReplyFor().
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface DhcpService
{
    /**
     * Retrieve the reply to a given message. The reply may be zero, if the
     * message should be ignored.
     * @param localAddress TODO
     * @param clientAddress 
     * @param request
     * @return DhcpMessage
     * @throws DhcpException 
     */
    public DhcpMessage getReplyFor( InetSocketAddress localAddress, InetSocketAddress clientAddress, DhcpMessage request )
        throws DhcpException;
}
