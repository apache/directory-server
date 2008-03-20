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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeCursor;

import java.util.Comparator;


/**
 * Cursor over a set of values for the same key which are store in an in
 * memory AvlTree.  This Cursor is limited to the same key and it's tuples
 * will always return the same key.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KeySetTupleCursor<K,V> extends AbstractCursor<Tuple<K,V>>
{
    private final Comparator<V> comparator;
    private final AvlTreeCursor<V> wrapped;
    private final K key;

    private Tuple<K,V> returnedTuple = new Tuple<K,V>();
    private boolean valueAvailable;


    /**
     * Creates a Cursor over the tuples of an AvlTree.
     *
     * @param avlTree the AvlTree to build a Tuple returning Cursor over
     * @param key the constant key for which values are returned
     * @param comparator the Comparator used to determine <b>key</b> ordering
     */
    public KeySetTupleCursor( AvlTree<V> avlTree, K key, Comparator<V> comparator )
    {
        this.key = key;
        this.comparator = comparator;
        this.wrapped = new AvlTreeCursor<V>( avlTree );
    }


    private void clearValue()
    {
        returnedTuple.setKey( key );
        returnedTuple.setValue( null );
        valueAvailable = false;
    }


    public boolean available()
    {
        return valueAvailable;
    }


    /**
     * Positions this Cursor over the same keys before the value of the
     * supplied element Tuple.  The supplied element Tuple's key is not
     * considered at all.
     *
     * @param element the valueTuple who's value is used to position this Cursor
     * @throws Exception if there are failures to position the Cursor
     */
    public void before( Tuple<K,V> element ) throws Exception
    {
        wrapped.before( element.getValue() );
        clearValue();
    }


    public void after( Tuple<K,V> element ) throws Exception
    {
        wrapped.after( element.getValue() );

        /*
         * While the next value is less than or equal to the element keep
         * advancing forward to the next item.  If we cannot advance any
         * further then stop and return.  If we find a value greater than
         * the element then we stop, backup, and return so subsequent calls
         * to getNext() will return a value greater than the element.
         */
        while ( wrapped.next() )
        {
            V next = wrapped.get();

            int nextCompared = comparator.compare( next, element.getValue() );

            if ( nextCompared <= 0 )
            {
                // just continue
            }
            else if ( nextCompared > 0 )
            {
                /*
                 * If we just have values greater than the element argument
                 * then we are before the first element and cannot backup, and
                 * the call below to previous() will fail.  In this special
                 * case we just reset the Cursor's position and return.
                 */
                if ( wrapped.previous() )
                {
                }
                else
                {
                    wrapped.before( element.getValue() );
                }

                clearValue();
                return;
            }
        }

        clearValue();
    }


    public void beforeFirst() throws Exception
    {
        wrapped.beforeFirst();
        clearValue();
    }


    public void afterLast() throws Exception
    {
        wrapped.afterLast();
    }


    public boolean first() throws Exception
    {
        return wrapped.first();
    }


    public boolean last() throws Exception
    {
        return wrapped.last();
    }


    public boolean previous() throws Exception
    {
        if ( wrapped.previous() )
        {
            returnedTuple.setKey( key );
            returnedTuple.setValue( wrapped.get() );
            return valueAvailable = true;
        }
        else
        {
            clearValue();
            return false;
        }
    }


    public boolean next() throws Exception
    {
        if ( wrapped.next() )
        {
            returnedTuple.setKey( key );
            returnedTuple.setValue( wrapped.get() );
            return valueAvailable = true;
        }
        else
        {
            clearValue();
            return false;
        }
    }


    public Tuple<K,V> get() throws Exception
    {
        if ( valueAvailable )
        {
            return returnedTuple;
        }

        throw new InvalidCursorPositionException();
    }


    public boolean isElementReused()
    {
        return true;
    }
}