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


import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.Serializer;
import jdbm.helper.TupleBrowser;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.partition.impl.btree.*;
import org.apache.directory.server.schema.SerializableComparator;

import java.io.IOException;
import java.util.*;


/**
 * A jdbm Btree wrapper that enables duplicate sorted keys using collections.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmTable<K,V> implements Table<K,V>
{
    private static final byte[] EMPTY_BYTES = new byte[0];

    /** the key to store and retreive the count information */
    private static final String SZSUFFIX = "_btree_sz";

    /** the name of this table */
    private final String name;
    /** the JDBM record manager for the file this table is managed in */
    private final RecordManager recMan;
    /** whether or not this table allows for duplicates */
    private final boolean allowsDuplicates;
    /** a pair of comparators for the keys and values in this Table */
    private final TupleComparator<K,V> comparator;

    /** the current count of entries in this Table */
    private int count;
    /** the underlying JDBM btree used in this Table */
    private BTree bt;


    /** the renderer to use for btree tuples */
    private TupleRenderer renderer;
    /** the limit at which we start using btree redirection for duplicates */
    private int numDupLimit = JdbmIndex.DEFAULT_DUPLICATE_LIMIT;
    /** @TODO should really be a cache of duplicate BTrees */
    private Map<Long, BTree> duplicateBtrees = new HashMap<Long, BTree>();
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R
    // ------------------------------------------------------------------------

    
    /**
     * Creates a Jdbm BTree based tuple Table abstraction that enables 
     * duplicates.
     *
     * @param name the name of the table
     * @param allowsDuplicates whether or not duplicates are enabled 
     * @param numDupLimit the size limit of duplicates before switching to
     * BTrees for values instead of TreeSets
     * @param manager the record manager to be used for this table
     * @param comparator a tuple comparator
     * @param keySerializer a serializer to use for the keys instead of using
     * default Java serialization which could be very expensive
     * @param valueSerializer a serializer to use for the values instead of
     * using default Java serialization which could be very expensive
     * @throws IOException if the table's file cannot be created
     */
    public JdbmTable( String name, boolean allowsDuplicates, int numDupLimit,
        RecordManager manager, TupleComparator<K,V> comparator, Serializer keySerializer, 
        Serializer valueSerializer )
        throws IOException
    {
        /*System.out.println( "Creating BTree for " + name + ", key serializer = " + 
            (keySerializer == null ? "null" : keySerializer.getClass().getName()) +
            ", valueSerializer = " + 
            (valueSerializer == null ? "null" : valueSerializer.getClass().getName()) );*/
        this.numDupLimit = numDupLimit;
        this.name = name;
        this.recMan = manager;
        this.comparator = comparator;
        this.allowsDuplicates = allowsDuplicates;

        long recId = recMan.getNamedObject( name );

        //
        // Load existing BTree
        //

        if ( recId != 0 )
        {
            bt = BTree.load( recMan, recId );
            recId = recMan.getNamedObject( name + SZSUFFIX );
            count = ( Integer ) recMan.fetch( recId );
        }
        else
        {
            bt = BTree.createInstance( recMan, comparator.getKeyComparator(), keySerializer, valueSerializer );
            recId = bt.getRecid();
            recMan.setNamedObject( name, recId );
            recId = recMan.insert( 0 );
            recMan.setNamedObject( name + SZSUFFIX, recId );
        }
    }


    /**
     * Creates a Jdbm BTree based tuple Table abstraction without duplicates 
     * enabled using a simple key comparator.
     *
     * @param name the name of the table
     * @param manager the record manager to be used for this table
     * @param keyComparator a tuple comparator
     * @param keySerializer a serializer to use for the keys instead of using
     * default Java serialization which could be very expensive
     * @param valueSerializer a serializer to use for the values instead of
     * using default Java serialization which could be very expensive
     * @throws IOException if the table's file cannot be created
     */
    public JdbmTable( String name, RecordManager manager, SerializableComparator<K> keyComparator,
                      Serializer keySerializer, Serializer valueSerializer )
        throws IOException
    {
        this( name, false, Integer.MAX_VALUE, manager, new KeyOnlyComparator<K,V>( keyComparator ),
                keySerializer, valueSerializer );
    }


    // ------------------------------------------------------------------------
    // Simple Table Properties
    // ------------------------------------------------------------------------

    
    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#getComparator()
     */
    public TupleComparator<K,V> getComparator()
    {
        return comparator;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#isDupsEnabled()
     */
    public boolean isDupsEnabled()
    {
        return allowsDuplicates;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#getName()
     */
    public String getName()
    {
        return name;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#getRenderer()
     */
    public TupleRenderer getRenderer()
    {
        return renderer;
    }


    /**
     * @see Table#setRenderer(TupleRenderer)
     */
    public void setRenderer( TupleRenderer renderer )
    {
        this.renderer = renderer;
    }

    
    public boolean isCountExact()
    {
        return false;
    }
        

    // ------------------------------------------------------------------------
    // Count Overloads
    // ------------------------------------------------------------------------

    
    /**
     * @see Table#greaterThanCount(Object)
     */
    public int greaterThanCount( K key ) throws IOException
    {
        // take a best guess
        return count;
    }
    
    
    /**
     * @see Table#lessThanCount(Object)
     */
    public int lessThanCount( K key ) throws IOException
    {
        // take a best guess
        return count;
    }


    /**
     * @see Table#count(java.lang.Object)
     */
    public int count( K key ) throws IOException
    {
        if ( !allowsDuplicates )
        {
            if ( null == getRaw( key ) )
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }

        Object values = getRaw( key );
        
        if ( values == null )
        {
            return 0;
        }
        
        // -------------------------------------------------------------------
        // Handle the use of a TreeSet for storing duplicates
        // -------------------------------------------------------------------

        if ( values instanceof TreeSet )
        {
            return ( ( TreeSet ) values ).size();
        }

        // -------------------------------------------------------------------
        // Handle the use of a BTree for storing duplicates
        // -------------------------------------------------------------------

        if ( values instanceof BTreeRedirect )
        {
            return getBTree( ( BTreeRedirect ) values ).size();
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#count()
     */
    public int count() throws IOException
    {
        return count;
    }

    
    // ------------------------------------------------------------------------
    // get/has/put/remove Methods and Overloads
    // ------------------------------------------------------------------------


    public V get( K key ) throws IOException
    {
        if ( ! allowsDuplicates )
        {
            return getRaw( key );
        }

        Object values = getRaw( key );
        
        if ( values == null )
        {
            return null;
        }
        
        if ( values instanceof TreeSet )
        {
            //noinspection unchecked
            TreeSet<V> set = ( TreeSet<V> ) values;
            
            if ( set.size() == 0 )
            {
                return null;
            }
            else
            {
                return set.first();
            }
        }

        if ( values instanceof BTreeRedirect )
        {
            BTree tree = getBTree( ( BTreeRedirect ) values );
            
            if ( tree.size() == 0 )
            {
                return null;
            }
            else
            {
                return firstKey( tree );
            }
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }

    
    /**
     * @see Table#has(java.lang.Object,
     * java.lang.Object, boolean)
     */
    @SuppressWarnings("unchecked")
    public boolean has( K key, V val, boolean isGreaterThan ) throws IOException
    {
        if ( !allowsDuplicates )
        {
            Object rval = getRaw( key );

            // key does not exist so return nothing
            if ( null == rval )
            {
                return false;
            }
            // val == val return true
            else if ( val.equals( rval ) )
            {
                return true;
            }
            // val >= val and test is for greater then return true
            else if ( comparator.compareValue( ( V ) rval, val ) >= 1 && isGreaterThan )
            {
                return true;
            }
            // val <= val and test is for lesser then return true
            else if ( comparator.compareValue( ( V ) rval, val ) <= 1 && !isGreaterThan )
            {
                return true;
            }

            return false;
        }

        Object values = getRaw( key );
        
        if ( values == null )
        {
            return false;
        }
        
        if ( values instanceof TreeSet )
        {
            TreeSet set = ( TreeSet ) values;
            SortedSet subset;
    
            if ( set.size() == 0 )
            {
                return false;
            }
    
            if ( isGreaterThan )
            {
                subset = set.tailSet( val );
            }
            else
            {
                subset = set.headSet( val );
            }

            return subset.size() > 0 || set.contains( val );
        }
        
        if ( values instanceof BTreeRedirect )
        {
            BTree tree = getBTree( ( BTreeRedirect ) values );
            return tree.size() != 0 && btreeHas( tree, val, isGreaterThan );
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }
    

    /**
     * @see Table#has(java.lang.Object, boolean)
     */
    public boolean has( K key, boolean isGreaterThan ) throws IOException
    {
        // See if we can find the border between keys greater than and less
        // than in the set of keys.  This will be the spot we search from.
        jdbm.helper.Tuple tuple = bt.findGreaterOrEqual( key );

        // Test for equality first since it satisfies both greater/less than
        //noinspection unchecked
        if ( null != tuple && comparator.compareKey( ( K ) tuple.getKey(), key ) == 0 )
        {
            return true;
        }

        // Greater searches are easy and quick thanks to findGreaterOrEqual
        if ( isGreaterThan )
        {
            // A null return above means there were no equal or greater keys
            if ( null == tuple )
            {
                return false;
            }

            // Not Null! - we found a tuple with equal or greater key value
            return true;
        }

        // Less than searches occur below and are not as efficient or easy.
        // We need to scan up from the begining if findGreaterOrEqual failed
        // or scan down if findGreaterOrEqual succeed.
        TupleBrowser browser;
        if ( null == tuple )
        {
            // findGreaterOrEqual failed so we create a tuple and scan from
            // the lowest values up via getNext comparing each key to key
            tuple = new jdbm.helper.Tuple();
            browser = bt.browse();

            // We should at most have to read one key.  If 1st key is not
            // less than or equal to key then all keys are > key
            // since the keys are assorted in ascending order based on the
            // comparator.
            if ( browser.getNext( tuple ) )
            {
                //noinspection unchecked
                return comparator.compareKey( ( K ) tuple.getKey(), key ) <= 0;
            }
        }
        else
        {
            // findGreaterOrEqual succeeded so use the existing tuple and
            // scan the down from the highest key less than key via
            // getPrevious while comparing each key to key.
            browser = bt.browse( tuple.getKey() );

            // The above call positions the browser just before the given
            // key so we need to step forward once then back.  Remember this
            // key represents a key greater than or equal to key.
            //noinspection unchecked
            if ( comparator.compareKey( ( K ) tuple.getKey(), key ) <= 0 )
            {
                return true;
            }

            browser.getNext( tuple );

            // We should at most have to read one key, but we don't short
            // the search as in the search above first because the chance of
            // unneccessarily looping is nil since values get smaller.
            while ( browser.getPrevious( tuple ) )
            {
                //noinspection unchecked
                if ( comparator.compareKey( ( K ) tuple.getKey(), key ) <= 0 )
                {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#has(java.lang.Object,
     * java.lang.Object)
     */
    public boolean has( K key, V value ) throws IOException
    {
        if ( ! allowsDuplicates )
        {
            Object obj = getRaw( key );
            return null != obj && obj.equals( value );
        }
        
        Object values = getRaw( key );
        
        if ( values == null )
        {
            return false;
        }
        
        if ( values instanceof TreeSet )
        {
            return ( ( TreeSet ) values ).contains( value );
        }
        
        if ( values instanceof BTreeRedirect )
        {
            return btreeHas( getBTree( ( BTreeRedirect ) values ), value );
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }
    

    /**
     * @see Table#has(java.lang.Object)
     */
    public boolean has( K key ) throws IOException
    {
        return getRaw( key ) != null;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#put(java.lang.Object,
     * java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public V put( K key, V value ) throws IOException
    {
        V replaced;

        if ( ! allowsDuplicates )
        {
            replaced = ( V ) bt.insert( key, value, true );

            if ( null == replaced )
            {
                count++;
            }

            return replaced;
        }
        
        Object values = getRaw( key );
        
        if ( values == null )
        {
            values = new TreeSet<V>( comparator.getValueComparator() );
        }
        
        if ( values instanceof TreeSet )
        {
            TreeSet<V> set = ( TreeSet ) values;
            
            if ( set.contains( value ) )
            {
                return value;
            }
            
            boolean addSuccessful = set.add( value );
            
            if ( set.size() > numDupLimit )
            {
                BTree tree = convertToBTree( set );
                BTreeRedirect redirect = new BTreeRedirect( tree.getRecid() );
                replaced = ( V ) bt.insert( key, redirect, true );
            }
            else
            {
                replaced = ( V ) bt.insert( key, set, true );
            }
            
            if ( addSuccessful )
            {
                count++;
                return replaced;
            }
            return null;
        }
        
        if ( values instanceof BTreeRedirect )
        {
            BTree tree = getBTree( ( BTreeRedirect ) values );
            
            if ( value != null insertDupIntoBTree( tree, value ) )
            {
                count++;
            }
            return null;
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }
    

    /**
     * @see Table#remove(java.lang.Object,
     * java.lang.Object)
     */
    public V remove( K key, V value ) throws IOException
    {
        if ( ! allowsDuplicates )
        {
            V oldValue = getRaw( key );
        
            // Remove the value only if it is the same as value.
            if ( oldValue != null && oldValue.equals( value ) )
            {
                count--;
                return oldValue;
            }

            return null;
        }

        Object values = getRaw( key );
        
        if ( values == null )
        {
            return null;
        }
        
        if ( values instanceof TreeSet )
        {
            TreeSet set = ( TreeSet ) values;

            // If removal succeeds then remove if set is empty else replace it
            if ( set.remove( value ) )
            {
                bt.insert( key, set, true );
                count--;
                return value;
            }

            return null;
        }

        // TODO might be nice to add code here that reverts to a TreeSet
        // if the number of duplicates falls below the numDupLimit value
        if ( values instanceof BTreeRedirect )
        {
            BTree tree = getBTree( ( BTreeRedirect ) values );
            
            if ( removeDupFromBTree( tree, value ) )
            {
                if ( tree.size() == 0 )
                {
                }
                
                count--;
                return value;
            }
            
            return null;
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }


    /**
     * @see Table#remove(java.lang.Object)
     */
    public V remove( K key ) throws IOException
    {
        //noinspection unchecked
        V returned = ( V ) bt.remove( key );

        if ( null == returned )
        {
            return null;
        }

        if ( ! allowsDuplicates )
        {
            this.count--;
            return returned;
        }

        if ( returned instanceof TreeSet )
        {
            //noinspection unchecked
            TreeSet<V> set = ( TreeSet<V> ) returned;
            this.count -= set.size();
            return set.first();
        }
        
        if ( returned instanceof BTreeRedirect )
        {
            BTree tree = getBTree( ( BTreeRedirect ) returned );
            this.count -= tree.size();
            return removeAll( tree );
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }


    public Cursor<Tuple<K,V>> cursor() throws IOException
    {
        if ( allowsDuplicates )
        {
            return new JdbmDupsCursor<K,V>( this );
        }

        return new JdbmNoDupsCursor<K,V>( this );
    }


    // ------------------------------------------------------------------------
    // Maintenance Operations 
    // ------------------------------------------------------------------------


    /**
     * @see Table#close()
     */
    public synchronized void close() throws IOException
    {
        sync();
    }


    /**
     * Synchronizes the buffers with disk.
     *
     * @throws IOException if errors are encountered on the flush
     */
    public void sync() throws IOException
    {
        long recId = recMan.getNamedObject( name + SZSUFFIX );

        if ( 0 == recId )
        {
            //noinspection UnusedAssignment
            recId = recMan.insert( count );
        }
        else
        {
            recMan.update( recId, count );
        }
    }


    // ------------------------------------------------------------------------
    // Private Utility Methods 
    // ------------------------------------------------------------------------


    /**
     * Gets a Tuple value from the btree.
     *
     * @param key the key of the Tuple to get the value of 
     * @return the raw value object from the btree
     * @throws IOException if there are any problems accessing the btree.
     */
    private V getRaw( K key ) throws IOException
    {
        V val;

        if ( null == key )
        {
            return null;
        }

        if ( ! allowsDuplicates )
        {
            //noinspection unchecked
            val = ( V ) bt.find( key );
        }
        else
        {
            //noinspection unchecked
            val = ( V ) bt.find( key );
        }

        return val;
    }


    /**
     * Returns the main BTree used by this table.
     *
     * @return the main JDBM BTree used by this table
     */
    BTree getBTree()
    {
        return bt;
    }


    BTree getBTree( BTreeRedirect redirect ) throws IOException
    {
        if ( duplicateBtrees.containsKey( redirect.getRecId() ) )
        {
            return duplicateBtrees.get( redirect.getRecId() );
        }
        
        BTree tree = BTree.load( recMan, redirect.getRecId() );
        duplicateBtrees.put( redirect.getRecId(), tree );
        return tree;
    }


    private V firstKey ( BTree tree ) throws IOException
    {
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
        boolean success = tree.browse().getNext( tuple );
            
        if ( success )
        {
            //noinspection unchecked
            return ( V ) tuple.getKey();
        }
        else
        {
            return null;
        }
    }

    
    private boolean btreeHas( BTree tree, V key, boolean isGreaterThan ) throws IOException
    {
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
        
        TupleBrowser browser = tree.browse( key );
        if ( isGreaterThan )
        {
            return browser.getNext( tuple );
        }
        else
        {
            boolean success = browser.getPrevious( tuple );
            if ( success )
            {
                return true;
            }
            else
            {
                /*
                 * Calls to getPrevious() will return a lower key even
                 * if there exists a key equal to the one searched
                 * for.  Since isGreaterThan when false really means
                 * 'less than or equal to' we must check to see if
                 * the key in front is equal to the key argument provided.
                 */
                success = browser.getNext( tuple );
                if ( success )
                {
                    /*
                     * Note that keys in these embedded BTrees really store
                     * duplicate values of similar keys in this BTree.
                     */

                    //noinspection unchecked
                    V biggerKey = ( V ) tuple.getKey();
                    if ( comparator.compareValue( key, biggerKey ) == 0 )
                    {
                        return true;
                    }
                }
                return false;
            }
        }
    }


    private boolean btreeHas( BTree tree, V key ) throws IOException
    {
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
        
        TupleBrowser browser = tree.browse( key );
        boolean success = browser.getNext( tuple );
        if ( success )
        {
            /*
             * Note that keys in these embedded BTrees really store
             * duplicate values of similar keys in this BTree.
             */

            //noinspection unchecked
            if ( comparator.compareValue( key, ( V ) tuple.getKey() ) == 0 )
            {
                return true;
            }
        }

        return false;
    }

    
    private boolean insertDupIntoBTree( BTree tree, V value ) throws IOException
    {
        Object replaced = tree.insert( value, EMPTY_BYTES, true );
        return null == replaced;
    }
    

    private boolean removeDupFromBTree( BTree tree, V value ) throws IOException
    {
        Object removed = null;
        if ( tree.find( value ) != null )
        {
            removed = tree.remove( value );
        }
        return null != removed;
    }
    

    private BTree convertToBTree( TreeSet set ) throws IOException
    {
        BTree tree = BTree.createInstance( recMan, comparator.getValueComparator() );
        for ( Object element : set )
        {
            tree.insert( element, EMPTY_BYTES, true );
        }
        return tree;
    }
    
    
    private V removeAll( BTree tree ) throws IOException
    {
        V first = null;
        jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
        TupleBrowser browser = tree.browse();

        while( browser.getNext( jdbmTuple ) )
        {
            tree.remove( jdbmTuple.getKey() );
            if ( first == null )
            {
                //noinspection unchecked
                first = ( V ) jdbmTuple.getKey();
            }
        }

        return first;
    }
}