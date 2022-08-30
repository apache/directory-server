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
package org.apache.directory.server.core.subtree;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.Subentries;
import org.apache.directory.api.ldap.model.message.controls.SubentriesImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Testcases for the SubentryInterceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "SubentryServiceIT-class")
@ApplyLdifs(
    {
        // A test branch
        "dn: dc=test,ou=system",
        "objectClass: top",
        "objectClass: domain",
        "dc: test",
        "",
        // The first level AP
        "dn: dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: domain",
        "administrativeRole: collectiveAttributeSpecificArea",
        "dc: AP-A",
        "",
        // entry A1
        "dn: cn=A1,dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: A1",
        "sn: a1",
        "",
        // entry A1-1
        "dn: cn=A1-1,cn=A1,dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: A1-1",
        "sn: a1-1",
        "",
        // entry A1-2
        "dn: cn=A1-2,cn=A1,dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: A1-2",
        "sn: a1-2",
        "",
        // entry A2
        "dn: cn=A2,dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: A2",
        "sn: a2",
        "",
        // entry A2-1
        "dn: cn=A2-1,cn=A2,dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: A2-1",
        "sn: a2-1",
        "",
        // The second level AP
        "dn: dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: domain",
        "administrativeRole: collectiveAttributeSpecificArea",
        "dc: AP-B",
        "",
        // entry B1
        "dn: cn=B1,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: B1",
        "sn: b1",
        "",
        // entry B2
        "dn: cn=B2,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: B2",
        "sn: b2",
        "",
        // The first level non AP
        "dn: dc=not-AP,dc=test,ou=system",
        "objectClass: top",
        "objectClass: domain",
        "dc: not-AP",
        "",
        // An entry under non-AP
        "dn: cn=C,dc=not-AP,dc=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: C",
        "sn: entry-C",
        "",
        // An entry used to create a User session
        "dn: cn=testUser,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: testUser",
        "sn: test User",
        "userpassword: test",
        "" })
public class SubentryServiceIT extends AbstractLdapTestUnit
{
    // The shared LDAP user connection
    protected static LdapConnection userConnection;

    public Entry getTestEntry( String dn, String cn ) throws LdapException
    {
        return new DefaultEntry( 
            dn,
            "objectClass", "top",
            "objectClass", "person",
            "cn", cn,
            "sn", "testentry" );
    }


    public Entry getTestSubentry( String dn ) throws LdapException
    {
        return new DefaultEntry( 
            dn,
            "objectClass", "top",
            "objectClass", "subentry",
            "objectClass", "collectiveAttributeSubentry",
            "subtreeSpecification", "{ base \"ou=configuration\" }",
            "c-o", "Test Org",
            "cn", "testsubentry" );

    }


