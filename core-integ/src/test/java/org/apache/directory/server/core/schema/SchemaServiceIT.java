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


import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.parsers.AttributeTypeDescriptionSchemaParser;
import org.apache.directory.api.ldap.util.JndiUtils;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the schema getService().  This is for 
 * <a href="http://issues.apache.org/jira/browse/DIREVE-276">DIREVE-276</a>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=person0,ou=system",
        "objectClass: person",
        "cn: person0",
        "sn: sn_person0\n",
        // Entry # 2
        "dn: cn=person1,ou=system",
        "objectClass: organizationalPerson",
        "cn: person1",
        "sn: sn_person1",
        "seealso: cn=Good One,ou=people,o=sevenSeas",
        //"seealso:: Y249QmFkIEXDqWvDoCxvdT1wZW9wbGUsbz1zZXZlblNlYXM=\n",
        // Entry # 3
        "dn: cn=person2,ou=system",
        "objectClass: inetOrgPerson",
        "cn: person2",
        "sn: sn_person2" })
@RunWith(FrameworkRunner.class)
@CreateDS(name = "SchemaServiceIT")
public class SchemaServiceIT extends AbstractLdapTestUnit
{

    /**
     * For <a href="https://issues.apache.org/jira/browse/DIRSERVER-925">DIRSERVER-925</a>.
     *
     * @throws NamingException on error
     */
    @Test
    public void testNoStructuralObjectClass() throws Exception
    {
        Attributes attrs = new BasicAttributes( "objectClass", "top", true );
        attrs.get( "objectClass" ).add( "uidObject" );
        attrs.put( "uid", "invalid" );

        try
        {
            getSystemContext( getService() ).createSubcontext( "uid=invalid", attrs );
        }
        catch ( SchemaViolationException e )
        {
        }
    }


    /**
     * For <a href="https://issues.apache.org/jira/browse/DIRSERVER-925">DIRSERVER-925</a>.
     *
     * @throws NamingException on error
     */
    @Test
    public void testMultipleStructuralObjectClasses() throws Exception
    {
        Attributes attrs = new BasicAttributes( "objectClass", "top", true );
        attrs.get( "objectClass" ).add( "organizationalUnit" );
        attrs.get( "objectClass" ).add( "person" );
        attrs.put( "ou", "comedy" );
        attrs.put( "cn", "Jack Black" );
        attrs.put( "sn", "Black" );

        try
        {
            getSystemContext( getService() ).createSubcontext( "cn=Jack Black", attrs );
        }
        catch ( SchemaViolationException e )
        {
        }
    }


    /**
     * For <a href="https://issues.apache.org/jira/browse/DIRSERVER-904">DIRSERVER-904</a>.
     *
     * @throws NamingException on error
     */
    @Test
    public void testAddingTwoDifferentEntitiesWithSameOid() throws Exception
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
        LdifEntry numberOfGunsAttrEntry = ldifReader.next();
        LdifEntry shipOCEntry = ldifReader.next();
        assertFalse( ldifReader.hasNext() );
        ldifReader.close();

        // should be fine with unique OID
        getService().getAdminSession().add(
            new DefaultEntry( getService().getSchemaManager(), numberOfGunsAttrEntry.getEntry() ) );

