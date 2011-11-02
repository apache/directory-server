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


import java.util.TreeSet;

import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.ForwardIndexComparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TxnIndexCursorTest
{
    /** index entry comparator */
    private ForwardIndexComparator<?, Long> comparator = new ForwardIndexComparator<Long, Long>( LongComparator.INSTANCE,
        LongComparator.INSTANCE );

    /** sorted change set for the cursor */
    private TreeSet<IndexEntry<Object, Long>> changedSet;

    /** Cursor */
    TxnIndexCursor<Long> cursor;

    /** Only Key Cursor */
    TxnIndexCursor<Long> onlyKeyCursor;


    @Before
    public void setup()
    {
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

    }


    @After
    public void teardown()
    {
        try
        {
            if ( cursor != null )
            {
                cursor.close();
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
            cursor.afterValue( new Long( 0 ), new Long( 0 ) );
            assertTrue( cursor.next() );

            IndexEntry<?, Long> next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 1 ) ) );
            assertTrue( next.getId().equals( new Long( 1 ) ) );

            cursor.afterValue( new Long( 5 ), new Long( 5 ) );
            assertTrue( cursor.previous() );

            IndexEntry<?, Long> prev = cursor.get();
            assertTrue( prev.getValue().equals( new Long( 4 ) ) );
            assertTrue( prev.getId().equals( new Long( 5 ) ) );
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testBefore()
    {
        try
        {
            cursor.beforeValue( new Long( 0 ), new Long( 0 ) );
            assertTrue( cursor.next() );

            IndexEntry<?, Long> next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 0 ) ) );
            assertTrue( next.getId().equals( new Long( 0 ) ) );

            cursor.beforeValue( new Long( 5 ), new Long( 4 ) );
            assertTrue( cursor.previous() );

            IndexEntry<?, Long> prev = cursor.get();
            assertTrue( prev.getValue().equals( new Long( 4 ) ) );
            assertTrue( prev.getId().equals( new Long( 4 ) ) );
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testAfterLast()
    {
        try
        {
            cursor.afterLast();
            assertTrue( cursor.previous() );

            IndexEntry<?, Long> prev = cursor.get();
            assertTrue( prev.getValue().equals( new Long( 9 ) ) );
            assertTrue( prev.getId().equals( new Long( 10 ) ) );

            assertTrue( cursor.next() == false );
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testBeforeFirst()
    {
        try
        {
            cursor.beforeFirst();
            assertTrue( cursor.next() );

            IndexEntry<?, Long> next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 0 ) ) );
            assertTrue( next.getId().equals( new Long( 0 ) ) );

            assertTrue( cursor.previous() == false );
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testSkipKey()
    {
        try
        {
            cursor.afterValue( null, new Long( 4 ) );
            assertTrue( cursor.next() );

            IndexEntry<?, Long> next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 6 ) ) );
            assertTrue( next.getId().equals( new Long( 6 ) ) );

            cursor.beforeValue( null, new Long( 4 ) );
            assertTrue( cursor.next() );

            next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 4 ) ) );
            assertTrue( next.getId().equals( new Long( 4 ) ) );
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testLockDownByExistingKey()
    {
        onlyKeyCursor = new TxnIndexCursor<Long>( changedSet, true, new Long( 7 ), null, comparator );
        try
        {
            onlyKeyCursor.beforeFirst();

            assertTrue( onlyKeyCursor.next() );
            IndexEntry<?, Long> next = onlyKeyCursor.get();
            assertTrue( next.getValue().equals( new Long( 7 ) ) );
            assertTrue( next.getId().equals( new Long( 7 ) ) );

            assertTrue( onlyKeyCursor.next() );
            next = onlyKeyCursor.get();
            assertTrue( next.getValue().equals( new Long( 7 ) ) );
            assertTrue( next.getId().equals( new Long( 8 ) ) );

            assertTrue( onlyKeyCursor.next() == false );
            assertTrue( onlyKeyCursor.previous() );
            IndexEntry<?, Long> prev = onlyKeyCursor.get();
            assertTrue( prev.getValue().equals( new Long( 7 ) ) );
            assertTrue( prev.getId().equals( new Long( 8 ) ) );

            assertTrue( onlyKeyCursor.previous() );
            prev = onlyKeyCursor.get();
            assertTrue( prev.getValue().equals( new Long( 7 ) ) );
            assertTrue( prev.getId().equals( new Long( 7 ) ) );

            assertTrue( onlyKeyCursor.previous() == false );
            assertTrue( onlyKeyCursor.previous() == false );
            assertTrue( onlyKeyCursor.next() == true );
            next = onlyKeyCursor.get();
            assertTrue( next.getValue().equals( new Long( 7 ) ) );
            assertTrue( next.getId().equals( new Long( 7 ) ) );

            onlyKeyCursor.afterValue( null, new Long( 7 ) );

            assertTrue( onlyKeyCursor.next() == false );
            assertTrue( onlyKeyCursor.previous() );
            prev = onlyKeyCursor.get();
            assertTrue( prev.getValue().equals( new Long( 7 ) ) );
            assertTrue( prev.getId().equals( new Long( 8 ) ) );

        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testLockDownByNonExistingKey()
    {
        onlyKeyCursor = new TxnIndexCursor<Long>( changedSet, true, new Long( 5 ), null, comparator );
        try
        {
            onlyKeyCursor.beforeFirst();
            assertTrue( onlyKeyCursor.next() == false );
            assertTrue( onlyKeyCursor.previous() == false );

        }
        catch ( Exception e )
        {
            fail();
        }
    }
}
