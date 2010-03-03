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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Hashtable;

import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.net.SocketClient;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.message.MessageDecoder;
import org.apache.directory.shared.ldap.message.MessageEncoder;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.internal.InternalBindResponse;
import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link AbstractServerTest} testing SASL authentication.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( FrameworkRunner.class ) 
@ApplyLdifs( {
    // Entry # 1
    "dn: ou=users,dc=example,dc=com", 
    "objectClass: organizationalUnit", 
    "objectClass: top", 
    "ou: users\n",  
    // Entry # 2
    "dn: uid=hnelson,ou=users,dc=example,dc=com", 
    "objectClass: inetOrgPerson", 
    "objectClass: organizationalPerson", 
    "objectClass: person", 
    "objectClass: top", 
    "uid: hnelson", 
    "userPassword: secret", 
    "cn: Horatio Nelson", 
    "sn: Nelson" 
    }
)
@CreateDS( allowAnonAccess=true, name="SaslBindIT-class",
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry( 
                    entryLdif =
                        "dn: dc=example,dc=com\n" +
                        "dc: example\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" ),
                indexes = 
                {
                    @CreateIndex( attribute = "objectClass" ),
                    @CreateIndex( attribute = "dc" ),
                    @CreateIndex( attribute = "ou" )
                } )
        })
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" )
    },
    saslHost="localhost",
    saslMechanisms = 
    {
        @SaslMechanism( name=SupportedSaslMechanisms.PLAIN, implClass=PlainMechanismHandler.class ),
        @SaslMechanism( name=SupportedSaslMechanisms.CRAM_MD5, implClass=CramMd5MechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.DIGEST_MD5, implClass=DigestMd5MechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.GSSAPI, implClass=GssapiMechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.NTLM, implClass=NtlmMechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.GSS_SPNEGO, implClass=NtlmMechanismHandler.class)
    },
    extendedOpHandlers = 
    {
        StoredProcedureExtendedOperationHandler.class
    },
    ntlmProvider=BogusNtlmProvider.class
    )
public class SaslBindIT extends AbstractLdapTestUnit
{

    
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

//             Thread.sleep( 10 * 60 * 1000 );
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
         catch ( Exception e )
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
         BogusNtlmProvider provider = getNtlmProviderUsingReflection();
         
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
         BogusNtlmProvider provider = new BogusNtlmProvider();

         // the provider configured in @CreateLdapServer only sets for the NTLM mechanism
         // but we use the same NtlmMechanismHandler class for GSS_SPNEGO too but this is a separate
         // instance, so we need to set the provider in the NtlmMechanismHandler instance of GSS_SPNEGO mechanism
         NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) ldapServer.getSaslMechanismHandlers().get( SupportedSaslMechanisms.GSS_SPNEGO );
         ntlmHandler.setNtlmProvider( provider );

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
             request.setName( new DN( "uid=admin,ou=system" ) ) ;
             request.setSimple( false ) ;
             request.setCredentials( type1response ) ;
             request.setSaslMechanism( mechanism );
             request.setVersion3( true ) ;
             
             // Setup the ASN1 Encoder and Decoder
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
             request.setName( new DN( "uid=admin,ou=system" ) ) ;
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
     
     
     private BogusNtlmProvider getNtlmProviderUsingReflection()
     {
         BogusNtlmProvider provider = null;
         try
         {
             NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) ldapServer.getSaslMechanismHandlers().get( SupportedSaslMechanisms.NTLM );
             
             // there is no getter for 'provider' field hence this hack
             Field field = ntlmHandler.getClass().getDeclaredField( "provider" );
             field.setAccessible( true );
             provider = ( BogusNtlmProvider ) field.get( ntlmHandler );
         }
         catch( Exception e )
         {
             e.printStackTrace();
         }
         
         return provider;
     }

}

