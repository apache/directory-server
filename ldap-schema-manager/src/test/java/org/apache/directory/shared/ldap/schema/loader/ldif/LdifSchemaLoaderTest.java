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


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the LdifSchemaLoader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class LdifSchemaLoaderTest
{
    private static String workingDirectory;

    
    @BeforeClass
    public static void setup() throws IOException
    {
        workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = LdifSchemaLoaderTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }
        
        // Cleanup the target directory
        FileUtils.deleteDirectory( new File( workingDirectory + "/schema" ) );
    }
    
    
    @AfterClass
    public static void cleanup() throws IOException
    {
        // Cleanup the target directory
        FileUtils.deleteDirectory( new File( workingDirectory + "/schema" ) );
    }

    
    @Test
    public void testLoader() throws Exception
    {
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();
        
        LdifSchemaLoader loader = new LdifSchemaLoader( new File( workingDirectory, "schema" ) );
        SchemaManager sm = new DefaultSchemaManager( loader );

        boolean loaded = sm.loadAllEnabled();
        
        if ( !loaded )
        {
            fail( "Schema load failed : " + ExceptionUtils.printErrors( sm.getErrors() ) );
        }
        
        assertTrue( sm.getRegistries().getAttributeTypeRegistry().contains( "cn" ) );
    }
}
