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
import java.util.UUID;

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
    private ForwardIndexComparator<?> comparator = new ForwardIndexComparator<Long>( LongComparator.INSTANCE );

    /** sorted change set for the cursor */
    private TreeSet<IndexEntry<Object>> changedSet;

    /** Cursor */
    TxnIndexCursor cursor;

    /** Only Key Cursor */
    TxnIndexCursor onlyKeyCursor;

    /** UUID string */
    UUID baseUUID = UUID.fromString( "00000000-0000-0000-0000-000000000001" );

    @Before
    public void setup()
    {
        ForwardIndexEntry<Object> idxEntry;
        changedSet = new TreeSet<IndexEntry<Object>>( ( ForwardIndexComparator<Object> ) comparator );

        for ( int idx = 0; idx < 10; idx++ )
        {
            if ( idx != 5 )
            {
                idxEntry = new ForwardIndexEntry<Object>();
                idxEntry.setValue( new Long( idx ) );
                idxEntry.setId( getUUIDString( idx ) );
                changedSet.add( idxEntry );
            }

            if ( idx != 5 && idx != 0 )
            {
                idxEntry = new ForwardIndexEntry<Object>();
                idxEntry.setValue( new Long( idx ) );
                idxEntry.setId( getUUIDString( idx + 1 ) );
                changedSet.add( idxEntry );
            }
        }

        cursor = new TxnIndexCursor( changedSet, true, null, null, ( ForwardIndexComparator<Object> )comparator );

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
            cursor.afterValue( getUUIDString( 0 ), new Long( 0 ) );
            assertTrue( cursor.next() );

            IndexEntry<?> next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 1 ) ) );
            assertTrue( next.getId().equals( getUUIDString( 1 ) ) );

            cursor.afterValue( getUUIDString( 5 ), new Long( 5 ) );
            assertTrue( cursor.previous() );

            IndexEntry<?> prev = cursor.get();
            assertTrue( prev.getValue().equals( new Long( 4 ) ) );
            assertTrue( prev.getId().equals( getUUIDString( 5 ) ) );
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
            cursor.beforeValue( getUUIDString( 0 ), new Long( 0 ) );
            assertTrue( cursor.next() );

            IndexEntry<?> next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 0 ) ) );
            assertTrue( next.getId().equals( getUUIDString( 0 ) ) );

            cursor.beforeValue( getUUIDString( 5 ), new Long( 4 ) );
            assertTrue( cursor.previous() );

            IndexEntry<?> prev = cursor.get();
            assertTrue( prev.getValue().equals( new Long( 4 ) ) );
            assertTrue( prev.getId().equals( getUUIDString( 4 ) ) );
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

            IndexEntry<?> prev = cursor.get();
            assertTrue( prev.getValue().equals( new Long( 9 ) ) );
            assertTrue( prev.getId().equals( getUUIDString( 10 ) ) );

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

            IndexEntry<?> next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 0 ) ) );
            assertTrue( next.getId().equals( getUUIDString( 0 ) ) );

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

            IndexEntry<?> next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 6 ) ) );
            assertTrue( next.getId().equals( getUUIDString( 6 ) ) );

            cursor.beforeValue( null, new Long( 4 ) );
            assertTrue( cursor.next() );

            next = cursor.get();
            assertTrue( next.getValue().equals( new Long( 4 ) ) );
            assertTrue( next.getId().equals( getUUIDString( 4 ) ) );
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testLockDownByExistingKey()
    {
        onlyKeyCursor = new TxnIndexCursor( changedSet, true, new Long( 7 ), null, ( ForwardIndexComparator<Object> )comparator );
        try
        {
            onlyKeyCursor.beforeFirst();

            assertTrue( onlyKeyCursor.next() );
            IndexEntry<?> next = onlyKeyCursor.get();
            assertTrue( next.getValue().equals( new Long( 7 ) ) );
            assertTrue( next.getId().equals( getUUIDString( 7 ) ) );

            assertTrue( onlyKeyCursor.next() );
            next = onlyKeyCursor.get();
            assertTrue( next.getValue().equals( new Long( 7 ) ) );
            assertTrue( next.getId().equals( getUUIDString( 8 ) ) );

            assertTrue( onlyKeyCursor.next() == false );
            assertTrue( onlyKeyCursor.previous() );
            IndexEntry<?> prev = onlyKeyCursor.get();
            assertTrue( prev.getValue().equals( new Long( 7 ) ) );
            assertTrue( prev.getId().equals( getUUIDString( 8 ) ) );

            assertTrue( onlyKeyCursor.previous() );
            prev = onlyKeyCursor.get();
            assertTrue( prev.getValue().equals( new Long( 7 ) ) );
            assertTrue( prev.getId().equals( getUUIDString( 7 ) ) );

            assertTrue( onlyKeyCursor.previous() == false );
            assertTrue( onlyKeyCursor.previous() == false );
            assertTrue( onlyKeyCursor.next() == true );
            next = onlyKeyCursor.get();
            assertTrue( next.getValue().equals( new Long( 7 ) ) );
            assertTrue( next.getId().equals( getUUIDString( 7 ) ) );

            onlyKeyCursor.afterValue( null, new Long( 7 ) );

            assertTrue( onlyKeyCursor.next() == false );
            assertTrue( onlyKeyCursor.previous() );
            prev = onlyKeyCursor.get();
            assertTrue( prev.getValue().equals( new Long( 7 ) ) );
            assertTrue( prev.getId().equals( getUUIDString( 8 ) ) );

        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testLockDownByNonExistingKey()
    {
        onlyKeyCursor = new TxnIndexCursor( changedSet, true, new Long( 5 ), null, ( ForwardIndexComparator<Object> )comparator );
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
    
    private UUID getUUIDString( int idx )
    {
        long low = baseUUID.getLeastSignificantBits();
        long high = baseUUID.getMostSignificantBits();
        low = low + idx;
        
        return new UUID( high, low );
    }
}
