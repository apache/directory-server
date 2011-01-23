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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NameNotFoundException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.JndiUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "MetaAttributeTypeHandlerIT")
public class MetaAttributeTypeHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String DESCRIPTION0 = "A test attributeType";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.2.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.2.100001";
    private static final String DEPENDEE_OID = "1.3.6.1.4.1.18060.0.4.0.2.100002";

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
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid: 2.5.4.58",
            "m-name: attributeCertificateAttribute",
            "m-syntax: 1.3.6.1.4.1.1466.115.121.1.8",
            "m-description: attribute certificate use ;binary"
         );

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=2.5.4.58" );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( service.getSchemaManager().getAttributeTypeRegistry().contains( "2.5.4.58" ) );

        // Addition
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        // Post-checks
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( "2.5.4.58" ) );
        assertEquals( service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( "2.5.4.58" ), "apachemeta" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddAttributeTypeToEnabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid:" + OID,
            "m-syntax:" + SchemaConstants.INTEGER_SYNTAX,
            "m-description:" + DESCRIPTION0,
            "m-equality: caseIgnoreMatch",
            "m-singleValue: FALSE",
            "m-usage: directoryOperation" );

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        // Addition
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        // Post-checks
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
        assertEquals( service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddAttributeTypeToUnLoadedSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid:" + OID,
            "m-syntax:" + SchemaConstants.INTEGER_SYNTAX,
            "m-description:" + DESCRIPTION0,
            "m-equality: caseIgnoreMatch",
            "m-singleValue: FALSE",
            "m-usage: directoryOperation" );

        Dn dn = getAttributeTypeContainer( "notloaded" );
        dn = dn.add( "m-oid=" + OID );

        try
        {
            getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail( "Should not be there" );
        }
        catch( NameNotFoundException nnfe )
        {
            // Expected result.
        }

        assertFalse( "adding new attributeType to disabled schema should not register it into the registries",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        // The added entry must not be present on disk
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testAddAttributeTypeToDisabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid:" + OID,
            "m-syntax:" + SchemaConstants.INTEGER_SYNTAX,
            "m-description:" + DESCRIPTION0,
            "m-equality: caseIgnoreMatch",
            "m-singleValue: FALSE",
            "m-usage: directoryOperation" );

        Dn dn = getAttributeTypeContainer( "nis" );
        dn = dn.add( "m-oid=" + OID );

        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertFalse( "adding new attributeType to disabled schema should not register it into the registries",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        // The GlobalOidRegistries must not contain the AT
        assertFalse( service.getSchemaManager().getGlobalOidRegistry().contains( OID ) );

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
        Attributes attrs = LdifUtils.createAttributes(
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

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=1.3.6.1.4.1.8104.1.1.37" );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( service.getSchemaManager().getAttributeTypeRegistry().contains( "1.3.6.1.4.1.8104.1.1.37" ) );

        // Addition
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        // Post-checks
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( "1.3.6.1.4.1.8104.1.1.37" ) );
        assertEquals( service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( "1.3.6.1.4.1.8104.1.1.37" ), "apachemeta" );
        assertTrue( isOnDisk( dn ) );
    }


    // ----------------------------------------------------------------------
    // Test Delete operation
    // ----------------------------------------------------------------------
    @Test
    public void testDeleteAttributeTypeFromEnabledSchema() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        // Check in Registries
        assertTrue( "attributeType should be removed from the registry after being deleted",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        // Check on disk that the added SchemaObject exist
        assertTrue( isOnDisk( dn ) );

        getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );

        // Check in Registries
        assertFalse( "attributeType should be removed from the registry after being deleted",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

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

        Dn dn = getAttributeTypeContainer( "nis" );
        dn = dn.add( "m-oid=" + OID );

        // Check in Registries
        assertFalse( "attributeType should be removed from the registry after being deleted",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        // Check on disk that the added SchemaObject exists
        assertTrue( isOnDisk( dn ) );

        // Remove the AT
        getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );

        // Check in Registries
        assertFalse( "attributeType should be removed from the registry after being deleted",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
        assertFalse( service.getSchemaManager().getGlobalOidRegistry().contains( OID ) );

        // Check on disk that the deleted SchemaObject does not exist anymore
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );
        addDependeeAttributeType();

        try
        {
            getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );
            fail( "should not be able to delete a attributeType in use" );
        }
        catch( OperationNotSupportedException e )
        {
        }

        assertTrue( "attributeType should still be in the registry after delete failure",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
    }


    // ----------------------------------------------------------------------
    // Test Modify operation
    // ----------------------------------------------------------------------
    @Test
    @Ignore
    public void testModifyAttributeTypeWithModificationItems() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        AttributeType at = service.getSchemaManager().lookupAttributeTypeRegistry( OID );
        assertEquals( at.getDescription(), DESCRIPTION0 );
        assertEquals( at.getSyntax().getOid(), SchemaConstants.INTEGER_SYNTAX );

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        ModificationItem[] mods = new ModificationItem[2];
        Attribute attr = new BasicAttribute( "m-description", DESCRIPTION1 );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new BasicAttribute( "m-syntax", SchemaConstants.DIRECTORY_STRING_SYNTAX );
        mods[1] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( JndiUtils.toName( dn ), mods );

        assertTrue( "attributeType OID should still be present",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        assertEquals( "attributeType schema should be set to apachemeta",
            service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );

        at = service.getSchemaManager().lookupAttributeTypeRegistry( OID );
        assertEquals( at.getDescription(), DESCRIPTION1 );
        assertEquals( at.getSyntax().getOid(), SchemaConstants.DIRECTORY_STRING_SYNTAX );
    }


    @Test
    @Ignore
    public void testModifyAttributeTypeWithAttributes() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        AttributeType at = service.getSchemaManager().lookupAttributeTypeRegistry( OID );
        assertEquals( at.getDescription(), DESCRIPTION0 );
        assertEquals( at.getSyntax().getOid(), SchemaConstants.INTEGER_SYNTAX );

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        Attributes mods = new BasicAttributes( true );
        mods.put( "m-description", DESCRIPTION1 );
        mods.put( "m-syntax", SchemaConstants.DIRECTORY_STRING_SYNTAX );
        getSchemaContext( service ).modifyAttributes( JndiUtils.toName( dn ), DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "attributeType OID should still be present",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        assertEquals( "attributeType schema should be set to apachemeta",
            service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );

        at = service.getSchemaManager().lookupAttributeTypeRegistry( OID );
        assertEquals( at.getDescription(), DESCRIPTION1 );
        assertEquals( at.getSyntax().getOid(), SchemaConstants.DIRECTORY_STRING_SYNTAX );
    }


    // ----------------------------------------------------------------------
    // Test Rename operation
    // ----------------------------------------------------------------------
    @Test
    @Ignore
    public void testRenameAttributeType() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        LdapContext schemaRoot = getSchemaContext( service );
        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        Dn newdn = getAttributeTypeContainer( "apachemeta" );
        dn = newdn.add( "m-oid=" + NEW_OID );
        schemaRoot.rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "old attributeType OID should be removed from the registry after being renamed",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        service.getSchemaManager().getAttributeTypeRegistry().lookup( OID );
        fail( "attributeType lookup should fail after renaming the attributeType" );

        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( NEW_OID ) );
    }


    @Test
    @Ignore
    public void testRenameAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );
        addDependeeAttributeType();

        Dn newdn = getAttributeTypeContainer( "apachemeta" );
        newdn = newdn.add( "m-oid=" + NEW_OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to rename a attributeType in use" );
        }
        catch( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after rename failure",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
    }


    // ----------------------------------------------------------------------
    // Test Move operation
    // ----------------------------------------------------------------------
    @Test
    @Ignore
    public void testMoveAttributeType() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        Dn newdn = getAttributeTypeContainer( "apache" );
        newdn = newdn.add( "m-oid=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName(newdn) );

        assertTrue( "attributeType OID should still be present",
                service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        assertEquals( "attributeType schema should be set to apache not apachemeta",
            service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( OID ), "apache" );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeAndChangeRdn() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        Dn newdn = getAttributeTypeContainer( "apache" );
        newdn = newdn.add( "m-oid=" + NEW_OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "old attributeType OID should NOT be present",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        assertTrue( "new attributeType OID should be present",
            service.getSchemaManager().getAttributeTypeRegistry().contains( NEW_OID ) );

        assertEquals( "attributeType with new oid should have schema set to apache NOT apachemeta",
            service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( NEW_OID ), "apache" );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeToTop() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        Dn top = new Dn();
        top = top.add( "m-oid=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( top ) );
            fail( "should not be able to move a attributeType up to ou=schema" );
        }
        catch( LdapInvalidDnException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "attributeType should still be in the registry after move failure",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeToComparatorContainer() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        Dn newdn = new Dn( "ou=comparators,cn=apachemeta" );
        newdn = newdn.add( "m-oid=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a attributeType into comparators container" );
        }
        catch( LdapInvalidDnException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "attributeType should still be in the registry after move failure",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeToDisabledSchema() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        // nis is inactive by default
        Dn newdn = getAttributeTypeContainer( "nis" );
        newdn = newdn.add( "m-oid=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "attributeType OID should no longer be present",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();
        addDependeeAttributeType();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        Dn newdn = getAttributeTypeContainer( "apache" );
        newdn = newdn.add( "m-oid=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a attributeType in use" );
        }
        catch( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after move failure",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeAndChangeRdnWhenInUse() throws Exception
    {
        testAddAttributeTypeToEnabledSchema();
        addDependeeAttributeType();

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + OID );

        Dn newdn = getAttributeTypeContainer( "apache" );
        newdn = newdn.add( "m-oid=" + NEW_OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a attributeType in use" );
        }
        catch( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after move failure",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------
    private void addDependeeAttributeType() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
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

        Dn dn = getAttributeTypeContainer( "apachemeta" );
        dn = dn.add( "m-oid=" + DEPENDEE_OID );
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( DEPENDEE_OID ) );
        assertEquals( service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( DEPENDEE_OID ), "apachemeta" );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------
    /*
    @Test
    @Ignore
    public void testMoveMatchingRuleToEnabledSchema() throws Exception
    {
        testAddAttributeTypeToDisabledSchema();

        // nis is inactive by default
        Dn dn = getAttributeTypeContainer( "nis" );
        dn.add( "m-oid=" + OID );

        assertFalse( "attributeType OID should NOT be present when added to disabled nis schema",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        Dn newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( "m-oid=" + OID );

        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "attributeType OID should be present when moved to enabled schema",
            service.getSchemaManager().getAttributeTypeRegistry().contains( OID ) );

        assertEquals( "attributeType should be in apachemeta schema after move",
            service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
    }
    */
}
