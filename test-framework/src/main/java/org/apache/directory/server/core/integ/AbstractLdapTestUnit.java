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

import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.security.CertificateUtil;
import org.apache.directory.server.ldap.LdapServer;


/**
 * An abstract class created to hold common elements.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractLdapTestUnit
{
    /** The class DirectoryService instance */
    public static DirectoryService classDirectoryService;

    /** The test DirectoryService instance */
    public static DirectoryService methodDirectoryService;

    /** The current DirectoryService instance */
    public static DirectoryService directoryService;

    /** The class LdapServer instance */
    public static LdapServer classLdapServer;

    /** The test LdapServer instance */
    public static LdapServer methodLdapServer;

    /** The current LdapServer instance */
    public static LdapServer ldapServer;

    /** The Ldap connection template */
    public static LdapConnectionTemplate ldapConnectionTemplate;
    
    /** The current revision */
    public static long revision = 0L;

    public DirectoryService getService()
    {
        return directoryService;
    }


    public void setService( DirectoryService directoryService )
    {
        AbstractLdapTestUnit.directoryService = directoryService;
    }


    public LdapServer getLdapServer()
    {
        return ldapServer;
    }


    public void setLdapServer( LdapServer ldapServer )
    {
        AbstractLdapTestUnit.ldapServer = ldapServer;
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
