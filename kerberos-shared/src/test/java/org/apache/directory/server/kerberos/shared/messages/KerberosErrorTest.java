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

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;

/**
 * Test the KRB-ERROR encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosErrorTest extends TestCase
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
    
    public void testKrbErrorBase() throws Exception
    {
        KerberosError kem = new KerberosError();
        
        KerberosTime serverTime = new KerberosTime( date );
        kem.setServerTime( serverTime );
        kem.setServerMicroseconds( 128 );
        
        kem.setErrorCode( KerberosErrorType.KDC_ERR_C_PRINCIPAL_UNKNOWN );
        
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "admin@APACHE.ORG" );
        kem.setServerPrincipal( serverPrincipal );
        kem.setServerRealm( serverPrincipal.getRealm() );
        
        ByteBuffer encoded = ByteBuffer.allocate( kem.computeLength() );
        
        kem.encode( encoded );
        
        byte[] expectedResult = new byte[]
            {
              0x7E, 0x4C,
                0x30, 0x4A,
                  (byte)0xA0, 0x03,
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x03,
                    0x02, 0x01, 0x1E,
                  (byte)0xA4, 0x11,
                    0x18, 0x0F,
                      '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                  (byte)0xA5, 0x04,
                    0x02, 0x02, 0x00, (byte)0x80,
                  (byte)0xA6, 0x03,
                    0x02, 0x01, 0x06,
                  (byte)0xA9, 0x0C,
                    0x1B, 0x0A,
                      'A', 'P', 'A', 'C', 'H', 'E', '.', 'O', 'R', 'G',
                  (byte)0xAA, 0x12,
                    0x30, 0x10,
                      (byte)0xA0, 0x03,
                        0x02, 0x01, 0x01,
                      (byte)0xA1, 0x09,
                        0x30, 0x07,
                          0x1B, 0x05,
                            'a', 'd', 'm', 'i', 'n'
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }

    public void testKrbErrorWithClientTime() throws Exception
    {
        KerberosError kem = new KerberosError();
        
        KerberosTime serverTime = new KerberosTime( date );
        kem.setServerTime( serverTime );
        kem.setServerMicroseconds( 128 );
        
        kem.setClientTime( serverTime );
        
        kem.setErrorCode( KerberosErrorType.KDC_ERR_C_PRINCIPAL_UNKNOWN );
        
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "admin@APACHE.ORG" );
        kem.setServerPrincipal( serverPrincipal );
        kem.setServerRealm( serverPrincipal.getRealm() );
        
        ByteBuffer encoded = ByteBuffer.allocate( kem.computeLength() );
        
        kem.encode( encoded );
        
        byte[] expectedResult = new byte[]
            {
              0x7E, 0x5F,
                0x30, 0x5D,
                  (byte)0xA0, 0x03,
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x03,
                    0x02, 0x01, 0x1E,
                  (byte)0xA2, 0x11,
                    0x18, 0x0F,
                      '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                  (byte)0xA4, 0x11,
                    0x18, 0x0F,
                      '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                  (byte)0xA5, 0x04,
                    0x02, 0x02, 0x00, (byte)0x80,
                  (byte)0xA6, 0x03,
                    0x02, 0x01, 0x06,
                  (byte)0xA9, 0x0C,
                    0x1B, 0x0A,
                      'A', 'P', 'A', 'C', 'H', 'E', '.', 'O', 'R', 'G',
                  (byte)0xAA, 0x12,
                    0x30, 0x10,
                      (byte)0xA0, 0x03,
                        0x02, 0x01, 0x01,
                      (byte)0xA1, 0x09,
                        0x30, 0x07,
                          0x1B, 0x05,
                            'a', 'd', 'm', 'i', 'n'
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }

    public void testKrbErrorWithAll() throws Exception
    {
        KerberosError kem = new KerberosError();
        
        KerberosTime serverTime = new KerberosTime( date );
        kem.setServerTime( serverTime );
        kem.setServerMicroseconds( 128 );
        
        kem.setClientTime( serverTime );
        kem.setClientMicroSecond( 128 );
        
        kem.setErrorCode( KerberosErrorType.KDC_ERR_C_PRINCIPAL_UNKNOWN );
        
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "admin@APACHE.ORG" );
        kem.setServerPrincipal( serverPrincipal );
        kem.setServerRealm( serverPrincipal.getRealm() );
        
        kem.setClientPrincipal( serverPrincipal );
        kem.setClientRealm( serverPrincipal.getRealm() );
        
        kem.setExplanatoryText( "test" );
        kem.setExplanatoryData( new byte[]{0x00, 0x01, 0x02, 0x03} ); 

        ByteBuffer encoded = ByteBuffer.allocate( kem.computeLength() );
        
        kem.encode( encoded );
        
        byte[] expectedResult = new byte[]
            {
              0x7E, (byte)0x81, (byte)0x98,
                0x30, (byte)0x81, (byte)0x95,
                  (byte)0xA0, 0x03,
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x03,
                    0x02, 0x01, 0x1E,
                  (byte)0xA2, 0x11,
                    0x18, 0x0F,
                      '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                  (byte)0xA3, 0x04,
                    0x02, 0x02, 0x00, (byte)0x80,
                  (byte)0xA4, 0x11,
                    0x18, 0x0F,
                      '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                  (byte)0xA5, 0x04,
                    0x02, 0x02, 0x00, (byte)0x80,
                  (byte)0xA6, 0x03,
                    0x02, 0x01, 0x06,
                  (byte)0xA7, 0x0C,
                    0x1B, 0x0A,
                      'A', 'P', 'A', 'C', 'H', 'E', '.', 'O', 'R', 'G',
                  (byte)0xA8, 0x12,
                    0x30, 0x10,
                      (byte)0xA0, 0x03,
                        0x02, 0x01, 0x01,
                      (byte)0xA1, 0x09,
                        0x30, 0x07,
                          0x1B, 0x05,
                            'a', 'd', 'm', 'i', 'n',
                  (byte)0xA9, 0x0C,
                    0x1B, 0x0A,
                      'A', 'P', 'A', 'C', 'H', 'E', '.', 'O', 'R', 'G',
                  (byte)0xAA, 0x12,
                    0x30, 0x10,
                      (byte)0xA0, 0x03,
                        0x02, 0x01, 0x01,
                      (byte)0xA1, 0x09,
                        0x30, 0x07,
                          0x1B, 0x05,
                            'a', 'd', 'm', 'i', 'n',
                  (byte)0xAB, 0x06,
                    0x1B, 0x04,
                      't', 'e', 's', 't',
                  (byte)0xAC, 0x06,
                    0x04, 0x04,
                      0x00, 0x01, 0x02, 0x03
            };

        assertEquals( StringTools.dumpBytes( expectedResult ), StringTools.dumpBytes( encoded.array() ) );
        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }
}
