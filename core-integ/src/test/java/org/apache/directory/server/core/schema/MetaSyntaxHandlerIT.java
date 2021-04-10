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

import java.util.ArrayList;
import java.util.List;

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
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
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
        assertEquals( "apachemeta", schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ) );
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

        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "adding new syntax to disabled schema should not register it into the registries" );
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
        catch ( LdapException le )
        {
            // Expected result.
        }

        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "adding new syntax to disabled schema should not register it into the registries" );
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteSyntaxFromEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        testAddSyntaxToEnabledSchema();

        assertTrue( isOnDisk( dn ) );
        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should be removed from the registry after being deleted" );

        connection.delete( dn );

        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should be removed from the registry after being deleted" );

        try
        {
            schemaManager.getLdapSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting it" );
        }
        catch ( LdapException e )
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
        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should be removed from the registry after being deleted" );

        connection.delete( dn );

        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should be removed from the registry after being deleted" );

        try
        {
            schemaManager.getLdapSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting it" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    @Disabled
    public void testRenameSyntax() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        testAddSyntaxToEnabledSchema();

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );

        connection.rename( dn, rdn );

        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "old syntax OID should be removed from the registry after being renamed" );

        //noinspection EmptyCatchBlock
        try
        {
            schemaManager.getLdapSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting the syntax" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( NEW_OID ) );
    }


    @Test
    @Disabled
    public void testMoveSyntax() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax OID should still be present" );

        assertEquals( "syntax schema should be set to apache not apachemeta", "apache",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ) );
    }


    @Test
    @Disabled
    public void testMoveSyntaxAndChangeRdn() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=syntaxes,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "old syntax OID should NOT be present" );

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( NEW_OID ) ,
             "new syntax OID should be present" );

        assertEquals( "syntax with new oid should have schema set to apache NOT apachemeta", "apache",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( NEW_OID ) );
    }


    @Test
    @Disabled
    public void testModifySyntaxWithModificationItems() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        LdapSyntax syntax = schemaManager.getLdapSyntaxRegistry().lookup( OID );
        assertEquals( DESCRIPTION0, syntax.getDescription() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-description", DESCRIPTION1 );
        connection.modify( dn, mod );

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax OID should still be present" );

        assertEquals( "syntax schema should be set to apachemeta", "apachemeta",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ) );

        syntax = schemaManager.getLdapSyntaxRegistry().lookup( OID );
        assertEquals( DESCRIPTION1, syntax.getDescription() );
    }


    @Test
    @Disabled
    public void testModifySyntaxWithAttributes() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        LdapSyntax syntax = schemaManager.getLdapSyntaxRegistry().lookup( OID );
        assertEquals( DESCRIPTION0, syntax.getDescription() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-description", DESCRIPTION1 );
        connection.modify( dn, mod );

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax OID should still be present" );

        assertEquals( "syntax schema should be set to apachemeta", "apachemeta",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ) );

        syntax = schemaManager.getLdapSyntaxRegistry().lookup( OID );
        assertEquals( DESCRIPTION1, syntax.getDescription() );
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
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should still be in the registry after delete failure" );
    }


    @Test
    @Disabled
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
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should still be in the registry after move failure" );
    }


    @Test
    @Disabled
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
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should still be in the registry after move failure" );
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
            "m-description", MR_DESCRIPTION );

        connection.add( entry );

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( MR_OID ) );
        assertEquals( "apachemeta", schemaManager.getMatchingRuleRegistry().getSchemaName( MR_OID ) );
    }


    @Test
    @Disabled
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
        catch ( LdapUnwillingToPerformException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should still be in the registry after rename failure" );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------

    @Test
    @Disabled
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
        catch ( LdapInvalidDnException e )
        {
            assertEquals( ResultCodeEnum.NAMING_VIOLATION, e.getResultCode() );
        }

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should still be in the registry after move failure" );
    }


    @Test
    @Disabled
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
        catch ( LdapInvalidDnException e )
        {
            assertEquals( ResultCodeEnum.NAMING_VIOLATION, e.getResultCode() );
        }

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveSyntaxToDisabledSchema() throws Exception
    {
        testAddSyntaxToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        // nis is inactive by default
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=nis,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax OID should no longer be present" );
    }


    @Test
    @Disabled
    public void testMoveSyntaxToEnabledSchema() throws Exception
    {
        testAddSyntaxToDisabledSchema();

        // nis is inactive by default
        Dn dn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=nis,ou=schema" );

        assertFalse( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax OID should NOT be present when added to disabled nis schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=syntaxes,cn=apachemeta,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) ,
             "syntax OID should be present when moved to enabled schema" );

        assertEquals( "syntax should be in apachemeta schema after move", "apachemeta",
            schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ) );
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