        // should blow chuncks using same OID
        try
        {
            getService().getAdminSession().add(
                new DefaultEntry( getService().getSchemaManager(), shipOCEntry.getEntry() ) );

            fail( "Should not be possible to create two schema entities with the same OID." );
        }
        catch ( LdapOtherException e )
        {
            assertTrue( true );
        }
    }


    /**
     * Test that we have all the needed ObjectClasses
     * 
     * @throws NamingException on error
     */
    @Test
    public void testFillInObjectClasses() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
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


    /**
     * Search all the entries with a 'person' ObjectClass, or an ObjectClass
     * inheriting from 'person' 
     *
     * @throws NamingException on error
     */
    @Test
    public void testSearchForPerson() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(objectClass=*person)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // admin is extra
        assertEquals( 4, persons.size() );

        Attributes person = persons.get( "cn=person0,ou=system" );
        assertNotNull( person );
        Attribute ocs = person.get( "objectClass" );
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


    @Test
    public void testSearchForOrgPerson() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> orgPersons = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(objectClass=organizationalPerson)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            orgPersons.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // admin is extra
        assertEquals( 3, orgPersons.size() );

        Attributes orgPerson = orgPersons.get( "cn=person1,ou=system" );
        assertNotNull( orgPerson );
        Attribute ocs = orgPerson.get( "objectClass" );
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


    @Test
    public void testSearchForInetOrgPerson() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> inetOrgPersons = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(objectClass=inetOrgPerson)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            inetOrgPersons.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // admin is extra
        assertEquals( 2, inetOrgPersons.size() );

        Attributes inetOrgPerson = inetOrgPersons.get( "cn=person2,ou=system" );
        assertNotNull( inetOrgPerson );
        Attribute ocs = inetOrgPerson.get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }


    @Test
    public void testSearchForSubSchemaSubEntryUserAttrsOnly() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() ).search( "cn=schema",
            "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "cn=schema" );

        assertNotNull( attrs );

        // We should have 2 attributes in the result :
        // - attributeTypes
        // - cn
        // - objectClass
        assertEquals( 2, attrs.size() );

        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "objectClass" ) );
    }


    @Test
    public void testSearchForSubSchemaSubEntryAllAttrs() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "*", "+" } );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() ).search(
            "cn=schema", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "cn=schema" );

        assertNotNull( attrs );

        // We should not have any NameForms
        assertNull( attrs.get( "nameForms" ) );
    }


    @Test
    public void testSearchForSubSchemaSubEntrySingleAttributeSelected() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "objectClasses" } );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() )
            .search( "cn=schema", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "cn=schema" );

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
        assertNotNull( attrs.get( "objectClasses" ) );
        assertNull( attrs.get( "objectClass" ) );
        assertNull( attrs.get( "nameForms" ) );
    }


    /**
     * Test for DIRSERVER-1055.
     * Check if modifyTimestamp and createTimestamp are present in the search result,
     * if they are requested.
     */
    @Test
    public void testSearchForSubSchemaSubEntryOperationalAttributesSelected() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "creatorsName", "createTimestamp", "modifiersName", "modifyTimestamp" } );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() )
            .search( "cn=schema", "(objectClass=subschema)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "cn=schema" );

        assertNotNull( attrs );

        // We should have 4 attribute in the result :
        assertEquals( 4, attrs.size() );

        assertNull( attrs.get( "attributeTypes" ) );
        assertNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNotNull( attrs.get( "createTimestamp" ) );
        assertNull( attrs.get( "dITContentRules" ) );
        assertNull( attrs.get( "dITStructureRules" ) );
        assertNull( attrs.get( "ldapSyntaxes" ) );
        assertNull( attrs.get( "matchingRules" ) );
        assertNull( attrs.get( "matchingRuleUse" ) );
        assertNotNull( attrs.get( "modifiersName" ) );
        assertNotNull( attrs.get( "modifyTimestamp" ) );
        assertNull( attrs.get( "nameForms" ) );
        assertNull( attrs.get( "objectClass" ) );
        assertNull( attrs.get( "objectClasses" ) );
    }


    @Test
    public void testSearchForSubSchemaSubEntryBadFilter() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+" } );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() )
            .search( "cn=schema", "(objectClass=nothing)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }


    @Test
    public void testSearchForSubSchemaSubEntryFilterEqualTop() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "*", "+" } );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() )
            .search( "cn=schema", "(objectClass=top)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "cn=schema" );

        assertNotNull( attrs );

        // We should have 14 attributes in the result :
        assertEquals( 14, attrs.size() );

        assertNotNull( attrs.get( "attributeTypes" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "comparators" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNotNull( attrs.get( "createTimestamp" ) );
        assertNull( attrs.get( "dITContentRules" ) );
        assertNull( attrs.get( "dITStructureRules" ) );
        assertNotNull( attrs.get( "ldapSyntaxes" ) );
        assertNotNull( attrs.get( "matchingRules" ) );
        assertNull( attrs.get( "matchingRuleUse" ) );
        assertNotNull( attrs.get( "modifiersName" ) );
        assertNotNull( attrs.get( "modifyTimestamp" ) );
        assertNull( attrs.get( "nameForms" ) );
        assertNotNull( attrs.get( "normalizers" ) );
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "objectClasses" ) );
        assertNotNull( attrs.get( "subtreeSpecification" ) );
        assertNotNull( attrs.get( "syntaxCheckers" ) );
    }


    @Test
    public void testSearchForSubSchemaSubEntryFilterEqualSubSchema() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "*", "+" } );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() )
            .search( "cn=schema", "(objectClass=subSchema)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Attributes attrs = subSchemaEntry.get( "cn=schema" );

        assertNotNull( attrs );

        // We should have 14 attributes in the result :
        assertEquals( 14, attrs.size() );

        assertNotNull( attrs.get( "attributeTypes" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "subtreeSpecification" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNotNull( attrs.get( "createTimestamp" ) );
        assertNull( attrs.get( "dITContentRules" ) );
        assertNull( attrs.get( "dITStructureRules" ) );
        assertNotNull( attrs.get( "ldapSyntaxes" ) );
        assertNotNull( attrs.get( "matchingRules" ) );
        assertNull( attrs.get( "matchingRuleUse" ) );
        assertNotNull( attrs.get( "modifiersName" ) );
        assertNotNull( attrs.get( "modifyTimestamp" ) );
        assertNull( attrs.get( "nameForms" ) );
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "objectClasses" ) );
    }


    @Test
    public void testSearchForSubSchemaSubEntryNotObjectScope() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+" } );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() )
            .search( "cn=schema", "(objectClass=nothing)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }


    @Test
    public void testSearchForSubSchemaSubEntryComposedFilters() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+" } );

        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getRootContext( getService() )
            .search( "cn=schema", "(&(objectClass=*)(objectClass=top))", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        results.close();

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }


    /**
     * Test for DIRSERVER-844: storing of base 64 encoded values into H-R attributes
     *
     * @throws NamingException on error
     */
    @Test
    public void testSearchSeeAlso() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> results = getSystemContext( getService() )
            .search( "", "(seeAlso=cn=Good One,ou=people,o=sevenSeas)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        results.close();

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

        Attribute seeAlso = person.get( "seeAlso" );
        assertTrue( seeAlso.contains( "cn=Good One,ou=people,o=sevenSeas" ) );
        //assertTrue( seeAlso.contains( "cn=Bad E\u00e9k\u00e0,ou=people,o=sevenSeas" ) );
    }


    /**
     * Doing a search with filtering attributes should work even if the attribute
     * is not valid 
     *
     * @throws NamingException on error
     */
    @Test
    public void testSearchForUnknownAttributes() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        controls.setReturningAttributes( new String[]
            { "9.9.9" } );

        NamingEnumeration<SearchResult> results = getSystemContext( getService() )
            .search( "", "(objectClass=person)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        results.close();

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
    @Test
    public void testSearchAttributesOIDObjectClass() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        controls.setReturningAttributes( new String[]
            { "2.5.6.6" } );

        NamingEnumeration<SearchResult> results = getSystemContext( getService() )
            .search( "", "(objectClass=person)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        results.close();

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
    @Test
    public void testSearchAttributesOIDObjectClassName() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        controls.setReturningAttributes( new String[]
            { "person" } );

        NamingEnumeration<SearchResult> results = getSystemContext( getService() )
            .search( "", "(objectClass=person)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        results.close();

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


    /**
     * Check that if we search for an attribute using its inherited
     * AttributeType (ie, looking for name instead of givenName, surname, 
     * commonName), we find all the entries.
     *
     * @throws NamingException
     */
    @Test
    public void testSearchForName() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(name=person*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        results.close();

        assertEquals( 3, persons.size() );

        Attributes person = persons.get( "cn=person0,ou=system" );
        assertNotNull( person );
        Attribute ocs = person.get( "objectClass" );
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


    /**
     * Search all the entries with a 'metaTop' ObjectClass, or an ObjectClass
     * inheriting from 'metaTop' 
     *
     * @throws NamingException on error
     */
    @Test
    public void testSearchForMetaTop() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( getService() );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> results = schemaRoot.search( "", "(objectClass=top)", controls );
        assertTrue( "Expected some results", results.hasMore() );
        results.close();

        controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        results = schemaRoot.search( "", "(objectClass=metaAttributeType)", controls );
        assertTrue( "Expected some results", results.hasMore() );
        results.close();

        controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        results = schemaRoot.search( "", "(objectClass=metaTop)", controls );
        assertTrue( "Expected some results", results.hasMore() );
        results.close();
    }

    // -----------------------------------------------------------------------
    // Private Utility Methods
    // -----------------------------------------------------------------------
    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";
    private static final AttributeTypeDescriptionSchemaParser ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER = new AttributeTypeDescriptionSchemaParser();


    private void modify( int op, List<String> descriptions, String opAttr ) throws Exception
    {
        Dn dn = new Dn( getSubschemaSubentryDN() );
        Attribute attr = new BasicAttribute( opAttr );

        for ( String description : descriptions )
        {
            attr.add( description );
        }

        Attributes mods = new BasicAttributes( true );
        mods.put( attr );

        getRootContext( getService() ).modifyAttributes( JndiUtils.toName( dn ), op, mods );
    }


    /**
     * Gets the subschemaSubentry attributes for the global schema.
     *
     * @return all operational attributes of the subschemaSubentry
     * @throws NamingException if there are problems accessing this entry
     */
    private Attributes getSubschemaSubentryAttributes() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );

        NamingEnumeration<SearchResult> results = getRootContext( getService() ).search( getSubschemaSubentryDN(),
            "(objectClass=*)", controls );
        SearchResult result = results.next();
        results.close();
        return result.getAttributes();
    }


    /**
     * Get's the subschemaSubentry attribute value from the rootDSE.
     *
     * @return the subschemaSubentry distinguished name
     * @throws NamingException if there are problems accessing the RootDSE
     */
    private String getSubschemaSubentryDN() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { SUBSCHEMA_SUBENTRY } );

        NamingEnumeration<SearchResult> results = getRootContext( getService() ).search( "", "(objectClass=*)",
            controls );
        SearchResult result = results.next();
        results.close();
        Attribute subschemaSubentry = result.getAttributes().get( SUBSCHEMA_SUBENTRY );
        return ( String ) subschemaSubentry.get();
    }


    private void enableSchema( String schemaName ) throws Exception
    {
        // now enable the test schema
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-disabled", "FALSE" );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( getService() ).modifyAttributes( "cn=" + schemaName, mods );
    }


    private void checkAttributeTypePresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------

        Attributes attrs = getSubschemaSubentryAttributes();
        Attribute attrTypes = attrs.get( "attributeTypes" );
        AttributeType attributeType = null;

        for ( int ii = 0; ii < attrTypes.size(); ii++ )
        {
            String desc = ( String ) attrTypes.get( ii );

            if ( desc.indexOf( oid ) != -1 )
            {
                attributeType = ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER
                    .parseAttributeTypeDescription( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( attributeType );
            assertEquals( oid, attributeType.getOid() );
        }
        else
        {
            assertNull( attributeType );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------

        attrs = null;

        if ( isPresent )
        {
            attrs = getSchemaContext( getService() ).getAttributes(
                "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
            assertNotNull( attrs );
        }
        else
        {
            //noinspection EmptyCatchBlock
            try
            {
                attrs = getSchemaContext( getService() ).getAttributes(
                    "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
                fail( "should never get here" );
            }
            catch ( NamingException e )
            {
            }
            assertNull( attrs );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the attributeTypeRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getAttributeTypeRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getAttributeTypeRegistry().contains( oid ) );
        }
    }


    /**
     * Tests to see if an attributeType is persisted when added, then server
     * is shutdown, then restarted again.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddAttributeTypePersistence() throws Exception
    {
        try
        {
            enableSchema( "nis" );

            List<String> descriptions = new ArrayList<String>();

            // -------------------------------------------------------------------
            // test successful add with everything
            // -------------------------------------------------------------------

            descriptions.add(
                "( 1.3.6.1.4.1.18060.0.9.3.1.9" +
                    "  NAME 'ibm-imm' " +
                    "  DESC 'the actual block data being stored' " +
                    "  EQUALITY caseIgnoreIA5Match " +
                    "  ORDERING caseIgnoreOrderingMatch " +
                    "  SUBSTR caseIgnoreSubstringsMatch " +
                    "  SYNTAX 1.3.6.1.4.1.1466.115.121.1.40{32700} " +
                    "  SINGLE-VALUE " +
                    "  USAGE userApplications " +
                    "  X-SCHEMA 'nis' )" );

            modify( DirContext.ADD_ATTRIBUTE, descriptions, "attributeTypes" );

            checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.9.3.1.9", "nis", true );

            // sync operation happens anyway on shutdowns but just to make sure we can do it again
            getService().sync();

            getService().shutdown();
            getService().startup();

            Attributes attrs = new BasicAttributes( true );

            Attribute attr = new BasicAttribute( "objectClass" );
            attr.add( "top" );
            attr.add( "person" );
            attr.add( "extensibleObject" );

            attrs.put( attr );
            attrs.put( "cn", "blah" );
            attrs.put( "sn", "Blah" );
            attrs.put( "ibm-imm", "test" );
            getSystemContext( getService() ).createSubcontext( "cn=blah", attrs );

            checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.9.3.1.9", "nis", true );
        }
        catch ( Exception e )
        {
            throw e;
        }
    }
}
