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
import org.apache.directory.api.ldap.model.schema.comparators.StringComparator;
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
@CreateDS(name = "MetaMatchingRuleHandlerIT")
public class MetaMatchingRuleHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String DESCRIPTION0 = "A test matchingRule";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";

    public static SchemaManager schemaManager;
    private static LdapConnection connection;


    @BeforeEach
    public void setup() throws Exception
    {
        super.init();
        connection = IntegrationUtils.getAdminConnection( getService() );
        schemaManager = getService().getSchemaManager();
    }


    private void createComparator() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=comparators,cn=apachemeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaComparator",
            "m-fqcn: " + StringComparator.class.getName(),
            "m-oid: " + OID,
            "m-description: A test comparator" );

        // Addition
        connection.add( entry );

        assertTrue( isOnDisk( dn ) );
        assertTrue( schemaManager.getComparatorRegistry().contains( OID ) );
    }


    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------
    // Test Add operation
    // ----------------------------------------------------------------------
    @Test
    public void testAddMatchingRuleToEnabledSchema() throws Exception
    {
        createComparator();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaMatchingRule",
            "m-oid", OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description", DESCRIPTION0 );

        // Pre-checks
        assertFalse( isOnDisk( dn ) );
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( OID ) );

        // Addition
        connection.add( entry );

        // Post-checks
        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) );
        assertEquals( "apachemeta", schemaManager.getMatchingRuleRegistry().getSchemaName( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddMatchingRuleToDisabledSchema() throws Exception
    {
        createComparator();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=nis,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaMatchingRule",
            "m-oid", OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description", DESCRIPTION0 );

        connection.add( entry );

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "adding new matchingRule to disabled schema should not register it into the registries" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddMatchingRuleToUnloadedSchema() throws Exception
    {
        createComparator();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=notloaded,ou=schema" );
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaMatchingRule",
            "m-oid", OID,
            "m-syntax", SchemaConstants.INTEGER_SYNTAX,
            "m-description", DESCRIPTION0 );

        try
        {
            connection.add( entry );
            fail( "Should not be there" );
        }
        catch ( LdapException le )
        {
            // Expected result
        }

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "adding new matchingRule to disabled schema should not register it into the registries" );
        assertFalse( isOnDisk( dn ) );
    }


    // ----------------------------------------------------------------------
    // Test Delete operation
    // ----------------------------------------------------------------------
    @Test
    public void testDeleteMatchingRuleFromEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        testAddMatchingRuleToEnabledSchema();

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule should be removed from the registry after being deleted" );
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule should be removed from the registry after being deleted" );

        try
        {
            schemaManager.getMatchingRuleRegistry().lookup( OID );
            fail( "matchingRule lookup should fail after deleting it" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteMatchingRuleFromDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=nis,ou=schema" );

        testAddMatchingRuleToDisabledSchema();

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule should be removed from the registry after being deleted" );
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule should be removed from the registry after being deleted" );
        assertFalse( isOnDisk( dn ) );
    }


    // ----------------------------------------------------------------------
    // Test Modify operation
    // ----------------------------------------------------------------------
    @Test
    @Disabled
    public void testModifyMatchingRuleWithModificationItems() throws Exception
    {
        testAddMatchingRuleToEnabledSchema();

        MatchingRule mr = schemaManager.getMatchingRuleRegistry().lookup( OID );
        assertEquals( DESCRIPTION0, mr.getDescription() );
        assertEquals( SchemaConstants.INTEGER_SYNTAX, mr.getSyntax().getOid() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        Modification mod1 = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-description", DESCRIPTION1 );
        Modification mod2 = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-syntax", SchemaConstants.DIRECTORY_STRING_SYNTAX );

        connection.modify( dn, mod1, mod2 );

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule OID should still be present" );

        assertEquals( "matchingRule schema should be set to apachemeta", "apachemeta", 
            schemaManager.getMatchingRuleRegistry().getSchemaName( OID ) );

        mr = schemaManager.getMatchingRuleRegistry().lookup( OID );
        assertEquals( DESCRIPTION1, mr.getDescription() );
        assertEquals( SchemaConstants.DIRECTORY_STRING_SYNTAX, mr.getSyntax().getOid() );
    }


    @Test
    @Disabled
    public void testModifyMatchingRuleWithAttributes() throws Exception
    {
        testAddMatchingRuleToEnabledSchema();

        MatchingRule mr = schemaManager.getMatchingRuleRegistry().lookup( OID );
        assertEquals( DESCRIPTION0, mr.getDescription() );
        assertEquals( SchemaConstants.INTEGER_SYNTAX, mr.getSyntax().getOid() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        Modification mod1 = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-description", DESCRIPTION1 );
        Modification mod2 = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-syntax", SchemaConstants.DIRECTORY_STRING_SYNTAX );

        connection.modify( dn, mod1, mod2 );

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule OID should still be present" );

        assertEquals( "matchingRule schema should be set to apachemeta", "apachemeta", 
            schemaManager.getMatchingRuleRegistry().getSchemaName( OID ) );

        mr = schemaManager.getMatchingRuleRegistry().lookup( OID );
        assertEquals( DESCRIPTION1, mr.getDescription() );
        assertEquals( SchemaConstants.DIRECTORY_STRING_SYNTAX, mr.getSyntax().getOid() );
    }


    // ----------------------------------------------------------------------
    // Test Rename operation
    // ----------------------------------------------------------------------
    @Test
    @Disabled
    public void testRenameMatchingRule() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        testAddMatchingRuleToEnabledSchema();

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );
        connection.rename( dn, rdn );

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "old matchingRule OID should be removed from the registry after being renamed" );

        try
        {
            schemaManager.getMatchingRuleRegistry().lookup( OID );
            fail( "matchingRule lookup should fail after renaming the matchingRule" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( NEW_OID ) );
    }


    // ----------------------------------------------------------------------
    // Test Move operation
    // ----------------------------------------------------------------------
    @Test
    @Disabled
    public void testMoveMatchingRule() throws Exception
    {
        testAddMatchingRuleToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule OID should still be present" );

        assertEquals( "matchingRule schema should be set to apache not apachemeta", "apache",
            schemaManager.getMatchingRuleRegistry().getSchemaName( OID ) );
    }


    @Test
    @Disabled
    public void testMoveMatchingRuleAndChangeRdn() throws Exception
    {
        testAddMatchingRuleToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=matchingRules,cn=apache,ou=schema" );

        connection.moveAndRename( dn, newDn );

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "old matchingRule OID should NOT be present" );

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( NEW_OID ) ,
             "new matchingRule OID should be present" );

        assertEquals( "matchingRule with new oid should have schema set to apache NOT apachemeta", "apache",
            schemaManager.getMatchingRuleRegistry().getSchemaName( NEW_OID ) );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------
    //    @Test
    //    public void testDeleteSyntaxWhenInUse() throws Exception
    //    {
    //        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );
    //
    //        testAddSyntax();
    //        addDependeeMatchingRule();
    //
    //        try
    //        {
    //            connection.delete( dn );
    //            fail( "should not be able to delete a syntax in use" );
    //        }
    //        catch( LdapException e )
    //        {
    //            //assertEquals( e.@getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
    //        }
    //
    //        assertTrue( getLdapSyntaxRegistry().hasSyntax( OID ) ,
    //          "syntax should still be in the registry after delete failure" );
    //    }

    //    public void testMoveSyntaxWhenInUse() throws NamingException
    //    {
    //        testAddSyntax();
    //        addDependeeMatchingRule();
    //
    //        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );
    //        Dn newDn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apache,ou=schema" );
    //
    //        try
    //        {
    //            super.schemaRoot.rename( dn, newDn );
    //            fail( "should not be able to move a syntax in use" );
    //        }
    //        catch( LdapUnwillingToPerformException e )
    //        {
    //            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
    //        }
    //
    //        assertTrue( registries.getLdapSyntaxRegistry().hasSyntax( OID ) ,
    //          "syntax should still be in the registry after move failure" );
    //    }
    //
    //
    //    public void testMoveSyntaxAndChangeRdnWhenInUse() throws NamingException
    //    {
    //        testAddSyntax();
    //        addDependeeMatchingRule()
    //
    //        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );
    //
    //        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=matchingRules,cn=apache,ou=schema" );

    //        try
    //        {
    //            super.schemaRoot.moveAndRename( dn, JndiUtils.toName( newdn ) );
    //            fail( "should not be able to move a syntax in use" );
    //        }
    //        catch( LdapUnwillingToPerformException e )
    //        {
    //            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
    //        }
    //
    //        assertTrue( registries.getSyntaxRegistry().hasSyntax( OID ) ,
    //          "syntax should still be in the registry after move failure" );
    //    }
    //
    //

    // Need to add body to this method which creates a new matchingRule after
    // the matchingRule addition code has been added.

    //    private void addDependeeMatchingRule()
    //    {
    //        throw new NotImplementedException();
    //    }
    //
    //    public void testRenameNormalizerWhenInUse() throws NamingException
    //    {
    //        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );
    //
    //        testAddSyntax();
    //        addDependeeMatchingRule();
    //
    //        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );
    //
    //        try
    //        {
    //            super.schemaRoot.rename( dn, rdn );
    //            fail( "should not be able to rename a syntax in use" );
    //        }
    //        catch( LdapUnwillingToPerformException e )
    //        {
    //            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
    //        }
    //
    //        assertTrue( registries.getSyntaxRegistry().hasSyntax( OID ) ,
    //          "syntax should still be in the registry after rename failure" );
    //    }

    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------

    @Test
    @Disabled
    public void testMoveMatchingRuleToTop() throws Exception
    {
        testAddMatchingRuleToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        Dn top = new Dn( "m-oid=" + OID );

        try
        {
            connection.move( dn, top );
            fail( "should not be able to move a matchingRule up to ou=schema" );
        }
        catch ( LdapInvalidDnException e )
        {
            assertEquals( ResultCodeEnum.NAMING_VIOLATION, e.getResultCode() );
        }

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveMatchingRuleToComparatorContainer() throws Exception
    {
        testAddMatchingRuleToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=comparatos,cn=apachemeta,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a matchingRule into comparators container" );
        }
        catch ( LdapInvalidDnException e )
        {
            assertEquals( ResultCodeEnum.NAMING_VIOLATION, e.getResultCode() );
        }

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveMatchingRuleToDisabledSchema() throws Exception
    {
        testAddMatchingRuleToEnabledSchema();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        // nis is inactive by default
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=nis,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule OID should no longer be present" );
    }


    @Test
    @Disabled
    public void testMoveMatchingRuleToEnabledSchema() throws Exception
    {
        testAddMatchingRuleToDisabledSchema();

        // nis is inactive by default
        Dn dn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=nis,ou=schema" );

        assertFalse( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule OID should NOT be present when added to disabled nis schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=matchingRules,cn=apachemeta,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( OID ) ,
             "matchingRule OID should be present when moved to enabled schema" );

        assertEquals( "matchingRule should be in apachemeta schema after move", "apachemeta", 
            schemaManager.getMatchingRuleRegistry().getSchemaName( OID ) );
    }
}
