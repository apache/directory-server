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
package org.apache.directory.server.core.avltree;


import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;


/**
 * A Cursor for an ArrayTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ArrayTreeCursor<E> extends AbstractCursor<E>
{
    /** The underlying ArrayTree */
    private ArrayTree<E> array;

    /** The current position/index in the array */
    private int current;

    /** The current position of this cursor, relative to the node */
    private Position position;


    /**
     * Create a cursor on an ArrayTree
     * @param array The array we want a cursor for
     */
    public ArrayTreeCursor( ArrayTree<E> array )
    {
        this.array = array;
        position = Position.BEFORE_FIRST;
    }


    /**
     * {@inheritDoc}
     */
    public void after( E element ) throws Exception
    {
        checkNotClosed( "after" );

        if ( element == null )
        {
            afterLast();
            return;
        }

        current = array.getAfterPosition( element );

        if ( current == -1 )
        {
            // As the element has not been found, we move after the last position
            position = Position.AFTER_LAST;
        }
        else
        {
            // the cursor should be positioned after the given element
            // we just fetched the next greater element so the cursor
            // is positioned before the fetched element
            position = Position.BEFORE_NODE;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast" );

        current = -1;
        position = Position.AFTER_LAST;
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return position == Position.ON_NODE;
    }


    /**
     * {@inheritDoc}
     */
    public void before( E element ) throws Exception
    {
        checkNotClosed( "before" );

        if ( element == null )
        {
            beforeFirst();
            return;
        }

        current = array.getBeforePosition( element );

        if ( current == -1 )
        {
            // If the element has not been found, move to thea first position
            position = Position.BEFORE_FIRST;
        }
        else
        {
            // the cursor should be positioned before the given element
            // we just fetched the next less element so the cursor
            // is positioned after the fetched element
            position = Position.AFTER_NODE;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst" );

        current = -1;
        position = Position.BEFORE_FIRST;
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        checkNotClosed( "first" );

        if ( array.isEmpty() )
        {
            current = -1;
            position = Position.BEFORE_FIRST;
            return false;
        }
        else
        {
            current = 0;
            position = Position.ON_NODE;
            return true;
        }
    }


    /**
     * {@inheritDoc}
     */
    public E get() throws Exception
    {
        checkNotClosed( "get" );

        if ( position == Position.ON_NODE )
        {
            return array.get( current );
        }

        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        checkNotClosed( "last" );

        if ( array.isEmpty() )
        {
            current = -1;
            position = Position.AFTER_LAST;
            return false;
        }
        else
        {
            current = array.size() - 1;
            position = Position.ON_NODE;
            return true;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        checkNotClosed( "next" );

        // If the array is empty, return false
        if ( array.size() == 0 )
        {
            return false;
        }

        switch ( position )
        {
            case BEFORE_FIRST:
                return first();

            case BEFORE_NODE:
                position = Position.ON_NODE;
                return true;

            case ON_NODE:
            case AFTER_NODE:
                current++;
                if ( current > array.size() - 1 )
                {
                    afterLast();
                    return false;
                }
                else
                {
                    position = Position.ON_NODE;
                    return true;
                }

            case AFTER_LAST:
                return false;

            default:
                throw new IllegalStateException( "Unexpected position " + position );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        checkNotClosed( "previous" );

        if ( array.size() == 0 )
        {
            return false;
        }

        switch ( position )
        {
            case BEFORE_FIRST:
                return false;

            case BEFORE_NODE:
            case ON_NODE:
                current--;
                if ( current < 0 )
                {
                    beforeFirst();
                    return false;
                }
                else
                {
                    position = Position.ON_NODE;
                    return true;
                }

            case AFTER_NODE:
                position = Position.ON_NODE;
                return true;

            case AFTER_LAST:
                return last();

            default:
                throw new IllegalStateException( "Unexpected position " + position );
        }
    }
}
