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
import jdbm.helper.*;

import org.apache.directory.server.core.avltree.*;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.EmptyCursor;
import org.apache.directory.server.core.cursor.SingletonCursor;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.xdbm.*;
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
    /** the wrappedCursor JDBM btree used in this Table */
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

        if ( recId == 0 ) // Create new main BTree
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
        else // Load existing BTree
        {
            bt = BTree.load( recMan, recId );
            bt.setValueSerializer( valueSerializer );
            recId = recMan.getNamedObject( name + SZSUFFIX );
            count = ( Integer ) recMan.fetch( recId );
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
     * @see org.apache.directory.server.xdbm.Table#isDupsEnabled()
     */
    public boolean isDupsEnabled()
    {
        return allowsDuplicates;
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#getName()
     */
    public String getName()
    {
        return name;
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#getRenderer()
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
     * @see org.apache.directory.server.xdbm.Table#count(java.lang.Object)
     */
    public int count( K key ) throws IOException
    {
        if ( key == null )
        {
            return 0;
        }

        if ( ! allowsDuplicates )
        {
            if ( null == bt.find( key ) )
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );
        if ( values.isAvlTree() )
        {
            return values.getAvlTree().getSize();
        }

        return getBTree( values.getBTreeRedirect() ).size();
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#count()
     */
    public int count() throws IOException
    {
        return count;
    }

    
    // ------------------------------------------------------------------------
    // get/has/put/remove Methods and Overloads
    // ------------------------------------------------------------------------


    public V get( K key ) throws Exception
    {
        if ( key == null )
        {
            return null;
        }

        if ( ! allowsDuplicates )
        {
            //noinspection unchecked
            return ( V ) bt.find( key );
        }                         

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );
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
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
        tree.browse().getNext( tuple );
        //noinspection unchecked
        return ( V ) tuple.getKey();
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

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );
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

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );
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
     * @see org.apache.directory.server.xdbm.Table#hasGreaterOrEqual(Object)
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
        // Can only find greater than or equal to with JDBM so we find that
        // and work backwards to see if we can find one less than the key
        jdbm.helper.Tuple tuple = bt.findGreaterOrEqual( key );

        // Test for equality first since it satisfies equal to condition
        //noinspection unchecked
        if ( null != tuple && keyComparator.compare( ( K ) tuple.getKey(), key ) == 0 )
        {
            return true;
        }

        if ( null == tuple )
        {
            /*
             * Jdbm failed to find a key greater than or equal to the argument
             * which means all the keys in the table are less than the
             * supplied key argument.  We can hence return true if the table
             * contains any Tuples.
             */
            return count > 0;
        }
        else
        {
            /*
             * We have the next tuple whose key is the next greater than the
             * key argument supplied.  We use this key to advance a browser to
             * that tuple and scan down to lesser Tuples until we hit one
             * that is less than the key argument supplied.  Usually this will
             * be the previous tuple if it exists.
             */
            TupleBrowser browser = bt.browse( tuple.getKey() );
            if ( browser.getPrevious( tuple ) )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#has(java.lang.Object,
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
            //noinspection unchecked
            V stored = ( V ) bt.find( key );
            return null != stored && stored.equals( value );
        }
        
        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );
        if ( values.isAvlTree() )
        {
            return values.getAvlTree().find( value ) != null;
        }
        
        return getBTree( values.getBTreeRedirect() ).find( value ) != null;
    }
    

    /**
     * @see Table#has(java.lang.Object)
     */
    public boolean has( K key ) throws IOException
    {
        return key != null && bt.find(key) != null;
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#put(java.lang.Object,
     * java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public V put( K key, V value ) throws Exception
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
        
        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );
        if ( values.isAvlTree() )
        {
            AvlTree<V> set = values.getAvlTree();
            replaced = set.insert( value );
            
            if ( replaced != null )// if the value already present returns the same value
            {
                return value;
            }
            
            if ( set.getSize() > numDupLimit )
            {
                BTree tree = convertToBTree( set );
                BTreeRedirect redirect = new BTreeRedirect( tree.getRecid() );
                bt.insert( key, BTreeRedirectMarshaller.INSTANCE.serialize( redirect ), true );
            }
            else
            {
                bt.insert( key, marshaller.serialize( set ), true );
            }

            count++;
            return replaced;
        }
        
        BTree tree = getBTree( values.getBTreeRedirect() );
        replaced = ( V ) tree.insert( value, EMPTY_BYTES, true );
        
        if ( replaced == null )
        {
            count++;
        }
        return replaced;
    }
    

    /**
     * @see org.apache.directory.server.xdbm.Table#remove(java.lang.Object,
     * java.lang.Object)
     */
    public V remove( K key, V value ) throws IOException
    {
        if ( key == null )
        {
            return null;
        }

        if ( ! allowsDuplicates )
        {
            //noinspection unchecked
            V oldValue = ( V ) bt.find( key );
        
            // Remove the value only if it is the same as value.
            if ( oldValue != null && oldValue.equals( value ) )
            {
                bt.remove( key );
                count--;
                return oldValue;
            }

            return null;
        }

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );
        if ( values.isAvlTree() )
        {
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

        // if the number of duplicates falls below the numDupLimit value
        BTree tree = getBTree( values.getBTreeRedirect() );
        if ( removeDupFromBTree( tree, value ) )
        {
            /*
             * If we drop below the duplicate limit then we revert from using
             * a Jdbm BTree to using an in memory AvlTree.
             */
            if ( tree.size() <= numDupLimit )
            {
                AvlTree<V> avlTree = convertToAvlTree( tree );
                bt.insert( key, marshaller.serialize( avlTree ), true );
                recMan.delete( tree.getRecid() );
            }
            
            count--;
            return value;
        }
        
        return null;
    }


    /**
     * @see Table#remove(Object)
     */
    public V remove( K key ) throws IOException
    {
        if ( key == null )
        {
            return null;
        }

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

        byte[] serialized = ( byte[] ) returned;

        if ( BTreeRedirectMarshaller.isRedirect( serialized ) )
        {
            BTree tree = getBTree( BTreeRedirectMarshaller.INSTANCE.deserialize( serialized ) );
            this.count -= tree.size();
            return removeAll( tree );
        }
        else
        {
            AvlTree<V> set = marshaller.deserialize( serialized );
            this.count -= set.getSize();
            return set.getFirst().getKey();
        }
    }


    public Cursor<org.apache.directory.server.xdbm.Tuple<K,V>> cursor() throws Exception
    {
        if ( allowsDuplicates )
        {
            return new DupsCursor<K,V>( this );
        }

        return new NoDupsCursor<K,V>( this );
    }


    public Cursor<org.apache.directory.server.xdbm.Tuple<K,V>> cursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<org.apache.directory.server.xdbm.Tuple<K,V>>();
        }

        Object raw = bt.find( key );

        if ( null == raw )
        {
            return new EmptyCursor<org.apache.directory.server.xdbm.Tuple<K,V>>();
        }

        if ( ! allowsDuplicates )
        {
            //noinspection unchecked
            return new SingletonCursor<org.apache.directory.server.xdbm.Tuple<K,V>>( new org.apache.directory.server.xdbm.Tuple<K,V>( key, ( V ) raw ) );
        }

        byte[] serialized = ( byte[] ) raw;
        if ( BTreeRedirectMarshaller.isRedirect( serialized ) )
        {
            BTree tree = getBTree( BTreeRedirectMarshaller.INSTANCE.deserialize( serialized ) );
            return new KeyTupleBTreeCursor<K,V>( tree, key, valueComparator );
        }

        AvlTree<V> set = marshaller.deserialize( serialized );
        return new KeyTupleAvlCursor<K,V>( set, key );
    }


    public Cursor<V> valueCursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<V>();
        }

        Object raw = bt.find( key );

        if ( null == raw )
        {
            return new EmptyCursor<V>();
        }

        if ( ! allowsDuplicates )
        {
            //noinspection unchecked
            return new SingletonCursor<V>( ( V ) raw );
        }

        byte[] serialized = ( byte[] ) raw;
        if ( BTreeRedirectMarshaller.isRedirect( serialized ) )
        {
            BTree tree = getBTree( BTreeRedirectMarshaller.INSTANCE.deserialize( serialized ) );
            return new KeyBTreeCursor<V>( tree, valueComparator );
        }

        return new AvlTreeCursor<V>( marshaller.deserialize( serialized ) );
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
        recMan.update( recId, count );
    }

    
    public Marshaller<AvlTree<V>> getMarshaller()
    {
        return marshaller;
    }
    

    // ------------------------------------------------------------------------
    // Private/Package Utility Methods 
    // ------------------------------------------------------------------------


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
            if ( browser.getPrevious( tuple ) )
            {
                return true;
            }
            else
            {
                /*
                 * getPrevious() above fails which means the browser has is
                 * before the first Tuple of the btree.  A call to getNext()
                 * should work every time.
                 */
                browser.getNext( tuple );

                /*
                 * Since the browser is positioned now on the Tuple with the
                 * smallest key we just need to check if it equals this key
                 * which is the only chance for returning true.
                 */
                //noinspection unchecked
                V firstKey = ( V ) tuple.getKey();
                return valueComparator.compare( key, firstKey ) == 0;
            }
        }
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


    private AvlTree<V> convertToAvlTree( BTree bTree ) throws IOException
    {
        AvlTree<V> avlTree = new AvlTree<V>( valueComparator );
        TupleBrowser browser = bTree.browse();
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
        while ( browser.getNext( tuple ) )
        {
            //noinspection unchecked
            avlTree.insert( ( V ) tuple.getKey() );
        }

        return avlTree;
    }
    

    private BTree convertToBTree( AvlTree<V> avlTree ) throws Exception
    {
        BTree bTree;

        if ( valueSerializer != null )
        {
            bTree = BTree.createInstance( recMan, valueComparator, valueSerializer, null );
        }
        else
        {
            bTree = BTree.createInstance( recMan, valueComparator );
        }

        Cursor<V> keys = new AvlTreeCursor<V>( avlTree );
        keys.beforeFirst();
        while ( keys.next() )
        {
            bTree.insert( keys.get(), EMPTY_BYTES, true );
        }
        return bTree;
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
