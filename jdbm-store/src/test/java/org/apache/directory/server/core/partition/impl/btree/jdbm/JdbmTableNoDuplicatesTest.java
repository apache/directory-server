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
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JdbmTableNoDuplicatesTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmTableNoDuplicatesTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    transient Table<Integer,Integer> table;
    transient File dbFile;
    transient RecordManager recman;


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

        // gosh this is a terrible use of a global static variable
        SerializableComparator.setRegistry( new MockComparatorRegistry() );
        table = new JdbmTable<Integer,Integer>( "test", recman, new SerializableComparator<Integer>( "" ), null, null );
        LOG.debug( "Created new table and populated it with data" );
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
            dbFile.delete();
        }
        
        dbFile = null;
    }
    

    @Test
    public void testCloseReopen() throws Exception
    {
        table.put( 1, 2 );
        table.close();
        table = new JdbmTable<Integer,Integer>( "test", recman, new SerializableComparator<Integer>( "" ), null, null );
        assertTrue( 2 == table.get( 1 ) );
    }

    
    @Test 
    public void testConfigMethods() throws Exception
    {
        assertFalse( table.isDupsEnabled() );
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
        assertNotNull( table.getKeyComparator() );
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
        assertFalse( table.hasGreaterOrEqual( 1 ) );
        assertFalse( table.hasLessOrEqual( 1 ) );

        try
        {
            assertFalse( table.hasGreaterOrEqual( 1, 0 ) );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }

        try
        {
            assertFalse( table.hasLessOrEqual( 1, 0 ) );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }
    }

    
    @Test
    public void testLoadData() throws Exception
    {
        // add some data to it
        for ( int ii = 0; ii < 10; ii++ )
        {
            table.put( ii, ii );
        }
        
        assertEquals( 10, table.count() );
        assertEquals( 1, table.count( 0 ) );
        
        /*
         * If counts are exact then we can test for exact values.  Again this 
         * is not a critical function but one used for optimization so worst 
         * case guesses are allowed.
         */
        
        if ( table.isCountExact() )
        {
            assertEquals( 5, table.lessThanCount( 5 ) );
            assertEquals( 4, table.greaterThanCount( 5 ) );
        }
        else
        {
            assertEquals( 10, table.lessThanCount( 5 ) );
            assertEquals( 10, table.greaterThanCount( 5 ) );
        }
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
        
        // Let's add the key with a valid value and remove just the value
        assertEquals( 0, table.count( 1 ) );
        table.remove( 1 );
        assertEquals( 0, table.count( 1 ) );
        table.put( 1, 1 );
        assertEquals( 1, table.count( 1 ) );
        table.remove( 1, 1 );
        assertEquals( 0, table.count( 1 ) );
        assertNull( table.get( 1 ) );
        assertFalse( table.has( 1 ) );
    }
    

    @Test
    public void testPut() throws Exception
    {

    }
    

    @Test
    public void testHas() throws Exception
    {
        assertFalse( table.has( 1 ) );
        final int SIZE = 15;
        
        for ( int ii = 0; ii < SIZE; ii++ )
        {
            table.put( ii, ii );
        }
        assertEquals( SIZE, table.count() );

        assertFalse( table.has( -1 ) );
        assertTrue( table.hasGreaterOrEqual( -1 ) );
        assertFalse( table.hasLessOrEqual( -1 ) );
        
        assertTrue( table.has( 0 ) );
        assertTrue( table.hasGreaterOrEqual( 0 ) );
        assertTrue( table.hasLessOrEqual( 0 ) );
        
        assertTrue( table.has( SIZE - 1 ) );
        assertTrue( table.hasGreaterOrEqual( SIZE - 1 ) );
        assertTrue( table.hasLessOrEqual( SIZE - 1 ) );
        
        assertFalse( table.has( SIZE ) );
        assertFalse( table.hasGreaterOrEqual( SIZE ) );
        assertTrue( table.hasLessOrEqual( SIZE ) );
        table.remove( 10 );
        table.remove( 11 );
        assertTrue( table.hasLessOrEqual( 11 ) );
        
        try
        {
            assertFalse( table.hasGreaterOrEqual( 1, 1 ) );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }

        try
        {
            assertFalse( table.hasLessOrEqual( 1, 1 ) );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }

        try
        {
            assertTrue( table.hasLessOrEqual( 1, 2 ) );
            fail( "Should never get here since no dups tables " +
            		"freak when they cannot find a value comparator" );
        } 
        catch ( UnsupportedOperationException e )
        {
            assertNotNull( e );
        }
    }
    
    
    private class MockComparatorRegistry implements ComparatorRegistry
    {
        private Comparator comparator = new Comparator<Integer>()
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
