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


import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.SetupMode;
import org.apache.directory.server.core.integ.annotations.Mode;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;
import java.util.Map;


/**
 * A test case which tests the correct operation of the schema 
 * entity handler.  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@Mode ( SetupMode.PRISTINE )
public class MetaSchemaHandlerIT
{
    /** the schema to use for this test: one that is not loaded by default */
    private static final String TEST_SCHEMA = "nis";
    /** a test attribute in the test schema: uidNumber in nis schema */
    private static final String TEST_ATTR_OID = "1.3.6.1.1.1.1.0";
    /** the name of the dummy schema to test metaSchema adds/deletes with */
    private static final String DUMMY_SCHEMA = "dummy";
    

    public static DirectoryService service;


    private static AttributeTypeRegistry getAttributeTypeRegistry()
    {
        return service.getRegistries().getAttributeTypeRegistry();
    }


    private static Map<String, Schema> getLoadedSchemas()
    {
        return service.getRegistries().getLoadedSchemas();
    }


    // -----------------------------------------------------------------------
    // Schema Add Tests
    // -----------------------------------------------------------------------

    
    /**
     * Tests the addition of a new metaSchema object that is disabled 
     * on addition and has no dependencies.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddDisabledSchemaNoDeps() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = new AttributesImpl( "objectClass", "top" );
        dummySchema.get( "objectClass" ).add( MetaSchemaConstants.META_SCHEMA_OC );
        dummySchema.put( "cn", DUMMY_SCHEMA );
        dummySchema.put( MetaSchemaConstants.M_DISABLED_AT, "TRUE" );
        schemaRoot.createSubcontext( "cn=" + DUMMY_SCHEMA, dummySchema );
        
        assertNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
        assertNotNull( schemaRoot.lookup( "cn=" + DUMMY_SCHEMA ) );
    }
    
    
    /**
     * Tests the addition of a new metaSchema object that is disabled 
     * on addition and has dependencies.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddDisabledSchemaWithDeps() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = new AttributesImpl( "objectClass", "top" );
        dummySchema.get( "objectClass" ).add( MetaSchemaConstants.META_SCHEMA_OC );
        dummySchema.put( "cn", DUMMY_SCHEMA );
        dummySchema.put( MetaSchemaConstants.M_DISABLED_AT, "TRUE" );
        dummySchema.put( MetaSchemaConstants.M_DEPENDENCIES_AT, TEST_SCHEMA );
        dummySchema.get( MetaSchemaConstants.M_DEPENDENCIES_AT ).add( "core" );
        schemaRoot.createSubcontext( "cn=" + DUMMY_SCHEMA, dummySchema );
        
        assertNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
        assertNotNull( schemaRoot.lookup( "cn=" + DUMMY_SCHEMA ) );
    }
    
    
    /**
     * Tests the rejection of a new metaSchema object that is disabled 
     * on addition and has missing dependencies.
     *
     * @throws Exception on error
     */
    @Test
    public void testRejectDisabledSchemaAddWithMissingDeps() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = new AttributesImpl( "objectClass", "top" );
        dummySchema.get( "objectClass" ).add( MetaSchemaConstants.META_SCHEMA_OC );
        dummySchema.put( "cn", DUMMY_SCHEMA );
        dummySchema.put( MetaSchemaConstants.M_DISABLED_AT, "TRUE" );
        dummySchema.put( MetaSchemaConstants.M_DEPENDENCIES_AT, "missing" );
        dummySchema.get( MetaSchemaConstants.M_DEPENDENCIES_AT ).add( "core" );
        
        try
        {
            schemaRoot.createSubcontext( "cn=" + DUMMY_SCHEMA, dummySchema );
        } 
        catch( LdapOperationNotSupportedException e )
        {
            assertTrue( e.getResultCode().equals( ResultCodeEnum.UNWILLING_TO_PERFORM ) );
        }
        
        assertNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=" + DUMMY_SCHEMA );
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
    public void testAddEnabledSchemaNoDeps() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = new AttributesImpl( "objectClass", "top" );
        dummySchema.get( "objectClass" ).add( MetaSchemaConstants.META_SCHEMA_OC );
        dummySchema.put( "cn", DUMMY_SCHEMA );
        schemaRoot.createSubcontext( "cn=" + DUMMY_SCHEMA, dummySchema );
        
        assertNotNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
        assertNotNull( schemaRoot.lookup( "cn=" + DUMMY_SCHEMA ) );
    }
    
    
    /**
     * Tests the rejection of a metaSchema object add that is enabled 
     * on addition yet has disabled dependencies.
     *
     * @throws Exception on error
     */
    @Test
    public void testRejectEnabledSchemaAddWithDisabledDeps() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes dummySchema = new AttributesImpl( "objectClass", "top" );
        dummySchema.get( "objectClass" ).add( MetaSchemaConstants.META_SCHEMA_OC );
        dummySchema.put( "cn", DUMMY_SCHEMA );
        dummySchema.put( MetaSchemaConstants.M_DEPENDENCIES_AT, TEST_SCHEMA );
        
        try
        {
            schemaRoot.createSubcontext( "cn=" + DUMMY_SCHEMA, dummySchema );
            fail( "should not be able to add enabled schema with deps on disabled schemas" );
        }
        catch( LdapOperationNotSupportedException e )
        {
            assertTrue( e.getResultCode().equals( ResultCodeEnum.UNWILLING_TO_PERFORM ) );
        }
        
        assertNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=" + DUMMY_SCHEMA );
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
    public void testDeleteSchemaNoDependents() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // add the dummy schema enabled 
        testAddEnabledSchemaNoDeps();
        assertNotNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
        
        // delete it now
        schemaRoot.destroySubcontext( "cn=" + DUMMY_SCHEMA );
        assertNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
    }
    
    
    /**
     * Makes sure we can NOT delete schemas that have dependents.
     *
     * @throws Exception on error
     */
    @Test
    public void testRejectSchemaDeleteWithDependents() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // add the dummy schema enabled
        testAddEnabledSchemaNoDeps();
        assertNotNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
        
        // make the nis schema depend on the dummy schema
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        mods[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE,
                new AttributeImpl( MetaSchemaConstants.M_DEPENDENCIES_AT, DUMMY_SCHEMA ) );
        schemaRoot.modifyAttributes( "cn=" + TEST_SCHEMA, mods );
        
        // attempt to delete it now & it should fail
        try
        {
            schemaRoot.destroySubcontext( "cn=" + DUMMY_SCHEMA );
            fail( "should not be able to delete a schema with dependents" );
        }
        catch ( LdapOperationNotSupportedException e )
        {
            assertTrue( e.getResultCode().equals( ResultCodeEnum.UNWILLING_TO_PERFORM ) );
        }

        assertNotNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
    }
    
    
    /**
     * Tests the rejection of a new metaSchema object that is enabled 
     * on addition and missing dependencies.
     *
     * @throws Exception on error
     */
    @Test
    public void testRejectEnabledSchemaAddWithMisingDeps() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        Attributes dummySchema = new AttributesImpl( "objectClass", "top" );
        dummySchema.get( "objectClass" ).add( MetaSchemaConstants.META_SCHEMA_OC );
        dummySchema.put( "cn", DUMMY_SCHEMA );
        dummySchema.put( MetaSchemaConstants.M_DEPENDENCIES_AT, "missing" );
        
        try
        {
            schemaRoot.createSubcontext( "cn=" + DUMMY_SCHEMA, dummySchema );
            fail( "should not be able to add enabled schema with deps on missing schemas" );
        }
        catch( LdapOperationNotSupportedException e )
        {
            assertTrue( e.getResultCode().equals( ResultCodeEnum.UNWILLING_TO_PERFORM ) );
        }
        
        assertNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=" + DUMMY_SCHEMA );
            fail( "schema should not be added to schema partition" );
        }
        catch( NamingException e )
        {
        }
    }

    
    // -----------------------------------------------------------------------
    // Enable/Disable Schema Tests
    // -----------------------------------------------------------------------

    
    private void enableSchema( String schemaName ) throws NamingException
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // now enable the test schema
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-disabled", "FALSE" );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        schemaRoot.modifyAttributes( "cn=" + schemaName, mods );
    }
    
    
    private void disableSchema( String schemaName ) throws NamingException
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // now enable the test schema
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-disabled", "TRUE" );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        schemaRoot.modifyAttributes( "cn=" + schemaName, mods );
    }
    
    
    /**
     * Checks to make sure updates enabling a metaSchema object in
     * the schema partition triggers the loading of that schema into
     * the global registries.
     *
     * @throws Exception on error
     */
    @Test
    public void testEnableSchema() throws Exception
    {
        AttributeTypeRegistry atr = getAttributeTypeRegistry();
        
        // check that the nis schema is not loaded
        assertNull( getLoadedSchemas().get( TEST_SCHEMA ) );
        
        // double check and make sure an attribute from that schema is 
        // not in the AttributeTypeRegistry
        assertFalse( atr.hasAttributeType( TEST_ATTR_OID ) );
        
        // now enable the test schema
        enableSchema( "nis" );
        
        // now test that the schema is loaded 
        assertNotNull( getLoadedSchemas().get( TEST_SCHEMA ) );
        
        // double check and make sure the test attribute from the 
        // test schema is now loaded and present within the attr registry
        assertTrue( atr.hasAttributeType( TEST_ATTR_OID ) );
    }


    /**
     * Checks to make sure an attempt to disable a metaSchema fails if 
     * that schema has dependents which are enabled.
     *
     * @throws Exception on error
     */
    @Test
    public void testDisableSchema() throws Exception
    {
        // let's enable the test schema
        testEnableSchema();
        
        AttributeTypeRegistry atr = getAttributeTypeRegistry();
        
        // check that the nis schema is loaded
        assertNotNull( getLoadedSchemas().get( TEST_SCHEMA ) );
        
        // double check and make sure an attribute from that schema is 
        // in the AttributeTypeRegistry
        assertTrue( atr.hasAttributeType( TEST_ATTR_OID ) );
        
        // now disable the test schema 
        disableSchema( "samba" );
        disableSchema( "nis" );
        
        // now test that the schema is NOT loaded 
        assertNull( getLoadedSchemas().get( TEST_SCHEMA ) );
        
        // double check and make sure the test attribute from the test  
        // schema is now NOT loaded and present within the attr registry
        assertFalse( atr.hasAttributeType( TEST_ATTR_OID ) );
    }

    
    /**
     * Checks to make sure updates disabling a metaSchema object in
     * the schema partition triggers the unloading of that schema from
     * the global registries.
     *
     * @throws Exception on error
     */
    @Test
    public void testDisableSchemaWithEnabledDependents() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // let's enable the test schema and add the dummy schema
        // as enabled by default and dependends on the test schema
        
