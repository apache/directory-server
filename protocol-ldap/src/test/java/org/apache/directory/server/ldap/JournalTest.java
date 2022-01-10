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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.controls.ChangeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmTable;
import org.apache.directory.server.core.partition.impl.btree.jdbm.StringSerializer;
import org.apache.directory.server.ldap.replication.ReplicaEventMessage;
import org.apache.directory.server.ldap.replication.ReplicaEventMessageSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.TransactionManager;


/**
 * A test to check that we can correctly create a Journal to store the ReplicaEventMessages.
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
    private JdbmTable<String, ReplicaEventMessage> journal;

    /** The CsnFactory */
    private static CsnFactory csnFactory;
    
    /** The partition transaction */
    private PartitionTxn partitionTxn;


    /**
     * Load the SchemaManager
     * @throws Exception
     */
    @BeforeAll
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
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        csnFactory = new CsnFactory( 0 );
    }


    /**
     * Create the JdbmTable
     */
    @BeforeEach
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
        TransactionManager transactionManager = ( ( BaseRecordManager ) recman ).getTransactionManager();
        transactionManager.setMaximumTransactionsInLog( 200 );

        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.CSN_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        journal = new JdbmTable<String, ReplicaEventMessage>( schemaManager, "test", recman, comparator,
            StringSerializer.INSTANCE, new ReplicaEventMessageSerializer( schemaManager ) );
        
        partitionTxn = new MockPartitionTxn();
    }


    /**
     * Delete the files on disk
     */
    @AfterEach
    public void destroyTable() throws Exception
    {
        if ( journal != null )
        {
            journal.close( partitionTxn );
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
     * test that we can write 1000 ReplicaEventMessages, and read them back in the right order 
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

            ReplicaEventMessage replicaEventMessage = new ReplicaEventMessage( ChangeType.ADD, entry );
            journal.put( partitionTxn, entryCsn.toString(), replicaEventMessage );

            entryCsn = csnFactory.newInstance();
        }

        // Now check that the ReplicaEventMessages has been written
        ReplicaEventMessage firstMessage = journal.get( partitionTxn, firstCsn.toString() );

        assertEquals( ChangeType.ADD, firstMessage.getChangeType() );
        assertEquals( "test0", firstMessage.getEntry().get( "ou" ).getString() );

        // Read entry from the 100th element
        Cursor<Tuple<String, ReplicaEventMessage>> cursor = journal.cursor( partitionTxn, csn100.toString() );
        int pos = 100;

        while ( cursor.next() )
        {
            Tuple<String, ReplicaEventMessage> tuple = cursor.get();
            ReplicaEventMessage replicaEventMessage = tuple.getValue();

            assertEquals( ChangeType.ADD, replicaEventMessage.getChangeType() );
            assertEquals( "test" + pos, replicaEventMessage.getEntry().get( "ou" ).getString() );

            pos++;
        }

        cursor.close();
    }


    /**
     * test that we can write 1000 ReplicaEventMessages, remove 500 of them, and read the 
     * remaining ones.
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

            ReplicaEventMessage replicaEventMessage = new ReplicaEventMessage( ChangeType.ADD, entry );
            journal.put( partitionTxn, entryCsn.toString(), replicaEventMessage );

            entryCsn = csnFactory.newInstance();
        }

        // Remove the first 500 ReplicaEventMessages
        Cursor<Tuple<String, ReplicaEventMessage>> deleteCursor = journal.cursor();
        int deleted = 0;

        while ( deleteCursor.next() && ( deleted < 500 ) )
        {
            Tuple<String, ReplicaEventMessage> tuple = deleteCursor.get();
            ReplicaEventMessage replicaEventMessage = tuple.getValue();

            assertEquals( ChangeType.ADD, replicaEventMessage.getChangeType() );
            assertEquals( "test" + deleted, replicaEventMessage.getEntry().get( "ou" ).getString() );

            journal.remove( partitionTxn, replicaEventMessage.getEntry().get( "entryCsn" ).getString() );
            deleted++;
        }

        deleteCursor.close();

        // Now check that the first mod is the 501th
        assertEquals( 500, journal.count( partitionTxn ) );

        Cursor<Tuple<String, ReplicaEventMessage>> cursor = journal.cursor();

        cursor.next();

        Tuple<String, ReplicaEventMessage> tuple = cursor.get();
        ReplicaEventMessage replicaEventMessage = tuple.getValue();
        assertEquals( ChangeType.ADD, replicaEventMessage.getChangeType() );
        assertEquals( "test500", replicaEventMessage.getEntry().get( "ou" ).getString() );

        cursor.close();
    }


    /**
     * Test the performances for 100 000 writes, read and delete.
     * On my laptop, it takes : <br>
     * <ul>
     * <li>457 seconds to create 100 000 ReplicaEventMessages ( 219/s )</li>
     * <li>17 seconds to read 100 000 ReplicaEventMessages ( 5893/s )</li>
     * <li>546 seconds to delete 100 000 ReplicaEventMessages ( 183/s )</li>
     * </ul>
     * 
     */
    @Test
    @Disabled("Performance test")
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

            ReplicaEventMessage replicaEventMessage = new ReplicaEventMessage( ChangeType.ADD, entry );
            journal.put( partitionTxn, entryCsn.toString(), replicaEventMessage );
            recman.commit();

            entryCsn = csnFactory.newInstance();
        }

        long t1 = System.currentTimeMillis();

        System.out.println( "Time to write 100 000 ReplicaEventMessages : " + ( t1 - t0 ) );

        // The read perf
        long t2 = System.currentTimeMillis();

        Cursor<Tuple<String, ReplicaEventMessage>> readCursor = journal.cursor();

        int pos = 0;

        while ( readCursor.next() )
        {
            Tuple<String, ReplicaEventMessage> tuple = readCursor.get();
            ReplicaEventMessage replicaEventMessage = tuple.getValue();

            assertEquals( ChangeType.ADD, replicaEventMessage.getChangeType() );
            assertEquals( "test" + pos, replicaEventMessage.getEntry().get( "ou" ).getString() );

            pos++;
        }

        long t3 = System.currentTimeMillis();

        System.out.println( "Time to read 100 000 ReplicaEventMessages : " + ( t3 - t2 ) );

        // The delete perf
        long t4 = System.currentTimeMillis();

        Cursor<Tuple<String, ReplicaEventMessage>> deleteCursor = journal.cursor();
        int deleted = 0;

        while ( deleteCursor.next() )
        {
            Tuple<String, ReplicaEventMessage> tuple = deleteCursor.get();
            ReplicaEventMessage replicaEventMessage = tuple.getValue();

            assertEquals( ChangeType.ADD, replicaEventMessage.getChangeType() );
            assertEquals( "test" + deleted, replicaEventMessage.getEntry().get( "ou" ).getString() );

            journal.remove( partitionTxn, replicaEventMessage.getEntry().get( "entryCsn" ).getString() );
            recman.commit();

            deleted++;
        }

        long t5 = System.currentTimeMillis();

        System.out.println( "Time to delete 100 000 ReplicaEventMessages : " + ( t5 - t4 ) );
    }
}
