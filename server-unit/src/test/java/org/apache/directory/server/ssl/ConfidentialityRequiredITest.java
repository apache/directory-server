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


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
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
public class ConfidentialityRequiredITest extends AbstractServerTest 
{
    private static final Logger LOG = LoggerFactory.getLogger( ConfidentialityRequiredITest.class );
    private File ksFile;
    
    
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
    public void setUp() throws Exception
    {
    	super.setUp();
    	
    	if ( ksFile != null && ksFile.exists() )
    	{
    		ksFile.delete();
    	}
    	
    	ksFile = File.createTempFile( "testStore", "ks" );

        Attributes adminEntry = sysRoot.getAttributes( "uid=admin" );
    	Attribute userCertificateAttr = adminEntry.get( "userCertificate" );
    	
    	assertNotNull( userCertificateAttr );
    	byte[] userCertificate = ( byte[] ) userCertificateAttr.get();
    	assertNotNull( userCertificate );
    	ByteArrayInputStream in = new ByteArrayInputStream( userCertificate );
    	
    	CertificateFactory factory = CertificateFactory.getInstance( "X.509" );
    	Certificate cert = factory.generateCertificate( in );
    	KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
    	ks.load( null, null );
    	ks.setCertificateEntry( "apacheds", cert );
    	ks.store( new FileOutputStream( ksFile ), "changeit".toCharArray() );
    	LOG.debug( "Keystore file installed: {}", ksFile.getAbsolutePath() );
    }
    
    
    /**
     * Just deletes the generated key store file.
     */
    public void tearDown() throws Exception
    {
    	if ( ksFile != null && ksFile.exists() )
    	{
    		ksFile.delete();
    	}
    	
    	LOG.debug( "Keystore file deleted: {}", ksFile.getAbsolutePath() );
    	super.tearDown();
    }
    

    /**
     * Setup confidentiality to be required.
     */
    protected void configureLdapServer()
    {
    	super.configureLdapServer();
    	/*
    	 * TODO un-comment and enable tests after adding this feature to 1.5.4
    	 * see https://issues.apache.org/jira/browse/DIRSERVER-1194
    	 *
    	ldapServer.setConfidentialityRequired( true );
    	*/
    }

    
    private LdapContext getSecuredContext() throws Exception
    {
    	System.setProperty ( "javax.net.ssl.trustStore", ksFile.getAbsolutePath() );
    	System.setProperty ( "javax.net.ssl.keyStore", ksFile.getAbsolutePath() );
    	System.setProperty ( "javax.net.ssl.keyStorePassword", "changeit" );
    	LOG.debug( "testStartTls() test starting ... " );
    	
    	// Set up environment for creating initial context
    	Hashtable<String, Object> env = new Hashtable<String,Object>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        
        // Must use the name of the server that is found in its certificate?
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

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
        return ctx;
    }
    

    /**
     * Checks to make sure insecure binds fail while secure binds succeed.
     */
    public void testConfidentiality() throws Exception
    {
    	// -------------------------------------------------------------------
    	// Unsecured bind should fail
    	// -------------------------------------------------------------------

    	try
    	{
    		getWiredContext();
    		// TODO un comment when confidentiality requirement feature is enabled
    		// see https://issues.apache.org/jira/browse/DIRSERVER-1194
//    		fail( "Should not get here due to violation of confidentiality requirements" );
    	}
    	catch( AuthenticationNotSupportedException e )
    	{
    	}
    	
    	// -------------------------------------------------------------------
    	// get anonymous connection with StartTLS (no bind request sent)
    	// -------------------------------------------------------------------

    	LdapContext ctx = getSecuredContext();
    	assertNotNull( ctx );
    	
    	// -------------------------------------------------------------------
    	// upgrade connection via bind request (same physical connection - TLS)
    	// -------------------------------------------------------------------

    	ctx.addToEnvironment( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
    	ctx.addToEnvironment( Context.SECURITY_CREDENTIALS, "secret" );
    	ctx.addToEnvironment( Context.SECURITY_AUTHENTICATION, "simple" );
    	ctx.reconnect( null );
    	
    	// -------------------------------------------------------------------
    	// do a search and confirm
    	// -------------------------------------------------------------------

    	NamingEnumeration<SearchResult> results = ctx.search( "ou=system", "(objectClass=*)", new SearchControls() );
    	Set<String> names = new HashSet<String>();
    	while( results.hasMore() )
    	{
    		names.add( results.next().getName() );
    	}
    	results.close();
    	assertTrue( names.contains( "prefNodeName=sysPrefRoot" ) );
    	assertTrue( names.contains( "ou=users" ) );
    	assertTrue( names.contains( "ou=configuration" ) );
    	assertTrue( names.contains( "uid=admin" ) );
    	assertTrue( names.contains( "ou=groups" ) );
    	
    	// -------------------------------------------------------------------
    	// do add and confirm
    	// -------------------------------------------------------------------

    	AttributesImpl attrs = new AttributesImpl( "objectClass", "person", true );
    	attrs.put( "sn", "foo" );
    	attrs.put( "cn", "foo bar" );
    	ctx.createSubcontext( "cn=foo bar,ou=system", attrs );
    	assertNotNull( ctx.lookup( "cn=foo bar,ou=system" ) );
    	
    	// -------------------------------------------------------------------
    	// do modify and confirm
    	// -------------------------------------------------------------------

    	ModificationItem[] mods = new ModificationItem[] {
    			new ModificationItem( DirContext.ADD_ATTRIBUTE, new AttributeImpl( "cn", "fbar" ) )
    	};
    	ctx.modifyAttributes( "cn=foo bar,ou=system", mods );
    	Attributes reread = ( Attributes ) ctx.getAttributes( "cn=foo bar,ou=system" );
    	assertTrue( reread.get( "cn" ).contains( "fbar" ) );
    	
    	// -------------------------------------------------------------------
    	// do rename and confirm 
    	// -------------------------------------------------------------------

    	ctx.rename( "cn=foo bar,ou=system", "cn=fbar,ou=system" );
    	try
    	{
    		ctx.getAttributes( "cn=foo bar,ou=system" );
    		fail( "old name of renamed entry should not be found" );
    	}
    	catch ( NameNotFoundException e )
    	{
    	}
    	reread = ( Attributes ) ctx.getAttributes( "cn=fbar,ou=system" );
    	assertTrue( reread.get( "cn" ).contains( "fbar" ) );
    	
    	// -------------------------------------------------------------------
    	// do delete and confirm
    	// -------------------------------------------------------------------

    	ctx.destroySubcontext( "cn=fbar,ou=system" );
    	try
    	{
    		ctx.getAttributes( "cn=fbar,ou=system" );
    		fail( "deleted entry should not be found" );
    	}
    	catch ( NameNotFoundException e )
    	{
    	}
    	
    	ctx.close();
    }
}
