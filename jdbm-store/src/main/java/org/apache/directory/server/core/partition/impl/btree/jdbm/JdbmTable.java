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
import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.EmptyCursor;
import org.apache.directory.server.core.cursor.IteratorCursor;
import org.apache.directory.server.core.cursor.SingletonCursor;
import org.apache.directory.server.core.partition.impl.btree.*;
import org.apache.directory.server.schema.SerializableComparator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.*;


/**
 * A jdbm Btree wrapper that enables duplicate sorted keys using collections.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmTable implements Table
{
    /**  */
    private static final String SZSUFFIX = "_btree_sz";

    private static final byte[] EMPTY_BYTES = new byte[0];

    /** */
    private final String name;
    /** */
    private final RecordManager recMan;
    /** */
    private final boolean allowsDuplicates;
    /** */
    private final TupleComparator comparator;

    /** */
    private int count;
    /** */
    private BTree bt;
    /** */
    private TupleBrowserFactory browserFactory;


    /** */
    private TupleRenderer renderer;

    private int numDupLimit = JdbmIndex.DEFAULT_DUPLICATE_LIMIT;

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
     * @param manager the record manager to be used for this table
     * @param comparator a tuple comparator
     * @throws NamingException if the table's file cannot be created
     */
    public JdbmTable( String name, boolean allowsDuplicates, int numDupLimit, 
        RecordManager manager, TupleComparator comparator, Serializer keySerializer, 
        Serializer valueSerializer )
        throws NamingException
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

        long recId;

        try
        {
            recId = recMan.getNamedObject( name );
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }

        try
        {

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
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }

        browserFactory = new TupleBrowserFactory()
        {
            public long size() throws IOException
            {
                return count;
            }


            public org.apache.directory.server.core.partition.impl.btree.TupleBrowser beforeFirst() throws IOException
            {
                return new JdbmTupleBrowser( bt.browse() );
            }


            public org.apache.directory.server.core.partition.impl.btree.TupleBrowser afterLast() throws IOException
            {
                return new JdbmTupleBrowser( bt.browse( null ) );
            }
        };
    }


    /**
     * Creates a Jdbm BTree based tuple Table abstraction without duplicates 
     * enabled using a simple key comparator.
     *
     * @param name the name of the table
     * @param manager the record manager to be used for this table
     * @param keyComparator a tuple comparator
     * @throws NamingException if the table's file cannot be created
     */
    public JdbmTable( String name, RecordManager manager, SerializableComparator keyComparator, Serializer keySerializer, Serializer valueSerializer ) 
        throws NamingException
    {
        this( name, false, Integer.MAX_VALUE, manager, new KeyOnlyComparator( keyComparator ), keySerializer, valueSerializer );
    }


    // ------------------------------------------------------------------------
    // Simple Table Properties
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#getComparator()
     */
    public TupleComparator getComparator()
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
     * @see Table#setRenderer(
     * TupleRenderer)
     */
    public void setRenderer( TupleRenderer renderer )
    {
        this.renderer = renderer;
    }


    /**
     * @see Table#isSortedDupsEnabled()
     */
    public boolean isSortedDupsEnabled()
    {
        // If duplicates are enabled than duplicates will be maintained in
        // sorted order.
        return allowsDuplicates;
    }


    // ------------------------------------------------------------------------
    // Count Overloads
    // ------------------------------------------------------------------------

    /**
     * @see Table#count(java.lang.Object, boolean)
     */
    public int count( Object key, boolean isGreaterThan ) throws IOException
    {
        // take a best guess
        return count;
    }


    /**
     * @see Table#count(java.lang.Object)
     */
    public int count( Object key ) throws IOException
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

    /**
     * @see Table#get(java.lang.Object)
     */
    public Object get( Object key ) throws IOException
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
            TreeSet set = ( TreeSet ) values;
            
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
    public boolean has( Object key, Object val, boolean isGreaterThan ) throws IOException
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
            else if ( comparator.compareValue( rval, val ) >= 1 && isGreaterThan )
            {
                return true;
            }
            // val <= val and test is for lesser then return true
            else if ( comparator.compareValue( rval, val ) <= 1 && !isGreaterThan )
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
    public boolean has( Object key, boolean isGreaterThan ) throws IOException
    {
        // See if we can find the border between keys greater than and less
        // than in the set of keys.  This will be the spot we search from.
        jdbm.helper.Tuple tuple = bt.findGreaterOrEqual( key );

        // Test for equality first since it satisfies both greater/less than
        if ( null != tuple && comparator.compareKey( tuple.getKey(), key ) == 0 )
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
                return comparator.compareKey( tuple.getKey(), key ) <= 0;
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
            if ( comparator.compareKey( tuple.getKey(), key ) <= 0 )
            {
                return true;
            }

            browser.getNext( tuple );

            // We should at most have to read one key, but we don't short
            // the search as in the search above first because the chance of
            // unneccessarily looping is nil since values get smaller.
            while ( browser.getPrevious( tuple ) )
            {
                if ( comparator.compareKey( tuple.getKey(), key ) <= 0 )
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
    public boolean has( Object key, Object value ) throws IOException
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
    public boolean has( Object key ) throws IOException
    {
        return getRaw( key ) != null;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#put(java.lang.Object,
     * java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public Object put( Object key, Object value ) throws IOException
    {
        Object replaced;

        if ( ! allowsDuplicates )
        {
            replaced = putRaw( key, value, true );

            if ( null == replaced )
            {
                count++;
            }

            return replaced;
        }
        
        Object values = getRaw( key );
        
        if ( values == null )
        {
            values = new TreeSet( comparator.getValueComparator() );
        }
        
        if ( values instanceof TreeSet )
        {
            TreeSet set = ( TreeSet ) values;
            
            if ( set.contains( value ) )
            {
                return value;
            }
            
            boolean addSuccessful = set.add( value );
            
            if ( set.size() > numDupLimit )
            {
                BTree tree = convertToBTree( set );
                BTreeRedirect redirect = new BTreeRedirect( tree.getRecid() );
                replaced = putRaw( key, redirect, true );
            }
            else
            {
                replaced = putRaw( key, set, true );
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
            if ( insertDupIntoBTree( tree, value ) )
            {
                count++;
                return values;
            }
            return null;
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }
    

    /**
     * @see Table#put(Object, Cursor
     */
    @SuppressWarnings("unchecked")
    public Object put( Object key, Cursor<Object> values ) throws IOException
    {
        /*
         * If we do not allow duplicates call the single add put using the
         * first value in the enumeration if it exists.  If it does not we
         * just return null without doing anything.  If more than one value
         * is in the enumeration than we blow a UnsupportedOperationException.
         */
        if ( !allowsDuplicates )
        {
            if ( values.next() )
            {
                Object value = values.get();

                if ( values.next() )
                {
                    throw new IllegalStateException( "Attempting to put duplicate keys into table " + name
                        + " which does not support duplicates" );
                }

                return put( key, value );
            }

            return null;
        }

        Object storedValues = getRaw( key );
        if ( storedValues == null )
        {
            storedValues = new TreeSet( comparator.getValueComparator() );
        }
        
        if ( storedValues instanceof TreeSet )
        {
            /*
             * Here the table allows duplicates so we get the TreeSet from the 
             * Table holding all the duplicate key values or create one if it
             * does not exist for key.  We check if the value is present and
             * if it is we add it and increment the table entry counter.
             */
            TreeSet set = ( TreeSet ) storedValues;
            while ( values.next() )
            {
                Object val = values.get();
    
                if ( !set.contains( val ) )
                {
                    boolean isAddSuccessful = set.add( val );
                    if ( isAddSuccessful )
                    {
                        count++;
                    }
                }
            }
    
            if ( set.size() > numDupLimit )
            {
                BTree tree = convertToBTree( set );
                BTreeRedirect redirect = new BTreeRedirect( tree.getRecid() );
                return putRaw( key, redirect, true );
            }
            else
            {
                return putRaw( key, set, true );
            }
        }
        
        if ( storedValues instanceof BTreeRedirect )
        {
            BTree tree = getBTree( ( BTreeRedirect ) storedValues );
            while ( values.next() )
            {
                Object val = values.get();
                
                if ( insertDupIntoBTree( tree, val ) )
                {
                    count++;
                }
            }

            return storedValues;
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }


    /**
     * @see Table#remove(java.lang.Object,
     * java.lang.Object)
     */
    public Object remove( Object key, Object value ) throws IOException
    {
        if ( ! allowsDuplicates )
        {
            Object oldValue = getRaw( key );
        
            // Remove the value only if it is the same as value.
            if ( oldValue != null && oldValue.equals( value ) )
            {
                removeRaw( key );
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
                if ( set.isEmpty() )
                {
                    removeRaw( key );
                }
                else
                {
                    putRaw( key, set, true );
                }

                // Decrement counter if removal occurs.
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
                    removeRaw( key );
                }
                
                count--;
                return value;
            }
            
            return null;
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }


    /**
     * @see Table#remove(Object, Cursor
     */
    public Object remove( Object key, Cursor<Object> values ) throws IOException
    {
        /*
         * If we do not allow dupliicates call the single remove using the
         * first value in the enumeration if it exists.  If it does not we
         * just return null without doing anything.  If more than one value
         * is in the enumeration than we blow a UnsupportedOperationException.
         */
        if ( !allowsDuplicates )
        {
            if ( values.next() )
            {
                Object value = values.get();

                if ( values.next() )
                {
                    throw new IllegalStateException( "Attempting to remove duplicate keys from table " + name
                        + " which does not support duplicates" );
                }

                return remove( key, value );
            }

            return null;
        }

        Object storedValues = getRaw( key );
        
        if ( storedValues == null )
        {
            return null;
        }
        
        if ( storedValues instanceof TreeSet )
        {
            /*
             * Here the table allows duplicates so we get the TreeSet from the 
             * Table holding all the duplicate key values or return null if it
             * does not exist for key - nothing to do here.
             */
            TreeSet set = ( TreeSet ) storedValues;
    
            /*
             * So we have a valid TreeSet with values in it.  We check if each value
             * is in the set and remove it if it is present.  We decrement the 
             * counter while doing so.
             */
            Object firstValue = null;
            while ( values.next() )
            {
                Object val = values.get();
    
                // get the first value
                if ( firstValue == null )
                {
                    firstValue = val;
                }

                if ( set.contains( val ) )
                {
                    set.remove( val );
                    count--;
                }
            }
    
            // Return the raw TreeSet and put the changed one back.
            putRaw( key, set, true );
            return firstValue;
        }
        
        // TODO might be nice to add code here that reverts to a TreeSet
        // if the number of duplicates falls below the numDupLimit value
        if ( storedValues instanceof BTreeRedirect )
        {
            BTree tree = getBTree( ( BTreeRedirect ) storedValues );
            Object first = null;
            while ( values.next() )
            {
                Object val = values.get();
                
                if ( removeDupFromBTree( tree, val ) )
                {
                    count--;
                    
                    if ( first == null )
                    {
                        first = val;
                    }
                }
            }
            
            return first;
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }


    /**
     * @see Table#remove(java.lang.Object)
     */
    public Object remove( Object key ) throws IOException
    {
        Object returned = removeRaw( key );

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
            TreeSet set = ( TreeSet ) returned;
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


    /**
     * @see Table#listValues(java.lang.Object)
     */
    public Cursor<Object> listValues( Object key ) throws IOException
    {
        if ( !allowsDuplicates )
        {
            Object value = get( key );

            if ( null == value )
            {
                return new EmptyCursor();
            }
            else
            {
                return new SingletonCursor( value );
            }
        }

        Object values = getRaw( key );
        
        if ( values == null )
        {
            return new EmptyCursor();
        }
        
        if ( values instanceof TreeSet )
        {
            TreeSet set = ( TreeSet ) values;
            return new IteratorCursor( set.iterator() );
        }
        
        if ( values instanceof BTreeRedirect )
        {
            BTree tree = getBTree( ( BTreeRedirect ) values );
            return new BTreeCursor( tree );
        }
        
        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }


    // ------------------------------------------------------------------------
    // listTuple Overloads 
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#listTuples()
     */
    public Cursor<Tuple> listTuples() throws IOException
    {
        Cursor<Tuple> list = new NoDupsCursor( browserFactory );

        if ( allowsDuplicates )
        {
            return new DupsEnumeration( this, ( NoDupsEnumeration ) list );
        }

        return list;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#listTuples(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public Cursor<Tuple> listTuples( Object key ) throws IOException
    {
        // Handle single and zero value returns without duplicates enabled
        if ( !allowsDuplicates )
        {
            Object val = getRaw( key );

            if ( null == val )
            {
                return new EmptyCursor();
            }
            else
            {
                return new SingletonCursor( new Tuple( key, getRaw( key ) ) );
            }
        }

        Object values = getRaw( key );

        if ( values == null )
        {
            return new EmptyCursor();
        }
        
        if ( values instanceof TreeSet )
        {
            TreeSet set = ( TreeSet ) values;
            Object[] objs = new Object[set.size()];
            objs = set.toArray( objs );
            return new ValueArrayCursor( key, objs );
        }
        
        if ( values instanceof BTreeRedirect )
        {
            return new BTreeTupleEnumeration( getBTree( ( BTreeRedirect ) values ), key );
        }

        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
    }


    /**
     * @see Table#listTuples(java.lang.Object,
     * boolean)
     */
    public Cursor<Tuple> listTuples( Object key, boolean isGreaterThan ) throws IOException
    {
        Cursor<Tuple> list = null;

        if ( isGreaterThan )
        {
            // JdbmTupleBrowser browser = new JdbmTupleBrowser( bt.browse( key ) );
            // list = new NoDupsEnumeration( browser, isGreaterThan );

            list = new NoDupsCursor( browserFactory, key );
        }
        else
        {
            /* According to the jdbm docs a browser is positioned right
             * before a key greater than or equal to key.  getNext() will
             * return the next tuple with a key greater than or equal to
             * key.  getPrevious() used in descending scans for less than
             * for equal to comparisions will not.  We need to advance
             * forward once and check if the returned Tuple key equals
             * key.  If it does then we do nothing feeding in the browser
             * to the NoDupsCursor.  If it does not we call getPrevious and
             * pass it into the NoDupsCursor constructor.
             */
            jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
            TupleBrowser browser = bt.browse( key );

            if ( browser.getNext( tuple ) )
            {
                Object greaterKey = tuple.getKey();

                if ( 0 != comparator.compareKey( key, greaterKey ) )
                {
                    // Make sure we don't return greaterKey in cursor
                    browser.getPrevious( tuple );
                }
            }

            // If greaterKey != key above then it will not be returned.
//            list = new NoDupsCursor( new JdbmTupleBrowser( browser ), isGreaterThan );
            list = new NoDupsCursor( browserFactory, isGreaterThan );
        }

        if ( allowsDuplicates )
        {
            list = new DupsEnumeration( this, ( NoDupsEnumeration ) list );
        }

        return list;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#listTuples(java.lang.Object,
     * java.lang.Object, boolean)
     */
    @SuppressWarnings("unchecked")
    public Cursor<Tuple> listTuples( Object key, Object val, boolean isGreaterThan ) throws IOException
    {
        if ( !allowsDuplicates )
        {
            throw new IllegalStateException( "Cannot list tuples over duplicates on table that " +
                    "does not support duplicates." );
        }

        Object values = getRaw( key );
        
        if ( values == null )
        {
            return new EmptyCursor();
        }

        if ( values instanceof TreeSet )
        {
            TreeSet set = ( TreeSet ) values;
    
            if ( isGreaterThan )
            {
                Set tailSet = set.tailSet( val );
                
                if ( tailSet.isEmpty() )
                {
                    return new EmptyCursor();
                }
                
                Object[] objs = new Object[tailSet.size()];
                objs = tailSet.toArray( objs );
                return new ValueArrayCursor( key, objs );
            }
            else
            {
                // Get all values from the smallest upto val and put them into
                // a list.  They will be in ascending order so we need to reverse
                // the list after adding val which is not included in headSet.
                SortedSet headset = set.headSet( val );
                List list = new ArrayList( headset.size() + 1 );
                list.addAll( headset );
    
                // Add largest value (val) if it is in the set.  TreeSet.headSet
                // does not get val if val is in the set.  So we add it now to
                // the end of the list.  List is now ascending from smallest to
                // val
                if ( set.contains( val ) )
                {
                    list.add( val );
                }
    
                // Reverse the list now we have descending values from val to the
                // smallest value that key has.  Return tuple cursor over list.
                Collections.reverse( list );
                return new ValueArrayCursor( key, list );
            }
        }
        
        if ( values instanceof BTreeRedirect )
        {
            return new BTreeTupleEnumeration( getBTree( ( BTreeRedirect ) values ), 
                comparator.getValueComparator(), key, val, isGreaterThan );
        }

        throw new IllegalStateException( "When using duplicate keys either a TreeSet or BTree is used for values." );
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
     * Renders a key using the renderer associated with this table.
     *
     * @param obj the key to render.
     * @return the rendered String representation of obj
     */
    private String renderKey( Object obj )
    {
        StringBuffer buf = new StringBuffer();

        buf.append( "\'" );
        if ( null == renderer )
        {
            buf.append( obj.toString() );
        }
        else
        {
            buf.append( renderer.getKeyString( obj ) );
        }

        buf.append( "\'" );
        return buf.toString();
    }


    /**
     * Gets a Tuple value from the btree.
     *
     * @param key the key of the Tuple to get the value of 
     * @return the raw value object from the btree
     * @throws IOException if there are any problems accessing the btree.
     */
    private Object getRaw( Object key ) throws IOException
    {
        Object val;

        if ( null == key )
        {
            return null;
        }

        if ( !allowsDuplicates )
        {
            val = bt.find( key );
        }
        else
        {
            val = bt.find( key );
        }

        return val;
    }


    /**
     * @todo what's the reason for keeping this?
     *
     * Puts a Tuple into the btree.
     *
     * @param key the key of the Tuple to put
     * @param value the value of the Tuple to put
     * @param doReplace whether or not to replace the object if it exists
     * @return the raw value object removed from the btree on replacement
     * @throws IOException if there are any problems accessing the btree.
     */
    private Object putRaw( Object key, Object value, boolean doReplace ) throws IOException
    {
        return bt.insert( key, value, doReplace );
    }


    /**
     * @todo what's the reason for keeping this?
     *
     * Removes a entry from the btree while wrapping any IOExceptions with a
     * NamingException.
     *
     * @param key the key of the Tuple to remove
     * @return the raw value object removed from the btree
     * @throws IOException if there are any problems accessing the btree.
     */
    private Object removeRaw( Object key ) throws IOException
    {
        return bt.remove( key );
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


    private Object firstKey ( BTree tree ) throws IOException
    {
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
        boolean success = tree.browse().getNext( tuple );
            
        if ( success )
        {
            return tuple.getKey();
        }
        else
        {
            return null;
        }
    }

    
    private boolean btreeHas( BTree tree, Object key, boolean isGreaterThan ) throws IOException
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
                    Object biggerKey = tuple.getKey();
                    if ( comparator.compareValue( key, biggerKey ) == 0 )
                    {
                        return true;
                    }
                }
                return false;
            }
        }
    }


    private boolean btreeHas( BTree tree, Object key ) throws IOException
    {
        jdbm.helper.Tuple tuple = new jdbm.helper.Tuple();
        
        TupleBrowser browser = tree.browse( key );
        boolean success = browser.getNext( tuple );
        if ( success )
        {
            if ( comparator.compareValue( key, tuple.getKey() ) == 0 )
            {
                return true;
            }
        }

        return false;
    }

    
    private boolean insertDupIntoBTree( BTree tree, Object value ) throws IOException
    {
        Object replaced = tree.insert( value, EMPTY_BYTES, true );
        return null == replaced;
    }
    

    private boolean removeDupFromBTree( BTree tree, Object value ) throws IOException
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
    
    
    private Object removeAll( BTree tree ) throws IOException
    {
        Object first = null;
        jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
        TupleBrowser browser = tree.browse();
        while( browser.getNext( jdbmTuple ) )
        {
            tree.remove( jdbmTuple.getKey() );
            if ( first == null )
            {
                first = jdbmTuple.getKey();
            }
        }

        return first;
    }
}

