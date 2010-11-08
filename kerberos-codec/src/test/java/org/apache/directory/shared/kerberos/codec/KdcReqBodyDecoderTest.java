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
package org.apache.directory.shared.kerberos.codec;


import static org.junit.Assert.assertEquals;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.HostAddrType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.Test;
import org.junit.runner.RunWith;

import sun.security.krb5.internal.KerberosTime;


/**
 * Test the decoder for a KdcReqBody
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class KdcReqBodyDecoderTest
{
    /**
     * Test the decoding of a KdcReqBody message
     */
    @Test
    public void testEncodeTicket() throws Exception
    {
        KdcReqBody body = new KdcReqBody();
        
        body.setKdcOptions( new KdcOptions( new byte[]{0x01, 0x02, 0x03, 0x04} ) );
        body.setCName( new PrincipalName( "client", PrincipalNameType.KRB_NT_ENTERPRISE ) );
        body.setRealm( "EXAMPLE.COM" );
        body.setSName( new PrincipalName( "server", PrincipalNameType.KRB_NT_ENTERPRISE ) );
        body.setFrom( new KerberosTime( System.currentTimeMillis() ) );
        body.setTill( new KerberosTime( System.currentTimeMillis() ) );
        body.setRtime( new KerberosTime( System.currentTimeMillis() ) );
        body.setNonce( 12345 );
        
        body.addEType( EncryptionType.AES256_CTS_HMAC_SHA1_96 );
        body.addEType( EncryptionType.DES3_CBC_MD5 );
        body.addEType( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        
        HostAddresses addresses = new HostAddresses();
        addresses.addHostAddress( new HostAddress( HostAddrType.ADDRTYPE_INET, "192.168.0.1".getBytes() ) );
        addresses.addHostAddress( new HostAddress( HostAddrType.ADDRTYPE_INET, "192.168.0.2".getBytes() ) );
        body.setAddresses( addresses );

        EncryptedData encAuthorizationData = new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, "abcdef".getBytes() );
        body.setEncAuthorizationData( encAuthorizationData );
        
        Ticket ticket1 = new Ticket();
        ticket1.setTktVno( 5 );
        ticket1.setRealm( "EXAMPLE.COM" );
        ticket1.setSName( new PrincipalName( "client", PrincipalNameType.KRB_NT_PRINCIPAL ) );
        ticket1.setEncPart( new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, "abcdef".getBytes() ) );
        
        body.addAdditionalTicket( ticket1 );

        Ticket ticket2 = new Ticket();
        ticket2.setTktVno( 5 );
        ticket2.setRealm( "EXAMPLE.COM" );
        ticket2.setSName( new PrincipalName( "server", PrincipalNameType.KRB_NT_PRINCIPAL ) );
        ticket2.setEncPart( new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, "abcdef".getBytes() ) );
        
        body.addAdditionalTicket( ticket2 );
        
        // Check the encoding
        int length = body.computeLength();

        // Check the length
        assertEquals( 0x15E, length );
    }
}
