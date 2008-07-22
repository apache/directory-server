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


import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * An {@link AbstractServerTest} testing SIMPLE authentication.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SimpleBindITest extends AbstractServerTest
{
    private DirContext ctx;


    /**
     * Set up a partition for EXAMPLE.COM and add a user to
     * test authentication with.
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

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
        directoryService.setAllowAnonymousAccess( true );

        Set<Partition> partitions = new HashSet<Partition>();
        JdbmPartition partition = new JdbmPartition();
        partition.setId( "example" );
        partition.setSuffix( "dc=example,dc=com" );

        Set<Index<?,ServerEntry>> indexedAttrs = new HashSet<Index<?,ServerEntry>>();
        indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "ou" ) );
        indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "dc" ) );
        indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "objectClass" ) );
        partition.setIndexedAttributes( indexedAttrs );

        LdapDN exampleDn = new LdapDN( "dc=example,dc=com" );
        ServerEntry serverEntry = new DefaultServerEntry( directoryService.getRegistries(), exampleDn );
        serverEntry.put( "objectClass", "top", "domain" );
        serverEntry.put( "dc", "example" );

        partition.setContextEntry( serverEntry );

        partitions.add( partition );
        directoryService.setPartitions( partitions );
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
     * Tests to make sure SIMPLE binds works.
     */
    @Test
    public void testSimpleBind()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
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
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
        env.put( Context.SECURITY_CREDENTIALS, "badsecret" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( AuthenticationException ae )
        {
    		// Error code 49 : LDAP_INVALID_CREDENTIALS
            assertTrue( ae.getMessage().startsWith( "[LDAP: error code 49 - Bind failed" ) );
        }
        catch ( NamingException e )
        {
            fail();
        }
    }

    /**
     * try to connect using a user with an invalid DN: we should get a invalidDNSyntax error.
     * 
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindBadUserPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "hnelson" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( InvalidNameException ine )
        {
    		// Error code 34 : LDAP_INVALID_DN_SYNTAX
        	assertTrue( ine.getMessage().startsWith( "[LDAP: error code 34 - Incorrect DN given" ) );
        }
        catch ( NamingException e )
        {
            fail();
        }
    }

    
    /**
     * try to connect using a unknown user: we should get a invalidCredentials error.
     * 
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindUnknowUserPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=unknown,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( AuthenticationException ae )
        {
    		// Error code 49 : LDAP_INVALID_CREDENTIALS
            assertTrue( ae.getMessage().startsWith( "[LDAP: error code 49 - Bind failed" ) );
        }
        catch ( NamingException e )
        {
            fail();
        }
    }
    

    /**
     * covers the anonymous authentication : we should be able to read the rootDSE, but that's it
     * 
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindNoUserNoPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_CREDENTIALS, "" );

        String[] attrIDs = { "*", "+" };
    	DirContext ctx = null;
    	
    	// Create the initial context
    	try
    	{
    		ctx = new InitialDirContext(env);
    	}
    	catch ( NamingException ne )
    	{
    		fail();
    	}
    	
    	// We should be anonymous here. 
    	// Check that we can read the rootDSE
    	try
    	{
            Attributes attrs = ctx.getAttributes( "", attrIDs );
    		
    		assertNotNull( attrs );
    		assertEquals( "Apache Software Foundation", attrs.get( "vendorName" ).get() );
    	}
    	catch ( NamingException ne )
    	{
    		fail();
    	}

    	// Check that we cannot read another entry being anonymous
    	try
    	{
            Attributes attrs = ctx.getAttributes( "uid=admin,ou=system", attrIDs );
    		
    		assertNotNull( attrs );
    		assertEquals( 0, attrs.size() );
    	}
    	catch ( NamingException ne )
    	{
    		fail();
    	}
    }
    
    
    /**
     * covers the Unauthenticated case : we should get a UnwillingToPerform error.
     * 
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindUserNoPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "" );

        // Create the initial context
    	try
    	{
    		new InitialDirContext(env);
    	}
    	catch ( OperationNotSupportedException onse )
    	{
    		// Error code 53 : LDAP_UNWILLING_TO_PERFORM
    		assertTrue( onse.getMessage().startsWith( "[LDAP: error code 53 - Bind failed" ) );
    	}
    	catch ( NamingException ne )
    	{
    		fail();
    	}
    }    
    
    /**
     * not allowed by the server. We should get a invalidCredentials error.
     * 
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindNoUserPassword() throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        // Create the initial context
    	try
    	{
    		new InitialDirContext(env);
    	}
    	catch ( AuthenticationException ae )
    	{
    		// Error code 32 : LDAP_NO_SUCH_OBJECT
    		assertTrue( ae.getMessage().startsWith( "[LDAP: error code 32 - Bind failed" ) );
    	}
    	catch ( NamingException ne )
    	{
    		fail();
    	}
    }    
}
