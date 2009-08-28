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
import org.apache.directory.server.xdbm.Tuple;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.comparators.SerializableComparator;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.registries.ComparatorRegistry;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Iterator;

import jdbm.RecordManager;
import jdbm.helper.IntegerSerializer;
import jdbm.recman.BaseRecordManager;

import javax.naming.NamingException;


/**
 * Tests the DupsContainerCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DupsContainerCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( NoDupsCursorTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    transient JdbmTable<Integer,Integer> table;
    transient File dbFile;
    transient RecordManager recman;
    private static final int SIZE = 15;


    @Before
    public void createTable() throws Exception
    {
        File tmpDir = null;
        
        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
            tmpDir.deleteOnExit();
        }

        dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        dbFile.deleteOnExit();
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        // gosh this is a terrible use of a global static variable
        SerializableComparator.setRegistry( new MockComparatorRegistry() );

        table = new JdbmTable<Integer,Integer>( "test", SIZE, recman,
                new SerializableComparator<Integer>( "" ),
                new SerializableComparator<Integer>( "" ),
                null, new IntegerSerializer() );
        LOG.debug( "Created new table and populated it with data" );

    }


    @After
    public void destroyTable() throws Exception
    {
        table.close();
        table = null;
        recman.close();
        recman = null;
        String fileToDelete = dbFile.getAbsolutePath();
        new File( fileToDelete ).delete();
        new File( fileToDelete + ".db" ).delete();
        new File( fileToDelete + ".lg" ).delete();
        dbFile = null;
    }


    @Test( expected=IllegalStateException.class )
    public void testUsingNoDuplicates() throws Exception
    {
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        // gosh this is a terrible use of a global static variable
        SerializableComparator.setRegistry( new MockComparatorRegistry() );
        table = new JdbmTable<Integer,Integer>( "test", recman, new SerializableComparator<Integer>( "" ), null, null );

        Cursor<Tuple<Integer,DupsContainer<Integer>>> cursor =
            new DupsContainerCursor<Integer,Integer>( table );
        assertNotNull( cursor );
    }


    @Test( expected=InvalidCursorPositionException.class )
    public void testEmptyTable() throws Exception
    {
        Cursor<Tuple<Integer,DupsContainer<Integer>>> cursor =
            new DupsContainerCursor<Integer,Integer>( table );
        assertNotNull( cursor );

        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        assertTrue( cursor.isElementReused() );

        cursor = new DupsContainerCursor<Integer,Integer>( table );
        assertFalse( cursor.previous() );

        cursor = new DupsContainerCursor<Integer,Integer>( table );
        assertFalse( cursor.next() );

        cursor.after( new Tuple<Integer,DupsContainer<Integer>>( 7, null ) );
        cursor.get();
    }


    @Test
    public void testOnTableWithSingleEntry() throws Exception
    {
        table.put( 1, 1 );
        Cursor<Tuple<Integer,DupsContainer<Integer>>> cursor =
            new DupsContainerCursor<Integer,Integer>( table );
        assertTrue( cursor.first() );

        Tuple<Integer,DupsContainer<Integer>> tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 1 ) );
        assertEquals( 1, ( int ) tuple.getValue().getArrayTree().getFirst() );

        cursor.beforeFirst();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
    }


    @Test
    public void testOnTableWithMultipleEntries() throws Exception
    {
        for( int i=1; i < 10; i++ )
        {
            table.put( i, i );
        }

        Cursor<Tuple<Integer,DupsContainer<Integer>>> cursor =
            new DupsContainerCursor<Integer,Integer>( table );

        cursor.after( new Tuple<Integer,DupsContainer<Integer>>( 2, null ) );
        assertTrue( cursor.next() );

        Tuple<Integer,DupsContainer<Integer>> tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 3 ) );
        assertEquals( 3, ( int ) tuple.getValue().getArrayTree().getFirst() );

        cursor.before( new Tuple<Integer,DupsContainer<Integer>>( 7, null ) );
        cursor.next();
        tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 7 ) );
        assertEquals( 7, ( int ) tuple.getValue().getArrayTree().getFirst() );

        cursor.last();
        cursor.next();
        assertFalse( cursor.available() );
       /* tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 9 ) );
        assertEquals( 9, ( int ) tuple.getValue().getAvlTree().getFirst().getKey() ); */
        
        cursor.beforeFirst();
        cursor.next();
        tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 1 ) );
        assertEquals( 1, ( int ) tuple.getValue().getArrayTree().getFirst() );

        cursor.afterLast();
        assertFalse( cursor.next() );

        cursor.beforeFirst();
        assertFalse( cursor.previous() );

        // just to clear the jdbmTuple value so that line 127 inside after(tuple) method
        // can be executed as part of the below after(tuple) call
        cursor.before(new Tuple<Integer,DupsContainer<Integer>>( 1, null ) );
        cursor.after( new Tuple<Integer,DupsContainer<Integer>>( 0, null ) ); // this positions on tuple with key 1

        cursor.next(); // this moves onto tuple with key 2
        tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 2 ) );
        assertEquals( 2, ( int ) tuple.getValue().getArrayTree().getFirst() );
    }


    @Test
    public void testMiscellaneous() throws Exception
    {
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


        public void register(LdapComparator<?> comparator ) throws NamingException
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