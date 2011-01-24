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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.naming.NameNotFoundException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.comparators.BooleanComparator;
import org.apache.directory.shared.ldap.schema.comparators.StringComparator;
import org.apache.directory.shared.ldap.util.JndiUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "MetaComparatorHandlerIT")
public class MetaComparatorHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";

    public static SchemaManager schemaManager;

    class DummyMR extends MatchingRule
    {
        public DummyMR()
        {
            super( OID );
            addName( "dummy" );
        }

        private static final long serialVersionUID = 1L;
    }


    @Before
    public void setup()
    {
        schemaManager = service.getSchemaManager();
    }


    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------
    // Test Add operation
    // ----------------------------------------------------------------------
    @Test
    public void testAddComparatorToEnabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes( "objectClass: top", "objectClass: metaTop",
            "objectClass: metaComparator", "m-fqcn: " + StringComparator.class.getName(), "m-oid: " + OID,
            "m-description: A test comparator" );

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( service.getSchemaManager().getComparatorRegistry().contains( OID ) );

        // Addition
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        // Post-checks
        assertTrue( schemaManager.getComparatorRegistry().contains( OID ) );
        assertEquals( schemaManager.getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = schemaManager.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, StringComparator.class );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddComparatorToDisabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes( "objectClass: top", "objectClass: metaTop",
            "objectClass: metaComparator", "m-fqcn: " + StringComparator.class.getName(), "m-oid: " + OID,
            "m-description: A test comparator" );

        // nis is by default inactive
        Dn dn = getComparatorContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( service.getSchemaManager().getComparatorRegistry().contains( OID ) );

        // Addition
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        // Post-checks
        assertFalse( "adding new comparator to disabled schema should not register it into the registries",
            schemaManager.getComparatorRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddComparatorToUnloadedSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes( "objectClass: top", "objectClass: metaTop",
            "objectClass: metaComparator", "m-fqcn: " + StringComparator.class.getName(), "m-oid: " + OID,
            "m-description: A test comparator" );

        // nis is by default inactive
        Dn dn = getComparatorContainer( "notloaded" );
        dn = dn.add( "m-oid" + "=" + OID );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( service.getSchemaManager().getComparatorRegistry().contains( OID ) );

        // Addition
        try
        {
            getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail( "Should not be there" );
        }
        catch ( NameNotFoundException nnfe )
        {
            // Expected result.
        }

        // Post-checks
        assertFalse( "adding new comparator to disabled schema should not register it into the registries",
            schemaManager.getComparatorRegistry().contains( OID ) );
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testAddComparatorWithByteCodeToEnabledSchema() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummyComparator.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }

        Attributes attrs = LdifUtils.createAttributes( "objectClass: top", "objectClass: metaTop",
            "objectClass: metaComparator",
            "m-fqcn: org.apache.directory.shared.ldap.schema.comparators.DummyComparator", "m-bytecode", out
                .toByteArray(), "m-oid", OID, "m-description: A test comparator" );

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( service.getSchemaManager().getComparatorRegistry().contains( OID ) );

        // Addition
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        // Post-checks
        assertTrue( schemaManager.getComparatorRegistry().contains( OID ) );
        assertEquals( schemaManager.getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = schemaManager.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz.getName(), "org.apache.directory.shared.ldap.schema.comparators.DummyComparator" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddComparatorWithByteCodeToDisabledSchema() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummyComparator.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }

        Attributes attrs = LdifUtils.createAttributes( "objectClass: top", "objectClass: metaTop",
            "objectClass: metaComparator",
            "m-fqcn: org.apache.directory.shared.ldap.schema.comparators.DummyComparator", "m-bytecode", out
                .toByteArray(), "m-oid", OID, "m-description: A test comparator" );

        // nis is by default inactive
        Dn dn = getComparatorContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( service.getSchemaManager().getComparatorRegistry().contains( OID ) );

        // Addition
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        // Post-checks
        assertFalse( "adding new comparator to disabled schema should not register it into the registries",
            schemaManager.getComparatorRegistry().contains( OID ) );

        assertTrue( isOnDisk( dn ) );
    }


    // ----------------------------------------------------------------------
    // Test Delete operation
    // ----------------------------------------------------------------------
    @Test
    public void testDeleteComparatorFromEnabledSchema() throws Exception
    {
        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        testAddComparatorToEnabledSchema();

        // Pre-checks
        assertTrue( schemaManager.getComparatorRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );

        // Deletion
        getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );

        // Post-checks
        assertFalse( "comparator should be removed from the registry after being deleted", schemaManager
            .getComparatorRegistry().contains( OID ) );

        try
        {
            schemaManager.getComparatorRegistry().lookup( OID );
            fail( "comparator lookup should fail after deleting the comparator" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteComparatorFromDisabledSchema() throws Exception
    {
        Dn dn = getComparatorContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddComparatorToDisabledSchema();

        // Pre-checks
        assertFalse( "comparator should be removed from the registry after being deleted", schemaManager
            .getComparatorRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );

        // Deletion
        getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );

        // Post-checks
        assertFalse( "comparator should be removed from the registry after being deleted", schemaManager
            .getComparatorRegistry().contains( OID ) );

        try
        {
            schemaManager.getComparatorRegistry().lookup( OID );
            fail( "comparator lookup should fail after deleting the comparator" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteComparatorWhenInUse() throws Exception
    {
        Dn cDn = getComparatorContainer( "apachemeta" );
        cDn = cDn.add( "m-oid" + "=" + OID );

        // Create a new Comparator
        testAddComparatorToEnabledSchema();
        assertTrue( isOnDisk( cDn ) );
        assertTrue( service.getSchemaManager().getComparatorRegistry().contains( OID ) );

        // Create a MR using this comparator
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaMatchingRule",
            "m-oid", OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description: test" );

        Dn mrDn = getMatchingRuleContainer( "apachemeta" );
        mrDn = mrDn.add( "m-oid" + "=" + OID );

        // Pre-checks
        assertFalse( isOnDisk( mrDn ) );
        assertFalse( service.getSchemaManager().getMatchingRuleRegistry().contains( OID ) );

        // MatchingRule Addition
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( mrDn ), attrs );

        // Post-checks
        assertTrue( isOnDisk( mrDn ) );
        assertTrue( service.getSchemaManager().getMatchingRuleRegistry().contains( OID ) );

        try
        {
            getSchemaContext( service ).destroySubcontext( JndiUtils.toName( cDn ) );
            fail( "should not be able to delete a comparator in use" );
        }
        catch ( OperationNotSupportedException e )
        {
        }

        assertTrue( "comparator should still be in the registry after delete failure", schemaManager
            .getComparatorRegistry().contains( OID ) );
    }


    // ----------------------------------------------------------------------
    // Test Modify operation
    // ----------------------------------------------------------------------
    @Test
    @Ignore
    public void testModifyComparatorWithModificationItems() throws Exception
    {
        testAddComparatorToEnabledSchema();

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-fqcn", BooleanComparator.class.getName() );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( JndiUtils.toName( dn ), mods );

        assertTrue( "comparator OID should still be present", schemaManager.getComparatorRegistry().contains( OID ) );

        assertEquals( "comparator schema should be set to apachemeta", schemaManager.getComparatorRegistry()
            .getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = schemaManager.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, BooleanComparator.class );
    }


    @Test
    @Ignore
    public void testModifyComparatorWithAttributes() throws Exception
    {
        testAddComparatorToEnabledSchema();

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Attributes mods = new BasicAttributes( true );
        mods.put( "m-fqcn", BooleanComparator.class.getName() );
        getSchemaContext( service ).modifyAttributes( JndiUtils.toName(dn), DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "comparator OID should still be present", schemaManager.getComparatorRegistry().contains( OID ) );

        assertEquals( "comparator schema should be set to apachemeta", schemaManager.getComparatorRegistry()
            .getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = schemaManager.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, BooleanComparator.class );
    }


    // ----------------------------------------------------------------------
    // Test Rename operation
    // ----------------------------------------------------------------------
    @Test
    @Ignore
    public void testRenameComparator() throws Exception
    {
        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddComparatorToEnabledSchema();

        Dn newdn = getComparatorContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "old comparator OID should be removed from the registry after being renamed", schemaManager
            .getComparatorRegistry().contains( OID ) );

        try
        {
            schemaManager.getComparatorRegistry().lookup( OID );
            fail( "comparator lookup should fail after deleting the comparator" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getComparatorRegistry().contains( NEW_OID ) );
        Class<?> clazz = schemaManager.getComparatorRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }


    @Test
    @Ignore
    public void testRenameComparatorWhenInUse() throws Exception
    {
        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddComparatorToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Dn newdn = getComparatorContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to rename a comparator in use" );
        }
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after rename failure", schemaManager
            .getComparatorRegistry().contains( OID ) );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        service.getSchemaManager().getGlobalOidRegistry().unregister( OID );
    }


    // ----------------------------------------------------------------------
    // Test Move operation
    // ----------------------------------------------------------------------
    @Test
    @Ignore
    public void testMoveComparator() throws Exception
    {
        testAddComparatorToEnabledSchema();

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getComparatorContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertTrue( "comparator OID should still be present", schemaManager.getComparatorRegistry().contains( OID ) );

        assertEquals( "comparator schema should be set to apache not apachemeta", schemaManager.getComparatorRegistry()
            .getSchemaName( OID ), "apache" );

        Class<?> clazz = schemaManager.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }


    @Test
    @Ignore
    public void testMoveComparatorAndChangeRdn() throws Exception
    {
        testAddComparatorToEnabledSchema();

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getComparatorContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "old comparator OID should NOT be present", schemaManager.getComparatorRegistry().contains( OID ) );

        assertTrue( "new comparator OID should be present", schemaManager.getComparatorRegistry().contains( NEW_OID ) );

        assertEquals( "comparator with new oid should have schema set to apache NOT apachemeta", schemaManager
            .getComparatorRegistry().getSchemaName( NEW_OID ), "apache" );

        Class<?> clazz = schemaManager.getComparatorRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Comparator
    // ----------------------------------------------------------------------
    @Test
    @Ignore
    public void testMoveComparatorWhenInUse() throws Exception
    {
        testAddComparatorToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getComparatorContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a comparator in use" );
        }
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after move failure", schemaManager
            .getComparatorRegistry().contains( OID ) );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        service.getSchemaManager().getGlobalOidRegistry().unregister( OID );
    }


    @Test
    @Ignore
    public void testMoveComparatorAndChangeRdnWhenInUse() throws Exception
    {
        testAddComparatorToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getComparatorContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a comparator in use" );
        }
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after move failure", schemaManager
            .getComparatorRegistry().contains( OID ) );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        service.getSchemaManager().getGlobalOidRegistry().unregister( OID );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------

    @Test
    @Ignore
    public void testMoveComparatorToTop() throws Exception
    {
        testAddComparatorToEnabledSchema();

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn top = new Dn();
        top = top.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( top ) );
            fail( "should not be able to move a comparator up to ou=schema" );
        }
        catch ( LdapInvalidDnException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "comparator should still be in the registry after move failure", schemaManager
            .getComparatorRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveComparatorToNormalizers() throws Exception
    {
        testAddComparatorToEnabledSchema();

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = new Dn( "ou=normalizers,cn=apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a comparator up to normalizers container" );
        }
        catch ( LdapInvalidDnException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "comparator should still be in the registry after move failure", schemaManager
            .getComparatorRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveComparatorToDisabledSchema() throws Exception
    {
        testAddComparatorToEnabledSchema();

        Dn dn = getComparatorContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        // nis is inactive by default
        Dn newdn = getComparatorContainer( "nis" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "comparator OID should no longer be present", schemaManager.getComparatorRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveComparatorToEnabledSchema() throws Exception
    {
        testAddComparatorToDisabledSchema();

        // nis is inactive by default
        Dn dn = getComparatorContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );

        assertFalse( "comparator OID should NOT be present when added to disabled nis schema", schemaManager
            .getComparatorRegistry().contains( OID ) );

        Dn newdn = getComparatorContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertTrue( "comparator OID should be present when moved to enabled schema", schemaManager
            .getComparatorRegistry().contains( OID ) );

        assertEquals( "comparator should be in apachemeta schema after move", schemaManager.getComparatorRegistry()
            .getSchemaName( OID ), "apachemeta" );
    }
}
