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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import jdbm.RecordManager;
import jdbm.helper.DefaultSerializer;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.comparators.SerializableComparator;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the DupsContainerCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DupsContainerCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( NoDupsCursorTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    transient JdbmTable<String,String> table;
    transient File dbFile;
    private static SchemaManager schemaManager;
    transient RecordManager recman;
    private static final int SIZE = 15;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = DupsContainerCursorTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
    }

    
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
        
        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );
        table = new JdbmTable<String,String>( schemaManager, "test", SIZE, recman,
                comparator, comparator, null, new DefaultSerializer() );
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
        //SerializableComparator.setRegistry( 
        //    new MockComparatorRegistry(
        //        new OidRegistry() ) );
        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );
        table = new JdbmTable<String,String>( schemaManager, "test", recman, comparator, null, null );

        Cursor<Tuple<String,DupsContainer<String>>> cursor =
            new DupsContainerCursor<String,String>( table );
        assertNotNull( cursor );
    }


    @Test( expected=InvalidCursorPositionException.class )
    public void testEmptyTable() throws Exception
    {
        Cursor<Tuple<String,DupsContainer<String>>> cursor =
            new DupsContainerCursor<String,String>( table );
        assertNotNull( cursor );

        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        assertTrue( cursor.isElementReused() );

        cursor = new DupsContainerCursor<String,String>( table );
        assertFalse( cursor.previous() );

        cursor = new DupsContainerCursor<String,String>( table );
        assertFalse( cursor.next() );

        cursor.after( new Tuple<String,DupsContainer<String>>( "7", null ) );
        cursor.get();
    }


    @Test
    public void testOnTableWithSingleEntry() throws Exception
    {
        table.put( "1", "1" );
        Cursor<Tuple<String,DupsContainer<String>>> cursor =
            new DupsContainerCursor<String,String>( table );
        assertTrue( cursor.first() );

        Tuple<String,DupsContainer<String>> tuple = cursor.get();
        assertEquals( "1", tuple.getKey() );
        assertEquals( "1", tuple.getValue().getArrayTree().getFirst() );

        cursor.beforeFirst();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
    }


    @Test
    public void testOnTableWithMultipleEntries() throws Exception
    {
        for( int i=1; i < 10; i++ )
        {
            String istr = Integer.toString( i );
            table.put( istr, istr );
        }

        Cursor<Tuple<String,DupsContainer<String>>> cursor =
            new DupsContainerCursor<String,String>( table );

        cursor.after( new Tuple<String,DupsContainer<String>>( "2", null ) );
        assertTrue( cursor.next() );

        Tuple<String,DupsContainer<String>> tuple = cursor.get();
        assertEquals( "3", tuple.getKey() );
        assertEquals( "3", tuple.getValue().getArrayTree().getFirst() );

        cursor.before( new Tuple<String,DupsContainer<String>>( "7", null ) );
        cursor.next();
        tuple = cursor.get();
        assertEquals( "7", tuple.getKey() );
        assertEquals( "7", tuple.getValue().getArrayTree().getFirst() );

        cursor.last();
        cursor.next();
        assertFalse( cursor.available() );
       /* tuple = cursor.get();
        assertTrue( tuple.getKey().equals( 9 ) );
        assertEquals( 9, ( int ) tuple.getValue().getAvlTree().getFirst().getKey() ); */
        
        cursor.beforeFirst();
        cursor.next();
        tuple = cursor.get();
        assertEquals( "1", tuple.getKey() );
        assertEquals( "1", tuple.getValue().getArrayTree().getFirst() );

        cursor.afterLast();
        assertFalse( cursor.next() );

        cursor.beforeFirst();
        assertFalse( cursor.previous() );

        // just to clear the jdbmTuple value so that line 127 inside after(tuple) method
        // can be executed as part of the below after(tuple) call
        cursor.before(new Tuple<String,DupsContainer<String>>( "1", null ) );
        cursor.after( new Tuple<String,DupsContainer<String>>( "0", null ) ); // this positions on tuple with key 1

        cursor.next(); // this moves onto tuple with key 2
        tuple = cursor.get();
        assertEquals( "2", tuple.getKey() );
        assertEquals( "2", tuple.getValue().getArrayTree().getFirst() );
    }


    @Test
    public void testMiscellaneous() throws Exception
    {
    }
}