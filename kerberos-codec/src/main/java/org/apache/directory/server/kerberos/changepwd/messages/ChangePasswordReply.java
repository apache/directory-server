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


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswdErrorType;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.ApRep;
import org.apache.directory.shared.kerberos.messages.KrbPriv;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordReply extends AbstractPasswordMessage
{
    private ApRep applicationReply;
    private KrbPriv privateMessage;

    private short applicationReplyLen;
    private short privateMessageLen;
    private short messageLength;


    public ChangePasswordReply( ApRep applicationReply, KrbPriv privateMessage )
    {
        this( PVNO, applicationReply, privateMessage );
    }


    /**
     * Creates a new instance of ChangePasswordReply.
     *
     * @param versionNumber The version number
     * @param applicationReply The application reply
     * @param privateMessage The private part
     */
    public ChangePasswordReply( short versionNumber, ApRep applicationReply, KrbPriv privateMessage )
    {
        super( versionNumber );

        this.applicationReply = applicationReply;
        this.privateMessage = privateMessage;
    }


    /**
     * Returns the {@link ApRep} instance.
     *
     * @return The {@link ApRep} instance.
     */
    public ApRep getApplicationReply()
    {
        return applicationReply;
    }


    /**
     * Returns the {@link KrbPriv} instance.
     *
     * @return The {@link KrbPriv} instance.
     */
    public KrbPriv getPrivateMessage()
    {
        return privateMessage;
    }


    @Override
    public short computeLength()
    {
        applicationReplyLen = ( short ) applicationReply.computeLength();
        privateMessageLen = ( short ) privateMessage.computeLength();

        messageLength = ( short ) ( HEADER_LENGTH + applicationReplyLen + privateMessageLen );

        return messageLength;
    }


    @Override
    public ByteBuffer encode( ByteBuffer buf ) throws EncoderException
    {
        buf.putShort( messageLength );
        buf.putShort( getVersionNumber() );
        buf.putShort( applicationReplyLen );

        applicationReply.encode( buf );
        privateMessage.encode( buf );

        return buf;
    }


    /**
     * Decodes a {@link ByteBuffer} into a {@link ChangePasswordReply}.
     *
     * @param buf
     * @return The {@link ChangePasswordReply}.
     * @throws ChangePasswordException If teh decoding failed
     */
    public static ChangePasswordReply decode( ByteBuffer buf ) throws ChangePasswordException
    {
        try
        {
            short messageLength = buf.getShort();
            short protocolVersion = buf.getShort();
            short encodedAppReplyLength = buf.getShort();

            byte[] encodedAppReply = new byte[encodedAppReplyLength];
            buf.get( encodedAppReply );

            ApRep applicationReply = KerberosDecoder.decodeApRep( encodedAppReply );

            int privateBytesLength = messageLength - HEADER_LENGTH - encodedAppReplyLength;
            byte[] encodedPrivateMessage = new byte[privateBytesLength];
            buf.get( encodedPrivateMessage );

            KrbPriv privateMessage = KerberosDecoder.decodeKrbPriv( encodedPrivateMessage );

            return new ChangePasswordReply( protocolVersion, applicationReply, privateMessage );
        }
        catch ( KerberosException e )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_MALFORMED, e );
        }
    }
}
