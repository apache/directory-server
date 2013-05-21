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
package org.apache.directory.server.core.collective;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.ldif.LdapLdifException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the collective attribute service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "CollectiveAttributeServiceIT")
public class CollectiveAttributeServiceIT extends AbstractLdapTestUnit
{
    private Entry getTestEntry( String dn, String cn ) throws LdapLdifException, LdapException
    {
        Entry subentry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: person",
            "cn", cn,
            "sn: testentry" );

        return subentry;
    }


    private Entry getTestSubentry( String dn ) throws LdapLdifException, LdapException
    {
        Entry subentry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: subentry",
            "objectClass: collectiveAttributeSubentry",
            "c-ou: configuration",
            "subtreeSpecification: { base \"ou=configuration\" }",
            "cn: testsubentry" );

        return subentry;
    }


    private Entry getTestSubentry2( String dn ) throws LdapLdifException, LdapException
    {
        Entry subentry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: subentry",
            "objectClass: collectiveAttributeSubentry",
            "c-ou: configuration2",
            "subtreeSpecification: { base \"ou=configuration\" }",
            "cn: testsubentry2" );

        return subentry;
    }


    private Entry getTestSubentry3( String dn ) throws LdapLdifException, LdapException
    {
        Entry subentry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: subentry",
            "objectClass: collectiveAttributeSubentry",
            "c-st: FL",
            "subtreeSpecification: { base \"ou=configuration\" }",
            "cn: testsubentry3" );

        return subentry;
    }


    private void addAdministrativeRole( LdapConnection connection, String role ) throws Exception
    {
        Attribute attribute = new DefaultAttribute( "administrativeRole", role );

        connection.modify( "ou=system", new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute ) );
    }


    private Map<String, Entry> getAllEntries( LdapConnection connection ) throws Exception
    {
        Map<String, Entry> resultMap = new HashMap<String, Entry>();

        EntryCursor cursor = connection.search( "ou=system", "(objectClass=*)", SearchScope.SUBTREE, "+",
            "*" );

        while ( cursor.next() )
        {
            Entry entry = cursor.get();

            resultMap.put( entry.getDn().getName(), entry );
        }

        cursor.close();

        return resultMap;
    }


    private Map<String, Entry> getAllEntriesRestrictAttributes( LdapConnection connection ) throws Exception
    {
        Map<String, Entry> resultMap = new HashMap<String, Entry>();

        EntryCursor cursor = connection.search( "ou=system", "(objectClass=*)", SearchScope.SUBTREE, "cn" );

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            resultMap.put( entry.getDn().getName(), entry );
        }

        cursor.close();

        return resultMap;
    }


    private Map<String, Entry> getAllEntriesCollectiveAttributesOnly( LdapConnection connection ) throws Exception
    {
        Map<String, Entry> resultMap = new HashMap<String, Entry>();

        EntryCursor cursor = connection.search( "ou=system", "(objectClass=*)", SearchScope.SUBTREE,
            "c-ou", "c-st" );

        while ( cursor.next() )
        {
            Entry entry = cursor.get();

            resultMap.put( entry.getDn().getName(), entry );
        }

        cursor.close();

        return resultMap;
    }


    @Test
    public void testLookup() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        // -------------------------------------------------------------------
        // Setup the collective attribute specific administration point
        // -------------------------------------------------------------------
        addAdministrativeRole( connection, "collectiveAttributeSpecificArea" );
        Entry subentry = getTestSubentry( "cn=testsubentry,ou=system" );
        connection.add( subentry );

        // -------------------------------------------------------------------
        // test an entry that should show the collective attribute c-ou
        // -------------------------------------------------------------------

        Entry entry = connection.lookup( "ou=services,ou=configuration,ou=system" );
        Attribute c_ou = entry.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertEquals( "configuration", c_ou.getString() );

        // -------------------------------------------------------------------
        // test an entry that should not show the collective attribute
        // -------------------------------------------------------------------

        entry = connection.lookup( "ou=users,ou=system" );
        c_ou = entry.get( "c-ou" );
        assertNull( "the c-ou collective attribute should not be present", c_ou );

        // -------------------------------------------------------------------
        // now modify entries included by the subentry to have collectiveExclusions
        // -------------------------------------------------------------------
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultAttribute( "collectiveExclusions", "c-ou" ) );
        connection.modify( "ou=services,ou=configuration, ou=system", modification );

        // entry should not show the c-ou collective attribute anymore
        entry = connection.lookup( "ou=services,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );

        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        // now add more collective subentries - the c-ou should still not show due to exclusions
        Entry subentry2 = getTestSubentry2( "cn=testsubentry2,ou=system" );
        connection.add( subentry2 );

        entry = connection.lookup( "ou=services,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );

        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        // entries without the collectiveExclusion should still show both values of c-ou
        entry = connection.lookup( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );

        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );

        // request the collective attribute specifically
        entry = connection.lookup( "ou=interceptors,ou=configuration,ou=system", "c-ou" );
        c_ou = entry.get( "c-ou" );

        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );

        // unspecify the collective attribute in the returning attribute list
        entry = connection.lookup( "ou=interceptors,ou=configuration,ou=system", "objectClass" );
        c_ou = entry.get( "c-ou" );

        assertNull( "a collective c-ou attribute should not be present", c_ou );

        // -------------------------------------------------------------------
        // now add the subentry for the c-st collective attribute
        // -------------------------------------------------------------------
        connection.add( getTestSubentry3( "cn=testsubentry3,ou=system" ) );

        // the new attribute c-st should appear in the node with the c-ou exclusion
        entry = connection.lookup( "ou=services,ou=configuration,ou=system" );
        Attribute c_st = entry.get( "c-st" );

        assertNotNull( "a collective c-st attribute should be present", c_st );
        assertTrue( c_st.contains( "FL" ) );

        // in node without exclusions both values of c-ou should appear with c-st value
        entry = connection.lookup( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );

        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );

        c_st = entry.get( "c-st" );
        assertNotNull( "a collective c-st attribute should be present", c_st );
        assertTrue( c_st.contains( "FL" ) );

        // -------------------------------------------------------------------
        // now modify an entry to exclude all collective attributes
        // -------------------------------------------------------------------
        modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, new DefaultAttribute(
            "collectiveExclusions", "excludeAllCollectiveAttributes" ) );
        connection.modify( "ou=interceptors,ou=configuration, ou=system", modification );

        // none of the attributes should appear any longer
        entry = connection.lookup( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );

        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        c_st = entry.get( "c-st" );

        if ( c_st != null )
        {
            assertEquals( "the c-st collective attribute should not be present", 0, c_st.size() );
        }

        connection.close();
    }


    @Test
    @Ignore("This test is failing until we fix the handling of collective attributes in filters")
    public void testSearchFilterCollectiveAttribute() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        // -------------------------------------------------------------------
        // Setup the collective attribute specific administration point
        // -------------------------------------------------------------------
        addAdministrativeRole( connection, "collectiveAttributeSpecificArea" );
        connection.add( getTestSubentry( "cn=testsubentry,ou=system" ) );

        EntryCursor cursor = connection.search( "ou=system", "(c-ou=configuration)", SearchScope.SUBTREE, "+",
            "*" );

        boolean found = false;

        while ( cursor.next() )
        {
            Entry entry = cursor.get();

            found = true;
            break;
        }

        cursor.close();

        assertTrue( found );

        connection.close();
    }


    @Test
    public void testSearch() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        // -------------------------------------------------------------------
        // Setup the collective attribute specific administration point
        // -------------------------------------------------------------------
        addAdministrativeRole( connection, "collectiveAttributeSpecificArea" );
        connection.add( getTestSubentry( "cn=testsubentry,ou=system" ) );

        // -------------------------------------------------------------------
        // test an entry that should show the collective attribute c-ou
        // -------------------------------------------------------------------
        Map<String, Entry> entries = getAllEntries( connection );
        Entry entry = entries.get( "ou=services,ou=configuration,ou=system" );
        Attribute c_ou = entry.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertEquals( "configuration", c_ou.getString() );

        // -------------------------------------------------------------------
        // Test searching for subtypes
        // -------------------------------------------------------------------
        EntryCursor responses = connection.search( "ou=services,ou=configuration,ou=system",
            "(ObjectClass=*)", SearchScope.OBJECT, "ou" );

        while ( responses.next() )
        {
            entry = responses.get();

            assertEquals( 2, entry.size() );
            assertTrue( entry.containsAttribute( "ou" ) );
            assertTrue( entry.containsAttribute( "c-ou" ) );
            assertTrue( entry.contains( "ou", "services" ) );
            assertTrue( entry.contains( "c-ou", "configuration" ) );
        }

        responses.close();

        // ------------------------------------------------------------------
        // test an entry that should show the collective attribute c-ou,
        // but restrict returned attributes to c-ou and c-st
        // ------------------------------------------------------------------
        entries = getAllEntriesCollectiveAttributesOnly( connection );
        entry = entries.get( "ou=services,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertEquals( "configuration", c_ou.getString() );

        // -------------------------------------------------------------------
        // test an entry that should not show the collective attribute
        // -------------------------------------------------------------------
        entry = entries.get( "ou=users,ou=system" );
        c_ou = entry.get( "c-ou" );
        assertNull( "the c-ou collective attribute should not be present", c_ou );

        // -------------------------------------------------------------------
        // now modify entries included by the subentry to have collectiveExclusions
        // -------------------------------------------------------------------
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultAttribute( "collectiveExclusions", "c-ou" ) );
        connection.modify( "ou=services,ou=configuration, ou=system", modification );

        entries = getAllEntries( connection );

        // entry should not show the c-ou collective attribute anymore
        entry = entries.get( "ou=services,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );

        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        // now add more collective subentries - the c-ou should still not show due to exclusions
        connection.add( getTestSubentry2( "cn=testsubentry2,ou=system" ) );
        entries = getAllEntries( connection );

        entry = entries.get( "ou=services,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );

        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        // entries without the collectiveExclusion should still show both values of c-ou
        entry = entries.get( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );

        // -------------------------------------------------------------------
        // now add the subentry for the c-st collective attribute
        // -------------------------------------------------------------------

        connection.add( getTestSubentry3( "cn=testsubentry3,ou=system" ) );
        entries = getAllEntries( connection );

        // the new attribute c-st should appear in the node with the c-ou exclusion
        entry = entries.get( "ou=services,ou=configuration,ou=system" );
        Attribute c_st = entry.get( "c-st" );
        assertNotNull( "a collective c-st attribute should be present", c_st );
        assertTrue( c_st.contains( "FL" ) );

        // in node without exclusions both values of c-ou should appear with c-st value
        entry = entries.get( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );
        c_st = entry.get( "c-st" );
        assertNotNull( "a collective c-st attribute should be present", c_st );
        assertTrue( c_st.contains( "FL" ) );

        // -------------------------------------------------------------------
        // now modify an entry to exclude all collective attributes
        // -------------------------------------------------------------------
        modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, new DefaultAttribute(
            "collectiveExclusions", "excludeAllCollectiveAttributes" ) );
        connection.modify( "ou=interceptors,ou=configuration, ou=system", modification );

        entries = getAllEntries( connection );

        // none of the attributes should appear any longer
        entry = entries.get( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );

        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        c_st = entry.get( "c-st" );

        if ( c_st != null )
        {
            assertEquals( "the c-st collective attribute should not be present", 0, c_st.size() );
        }

        // -------------------------------------------------------------------
        // Now search attributes but restrict returned attributes to cn and ou
        // -------------------------------------------------------------------

        entries = getAllEntriesRestrictAttributes( connection );

        // we should no longer see collective attributes with restricted return attribs
        entry = entries.get( "ou=services,ou=configuration,ou=system" );
        c_st = entry.get( "c-st" );
        assertNull( "a collective c-st attribute should NOT be present", c_st );

        entry = entries.get( "ou=partitions,ou=configuration,ou=system" );
        c_ou = entry.get( "c-ou" );
        c_st = entry.get( "c-st" );
        assertNull( c_ou );
        assertNull( c_st );

        connection.close();
    }


    @Test(expected = LdapSchemaViolationException.class)
    public void testAddRegularEntryWithCollectiveAttribute() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        Entry entry = getTestEntry( "cn=Ersin Er,ou=system", "Ersin Er" );
        entry.put( "c-l", "Turkiye" );

        connection.add( entry );
        connection.close();
    }


    @Test(expected = LdapSchemaViolationException.class)
    public void testModifyRegularEntryAddingCollectiveAttribute() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Entry entry = getTestEntry( "cn=Ersin Er,ou=system", "Ersin Er" );
        connection.add( entry );

        connection.modify( "cn=Ersin Er,ou=system", new DefaultModification(
            ModificationOperation.ADD_ATTRIBUTE, new DefaultAttribute( "c-l", "Turkiye" ) ) );

        connection.close();
    }


    @Test
    public void testPolymorphicReturnAttrLookup() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        // -------------------------------------------------------------------
        // Setup the collective attribute specific administration point
        // -------------------------------------------------------------------
        addAdministrativeRole( connection, "collectiveAttributeSpecificArea" );
        Entry subentry = getTestSubentry( "cn=testsubentry,ou=system" );
        connection.add( subentry );

        // request the collective attribute's super type specifically
        Entry entry = connection.lookup( "ou=interceptors,ou=configuration,ou=system", "ou" );

        Attribute c_ou = entry.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );

        connection.close();
    }
}
