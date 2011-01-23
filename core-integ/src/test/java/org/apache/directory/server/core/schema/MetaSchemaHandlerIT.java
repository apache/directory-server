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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.name.Dn;
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
    

    @Before
    public void checkSambaSchema() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // check that there is a samba schema installed and that is is disabled
        Attributes attributes = schemaRoot.getAttributes( "cn=samba" );
        assertNotNull( attributes );
        assertTrue( attributes.get( MetaSchemaConstants.M_DISABLED_AT ).contains( "TRUE" ) );
        attributes = schemaRoot.getAttributes( "ou=attributeTypes,cn=samba" );
        assertNotNull( attributes );
        assertTrue( attributes.get( SchemaConstants.OU_AT ).contains( "attributetypes" ) );
        
        // Disable the NIS schema
        IntegrationUtils.disableSchema( service, "nis" );
    }

    
    private void createDisabledBrokenSchema() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // Create the schema
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: broken",
            MetaSchemaConstants.M_DISABLED_AT, "TRUE" );
        
        schemaRoot.createSubcontext( "cn=broken", dummySchema );
    }

    
    private void createEnabledValidSchema( String schemaName ) throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // Create the schema
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn", schemaName );
        
        schemaRoot.createSubcontext( "cn=" + schemaName, dummySchema );
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
        assertTrue( IntegrationUtils.isLoaded( service, "nis" ) );

        // check that the nis schema is not enabled
        assertTrue( IntegrationUtils.isDisabled( service, "nis" ) );
        
        // double check and make sure an attribute from that schema is 
        // not in the AttributeTypeRegistry
        assertFalse( service.getSchemaManager().getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
        
        // now enable the test schema
        IntegrationUtils.enableSchema( service, "nis" );
        
        // now test that the schema is loaded 
        assertTrue( IntegrationUtils.isEnabled( service, "nis" ) );
        
        // double check and make sure the test attribute from the 
        // test schema is now loaded and present within the attr registry
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
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
        assertFalse( IntegrationUtils.isLoaded( service, "wrong" ) );
        
        // now enable the 'wrong' schema
        try
        {
            IntegrationUtils.enableSchema( service, "wrong" );
            fail();
        }
        catch ( NameNotFoundException lnnfe )
        {
            // Expected
            assertTrue( true );
        }
        
        // Test again that the schema is not loaded 
        assertFalse( IntegrationUtils.isLoaded( service, "wrong" ) );
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
        assertTrue( IntegrationUtils.isLoaded(  service, "nis" ) );
        
        // Ceck that it's not enabled
        assertTrue( IntegrationUtils.isDisabled( service, "nis" ) );
        
        // double check and make sure an attribute from that schema is 
        // not in the AttributeTypeRegistry
        assertFalse( service.getSchemaManager().getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
        
        // now enable the test schema
        IntegrationUtils.enableSchema( service, "nis" );
        
        // and enable it again (it should not do anything)
        IntegrationUtils.enableSchema( service, "nis" );
        
        // now test that the schema is loaded 
        assertTrue( IntegrationUtils.isEnabled( service, "nis" ) );
        
        // double check and make sure the test attribute from the 
        // test schema is now loaded and present within the attr registry
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
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
        assertTrue( IntegrationUtils.isLoaded( service, "krb5kdc" ) );

        // check that the krb5kdc schema is enabled
        assertTrue( IntegrationUtils.isEnabled( service, "krb5kdc" ) );
        
        // double check and make sure an attribute from that schema is 
        // in the AttributeTypeRegistry
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( KRB5_PRINCIPAL_NAME_ATTR ) );
        
        // now disable the krb5kdc schema
        IntegrationUtils.disableSchema( service, "krb5kdc" );
        
        // now test that the schema is not enabled 
        assertTrue( IntegrationUtils.isDisabled( service, "krb5kdc" ) );
        
        // double check and make sure the test attribute from the 
        // test schema is now loaded and present within the attr registry
        assertFalse( service.getSchemaManager().getAttributeTypeRegistry().contains( KRB5_PRINCIPAL_NAME_ATTR ) );
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
        assertFalse( IntegrationUtils.isLoaded( service, "wrong" ) );
        
        // now disable the 'wrong' schema
        try
        {
            IntegrationUtils.disableSchema( service, "wrong" );
            fail();
        }
        catch ( NameNotFoundException lnnfe )
        {
            // Expected
            assertTrue( true );
        }
        
        // Test again that the schema is not loaded 
        assertFalse( IntegrationUtils.isLoaded( service, "wrong" ) );
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
        assertTrue( IntegrationUtils.isLoaded(  service, "nis" ) );
        
        // Check that it's not enabled
        assertTrue( IntegrationUtils.isDisabled( service, "nis" ) );
        
        // double check and make sure an attribute from that schema is 
        // not in the AttributeTypeRegistry
        assertFalse( service.getSchemaManager().getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
        
        // now disable the test schema again
        IntegrationUtils.disableSchema( service, "nis" );

        // now test that the schema is not loaded 
        assertTrue( IntegrationUtils.isDisabled( service, "nis" ) );
        
        // double check and make sure the test attribute from the 
        // test schema is not loaded and present within the attr registry
        assertFalse( service.getSchemaManager().getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
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
        assertTrue( IntegrationUtils.isLoaded(  service, "system" ) );
        
        // Check that it's enabled
        assertTrue( IntegrationUtils.isEnabled( service, "system" ) );
        
        // double check and make sure an attribute from that schema is 
        // in the AttributeTypeRegistry
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( "cn" ) );
        
        // now disable the system schema : it should break the registries, thus being rejected
        IntegrationUtils.disableSchema( service, "system" );

        // now test that the schema is not loaded 
        assertTrue( IntegrationUtils.isEnabled( service, "system" ) );
        
        // double check and make sure the test attribute from the 
        // test schema is loaded and present within the attr registry
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( "cn" ) );
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
        Dn dn = getSchemaContainer( "dummy" );

        assertFalse( isOnDisk( dn ) );

        LdapContext schemaRoot = getSchemaContext( service );
        
        createEnabledValidSchema( "dummy" );
        
        assertTrue( IntegrationUtils.isEnabled( service, "dummy" ) );
        assertNotNull( schemaRoot.lookup( "cn=dummy" ) );
        
        assertTrue( isOnDisk( dn ) );
    }
    
    
    /**
     * Add a valid and enabled schema with existing enabled deps
     */
    @Test
    public void testAddEnabledSchemaWithExistingEnabledDeps() throws Exception
    {
        Dn dn = getSchemaContainer( "dummy" );

        assertFalse( isOnDisk( dn ) );

        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: core",
            "m-dependencies: system",
            MetaSchemaConstants.M_DISABLED_AT, "FALSE" );
        
        schemaRoot.createSubcontext( "cn=dummy", dummySchema );
        
        assertTrue( IntegrationUtils.isEnabled( service, "dummy" ) );
        assertNotNull( schemaRoot.lookup( "cn=dummy" ) );
        
        assertTrue( isOnDisk( dn ) );
    }
    
    
    /**
     * Add a valid and enabled schema with existing disabled deps. It should not be loaded.
     */
    @Test
    public void testAddEnabledSchemaWithExistingDisabledDeps() throws Exception
    {
        Dn dn = getSchemaContainer( "dummy" );

        assertFalse( isOnDisk( dn ) );

        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: core",
            "m-dependencies: nis",
            MetaSchemaConstants.M_DISABLED_AT, "FALSE" );
        
        try
        {
            schemaRoot.createSubcontext( "cn=dummy", dummySchema );
            fail();
        }
        catch ( OperationNotSupportedException lonse )
        {
            // expected        
        }
        
        assertFalse( IntegrationUtils.isLoaded( service, "dummy" ) );
        assertFalse( isOnDisk( dn ) );
    }
    
    
    /**
     * Add a valid and enabled schema with not existing deps. It should not be loaded.
     */
    @Test
    public void testAddEnabledSchemaWithNotExistingDeps() throws Exception
    {
        Dn dn = getSchemaContainer( "dummy" );

        assertFalse( isOnDisk( dn ) );

        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: core",
            "m-dependencies: wrong",
            MetaSchemaConstants.M_DISABLED_AT, "FALSE" );
        
        try
        {
            schemaRoot.createSubcontext( "cn=dummy", dummySchema );
            fail();
        }
        catch ( OperationNotSupportedException lonse )
        {
            // expected       
        }
        
        assertFalse( IntegrationUtils.isLoaded( service, "dummy" ) );
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
        Dn dn = getSchemaContainer( "dummy" );
        LdapContext schemaRoot = getSchemaContext( service );
        
        // Create a schema we will delete
        createEnabledValidSchema( "dummy" );
        assertTrue( isOnDisk( dn ) );
        assertTrue( IntegrationUtils.isLoaded( service, "dummy" ) );

        // Delete the schema
        schemaRoot.destroySubcontext( "cn=dummy" );

        assertFalse( isOnDisk( dn ) );
        assertFalse( IntegrationUtils.isLoaded( service, "dummy" ) );
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
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            MetaSchemaConstants.M_DISABLED_AT, "TRUE" );
        
        schemaRoot.createSubcontext( "cn=dummy", dummySchema );
        
        assertFalse( IntegrationUtils.isEnabled( service, "dummy" ) );
        assertNotNull( schemaRoot.lookup( "cn=dummy" ) );
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
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            MetaSchemaConstants.M_DISABLED_AT, "TRUE",
            "m-dependencies: nis",
            "m-dependencies: core" );
        
        schemaRoot.createSubcontext( "cn=dummy", dummySchema );
        
        assertFalse( IntegrationUtils.isEnabled( service, "dummy" ) );
        assertNotNull( schemaRoot.lookup( "cn=dummy" ) );
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
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            MetaSchemaConstants.M_DISABLED_AT, "TRUE",
            "m-dependencies: missing",
            "m-dependencies: core" );
        
        try
        {
            schemaRoot.createSubcontext( "cn=dummy", dummySchema );
        } 
        catch( OperationNotSupportedException e )
        {
            // expected        
        }
        
        assertFalse( IntegrationUtils.isEnabled( service, "dummy" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=dummy" );
            fail( "schema should not be added to schema partition" );
        }
        catch( NamingException e )
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
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass: metaSchema",
            "cn: dummy"
            );

        schemaRoot.createSubcontext( "cn=dummy", dummySchema );
        
        assertTrue( IntegrationUtils.isEnabled( service, "dummy" ) );
        assertNotNull( schemaRoot.lookup( "cn=dummy" ) );
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
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: nis" );
        
        try
        {
            schemaRoot.createSubcontext( "cn=dummy", dummySchema );
            fail( "should not be able to add enabled schema with deps on disabled schemas" );
        }
        catch( OperationNotSupportedException e )
        {
            // expected        
        }
        
        assertFalse( IntegrationUtils.isEnabled( service, "dummy" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=dummy" );
            fail( "schema should not be added to schema partition" );
        }
        catch( NamingException e )
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
        LdapContext schemaRoot = getSchemaContext( service );

        // add the dummy schema enabled 
        testAddEnabledSchemaNoDeps();
        assertTrue( IntegrationUtils.isEnabled( service, "dummy" ) );
        
        // delete it now
        schemaRoot.destroySubcontext( "cn=dummy" );
        assertFalse( IntegrationUtils.isEnabled( service, "dummy" ) );
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
        LdapContext schemaRoot = getSchemaContext( service );

        // add the dummy schema enabled
        testAddEnabledSchemaNoDeps();
        assertTrue( IntegrationUtils.isEnabled( service, "dummy" ) );
        
        // make the nis schema depend on the dummy schema
        ModificationItem[] mods = new ModificationItem[1];
        mods[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE,
                new BasicAttribute( "m-dependencies", "dummy" ) );
        schemaRoot.modifyAttributes( "cn=nis", mods );
        
        // attempt to delete it now & it should fail
        try
        {
            schemaRoot.destroySubcontext( "cn=dummy" );
            fail( "should not be able to delete a schema with dependents" );
        }
        catch ( OperationNotSupportedException e )
        {
            // expected        
        }

        assertTrue( IntegrationUtils.isEnabled( service, "dummy" ) );
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
        LdapContext schemaRoot = getSchemaContext( service );

        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: missing" );
        
        try
        {
            schemaRoot.createSubcontext( "cn=dummy", dummySchema );
            fail( "should not be able to add enabled schema with deps on missing schemas" );
        }
        catch( OperationNotSupportedException e )
        {
            // expected        
        }
        
        assertFalse( IntegrationUtils.isEnabled( service, "dummy" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=dummy" );
            fail( "schema should not be added to schema partition" );
        }
        catch( NamingException e )
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
        LdapContext schemaRoot = getSchemaContext( service );

        // let's enable the test schema and add the dummy schema
        // as enabled by default and dependends on the test schema
        
        // enables the test schema and samba
        testEnableExistingSchema(); 
        
        // adds enabled dummy schema that depends on the test schema  
        Attributes dummySchema = LdifUtils.createAttributes( 
            "objectClass: top", 
            "objectClass", MetaSchemaConstants.META_SCHEMA_OC,
            "cn: dummy",
            "m-dependencies: nis" );
        
        schemaRoot.createSubcontext( "cn=dummy", dummySchema );
        
        // check that the nis schema is loaded and the dummy schema is loaded
        assertTrue( IntegrationUtils.isEnabled( service, "nis" ) );
        assertTrue( IntegrationUtils.isEnabled( service, "dummy" ) );
        
        // double check and make sure an attribute from that schema is 
        // in the AttributeTypeRegistry
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
        
        // now try to disable the test schema which should fail 
        // since it's dependent, the dummy schema, is enabled
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-disabled", "TRUE" );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        
        try
        {
            schemaRoot.modifyAttributes( "cn=nis", mods );
            fail( "attempt to disable schema with enabled dependents should fail" );
        }
        catch ( OperationNotSupportedException e )
        {
            // expected        
        }
        
        // now test that both schema are still loaded 
        assertTrue( IntegrationUtils.isEnabled( service, "nis" ) );
        assertTrue( IntegrationUtils.isEnabled( service, "dummy" ) );
        
        // double check and make sure the test attribute from the test  
        // schema is still loaded and present within the attr registry
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( UID_NUMBER_ATTR ) );
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
        LdapContext schemaRoot = getSchemaContext( service );
        schemaRoot.rename( "cn=samba", "cn=foo" );
        assertNotNull( schemaRoot.lookup( "cn=foo" ) );

        // check that there is a samba schema installed and that is is disabled
        Attributes attributes = schemaRoot.getAttributes( "cn=foo" );
        assertNotNull( attributes );
        assertTrue( attributes.get( MetaSchemaConstants.M_DISABLED_AT ).contains( "TRUE" ) );
        attributes = schemaRoot.getAttributes( "ou=attributeTypes,cn=foo" );
        assertNotNull( attributes );
        assertTrue( attributes.get( SchemaConstants.OU_AT ).contains( "attributetypes" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=samba" );
            fail( "the samba schema should not be present after a rename to foo" );
        }
        catch( NameNotFoundException e )
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
        LdapContext schemaRoot = getSchemaContext( service );
        try
        {
            schemaRoot.rename( "cn=nis", "cn=foo" );
            fail( "should not be able to rename nis which has samba as it's dependent" );
        }
        catch ( OperationNotSupportedException onse )
        {
            // expected        
        }
        
        assertNotNull( schemaRoot.lookup( "cn=nis" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=foo" );
            fail( "the foo schema should not be present after rejecting the rename" );
        }
        catch( NameNotFoundException e )
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
        LdapContext schemaRoot = getSchemaContext( service );

        IntegrationUtils.enableSchema( service, "samba" );
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( "sambaNTPassword" ) );
        assertEquals( "samba", service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( "sambaNTPassword" ) );
        
        schemaRoot.rename( "cn=samba", "cn=foo" );
        assertNotNull( schemaRoot.lookup( "cn=foo" ) );
        assertTrue( service.getSchemaManager().getAttributeTypeRegistry().contains( "sambaNTPassword" ) );
        assertEquals( "foo", service.getSchemaManager().getAttributeTypeRegistry().getSchemaName( "sambaNTPassword" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=samba" );
            fail( "the samba schema should not be present after a rename to foo" );
        }
        catch( NameNotFoundException e )
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
        LdapContext schemaRoot = getSchemaContext( service );

        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-dependencies", "bogus" );
        mods[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        
        try
        {
            schemaRoot.modifyAttributes( "cn=nis", mods );
            fail( "Should not be able to add bogus dependency to schema" );
        }
        catch ( OperationNotSupportedException onse )
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
        LdapContext schemaRoot = getSchemaContext( service );
        IntegrationUtils.enableSchema( service, "nis" );
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-dependencies", "mozilla" );
        mods[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        
        try
        {
            schemaRoot.modifyAttributes( "cn=nis", mods );
            fail( "Should not be able to add disabled dependency to schema" );
        }
        catch ( OperationNotSupportedException onse )
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
        LdapContext schemaRoot = getSchemaContext( service );
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-dependencies", "mozilla" );
        mods[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        schemaRoot.modifyAttributes( "cn=nis", mods );
        Attributes attrs = schemaRoot.getAttributes( "cn=nis" );
        Attribute dependencies = attrs.get( "m-dependencies" );
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
        LdapContext schemaRoot = getSchemaContext( service );
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-dependencies", "java" );
        mods[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        schemaRoot.modifyAttributes( "cn=nis", mods );
        Attributes attrs = schemaRoot.getAttributes( "cn=nis" );
        Attribute dependencies = attrs.get( "m-dependencies" );
        assertTrue( dependencies.contains( "java" ) );
    }
}
