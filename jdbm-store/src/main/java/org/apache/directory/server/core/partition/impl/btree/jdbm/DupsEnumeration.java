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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import jdbm.btree.BTree;

import org.apache.directory.server.core.partition.impl.btree.NoDupsEnumeration;
import org.apache.directory.server.core.partition.impl.btree.Tuple;


/**
 * NamingEnumeration that enumerates over duplicate values nested into a value 
 * using a TreeSet.
 *
 * @warning The Tuple returned by this listing is always the same instance 
 * object returned every time. It is reused to for the sake of efficency rather 
 * than creating a new tuple for each next() call.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DupsEnumeration implements NamingEnumeration
{
    /** Marker for whether or not next() will return successfully */
    private boolean hasMore = true;
    /** The Tuple to return */
    private final Tuple returned = new Tuple();
    /** The Tuple to store prefetched values with */
    private final Tuple prefetched = new Tuple();
    /** The underlying no duplicates enumeration this enum expands out */
    private final NoDupsEnumeration underlying;

    /** 
     * The current Tuple returned from the underlying NoDupsEnumeration which
     * contains TreeSets for Tuple values.  A NoDupsEnumeration on a Table that
     * allows duplicates essentially returns Strings for keys and TreeSets for 
     * their values.
     */
    private Tuple duplicates;
    /** 
     * The iterator over a set of Tuple values with the same key.  Basically
     * iterates over the TreeSet values in the duplicates Tuple. 
     */
    private Iterator dupIterator;

    private JdbmTable table;

    
    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Creates a DupsEnumeration over a enumeration of Tuples holding TreeSets
     * for values that have the same key.
     *
     * @param list the underlying enumeration
     * @throws NamingException if there is a problem
     */
    public DupsEnumeration( JdbmTable table, NoDupsEnumeration list ) throws NamingException
    {
        this.table = table;
        underlying = list;

        // Protect against closed cursors
        if ( !underlying.hasMore() )
        {
            close();
            return;
        }

        prefetch();
    }


    // ------------------------------------------------------------------------
    // NamingEnumeration Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Returns the same Tuple every time but with different key/value pairs.
     * 
     * @see javax.naming.NamingEnumeration#next()
     */
    public Object next() throws NamingException
    {
        returned.setKey( prefetched.getKey() );
        returned.setValue( prefetched.getValue() );

        prefetch();

        return returned;
    }


    /**
     * Returns the same Tuple every time but with different key/value pairs.
     * 
     * @see java.util.Enumeration#nextElement()
     */
    public Object nextElement()
    {
        try
        {
            return next();
        }
        catch ( NamingException ne )
        {
            throw new NoSuchElementException();
        }
    }


    /**
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore()
    {
        return hasMore;
    }


    /**
     * Calls hasMore.
     *
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return hasMore;
    }


    /**
     * Closes the underlying NamingEnumeration
     *
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close()
    {
        hasMore = false;
        underlying.close();
    }


    // ------------------------------------------------------------------------
    // Private/Package Friendly Methods
    // ------------------------------------------------------------------------

    /**
     * Prefetches values into the prefetched Tuple taking into account that 
     * the returned Tuple values of the underlying enumeration list are really
     * TreeSets that hold multiple sorted values for the same key.  
     * 
     * <p> The values prefetched into the prefetched Tuple are actual values 
     * taken from the TreeSet.  So this NamingEnumeration simply expands out 
     * duplicate keyed Tuples which it returns.  iterator is an iteration over
     * the values held in the TreeSet returned by the underlying enumeration.  
     * The values pulled off of this iterator are put into prefetched. 
     * </p>
     */
    @SuppressWarnings("unchecked")
    private void prefetch() throws NamingException
    {
        /*
         * If the iterator over the values of the current key is null or is 
         * extinguished then we need to advance to the next key.
         */
        while ( null == dupIterator || !dupIterator.hasNext() )
        {
            /*
             * If the underlying enumeration has more elements we get the next
             * key/TreeSet Tuple to work with and get an iterator over it. 
             */
            if ( underlying.hasMore() )
            {
                duplicates = ( Tuple ) underlying.next();
                
                Object values = duplicates.getValue();
                
                if ( values instanceof TreeSet )
                {
                    TreeSet set = ( TreeSet ) duplicates.getValue();
    
                    if ( underlying.doAscendingScan() )
                    {
                        dupIterator = set.iterator();
                    }
                    else
                    {
                        /*
                         * Need to reverse the list and iterate over the reversed
                         * list.  
                         * 
                         * TODO This can be optimized by using a ReverseIterator 
                         * over the array list.  I don't think there is a way to 
                         * do this on the TreeSet.
                         */
                        List list = new ArrayList( set.size() );
                        list.addAll( set );
                        Collections.reverse( list );
                        dupIterator = list.iterator();
                    }
                }
                else if ( values instanceof BTreeRedirect )
                {
                    BTree tree = table.getBTree( ( BTreeRedirect ) values );
                    dupIterator = new BTreeIterator( tree, underlying.doAscendingScan() );
                }
            }
            else
            {
                close();
                return;
            }
        }

        /*
         * If we get to this point then iterator has more elements and 
         * duplicates holds the Tuple containing the key and TreeSet of 
         * values for that key which the iterator iterates over.  All we
         * need to do is populate the prefetched Tuple with the key and the
         * next value in the iterator.
         */
        prefetched.setKey( duplicates.getKey() );
        prefetched.setValue( dupIterator.next() );
    }
}
