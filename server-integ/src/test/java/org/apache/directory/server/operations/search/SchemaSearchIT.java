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
package org.apache.directory.server.operations.search;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case with different search operations on the cn=schema entry. 
 * Created to demonstrate DIRSERVER-1055
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" )
    })
@ApplyLdifs( {
    
    // Bogus AD schema (not real)
    
    "dn: cn=active-directory, ou=schema",
    "objectclass: metaSchema",
    "objectclass: top",
    "cn: active-directory",
    "m-dependencies: core",
    
    "dn: ou=attributeTypes, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: attributeTypes",
    
    "dn: m-oid=1.1, ou=attributeTypes, cn=active-directory, ou=schema",
    "objectclass: metaAttributeType",
    "objectclass: metaTop",
    "objectclass: top",
    "m-oid: 1.1",
    "m-name: sAMAccountName",
    "m-syntax: 1.3.6.1.4.1.1466.115.121.1.15",
    
    "dn: m-oid=1.2, ou=attributeTypes, cn=active-directory, ou=schema",
    "objectclass: metaAttributeType",
    "objectclass: metaTop",
    "objectclass: top",
    "m-oid: 1.2",
    "m-name: pwdLastSet",
    "m-equality: integerMatch",
    "m-ordering: integerMatch",
    "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",

    "dn: m-oid=1.4, ou=attributeTypes, cn=active-directory, ou=schema",
    "objectclass: metaAttributeType",
    "objectclass: metaTop",
    "objectclass: top",
    "m-oid: 1.4",
    "m-name: useraccountcontrol",
    "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",

    "dn: m-oid=1.5, ou=attributeTypes, cn=active-directory, ou=schema",
    "objectclass: metaAttributeType",
    "objectclass: metaTop",
    "objectclass: top",
    "m-oid: 1.5",
    "m-name: SourceAD",
    "m-syntax: 1.3.6.1.4.1.1466.115.121.1.15",
    "m-length: 0",

    "dn: ou=comparators, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: comparators",

    "dn: ou=ditContentRules, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: ditContentRules",

    "dn: ou=ditStructureRules, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: ditStructureRules",

    "dn: ou=matchingRules, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: matchingRules",
    
    "dn: ou=nameForms, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: nameForms",

    "dn: ou=normalizers, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: normalizers",

    "dn: ou=objectClasses, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: objectClasses",

    "dn: m-oid=1.3, ou=objectClasses, cn=active-directory, ou=schema",
    "objectclass: metaObjectClass",
    "objectclass: metaTop",
    "objectclass: top",
    "m-oid: 1.3",
    "m-name: personActiveDirectory",
    "m-supObjectClass: person",
    "m-must: pwdLastSet",
    "m-must: sAMAccountName",
    "m-must: useraccountcontrol",
    "m-must: SourceAD",

    "dn: ou=syntaxCheckers, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: syntaxCheckers",

    "dn: ou=syntaxes, cn=active-directory, ou=schema",
    "objectclass: organizationalUnit",
    "objectclass: top",
    "ou: syntaxes"
    }
)
public class SchemaSearchIT extends AbstractLdapTestUnit 
{
    private static final String DN = "cn=schema";
    private static final String FILTER = "(objectclass=subschema)";


    protected void checkForAttributes( Attributes attrs, String[] attrNames )
    {
        for ( int i = 0; i < attrNames.length; i++ )
        {
            String attrName = attrNames[i];

            assertNotNull( "Check if attr " + attrName + " is present", attrs.get( attrNames[i] ) );
        }
    }


