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

package org.apache.directory.server.config;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test class for ConfigWriter
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class ConfigWriterTest
{
    private static SchemaManager schemaManager;

    private static File workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/server-work" );


    @BeforeClass
    public static void readConfig() throws Exception
    {
        FileUtils.deleteDirectory( workDir );
        workDir.mkdir();

        String workingDirectory = workDir.getPath();
        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );

        if ( schemaRepository.exists() )
        {
            FileUtils.deleteDirectory( schemaRepository );
        }

        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( "Schema load failed : " + Exceptions.printErrors(errors) );
        }
    }


    @Test
    public void testConfigWriter() throws Exception
    {
        // Extracting of the config file
        File configDir = new File( workDir, "configWriter" ); // could be any directory, cause the config is now in a single file
        String configFile = LdifConfigExtractor.extractSingleFileConfig( configDir, "config.ldif", true );

        // Creating of the config partition
        SingleFileLdifPartition configPartition = new SingleFileLdifPartition();
        configPartition.setId( "config" );
        configPartition.setPartitionPath( new File( configFile ).toURI() );
        configPartition.setSuffix( new Dn( "ou=config" ) );
        configPartition.setSchemaManager( schemaManager );
        configPartition.initialize();

        // Reading the config partition
        ConfigPartitionReader cpReader = new ConfigPartitionReader( configPartition );
        ConfigBean configBean = cpReader.readConfig();
        assertNotNull( configBean );

        // Creating the config writer
        ConfigWriter configWriter = new ConfigWriter( schemaManager, configBean );

        // Reading the original config file
        LdifReader ldifReader = new LdifReader( configFile );
        List<LdifEntry> originalConfigEntries = new ArrayList<LdifEntry>();
        while ( ldifReader.hasNext() )
        {
            originalConfigEntries.add( ldifReader.next() );
        }
        ldifReader.close();

        // Getting the list of entries of generated config
        List<LdifEntry> generatedConfigEntries = configWriter.getConvertedLdifEntries();

        // Comparing the number of entries
        assertEquals( originalConfigEntries.size(), generatedConfigEntries.size() );

        // Comparing each entry in both lists (which have been sorted before)
        Comparator<LdifEntry> dnComparator = new Comparator<LdifEntry>()
        {
            public int compare( LdifEntry o1, LdifEntry o2 )
            {
                return o1.getDn().toString().compareToIgnoreCase( o2.getDn().toString() );
            }
        };
        Collections.sort( originalConfigEntries, dnComparator );
        Collections.sort( generatedConfigEntries, dnComparator );
        for ( int i = 0; i < originalConfigEntries.size(); i++ )
        {
            Entry originalConfigEntry = originalConfigEntries.get( i ).getEntry();
            Entry generatedConfigEntry = generatedConfigEntries.get( i ).getEntry();

            // Comparing DNs
            assertTrue( originalConfigEntry.getDn().getNormName().equals( generatedConfigEntry.getDn().getNormName() ) );
        }

        // Destroying the config partition
        configPartition.destroy();
    }
}
