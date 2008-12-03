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


import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * An {@link AbstractServerTest} testing SIMPLE authentication.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@ApplyLdifs( {
    // Entry # 1
    "dn: uid=hnelson,ou=users,ou=system\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "userPassword: secret\n" +
    "uid: hnelson\n" +
    "cn: Horatio Nelson\n" +
    "sn: Nelson\n\n"
    }
)
public class SimpleBindIT
{
    private static final String BASE = "ou=users,ou=system";

    
    public static LdapService ldapService;

    
    /**
     * Convenience method for creating a person.
     */
    protected Attributes getPersonAttributes( String sn, String cn, String uid, String userPassword )
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
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
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
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
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson," + BASE );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            Attributes attrs = context.getAttributes( "uid=hnelson," + BASE, attrIDs );

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
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson," + BASE );
        env.put( Context.SECURITY_CREDENTIALS, "badsecret" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( AuthenticationException ae )
        {
            // Error code 49 : LDAP_INVALID_CREDENTIALS
            assertTrue( ae.getMessage().contains( "error code 49" ) );
        }
        catch ( NamingException e )
        {
            fail();
        }
    }

    
    /**
     * try to connect using a user with an invalid DN: we should get a invalidDNSyntax error.
     */
    @Test
    public void testSimpleBindBadUserPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );

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
     */
    @Test
    public void testSimpleBindUnknowUserPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=unknown,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( AuthenticationException ae )
        {
        }
        catch ( NamingException e )
        {
            fail( "Expected AuthenticationException with error code 49 for invalidate credentials instead got: " 
                + e.getMessage() );
        }
    }
    

    /**
     * covers the anonymous authentication : we should be able to read the rootDSE, but that's it
     */
    @Test
    public void testSimpleBindNoUserNoPassword()
    {
        boolean oldValue = ldapService.getDirectoryService().isAllowAnonymousAccess();
        ldapService.getDirectoryService().setAllowAnonymousAccess( false );
        ldapService.setAllowAnonymousAccess( false );

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );
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
            fail( "Should not be able to read the root DSE" );
        }
        catch ( NamingException ne )
        {
        }
        
        ldapService.getDirectoryService().setAllowAnonymousAccess( oldValue );
        ldapService.setAllowAnonymousAccess( oldValue );
    }
    
    
    /**
     * covers the Unauthenticated case : we should get a UnwillingToPerform error.
     */
    @Test
    public void testSimpleBindUserNoPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );
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
            assertTrue( onse.getMessage().contains( "error code 53" ) );
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }    
    
    
    /**
     * not allowed by the server. We should get a invalidCredentials error.
     */
    @Test
    public void testSimpleBindNoUserPassword() throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );

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
        }
        catch ( NamingException ne )
        {
            fail( "Expected AuthenticationException but instead got: " + ne.getMessage() );
        }
    }    


    /**
     * Tests to make sure we still have anonymous access to the RootDSE.
     * The configuration for this test case MUST disable anonymous access.
     */
    @Test
    public void testAnonymousRootDSE()
    {
        boolean oldValue = ldapService.getDirectoryService().isAllowAnonymousAccess();
        ldapService.getDirectoryService().setAllowAnonymousAccess( false );

        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );

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
            e.printStackTrace();
            fail( "Should not have caught exception." );
        }
        finally
        {
            ldapService.getDirectoryService().setAllowAnonymousAccess( oldValue );
        }
    }
}
