/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.jndi.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.naming.NamingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.ldap.server.configuration.ServerStartupConfiguration;
import org.apache.ldap.server.jndi.ssl.support.ServerX509TrustManager;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.filter.SSLFilter;

/**
 * Loads the certificate file for LDAPS support and creates the appropriate
 * MINA filter chain.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev: 368358 $, $Date: 2006-01-12 12:50:40Z $
 *
 */
public class LdapsInitializer
{
    public static IoFilterChainBuilder init( ServerStartupConfiguration cfg ) throws NamingException
    {
        // Load the certificate
        char[] certPasswdChars = cfg.getLdapsCertificatePassword().toCharArray();
        String storePath = cfg.getLdapsCertificateFile().getPath();
        
        KeyStore ks = null;
        try
        {
            ks = loadKeyStore( storePath, "PKCS12" );
        }
        catch( Exception e )
        {
            try
            {
                ks = loadKeyStore( storePath, "JKS" );
            }
            catch( Exception e2 )
            {
                throw ( NamingException ) new NamingException( "Failed to load a certificate: " + storePath ).initCause( e );
            }
        }

        SSLContext sslCtx;
        try
        {
            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
            kmf.init( ks, certPasswdChars );
    
            // Initialize the SSLContext to work with our key managers.
            sslCtx = SSLContext.getInstance( "TLS" );
            sslCtx.init( kmf.getKeyManagers(),
                    new TrustManager[] { new ServerX509TrustManager() }, new SecureRandom() );
        }
        catch( Exception e )
        {
            throw ( NamingException ) new NamingException( "Failed to create a SSL context." ).initCause( e );
        }
        
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        chain.addLast( "SSL", new SSLFilter( sslCtx ) );
        return chain;
    }
    
    private static KeyStore loadKeyStore( String storePath, String storeType ) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException
    {
        FileInputStream in = null;
        // Create keystore
        KeyStore ks = KeyStore.getInstance( storeType );
        try
        {
            in = new FileInputStream( storePath );
            ks.load( in, null );
            return ks;
        }
        finally
        {
            if( in != null )
            {
                try
                {
                    in.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }
    }
}
