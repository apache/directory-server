/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.TupleBrowser;

import org.apache.directory.server.core.partition.impl.btree.DupsEnumeration;
import org.apache.directory.server.core.partition.impl.btree.KeyOnlyComparator;
import org.apache.directory.server.core.partition.impl.btree.NoDupsEnumeration;
import org.apache.directory.server.core.partition.impl.btree.Table;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.partition.impl.btree.TupleComparator;
import org.apache.directory.server.core.partition.impl.btree.TupleEnumeration;
import org.apache.directory.server.core.partition.impl.btree.TupleRenderer;
import org.apache.directory.server.core.schema.SerializableComparator;

import org.apache.directory.shared.ldap.util.EmptyEnumeration;
import org.apache.directory.shared.ldap.util.SingletonEnumeration;


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

    /** */
    private final String name;
    /** */
    private final RecordManager recMan;
    /** */
    private final boolean allowsDuplicates;
    /** */
    private final TupleComparator comparator;

    /** */
    private int count = 0;
    /** */
    private BTree bt;
    /** */
    private TupleRenderer renderer;


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
    public JdbmTable(String name, boolean allowsDuplicates, RecordManager manager, TupleComparator comparator)
        throws NamingException
    {
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
                count = ( ( Integer ) recMan.fetch( recId ) ).intValue();
            }
            else
            {
                bt = BTree.createInstance( recMan, comparator.getKeyComparator() );
                recId = bt.getRecid();
                recMan.setNamedObject( name, recId );
                recId = recMan.insert( new Integer( 0 ) );
                recMan.setNamedObject( name + SZSUFFIX, recId );
            }
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
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
    public JdbmTable(String name, RecordManager manager, SerializableComparator keyComparator) throws NamingException
    {
        this( name, false, manager, new KeyOnlyComparator( keyComparator ) );
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
    public int count( Object key, boolean isGreaterThan ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * @see Table#count(java.lang.Object)
     */
    public int count( Object key ) throws NamingException
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

        TreeSet set = ( TreeSet ) getRaw( key );

        if ( set != null )
        {
            return set.size();
        }

        return 0;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#count()
     */
    public int count() throws NamingException
    {
        return count;
    }


    // ------------------------------------------------------------------------
    // get/has/put/remove Methods and Overloads
    // ------------------------------------------------------------------------

    /**
     * @see Table#get(java.lang.Object)
     */
    public Object get( Object key ) throws NamingException
    {
        if ( allowsDuplicates )
        {
            TreeSet set = ( TreeSet ) getRaw( key );
            if ( null == set || set.size() == 0 )
            {
                return null;
            }
            else
            {
                return set.first();
            }
        }

        Object value = getRaw( key );
        return value;
    }


    /**
     * @see Table#has(java.lang.Object,
     * java.lang.Object, boolean)
     */
    public boolean has( Object key, Object val, boolean isGreaterThan ) throws NamingException
    {
        TreeSet set = null;
        SortedSet subset = null;

        if ( !allowsDuplicates )
        {
            Object rval = getRaw( key );

            // key does not exist so return nothing
            if ( null == rval )
            {
                return false;
            }
            // val == val return tuple
            else if ( val.equals( rval ) )
            {
                return true;
            }
            // val >= val and test is for greater then return tuple
            else if ( comparator.compareValue( rval, val ) >= 1 && isGreaterThan )
            {
                return true;
            }
            // val <= val and test is for lesser then return tuple
            else if ( comparator.compareValue( rval, val ) <= 1 && !isGreaterThan )
            {
                return true;
            }

            return false;
        }

        set = ( TreeSet ) getRaw( key );

        if ( null == set || set.size() == 0 )
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

        if ( subset.size() > 0 || set.contains( val ) )
        {
            return true;
        }

        return false;
    }


    /**
     * @see Table#has(java.lang.Object, boolean)
     */
    public boolean has( Object key, boolean isGreaterThan ) throws NamingException
    {
        try
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
            TupleBrowser browser = null;
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
                while ( browser.getNext( tuple ) )
                {
                    if ( comparator.compareKey( tuple.getKey(), key ) <= 0 )
                    {
                        return true;
                    }

                    return false;
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
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }

        return false;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#has(java.lang.Object,
     * java.lang.Object)
     */
    public boolean has( Object key, Object value ) throws NamingException
    {
        if ( allowsDuplicates )
        {
            TreeSet set = ( TreeSet ) getRaw( key );

            if ( null == set )
            {
                return false;
            }

            return set.contains( value );
        }

        Object obj = getRaw( key );

        if ( null == obj )
        {
            return false;
        }

        return obj.equals( value );
    }


    /**
     * @see Table#has(java.lang.Object)
     */
    public boolean has( Object key ) throws NamingException
    {
        return getRaw( key ) != null;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#put(java.lang.Object,
     * java.lang.Object)
     */
    public Object put( Object key, Object value ) throws NamingException
    {
        Object replaced = null;

        if ( allowsDuplicates )
        {
            TreeSet set = ( TreeSet ) getRaw( key );

            if ( null == set )
            {
                set = new TreeSet( comparator.getValueComparator() );
            }
            else if ( set.contains( value ) )
            {
                return value;
            }

            set.add( value );
            putRaw( key, set, true );
            count++;
            return null;
        }

        replaced = putRaw( key, value, true );

        if ( null == replaced )
        {
            count++;
        }

        return replaced;
    }


    /**
     * @see Table#put(java.lang.Object,
     * javax.naming.NamingEnumeration)
     */
    public Object put( Object key, NamingEnumeration values ) throws NamingException
    {
        TreeSet set = null;

        /*
         * If we do not allow duplicates call the single add put using the
         * first value in the enumeration if it exists.  If it does not we
         * just return null without doing anything.  If more than one value
         * is in the enumeration than we blow a UnsupportedOperationException.
         */
        if ( !allowsDuplicates )
        {
            if ( values.hasMore() )
            {
                Object value = values.next();

                if ( values.hasMore() )
                {
                    throw new UnsupportedOperationException( "Attempting to put duplicate keys into table " + name
                        + " which does not support duplicates" );
                }

                return put( key, value );
            }

            return null;
        }

        /*
         * Here the table allows duplicates so we get the TreeSet from the 
         * Table holding all the duplicate key values or create one if it
         * does not exist for key.  We check if the value is present and
         * if it is we add it and increment the table entry counter.
         */
        set = ( TreeSet ) getRaw( key );

        if ( null == set )
        {
            set = new TreeSet( comparator.getValueComparator() );
        }

        while ( values.hasMore() )
        {
            Object val = values.next();

            if ( !set.contains( val ) )
            {
                set.add( val );
                count++;
            }
        }

        // Return the raw TreeSet
        return putRaw( key, set, true );
    }


    /**
     * @see Table#remove(java.lang.Object,
     * java.lang.Object)
     */
    public Object remove( Object key, Object value ) throws NamingException
    {
        if ( allowsDuplicates )
        {
            TreeSet set = ( TreeSet ) getRaw( key );

            if ( null == set )
            {
                return null;
            }

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

        // Remove the value only if it is the same as value.
        if ( getRaw( key ).equals( value ) )
        {
            return removeRaw( key );
        }

        return null;
    }


    /**
     * @see Table#remove(java.lang.Object,
     * javax.naming.NamingEnumeration)
     */
    public Object remove( Object key, NamingEnumeration values ) throws NamingException
    {
        TreeSet set = null;

        /*
         * If we do not allow dupliicates call the single remove using the
         * first value in the enumeration if it exists.  If it does not we
         * just return null without doing anything.  If more than one value
         * is in the enumeration than we blow a UnsupportedOperationException.
         */
        if ( !allowsDuplicates )
        {
            if ( values.hasMore() )
            {
                Object value = values.next();

                if ( values.hasMore() )
                {
                    throw new UnsupportedOperationException( "Attempting to put duplicate keys into table " + name
                        + " which does not support duplicates" );
                }

                return remove( key, value );
            }

            return null;
        }

        /*
         * Here the table allows duplicates so we get the TreeSet from the 
         * Table holding all the duplicate key values or return null if it
         * does not exist for key - nothing to do here.
         */
        set = ( TreeSet ) getRaw( key );
        if ( null == set )
        {
            return null;
        }

        /*
         * So we have a valid TreeSet with values in it.  We check if each value
         * is in the set and remove it if it is present.  We decrement the 
         * counter while doing so.
         */
        while ( values.hasMore() )
        {
            Object val = values.next();

            if ( !set.contains( val ) )
            {
                set.remove( val );
                count--;
            }
        }

        // Return the raw TreeSet and put the changed one back.
        return putRaw( key, set, true );
    }


    /**
     * @see Table#remove(java.lang.Object)
     */
    public Object remove( Object key ) throws NamingException
    {
        Object returned = removeRaw( key );

        if ( null == returned )
        {
            return null;
        }

        if ( allowsDuplicates )
        {
            TreeSet set = ( TreeSet ) returned;
            this.count -= set.size();
            return set.first();
        }

        this.count--;
        return returned;
    }


    /**
     * @see Table#listValues(java.lang.Object)
     */
    public NamingEnumeration listValues( Object key ) throws NamingException
    {
        TreeSet set = null;

        if ( !allowsDuplicates )
        {
            Object value = get( key );

            if ( null == value )
            {
                return new EmptyEnumeration();
            }
            else
            {
                return new SingletonEnumeration( value );
            }
        }

        set = ( TreeSet ) getRaw( key );
        if ( null == set )
        {
            return new EmptyEnumeration();
        }

        final Iterator list = set.iterator();
        return new NamingEnumeration()
        {
            public void close()
            {
            }


            public Object nextElement()
            {
                return list.next();
            }


            public Object next()
            {
                return list.next();
            }


            public boolean hasMore()
            {
                return list.hasNext();
            }


            public boolean hasMoreElements()
            {
                return list.hasNext();
            }
        };
    }


    // ------------------------------------------------------------------------
    // listTuple Overloads 
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#listTuples()
     */
    public NamingEnumeration listTuples() throws NamingException
    {
        NamingEnumeration list = null;

        try
        {
            JdbmTupleBrowser browser = new JdbmTupleBrowser( bt.browse() );
            list = new NoDupsEnumeration( browser, true );
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }

        if ( allowsDuplicates )
        {
            return new DupsEnumeration( ( NoDupsEnumeration ) list );
        }

        return list;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#listTuples(java.lang.Object)
     */
    public NamingEnumeration listTuples( Object key ) throws NamingException
    {
        TreeSet set = null;

        // Handle single and zero value returns without duplicates enabled
        if ( !allowsDuplicates )
        {
            Object val = getRaw( key );

            if ( null == val )
            {
                return new EmptyEnumeration();
            }
            else
            {
                return new SingletonEnumeration( new Tuple( key, getRaw( key ) ) );
            }
        }

        set = ( TreeSet ) getRaw( key );
        if ( set == null )
        {
            return new EmptyEnumeration();
        }

        return new TupleEnumeration( key, set.iterator() );
    }


    /**
     * @see Table#listTuples(java.lang.Object,
     * boolean)
     */
    public NamingEnumeration listTuples( Object key, boolean isGreaterThan ) throws NamingException
    {
        NamingEnumeration list = null;

        try
        {
            if ( isGreaterThan )
            {
                JdbmTupleBrowser browser = new JdbmTupleBrowser( bt.browse( key ) );
                list = new NoDupsEnumeration( browser, isGreaterThan );
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
                list = new NoDupsEnumeration( new JdbmTupleBrowser( browser ), isGreaterThan );
            }
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException( "Failed to get TupleBrowser on table " + name + " using key "
                + renderKey( key ) );
            ne.setRootCause( e );
            throw ne;
        }

        if ( allowsDuplicates )
        {
            list = new DupsEnumeration( ( NoDupsEnumeration ) list );
        }

        return list;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Table#listTuples(java.lang.Object,
     * java.lang.Object, boolean)
     */
    public NamingEnumeration listTuples( Object key, Object val, boolean isGreaterThan ) throws NamingException
    {
        TreeSet set = null;

        if ( !allowsDuplicates )
        {
            Object rval = getRaw( key );

            if ( null == rval ) // key does not exist so return nothing
            {
                return new EmptyEnumeration();
            }
            else if ( val.equals( rval ) ) // val == rval return tuple
            {
                return new SingletonEnumeration( new Tuple( key, val ) );
            }
            // val >= val and test is for greater then return tuple
            else if ( comparator.compareValue( val, rval ) >= 1 && isGreaterThan )
            {
                return new SingletonEnumeration( new Tuple( key, val ) );
            }
            // val <= val and test is for lesser then return tuple
            else if ( comparator.compareValue( val, rval ) <= 1 && !isGreaterThan )
            {
                return new SingletonEnumeration( new Tuple( key, val ) );
            }

            return new EmptyEnumeration();
        }

        set = ( TreeSet ) getRaw( key );
        if ( set == null )
        {
            return new EmptyEnumeration();
        }

        if ( isGreaterThan )
        {
            return new TupleEnumeration( key, set.tailSet( val ).iterator() );
        }
        else
        {
            // Get all values from the smallest upto val and put them into
            // a list.  They will be in ascending order so we need to reverse
            // the list after adding val which is not included in headSet.
            SortedSet headset = set.headSet( val );
            ArrayList list = new ArrayList( set.size() + 1 );
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
            return new TupleEnumeration( key, list.iterator() );
        }
    }


    // ------------------------------------------------------------------------
    // Maintenance Operations 
    // ------------------------------------------------------------------------

    /**
     * @see Table#close()
     */
    public synchronized void close() throws NamingException
    {
        sync();
    }


    /**
     * Synchronizes the buffers with disk.
     *
     * @throws NamingException if errors are encountered on the flush
     */
    public void sync() throws NamingException
    {
        try
        {
            long recId = recMan.getNamedObject( name + SZSUFFIX );

            if ( 0 == recId )
            {
                recId = recMan.insert( new Integer( count ) );
            }
            else
            {
                recMan.update( recId, new Integer( count ) );
            }
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
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
     * Gets a Tuple value from the btree while wrapping any IOExceptions with a 
     * NamingException.
     *
     * @param key the key of the Tuple to get the value of 
     * @return the raw value object from the btree
     * @throws NamingException if there are any problems accessing the btree.
     */
    private Object getRaw( Object key ) throws NamingException
    {
        Object val = null;

        if ( null == key )
        {
            return null;
        }

        try
        {
            if ( !allowsDuplicates )
            {
                val = bt.find( key );
            }
            else
            {
                val = bt.find( key );
            }
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }

        return val;
    }


    /**
     * Puts a Tuple into the btree while wrapping any IOExceptions with a 
     * NamingException.
     *
     * @param key the key of the Tuple to put
     * @param value the value of the Tuple to put
     * @param doReplace whether or not to replace the object if it exists
     * @return the raw value object removed from the btree on replacement
     * @throws NamingException if there are any problems accessing the btree.
     */
    private Object putRaw( Object key, Object value, boolean doReplace ) throws NamingException
    {
        Object val = null;

        try
        {
            val = bt.insert( key, value, doReplace );
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }

        return val;
    }


    /**
     * Removes a entry from the btree while wrapping any IOExceptions with a 
     * NamingException.
     *
     * @param key the key of the Tuple to remove
     * @return the raw value object removed from the btree
     * @throws NamingException if there are any problems accessing the btree.
     */
    private Object removeRaw( Object key ) throws NamingException
    {
        Object val = null;

        try
        {
            val = bt.remove( key );
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }

        return val;
    }
}
