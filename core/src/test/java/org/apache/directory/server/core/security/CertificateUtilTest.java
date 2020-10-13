/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.junit.Test;

/**
 * Test for the CertificateUtil class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("restriction")
public class CertificateUtilTest
{

    @Test
    public void testSelfSignedCertificateCreation() throws IOException, GeneralSecurityException
    {
        // Generate the subject's name
        X500Principal owner = new X500Principal( "CN=apacheds,OU=directory,O=apache,C=US" );
        
        
        // generate the asymetric keys
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "EC" );
        keyPairGenerator.initialize( 256 );
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
            

        X509Certificate certificate = CertificateUtil.generateSelfSignedCertificate( owner, keyPair, 3650, "SHA256WithECDSA" );
        System.out.println( certificate );
    }
}
