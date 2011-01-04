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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.codec.search.controls.subentries.SubentriesControl;
import org.apache.directory.shared.ldap.cursor.SearchCursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.AddResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.message.SearchResultEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for the AdministrativePoint interceptor Search operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AdministrativePointServiceIT")
@ApplyLdifs(
    {
        // An entry used to create a User session
        "dn: cn=testUser,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: testUser",
        "sn: test User",
        "userpassword: test"
    })
public class SubentrySearchOperationIT extends AbstractSubentryUnitTest
{
    // ===================================================================
    // Test the Search operation on APs
    // -------------------------------------------------------------------
    /**
     * Test the Search of an AP. All APs are searcheable by default.
     */
    @Test
    public void testSearchAP() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        
        SearchCursor results = adminConnection.search( "ou=system", "(administrativeRole=*)", SearchScope.SUBTREE, "+" );
        
        assertNotNull( results );
        int nbEntry = 0;
        
        while ( results.next() )
        {
            Entry entry = ( ( SearchResultEntry ) results.get() ).getEntry();
            assertEquals ( "-1", entry.get( "AccessControlSeqNumber" ).getString() );
            assertEquals ( "-1", entry.get( "CollectiveAttributeSeqNumber" ).getString() );
            assertEquals ( "-1", entry.get( "SubSchemaSeqNumber" ).getString() );
            assertEquals ( "-1", entry.get( "TriggerExecutionSeqNumber" ).getString() );
            
            nbEntry++;
        }
        
