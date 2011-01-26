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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.codec.search.controls.subentries.Subentries;
import org.apache.directory.shared.ldap.codec.search.controls.subentries.SubentriesDecorator;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.message.AddResponse;
import org.apache.directory.shared.ldap.model.message.*;
import org.apache.directory.shared.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.util.JndiUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Testcases for the SubentryInterceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
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

    private class JndiSubentriesControl implements javax.naming.ldap.Control
    {

        public boolean isCritical()
        {
            return false;
        }

        public byte[] getEncodedValue()
        {
            return new byte[]{0x01, 0x01, (byte)0xFF};
        }

        public String getID()
        {
            return "1.3.6.1.4.1.4203.1.10.1";
        }
    }


    public Attributes getTestEntry( String cn )
    {
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        subentry.put( objectClass );
        subentry.put( "cn", cn );
        subentry.put( "sn", "testentry" );
        return subentry;
    }


    public Attributes getTestSubentry()
    {
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "c-o", "Test Org" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public Entry getSubentry( String dn ) throws Exception
    {
        Entry subentry = LdifUtils.createEntry( new Dn( dn ), "objectClass: top", "objectClass: subentry",
            "objectClass: collectiveAttributeSubentry", "subtreeSpecification: { base \"ou=configuration\" }",
            "c-o: Test Org", "cn: testsubentry" );

        return subentry;
    }


    public Attributes getTestSubentryWithExclusion()
    {
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        String spec = "{ base \"ou=configuration\", specificExclusions { chopBefore:\"cn=unmarked\" } }";
        subentry.put( "subtreeSpecification", spec );
        subentry.put( "c-o", "Test Org" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public Entry getTestSubentryWithExclusion( String dn ) throws LdapException
    {
        Entry subentry = LdifUtils.createEntry( new Dn( dn ),
            "objectClass: top",
            "objectClass: subentry",
            "objectClass: collectiveAttributeSubentry",
            "subtreeSpecification: { base \"ou=configuration\", specificExclusions { chopBefore:\"cn=unmarked\" } }",
            "c-o: Test Org",
            "cn: testsubentry" );
        
        return subentry;
    }


    private void addAdministrativeRole( String role ) throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        Attribute attribute = new BasicAttribute( "administrativeRole" );
        attribute.add( role );
        ModificationItem item = new ModificationItem( DirContext.ADD_ATTRIBUTE, attribute );
        sysRoot.modifyAttributes( "", new ModificationItem[]
            { item } );
    }


    private void addAdministrativeRole( LdapConnection connection, String dn, String role ) throws Exception
    {
        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( new Dn( dn ) );
        modifyRequest.add( "administrativeRole", role );
        connection.modify( modifyRequest );
    }


    private Map<String, Attributes> getAllEntries() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );
        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(objectClass=*)", controls );

        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }

        return resultMap;
    }


    private Map<String, Entry> getAllEntries( LdapConnection connection, String dn ) throws Exception
    {
        Map<String, Entry> results = new HashMap<String, Entry>();

        Cursor<Response> responses = connection.search( dn, "(objectClass=*)", SearchScope.SUBTREE, "+", "*" );

        while ( responses.next() )
        {
            Response response = responses.get();

            if ( response instanceof SearchResultEntry)
            {
                Entry entry = ( ( SearchResultEntry ) response ).getEntry();

                results.put( entry.getDn().getName(), entry );
            }
        }

        return results;
    }


    @Test
    public void testEntryAdd() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( "collectiveAttributeSubentries" ) );
    }


    private void checkHasOpAttr( Entry entry, String opAttr, int nbOpAttr, String... subentryDns ) throws Exception
    {
        EntryAttribute attribute = entry.get( opAttr );
        assertNotNull( attribute );
        assertEquals( nbOpAttr, attribute.size() );

        for ( String subentryDn : subentryDns )
        {
            assertTrue( attribute.contains( subentryDn ) );
        }
    }


    private void checkDoesNotHaveOpAttr( Entry entry, String opAttr ) throws Exception
    {
        EntryAttribute attribute = entry.get( opAttr );
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
    public void testSubentryAdd() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        // Add the subentry
        Entry subEntryA = LdifUtils.createEntry( new Dn( "cn=testsubentryA,dc=AP-A,dc=test,ou=system" ),
            "objectClass: top", "objectClass: subentry", "objectClass: collectiveAttributeSubentry",
            "subtreeSpecification: {}", // All the entry from the AP, including the AP
            "c-o: Test Org", "cn: testsubentryA" );

        AddResponse response = connection.add( subEntryA );

        assertTrue( response.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS );

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
        Entry subEntryB = LdifUtils.createEntry(new Dn("cn=testsubentryB,dc=AP-B,cn=A2,dc=AP-A,dc=test,ou=system"),
                "objectClass: top", "objectClass: subentry", "objectClass: collectiveAttributeSubentry",
                "subtreeSpecification: {}", // All the entry from the AP, including the AP
                "c-o: Test Org", "cn: testsubentryB");

        response = connection.add( subEntryB );

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

        connection.close();
    }


    public void testSubentryAddOld() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        Entry subEntry = getSubentry( "cn=testsubentry,ou=system" );
        AddResponse response = connection.add( subEntry );

        assertTrue( "should never get here: cannot create subentry under regular entries", response.getLdapResult()
            .getResultCode() == ResultCodeEnum.NO_SUCH_ATTRIBUTE );

        addAdministrativeRole( connection, "ou=system", "collectiveAttributeSpecificArea" );
        connection.add( subEntry );

        // All the entries under ou=configuration,ou=system will have a
        // collectiveAttributeSubentries = "cn=testsubentry, ou=system"
        // operational attribute
        Map<String, Entry> results = getAllEntries( connection, "ou=system" );

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------
        String subEntryDn = "2.5.4.3=testsubentry,2.5.4.11=system";

        String[] modifiedEntries = new String[]
            { "ou=configuration,ou=system", "ou=interceptors,ou=configuration,ou=system",
                "ou=partitions,ou=configuration,ou=system", "ou=configuration,ou=system",
                "ou=services,ou=configuration,ou=system" };

        for ( String dn : modifiedEntries )
        {
            checkHasOpAttr( results.get( dn ), "collectiveAttributeSubentries", 1, subEntryDn );
        }

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------
        String[] unchangedEntries = new String[]
            { "ou=system", "ou=users,ou=system", "ou=groups,ou=system", "uid=admin,ou=system",
                "prefNodeName=sysPrefRoot,ou=system" };

        for ( String dn : unchangedEntries )
        {
            checkDoesNotHaveOpAttr( results.get( dn ), "collectiveAttributeSubentries" );
        }

        connection.close();
    }


    @Test
    public void testSubentryModify() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration
            .get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups
            .get( "collectiveAttributeSubentries" ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attribute subtreeSpecification = new BasicAttribute( "subtreeSpecification" );
        subtreeSpecification.add( "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=services\" } }" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, subtreeSpecification );
        sysRoot.modifyAttributes( "cn=testsubentry", new ModificationItem[]
            { item } );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = results.get( "ou=configuration,ou=system" );
        collectiveAttributeSubentries = configuration.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups
            .get( "collectiveAttributeSubentries" ) );

        admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

        services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0,
                collectiveAttributeSubentries.size() );
        }
    }


    @Test
    public void testSubentryModify2() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration
            .get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups
            .get( "collectiveAttributeSubentries" ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attributes changes = new BasicAttributes( true );
        changes.put( "subtreeSpecification",
            "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=services\" } }" );
        sysRoot.modifyAttributes( "cn=testsubentry", DirContext.REPLACE_ATTRIBUTE, changes );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = results.get( "ou=configuration,ou=system" );
        collectiveAttributeSubentries = configuration.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups
            .get( "collectiveAttributeSubentries" ) );

        admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

        services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0,
                collectiveAttributeSubentries.size() );
        }
    }


    @Test
    public void testSubentryDelete() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.destroySubcontext( "cn=testsubentry" );

        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration
            .get( "collectiveAttributeSubentries" );

        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );

        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=interceptors,ou=configuration,ou=system should not be marked", 0,
                collectiveAttributeSubentries.size() );
        }

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );

        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=partitions,ou=configuration,ou=system should not be marked", 0,
                collectiveAttributeSubentries.size() );
        }

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );

        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0,
                collectiveAttributeSubentries.size() );
        }

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

    }


    @Test
    public void testSubentryModifyRdn() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.rename( "cn=testsubentry", "cn=newname" );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration
            .get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups
            .get( "collectiveAttributeSubentries" ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

    }


    @Test
    public void testEntryModifyRdn() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked,ou=configuration", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration
            .get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups
            .get( "collectiveAttributeSubentries" ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( "collectiveAttributeSubentries" ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked,ou=configuration" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked,ou=configuration" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( "collectiveAttributeSubentries" ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        sysRoot.rename( "cn=unmarked,ou=configuration", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=configuration,ou=system" ) );
        marked = results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    @Test
    public void testEntryMoveWithRdnChange() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration
            .get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups
            .get( "collectiveAttributeSubentries" ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( "collectiveAttributeSubentries" ) );

        // --------------------------------------------------------------------
        // Now destroy one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( "collectiveAttributeSubentries" ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        sysRoot.rename( "cn=unmarked", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=system" ) );
        marked = results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    @Test
    public void testEntryMove() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration
            .get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( "collectiveAttributeSubentries" );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( "collectiveAttributeSubentries" ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users
            .get( "collectiveAttributeSubentries" ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups
            .get( "collectiveAttributeSubentries" ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin
            .get( "collectiveAttributeSubentries" ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( "collectiveAttributeSubentries" ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( "collectiveAttributeSubentries" ) );

        // --------------------------------------------------------------------
        // Now destroy one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=marked,ou=services,ou=configuration" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        marked = results.get( "cn=marked,ou=services,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( "collectiveAttributeSubentries" );
        assertNotNull( "cn=marked,ou=services,ou=configuration should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    @Test
    public void testSubentriesControl() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        // perform the search without the control
        Map<String, SearchResult> entries = new HashMap<String, SearchResult>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(objectClass=*)", searchControls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            entries.put( result.getName(), result );
        }

        assertTrue( entries.size() > 1 );
        assertNull( entries.get( "cn=testsubentry,ou=system" ) );

        // now add the control with visibility set to true where all entries
        // except subentries disappear
        Subentries ctl = new Subentries();
        ctl.setVisibility( true );
        sysRoot.setRequestControls( JndiUtils.toJndiControls(new Control[]
                {ctl}) );
        list = sysRoot.search( "", "(objectClass=*)", searchControls );
        SearchResult result = list.next();
        assertFalse( list.hasMore() );
        assertEquals( "cn=testsubentry,ou=system", result.getName() );
    }


    @Test
    public void testBaseScopeSearchSubentryVisibilityWithoutTheControl() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.OBJECT_SCOPE );

        Map<String, SearchResult> entries = new HashMap<String, SearchResult>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "cn=testsubentry", "(objectClass=subentry)",
            searchControls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            entries.put( result.getName(), result );
        }

        assertEquals( 1, entries.size() );
        assertNotNull( entries.get( "cn=testsubentry,ou=system" ) );
    }


    @Test
    public void testSubtreeScopeSearchSubentryVisibilityWithoutTheControl() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        Map<String, SearchResult> entries = new HashMap<String, SearchResult>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "cn=testsubentry", "(objectClass=subentry)",
            searchControls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            entries.put( result.getName(), result );
        }

        assertEquals( 0, entries.size() );
    }


    @Test
    public void testSubtreeScopeSearchSubentryVisibilityWithTheSubentriesControl() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        sysRoot.setRequestControls( new javax.naming.ldap.Control[]{ new JndiSubentriesControl() } );
        Map<String, SearchResult> entries = new HashMap<String, SearchResult>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "cn=testsubentry", "(objectClass=subentry)",
            searchControls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            entries.put( result.getName(), result );
        }

        assertEquals( 1, entries.size() );
        assertNotNull( entries.get( "cn=testsubentry,ou=system" ) );
    }


    @Test
    public void testLookupSubentryWithTheSubentriesControl() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        
        sysRoot.setRequestControls( new javax.naming.ldap.Control[]{ new JndiSubentriesControl() } );
        Attributes attributes = sysRoot.getAttributes( "cn=testsubentry", new String[]{"subtreeSpecification"} );

        assertNotNull( attributes );
        Attribute ss = attributes.get( "SubtreeSpecification" );
        assertNotNull( ss );
    }


    @Test
    public void testLookupSubentryAPIWithTheSubentriesControl() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
        
        Entry result = connection.lookup( "cn=testsubentry,ou=system", new Control[]{ new SubentriesDecorator()},"subtreeSpecification" );

        assertNotNull( result );
        String ss = result.get( "SubtreeSpecification" ).getString();
        assertNotNull( ss );
    }


    @Test
    public void testLookupSubentryAPIWithoutTheSubentriesControl() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
        
        Entry result = connection.lookup( "cn=testsubentry,ou=system", "subtreeSpecification" );

        assertNotNull( result );
        String ss = result.get( "SubtreeSpecification" ).getString();
        assertNotNull( ss );
    }


    @Test
    public void testUserInjectAccessControlSubentries() throws Exception
    {
        userConnection = IntegrationUtils.getConnectionAs( service, "cn=testUser,ou=system", "test" );

        Entry sap = LdifUtils.createEntry(
            "ou=dummy,ou=system",
            "objectClass: organizationalUnit",
            "objectClass: top",
            "ou: dummy",
            "accessControlSubentries: ou=system" );

        // It should fail
        AddResponse response = userConnection.add( sap );

        assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, response.getLdapResult().getResultCode() );

        userConnection.close();
    }
}
