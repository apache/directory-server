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
        addReferralEntries();
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
        Hashtable<String, Object> env = new Hashtable<String, Object>();
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
        usa.get( "objectClass" ).add( "country" );
        usa.put( "c", "USA" );
        
        try
        {
            td.ctx.createSubcontext( "c=france", france );
            td.ctx.createSubcontext( "c=usa", usa );
        }
        catch ( NameAlreadyBoundException e )
        {
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
            td.ctx.createSubcontext( "cn=emmanuel lecharny,l=paris,c=france", emmanuel );
            td.ctx.createSubcontext( "cn=alex karasulu,l=jacksonville,c=usa", alex );
        }
        catch ( NameAlreadyBoundException e )
        {
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
        Set<String> results = new HashSet<String>();
    
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
        NamingEnumeration<SearchResult> ii = ctx.search( "", filter, controls );

        // collect all results
        Set<String> results = new HashSet<String>();

        while ( ii.hasMore() )
        {
            SearchResult result = ii.next();
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
            assertEquals( "ldap://localhost:" + port + "/cn=pierre-arnaud%20marcelot,l=paris,c=france,ou=system", referral );
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
}
