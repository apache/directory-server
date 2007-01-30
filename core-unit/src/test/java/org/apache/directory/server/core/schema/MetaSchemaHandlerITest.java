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


import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * A test case which tests the correct operation of the schema 
 * entity handler.  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MetaSchemaHandlerITest extends AbstractAdminTestCase
{
    /** the schema to use for this test: one that is not loaded by default */
    private static final String TEST_SCHEMA = "nis";
    /** a test attribute in the test schema: uidNumber in nis schema */
    private static final String TEST_ATTR_OID = "1.3.6.1.1.1.1.0";
    
    
    /**
     * Checks to make sure updates enabling a metaSchema object in
     * the schema partition triggers the loading of that schema into
     * the global registries.
     */
    public void testEnableSchema() throws Exception
    {
        AttributeTypeRegistry atr = registries.getAttributeTypeRegistry();
        
        // check that the nis schema is not loaded
        assertNull( registries.getLoadedSchemas().get( TEST_SCHEMA ) );
        
        // double check and make sure an attribute from that schema is 
        // not in the AttributeTypeRegistry
        assertFalse( atr.hasAttributeType( TEST_ATTR_OID ) );
        
        // now enable the test schema
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-disabled", "FALSE" );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( "cn=nis", mods );
        
        // now test that the schema is loaded 
        assertNotNull( registries.getLoadedSchemas().get( TEST_SCHEMA ) );
        
        // double check and make sure the test attribute from the 
        // test schema is now loaded and present within the attr registry
        assertTrue( atr.hasAttributeType( TEST_ATTR_OID ) );
    }


    /**
     * Checks to make sure updates disabling a metaSchema object in
     * the schema partition triggers the unloading of that schema from
     * the global registries.
     */
    public void testDisableSchema() throws Exception
    {
        // let's enable the test schema
        testEnableSchema();
        
        AttributeTypeRegistry atr = registries.getAttributeTypeRegistry();
        
        // check that the nis schema is loaded
        assertNotNull( registries.getLoadedSchemas().get( TEST_SCHEMA ) );
        
        // double check and make sure an attribute from that schema is 
        // in the AttributeTypeRegistry
        assertTrue( atr.hasAttributeType( TEST_ATTR_OID ) );
        
        // now disable the test schema
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-disabled", "TRUE" );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( "cn=nis", mods );
        
        // now test that the schema is NOT loaded 
        assertNull( registries.getLoadedSchemas().get( TEST_SCHEMA ) );
        
        // double check and make sure the test attribute from the test  
        // schema is now NOT loaded and present within the attr registry
        assertFalse( atr.hasAttributeType( TEST_ATTR_OID ) );
    }
}
