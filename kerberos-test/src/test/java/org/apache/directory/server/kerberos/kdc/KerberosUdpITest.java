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
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test to obtain TGTs and Service Tickets from KDCs via UDP.
 * 
 * We use some internal knowledge of the Sun/Oracle implementation here:
 * In sun.security.krb5.KrbKdcReq the field udpPrefLimit is set to -1 which means
 * that UDP is always used first. Only if the KDC replies with RB_ERR_RESPONSE_TOO_BIG
 * TCP is used.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "KerberosUdpIT-class",
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
       @CreateTransport(protocol = "UDP")
   })
@ApplyLdifFiles("org/apache/directory/server/kerberos/kdc/KerberosIT.ldif")
public class KerberosUdpITest extends AbstractKerberosITest
{

    // TODO: fix failing tests
    // TODO: add tests for other encryption types
    // TODO: add tests for different options

    @Test
    @Ignore("Fails")
    public void testObtainTickets_DES_CBC_CRC() throws Exception
    {
        setupEnv( EncryptionType.DES_CBC_CRC );
        testObtainTickets( EncryptionType.DES_CBC_CRC );
    }


    @Test
    @Ignore("Fails")
    public void testObtainTickets_DES_CBC_MD4() throws Exception
    {
        setupEnv( EncryptionType.DES_CBC_MD4 );
        testObtainTickets( EncryptionType.DES_CBC_MD4 );
    }


    @Test
    public void testObtainTickets_DES_CBC_MD5() throws Exception
    {
        setupEnv( EncryptionType.DES_CBC_MD5 );
        testObtainTickets( EncryptionType.DES_CBC_MD5 );
    }


    @Test
    public void testObtainTickets_DES3_CBC_SHA1_KD() throws Exception
    {
        setupEnv( EncryptionType.DES3_CBC_SHA1_KD );
        testObtainTickets( EncryptionType.DES3_CBC_SHA1_KD );
    }


    @Test
    @Ignore("Fails with KrbException: Integrity check on decrypted field failed (31) - Integrity check on decrypted field failed")
    public void testObtainTickets_RC4_HMAC() throws Exception
    {
        setupEnv( EncryptionType.RC4_HMAC );
        testObtainTickets( EncryptionType.RC4_HMAC );
    }


    @Test
    public void testObtainTickets_AES128() throws Exception
    {
        setupEnv( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        testObtainTickets( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
    }


    @Test
    public void testObtainTickets_AES256() throws Exception
    {
        setupEnv( EncryptionType.AES256_CTS_HMAC_SHA1_96 );
        testObtainTickets( EncryptionType.AES256_CTS_HMAC_SHA1_96 );
    }

}
