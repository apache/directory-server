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
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.AddResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the AdministrativePoint interceptor Add operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AdministrativePointServiceIT")
@ApplyLdifs(
    {
        // A test branch
        "dn: cn=test,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: test",
        "sn: test",
        "userpassword: test"
    })
public class SubentryAddOperationIT extends AbstractLdapTestUnit
{
    // The shared LDAP admin connection
    private static LdapConnection adminConnection;

    // The shared LDAP user connection
    private static LdapConnection userConnection;


    @Before
    public void init() throws Exception
    {
        adminConnection = IntegrationUtils.getAdminConnection( service );
        userConnection = IntegrationUtils.getConnectionAs( service, "cn=test,ou=system", "test" );
    }


    @After
    public void shutdown() throws Exception
    {
        adminConnection.close();
        userConnection.close();
    }


    private Entry getAdminRole( String dn ) throws Exception
    {
        Entry lookup = adminConnection.lookup( dn, "administrativeRole" );

        assertNotNull( lookup );

        return lookup;
    }


    // ===================================================================
    // Test the Add operation for APs
    // -------------------------------------------------------------------
    // Failure expected
    // -------------------------------------------------------------------
    /**
     * Test the addition of an autonomous area in the rootDN
     */
    @Test
    public void testAddAutonomousAreaRootDN() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        // It should fail
        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.ENTRY_ALREADY_EXISTS, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of an autonomous area in a naming context
     */
    @Test
    public void testAddAutonomousAreaNamingContext() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        // It should fail
        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.ENTRY_ALREADY_EXISTS, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of an autonomous area in an existing entry
     */
    @Test
    public void testAddAutonomousAreaExistingEntry() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "uid=admin,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        // It should fail
        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.ENTRY_ALREADY_EXISTS, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of an IAP with no parent SAP
     */
    @Test
    public void testAddIAPWithNoSAP() throws Exception
    {
        Entry iap = LdifUtils.createEntry( 
            "ou=IAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP", 
            "administrativeRole: accessControlInnerArea" );

        // It should fail
        AddResponse response = adminConnection.add( iap );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of an SAP with the same IAP (AccessControl)
     */
    @Test
    public void testAddIAPWithSAPAccessControl() throws Exception
    {
        Entry sapiap = LdifUtils.createEntry( 
            "ou=SAP-IAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: SAP-IAP", 
            "administrativeRole: accessControlInnerArea",
            "administrativeRole: accessControlSpecificArea"
            );

        // It should fail
        AddResponse response = adminConnection.add( sapiap );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of an SAP with the same IAP (CollectiveAttribute)
     */
    @Test
    public void testAddIAPWithSAPCollectiveAttribute() throws Exception
    {
        Entry sapiap = LdifUtils.createEntry( 
            "ou=SAP-IAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: SAP-IAP", 
            "administrativeRole: CollectiveAttributeInnerArea",
            "administrativeRole: CollectiveAttributeSpecificArea"
            );

        // It should fail
        AddResponse response = adminConnection.add( sapiap );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of an SAP with the same IAP (TriggerExecution)
     */
    @Test
    public void testAddIAPWithSAPTriggerExecution() throws Exception
    {
        Entry sapiap = LdifUtils.createEntry( 
            "ou=SAP-IAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: SAP-IAP", 
            "administrativeRole: TriggerExecutionInnerArea",
            "administrativeRole: TriggerExecutionSpecificArea"
            );

        // It should fail
        AddResponse response = adminConnection.add( sapiap );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of an IAP within an IAP (TriggerExecution)
     */
    @Test
    public void testAddIAPWithinAAPTriggerExecution() throws Exception
    {
        Entry sapiap = LdifUtils.createEntry( 
            "ou=SAP-IAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: SAP-IAP", 
            "administrativeRole: autonomousArea",
            "administrativeRole: TriggerExecutionInnerArea"
            );

        // It should fail
        AddResponse response = adminConnection.add( sapiap );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of an AAP with no role
     */
    @Test
    public void testAddAAPWithNoRole() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: AAP", 
            "administrativeRole: " );

        // It should fail
        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of an AP directly under a subentry
     */
    @Test
    public void testAddAPUnderSubentry() throws Exception
    {
        // First add an AAP
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: SAP", 
            "administrativeRole: autonomousArea" );

        AddResponse response = adminConnection.add( autonomousArea );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        // Add a subentry now
        Entry subentry = LdifUtils.createEntry( 
            "cn=test,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: subentry", 
            "ObjectClass: collectiveAttributeSubentry",
            "cn: test",
            "subtreeSpecification: {}", 
            "c-o: Test Org" );

        response = adminConnection.add( subentry );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        Entry subentryEntry = adminConnection.lookup( "cn=test,ou=SAP,ou=system", "+", "*" );
        assertNotNull( subentryEntry );

        Entry ap = adminConnection.lookup( "ou=SAP,ou=system", "+", "*" );
        assertNotNull( ap );
        assertEquals( "0", ap.get( "APSeqNumber" ).getString() );
        
        // Now, try to inject an AP under the subentry
        // First add an AAP
        Entry badAP = LdifUtils.createEntry( 
            "ou=BADAP,cn=test,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: BADAP", 
            "administrativeRole: autonomousArea" );

        response = adminConnection.add( badAP );
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of a SAP with a normal user
     */
    @Test
    public void testAddSAPWithNonAdmin() throws Exception
    {
        Entry sap = LdifUtils.createEntry( 
            "ou=IAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP", 
            "administrativeRole: accessControlSpecificArea" );

        // It should fail
        AddResponse response = userConnection.add( sap );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of some invalid role
     * @throws Exception
     */
    @Test
    public void testAddInvalidRole() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: accessControlBadArea",
            "administrativeRole: TRIGGEREXECUTIONINNERAREA" );

        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of some roles more than once
     * @throws Exception
     */
    @Test
    public void testAddRoleMoreThanOnce() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea",
            "administrativeRole: 2.5.23.1" );

        // It should not succeed
        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of an IAP under a SAP for a different role
     */
    @Test
    public void testAddIAPUnderSAPDifferentRole() throws Exception
    {
        // First add an SAP
        Entry sap = LdifUtils.createEntry( 
            "ou=SAP1,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: SAP", 
            "administrativeRole: accessControlSpecificArea" );

        AddResponse response = adminConnection.add( sap );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        // Add a IAP now
        Entry iap = LdifUtils.createEntry( 
            "ou=IAP,ou=SAP1,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP",
            "administrativeRole: collectiveATtributeInnerArea" );

        response = adminConnection.add( iap );
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    /**
     * Test the addition of an AAP with SAPs
     */
    @Test
    public void testAddAAPWithSAPs() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: AAP", 
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: autonomousArea"
            );

        // It should fail
        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        Entry adminPoint = adminConnection.lookup( "ou=AAP,ou=system", "+", "*" );
        
        EntryAttribute roles = adminPoint.get( "administrativeRole" );
        
        assertNotNull( roles );
        assertEquals( 5, roles.size() );
        assertTrue( roles.contains( "autonomousArea", "accessControlSpecificArea", "collectiveAttributeSpecificArea", 
            "triggerExecutionSpecificArea", "subSchemaSpecificArea" ) );
    }


    /**
     * Test the addition of an autonomous area
     * @throws Exception
     */
    @Test
    public void testAddAutonomousArea() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        // It should succeed
        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        Entry adminPoint = adminConnection.lookup( "ou=autonomousArea, ou=system", "+" );
        assertNotNull( adminPoint );
        assertEquals( -1, Long.parseLong( adminPoint.get( "APSeqNumber" ).getString() ) );

        // Check that the entry is containing all the roles
        Entry entry = getAdminRole( "ou=autonomousArea, ou=system" );

        assertTrue( entry.contains( "administrativeRole", "autonomousArea" ) );
        assertTrue( entry.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "subschemaSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "triggerExecutionSpecificArea" ) );

        autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea2, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea2", 
            "administrativeRole: autonomousArea",
            "administrativeRole: accessControlSpecificArea", 
            "administrativeRole: collectiveAttributeInnerArea",
            "administrativeRole: subschemaSpecificArea", 
            "administrativeRole: TRIGGEREXECUTIONSPECIFICAREA" );

        // It should fail, as an autonomous area is already defining the specific areas
        response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of some specific area
     * @throws Exception
     */
    @Test
    public void testAddSpecificAreas() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: TRIGGEREXECUTIONSPECIFICAREA" );

        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // Check that the entry is containing all the roles
        Entry entry = getAdminRole( "ou=autonomousArea, ou=system" );

        assertFalse( entry.contains( "administrativeRole", "autonomousArea" ) );
        assertTrue( entry.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertFalse( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertFalse( entry.contains( "administrativeRole", "2.5.23.4" ) );
        assertTrue( entry.contains( "administrativeRole", "triggerExecutionSpecificArea" ) );
    }


    /**
     * Test the addition of some inner area
     * @throws Exception
     */
    @Test
    public void testAddInnerAreas() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: accessControlINNERArea",
            "administrativeRole: TRIGGEREXECUTIONINNERAREA" );

        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // Check that the entry is containing all the roles
        Entry entry = getAdminRole( "ou=autonomousArea, ou=system" );

        assertFalse( entry.contains( "administrativeRole", "autonomousArea" ) );
        assertTrue( entry.contains( "administrativeRole", "accessControlInnerArea" ) );
        assertTrue( entry.contains( "administrativeRole", "triggerExecutionInnerArea" ) );
    }
    
    
    /**
     * Test the addition of an IAP under a SAP
     */
    @Test
    public void testAddIAPUnderSAP() throws Exception
    {
        // First add an SAP
        Entry sap = LdifUtils.createEntry( 
            "ou=SAP1,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: SAP", 
            "administrativeRole: accessControlSpecificArea" );

        AddResponse response = adminConnection.add( sap );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        // Add a IAP now
        Entry iap = LdifUtils.createEntry( 
            "ou=IAP,ou=SAP1,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP",
            "administrativeRole: accessControlInnerArea" );

        response = adminConnection.add( iap );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
    }
    
    
    // ===================================================================
    // Test the Add operation for Subentrys
    // -------------------------------------------------------------------
    // Failure expected
    // -------------------------------------------------------------------

    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    
    
    // ===================================================================
    // Test the Add operation for Entries
    // -------------------------------------------------------------------
    // Failure expected
    // -------------------------------------------------------------------

    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
}
