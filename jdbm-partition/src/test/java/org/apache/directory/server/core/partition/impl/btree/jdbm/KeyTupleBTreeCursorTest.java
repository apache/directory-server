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


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Comparator;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.DefaultSerializer;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test case for KeyTupleBTreeCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KeyTupleBTreeCursorTest
{
    JdbmTable<String,String> table;
    Comparator<String> comparator;
    KeyTupleBTreeCursor<String, String> cursor;
    File dbFile;
    RecordManager recman;
    
    private static final String KEY = "1";
    private static final String TEST_OUTPUT_PATH = "test.output.path";
    private static SchemaManager schemaManager;


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
    public void createTree() throws Exception
    {
        comparator = new Comparator<String>() 
        {
            public int compare( String i1, String i2 )
            {
                return i1.compareTo( i2 );
            }
        };

        File tmpDir = null;
        
        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );
        
        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        table = new JdbmTable<String,String>( schemaManager, "test", 6, recman,
                comparator, comparator, new DefaultSerializer(), new DefaultSerializer() );

        cursor = new KeyTupleBTreeCursor<String, String>( table.getBTree(), KEY, comparator );
    }
    
    
    @After 
    public void destroytable() throws Exception
    {
        recman.close();
        recman = null;
        dbFile.deleteOnExit();

        String fileToDelete = dbFile.getAbsolutePath();
        new File( fileToDelete + ".db" ).delete();
        new File( fileToDelete + ".lg" ).delete();

        dbFile = null;
    }
    

    @Test( expected = InvalidCursorPositionException.class )
    public void testEmptyCursor() throws Exception
    {
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        
        assertFalse( cursor.isClosed() );
        
        assertFalse( cursor.first() );
        assertFalse( cursor.last() );
        
        cursor.get(); // should throw InvalidCursorPositionException
    }
    

    @Test
    public void testNonEmptyCursor() throws Exception
    {
        table.put( KEY, "3" );
        table.put( KEY, "5" );
        table.put( KEY, "7" );
        table.put( KEY, "12" );
        table.put( KEY, "0" );
        table.put( KEY, "30" );
        table.put( KEY, "25" );
       
        cursor = new KeyTupleBTreeCursor<String, String>( getDupsContainer(), KEY, comparator );
   
        cursor.before( new Tuple<String, String>( KEY, "3" ) );
        assertTrue( cursor.next() );
        assertEquals( "3", cursor.get().getValue() );
        
        cursor.after( new Tuple<String, String>( KEY, "100" ) );
        assertFalse( cursor.next() );
        
        cursor.beforeFirst();
        cursor.after( new Tuple<String, String>( KEY, "13" ) );
        assertTrue( cursor.next() );
        assertEquals( "25", cursor.get().getValue() );
        
        cursor.beforeFirst();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
        assertEquals( "0", cursor.get().getValue() );
        
        cursor.afterLast();
        assertFalse( cursor.next() );
        
        assertTrue( cursor.first() );
        assertTrue( cursor.available() );
        assertEquals( "0", cursor.get().getValue() );
        
        assertTrue( cursor.last() );
        assertTrue( cursor.available() );
        assertEquals( "30", cursor.get().getValue() );
        
        assertTrue( cursor.previous() );
        assertEquals( "25", cursor.get().getValue() );
    
        assertTrue( cursor.next() );
        assertEquals( "30", cursor.get().getValue() ); 
    
    }

    private BTree getDupsContainer() throws Exception
    {
        BTree tree = table.getBTree();
        
        DupsContainer<String> values = table.getDupsContainer( ( byte[] ) tree.find( KEY ) );
        
        return table.getBTree( values.getBTreeRedirect() );   
    }
}
