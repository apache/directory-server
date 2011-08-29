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


import java.util.Comparator;

import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;


/**
 * Cursor over the keys of a JDBM BTree.  Obviously does not return duplicate
 * keys since JDBM does not natively support multiple values for the same key.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KeyBTreeCursor<E> extends AbstractCursor<E>
{
    private final Tuple tuple = new Tuple();

    private final BTree btree;
    private final Comparator<E> comparator;
    private boolean valueAvailable;
    private TupleBrowser browser;


    /**
     * Creates a Cursor over the keys of a JDBM BTree.
     *
     * @param btree the JDBM BTree to build a Cursor over
     * @param comparator the Comparator used to determine key ordering
     * @throws Exception of there are problems accessing the BTree
     */
    public KeyBTreeCursor( BTree btree, Comparator<E> comparator ) throws Exception
    {
        this.btree = btree;
        this.comparator = comparator;
    }


    private void clearValue()
    {
        tuple.setKey( null );
        tuple.setValue( null );
        valueAvailable = false;
    }


    public boolean available()
    {
        return valueAvailable;
    }


    public void before( E element ) throws Exception
    {
        checkNotClosed( "before()" );
        this.closeBrowser( browser );
        browser = btree.browse( element );
        clearValue();
    }


    @SuppressWarnings("unchecked")
    public void after( E element ) throws Exception
    {
        this.closeBrowser( browser );
        browser = btree.browse( element );

        /*
         * While the next value is less than or equal to the element keep
         * advancing forward to the next item.  If we cannot advance any
         * further then stop and return.  If we find a value greater than
         * the element then we stop, backup, and return so subsequent calls
         * to getNext() will return a value greater than the element.
         */
        while ( browser.getNext( tuple ) )
        {
            checkNotClosed( "after()" );
            E next = ( E ) tuple.getKey();
            int nextCompared = comparator.compare( next, element );

            if ( nextCompared <= 0 )
            {
                // just continue
            }
            else 
            {
                /*
                 * If we just have values greater than the element argument
                 * then we are before the first element and must backup to
                 * before the first element state for the JDBM browser which 
                 * apparently the browser supports.
                 */
                browser.getPrevious( tuple );
                clearValue();
                return;
            }
        }

        clearValue();
        // just return
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        this.closeBrowser( browser );
        browser = btree.browse();
        clearValue();
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        this.closeBrowser( browser );
        browser = btree.browse( null );
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        return next();
    }


    public boolean last() throws Exception
    {
        afterLast();
        return previous();
    }


    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );
        
        if ( browser == null )
        {
            browser = btree.browse( null );
        }

        if ( browser.getPrevious( tuple ) )
        {
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
        checkNotClosed( "next()" );
        
        if ( browser == null )
        {
            browser = btree.browse();
        }

        if ( browser.getNext( tuple ) )
        {
            return valueAvailable = true;
        }
        else
        {
            clearValue();
            
            return false;
        }
    }


    @SuppressWarnings("unchecked")
    public E get() throws Exception
    {
        checkNotClosed( "get()" );
        
        if ( valueAvailable )
        {
            return ( E ) tuple.getKey();
        }

        throw new InvalidCursorPositionException();
    }
 
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception
    {
        super.close();
        this.closeBrowser( browser );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws Exception
    {
        super.close( cause );
        this.closeBrowser( browser );
    }
    
    
    private void closeBrowser(TupleBrowser browser)
    {
        if ( browser != null )
        {
            browser.close();
        }
    }
}
