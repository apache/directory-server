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
package org.apache.directory.server.core.admin;


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
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.AddResponse;
import org.apache.directory.shared.ldap.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.message.ModifyResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the AdministrativePoint interceptor.
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
public class AdministrativePointServiceIT extends AbstractLdapTestUnit
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
    // Test the Add operation
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
     * Test the addition of some specific and inner for the same role at the same place
     * @throws Exception
     */
    @Test
    @Ignore
    public void testAddInnerAndSpecificRole() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: accessControlInnerArea" );

        AddResponse response = adminConnection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of some roles more than once
     * @throws Exception
     */
    @Test
    @Ignore
    public void testAddRoleMorehanOnce() throws Exception
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


    // -------------------------------------------------------------------
    // Test the Modify operation
    // -------------------------------------------------------------------
    /**
     * Test the addition of a ACSA to a CASA
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyAddSpecificArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "accessControlSpecificArea" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertTrue( entry.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
    }


    /**
     * Test the addition of a ACIA to a CASA
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyAddInnerArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "accessControlInnerArea" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertTrue( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "accessControlInnerArea" ) );
    }


    /**
     * Test the addition of a CAIA to a CASA
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyAddInnerAreaToSameSpecificArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "collectiveAttributeInnerArea" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of the same CASA
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyAddSameSpecificArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.ATTRIBUTE_OR_VALUE_EXISTS, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the deletion of all the roles
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyDeleteAll() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertFalse( entry.contains( "administrativeRole" ) );
    }


    /**
     * Test the deletion of all the roles
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyDeleteAll2() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "collectiveAttributeSpecificArea",
                "accessControlSpecificArea" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertFalse( entry.contains( "administrativeRole" ) );
    }


    /**
     * Test the deletion of some role
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyDeleteSomeRole() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "accessControlSpecificArea" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertTrue( entry.contains( "administrativeRole" ) );
        assertTrue( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertFalse( entry.contains( "administrativeRole", "accessControlSpecificArea" ) );
    }


    /**
     * Test the deletion of some role
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyDeleteSomeInexistingRole() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.NO_SUCH_ATTRIBUTE, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the a combined operation
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyCombined() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        adminConnection.add( caArea );

        // Add another specific area
        Modification modification1 = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );
        Modification modification2 = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );

        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification1, modification2,
            modification1 );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertTrue( entry.contains( "administrativeRole" ) );
        assertTrue( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertTrue( entry.contains( "administrativeRole", "triggerExecutionSpecificArea" ) );
    }


    /**
     * Test the replace modification : it's not supported
     * @throws Exception
     */
    @Test
    @Ignore
    public void testModifyReplace() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        adminConnection.add( caArea );

        // Try to modify it to an InnerArea
        Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        ModifyResponse response = adminConnection.modify( "ou=caArea, ou=system", modification );

        // Should fail
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    // -------------------------------------------------------------------
    // Test the Move operation
    // -------------------------------------------------------------------
    /**
     * Test the move of an autonomous area
     * @throws Exception
     */
    @Test
    @Ignore
    public void testMoveAutonomousArea() throws Exception
    {
        // Inject an AAA
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        adminConnection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        ModifyDnResponse response = adminConnection.move( "ou=autonomousArea, ou=system", "uid=admin, ou=system" );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    // -------------------------------------------------------------------
    // Test the Move And Rename operation
    // -------------------------------------------------------------------
    /**
     * Test the move and rename of an autonomous area
     * @throws Exception
     */
    @Test
    @Ignore
    public void testMoveAndRenameAutonomousArea() throws Exception
    {
        // Inject an AAA
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        adminConnection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        ModifyDnResponse response = adminConnection.moveAndRename( "ou=autonomousArea, ou=system",
            "ou=new autonomousArea, uid=admin, ou=system" );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    // -------------------------------------------------------------------
    // Test the Rename operation
    // -------------------------------------------------------------------
    /**
     * Test the renaming of an autonomous area
     * @throws Exception
     */
    @Test
    @Ignore
    public void testRenameAutonomousArea() throws Exception
    {
        // Inject an AAA
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        adminConnection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        ModifyDnResponse response = adminConnection.rename( "ou=autonomousArea, ou=system", "ou=new autonomousArea" );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
}
