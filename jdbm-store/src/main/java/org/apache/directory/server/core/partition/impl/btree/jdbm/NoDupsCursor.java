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

import java.io.IOException;

import jdbm.helper.TupleBrowser;


/**
 * Cursor over the Tuples of a JDBM BTree.  Duplicate keys are not supported
 * by JDBM natively so you will not see duplicate keys.  For this reason as
 * well before() and after() positioning only considers the key of the Tuple
 * arguments provided.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class NoDupsCursor<K,V> extends AbstractCursor<Tuple<K,V>>
{
    private final JdbmTable<K,V> table;

    private jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
    private Tuple<K,V> returnedTuple = new Tuple<K,V>();
    private TupleBrowser browser;
    private boolean valueAvailable;


    /**
     * Creates a Cursor over the tuples of a JDBM table.
     *
     * @param table the JDBM Table to build a Cursor over
     * @throws IOException of there are problems accessing the BTree
     */
    public NoDupsCursor( JdbmTable<K,V> table ) throws IOException
    {
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
    public void before( Tuple<K,V> element ) throws IOException
    {
        browser = table.getBTree().browse( element.getKey() );
        clearValue();
    }


    public void after( Tuple<K,V> element ) throws IOException
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

            //noinspection unchecked
            int nextCompared = table.getKeyComparator().compare( next, element.getKey() );

            if ( nextCompared <= 0 )
            {
                // just continue
            }
            else if ( nextCompared > 0 )
            {
                /*
                 * If we just have values greater than the element argument
                 * then we are before the first element and cannot backup, and
                 * the call below to getPrevious() will fail.  In this special
                 * case we just reset the Cursor's browser and return.
                 */
                if ( browser.getPrevious( jdbmTuple ) )
                {
                }
                else
                {
                    browser = table.getBTree().browse( element.getKey() );
                }

                clearValue();
                return;
            }
        }

        clearValue();
        // just return
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

        if ( browser.getPrevious( jdbmTuple ) )
        {
            //noinspection unchecked
            returnedTuple.setKey( ( K ) jdbmTuple.getKey() );
            //noinspection unchecked
            returnedTuple.setValue( ( V ) jdbmTuple.getValue() );
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

        if ( browser.getNext( jdbmTuple ) )
        {
            //noinspection unchecked
            returnedTuple.setKey( ( K ) jdbmTuple.getKey() );
            //noinspection unchecked
            returnedTuple.setValue( ( V ) jdbmTuple.getValue() );
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
            //noinspection unchecked
            return returnedTuple;
        }

        throw new InvalidCursorPositionException();
    }


    public boolean isElementReused()
    {
        return true;
    }
}
