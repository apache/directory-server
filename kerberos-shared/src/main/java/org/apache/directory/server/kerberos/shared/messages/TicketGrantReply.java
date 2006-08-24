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
package org.apache.directory.server.kerberos.shared.messages;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;


public class TicketGrantReply extends KdcReply
{
    /**
     * Class constructors
     */
    public TicketGrantReply()
    {
        super( MessageType.KRB_TGS_REP );
    }


    public TicketGrantReply(PreAuthenticationData[] pAData, KerberosPrincipal clientPrincipal, Ticket ticket,
        EncryptedData encPart)
    {
        super( pAData, clientPrincipal, ticket, encPart, MessageType.KRB_TGS_REP );
    }
}
