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

        // close test
        cursor.close();
        assertClosed( cursor, "cursor.isCloased() should return true after closing the cursor", true );
    }


    public void testSingleElementList() throws IOException
    {
        ListCursor<String> cursor = new ListCursor<String>( Collections.singletonList( "singleton" ) );
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
        cursor.close();

        // test with nonzero lower bound
        cursor = new ListCursor<String>( 1, list );
        cursor.close();

        // test with nonzero lower bound and upper bound
        cursor = new ListCursor<String>( 1, list, 4 );

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
    }
}