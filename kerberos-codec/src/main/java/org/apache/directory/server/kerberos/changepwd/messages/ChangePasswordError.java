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

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswdErrorType;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.shared.kerberos.codec.krbError.KrbErrorContainer;
import org.apache.directory.shared.kerberos.messages.KrbError;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordError extends AbstractPasswordMessage
{
    private KrbError krbError;

    private short krbErrorLen;
    private short messageLength;

    public ChangePasswordError( KrbError krbError )
    {
        this( PVNO, krbError );
    }
    
    /**
     * Creates a new instance of ChangePasswordError.
     *
     * @param versionNumber The version number 
     * @param krbError The KRB-ERROR
     */
    public ChangePasswordError( short versionNumber, KrbError krbError )
    {
        super( versionNumber );

        this.krbError = krbError;
    }


    /**
     * Returns the {@link KrbError}.
     *
     * @return The {@link KrbError}.
     */
    public KrbError getKrbError()
    {
        return krbError;
    }


    @Override
    public short computeLength()
    {
        krbErrorLen = ( short ) krbError.computeLength();
        messageLength = ( short ) ( HEADER_LENGTH + krbErrorLen );
        
        return messageLength;
    }

    @Override
    public ByteBuffer encode( ByteBuffer buf ) throws EncoderException
    {
        buf.putShort( messageLength );

        buf.putShort( getVersionNumber() );

        buf.putShort( ( short )0 ); // zero means, what follows is an error

        krbError.encode( buf );
        
        return buf;
    }

    /**
     * Decodes a {@link ByteBuffer} into a {@link ChangePasswordError}.
     *
     * @param buf The buffer containing the ChangePasswordError to decode
     * @return The {@link ChangePasswordError}.
     * @throws ChangePasswordException If the decoding failed
     */
    public static ChangePasswordError decode( ByteBuffer buf ) throws ChangePasswordException
    {
        short messageLength = buf.getShort();

        short pvno = buf.getShort();

        // AP_REQ length will be 0 for error messages
        buf.getShort(); // authHeader length

        int errorLength = messageLength - HEADER_LENGTH;

        byte[] errorBytes = new byte[errorLength];

        buf.get( errorBytes );
        ByteBuffer errorBuffer = ByteBuffer.wrap( errorBytes );

        KrbErrorContainer container = new KrbErrorContainer( errorBuffer );
        Asn1Decoder decoder = new Asn1Decoder();

        try
        {
            decoder.decode( errorBuffer, container );
        }
        catch( DecoderException e )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_MALFORMED, e );
        }
        
        KrbError errorMessage = container.getKrbError();

        return new ChangePasswordError( pvno, errorMessage );
    }

    public ChangePasswdErrorType getResultCode()
    {
        ByteBuffer buf = ByteBuffer.wrap( krbError.getEData() );
        
        return ChangePasswdErrorType.getTypeByValue( buf.getShort() );
    }
    
    public String getResultString()
    {
        byte[] edata = krbError.getEData();

        // first two bytes contain the result code
        return Strings.utf8ToString( edata, 2, edata.length - 2 );
    }
}
