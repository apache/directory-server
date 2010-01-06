/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.schema.ldif.extractor;


import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

 
/**
 * Tests the DefaultSchemaLdifExtractor class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaLdifExtractorTest
{
    private static String workingDirectory;

    
    @BeforeClass
    public static void setup() throws IOException
    {
        workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaLdifExtractorTest.class.getResource( "" ).getPath();
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
    public void testExtract() throws Exception
    {
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(
            new File( workingDirectory ) );
        extractor.extractOrCopy();
    }
}
