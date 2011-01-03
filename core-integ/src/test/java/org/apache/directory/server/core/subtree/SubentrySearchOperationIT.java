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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.cursor.SearchCursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.AddResponse;
import org.apache.directory.shared.ldap.message.DeleteResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.SearchResultEntry;
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
    // Test the Lookup operation on Subentries
    // -------------------------------------------------------------------
    /**
     * Test the lookup of a subentry. Subentries can be read directly as it's 
     * a OBJECT search.
     */
    @Test
    public void testLookupSubentry() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        createCaSubentry( "cn=test, ou=AAP, ou=system", "{}" );
        
        Entry aap = adminConnection.lookup( "ou=AAP, ou=system", "+" );
        
        assertNotNull( aap );
        long acSN = Long.parseLong( aap.get( "AccessControlSeqNumber" ).getString() );
        long caSN = Long.parseLong( aap.get( "CollectiveAttributeSeqNumber" ).getString() );
        long ssSN = Long.parseLong( aap.get( "SubSchemaSeqNumber" ).getString() );
        long teSN = Long.parseLong( aap.get( "TriggerExecutionSeqNumber" ).getString() );
        
        assertEquals( -1L, acSN );
        assertNotSame( -1L, caSN );
        assertEquals( -1L, ssSN );
        assertEquals( -1L, teSN );
        
        Entry subentry = adminConnection.lookup( "cn=test, ou=AAP, ou=system", "+" );
        assertNotNull( subentry );
        assertNull( subentry.get( "AccessControlSeqNumber" ) );
        assertNull( subentry.get( "CollectiveAttributeSeqNumber" ) );
        assertNull( subentry.get( "SubSchemaSeqNumber" ) );
        assertNull( subentry.get( "TriggerExecutionSeqNumber" ) );
    }


    /**
     * Test the lookup of a subentry. Subentries can be read directly as it's 
     * a OBJECT search. We don't use the admin in this test
     */
    @Test
    public void testLookupSubentryNotAdmin() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        createCaSubentry( "cn=test, ou=AAP, ou=system", "{}" );
        
        Entry aap = userConnection.lookup( "ou=AAP, ou=system", "+" );
        
        assertNotNull( aap );
        long acSN = Long.parseLong( aap.get( "AccessControlSeqNumber" ).getString() );
        long caSN = Long.parseLong( aap.get( "CollectiveAttributeSeqNumber" ).getString() );
        long ssSN = Long.parseLong( aap.get( "SubSchemaSeqNumber" ).getString() );
        long teSN = Long.parseLong( aap.get( "TriggerExecutionSeqNumber" ).getString() );
        
        assertEquals( -1L, acSN );
        assertEquals( service.getApSeqNumber(), caSN );
        assertEquals( -1L, ssSN );
        assertEquals( -1L, teSN );
        
        Entry subentry = userConnection.lookup( "cn=test, ou=AAP, ou=system", "+" );
        assertNotNull( subentry );
        assertNull( subentry.get( "AccessControlSeqNumber" ) );
        assertNull( subentry.get( "CollectiveAttributeSeqNumber" ) );
        assertNull( subentry.get( "SubSchemaSeqNumber" ) );
        assertNull( subentry.get( "TriggerExecutionSeqNumber" ) );
    }
    
    
    // ===================================================================
    // Test the Lookup operation on Entries
    // -------------------------------------------------------------------
    /**
     * Test the lookup of a entry with no APs. The entry should not have
     * any SN
     */
    @Test
    public void testLookupEntryNoAp() throws Exception
    {
        // Create a first entry
        Entry e1 = LdifUtils.createEntry( 
            "cn=e1,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e1", 
            "sn: entry 1" );
        
        AddResponse response = adminConnection.add( e1 );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        assertEquals( Long.MIN_VALUE, getCaSeqNumber( "cn=e1,ou=system" ) );
    }


    /**
     * Test the lookup of a entry added under an AP with no subentry. All
     * the entry SN must be set to -1, and not have any subentries reference
     */
    @Test
    public void testLookupEntryUnderApNoSubentry() throws Exception
    {
        // Create an AP
        createAAP( "ou=AAP,ou=system" );
        
        // Create a first entry
        Entry e1 = LdifUtils.createEntry( 
            "cn=e1,ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e1", 
            "sn: entry 1" );
        
        AddResponse response = adminConnection.add( e1 );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        assertEquals( -1L, getCaSeqNumber( "cn=e1,ou=AAP,ou=system" ) );
        assertNull( getCaUuidRef( "cn=e1,ou=AAP,ou=system" ) );
    }


    /**
     * Test the lookup of a entry added under an AP with a subentry. 
     * The entry is part of the subtreeSpecification.
     * All the entry SN must be set to the AP SN, and have any subentries reference
     */
    @Test
    public void testLookupEntryUnderApWithSubentrySelected() throws Exception
    {
        // Create a CA SAP and a subentry
        createCaSAP( "ou=SAP,ou=System" );
        createCaSubentry( "cn=test,ou=SAP,ou=System", "{specificationFilter item: person}" );
        
        // Now, created a selected entry 
        Entry e1 = LdifUtils.createEntry( 
            "cn=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e1", 
            "sn: entry 1" );
        
        AddResponse response = adminConnection.add( e1 );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        long seqNumber = getCaSeqNumber( "ou=SAP,ou=System" );
        
        // Check that the added entry has its AP seqNumber and its subentry UUID 
        assertEquals( seqNumber, getCaSeqNumber( "cn=e1,ou=SAP,ou=system" ) );
        assertEquals( getCaUuidRef( "cn=test,ou=SAP,ou=System" ), getCaUuidRef( "cn=e1,ou=AAP,ou=system" ) );
    }


    /**
     * Test the lookup of a entry when an AP with a subentry is added. 
     * The entry is not part of the subtreeSpecification.
     * All the entry SN must be set to the AP SN, and have no subentries reference
     */
    @Test
    public void testLookupEntryAfterApAdditionWithSubentrySelected() throws Exception
    {
        // Create a CA SAP and a subentry
        createCaSAP( "ou=SAP,ou=System" );
        createCaSubentry( "cn=test,ou=SAP,ou=System", "{specificationFilter item: organization}" );
        
        // Now, created a selected entry 
        Entry e1 = LdifUtils.createEntry( 
            "cn=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e1", 
            "sn: entry 1" );
        
        AddResponse response = adminConnection.add( e1 );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        long seqNumber = getCaSeqNumber( "ou=SAP,ou=System" );
        
        // Check that the added entry has its AP seqNumber and no ref to the subentry 
        assertEquals( seqNumber, getCaSeqNumber( "cn=e1,ou=SAP,ou=system" ) );
        assertNull( getCaUuidRef( "cn=e1,ou=AAP,ou=system" ) );
    }


    /**
     * Test the lookup of a entry when the subentry it referes has been 
     * removed. The entry's reference to the subentry must be removed, the 
     * SN must be the new AP SN
     */
    @Test
    public void testLookupEntryAfterSubentryDeletion() throws Exception
    {
        // Create a CA SAP and a subentry
        createCaSAP( "ou=SAP,ou=System" );
        createCaSubentry( "cn=test,ou=SAP,ou=System", "{specificationFilter item: person}" );
        
        // Now, created a selected entry 
        Entry e1 = LdifUtils.createEntry( 
            "cn=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e1", 
            "sn: entry 1" );
        
        AddResponse addResponse = adminConnection.add( e1 );
        assertEquals( ResultCodeEnum.SUCCESS, addResponse.getLdapResult().getResultCode() );

        long seqNumber1 = getCaSeqNumber( "ou=SAP,ou=System" );
        
        // Check that the added entry has its AP seqNumber and its subentry UUID 
        assertEquals( seqNumber1, getCaSeqNumber( "cn=e1,ou=SAP,ou=system" ) );
        assertEquals( getCaUuidRef( "cn=test,ou=SAP,ou=System" ), getCaUuidRef( "cn=e1,ou=AAP,ou=system" ) );
        
        // Now, remove the subentry
        DeleteResponse delResponse = adminConnection.delete( "cn=test,ou=SAP,ou=System" );
        assertEquals( ResultCodeEnum.SUCCESS, delResponse.getLdapResult().getResultCode() );

        // The AP seqNumber must have been incremented
        long seqNumber2 = getCaSeqNumber( "ou=SAP,ou=System" );
        
        assertTrue( seqNumber1 < seqNumber2 );

        // Now, check the entry 
        assertEquals( seqNumber2, getCaSeqNumber( "cn=e1,ou=SAP,ou=system" ) );
        assertNull( getCaUuidRef( "cn=e1,ou=AAP,ou=system" ) );
    }
}
