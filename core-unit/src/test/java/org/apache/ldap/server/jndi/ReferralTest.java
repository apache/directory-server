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
package org.apache.ldap.server.jndi;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.server.unit.AbstractAdminTestCase;


/**
 * Tests the referral handling functionality within the server's core.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReferralTest extends AbstractAdminTestCase
{
    private static final boolean SUNJNDI = false;
    private boolean sunjndi = System.getProperty( "sunjndi" ) != null || SUNJNDI;
    
    
    public void setUp() throws Exception
    {
        if ( ! sunjndi )
        {
            super.setUp();
        }
    }
    
    
    public void tearDown() throws Exception
    {
        if ( ! sunjndi )
        {
            super.tearDown();
        }
    }

    /*
     * NOTE: We may encounter conflicting circumstances where the ManageDsaIT control
     * is included in the request controls yet the Context.REFERRAL is set to 
     * something other than ignore: throw or follow.  We have to figure out what to
     * do in these cases.
     * 
     * Simply throw an illegal state exception when this happens?
     * 
     * NOTE: Need to figure out the behavoir of referral handling during search
     * when aliases are being dereferenced.
     */

    /**
     * Get's the root "ou=system" in either the embedded instance or an
     * external instance.  We do this to mold test cases to make sure the 
     * ApacheDS JNDI LDAP provider behaves just like the SUN JNDI LDAP
     * provider does.
     */
    private LdapContext getSystemRoot() throws NamingException
    {
        if ( ! sunjndi )
        {
            return sysRoot;
        }
        
        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://hertz.karasulu.homeip.net:10390/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "longsecret" );
        env.put( "java.naming.security.authentication", "simple" );
        return new InitialLdapContext( env, null );
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an add operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testAddWithReferralParent() throws Exception
    {
        String ref0 = "ldap://fermi:10389/ou=users,ou=system";
        String ref1 = "ldap://hertz:10389/ou=users,dc=example,dc=com";
        String ref2 = "ldap://maxwell:10389/ou=users,ou=system";
        LdapContext root = getSystemRoot();
        LdapContext refctx = null;

        // -------------------------------------------------------------------
        // Adds a referral entry regardless of referral handling settings
        // -------------------------------------------------------------------
        
        // Add a referral entry ( should be fine with or without the control )
        Attributes referral = new BasicAttributes( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "referral" );
        referral.get( "objectClass" ).add( "extensibleObject" );
        referral.put( "ref", ref0 );
        referral.get( "ref" ).add( ref1 );
        referral.get( "ref" ).add( ref2 );
        referral.put( "ou", "users" );

        // Just in case if server is a remote server destroy remaing referral
        root.addToEnvironment( Context.REFERRAL, "ignore" );
        try { root.destroySubcontext( "uid=akarasulu,ou=users" ); } catch( NameNotFoundException e ) { e.printStackTrace(); }
        try { root.destroySubcontext( "ou=users" ); } catch( NameNotFoundException e ) { e.printStackTrace(); }
        try
        {
            refctx = ( LdapContext ) root.createSubcontext( "ou=users", referral );
        }
        catch( NameAlreadyBoundException e )
        {
            refctx = ( LdapContext ) root.lookup( "ou=users" );
        }
        referral = refctx.getAttributes( "" );
        assertTrue( referral.get( "ou" ).contains( "users" ) );
        assertTrue( referral.get( "objectClass" ).contains( "referral" ) );

        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral parent. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------
        
        // attempt to add another entry below the referral entry but now throw and exception
        refctx.addToEnvironment( Context.REFERRAL, "throw" );
        Attributes userEntry = new BasicAttributes( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "karasulu" );
        userEntry.put( "cn", "alex karasulu" );

        //try { refctx.destroySubcontext( "cn=Alex Karasulu" ); } catch( NameNotFoundException e ) {}
        try 
        {
            refctx.createSubcontext( "cn=alex karasulu", userEntry );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex karasulu,ou=users,dc=example,dc=com", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
        refctx.close();
        
        if ( sunjndi )
        {
            root.close();
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an add operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testAddWithReferralAncestor() throws Exception
    {
        String ref0 = "ldap://fermi:10389/ou=users,ou=system";
        String ref1 = "ldap://hertz:10389/ou=users,dc=example,dc=com";
        String ref2 = "ldap://maxwell:10389/ou=users,ou=system";
        LdapContext root = getSystemRoot();
        LdapContext refctx = null;

        // -------------------------------------------------------------------
        // Adds a referral entry regardless of referral handling settings
        // -------------------------------------------------------------------
        
        // Add a referral entry ( should be fine with or without the control )
        Attributes referral = new BasicAttributes( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "referral" );
        referral.get( "objectClass" ).add( "extensibleObject" );
        referral.put( "ref", ref0 );
        referral.get( "ref" ).add( ref1 );
        referral.get( "ref" ).add( ref2 );
        referral.put( "ou", "users" );

        // Just in case if server is a remote server destroy remaing referral
        root.addToEnvironment( Context.REFERRAL, "ignore" );
        try { root.destroySubcontext( "uid=akarasulu,ou=users" ); } catch( NameNotFoundException e ) { e.printStackTrace(); }
        try { root.destroySubcontext( "ou=users" ); } catch( NameNotFoundException e ) { e.printStackTrace(); }
        try
        {
            refctx = ( LdapContext ) root.createSubcontext( "ou=users", referral );
        }
        catch( NameAlreadyBoundException e )
        {
            refctx = ( LdapContext ) root.lookup( "ou=users" );
        }
        referral = refctx.getAttributes( "" );
        assertTrue( referral.get( "ou" ).contains( "users" ) );
        assertTrue( referral.get( "objectClass" ).contains( "referral" ) );

        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral ancestor. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------
        
        // attempt to add another entry below the referral entry but now throw and exception
        refctx.addToEnvironment( Context.REFERRAL, "throw" );
        Attributes userEntry = new BasicAttributes( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "karasulu" );
        userEntry.put( "cn", "alex karasulu" );

        try 
        {
            refctx.createSubcontext( "cn=alex karasulu,ou=apache", userEntry );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex karasulu,ou=apache,ou=users,dc=example,dc=com", 
                e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
        refctx.close();
        
        if ( sunjndi )
        {
            root.close();
        }
    }
}
