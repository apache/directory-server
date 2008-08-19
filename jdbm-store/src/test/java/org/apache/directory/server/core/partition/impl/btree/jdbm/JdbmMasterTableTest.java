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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.directory.server.schema.bootstrap.*;
import org.apache.directory.server.schema.registries.*;
import org.apache.directory.server.schema.SerializableComparator;

import java.io.File;
import java.util.Set;
import java.util.HashSet;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;


/**
 * Test cases for JdbmMasterTable.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class JdbmMasterTableTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmMasterTableTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    transient JdbmMasterTable<Integer> table;
    transient File dbFile;
    transient RecordManager recman;
    transient Registries registries = null;
    transient AttributeTypeRegistry attributeRegistry;


    public JdbmMasterTableTest() throws Exception
    {
        // setup the standard registries
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        OidRegistry oidRegistry = new DefaultOidRegistry();
        registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );
        SerializableComparator.setRegistry( registries.getComparatorRegistry() );

        // load essential bootstrap schemas
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
        attributeRegistry = registries.getAttributeTypeRegistry();
    }


    @Before
    public void createTable() throws Exception
    {
        destryTable();
        File tmpDir = null;
        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        table = new JdbmMasterTable<Integer>( recman, registries );
        LOG.debug( "Created new table and populated it with data" );

        JdbmMasterTable<Integer> t2 = new JdbmMasterTable<Integer>( recman, registries );
        t2.close();
    }


    @After
    public void destryTable() throws Exception
    {
        if ( table != null )
        {
            table.close();
        }

        table = null;

        if ( recman != null )
        {
            recman.close();
        }

        recman = null;

        if ( dbFile != null )
        {
            String fileToDelete = dbFile.getAbsolutePath();
            new File( fileToDelete + ".db" ).delete();
            new File( fileToDelete + ".lg" ).delete();

            dbFile.delete();
        }

        dbFile = null;
    }


    @Test
    public void testAll() throws Exception
    {
        assertNull( table.get( 0L ) );
        assertEquals( 0, table.count() );

        assertEquals( 0, ( long ) table.getCurrentId() );
        assertEquals( 1, ( long ) table.getNextId() );
        assertEquals( 1, ( long ) table.getCurrentId() );
        assertEquals( 0, table.count() );

        assertEquals( 1, ( long ) table.getCurrentId() );
        assertEquals( 2, ( long ) table.getNextId() );
        assertEquals( 2, ( long ) table.getCurrentId() );

        assertNull( table.getProperty( "foo" ) );
        table.setProperty( "foo", "bar" );
        assertEquals( "bar", table.getProperty( "foo" ) );
    }
}
