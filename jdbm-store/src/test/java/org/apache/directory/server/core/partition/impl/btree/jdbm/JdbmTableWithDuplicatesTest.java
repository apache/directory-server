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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.directory.server.xdbm.Table;
import org.apache.directory.server.xdbm.Tuple;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.registries.ComparatorRegistry;

import java.io.File;
import java.util.Iterator;

import jdbm.RecordManager;
import jdbm.helper.IntegerSerializer;
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

        table = new JdbmTable<Integer,Integer>( "test", SIZE, recman,
                new SerializableComparator<Integer>( "" ),
                new SerializableComparator<Integer>( "" ),
                new IntegerSerializer(), new IntegerSerializer() );
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
            String fileToDelete = dbFile.getAbsolutePath();
            new File( fileToDelete + ".db" ).delete();
            new File( fileToDelete + ".lg" ).delete();

            dbFile.delete();
        }

        dbFile = null;
    }


    @Test
    public void testSerializers() throws Exception
    {
        assertNotNull( ( ( JdbmTable ) table ).getKeySerializer() );
        assertNotNull( ( ( JdbmTable ) table ).getValueSerializer() );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        assertEquals( 0, table.count( 3 ) );
        assertEquals( 0, table.count( null ) );
    }


    @Test( expected = NullPointerException.class )
    public void testNullKeyComparator() throws Exception
    {
        assertNotNull( ( ( JdbmTable ) table ).getKeyComparator() );
        new JdbmTable<Integer,Integer>( "test", SIZE, recman,
            null,
            new SerializableComparator<Integer>( "" ),
            null, new IntegerSerializer() );
    }


    @Test( expected = NullPointerException.class )
    public void testNullValueComparator() throws Exception
    {
        assertNotNull( ( ( JdbmTable ) table ).getValueComparator() );
        new JdbmTable<Integer,Integer>( "test", SIZE, recman,
            new SerializableComparator<Integer>( "" ),
            null,
            null, new IntegerSerializer() );
    }


    @Test
    public void testCloseReopen() throws Exception
    {
        table.put( 1, 2 );
        assertTrue( 2 == table.get( 1 ) );
        table.close();
        table = new JdbmTable<Integer,Integer>( "test", SIZE, recman,
                new SerializableComparator<Integer>( "" ),
                new SerializableComparator<Integer>( "" ),
                new IntegerSerializer(), new IntegerSerializer() );
        assertTrue( 2 == table.get( 1 ) );
    }

    
    @Test 
    public void testConfigMethods() throws Exception
    {
        assertTrue( table.isDupsEnabled() );
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
        assertNull( table.get( null ) );

        // Test remove methods
        table.remove( 1 );
        assertFalse( table.has( 1 ) );
        
        // Test has operations
        assertFalse( table.has( 1 ) );
        assertFalse( table.has( 1, 0 ) );
        assertFalse( table.hasGreaterOrEqual( 1 ) );
        assertFalse( table.hasLessOrEqual( 1 ) );
        assertFalse( table.hasGreaterOrEqual( 1, 0 ) );
        assertFalse( table.hasLessOrEqual( 1, 0 ) );
    }


    @Test
    public void testPut() throws Exception
    {
        final int SIZE = 15;

        for ( int ii = 0; ii < SIZE; ii++ )
        {
            table.put( ii, ii );
        }
        assertEquals( SIZE, table.count() );
        table.put( 0, 0 );
        assertTrue( table.has( 0, 0 ) );

        // add some duplicates
        for ( int ii = 0; ii < SIZE*2; ii++ )
        {
            table.put( SIZE*2, ii );
        }
        assertEquals( SIZE*3, table.count() );
        
        table.put( 0, 0 );
        assertTrue( table.has( 0, 0 ) );
        
        table.put( SIZE*2, 0 );
        assertTrue( table.has( SIZE*2, 0 ) );
    }


    @Test
    public void testHas() throws Exception
    {
        assertFalse( table.has( 1 ) );
        
        for ( int ii = 0; ii < SIZE*2; ii++ )
        {
            table.put( 1, ii );
        }
        assertEquals( SIZE*2, table.count() );

        assertTrue( table.has( 1 ) );
        assertTrue( table.has( 1, 0 ) );
        assertFalse( table.has( 1, SIZE*2 ) );

        assertTrue( table.hasGreaterOrEqual( 1, 0 ) );
        assertTrue( table.hasLessOrEqual( 1, 0 ) );
        assertFalse( table.hasLessOrEqual( 1, -1 ) );

        assertTrue( table.hasGreaterOrEqual( 1, SIZE*2 - 1 ) );
        assertTrue( table.hasLessOrEqual( 1, SIZE*2 - 1 ) );
        assertTrue( table.hasGreaterOrEqual( 1, SIZE*2 - 1 ) );
        assertTrue( table.hasLessOrEqual( 1, SIZE*2 ) );
        assertFalse( table.hasGreaterOrEqual( 1, SIZE*2 ) );
        assertFalse( table.has( 1, SIZE*2 ) );

        // let's go over the this limit now and ask the same questions
        table.put( 1, SIZE*2 );

        assertTrue( table.has( 1 ) );
        assertTrue( table.has( 1, 0 ) );
        assertTrue( table.has( 1, SIZE*2 ) );
        assertFalse( table.has( null, null ) );

        assertTrue( table.hasGreaterOrEqual( 1, 0 ) );
        assertTrue( table.hasLessOrEqual( 1, 0 ) );
        assertFalse( table.hasLessOrEqual( 1, -1 ) );
        assertFalse( table.hasGreaterOrEqual( null, null ) );
        assertFalse( table.hasLessOrEqual( null, null ) );

        assertTrue( table.hasGreaterOrEqual( 1, SIZE*2 ) );
        assertTrue( table.hasLessOrEqual( 1, SIZE*2 ) );
        assertTrue( table.hasGreaterOrEqual( 1, SIZE*2 ) );
        assertTrue( table.hasLessOrEqual( 1, SIZE*2 + 1 ) );
        assertFalse( table.hasGreaterOrEqual( 1, SIZE*2 + 1 ) );
        assertFalse( table.has( 1, SIZE*2 + 1 ) );
        
        // now do not add duplicates and check has( key, boolean )
        for ( int ii = 0; ii < SIZE; ii++ )
        {
            // note we are not adding duplicates not put( 1, ii )
            table.put( ii, ii );
        }
        
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

        for ( int ii = 0; ii < SIZE; ii++ )
        {
            if ( ii == 1 ) // don't delete the node which had multiple values
            {
                continue;
            }
            table.remove( ii, ii );
        }
        
        // delete all values of the duplicate key one by one
        for ( int ii = 0; ii < SIZE * 2 + 1; ii++ )
        {
            table.remove( 1, ii );
        }

        Cursor<Tuple<Integer, Integer>> cursor = table.cursor();
        //System.out.println( "remaining ..." );
        cursor.beforeFirst();
        while ( cursor.next() )
        {
            //System.out.println( cursor.get() );
        }

        assertFalse( table.hasLessOrEqual( 1 ) );
        assertFalse( table.hasLessOrEqual( 1, 10 ) );
        assertFalse( table.hasGreaterOrEqual( 1 ) );
        assertFalse( table.hasGreaterOrEqual( 1, 0 ) );

        table.put( 1, 0 );

    }

    
    @Test
    public void testRemove() throws Exception
    {
        assertEquals( 0, table.count() );

        table.put( 1, 1 );
        table.put( 1, 2 );
        assertEquals( 2, table.count() );
        table.remove( 1 );
        assertFalse( table.has( 1 ) );
        assertEquals( 0, table.count() );

        table.put( 10, 10 );
        assertEquals( 1, table.count() );
        table.remove( 10, 11 );
        assertFalse( table.has( 10, 11 ) );
        assertEquals( 1, table.count() );
        table.remove( 10, 10 );
        assertFalse( table.has( 10, 10 ) );
        assertEquals( 0, table.count() );

        // add duplicates
        for ( int ii = 0; ii < SIZE*2; ii++ )
        {
            table.put( 0, ii );
        }

        assertEquals( SIZE*2, table.count() );
        table.remove( 0, 100 );
        assertFalse( table.has( 0, 100 ) );
        assertEquals( SIZE*2, table.count() );
        
        table.remove( 0 );
        assertNull( table.get( 0 ) );
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
        for ( int ii = 0; ii < SIZE; ii++ )
        {
            table.put( 1, ii );
        }
        assertEquals( SIZE, table.count() );
        assertEquals( SIZE, table.count( 1 ) );

        // this switches to B+Trees from AvlTree
        table.put( 1, SIZE );
        assertEquals( SIZE + 1, table.count() );
        assertEquals( SIZE + 1, table.count( 1 ) );

        // go one more over still a B+Tree
        table.put( 1, SIZE + 1 );
        assertEquals( SIZE + 2, table.count() );
        assertEquals( SIZE + 2, table.count( 1 ) );
        assertEquals( 0, ( int ) table.get( 1 ) );
        
        // now start removing and see what happens 
        table.remove( 1, SIZE + 1 );
        assertFalse( table.has( 1, SIZE + 1 ) );
        assertTrue( table.has( 1, SIZE ) );
        assertEquals( SIZE + 1, table.count() );
        assertEquals( SIZE + 1, table.count( 1 ) );

        // this switches to AvlTree from B+Trees
        table.remove( 1, SIZE );
        assertFalse( table.has( 1, SIZE ) );
        assertEquals( SIZE, table.count() );
        assertEquals( SIZE, table.count( 1 ) );
        assertTrue( 0 == table.get( 1 ) );
    
        for ( int ii = SIZE - 1; ii >= 0; ii-- )
        {
            table.remove( 1, ii );
        }
        assertEquals( 0, table.count() );

        for ( int ii = 0; ii < SIZE - 1; ii++ )
        {
            table.put( 1, ii );
        }
        assertEquals( SIZE - 1, table.count() );
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


    @Test
    public void testMiscellaneous() throws Exception
    {
        assertNotNull( ( ( JdbmTable ) table ).getMarshaller() );
        ( ( JdbmTable ) table ).close();

        // test value btree creation without serializer
        table = new JdbmTable<Integer,Integer>( "test", SIZE, recman,
                new SerializableComparator<Integer>( "" ),
                new SerializableComparator<Integer>( "" ),
                new IntegerSerializer(), null );
        assertNull( ( ( JdbmTable ) table ).getValueSerializer() );
        for ( int ii = 0; ii < SIZE + 1; ii++ )
        {
            table.put( 0, ii );
        }
        table.remove( 0 );
        assertFalse( table.has( 0 ) );
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
        private LdapComparator<Integer> comparator = new LdapComparator<Integer>( "1.1.1" )
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


        public void register( LdapComparatorDescription description, LdapComparator<?> comparator ) throws NamingException
        {
        }


        public LdapComparator<?> lookup( String oid ) throws NamingException
        {
            return comparator;
        }


        public boolean contains( String oid )
        {
            return true;
        }


        public void register(LdapComparator<?> comparator ) throws NamingException
        {
        }


        public Iterator<LdapComparator<?>> iterator()
        {
            return null;
        }


        public Iterator<String> oidsIterator()
        {
            return null;
        }

        
        public Iterator<LdapComparatorDescription> ldapComparatorDescriptionIterator()
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
