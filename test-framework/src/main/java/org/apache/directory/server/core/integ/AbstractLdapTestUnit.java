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
package org.apache.directory.server.core.integ;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.security.CertificateUtil;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;


/**
 * An abstract class created to hold common elements.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractLdapTestUnit
{
    /** The used DirectoryService instance */
    public static DirectoryService service;

    /** The test DirectoryService instance */
    public static DirectoryService methodService;

    /** The used LdapServer instance */
    public static LdapServer ldapServer;

    /** The used KdcServer instance */
    public static KdcServer kdcServer;
    
    /** The current revision */
    public static long revision = 0L;

    public static DirectoryService getService()
    {
        return service;
    }


    public static void setService( DirectoryService service )
    {
        AbstractLdapTestUnit.service = service;
    }


    public static LdapServer getLdapServer()
    {
        return ldapServer;
    }


    public static void setLdapServer( LdapServer ldapServer )
    {
        AbstractLdapTestUnit.ldapServer = ldapServer;
    }


    public static KdcServer getKdcServer()
    {
        return kdcServer;
    }


    public static void setKdcServer( KdcServer kdcServer )
    {
        AbstractLdapTestUnit.kdcServer = kdcServer;
    }
    
    
    public void changeCertificate( String keyStoreFile, String password, String issuerDn, String subjectDn, int days, String algorithm ) 
        throws IOException, GeneralSecurityException
    {
        KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        char[] keyStorePassword = password.toCharArray();
        
        try ( InputStream keyStoreData = new FileInputStream( keyStoreFile ) )
        {
            keyStore.load( null, keyStorePassword );
        }
        
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "EC" );
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        // Generate the subject's name
        X500Principal subject = new X500Principal( "CN=" + subjectDn + ",OU=directory,O=apache,C=US" );
        
        // Generate the issuer's name
        X500Principal issuer = new X500Principal( "CN=" + issuerDn + ",OU=directory,O=apache,C=US" );

        // Create the self-signed certificate
        X509Certificate certificate = CertificateUtil.generateCertificate( subject, issuer, keyPair, days, algorithm );
        
        keyStore.setKeyEntry( "apachedsKey", keyPair.getPrivate(), keyStorePassword, new X509Certificate[] { certificate } );
        
        try ( FileOutputStream out = new FileOutputStream( keyStoreFile ) )
        {
            keyStore.store( out, keyStorePassword );
        }
    }
}
