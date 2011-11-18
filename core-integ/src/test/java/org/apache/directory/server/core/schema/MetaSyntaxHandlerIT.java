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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.LdapSyntax;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.OctetStringSyntaxChecker;
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
@CreateDS(name = "MetaSyntaxHandlerIT")
public class MetaSyntaxHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String DESCRIPTION0 = "A test normalizer";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.0.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.0.100001";

    private static final String MR_OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String MR_DESCRIPTION = "A test matchingRule";

    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";

    private static SchemaManager schemaManager;
    private static LdapConnection connection;


    @Before
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
    public void testAddSyntaxToEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntax",
            "m-oid", OID,
            "m-description", DESCRIPTION0 );

        createDummySyntaxChecker( OID, "apachemeta" );
        connection.add( entry );

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) );
        assertEquals( schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxToDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=nis,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntax",
            "m-oid", OID,
            "m-description", DESCRIPTION0 );

        // nis is by default inactive
        createDummySyntaxChecker( OID, "nis" );
        connection.add( entry );

        assertFalse( "adding new syntax to disabled schema should not register it into the registries",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxToUnloadedSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=notloaded,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntax",
            "m-oid", OID,
            "m-description", DESCRIPTION0 );

        // nis is by default inactive
        createDummySyntaxChecker( OID, "nis" );

        try
        {
            connection.add( entry );
            fail( "Should not be there" );
        }
        catch( LdapException le )
        {
            // Expected result.
        }

        assertFalse( "adding new syntax to disabled schema should not register it into the registries",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteSyntaxFromEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        testAddSyntaxToEnabledSchema();

        assertTrue( isOnDisk( dn ) );
        assertTrue( "syntax should be removed from the registry after being deleted",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        connection.delete( dn );

        assertFalse( "syntax should be removed from the registry after being deleted",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        try
        {
            schemaManager.getLdapSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting it" );
        }
        catch( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteSyntaxFromDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=nis,ou=schema" );

        testAddSyntaxToDisabledSchema();

        assertTrue( isOnDisk( dn ) );
        assertFalse( "syntax should be removed from the registry after being deleted",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        connection.delete( dn );

        assertFalse( "syntax should be removed from the registry after being deleted",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        try
        {
            schemaManager.getLdapSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting it" );
        }
        catch( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    @Ignore
    public void testRenameSyntax() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        testAddSyntaxToEnabledSchema();

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );

        connection.rename( dn, rdn );

        assertFalse( "old syntax OID should be removed from the registry after being renamed",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaManager.getLdapSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting the syntax" );
        }
        catch( LdapException e )
        {
        }

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( NEW_OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntax() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( "syntax OID should still be present",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        assertEquals( "syntax schema should be set to apache not apachemeta",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ), "apache" );
    }


    @Test
    @Ignore
    public void testMoveSyntaxAndChangeRdn() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=syntaxes,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( "old syntax OID should NOT be present",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        assertTrue( "new syntax OID should be present",
            schemaManager.getLdapSyntaxRegistry().contains( NEW_OID ) );

        assertEquals( "syntax with new oid should have schema set to apache NOT apachemeta",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( NEW_OID ), "apache" );
    }


    @Test
    @Ignore
    public void testModifySyntaxWithModificationItems() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        LdapSyntax syntax = schemaManager.getLdapSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION0 );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-description", DESCRIPTION1 );
        connection.modify( dn, mod );

        assertTrue( "syntax OID should still be present",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        assertEquals( "syntax schema should be set to apachemeta",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ), "apachemeta" );

        syntax = schemaManager.getLdapSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION1 );
    }


    @Test
    @Ignore
    public void testModifySyntaxWithAttributes() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        LdapSyntax syntax = schemaManager.getLdapSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION0 );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-description", DESCRIPTION1 );
        connection.modify( dn, mod );

        assertTrue( "syntax OID should still be present",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        assertEquals( "syntax schema should be set to apachemeta",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ), "apachemeta" );

        syntax = schemaManager.getLdapSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION1 );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------


    @Test
    public void testDeleteSyntaxWhenInUse() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        testAddSyntaxToEnabledSchema();
        addDependeeMatchingRule( OID );

        try
        {
            connection.delete( dn );
            fail( "should not be able to delete a syntax in use" );
        }
        catch( LdapException e )
        {
        }

        assertTrue( "syntax should still be in the registry after delete failure",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntaxWhenInUse() throws Exception
    {
        testAddSyntaxToEnabledSchema();
        addDependeeMatchingRule( OID );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apache,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a syntax in use" );
        }
        catch( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after move failure",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntaxAndChangeRdnWhenInUse() throws Exception
    {
        testAddSyntaxToEnabledSchema();
        addDependeeMatchingRule( OID );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=syntaxes,cn=apache,ou=schema" );

        try
        {
            connection.moveAndRename( dn, newDn );
            fail( "should not be able to move a syntax in use" );
        }
        catch( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after move failure",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
    }


    private void addDependeeMatchingRule( String oid ) throws Exception
    {
        Dn dn = new Dn( "m-oid=" + MR_OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );
        
        Entry entry = new DefaultEntry(
                dn,
                "objectClass: top",
                "objectClass: metaTop",
                "objectClass: metaMatchingRule",
                "m-oid", MR_OID,
                "m-syntax", OID,
                "m-description", MR_DESCRIPTION);

        connection.add( entry );

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( MR_OID ) );
        assertEquals( schemaManager.getMatchingRuleRegistry().getSchemaName( MR_OID ), "apachemeta" );
    }


    @Test
    @Ignore
    public void testRenameNormalizerWhenInUse() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        testAddSyntaxToEnabledSchema();
        addDependeeMatchingRule( OID );

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );

        try
        {
            connection.rename( dn, rdn );
            fail( "should not be able to rename a syntax in use" );
        }
        catch( LdapUnwillingToPerformException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after rename failure",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    @Ignore
    public void testMoveSyntaxToTop() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Dn top = new Dn( "m-oid=" + OID );

        try
        {
            connection.move( dn, top );
            fail( "should not be able to move a syntax up to ou=schema" );
        }
        catch( LdapInvalidDnException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "syntax should still be in the registry after move failure",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntaxToComparatorContainer() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=comparators,cn=apachemeta,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a syntax into comparators container" );
        }
        catch( LdapInvalidDnException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "syntax should still be in the registry after move failure",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntaxToDisabledSchema() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        // nis is inactive by default
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=nis,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( "syntax OID should no longer be present",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntaxToEnabledSchema() throws Exception
    {
        testAddSyntaxToDisabledSchema();

        // nis is inactive by default
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=nis,ou=schema" );

        assertFalse( "syntax OID should NOT be present when added to disabled nis schema",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( "syntax OID should be present when moved to enabled schema",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        assertEquals( "syntax should be in apachemeta schema after move",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
    }


    private void createDummySyntaxChecker( String oid, String schema ) throws Exception
    {
        List<String> descriptions = new ArrayList<String>();
        descriptions.add( "( " + oid + " DESC 'bogus desc' FQCN " + OctetStringSyntaxChecker.class.getName()
            + " X-SCHEMA '" + schema + "' )" );
        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "syntaxCheckers" );
    }


    private void modify( ModificationOperation op, List<String> descriptions, String opAttr ) throws Exception
    {
        Dn dn = new Dn( schemaManager, getSubschemaSubentryDN() );
        Attribute attr = new DefaultAttribute( opAttr );

        for ( String description : descriptions )
        {
            attr.add( description );
        }

        Modification mod = new DefaultModification( op, attr );

        connection.modify( dn, mod );
    }


    /**
     * Get's the subschemaSubentry attribute value from the rootDSE.
     *
     * @return the subschemaSubentry distinguished name
     * @throws Exception if there are problems accessing the RootDSE
     */
    private String getSubschemaSubentryDN() throws Exception
    {
        Entry rootDse = connection.getRootDse( SUBSCHEMA_SUBENTRY );

        String subschemaSubentry = rootDse.get( SUBSCHEMA_SUBENTRY ).getString();
        
        return subschemaSubentry;
    }
}
