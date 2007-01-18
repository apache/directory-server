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
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;


/**
 * Tests to make sure the server is operating correctly when handling referrals.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 493916 $
 */
public class ReferralITest extends AbstractServerTest
{
    TestData td = new TestData();


    /**
     * Create attributes for a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new AttributesImpl();
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );
        attributes.put( "description", cn + " is a person." );
        return attributes;
    }


    public void setUp() throws Exception
    {
        super.setUp();
        addReferralEntries();
        addCountries();
        addCities();
        addUsers();
    }


    public void tearDown() throws Exception
    {
        if ( td.refEurop != null )
        {
            td.refEurop.close();
        }

        if ( td.refAmerica != null )
        {
            td.refAmerica.close();
        }

        td.ctx.close();

        super.tearDown();
    }


    /**
     * Get's the root "ou=system" using the SUN JNDI provider on the embedded ApacheDS instance
     */
    private LdapContext getSystemRoot() throws NamingException
    {
        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        LdapContext ctx = new InitialLdapContext( env, null );
        assertNotNull( ctx );
        return ctx;
    }

    private class TestData
    {
        LdapContext ctx;
        Name ctxDn;
        LdapContext refEurop;
        LdapContext refAmerica;
        List refs;
    }

    /**
     * Create entries  
     * c=europ, ou=system 
     * and
     * c=america, ou=system 
     */
    private void addReferralEntries() throws NamingException
    {
        String europURL = "ldap://localhost:" + port + "/c=france,ou=system";
        String americaURL = "ldap://localhost:" + port + "/c=usa,ou=system";

        td.ctx = getSystemRoot();

        // -------------------------------------------------------------------
        // Adds a referral entry regardless of referral handling settings
        // -------------------------------------------------------------------

        // Add a referral entry for europ
        Attributes europ = new AttributesImpl( "objectClass", "top", true );
        europ.get( "objectClass" ).add( "referral" );
        europ.get( "objectClass" ).add( "extensibleObject" );
        europ.put( "ref", europURL );
        europ.put( "c", "europ" );

        // Add a referral entry for america
        Attributes america = new AttributesImpl( "objectClass", "top", true );
        america.get( "objectClass" ).add( "referral" );
        america.get( "objectClass" ).add( "extensibleObject" );
        america.put( "ref", americaURL );
        america.put( "c", "america" );

        // Just in case if server is a remote server destroy remaing referral
        td.ctx.addToEnvironment( Context.REFERRAL, "ignore" );
        
        try
        {
            td.refEurop = ( LdapContext ) td.ctx.createSubcontext( "c=europ", europ );
            td.refAmerica = ( LdapContext ) td.ctx.createSubcontext( "c=america", america );
        }
        catch ( NameAlreadyBoundException e )
        {
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Create entries 
     * c=france, ou=system 
     * and
     * c=usa, ou=system 
     */
    private void addCountries() throws NamingException
    {
        td.ctx = getSystemRoot();

        // -------------------------------------------------------------------
        // Adds a referral entry regardless of referral handling settings
        // -------------------------------------------------------------------

        // Add a referral entry for europ
        Attributes france = new AttributesImpl( "objectClass", "top", true );
        france.get( "objectClass" ).add( "country" );
        france.put( "c", "France" );

        // Add a referral entry for america
        Attributes usa = new AttributesImpl( "objectClass", "top", true );
        france.get( "objectClass" ).add( "country" );
        usa.put( "c", "USA" );

        try
        {
            td.ctx.createSubcontext( "c=france", france );
            td.ctx.createSubcontext( "c=usa", usa );
        }
        catch ( NameAlreadyBoundException e )
        {
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Create cities 
     * l=paris, c=france, ou=system 
     * and
     * l=jacksonville, c=usa, ou=system 
     */
    private void addCities() throws NamingException
    {
        td.ctx = getSystemRoot();

        // -------------------------------------------------------------------
        // Adds a referral entry regardless of referral handling settings
        // -------------------------------------------------------------------

        // Add a referral entry for europ
        Attributes paris = new AttributesImpl( "objectClass", "top", true );
        paris.get( "objectClass" ).add( "locality" );
        paris.put( "l", "Paris" );

        // Add a referral entry for america
        Attributes jacksonville = new AttributesImpl( "objectClass", "top", true );
        jacksonville.get( "objectClass" ).add( "locality" );
        jacksonville.put( "l", "jacksonville" );

        try
        {
            td.ctx.createSubcontext( "l=paris, c=france", paris );
            td.ctx.createSubcontext( "l=jacksonville, c=usa", jacksonville );
        }
        catch ( NameAlreadyBoundException e )
        {
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Create users 
     * cn=emmanuel lecharny, l=paris, c=france, ou=system 
     * and
     * cn=alex karasulu, l=jacksonville, c=usa, ou=system 
     */
    private void addUsers() throws NamingException
    {
        td.ctx = getSystemRoot();

        // -------------------------------------------------------------------
        // Adds a referral entry regardless of referral handling settings
        // -------------------------------------------------------------------

        // Add a referral entry for europ
        Attributes emmanuel = new AttributesImpl( "objectClass", "top", true );
        emmanuel.get( "objectClass" ).add( "person" );
        emmanuel.get( "objectClass" ).add( "residentialperson" );
        emmanuel.put( "cn", "emmanuel lecharny" );
        emmanuel.put( "sn", "elecharny" );
        emmanuel.put( "l", "Paris" );

        // Add a referral entry for america
        Attributes alex = new AttributesImpl( "objectClass", "top", true );
        alex.get( "objectClass" ).add( "person" );
        alex.get( "objectClass" ).add( "residentialperson" );
        alex.put( "cn", "alex karasulu" );
        alex.put( "sn", "akarasulu" );
        alex.put( "l", "Jacksonville" );

        try
        {
            td.ctx.createSubcontext( "cn=emmanuel lecharny, l=paris, c=france", emmanuel );
            td.ctx.createSubcontext( "cn=alex karasulu, l=jacksonville, c=usa", alex );
        }
        catch ( NameAlreadyBoundException e )
        {
            System.out.println( e.getMessage() );
        }
    }
    
    /**
     * Performs a search from a base and 
     * check that the expected result is found
     */
    private boolean exist( LdapContext ctx, String filter, String expected ) throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        return exist( ctx, filter, expected, controls );
    }
    
    /**
     * Performs a search from a base and 
     * check that the expected result is found
     */
    private boolean exist( LdapContext ctx, String filter, String expected, 
        SearchControls controls ) throws NamingException
    {
        NamingEnumeration ii = ctx.search( "", filter, controls );
        
        // collect all results 
        Set results = new HashSet();
        
        while ( ii.hasMore() )
        {
            SearchResult result = ( SearchResult ) ii.next();
            results.add( result.getName() );
        }
        
        if ( results.size() == 1 )
        {
            return results.contains( expected );
        }
        
        return false;
    }
    
    /**
     * Performs a single level search from a contect base and 
     * return the result as a Set, or throws an exception 
     */
    private Set search( LdapContext ctx, String filter ) throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        return search( ctx, filter, controls );
    }
    
    /**
     * Performs a single level search from a contect base and 
     * return the result as a Set, or throws an exception 
     */
    private Set search( LdapContext ctx, String filter, SearchControls controls ) throws NamingException
    {
        NamingEnumeration ii = ctx.search( "", filter, controls );
        
        // collect all results 
        Set results = new HashSet();
        
        while ( ii.hasMore() )
        {
            SearchResult result = ( SearchResult ) ii.next();
            results.add( result.getName() );
        }
        
        return results;
    }
    
    //-------------------------------------------------------------------------
    //
    // Search operations
    //
    //-------------------------------------------------------------------------
    /**
     * Test of an search operation with a referral
     * 
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    public void testSearchWithReferralThrow() throws Exception
    {
        td.refAmerica.addToEnvironment( Context.REFERRAL, "throw" );
        
        try
        {
            search( td.refAmerica, "(cn=alex karasulu)" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            assertEquals( "ldap://localhost:" + port + "/c=usa,ou=system??sub", referral );
        }
    }

    /**
     * Test of an search operation with a referral
     * 
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    public void testSearchBaseWithReferralThrow() throws Exception
    {
        td.refAmerica.addToEnvironment( Context.REFERRAL, "throw" );
        
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        
        try
        {
            search( td.refAmerica, "(cn=alex karasulu)", controls );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            assertEquals( "ldap://localhost:" + port + "/c=usa,ou=system??base", referral );
        }
    }

    /**
     * Test of an search operation with a referral
     * 
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    public void testSearchOneLevelWithReferralThrow() throws Exception
    {
        td.refAmerica.addToEnvironment( Context.REFERRAL, "throw" );
        
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        try
        {
            search( td.refAmerica, "(cn=alex karasulu)", controls );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            assertEquals( "ldap://localhost:" + port + "/c=usa,ou=system??one", referral );
        }
    }

    /**
     * Test of an search operation with a referral
     * 
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    public void testSearchWithReferralContinuation() throws Exception
    {
        assertTrue( exist( td.ctx, "(cn=alex karasulu)",
        "cn=alex karasulu,l=jacksonville,c=usa" ) );

        td.refAmerica.addToEnvironment( Context.REFERRAL, "follow" );
        
        assertTrue( exist( td.refAmerica, "(cn=alex karasulu)",
            "ldap://localhost:" + port + "/cn=alex%20karasulu,l=jacksonville,c=usa,ou=system" ) );
    }

    /**
     * Test of an search operation with a referral
     * 
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    public void testSearchBaseWithReferralContinuation() throws Exception
    {
        td.refAmerica.addToEnvironment( Context.REFERRAL, "follow" );
        
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        
        assertFalse( exist( td.refAmerica, "(cn=alex karasulu)",
            "cn=alex karasulu,l=jacksonville,c=usa", controls ) );
    }

    /**
     * Test of an search operation with a referral
     * 
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    public void testSearchOneLevelWithReferralContinuation() throws Exception
    {
        td.refAmerica.addToEnvironment( Context.REFERRAL, "follow" );
        
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        assertFalse( exist( td.refAmerica, "(cn=alex karasulu)",
            "cn=alex karasulu,l=jacksonville,c=usa", controls ) );
    }

    //-------------------------------------------------------------------------
    //
    // Add operations
    //
    //-------------------------------------------------------------------------

    /**
     * Test of an add operation with a referral
     * 
     * "cn=pierre-arnaud marcelot,l=paris" is added to "c=france,ou=system"
     * we should get a referral URL thrown
     */
    public void testAddWithReferralThrow() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral parent. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------

        td.refEurop.addToEnvironment( Context.REFERRAL, "throw" );
        
        Attributes userEntry = new AttributesImpl( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "marcelot" );
        userEntry.put( "cn", "pierre-arnaud marcelot" );

        try
        {
            td.refEurop.createSubcontext( "cn=pierre-arnaud marcelot,l=paris", userEntry );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            // @TODO : the returned LDAPURL must be escaped !!!
            assertEquals( "ldap://localhost:" + port + "/cn=pierre-arnaud marcelot,l=paris,c=france,ou=system", referral );
        }
    }

    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an add operation with the parent context being a referral.
     * 
     * Test an add operation with a continuation
     * 
     * "cn=pierre-arnaud marcelot,l=paris" is added to "c=europ,ou=system"
     * 
     * The entry should be added to "l=paris,c=france,ou=system"
     */
    public void testAddWithReferralContinuation() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral parent. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------

        td.refEurop.addToEnvironment( Context.REFERRAL, "follow" );
        
        Attributes userEntry = new AttributesImpl( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "marcelot" );
        userEntry.put( "cn", "pierre-arnaud marcelot" );

        td.refEurop.createSubcontext( "cn=pierre-arnaud marcelot,l=paris", userEntry );
        
        assertTrue( exist( td.ctx, "(cn=pierre-arnaud marcelot)", "cn=pierre-arnaud marcelot,l=paris,c=france" ) );

        td.refEurop.destroySubcontext( "cn=pierre-arnaud marcelot,l=paris" );
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an add operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    /*public void testAddWithReferralAncestor() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral ancestor. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------

        td.refUsa.addToEnvironment( Context.REFERRAL, "throw" );
        Attributes userEntry = new AttributesImpl( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "karasulu" );
        userEntry.put( "cn", "alex karasulu" );

        try
        {
            td.refCtx.createSubcontext( "cn=alex karasulu,ou=apache", userEntry );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
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
    /*public void testDeleteWithReferralParent() throws Exception
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
        catch ( ReferralException e )
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
    /*public void testDeleteWithReferralAncestor() throws Exception
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
        catch ( ReferralException e )
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
    /*public void testModifyWithReferralParent() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // parent.  We should encounter referral errors with referral setting 
        // set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.modifyAttributes( "cn=alex karasulu", DirContext.ADD_ATTRIBUTE, new AttributesImpl(
                "description", "just some text", true ) );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
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
    /*public void testModifyWithReferralAncestor() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // ancestor. We should encounter referral errors when referral setting 
        // is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.modifyAttributes( "cn=alex karasulu,ou=apache", DirContext.ADD_ATTRIBUTE, new AttributesImpl(
                "description", "just some text", true ) );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
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
    /*public void testModifyWithReferralParent2() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // parent.  We should encounter referral errors with referral setting 
        // set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            ModificationItemImpl[] mods = new ModificationItemImpl[]
                { new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, new AttributeImpl( "description", "just some text" ) ) };
            td.refCtx.modifyAttributes( "cn=alex karasulu", mods );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
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
    /*public void testModifyWithReferralAncestor2() throws Exception
    {
        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // ancestor. We should encounter referral errors when referral setting 
        // is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            ModificationItemImpl[] mods = new ModificationItemImpl[]
                { new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, new AttributeImpl( "description", "just some text" ) ) };
            td.refCtx.modifyAttributes( "cn=alex karasulu,ou=apache", mods );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
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
    /*public void testModifyRdnWithReferralParent() throws Exception
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
        catch ( ReferralException e )
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
    /*public void testModifyRdnWithReferralAncestor() throws Exception
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
        catch ( ReferralException e )
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
    /*public void testMoveWithReferralParent() throws Exception
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
        catch ( PartialResultException e )
        {
        }
        catch ( ReferralException e )
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
    /*public void testMoveWithReferralAncestor() throws Exception
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
        catch ( PartialResultException e )
        {
        }
        catch ( ReferralException e )
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
    /*public void testMoveWithReferralParent2() throws Exception
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
        catch ( PartialResultException e )
        {
        }
        catch ( ReferralException e )
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
    /*public void testMoveWithReferralAncestor2() throws Exception
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
        catch ( PartialResultException e )
        {
        }
        catch ( ReferralException e )
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
    /*public void testMoveWithReferralParentDest() throws Exception
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
        catch ( NamingException e )
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
    /*public void testMoveWithReferralAncestorDest() throws Exception
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
        catch ( NamingException e )
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
    /*public void testMoveWithReferralParent2Dest() throws Exception
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
        catch ( NamingException e )
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
    /*public void testMoveWithReferralAncestor2Dest() throws Exception
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
        catch ( NamingException e )
        {
        }
    }


    public void createLocalUser() throws Exception
    {
        LdapContext userCtx = null;
        Attributes referral = new AttributesImpl( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "person" );
        referral.put( "cn", "akarasulu" );
        referral.put( "sn", "karasulu" );

        try
        {
            td.rootCtx.destroySubcontext( "uid=akarasulu" );
        }
        catch ( NameNotFoundException e )
        {
        }
        try
        {
            userCtx = ( LdapContext ) td.rootCtx.createSubcontext( "cn=akarasulu", referral );
        }
        catch ( NameAlreadyBoundException e )
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
        Attributes referral = new AttributesImpl( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "person" );
        referral.put( "cn", "akarasulu" );
        referral.put( "sn", "karasulu" );

        try
        {
            td.rootCtx.destroySubcontext( "uid=akarasulu,ou=deep" );
        }
        catch ( NameNotFoundException e )
        {
        }
        try
        {
            td.rootCtx.destroySubcontext( "ou=deep" );
        }
        catch ( NameNotFoundException e )
        {
        }
        try
        {
            Attributes ouAttrs = new AttributesImpl( "objectClass", "top", true );
            ouAttrs.get( "objectClass" ).add( "organizationalUnit" );
            ouAttrs.put( "ou", "deep" );
            td.rootCtx.createSubcontext( "ou=deep", ouAttrs );
            userCtx = ( LdapContext ) td.rootCtx.createSubcontext( "cn=akarasulu,ou=deep", referral );
        }
        catch ( NameAlreadyBoundException e )
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
        catch ( ReferralException e )
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
        catch ( ReferralException e )
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
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/cn=alex karasulu,ou=apache,ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex karasulu,ou=apache,ou=users,dc=example,dc=com??base", e
                .getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/cn=alex karasulu,ou=apache,ou=users,ou=system??base", e
                .getReferralInfo() );
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
            SearchResult result = ( SearchResult ) list.next();
            results.put( result.getName(), result );
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
                results.put( result.getName(), result );
            }
        }
        catch ( ReferralException e )
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
                results.put( result.getName(), result );
            }
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??base", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }

        assertNull( results.get( "ou=users" ) );
    }*/
}