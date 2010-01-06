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
 * A test class for SchemaManager loadWithDeps() method. We test those methods here :
 * 
 *  Server API
 *     boolean loadWithDeps( Schema... schemas ) throws Exception
 *     boolean loadWithDeps( String... schemas ) throws Exception
 *
 *  Studio API :
 *     boolean loadWithDepsRelaxed( Schema... schemas ) throws Exception
 *     boolean loadWithDepsRelaxed( String... schemas ) throws Exception
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaManagerLoadWithDepsTest
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
            String path = SchemaManagerLoadWithDepsTest.class.getResource( "" ).getPath();
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

        schemaManager.loadWithDeps( "system" );

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

        schemaManager.loadWithDeps( "core" );

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

        schemaManager.loadWithDeps( "apache" );

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

        schemaManager.loadWithDeps( "apacheMeta" );

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

        schemaManager.loadWithDeps( "Java" );

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
     * "apache" and "apacheMeta"
     */
    @Test
    public void testLoadOther() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        schemaManager.loadWithDeps( "other" );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 176, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 48, schemaManager.getComparatorRegistry().size() );
        assertEquals( 48, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 50, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 66, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 66, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 71, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 361, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 5, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "apache" ) );
        assertNotNull( schemaManager.getRegistries().getLoadedSchema( "apacheMeta" ) );
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

        schemaManager.loadWithDeps( "cosine" );

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

        schemaManager.loadWithDeps( "InetOrgPerson" );

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

        schemaManager.loadWithDeps( "Collective" );

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

        schemaManager.loadWithDeps( "Krb5Kdc" );

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

        schemaManager.loadWithDeps( "nis" );

        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 0, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 0, schemaManager.getComparatorRegistry().size() );
        assertEquals( 0, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 0, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 0, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 0, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 0, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 0, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 0, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "core" ) );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "cosine" ) );
    }


    /**
     * Test loading a wrong schema
     */
    @Test
    public void testLoadWrongSchema() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

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
        assertEquals( 0, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 0, schemaManager.getComparatorRegistry().size() );
        assertEquals( 0, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 0, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 0, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 0, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 0, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 0, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 0, schemaManager.getRegistries().getLoadedSchemas().size() );
    }


    /**
     * test loading the "InetOrgPerson" and "core" schema, which depends on "system" and "cosine"
     */
    @Test
    public void testLoadCoreAndInetOrgPerson() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        schemaManager.loadWithDeps( "core", "InetOrgPerson" );

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

        try
        {
            schemaManager.loadWithDeps( "core", "bad", "InetOrgPerson" );
            fail();
        }
        catch ( LdapOperationNotSupportedException lonse )
        {
            // expected
        }

        // No SchemaObject should be loaded as we had an error
        assertTrue( schemaManager.getErrors().isEmpty() );
        assertEquals( 0, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( 0, schemaManager.getComparatorRegistry().size() );
        assertEquals( 0, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( 0, schemaManager.getNormalizerRegistry().size() );
        assertEquals( 0, schemaManager.getObjectClassRegistry().size() );
        assertEquals( 0, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( 0, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( 0, schemaManager.getGlobalOidRegistry().size() );

        assertEquals( 0, schemaManager.getRegistries().getLoadedSchemas().size() );
        assertNull( schemaManager.getRegistries().getLoadedSchema( "system" ) );
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

        schemaManager.loadWithDeps( "core", "nis", "InetOrgPerson" );

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
     * test loading the "InetOrgPerson", "core" and a disabled schema
     */
    @Test
    public void testLoadWithDepsCoreInetOrgPersonAndNis() throws Exception
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
    }
}
