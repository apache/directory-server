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
package org.apache.directory.server;


import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;


/**
 * Testcase with different modify operations on a person entry. Each includes a
 * single add op only. Created to demonstrate DIREVE-241 ("Adding an already
 * existing attribute value with a modify operation does not cause an error.").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchTest extends AbstractServerTest
{
    private LdapContext ctx = null;
    public static final String RDN = "cn=Tori Amos";
    public static final String RDN2 = "cn=Rolling-Stones";
    public static final String PERSON_DESCRIPTION = "an American singer-songwriter";


    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new BasicAttributes();
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );

        return attributes;
    }


    /**
     * Create context and a person entry.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );

        ctx = new InitialLdapContext( env, null );
        assertNotNull( ctx );

        // Create a person with description
        Attributes attributes = this.getPersonAttributes( "Amos", "Tori Amos" );
        attributes.put( "description", "an American singer-songwriter" );
        ctx.createSubcontext( RDN, attributes );

        // Create a second person with description
        attributes = this.getPersonAttributes( "Jagger", "Rolling-Stones" );
        attributes.put( "description", "an English singer-songwriter" );
        ctx.createSubcontext( RDN2, attributes );
        
    }


    /**
     * Remove person entry and close context.
     */
    public void tearDown() throws Exception
    {
        ctx.unbind( RDN );
        ctx.close();
        ctx = null;
        super.tearDown();
    }
    
    
    /**
     * Performs a single level search from ou=system base and 
     * returns the set of DNs found.
     */
    private Set search( String filter ) throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration ii = ctx.search( "", filter, controls );
        
        // collect all results 
        HashSet results = new HashSet();
        while ( ii.hasMore() )
        {
            SearchResult result = ( SearchResult ) ii.next();
            results.add( result.getName() );
        }
        
        return results;
    }

    
    public void testDirserver635() throws NamingException
    {
        // create additional entry
        Attributes attributes = this.getPersonAttributes( "Bush", "Kate Bush" );
        ctx.createSubcontext( "cn=Kate Bush", attributes );

        // -------------------------------------------------------------------
        Set results = search( "(|(cn=Kate*)(cn=Tori*))" );
        assertEquals( "returned size of results", 2, results.size() );
        assertTrue( "contains cn=Tori Amos", results.contains( "cn=Tori Amos" ) );
        assertTrue( "contains cn=Kate Bush", results.contains( "cn=Kate Bush" ) );

        // -------------------------------------------------------------------
        results = search( "(|(cn=*Amos)(cn=Kate*))" );
        assertEquals( "returned size of results", 2, results.size() );
        assertTrue( "contains cn=Tori Amos", results.contains( "cn=Tori Amos" ) );
        assertTrue( "contains cn=Kate Bush", results.contains( "cn=Kate Bush" ) );

        // -------------------------------------------------------------------
        results = search( "(|(cn=Kate Bush)(cn=Tori*))" );
        assertEquals( "returned size of results", 2, results.size() );
        assertTrue( "contains cn=Tori Amos", results.contains( "cn=Tori Amos" ) );
        assertTrue( "contains cn=Kate Bush", results.contains( "cn=Kate Bush" ) );

        // -------------------------------------------------------------------
        results = search( "(|(cn=*Amos))" );
        assertEquals( "returned size of results", 1, results.size() );
        assertTrue( "contains cn=Tori Amos", results.contains( "cn=Tori Amos" ) );
    }

    
    /**
     * Search operation with a base DN which contains a BER encoded value.
     */
    public void testSearchBEREncodedBase() throws NamingException
    {
        // create additional entry
        Attributes attributes = this.getPersonAttributes( "Ferry", "Bryan Ferry" );
        ctx.createSubcontext( "sn=Ferry", attributes );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String filter = "(cn=Bryan Ferry)";

        // sn=Ferry with BEROctetString values
        String base = "sn=#4665727279";

        try
        {
            // Check entry
            NamingEnumeration enm = ctx.search( base, filter, sctls );
            assertTrue( enm.hasMore() );
            while ( enm.hasMore() )
            {
                SearchResult sr = ( SearchResult ) enm.next();
                Attributes attrs = sr.getAttributes();
                Attribute sn = attrs.get( "sn" );
                assertNotNull( sn );
                assertTrue( sn.contains( "Ferry" ) );
            }
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }
    }

    
    /**
     * Search operation with a base DN which contains a BER encoded value.
     */
    public void testSearchWithBackslashEscapedBase() throws NamingException
    {
        // create additional entry
        Attributes attributes = this.getPersonAttributes( "Ferry", "Bryan Ferry" );
        ctx.createSubcontext( "sn=Ferry", attributes );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String filter = "(cn=Bryan Ferry)";

        // sn=Ferry with BEROctetString values
        String base = "sn=\\46\\65\\72\\72\\79";

        try
        {
            // Check entry
            NamingEnumeration enm = ctx.search( base, filter, sctls );
            assertTrue( enm.hasMore() );
            while ( enm.hasMore() )
            {
                SearchResult sr = ( SearchResult ) enm.next();
                Attributes attrs = sr.getAttributes();
                Attribute sn = attrs.get( "sn" );
                assertNotNull( sn );
                assertTrue( sn.contains( "Ferry" ) );
            }
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }
    }

    
    /**
     * Add a new attribute to a person entry.
     * 
     * @throws NamingException
     */
    public void testSearchValue() throws NamingException
    {
        // Setting up search controls for compare op
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes( new String[]
            { "*" } ); // no attributes
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );

        // Search for all entries
        NamingEnumeration results = ctx.search( RDN, "(cn=*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*)", ctls );
        assertTrue( results.hasMore() );

        // Search for all entries ending by Amos
        results = ctx.search( RDN, "(cn=*Amos)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*Amos)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries ending by amos
        results = ctx.search( RDN, "(cn=*amos)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*amos)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries starting by Tori
        results = ctx.search( RDN, "(cn=Tori*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=Tori*)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries starting by tori
        results = ctx.search( RDN, "(cn=tori*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=tori*)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries containing ori
        results = ctx.search( RDN, "(cn=*ori*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*ori*)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries containing o and i
        results = ctx.search( RDN, "(cn=*o*i*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*o*i*)", ctls );
        assertTrue( results.hasMore() );

        // Search for all entries containing o, space and o
        results = ctx.search( RDN, "(cn=*o* *o*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*o* *o*)", ctls );
        assertFalse( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*o*-*o*)", ctls );
        assertTrue( results.hasMore() );

        // Search for all entries starting by To and containing A
        results = ctx.search( RDN, "(cn=To*A*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=To*A*)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries ending by os and containing ri
        results = ctx.search( RDN, "(cn=*ri*os)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*ri*os)", ctls );
        assertFalse( results.hasMore() );
    }
    
    /**
     * Search operation with a base DN with quotes
     */
    public void testSearchWithQuotesInBase() throws NamingException {

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope(SearchControls.OBJECT_SCOPE);
        String filter = "(cn=Tori Amos)";

        // sn="Kylie Minogue" (with quotes)
        String base = "cn=\"Tori Amos\"";

        try {
            // Check entry
            NamingEnumeration enm = ctx.search( base, filter, sctls );
            assertTrue( enm.hasMore() );
            
            while ( enm.hasMore() ) {
                SearchResult sr = (SearchResult) enm.next();
                Attributes attrs = sr.getAttributes();
                Attribute sn = attrs.get("sn");
                assertNotNull(sn);
                assertTrue( sn.contains( "Amos" ) );
            }
        } catch (Exception e) {
            fail( e.getMessage() );
        }
    }
 
    
    /**
     * Tests for <a href="http://issues.apache.org/jira/browse/DIRSERVER-645">
     * DIRSERVER-645<\a>: Wrong search filter evaluation with AND 
     * operator and undefined operands.
     */
    public void testUndefinedAvaInBranchFilters() throws Exception
    {
        // create additional entry
        Attributes attributes = this.getPersonAttributes( "Bush", "Kate Bush" );
        ctx.createSubcontext( "cn=Kate Bush", attributes );

        // -------------------------------------------------------------------
        Set results = search( "(|(sn=Bush)(numberOfOctaves=4))" );
        assertEquals( "returned size of results", 1, results.size() );
        assertTrue( "contains cn=Kate Bush", results.contains( "cn=Kate Bush" ) );

        // if numberOfOctaves is undefined then this whole filter is undefined
        results = search( "(&(sn=Bush)(numberOfOctaves=4))" );
        assertEquals( "returned size of results", 0, results.size() );
    }
    
    
    public void testSearchSchema() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[] { "objectClasses" } );
        
        NamingEnumeration results = ctx.search( "cn=schema", "objectClass=subschema", controls );
        assertTrue( results.hasMore() );
        SearchResult result = ( SearchResult ) results.next();
        assertNotNull( result );
        assertFalse( results.hasMore() );
        assertNotNull( result.getAttributes().get( "objectClasses" ) );
        assertEquals( 1, result.getAttributes().size() );
    }
}
