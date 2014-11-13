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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test case which tests the correct operation of the schema entity handler.
 * <br>
 * We have many things to check. Here is a list of what we want to control :
 * <ul>
 *   <li>Enabling Schema</li>
 *   <ul>
 *     <li>an existing schema</li>
 *     <li>a non existing schema</li>
 *     <li>an already enabled Schema</li>
 *     <li>an existing schema which will break the Registries when enabled</li>
 *   </ul>
 *   <li>Disabling Schema</li>
 *   <ul>
 *     <li>an already disabled Schema</li>
 *     <li>an existing schema</li>
 *     <li>an existing schema which will break the Registries when disabled</li>
 *     <li>a non existing schema</li>
 *   </ul>
 * </ul>
 * <ul>
 *   <li>Adding a new schema</li>
 *   <ul>
 *     <li>enabled schema</li>
 *     <ul>
 *       <li>Adding a new enabled valid schema with no deps</li>
 *       <li>Adding a new enabled schema with existing deps</li>
 *       <li>Adding a new enabled schema with non existing deps</li>
 *       <li>Adding a new enabled schema with an already existing name</li>
 *       <li>Adding a new enabled schema with deps on disabled schema</li>
 *     </ul>
 *     <li>disabled schema</li>
 *     <ul>
 *       <li>Adding a new disabled valid schema with no deps</li>
 *       <li>Adding a new disabled schema with existing deps</li>
 *       <li>Adding a new disabled schema with non existing deps</li>
 *       <li>Adding a new disabled schema with an already existing name</li>
 *       <li>Adding a new disabled schema with deps on disabled schema</li>
 *     </ul>
 *   </ul>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "MetaSchemaHandlerIT")
public class MetaSchemaHandlerIT extends AbstractMetaSchemaObjectHandler
{
    /** a test attribute in the test schema: uidNumber in nis schema */
    private static final String UID_NUMBER_ATTR = "uidnumber";

    /** Another test attribute : krb5principalName taken from the Krb5Kdc schema */
    private static final String KRB5_PRINCIPAL_NAME_ATTR = "krb5PrincipalName";

    public static SchemaManager schemaManager;
    private static LdapConnection connection;


    @Before
    public void setup() throws Exception
    {
        super.init();
        connection = IntegrationUtils.getAdminConnection( getService() );
        schemaManager = getService().getSchemaManager();

        // check that there is a samba schema installed and that is is disabled
        Entry entry = connection.lookup( "cn=samba,ou=schema" );
        assertNotNull( entry );
        assertTrue( entry.get( MetaSchemaConstants.M_DISABLED_AT ).contains( "TRUE" ) );
        entry = connection.lookup( "ou=attributeTypes,cn=samba,ou=schema" );
        assertNotNull( entry );
        assertTrue( entry.get( SchemaConstants.OU_AT ).contains( "attributetypes" ) );

        // Disable the NIS schema
        IntegrationUtils.disableSchema( getService(), "nis" );
    }


    private void createDisabledBrokenSchema() throws Exception
    {
        Dn dn = new Dn( "cn=broken,ou=schema" );

        // Create the schema
        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: broken",
            MetaSchemaConstants.M_DISABLED_AT, "TRUE" );

