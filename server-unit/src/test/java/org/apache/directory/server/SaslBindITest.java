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


import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.MutableBTreePartitionConfiguration;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;


/**
 * An {@link AbstractServerTest} testing SASL DIGEST-MD5 and CRAM-MD5
 * authentication and security layer negotiation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SaslBindITest extends AbstractServerTest
{
    private DirContext ctx = null;


    /**
     * Set up a partition for EXAMPLE.COM and add a user to
     * test authentication with.
     */
    public void setUp() throws Exception
    {
        configuration.setAllowAnonymousAccess( false );
        configuration.getLdapConfiguration().setSaslHost( "localhost" );

        Attributes attrs;
        Set<PartitionConfiguration> pcfgs = new HashSet<PartitionConfiguration>();

        MutableBTreePartitionConfiguration pcfg;

        // Add partition 'example'
        pcfg = new MutableBTreePartitionConfiguration();
        pcfg.setName( "example" );
        pcfg.setSuffix( "dc=example,dc=com" );

        Set<Index> indexedAttrs = new HashSet<Index>();
        indexedAttrs.add( new JdbmIndex( "ou" ) );
        indexedAttrs.add( new JdbmIndex( "dc" ) );
        indexedAttrs.add( new JdbmIndex( "objectClass" ) );
        pcfg.setIndexedAttributes( indexedAttrs );

        attrs = new AttributesImpl( true );
        Attribute attr = new AttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attrs.put( attr );
        attr = new AttributeImpl( "dc" );
        attr.add( "example" );
        attrs.put( attr );
        pcfg.setContextEntry( attrs );

        pcfgs.add( pcfg );
        configuration.setPartitionConfigurations( pcfgs );
        super.setUp();

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/dc=example,dc=com" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );

        attrs = getOrgUnitAttributes( "users" );
        DirContext users = ctx.createSubcontext( "ou=users", attrs );

        attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "secret" );
        users.createSubcontext( "uid=hnelson", attrs );
    }


    /**
     * Tear down.
     */
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
    public void testSupportedSASLMechanisms()
    {
        try
        {
            DirContext ctx = new InitialDirContext();

            Attributes attrs = ctx.getAttributes( "ldap://localhost:" + port, new String[]
                { "supportedSASLMechanisms" } );

            NamingEnumeration answer = attrs.getAll();

            if ( answer.hasMore() )
            {
                Attribute result = ( Attribute ) answer.next();
                assertTrue( result.size() == 3 );
                assertTrue( result.contains( "GSSAPI" ) );
                assertTrue( result.contains( "DIGEST-MD5" ) );
                assertTrue( result.contains( "CRAM-MD5" ) );
            }
            else
            {
                fail( "Should have returned 3 SASL mechanisms." );
            }
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
    public void testAnonymousRootDSE()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "vendorName" };

            Attributes attrs = ctx.getAttributes( "", attrIDs );

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
    public void testAnonymousBelowRootDSE()
    {
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "vendorName" };

            ctx.getAttributes( "dc=example,dc=com", attrIDs );

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

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            Attributes attrs = ctx.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

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

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            ctx.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

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
    public void testSaslDigestMd5Bind()
    {
        try
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

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            Attributes attrs = ctx.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

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
     * Tests to make sure DIGEST-MD5 binds below the RootDSE fail if the realm is bad.
     */
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

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            ctx.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

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

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            ctx.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

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

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            Attributes attrs = ctx.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

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

            DirContext ctx = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            ctx.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

            fail( "Should have thrown exception." );
        }
        catch ( NamingException e )
        {
            assertTrue( e.getMessage().contains( "Invalid response" ) );
        }
    }
}
