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

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.io.encoder.AuthenticatorEncoder;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationDataEntry;
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.types.AuthorizationType;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;

/**
 * Test the Authenticator encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 542147 $, $Date: 2007-05-28 10:14:21 +0200 (Mon, 28 May 2007) $
 */
public class AuthenticatorTest extends TestCase
{
    private static Date date = null;
    
    static
    {
        try
        {
            date = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" ).parse( "20070717114503Z" );
        }
        catch ( ParseException pe )
        {
            // Do nothing
        }
    }

    public void testAuthenticator() throws Exception
    {
        Authenticator authenticator = new Authenticator();
        
        // authenticator-vno
        authenticator.setVersionNumber( 5 );
        
        // crealm
        authenticator.setClientRealm( "EXAMPLE.COM" );

        // cname
        //PrincipalName cname = new PrincipalName( "test@APACHE.ORG", PrincipalNameType.KRB_NT_PRINCIPAL );
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "test@APACHE.ORG" );
        authenticator.setClientPrincipal( clientPrincipal );

        // cksum
        Checksum chk = new Checksum( ChecksumType.CRC32, new byte[] { 0x01, 0x02, 0x03 } );
        authenticator.setChecksum( chk );
        
        // cusec
        authenticator.setClientMicroSecond( 128 );
        
        // ctime
        KerberosTime cTime = new KerberosTime( date );
        authenticator.setClientTime( cTime );
        
        // subkey
        EncryptionKey subkey = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, 
            new byte[] { 0x01, 0x02, 0x03 } );
        authenticator.setSubSessionKey( subkey );
        
        // seqNumber
        authenticator.setSequenceNumber( 0x1010 );
        
        // authorization-data
        AuthorizationData ad = new AuthorizationData();
        ad.add( new AuthorizationDataEntry( AuthorizationType.AD_KDC_ISSUED, new byte[]
            { 0x01, 0x02, 0x03, 0x04 } ) );
        authenticator.setAuthorizationData( ad );
        
        byte[] encodedAuthenticator = new AuthenticatorEncoder().encode( authenticator );
        
        ByteBuffer encoded = authenticator.encode( null );
        
        byte[] expectedResult = new byte[]
            {
              0x62, 0x7B,
                0x30, 0x79,
                  (byte)0xA0, 0x03,
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x0D,
                    0x1B, 0x0B, 
                      'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                  (byte)0xA2, 0x11,
                    0x30, 0x0F, 
                      (byte) 0xA0, 0x03, 
                        0x02, 0x01, 0x01, 
                      (byte) 0xA1, 0x08, 
                        0x30, 0x06, 
                          0x1B, 0x04, 
                            't', 'e', 's', 't',
                  (byte)0xA3, 0x0E,
                    0x30, 0x0c, 
                      (byte)0xA0, 0x03, 
                        0x02, 0x01, 0x01, 
                      (byte)0xA1, 0x05, 
                        0x04, 0x03, 
                          0x01, 0x02, 0x03,
                  (byte)0xA4, 0x04,
                    0x02, 0x02, 0x00, (byte)0x80,
                  (byte)0xA5, 0x11,
                    0x18, 0x0F,
                      '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                  (byte)0xA6, 0x0E,
                    0x30, 0x0c, 
                      (byte)0xA0, 0x03, 
                        0x02, 0x01, 0x11, 
                      (byte)0xA1, 0x05, 
                        0x04, 0x03, 0x01, 0x02, 0x03,
                  (byte)0xA7, 0x04,
                    0x02, 0x02, 0x10, 0x10,
                  (byte)0xA8, 0x11, 
                    0x30, 0x0F, 
                      0x30, 0x0d, 
                        (byte)0xA0, 0x03, 
                          0x02, 0x01, 0x04, 
                        (byte)0xA1, 0x06, 
                          0x04, 0x04, 0x01, 0x02, 0x03, 0x04 
            };

        assertEquals( StringTools.dumpBytes( expectedResult ) , StringTools.dumpBytes( encoded.array() ) );
        assertEquals( StringTools.dumpBytes( encodedAuthenticator ) , StringTools.dumpBytes( encoded.array() ) );
    }
}
