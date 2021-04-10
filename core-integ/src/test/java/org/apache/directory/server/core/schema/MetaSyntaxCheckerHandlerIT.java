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

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.OctetStringSyntaxChecker;
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
@CreateDS(name = "MetaSyntaxCheckerHandlerIT")
public class MetaSyntaxCheckerHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.0.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.0.100001";

    private static LdapConnection connection;
    private static SchemaManager schemaManager;


    @BeforeEach
    public void setup() throws Exception
    {
        super.init();
        connection = IntegrationUtils.getAdminConnection( getService() );
        schemaManager = getService().getSchemaManager();
    }


    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------
    @Test
    public void testAddSyntaxCheckerToEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", OctetStringSyntaxChecker.class.getName(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );

        connection.add( entry );

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertEquals( "apachemeta", schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ) );
        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, OctetStringSyntaxChecker.class );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxCheckerToDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=nis,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", OctetStringSyntaxChecker.class.getName(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );

        // nis is by default inactive
        connection.add( entry );

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "adding new syntaxChecker to disabled schema should not register it into the registries" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxCheckerToUnloadedSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=notloaded,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", OctetStringSyntaxChecker.class.getName(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );

        // nis is by default inactive
        try
        {
            connection.add( entry );
            fail( "Should not be there" );
        }
        catch ( LdapException le )
        {
            // Expected result.
        }

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "adding new syntaxChecker to disabled schema should not register it into the registries" );
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxCheckerWithByteCodeOnEnabledSchema() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try ( InputStream in = getClass().getResourceAsStream( "DummySyntaxChecker.bytecode" ) )
        {
            while ( in.available() > 0 )
            {
                out.write( in.read() );
            }
        }

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", "org.apache.directory.api.ldap.model.schema.syntaxCheckers.DummySyntaxChecker",
            "m-bytecode", out.toByteArray(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );

        connection.add( entry );

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertEquals( "apachemeta", schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ) );
        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( "org.apache.directory.api.ldap.model.schema.syntaxCheckers.DummySyntaxChecker", clazz.getName() );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxCheckerWithByteCodeOnDisabledSchema() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try ( InputStream in = getClass().getResourceAsStream( "DummySyntaxChecker.bytecode" ) )
        {
            while ( in.available() > 0 )
            {
                out.write( in.read() );
            }
        }

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=nis,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", "org.apache.directory.api.ldap.model.schema.syntaxCheckers.DummySyntaxChecker",
            "m-bytecode", out.toByteArray(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );

        connection.add( entry );

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteSyntaxCheckerFromEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        testAddSyntaxCheckerToEnabledSchema();

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should be removed from the registry after being deleted" );
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should be removed from the registry after being deleted" );

        try
        {
            schemaManager.getSyntaxCheckerRegistry().lookup( OID );
            fail( "syntaxChecker lookup should fail after deleting the syntaxChecker" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteSyntaxCheckerFromDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        testAddSyntaxCheckerToEnabledSchema();

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should be removed from the registry after being deleted" );
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should be removed from the registry after being deleted" );

        try
        {
            schemaManager.getSyntaxCheckerRegistry().lookup( OID );
            fail( "syntaxChecker lookup should fail after deleting the syntaxChecker" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    @Disabled
    public void testRenameSyntaxChecker() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        testAddSyntaxCheckerToEnabledSchema();

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );
        connection.rename( dn, rdn );

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "old syntaxChecker OID should be removed from the registry after being renamed" );

        //noinspection EmptyCatchBlock
        try
        {
            schemaManager.getSyntaxCheckerRegistry().lookup( OID );
            fail( "syntaxChecker lookup should fail after deleting the syntaxChecker" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( NEW_OID ) );
        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, OctetStringSyntaxChecker.class );
    }


    @Test
    @Disabled
    public void testMoveSyntaxChecker() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker OID should still be present" );

        assertEquals( "syntaxChecker schema should be set to apache not apachemeta", "apache",
                schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ) );

        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, OctetStringSyntaxChecker.class );
    }


    @Test
    @Disabled
    public void testMoveSyntaxCheckerAndChangeRdn() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=syntaxCheckers,cn=apache,ou=schema" );

        connection.moveAndRename( dn, newDn );

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains(OID ) ,
             "old syntaxChecker OID should NOT be present" );

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains(NEW_OID ) ,
             "new syntaxChecker OID should be present" );

        assertEquals( "syntaxChecker with new oid should have schema set to apache NOT apachemeta", "apache",
                schemaManager.getSyntaxCheckerRegistry().getSchemaName( NEW_OID ) );

        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, OctetStringSyntaxChecker.class );
    }


    @Test
    @Disabled
    public void testModifySyntaxCheckerWithModificationItems() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-fqcn", BogusSyntaxChecker.class.getName() );
        connection.modify( dn, mod );

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker OID should still be present" );

        assertEquals( "syntaxChecker schema should be set to apachemeta", "apachemeta",
                schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ) );

        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, BogusSyntaxChecker.class );
    }


    @Test
    @Disabled
    public void testModifySyntaxCheckerWithAttributes() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-fqcn", BogusSyntaxChecker.class.getName() );
        connection.modify( dn, mod );

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker OID should still be present" );

        assertEquals( "syntaxChecker schema should be set to apachemeta", "apachemeta",
                schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ) );

        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, BogusSyntaxChecker.class );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------
    @Test
    public void testDeleteSyntaxCheckerWhenInUse() throws Exception
    {
        Dn scDn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        // Create a new SyntaxChecker
        testAddSyntaxCheckerToEnabledSchema();
        assertTrue( isOnDisk( scDn ) );
        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) );

        // Create a Syntax using this comparator
        Dn sDn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Entry entry = new DefaultEntry(
            sDn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntax",
            "m-oid", OID,
            "m-description: test" );

        // Pre-checks
        assertFalse( isOnDisk( sDn ) );
        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        // Syntax Addition
        connection.add( entry );

        // Post-checks
        assertTrue( isOnDisk( sDn ) );
        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        try
        {
            connection.delete( scDn );
            fail( "should not be able to delete a syntaxChecker in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should still be in the registry after delete failure" );
    }


    @Test
    @Disabled
    public void testMoveSyntaxCheckerWhenInUse() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        schemaManager.getLdapSyntaxRegistry().register( new DummySyntax() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apache,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a syntaxChecker in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should still be in the registry after move failure" );
        schemaManager.getLdapSyntaxRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    @Test
    @Disabled
    public void testMoveSyntaxCheckerAndChangeRdnWhenInUse() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        schemaManager.getLdapSyntaxRegistry().register( new DummySyntax() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );
        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=syntaxCheckers,cn=apache,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a syntaxChecker in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should still be in the registry after move failure" );
        schemaManager.getLdapSyntaxRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    @Test
    @Disabled
    public void testRenameSyntaxCheckerWhenInUse() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        testAddSyntaxCheckerToEnabledSchema();
        schemaManager.getLdapSyntaxRegistry().register( new DummySyntax() );

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );

        try
        {
            connection.rename( dn, rdn );
            fail( "should not be able to rename a syntaxChecker in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should still be in the registry after rename failure" );
        schemaManager.getLdapSyntaxRegistry().unregister( OID );
        schemaManager.getGlobalOidRegistry().unregister( OID );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------

    @Test
    @Disabled
    public void testMoveSyntaxCheckerToTop() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        Dn top = new Dn( "m-oid=" + OID );

        try
        {
            connection.move( dn, top );
            fail( "should not be able to move a syntaxChecker up to ou=schema" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveSyntaxCheckerToComparatorContainer() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=comparators,cn=apachemeta,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a syntaxChecker into comparators container" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveSyntaxCheckerToDisabledSchema() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        // nis is inactive by default
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=nis,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker OID should no longer be present" );
    }


    @Test
    @Disabled
    public void testMoveSyntaxCheckerToEnabledSchema() throws Exception
    {
        testAddSyntaxCheckerToDisabledSchema();

        // nis is inactive by default
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=nis,ou=schema" );

        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker OID should NOT be present when added to disabled nis schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxCheckers,cn=apachemeta,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) ,
             "syntaxChecker OID should be present when moved to enabled schema" );

        assertEquals( "syntaxChecker should be in apachemeta schema after move", "apachemeta", 
                schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ) );
    }

    public static class BogusSyntaxChecker extends SyntaxChecker
    {
        public BogusSyntaxChecker()
        {
            super( OID );
        }


        public boolean isValidSyntax( Object value )
        {
            return false;
        }
    }

    class DummySyntax extends LdapSyntax
    {
        public DummySyntax()
        {
            super( OID );
            addName( "dummy" );
            isObsolete = false;
            isHumanReadable = false;
            syntaxChecker = null;
        }
    }
}
