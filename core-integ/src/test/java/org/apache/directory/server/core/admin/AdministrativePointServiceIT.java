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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.*;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.message.AddResponse;
import org.apache.directory.shared.ldap.model.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.model.message.ModifyResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
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
@Ignore
public class AdministrativePointServiceIT extends AbstractLdapTestUnit
{
    // The shared LDAP connection
    private static LdapConnection connection;


    @Before
    public void init() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( service );
    }


    @After
    public void shutdown() throws Exception
    {
        connection.close();
    }


    private Entry getAdminRole( String dn ) throws Exception
    {
        Entry lookup = connection.lookup( dn, "administrativeRole" );

        assertNotNull( lookup );

        return lookup;
    }


    // -------------------------------------------------------------------
    // Test the Add operation
    // -------------------------------------------------------------------
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
        AddResponse response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // Check that the entry is containing all the roles
        Entry entry = getAdminRole( "ou=autonomousArea, ou=system" );

        assertTrue( entry.contains( "administrativeRole", "autonomousArea" ) );
        assertFalse( entry.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertFalse( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertFalse( entry.contains( "administrativeRole", "2.5.23.4" ) );
        assertFalse( entry.contains( "administrativeRole", "triggerExecutionSpecificArea" ) );

        autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea2, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea2", 
            "administrativeRole: autonomousArea",
            "administrativeRole: accessControlSpecificArea", 
            "administrativeRole: collectiveAttributeInnerArea",
            "administrativeRole: 2.5.23.4", // This is the subSchemaSpecificArea OID
            "administrativeRole: TRIGGEREXECUTIONSPECIFICAREA" );

        // It should fail, as an autonomous area is already defining the specific areas
        response = connection.add( autonomousArea );

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

        AddResponse response = connection.add( autonomousArea );

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

        AddResponse response = connection.add( autonomousArea );

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
    public void testAddInvalidRole() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: accessControlBadArea",
            "administrativeRole: TRIGGEREXECUTIONINNERAREA" );

        AddResponse response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of some specific and inner for the same role at the same place
     * @throws Exception
     */
    @Test
    public void testAddInnerAndSpecificRole() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: accessControlInnerArea" );

        AddResponse response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the addition of some roles more than once
     * @throws Exception
     */
    @Test
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
        AddResponse response = connection.add( autonomousArea );

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
    public void testModifyAddSpecificArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "accessControlSpecificArea" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

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
    public void testModifyAddInnerArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry(
                "ou=caArea, ou=system",
                "ObjectClass: top",
                "ObjectClass: organizationalUnit",
                "ou: caArea",
                "administrativeRole: collectiveAttributeSpecificArea");

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "accessControlInnerArea" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

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
    public void testModifyAddInnerAreaToSameSpecificArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "collectiveAttributeInnerArea" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of the same CASA
     * @throws Exception
     */
    @Test
    public void testModifyAddSameSpecificArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.ATTRIBUTE_OR_VALUE_EXISTS, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the deletion of all the roles
     * @throws Exception
     */
    @Test
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

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertFalse( entry.contains( "administrativeRole" ) );
    }


    /**
     * Test the deletion of all the roles
     * @throws Exception
     */
    @Test
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

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "collectiveAttributeSpecificArea",
                "accessControlSpecificArea" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertFalse( entry.contains( "administrativeRole" ) );
    }


    /**
     * Test the deletion of some role
     * @throws Exception
     */
    @Test
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

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "accessControlSpecificArea" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

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

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

        assertEquals( ResultCodeEnum.NO_SUCH_ATTRIBUTE, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the a combined operation
     * @throws Exception
     */
    @Test
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

        connection.add( caArea );

        // Add another specific area
        Modification modification1 = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );
        Modification modification2 = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );

        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification1, modification2,
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
    public void testModifyReplace() throws Exception
    {
        // Inject an CASA
        Entry caArea = LdifUtils.createEntry( 
            "ou=caArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: caArea", 
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Try to modify it to an InnerArea
        Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultEntryAttribute( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        ModifyResponse response = connection.modify( "ou=caArea, ou=system", modification );

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
    public void testMoveAutonomousArea() throws Exception
    {
        // Inject an AAA
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        connection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        ModifyDnResponse response = connection.move( "ou=autonomousArea, ou=system", "uid=admin, ou=system" );

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
    public void testMoveAndRenameAutonomousArea() throws Exception
    {
        // Inject an AAA
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        connection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        ModifyDnResponse response = connection.moveAndRename( "ou=autonomousArea, ou=system",
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
    public void testRenameAutonomousArea() throws Exception
    {
        // Inject an AAA
        Entry autonomousArea = LdifUtils.createEntry( 
            "ou=autonomousArea, ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: autonomousArea", 
            "administrativeRole: autonomousArea" );

        connection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        ModifyDnResponse response = connection.rename( "ou=autonomousArea, ou=system", "ou=new autonomousArea" );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
}
