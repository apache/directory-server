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


import org.apache.directory.shared.ldap.NotImplementedException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


/**
 * A simple implementation of a Cursor on a {@link List}.  Optionally, the
 * Cursor may be limited to a specific range within the list.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ListCursor<E> extends AbstractCursor<E>
{
    private final List<E> list;
    private final int start;
    private final int end;
    private int index = -1;


    /**
     * Creates a new ListCursor with lower (inclusive) and upper (exclusive)
     * bounds.
     *
     * As with all Cursors, this ListCursor requires a successful return from
     * advance operations (next() or previous()) to properly return values
     * using the get() operation.
     *
     * @param start the lower bound index
     * @param list the list this ListCursor operates on
     * @param end the upper bound index
     */
    public ListCursor( int start, List<E> list, int end )
    {
        if ( start < 0 || start > list.size() )
        {
            throw new IllegalArgumentException( "start index '" + start + "' out of range" );
        }

        if ( end < 0 || end > list.size() )
        {
            throw new IllegalArgumentException( "end index '" + end + "' out of range" );
        }

        // check list is not empty list since the empty list is the only situation
        // where we allow for start to equal the end: in other cases it makes no sense
        if ( list.size() > 0 && start >= end )
        {
            throw new IllegalArgumentException( "start index '" + start + "' greater than or equal to end index '"
                    + end + "' just does not make sense" );
        }

        //noinspection ConstantConditions
        if ( list != null )
        {
            this.list = list;
        }
        else
        {
            //noinspection unchecked
            this.list = Collections.EMPTY_LIST;
        }

        this.start = start;
        this.end = end;
    }


    /**
     * Creates a new ListCursor with a specific upper (exclusive) bound: the
     * lower (inclusive) bound defaults to 0.
     *
     * @param list the backing for this ListCursor
     * @param end the upper bound index representing the position after the
     * last element
     */
    public ListCursor( List<E> list, int end )
    {
        this( 0, list, end );
    }


    /**
     * Creates a new ListCursor with a lower (inclusive) bound: the upper
     * (exclusive) bound is the size of the list.
     *
     * @param start the lower (inclusive) bound index: the position of the
     * first entry
     * @param list the backing for this ListCursor
     */
    public ListCursor( int start, List<E> list )
    {
        this( start, list, list.size() );
    }


    /**
     * Creates a new ListCursor without specific bounds: the bounds are
     * acquired from the size of the list.
     *
     * @param list the backing for this ListCursor
     */
    public ListCursor( List<E> list )
    {
        this( 0, list, list.size() );
    }


    /**
     * Creates a new ListCursor without any elements.
     */
    public ListCursor()
    {
        //noinspection unchecked
        this( 0, Collections.EMPTY_LIST, 0 );
    }


    public boolean before( E element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean after( E element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws IOException
    {
        checkClosed( "beforeFirst()" );
        this.index = -1;
    }


    public void afterLast() throws IOException
    {
        checkClosed( "afterLast()" );
        this.index = end;
    }


    public boolean absolute( int index ) throws IOException
    {
        checkClosed( "absolute()" );

        if ( index < start )
        {
            this.index = -1;
            return false;
        }

        if ( index >= end )
        {
            this.index = end;
            return false;
        }

        this.index = index;
        return true;
    }


    public boolean relative( int index ) throws IOException
    {
        checkClosed( "relative()" );

        if ( this.index + index < start )
        {
            this.index = -1;
            return false;
        }

        if ( this.index + index >= end )
        {
            this.index = end;
            return false;
        }

        this.index += index;
        return true;
    }


    public boolean first() throws IOException
    {
        checkClosed( "first()" );

        if ( list.size() > 0 )
        {
            index = start;
            return true;
        }

        return false;
    }


    public boolean last() throws IOException
    {
        checkClosed( "last()" );

        if ( list.size() > 0 )
        {
            index = end - 1;
            return true;
        }
        
        return false;
    }


    public boolean isFirst() throws IOException
    {
        checkClosed( "isFirst()" );
        return list.size() > 0 && index == start;
    }


    public boolean isLast() throws IOException
    {
        checkClosed( "isLast()" );
        return list.size() > 0 && index == end - 1;

    }


    public boolean isAfterLast() throws IOException
    {
        checkClosed( "isAfterLast()" );
        return index == end;
    }


    public boolean isBeforeFirst() throws IOException
    {
        checkClosed( "isBeforeFirst()" );
        return index == -1;
    }


    public boolean previous() throws IOException
    {
        checkClosed( "previous()" );

        // if parked at -1 we cannot go backwards
        if ( index == -1 )
        {
            return false;
        }

        // if the index moved back is still greater than or eq to start then OK
        if ( index - 1 >= start )
        {
            index--;
            return true;
        }

        // if the index currently less than or equal to start we need to park it at -1 and return false
        if ( index <= start )
        {
            index = -1;
            return false;
        }

        if ( list.size() <= 0 )
        {
            index = -1;
        }

        return false;
    }


    public boolean next() throws IOException
    {
        checkClosed( "next()" );

        // if parked at -1 we advance to the start index and return true
        if ( list.size() > 0 && index == -1 )
        {
            index = start;
            return true;
        }

        // if the index plus one is less than the end then increment and return true
        if ( list.size() > 0 && index + 1 < end )
        {
            index++;
            return true;
        }

        // if the index plus one is equal to the end then increment and return false
        if ( list.size() > 0 && index + 1 == end )
        {
            index++;
            return false;
        }

        if ( list.size() <= 0 )
        {
            index = end;
        }

        return false;
    }


    public E get() throws IOException
    {
        checkClosed( "get()" );
        if ( index < start || index >= end )
        {
            throw new IOException( "Cursor not positioned at an element" );
        }

        return list.get( index );
    }


    public boolean isElementReused()
    {
        return true;
    }
}
