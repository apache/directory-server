/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.core.shared.txn;


import java.io.IOException;
import java.util.TreeSet;

import org.apache.directory.server.core.api.partition.index.ForwardIndexComparator;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.GenericIndex;
import org.apache.directory.server.core.api.partition.index.ReverseIndexComparator;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.txn.TxnLogManager;

import org.apache.directory.server.core.api.log.InvalidLogException;

import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.entry.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;

import org.apache.directory.server.core.shared.txn.logedit.IndexChange;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class IndexCursorWrapperTest
{
    /** Test partition Dn */
    private Dn dn;

    /** Mock attribute oid */
    private String attributeOid = "mockOid";

    /** Mock index with the mock attributeoid */
    private MockIndex mockIndex = new MockIndex();

    /** Log buffer size : 4096 bytes */
    private int logBufferSize = 1 << 12;

    /** Log File Size : 8192 bytes */
    private long logFileSize = 1 << 13;

    /** log suffix */
    private static String LOG_SUFFIX = "log";

    /** Txn manager */
    private TxnManagerInternal<Long> txnManager;

    /** Txn log manager */
    private TxnLogManager<Long> txnLogManager;

    /** index entry comparator */
    private ForwardIndexComparator<?, Long> comparator = new ForwardIndexComparator<Long, Long>(
        LongComparator.INSTANCE,
        LongComparator.INSTANCE );

    /** sorted change set for the cursor */
    private TreeSet<IndexEntry<Object, Long>> changedSet;

    /** Cursor to be wrapped*/
    private TxnIndexCursor<Long> cursor;

    /** Cursor wrapper */
    private IndexCursor<Object, Entry, Long> cursorWrapper;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    /**
     * Get the Log folder
     */
    private String getLogFolder() throws IOException
    {
        String file = folder.newFolder( LOG_SUFFIX ).getAbsolutePath();

        return file;
    }


    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws IOException, InvalidLogException
    {
        try
        {
            // Init the test dn
            dn = new Dn( "cn=Test", "ou=department" );

            // Init the txn manager
            TxnManagerFactory.<Long> init( LongComparator.INSTANCE, LongSerializer.INSTANCE, getLogFolder(),
                logBufferSize, logFileSize );
            txnManager = TxnManagerFactory.<Long> txnManagerInternalInstance();
            txnLogManager = TxnManagerFactory.<Long> txnLogManagerInstance();

            // Prepare the to be wrapped cursor
            ForwardIndexEntry<Object, Long> idxEntry;
            changedSet = new TreeSet<IndexEntry<Object, Long>>( ( ForwardIndexComparator<Object, Long> ) comparator );

            for ( int idx = 0; idx < 10; idx++ )
            {
                if ( idx != 5 )
                {
                    idxEntry = new ForwardIndexEntry<Object, Long>();
                    idxEntry.setValue( new Long( idx ) );
                    idxEntry.setId( new Long( idx ) );
                    changedSet.add( idxEntry );
                }

                if ( idx != 5 && idx != 0 )
                {
                    idxEntry = new ForwardIndexEntry<Object, Long>();
                    idxEntry.setValue( new Long( idx ) );
                    idxEntry.setId( new Long( idx + 1 ) );
                    changedSet.add( idxEntry );
                }
            }

            cursor = new TxnIndexCursor<Long>( changedSet, true, null, null, comparator );

            IndexChange<Long> idxChange;

            // Begin a txn and do some index changes.
            DataChangeContainer<Long> changeContainer = new DataChangeContainer<Long>( dn );
            txnManager.beginTransaction( false );

            // Add (5,5) missing in the original index 
            idxChange = new IndexChange<Long>( mockIndex, attributeOid, new Long( 5 ), new Long( 5 ),
                IndexChange.Type.ADD );
            changeContainer.getChanges().add( idxChange );

            // Add (10,11) missing in the original index 
            idxChange = new IndexChange<Long>( mockIndex, attributeOid, new Long( 10 ), new Long( 11 ),
                IndexChange.Type.ADD );
            changeContainer.getChanges().add( idxChange );

            // Delete (6,6) existing in the original index 
            idxChange = new IndexChange<Long>( mockIndex, attributeOid, new Long( 6 ), new Long( 6 ),
                IndexChange.Type.DELETE );
            changeContainer.getChanges().add( idxChange );

            // add the log edit to the current txn
            txnLogManager.log( changeContainer, false );

            txnManager.commitTransaction();

            // Begin another txn and do some more index changes
            changeContainer = new DataChangeContainer<Long>( dn );
            txnManager.beginTransaction( false );

            // Add (4,5) already existing in the original index 
            idxChange = new IndexChange<Long>( mockIndex, attributeOid, new Long( 4 ), new Long( 5 ),
                IndexChange.Type.ADD );
            changeContainer.getChanges().add( idxChange );

            // Re add (0,1) missing in the original index 
            idxChange = new IndexChange<Long>( mockIndex, attributeOid, new Long( 0 ), new Long( 1 ),
                IndexChange.Type.ADD );
            changeContainer.getChanges().add( idxChange );

            // Delete (10,11) added by the previous txn 
            idxChange = new IndexChange<Long>( mockIndex, attributeOid, new Long( 10 ), new Long( 11 ),
                IndexChange.Type.DELETE );
            changeContainer.getChanges().add( idxChange );

            txnLogManager.log( changeContainer, false );

            txnManager.commitTransaction();

            // Begin a read only txn and prepare the cursor wrapper 
            txnManager.beginTransaction( true );

            cursorWrapper = txnLogManager.wrap( dn, cursor, ( ForwardIndexComparator<Object, Long> ) comparator,
                attributeOid, true, null, null );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }


    @After
    public void teardown() throws IOException
    {
        try
        {
            txnManager.commitTransaction();

            if ( cursorWrapper != null )
            {
                cursorWrapper.close();
            }
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testAfter()
    {
        try
        {
            cursorWrapper.afterValue( new Long( 0 ), new Long( 0 ) );
            assertTrue( cursorWrapper.next() );

            IndexEntry<?, Long> next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 0 ) ) );
            assertTrue( next.getId().equals( new Long( 1 ) ) );

            assertTrue( cursorWrapper.next() );
            next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 1 ) ) );
            assertTrue( next.getId().equals( new Long( 1 ) ) );

            cursorWrapper.afterValue( new Long( 5 ), new Long( 4 ) );
            assertTrue( cursorWrapper.next() );

            next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 5 ) ) );
            assertTrue( next.getId().equals( new Long( 5 ) ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testBefore()
    {
        try
        {
            cursorWrapper.beforeValue( new Long( 5 ), new Long( 4 ) );
            assertTrue( cursorWrapper.next() );

            IndexEntry<?, Long> next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 4 ) ) );
            assertTrue( next.getId().equals( new Long( 5 ) ) );

            assertTrue( cursorWrapper.next() );
            next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 5 ) ) );
            assertTrue( next.getId().equals( new Long( 5 ) ) );

            assertTrue( cursorWrapper.next() );
            next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 6 ) ) );
            assertTrue( next.getId().equals( new Long( 7 ) ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testAfterLast()
    {
        try
        {
            cursorWrapper.afterLast();
            assertTrue( cursorWrapper.previous() );

            IndexEntry<?, Long> prev = cursorWrapper.get();
            assertTrue( prev.getValue().equals( new Long( 9 ) ) );
            assertTrue( prev.getId().equals( new Long( 10 ) ) );

            assertTrue( cursorWrapper.previous() );
            prev = cursorWrapper.get();
            assertTrue( prev.getValue().equals( new Long( 9 ) ) );
            assertTrue( prev.getId().equals( new Long( 9 ) ) );

            assertTrue( cursorWrapper.next() );

            assertTrue( cursorWrapper.next() == false );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testBeforeFirst()
    {
        try
        {
            cursorWrapper.beforeFirst();
            assertTrue( cursorWrapper.next() );

            IndexEntry<?, Long> next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 0 ) ) );
            assertTrue( next.getId().equals( new Long( 0 ) ) );

            assertTrue( cursorWrapper.previous() == false );

            assertTrue( cursorWrapper.next() );
            next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 0 ) ) );
            assertTrue( next.getId().equals( new Long( 0 ) ) );

            assertTrue( cursorWrapper.next() );
            next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 0 ) ) );
            assertTrue( next.getId().equals( new Long( 1 ) ) );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testSkipKey()
    {
        try
        {
            cursorWrapper.afterValue( null, new Long( 5 ) );
            assertTrue( cursorWrapper.next() );

            IndexEntry<?, Long> next = cursorWrapper.get();
            assertTrue( next.getValue().equals( new Long( 6 ) ) );
            assertTrue( next.getId().equals( new Long( 7 ) ) );

            cursorWrapper.beforeValue( null, new Long( 1 ) );
            assertTrue( cursorWrapper.previous() );

            IndexEntry<?, Long> prev = cursorWrapper.get();
            assertTrue( prev.getValue().equals( new Long( 0 ) ) );
            assertTrue( prev.getId().equals( new Long( 1 ) ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }

    class MockIndex extends GenericIndex<Long, Entry, Long>
    {
        public MockIndex()
        {
            super( attributeOid );
        }


        public ForwardIndexComparator<Long, Long> getForwardIndexEntryComparator()
        {
            return new ForwardIndexComparator<Long, Long>( LongComparator.INSTANCE,
                LongComparator.INSTANCE );
        }


        public ReverseIndexComparator<Long, Long> getReverseIndexEntryComparator()
        {
            return new ReverseIndexComparator<Long, Long>( LongComparator.INSTANCE,
                LongComparator.INSTANCE );
        }
    }

}
