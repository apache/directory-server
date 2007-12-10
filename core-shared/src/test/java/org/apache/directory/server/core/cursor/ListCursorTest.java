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
package org.apache.directory.server.core.cursor;


import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Tests the ListCursor class.  The assertXxxx() methods defined in this class
 * can be collected in an abstract test case class that can be used to test
 * the behavior of any Cursor implementation down the line.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ListCursorTest extends TestCase
{
    public void testEmptyList() throws IOException
    {
        ListCursor<String> cursor = new ListCursor<String>();

        assertFirstLastOnNewCursor( cursor, 0, 0, 0 );
        assertAbsolute( cursor, 0, 0, 0 );
        assertRelative( cursor, 0, 0, 0 );

        // close test
        cursor.close();
        assertClosed( cursor, "cursor.isCloased() should return true after closing the cursor", true );
    }


    public void testSingleElementList() throws IOException
    {
        ListCursor<String> cursor = new ListCursor<String>( Collections.singletonList( "singleton" ) );
        assertFirstLastOnNewCursor( cursor, 1, 0, 1 );
        assertAbsolute( cursor, 1, 0, 1 );
        assertRelative( cursor, 1, 0, 1 );
        cursor.close();

        // close test
        cursor.close();
        assertClosed( cursor, "cursor.isCloased() should return true after closing the cursor", true );

        // bad bounds: start = end is senseless
        try
        {
            cursor = new ListCursor<String>( Collections.singletonList( "singleton" ), 0 );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: start = end is senseless
        try
        {
            cursor = new ListCursor<String>( 1, Collections.singletonList( "singleton" ) );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: start > end is senseless
        try
        {
            cursor = new ListCursor<String>( 5, Collections.singletonList( "singleton" ) );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: end < start is senseless too in another way :)
        try
        {
            cursor = new ListCursor<String>( Collections.singletonList( "singleton" ), -5 );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: start out of range
        try
        {
            cursor = new ListCursor<String>( -5, Collections.singletonList( "singleton" ) );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: end out of range
        try
        {
            cursor = new ListCursor<String>( Collections.singletonList( "singleton" ), 5 );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
    }


    public void testManyElementList() throws IOException
    {
        List<String> list = new ArrayList<String>();
        list.add( "item 1" );
        list.add( "item 2" );
        list.add( "item 3" );
        list.add( "item 4" );
        list.add( "item 5" );

        // test with bounds of the list itself
        ListCursor<String> cursor = new ListCursor<String>( list );
        assertFirstLastOnNewCursor( cursor, 5, 0, 5 );
        assertAbsolute( cursor, 5, 0, 5 );
        assertRelative( cursor, 5, 0, 5 );
        cursor.close();

        // test with nonzero lower bound
        cursor = new ListCursor<String>( 1, list );
        assertFirstLastOnNewCursor( cursor, 5, 1, 5 );
        assertAbsolute( cursor, 5, 1, 5 );
        assertRelative( cursor, 5, 1, 5 );
        cursor.close();

        // test with nonzero lower bound and upper bound
        cursor = new ListCursor<String>( 1, list, 4 );
        assertFirstLastOnNewCursor( cursor, 5, 1, 4 );
        assertAbsolute( cursor, 5, 1, 4 );
        assertRelative( cursor, 5, 1, 4 );

        // close test
        cursor.close();
        assertClosed( cursor, "cursor.isCloased() should return true after closing the cursor", true );

        // bad bounds: start = end is senseless
        try
        {
            cursor = new ListCursor<String>( list, 0 );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: start = end is senseless
        try
        {
            cursor = new ListCursor<String>( 5, list );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: start > end is senseless
        try
        {
            cursor = new ListCursor<String>( 10, list );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: end < start is senseless too in another way :)
        try
        {
            cursor = new ListCursor<String>( list, -5 );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: start out of range
        try
        {
            cursor = new ListCursor<String>( -5, list );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        // bad bounds: end out of range
        try
        {
            cursor = new ListCursor<String>( list, 10 );
            cursor.close();
            fail( "when the start = end bounds this is senseless and should complain" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
    }


    @SuppressWarnings ( { "ConstantConditions" } )
    protected void assertFirstLastOnNewCursor( Cursor cursor, int listSize, int lowerBound, int upperBound )
            throws IOException
    {
        assertNotNull( cursor );

        String prefix = "[size, " + listSize + "] [lower, " + lowerBound + "] [upper, " + upperBound + "]: ";

        assertFalse( prefix + "new cursor should not be positioned after last", cursor.isAfterLast() );
        assertTrue( prefix + "new cursor should be positioned before first", cursor.isBeforeFirst() );
        assertFalse( prefix + "new cursor should not be closed", cursor.isClosed() );
        assertFalse( prefix + "new cursor should not be positioned at first", cursor.isFirst() );
        assertFalse( prefix + "new cursor should not be positioned at last", cursor.isLast() );

        // beforeFirst and afterLast tests
        cursor.afterLast();
        assertTrue( prefix + "cursor.afterLast() should return true on isAfterLast()", cursor.isAfterLast() );
        assertFalse( prefix + "cursor.afterLast() should return false on isBeforeFirst()", cursor.isBeforeFirst() );
        assertFalse( prefix + "cursor.afterLast() should return false on isFirst()", cursor.isFirst() );
        assertFalse( prefix + "cursor.afterLast() should return false on isLast()", cursor.isLast() );

        cursor.beforeFirst();
        assertTrue( prefix + "cursor.beforeFirst() should return true on isBeforeFirst()", cursor.isBeforeFirst() );
        assertFalse( prefix + "cursor.beforeFirst() should return false on isAfterLast()", cursor.isAfterLast() );
        assertFalse( prefix + "cursor.beforeFirst() should return false on isFirst()", cursor.isFirst() );
        assertFalse( prefix + "cursor.beforeFirst() should return false on isLast()", cursor.isLast() );

        // first() tests
        cursor.afterLast();
        if ( listSize <= 0 )
        {
            assertFalse( "cursor.first() on empty cursor should return false", cursor.first() );
            assertFalse( "cursor.first() on empty cursor should return false on isFirst()", cursor.isFirst() );
            assertFalse( "cursor.first() on empty cursor should should change position state", cursor.isBeforeFirst() );
            assertTrue( "cursor.first() on empty cursor should should change position state", cursor.isAfterLast() );
        }
        else
        {
            assertTrue( prefix + "cursor.first() should return true", cursor.first() );
            assertTrue( prefix + "cursor.first() should return true on isFirst()", cursor.isFirst() );
            assertFalse( prefix + "cursor.first() should change position", cursor.isBeforeFirst() );
            assertFalse( prefix + "cursor.first() should change position", cursor.isAfterLast() );
        }

        // last() tests
        cursor.beforeFirst();
        if ( listSize <= 0 )
        {
            assertFalse( "cursor.last() on empty cursor should return false", cursor.last() );
            assertFalse( "cursor.last() on empty cursor should return false on isLast()", cursor.isLast() );
            assertFalse( "cursor.last() on empty cursor should should change position state", cursor.isAfterLast() );
            assertTrue( "cursor.last() on empty cursor should should change position state", cursor.isBeforeFirst() );
        }
        else
        {
            assertTrue( prefix + "cursor.last() should return true", cursor.last() );
            assertTrue( prefix + "cursor.last() should return true on isLast()", cursor.isLast() );
            assertFalse( prefix + "cursor.last() should not park position after last", cursor.isAfterLast() );
            assertFalse( prefix + "cursor.last() should not park position before first", cursor.isBeforeFirst() );
        }

        // next() tests
        cursor.beforeFirst();
        if ( listSize <= 0 )
        {
            assertFalse( "empty cursor.next() should return false", cursor.next() );
            assertTrue( "empty cursor.next() should change pos to after last", cursor.isAfterLast() );
            assertFalse( "empty cursor.next() should change pos to after last", cursor.isBeforeFirst() );
        }
        else
        {
            assertTrue( prefix + "cursor.next() should return true", cursor.next() );
            assertTrue( prefix + "cursor.next() should change pos to first element", cursor.isFirst() );
            assertFalse( prefix + "cursor.next() should not change pos to after last", cursor.isAfterLast() );
            assertFalse( prefix + "cursor.next() should not change pos to before first", cursor.isBeforeFirst() );

            while( cursor.next() )
            {
                assertFalse( prefix + "cursor.next() should not change pos to before first", cursor.isBeforeFirst() );
                assertFalse( prefix + "cursor.next() should not change pos to first after first advance forward",
                        cursor.isFirst() );
            }

            assertTrue( prefix + "cursor.next() failure should put pos to after last", cursor.isAfterLast() );
        }

        // previous() tests
        cursor.afterLast();
        if ( listSize <= 0 )
        {
            assertFalse( "empty cursor.previous() should return false", cursor.previous() );
            assertTrue( "empty cursor.previous() should change pos to before first", cursor.isBeforeFirst() );
            assertFalse( "empty cursor.previous() should change pos to before first", cursor.isAfterLast() );
        }
        else
        {
            assertTrue( prefix + "cursor.previous() should return true", cursor.previous() );
            assertTrue( prefix + "cursor.previous() should change pos to last element", cursor.isLast() );
            assertFalse( prefix + "cursor.previous() should not change pos to before first", cursor.isBeforeFirst() );
            assertFalse( prefix + "cursor.previous() should not change pos to after last", cursor.isAfterLast() );

            while( cursor.previous() )
            {
                assertFalse( prefix + "cursor.previous() should not change pos to after last", cursor.isAfterLast() );
                assertFalse( prefix + "cursor.previous() should not change pos to last after first advance backward",
                        cursor.isLast() );
            }

            assertTrue( prefix + "cursor.previous() failure should put pos to before first", cursor.isBeforeFirst() );
        }
    }


    protected void assertAbsolute( Cursor cursor, int listSize, int lowerBound, int upperBound )
            throws IOException
    {
        String prefix = "[size, " + listSize + "] [lower, " + lowerBound + "] [upper, " + upperBound + "]: ";

        // test absolute() advance with change of position below lower bound
        cursor.afterLast();
        assertFalse( prefix + "cursor.absolute(" + ( lowerBound - 1 ) +
                ") should return false and change state to before first", cursor.absolute( lowerBound - 1 ) );
        assertTrue( prefix + "cursor.relative(" + ( lowerBound - 1 ) +
                ") should change pos to before first", cursor.isBeforeFirst() );
        assertFalse( prefix + "cursor.relative(" + ( lowerBound - 1 ) +
                ") should --NOT-- change pos to after last", cursor.isAfterLast() );

        if ( listSize == 0 )
        {
            // Corner case!!!  Technically the 0th index is the 1st element
            // which is greater than 0 elements which is the size of the list
            // so technically the observed state change for index = 0 should be
            // the same as when index > 0.
            cursor.beforeFirst();
            assertFalse( "empty cursor.absolute(0) should fail but change state to after last", cursor.absolute( 0 ) );
            assertFalse( "empty cursor.absolute(0) should change pos to after last", cursor.isBeforeFirst() );
            assertTrue( "empty cursor.absolute(0) should change pos to after last", cursor.isAfterLast() );
        }

        // test absolute() advance with change of position above upper bound
        cursor.beforeFirst();
        assertFalse( prefix + "cursor.absolute(" + ( upperBound + 1 )
                + ") should return false but change state to after last", cursor.absolute( upperBound + 1 ) );
        assertFalse( prefix + "cursor.absolute(" + ( upperBound + 1 ) + ") should change pos to after last",
                cursor.isBeforeFirst() );
        assertTrue( prefix + "cursor.absolute(" + ( upperBound + 1 ) + ") should change pos to after last",
                cursor.isAfterLast() );
    }


    protected void assertRelative( Cursor cursor, int listSize, int lowerBound, int upperBound )
            throws IOException
    {
        String prefix = "[size, " + listSize + "] [lower, " + lowerBound + "] [upper, " + upperBound + "]: ";

        // test relative() advance which changes position below lower bound
        cursor.afterLast();
        int relativePos = - ( upperBound - lowerBound + 1 );

        assertFalse( prefix + "cursor.relative(" + relativePos +
                ") should return false and change state to before first", cursor.relative( - ( upperBound + 1 ) ) );
        assertTrue( prefix + "cursor.relative(" + relativePos +
                ") should change pos to before first", cursor.isBeforeFirst() );
        assertFalse( prefix + "cursor.relative(" + relativePos +
                ") should --NOT-- change pos to after last", cursor.isAfterLast() );

        // make sure relative(0) does not change pos if begin state is after last
        cursor.afterLast();
        assertFalse( prefix + "cursor.relative(0) should return false and have no effect on pos",
                cursor.relative( 0 ) );
        assertFalse( prefix + "cursor.relative(0) should have no effect on changing state", cursor.isBeforeFirst() );
        assertTrue( prefix + "cursor.relative(0) should have no effect on changing state", cursor.isAfterLast() );

        // make sure relative(0) does not change pos if begin state is before first
        cursor.beforeFirst();
        assertFalse( prefix + "cursor.relative(0) should return false and have no effect on pos",
                cursor.relative( 0 ) );
        assertTrue( prefix + "cursor.relative(0) should have no effect on changing state", cursor.isBeforeFirst() );
        assertFalse( prefix + "cursor.relative(0) should have no effect on changing state", cursor.isAfterLast() );

        // make relative() advance which changes position above upper bound
        cursor.beforeFirst();
        assertFalse( prefix + "cursor.relative(" + ( upperBound + 1 )
                + ") should return false but change state to after last", cursor.relative( upperBound + 1 ) );
        assertFalse( prefix + "cursor.relative(" + ( upperBound + 1 ) + ") should change pos to after last",
                cursor.isBeforeFirst() );
        assertTrue( prefix + "cursor.relative(" + ( upperBound + 1 ) + ") should change pos to after last",
                cursor.isAfterLast() );
    }


    protected void assertClosed( Cursor cursor, String msg, boolean expected )
    {
        try
        {
            assertEquals( msg, expected, cursor.isClosed() );
        }
        catch ( IOException e )
        {
            fail( "cursor.isClosed() test should not fail after closing the cursor" );
        }

        try
        {
            cursor.close();
        }
        catch ( IOException e )
        {
            fail( "cursor.close() after closing the cursor should not fail with exceptions" );
        }


        try
        {
            cursor.absolute( 1 );
            fail( "cursor.absolute() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.afterLast();
            fail( "cursor.afterLast() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.beforeFirst();
            fail( "cursor.beforeFirst() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.first();
            fail( "cursor.first() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.get();
            fail( "cursor.get() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.isAfterLast();
            fail( "cursor.isAfterLast() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.isBeforeFirst();
            fail( "cursor.isBeforeFirst() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.isFirst();
            fail( "cursor.isFirst() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.isLast();
            fail( "cursor.isLast() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.last();
            fail( "cursor.last() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.next();
            fail( "cursor.next() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.previous();
            fail( "cursor.previous() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }

        try
        {
            cursor.relative( 1 );
            fail( "cursor.relative() after closing the cursor should fail with an IOException" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }
    }
}