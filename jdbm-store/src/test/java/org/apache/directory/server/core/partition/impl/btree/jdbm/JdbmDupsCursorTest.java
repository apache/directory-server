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
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;

import jdbm.RecordManager;
import jdbm.helper.IntegerSerializer;
import jdbm.recman.BaseRecordManager;

import javax.naming.NamingException;


/**
 * Tests the Cursor functionality of a JdbmTable when duplicate keys are 
 * supported.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JdbmDupsCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmDupsCursorTest.class.getSimpleName() );
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

        table = new JdbmTable<Integer,Integer>( "test", SIZE, recman,
                new SerializableComparator<Integer>( "" ),
                new SerializableComparator<Integer>( "" ),
                null, new IntegerSerializer() );
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
    public void testOnEmptyTable() throws Exception
    {
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<Integer, Integer>( 1, 2 ) );
        assertFalse( cursor.available() );
    }

    
    @Test
    public void testOverDupLimit() throws Exception
    {
        table.put( 5, 5 );
        table.put( 6, 6 );
        for ( int ii = 0; ii < 20; ii++ )
        {
            table.put( 7, ii );
        }
        table.put( 8, 8 );
        table.put( 9, 9 );
        
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<Integer, Integer>( 7, 2 ) );
        assertFalse( cursor.available() );
    }

    
    @Test
    public void testUnderDupLimit() throws Exception
    {
        table.put( 5, 5 );
        table.put( 6, 6 );
        for ( int ii = 0; ii < 10; ii++ )
        {
            table.put( 7, ii );
        }
        table.put( 8, 8 );
        table.put( 9, 9 );
        
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<Integer, Integer>( 7, 2 ) );
        assertFalse( cursor.available() );
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
