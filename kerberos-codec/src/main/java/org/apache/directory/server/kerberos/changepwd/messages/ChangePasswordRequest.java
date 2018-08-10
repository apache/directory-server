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
package org.apache.directory.server.kerberos.changepwd.messages;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswdErrorType;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.KrbPriv;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordRequest extends AbstractPasswordMessage
{
    private ApReq authHeader;
    private KrbPriv privateMessage;

    private short authHeaderLen;
    private short privateMessageLen;
    private short messageLength;


    public ChangePasswordRequest( ApReq authHeader, KrbPriv privateMessage )
    {
        this( PVNO, authHeader, privateMessage );
    }


    /**
     * Creates a new instance of ChangePasswordRequest.
     *
     * @param versionNumber
     * @param authHeader
     * @param privateMessage
     */
    public ChangePasswordRequest( short versionNumber, ApReq authHeader, KrbPriv privateMessage )
    {
        super( versionNumber );

        this.authHeader = authHeader;
        this.privateMessage = privateMessage;
    }


    /**
     * Returns the {@link ApReq}.
     *
     * @return The {@link ApReq}.
     */
    public ApReq getAuthHeader()
    {
        return authHeader;
    }


    /**
     * Returns the {@link KrbPriv}.
     *
     * @return The {@link KrbPriv}.
     */
    public KrbPriv getPrivateMessage()
    {
        return privateMessage;
    }


    @Override
    public short computeLength()
    {
        authHeaderLen = ( short ) authHeader.computeLength();
        privateMessageLen = ( short ) privateMessage.computeLength();

        messageLength = ( short ) ( HEADER_LENGTH + authHeaderLen + privateMessageLen );

        return messageLength;
    }


    @Override
    public ByteBuffer encode( ByteBuffer buf ) throws EncoderException
    {
        buf.putShort( messageLength );
        buf.putShort( getVersionNumber() );

        // Build application request bytes
        buf.putShort( authHeaderLen );
        authHeader.encode( buf );

        privateMessage.encode( buf );

        return buf;
    }


    /**
     * Decodes a {@link ByteBuffer} into a {@link ChangePasswordRequest}.
     *
     * @param buf
     * @return The {@link ChangePasswordRequest}.
     * @throws ChangePasswordException If the decoding failed
     */
    public static ChangePasswordRequest decode( ByteBuffer buf ) throws ChangePasswordException
    {
        try
        {
            buf.getShort(); // message length

            short pvno = buf.getShort();

            short authHeaderLength = buf.getShort();

            byte[] undecodedAuthHeader = new byte[authHeaderLength];
            buf.get( undecodedAuthHeader, 0, authHeaderLength );

            ApReq authHeader = KerberosDecoder.decodeApReq( undecodedAuthHeader );

            byte[] encodedPrivate = new byte[buf.remaining()];
            buf.get( encodedPrivate, 0, buf.remaining() );

            KrbPriv privMessage = KerberosDecoder.decodeKrbPriv( encodedPrivate );

            return new ChangePasswordRequest( pvno, authHeader, privMessage );
        }
        catch ( KerberosException e )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_MALFORMED, e );
        }
    }

}