        connection.add( dummySchema );
    }


    private void createEnabledValidSchema( String schemaName ) throws Exception
    {
        Dn dn = new Dn( "cn=" + schemaName + ",ou=schema" );

        // Create the schema
        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn", schemaName );

        connection.add( entry );
    }


    // -----------------------------------------------------------------------
    // Enabling Schema tests
    // -----------------------------------------------------------------------
    /**
     * Checks to make sure updates enabling a metaSchema object in
     * the schema partition triggers the loading of that schema into
     * the global registries.
     *
     * @throws Exception on error
     */
    @Test
    public void testEnableExistingSchema() throws Exception
    {
        // Chck that the nis schema is loaded
        assertTrue( IntegrationUtils.isLoaded( getService(), "nis" ) );

        // check that the nis schema is not enabled
        assertTrue( IntegrationUtils.isDisabled( getService(), "nis" ) );

        // double check and make sure an attribute from that schema is
        // not in the AttributeTypeRegistry
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );

        // now enable the test schema
        IntegrationUtils.enableSchema( getService(), "nis" );

        // now test that the schema is loaded
        assertTrue( IntegrationUtils.isEnabled( getService(), "nis" ) );

        // double check and make sure the test attribute from the
        // test schema is now loaded and present within the attr registry
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
    }


    /**
     * Checks that trying to enable a non existing schema does not work
     *
     * @throws Exception on error
     */
    @Test
    public void testEnableNotExistingSchema() throws Exception
    {
        // check that the 'wrong' schema is not loaded
        assertFalse( IntegrationUtils.isLoaded( getService(), "wrong" ) );

        // now enable the 'wrong' schema
        try
        {
            IntegrationUtils.enableSchema( getService(), "wrong" );
            fail();
        }
        catch ( LdapException lnnfe )
        {
            // Expected
            assertTrue( true );
        }

        // Test again that the schema is not loaded
        assertFalse( IntegrationUtils.isLoaded( getService(), "wrong" ) );
    }


    /**
     * Checks to make sure that if we try to enable an already enabled
     * schema, we don't do anything.
     *
     * @throws Exception on error
     */
    @Test
    public void testEnableSchemaAlreadyEnabled() throws Exception
    {
        // check that the nis schema is loaded
        assertTrue( IntegrationUtils.isLoaded( getService(), "nis" ) );

        // Ceck that it's not enabled
        assertTrue( IntegrationUtils.isDisabled( getService(), "nis" ) );

        // double check and make sure an attribute from that schema is
        // not in the AttributeTypeRegistry
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );

        // now enable the test schema
        IntegrationUtils.enableSchema( getService(), "nis" );

        // and enable it again (it should not do anything)
        IntegrationUtils.enableSchema( getService(), "nis" );

        // now test that the schema is loaded
        assertTrue( IntegrationUtils.isEnabled( getService(), "nis" ) );

        // double check and make sure the test attribute from the
        // test schema is now loaded and present within the attr registry
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
    }


    /**
     * Checks that if we enable a schema which will break the registries, we get
     * an error.
     *
     * @throws Exception on error
     */
    @Test
    public void testEnableSchemaBreakingRegistries() throws Exception
    {
        // TODO : create a special Schema colliding with an existing one
    }


    // -----------------------------------------------------------------------
    // Disabling Schema tests
    // -----------------------------------------------------------------------
    /**
     * Checks to make sure updates disabling a metaSchema object in
     * the schema partition triggers the loading of that schema into
     * the global registries.
     *
     * @throws Exception on error
     */
    @Test
    public void testDisableExistingSchema() throws Exception
    {
        // Check that the krb5kdc schema is loaded
        assertTrue( IntegrationUtils.isLoaded( getService(), "krb5kdc" ) );

        // check that the krb5kdc schema is enabled
        assertTrue( IntegrationUtils.isEnabled( getService(), "krb5kdc" ) );

        // double check and make sure an attribute from that schema is
        // in the AttributeTypeRegistry
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( KRB5_PRINCIPAL_NAME_ATTR ) );

        // now disable the krb5kdc schema
        IntegrationUtils.disableSchema( getService(), "krb5kdc" );

        // now test that the schema is not enabled
        assertTrue( IntegrationUtils.isDisabled( getService(), "krb5kdc" ) );

        // double check and make sure the test attribute from the
        // test schema is now loaded and present within the attr registry
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( KRB5_PRINCIPAL_NAME_ATTR ) );
    }


    /**
     * Checks that trying to disable a non existing schema does not work
     *
     * @throws Exception on error
     */
    @Test
    public void testDisableNotExistingSchema() throws Exception
    {
        // check that the 'wrong' schema is not loaded
        assertFalse( IntegrationUtils.isLoaded( getService(), "wrong" ) );

        // now disable the 'wrong' schema
        try
        {
            IntegrationUtils.disableSchema( getService(), "wrong" );
            fail();
        }
        catch ( LdapException lnnfe )
        {
            // Expected
            assertTrue( true );
        }

        // Test again that the schema is not loaded
        assertFalse( IntegrationUtils.isLoaded( getService(), "wrong" ) );
    }


    /**
     * Checks to make sure that if we try to disable an already disabled
     * schema, we don't do anything.
     *
     * @throws Exception on error
     */
    @Test
    public void testDisableSchemaAlreadyDisabled() throws Exception
    {
        // check that the nis schema is loaded
        assertTrue( IntegrationUtils.isLoaded( getService(), "nis" ) );

        // Check that it's not enabled
        assertTrue( IntegrationUtils.isDisabled( getService(), "nis" ) );

        // double check and make sure an attribute from that schema is
        // not in the AttributeTypeRegistry
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );

        // now disable the test schema again
        IntegrationUtils.disableSchema( getService(), "nis" );

        // now test that the schema is not loaded
        assertTrue( IntegrationUtils.isDisabled( getService(), "nis" ) );

        // double check and make sure the test attribute from the
        // test schema is not loaded and present within the attr registry
        assertFalse( schemaManager.getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
    }


    /**
     * Checks that if we disable a schema which will break the registries, we get
     * an error.
     *
     * @throws Exception on error
     */
    @Test
    public void testDisableSchemaBreakingRegistries() throws Exception
    {
        // check that the nis schema is loaded
        assertTrue( IntegrationUtils.isLoaded( getService(), "system" ) );

        // Check that it's enabled
        assertTrue( IntegrationUtils.isEnabled( getService(), "system" ) );

        // double check and make sure an attribute from that schema is
        // in the AttributeTypeRegistry
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( "cn" ) );

        // now disable the system schema : it should break the registries, thus being rejected
        IntegrationUtils.disableSchema( getService(), "system" );

        // now test that the schema is not loaded
        assertTrue( IntegrationUtils.isEnabled( getService(), "system" ) );

        // double check and make sure the test attribute from the
        // test schema is loaded and present within the attr registry
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( "cn" ) );
    }


    // -----------------------------------------------------------------------
    // Schema Add Tests
    // -----------------------------------------------------------------------
    /**
     * Add a valid and enabled schema
     */
    @Test
    public void testAddEnabledValidSchema() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        assertFalse( isOnDisk( dn ) );

        createEnabledValidSchema( "dummy" );

        assertTrue( IntegrationUtils.isEnabled( getService(), "dummy" ) );
        assertNotNull( connection.lookup( "cn=dummy,ou=schema" ) );

        assertTrue( isOnDisk( dn ) );
    }


    /**
     * Add a valid and enabled schema with existing enabled deps
     */
    @Test
    public void testAddEnabledSchemaWithExistingEnabledDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        assertFalse( isOnDisk( dn ) );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: core",
            "m-dependencies: system",
            MetaSchemaConstants.M_DISABLED_AT, "FALSE" );

        connection.add( dummySchema );

        assertTrue( IntegrationUtils.isEnabled( getService(), "dummy" ) );
        assertNotNull( connection.lookup( "cn=dummy, ou=schema" ) );

        assertTrue( isOnDisk( dn ) );
    }


    /**
     * Add a valid and enabled schema with existing disabled deps. It should not be loaded.
     */
    @Test
    public void testAddEnabledSchemaWithExistingDisabledDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        assertFalse( isOnDisk( dn ) );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: core",
            "m-dependencies: nis",
            MetaSchemaConstants.M_DISABLED_AT, "FALSE" );

        try
        {
            connection.add( dummySchema );
            fail();
        }
        catch ( LdapException lonse )
        {
            // expected
        }

        assertFalse( IntegrationUtils.isLoaded( getService(), "dummy" ) );
        assertFalse( isOnDisk( dn ) );
    }


    /**
     * Add a valid and enabled schema with not existing deps. It should not be loaded.
     */
    @Test
    public void testAddEnabledSchemaWithNotExistingDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        assertFalse( isOnDisk( dn ) );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: core",
            "m-dependencies: wrong",
            MetaSchemaConstants.M_DISABLED_AT, "FALSE" );

        try
        {
            connection.add( dummySchema );
            fail();
        }
        catch ( LdapException lonse )
        {
            // expected
        }

        assertFalse( IntegrationUtils.isLoaded( getService(), "dummy" ) );
        assertFalse( isOnDisk( dn ) );
    }


    // -----------------------------------------------------------------------
    // Schema Delete Tests
    // -----------------------------------------------------------------------
    /**
     * Delete a valid and enabled schema
     */
    @Test
    public void testDeleteEnabledValidSchema() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        // Create a schema we will delete
        createEnabledValidSchema( "dummy" );
        assertTrue( isOnDisk( dn ) );
        assertTrue( IntegrationUtils.isLoaded( getService(), "dummy" ) );

        // Delete the schema
        connection.delete( dn );

        assertFalse( isOnDisk( dn ) );
        assertFalse( IntegrationUtils.isLoaded( getService(), "dummy" ) );
    }


    /**
     * Tests the addition of a new metaSchema object that is disabled
     * on addition and has no dependencies.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testAddDisabledSchemaNoDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            MetaSchemaConstants.M_DISABLED_AT, "TRUE" );

        connection.add( dummySchema );

        assertFalse( IntegrationUtils.isEnabled( getService(), "dummy" ) );
        assertNotNull( connection.lookup( "cn=dummy,ou=schema" ) );
    }


    /**
     * Tests the addition of a new metaSchema object that is disabled
     * on addition and has dependencies.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testAddDisabledSchemaWithDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            MetaSchemaConstants.M_DISABLED_AT, "TRUE",
            "m-dependencies: nis",
            "m-dependencies: core" );

        connection.add( dummySchema );

        assertFalse( IntegrationUtils.isEnabled( getService(), "dummy" ) );
        assertNotNull( connection.lookup( "cn=dummy,ou=schema" ) );
    }


    /**
     * Tests the rejection of a new metaSchema object that is disabled
     * on addition and has missing dependencies.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testRejectDisabledSchemaAddWithMissingDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            MetaSchemaConstants.M_DISABLED_AT, "TRUE",
            "m-dependencies: missing",
            "m-dependencies: core" );

        try
        {
            connection.add( dummySchema );
        }
        catch ( LdapException e )
        {
            // expected
        }

        assertFalse( IntegrationUtils.isEnabled( getService(), "dummy" ) );

        //noinspection EmptyCatchBlock
        try
        {
            connection.lookup( "cn=dummy,ou=schema" );
            fail( "schema should not be added to schema partition" );
        }
        catch ( LdapException e )
        {
        }
    }


    /**
     * Tests the addition of a new metaSchema object that is enabled
     * on addition and has no dependencies.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testAddEnabledSchemaNoDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaSchema",
            "cn: dummy"
            );

        connection.add( dummySchema );

        assertTrue( IntegrationUtils.isEnabled( getService(), "dummy" ) );
        assertNotNull( connection.lookup( "cn=dummy,ou=schema" ) );
    }


    /**
     * Tests the rejection of a metaSchema object add that is enabled
     * on addition yet has disabled dependencies.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testRejectEnabledSchemaAddWithDisabledDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: nis" );

        try
        {
            connection.add( dummySchema );
            fail( "should not be able to add enabled schema with deps on disabled schemas" );
        }
        catch ( LdapException e )
        {
            // expected
        }

        assertFalse( IntegrationUtils.isEnabled( getService(), "dummy" ) );

        //noinspection EmptyCatchBlock
        try
        {
            connection.lookup( "cn=dummy,ou=schema" );
            fail( "schema should not be added to schema partition" );
        }
        catch ( LdapException e )
        {
        }
    }


    // -----------------------------------------------------------------------
    // Schema Delete Tests
    // -----------------------------------------------------------------------

    /**
     * Makes sure we can delete schemas that have no dependents.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testDeleteSchemaNoDependents() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        // add the dummy schema enabled
        testAddEnabledSchemaNoDeps();
        assertTrue( IntegrationUtils.isEnabled( getService(), "dummy" ) );

        // delete it now
        connection.delete( dn );
        assertFalse( IntegrationUtils.isEnabled( getService(), "dummy" ) );
    }


    /**
     * Makes sure we can NOT delete schemas that have dependents.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testRejectSchemaDeleteWithDependents() throws Exception
    {
        // add the dummy schema enabled
        testAddEnabledSchemaNoDeps();
        assertTrue( IntegrationUtils.isEnabled( getService(), "dummy" ) );

        // make the nis schema depend on the dummy schema
        connection.modify( "cn=nis,ou=schema",
            new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "m-dependencies", "dummy" ) );

        // attempt to delete it now & it should fail
        try
        {
            connection.delete( "cn=dummy,ou=schema" );
            fail( "should not be able to delete a schema with dependents" );
        }
        catch ( LdapException e )
        {
            // expected
        }

        assertTrue( IntegrationUtils.isEnabled( getService(), "dummy" ) );
    }


    /**
     * Tests the rejection of a new metaSchema object that is enabled
     * on addition and missing dependencies.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testRejectEnabledSchemaAddWithMisingDeps() throws Exception
    {
        Dn dn = new Dn( "cn=dummy,ou=schema" );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: missing" );

        try
        {
            connection.add( dummySchema );
            fail( "should not be able to add enabled schema with deps on missing schemas" );
        }
        catch ( LdapException e )
        {
            // expected
        }

        assertFalse( IntegrationUtils.isEnabled( getService(), "dummy" ) );

        //noinspection EmptyCatchBlock
        try
        {
            connection.lookup( "cn=dummy,ou=schema" );
            fail( "schema should not be added to schema partition" );
        }
        catch ( LdapException e )
        {
        }
    }


    /**
     * Checks to make sure updates disabling a metaSchema object in
     * the schema partition triggers the unloading of that schema from
     * the global registries.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testDisableSchemaWithEnabledDependents() throws Exception
    {
        // let's enable the test schema and add the dummy schema
        // as enabled by default and dependends on the test schema

        // enables the test schema and samba
        testEnableExistingSchema();

        // adds enabled dummy schema that depends on the test schema
        Dn dn = new Dn( "cn=dummy, ou=schema" );

        Entry dummySchema = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: nis" );

        connection.add( dummySchema );

        // check that the nis schema is loaded and the dummy schema is loaded
        assertTrue( IntegrationUtils.isEnabled( getService(), "nis" ) );
        assertTrue( IntegrationUtils.isEnabled( getService(), "dummy" ) );

        // double check and make sure an attribute from that schema is
        // in the AttributeTypeRegistry
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );

        // now try to disable the test schema which should fail
        // since it's dependent, the dummy schema, is enabled
        try
        {
            connection.modify( "cn=nis,ou=schema",
                new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "m-disabled", "TRUE" ) );
            fail( "attempt to disable schema with enabled dependents should fail" );
        }
        catch ( LdapException e )
        {
            // expected
        }

        // now test that both schema are still loaded
        assertTrue( IntegrationUtils.isEnabled( getService(), "nis" ) );
        assertTrue( IntegrationUtils.isEnabled( getService(), "dummy" ) );

        // double check and make sure the test attribute from the test
        // schema is still loaded and present within the attr registry
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
    }


    // -----------------------------------------------------------------------
    // Schema Rename Tests
    // -----------------------------------------------------------------------
    /**
     * Makes sure we can change the name of a schema with entities in it.
     * Will use the samba schema which comes out of the box and nothing
     * depends on.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testSchemaRenameDisabledSchema() throws Exception
    {
        connection.rename( "cn=samba,ou=schema", "cn=foo" );
        assertNotNull( connection.lookup( "cn=foo,ou=schema" ) );

        // check that there is a samba schema installed and that is is disabled
        Entry entry = connection.lookup( "cn=foo,ou=schema" );
        assertNotNull( entry );
        assertTrue( entry.get( MetaSchemaConstants.M_DISABLED_AT ).contains( "TRUE" ) );
        entry = connection.lookup( "ou=attributeTypes,cn=foo,ou=schema" );
        assertNotNull( entry );
        assertTrue( entry.get( SchemaConstants.OU_AT ).contains( "attributetypes" ) );

        //noinspection EmptyCatchBlock
        try
        {
            connection.lookup( "cn=samba,ou=schema" );
            fail( "the samba schema should not be present after a rename to foo" );
        }
        catch ( LdapException e )
        {
        }
    }


    /**
     * Makes sure we can NOT change the name of a schema that has dependents.
     * Will use the nis schema which comes out of the box and has samba as
     * it's dependent.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testRejectSchemaRenameWithDeps() throws Exception
    {
        try
        {
            connection.rename( "cn=nis,ou=schema", "cn=foo" );
            fail( "should not be able to rename nis which has samba as it's dependent" );
        }
        catch ( LdapException onse )
        {
            // expected
        }

        assertNotNull( connection.lookup( "cn=nis,ou=schema" ) );

        //noinspection EmptyCatchBlock
        try
        {
            connection.lookup( "cn=foo,ou=schema" );
            fail( "the foo schema should not be present after rejecting the rename" );
        }
        catch ( LdapException e )
        {
        }
    }


    /**
     * Makes sure we can change the name of a schema with entities in it.
     * Will use the samba schema which comes out of the box and nothing
     * depends on.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testSchemaRenameEnabledSchema() throws Exception
    {
        IntegrationUtils.enableSchema( getService(), "samba" );
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( "sambaNTPassword" ) );
        assertEquals( "samba", schemaManager.getAttributeTypeRegistry().getSchemaName( "sambaNTPassword" ) );

        connection.rename( "cn=samba,ou=schema", "cn=foo" );
        assertNotNull( connection.lookup( "cn=foo, ou=schema" ) );
        assertTrue( schemaManager.getAttributeTypeRegistry().contains( "sambaNTPassword" ) );
        assertEquals( "foo", schemaManager.getAttributeTypeRegistry().getSchemaName( "sambaNTPassword" ) );

        //noinspection EmptyCatchBlock
        try
        {
            connection.lookup( "cn=samba, ou=schema" );
            fail( "the samba schema should not be present after a rename to foo" );
        }
        catch ( LdapException e )
        {
        }
    }


    // -----------------------------------------------------------------------
    // Dependency Modify Tests
    // -----------------------------------------------------------------------
    /**
     * Checks to make sure the addition of an undefined schema to the dependencies
     * of an existing schema fail with an UNWILLING_TO_PERFORM result code.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testRejectAddBogusDependency() throws Exception
    {
        try
        {
            connection.modify( "cn=nis,ou=schema",
                new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "m-dependencies", "bogus" ) );
            fail( "Should not be able to add bogus dependency to schema" );
        }
        catch ( LdapException onse )
        {
            // expected
        }
    }


    /**
     * Checks to make sure the addition of an defined yet disabled schema to the
     * dependencies of an existing enabled schema fails with an UNWILLING_TO_PERFORM
     * result code.  You must enable the dependency to add it or disable the schema
     * depending on it to add it.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testRejectAddOfDisabledDependencyToEnabledSchema() throws Exception
    {
        IntegrationUtils.enableSchema( getService(), "nis" );

        try
        {
            connection.modify( "cn=nis,ou=schema",
                new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "m-dependencies", "mozilla" ) );
            fail( "Should not be able to add disabled dependency to schema" );
        }
        catch ( LdapException onse )
        {
            // expected
        }
    }


    /**
     * Checks to make sure the addition of an defined yet disabled schema to the
     * dependencies of an existing disabled schema succeeds.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testAddOfDisabledDependencyToDisabledSchema() throws Exception
    {
        connection.modify( "cn=nis,ou=schema",
            new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "m-dependencies", "mozilla" ) );

        Entry entry = connection.lookup( "cn=nis,ou=schema" );
        Attribute dependencies = entry.get( "m-dependencies" );
        assertTrue( dependencies.contains( "mozilla" ) );
    }


    /**
     * Checks to make sure the addition of an defined yet enabled schema to the
     * dependencies of an existing disabled schema succeeds.
     *
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testAddOfEnabledDependencyToDisabledSchema() throws Exception
    {
        connection.modify( "cn=nis,ou=schema",
            new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "m-dependencies", "java" ) );
        Entry entry = connection.lookup( "cn=nis,ou=schema" );
        Attribute dependencies = entry.get( "m-dependencies" );
        assertTrue( dependencies.contains( "java" ) );
    }
}
