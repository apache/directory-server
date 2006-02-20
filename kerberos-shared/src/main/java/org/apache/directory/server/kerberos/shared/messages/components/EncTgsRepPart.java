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
package org.apache.directory.server.kerberos.shared.messages.components;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.LastRequest;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;


/**
 * Encrypted part of TGS responses
 */
public class EncTgsRepPart extends EncKdcRepPart
{
    /**
     * Class constructor
     */
    public EncTgsRepPart(EncryptionKey key, LastRequest lastReq, int nonce, KerberosTime keyExpiration,
        TicketFlags flags, KerberosTime authtime, KerberosTime starttime, KerberosTime endtime, KerberosTime renewTill,
        KerberosPrincipal serverPrincipal, HostAddresses caddr)
    {

        super( key, lastReq, nonce, keyExpiration, flags, authtime, starttime, endtime, renewTill, serverPrincipal,
            caddr, MessageComponentType.KRB_ENC_TGS_REP_PART );
    }
}
