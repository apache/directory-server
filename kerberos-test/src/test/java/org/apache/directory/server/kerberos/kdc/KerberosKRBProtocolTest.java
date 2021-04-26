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
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Some tests for the new "KRB" protocol introduced as part of DIRSERVER-2031:
 * 
 * https://issues.apache.org/jira/browse/DIRSERVER-2031
 */
@ExtendWith(ApacheDSTestExtension.class)
@CreateDS(name = "KerberosKRBProtocolTest-class",
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
            @CreateTransport(protocol = "KRB")
    })
@ApplyLdifFiles("org/apache/directory/server/kerberos/kdc/KerberosIT.ldif")
public class KerberosKRBProtocolTest extends AbstractKerberosITest
{

    @Test
    public void testObtainTickets_AES128_TCP() throws Exception
    {
        // RFC3962, Section 7: aes128-cts-hmac-sha1-96 + hmac-sha1-96-aes128
        ObtainTicketParameters parameters = new ObtainTicketParameters( TcpTransport.class,
            EncryptionType.AES128_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES128 );
        testObtainTickets( parameters );
    }
    
    @Test
    public void testObtainTickets_AES128_UDP() throws Exception
    {
        // RFC3962, Section 7: aes128-cts-hmac-sha1-96 + hmac-sha1-96-aes128
        ObtainTicketParameters parameters = new ObtainTicketParameters( UdpTransport.class,
            EncryptionType.AES128_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES128 );
        testObtainTickets( parameters );
    }

}