    public Entry getSubentry( String dn ) throws Exception
    {
        return new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: subentry",
            "objectClass: collectiveAttributeSubentry",
            "subtreeSpecification: { base \"ou=configuration\" }",
            "c-o: Test Org",
            "cn: testsubentry" );
    }


    public Entry getTestSubentryWithExclusion( String dn ) throws Exception
    {
        return new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: subentry",
            "objectClass: collectiveAttributeSubentry",
            "subtreeSpecification", "{ base \"ou=configuration\", specificExclusions { chopBefore:\"cn=unmarked\" } }",
            "c-o", "Test Org",
            "cn", "testsubentry"
            );
    }


    private void addAdministrativeRole( LdapConnection connection, String dn, String role ) throws Exception
    {
        connection.modify( dn, 
            new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "administrativeRole", role ) );
    }


    public Map<String, Entry> getAllEntries( LdapConnection connection, String dn ) throws Exception
    {
        Map<String, Entry> resultMap = new HashMap<>();
        
        try ( EntryCursor cursor = connection.search( dn, "(objectClass=*)", SearchScope.SUBTREE, "*", "+" ) )
        {
            while ( cursor.next() )
            {
                Entry entry = cursor.get(); 
                
                resultMap.put( entry.getDn().getName(), entry );
            }
        }

        return resultMap;
    }


    @Test
    @Disabled
    public void testEntryAdd() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        { 
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentry( "cn=testsubentry,ou=system" ) );
            connection.add( getTestEntry( "cn=unmarked,ou=system", "unmarked" ) );
            connection.add( getTestEntry( "cn=marked,ou=configuration,ou=system", "marked" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry marked = results.get( "cn=marked,ou=configuration,ou=system" );
            Attribute collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked.get( "collectiveAttributeSubentries" ),
                "cn=unmarked,ou=system should not be marked" );
        }
    }


    private void checkHasOpAttr( Entry entry, String opAttr, int nbOpAttr, String... subentryDns ) throws Exception
    {
        Attribute attribute = entry.get( opAttr );
        assertNotNull( attribute );
        assertEquals( nbOpAttr, attribute.size() );

        for ( String subentryDn : subentryDns )
        {
            assertTrue( attribute.contains( subentryDn ) );
        }
    }


    private void checkDoesNotHaveOpAttr( Entry entry, String opAttr ) throws Exception
    {
        Attribute attribute = entry.get( opAttr );
        assertNull( attribute );
    }


    /**
     * Add a subentry under AP-A.
     * The following entries must be modified :
     * A1
     *   A1-1
     *   A1-2
     * A2
     *   A2-1
     *   AP-B
     *     B1
     *     B2
     * The following entries must not be be modified :
     * AP-A
     * not-AP
     *   C
     *
     * Then add a subentry under AP-B
     * The following entries must be modified :
     *   AP-B
     *     B1
     *     B2
     *
     * Then suppress the subentry under AP-B
     */
    @Test
    //@Disabled
    public void testSubentryAdd() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        { 
            // Add the subentry
            Entry subEntryA = new DefaultEntry(
                "cn=testsubentryA,dc=AP-A,dc=test,ou=system",
                "objectClass: top",
                "objectClass: subentry",
                "objectClass: collectiveAttributeSubentry",
                "subtreeSpecification: {}", // All the entry from the AP, including the AP
                "c-o: Test Org",
                "cn: testsubentryA" );
        
            connection.add( subEntryA );
        
            assertTrue( connection.exists( "cn=testsubentryA,dc=AP-A,dc=test,ou=system" ) );
        
            // Check the resulting modifications
            Map<String, Entry> results = getAllEntries( connection, "dc=test,ou=system" );
        
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentryA do have the mark
            // --------------------------------------------------------------------
            String subEntryAPADn = "2.5.4.3=testsubentrya,0.9.2342.19200300.100.1.25=ap-a,0.9.2342.19200300.100.1.25=test,2.5.4.11=system";
        
            String[] modifiedEntriesA = new String[]
                { "dc=AP-A,dc=test,ou=system", "cn=A1,dc=AP-A,dc=test,ou=system",
                    "cn=A1-1,cn=A1,dc=AP-A,dc=test,ou=system", "cn=A1-2,cn=A1,dc=AP-A,dc=test,ou=system",
                    "cn=A2,dc=AP-A,dc=test,ou=system", "cn=A2-1,cn=A2,dc=AP-A,dc=test,ou=system",
                    "dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system", "cn=B1,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system",
                    "cn=B2,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system", };
        
            for ( String dn : modifiedEntriesA )
            {
                checkHasOpAttr( results.get( dn ), "collectiveAttributeSubentries", 1, subEntryAPADn );
            }
        
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentryA do not have the mark
            // --------------------------------------------------------------------
            String[] unchangedEntriesA = new String[]
                { "dc=test,ou=system", "dc=not-AP,dc=test,ou=system", "cn=C,dc=not-AP,dc=test,ou=system", };
        
            for ( String dn : unchangedEntriesA )
            {
                checkDoesNotHaveOpAttr( results.get( dn ), "collectiveAttributeSubentries" );
            }
        
            // Now add another subentry on AP-B
            // Add the subentry
            Entry subEntryB = new DefaultEntry(
                "cn=testsubentryB,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system",
                "objectClass: top",
                "objectClass: subentry",
                "objectClass: collectiveAttributeSubentry",
                "subtreeSpecification: {}", // All the entry from the AP, including the AP
                "c-o: Test Org",
                "cn: testsubentryB" );
        
            connection.add( subEntryB );
            assertTrue( connection.exists( "cn=testsubentryB,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system" ) );
        
            // Check the resulting modifications
            results = getAllEntries( connection, "dc=test,ou=system" );
        
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentryA do have the mark for
            // the subentry A
            // --------------------------------------------------------------------
            String[] modifiedEntriesAB = new String[]
                { "dc=AP-A,dc=test,ou=system", "cn=A1,dc=AP-A,dc=test,ou=system",
                    "cn=A1-1,cn=A1,dc=AP-A,dc=test,ou=system", "cn=A1-2,cn=A1,dc=AP-A,dc=test,ou=system",
                    "cn=A2,dc=AP-A,dc=test,ou=system", "cn=A2-1,cn=A2,dc=AP-A,dc=test,ou=system", };
        
            for ( String dn : modifiedEntriesAB )
            {
                checkHasOpAttr( results.get( dn ), "collectiveAttributeSubentries", 1, subEntryAPADn );
            }
        
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentryB do have the mark for
            // the two subentries
            // --------------------------------------------------------------------
            String subEntryAPBDn = "2.5.4.3=testsubentryb,0.9.2342.19200300.100.1.25=ap-b,2.5.4.3=a2,0.9.2342.19200300.100.1.25=ap-a,0.9.2342.19200300.100.1.25=test,2.5.4.11=system";
        
            String[] modifiedEntriesB = new String[]
                { "dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system", "cn=B1,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system",
                    "cn=B2,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system", };
        
            for ( String dn : modifiedEntriesB )
            {
                checkHasOpAttr( results.get( dn ), "collectiveAttributeSubentries", 2, subEntryAPADn, subEntryAPBDn );
            }
        
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentryA do not have the mark
            // --------------------------------------------------------------------
            String[] unchangedEntriesB = new String[]
                { "dc=test,ou=system", "dc=not-AP,dc=test,ou=system", "cn=C,dc=not-AP,dc=test,ou=system", };
        
            for ( String dn : unchangedEntriesB )
            {
                checkDoesNotHaveOpAttr( results.get( dn ), "collectiveAttributeSubentries" );
            }
        
            // Now delete the AP-B subentry
            connection.delete( "cn=testsubentryB,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system" );
        
            // --------------------------------------------------------------------
            // Check that we are back to where we were before the addition of the B
            // subentry
            // --------------------------------------------------------------------
            results = getAllEntries( connection, "dc=test,ou=system" );
        
            for ( String dn : modifiedEntriesA )
            {
                checkHasOpAttr( results.get( dn ), "collectiveAttributeSubentries", 1, subEntryAPADn );
            }
        
            for ( String dn : unchangedEntriesA )
            {
                checkDoesNotHaveOpAttr( results.get( dn ), "collectiveAttributeSubentries" );
            }
        }
    }


    @Test
    @Disabled
    public void testSubentryAddOld() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Entry subEntry = getSubentry( "cn=testsubentry,ou=system" );
    
            try
            {
                connection.add( subEntry );
                fail();
            }
            catch ( LdapNoSuchAttributeException lnsae )
            {
                assertTrue( true );
            }
    
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( subEntry );
    
            // All the entries under ou=configuration,ou=system will have a
            // collectiveAttributeSubentries = "cn=testsubentry, ou=system"
            // operational attribute
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
            String subEntryDn = "2.5.4.3= testsubentry ,2.5.4.11= system ";
    
            String[] modifiedEntries = new String[]
                { 
                    "ou=configuration,ou=system", 
                    "ou=interceptors,ou=configuration,ou=system",
                    "ou=partitions,ou=configuration,ou=system", 
                    "ou=services,ou=configuration,ou=system" 
                };
    
            for ( String dn : modifiedEntries )
            {
                checkHasOpAttr( results.get( dn ), "collectiveAttributeSubentries", 1, subEntryDn );
            }
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
            String[] unchangedEntries = new String[]
                { 
                    "ou=system", 
                    "ou=users,ou=system", 
                    "ou=groups,ou=system", 
                    "uid=admin,ou=system",
                    "prefNodeName=sysPrefRoot,ou=system" 
                };
    
            for ( String dn : unchangedEntries )
            {
                checkDoesNotHaveOpAttr( results.get( dn ), "collectiveAttributeSubentries" );
            }
        }
    }


    @Test
    @Disabled
    public void testSubentryModify() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getSubentry( "cn=testsubentry,ou=system" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute collectiveAttributeSubentries = configuration
                .get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=partitions,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=services,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ), "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ),
                "ou=users,ou=system should not be marked" );
    
            Entry groups = results.get( "ou=groups,ou=system" );
            assertNull( groups.get( "collectiveAttributeSubentries" ),
                "ou=groups,ou=system should not be marked" );
    
            Entry admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ),
                "uid=admin,ou=system should not be marked" );
    
            Entry sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ),
                "prefNode=sysPrefRoot,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now modify the subentry by introducing an exclusion
            // --------------------------------------------------------------------
    
            connection.modify( "cn=testsubentry, ou=system",
                new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                    "subtreeSpecification",
                    "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=services\" } }" ) );
            
            results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            configuration = results.get( "ou=configuration,ou=system" );
            collectiveAttributeSubentries = configuration.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=partitions,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ) ,  "ou=system should not be marked" );
    
            users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ), 
                "ou=users,ou=system should not be marked" );
    
            groups = results.get( "ou=groups,ou=system" );
            assertNull( groups.get( "collectiveAttributeSubentries" ) ,
                 "ou=groups,ou=system should not be marked" );
    
            admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ),
                "uid=admin,ou=system should not be marked" );
    
            sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ) ,
                 "prefNode=sysPrefRoot,ou=system should not be marked" );
    
            services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
            
            if ( collectiveAttributeSubentries != null )
            {
                assertEquals( 0, collectiveAttributeSubentries.size(),
                    "ou=services,ou=configuration,ou=system should not be marked" );
            }
        }
    }


    @Test
    @Disabled
    public void testSubentryModify2() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getSubentry( "cn=testsubentry,ou=system" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute collectiveAttributeSubentries = configuration
                .get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=partitions,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=services,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ) ,  "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ),
                "ou=users,ou=system should not be marked" );
    
            Entry groups = results.get( "ou=groups,ou=system" );
            assertNull( groups.get( "collectiveAttributeSubentries" ),
                 "ou=groups,ou=system should not be marked" );
    
            Entry admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ),
                 "uid=admin,ou=system should not be marked" );
    
            Entry sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ) ,
                 "prefNode=sysPrefRoot,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now modify the subentry by introducing an exclusion
            // --------------------------------------------------------------------
    
            connection.modify( "cn=testsubentry, ou=system",
                new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                    "subtreeSpecification",
                    "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=services\" } }" ) );
            
            results = getAllEntries( connection, "ou=system" );

            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            configuration = results.get( "ou=configuration,ou=system" );
            collectiveAttributeSubentries = configuration.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=partitions,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ) , "ou=system should not be marked" );
    
            users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ) ,
                 "ou=users,ou=system should not be marked" );
    
            groups = results.get( "ou=groups,ou=system" );
            assertNull( groups.get( "collectiveAttributeSubentries" ) ,
                 "ou=groups,ou=system should not be marked" );
    
            admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ) ,
                 "uid=admin,ou=system should not be marked" );
    
            sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ) ,
                 "prefNode=sysPrefRoot,ou=system should not be marked" );
    
            services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
            
            if ( collectiveAttributeSubentries != null )
            {
                assertEquals( 0, collectiveAttributeSubentries.size(),
                    "ou=services,ou=configuration,ou=system should not be marked" );
            }
        }
    }


    @Test
    @Disabled
    public void testSubentryDelete() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getSubentry( "cn=testsubentry,ou=system" ) );
            connection.delete( "cn=testsubentry,ou=system" );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute collectiveAttributeSubentries = configuration.get( "collectiveAttributeSubentries" );
    
            if ( collectiveAttributeSubentries != null )
            {
                assertEquals( 0, collectiveAttributeSubentries.size(), "ou=configuration,ou=system should not be marked" );
            }
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
    
            if ( collectiveAttributeSubentries != null )
            {
                assertEquals( 0, collectiveAttributeSubentries.size(),
                    "ou=interceptors,ou=configuration,ou=system should not be marked" );
            }
    
            Entry partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
    
            if ( collectiveAttributeSubentries != null )
            {
                assertEquals( 0, collectiveAttributeSubentries.size(),
                    "ou=partitions,ou=configuration,ou=system should not be marked" );
            }
    
            Entry services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
    
            if ( collectiveAttributeSubentries != null )
            {
                assertEquals( 0, collectiveAttributeSubentries.size(),
                    "ou=services,ou=configuration,ou=system should not be marked" );
            }
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ) ,  "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ) ,
                 "ou=users,ou=system should not be marked" );
    
            Entry admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ) ,
                 "uid=admin,ou=system should not be marked" );
    
            Entry sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ) ,
                 "prefNode=sysPrefRoot,ou=system should not be marked" );
        }
    }


    @Test
    @Disabled
    public void testSubentryModifyRdn() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getSubentry( "cn=testsubentry,ou=system" ) );
            connection.rename( "cn=testsubentry,ou=system", "cn=newname" );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute collectiveAttributeSubentries = configuration
                .get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= newname ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= newname ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=partitions,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= newname ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=services,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= newname ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ) ,  "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ) ,
                 "ou=users,ou=system should not be marked" );
    
            Entry groups = results.get( "ou=groups,ou=system" );
            assertNull( groups.get( "collectiveAttributeSubentries" ) ,
                 "ou=groups,ou=system should not be marked" );
    
            Entry admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ) ,
                 "uid=admin,ou=system should not be marked" );
    
            Entry sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ) ,
                 "prefNode=sysPrefRoot,ou=system should not be marked" );
        }
    }


    @Test
    @Disabled
    public void testEntryModifyRdn() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
            connection.add( getTestEntry( "cn=unmarked,ou=configuration,ou=system", "unmarked" ) );
            connection.add( getTestEntry( "cn=marked,ou=configuration,ou=system", "marked" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute collectiveAttributeSubentries = configuration.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=partitions,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "ou=services,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry marked = results.get( "cn=marked,ou=configuration,ou=system" );
            collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries , "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ) , "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ) ,
                 "ou=users,ou=system should not be marked" );
    
            Entry groups = results.get( "ou=groups,ou=system" );
            assertNull( groups.get( "collectiveAttributeSubentries" ) ,
                 "ou=groups,ou=system should not be marked" );
    
            Entry admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ) ,
                 "uid=admin,ou=system should not be marked" );
    
            Entry sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ) ,
                 "prefNode=sysPrefRoot,ou=system should not be marked" );
    
            Entry unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
            assertNull( unmarked.get( "collectiveAttributeSubentries" ) ,
                 "cn=unmarked,ou=configuration,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now destry one of the marked/unmarked and rename to deleted entry
            // --------------------------------------------------------------------
    
            connection.delete( "cn=unmarked,ou=configuration,ou=system" );
            connection.rename( "cn=marked,ou=configuration,ou=system", "cn=unmarked" );
            results = getAllEntries( connection, "ou=system" );
    
            unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
            assertNull( unmarked.get( "collectiveAttributeSubentries" ) ,
                 "cn=unmarked,ou=configuration,ou=system should not be marked" );
            assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );
    
            // --------------------------------------------------------------------
            // Now rename unmarked to marked and see that subentry op attr is there
            // --------------------------------------------------------------------
    
            connection.rename( "cn=unmarked,ou=configuration,ou=system", "cn=marked" );
            results = getAllEntries( connection, "ou=system" );
            assertNull( results.get( "cn=unmarked,ou=configuration,ou=system" ) );
            marked = results.get( "cn=marked,ou=configuration,ou=system" );
            assertNotNull( marked );
            collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
        }
    }


    @Test
    @Disabled
    public void testEntryMoveWithRdnChange() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
            connection.add( getTestEntry( "cn=unmarked,ou=system", "unmarked" ) );
            connection.add( getTestEntry( "cn=marked,ou=configuration,ou=system", "marked" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );

            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute collectiveAttributeSubentries = configuration.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=partitions,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=services,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry marked = results.get( "cn=marked,ou=configuration,ou=system" );
            collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ) ,  "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ) ,
                 "ou=users,ou=system should not be marked" );
    
            Entry groups = results.get( "ou=groups,ou=system" );
            assertNull( groups.get( "collectiveAttributeSubentries" ) ,
                 "ou=groups,ou=system should not be marked" );
    
            Entry admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ) ,
                 "uid=admin,ou=system should not be marked" );
    
            Entry sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ) ,
                 "prefNode=sysPrefRoot,ou=system should not be marked" );
    
            Entry unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked.get( "collectiveAttributeSubentries" ) ,
                 "cn=unmarked,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now destroy one of the marked/unmarked and rename to deleted entry
            // --------------------------------------------------------------------
    
            connection.delete( "cn=unmarked,ou=system" );
            connection.moveAndRename( "cn=marked,ou=configuration,ou=system", "cn=unmarked,ou=system" );
            results = getAllEntries( connection, "ou=system" );
    
            unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked.get( "collectiveAttributeSubentries" ), 
                "cn=unmarked,ou=system should not be marked" );
            assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );
    
            // --------------------------------------------------------------------
            // Now rename unmarked to marked and see that subentry op attr is there
            // --------------------------------------------------------------------
    
            connection.moveAndRename( "cn=unmarked,ou=system", "cn=marked,ou=configuration,ou=system" );
            results = getAllEntries( connection, "ou=system" );
            assertNull( results.get( "cn=unmarked,ou=system" ) );
            marked = results.get( "cn=marked,ou=configuration,ou=system" );
            assertNotNull( marked );
            collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
        }
    }


    @Test
    @Disabled
    public void testEntryMove() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
            connection.add( getTestEntry( "cn=unmarked,ou=system", "unmarked" ) );
            connection.add( getTestEntry( "cn=marked,ou=configuration,ou=system", "marked" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );

            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute collectiveAttributeSubentries = configuration
                .get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
            collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=partitions,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry services = results.get( "ou=services,ou=configuration,ou=system" );
            collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "ou=services,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            Entry marked = results.get( "cn=marked,ou=configuration,ou=system" );
            collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( "collectiveAttributeSubentries" ) ,  "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( "collectiveAttributeSubentries" ) ,
                 "ou=users,ou=system should not be marked" );
    
            Entry groups = results.get( "ou=groups,ou=system" );
            assertNull( groups.get( "collectiveAttributeSubentries" ) ,
                 "ou=groups,ou=system should not be marked" );
    
            Entry admin = results.get( "uid=admin,ou=system" );
            assertNull( admin.get( "collectiveAttributeSubentries" ) ,
                 "uid=admin,ou=system should not be marked" );
    
            Entry sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
            assertNull( sysPrefRoot.get( "collectiveAttributeSubentries" ) ,
                 "prefNode=sysPrefRoot,ou=system should not be marked" );
    
            Entry unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked.get( "collectiveAttributeSubentries" ) ,
                 "cn=unmarked,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now destroy one of the marked/unmarked and rename to deleted entry
            // --------------------------------------------------------------------
    
            connection.delete( "cn=unmarked,ou=system" );
            connection.move( "cn=marked,ou=configuration,ou=system", "ou=services,ou=configuration,ou=system" );
            results = getAllEntries( connection, "ou=system" );
    
            unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked ,  "cn=unmarked,ou=system should not be marked" );
            assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );
    
            marked = results.get( "cn=marked,ou=services,ou=configuration,ou=system" );
            assertNotNull( marked );
            collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries ,  "cn=marked,ou=services,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", collectiveAttributeSubentries.get().getNormalized() );
            assertEquals( 1, collectiveAttributeSubentries.size() );
        }
    }


    @Test
    public void testSubentriesControl() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry, ou=system" ) );
            
            Map<String, Entry> entries = new HashMap<>();

            try ( EntryCursor cursor = connection.search( "ou=system", "(objectClass=*)", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertTrue( entries.size() > 1 );
            assertNull( entries.get( "cn=testsubentry,ou=system" ) );
    
            // now add the control with visibility set to true where all entries
            // except subentries disappear
            Subentries subentries = new SubentriesImpl();
            subentries.setVisibility( true );
            
            SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBase( new Dn( "ou=system" ) );
            searchRequest.setScope( SearchScope.SUBTREE );
            searchRequest.setFilter( "(objectClass=*)" );
            searchRequest.addControl( subentries );
            
            try ( SearchCursor cursor = connection.search( searchRequest ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.getEntry(); 
                    
                    assertEquals( "cn=testsubentry,ou=system", entry.getDn().getName() );
                }
            }
        }
    }


    @Test
    @Disabled
    public void testBaseScopeSearchSubentryVisibilityWithoutTheControl() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry, ou=system" ) );
            
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "cn=testsubentry,ou=system", "(objectClass=subentry)", SearchScope.OBJECT ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size() );
            assertNotNull( entries.get( "cn=testsubentry,ou=system" ) );
        }
    }


    @Test
    @Disabled
    public void testSubtreeScopeSearchSubentryVisibilityWithoutTheControl() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry, ou=system" ) );
            
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "cn=testsubentry,ou=system", "(objectClass=subentry)", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }

            assertEquals( 0, entries.size() );
        }
    }


    @Test
    @Disabled
    public void testSubtreeScopeSearchSubentryVisibilityWithTheSubentriesControl() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry, ou=system" ) );
            
            Map<String, Entry> entries = new HashMap<>();
            
            Subentries subentries = new SubentriesImpl();
            subentries.setVisibility( true );
            
            SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBase( new Dn( "cn=testsubentry,ou=system" ) );
            searchRequest.setScope( SearchScope.SUBTREE );
            searchRequest.setFilter( "(objectClass=subentry)" );
            searchRequest.addControl( subentries );
            
            try ( SearchCursor cursor = connection.search( searchRequest ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.getEntry(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size() );
            assertNotNull( entries.get( "cn=testsubentry,ou=system" ) );
        }
    }


    @Test
    @Disabled
    public void testLookupSubentryWithTheSubentriesControl() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry, ou=system" ) );
                        
            Subentries subentries = new SubentriesImpl();
            subentries.setVisibility( true );
            

            Entry entry = connection.lookup( "cn=testsubentry,ou=system", new Control[]{subentries}, "subtreeSpecification" );

            assertNotNull( entry );
            Attribute ss = entry.get( "SubtreeSpecification" );
            assertNotNull( ss );
        }
    }

    @Test
    @Disabled
    public void testLookupSubentryAPIWithTheSubentriesControl() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
    
            Entry result = connection.lookup( "cn=testsubentry,ou=system", new Control[]
                { new SubentriesImpl() }, "subtreeSpecification" );
    
            assertNotNull( result );
            String ss = result.get( "SubtreeSpecification" ).getString();
            assertNotNull( ss );
        }
    }


    @Test
    @Disabled
    public void testLookupSubentryAPIWithoutTheSubentriesControl() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
    
            Entry result = connection.lookup( "cn=testsubentry,ou=system", "subtreeSpecification" );
    
            assertNotNull( result );
            String ss = result.get( "SubtreeSpecification" ).getString();
            assertNotNull( ss );
        }
    }


    @Test
    //@Disabled
    public void testUserInjectAccessControlSubentries() throws Exception
    {
        Assertions.assertThrows( LdapNoPermissionException.class, () -> 
        {
            userConnection = IntegrationUtils.getConnectionAs( getService(), "cn=testUser,ou=system", "test" );
    
            Entry sap = new DefaultEntry(
                "ou=dummy,ou=system",
                "objectClass: organizationalUnit",
                "objectClass: top",
                "ou: dummy",
                "accessControlSubentries: ou=system" );
    
            // It should fail
            try
            {
                userConnection.add( sap );
            }
            finally
            {
                userConnection.close();
            }
        } );
    }
}
