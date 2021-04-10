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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.DeepTrimNormalizer;
import org.apache.directory.api.ldap.model.schema.normalizers.NoOpNormalizer;
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
@CreateDS(name = "MetaNormalizerHandlerIT")
public class MetaNormalizerHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";

    public static SchemaManager schemaManager;
    private static LdapConnection connection;


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
    @Test
    public void testAddNormalizerToEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", NoOpNormalizer.class.getName(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        connection.add( entry );

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) );
        assertEquals( "apachemeta", schemaManager.getNormalizerRegistry().getSchemaName( OID ) );
        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddNormalizerToDisabledSchema() throws Exception
    {
        // nis is by default inactive
        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=nis,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", NoOpNormalizer.class.getName(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        connection.add( entry );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "adding new normalizer to disabled schema should not register it into the registries" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddNormalizerToUnloadedSchema() throws Exception
    {
        // nis is by default inactive
        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=notloaded,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", NoOpNormalizer.class.getName(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        try
        {
            connection.add( entry );
            fail( "Should not be there" );
        }
        catch ( LdapException nnfe )
        {
            // Expected result.
        }

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "adding new normalizer to disabled schema should not register it into the registries" );
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testAddNormalizerWithByteCodeToEnabledSchema() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try ( InputStream in = getClass().getResourceAsStream( "DummyNormalizer.bytecode" ) )
        {
            while ( in.available() > 0 )
            {
                out.write( in.read() );
            }
        }

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", "org.apache.directory.api.ldap.model.schema.normalizers.DummyNormalizer",
            "m-bytecode", out.toByteArray(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        connection.add( entry );

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) );
        assertEquals( "apachemeta", schemaManager.getNormalizerRegistry().getSchemaName( OID ) );
        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( "org.apache.directory.api.ldap.model.schema.normalizers.DummyNormalizer", clazz.getName() );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddNormalizerWithByteCodeToDisabledSchema() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try ( InputStream in = getClass().getResourceAsStream( "DummyNormalizer.bytecode" ) )
        {
            while ( in.available() > 0 )
            {
                out.write( in.read() );
            }
        }

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=nis,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaNormalizer",
            "m-fqcn", "org.apache.directory.api.ldap.model.schema.normalizers.DummyNormalizer",
            "m-bytecode", out.toByteArray(),
            "m-oid", OID,
            "m-description: A test normalizer" );

        connection.add( entry );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteNormalizerFromEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        testAddNormalizerToEnabledSchema();

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should be removed from the registry after being deleted" );
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should be removed from the registry after being deleted" );

        try
        {
            schemaManager.getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteNormalizerFromDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        testAddNormalizerToEnabledSchema();

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should be removed from the registry after being deleted" );
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should be removed from the registry after being deleted" );

        try
        {
            schemaManager.getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    @Disabled
    public void testRenameNormalizer() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );
        testAddNormalizerToEnabledSchema();

        Rdn rdn = new Rdn( "m-oid" + "=" + NEW_OID );

        connection.rename( dn, rdn );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "old normalizer OID should be removed from the registry after being renamed" );

        //noinspection EmptyCatchBlock
        try
        {
            schemaManager.getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getNormalizerRegistry().contains( NEW_OID ) );
        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    @Test
    @Disabled
    public void testMoveNormalizer() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer OID should still be present" );

        assertEquals( "normalizer schema should be set to apache not apachemeta", "apache",
            schemaManager.getNormalizerRegistry().getSchemaName( OID ) );

        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    @Test
    @Disabled
    public void testMoveNormalizerAndChangeRdn() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );
        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=normalizers,cn=apache,ou=schema" );

        connection.moveAndRename( dn, newDn );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "old normalizer OID should NOT be present" );

        assertTrue( schemaManager.getNormalizerRegistry().contains( NEW_OID ) ,
             "new normalizer OID should be present" );

        assertEquals( "normalizer with new oid should have schema set to apache NOT apachemeta", "apache",
            schemaManager.getNormalizerRegistry().getSchemaName( NEW_OID ) );

        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    @Test
    @Disabled
    public void testModifyNormalizerWithModificationItems() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-fqcn", DeepTrimNormalizer.class.getName() );

        connection.modify( dn, mod );

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer OID should still be present" );

        assertEquals( "normalizer schema should be set to apachemeta", "apachemeta",
            schemaManager.getNormalizerRegistry().getSchemaName( OID ) );

        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, DeepTrimNormalizer.class );
    }


    @Test
    @Disabled
    public void testModifyNormalizerWithAttributes() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-fqcn", DeepTrimNormalizer.class.getName() );
        connection.modify( dn, mod );

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer OID should still be present" );

        assertEquals( "normalizer schema should be set to apachemeta", "apachemeta",
            schemaManager.getNormalizerRegistry().getSchemaName( OID ) );

        Class<?> clazz = schemaManager.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, DeepTrimNormalizer.class );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------
    @Test
    public void testDeleteNormalizerWhenInUse() throws Exception
    {
        Dn nDn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apachemeta,ou=schema" );

        // Create a new Normalizer
        testAddNormalizerToEnabledSchema();
        assertTrue( isOnDisk( nDn ) );
        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) );

        // Create a MR using this Normalizer
        Dn mrDn = new Dn( "m-oid=" + OID + ",ou=matchingrules,cn=apachemeta,ou=schema" );

        Entry entry = new DefaultEntry(
            mrDn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaMatchingRule",
            "m-oid", OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description: test" );

        // Pre-checks
        assertFalse( isOnDisk( mrDn ) );
        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) );

        // MatchingRule Addition
        connection.add( entry );

        // Post-checks
        assertTrue( isOnDisk( mrDn ) );
        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) );

        try
        {
            connection.delete( nDn );
            fail( "should not be able to delete a Normalizer in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ),
            "Normalizer should still be in the registry after delete failure" );
    }


    @Test
    @Disabled
    public void testMoveNormalizerWhenInUse() throws Exception
    {
        testAddNormalizerToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apache,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a normalizer in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should still be in the registry after move failure" );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    @Test
    @Disabled
    public void testMoveNormalizerAndChangeRdnWhenInUse() throws Exception
    {
        testAddNormalizerToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );
        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=normalizers,cn=apache,ou=schema" );

        try
        {
            connection.moveAndRename( dn, newDn );
            fail( "should not be able to move a normalizer in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should still be in the registry after move failure" );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    @Test
    @Disabled
    public void testRenameNormalizerWhenInUse() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        testAddNormalizerToEnabledSchema();
        schemaManager.getMatchingRuleRegistry().register( new DummyMR() );

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );

        try
        {
            connection.rename( dn, rdn );
            fail( "should not be able to rename a normalizer in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should still be in the registry after rename failure" );
        schemaManager.getMatchingRuleRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------

    @Test
    @Disabled
    public void testMoveNormalizerToTop() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        Dn top = new Dn( "m-oid=" + OID );

        try
        {
            connection.move( dn, top );
            fail( "should not be able to move a normalizer up to ou=schema" );
        }
        catch ( LdapInvalidDnException e )
        {
            assertEquals( ResultCodeEnum.NAMING_VIOLATION, e.getResultCode() );
        }

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveNormalizerToComparatorContainer() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=comparators,cn=apacheMeta,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a normalizer into comparators container" );
        }
        catch ( LdapInvalidDnException e )
        {
            assertEquals( ResultCodeEnum.NAMING_VIOLATION, e.getResultCode() );
        }

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveNormalizerToDisabledSchema() throws Exception
    {
        testAddNormalizerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        // nis is inactive by default
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=nis,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer OID should no longer be present" );
    }


    @Test
    @Disabled
    public void testMoveNormalizerToEnabledSchema() throws Exception
    {
        testAddNormalizerToDisabledSchema();

        // nis is inactive by default
        Dn dn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=nis,ou=schema" );

        assertFalse( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer OID should NOT be present when added to disabled nis schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=normalizers,cn=apacheMeta,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getNormalizerRegistry().contains( OID ) ,
             "normalizer OID should be present when moved to enabled schema" );

        assertEquals( "normalizer should be in apachemeta schema after move", "apachemeta",
            schemaManager.getNormalizerRegistry().getSchemaName( OID ) );
    }

    class DummyMR extends MatchingRule
    {
        public DummyMR()
        {
            super( OID );
            addName( "dummy" );
        }
    }
}
