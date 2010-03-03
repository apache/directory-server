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
package org.apache.directory.server.ssl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test case to verify proper operation of confidentiality requirements as 
 * specified in https://issues.apache.org/jira/browse/DIRSERVER-1189.  
 * 
 * Starts up the server binds via SUN JNDI provider to perform various 
 * operations on entries which should be rejected when a TLS secured 
 * connection is not established.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 639006 $
 */
@RunWith ( FrameworkRunner.class ) 
@CreateDS( allowAnonAccess=true, name="StartTlsIT-class")
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" ),
        @CreateTransport( protocol = "LDAPS" )
    },
    extendedOpHandlers={ StartTlsHandler.class }
    )
public class StartTlsIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( StartTlsIT.class );
    private static final String[] CERT_IDS = new String[] { "userCertificate" };
    private static final int CONNECT_ITERATIONS = 10;
    private static final boolean VERBOSE = false;
    private File ksFile;

    
    boolean oldConfidentialityRequiredValue;
    
    
    /**
     * Sets up the key store and installs the self signed certificate for the 
     * server (created on first startup) which is to be used by the StartTLS 
     * JDNDI client that will connect.  The key store is created from scratch
     * programmatically and whipped on each run.  The certificate is acquired 
     * by pulling down the bytes for administrator's userCertificate from 
     * uid=admin,ou=system.  We use sysRoot direct context instead of one over
     * the wire since the server is configured to prevent connections without
     * TLS secured connections.
     */
    @Before
    public void installKeyStoreWithCertificate() throws Exception
    {
        if ( ksFile != null && ksFile.exists() )
        {
            ksFile.delete();
        }
        
        ksFile = File.createTempFile( "testStore", "ks" );
        CoreSession session = ldapServer.getDirectoryService().getAdminSession();
        ClonedServerEntry entry = session.lookup( new DN( "uid=admin,ou=system" ), CERT_IDS );
        byte[] userCertificate = entry.get( CERT_IDS[0] ).getBytes();
        assertNotNull( userCertificate );

        ByteArrayInputStream in = new ByteArrayInputStream( userCertificate );
        CertificateFactory factory = CertificateFactory.getInstance( "X.509" );
        Certificate cert = factory.generateCertificate( in );
        KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
        ks.load( null, null );
        ks.setCertificateEntry( "apacheds", cert );
        ks.store( new FileOutputStream( ksFile ), "changeit".toCharArray() );
        LOG.debug( "Keystore file installed: {}", ksFile.getAbsolutePath() );
        
        oldConfidentialityRequiredValue = ldapServer.isConfidentialityRequired();
    }
    
    
    /**
     * Just deletes the generated key store file.
     */
    @After
    public void deleteKeyStore() throws Exception
    {
        if ( ksFile != null && ksFile.exists() )
        {
            ksFile.delete();
        }
        
        LOG.debug( "Keystore file deleted: {}", ksFile.getAbsolutePath() );
        ldapServer.setConfidentialityRequired( oldConfidentialityRequiredValue );
    }
    

    private void search( int ii, LdapContext securedContext ) throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        
        if ( VERBOSE )
        {
            System.out.println( "Searching on " + ii + "-th iteration:" );
        }
        
        List<String> results = new ArrayList<String>();
        NamingEnumeration<SearchResult> ne = securedContext.search( "ou=system", "(objectClass=*)", controls );
        while ( ne.hasMore() )
        {
            String dn = ne.next().getNameInNamespace();
            results.add( dn );
            
            if ( VERBOSE )
            {
                System.out.println( "\tSearch Result = " + dn );
            }
        }
        ne.close();
        
        assertEquals( "ou=system", results.get( 0 ) );
        assertEquals( "uid=admin,ou=system", results.get( 1 ) );
        assertEquals( "ou=users,ou=system", results.get( 2 ) );
        assertEquals( "ou=groups,ou=system", results.get( 3 ) );
        assertEquals( "cn=Administrators,ou=groups,ou=system", results.get( 4 ) );
        assertEquals( "ou=configuration,ou=system", results.get( 5 ) );
        assertEquals( "ou=partitions,ou=configuration,ou=system", results.get( 6 ) );
        assertEquals( "ou=services,ou=configuration,ou=system", results.get( 7 ) );
        assertEquals( "ou=interceptors,ou=configuration,ou=system", results.get( 8 ) );
        assertEquals( "prefNodeName=sysPrefRoot,ou=system", results.get( 9 ) );
    }
    
    
    /**
     * Tests StartTLS by creating a JNDI connection using the generated key 
     * store with the installed self signed certificate.  It then searches 
     * the server and verifies the presence of the expected entries and closes
     * the connection.  This process repeats for a number of iterations.  
     * Modify the CONNECT_ITERATIONS constant to change the number of 
     * iterations.  Modify the VERBOSE constant to print out information while
     * performing searches.
     */
    @Test
    public void testStartTls() throws Exception
    {
        for ( int ii = 0; ii < CONNECT_ITERATIONS; ii++ )
        {
            if ( VERBOSE )
            {
                System.out.println( "Performing " + ii + "-th iteration to connect via StartTLS." );
            }

            System.setProperty ( "javax.net.ssl.trustStore", ksFile.getAbsolutePath() );
            System.setProperty ( "javax.net.ssl.keyStore", ksFile.getAbsolutePath() );
            System.setProperty ( "javax.net.ssl.keyStorePassword", "changeit" );
            LOG.debug( "testStartTls() test starting ... " );
            
            // Set up environment for creating initial context
            Hashtable<String, Object> env = new Hashtable<String,Object>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( "java.naming.security.principal", "uid=admin,ou=system" );
            env.put( "java.naming.security.credentials", "secret" );
            env.put( "java.naming.security.authentication", "simple" );
            
            // Must use the name of the server that is found in its certificate?
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );
    
            // Create initial context
            LOG.debug( "About to get initial context" );
            LdapContext ctx = new InitialLdapContext( env, null );
    
            // Start TLS
            LOG.debug( "About send startTls extended operation" );
            StartTlsResponse tls = ( StartTlsResponse ) ctx.extendedOperation( new StartTlsRequest() );
            LOG.debug( "Extended operation issued" );
            tls.setHostnameVerifier( new HostnameVerifier() {
                public boolean verify( String hostname, SSLSession session )
                {
                    return true;
                } 
            } );
            LOG.debug( "TLS negotion about to begin" );
            tls.negotiate();

            search( ii, ctx );
            
            tls.close();
            ctx.close();
        }
    }
}
