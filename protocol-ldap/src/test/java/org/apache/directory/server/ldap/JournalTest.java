/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmTable;
import org.apache.directory.server.core.partition.impl.btree.jdbm.StringSerializer;
import org.apache.directory.server.ldap.replication.Modification;
import org.apache.directory.server.ldap.replication.ModificationSerializer;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.Csn;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
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
import org.junit.Ignore;
import org.junit.Test;

/**
 * A test to check that we can correctly create a Journal to store the modifications.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public class JournalTest
{
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    /** the underlying file  */
    private File dbFile;
    
    /** The record manager*/
    private RecordManager recman;
    
    /** The SchemaManager instance */
    private static SchemaManager schemaManager;

    /** The Journal */
    private JdbmTable<String, Modification> journal;
    
    /** The CsnFactory */
    private static CsnFactory csnFactory;

    /**
     * Load the SchemaManager
     * @throws Exception
     */
    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = JournalTest.class.getResource( "" ).getPath();
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

        csnFactory = new CsnFactory( 0 );
    }

    
    /**
     * Create the JdbmTable
     */
    @Before
    public void createTable() throws Exception
    {
        destroyTable();
        File tmpDir = null;
        
        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.CSN_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );
        
        journal = new JdbmTable<String, Modification>( schemaManager, "test", recman, comparator, 
            new StringSerializer(), new ModificationSerializer( schemaManager ) );
    }

    /**
     * Delete the files on disk
     */
    @After
    public void destroyTable() throws Exception
    {
        if ( journal != null )
        {
            journal.close();
        }

        journal = null;

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
    

    /**
     * test that we can write 1000 modifications, and read them back in the right order 
     * starting in the middle.
     */
    @Test
    public void testJournalWriting() throws Exception
    {
        Csn entryCsn = csnFactory.newInstance();
        Csn firstCsn = entryCsn; 
        Csn csn100 = null;

        for ( int i = 0; i < 1000; i++ )
        {
            if ( i == 100 )
            {
                csn100 = entryCsn;
            }
            
            Entry entry = new DefaultEntry( schemaManager, "ou=test" + i + ",ou=system", 
                "ObjectClass: top",
                "ObjectClass: organizationalUnit",
                "ou", "test" + i,
                "entryCsn", entryCsn.toString()
                );
            
            Modification modification = new Modification( EventType.ADD, entry );
            journal.put( entryCsn.toString(), modification );
            journal.sync();

            entryCsn = csnFactory.newInstance();
        }
        
        // Now check that the modification has been written
        Modification firstModification = journal.get( firstCsn.toString() );
        
        assertEquals( EventType.ADD, firstModification.getEventType());
        assertEquals( "test0", firstModification.getEntry().get( "ou" ).getString() );
        
        // Read entry from the 100th element
        Cursor<Tuple<String, Modification>> cursor = journal.cursor( csn100.toString() );
        int pos = 100;
        
        while ( cursor.next() )
        {
            Tuple<String, Modification> tuple = cursor.get();
            Modification modification = tuple.getValue();
            
            assertEquals( EventType.ADD, modification.getEventType());
            assertEquals( "test" + pos, modification.getEntry().get( "ou" ).getString() );
            
            pos++;
        }
    }


    /**
     * test that we can write 1000 modifications, remove 500 of them, and read the 
     * remining ones.
     */
    @Test
    public void testJournalTruncate() throws Exception
    {
        Csn entryCsn = csnFactory.newInstance();

        for ( int i = 0; i < 1000; i++ )
        {
            Entry entry = new DefaultEntry( schemaManager, "ou=test" + i + ",ou=system", 
                "ObjectClass: top",
                "ObjectClass: organizationalUnit",
                "ou", "test" + i,
                "entryCsn", entryCsn.toString()
                );
            
            Modification modification = new Modification( EventType.ADD, entry );
            journal.put( entryCsn.toString(), modification );
            journal.sync();

            entryCsn = csnFactory.newInstance();
        }
        
        // Remove the first 500 modifications
        Cursor<Tuple<String, Modification>> deleteCursor = journal.cursor();
        int deleted = 0;

        while ( deleteCursor.next() && ( deleted < 500 ) )
        {
            Tuple<String, Modification> tuple = deleteCursor.get();
            Modification modification = tuple.getValue();
            
            assertEquals( EventType.ADD, modification.getEventType());
            assertEquals( "test" + deleted, modification.getEntry().get( "ou" ).getString() );
            
            journal.remove( modification.getEntry().get( "entryCsn" ).getString() );
            deleted++;
        }
        
        // Now check that the first mod is the 501th
        assertEquals( 500, journal.count() );
        
        Cursor<Tuple<String, Modification>> cursor = journal.cursor();
        
        cursor.next();

        Tuple<String, Modification> tuple = cursor.get();
        Modification modification = tuple.getValue();
        assertEquals( EventType.ADD, modification.getEventType() );
        assertEquals( "test500", modification.getEntry().get( "ou" ).getString() );
    }


    /**
     * Test the performances for 100 000 writes, read and delete.
     * On my laptop, it takes : <br>
     * <ul>
     * <li>63,4 seconds to create 100 000 modifications ( 1577/s )</li>
     * <li>18,9 seconds to read 100 000 modifications ( 5298/s )</li>
     * <li>329 seconds to delete 100 000 modifications ( 303/s )</li>
     * </ul>
     * 
     */
    @Test
    @Ignore( "Performance test" )
    public void testJournalPerf() throws Exception
    {
        Csn entryCsn = csnFactory.newInstance();

        // The write perf
        long t0 = System.currentTimeMillis();

        for ( int i = 0; i < 100000; i++ )
        {
            Entry entry = new DefaultEntry( schemaManager, "ou=test" + i + ",ou=system", 
                "ObjectClass: top",
                "ObjectClass: organizationalUnit",
                "ou", "test" + i,
                "entryCsn", entryCsn.toString()
                );
            
            Modification modification = new Modification( EventType.ADD, entry );
            journal.put( entryCsn.toString(), modification );
            journal.sync();

            entryCsn = csnFactory.newInstance();
        }
        
        long t1 = System.currentTimeMillis();
        
        System.out.println( "Time to write 100 000 modifications : " + ( t1 - t0 ) );
        
        // The read perf
        long t2 = System.currentTimeMillis();
        
        Cursor<Tuple<String, Modification>> readCursor = journal.cursor();

        int pos = 0;
        
        while ( readCursor.next() )
        {
            Tuple<String, Modification> tuple = readCursor.get();
            Modification modification = tuple.getValue();
            
            assertEquals( EventType.ADD, modification.getEventType());
            assertEquals( "test" + pos, modification.getEntry().get( "ou" ).getString() );
            
            pos++;
        }
        
        long t3 = System.currentTimeMillis();
        
        System.out.println( "Time to read 100 000 modifications : " + ( t3 - t2 ) );

        // The delete perf
        long t4 = System.currentTimeMillis();
        
        Cursor<Tuple<String, Modification>> deleteCursor = journal.cursor();
        int deleted = 0;

        while ( deleteCursor.next() )
        {
            Tuple<String, Modification> tuple = deleteCursor.get();
            Modification modification = tuple.getValue();
            
            assertEquals( EventType.ADD, modification.getEventType());
            assertEquals( "test" + deleted, modification.getEntry().get( "ou" ).getString() );
            
            journal.remove( modification.getEntry().get( "entryCsn" ).getString() );
            deleted++;
        }

        long t5 = System.currentTimeMillis();

        System.out.println( "Time to delete 100 000 modifications : " + ( t5 - t4 ) );
    }
}
