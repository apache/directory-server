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
package org.apache.directory.shared.ldap.schema.loader.ldif;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.DefaultSchema;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * A test class for SchemaManager load() method. We test those methods here :
 * 
 *  Server API
 *     boolean load( Schema... schemas ) throws Exception
 *     boolean load( String... schemas ) throws Exception
 *     boolean loadDisabled( Schema... schemas ) throws Exception
 *     boolean loadDisabled( String... schemas ) throws Exception
 *     boolean loadAllEnabled() throws Exception
 *
 *  Studio API :
 *     boolean loadRelaxed( Schema... schemas ) throws Exception
 *     boolean loadRelaxed( String... schemas ) throws Exception
 *     boolean loadAllEnabledRelaxed() throws Exception 
 *     
 * We check the resulting number of SchemaObjects in the registries. Those number are :
 * 
 * Apache :
 *   AT :  53
 *   C  :   8
 *   MR :   8
 *   N  :   8
 *   OC :  17
 *   SC :   3
 *   S  :   7
 *   OID:  85
 *   
 * ApacheDns :
 *   AT :  16
 *   OC :  11
 *   OID:  27
 *   
 * ApacheMeta :
 *   AT :  31
 *   C  :   5
 *   MR :   5
 *   N  :   7
 *   OC :  13
 *   SC :   4
 *   S  :   5
 *   OID:  54
 * 
 * AutoFs :
 *   AT :   1
 *   OC :   2
 *   OID:   3
 * 
 * Collective :
 *   AT :  13
 *   OID:  13
 * 
 * Corba :
 *   AT :   2
 *   OC :   3
 *   OID:   5
 * 
 * Core :
 *   AT :  54
 *   OC :  27
 *   OID:  81
 * 
 * Cosine :
 *   AT :  41
 *   OC :  13
 *   OID:  54
 * 
 * Dhcp :
 *   AT :  39
 *   OC :  12
 *   OID:  51
 * 
 * InetOrgPerson :
 *   AT :   9
 *   OC :   1
 *   OID:  10
 * 
 * Java :
 *   AT :   7
 *   OC :   5
 *   OID:  12
 * 
 * Krb5Kdc :
 *   AT :  15
 *   OC :   3
 *   OID:  18
 * 
 * Mozilla :
 *   AT :  17
 *   OC :   1
 *   OID:  18
 * 
 * Nis :
 *   AT :  27
 *   C  :   1
 *   MR :   1
 *   N  :   1
 *   OC :  13
 *   SC :   2
 *   S  :   2
 *   OID:  43
 * 
 * Other :
 *   OID:   0
 * 
 * Samba :
 *   AT :  37
 *   OC :  11
 *   OID:  48
 * 
 * System :
 *   AT :  38
 *   C  :  35
 *   MR :  35
 *   N  :  35
 *   OC :   9
 *   SC :  59
 *   S  :  59
 *   OID: 141
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaManagerLoadTest
{
    // A directory in which the ldif files will be stored
    private static String workingDirectory;

    // The schema repository
    private static File schemaRepository;


    @BeforeClass
    public static void setup() throws Exception
    {
        workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaManagerLoadTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        schemaRepository = new File( workingDirectory, "schema" );

        // Cleanup the target directory
        FileUtils.deleteDirectory( schemaRepository );

        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();
    }


    @AfterClass
    public static void cleanup() throws IOException
    {
        // Cleanup the target directory
        FileUtils.deleteDirectory( schemaRepository );
    }


    //-------------------------------------------------------------------------
    // Test the load( String... schemaName) method
    //-------------------------------------------------------------------------
    /**
     * test loading the "system" schema 
     */
    @Test
    public void testLoadSystem() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 38, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 9, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 141, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 1, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
    }


    /**
     * test loading the "core" schema, which depends on "system"
     */
    @Test
    public void testLoadCore() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        // Check that we can't load a schema without its dependencies
        assertFalse( schemaManager.load( "core" ) );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 92, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 36, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 222, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 2, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
    }


    /**
     * test loading the "apache" schema, which depends on "system" and "core"
     */
    @Test
    public void testLoadApache() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );
        assertTrue( schemaManager.load( "apache" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 145, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 43, schemaManager.getComparatorRegistry().size() );
        assertEquals( 43, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 43, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 53, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 62, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 66, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 307, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 3, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "apache" ) );
    }


    /**
     * test loading the "apacheMeta" schema, which depends on "system"
     */
    @Test
    public void testLoadApacheMeta() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "apacheMeta" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 69, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 40, schemaManager.getComparatorRegistry().size() );
        assertEquals( 40, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 42, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 22, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 63, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 64, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 195, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 2, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "apachemeta" ) );
    }


    /**
     * test loading the "java" schema, which depends on "system" and "core"
     */
    @Test
    public void testLoadJava() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );
        assertTrue( schemaManager.load( "Java" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 99, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 41, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 234, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 3, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "Java" ) );
    }


    /**
     * test loading the "other" schema, which depends on "system", "core",
     * "apache" and "apacheMeta". As we don't have any cross dependencies
     * with any of this other schemas, we can only load core and system
     */
    @Test
    public void testLoadOther() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );
        assertTrue( schemaManager.load( "other" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 92, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 36, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 222, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 3, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "other" ) );
    }


    /**
     * test loading the "cosine" schema, which depends on "system" and "core"
     */
    @Test
    public void testLoadCosine() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );
        assertTrue( schemaManager.load( "cosine" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 133, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 49, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 276, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 3, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "cosine" ) );
    }


    /**
     * test loading the "InetOrgPerson" schema, which depends on "system", "core"
     * and "cosine"
     */
    @Test
    public void testLoadInetOrgPerson() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );
        assertTrue( schemaManager.load( "cosine" ) );
        assertTrue( schemaManager.load( "InetOrgPerson" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 142, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 50, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 286, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 4, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "cosine" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "InetOrgPerson" ) );
    }


    /**
     * test loading the "Collective" schema, which depends on "system" and "core"
     */
    @Test
    public void testLoadCollective() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );
        assertTrue( schemaManager.load( "Collective" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 105, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 36, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 235, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 3, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "collective" ) );
    }


    /**
     * test loading the "Krb5Kdc" schema, which depends on "system" and "core"
     */
    @Test
    public void testLoadKrb5Kdc() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );
        assertTrue( schemaManager.load( "Krb5Kdc" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 107, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 39, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 240, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 3, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "Krb5Kdc" ) );
    }


    /**
     * test loading the "nis" schema, which depends on "system", "core" and "cosine",
     * but is disabled
     */
    @Test
    public void testLoadNis() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core" ) );
        assertTrue( schemaManager.load( "cosine" ) );
        assertTrue( schemaManager.load( "nis" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 133, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 49, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 276, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 3, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "cosine" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "nis" ) );
    }


    /**
     * Test loading a wrong schema
     */
    @Test
    public void testLoadWrongSchema() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        try
        {
            schemaManager.loadWithDeps( "bad" );
            fail();
        }
        catch ( LdapOperationNotSupportedException lonse )
        {
            // expected
        }

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 38, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 9, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 141, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 1, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "bad" ) );
    }


    /**
     * test loading the "InetOrgPerson" and "core" schema, which depends on "system" and "cosine"
     */
    @Test
    public void testLoadCoreAndInetOrgPerson() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );
        assertTrue( schemaManager.load( "core", "cosine", "InetOrgPerson" ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 142, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 50, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 286, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 4, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "cosine" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "InetOrgPerson" ) );
    }


    /**
     * test loading the "InetOrgPerson", "core" and a bad schema
     */
    @Test
    public void testLoadCoreInetOrgPersonAndBad() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        assertTrue( schemaManager.load( "system" ) );

        try
        {
            assertFalse( schemaManager.load( "core", "bad", "cosine", "InetOrgPerson" ) );
            fail();
        }
        catch ( LdapOperationNotSupportedException lonse )
        {
            // expected
        }

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 38, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 9, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 141, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 1, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "cosine" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "InetOrgPerson" ) );
    }


    /**
     * test loading the "InetOrgPerson", "core" and a disabled schema
     */
    @Test
    public void testLoadCoreInetOrgPersonAndNis() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );
        
        assertTrue( schemaManager.load( "system" ) );
        
        // Try to load a disabled schema when the registries does
        // ot allow disabled schema to be loaded
        assertFalse( schemaManager.load( "core", "nis", "cosine", "InetOrgPerson" ) );

        assertFalse( schemaManager.getErrors().isEmpty() );
        assertEquals( 38, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 9, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 141, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 1, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "cosine" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "InetOrgPerson" ) );
    }


    //---------------------------------------------------------------------------
    // Test the load( Schema... ) method
    //---------------------------------------------------------------------------
    /**
     * test loading the "InetOrgPerson", "core" and an empty schema. The empty schema
     * should be present in the registries, as it's a vaid schema
     */
    @Test
    public void testLoadSchemasWithDepsCoreInetOrgPersonAndBad() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        Schema system = loader.getSchema( "system" );
        Schema core = loader.getSchema( "core" );
        Schema empty = new DefaultSchema( "empty" );
        Schema cosine = loader.getSchema( "cosine" );
        Schema inetOrgPerson = loader.getSchema( "InetOrgPerson" );

        assertTrue( schemaManager.load( system, core, empty, cosine, inetOrgPerson ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 142, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 35, schemaManager.getComparatorRegistry().size() );
        assertEquals( 35, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 35, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 50, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 59, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 59, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 286, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 5, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "cosine" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "InetOrgPerson" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "empty" ) );
    }
    
    
    /**
     * Test that we can load a new schema
     */
    @Test
    public void loadNewSchema() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        Schema dummy = new DefaultSchema( "dummy" );

        assertTrue( schemaManager.load( dummy ) );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 0, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 0, schemaManager.getComparatorRegistry().size() );
        assertEquals( 0, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 0, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 0, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 0, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 0, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 0, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 1, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "dummy" ) );
    }
    
    
    /**
     * Test that we can't load a new schema with bad dependencies
     */
    @Test
    public void loadNewSchemaBadDependencies() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        Schema dummy = new DefaultSchema( "dummy" );
        dummy.addDependencies( "bad" );

        assertFalse( schemaManager.load( dummy ) );

        assertFalse( schemaManager.getErrors().isEmpty() );
        assertEquals( 0, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 0, schemaManager.getComparatorRegistry().size() );
        assertEquals( 0, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 0, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 0, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 0, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 0, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 0, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 0, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "dummy" ) );
    }
}