    /**
     * Check if modifyTimestamp and createTimestamp are present in the search result,
     * if they are requested.
     */
    @Test
    public void testRequestOperationalAttributes() throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );
        SearchControls ctls = new SearchControls();

        String[] attrNames =
            { "creatorsName", "createTimestamp", "modifiersName", "modifyTimestamp" };

        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( attrNames );

        NamingEnumeration<SearchResult> result = ctx.search( DN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            checkForAttributes( entry.getAttributes(), attrNames );
        }
        else
        {
            fail( "entry " + DN + " not found" );
        }

        result.close();
    }


    /**
     * Check if modifyTimestamp and createTimestamp are present in the search result,
     * if + is requested.
     */
    @Test
    public void testRequestAllOperationalAttributes() throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );
        SearchControls ctls = new SearchControls();

        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "+" } );

        NamingEnumeration<SearchResult> result = ctx.search( DN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            String[] attrNames =
                { "creatorsName", "createTimestamp", "modifiersName", "modifyTimestamp" };
            checkForAttributes( entry.getAttributes(), attrNames );
        }
        else
        {
            fail( "entry " + DN + " not found" );
        }

        result.close();
    }

    
    /**
     * Test case for DIRSERVER-1083: Search on an custom attribute added to 
     * the dynamic schema fails when no result is found. 
     */
    @Test
    public void testSearchingNewSchemaElements() throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );
        
        // create an entry with the schema objectClass personActiveDirectory
        Attributes person = new BasicAttributes( "objectClass", "top", true );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "personActiveDirectory" );
        person.put( "cn", "foobar" );
        person.put( "sn", "bar" );
        person.put( "pwdLastSet", "3" );
        person.put( "SourceAD", "blah" );
        person.put( "useraccountcontrol", "7" );
        person.put( "sAMAccountName", "foobar" );
        ctx.createSubcontext( "cn=foobar,ou=system", person );
        
        // Confirm creation with a lookup
        Attributes read = ctx.getAttributes( "cn=foobar,ou=system" );
        assertNotNull( read );
        assertEquals( "3", read.get( "pwdLastSet" ).get() );
        
        // Now search for foobar with pwdLastSet value of 3
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> results = ctx.search( "ou=system", "(pwdLastSet=3)", searchControls );
        assertTrue( results.hasMore() );
        SearchResult result = results.next();
        assertNotNull( result );
        assertEquals( "cn=foobar", result.getName() );
        Attributes attributes = result.getAttributes();
        assertEquals( "3", attributes.get( "pwdLastSet" ).get() );
        results.close();
        
        // Now search with bogus value for pwdLastSet
        results = ctx.search( "ou=system", "(pwdLastSet=300)", searchControls );
        assertFalse( results.hasMore() );
        results.close();
    }
    
    
    /**
     * Test case for DIRSERVER-: Ensure that schema entry is returned, 
     * even if no ManageDsaIT decorator is present in the search request.
     */
    @Test
    public void testRequestWithoutManageDsaITControl() throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );

        // this removes the ManageDsaIT decorator from the search request
        ctx.addToEnvironment( DirContext.REFERRAL, "throw" );

        SearchControls ctls = new SearchControls();
        String[] attrNames =
            { "objectClasses", "attributeTypes", "ldapSyntaxes", "matchingRules", "matchingRuleUse", "createTimestamp",
                "modifyTimestamp" };
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( attrNames );

        NamingEnumeration<SearchResult> result = ctx.search( DN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            checkForAttributes( entry.getAttributes(), attrNames );
        }
        else
        {
            fail( "entry " + DN + " not found" );
        }

        result.close();
    }

    
    /**
     * Test a search done on cn=schema 
     */
    @Test
    public void testSubSchemaSubEntrySearch() throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.OBJECT_SCOPE );
        searchControls.setReturningAttributes( new String[]
            { "objectClasses" } );
        NamingEnumeration<SearchResult> results = ctx.search( "cn=schema", "(ObjectClass=*)", searchControls );

        assertTrue( results.hasMore() );
        SearchResult result = results.next();
        Attributes entry = result.getAttributes();

        Attribute objectClasses = entry.get( "objectClasses" );
        NamingEnumeration<?> ocs = objectClasses.getAll();

        while ( ocs.hasMore() )
        {
            String oc = ( String ) ocs.nextElement();
            if ( oc.contains( "2.5.6.6" ) )
            {
                assertEquals(
                    "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top STRUCTURAL MUST ( sn $ cn ) MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) X-SCHEMA 'core' )",
                    oc );
            }
        }

        results.close();
    }
}
