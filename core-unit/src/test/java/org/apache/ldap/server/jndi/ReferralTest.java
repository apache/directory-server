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
import java.util.List;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.name.LdapName;
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
    TestData td = new TestData();
    
    
    public void setUp() throws Exception
    {
        if ( ! sunjndi )
        {
            super.setUp();
        }
        
        addReferralEntry();
    }
    
    
    public void tearDown() throws Exception
    {
        if ( td.refCtx != null )
        {
            td.refCtx.close();
        }
        
        if ( ! sunjndi )
        {
            super.tearDown();
        }
        else if ( td.rootCtx != null )
        {
            td.rootCtx.close();
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
    
    
    class TestData {
        LdapContext rootCtx;
        Name ctxDn;
        LdapContext refCtx;
        List refs;
    }
    
    
    public void addReferralEntry() throws NamingException
    {
        String ref0 = "ldap://fermi:10389/ou=users,ou=system";
        String ref1 = "ldap://hertz:10389/ou=users,dc=example,dc=com";
        String ref2 = "ldap://maxwell:10389/ou=users,ou=system";
        td.rootCtx = getSystemRoot();

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
        td.rootCtx.addToEnvironment( Context.REFERRAL, "ignore" );
        try { td.rootCtx.destroySubcontext( "uid=akarasulu,ou=users" ); } catch( NameNotFoundException e ) { e.printStackTrace(); }
        try { td.rootCtx.destroySubcontext( "ou=users" ); } catch( NameNotFoundException e ) { e.printStackTrace(); }
        try
        {
            td.refCtx = ( LdapContext ) td.rootCtx.createSubcontext( "ou=users", referral );
        }
        catch( NameAlreadyBoundException e )
        {
            td.refCtx = ( LdapContext ) td.rootCtx.lookup( "ou=users" );
        }
        referral = td.refCtx.getAttributes( "" );
        assertTrue( referral.get( "ou" ).contains( "users" ) );
        assertTrue( referral.get( "objectClass" ).contains( "referral" ) );
    }

    
    public void checkAncestorReferrals( ReferralException e ) throws Exception
    {
        assertEquals( "ldap://fermi:10389", e.getReferralInfo() );
        assertTrue( e.skipReferral() );
        assertEquals( "ldap://hertz:10389/cn=alex karasulu,ou=apache,ou=users,dc=example,dc=com", 
            e.getReferralInfo() );
        assertTrue( e.skipReferral() );
        assertEquals( "ldap://maxwell:10389", e.getReferralInfo() );
        assertFalse( e.skipReferral() );
    }

    
    public void checkParentReferrals( ReferralException e ) throws Exception
    {
        assertEquals( "ldap://fermi:10389", e.getReferralInfo() );
        assertTrue( e.skipReferral() );
        assertEquals( "ldap://hertz:10389/cn=alex karasulu,ou=users,dc=example,dc=com", e.getReferralInfo() );
        assertTrue( e.skipReferral() );
        assertEquals( "ldap://maxwell:10389", e.getReferralInfo() );
        assertFalse( e.skipReferral() );
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an add operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testAddWithReferralParent() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral parent. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        Attributes userEntry = new BasicAttributes( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "karasulu" );
        userEntry.put( "cn", "alex karasulu" );

        try 
        {
            td.refCtx.createSubcontext( "cn=alex karasulu", userEntry );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkParentReferrals( e );
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
        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral ancestor. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        Attributes userEntry = new BasicAttributes( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "karasulu" );
        userEntry.put( "cn", "alex karasulu" );

        try 
        {
            td.refCtx.createSubcontext( "cn=alex karasulu,ou=apache", userEntry );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an delete operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testDeleteWithReferralParent() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to delete a non-existent entry below the referral parent. 
        // We should encounter referral errors with referral setting set to 
        // throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.destroySubcontext( "cn=alex karasulu" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a delete operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testDeleteWithReferralAncestor() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to delete a non-existent entry below the referral ancestor. 
        // We should encounter referral errors when referral setting is set to 
        // throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.destroySubcontext( "cn=alex karasulu,ou=apache" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an delete operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testCompareWithReferralParent() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to compare attributes in an entry below the referral parent. 
        // We should encounter referral errors with referral setting set to 
        // throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            if ( td.refCtx instanceof ServerLdapContext )
            {
                LdapName dn = new LdapName( "cn=alex karasulu,ou=users,ou=system" );
                ( ( ServerLdapContext ) td.refCtx ).compare( dn, "sn", "karasulu" );
            }
            else
            {
                // abort the test because we're using the sun jdni provider
                return;
            }
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a compare operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testCompareWithReferralAncestor() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to compare attributes in an entry below the referral ancestor. 
        // We should encounter referral errors when referral setting is set to 
        // throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            if ( td.refCtx instanceof ServerLdapContext )
            {
                LdapName dn = new LdapName( "cn=alex karasulu,ou=apache,ou=users,ou=system" );
                ( ( ServerLdapContext ) td.refCtx ).compare( dn, "sn", "karasulu" );
            }
            else
            {
                // abort the test because we're using the sun jdni provider
                return;
            }
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testModifyWithReferralParent() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // parent.  We should encounter referral errors with referral setting 
        // set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.modifyAttributes( "cn=alex karasulu", DirContext.ADD_ATTRIBUTE, 
                new BasicAttributes( "description", "just some text", true ) );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testModifyWithReferralAncestor() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // ancestor. We should encounter referral errors when referral setting 
        // is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.modifyAttributes( "cn=alex karasulu,ou=apache", DirContext.ADD_ATTRIBUTE, 
                new BasicAttributes( "description", "just some text", true ) );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testModifyWithReferralParent2() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // parent.  We should encounter referral errors with referral setting 
        // set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            ModificationItem[] mods = new ModificationItem[] { new ModificationItem( 
                DirContext.ADD_ATTRIBUTE, new BasicAttribute( "description", "just some text" ) ) };
            td.refCtx.modifyAttributes( "cn=alex karasulu", mods );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testModifyWithReferralAncestor2() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // ancestor. We should encounter referral errors when referral setting 
        // is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            ModificationItem[] mods = new ModificationItem[] { new ModificationItem( 
                DirContext.ADD_ATTRIBUTE, new BasicAttribute( "description", "just some text" ) ) };
            td.refCtx.modifyAttributes( "cn=alex karasulu,ou=apache", mods );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }
}
