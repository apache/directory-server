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
package org.apache.directory.server.core.schema;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "MetaAttributeTypeHandlerIT")
public class MetaAttributeTypeHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String DESCRIPTION0 = "A test attributeType";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.2.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.2.100001";
    private static final String DEPENDEE_OID = "1.3.6.1.4.1.18060.0.4.0.2.100002";

    private static LdapConnection connection;
    private SchemaManager schemaManager;


    @BeforeEach
    public void init() throws Exception
    {
        super.init();
        connection = IntegrationUtils.getAdminConnection( getService() );
        schemaManager = getService().getSchemaManager();
    }


    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------
    // Test Add operation
    // ----------------------------------------------------------------------
    /**
     * Test for DIRSHARED-60.
     * It is allowed to add an attribute type description without any matching rule.
     * Adding it via ou=schema partition worked. Adding it via the subschema subentry failed.
     */
    @Test
    public void testAddAttributeTypeWithoutMatchingRule() throws Exception
    {
        Dn dn = new Dn( "m-oid=2.5.4.58,ou=attributeTypes,cn=apachemeta,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid: 2.5.4.58",
            "m-name: attributeCertificateAttribute",
            "m-syntax: 1.3.6.1.4.1.1466.115.121.1.8",
            "m-description: attribute certificate use ;binary"
            );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( "2.5.4.58" ) );

        // Addition
        connection.add( entry );

        // Post-checks
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( "2.5.4.58" ) );
        assertEquals( "apachemeta", schemaManager.getAttributeTypeRegistry().getSchemaName( "2.5.4.58" ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddAttributeTypeToEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid", OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description", DESCRIPTION0,
            "m-equality: caseIgnoreMatch",
            "m-singleValue: FALSE",
            "m-usage: directoryOperation" );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ) );

        // Addition
        connection.add( entry );

        // Post-checks
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ) );
        assertEquals( "apachemeta", schemaManager.getAttributeTypeRegistry().getSchemaName( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddAttributeTypeToUnLoadedSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=notloaded,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid", OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description", DESCRIPTION0,
            "m-equality: caseIgnoreMatch",
            "m-singleValue: FALSE",
            "m-usage: directoryOperation" );

        try
        {
            connection.add( entry );
            fail( "Should not be there" );
        }
        catch ( LdapException nnfe )
        {
            // Expected result.
        }

        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "adding new attributeType to disabled schema should not register it into the registries" );

        // The added entry must not be present on disk
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testAddAttributeTypeToDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=nis,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid", OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description", DESCRIPTION0,
            "m-equality: caseIgnoreMatch",
            "m-singleValue: FALSE",
            "m-usage: directoryOperation" );

        connection.add( entry );

        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "adding new attributeType to disabled schema should not register it into the registries" );

        // The GlobalOidRegistries must not contain the AT
        assertFalse( schemaManager.getGlobalOidRegistry().contains( OID ) );

        // The added entry must be present on disk
        assertTrue( isOnDisk( dn ) );
    }


    /**
     * Test for DIRSERVER-1581.
     * Add an AT with DESC containing an ending space
     */
    @Test
    public void testAddAttributeTypeDescWithEndingSpace() throws Exception
    {
        Dn dn = new Dn( "m-oid=1.3.6.1.4.1.8104.1.1.37,ou=attributeTypes,cn=apachemeta,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid: 1.3.6.1.4.1.8104.1.1.37",
            "m-name: versionNumber",
            "m-description:: dmVyc2lvbk51bWJlciA=",
            "m-equality: caseIgnoreMatch",
            "m-substr: caseIgnoreSubstringsMatch",
            "m-syntax: 1.3.6.1.4.1.1466.115.121.1.8",
            "m-length: 0",
            "m-singleValue: TRUE"
            );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( "1.3.6.1.4.1.8104.1.1.37" ) );

        // Addition
        connection.add( entry );

        // Post-checks
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( "1.3.6.1.4.1.8104.1.1.37" ) );
        assertEquals( "apachemeta", schemaManager.getAttributeTypeRegistry().getSchemaName( "1.3.6.1.4.1.8104.1.1.37" ) );
        assertTrue( isOnDisk( dn ) );
    }


    // ----------------------------------------------------------------------
    // Test Delete operation
    // ----------------------------------------------------------------------
    @Test
    public void testDeleteAttributeTypeFromEnabledSchema() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        // Check in Registries
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should be removed from the registry after being deleted" );

        // Check on disk that the added SchemaObject exist
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        // Check in Registries
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should be removed from the registry after being deleted" );

        // Check on disk that the deleted SchemaObject does not exist anymore
        assertFalse( isOnDisk( dn ) );
    }


    /**
     * Try to delete an AT from a disabled schema. The AT is first
     * added, then deleted. The AT should be present on disk but not
     * in the registries before the deletion, and removed from disk
     * after the deletion.
     */
    @Test
    public void testDeleteAttributeTypeFromDisabledSchema() throws Exception
    {
        testAddAttributeTypeToDisabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=nis,ou=schema" );

        // Check in Registries
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should be removed from the registry after being deleted" );

        // Check on disk that the added SchemaObject exists
        assertTrue( isOnDisk( dn ) );

        // Remove the AT
        connection.delete( dn );

        // Check in Registries
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should be removed from the registry after being deleted" );
        assertFalse( schemaManager.getGlobalOidRegistry().contains( OID ) );

        // Check on disk that the deleted SchemaObject does not exist anymore
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );
        addDependeeAttributeType();

        try
        {
            connection.delete( dn );
            fail( "should not be able to delete a attributeType in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should still be in the registry after delete failure" );
    }


    // ----------------------------------------------------------------------
    // Test Modify operation
    // ----------------------------------------------------------------------
    @Test
    @Disabled
    public void testModifyAttributeTypeWithModificationItems() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        AttributeType at = schemaManager.lookupAttributeTypeRegistry( OID );
        assertEquals( DESCRIPTION0, at.getDescription() );
        assertEquals( SchemaConstants.INTEGER_SYNTAX, at.getSyntax().getOid() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Attribute attr = new DefaultAttribute( "m-description", DESCRIPTION1 );
        Modification mod1 = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attr );
        attr = new DefaultAttribute( "m-syntax", SchemaConstants.DIRECTORY_STRING_SYNTAX );
        Modification mod2 = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attr );

        connection.modify( dn, mod1, mod2 );

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType OID should still be present" );

        assertEquals( "attributeType schema should be set to apachemeta", "apachemeta", 
            schemaManager.getAttributeTypeRegistry().getSchemaName( OID ) );

        at = schemaManager.lookupAttributeTypeRegistry( OID );
        assertEquals( DESCRIPTION1, at.getDescription() );
        assertEquals( SchemaConstants.DIRECTORY_STRING_SYNTAX, at.getSyntax().getOid() );
    }


    @Test
    @Disabled
    public void testModifyAttributeTypeWithAttributes() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        AttributeType at = schemaManager.lookupAttributeTypeRegistry( OID );
        assertEquals( DESCRIPTION0, at.getDescription() );
        assertEquals( SchemaConstants.INTEGER_SYNTAX, at.getSyntax().getOid() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Modification mod1 = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultAttribute( "m-description", DESCRIPTION1 ) );

        Modification mod2 = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultAttribute( "m-syntax", SchemaConstants.DIRECTORY_STRING_SYNTAX ) );

        connection.modify( dn, mod1, mod2 );

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType OID should still be present" );

        assertEquals( "attributeType schema should be set to apachemeta", "apachemeta", 
            schemaManager.getAttributeTypeRegistry().getSchemaName( OID ) );

        at = schemaManager.lookupAttributeTypeRegistry( OID );
        assertEquals( DESCRIPTION1, at.getDescription() );
        assertEquals( SchemaConstants.DIRECTORY_STRING_SYNTAX, at.getSyntax().getOid() );
    }


    // ----------------------------------------------------------------------
    // Test Rename operation
    // ----------------------------------------------------------------------
    @Test
    @Disabled
    public void testRenameAttributeType() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        connection.rename( dn, rdn );

        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "old attributeType OID should be removed from the registry after being renamed" );

        schemaManager.lookupAttributeTypeRegistry( OID );
        fail( "attributeType lookup should fail after renaming the attributeType" );

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( NEW_OID ) );
    }


    @Test
    @Disabled
    public void testRenameAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );
        addDependeeAttributeType();

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );

        try
        {
            connection.rename( dn, rdn );
            fail( "should not be able to rename a attributeType in use" );
        }
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should still be in the registry after rename failure" );
    }


    // ----------------------------------------------------------------------
    // Test Move operation
    // ----------------------------------------------------------------------
    @Test
    @Disabled
    public void testMoveAttributeType() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ), "attributeType OID should still be present" );

        assertEquals( "attributeType schema should be set to apache not apachemeta", "apachemeta", 
            schemaManager.getAttributeTypeRegistry().getSchemaName( OID ) );
    }


    @Test
    @Disabled
    public void testMoveAttributeTypeAndChangeRdn() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ), "old attributeType OID should NOT be present" );

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( NEW_OID ), "new attributeType OID should be present" );

        assertEquals( "attributeType with new oid should have schema set to apache NOT apachemeta", "apachemeta", 
            schemaManager.getAttributeTypeRegistry().getSchemaName( NEW_OID ) );
    }


    @Test
    @Disabled
    public void testMoveAttributeTypeToTop() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Dn top = new Dn();
        top = top.add( "m-oid=" + OID );

        try
        {
            connection.move( dn, top );
            fail( "should not be able to move a attributeType up to ou=schema" );
        }
        catch ( LdapInvalidDnException e )
        {
            assertEquals( ResultCodeEnum.NAMING_VIOLATION, e.getResultCode() );
        }

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ), 
            "attributeType should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveAttributeTypeToComparatorContainer() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=comparators,cn=apachemeta,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a attributeType into comparators container" );
        }
        catch ( LdapInvalidDnException e )
        {
            assertEquals( ResultCodeEnum.NAMING_VIOLATION, e.getResultCode() );
        }

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveAttributeTypeToDisabledSchema() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        // nis is inactive by default
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=nis,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType OID should no longer be present" );
    }


    @Test
    @Disabled
    public void testMoveAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();
        addDependeeAttributeType();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apache,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a attributeType in use" );
        }
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveAttributeTypeAndChangeRdnWhenInUse() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();
        addDependeeAttributeType();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=attributeTypes,cn=apache,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a attributeType in use" );
        }
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( OID ),
            "attributeType should still be in the registry after move failure" );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------
    private void addDependeeAttributeType() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + DEPENDEE_OID + ",ou=attributeTypes,cn=apachemeta,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid", DEPENDEE_OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description", DESCRIPTION0,
            "m-equality: caseIgnoreMatch",
            "m-singleValue: FALSE",
            "m-usage: directoryOperation",
            "m-supAttributeType", OID );

        connection.add( entry );

        assertTrue( schemaManager.getAttributeTypeRegistry().contains( DEPENDEE_OID ) );
        assertEquals( "apachemeta", schemaManager.getAttributeTypeRegistry().getSchemaName( DEPENDEE_OID ) );
    }

    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------
    /*
    @Test
    @Disabled
    public void testMoveMatchingRuleToEnabledSchema() throws Exception
    {
        testAddAttributeTypeToDisabledSchema();

        // nis is inactive by default
        Dn dn = getAttributeTypeContainer( "nis" );
        dn.add( "m-oid=" + OID );

        assertFalse( "attributeType OID should NOT be present when added to disabled nis schema",
            schemaManager.getAttributeTypeRegistry().contains( OID ) );

        Dn newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( "m-oid=" + OID );

        connection.rename( dn, newdn );

        assertTrue( "attributeType OID should be present when moved to enabled schema",
            schemaManager.getAttributeTypeRegistry().contains( OID ) );

        assertEquals( "attributeType should be in apachemeta schema after move",
            schemaManager.getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
    }
    */
}
