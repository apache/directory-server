/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.ldap.server;


import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;


/**
 * Tests to make sure the server is operating correctly when handling referrals.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReferralTest extends AbstractServerTest
{
    TestData td = new TestData();


    /**
     * Create attributes for a person entry.
     */
    protected Attributes getPersonAttributes(String sn, String cn)
    {
        Attributes attributes = new BasicAttributes();
        Attribute attribute = new BasicAttribute("objectClass");
        attribute.add("top");
        attribute.add("person");
        attributes.put(attribute);
        attributes.put("cn", cn);
        attributes.put("sn", sn);
        attributes.put("description", cn + " is a person.");
        return attributes;
    }

    
    public void setUp() throws Exception
    {
        super.setUp();
        addReferralEntry();
    }
    
    
    public void tearDown() throws Exception
    {
        if ( td.refCtx != null )
        {
            td.refCtx.close();
        }
        
        super.tearDown();
    }

    
    /**
     * Get's the root "ou=system" using the SUN JNDI provider on the embedded ApacheDS instance
     */
    private LdapContext getSystemRoot() throws NamingException
    {
        Hashtable env = new Hashtable();
        env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" ); 
        env.put("java.naming.security.principal", "uid=admin,ou=system" ); 
        env.put("java.naming.security.credentials", "secret" );
        env.put("java.naming.security.authentication", "simple");
        LdapContext ctx = new InitialLdapContext(env, null);
        assertNotNull(ctx);
        return ctx;
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
        try { td.rootCtx.destroySubcontext( "uid=akarasulu,ou=users" ); } catch( NameNotFoundException e ) {}
        try { td.rootCtx.destroySubcontext( "ou=users" ); } catch( NameNotFoundException e ) {}
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

    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify rdn interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testModifyRdnWithReferralParent() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.rename( "cn=alex karasulu", "cn=aok" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify rdn interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testModifyRdnWithReferralAncestor() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.rename( "cn=alex karasulu,ou=apache", "cn=aok,ou=apache" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }

    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testMoveWithReferralParent() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.rename( "cn=alex karasulu", "cn=alex karasulu,ou=groups" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( PartialResultException e )
        {
        }
        catch( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testMoveWithReferralAncestor() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.rename( "cn=alex karasulu,ou=apache", "cn=alex karasulu,ou=groups" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( PartialResultException e )
        {
        }
        catch( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }

    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testMoveWithReferralParent2() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.rename( "cn=alex karasulu", "cn=aok,ou=groups" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( PartialResultException e )
        {
        }
        catch( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testMoveWithReferralAncestor2() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.refCtx.rename( "cn=alex karasulu,ou=apache", "cn=aok,ou=groups" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch( PartialResultException e )
        {
        }
        catch( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }

    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testMoveWithReferralParentDest() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        createLocalUser();
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.rootCtx.rename( "cn=akarasulu", "cn=akarasulu,ou=users" );
            fail( "Should fail here throwing a LdapNamingException with ResultCodeEnum = AFFECTSMULTIPLEDSAS" );
        }
        // this should have a result code of 71 for affectsMultipleDSAs
        // however there is absolutely no way for us to tell if this is 
        // the case because of JNDI exception resolution issues with LDAP
        catch( NamingException e )
        {
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testMoveWithReferralAncestorDest() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        createDeepLocalUser();
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.rootCtx.rename( "cn=akarasulu,ou=deep", "cn=akarasulu,ou=users" );
            fail( "Should fail here throwing a LdapNamingException with ResultCodeEnum = AFFECTSMULTIPLEDSAS" );
        }
        // this should have a result code of 71 for affectsMultipleDSAs
        // however there is absolutely no way for us to tell if this is 
        // the case because of JNDI exception resolution issues with LDAP
        catch( NamingException e )
        {
        }
    }

    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testMoveWithReferralParent2Dest() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        createLocalUser();
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.rootCtx.rename( "cn=akarasulu", "cn=aok,ou=users" );
            fail( "Should fail here throwing a LdapNamingException with ResultCodeEnum = AFFECTSMULTIPLEDSAS" );
        }
        // this should have a result code of 71 for affectsMultipleDSAs
        // however there is absolutely no way for us to tell if this is 
        // the case because of JNDI exception resolution issues with LDAP
        catch( NamingException e )
        {
        }
    }
    
    
    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    public void testMoveWithReferralAncestor2Dest() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        createDeepLocalUser();
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try 
        {
            td.rootCtx.rename( "cn=akarasulu,ou=deep", "cn=aok,ou=users" );
            fail( "Should fail here throwing a LdapNamingException with ResultCodeEnum = AFFECTSMULTIPLEDSAS" );
        }
        // this should have a result code of 71 for affectsMultipleDSAs
        // however there is absolutely no way for us to tell if this is 
        // the case because of JNDI exception resolution issues with LDAP
        catch( NamingException e )
        {
        }
    }
    
    
    public void createLocalUser() throws Exception
    {
        LdapContext userCtx = null;
        Attributes referral = new BasicAttributes( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "person" );
        referral.put( "cn", "akarasulu" );
        referral.put( "sn", "karasulu" );

        try { td.rootCtx.destroySubcontext( "uid=akarasulu" ); } catch( NameNotFoundException e ) {}
        try
        {
            userCtx = ( LdapContext ) td.rootCtx.createSubcontext( "cn=akarasulu", referral );
        }
        catch( NameAlreadyBoundException e )
        {
            td.refCtx = ( LdapContext ) td.rootCtx.lookup( "cn=akarasulu" );
        }
        referral = userCtx.getAttributes( "" );
        assertTrue( referral.get( "cn" ).contains( "akarasulu" ) );
        assertTrue( referral.get( "sn" ).contains( "karasulu" ) );
    }
    
    
    public void createDeepLocalUser() throws Exception
    {
        LdapContext userCtx = null;
        Attributes referral = new BasicAttributes( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "person" );
        referral.put( "cn", "akarasulu" );
        referral.put( "sn", "karasulu" );

        try { td.rootCtx.destroySubcontext( "uid=akarasulu,ou=deep" ); } catch( NameNotFoundException e ) {}
        try { td.rootCtx.destroySubcontext( "ou=deep" ); } catch( NameNotFoundException e ) {}
        try
        {
            td.rootCtx.createSubcontext( "ou=deep" );
            userCtx = ( LdapContext ) td.rootCtx.createSubcontext( "cn=akarasulu,ou=deep", referral );
        }
        catch( NameAlreadyBoundException e )
        {
            td.refCtx = ( LdapContext ) td.rootCtx.lookup( "cn=akarasulu,ou=deep" );
        }
        referral = userCtx.getAttributes( "" );
        assertTrue( referral.get( "cn" ).contains( "akarasulu" ) );
        assertTrue( referral.get( "sn" ).contains( "karasulu" ) );
    }
    
    
    public void testSearchBaseIsReferral() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.rootCtx.search( "ou=users", "(objectClass=*)", controls );
            fail( "should never get here" );
        }
        catch( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }

    
    public void testSearchBaseParentIsReferral() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.search( "cn=alex karasulu", "(objectClass=*)", controls );
        }
        catch( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/cn=alex karasulu,ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex karasulu,ou=users,dc=example,dc=com??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/cn=alex karasulu,ou=users,ou=system??base", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }

    
    public void testSearchBaseAncestorIsReferral() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.search( "cn=alex karasulu,ou=apache", "(objectClass=*)", controls );
        }
        catch( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/cn=alex karasulu,ou=apache,ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex karasulu,ou=apache,ou=users,dc=example,dc=com??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/cn=alex karasulu,ou=apache,ou=users,ou=system??base", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }

    
    public void testSearchContinuations() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration list = td.rootCtx.search( "", "(objectClass=*)", controls );
        Map results = new HashMap();
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult) list.next();
            results.put ( result.getName(), result );
        }
        
        assertNotNull( results.get( "ou=users" ) );
        
        // -------------------------------------------------------------------
        // Now we will throw exceptions when searching for referrals 
        // -------------------------------------------------------------------
        
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        list = td.rootCtx.search( "", "(objectClass=*)", controls );
        results = new HashMap();
        
        try
        {
            while ( list.hasMore() )
            {
                SearchResult result = ( SearchResult ) list.next();
                results.put ( result.getName(), result );
            }
        }
        catch( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
        
        assertNull( results.get( "ou=users" ) );

        // try again but this time with single level scope
        
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        list = td.rootCtx.search( "", "(objectClass=*)", controls );
        results = new HashMap();
        
        try
        {
            while ( list.hasMore() )
            {
                SearchResult result = ( SearchResult ) list.next();
                results.put ( result.getName(), result );
            }
        }
        catch( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??base", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
        
        assertNull( results.get( "ou=users" ) );
    }
}