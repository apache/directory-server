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
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.administrative.AdministrativePoint;
import org.apache.directory.server.core.administrative.Subentry;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.AddResponse;
import org.apache.directory.shared.ldap.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.Ignore;
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
        // An entry used to create a User session
        "dn: cn=testUser,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: testUser",
        "sn: test User",
        "userpassword: test"
    })
public class SubentryRenameOperationIT extends AbstractSubentryUnitTest
{
    // ===================================================================
    // Test the Rename operation for APs
    // -------------------------------------------------------------------
    // Failure expected
    // -------------------------------------------------------------------
    /**
     * Test the addition of an autonomous area in the rootDN
     */
    @Test
    @Ignore
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
    @Ignore
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
    @Ignore
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
    @Ignore
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
    @Ignore
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
    @Ignore
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
    @Ignore
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
    @Ignore
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
    @Ignore
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
    @Ignore
    public void testAddAPUnderSubentry() throws Exception
    {
        // First add an AAP
        createAAP( "ou=AAP,ou=system" );
        
        // Add a subentry now
        createCaSubentry( "cn=test,ou=AAP,ou=system", "{}" );
        
        Entry subentryEntry = adminConnection.lookup( "cn=test,ou=AAP,ou=system", "+", "*" );
        assertNotNull( subentryEntry );

        Entry ap = adminConnection.lookup( "ou=AAP,ou=system", "+", "*" );
        assertNotNull( ap );
        assertEquals( "1", ap.get( "CollectiveAttributeSeqNumber" ).getString() );
        
        // Now, try to inject an AP under the subentry
        // First add an AAP
        Entry badAP = LdifUtils.createEntry( 
            "ou=BADAP,cn=test,ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: BADAP", 
            "administrativeRole: autonomousArea" );

        AddResponse response = adminConnection.add( badAP );
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of a SAP with a normal user
     */
    @Test
    @Ignore
    public void testAddSAPWithNonAdmin() throws Exception
    {
        Entry sap = LdifUtils.createEntry( 
            "ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: SAP", 
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
    @Ignore
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
    @Ignore
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
    @Ignore
    public void testAddIAPUnderSAPDifferentRole() throws Exception
    {
        // First add an SAP
        createAcSAP( "ou=SAP,ou=system" ); 
        
        // Add a IAP now
        Entry iap = LdifUtils.createEntry( 
            "ou=IAP,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP",
            "administrativeRole: collectiveATtributeInnerArea" );

        AddResponse response = adminConnection.add( iap );
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    /**
     * Test the addition of an AAP with SAPs
     */
    @Test
    @Ignore
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

        // It should succeed
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
    @Ignore
    public void testAddAutonomousArea() throws Exception
    {
        createAAP( "ou=autonomousArea, ou=system" );
        
        Entry adminPoint = adminConnection.lookup( "ou=autonomousArea, ou=system", "+" );
        assertNotNull( adminPoint );
        assertEquals( -1, Long.parseLong( adminPoint.get( "AccessControlSeqNumber" ).getString() ) );
        assertEquals( -1, Long.parseLong( adminPoint.get( "CollectiveAttributeSeqNumber" ).getString() ) );
        assertEquals( -1, Long.parseLong( adminPoint.get( "SubSchemaSeqNumber" ).getString() ) );
        assertEquals( -1, Long.parseLong( adminPoint.get( "TriggerExecutionSeqNumber" ).getString() ) );

        // Check that the entry is containing all the roles
        Entry entry = getAdminRole( "ou=autonomousArea, ou=system" );

        assertTrue( entry.contains( "administrativeRole", "autonomousArea" ) );
        assertTrue( entry.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "subschemaSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "triggerExecutionSpecificArea" ) );

        Entry autonomousArea = LdifUtils.createEntry( 
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
        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of some specific area
     * @throws Exception
     */
    @Test
    @Ignore
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
    @Ignore
    public void testAddInnerAreas() throws Exception
    {
        createAAP( "ou=AAP,ou=system" );
        
        // Now add the IAPs
        Entry innerAreas = LdifUtils.createEntry( 
            "ou=innerAreas, ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: innerAreas", 
            "administrativeRole: accessControlINNERArea",
            "administrativeRole: TRIGGEREXECUTIONINNERAREA" );

        AddResponse response = adminConnection.add( innerAreas );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // Check that the entry is containing all the roles
        Entry entry = getAdminRole( "ou=innerAreas, ou=AAP,ou=system" );

        assertFalse( entry.contains( "administrativeRole", "autonomousArea" ) );
        assertTrue( entry.contains( "administrativeRole", "accessControlInnerArea" ) );
        assertTrue( entry.contains( "administrativeRole", "triggerExecutionInnerArea" ) );
    }
    
    
    /**
     * Test the addition of an IAP under a SAP
     */
    @Test
    @Ignore
    public void testAddIAPUnderSAP() throws Exception
    {
        // First add an SAP
        createAcSAP( "ou=SAP,ou=system" );
        
        // Add a IAP now
        Entry iap = LdifUtils.createEntry( 
            "ou=IAP,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP",
            "administrativeRole: accessControlInnerArea" );

        AddResponse response = adminConnection.add( iap );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
    }
    
    
    // ===================================================================
    // Test the Add operation for Subentries
    // -------------------------------------------------------------------
    // Failure expected
    // -------------------------------------------------------------------
    /**
     * Test the renaming of a subentry using another AT than CN 
     */
    @Test
    public void testRenameSubentryUsingOC() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );

        // Now, try to rename the subentry to "objectClass=subentry, ..."
        ModifyDnResponse response = adminConnection.rename( "cn=test,ou=SAP,ou=system", "objectClass=subentry" );
        
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the renaming of a subentry to a name already in use 
     */
    @Test
    public void testRenameSubentryAlreadyExist() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );

        // Add a second subentry
        createCaSubentry( "cn=test2,ou=SAP,ou=system", "{}" );

        // Now, try to rename the subentry to "sn=test, ..."
        ModifyDnResponse response = adminConnection.rename( "cn=test,ou=SAP,ou=system", "cn=test2" );
        
        assertEquals( ResultCodeEnum.ENTRY_ALREADY_EXISTS, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the renaming of a subentry using an empty CN 
     */
    @Test
    public void testRenameSubentryEmptyCN() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );

        // Now, try to rename the subentry to "cn=,..."
        ModifyDnResponse response = adminConnection.rename( "cn=test,ou=SAP,ou=system", "cn=" );
        
        assertEquals( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, response.getLdapResult().getResultCode() );
    }
    
    
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    /**
     * Test the addition of a subentry under an AAP
     */
    @Test
    @Ignore
    public void testAddSubentryUnderAAP() throws Exception
    {
        // First add an AAP
        createAAP( "ou=AAP,ou=system" );
        
        // Add a subentry now
        Entry subentry = LdifUtils.createEntry( 
            "cn=test,ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: subentry", 
            "ObjectClass: collectiveAttributeSubentry",
            "cn: test",
            "subtreeSpecification: {}", 
            "c-o: Test Org" );

        AddResponse response = adminConnection.add( subentry );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of a subentry under a SAP
     */
    @Test
    @Ignore
    public void testAddSubentryUnderSAP() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry now
        Entry subentry = LdifUtils.createEntry( 
            "cn=test,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: subentry", 
            "ObjectClass: collectiveAttributeSubentry",
            "cn: test",
            "subtreeSpecification: {}", 
            "c-o: Test Org" );

        AddResponse response = adminConnection.add( subentry );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of a subentry under an IAP
     */
    @Test
    @Ignore
    public void testAddSubentryUnderIAP() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a IAP now
        Entry iap = LdifUtils.createEntry( 
            "ou=IAP,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: IAP",
            "administrativeRole: collectiveAttributeInnerArea" );

        AddResponse response = adminConnection.add( iap );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        // Add a subentry now
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );
    }
    
    
    /**
     * Test the addition of a subentry with 2 roles under an AAP
     */
    @Test
    @Ignore
    public void testAddSubentryWith2Roles() throws Exception
    {
        // First add an AAP
        createAAP( "ou=AAP,ou=system" ); 
        
        // Add a subentry now
        Entry subentry = LdifUtils.createEntry( 
            "cn=test,ou=AAP,ou=system", 
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
        
        DN subentryDN = new DN( "cn=test, ou=AAP,ou=system" );
        
        // Get back the subentry
        Entry addedSE = adminConnection.lookup( subentryDN, "+" );
        String subentryUUID = addedSE.get( "entryUUID" ).getString();
        
        DN apDn = new DN( "ou=AAP,ou=system" );
        apDn.normalize( service.getSchemaManager() );
        
        // Check that we have a ref to the added subentry in the two APs (AC and CA)
        AdministrativePoint apAC = service.getAccessControlAPCache().getElement( apDn );
        
        assertNotNull( apAC.getSubentries() );
        assertEquals( 1, apAC.getSubentries().size() );
        Subentry subentryAC = (Subentry)(apAC.getSubentries().toArray()[0]);
        
        assertEquals( subentryAC.getUuid(), subentryUUID );

        AdministrativePoint apCA = service.getCollectiveAttributeAPCache().getElement( apDn );
        
        assertNotNull( apCA.getSubentries() );
        assertEquals( 1, apCA.getSubentries().size() );
        Subentry subentryCA = (Subentry)(apCA.getSubentries().toArray()[0]);
        
        assertEquals( subentryCA.getUuid(), subentryUUID );
    }
    
    
    // ===================================================================
    // Test the Add operation for Entries
    // -------------------------------------------------------------------
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    /**
     * Test the addition of a SAP, SE and 2 entries
     */
    @Test
    @Ignore
    public void testAdd2Entries() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" );
        assertEquals( -1L, getCaSeqNumber( "ou=SAP,ou=system" ) );
        
        // Create a first entry
        Entry e1 = LdifUtils.createEntry( 
            "cn=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e1", 
            "sn: entry 1" );
        
        AddResponse response = adminConnection.add( e1 );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        assertEquals( -1L, getCaSeqNumber( "cn=e1,ou=SAP,ou=system" ) );
        
        // Create a second entry
        Entry e2 = LdifUtils.createEntry( 
            "cn=e2,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e2", 
            "sn: entry 2" );

        response = adminConnection.add( e2 );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        assertEquals( -1L, getCaSeqNumber( "cn=e2,ou=SAP,ou=system" ) );

        // Add a subentry now
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );
        
        // Get back the CA SeqNumber
        long caSeqNumber = getCaSeqNumber( "ou=SAP,ou=system" );
        
        assertTrue( caSeqNumber > -1L );
        
        // Create a third entry
        Entry e3 = LdifUtils.createEntry( 
            "cn=e3,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e3", 
            "sn: entry 3" );

        response = adminConnection.add( e3 );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // The CASeqNumber for this entry must be the same than it's AP
        assertEquals( caSeqNumber, getCaSeqNumber( "cn=e3,ou=SAP,ou=system" ) );

        // Now, check that when we read the other entries, their CA seqNumber is also updated
        assertEquals( caSeqNumber, getCaSeqNumber( "cn=e1,ou=SAP,ou=system" ) );
        assertEquals( caSeqNumber, getCaSeqNumber( "cn=e2,ou=SAP,ou=system" ) );
    }
    
    
    /**
     * Test an addition of AP, SE and entries with a selection
     */
    @Test
    @Ignore
    public void testAddComplex() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" );
        
        // Create a first entry
        Entry e1 = LdifUtils.createEntry( 
            "ou=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: e1" );
        
        AddResponse response = adminConnection.add( e1 );
        assertEquals( -1L, getCaSeqNumber( "ou=e1,ou=SAP,ou=system" ) );
        
        // Create a second entry
        Entry e2 = LdifUtils.createEntry( 
            "cn=e2,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e2", 
            "sn: entry 2" );

        response = adminConnection.add( e2 );
        assertEquals( -1L, getCaSeqNumber( "cn=e2,ou=SAP,ou=system" ) );

        // Add a subentry now, selecting only entries with a person AT
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{ specificationFilter item:person }" );
        
        // Get back the CA SeqNumber
        long caSeqNumber = getCaSeqNumber( "ou=SAP,ou=system" );
        
        assertTrue( caSeqNumber > -1L );
        
        // Create a third entry under e1
        Entry e3 = LdifUtils.createEntry( 
            "cn=e3,ou=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: person", 
            "cn: e3", 
            "sn: entry 3" );

        response = adminConnection.add( e3 );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        // Check the entries now: get the SAP SN and the Subentry UUID
        long sapSN = getCaSeqNumber( "ou=SAP,ou=system" );
        String uuid = adminConnection.lookup( "cn=test,ou=SAP,ou=system", "entryUUID" ).get( "entryUUID" ).getString();

        // E1
        e1 = adminConnection.lookup( "ou=e1,ou=SAP,ou=system", "+" );
        
        assertNull( e1.get( "CollectiveAttributeSubentriesUUID" ) );
        assertEquals( sapSN, Long.parseLong( e1.get( "CollectiveAttributeSeqNumber" ).getString() ) );
        
        // E2
        e2 = adminConnection.lookup( "cn=e2,ou=SAP,ou=system", "+" );
        
        assertEquals( uuid, e2.get( "CollectiveAttributeSubentriesUUID" ).getString() );
        assertEquals( sapSN, Long.parseLong( e2.get( "CollectiveAttributeSeqNumber" ).getString() ) );

        // E3
        e3 = adminConnection.lookup( "cn=e3,ou=e1,ou=SAP,ou=system", "+" );
        
        assertEquals( uuid, e3.get( "CollectiveAttributeSubentriesUUID" ).getString() );
        assertEquals( sapSN, Long.parseLong( e3.get( "CollectiveAttributeSeqNumber" ).getString() ) );
    }
}
