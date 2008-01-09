/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.directory.server.core.partition.impl.btree.Table;
import org.apache.directory.server.core.partition.impl.btree.TupleComparator;
import org.apache.directory.server.core.partition.impl.btree.DefaultTupleComparator;
import org.apache.directory.server.core.partition.impl.btree.TupleRenderer;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;

import jdbm.RecordManager;
import jdbm.helper.StringComparator;
import jdbm.recman.BaseRecordManager;

import javax.naming.NamingException;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JdbmTableWithDuplicatesTest
{
    private static final Logger LOG = LoggerFactory.getLogger( KeyCursorTest.class.getSimpleName() );
    private static final String EMPTY_STRING = "";
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    transient Table<String,String> table;
    transient File dbFile;
    transient RecordManager recman;

    @Before
    public void createTable() throws Exception
    {
        File tmpDir = null;
        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( "test", "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        // gosh this is a terrible use of a global static variable
        SerializableComparator.setRegistry( new TestComparatorRegistry() );

        TupleComparator<String,String> comparator = 
                new DefaultTupleComparator<String,String>(
                        new SerializableComparator<String>( "" ),
                        new SerializableComparator<String>( "" ) );
        table = new JdbmTable<String,String>( "test", true, 100, recman, comparator, null, null );
        LOG.debug( "Created new table and populated it with data" );
    }


    @After
    public void destryTable() throws Exception
    {
        table.close();
        table = null;
        recman.close();
        recman = null;
        dbFile.deleteOnExit();
        dbFile = null;
    }


    public void loadDataNoDupKeys() throws IOException
    {
        // add some data to it
        table.put( "0", EMPTY_STRING );
        table.put( "1", EMPTY_STRING );
        table.put( "2", EMPTY_STRING );
        table.put( "3", EMPTY_STRING );
        table.put( "4", EMPTY_STRING );
        table.put( "5", EMPTY_STRING );
        table.put( "6", EMPTY_STRING );
        table.put( "7", EMPTY_STRING );
        table.put( "8", EMPTY_STRING );
        table.put( "9", EMPTY_STRING );
    }

    
    public void loadDataWithDupKeys() throws IOException
    {
        // add some data to it
        table.put( "0", "0" );
        table.put( "1", "0" );
        table.put( "1", "1" );
        table.put( "1", "2" );
        table.put( "4", "4" );
        table.put( "5", "5" );
        table.put( "6", "6" );
        table.put( "7", "7" );
        table.put( "8", "8" );
        table.put( "9", "9" );
    }

    
    @Test
    public void testCloseReopen() throws Exception
    {
        table.put( "key", "value" );
        table.close();
        TupleComparator<String,String> comparator = 
            new DefaultTupleComparator<String,String>(
                    new SerializableComparator<String>( "" ),
                    new SerializableComparator<String>( "" ) );
        table = new JdbmTable<String,String>( "test", true, 100, recman, comparator, null, null );
        assertEquals( "value", table.get( "key" ) );
    }

    
    @Test 
    public void testConfigMethods() throws Exception
    {
        assertTrue( table.isDupsEnabled() );
        assertNull( table.getRenderer() );
        table.setRenderer( new TupleRenderer() {
            public String getKeyString( Object key )
            {
                return null;
            }
            public String getValueString( Object value )
            {
                return null;
            }} 
        );
        assertNotNull( table.getRenderer() );
        table.setRenderer( null );
        assertNull( table.getRenderer() );
        assertEquals( "test", table.getName() );
        assertNotNull( table.getComparator() );
    }

    
    @Test
    public void testWhenEmpty() throws Exception
    {
        // Test the count methods
        assertEquals( 0, table.count() );
        assertEquals( 0, table.count( "1" ) );

        // Test get method
        assertNull( table.get( "0" ) );
        
        // Test remove methods
        assertNull( table.remove( "1" ) );
        
        // Test has operations
        assertFalse( table.has( "1" ) );
        assertFalse( table.has( "1", "0" ) );
        assertFalse( table.has( "1", true ) );
        assertFalse( table.has( "1", false ) );
        assertFalse( table.has( "1", "0", true ) );
        assertFalse( table.has( "1", "0", false ) );
    }


    private class TestComparatorRegistry implements ComparatorRegistry
    {
        private StringComparator comparator = new StringComparator();

        public String getSchemaName( String oid ) throws NamingException
        {
            return null;
        }


        public void register( ComparatorDescription description, Comparator comparator ) throws NamingException
        {
        }


        public Comparator lookup( String oid ) throws NamingException
        {
            return comparator;
        }


        public boolean hasComparator( String oid )
        {
            return true;
        }


        public Iterator<String> oidIterator()
        {
            return null;
        }


        public Iterator<ComparatorDescription> comparatorDescriptionIterator()
        {
            return null;
        }


        public void unregister( String oid ) throws NamingException
        {
        }


        public void unregisterSchemaElements( String schemaName )
        {
        }


        public void renameSchema( String originalSchemaName, String newSchemaName )
        {
        }
    }
}
