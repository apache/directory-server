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


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;


/**
 * Test cases for the schema service.  This is for 
 * <a href="http://issues.apache.org/jira/browse/DIREVE-276">DIREVE-276</a>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaServiceITest extends AbstractAdminTestCase
{
    public void setUp() throws Exception
    {
        super.setLdifPath( "./nonspecific.ldif", getClass() );
        super.setUp();
    }


    public void testFillInObjectClasses() throws NamingException
    {
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
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> persons = new HashMap<String, Attributes>();
        NamingEnumeration results = sysRoot.search( "", "(objectClass=person)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 4, persons.size() );

        Attributes person = null;
        Attribute ocs = null;

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
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> orgPersons = new HashMap<String, Attributes>();
        NamingEnumeration results = sysRoot.search( "", "(objectClass=organizationalPerson)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            orgPersons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 3, orgPersons.size() );

        Attributes orgPerson = null;
        Attribute ocs = null;

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
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map<String, Attributes> inetOrgPersons = new HashMap<String, Attributes>();
        NamingEnumeration results = sysRoot.search( "", "(objectClass=inetOrgPerson)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            inetOrgPersons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 2, inetOrgPersons.size() );

        Attributes inetOrgPerson = null;
        Attribute ocs = null;

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
        NamingEnumeration results = sysRoot.search( "cn=schema", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema,ou=system
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema,2.5.4.11=system" );
        
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
        NamingEnumeration results = sysRoot.search( "cn=schema", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema,ou=system
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema,2.5.4.11=system" );
        
        assertNotNull( attrs );
        
        // We should have 14 attributes in the result :
        // - attributeTypes
        // - cn
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
        assertEquals( 14, attrs.size() );
        
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
    }

    public void testSearchForSubSchemaSubEntrySingleAttributeSelected() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ "nameForms" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration results = sysRoot.search( "cn=schema", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema,ou=system
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema,2.5.4.11=system" );
        
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
        NamingEnumeration results = sysRoot.search( "cn=schema", "(objectClass=nothing)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
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
        NamingEnumeration results = sysRoot.search( "cn=schema", "(objectClass=top)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema,ou=system
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema,2.5.4.11=system" );
        
        assertNotNull( attrs );
        
        // We should have 14 attribute in the result :
        // - nameForms
        assertEquals( 14, attrs.size() );
        
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
    }

    public void testSearchForSubSchemaSubEntryFilterEqualSubSchema() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ "+" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration results = sysRoot.search( "cn=schema", "(objectClass=subSchema)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );
        
        // It should be the normalized form of cn=schema,ou=system
        Attributes attrs = subSchemaEntry.get( "2.5.4.3=schema,2.5.4.11=system" );
        
        assertNotNull( attrs );
        
        // We should have 14 attribute in the result :
        // - nameForms
        assertEquals( 14, attrs.size() );
        
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
    }

    public void testSearchForSubSchemaSubEntryNotObjectScope() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{ "+" } );
        
        Map<String, Attributes> subSchemaEntry = new HashMap<String, Attributes>();
        NamingEnumeration results = sysRoot.search( "cn=schema", "(objectClass=nothing)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
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
        NamingEnumeration results = sysRoot.search( "cn=schema", "(&(objectClass=*)(objectClass=top))", controls );

        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            subSchemaEntry.put( result.getName(), result.getAttributes() );
        }

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }
}
