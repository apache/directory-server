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
package org.apache.directory.server.kerberos.protocol;


import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.List;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.kerberos.components.PaEncTsEnc;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSessionConfig;


/**
 * Abstract base class for Authentication Service (AS) tests, with utility methods
 * for generating message components.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAuthenticationServiceTest
{
    protected CipherTextHandler lockBox;
    protected static final SecureRandom random = new SecureRandom();


    protected PaData[] getPreAuthEncryptedTimeStamp( KerberosPrincipal clientPrincipal, String passPhrase )
        throws Exception
    {
        KerberosTime timeStamp = new KerberosTime();

        return getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase, timeStamp );
    }


    protected PaData[] getPreAuthEncryptedTimeStamp( KerberosPrincipal clientPrincipal,
        String passPhrase, KerberosTime timeStamp ) throws Exception
    {
        PaData[] paData = new PaData[1];

        PaEncTsEnc encryptedTimeStamp = new PaEncTsEnc( timeStamp, 0 );

        EncryptionKey clientKey = getEncryptionKey( clientPrincipal, passPhrase );

        EncryptedData encryptedData = lockBox.seal( clientKey, encryptedTimeStamp, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );

        ByteBuffer buffer = ByteBuffer.allocate( encryptedData.computeLength() );
        byte[] encodedEncryptedData = encryptedData.encode( buffer ).array();

        PaData preAuth = new PaData();
        preAuth.setPaDataType( PaDataType.PA_ENC_TIMESTAMP );
        preAuth.setPaDataValue( encodedEncryptedData );

        paData[0] = preAuth;

        return paData;
    }


    protected PrincipalName getPrincipalName( String name )
    {
        PrincipalName principalName = new PrincipalName();
        principalName.addName( name );
        principalName.setNameType( PrincipalNameType.KRB_NT_PRINCIPAL );

        return principalName;
    }


    /**
     * Returns an encryption key derived from a principal name and passphrase.
     *
     * @param principal
     * @param passPhrase
     * @return The server's {@link EncryptionKey}.
     */
    protected EncryptionKey getEncryptionKey( KerberosPrincipal principal, String passPhrase )
    {
        KerberosKey kerberosKey = new KerberosKey( principal, passPhrase.toCharArray(), "AES128" );
        byte[] keyBytes = kerberosKey.getEncoded();
        EncryptionKey key = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, keyBytes );

        return key;
    }

    protected static class KrbDummySession extends DummySession
    {
        Object message;


        public KrbDummySession()
        {
            super();
        }


        public KrbDummySession( IoService service )
        {
            try
            {
                ( ( AbstractIoSession ) this ).setAttributeMap( service
                    .getSessionDataStructureFactory().getAttributeMap( this ) );
            }
            catch ( Exception e )
            {

            }
        }


        public WriteFuture write( Object message )
        {
            this.message = message;

            return null;
        }


        protected Object getMessage()
        {
            return message;
        }


        protected void updateTrafficMask()
        {
            // Do nothing.
        }


        public IoService getService()
        {
            return null;
        }


        public IoHandler getHandler()
        {
            return null;
        }


        public SocketAddress getRemoteAddress()
        {
            return new InetSocketAddress( 10088 );
        }


        public SocketAddress getLocalAddress()
        {
            return null;
        }


        public IoSessionConfig getConfig()
        {
            return null;
        }


        public int getScheduledWriteRequests()
        {
            return 0;
        }


        public SocketAddress getServiceAddress()
        {
            return null;
        }
    }
}
