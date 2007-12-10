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

import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Authenticator implements Encodable
{
    /**
     * Constant for the authenticator version number.
     */
    public static final int AUTHENTICATOR_VNO = 5;

    private int versionNumber;
    private KerberosPrincipal clientPrincipal;
    private Checksum checksum;
    private int clientMicroSecond;
    private KerberosTime clientTime;
    private EncryptionKey subSessionKey;
    private int sequenceNumber;
    private AuthorizationData authorizationData;


    /**
     * Creates a new instance of Authenticator.
     *
     * @param clientPrincipal
     * @param checksum
     * @param clientMicroSecond
     * @param clientTime
     * @param subSessionKey
     * @param sequenceNumber
     * @param authorizationData
     */
    public Authenticator( KerberosPrincipal clientPrincipal, Checksum checksum, int clientMicroSecond,
        KerberosTime clientTime, EncryptionKey subSessionKey, int sequenceNumber, AuthorizationData authorizationData )
    {
        this( AUTHENTICATOR_VNO, clientPrincipal, checksum, clientMicroSecond, clientTime, subSessionKey,
            sequenceNumber, authorizationData );
    }


    /**
     * Creates a new instance of Authenticator.
     *
     * @param versionNumber
     * @param clientPrincipal
     * @param checksum
     * @param clientMicroSecond
     * @param clientTime
     * @param subSessionKey
     * @param sequenceNumber
     * @param authorizationData
     */
    public Authenticator( int versionNumber, KerberosPrincipal clientPrincipal, Checksum checksum,
        int clientMicroSecond, KerberosTime clientTime, EncryptionKey subSessionKey, int sequenceNumber,
        AuthorizationData authorizationData )
    {
        this.versionNumber = versionNumber;
        this.clientPrincipal = clientPrincipal;
        this.checksum = checksum;
        this.clientMicroSecond = clientMicroSecond;
        this.clientTime = clientTime;
        this.subSessionKey = subSessionKey;
        this.sequenceNumber = sequenceNumber;
        this.authorizationData = authorizationData;
    }


    /**
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }


    /**
     * Returns the client {@link KerberosTime}.
     *
     * @return The client {@link KerberosTime}.
     */
    public KerberosTime getClientTime()
    {
        return clientTime;
    }


    /**
     * Returns the client microsecond.
     *
     * @return The client microsecond.
     */
    public int getClientMicroSecond()
    {
        return clientMicroSecond;
    }


    /**
     * Returns the {@link AuthorizationData}.
     *
     * @return The {@link AuthorizationData}.
     */
    public AuthorizationData getAuthorizationData()
    {
        return authorizationData;
    }


    /**
     * Returns the {@link Checksum}.
     *
     * @return The {@link Checksum}.
     */
    public Checksum getChecksum()
    {
        return checksum;
    }


    /**
     * Returns the sequence number.
     *
     * @return The sequence number.
     */
    public int getSequenceNumber()
    {
        return sequenceNumber;
    }


    /**
     * Returns the sub-session key.
     *
     * @return The sub-session key.
     */
    public EncryptionKey getSubSessionKey()
    {
        return subSessionKey;
    }


    /**
     * Returns the version number of the {@link Authenticator}.
     *
     * @return The version number of the {@link Authenticator}.
     */
    public int getVersionNumber()
    {
        return versionNumber;
    }
}
