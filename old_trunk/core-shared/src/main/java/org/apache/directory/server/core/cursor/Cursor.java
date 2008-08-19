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

import java.io.IOException;


/**
 * A simple cursor concept for bidirectionally enumerating over elements.
 * Cursors unlike iterators request to advance to an element by calling next()
 * or previous() which returns true or false if the request succeeds.  Other
 * operations for relative and absolute advances are provided.  If the cursor
 * does not advance, then the Cursor is either positioned before the first
 * element or after the last element in which case the user of the Cursor must
 * stop advancing in the respective direction.  If an advance succeeds a get()
 * operation retreives the current object at the Cursors position.
 *
 * Although this interface presumes Cursors can advance bidirectionally, one
 * or more either direction may not be supported.  In this case
 * implementations should throw UnsupportedOperationExceptions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Cursor<E>
{
    /**
     * Positions this Curser before the first element.
     *
     * @throws IOException if there are problems positioning this cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    void beforeFirst() throws IOException;


    /**
     * Positions this Curser after the last element.
     *
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    void afterLast() throws IOException;


    /**
     * Positions this Curser at the nth element.  Zero based indexing is used.
     *
     * If the specified position is past the first or last element, the Cursor
     * is positioned before the first or after the last element respectively.
     *
     * @param absolutePosition the absolute position to move this Cursor to
     * @return true if the position has been successfully changed to the
     * element at the specified position, false otherwise
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean absolute( int absolutePosition ) throws IOException;


    /**
     * Positions this Curser n places relative to the present position.  Zero
     * based indexing is used and negative index values may be provided for
     * representing the direction.
     *
     * If the specified position is past the first or last element, the Cursor
     * is positioned before the first or after the last element respectively.
     *
     * @param relativePosition the relative position to move this Cursor to
     * @return true if the position has been successfully changed to the
     * element relative to the current position, false otherwise
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean relative( int relativePosition ) throws IOException;


    /**
     * Positions this Curser at the first element.
     *
     * @return true if the position has been successfully changed to the first
     * element, false otherwise
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean first() throws IOException;


    /**
     * Positions this Curser at the last element.
     *
     * @return true if the position has been successfully changed to the last
     * element, false otherwise
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean last() throws IOException;


    /**
     * Checks if this Curser is positioned at the first element.
     *
     * @return true if the current position is at the first element, false
     * otherwise
     * @throws IOException if there are problems determining this Cursor's
     * position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean isFirst() throws IOException;


    /**
     * Checks if this Curser is positioned at the last element.
     *
     * @return true if the current position is at the last element, false
     * otherwise
     * @throws IOException if there are problems determining this Cursor's
     * position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean isLast() throws IOException;


    /**
     * Checks if this Curser is positioned after the last element.
     *
     * @return true if the current position is after the last element, false
     * otherwise
     * @throws IOException if there are problems determining this Cursor's
     * position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean isAfterLast() throws IOException;


    /**
     * Checks if this Curser is positioned before the first element.
     *
     * @return true if the current position is before the first element, false
     * otherwise
     * @throws IOException if there are problems determining this Cursor's
     * position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean isBeforeFirst() throws IOException;


    /**
     * Checks if this Curser is closed.  Calls to this operation should not
     * fail with exceptions if and only if the cursor is in the closed state.
     *
     * @return true if this Cursor is closed, false otherwise
     * @throws IOException if there are problems determining the cursor's closed state
     * @throws UnsupportedOperationException if this operation is not supported
     */
    boolean isClosed() throws IOException;


    /**
     * Advances this Cursor to the previous position.
     *
     * @return true if the advance succeeded, false otherwise
     * @throws IOException if there are problems advancing to the next position
     * @throws UnsupportedOperationException if advances in this direction are not supported
     */
    boolean previous() throws IOException;


    /**
     * Advances this Cursor to the next position.
     *
     * @return true if the advance succeeded, false otherwise
     * @throws IOException if there are problems advancing to this Cursor to
     * the next position, or if this Cursor is closed
     * @throws UnsupportedOperationException if advances in this direction are not supported
     */
    boolean next() throws IOException;


    /**
     * Gets the object at the current position.  Cursor implementations may
     * choose to reuse element objects by re-populating them on advances
     * instead of creating new objects on each advance.
     *
     * @return the object at the current position
     * @throws IOException if the object at this Cursor's current position
     * cannot be retrieved, or if this Cursor is closed
     */
    E get() throws IOException;


    /**
     * Gets whether or not this Cursor will return the same element object
     * instance on get() operations for any position of this Cursor.  Some
     * Cursor implementations may reuse the same element copying values into
     * it for every position rather than creating and emiting new element
     * objects on each advance.  Some Cursor implementations may return
     * different elements for each position yet the same element instance
     * is returned for the same position. In these cases this method should
     * return true.
     *
     * @return true if elements are reused by this Cursor
     */
    boolean isElementReused();


    /**
     * Closes this Cursor and frees any resources it my have allocated.
     * Repeated calls to this method after this Cursor has already been
     * called should not fail with exceptions.
     *
     * @throws IOException if this Cursor cannot be closed
     */
    void close() throws IOException;
}
