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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.xdbm.Tuple;
import jdbm.helper.TupleBrowser;

import java.io.IOException;


/**
 * A cursor for browsing tables with duplicates which returns the container
 * for values rather than just the value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class DupsContainerCursor<K,V> extends AbstractCursor<Tuple<K, DupsContainer<V>>>
{
    private final JdbmTable<K,V> table;

    private jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
    private Tuple<K,DupsContainer<V>> returnedTuple = new Tuple<K,DupsContainer<V>>();
    private TupleBrowser browser;
    private boolean valueAvailable;
    private Boolean forwardDirection;


    /**
     * Creates a Cursor over the tuples of a JDBM table.
     *
     * @param table the JDBM Table to build a Cursor over
     * @throws java.io.IOException of there are problems accessing the BTree
     */
    public DupsContainerCursor( JdbmTable<K,V> table ) throws IOException
    {
        if ( ! table.isDupsEnabled() )
        {
            throw new IllegalStateException(
                "This cursor can only be used with tables that have duplicate keys enabled." );
        }

        this.table = table;
    }


    private void clearValue()
    {
        returnedTuple.setKey( null );
        returnedTuple.setValue( null );
        jdbmTuple.setKey( null );
        jdbmTuple.setValue( null );
        valueAvailable = false;
    }


    public boolean available()
    {
        return valueAvailable;
    }


    /**
     * Positions this Cursor before the key of the supplied tuple.
     *
     * @param element the tuple who's key is used to position this Cursor
     * @throws IOException if there are failures to position the Cursor
     */
    public void before( Tuple<K,DupsContainer<V>> element ) throws IOException
    {
        browser = table.getBTree().browse( element.getKey() );
        clearValue();
    }


    public void after( Tuple<K,DupsContainer<V>> element ) throws IOException
    {
        browser = table.getBTree().browse( element.getKey() );

        /*
         * While the next value is less than or equal to the element keep
         * advancing forward to the next item.  If we cannot advance any
         * further then stop and return.  If we find a value greater than
         * the element then we stop, backup, and return so subsequent calls
         * to getNext() will return a value greater than the element.
         */
        while ( browser.getNext( jdbmTuple ) )
        {
            //noinspection unchecked
            K next = ( K ) jdbmTuple.getKey();

            int nextCompared = table.getKeyComparator().compare( next, element.getKey() );

            if ( nextCompared > 0 )
            {
                browser.getPrevious( jdbmTuple );

                // switch in direction bug workaround: when a JDBM browser
                // switches direction with next then previous as is occuring
                // here then two previous moves are needed.
                browser.getPrevious( jdbmTuple );
                forwardDirection = false;
                clearValue();
                return;
            }
        }

        clearValue();
    }


    public void beforeFirst() throws IOException
    {
        browser = table.getBTree().browse();
        clearValue();
    }


    public void afterLast() throws IOException
    {
        browser = table.getBTree().browse( null );
        clearValue();
    }


    public boolean first() throws IOException
    {
        beforeFirst();
        return next();
    }


    public boolean last() throws IOException
    {
        afterLast();
        return previous();
    }


    public boolean previous() throws IOException
    {
        if ( browser == null )
        {
            afterLast();
        }

        boolean advanceSuccess = browser.getPrevious( jdbmTuple );

        if ( forwardDirection == null )
        {
            forwardDirection = false;
        }

        if ( forwardDirection )
        {
            advanceSuccess = browser.getPrevious( jdbmTuple );
            forwardDirection = false;
        }

        if ( advanceSuccess )
        {
            //noinspection unchecked
            returnedTuple.setKey( ( K ) jdbmTuple.getKey() );
            returnedTuple.setValue( table.getDupsContainer( ( byte[] ) jdbmTuple.getValue() ) );
            return valueAvailable = true;
        }
        else
        {
            clearValue();
            return false;
        }
    }


    public boolean next() throws IOException
    {
        if ( browser == null )
        {
            beforeFirst();
        }

        boolean advanceSuccess = browser.getNext( jdbmTuple );

        if ( forwardDirection == null )
        {
            forwardDirection = true;
        }

        if ( ! forwardDirection )
        {
            advanceSuccess = browser.getNext( jdbmTuple );
            forwardDirection = true;
        }

        if ( advanceSuccess )
        {
            //noinspection unchecked
            returnedTuple.setKey( ( K ) jdbmTuple.getKey() );
            returnedTuple.setValue( table.getDupsContainer( ( byte[] ) jdbmTuple.getValue() ) );
            return valueAvailable = true;
        }
        else
        {
            clearValue();
            return false;
        }
    }


    public Tuple<K,DupsContainer<V>> get() throws Exception
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
