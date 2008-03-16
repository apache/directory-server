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

import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeMarshaller;
import org.apache.directory.server.core.avltree.Marshaller;
import org.apache.directory.server.core.avltree.LinkedAvlNode;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.partition.impl.btree.*;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.shared.ldap.util.SynchronizedLRUMap;

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
    /** a key comparator for the keys in this Table */
    private final Comparator<K> keyComparator;
    /** a value comparator for the values in this Table */
    private final Comparator<V> valueComparator;

    /** the current count of entries in this Table */
    private int count;
    /** the underlying JDBM btree used in this Table */
    private BTree bt;


    /** the renderer to use for btree tuples */
    private TupleRenderer renderer;
    /** the limit at which we start using btree redirection for duplicates */
    private int numDupLimit = JdbmIndex.DEFAULT_DUPLICATE_LIMIT;
    /** a cache of duplicate BTrees */
    private final Map<Long, BTree> duplicateBtrees;

    private final Serializer keySerializer;

    private final Serializer valueSerializer;

    AvlTreeMarshaller<V> marshaller;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R
    // ------------------------------------------------------------------------

    
    /**
     * Creates a Jdbm BTree based tuple Table abstraction that enables 
     * duplicates.
     *
     * @param name the name of the table
     * @param numDupLimit the size limit of duplicates before switching to
     * BTrees for values instead of AvlTrees
     * @param manager the record manager to be used for this table
     * @param keyComparator a key comparator
     * @param valueComparator a value comparator
     * @param keySerializer a serializer to use for the keys instead of using
     * default Java serialization which could be very expensive
     * @param valueSerializer a serializer to use for the values instead of
     * using default Java serialization which could be very expensive
     * @throws IOException if the table's file cannot be created
     */
    public JdbmTable( String name, int numDupLimit, RecordManager manager,
        Comparator<K> keyComparator, Comparator<V> valueComparator,
        Serializer keySerializer, Serializer valueSerializer )
        throws IOException
    {
        // TODO make the size of the duplicate btree cache configurable via constructor
        //noinspection unchecked
        duplicateBtrees = new SynchronizedLRUMap( 100 );

        if ( valueSerializer != null )
        {
            marshaller = new AvlTreeMarshaller<V>( valueComparator,
                    new MarshallerSerializerBridge<V>( valueSerializer ) );
        }
        else
        {
            marshaller = new AvlTreeMarshaller<V>( valueComparator );
        }

        if ( keyComparator == null )
        {
            throw new NullPointerException( "Key comparator cannot be null." );
        }
        else
        {
            this.keyComparator = keyComparator;
        }

        if ( valueComparator == null )
        {
            throw new NullPointerException( "Value comparator must not be null for tables with duplicate keys." );
        }
        else
        {
            this.valueComparator = valueComparator;
        }

        this.numDupLimit = numDupLimit;
        this.name = name;
        this.recMan = manager;

        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;

        this.allowsDuplicates = true;

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
            // we do not use the value serializer in the btree since duplicates will use
            // either BTreeRedirect objects or AvlTree objects whose marshalling is
            // explicitly managed by this code.  Value serialization is delegated to these
            // marshallers.
            
            bt = BTree.createInstance( recMan, keyComparator, keySerializer, null );
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
        this.duplicateBtrees = null;
        this.numDupLimit = Integer.MAX_VALUE;
        this.name = name;
        this.recMan = manager;

        this.keyComparator = keyComparator;
        this.valueComparator = null;

        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;

        this.allowsDuplicates = false;

        long recId = recMan.getNamedObject( name );

        if ( recId != 0 )
        {
            bt = BTree.load( recMan, recId );
            recId = recMan.getNamedObject( name + SZSUFFIX );
            count = ( Integer ) recMan.fetch( recId );
        }
        else
        {
            bt = BTree.createInstance( recMan, keyComparator, keySerializer, valueSerializer );
            recId = bt.getRecid();
            recMan.setNamedObject( name, recId );
            recId = recMan.insert( 0 );
            recMan.setNamedObject( name + SZSUFFIX, recId );
        }
    }


    // ------------------------------------------------------------------------
    // Simple Table Properties
    // ------------------------------------------------------------------------

    
    /**
     * @see Table#getKeyComparator()
     */
    public Comparator<K> getKeyComparator()
    {
        return keyComparator;
    }


    /**
     * @see Table#getValueComparator()
     */
    public Comparator<V> getValueComparator()
    {
        return valueComparator;
    }


    public Serializer getKeySerializer()
    {
        return keySerializer;
    }


    public Serializer getValueSerializer()
    {
        return valueSerializer;
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
        if ( key == null )
        {
            return 0;
        }

        if ( ! allowsDuplicates )
        {
            if ( null == getNoDups( key ) )
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }

        DupsContainer values = getDups( key );
        
        // -------------------------------------------------------------------
        // Handle the use of a AvlTree for storing duplicates
        // -------------------------------------------------------------------

        if ( values.isAvlTree() )
        {
            return values.getAvlTree().getSize();
        }

        // -------------------------------------------------------------------
        // Handle the use of a BTree for storing duplicates
        // -------------------------------------------------------------------

        return getBTree( values.getBTreeRedirect() ).size();
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
        if ( key == null )
        {
            return null;
        }

        if ( ! allowsDuplicates )
        {
            return getNoDups( key );
        }                         

        DupsContainer values = getDups( key );

        if ( values.isAvlTree() )
        {
            //noinspection unchecked
            AvlTree<V> set = values.getAvlTree();

            if ( set.getFirst() == null )
            {
                return null;
            }
            
            return set.getFirst().getKey();
        }

        // Handle values if they are stored in another BTree
        BTree tree = getBTree( values.getBTreeRedirect() );
        return firstKey( tree );
    }

    
    /**
     * @see Table#hasGreaterOrEqual(Object,Object)
     */
    @SuppressWarnings("unchecked")
    public boolean hasGreaterOrEqual( K key, V val ) throws IOException
    {
        if ( key == null )
        {
            return false;
        }

        if ( ! allowsDuplicates )
        {
            throw new UnsupportedOperationException( "Unfortunately this Table without duplicates enabled " +
            		"does not contain a value comparator which is needed to answer your ordering question." );
        }

        DupsContainer values = getDups( key );

        if ( values.isAvlTree() )
        {
            AvlTree<V> set = values.getAvlTree();
            LinkedAvlNode<V> result = set.findGreaterOrEqual( val );
            return result != null;
        }

        // last option is to try a btree with BTreeRedirects
        BTree tree = getBTree( values.getBTreeRedirect() );
        return tree.size() != 0 && btreeHas( tree, val, true );
    }


    /**
     * @see Table#hasLessOrEqual(Object,Object)
     */
    @SuppressWarnings("unchecked")
    public boolean hasLessOrEqual( K key, V val ) throws IOException
    {
        if ( key == null )
        {
            return false;
        }

        if ( ! allowsDuplicates )
        {
            throw new UnsupportedOperationException( "Unfortunately this Table without duplicates enabled " +
            		"does not contain a value comparator which is needed to answer your ordering question." );
        }

        DupsContainer values = getDups( key );

        if ( values.isAvlTree() )
        {
            AvlTree<V> set = values.getAvlTree();
            LinkedAvlNode<V> result = set.findLessOrEqual( val );
            return result != null;
        }

        // last option is to try a btree with BTreeRedirects
        BTree tree = getBTree( values.getBTreeRedirect() );
        return tree.size() != 0 && btreeHas( tree, val, false );
    }


    /**
     * @see Table#hasGreaterOrEqual(Object)
     */
    public boolean hasGreaterOrEqual( K key ) throws IOException
    {
        // See if we can find the border between keys greater than and less
        // than in the set of keys.  This will be the spot we search from.
        jdbm.helper.Tuple tuple = bt.findGreaterOrEqual( key );

        // Test for equality first since it satisfies both greater/less than
        //noinspection unchecked
        if ( null != tuple && keyComparator.compare( ( K ) tuple.getKey(), key ) == 0 )
        {
            return true;
        }

        // Greater searches are easy and quick thanks to findGreaterOrEqual
        // A null return above means there were no equal or greater keys
        if ( null == tuple )
        {
            return false;
        }

        // Not Null! - we found a tuple with equal or greater key value
        return true;
    }


    /**
     * @see Table#hasLessOrEqual(Object)
     */
    public boolean hasLessOrEqual( K key ) throws IOException
    {
        // See if we can find the border between keys greater than and less
        // than in the set of keys.  This will be the spot we search from.
        jdbm.helper.Tuple tuple = bt.findGreaterOrEqual( key );

        // Test for equality first since it satisfies both greater/less than
        //noinspection unchecked
        if ( null != tuple && keyComparator.compare( ( K ) tuple.getKey(), key ) == 0 )
        {
            return true;
        }

        // Less than searches are not as efficient or easy as greaterOrEqual.
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
                return keyComparator.compare( ( K ) tuple.getKey(), key ) <= 0;
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
            if ( keyComparator.compare( ( K ) tuple.getKey(), key ) <= 0 )
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
                if ( keyComparator.compare( ( K ) tuple.getKey(), key ) <= 0 )
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
        if ( key == null )
        {
            return false;
        }

        if ( ! allowsDuplicates )
        {
            V stored = getNoDups( key );
            return null != stored && stored.equals( value );
        }
        
        DupsContainer values = getDups( key );
        
        if ( values.isAvlTree() )
        {
            //noinspection unchecked
            return values.getAvlTree().find( value ) != null;
        }
        
        return btreeHas( getBTree( values.getBTreeRedirect() ), value );
    }
    

    /**
     * @see Table#has(java.lang.Object)
     */
    public boolean has( K key ) throws IOException
    {
        return key != null && bt.find(key) != null;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#put(java.lang.Object,
     * java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public V put( K key, V value ) throws IOException
    {
        if ( value == null || key == null )
        {
            throw new IllegalArgumentException( "null for key or value is not valid" );
        }
        
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
        
        DupsContainer values = getDups( key );
        
        if ( values.isAvlTree() )
        {
            AvlTree<V> set = values.getAvlTree();
            
            V result = set.insert( value );
            
            if ( result != null )// if the value already present returns the same value
            {
                return value;
            }
            
            if ( set.getSize() > numDupLimit )
            {
                BTree tree = convertToBTree( set );
                BTreeRedirect redirect = new BTreeRedirect( tree.getRecid() );
                replaced = ( V ) bt.insert( key,
                        BTreeRedirectMarshaller.INSTANCE.serialize( redirect ), true );
            }
            else
            {
                replaced = ( V ) bt.insert( key, marshaller.serialize( set ), true );
            }

            count++;
            return replaced;
        }
        
        BTree tree = getBTree( values.getBTreeRedirect() );
        
        if ( insertDupIntoBTree( tree, value ) )
        {
            count++;
        }
        return null;
    }
    

    /**
     * @see Table#remove(java.lang.Object,
     * java.lang.Object)
     */
    public V remove( K key, V value ) throws IOException
    {
        if ( ! allowsDuplicates )
        {
            V oldValue = getNoDups( key );
        
            // Remove the value only if it is the same as value.
            if ( oldValue != null && oldValue.equals( value ) )
            {
                bt.remove( key );
                count--;
                return oldValue;
            }

            return null;
        }

        DupsContainer values = getDups( key );
        
        if ( values == null )
        {
            return null;
        }
        
        if ( values.isAvlTree() )
        {
            //noinspection unchecked
            AvlTree<V> set = values.getAvlTree();

            // If removal succeeds then remove if set is empty else replace it
            if ( set.remove( value ) != null )
            {
                if ( set.isEmpty() )
                {
                    bt.remove( key );
                }
                else
                {
                    bt.insert( key, marshaller.serialize( set ), true );
                }
                count--;
                return value;
            }

            return null;
        }

        // TODO might be nice to add code here that reverts to a AvlTree
        // if the number of duplicates falls below the numDupLimit value
        BTree tree = getBTree( values.getBTreeRedirect() );
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


    /**
     * @see Table#remove(java.lang.Object)
     */
    public V remove( K key ) throws IOException
    {
        //noinspection unchecked
        Object returned = bt.remove( key );

        if ( null == returned )
        {
            return null;
        }

        if ( ! allowsDuplicates )
        {
            this.count--;
            //noinspection unchecked
            return ( V ) returned;
        }

        if ( ! ( returned instanceof byte[] ) )
        {
            throw new IllegalStateException( "Expecting byte[] from returned element." );
        }

        byte[] serialized = ( byte[] ) returned;

        if ( ! BTreeRedirectMarshaller.isRedirect( serialized ) )
        {
            //noinspection unchecked
            AvlTree<V> set = marshaller.deserialize( serialized );
            this.count -= set.getSize();
            return set.getFirst().getKey();
        }

        //noinspection ConstantConditions
        BTree tree = getBTree( BTreeRedirectMarshaller.INSTANCE.deserialize( serialized ) );
        this.count -= tree.size();
        return removeAll( tree );
    }


    public Cursor<Tuple<K,V>> cursor() throws Exception
    {
        if ( allowsDuplicates )
        {
            return new DupsCursor<K,V>( this );
        }

        return new NoDupsCursor<K,V>( this );
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

    
    public Marshaller<AvlTree<V>> getMarshaller()
    {
        return marshaller;
    }
    

    // ------------------------------------------------------------------------
    // Private Utility Methods 
    // ------------------------------------------------------------------------


    private V getNoDups( K key ) throws IOException
    {
        if ( null == key )
        {
            return null;
        }

        if ( ! allowsDuplicates )
        {
            //noinspection unchecked
            return ( V ) bt.find( key );
        }

        throw new IllegalStateException(
                "This method should not be called when duplicates are enabled" );
    }


    DupsContainer<V> getDupsContainer( byte[] serialized ) throws IOException
    {
        if ( serialized == null )
        {
            return new DupsContainer<V>( new AvlTree<V>( valueComparator ) );
        }

        if ( BTreeRedirectMarshaller.isRedirect( serialized ) )
        {
            return new DupsContainer<V>( BTreeRedirectMarshaller.INSTANCE.deserialize( serialized ) );
        }

        return new DupsContainer<V>( marshaller.deserialize( serialized ) );
    }


    private DupsContainer<V> getDups( K key ) throws IOException
    {
        if ( null == key )
        {
            return null;
        }

        if ( allowsDuplicates )
        {
            return getDupsContainer( ( byte[] ) bt.find( key ) );
        }

        throw new IllegalStateException(
                "This method should not be called when duplicates are enabled" );
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
                    if ( valueComparator.compare( key, biggerKey ) == 0 )
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
            if ( valueComparator.compare( key, ( V ) tuple.getKey() ) == 0 )
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
    

    private BTree convertToBTree( AvlTree<V> set ) throws IOException
    {
        BTree tree;

        if ( valueSerializer != null )
        {
            tree = BTree.createInstance( recMan, valueComparator, valueSerializer, null );
        }
        else
        {
            tree = BTree.createInstance( recMan, valueComparator );
        }

        List<V> keys = set.getKeys();
        for ( V element : keys )
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