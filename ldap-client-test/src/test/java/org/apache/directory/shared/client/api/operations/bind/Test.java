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

package org.apache.directory.shared.client.api.operations.bind;


/**
 * TODO Test.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.login.Configuration;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.SaslGssApiRequest;


public class Test
{
    public String bindusername = "ETHIXNet01";
    public String bindpassword = "get@get1";
    public LdapNetworkConnection ldapNetworkConnection;
    public boolean connectionStatus = true;
    public LdapConnectionConfig config;
    public boolean kerberos = true;
    public SaslGssApiRequest saslGssApiRequest;


    public static void main( String[] args ) throws LdapException, CursorException,
        NoSuchAlgorithmException, CertificateException, KeyStoreException
    {
        String keystore = "C:\\bea\\jrockit_160_05\\jre\\lib\\security\\ETHIXNetAdmin.pfx";
        System.setProperty( "javax.net.ssl.keyStore", keystore );
        System.setProperty( "javax.net.ssl.keyStorePassword", "P@ssw0rd" );
        System.setProperty( "javax.net.ssl.keyStoreType", "PKCS12" );
        String username = "RCW0000016";
        String password = "P@ssw0rd";
        Test ldapconn = new Test();
        ldapconn.connectAndBind();
        ldapconn.closeConnection();
    }


    public void connectAndBind()
    {
        config = new LdapConnectionConfig();
        config.setLdapHost( "localhost" );
        config.setLdapPort( 10389 );
        config.setName( bindusername );
        config.setCredentials( bindpassword );

        TrustManagerFactory tmf = null;
        
        try
        {
            tmf = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            tmf.init( ( KeyStore ) null );
        }

        catch ( NoSuchAlgorithmException e )
        {
            e.printStackTrace();
        }
        catch ( KeyStoreException e )
        {
            e.printStackTrace();
        }

        config.setTrustManagers( tmf.getTrustManagers() );
        config.setUseTls( true );
        config.setSslProtocol( "TLSv1" );
        ldapNetworkConnection = new LdapNetworkConnection( config );

        try
        {
            connectionStatus = ldapNetworkConnection.connect();
            System.out.println( ( connectionStatus ) ? "Connection Established" : "Connection ERROR" );
        }

        catch ( LdapException e )
        {
            e.printStackTrace();
        }

        if ( connectionStatus && kerberos )
        {
            saslGssApiRequest = new SaslGssApiRequest();
            System.setProperty( "java.security.auth.login.config", "bcsLogin.conf" );
            saslGssApiRequest.setLoginModuleConfiguration( Configuration.getConfiguration() );
            saslGssApiRequest.setLoginContextName( "org.apache.directory.ldap.client.api.SaslGssApiRequest" );

            saslGssApiRequest.setKrb5ConfFilePath( "/krb5.ini" );
            saslGssApiRequest.setMutualAuthentication( false );

            saslGssApiRequest.setUsername( bindusername );
            saslGssApiRequest.setCredentials( bindpassword );

            BindResponse br;
            
            try
            {
                br = ldapNetworkConnection.bind( saslGssApiRequest );
                ldapNetworkConnection.startTls();
                System.out.println( br.getLdapResult().getResultCode().SUCCESS );
            }
            catch ( LdapException e )
            {
                e.printStackTrace();
            }
        }
    }


    public void closeConnection()
    {
        try
        {
            ldapNetworkConnection.unBind();
            ldapNetworkConnection.close();
        }
        catch ( LdapException e )
        {
            e.printStackTrace();
        }
    }
}
