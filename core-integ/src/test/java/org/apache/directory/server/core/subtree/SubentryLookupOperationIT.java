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

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.entry.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for the AdministrativePoint interceptor lookup operation.
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
public class SubentryLookupOperationIT extends AbstractSubentryUnitTest
{
    // ===================================================================
    // Test the Lookup operation on APs
    // -------------------------------------------------------------------
    /**
     * Test the lookup of an AP. All APs are searcheable by default
     */
    @Test
    public void testLookupAP() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        
        Entry aap = adminConnection.lookup( "ou=AAP, ou=system", "+" );
        
        assertNotNull( aap );
        assertEquals ( "-1", aap.get( "AccessControlSeqNumber" ).getString() );
        assertEquals ( "-1", aap.get( "CollectiveAttributeSeqNumber" ).getString() );
        assertEquals ( "-1", aap.get( "SubSchemaSeqNumber" ).getString() );
        assertEquals ( "-1", aap.get( "TriggerExecutionSeqNumber" ).getString() );
    }

    
    /**
     * Test the lookup of an AP. All APs are searcheable by default
     */
    @Test
    public void testLookupAPNotAdmin() throws Exception
    {
        createAAP( "ou=AAP, ou=system" );
        
        Entry aap = userConnection.lookup( "ou=AAP, ou=system", "+" );
        
        assertNotNull( aap );
        assertEquals ( "-1", aap.get( "AccessControlSeqNumber" ).getString() );
        assertEquals ( "-1", aap.get( "CollectiveAttributeSeqNumber" ).getString() );
        assertEquals ( "-1", aap.get( "SubSchemaSeqNumber" ).getString() );
        assertEquals ( "-1", aap.get( "TriggerExecutionSeqNumber" ).getString() );
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
        createCASubentry( "cn=test, ou=AAP, ou=system" );
        
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
        createCASubentry( "cn=test, ou=AAP, ou=system" );
        
        Entry aap = userConnection.lookup( "ou=AAP, ou=system", "+" );
        
        assertNotNull( aap );
        long acSN = Long.parseLong( aap.get( "AccessControlSeqNumber" ).getString() );
        long caSN = Long.parseLong( aap.get( "CollectiveAttributeSeqNumber" ).getString() );
        long ssSN = Long.parseLong( aap.get( "SubSchemaSeqNumber" ).getString() );
        long teSN = Long.parseLong( aap.get( "TriggerExecutionSeqNumber" ).getString() );
        
        assertEquals( -1L, acSN );
        assertNotSame( -1L, caSN );
        assertEquals( -1L, ssSN );
        assertEquals( -1L, teSN );
        
        Entry subentry = userConnection.lookup( "cn=test, ou=AAP, ou=system", "+" );
        assertNotNull( subentry );
        assertNull( subentry.get( "AccessControlSeqNumber" ) );
        assertNull( subentry.get( "CollectiveAttributeSeqNumber" ) );
        assertNull( subentry.get( "SubSchemaSeqNumber" ) );
        assertNull( subentry.get( "TriggerExecutionSeqNumber" ) );
    }


    /**
     * Test the lookup of a subentry with the subentries control.
     */
    @Test
    public void testLookupSubentryWithControl() throws Exception
    {
        // TODO
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
        // TODO
    }


    /**
     * Test the lookup of a entry added under an AP with no subentry. All
     * the entry SN must be set to -1, and not have any subentries reference
     */
    @Test
    public void testLookupEntryUnderApNoSubentry() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry when an AP with no subentry has been added. All
     * the entry SN must be set to -1, and not have any subentries reference
     */
    @Test
    public void testLookupEntryAfterApAdditionNoSubentry() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry added under an AP with a subentry. 
     * The entry is part of the subtreeSpecification.
     * All the entry SN must be set to the AP SN, and have any subentries reference
     */
    @Test
    public void testLookupEntryUnderApWithSubentrySelected() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry when an AP with a subentry is added. 
     * The entry is part of the subtreeSpecification.
     * All the entry SN must be set to the AP SN, and have any subentries reference
     */
    @Test
    public void testLookupEntryAfterApAdditionWithSubentrySelected() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry when the subentry it referes has been 
     * removed. The entry's reference to the subentry must be removed, the 
     * SN must be the AP SN
     */
    @Test
    public void testLookupEntryAfterSubentryDeletion() throws Exception
    {
        // TODO
    }
}
