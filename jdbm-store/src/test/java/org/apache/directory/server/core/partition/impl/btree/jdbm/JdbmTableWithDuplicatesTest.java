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
    private static final int DUP_LIMIT = 15; // point at which the JDBM table starts using btrees
    
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

        dbFile = File.createTempFile( "JdbmTableWithDuplicatesTest", "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        // gosh this is a terrible use of a global static variable
        SerializableComparator.setRegistry( new MockComparatorRegistry() );

        TupleComparator<String,String> comparator = 
                new DefaultTupleComparator<String,String>(
                        new SerializableComparator<String>( "" ),
                        new SerializableComparator<String>( "" ) );
        table = new JdbmTable<String,String>( "test", true, DUP_LIMIT, recman, comparator, null, null );
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


    @Test
    public void testCloseReopen() throws Exception
    {
        table.put( "key", "value" );
        table.close();
        TupleComparator<String,String> comparator = 
            new DefaultTupleComparator<String,String>(
                    new SerializableComparator<String>( "" ),
                    new SerializableComparator<String>( "" ) );
        table = new JdbmTable<String,String>( "test", true, DUP_LIMIT, recman, comparator, null, null );
        Object storedValue = table.get( "key" );
        assertEquals( "value", storedValue );
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

    
    @Test
    public void testLoadData() throws Exception
    {
        // add some data to it
        table.put( "0", "zero" );
        table.put( "1", "one" );
        table.put( "2", "two" );
        table.put( "3", "three" );
        table.put( "4", "four" );
        table.put( "5", "five" );
        table.put( "6", "six" );
        table.put( "7", "seven" );
        table.put( "8", "eight" );
        table.put( "9", "nine" );

        assertEquals( 10, table.count() );
        assertEquals( 1, table.count( "0" ) );
        
        /*
         * If counts are exact then we can test for exact values.  Again this 
         * is not a critical function but one used for optimization so worst 
         * case guesses are allowed.
         */
        
        if ( table.isCountExact() )
        {
            assertEquals( 5, table.lessThanCount( "5" ) );
            assertEquals( 4, table.greaterThanCount( "5" ) );
        }
        else
        {
            assertEquals( 10, table.lessThanCount( "5" ) );
            assertEquals( 10, table.greaterThanCount( "5" ) );
        }
    }
    

    @Test
    public void testDuplicateLimit() throws Exception
    {
        for ( int ii = 0; ii < DUP_LIMIT-1; ii++ )
        {
            table.put( "key", String.valueOf( ii ) );
        }
        assertEquals( DUP_LIMIT-1, table.count() );
        
        table.put( "key", String.valueOf( DUP_LIMIT-1 ) );
        assertEquals( DUP_LIMIT, table.count() );
        
        // this switches to B+Trees in JDBM implementations
        table.put( "key", String.valueOf( DUP_LIMIT ) );
        assertEquals( DUP_LIMIT+1, table.count() );
        
        table.put( "key", String.valueOf( DUP_LIMIT+1 ) );
        assertEquals( DUP_LIMIT+2, table.count() );
        
        
        // now start removing and see what happens 

        table.remove( "key", String.valueOf( DUP_LIMIT+1 ) );
        assertFalse( table.has( "key", String.valueOf( DUP_LIMIT+1 ) ) );
        assertEquals( DUP_LIMIT+1, table.count() );
    
        table.remove( "key", String.valueOf( DUP_LIMIT ) );
        assertFalse( table.has( "key", String.valueOf( DUP_LIMIT ) ) );
        assertEquals( DUP_LIMIT, table.count() );
    
        for ( int ii = DUP_LIMIT-1; ii >= 0; ii-- )
        {
            table.remove( "key", String.valueOf( ii ) );
        }
        assertEquals( 0, table.count() );
    }
    
    
    /**
     * Let's test keys with a null or lack of any values.
     */
    @Test
    public void testNullOrEmptyValueAfterDuplicateLimit() throws Exception
    {
        testDuplicateLimit();
        assertEquals( 0, table.count() );
        table.put( "key", null );
        assertEquals( 1, table.count() );
        assertEquals( null, table.get( "key" ) );
        
        // Let's add the key with two valid values and remove all values
        table.remove( "key" );
        table.put( "key", "1" );
        table.put( "key", "2" );
        assertEquals( 2, table.count( "key" ) );
        table.remove( "key", "1" );
        assertEquals( 1, table.count( "key" ) );
        assertEquals( "2", table.get( "key" ) );

        
        table.remove( "key", "2" );
        String remainingValue = table.get( "key" );
        assertNull( remainingValue );
        assertEquals( 0, table.count( "key" ) );
        assertTrue( table.has( "key" ) );
        
        table.remove( "key", "1" );
        remainingValue = table.get( "key" );
        assertNull( remainingValue );
        assertEquals( 0, table.count( "key" ) );
        assertTrue( table.has( "key" ) );
    }
    
    
    /**
     * Let's test keys with a null or lack of any values.
     */
    @Test
    public void testNullOrEmptyValue() throws Exception
    {
        assertEquals( 0, table.count() );
        table.put( "key", null );
        assertEquals( 1, table.count() );
        assertEquals( null, table.get( "key" ) );
        
        // Let's add the key with two valid values and remove all values
        table.remove( "key" );
        table.put( "key", "1" );
        table.put( "key", "2" );
        assertEquals( 2, table.count( "key" ) );
        table.remove( "key", "1" );
        assertEquals( 1, table.count( "key" ) );
        assertEquals( "2", table.get( "key" ) );

        
        table.remove( "key", "2" );
        String remainingValue = table.get( "key" );
        assertNull( remainingValue );
        assertEquals( 0, table.count( "key" ) );
        assertTrue( table.has( "key" ) );
        
        table.remove( "key", "1" );
        remainingValue = table.get( "key" );
        assertNull( remainingValue );
        assertEquals( 0, table.count( "key" ) );
        assertTrue( table.has( "key" ) );
    }
    
    
    private class MockComparatorRegistry implements ComparatorRegistry
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
