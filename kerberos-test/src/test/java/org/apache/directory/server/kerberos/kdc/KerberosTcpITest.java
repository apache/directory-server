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


import java.lang.reflect.Field;

import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test to obtain TGTs and Service Tickets from KDCs via TCP.
 * 
 * We use some internal knowledge of the Sun/Oracle implementation here to force
 * the usage of TCP: In sun.security.krb5.KrbKdcReq the static field udpPrefLimit 
 * is set to 1 which means that TCP is always used.
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
       @CreateTransport(protocol = "TCP")
   })
@ApplyLdifFiles("org/apache/directory/server/kerberos/kdc/KerberosIT.ldif")
public class KerberosTcpITest extends AbstractKerberosITest
{
    private static Integer udpPrefLimit;


    @BeforeClass
    public static void setUdpPrefLimit() throws Exception
    {
        // System.setProperty( "sun.security.krb5.debug", "true" );

        // Save current value of sun.security.krb5.KrbKdcReq.udpPrefLimit field.
        // Then set it to 1 to force TCP.
        udpPrefLimit = getUdpPrefLimit();
        setUdpPrefLimit( 1 );
    }


    @AfterClass
    public static void resetUdpPrefLimit() throws Exception
    {
        // Reset sun.security.krb5.KrbKdcReq.udpPrefLimit field
        setUdpPrefLimit( udpPrefLimit );
    }


    private static Integer getUdpPrefLimit() throws Exception
    {
        Field udpPrefLimitField = getUdpPrefLimitField();
        Object value = udpPrefLimitField.get( null );
        return ( Integer ) value;
    }


    private static void setUdpPrefLimit( int limit ) throws Exception
    {
        Field udpPrefLimitField = getUdpPrefLimitField();
        udpPrefLimitField.setAccessible( true );
        udpPrefLimitField.set( null, limit );
    }


    private static Field getUdpPrefLimitField() throws ClassNotFoundException, NoSuchFieldException
    {
        String clazz = "sun.security.krb5.KrbKdcReq";
        Class<?> krbKdcReqClass = Class.forName( clazz );
        Field udpPrefLimitField = krbKdcReqClass.getDeclaredField( "udpPrefLimit" );
        udpPrefLimitField.setAccessible( true );
        return udpPrefLimitField;
    }


    // TODO: fix failing tests
    // TODO: add tests for other encryption types
    // TODO: add tests for different options

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
