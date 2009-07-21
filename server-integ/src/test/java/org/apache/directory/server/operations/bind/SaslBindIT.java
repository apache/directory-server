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
package org.apache.directory.server.operations.bind;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.net.SocketClient;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmProvider;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.message.InternalBindResponse;
import org.apache.directory.shared.ldap.message.MessageDecoder;
import org.apache.directory.shared.ldap.message.MessageEncoder;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


/**
 * An {@link AbstractServerTest} testing SASL authentication.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( SaslBindIT.Factory.class )
@ApplyLdifs( {
    // Entry #0
    "dn: dc=example,dc=com\n" +
    "dc: example\n" +
    "objectClass: top\n" +
    "objectClass: domain\n\n" +
    
    // Entry # 1
    "dn: ou=users,dc=example,dc=com\n" +
    "objectClass: organizationalUnit\n" +
    "objectClass: top\n" +
    "ou: users\n\n" + 
    // Entry # 2
    "dn: uid=hnelson,ou=users,dc=example,dc=com\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "uid: hnelson\n" +
    "userPassword: secret\n" +
    "cn: Horatio Nelson\n" +
    "sn: Nelson\n\n" 
    }
)
public class SaslBindIT
{
    public static LdapServer ldapServer;
    public BogusNtlmProvider provider = new BogusNtlmProvider();

     
     public static class Factory implements LdapServerFactory
     {
         public LdapServer newInstance() throws Exception
         {
             DirectoryService service = new DefaultDirectoryService();
             IntegrationUtils.doDelete( service.getWorkingDirectory() );
             service.getChangeLog().setEnabled( true );
             service.setAllowAnonymousAccess( false );
             service.setShutdownHookEnabled( false );

             Set<Partition> partitions = new HashSet<Partition>();
             JdbmPartition partition = new JdbmPartition();
             partition.setId( "example" );
             partition.setSuffix( "dc=example,dc=com" );

             Set<Index<?,ServerEntry>> indexedAttrs = new HashSet<Index<?,ServerEntry>>();
             indexedAttrs.add( new JdbmIndex<String,ServerEntry>( "ou" ) );
             indexedAttrs.add( new JdbmIndex<String,ServerEntry>( "dc" ) );
             indexedAttrs.add( new JdbmIndex<String,ServerEntry>( "objectClass" ) );
             partition.setIndexedAttributes( indexedAttrs );

             partitions.add( partition );
             service.setPartitions( partitions );

             // change the working directory to something that is unique
             // on the system and somewhere either under target directory
             // or somewhere in a temp area of the machine.

             LdapServer ldapServer = new LdapServer();
             int port = AvailablePortFinder.getNextAvailable( 1024 );
             ldapServer.setTransports( new TcpTransport( port ) );
             ldapServer.setDirectoryService( service );
             ldapServer.setAllowAnonymousAccess( false );
             ldapServer.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

             // Setup SASL Mechanisms
             
             Map<String, MechanismHandler> mechanismHandlerMap = new HashMap<String,MechanismHandler>();
             mechanismHandlerMap.put( SupportedSaslMechanisms.PLAIN, new PlainMechanismHandler() );

             CramMd5MechanismHandler cramMd5MechanismHandler = new CramMd5MechanismHandler();
             mechanismHandlerMap.put( SupportedSaslMechanisms.CRAM_MD5, cramMd5MechanismHandler );

             DigestMd5MechanismHandler digestMd5MechanismHandler = new DigestMd5MechanismHandler();
             mechanismHandlerMap.put( SupportedSaslMechanisms.DIGEST_MD5, digestMd5MechanismHandler );

             GssapiMechanismHandler gssapiMechanismHandler = new GssapiMechanismHandler();
             mechanismHandlerMap.put( SupportedSaslMechanisms.GSSAPI, gssapiMechanismHandler );

             NtlmMechanismHandler ntlmMechanismHandler = new NtlmMechanismHandler();
             mechanismHandlerMap.put( SupportedSaslMechanisms.NTLM, ntlmMechanismHandler );
             mechanismHandlerMap.put( SupportedSaslMechanisms.GSS_SPNEGO, ntlmMechanismHandler );

             ldapServer.setSaslMechanismHandlers( mechanismHandlerMap );
             ldapServer.setSaslHost( "localhost" );
             
             return ldapServer;
         }
     }
     
     
     @Before
     public void setupNewNtlmProvider()
     {
         provider = new BogusNtlmProvider();
         NtlmMechanismHandler handler = ( NtlmMechanismHandler ) 
             ldapServer.getSaslMechanismHandlers().get( SupportedSaslMechanisms.NTLM );
         handler.setNtlmProvider( provider );
     }
     

     /**
      * Tests to make sure the server properly returns the supportedSASLMechanisms.
      */
     @Test
     public void testSupportedSASLMechanisms()
     {
         try
         {
             // We have to tell the server that it should accept anonymous
             // auth, because we are reading the rootDSE
             ldapServer.setAllowAnonymousAccess( true );
             ldapServer.getDirectoryService().setAllowAnonymousAccess( true );
             
             // Point on rootDSE
             DirContext context = new InitialDirContext();

             Attributes attrs = context.getAttributes( "ldap://localhost:" 
                 + ldapServer.getPort(), new String[]
                 { "supportedSASLMechanisms" } );

             NamingEnumeration<? extends Attribute> answer = attrs.getAll();
             Attribute result = answer.next();
             assertEquals( 6, result.size() );
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
      * Tests to make sure PLAIN-binds works
      */
     @Test
     public void testSaslBindPLAIN()
     {
         try
         {
             Hashtable<String, String> env = new Hashtable<String, String>();
             env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
             env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );

             env.put( Context.SECURITY_AUTHENTICATION, "PLAIN" );
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
      * Test a SASL bind with an empty mechanism 
      */
     @Test
     public void testSaslBindNoMech()
     {
         try
         {
             Hashtable<String, String> env = new Hashtable<String, String>();
             env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
             env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );

             env.put( Context.SECURITY_AUTHENTICATION, "" );
             env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
             env.put( Context.SECURITY_CREDENTIALS, "secret" );

             new InitialDirContext( env );
             fail( "Should not be there" );
         }
         catch ( AuthenticationNotSupportedException anse )
         {
             assertTrue( true );
         }
         catch ( NamingException ne )
         {
             fail( "Should not have caught exception." );
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
             env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );

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
             env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );

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
      * Tests to make sure DIGEST-MD5 binds below the RootDSE work.
      */
     @Test
     public void testSaslDigestMd5Bind() throws Exception
     {
         Hashtable<String, String> env = new Hashtable<String, String>();
         env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
         env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );

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
             env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );

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
             env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );

             env.put( Context.SECURITY_AUTHENTICATION, "DIGEST-MD5" );
             env.put( Context.SECURITY_PRINCIPAL, "hnelson" );
             env.put( Context.SECURITY_CREDENTIALS, "badsecret" );

             DirContext context = new InitialDirContext( env );
             String[] attrIDs = { "uid" };

             context.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );
             fail( "Should have thrown exception." );
         }
         catch ( NamingException e )
         {
             assertTrue( e.getMessage().contains( "digest response format violation" ) );
         }
     }


     /**
      * Tests that the plumbing for NTLM bind works.
      */
     @Test
     public void testNtlmBind() throws Exception
     {
         NtlmSaslBindClient client = new NtlmSaslBindClient( SupportedSaslMechanisms.NTLM );
         InternalBindResponse type2response = client.bindType1( "type1_test".getBytes() );
         assertEquals( 1, type2response.getMessageId() );
         assertEquals( ResultCodeEnum.SASL_BIND_IN_PROGRESS, type2response.getLdapResult().getResultCode() );
         assertTrue( ArrayUtils.isEquals( "type1_test".getBytes(), provider.getType1Response() ) );
         assertTrue( ArrayUtils.isEquals( "challenge".getBytes(), type2response.getServerSaslCreds() ) );
         
         InternalBindResponse finalResponse = client.bindType3( "type3_test".getBytes() );
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
         InternalBindResponse type2response = client.bindType1( "type1_test".getBytes() );
         assertEquals( 1, type2response.getMessageId() );
         assertEquals( ResultCodeEnum.SASL_BIND_IN_PROGRESS, type2response.getLdapResult().getResultCode() );
         assertTrue( ArrayUtils.isEquals( "type1_test".getBytes(), provider.getType1Response() ) );
         assertTrue( ArrayUtils.isEquals( "challenge".getBytes(), type2response.getServerSaslCreds() ) );
         
         InternalBindResponse finalResponse = client.bindType3( "type3_test".getBytes() );
         assertEquals( 2, finalResponse.getMessageId() );
         assertEquals( ResultCodeEnum.SUCCESS, finalResponse.getLdapResult().getResultCode() );
         assertTrue( ArrayUtils.isEquals( "type3_test".getBytes(), provider.getType3Response() ) );
     }

     
     /**
      * A fake implementation of the NtlmProvider. We can't use a real one because
      * its license is not ASL 2.0 compatible.
      */
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


     /**
      * A NTLM client
      */
     class NtlmSaslBindClient extends SocketClient
     {
         private final Logger LOG = LoggerFactory.getLogger( NtlmSaslBindClient.class );
         
         private final String mechanism;
         
         
         NtlmSaslBindClient( String mechanism ) throws Exception
         {
             this.mechanism = mechanism;
             setDefaultPort( ldapServer.getPort() );
             connect( "localhost", ldapServer.getPort() );
             setTcpNoDelay( false );
             
             LOG.debug( "isConnected() = {}", isConnected() );
             LOG.debug( "LocalPort     = {}", getLocalPort() );
             LOG.debug( "LocalAddress  = {}", getLocalAddress() );
             LOG.debug( "RemotePort    = {}", getRemotePort() );
             LOG.debug( "RemoteAddress = {}", getRemoteAddress() );
         }

         
         InternalBindResponse bindType1( byte[] type1response ) throws Exception
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
             return ( InternalBindResponse ) decoder.decode( null, _input_ );
         }
         
         
         InternalBindResponse bindType3( byte[] type3response ) throws Exception
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
             return ( InternalBindResponse ) decoder.decode( null, _input_ );
         }
     }
}
