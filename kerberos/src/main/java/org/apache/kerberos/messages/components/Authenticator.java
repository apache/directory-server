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
package org.apache.kerberos.messages.components;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.messages.Encodable;
import org.apache.kerberos.messages.value.AuthorizationData;
import org.apache.kerberos.messages.value.Checksum;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.messages.value.KerberosTime;

public class Authenticator implements Encodable
{
	public static final int AUTHENTICATOR_VNO = 5;

	private int               versionNumber;
	private KerberosPrincipal clientPrincipal;
	private Checksum          checksum;
	private int               clientMicroSecond;
	private KerberosTime      clientTime;
	private EncryptionKey     subSessionKey;
	private int               sequenceNumber;
	private AuthorizationData authorizationData;

	public Authenticator( KerberosPrincipal clientPrincipal, Checksum checksum,
            int clientMicroSecond, KerberosTime clientTime, EncryptionKey subSessionKey,
            int sequenceNumber, AuthorizationData authorizationData )
    {
        this( AUTHENTICATOR_VNO, clientPrincipal, checksum, clientMicroSecond, clientTime,
                subSessionKey, sequenceNumber, authorizationData );
    }

    public Authenticator( int versionNumber, KerberosPrincipal clientPrincipal, Checksum checksum,
            int clientMicroSecond, KerberosTime clientTime, EncryptionKey subSessionKey,
            int sequenceNumber, AuthorizationData authorizationData )
    {
        this.versionNumber     = versionNumber;
        this.clientPrincipal   = clientPrincipal;
        this.checksum          = checksum;
        this.clientMicroSecond = clientMicroSecond;
        this.clientTime        = clientTime;
        this.subSessionKey     = subSessionKey;
        this.sequenceNumber    = sequenceNumber;
        this.authorizationData = authorizationData;
    }

    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }

    public KerberosTime getClientTime()
    {
        return clientTime;
    }

    public int getClientMicroSecond()
    {
        return clientMicroSecond;
    }

    public AuthorizationData getAuthorizationData()
    {
        return authorizationData;
    }

    public Checksum getChecksum()
    {
        return checksum;
    }

    public int getSequenceNumber()
    {
        return sequenceNumber;
    }

    public EncryptionKey getSubSessionKey()
    {
        return subSessionKey;
    }

    public int getVersionNumber()
    {
        return versionNumber;
    }
}
