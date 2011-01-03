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
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.AddResponse;
import org.apache.directory.shared.ldap.message.DeleteResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the AdministrativePoint interceptor delete operation.
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
public class SubentryDeleteOperationIT extends AbstractSubentryUnitTest
{
    // ===================================================================
    // Test the Delete operation on APs
    // -------------------------------------------------------------------
    // Failure expected
    // -------------------------------------------------------------------
    /**
     * Test the deletion of an AP with children
     */
    @Test
    public void testDeleteAPWithChildren() throws Exception
    {
        createAAP( "ou=AAP,ou=system" );

        // Add a subentry now
        createCaSubentry( "cn=test,ou=AAP,ou=system", "{}" );
        
        assertTrue( checkIsPresent( "cn=test,ou=AAP,ou=system" ) );

        Entry ap = adminConnection.lookup( "ou=AAP,ou=system", "+", "*" );
        assertNotNull( ap );
        assertEquals( "1", ap.get( "collectiveAttributeSeqNumber" ).getString() );
        
        // Now try to delete the AP
        DeleteResponse delResponse = adminConnection.delete( "ou=AAP,ou=system" );
        
        assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_NON_LEAF, delResponse.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Delete a SAP with a non admin user
     */
    @Test
    public void testDeleteSAPNonAdmin() throws Exception
    {
        createCaSAP( "ou=SAP,ou=system" );

        // Now try to delete the AP with another user
        DeleteResponse delResponse = userConnection.delete( "ou=SAP,ou=system" );
        
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, delResponse.getLdapResult().getResultCode() );
        
        // Check that the SAP is still present
        assertTrue( checkIsPresent( "ou=SAP,ou=system" ) );
    }

    
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    /**
     * Delete an AAP
     */
    @Test
    public void testDeleteAAP() throws Exception
    {
        createAAP( "ou=AAP,ou=system" );

        // Now try to delete the AP
        DeleteResponse delResponse = adminConnection.delete( "ou=AAP,ou=system" );
        
        assertEquals( ResultCodeEnum.SUCCESS, delResponse.getLdapResult().getResultCode() );
        
        // Check that the AAP is not anymore present
        assertTrue( checkIsAbsent( "ou=AAP,ou=system" ) );
    }
    
    
    /**
     * Delete a SAP
     */
    @Test
    public void testDeleteSAP() throws Exception
    {
        createCaSAP( "ou=SAP,ou=system" );

        // Now try to delete the AP
        DeleteResponse delResponse = adminConnection.delete( "ou=SAP,ou=system" );
        
        assertEquals( ResultCodeEnum.SUCCESS, delResponse.getLdapResult().getResultCode() );
        
        // Check that the SAP is not anymore present
        assertTrue( checkIsAbsent( "ou=SAP,ou=system" ) );
    }
    
    
    /** 
     * Delete an IAP
     */
    @Test
    public void testDeleteIAP() throws Exception
    {
        createCaSAP( "ou=SAP,ou=system" );
        
        // Add the IAP
        Entry iap = LdifUtils.createEntry( 
            "ou=IAP,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP", 
            "administrativeRole: collectiveAttributeInnerArea" );

        // It should succeed
        AddResponse response = adminConnection.add( iap );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // Now try to delete the SAP (it should fail)
        DeleteResponse delResponse = adminConnection.delete( "ou=SAP,ou=system" );
        
        assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_NON_LEAF, delResponse.getLdapResult().getResultCode() );
        
        // Remove the IAP first
        delResponse = adminConnection.delete( "ou=IAP,ou=SAP,ou=system" );
        
        assertEquals( ResultCodeEnum.SUCCESS, delResponse.getLdapResult().getResultCode() );
        
        // Check that the IAP is not anymore present
        assertTrue( checkIsAbsent( "ou=IAP,ou=SAP,ou=system" ) );
        
        // Remove the SAP
        delResponse = adminConnection.delete( "ou=SAP,ou=system" );
        
        assertEquals( ResultCodeEnum.SUCCESS, delResponse.getLdapResult().getResultCode() );
        
