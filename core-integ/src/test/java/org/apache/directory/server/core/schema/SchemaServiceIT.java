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
package org.apache.directory.server.core.schema;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import org.apache.directory.server.core.integ.ServiceScope;
import org.apache.directory.server.core.integ.SetupMode;
import org.apache.directory.server.core.integ.annotations.Mode;
import org.apache.directory.server.core.integ.annotations.Scope;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;


/**
 * Test cases for the schema service.  This is for 
 * <a href="http://issues.apache.org/jira/browse/DIREVE-276">DIREVE-276</a>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class SchemaServiceIT
{
    public static DirectoryService service;


    public void loadData() throws Exception
    {
        // super.setLdifPath( "./nonspecific.ldif", getClass() );
    }


    /**
     * For <a href="https://issues.apache.org/jira/browse/DIRSERVER-925">DIRSERVER-925</a>.
     *
     * @throws NamingException on error
     */
    @Test
    public void testNoStructuralObjectClass() throws NamingException
    {
        Attributes attrs = new AttributesImpl( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
        attrs.get( SchemaConstants.OBJECT_CLASS_AT ).add( "uidObject" );
        attrs.put( SchemaConstants.UID_AT, "invalid" );
        
        try
        {
            getSystemContext( service ).createSubcontext( "uid=invalid", attrs );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.OBJECT_CLASS_VIOLATION, e.getResultCode() );
        }
    }
    
    
    /**
     * For <a href="https://issues.apache.org/jira/browse/DIRSERVER-925">DIRSERVER-925</a>.
     *
     * @throws NamingException on error
     */
    public void testMultipleStructuralObjectClasses() throws NamingException
    {
        Attributes attrs = new AttributesImpl( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
        attrs.get( SchemaConstants.OBJECT_CLASS_AT ).add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        attrs.get( SchemaConstants.OBJECT_CLASS_AT ).add( SchemaConstants.PERSON_OC );
        attrs.put( SchemaConstants.OU_AT, "comedy" );
        attrs.put( SchemaConstants.CN_AT, "Jack Black" );
        attrs.put( SchemaConstants.SN_AT, "Black" );
        
        try
        {
            getSystemContext( service ).createSubcontext( "cn=Jack Black", attrs );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.OBJECT_CLASS_VIOLATION, e.getResultCode() );
        }
    }
    
    
    /**
     * For <a href="https://issues.apache.org/jira/browse/DIRSERVER-904">DIRSERVER-904</a>.
     *
     * @throws NamingException on error
     */
    public void testAddingTwoDifferentEntitiesWithSameOid() throws NamingException
    {
        String numberOfGunsAttrLdif = "dn: m-oid=1.3.6.1.4.1.18060.0.4.1.2.999,ou=attributeTypes,cn=other,ou=schema\n" +
            "m-usage: USER_APPLICATIONS\n" +
            "m-equality: integerOrderingMatch\n" +
            "objectClass: metaAttributeType\n" +
            "objectClass: metaTop\n" +
            "objectClass: top\n" +
            "m-name: numberOfGuns\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.1.2.999\n" +
            "m-singleValue: TRUE\n" +
            "m-description: Number of guns of a ship\n" +
            "m-collective: FALSE\n" +
            "m-obsolete: FALSE\n" +
            "m-noUserModification: FALSE\n" +
            "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27\n";
        String shipOCLdif = "dn: m-oid=1.3.6.1.4.1.18060.0.4.1.2.999,ou=objectClasses,cn=other,ou=schema\n" +
            "objectClass: top\n" +
            "objectClass: metaTop\n" +
            "objectClass: metaObjectclass\n" +
            "m-supObjectClass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.1.2.999\n" +
            "m-name: ship\n" +
            "m-must: cn\n" +
            "m-may: numberOfGuns\n" +
            "m-may: description\n" +
            "m-typeObjectClass: STRUCTURAL\n" +
            "m-obsolete: FALSE\n" +
            "m-description: A ship\n";

        StringReader in = new StringReader( numberOfGunsAttrLdif + "\n\n" + shipOCLdif );
        LdifReader ldifReader = new LdifReader( in );
        Entry numberOfGunsAttrEntry = ldifReader.next();
        Entry shipOCEntry = ldifReader.next();
        assertFalse( ldifReader.hasNext() );
        
        // should be fine with unique OID
        LdapContext root = getRootContext( service );
        root.createSubcontext( numberOfGunsAttrEntry.getDn(), numberOfGunsAttrEntry.getAttributes() );
         
        // should blow chuncks using same OID
        //noinspection EmptyCatchBlock
        try
        {
            root.createSubcontext( shipOCEntry.getDn(), shipOCEntry.getAttributes() );
            fail( "Should not be possible to create two schema entities with the same OID." );
        }
        catch( NamingException e )
        {
        }
    }
    
    
    public void testFillInObjectClasses() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        Attribute ocs = sysRoot.getAttributes( "cn=person0" ).get( "objectClass" );
        assertEquals( 2, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );

        ocs = sysRoot.getAttributes( "cn=person1" ).get( "objectClass" );
        assertEquals( 3, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );

        ocs = sysRoot.getAttributes( "cn=person2" ).get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }


    public void testSearchForPerson() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(objectClass=person)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 4, persons.size() );

        Attributes person;
        Attribute ocs;

        person = persons.get( "cn=person0,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertEquals( 2, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );

        person = persons.get( "cn=person1,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertEquals( 3, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );

        person = persons.get( "cn=person2,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }


    public void testSearchForOrgPerson() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> orgPersons = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(objectClass=organizationalPerson)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            orgPersons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 3, orgPersons.size() );

        Attributes orgPerson;
        Attribute ocs;

        orgPerson = orgPersons.get( "cn=person1,ou=system" );
        assertNotNull( orgPerson );
        ocs = orgPerson.get( "objectClass" );
        assertEquals( 3, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );

        orgPerson = orgPersons.get( "cn=person2,ou=system" );
        assertNotNull( orgPerson );
        ocs = orgPerson.get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }


    public void testSearchForInetOrgPerson() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> inetOrgPersons = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(objectClass=inetOrgPerson)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            inetOrgPersons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 2, inetOrgPersons.size() );

        Attributes inetOrgPerson;
        Attribute ocs;

        inetOrgPerson = inetOrgPersons.get( "cn=person2,ou=system" );
        assertNotNull( inetOrgPerson );
        ocs = inetOrgPerson.get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }
    
    public void testSearchForSubSchemaSubEntryUserAttrsOnly() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( service ).search( "cn=schema", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema" );
        
        assertNotNull( attrs );
        
        // We should have 2 attributes in the result :
        // - attributeTypes
        // - cn
        // - objectClass
        assertEquals( 2, attrs.size() );
        
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "objectClass" ) );
    }

    public void testSearchForSubSchemaSubEntryAllAttrs() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ "+" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( service ).search(
                "cn=schema", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema" );
        
        assertNotNull( attrs );
        
        // We should have 14 attributes in the result :
        // - attributeTypes
        // - cn
        // - subtreeSpecification
        // - creatorsName
        // - createTimestamp
        // - dITContentRules
        // - dITStructureRules
        // - ldapSyntaxes
        // - matchingRules
        // - matchingRuleUse
        // - modifiersName
        // - modifyTimestamp
        // - nameForms
        // - objectClass
        // - objectClasses
        // - comparators
        // - normalizers
        // - syntaxCheckers
        assertEquals( 18, attrs.size() );
        
        assertNotNull( attrs.get( "attributeTypes" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNotNull( attrs.get( "createTimestamp" ) );
        assertNotNull( attrs.get( "dITContentRules" ) );
        assertNotNull( attrs.get( "dITStructureRules" ) );
        assertNotNull( attrs.get( "ldapSyntaxes" ) );
        assertNotNull( attrs.get( "matchingRules" ) );
        assertNotNull( attrs.get( "matchingRuleUse" ) );
        assertNotNull( attrs.get( "modifiersName" ) );
        assertNotNull( attrs.get( "modifyTimestamp" ) );
        assertNotNull( attrs.get( "nameForms" ) );
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "objectClasses" ) );
        assertNotNull( attrs.get( "subtreeSpecification" ) );
    }

    public void testSearchForSubSchemaSubEntrySingleAttributeSelected() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ "nameForms" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( service )
                .search( "cn=schema", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema" );
        
        assertNotNull( attrs );
        
        // We should have 1 attribute in the result :
        // - nameForms
        assertEquals( 1, attrs.size() );
        
        assertNull( attrs.get( "attributeTypes" ) );
        assertNull( attrs.get( "cn" ) );
        assertNull( attrs.get( "creatorsName" ) );
        assertNull( attrs.get( "createTimestamp" ) );
        assertNull( attrs.get( "dITContentRules" ) );
        assertNull( attrs.get( "dITStructureRules" ) );
        assertNull( attrs.get( "ldapSyntaxes" ) );
        assertNull( attrs.get( "matchingRules" ) );
        assertNull( attrs.get( "matchingRuleUse" ) );
        assertNull( attrs.get( "modifiersName" ) );
        assertNull( attrs.get( "modifyTimestamp" ) );
        assertNotNull( attrs.get( "nameForms" ) );
        assertNull( attrs.get( "objectClass" ) );
        assertNull( attrs.get( "objectClasses" ) );
    }

    public void testSearchForSubSchemaSubEntryBadFilter() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ "+" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( service )
                .search( "cn=schema", "(objectClass=nothing)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }

    public void testSearchForSubSchemaSubEntryFilterEqualTop() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ "+" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( service )
                .search( "cn=schema", "(objectClass=top)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema" );
        
        assertNotNull( attrs );
        
        // We should have 18 attribute in the result :
        // - nameForms
        // - comparators
        // - normalizers
        // - syntaxCheckers
        assertEquals( 18, attrs.size() );
        
        assertNotNull( attrs.get( "attributeTypes" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "subtreeSpecification" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNotNull( attrs.get( "createTimestamp" ) );
        assertNotNull( attrs.get( "dITContentRules" ) );
        assertNotNull( attrs.get( "dITStructureRules" ) );
        assertNotNull( attrs.get( "ldapSyntaxes" ) );
        assertNotNull( attrs.get( "matchingRules" ) );
        assertNotNull( attrs.get( "matchingRuleUse" ) );
        assertNotNull( attrs.get( "modifiersName" ) );
        assertNotNull( attrs.get( "modifyTimestamp" ) );
        assertNotNull( attrs.get( "nameForms" ) );
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "objectClasses" ) );
    }

    public void testSearchForSubSchemaSubEntryFilterEqualSubSchema() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ "+" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( service )
                .search( "cn=schema", "(objectClass=subSchema)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema" );
        
        assertNotNull( attrs );
        
        // We should have 18 attribute in the result :
        // - nameForms
        // - comparators
        // - normalizers
        // - syntaxCheckers
        assertEquals( 18, attrs.size() );
        
        assertNotNull( attrs.get( "attributeTypes" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "subtreeSpecification" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNotNull( attrs.get( "createTimestamp" ) );
        assertNotNull( attrs.get( "dITContentRules" ) );
        assertNotNull( attrs.get( "dITStructureRules" ) );
        assertNotNull( attrs.get( "ldapSyntaxes" ) );
        assertNotNull( attrs.get( "matchingRules" ) );
        assertNotNull( attrs.get( "matchingRuleUse" ) );
        assertNotNull( attrs.get( "modifiersName" ) );
        assertNotNull( attrs.get( "modifyTimestamp" ) );
        assertNotNull( attrs.get( "nameForms" ) );
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "objectClasses" ) );
    }

    public void testSearchForSubSchemaSubEntryNotObjectScope() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{ "+" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( service )
                .search( "cn=schema", "(objectClass=nothing)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }

    public void testSearchForSubSchemaSubEntryComposedFilters() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{ "+" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( service )
                .search( "cn=schema", "(&(objectClass=*)(objectClass=top))", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }
    
    /**
     * Test for DIRSERVER-844: storing of base 64 encoded values into H-R attributes
     *
     * @throws NamingException on error
     */
    public void testSearchSeeAlso() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getSystemContext( service )
                    .search( "", "(seeAlso=cn=Good One,ou=people,o=sevenSeas)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 1, persons.size() );

        Attributes person;
        Attribute ocs;

        person = persons.get( "cn=person1,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertEquals( 3, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );

        Attribute seeAlso = person.get(  "seeAlso"  );
        assertTrue( seeAlso.contains( "cn=Good One,ou=people,o=sevenSeas" ) );
        assertTrue( seeAlso.contains( "cn=Bad E\u00e9k\u00e0,ou=people,o=sevenSeas" ) );
    }

    
    /**
     * Doing a search with filtering attributes should work even if the attribute
     * is not valid 
     *
     * @throws NamingException on error
     */
    public void testSearchForUnknownAttributes() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        controls.setReturningAttributes( new String[] { "9.9.9" } );

        NamingEnumeration<SearchResult> results = getSystemContext( service )
                .search( "", "(objectClass=person)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 4, persons.size() );

        Attributes person;
        Attribute ocs;

        person = persons.get( "cn=person0,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );
        
        ocs = person.get( "9.9.9" );
        assertNull( ocs );

        person = persons.get( "cn=person1,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );

        person = persons.get( "cn=person2,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );
    }

    
    /**
     * Check that if we request a Attribute which is not an AttributeType,
     * we still get a result
     *
     * @throws NamingException on error
     */
    public void testSearchAttributesOIDObjectClass() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        controls.setReturningAttributes( new String[] { "2.5.6.6" } );

        NamingEnumeration<SearchResult> results = getSystemContext( service )
                .search( "", "(objectClass=person)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 4, persons.size() );

        Attributes person;
        Attribute ocs;

        person = persons.get( "cn=person0,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );
        
        // We should not get this attribute (it's an ObjectClass)
        ocs = person.get( "2.5.6.6" );
        assertNull( ocs );

        person = persons.get( "cn=person1,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );

        person = persons.get( "cn=person2,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );
    }

    
    /**
     * Check that if we request a Attribute which is an ObjectClass.
     *
     * @throws NamingException on error
     */
    public void testSearchAttributesOIDObjectClassName() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        controls.setReturningAttributes( new String[] { "person" } );

        NamingEnumeration<SearchResult> results = getSystemContext( service )
                .search( "", "(objectClass=person)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 4, persons.size() );

        Attributes person;
        Attribute ocs;

        person = persons.get( "cn=person0,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );
        
        // We should not get this attrinute (it's an ObjectClass)
        ocs = person.get( "2.5.4.46" );
        assertNull( ocs );

        person = persons.get( "cn=person1,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );

        person = persons.get( "cn=person2,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertNull( ocs );
    }
}
