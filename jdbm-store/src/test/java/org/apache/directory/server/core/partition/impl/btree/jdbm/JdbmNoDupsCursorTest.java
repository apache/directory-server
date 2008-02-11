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
import org.apache.directory.server.core.partition.impl.btree.Table;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;

import javax.naming.NamingException;


/**
 * Tests the Cursor functionality of a JdbmTable when duplicate keys are not
 * supported.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JdbmNoDupsCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmNoDupsCursorTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";

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
        table = new JdbmTable<Integer,Integer>( "test", recman, new SerializableComparator<Integer>( "" ), null, null );
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


    @Test( expected=InvalidCursorPositionException.class )
    public void testOnEmptyTable() throws Exception
    {
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        assertNotNull( cursor );
        
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        assertTrue( cursor.isElementReused() );
        cursor.after( new Tuple<Integer,Integer>(7,7) );
        cursor.get();
    }


    @Test
    public void testOnTableWithSingleEntry() throws Exception
    {
        table.put( 1, 1 );
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        assertTrue( cursor.first() );
    
        Tuple<Integer,Integer> tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 1 ) );
        assertTrue( tuple.getValue().equals( 1 ) );
    
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
    
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        
        cursor.after( new Tuple<Integer,Integer>( 2,2 ) );
        assertTrue( cursor.next() );
    
        Tuple<Integer,Integer> tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 3 ) );
        assertTrue( tuple.getValue().equals( 3 ) );
    
        cursor.before( new Tuple<Integer,Integer>(7,7) );
        cursor.next();
        tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 7 ) );
        assertTrue( tuple.getValue().equals( 7 ) );
    
        cursor.last();
        cursor.next();
        tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 9 ) );
        assertTrue( tuple.getValue().equals( 9 ) );
    
        cursor.beforeFirst();
        cursor.next();
        tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 1 ) );
        assertTrue( tuple.getValue().equals( 1 ) );
    
        cursor.afterLast();
        assertFalse( cursor.next() );

        cursor.beforeFirst();
        assertFalse( cursor.previous() );
        
        // just to clear the jdbmTuple value so that line 127 inside after(tuple) method
        // can be executed as part of the below after(tuple) call
        cursor.before(new Tuple<Integer,Integer>( 1,1 )); 
        cursor.after( new Tuple<Integer,Integer>( 0,0 ) );
        
        cursor.next();
        tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 1 ) );
        assertTrue( tuple.getValue().equals( 1 ) );
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
