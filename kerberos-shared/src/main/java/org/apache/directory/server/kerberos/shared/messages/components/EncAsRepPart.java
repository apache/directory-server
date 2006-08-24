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
package org.apache.directory.server.kerberos.shared.messages.components;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.LastRequest;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;


/**
 * Encrypted part of the authentication service response
 */
public class EncAsRepPart extends EncKdcRepPart
{
    /**
     * Class constructor
     */
    public EncAsRepPart(EncryptionKey key, LastRequest lastReq, int nonce, KerberosTime keyExpiration,
        TicketFlags flags, KerberosTime authTime, KerberosTime startTime, KerberosTime endTime, KerberosTime renewTill,
        KerberosPrincipal serverPrincipal, HostAddresses caddr)
    {
        super( key, lastReq, nonce, keyExpiration, flags, authTime, startTime, endTime, renewTill, serverPrincipal,
            caddr, MessageComponentType.KRB_ENC_AS_REP_PART );
    }
}
