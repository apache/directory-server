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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosRequestBody;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.flags.KdcOption;
import org.apache.directory.server.kerberos.shared.messages.value.flags.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.types.HostAddressType;
import org.apache.directory.server.kerberos.shared.messages.value.types.PreAuthenticationDataType;
import org.apache.directory.server.kerberos.shared.messages.value.types.PrincipalNameType;
import org.apache.directory.server.kerberos.shared.store.TicketFactory;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;

/**
 * Test the TGS-REQ encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TicketGrantingServerRequestTest extends TestCase
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

    private KerberosRequestBody getReqBody() throws ParseException, KerberosException
    {
        KerberosRequestBody krb = new KerberosRequestBody();
        
        // KdcOptions
        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.setFlag( KdcOption.FORWARDABLE );
        kdcOptions.setFlag( KdcOption.PROXIABLE );
        kdcOptions.setFlag( KdcOption.POSTDATED );
        kdcOptions.setFlag( KdcOption.VALIDATE );
        
        krb.setKdcOptions( kdcOptions );
        
        // cName
        PrincipalName cname = new PrincipalName( "test@APACHE.ORG", PrincipalNameType.KRB_NT_PRINCIPAL );
        krb.setClientPrincipalName( cname );

        // Realm
        krb.setRealm( "APACHE.ORG" );
        
        // sName
        PrincipalName sname = new PrincipalName( "server@APACHE.ORG", PrincipalNameType.KRB_NT_PRINCIPAL );
        krb.setServerPrincipalName( sname );
        
        // from, till and renew
        KerberosTime kerberosTime = new KerberosTime( date );
        krb.setFrom( kerberosTime );
        krb.setTill( kerberosTime );
        krb.setRenewtime( kerberosTime );
        
        // nonce
        krb.setNonce( 1000 );
        
        // EncryptionTypes
        krb.addEncryptionType( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        krb.addEncryptionType( EncryptionType.DES3_CBC_MD5 );
        
        // addresses
        HostAddress[] ha = new HostAddress[]
            { 
                new HostAddress( HostAddressType.ADDRTYPE_INET, new byte[] { 0x01, 0x02, 0x03, 0x04 } ) 
            };

        HostAddresses addresses = new HostAddresses( ha );
        krb.setAddresses( addresses );
        
        // encAuthorizationData
        EncryptedData ed = new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, 1, new byte[]
            { 0x01, 0x02, 0x03, 0x04 } );
        krb.setEncAuthorizationData( ed );
        
        // additionalTickets
        TicketFactory ticketFactory = new TicketFactory();

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "kadmin/changepw@EXAMPLE.COM" );
        String serverPassword = "s3crEt";

        EncryptionKey serverKey = ticketFactory.getServerKey( serverPrincipal, serverPassword );

        Ticket serviceTicket = ticketFactory.getTicket( clientPrincipal, serverPrincipal, serverKey );
        
        krb.addAdditionalTicket( serviceTicket );
        
        return krb;
    }
    
    public void testTicketGrantingServerRequestBase() throws Exception
    {
        PreAuthenticationData pad = new PreAuthenticationData( 
            PreAuthenticationDataType.PA_ASF3_SALT, 
            new byte[] { 0x01, 0x02, 0x03 } );
        
        List<PreAuthenticationData> paData = new ArrayList<PreAuthenticationData>();
        paData.add(  pad  );
        
        TicketGrantingServerRequest tgsr = new TicketGrantingServerRequest( paData, getReqBody() );
        
        ByteBuffer encoded = tgsr.encode( null );
        
        byte[] expectedResult = new byte[]
            {
              0x6C, (byte)0x82, 0x01, (byte)0xD1,
                0x30, (byte)0x82, 0x01, (byte)0xCD,
                  (byte)0xA1, 0x03,
                    0x02, 0x01, 0x05,
                  (byte)0xA2, 0x03,
                    0x02, 0x01, 0x0C,
                  (byte)0xA3, 0x10,
                    0x30, 0x0E,
                      0x30, 0x0C, 
                        (byte)0xA1, 0x03, 
                          0x02, 0x01, 0x0A, 
                        (byte)0xA2, 0x05, 
                          0x04, 0x03, 
                            0x01, 0x02, 0x03, 
                  (byte)0xA4, (byte)0x82, 0x01, (byte)0xAD,
                    0x30, (byte)0x82, 0x01, (byte)0xA9, 
                      (byte)0xA0, 0x07, 
                        0x03, 0x05, 
                          0x00, (byte)0x52, 0x00, 0x00, 0x01,
                      (byte)0xA1, 0x11,
                        0x30, 0x0F, 
                          (byte) 0xA0, 0x03, 
                            0x02, 0x01, 0x01, 
                          (byte) 0xA1, 0x08, 
                            0x30, 0x06, 
                              0x1B, 0x04, 
                                't', 'e', 's', 't',
                      (byte)0xA2,0x0C,
                        0x1B, 0x0A,
                          'A', 'P', 'A', 'C', 'H', 'E', '.', 'O', 'R', 'G',
                      (byte)0xA3, 0x13,
                        0x30, 0x11, 
                          (byte) 0xA0, 0x03, 
                            0x02, 0x01, 0x01, 
                          (byte) 0xA1, 0x0A, 
                            0x30, 0x08, 
                              0x1B, 0x06, 
                                's', 'e', 'r', 'v', 'e', 'r',
                      (byte)0xA4, 0x11,
                        0x18, 0x0F,
                          '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                      (byte)0xA5, 0x11,
                        0x18, 0x0F,
                          '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                      (byte)0xA6, 0x11,
                        0x18, 0x0F,
                          '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
                      (byte)0xA7, 0x04,
                        0x02, (byte)0x02, 0x03, (byte)0xE8,
                      (byte)0xA8, 0x08,
                        0x30, 0x06,
                          0x02, 0x01, 0x11,
                          0x02, 0x01, 0x05,
                      (byte)0xA9, 0x11,
                        0x30, 0x0F, 
                          0x30, 0x0d, 
                            (byte)0xA0, 0x03, 
                              0x02, 0x01, 0x02, 
                            (byte)0xA1, 0x06, 
                              0x04, 0x04, 
                                0x01, 0x02, 0x03, 0x04,
                      (byte)0xAA, 0x14,
                        0x30, 0x12, 
                          (byte)0xA0, 0x03, 
                            0x02, 0x01, 0x11, 
                          (byte)0xA1, 0x03, 
                            0x02, 0x01, 0x01, 
                          (byte)0xA2, 0x06, 
                            0x04, 0x04, 0x01, 0x02, 0x03, 0x04,
                      (byte)0xAB, (byte)0x81, (byte)0xF5,
                        0x30, (byte)0x81, (byte)0xF2,
                          0x61, (byte)0x81, (byte)0xEF,
                            0x30,  (byte)0x81, (byte)0xEC, 
                              (byte)0xA0, 0x03,
                                0x02, 0x01, 0x05,
                              (byte)0xA1, 0x0D,
                                0x1B, 0x0B, 
                                  'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                              (byte)0xA2, 0x1D,
                                0x30, 0x1B,
                                  (byte)0xA0, 0x03, 
                                    0x02, 0x01, 0x01, 
                                  (byte)0xA1, 0x14, 
                                    0x30, 0x12, 
                                      0x1B, 0x06, 
                                        'k', 'a', 'd', 'm', 'i', 'n',
                                      0x1B, 0x08,
                                        'c', 'h', 'a', 'n', 'g', 'e', 'p', 'w',
                              (byte)0xA3, (byte)0x81, (byte)0xB6, 
                                0x30, (byte)0x81, (byte)0xB3,
                                  (byte)0xA0, 0x03,
                                    0x02, 0x01, 0x03,
                                  (byte)0xA2, (byte)0x81, (byte)0xAB,
                                    0x04, (byte)0x81, (byte)0xA8
                      
            };

        // We will just compared the first bytes (everyting before the encrypted data)
        String expectedResultString = StringTools.dumpBytes( expectedResult );
        String resultString = StringTools.dumpBytes( encoded.array() ).substring( 0,  expectedResultString.length() );
        
        assertEquals( expectedResultString, resultString );
    }
}