        assertEquals( 1, nbEntry );
        results.close();
    }

    
    /**
     * Test the search of an AP. All APs are searcheable by default. We should get the
     * AP even if we use a non admin user.
     */
    @Test
    public void testSearchAPNotAdmin() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        
        SearchCursor results = adminConnection.search( "ou=system", "(administrativeRole=*)", SearchScope.SUBTREE, "+" );
        
        assertNotNull( results );
        int nbEntry = 0;
        
        while ( results.next() )
        {
            Entry entry = ( ( SearchResultEntry ) results.get() ).getEntry();
            assertEquals ( "-1", entry.get( "AccessControlSeqNumber" ).getString() );
            assertEquals ( "-1", entry.get( "CollectiveAttributeSeqNumber" ).getString() );
            assertEquals ( "-1", entry.get( "SubSchemaSeqNumber" ).getString() );
            assertEquals ( "-1", entry.get( "TriggerExecutionSeqNumber" ).getString() );
            
            nbEntry++;
        }
        
        assertEquals( 1, nbEntry );
        results.close();
    }
    
    
    /**
     * Test the Search of many APs. All APs are searcheable by default.
     */
    @Test
    public void testSearchAPs() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        createCaIAP( "ou=IAP, ou=AAP, ou=system" );
        createCaSAP( "ou=SAP, ou=system" );
        
        SearchCursor results = adminConnection.search( "ou=system", "(administrativeRole=*)", SearchScope.SUBTREE, "+" );
        
        assertNotNull( results );
        int nbEntry = 0;
        
        while ( results.next() )
        {
            Entry entry = ( ( SearchResultEntry ) results.get() ).getEntry();
            
            if ( entry.getDn().equals( "ou=AAP, ou=system" ) )
            { 
                assertEquals( "-1", entry.get( "AccessControlSeqNumber" ).getString() );
                assertEquals( "-1", entry.get( "CollectiveAttributeSeqNumber" ).getString() );
                assertEquals( "-1", entry.get( "SubSchemaSeqNumber" ).getString() );
                assertEquals( "-1", entry.get( "TriggerExecutionSeqNumber" ).getString() );
            }
            else if ( entry.getDn().equals( "ou=IAP, ou=AAP, ou=system" ) )
            {
                assertNull( entry.get( "AccessControlSeqNumber" ) );
                assertEquals( "-1", entry.get( "CollectiveAttributeSeqNumber" ).getString() );
                assertNull( entry.get( "SubSchemaSeqNumber" ) );
                assertNull( entry.get( "TriggerExecutionSeqNumber" ) );
            }
            else if ( entry.getDn().equals( "ou=SAP, ou=system" ) )
            {
                assertNull( entry.get( "AccessControlSeqNumber" ) );
                assertEquals( "-1", entry.get( "CollectiveAttributeSeqNumber" ).getString() );
                assertNull( entry.get( "SubSchemaSeqNumber" ) );
                assertNull( entry.get( "TriggerExecutionSeqNumber" ) );
            }
            
            nbEntry++;
        }
        
        assertEquals( 3, nbEntry );
        results.close();
    }

    
    // ===================================================================
    // Test the Search operation on Subentries
    // -------------------------------------------------------------------
    /**
     * Test the search of a subentry with a non admin user, and a SUBTREE scope.
     * It should fail, as such a search is not allowed by the RFC
     */
    @Test
    public void testSearchSubentryNotAdminScopeSubtree() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        createCaSubentry( "cn=test, ou=AAP, ou=system", "{}" );
        
        SearchCursor results = userConnection.search( "ou=system", "(ObjectClass=subentry)", SearchScope.SUBTREE, "+" );
        
        assertNotNull( results );
        
        // the results should contain no entry
        assertFalse( results.available() );
    }


    /**
     * Test the search of a subentry with an admin user, and a SUBTREE scope.
     * It should fail, as such a search is not allowed by the RFC
     */
    @Test
    public void testSearchSubentryAdminScopeSubtree() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        createCaSubentry( "cn=test, ou=AAP, ou=system", "{}" );
        
        SearchCursor results = adminConnection.search( "ou=system", "(ObjectClass=subentry)", SearchScope.SUBTREE, "+" );
        
        assertNotNull( results );
        
        // the results should contain no entry
        assertFalse( results.available() );
    }


    /**
     * Test the search of a subentry with a admin user, and a SUBTREE scope.
     * It should succeed if we use the subentries control, and we should not have
     * anything but subentries in the returned set of entries
     */
    @Test
    public void testSearchSubentryScopeSubtreeWithControl() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        createCaSubentry( "cn=test, ou=AAP, ou=system", "{}" );
        
        SearchRequest searchRequest = new SearchRequestImpl();
        searchRequest.setBase( new DN( "ou=system" ) );
        searchRequest.setFilter( "(ObjectClass=*)" );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( "+" );
        SubentriesControl control = new SubentriesControl();
        control.setVisibility( true );
        searchRequest.addControl( control );
        SearchCursor results = adminConnection.search( searchRequest );
        
        assertNotNull( results );
        //assertTrue( results.available() );
        
        int nbEntry = 0;
        
        while ( results.next() )
        {
            Entry entry = ( ( SearchResultEntry ) results.get() ).getEntry();
            
            assertEquals( "cn=test,ou=AAP,ou=system", entry.getDn().toString() );
            nbEntry++;
        }
        
        assertEquals( 1, nbEntry );
    }


    // ===================================================================
    // Test the Search operation on normal entries
    // -------------------------------------------------------------------
    /**
     * Test the search of normal entries
     * The structure we will use is the following :
     * 
     * <pre>
     * [ou=system]
     *   |
     *   +-- (cn=e5)
     *   |
     *   +-- [ou=SAP] ... <cn=SECA>
     *   |     |
     *   |     +-- [ou=IAP] ... <cn=SECA>
     *   |     |     |
     *   |     |     +-- (ou=e3)
     *   |     |           |
     *   |     |           +-- (cn=e6)
     *   |     |
     *   |     +-- (ou=e1)
     *   |          |
     *   |          +-- (cn=e2)
     *   |
     *   +-- [ou=AAP] ... <cn=SEAA>
     *         |
     *         +-- (cn=e4)
     * </pre>
     * (xxx) are normal entries</br>
     * [xxx] are AdministrativePoints</br>
     * &lt;xxx&gt; are subentries
     */
    @Test
    public void testSearchEntries() throws Exception
    {
        // First, create the APs
        createCaSAP( "ou=SAP, ou=system" );
        createCaIAP( "ou=IAP,ou=SAP, ou=system" );
        createAAP( "ou=AAP, ou=system" );
        
        // Now, create the entries
        createEntryAdmin( LdifUtils.createEntry( 
            "ou=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: e1" ) );
        
        createEntryAdmin( LdifUtils.createEntry( 
            "cn=e2,ou=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "ObjectClass: extensibleObject",
            "cn: e2",
            "sn: entry 2" ) );
        
        createEntryAdmin( LdifUtils.createEntry( 
            "ou=e3,ou=IAP,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: e3" ) );
        
        createEntryAdmin( LdifUtils.createEntry( 
            "cn=e4,ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e4",
            "sn: entry 4" ) );
        
        createEntryAdmin( LdifUtils.createEntry( 
            "cn=e5,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e5",
            "sn: entry 5" ) );
        
        createEntryAdmin( LdifUtils.createEntry( 
            "cn=e6,ou=e3,ou=IAP,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person",
            "ObjectClass: extensibleObject",
            "cn: e6",
            "sn: entry 6" ) );
        
        // Now, inject the subentries
        // The CA subentry associated with the SAP select all entries having the 'person' or 'extensibleObject'
        // ObjectClasses.
        createCaSubentry( "cn=SESAP, ou=SAP, ou=system", "{ specificationFilter or: { item:person, item:extensibleObject } }" );
        
        // The CA subentry associated with the IAP select all entries having the 'organizationalUnit' ObjectClass 
        createCaSubentry( "cn=SEIAP, ou=IAP, ou=SAP, ou=system", "{ specificationFilter item:organizationalUnit }" );

        // The AC/CA subentry associated with the AAP manage 2 roles, and select of the underlying entries
        Entry subentry = LdifUtils.createEntry( 
            "cn=SEAA,ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: subentry", 
            "ObjectClass: collectiveAttributeSubentry",
            "ObjectClass: accessControlSubentry",
            "cn: test",
            "subtreeSpecification: {}", 
            "c-o: Test Org",
            "prescriptiveACI: { " 
            + "  identificationTag \"addAci\", "
            + "  precedence 14, " 
            + "  authenticationLevel none, " 
            + "  itemOrUserFirst userFirst: " 
            + "  { "
            + "    userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }," 
            + "    userPermissions "
            + "    { " 
            + "      { " 
            + "        protectedItems { entry, allUserAttributeTypesAndValues }, "
            + "        grantsAndDenials { grantCompare, grantRead, grantBrowse } " 
            + "      } " 
            + "    } " 
            + "  } "
            + "}" );

        AddResponse response = adminConnection.add( subentry );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        // Now, let's read all the entries.
        SearchCursor results = adminConnection.search( "ou=system", "(ObjectClass=*)", SearchScope.SUBTREE, "*", "+" );
        
        assertNotNull( results );
        int nbEntry = 0;
        
        while ( results.next() )
        {
            Entry entry = ( ( SearchResultEntry ) results.get() ).getEntry();

            System.out.println( entry );
            nbEntry++;
        }
    }
}
