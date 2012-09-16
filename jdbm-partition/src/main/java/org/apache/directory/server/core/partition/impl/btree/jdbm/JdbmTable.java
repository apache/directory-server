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
import java.util.Comparator;
import java.util.Map;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.Serializer;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import org.apache.directory.server.core.avltree.ArrayMarshaller;
import org.apache.directory.server.core.avltree.ArrayTree;
import org.apache.directory.server.core.avltree.ArrayTreeCursor;
import org.apache.directory.server.core.avltree.Marshaller;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractTable;
import org.apache.directory.server.xdbm.KeyTupleArrayCursor;
import org.apache.directory.server.xdbm.Table;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.model.cursor.SingletonCursor;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.shared.util.StringConstants;
import org.apache.directory.shared.util.SynchronizedLRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A jdbm Btree wrapper that enables duplicate sorted keys using collections.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmTable<K, V> extends AbstractTable<K, V>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmTable.class.getSimpleName() );

    /** the key to store and retreive the count information */
    private static final String SZSUFFIX = "_btree_sz";

    /** the JDBM record manager for the file this table is managed in */
    private final RecordManager recMan;

    /** the wrappedCursor JDBM btree used in this Table */
    private BTree<K, V> bt;

    /** the limit at which we start using btree redirection for duplicates */
    private int numDupLimit = JdbmIndex.DEFAULT_DUPLICATE_LIMIT;

    /** a cache of duplicate BTrees */
    private final Map<Long, BTree<K, V>> duplicateBtrees;

    /** A Key serializer */
    private final Serializer keySerializer;

    /** A value serializer */
    private final Serializer valueSerializer;

    /** A marshaller used to serialize/deserialize values stored in the Table */
    Marshaller<ArrayTree<V>> marshaller;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R
    // ------------------------------------------------------------------------

    /**
     * Creates a Jdbm BTree based tuple Table abstraction that enables 
     * duplicates.
     *
     * @param schemaManager The server schemaManager
     * @param name the name of the table
     * @param numDupLimit the size limit of duplicates before switching to BTrees for values instead of AvlTrees
     * @param manager the record manager to be used for this table
     * @param keyComparator a key comparator
     * @param valueComparator a value comparator
     * @param keySerializer a serializer to use for the keys instead of using
     * default Java serialization which could be very expensive
     * @param valueSerializer a serializer to use for the values instead of
     * using default Java serialization which could be very expensive
     * @throws IOException if the table's file cannot be created
     */
    @SuppressWarnings("unchecked")
    public JdbmTable( SchemaManager schemaManager, String name, int numDupLimit, RecordManager manager,
        Comparator<K> keyComparator, Comparator<V> valueComparator,
        Serializer keySerializer, Serializer valueSerializer )
        throws IOException
    {
        super( schemaManager, name, keyComparator, valueComparator );

        if ( valueComparator == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_592 ) );
        }

        // TODO make the size of the duplicate btree cache configurable via constructor
        duplicateBtrees = new SynchronizedLRUMap( 100 );

        if ( valueSerializer != null )
        {
            marshaller = new ArrayMarshaller<V>( valueComparator,
                new MarshallerSerializerBridge<V>( valueSerializer ) );
        }
        else
        {
            marshaller = new ArrayMarshaller<V>( valueComparator );
        }

        this.numDupLimit = numDupLimit;
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

            bt = new BTree<K, V>( recMan, keyComparator, keySerializer, null );
            recId = bt.getRecordId();
            recMan.setNamedObject( name, recId );
            recId = recMan.insert( 0 );
            recMan.setNamedObject( name + SZSUFFIX, recId );
        }
        else
        // Load existing BTree
        {
            bt = new BTree<K, V>().load( recMan, recId );
            ( ( SerializableComparator<K> ) bt.getComparator() ).setSchemaManager( schemaManager );
            recId = recMan.getNamedObject( name + SZSUFFIX );
            count = ( Integer ) recMan.fetch( recId );
        }
    }


    /**
     * Creates a Jdbm BTree based tuple Table abstraction without duplicates 
     * enabled using a simple key comparator.
     *
     * @param schemaManager The server schemaManager
     * @param name the name of the table
     * @param manager the record manager to be used for this table
     * @param keyComparator a tuple comparator
     * @param keySerializer a serializer to use for the keys instead of using
     * default Java serialization which could be very expensive
     * @param valueSerializer a serializer to use for the values instead of
     * using default Java serialization which could be very expensive
     * @throws IOException if the table's file cannot be created
     */
    public JdbmTable( SchemaManager schemaManager, String name, RecordManager manager, Comparator<K> keyComparator,
        Serializer keySerializer, Serializer valueSerializer )
        throws IOException
    {
        super( schemaManager, name, keyComparator, null );

        this.duplicateBtrees = null;
        this.numDupLimit = Integer.MAX_VALUE;
        this.recMan = manager;

        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;

        this.allowsDuplicates = false;

        long recId = recMan.getNamedObject( name );

        if ( recId != 0 )
        {
            bt = new BTree<K, V>().load( recMan, recId );
            ( ( SerializableComparator<K> ) bt.getComparator() ).setSchemaManager( schemaManager );
            bt.setValueSerializer( valueSerializer );
            recId = recMan.getNamedObject( name + SZSUFFIX );
            count = ( Integer ) recMan.fetch( recId );
        }
        else
        {
            bt = new BTree<K, V>( recMan, keyComparator, keySerializer, valueSerializer );
            recId = bt.getRecordId();
            recMan.setNamedObject( name, recId );
            recId = recMan.insert( 0 );
            recMan.setNamedObject( name + SZSUFFIX, recId );
        }
    }


    // ------------------------------------------------------------------------
    // Simple Table Properties
    // ------------------------------------------------------------------------

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

        if ( !allowsDuplicates )
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

        if ( values.isArrayTree() )
        {
            return values.getArrayTree().size();
        }

        return getBTree( values.getBTreeRedirect() ).size();
    }


    // ------------------------------------------------------------------------
    // get/has/put/remove Methods and Overloads
    // ------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public V get( K key ) throws Exception
    {
        if ( key == null )
        {
            return null;
        }

        if ( !allowsDuplicates )
        {
            return bt.find( key );
        }

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );

        if ( values.isArrayTree() )
        {
            ArrayTree<V> set = values.getArrayTree();

            if ( set.getFirst() == null )
            {
                return null;
            }

            return set.getFirst();
        }

        // Handle values if they are stored in another BTree
        BTree tree = getBTree( values.getBTreeRedirect() );

        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
        TupleBrowser<K, V> browser = tree.browse();
        browser.getNext( tuple );
        this.closeBrowser( browser );
        //noinspection unchecked

        return ( V ) tuple.getKey();
    }


    /**
     * @see Table#hasGreaterOrEqual(Object,Object)
     */
    public boolean hasGreaterOrEqual( K key, V val ) throws IOException
    {
        if ( key == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_593 ) );
        }

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );

        if ( values.isArrayTree() )
        {
            ArrayTree<V> set = values.getArrayTree();
            V result = set.findGreaterOrEqual( val );
            return result != null;
        }

        // last option is to try a btree with BTreeRedirects
        BTree<K, V> tree = getBTree( values.getBTreeRedirect() );

        return tree.size() != 0 && btreeHas( tree, val, true );
    }


    /**
     * @see Table#hasLessOrEqual(Object,Object)
     */
    public boolean hasLessOrEqual( K key, V val ) throws IOException
    {
        if ( key == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_593 ) );
        }

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );

        if ( values.isArrayTree() )
        {
            ArrayTree<V> set = values.getArrayTree();
            V result = set.findLessOrEqual( val );
            return result != null;
        }

        // last option is to try a btree with BTreeRedirects
        BTree<K, V> tree = getBTree( values.getBTreeRedirect() );

        return tree.size() != 0 && btreeHas( tree, val, false );
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#hasGreaterOrEqual(Object)
     */
    @SuppressWarnings("unchecked")
    public boolean hasGreaterOrEqual( K key ) throws IOException
    {
        // See if we can find the border between keys greater than and less
        // than in the set of keys.  This will be the spot we search from.
        jdbm.helper.Tuple tuple = bt.findGreaterOrEqual( key );

        // Test for equality first since it satisfies both greater/less than
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
    @SuppressWarnings("unchecked")
    public boolean hasLessOrEqual( K key ) throws IOException
    {
        // Can only find greater than or equal to with JDBM so we find that
        // and work backwards to see if we can find one less than the key
        Tuple<K, V> tuple = bt.findGreaterOrEqual( key );

        // Test for equality first since it satisfies equal to condition
        if ( null != tuple && keyComparator.compare( tuple.getKey(), key ) == 0 )
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
                this.closeBrowser( browser );
                return true;
            }

            this.closeBrowser( browser );
        }

        return false;
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#has(java.lang.Object,
     * java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public boolean has( K key, V value ) throws IOException
    {
        if ( key == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            V stored = bt.find( key );
            return null != stored && stored.equals( value );
        }

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );

        if ( values.isArrayTree() )
        {
            return values.getArrayTree().find( value ) != null;
        }

        return getBTree( values.getBTreeRedirect() ).find( value ) != null;
    }


    /**
     * @see Table#has(java.lang.Object)
     */
    public boolean has( K key ) throws IOException
    {
        return key != null && bt.find( key ) != null;
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#put(java.lang.Object,
     * java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public synchronized void put( K key, V value ) throws Exception
    {
        try
        {
            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "---> Add {} = {}", name, key );
            }

            if ( ( value == null ) || ( key == null ) )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_594 ) );
            }

            V replaced;

            if ( !allowsDuplicates )
            {
                replaced = ( V ) bt.insert( key, value, true );

                if ( null == replaced )
                {
                    count++;
                }

                if ( LOG.isDebugEnabled() )
                {
                    LOG.debug( "<--- Add ONE {} = {}", name, key );
                }

                return;
            }

            DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );

            if ( values.isArrayTree() )
            {
                ArrayTree<V> set = values.getArrayTree();
                replaced = set.insert( value );

                if ( replaced != null )// if the value already present returns the same value
                {
                    return;
                }

                if ( set.size() > numDupLimit )
                {
                    BTree tree = convertToBTree( set );
                    BTreeRedirect redirect = new BTreeRedirect( tree.getRecordId() );
                    bt.insert( key, ( V ) BTreeRedirectMarshaller.INSTANCE.serialize( redirect ), true );

                    if ( LOG.isDebugEnabled() )
                    {
                        LOG.debug( "<--- Add new BTREE {} = {}", name, key );
                    }
                }
                else
                {
                    bt.insert( key, ( V ) marshaller.serialize( set ), true );

                    if ( LOG.isDebugEnabled() )
                    {
                        LOG.debug( "<--- Add AVL {} = {}", name, key );
                    }
                }

                count++;
                return;
            }

            BTree tree = getBTree( values.getBTreeRedirect() );
            replaced = ( V ) tree.insert( value, StringConstants.EMPTY_BYTES, true );

            if ( replaced == null )
            {
                count++;
            }

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "<--- Add BTREE {} = {}", name, key );
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_131, key, name ), e );
            throw e;
        }
    }


    /**
     * @see org.apache.directory.server.xdbm.Table#remove(java.lang.Object,
     * java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public synchronized void remove( K key, V value ) throws IOException
    {
        try
        {
            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "---> Remove " + name + " = " + key + ", " + value );
            }

            if ( key == null )
            {
                if ( LOG.isDebugEnabled() )
                {
                    LOG.debug( "<--- Remove NULL key " + name );
                }

                return;
            }

            if ( !allowsDuplicates )
            {
                V oldValue = bt.find( key );

                // Remove the value only if it is the same as value.
                if ( ( oldValue != null ) && oldValue.equals( value ) )
                {
                    bt.remove( key );
                    count--;

                    if ( LOG.isDebugEnabled() )
                    {
                        LOG.debug( "<--- Remove ONE " + name + " = " + key + ", " + value );
                    }

                    return;
                }

                return;
            }

            DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );

            if ( values.isArrayTree() )
            {
                ArrayTree<V> set = values.getArrayTree();

                // If removal succeeds then remove if set is empty else replace it
                if ( set.remove( value ) != null )
                {
                    if ( set.isEmpty() )
                    {
                        bt.remove( key );
                    }
                    else
                    {
                        bt.insert( key, ( V ) marshaller.serialize( set ), true );
                    }

                    count--;

                    if ( LOG.isDebugEnabled() )
                    {
                        LOG.debug( "<--- Remove AVL " + name + " = " + key + ", " + value );
                    }

                    return;
                }

                return;
            }

            // if the number of duplicates falls below the numDupLimit value
            BTree tree = getBTree( values.getBTreeRedirect() );

            if ( tree.find( value ) != null && tree.remove( value ) != null )
            {
                /*
                 * If we drop below the duplicate limit then we revert from using
                 * a Jdbm BTree to using an in memory AvlTree.
                 */
                if ( tree.size() <= numDupLimit )
                {
                    ArrayTree<V> avlTree = convertToArrayTree( tree );
                    bt.insert( key, ( V ) marshaller.serialize( avlTree ), true );
                    recMan.delete( tree.getRecordId() );
                }

                count--;

                if ( LOG.isDebugEnabled() )
                {
                    LOG.debug( "<--- Remove BTREE " + name + " = " + key + ", " + value );
                }

                return;
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_132, key, value, name ), e );
        }
    }


    /**
     * @see Table#remove(Object)
     */
    public synchronized void remove( K key ) throws IOException
    {
        try
        {
            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "---> Remove {} = {}", name, key );
            }

            if ( key == null )
            {
                return;
            }

            Object returned = bt.remove( key );

            if ( null == returned )
            {
                if ( LOG.isDebugEnabled() )
                {
                    LOG.debug( "<--- Remove AVL {} = {} (not found)", name, key );
                }

                return;
            }

            if ( !allowsDuplicates )
            {
                this.count--;

                if ( LOG.isDebugEnabled() )
                {
                    LOG.debug( "<--- Remove ONE {} = {}", name, key );
                }

                return;
            }

            byte[] serialized = ( byte[] ) returned;

            if ( BTreeRedirectMarshaller.isRedirect( serialized ) )
            {
                BTree tree = getBTree( BTreeRedirectMarshaller.INSTANCE.deserialize( serialized ) );
                this.count -= tree.size();

                if ( LOG.isDebugEnabled() )
                {
                    LOG.debug( "<--- Remove BTree {} = {}", name, key );
                }

                recMan.delete( tree.getRecordId() );
                duplicateBtrees.remove( tree.getRecordId() );

                return;
            }
            else
            {
                ArrayTree<V> set = marshaller.deserialize( serialized );
                this.count -= set.size();

                if ( LOG.isDebugEnabled() )
                {
                    LOG.debug( "<--- Remove AVL {} = {}", name, key );
                }

                return;
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_133, key, name ), e );

            if ( e instanceof IOException )
            {
                throw ( IOException ) e;
            }
        }
    }


    public Cursor<org.apache.directory.shared.ldap.model.cursor.Tuple<K, V>> cursor() throws Exception
    {
        if ( allowsDuplicates )
        {
            return new DupsCursor<K, V>( this );
        }

        return new NoDupsCursor<K, V>( this );
    }


    @SuppressWarnings("unchecked")
    public Cursor<org.apache.directory.shared.ldap.model.cursor.Tuple<K, V>> cursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<org.apache.directory.shared.ldap.model.cursor.Tuple<K, V>>();
        }

        V raw = bt.find( key );

        if ( null == raw )
        {
            return new EmptyCursor<org.apache.directory.shared.ldap.model.cursor.Tuple<K, V>>();
        }

        if ( !allowsDuplicates )
        {
            return new SingletonCursor<org.apache.directory.shared.ldap.model.cursor.Tuple<K, V>>(
                new org.apache.directory.shared.ldap.model.cursor.Tuple<K, V>( key, raw ) );
        }

        byte[] serialized = ( byte[] ) raw;

        if ( BTreeRedirectMarshaller.isRedirect( serialized ) )
        {
            BTree tree = getBTree( BTreeRedirectMarshaller.INSTANCE.deserialize( serialized ) );
            return new KeyTupleBTreeCursor<K, V>( tree, key, valueComparator );
        }

        ArrayTree<V> set = marshaller.deserialize( serialized );

        return new KeyTupleArrayCursor<K, V>( set, key );
    }


    @SuppressWarnings("unchecked")
    public Cursor<V> valueCursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<V>();
        }

        V raw = bt.find( key );

        if ( null == raw )
        {
            return new EmptyCursor<V>();
        }

        if ( !allowsDuplicates )
        {
            return new SingletonCursor<V>( raw );
        }

        byte[] serialized = ( byte[] ) raw;

        if ( BTreeRedirectMarshaller.isRedirect( serialized ) )
        {
            BTree tree = getBTree( BTreeRedirectMarshaller.INSTANCE.deserialize( serialized ) );
            return new KeyBTreeCursor<V>( tree, valueComparator );
        }

        return new ArrayTreeCursor<V>( marshaller.deserialize( serialized ) );
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
    public synchronized void sync() throws IOException
    {
        long recId = recMan.getNamedObject( name + SZSUFFIX );
        recMan.update( recId, count );
    }


    public Marshaller<ArrayTree<V>> getMarshaller()
    {
        return marshaller;
    }


    // ------------------------------------------------------------------------
    // Private/Package Utility Methods 
    // ------------------------------------------------------------------------

    /**
     * Added to check that we actually switch from one data structure to the 
     * B+Tree structure on disk for duplicates that go beyond the threshold.
     */
    boolean isKeyUsingBTree( K key ) throws Exception
    {
        if ( key == null )
        {
            throw new IllegalArgumentException( "key is null" );
        }

        if ( !allowsDuplicates )
        {
            return false;
        }

        DupsContainer<V> values = getDupsContainer( ( byte[] ) bt.find( key ) );

        if ( values.isBTreeRedirect() )
        {
            return true;
        }

        return false;
    }


    DupsContainer<V> getDupsContainer( byte[] serialized ) throws IOException
    {
        if ( serialized == null )
        {
            return new DupsContainer<V>( new ArrayTree<V>( valueComparator ) );
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

        BTree<K, V> tree = new BTree<K, V>().load( recMan, redirect.getRecId() );
        ( ( SerializableComparator<K> ) tree.getComparator() ).setSchemaManager( schemaManager );
        duplicateBtrees.put( redirect.getRecId(), tree );

        return tree;
    }


    @SuppressWarnings("unchecked")
    private boolean btreeHas( BTree tree, V key, boolean isGreaterThan ) throws IOException
    {
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();

        TupleBrowser browser = tree.browse( key );

        try
        {
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
                    V firstKey = ( V ) tuple.getKey();

                    return valueComparator.compare( key, firstKey ) == 0;
                }
            }
        }
        finally
        {
            this.closeBrowser( browser );
        }
    }


    @SuppressWarnings("unchecked")
    private ArrayTree<V> convertToArrayTree( BTree bTree ) throws IOException
    {
        ArrayTree<V> avlTree = new ArrayTree<V>( valueComparator );
        TupleBrowser browser = bTree.browse();
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();

        while ( browser.getNext( tuple ) )
        {
            avlTree.insert( ( V ) tuple.getKey() );
        }

        this.closeBrowser( browser );

        return avlTree;
    }


    private BTree<V, K> convertToBTree( ArrayTree<V> arrayTree ) throws Exception
    {
        BTree<V, K> bTree;

        if ( valueSerializer != null )
        {
            bTree = new BTree<V, K>( recMan, valueComparator, valueSerializer, null );
        }
        else
        {
            bTree = new BTree<V, K>( recMan, valueComparator );
        }

        Cursor<V> keys = new ArrayTreeCursor<V>( arrayTree );
        keys.beforeFirst();

        while ( keys.next() )
        {
            bTree.insert( keys.get(), ( K ) StringConstants.EMPTY_BYTES, true );
        }

        keys.close();

        return bTree;
    }


    private void closeBrowser( TupleBrowser<K, V> browser )
    {
        if ( browser != null )
        {
            browser.close();
        }
    }
}
