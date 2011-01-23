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


import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OctetStringSyntaxChecker;
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
    public void testAddSyntaxToEnabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntax",
            "m-oid", OID,
            "m-description", DESCRIPTION0 );

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        createDummySyntaxChecker( OID, "apachemeta" );
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( schemaManager.getLdapSyntaxRegistry().contains( OID ) );
        assertEquals( schemaManager.getLdapSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxToDisabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntax",
            "m-oid", OID,
            "m-description", DESCRIPTION0 );

        // nis is by default inactive
        Dn dn = getSyntaxContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );
        createDummySyntaxChecker( OID, "nis" );
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertFalse( "adding new syntax to disabled schema should not register it into the registries",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxToUnloadedSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntax",
            "m-oid", OID,
            "m-description", DESCRIPTION0 );

        // nis is by default inactive
        Dn dn = getSyntaxContainer( "notloaded" );
        dn = dn.add( "m-oid" + "=" + OID );
        createDummySyntaxChecker( OID, "nis" );

        try
        {
            getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail( "Should not be there" );
        }
        catch( NameNotFoundException nnfe )
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
        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddSyntaxToEnabledSchema();

        assertTrue( isOnDisk( dn ) );
        assertTrue( "syntax should be removed from the registry after being deleted",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );

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
        Dn dn = getSyntaxContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddSyntaxToDisabledSchema();

        assertTrue( isOnDisk( dn ) );
        assertFalse( "syntax should be removed from the registry after being deleted",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );

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
        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddSyntaxToEnabledSchema();

        Dn newdn = getSyntaxContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getSyntaxContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getSyntaxContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-description", DESCRIPTION1 );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( JndiUtils.toName( dn ), mods );

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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Attributes mods = new BasicAttributes( true );
        mods.put( "m-description", DESCRIPTION1 );
        getSchemaContext( service ).modifyAttributes( JndiUtils.toName( dn ), DirContext.REPLACE_ATTRIBUTE, mods );

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
        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddSyntaxToEnabledSchema();
        addDependeeMatchingRule( OID );

        try
        {
            getSchemaContext( service ).destroySubcontext( JndiUtils.toName( dn ) );
            fail( "should not be able to delete a syntax in use" );
        }
        catch( OperationNotSupportedException e )
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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getSyntaxContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getSyntaxContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
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
        Attributes attrs = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaMatchingRule",
            "m-oid", MR_OID,
            "m-syntax", OID,
            "m-description", MR_DESCRIPTION );

        Dn dn = getMatchingRuleContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + MR_OID );
        getSchemaContext( service ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( schemaManager.getMatchingRuleRegistry().contains( MR_OID ) );
        assertEquals( schemaManager.getMatchingRuleRegistry().getSchemaName( MR_OID ), "apachemeta" );
    }


    @Test
    @Ignore
    public void testRenameNormalizerWhenInUse() throws Exception
    {
        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        testAddSyntaxToEnabledSchema();
        addDependeeMatchingRule( OID );

        Dn newdn = getSyntaxContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn top = new Dn();
        top.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( top ) );
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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = new Dn( "ou=comparators,cn=apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
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

        Dn dn = getSyntaxContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        // nis is inactive by default
        Dn newdn = getSyntaxContainer( "nis" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "syntax OID should no longer be present",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntaxToEnabledSchema() throws Exception
    {
        testAddSyntaxToDisabledSchema();

        // nis is inactive by default
        Dn dn = getSyntaxContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );

        assertFalse( "syntax OID should NOT be present when added to disabled nis schema",
            schemaManager.getLdapSyntaxRegistry().contains( OID ) );

        Dn newdn = getSyntaxContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( service ).rename( JndiUtils.toName( dn ), JndiUtils.toName(newdn) );

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
        modify( DirContext.ADD_ATTRIBUTE, descriptions, "syntaxCheckers" );
    }


    private void modify( int op, List<String> descriptions, String opAttr ) throws Exception
    {
        Dn dn = new Dn( getSubschemaSubentryDN(), service.getSchemaManager() );
        Attribute attr = new BasicAttribute( opAttr );

        for ( String description : descriptions )
        {
            attr.add( description );
        }

        Attributes mods = new BasicAttributes( true );
        mods.put( attr );

        getRootContext( service ).modifyAttributes( JndiUtils.toName( dn ), op, mods );
    }


    /**
     * Get's the subschemaSubentry attribute value from the rootDSE.
     *
     * @return the subschemaSubentry distinguished name
     * @throws Exception if there are problems accessing the RootDSE
     */
    private String getSubschemaSubentryDN() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ SUBSCHEMA_SUBENTRY } );

        NamingEnumeration<SearchResult> results = getRootContext( service ).search(
                "", "(objectClass=*)", controls );
        SearchResult result = results.next();
        results.close();
        Attribute subschemaSubentry = result.getAttributes().get( SUBSCHEMA_SUBENTRY );
        return ( String ) subschemaSubentry.get();
    }
}
