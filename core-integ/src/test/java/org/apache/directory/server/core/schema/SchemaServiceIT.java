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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.parsers.AttributeTypeDescriptionSchemaParser;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


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
@ExtendWith( ApacheDSTestExtension.class )
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
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Assertions.assertThrows( LdapSchemaViolationException.class, () -> 
            {
                connection.add( 
                    new DefaultEntry(
                        "uid=invalid, ou=system",
                        "objectClass", "top",
                        "objectClass", "uidObject",
                        "uid", "invalid" ) );
            } );
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
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Assertions.assertThrows( LdapSchemaViolationException.class, () -> 
            {
                connection.add( 
                    new DefaultEntry(
                        "cn=Jack Black, ou=system",
                        "objectClass", "top",
                        "objectClass", "person",
                        "objectClass", "organizationalUnit",
                        "ou", "comedy",
                        "cn", "Jack Black",
                        "sn", "Black" ) );
            } );
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
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Attribute ocs = connection.lookup( "cn=person0,ou=system" ).get( "objectClass" );
            assertEquals( 2, ocs.size() );
            assertTrue( ocs.contains( "top" ) );
            assertTrue( ocs.contains( "person" ) );
    
            ocs = connection.lookup( "cn=person1,ou=system" ).get( "objectClass" );
            assertEquals( 3, ocs.size() );
            assertTrue( ocs.contains( "top" ) );
            assertTrue( ocs.contains( "person" ) );
            assertTrue( ocs.contains( "organizationalPerson" ) );
    
            ocs = connection.lookup( "cn=person2,ou=system" ).get( "objectClass" );
            assertEquals( 4, ocs.size() );
            assertTrue( ocs.contains( "top" ) );
            assertTrue( ocs.contains( "person" ) );
            assertTrue( ocs.contains( "organizationalPerson" ) );
            assertTrue( ocs.contains( "inetOrgPerson" ) );
        }
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
        Map<String, Entry> persons = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(objectClass=person)", SearchScope.ONELEVEL ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    persons.put( person.getDn().getName(), person );
                }
            }
        }
        
        // admin is extra
        assertEquals( 4, persons.size() );

        Entry person = persons.get( "cn=person0,ou=system" );
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
        Map<String, Entry> orgPersons = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(objectClass=organizationalPerson)", SearchScope.ONELEVEL ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    orgPersons.put( person.getDn().getName(), person );
                }
            }
        }

        // admin is extra
        assertEquals( 3, orgPersons.size() );

        Entry orgPerson = orgPersons.get( "cn=person1,ou=system" );
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
        Map<String, Entry> inetOrgPersons = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(objectClass=inetOrgPerson)", SearchScope.ONELEVEL ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    inetOrgPersons.put( person.getDn().getName(), person );
                }
            }
        }

        // admin is extra
        assertEquals( 2, inetOrgPersons.size() );

        Entry inetOrgPerson = inetOrgPersons.get( "cn=person2,ou=system" );
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
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(objectClass=*)", SearchScope.OBJECT ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    subSchemaEntry.put( person.getDn().getName(), person );
                }
            }
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Entry attrs = subSchemaEntry.get( "cn=schema" );

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
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(objectClass=*)", SearchScope.OBJECT, "*", "+" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    subSchemaEntry.put( person.getDn().getName(), person );
                }
            }
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Entry attrs = subSchemaEntry.get( "cn=schema" );

        assertNotNull( attrs );

        // We should not have any NameForms
        assertNull( attrs.get( "nameForms" ) );
    }


    @Test
    public void testSearchForSubSchemaSubEntrySingleAttributeSelected() throws Exception
    {
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(objectClass=*)", SearchScope.OBJECT, "objectClasses" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    subSchemaEntry.put( person.getDn().getName(), person );
                }
            }
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Entry attrs = subSchemaEntry.get( "cn=schema" );

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
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(objectClass=subschema)", SearchScope.OBJECT, 
                "creatorsName", "createTimestamp", "modifiersName", "modifyTimestamp" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    subSchemaEntry.put( person.getDn().getName(), person );
                }
            }
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Entry attrs = subSchemaEntry.get( "cn=schema" );

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
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(objectClass=nothing)", SearchScope.OBJECT, "+" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    subSchemaEntry.put( person.getDn().getName(), person );
                }
            }
        }

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }


    @Test
    public void testSearchForSubSchemaSubEntryFilterEqualTop() throws Exception
    {
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(objectClass=top)", SearchScope.OBJECT, "*", "+" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    subSchemaEntry.put( person.getDn().getName(), person );
                }
            }
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Entry attrs = subSchemaEntry.get( "cn=schema" );

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
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(objectClass=subSchema)", SearchScope.OBJECT, "*", "+" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    subSchemaEntry.put( person.getDn().getName(), person );
                }
            }
        }

        // We should have only one entry in the result
        assertEquals( 1, subSchemaEntry.size() );

        // It should be the normalized form of cn=schema
        Entry attrs = subSchemaEntry.get( "cn=schema" );

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
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(objectClass=nothing)", SearchScope.ONELEVEL, "+" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    subSchemaEntry.put( person.getDn().getName(), person );
                }
            }
        }

        // We should have no entry in the result
        assertEquals( 0, subSchemaEntry.size() );
    }
    


    @Test
    public void testSearchForSubSchemaSubEntryComposedFilters() throws Exception
    {
        Map<String, Entry> subSchemaEntry = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "cn=schema", "(&(objectClass=*)(objectClass=top))", SearchScope.ONELEVEL, "+" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    subSchemaEntry.put( entry.getDn().getName(), entry );
                }
            }
        }

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
        Map<String, Entry> persons = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(seeAlso=cn=Good One,ou=people,o=sevenSeas)", SearchScope.ONELEVEL ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    persons.put( person.getDn().getName(), person );
                }
            }
        }

        // admin is extra
        assertEquals( 1, persons.size() );

        Entry person;
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
        Map<String, Entry> persons = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(objectClass=person)", SearchScope.ONELEVEL, "9.9.9" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    persons.put( person.getDn().getName(), person );
                }
            }
        }

        // admin is extra
        assertEquals( 4, persons.size() );

        Entry person;
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
        Map<String, Entry> persons = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(objectClass=person)", SearchScope.ONELEVEL, "2.5.6.6" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    persons.put( person.getDn().getName(), person );
                }
            }
        }

        // admin is extra
        assertEquals( 4, persons.size() );

        Entry person;
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
        Map<String, Entry> persons = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(objectClass=person)", SearchScope.ONELEVEL, "person" ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                    persons.put( person.getDn().getName(), person );
                }
            }
        }

        // admin is extra
        assertEquals( 4, persons.size() );

        Entry person;
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
        Map<String, Entry> persons = new HashMap<>();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(name=person*)", SearchScope.ONELEVEL ) )
            {
                while ( cursor.next() )
                {
                    Entry person = cursor.get(); 
                
                    persons.put( person.getDn().getName(), person );
                }
            }
        }
    
        assertEquals( 3, persons.size() );

        Entry person = persons.get( "cn=person0,ou=system" );
        assertNotNull( person );
        assertTrue( person.contains( "objectClass", "top", "person" ) );
        assertEquals( 2, person.get( "objectClass" ).size() );

        person = persons.get( "cn=person1,ou=system" );
        assertNotNull( person );
        assertTrue( person.contains( "objectClass", "top", "person", "organizationalPerson" ) );
        assertEquals( 3, person.get( "objectClass" ).size() );

        person = persons.get( "cn=person2,ou=system" );
        assertNotNull( person );
        assertTrue( person.contains( "objectClass", "top", "person", "organizationalPerson", "inetOrgPerson" ) );
        assertEquals( 4, person.get( "objectClass" ).size() );
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
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=schema", "(ObjectClass=top)", SearchScope.SUBTREE ) )
            {
                assertTrue( cursor.next() );
                assertNotNull( cursor.get() );
            }

            try ( EntryCursor cursor = connection.search( "ou=schema", "(objectClass=metaAttributeType)", SearchScope.SUBTREE ) )
            {
                assertTrue( cursor.next() );
                assertNotNull( cursor.get() );
            }
    
            try ( EntryCursor cursor = connection.search( "ou=schema", "(objectClass=metaTop)", SearchScope.SUBTREE ) )
            {
                assertTrue( cursor.next() );
                assertNotNull( cursor.get() );
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private Utility Methods
    // -----------------------------------------------------------------------
    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";
    private static final AttributeTypeDescriptionSchemaParser ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER = new AttributeTypeDescriptionSchemaParser();


    /**
     * Get's the subschemaSubentry attribute value from the rootDSE.
     *
     * @return the subschemaSubentry distinguished name
     * @throws NamingException if there are problems accessing the RootDSE
     */
    private String getSubschemaSubentryDN( LdapConnection connection ) throws Exception
    {
        Entry entry = connection.getRootDse( SUBSCHEMA_SUBENTRY );

        return entry.get( SUBSCHEMA_SUBENTRY ).getString();
    }


    private void enableSchema( LdapConnection connection, String schemaName ) throws Exception
    {
        // now enable the test schema
        connection.modify( "cn=" + schemaName + ",ou=schema", 
            new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "m-disabled", "FALSE" ) );
    }


    private void checkAttributeTypePresent( LdapConnection connection, String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        Entry entry = connection.lookup( "cn=schema", "attributeTypes" );
        Attribute attributeTypes = entry.get( "attributeTypes" );
        AttributeType attributeType = null;

        for ( Value value : attributeTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                attributeType = ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER.parse( desc );
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

        if ( isPresent )
        {
            entry = connection.lookup( "ou=schema", "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
            assertNotNull( entry );
        }
        else
        {
            entry = connection.lookup( "ou=schema", "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
            
            assertNull( entry );
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
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            enableSchema( connection, "nis" );

            // -------------------------------------------------------------------
            // test successful add with everything
            // -------------------------------------------------------------------
            String description =
                "( 1.3.6.1.4.1.18060.0.9.3.1.9" +
                    "  NAME 'ibm-imm' " +
                    "  DESC 'the actual block data being stored' " +
                    "  EQUALITY octetStringMatch " +
                    "  SYNTAX 1.3.6.1.4.1.1466.115.121.1.40{32700} " +
                    "  SINGLE-VALUE " +
                    "  USAGE userApplications " +
                    "  X-SCHEMA 'nis' )";

            connection.modify( getSubschemaSubentryDN( connection ), 
                new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "attributeTypes", description ) );
            
            checkAttributeTypePresent( connection, "1.3.6.1.4.1.18060.0.9.3.1.9", "nis", true );
        }

        // sync operation happens anyway on shutdowns but just to make sure we can do it again
        getService().sync();

        getService().shutdown();
        getService().startup();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            connection.add( 
                new DefaultEntry( 
                    "cn=blah,ou=system",
                    "objectClass", "top",
                    "objectClass", "person",
                    "objectClass", "extensibleObject",
                    "cn", "blah",
                    "sn", "Blah",
                    "ibm-imm", "test"
                    ) );

            checkAttributeTypePresent( connection, "1.3.6.1.4.1.18060.0.9.3.1.9", "nis", true );
        }
    }
}