//      // enables the test schema and samba
        testEnableSchema(); 
        
        // adds enabled dummy schema that depends on the test schema  
        Attributes dummySchema = new AttributesImpl( "objectClass", "top" );
        dummySchema.get( "objectClass" ).add( MetaSchemaConstants.META_SCHEMA_OC );
        dummySchema.put( "cn", DUMMY_SCHEMA );
        dummySchema.put( MetaSchemaConstants.M_DEPENDENCIES_AT, TEST_SCHEMA );
        schemaRoot.createSubcontext( "cn=" + DUMMY_SCHEMA, dummySchema );
        
        // check that the nis schema is loaded and the dummy schema is loaded
        assertNotNull( getLoadedSchemas().get( TEST_SCHEMA ) );
        assertNotNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
        
        AttributeTypeRegistry atr = getAttributeTypeRegistry();
        
        // double check and make sure an attribute from that schema is 
        // in the AttributeTypeRegistry
        assertTrue( atr.hasAttributeType( TEST_ATTR_OID ) );
        
        // now try to disable the test schema which should fail 
        // since it's dependent, the dummy schema, is enabled
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-disabled", "TRUE" );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        
        try
        {
            schemaRoot.modifyAttributes( "cn=nis", mods );
            fail( "attempt to disable schema with enabled dependents should fail" );
        }
        catch ( LdapOperationNotSupportedException e )
        {
            assertTrue( e.getResultCode().equals( ResultCodeEnum.UNWILLING_TO_PERFORM ) );
        }
        
        // now test that both schema are still loaded 
        assertNotNull( getLoadedSchemas().get( TEST_SCHEMA ) );
        assertNotNull( getLoadedSchemas().get( DUMMY_SCHEMA ) );
        
        // double check and make sure the test attribute from the test  
        // schema is still loaded and present within the attr registry
        assertTrue( atr.hasAttributeType( TEST_ATTR_OID ) );
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
    public void testSchemaRenameDisabledSchema() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        schemaRoot.rename( "cn=samba", "cn=foo" );
        assertNotNull( schemaRoot.lookup( "cn=foo" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=samba" );
            fail( "the samba schema should not be present after a rename to foo" );
        }
        catch( LdapNameNotFoundException e )
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
    public void testRejectSchemaRenameWithDeps() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        try
        {
            schemaRoot.rename( "cn=nis", "cn=foo" );
            fail( "should not be able to rename nis which has samba as it's dependent" );
        }
        catch ( LdapOperationNotSupportedException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }
        
        assertNotNull( schemaRoot.lookup( "cn=nis" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=foo" );
            fail( "the foo schema should not be present after rejecting the rename" );
        }
        catch( LdapNameNotFoundException e )
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
    public void testSchemaRenameEnabledSchema() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        enableSchema( "samba" );
        assertTrue( getAttributeTypeRegistry().hasAttributeType( "sambaNTPassword" ) );
        assertEquals( "samba", getAttributeTypeRegistry().getSchemaName( "sambaNTPassword" ) );
        
        schemaRoot.rename( "cn=samba", "cn=foo" );
        assertNotNull( schemaRoot.lookup( "cn=foo" ) );
        assertTrue( getAttributeTypeRegistry().hasAttributeType( "sambaNTPassword" ) );
        assertEquals( "foo", getAttributeTypeRegistry().getSchemaName( "sambaNTPassword" ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaRoot.lookup( "cn=samba" );
            fail( "the samba schema should not be present after a rename to foo" );
        }
        catch( LdapNameNotFoundException e )
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
    public void testRejectAddBogusDependency() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-dependencies", "bogus" );
        mods[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attr );
        
        try
        {
            schemaRoot.modifyAttributes( "cn=" + TEST_SCHEMA, mods );
            fail( "Should not be able to add bogus dependency to schema" );
        }
        catch ( LdapOperationNotSupportedException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
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
    public void testRejectAddOfDisabledDependencyToEnabledSchema() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        enableSchema( TEST_SCHEMA );
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-dependencies", "mozilla" );
        mods[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attr );
        
        try
        {
            schemaRoot.modifyAttributes( "cn=" + TEST_SCHEMA, mods );
            fail( "Should not be able to add disabled dependency to schema" );
        }
        catch ( LdapOperationNotSupportedException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }
    }


    /**
     * Checks to make sure the addition of an defined yet disabled schema to the 
     * dependencies of an existing disabled schema succeeds. 
     *
     * @throws Exception on error
     */
    @Test
    public void testAddOfDisabledDependencyToDisabledSchema() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-dependencies", "mozilla" );
        mods[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attr );
        schemaRoot.modifyAttributes( "cn=" + TEST_SCHEMA, mods );
        Attributes attrs = schemaRoot.getAttributes( "cn=" + TEST_SCHEMA );
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
    public void testAddOfEnabledDependencyToDisabledSchema() throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-dependencies", "java" );
        mods[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attr );
        schemaRoot.modifyAttributes( "cn=" + TEST_SCHEMA, mods );
        Attributes attrs = schemaRoot.getAttributes( "cn=" + TEST_SCHEMA );
        Attribute dependencies = attrs.get( "m-dependencies" );
        assertTrue( dependencies.contains( "java" ) );
    }
}