        // Check that the SAP is not anymore present
        assertTrue( checkIsAbsent( "ou=SAP,ou=system" ) );
    }
    
    
    // ===================================================================
    // Test the Delete operation on subentries
    // -------------------------------------------------------------------
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    /**
     * Test the deletion of a subentry under an AAP
     */
    @Test
    public void testDeleteSubentryUnderAAP() throws Exception
    {
        // First add an AAP
        createAAP( "ou=AAP,ou=system" );
        
        assertEquals( -1L, getAcSeqNumber( "ou=AAP,ou=system" ) );
        assertEquals( -1L, getCaSeqNumber( "ou=AAP,ou=system" ) );
        
        // Add a subentry now
        createCaSubentry( "cn=test,ou=AAP,ou=system", "{}" );

        long seqNumber = getCaSeqNumber( "ou=AAP,ou=system" );
        assertEquals( -1L, getAcSeqNumber( "ou=AAP,ou=system" ) );
        
        // Now delete it
        DeleteResponse delResponse = adminConnection.delete( "cn=test,ou=AAP,ou=system" );
        assertEquals( ResultCodeEnum.SUCCESS, delResponse.getLdapResult().getResultCode() );
        
        // Check the CASeqNumber, it must be 1 now
        assertEquals( seqNumber + 1, getCaSeqNumber( "ou=AAP,ou=system" ) );
    }
    
    
    /**
     * Test the deletion of a subentry under a SAP
     */
    @Test
    public void testDeleteSubentryUnderSAP() throws Exception
    {
        createCaSAP( "ou=SAP,ou=system" );
        
        assertEquals( Long.MIN_VALUE, getAcSeqNumber( "ou=SAP,ou=system" ) );
        assertEquals( -1L, getCaSeqNumber( "ou=SAP,ou=system" ) );
        
        // Add a subentry now
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}"); 

        long seqNumber = getCaSeqNumber( "ou=SAP,ou=system" );
        assertEquals( Long.MIN_VALUE, getAcSeqNumber( "ou=SAP,ou=system" ) );
        
        // Now delete it
        DeleteResponse delResponse = adminConnection.delete( "cn=test,ou=SAP,ou=system" );
        assertEquals( ResultCodeEnum.SUCCESS, delResponse.getLdapResult().getResultCode() );
        
        // Check the CASeqNumber, it must be 1 now
        assertEquals( seqNumber + 1, getCaSeqNumber( "ou=SAP,ou=system" ) );
    }
    
    
    /**
     * Test the deletion of a subentry under an IAP
     */
    @Test
    public void testDeleteSubentryUnderIAP() throws Exception
    {
        createCaSAP( "ou=SAP,ou=system" );
        
        // Add the IAP
        Entry iap = LdifUtils.createEntry( 
            "ou=IAP,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP", 
            "administrativeRole: collectiveAttributeInnerArea" );

        // It should succeed
        AddResponse response = adminConnection.add( iap );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        assertEquals( -1L, getCaSeqNumber( "ou=SAP,ou=system" ) );
        assertEquals( -1L, getCaSeqNumber( "ou=IAP,ou=SAP,ou=system" ) );
        
        // Add a subentry now
        createCaSubentry( "cn=test,ou=IAP,ou=SAP,ou=system", "{}" );

        long seqNumberSAP = getCaSeqNumber( "ou=SAP,ou=system" );
        assertEquals( -1L, seqNumberSAP );
        
        long seqNumberIAP = getCaSeqNumber( "ou=IAP,ou=SAP,ou=system" );
        assertTrue( seqNumberIAP > -1L );

        // Now delete it
        DeleteResponse delResponse = adminConnection.delete( "cn=test,ou=IAP,ou=SAP,ou=system" );
        assertEquals( ResultCodeEnum.SUCCESS, delResponse.getLdapResult().getResultCode() );
        
        // Check the CASeqNumbers, it must be 1 now
        assertEquals( -1L, getCaSeqNumber( "ou=SAP,ou=system" ) );
        assertEquals( seqNumberIAP + 1, getCaSeqNumber( "ou=IAP,ou=SAP,ou=system" ) );
        
        assertTrue( checkIsAbsent( "cn=test,ou=IAP,ou=SAP,ou=system" ) );
    }
}
