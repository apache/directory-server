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
package org.apache.directory.mavibot.btree;


import java.io.File;
import java.io.InputStream;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.server.config.LdifConfigExtractor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/**
 * Tests for MavibotPartitionBuilder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotPartitionBuilderTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    /** The configuration file */
    String configFile;
    
    private File outDir;

    private static File workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/server-work" );

    @Before
    public void init() throws Exception
    {
        outDir = folder.newFolder( "MavibotPartitionBuilderTest" );
        workDir.mkdir();
        File configDir = new File( workDir, "config" ); // could be any directory, cause the config is now in a single file

        configFile = LdifConfigExtractor.extractSingleFileConfig( configDir, "config.ldif", true );
    }
    
    @Test
    public void testBulkLoad() throws Exception
    {
        //File file = new File( outDir, "builder-test.ldif" );
        File file = new File( outDir, "30k.ldif" );
        //InputStream in = MavibotPartitionBuilder.class.getClassLoader().getResourceAsStream( "builder-test.ldif" );
        try ( InputStream in = MavibotPartitionBuilder.class.getClassLoader().getResourceAsStream( "30k.ldif" ) )
        {
            FileUtils.copyInputStreamToFile( in, file );
        }

        MavibotPartitionBuilder builder = new MavibotPartitionBuilder( configFile, file.getAbsolutePath(), outDir.getAbsolutePath() );
        
        builder.buildPartition();
        
        //test the trees
        
        RecordManager rm = builder.getRm();
        BTree masterTree = rm.getManagedTree( builder.getMasterTableName() );
        assertEquals( builder.getTotalEntries(), masterTree.getNbElems() );
    }
}
