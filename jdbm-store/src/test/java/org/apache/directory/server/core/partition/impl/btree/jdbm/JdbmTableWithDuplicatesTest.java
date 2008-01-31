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
import java.util.Comparator;
import java.util.Iterator;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;

import javax.naming.NamingException;


/**
 * Tests JdbmTable operations with duplicates.  Does not test Cursor capabilities.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JdbmTableWithDuplicatesTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmTableWithDuplicatesTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";
    private static final int SIZE = 15;
    
    transient Table<Integer,Integer> table;
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

        dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        // gosh this is a terrible use of a global static variable
        SerializableComparator.setRegistry( new MockComparatorRegistry() );

        TupleComparator<Integer,Integer> comparator = 
                new DefaultTupleComparator<Integer,Integer>(
                        new SerializableComparator<Integer>( "" ),
                        new SerializableComparator<Integer>( "" ) );
        table = new JdbmTable<Integer,Integer>( "test", true, SIZE, recman, comparator, null, null );
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
        table.put( 1, 2 );
        table.close();
        TupleComparator<Integer,Integer> comparator = 
            new DefaultTupleComparator<Integer,Integer>(
                    new SerializableComparator<Integer>( "" ),
                    new SerializableComparator<Integer>( "" ) );
        table = new JdbmTable<Integer,Integer>( "test", true, SIZE, recman, comparator, null, null );
        assertTrue( 2 == table.get( 1 ) );
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
        assertEquals( 0, table.count( 1 ) );

        // Test get method
        assertNull( table.get( 0 ) );
        
        // Test remove methods
        assertNull( table.remove( 1 ) );
        
        // Test has operations
        assertFalse( table.has( 1 ) );
        assertFalse( table.has( 1, 0 ) );
        assertFalse( table.has( 1, true ) );
        assertFalse( table.has( 1, false ) );
        assertFalse( table.has( 1, 0, true ) );
        assertFalse( table.has( 1, 0, false ) );
    }
    
    
    @Test
    public void testHas() throws Exception
    {
        assertFalse( table.has( 1 ) );
        
        for ( int ii = 0; ii < SIZE; ii++ )
        {
            table.put( 1, ii );
        }
        assertEquals( SIZE, table.count() );

        assertTrue( table.has( 1 ) );
        assertTrue( table.has( 1, 0 ) );
        assertFalse( table.has( 1, SIZE ) );

        assertTrue( table.has( 1, 0, true ) );
        assertTrue( table.has( 1, 0, false ) );
        assertFalse( table.has( 1, -1, false ) );

        assertTrue( table.has( 1, SIZE-1, true ) );
        assertTrue( table.has( 1, SIZE-1, false ) );
        assertTrue( table.has( 1, SIZE-1, true ) );
        assertTrue( table.has( 1, SIZE, false ) );
        assertFalse( table.has( 1, SIZE, true ) );
        assertFalse( table.has( 1, SIZE ) );

        // let's go over the this limit now and ask the same questions
        table.put( 1, SIZE );

        assertTrue( table.has( 1 ) );
        assertTrue( table.has( 1, 0 ) );
        assertTrue( table.has( 1, SIZE ) );

        assertTrue( table.has( 1, 0, true ) );
        assertTrue( table.has( 1, 0, false ) );
        assertFalse( table.has( 1, -1, false ) );

        assertTrue( table.has( 1, SIZE, true ) );
        assertTrue( table.has( 1, SIZE, false ) );
        assertTrue( table.has( 1, SIZE, true ) );
        assertTrue( table.has( 1, SIZE+1, false ) );
        assertFalse( table.has( 1, SIZE+1, true ) );
        assertFalse( table.has( 1, SIZE+1 ) );
        
        table.remove( 1 );

        
        
        // now do not add duplicates and check has( key, boolean )
        for ( int ii = 0; ii < SIZE; ii++ )
        {
            // note we are not adding duplicates not put( 1, ii )
            table.put( ii, ii );
        }
        
        assertFalse( table.has( -1 ) );
        assertTrue( table.has( -1 , true ) );
        assertFalse( table.has( -1 , false ) );
        
        assertTrue( table.has( 0 ) );
        assertTrue( table.has( 0 , true ) );
        assertTrue( table.has( 0 , false ) );
        
        assertTrue( table.has( SIZE-1 ) );
        assertTrue( table.has( SIZE-1, true ) );
        assertTrue( table.has( SIZE-1, false ) );
        
        assertFalse( table.has( SIZE ) );
        assertFalse( table.has( SIZE, true ) );
        assertTrue( table.has( SIZE, false ) );
    }

    
    @Test
    public void testRemove() throws Exception
    {
        table.put( 1, 1 );
        table.put( 1, 2 );
        table.remove( 1 );
    }
    
    
    @Test
    public void testLoadData() throws Exception
    {
        // add some data to it
        for ( int ii = 0; ii < SIZE; ii++ )
        {
            table.put( ii, ii );
        }
        
        assertEquals( 15, table.count() );
        assertEquals( 1, table.count( 0 ) );
        
        /*
         * If counts are exact then we can test for exact values.  Again this 
         * is not a critical function but one used for optimization so worst 
         * case guesses are allowed.
         */
        
        if ( table.isCountExact() )
        {
            assertEquals( 5, table.lessThanCount( 5 ) );
            assertEquals( 9, table.greaterThanCount( 5 ) );
        }
        else
        {
            assertEquals( SIZE, table.lessThanCount( 5 ) );
            assertEquals( SIZE, table.greaterThanCount( 5 ) );
        }
    }
    

    @Test
    public void testDuplicateLimit() throws Exception
    {
        for ( int ii = 0; ii < SIZE-1; ii++ )
        {
            table.put( 1, ii );
        }
        assertEquals( SIZE-1, table.count() );
        
        table.put( 1, SIZE-1 );
        assertEquals( SIZE, table.count() );
        
        // this switches to B+Trees in JDBM implementations
        table.put( 1, SIZE );
        assertEquals( SIZE+1, table.count() );
        
        table.put( 1, SIZE+1 );
        assertEquals( SIZE+2, table.count() );
        
        
        // now start removing and see what happens 

        table.remove( 1, SIZE+1 );
        assertFalse( table.has( 1, SIZE+1 ) );
        assertEquals( SIZE+1, table.count() );
    
        table.remove( 1, SIZE );
        assertFalse( table.has( 1, SIZE ) );
        assertEquals( SIZE, table.count() );
        assertEquals( SIZE, table.count( 1 ) );
        assertTrue( 0 == table.get( 1 ) );
    
        for ( int ii = SIZE-1; ii >= 0; ii-- )
        {
            table.remove( 1, ii );
        }
        assertEquals( 0, table.count() );

        for ( int ii = 0; ii < SIZE-1; ii++ )
        {
            table.put( 1, ii );
        }
        assertEquals( SIZE-1, table.count() );
        table.remove( 1 );
        assertEquals( 0, table.count() );
    }
    
    
    /**
     * Let's test keys with a null or lack of any values.
     * @throws Exception on error
     */
    @Test
    public void testNullOrEmptyKeyValueAfterDuplicateLimit() throws Exception
    {
        testDuplicateLimit();
        assertEquals( 0, table.count() );
        
        try
        {
            table.put( 1, null );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
        
        try
        {
            table.put( null, 1 );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
        
        assertEquals( 0, table.count() );
        assertEquals( null, table.get( 1 ) );
        
        // Let's add the key with two valid values and remove all values
        table.remove( 1 );
        table.put( 1, 1 );
        table.put( 1, 2 );
        assertEquals( 2, table.count( 1 ) );
        table.remove( 1, 1 );
        assertEquals( 1, table.count( 1 ) );
        assertTrue( 2 == table.get( 1 ) );

        table.remove( 1, 2 );
        assertNull( table.get( 1 ) );
        assertEquals( 0, table.count( 1 ) );
        assertFalse( table.has( 1 ) );
    }
    
    
    /**
     * Let's test keys with a null or lack of any values.
     * @throws Exception on error
     */
    @Test
    public void testNullOrEmptyKeyValue() throws Exception
    {
        assertEquals( 0, table.count() );
        
        try
        {
            table.put( 1, null );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
        
        try
        {
            table.put( null, 2 );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
        
        assertEquals( 0, table.count() );
        assertEquals( null, table.get( 1 ) );
        
        // Let's add the key with two valid values and remove all values
        table.remove( 1 );
        table.put( 1, 1 );
        table.put( 1, 2 );
        assertEquals( 2, table.count( 1 ) );
        table.remove( 1, 1 );
        assertEquals( 1, table.count( 1 ) );
        assertTrue( 2 == table.get( 1 ) );

        table.remove( 1, 2 );
        assertNull( table.get( 1 ) );
        assertEquals( 0, table.count( 1 ) );
        assertFalse( table.has( 1 ) );
    }
    
    
    private class MockComparatorRegistry implements ComparatorRegistry
    {
        private Comparator<Integer> comparator = new Comparator<Integer>()
        {
            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }
        };

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
