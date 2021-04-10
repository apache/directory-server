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


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapAttributeInUseException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test cases for the AdministrativePoint interceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "AdministrativePointServiceIT")
@Disabled
public class AdministrativePointServiceIT extends AbstractLdapTestUnit
{
    // The shared LDAP connection
    private static LdapConnection connection;


    @BeforeEach
    public void init() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );
    }


    @AfterEach
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
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: autonomousArea" );

        // It should succeed
        connection.add( autonomousArea );

        assertTrue( connection.exists( "ou=autonomousArea, ou=system" ) );

        // Check that the entry is containing all the roles
        Entry entry = getAdminRole( "ou=autonomousArea, ou=system" );

        assertTrue( entry.contains( "administrativeRole", "autonomousArea" ) );
        assertFalse( entry.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertFalse( entry.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertFalse( entry.contains( "administrativeRole", "2.5.23.4" ) );
        assertFalse( entry.contains( "administrativeRole", "triggerExecutionSpecificArea" ) );

        autonomousArea = new DefaultEntry(
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
        try
        {
            connection.add( autonomousArea );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the addition of some specific area
     * @throws Exception
     */
    @Test
    public void testAddSpecificAreas() throws Exception
    {
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: TRIGGEREXECUTIONSPECIFICAREA" );

        connection.add( autonomousArea );

        assertTrue( connection.exists( "ou=autonomousArea, ou=system" ) );

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
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: accessControlINNERArea",
            "administrativeRole: TRIGGEREXECUTIONINNERAREA" );

        connection.add( autonomousArea );

        assertTrue( connection.exists( "ou=autonomousArea, ou=system" ) );

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
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: accessControlBadArea",
            "administrativeRole: TRIGGEREXECUTIONINNERAREA" );

        try
        {
            connection.add( autonomousArea );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the addition of some specific and inner for the same role at the same place
     * @throws Exception
     */
    @Test
    public void testAddInnerAndSpecificRole() throws Exception
    {
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: accessControlInnerArea" );

        try
        {
            connection.add( autonomousArea );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the addition of some roles more than once
     * @throws Exception
     */
    @Test
    public void testAddRoleMorehanOnce() throws Exception
    {
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: autonomousArea",
            "administrativeRole: 2.5.23.1" );

        // It should not succeed
        try
        {
            connection.add( autonomousArea );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
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
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "accessControlSpecificArea" ) );
        connection.modify( "ou=caArea, ou=system", modification );

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
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "accessControlInnerArea" ) );
        connection.modify( "ou=caArea, ou=system", modification );

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
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "collectiveAttributeInnerArea" ) );

        try
        {
            connection.modify( "ou=caArea, ou=system", modification );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the addition of the same CASA
     * @throws Exception
     */
    @Test
    public void testModifyAddSameSpecificArea() throws Exception
    {
        // Inject an CASA
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "collectiveAttributeSpecificArea" ) );

        try
        {
            connection.modify( "ou=caArea, ou=system", modification );
            fail();
        }
        catch ( LdapAttributeInUseException lnsae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the deletion of all the roles
     * @throws Exception
     */
    @Test
    public void testModifyDeleteAll() throws Exception
    {
        // Inject an CASA
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole" ) );
        connection.modify( "ou=caArea, ou=system", modification );

        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertFalse( entry.containsAttribute( "administrativeRole" ) );
    }


    /**
     * Test the deletion of all the roles
     * @throws Exception
     */
    @Test
    public void testModifyDeleteAll2() throws Exception
    {
        // Inject an CASA
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "collectiveAttributeSpecificArea",
                "accessControlSpecificArea" ) );
        connection.modify( "ou=caArea, ou=system", modification );

        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertFalse( entry.containsAttribute( "administrativeRole" ) );
    }


    /**
     * Test the deletion of some role
     * @throws Exception
     */
    @Test
    public void testModifyDeleteSomeRole() throws Exception
    {
        // Inject an CASA
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "accessControlSpecificArea" ) );
        connection.modify( "ou=caArea, ou=system", modification );

        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertTrue( entry.containsAttribute( "administrativeRole" ) );
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
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );

        try
        {
            connection.modify( "ou=caArea, ou=system", modification );
            fail();
        }
        catch ( LdapNoSuchAttributeException lnsae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the a combined operation
     * @throws Exception
     */
    @Test
    public void testModifyCombined() throws Exception
    {
        // Inject an CASA
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea",
            "administrativeRole: accessControlSpecificArea" );

        connection.add( caArea );

        // Add another specific area
        Modification modification1 = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );
        Modification modification2 = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "triggerExecutionSpecificArea" ) );

        connection.modify( "ou=caArea, ou=system", modification1, modification2, modification1 );

        Entry entry = getAdminRole( "ou=caArea, ou=system" );

        assertTrue( entry.containsAttribute( "administrativeRole" ) );
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
        Entry caArea = new DefaultEntry(
            "ou=caArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: caArea",
            "administrativeRole: collectiveAttributeSpecificArea" );

        connection.add( caArea );

        // Try to modify it to an InnerArea
        Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole", "collectiveAttributeSpecificArea" ) );

        try
        {
            connection.modify( "ou=caArea, ou=system", modification );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
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
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: autonomousArea" );

        connection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        try
        {
            connection.move( "ou=autonomousArea, ou=system", "uid=admin, ou=system" );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
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
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: autonomousArea" );

        connection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        try
        {
            connection.moveAndRename( "ou=autonomousArea, ou=system", "ou=new autonomousArea, uid=admin, ou=system" );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
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
        Entry autonomousArea = new DefaultEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: autonomousArea" );

        connection.add( autonomousArea );

        // It should fail, as we haven't injected all the roles
        try
        {
            connection.rename( "ou=autonomousArea, ou=system", "ou=new autonomousArea" );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
    }
}
