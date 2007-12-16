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
 * A Cursor for bidirectional traversal over elements in a dataset. Cursors
 * unlike Iterators or Enumerations may advance to an element by calling
 * next() or previous() which returns true or false if the request succeeds
 * with a viable element at the new position.  Operations for relative
 * positioning in larger increments are provided.  If the cursor can not
 * advance, then the Cursor is either positioned before the first element or
 * after the last element in which case the user of the Cursor must stop
 * advancing in the respective direction.  If an advance succeeds a get()
 * operation retreives the current object at the Cursors position.
 *
 * Although this interface presumes Cursors can advance bidirectionally,
 * implementations may restrict this by throwing
 * UnsupportedOperationExceptions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Cursor<E>
{
    /**
     * Prepares this Cursor, so a subsequent call to Cursor#next() with a
     * true return value, will have positioned the Cursor on a dataset element
     * equal to or greater than the element argument but not less.  A call to
     * Cursor#previous() with a true return value will position the Cursor on
     * a dataset element less than the argument.  If Cursor#next() returns
     * false then the Cursor is past the last element and so all values in the
     * dataset are less than the argument.  If Cursor#previous() returns false
     * then the Cursor is positioned before the first element and all elements
     * in the dataset are greater than the argument.
     *
     * @param element the element to be positioned before
     * @throws IOException with problems accessing the underlying btree
     * @throws UnsupportedOperationException if this method is not supported
     */
    void before( E element ) throws IOException;


    /**
     * Prepares this Cursor, so a subsequent call to Cursor#previous() with a
     * true return value, will have positioned the Cursor on a dataset element
     * equal to or less than the element argument but not greater. A call to
     * Cursor#next() with a true return value will position the Cursor on a
     * dataset element greater than the argument.  If Cursor#next() returns
     * false then the Cursor is past the last element and so all values in the
     * dataset are less than or equal to the argument.  If Cursor#previous()
     * returns false then the Cursor is positioned before the first element
     * and all elements in the dataset are greater than the argument.
     *
     * @param element the element to be positioned after
     * @throws IOException if there are problems positioning this cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    void after( E element ) throws IOException;


    /**
     * Positions this Curser before the first element.
     *
     * @throws IOException if there are problems positioning this cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    void beforeFirst() throws IOException;


    /**
     * Positions this Curser after the last element.
     *
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    void afterLast() throws IOException;


    /**
     * Positions this Curser an offset number of elements before or after the
     * current position.  Negative offsets are used to move the Cursor's
     * position back, while positive offsets are used to advance the Cursor's
     * position forward.
     *
     * If the specified position is past the first or last element, the Cursor
     * is positioned before the first or after the last element respectively.
     *
     * Note that this is not the most efficient means to advance a Cursor over
     * a BTree.  The best mechanism is to advance by using a specific key via
     * the Cursor#before() and Cursor#after() methods.  Most implementations
     * of this method will have to walk through BTree records.
     *
     * @param offset the relative offset to move this Cursor to
     * @return true if the position has been successfully changed to an
     * existing element retreivable by Cursor#get() at the new relative
     * position, of this Cursor, otherwise false
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean relative( int offset ) throws IOException;


    /**
     * Positions this Curser at the first element.
     *
     * @return true if the position has been successfully changed to the first
     * element, false otherwise
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean first() throws IOException;


    /**
     * Positions this Curser at the last element.
     *
     * @return true if the position has been successfully changed to the last
     * element, false otherwise
     * @throws IOException if there are problems positioning this Cursor or if
     * this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean last() throws IOException;


    /**
     * Checks if this Curser is positioned at the first element.
     *
     * @return true if the current position is at the first element, false
     * otherwise
     * @throws IOException if there are problems determining this Cursor's
     * position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean isFirst() throws IOException;


    /**
     * Checks if this Curser is positioned at the last element.
     *
     * @return true if the current position is at the last element, false
     * otherwise
     * @throws IOException if there are problems determining this Cursor's
     * position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean isLast() throws IOException;


    /**
     * Checks if this Curser is positioned after the last element.
     *
     * @return true if the current position is after the last element, false
     * otherwise
     * @throws IOException if there are problems determining this Cursor's
     * position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean isAfterLast() throws IOException;


    /**
     * Checks if this Curser is positioned before the first element.
     *
     * @return true if the current position is before the first element, false
     * otherwise
     * @throws IOException if there are problems determining this Cursor's
     * position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean isBeforeFirst() throws IOException;


    /**
     * Checks if this Curser is closed.  Calls to this operation should not
     * fail with exceptions if and only if the cursor is in the closed state.
     *
     * @return true if this Cursor is closed, false otherwise
     * @throws IOException if there are problems determining the cursor's closed state
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean isClosed() throws IOException;


    /**
     * Advances this Cursor to the previous position.
     *
     * @return true if the advance succeeded, false otherwise
     * @throws IOException if there are problems advancing to the next position
     * @throws UnsupportedOperationException if this method is not supported
     */
    boolean previous() throws IOException;


    /**
     * Advances this Cursor to the next position.
     *
     * @return true if the advance succeeded, false otherwise
     * @throws IOException if there are problems advancing to this Cursor to
     * the next position, or if this Cursor is closed
     * @throws UnsupportedOperationException if this method is not supported
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
     * @throws IOException if for some reason this Cursor could not be closed
     */
    void close() throws IOException;
}
