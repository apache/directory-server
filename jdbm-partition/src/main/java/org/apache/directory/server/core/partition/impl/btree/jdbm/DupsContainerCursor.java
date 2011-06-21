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


import java.io.IOException;

import jdbm.btree.BTree;
import jdbm.helper.TupleBrowser;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.AbstractTupleCursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;


/**
 * A cursor for browsing tables with duplicates which returns the container
 * for values rather than just the value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @param K The Key
 * @param V The associated value
 */
public class DupsContainerCursor<K,V> extends AbstractTupleCursor<K, DupsContainer<V>>
{
    /** The JDBM table we are building a cursor over */
    private final JdbmTable<K,V> table;

    /** A container to pass to the underlying JDBM to get back a tuple */
    private jdbm.helper.Tuple<K,V> jdbmTuple = new jdbm.helper.Tuple<K, V>();
    
    private Tuple<K,DupsContainer<V>> returnedTuple = new Tuple<K,DupsContainer<V>>();
    
    /** A browser over the JDBM Table */
    private TupleBrowser<K,V> browser;
    
    /** Tells if we have a tuple to return */
    private boolean valueAvailable;
    
    /** TODO : do we need this flag ??? */
    private Boolean forwardDirection;


    /**
     * Creates a Cursor over the tuples of a JDBM table.
     *
     * @param table the JDBM Table to build a Cursor over
     * @throws java.io.IOException of there are problems accessing the BTree or if this table
     * does not allow duplicate values
     */
    public DupsContainerCursor( JdbmTable<K,V> table ) throws IOException
    {
        if ( ! table.isDupsEnabled() )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_572 ) );
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


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return valueAvailable;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void beforeKey( K key ) throws Exception
    {
        checkNotClosed( "beforeKey()" );
        browser = ((BTree<K,V>)table.getBTree()).browse( key );
        forwardDirection = null;
        clearValue();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void afterKey( K key ) throws Exception
    {
        browser = ((BTree<K,V>)table.getBTree()).browse( key );
        forwardDirection = null;

        /*
         * While the next value is less than or equal to the element keep
         * advancing forward to the next item.  If we cannot advance any
         * further then stop and return.  If we find a value greater than
         * the element then we stop, backup, and return so subsequent calls
         * to getNext() will return a value greater than the element.
         */
        while ( browser.getNext( jdbmTuple ) )
        {
            checkNotClosed( "afterKey()" );
            K next = ( K ) jdbmTuple.getKey();

            int nextCompared = table.getKeyComparator().compare( next, key );

            if ( nextCompared > 0 )
            {
                browser.getPrevious( jdbmTuple );

                // switch in direction bug workaround: when a JDBM browser
                // switches direction with next then previous as is occurring
                // here then two previous moves are needed.
                browser.getPrevious( jdbmTuple );
                forwardDirection = false;
                clearValue();
                
                return;
            }
        }

        clearValue();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( K key, DupsContainer<V> value ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_573 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void afterValue( K key, DupsContainer<V> value ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_573 ) );
    }


    /**
     * Positions this Cursor before the key of the supplied tuple.
     *
     * @param element the tuple who's key is used to position this Cursor
     * @throws IOException if there are failures to position the Cursor
     */
    public void before( Tuple<K,DupsContainer<V>> element ) throws Exception
    {
        beforeKey( element.getKey() );
    }


    /**
     * {@inheritDoc}
     */
    public void after( Tuple<K,DupsContainer<V>> element ) throws Exception
    {
        afterKey( element.getKey() );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        
        browser = table.getBTree().browse();
        forwardDirection = null;
        clearValue();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        
        browser = table.getBTree().browse( null );
        forwardDirection = null;
        clearValue();
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        beforeFirst();
        
        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        afterLast();
        
        return previous();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );
        
        if ( browser == null )
        {
            afterLast();
        }

        boolean advanceSuccess = browser.getPrevious( jdbmTuple );

        // only want to set this if the advance is a success which means we
        // are not at front
        if ( ( forwardDirection == null ) && advanceSuccess )
        {
            forwardDirection = false;
        }

        if ( ( forwardDirection != null ) && forwardDirection )
        {
            advanceSuccess = browser.getPrevious( jdbmTuple );
            forwardDirection = false;
        }

        if ( advanceSuccess )
        {
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


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        
        if ( browser == null )
        {
            // The tuple browser is not initialized : set it to the beginning of the cursor
            beforeFirst();
        }

        // Check if we can move forward and grab a tuple
        boolean advanceSuccess = browser.getNext( jdbmTuple );

        // only want to set this if the advance is a success which means
        // we are not at end
        if ( forwardDirection == null )
        {
            if ( advanceSuccess )
            {
                forwardDirection = true;
            }
            else
            {
                clearValue();
                
                // No value available
                return false;
            }
        }

        if ( ! forwardDirection )
        {
            advanceSuccess = browser.getNext( jdbmTuple );
            forwardDirection = true;
        }

        valueAvailable = advanceSuccess;

        if ( advanceSuccess )
        {
            // create the fetched tuple containing the key and the deserialized value
            returnedTuple.setKey( jdbmTuple.getKey() );
            returnedTuple.setValue( table.getDupsContainer( ( byte[] ) jdbmTuple.getValue() ) );
        }

        return valueAvailable;
    }


    /**
     * {@inheritDoc}
     */
    public Tuple<K,DupsContainer<V>> get() throws Exception
    {
        checkNotClosed( "get()" );
        
        if ( valueAvailable )
        {
            return returnedTuple;
        }

        throw new InvalidCursorPositionException();
    }
}
