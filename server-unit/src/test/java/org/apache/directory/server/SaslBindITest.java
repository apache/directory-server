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
package org.apache.directory.server;


import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.net.SocketClient;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmProvider;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.message.BindResponse;
import org.apache.directory.shared.ldap.message.MessageDecoder;
import org.apache.directory.shared.ldap.message.MessageEncoder;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.mina.common.IoSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link AbstractServerTest} testing SASL DIGEST-MD5 and CRAM-MD5
 * authentication and security layer negotiation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SaslBindITest extends AbstractServerTest
{
    private DirContext ctx;
    private BogusNtlmProvider provider;


    /**
     * Set up a partition for EXAMPLE.COM and add a user to
     * test authentication with.
     */
    @Before
    public void setUp() throws Exception
    {
        provider = new BogusNtlmProvider();
        super.setUp();
        setAllowAnonymousAccess( false );

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/dc=example,dc=com" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );

        Attributes attrs = new AttributesImpl( true );
        attrs = getOrgUnitAttributes( "users" );
        DirContext users = ctx.createSubcontext( "ou=users", attrs );

        attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "secret" );
        users.createSubcontext( "uid=hnelson", attrs );
    }


    @Override
    protected void configureDirectoryService() throws NamingException
    {
        Set<Partition> partitions = new HashSet<Partition>();
        JdbmPartition partition = new JdbmPartition();
        partition.setId( "example" );
        partition.setSuffix( "dc=example,dc=com" );

        Set<Index<?,ServerEntry>> indexedAttrs = new HashSet<Index<?,ServerEntry>>();
        indexedAttrs.add( new JdbmIndex( "ou" ) );
        indexedAttrs.add( new JdbmIndex( "dc" ) );
        indexedAttrs.add( new JdbmIndex( "objectClass" ) );
        partition.setIndexedAttributes( indexedAttrs );

        LdapDN exampleDn = new LdapDN( "dc=example,dc=com" );
        ServerEntry serverEntry = new DefaultServerEntry( directoryService.getRegistries(), exampleDn );
        serverEntry.put( "objectClass", "top", "domain" );
        serverEntry.put( "dc", "example" );

        partition.setContextEntry( serverEntry );


        partitions.add( partition );
        directoryService.setPartitions( partitions );
    }


    @Override
    protected void configureLdapServer()
    {
        ldapServer.setSaslHost( "localhost" );
        
        NtlmMechanismHandler ntlmMechanismHandler = new NtlmMechanismHandler();
        ntlmMechanismHandler.setNtlmProvider( provider  );
        
        ldapServer.removeSaslMechanismHandler( SupportedSaslMechanisms.NTLM );
        ldapServer.addSaslMechanismHandler( SupportedSaslMechanisms.NTLM, ntlmMechanismHandler );
        ldapServer.removeSaslMechanismHandler( SupportedSaslMechanisms.GSS_SPNEGO );
        ldapServer.addSaslMechanismHandler( SupportedSaslMechanisms.GSS_SPNEGO, ntlmMechanismHandler );
    }

    
    /**
     * Tear down.
     */
    @After
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }


    /**
     * Convenience method for creating a person.
     */
    protected Attributes getPersonAttributes( String sn, String cn, String uid, String userPassword )
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" ); // sn $ cn
        ocls.add( "inetOrgPerson" ); // uid
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );
        attrs.put( "uid", uid );
        attrs.put( "userPassword", userPassword );

        return attrs;
    }


    /**
     * Convenience method for creating an organizational unit.
     */
    protected Attributes getOrgUnitAttributes( String ou )
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "organizationalUnit" );
        attrs.put( ocls );
        attrs.put( "ou", ou );

        return attrs;
    }


    /**
     * Tests to make sure the server properly returns the supportedSASLMechanisms.
     */
    @Test
    public void testSupportedSASLMechanisms()
    {
        try
        {
            DirContext context = new InitialDirContext();

            Attributes attrs = context.getAttributes( "ldap://localhost:" + port, new String[]
                { "supportedSASLMechanisms" } );

            NamingEnumeration<? extends Attribute> answer = attrs.getAll();
            Attribute result = answer.next();
            assertTrue( result.size() == 6 );
            assertTrue( result.contains( SupportedSaslMechanisms.GSSAPI ) );
            assertTrue( result.contains( SupportedSaslMechanisms.DIGEST_MD5 ) );
            assertTrue( result.contains( SupportedSaslMechanisms.CRAM_MD5 ) );
            assertTrue( result.contains( SupportedSaslMechanisms.NTLM ) );
            assertTrue( result.contains( SupportedSaslMechanisms.PLAIN ) );
            assertTrue( result.contains( SupportedSaslMechanisms.GSS_SPNEGO ) );
        }
        catch ( NamingException e )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests to make sure we still have anonymous access to the RootDSE.  The
     * configuration for this testcase MUST disable anonymous access.
     */
    @Test
    public void testAnonymousRootDSE()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "vendorName" };

            Attributes attrs = context.getAttributes( "", attrIDs );

            String vendorName = null;

            if ( attrs.get( "vendorName" ) != null )
            {
                vendorName = ( String ) attrs.get( "vendorName" ).get();
            }

            assertEquals( "Apache Software Foundation", vendorName );
        }
        catch ( NamingException e )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests to make sure binds below the RootDSE require authentication.
     */
    @Test
    public void testAnonymousBelowRootDSE()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "vendorName" };

            context.getAttributes( "dc=example,dc=com", attrIDs );

            fail( "Should not have gotten here." );
        }
        catch ( NamingException e )
        {
            assertTrue( e.getMessage().contains( "Anonymous binds have been disabled!" ) );
        }
    }


    /**
     * Tests to make sure SIMPLE binds below the RootDSE work.
     */
    @Test
    public void testSimpleBindBelowRootDSE()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
            env.put( Context.SECURITY_CREDENTIALS, "secret" );

            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            Attributes attrs = context.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

            String uid = null;

            if ( attrs.get( "uid" ) != null )
            {
                uid = ( String ) attrs.get( "uid" ).get();
            }

            assertEquals( uid, "hnelson" );
        }
        catch ( NamingException e )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests to make sure SIMPLE binds below the RootDSE fail if the password is bad.
     */
    @Test
    public void testSimpleBindBadPassword()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
            env.put( Context.SECURITY_CREDENTIALS, "badsecret" );

            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            context.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

            fail( "Should not have gotten here." );
        }
        catch ( NamingException e )
        {
            assertTrue( e.getMessage().contains( "Bind failed" ) );
        }
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work.
     */
    @Test
    public void testSaslDigestMd5Bind() throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

        env.put( Context.SECURITY_AUTHENTICATION, "DIGEST-MD5" );
        env.put( Context.SECURITY_PRINCIPAL, "hnelson" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        // Specify realm
        env.put( "java.naming.security.sasl.realm", "example.com" );

        // Request privacy protection
        env.put( "javax.security.sasl.qop", "auth-conf" );

        DirContext context = new InitialDirContext( env );

        String[] attrIDs =
            { "uid" };

        Attributes attrs = context.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

        String uid = null;

        if ( attrs.get( "uid" ) != null )
        {
            uid = ( String ) attrs.get( "uid" ).get();
        }

        assertEquals( uid, "hnelson" );
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE fail if the realm is bad.
     */
    @Test
    public void testSaslDigestMd5BindBadRealm()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            env.put( Context.SECURITY_AUTHENTICATION, "DIGEST-MD5" );
            env.put( Context.SECURITY_PRINCIPAL, "hnelson" );
            env.put( Context.SECURITY_CREDENTIALS, "secret" );

            // Bad realm
            env.put( "java.naming.security.sasl.realm", "badrealm.com" );

            // Request privacy protection
            env.put( "javax.security.sasl.qop", "auth-conf" );

            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            context.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

            fail( "Should have thrown exception." );
        }
        catch ( NamingException e )
        {
            assertTrue( e.getMessage().contains( "Nonexistent realm" ) );
        }
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE fail if the password is bad.
     */
    @Test
    public void testSaslDigestMd5BindBadPassword()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            env.put( Context.SECURITY_AUTHENTICATION, "DIGEST-MD5" );
            env.put( Context.SECURITY_PRINCIPAL, "hnelson" );
            env.put( Context.SECURITY_CREDENTIALS, "badsecret" );

            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            context.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

            fail( "Should have thrown exception." );
        }
        catch ( NamingException e )
        {
            assertTrue( e.getMessage().contains( "digest response format violation" ) );
        }
    }


    /**
     * Tests to make sure CRAM-MD5 binds below the RootDSE work.
     */
    @Test
    public void testSaslCramMd5Bind()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            env.put( Context.SECURITY_AUTHENTICATION, "CRAM-MD5" );
            env.put( Context.SECURITY_PRINCIPAL, "hnelson" );
            env.put( Context.SECURITY_CREDENTIALS, "secret" );

            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            Attributes attrs = context.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

            String uid = null;

            if ( attrs.get( "uid" ) != null )
            {
                uid = ( String ) attrs.get( "uid" ).get();
            }

            assertEquals( uid, "hnelson" );
        }
        catch ( NamingException e )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests to make sure CRAM-MD5 binds below the RootDSE fail if the password is bad.
     */
    @Test
    public void testSaslCramMd5BindBadPassword()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            env.put( Context.SECURITY_AUTHENTICATION, "CRAM-MD5" );
            env.put( Context.SECURITY_PRINCIPAL, "hnelson" );
            env.put( Context.SECURITY_CREDENTIALS, "badsecret" );

            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            context.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

            fail( "Should have thrown exception." );
        }
        catch ( NamingException e )
        {
            assertTrue( e.getMessage().contains( "Invalid response" ) );
        }
    }


    /**
     * Tests that the plumbing for NTLM bind works.
     */
    @Test
    public void testNtlmBind() throws Exception
    {
        NtlmSaslBindClient client = new NtlmSaslBindClient( SupportedSaslMechanisms.NTLM );
        BindResponse type2response = client.bindType1( "type1_test".getBytes() );
        assertEquals( 1, type2response.getMessageId() );
        assertEquals( ResultCodeEnum.SASL_BIND_IN_PROGRESS, type2response.getLdapResult().getResultCode() );
        assertTrue( ArrayUtils.isEquals( "type1_test".getBytes(), provider.getType1Response() ) );
        assertTrue( ArrayUtils.isEquals( "challenge".getBytes(), type2response.getServerSaslCreds() ) );
        
        BindResponse finalResponse = client.bindType3( "type3_test".getBytes() );
        assertEquals( 2, finalResponse.getMessageId() );
        assertEquals( ResultCodeEnum.SUCCESS, finalResponse.getLdapResult().getResultCode() );
        assertTrue( ArrayUtils.isEquals( "type3_test".getBytes(), provider.getType3Response() ) );
    }


    /**
     * Tests that the plumbing for NTLM bind works.
     */
    @Test
    public void testGssSpnegoBind() throws Exception
    {
        NtlmSaslBindClient client = new NtlmSaslBindClient( SupportedSaslMechanisms.GSS_SPNEGO );
        BindResponse type2response = client.bindType1( "type1_test".getBytes() );
        assertEquals( 1, type2response.getMessageId() );
        assertEquals( ResultCodeEnum.SASL_BIND_IN_PROGRESS, type2response.getLdapResult().getResultCode() );
        assertTrue( ArrayUtils.isEquals( "type1_test".getBytes(), provider.getType1Response() ) );
        assertTrue( ArrayUtils.isEquals( "challenge".getBytes(), type2response.getServerSaslCreds() ) );
        
        BindResponse finalResponse = client.bindType3( "type3_test".getBytes() );
        assertEquals( 2, finalResponse.getMessageId() );
        assertEquals( ResultCodeEnum.SUCCESS, finalResponse.getLdapResult().getResultCode() );
        assertTrue( ArrayUtils.isEquals( "type3_test".getBytes(), provider.getType3Response() ) );
    }


    class BogusNtlmProvider implements NtlmProvider
    {
        private byte[] type1response;
        private byte[] type3response;
        
        
        public boolean authenticate( IoSession session, byte[] type3response ) throws Exception
        {
            this.type3response = type3response;
            return true;
        }


        public byte[] generateChallenge( IoSession session, byte[] type1reponse ) throws Exception
        {
            this.type1response = type1reponse;
            return "challenge".getBytes();
        }
        
        
        public byte[] getType1Response()
        {
            return type1response;
        }
        
        
        public byte[] getType3Response()
        {
            return type3response;
        }
    }


    class NtlmSaslBindClient extends SocketClient
    {
        private final Logger LOG = LoggerFactory.getLogger( NtlmSaslBindClient.class );
        
        private final String mechanism;
        
        
        NtlmSaslBindClient( String mechanism ) throws Exception
        {
            this.mechanism = mechanism;
            setDefaultPort( port );
            connect( "localhost", port );
            setTcpNoDelay( false );
            
            LOG.debug( "isConnected() = {}", _isConnected_ );
            LOG.debug( "LocalPort     = {}", getLocalPort() );
            LOG.debug( "LocalAddress  = {}", getLocalAddress() );
            LOG.debug( "RemotePort    = {}", getRemotePort() );
            LOG.debug( "RemoteAddress = {}", getRemoteAddress() );
        }

        
        BindResponse bindType1( byte[] type1response ) throws Exception
        {
            if ( ! isConnected() )
            {
                throw new IllegalStateException( "Client is not connected." );
            }
            
            // Setup the bind request
            BindRequestImpl request = new BindRequestImpl( 1 ) ;
            request.setName( new LdapDN( "uid=admin,ou=system" ) ) ;
            request.setSimple( false ) ;
            request.setCredentials( type1response ) ;
            request.setSaslMechanism( mechanism );
            request.setVersion3( true ) ;
            
            // Setup the ASN1 Enoder and Decoder
            MessageEncoder encoder = new MessageEncoder();
            MessageDecoder decoder = new MessageDecoder( new BinaryAttributeDetector() {
                public boolean isBinary( String attributeId )
                {
                    return false;
                }
            } );
     
            // Send encoded request to server
            encoder.encodeBlocking( null, _output_, request );
            _output_.flush();
            
            while ( _input_.available() <= 0 )
            {
                Thread.sleep( 100 );
            }
            
            // Retrieve the response back from server to my last request.
            return ( BindResponse ) decoder.decode( null, _input_ );
        }
        
        
        BindResponse bindType3( byte[] type3response ) throws Exception
        {
            if ( ! isConnected() )
            {
                throw new IllegalStateException( "Client is not connected." );
            }
            
            // Setup the bind request
            BindRequestImpl request = new BindRequestImpl( 2 ) ;
            request.setName( new LdapDN( "uid=admin,ou=system" ) ) ;
            request.setSimple( false ) ;
            request.setCredentials( type3response ) ;
            request.setSaslMechanism( mechanism );
            request.setVersion3( true ) ;
            
            // Setup the ASN1 Enoder and Decoder
            MessageEncoder encoder = new MessageEncoder();
            MessageDecoder decoder = new MessageDecoder( new BinaryAttributeDetector() {
                public boolean isBinary( String attributeId )
                {
                    return false;
                }
            } );
     
            // Send encoded request to server
            encoder.encodeBlocking( null, _output_, request );
            
            _output_.flush();
            
            while ( _input_.available() <= 0 )
            {
                Thread.sleep( 100 );
            }
            
            // Retrieve the response back from server to my last request.
            return ( BindResponse ) decoder.decode( null, _input_ );
        }
    }
}
