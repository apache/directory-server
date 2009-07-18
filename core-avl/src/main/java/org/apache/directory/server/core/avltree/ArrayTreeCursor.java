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

import org.apache.directory.shared.ldap.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.cursor.InvalidCursorPositionException;




/**
 * A Cursor for an ArrayTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ArrayTreeCursor<K> extends AbstractCursor<K>
{
    /** The underlying ArrayTree */
    private ArrayTree<K> array;
    
    /** The current node */
    private K node;
    
    /** A flag set to true if we are pointing to a node */
    private boolean onNode = false;
    
    /** A flag to tell if we are after the last node */
    private boolean isAfterLast = false;

    /** A flag to tell if we are before the first node */
    private boolean isBeforeFirst = true;
    
    /** The current position in the array */
    private int current;
 
    
    /**
     * Create a cursor on an ArrayTree
     * @param array The array we want a cursor for
     */
    public ArrayTreeCursor( ArrayTree<K> array )
    {
        this.array = array;
        current = -1;
    }

    
    /**
     * {@inheritDoc}
     */
    public void after( K element ) throws Exception 
    {
        checkNotClosed( "after" );

        if ( element == null )
        {
            afterLast();
            return;
        }

        int found = array.getAfterPosition( element );
        
        if ( found == -1 )
        {
            // As the element has not been found, we move after the last
            // position
            afterLast();
            return;
        }

        // The element has been found, we have to pick the node,
        // set the current position, and update the flags.
        current = found;
        //node = array.get( current );
        isAfterLast = false;
        isBeforeFirst = false;
        onNode = false;
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception 
    {
        checkNotClosed( "afterLast" );
        
        current = array.size() - 1;
        node = array.getLast();
        isBeforeFirst = false;
        isAfterLast = true;
        onNode = false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return onNode;
    }


    /**
     * {@inheritDoc}
     */
    public void before( K element ) throws Exception 
    {
        checkNotClosed( "before" );

        if ( element == null )
        {
            beforeFirst();
            return;
        }

        int found = array.getBeforePosition( element );

        // If the element has not been found, move to the
        // first position
        if ( found < 0 )
        {
            beforeFirst();
            return;
        }
        
        current = found;
        isAfterLast = false;
        isBeforeFirst = false;
        onNode = true;
        node = array.get( current );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception 
    {
        checkNotClosed( "beforeFirst" );
        
        current = 0;
        node = array.getFirst();
        isBeforeFirst = true;
        isAfterLast = false;
        onNode = false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception 
    {
        checkNotClosed( "first" );
        
        current = 0;
        node = array.getFirst();
        isBeforeFirst = false;
        isAfterLast = false;
        return onNode = node != null;
    }


    /**
     * {@inheritDoc}
     */
    public K get() throws Exception 
    {
        checkNotClosed( "get" );
        
        if ( onNode )
        {
            return node;
        }
        
        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isElementReused()
    {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception 
    {
        checkNotClosed( "last" );

        current = array.size() - 1;
        node = array.getLast();
        isBeforeFirst = false;
        isAfterLast = false;
        return onNode = node != null;
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
        
        // If we are at the beginning
        if ( isBeforeFirst ) 
        {
            current = 0;
            node = array.getFirst();
            isBeforeFirst = false;
            isAfterLast = false;
            onNode = node != null;
            return onNode;
        }
        
        if ( isAfterLast )
        {
            return false;
        }

        if ( onNode )
        {
            current++;
            
            if ( current == array.size() )
            {
                afterLast();
                return false;
            }
        }
        
        node = array.get( current );
        onNode = node != null;
        
        return onNode;
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
        
        if ( isBeforeFirst )
        {
            return false;
        }

        if ( isAfterLast )
        {
            current = array.size() - 1;
            node = array.getLast();
            isBeforeFirst = false;
            isAfterLast = false;
            return onNode = node != null;
        }

        if ( onNode )
        {
            current--;
    
            if ( current < 0 )
            {
                beforeFirst();
                return false;
            }
        }
        
        node = array.get( current );
        onNode = node != null;
        
        return onNode;
    }
}
