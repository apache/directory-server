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
package org.apache.directory.server.kerberos.kdc;


import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests to obtain TGTs and Service Tickets from KDCs via TCP.
 * 
 * We use some internal knowledge of the Sun/Oracle implementation here to force
 * the usage of TCP and checksum:
 * <li>In sun.security.krb5.KrbKdcReq the static field udpPrefLimit is set to 1
 * which means that TCP is always used.
 * <li>In sun.security.krb5.Checksum the static field CKSUMTYPE_DEFAULT is set
 * to the appropriate checksum value.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory
Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "KerberosTcpIT-class",
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com")
    },
    additionalInterceptors =
        {
            KeyDerivationInterceptor.class
    })
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
@CreateKdcServer(
    transports =
        {
            @CreateTransport(protocol = "TCP", address = "127.0.0.1", port = 6086)
    })
@ApplyLdifFiles("org/apache/directory/server/kerberos/kdc/KerberosIT.ldif")
public class KerberosTcpITest extends AbstractKerberosITest
{

    // TODO: fix failing tests
    // TODO: add tests for other encryption types
    // TODO: add tests for different options

    @Test
    public void testObtainTickets_DES_CBC_MD5() throws Exception
    {
        if ( System.getProperty( "java.version" ).startsWith( "1.8" ) )
        {
            // Java 8 does not support anymore dec-cbc-md5
            return;
        }

        // TODO: rsa-md5-des
        // RFC3961, Section 6.2.1: des-cbc-md5 + rsa-md5-des
        ObtainTicketParameters parameters = new ObtainTicketParameters( TcpTransport.class,
            EncryptionType.DES_CBC_MD5, ChecksumType.RSA_MD5 );
        testObtainTickets( parameters );
    }


    @Test
    public void testObtainTickets_DES3_CBC_SHA1_KD() throws Exception
    {
        // RFC3961, Section 6.3: des3-cbc-hmac-sha1-kd + hmac-sha1-des3-kd
        ObtainTicketParameters parameters = new ObtainTicketParameters( TcpTransport.class,
            EncryptionType.DES3_CBC_SHA1_KD, ChecksumType.HMAC_SHA1_DES3_KD );
        testObtainTickets( parameters );
    }


    @Test
    @Ignore("Fails with KrbException: Integrity check on decrypted field failed (31) - Integrity check on decrypted field failed")
    public void testObtainTickets_RC4_HMAC() throws Exception
    {
        // TODO: RFC4757: rc4-hmac + hmac-md5
        ObtainTicketParameters parameters = new ObtainTicketParameters( TcpTransport.class,
            EncryptionType.RC4_HMAC, ChecksumType.HMAC_MD5 );
        testObtainTickets( parameters );
    }


    @Test
    public void testObtainTickets_AES128() throws Exception
    {
        // RFC3962, Section 7: aes128-cts-hmac-sha1-96 + hmac-sha1-96-aes128
        ObtainTicketParameters parameters = new ObtainTicketParameters( TcpTransport.class,
            EncryptionType.AES128_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES128 );
        testObtainTickets( parameters );
    }


    @Test
    @Ignore("Fails with javax.security.auth.login.LoginException: No supported encryption types listed in default_tkt_enctypes")
    public void testObtainTickets_AES256() throws Exception
    {
        // RFC3962, Section 7: aes256-cts-hmac-sha1-96 + hmac-sha1-96-aes256
        ObtainTicketParameters parameters = new ObtainTicketParameters( TcpTransport.class,
            EncryptionType.AES256_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES256 );
        testObtainTickets( parameters );
    }

}
