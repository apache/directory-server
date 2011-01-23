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
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
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
@CreateDS(name = "MetaNormalizerHandlerIT")
public class MetaNormalizerHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";

    public static SchemaManager schemaManager;


    @Before
    public void setup()
    {
        schemaManager = service.getSchemaManager();
    }


    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------
    @Test
    public void testAddNormalizerToEnabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", NoOpNormalizer.class.getName(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) );
        assertEquals( schemaManager.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddNormalizerToDisabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", NoOpNormalizer.class.getName(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        // nis is by default inactive
        Dn dn = getNormalizerContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertFalse( "adding new normalizer to disabled schema should not register it into the registries",
            schemaManager.getNormalizerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddNormalizerToUnloadedSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", NoOpNormalizer.class.getName(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        // nis is by default inactive
        Dn dn = getNormalizerContainer( "notloaded" );
        dn = dn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail( "Should not be there" );
        }
        catch( NameNotFoundException nnfe )
        {
            // Expected result.
        }

        assertFalse( "adding new normalizer to disabled schema should not register it into the registries",
            schemaManager.getNormalizerRegistry().contains( OID ) );
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testAddNormalizerWithByteCodeToEnabledSchema() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummyNormalizer.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }

        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", "org.apache.directory.shared.ldap.schema.normalizers.DummyNormalizer",
            "m-bytecode", out.toByteArray(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) );
        assertEquals( schemaManager.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz.getName(), "org.apache.directory.shared.ldap.schema.normalizers.DummyNormalizer" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddNormalizerWithByteCodeToDisabledSchema() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummyNormalizer.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }

        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", "org.apache.directory.shared.ldap.schema.normalizers.DummyNormalizer",
            "m-bytecode", out.toByteArray(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        Dn dn = getNormalizerContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteNormalizerFromEnabledSchema() throws Exception
    {
        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddNormalizerToEnabledSchema();

        assertTrue( "normalizer should be removed from the registry after being deleted",
            schemaManager.getNormalizerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );

        getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );

        assertFalse( "normalizer should be removed from the registry after being deleted",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        try
        {
            schemaManager.getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteNormalizerFromDisabledSchema() throws Exception
    {
        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddNormalizerToEnabledSchema();

        assertTrue( "normalizer should be removed from the registry after being deleted",
            schemaManager.getNormalizerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );

        getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );

        assertFalse( "normalizer should be removed from the registry after being deleted",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        try
        {
            schemaManager.getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    @Ignore
    public void testRenameNormalizer() throws Exception
    {
        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddNormalizerToEnabledSchema();

        Dn newdn = getNormalizerContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName(newdn) );

        assertFalse( "old normalizer OID should be removed from the registry after being renamed",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaManager.getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch( LdapException e )
        {
        }

        assertTrue( schemaManager.getNormalizerRegistry().contains( NEW_OID ) );
        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    @Test
    @Ignore
    public void testMoveNormalizer() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getNormalizerContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertTrue( "normalizer OID should still be present",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        assertEquals( "normalizer schema should be set to apache not apachemeta",
            schemaManager.getNormalizerRegistry().getSchemaName( OID ), "apache" );

        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    @Test
    @Ignore
    public void testMoveNormalizerAndChangeRdn() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getNormalizerContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "old normalizer OID should NOT be present",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        assertTrue( "new normalizer OID should be present",
            schemaManager.getNormalizerRegistry().contains( NEW_OID ) );

        assertEquals( "normalizer with new oid should have schema set to apache NOT apachemeta",
            schemaManager.getNormalizerRegistry().getSchemaName( NEW_OID ), "apache" );

        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    @Test
    @Ignore
    public void testModifyNormalizerWithModificationItems() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-fqcn", DeepTrimNormalizer.class.getName() );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( JndiUtils.toName( dn ), mods );

        assertTrue( "normalizer OID should still be present",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        assertEquals( "normalizer schema should be set to apachemeta",
            schemaManager.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, DeepTrimNormalizer.class );
    }


    @Test
    @Ignore
    public void testModifyNormalizerWithAttributes() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Attributes mods = new BasicAttributes( true );
        mods.put( "m-fqcn", DeepTrimNormalizer.class.getName() );
        getSchemaContext( service ).modifyAttributes( JndiUtils.toName( dn ), DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "normalizer OID should still be present",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        assertEquals( "normalizer schema should be set to apachemeta",
            schemaManager.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, DeepTrimNormalizer.class );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------
    @Test
    public void testDeleteNormalizerWhenInUse() throws Exception
    {
        Dn nDn = getNormalizerContainer( "apachemeta" );
        nDn = nDn.add( "m-oid" + "=" + OID );

        // Create a new Normalizer
        testAddNormalizerToEnabledSchema();
        assertTrue( isOnDisk( nDn ) );
        assertTrue( service.getSchemaManager().getNormalizerRegistry().contains( OID ) );

        // Create a MR using this Normalizer
        Attributes attrs = LdifUtils.createAttributes(
                "objectClass: top",
                "objectClass: metaTop",
                "objectClass: metaMatchingRule",
                "m-oid", OID,
                "m-syntax", SchemaConstants.INTEGER_SYNTAX,
                "m-description: test");

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
            getSchemaContext( service ).destroySubcontext( JndiUtils.toName( nDn ) );
            fail( "should not be able to delete a Normalizer in use" );
        }
        catch ( OperationNotSupportedException e )
        {
        }

        assertTrue( "Normalizer should still be in the registry after delete failure", schemaManager
            .getNormalizerRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveNormalizerWhenInUse() throws Exception
    {
        testAddNormalizerToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getNormalizerContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a normalizer in use" );
        }
        catch( OperationNotSupportedException e )
        {
        }

        assertTrue( "normalizer should still be in the registry after move failure",
            schemaManager.getNormalizerRegistry().contains( OID ) );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    @Test
    @Ignore
    public void testMoveNormalizerAndChangeRdnWhenInUse() throws Exception
    {
        testAddNormalizerToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getNormalizerContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a normalizer in use" );
        }
        catch( OperationNotSupportedException e )
        {
        }

        assertTrue( "normalizer should still be in the registry after move failure",
            schemaManager.getNormalizerRegistry().contains( OID ) );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    @Test
    @Ignore
    public void testRenameNormalizerWhenInUse() throws Exception
    {
        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddNormalizerToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Dn newdn = getNormalizerContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to rename a normalizer in use" );
        }
        catch( OperationNotSupportedException e )
        {
        }

        assertTrue( "normalizer should still be in the registry after rename failure",
            schemaManager.getNormalizerRegistry().contains( OID ) );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    @Ignore
    public void testMoveNormalizerToTop() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn top = new Dn();
        top = top.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( top ) );
            fail( "should not be able to move a normalizer up to ou=schema" );
        }
        catch( LdapInvalidDnException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "normalizer should still be in the registry after move failure",
            schemaManager.getNormalizerRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveNormalizerToComparatorContainer() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = new Dn( "ou=comparators,cn=apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a normalizer into comparators container" );
        }
        catch( LdapInvalidDnException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "normalizer should still be in the registry after move failure",
            schemaManager.getNormalizerRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveNormalizerToDisabledSchema() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = getNormalizerContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        // nis is inactive by default
        Dn newdn = getNormalizerContainer( "nis" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "normalizer OID should no longer be present",
            schemaManager.getNormalizerRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveNormalizerToEnabledSchema() throws Exception
    {
        testAddNormalizerToDisabledSchema();

        // nis is inactive by default
        Dn dn = getNormalizerContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );

        assertFalse( "normalizer OID should NOT be present when added to disabled nis schema",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        Dn newdn = getNormalizerContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertTrue( "normalizer OID should be present when moved to enabled schema",
            schemaManager.getNormalizerRegistry().contains( OID ) );

        assertEquals( "normalizer should be in apachemeta schema after move",
            schemaManager.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
    }


    class DummyMR extends MatchingRule
    {
        public DummyMR()
        {
            super( OID );
            addName( "dummy" );
        }

        private static final long serialVersionUID = 1L;
    }
}
