/**
 * JDBM LICENSE v1.00
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "JDBM" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Cees de Groot.  For written permission,
 *    please contact cg@cdegroot.com.
 *
 * 4. Products derived from this Software may not be called "JDBM"
 *    nor may "JDBM" appear in their names without prior written
 *    permission of Cees de Groot.
 *
 * 5. Due credit should be given to the JDBM Project
 *    (http://jdbm.sourceforge.net/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE JDBM PROJECT AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * CEES DE GROOT OR ANY CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2001 (C) Alex Boisvert. All Rights Reserved.
 * Contributions are Copyright (C) 2001 by their associated contributors.
 *
 */

package jdbm.btree;


import jdbm.helper.Serializer;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.directory.server.i18n.I18n;


/**
 * Page of a Btree.
 * <p>
 * The page contains a number of key-value pairs.  Keys are ordered to allow
 * dichotomic search.
 * <p>
 * If the page is a leaf page, the keys and values are user-defined and
 * represent entries inserted by the user.
 * <p>
 * If the page is non-leaf, each key represents the greatest key in the
 * underlying BPages and the values are recids pointing to the children BPages.
 * The only exception is the rightmost BPage, which is considered to have an
 * "infinite" key value, meaning that any insert will be to the left of this
 * pseudo-key
 *
 * @author <a href="mailto:boisvert@intalio.com">Alex Boisvert</a>
 * @version $Id: BPage.java,v 1.6 2003/09/21 15:46:59 boisvert Exp $
 */
public final class BPage<K, V> implements Serializer
{
    private static final boolean DEBUG = false;

    /** Version id for serialization. */
    final static long serialVersionUID = 1L;

    /** Parent B+Tree. */
    transient BTree btree;

    /** This BPage's record ID in the PageManager. */
    protected transient long recid;

    /** Flag indicating if this is a leaf BPage. */
    protected boolean isLeaf;

    /** Keys of children nodes */
    protected K[] keys;

    /** Values associated with keys.  (Only valid if leaf BPage) */
    protected V[] values;

    /** Children pages (recids) associated with keys.  (Only valid if non-leaf BPage) */
    protected long[] children;

    /** Index of first used item at the page */
    protected int first;

    /** Previous leaf BPage (only if this BPage is a leaf) */
    protected long previous;

    /** Next leaf BPage (only if this BPage is a leaf) */
    protected long next;


    /**
     * No-argument constructor used by serialization.
     */
    public BPage()
    {
        // empty
    }


    /**
     * Root page overflow constructor
     */
    @SuppressWarnings("unchecked")
    BPage( BTree btree, BPage<K, V> root, BPage<K, V> overflow ) throws IOException
    {
        this.btree = btree;

        isLeaf = false;

        first = btree.pageSize - 2;

        keys = (K[])new Object[btree.pageSize];
        keys[btree.pageSize - 2] = overflow.getLargestKey();
        keys[btree.pageSize - 1] = root.getLargestKey();

        children = new long[btree.pageSize];
        children[btree.pageSize - 2] = overflow.recid;
        children[btree.pageSize - 1] = root.recid;

        recid = btree.recordManager.insert( this, this );
    }


    /**
     * Root page (first insert) constructor.
     */
    @SuppressWarnings("unchecked") // Cannot create an array of generic objects
    BPage( BTree btree, K key, V value ) throws IOException
    {
        this.btree = btree;

        isLeaf = true;

        first = btree.pageSize - 2;

        keys = (K[])new Object[btree.pageSize];
        keys[btree.pageSize - 2] = key;
        keys[btree.pageSize - 1] = null; // I am the root BPage for now

        values = (V[])new Object[btree.pageSize];
        values[btree.pageSize - 2] = value;
        values[btree.pageSize - 1] = null; // I am the root BPage for now

        recid = btree.recordManager.insert( this, this );
    }


    /**
     * Overflow page constructor.  Creates an empty BPage.
     */
    @SuppressWarnings("unchecked") // Cannot create an array of generic objects
    BPage( BTree btree, boolean isLeaf ) throws IOException
    {
        this.btree = btree;

        this.isLeaf = isLeaf;

        // page will initially be half-full
        first = btree.pageSize / 2;

        keys = (K[])new Object[btree.pageSize];
        
        if ( isLeaf )
        {
            values = (V[])new Object[btree.pageSize];
        }
        else
        {
            children = new long[btree.pageSize];
        }

        recid = btree.recordManager.insert( this, this );
    }


    /**
     * Get largest key under this BPage.  Null is considered to be the
     * greatest possible key.
     */
    K getLargestKey()
    {
        return keys[btree.pageSize - 1];
    }


    /**
     * Return true if BPage is empty.
     */
    boolean isEmpty()
    {
        if ( isLeaf )
        {
            return ( first == values.length - 1 );
        }
        else
        {
            return ( first == children.length - 1 );
        }
    }


    /**
     * Return true if BPage is full.
     */
    boolean isFull()
    {
        return ( first == 0 );
    }


    /**
     * Find the object associated with the given key.
     *
     * @param height Height of the current BPage (zero is leaf page)
     * @param key The key
     * @return TupleBrowser positionned just before the given key, or before
     *                      next greater key if key isn't found.
     */
    TupleBrowser<K, V> find( int height, K key ) throws IOException
    {
        int index = this.findChildren( key );
        BPage<K, V> child = this;

        while ( !child.isLeaf )
        {
            // non-leaf BPage
            child = child.loadBPage( child.children[index] );
            index = child.findChildren( key );
        }

        return new Browser( child, index );
    }


    /**
     * Find first entry and return a browser positioned before it.
     *
     * @return TupleBrowser positionned just before the first entry.
     */
    TupleBrowser<K, V> findFirst() throws IOException
    {
        if ( isLeaf )
        {
            return new Browser( this, first );
        }
        else
        {
            BPage<K, V> child = childBPage( first );
            
            return child.findFirst();
        }
    }


    /**
     * Insert the given key and value.
     * <p>
     * Since the Btree does not support duplicate entries, the caller must
     * specify whether to replace the existing value.
     *
     * @param height Height of the current BPage (zero is leaf page)
     * @param key Insert key
     * @param value Insert value
     * @param replace Set to true to replace the existing value, if one exists.
     * @return Insertion result containing existing value OR a BPage if the key
     *         was inserted and provoked a BPage overflow.
     */
    InsertResult<K, V> insert( int height, K key, V value, boolean replace ) throws IOException
    {
        InsertResult<K, V> result;
        long overflow;

        int index = findChildren( key );

        height -= 1;
        
        if ( height == 0 )
        {

            result = new InsertResult<K, V>();

            // inserting on a leaf BPage
            overflow = -1;
            
            if ( DEBUG )
            {
                System.out.println( "Bpage.insert() Insert on leaf Bpage key=" + key + " value=" + value + " index="
                    + index );
            }
            
            if ( compare( key, keys[index] ) == 0 )
            {
                // key already exists
                if ( DEBUG )
                {
                    System.out.println( "Bpage.insert() Key already exists." );
                }
                
                result.existing = values[index];
                
                if ( replace )
                {
                    values[index] = value;
                    btree.recordManager.update( recid, this, this );
                }
                
                // return the existing key
                return result;
            }
        }
        else
        {
            // non-leaf BPage
            BPage<K, V> child = childBPage( index );
            result = child.insert( height, key, value, replace );

            if ( result.existing != null )
            {
                // return existing key, if any.
                return result;
            }

            if ( result.overflow == null )
            {
                // no overflow means we're done with insertion
                return result;
            }

            // there was an overflow, we need to insert the overflow page
            // on this BPage
            if ( DEBUG )
            {
                System.out.println( "BPage.insert() Overflow page: " + result.overflow.recid );
            }
            
            key = result.overflow.getLargestKey();
            overflow = result.overflow.recid;

            // update child's largest key
            keys[index] = child.getLargestKey();

            // clean result so we can reuse it
            result.overflow = null;
        }

        // if we get here, we need to insert a new entry on the BPage
        // before children[ index ]
        if ( !isFull() )
        {
            if ( height == 0 )
            {
                insertEntry( this, index - 1, key, value );
            }
            else
            {
                insertChild( this, index - 1, key, overflow );
            }
            
            btree.recordManager.update( recid, this, this );
            return result;
        }

        // page is full, we must divide the page
        int half = btree.pageSize >> 1;
        BPage<K, V> newPage = new BPage<K, V>( btree, isLeaf );
        
        if ( index < half )
        {
            // move lower-half of entries to overflow BPage,
            // including new entry
            if ( DEBUG )
            {
                System.out
                    .println( "Bpage.insert() move lower-half of entries to overflow BPage, including new entry." );
            }
            
            if ( height == 0 )
            {
                copyEntries( this, 0, newPage, half, index );
                setEntry( newPage, half + index, key, value );
                copyEntries( this, index, newPage, half + index + 1, half - index - 1 );
            }
            else
            {
                copyChildren( this, 0, newPage, half, index );
                setChild( newPage, half + index, key, overflow );
                copyChildren( this, index, newPage, half + index + 1, half - index - 1 );
            }
        }
        else
        {
            // move lower-half of entries to overflow BPage,
            // new entry stays on this BPage
            if ( DEBUG )
            {
                System.out.println( "Bpage.insert() move lower-half of entries to overflow BPage. New entry stays" );
            }
            
            if ( height == 0 )
            {
                copyEntries( this, 0, newPage, half, half );
                copyEntries( this, half, this, half - 1, index - half );
                setEntry( this, index - 1, key, value );
            }
            else
            {
                copyChildren( this, 0, newPage, half, half );
                copyChildren( this, half, this, half - 1, index - half );
                setChild( this, index - 1, key, overflow );
            }
        }

        first = half - 1;

        // nullify lower half of entries
        for ( int i = 0; i < first; i++ )
        {
            if ( height == 0 )
            {
                setEntry( this, i, null, null );
            }
            else
            {
                setChild( this, i, null, -1 );
            }
        }

        if ( isLeaf )
        {
            // link newly created BPage
            newPage.previous = previous;
            newPage.next = recid;
            
            if ( previous != 0 )
            {
                BPage<K, V> previousBPage = loadBPage( previous );
                previousBPage.next = newPage.recid;
                btree.recordManager.update( previous, previousBPage, this );
            }
            
            previous = newPage.recid;
        }

        btree.recordManager.update( recid, this, this );
        btree.recordManager.update( newPage.recid, newPage, this );

        result.overflow = newPage;
        return result;
    }


    /**
     * Remove the entry associated with the given key.
     *
     * @param height Height of the current BPage (zero is leaf page)
     * @param key Removal key
     * @return Remove result object
     */
    RemoveResult<V> remove( int height, K key ) throws IOException
    {
        RemoveResult<V> result;

        int half = btree.pageSize / 2;
        int index = findChildren( key );

        height -= 1;
        
        if ( height == 0 )
        {
            // remove leaf entry
            if ( compare( keys[index], key ) != 0 )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_514, key ) );
            }
            
            result = new RemoveResult<V>();
            result.value = values[index];
            removeEntry( this, index );

            // update this BPage
            btree.recordManager.update( recid, this, this );
        }
        else
        {
            // recurse into Btree to remove entry on a children page
            BPage<K, V> child = childBPage( index );
            result = child.remove( height, key );

            // update children
            keys[index] = child.getLargestKey();
            btree.recordManager.update( recid, this, this );

            if ( result.underflow )
            {
                // underflow occured
                if ( child.first != half + 1 )
                {
                    throw new IllegalStateException( I18n.err( I18n.ERR_513, "1" ) );
                }
                
                if ( index < children.length - 1 )
                {
                    // exists greater brother page
                    BPage<K, V> brother = childBPage( index + 1 );
                    int bfirst = brother.first;
                    
                    if ( bfirst < half )
                    {
                        // steal entries from "brother" page
                        int steal = ( half - bfirst + 1 ) / 2;
                        brother.first += steal;
                        child.first -= steal;
                        
                        if ( child.isLeaf )
                        {
                            copyEntries( child, half + 1, child, half + 1 - steal, half - 1 );
                            copyEntries( brother, bfirst, child, 2 * half - steal, steal );
                        }
                        else
                        {
                            copyChildren( child, half + 1, child, half + 1 - steal, half - 1 );
                            copyChildren( brother, bfirst, child, 2 * half - steal, steal );
                        }

                        for ( int i = bfirst; i < bfirst + steal; i++ )
                        {
                            if ( brother.isLeaf )
                            {
                                setEntry( brother, i, null, null );
                            }
                            else
                            {
                                setChild( brother, i, null, -1 );
                            }
                        }

                        // update child's largest key
                        keys[index] = child.getLargestKey();

                        // no change in previous/next BPage

                        // update BPages
                        btree.recordManager.update( recid, this, this );
                        btree.recordManager.update( brother.recid, brother, this );
                        btree.recordManager.update( child.recid, child, this );

                    }
                    else
                    {
                        // move all entries from page "child" to "brother"
                        if ( brother.first != half )
                        {
                            throw new IllegalStateException( I18n.err( I18n.ERR_513, "2" ) );
                        }

                        brother.first = 1;
                        
                        if ( child.isLeaf )
                        {
                            copyEntries( child, half + 1, brother, 1, half - 1 );
                        }
                        else
                        {
                            copyChildren( child, half + 1, brother, 1, half - 1 );
                        }
                        
                        btree.recordManager.update( brother.recid, brother, this );

                        // remove "child" from current BPage
                        if ( isLeaf )
                        {
                            copyEntries( this, first, this, first + 1, index - first );
                            setEntry( this, first, null, null );
                        }
                        else
                        {
                            copyChildren( this, first, this, first + 1, index - first );
                            setChild( this, first, null, -1 );
                        }
                        
                        first += 1;
                        btree.recordManager.update( recid, this, this );

                        // re-link previous and next BPages
                        if ( child.previous != 0 )
                        {
                            BPage<K, V> prev = loadBPage( child.previous );
                            prev.next = child.next;
                            btree.recordManager.update( prev.recid, prev, this );
                        }
                        
                        if ( child.next != 0 )
                        {
                            BPage<K, V> next = loadBPage( child.next );
                            next.previous = child.previous;
                            btree.recordManager.update( next.recid, next, this );
                        }

                        // delete "child" BPage
                        btree.recordManager.delete( child.recid );
                    }
                }
                else
                {
                    // page "brother" is before "child"
                    BPage<K, V> brother = childBPage( index - 1 );
                    int bfirst = brother.first;
                    
                    if ( bfirst < half )
                    {
                        // steal entries from "brother" page
                        int steal = ( half - bfirst + 1 ) / 2;
                        brother.first += steal;
                        child.first -= steal;
                        
                        if ( child.isLeaf )
                        {
                            copyEntries( brother, 2 * half - steal, child, half + 1 - steal, steal );
                            copyEntries( brother, bfirst, brother, bfirst + steal, 2 * half - bfirst - steal );
                        }
                        else
                        {
                            copyChildren( brother, 2 * half - steal, child, half + 1 - steal, steal );
                            copyChildren( brother, bfirst, brother, bfirst + steal, 2 * half - bfirst - steal );
                        }

                        for ( int i = bfirst; i < bfirst + steal; i++ )
                        {
                            if ( brother.isLeaf )
                            {
                                setEntry( brother, i, null, null );
                            }
                            else
                            {
                                setChild( brother, i, null, -1 );
                            }
                        }

                        // update brother's largest key
                        keys[index - 1] = brother.getLargestKey();

                        // no change in previous/next BPage

                        // update BPages
                        btree.recordManager.update( recid, this, this );
                        btree.recordManager.update( brother.recid, brother, this );
                        btree.recordManager.update( child.recid, child, this );

                    }
                    else
                    {
                        // move all entries from page "brother" to "child"
                        if ( brother.first != half )
                        {
                            throw new IllegalStateException( I18n.err( I18n.ERR_513, "3" ) );
                        }

                        child.first = 1;
                        
                        if ( child.isLeaf )
                        {
                            copyEntries( brother, half, child, 1, half );
                        }
                        else
                        {
                            copyChildren( brother, half, child, 1, half );
                        }
                        
                        btree.recordManager.update( child.recid, child, this );

                        // remove "brother" from current BPage
                        if ( isLeaf )
                        {
                            copyEntries( this, first, this, first + 1, index - 1 - first );
                            setEntry( this, first, null, null );
                        }
                        else
                        {
                            copyChildren( this, first, this, first + 1, index - 1 - first );
                            setChild( this, first, null, -1 );
                        }
                        
                        first += 1;
                        btree.recordManager.update( recid, this, this );

                        // re-link previous and next BPages
                        if ( brother.previous != 0 )
                        {
                            BPage<K, V> prev = loadBPage( brother.previous );
                            prev.next = brother.next;
                            btree.recordManager.update( prev.recid, prev, this );
                        }
                        
                        if ( brother.next != 0 )
                        {
                            BPage<K, V> next = loadBPage( brother.next );
                            next.previous = brother.previous;
                            btree.recordManager.update( next.recid, next, this );
                        }

                        // delete "brother" BPage
                        btree.recordManager.delete( brother.recid );
                    }
                }
            }
        }

        // underflow if page is more than half-empty
        result.underflow = first > half;

        return result;
    }


    /**
     * Find the first children node with a key equal or greater than the given
     * key.
     *
     * @return index of first children with equal or greater key.
     */
    private int findChildren( K key )
    {
        int left = first;
        int right = btree.pageSize - 1;

        // binary search
        while ( left < right )
        {
            int middle = ( left + right ) >> 1;
            
            if ( compare( keys[middle], key ) < 0 )
            {
                left = middle + 1;
            }
            else
            {
                right = middle;
            }
        }
        
        return right;
    }


    /**
     * Insert entry at given position.
     */
    private void insertEntry( BPage<K, V> page, int index, K key, V value )
    {
        K[] keys = page.keys;
        V[] values = page.values;
        int start = page.first;
        int count = index - page.first + 1;

        // shift entries to the left
        System.arraycopy( keys, start, keys, start - 1, count );
        System.arraycopy( values, start, values, start - 1, count );
        page.first -= 1;
        keys[index] = key;
        values[index] = value;
    }


    /**
     * Insert child at given position.
     */
    private void insertChild( BPage<K, V> page, int index, K key, long child )
    {
        K[] keys = page.keys;
        long[] children = page.children;
        int start = page.first;
        int count = index - page.first + 1;

        // shift entries to the left
        System.arraycopy( keys, start, keys, start - 1, count );
        System.arraycopy( children, start, children, start - 1, count );
        page.first -= 1;
        keys[index] = key;
        children[index] = child;
    }


    /**
     * Remove entry at given position.
     */
    private void removeEntry( BPage<K, V> page, int index )
    {
        K[] keys = page.keys;
        V[] values = page.values;
        int start = page.first;
        int count = index - page.first;

        System.arraycopy( keys, start, keys, start + 1, count );
        keys[start] = null;
        System.arraycopy( values, start, values, start + 1, count );
        values[start] = null;
        page.first++;
    }


    /**
     * Set the entry at the given index.
     */
    private void setEntry( BPage<K, V> page, int index, K key, V value )
    {
        page.keys[index] = key;
        page.values[index] = value;
    }


    /**
     * Set the child BPage recid at the given index.
     */
    private void setChild( BPage<K, V> page, int index, K key, long recid )
    {
        page.keys[index] = key;
        page.children[index] = recid;
    }


    /**
     * Copy entries between two BPages
     */
    private void copyEntries( BPage<K, V> source, int indexSource, BPage<K, V> dest, int indexDest, int count )
    {
        System.arraycopy( source.keys, indexSource, dest.keys, indexDest, count );
        System.arraycopy( source.values, indexSource, dest.values, indexDest, count );
    }


    /**
     * Copy child BPage recids between two BPages
     */
    private void copyChildren( BPage<K, V> source, int indexSource, BPage<K, V> dest, int indexDest, int count )
    {
        System.arraycopy( source.keys, indexSource, dest.keys, indexDest, count );
        System.arraycopy( source.children, indexSource, dest.children, indexDest, count );
    }


    /**
     * Return the child BPage at given index.
     */
    BPage<K, V> childBPage( int index ) throws IOException
    {
        return loadBPage( children[index] );
    }


    /**
     * Load the BPage at the given recid.
     */
    @SuppressWarnings("unchecked") // The fetch method returns an Object
    private BPage<K, V> loadBPage( long recid ) throws IOException
    {
        BPage<K, V> child = ( BPage<K, V> ) btree.recordManager.fetch( recid, this );
        child.recid = recid;
        child.btree = btree;
        
        return child;
    }


    private final int compare( Object value1, Object value2 )
    {
        if ( value1 == value2 )
        {
            return 0;
        }
        
        if ( value1 == null )
        {
            return 1;
        }
        
        if ( value2 == null )
        {
            return -1;
        }
        
        return btree.comparator.compare( value1, value2 );
    }


    static byte[] readByteArray( ObjectInput in ) throws IOException
    {
        int len = in.readInt();
        
        if ( len < 0 )
        {
            return null;
        }
        
        byte[] buf = new byte[len];
        in.readFully( buf );
        
        return buf;
    }


    static void writeByteArray( ObjectOutput out, byte[] buf ) throws IOException
    {
        if ( buf == null )
        {
            out.writeInt( -1 );
        }
        else
        {
            out.writeInt( buf.length );
            out.write( buf );
        }
    }


    /**
     * Dump the structure of the tree on the screen.  This is used for debugging
     * purposes only.
     */
    private void dump( int height )
    {
        String prefix = "";
        
        for ( int i = 0; i < height; i++ )
        {
            prefix += "    ";
        }
        
        System.out.println( prefix + "-------------------------------------- BPage recid=" + recid );
        System.out.println( prefix + "first=" + first );
        
        for ( int i = 0; i < btree.pageSize; i++ )
        {
            if ( isLeaf )
            {
                System.out.println( prefix + "BPage [" + i + "] " + keys[i] + " " + values[i] );
            }
            else
            {
                System.out.println( prefix + "BPage [" + i + "] " + keys[i] + " " + children[i] );
            }
        }
        
        System.out.println( prefix + "--------------------------------------" );
    }


    /**
     * Recursively dump the state of the BTree on screen.  This is used for
     * debugging purposes only.
     */
    void dumpRecursive( int height, int level ) throws IOException
    {
        height -= 1;
        level += 1;
        
        if ( height > 0 )
        {
            for ( int i = first; i < btree.pageSize; i++ )
            {
                if ( keys[i] == null )
                { 
                    break;
                }
                
                BPage<K, V> child = childBPage( i );
                child.dump( level );
                child.dumpRecursive( height, level );
            }
        }
    }


    /**
     * Assert the ordering of the keys on the BPage.  This is used for testing
     * purposes only.
     */
    private void assertConsistency()
    {
        for ( int i = first; i < btree.pageSize - 1; i++ )
        {
            if ( compare( ( byte[] ) keys[i], ( byte[] ) keys[i + 1] ) >= 0 )
            {
                dump( 0 );
                throw new Error( I18n.err( I18n.ERR_515 ) );
            }
        }
    }


    /**
     * Recursively assert the ordering of the BPage entries on this page
     * and sub-pages.  This is used for testing purposes only.
     */
    void assertConsistencyRecursive( int height ) throws IOException
    {
        assertConsistency();
        
        if ( --height > 0 )
        {
            for ( int i = first; i < btree.pageSize; i++ )
            {
                if ( keys[i] == null )
                {
                    break;
                }
                
                BPage<K, V> child = childBPage( i );
                
                if ( compare( ( byte[] ) keys[i], child.getLargestKey() ) != 0 )
                {
                    dump( 0 );
                    child.dump( 0 );
                    throw new Error( I18n.err( I18n.ERR_516 ) );
                }
                
                child.assertConsistencyRecursive( height );
            }
        }
    }


    /**
     * Deserialize the content of an object from a byte array.
     *
     * @param serialized Byte array representation of the object
     * @return deserialized object
     *
     */
    @SuppressWarnings("unchecked") // Cannot create an array of generic objects
    public BPage<K, V> deserialize( byte[] serialized ) throws IOException
    {
        ByteArrayInputStream bais;
        ObjectInputStream ois;
        BPage<K, V> bpage;

        bpage = new BPage<K, V>();
        bais = new ByteArrayInputStream( serialized );
        ois = new ObjectInputStream( bais );

        bpage.isLeaf = ois.readBoolean();
        
        if ( bpage.isLeaf )
        {
            bpage.previous = ois.readLong();
            bpage.next = ois.readLong();
        }

        bpage.first = ois.readInt();

        bpage.keys = (K[])new Object[btree.pageSize];
        
        try
        {
            for ( int i = bpage.first; i < btree.pageSize; i++ )
            {
                if ( btree.keySerializer == null )
                {
                    bpage.keys[i] = (K)ois.readObject();
                }
                else
                {
                    serialized = readByteArray( ois );
                    
                    if ( serialized != null )
                    {
                        bpage.keys[i] = (K)btree.keySerializer.deserialize( serialized );
                    }
                }
            }
        }
        catch ( ClassNotFoundException except )
        {
            throw new IOException( except.getLocalizedMessage() );
        }

        if ( bpage.isLeaf )
        {
            bpage.values = (V[])new Object[btree.pageSize];
            
            try
            {
                for ( int i = bpage.first; i < btree.pageSize; i++ )
                {
                    if ( btree.valueSerializer == null )
                    {
                        bpage.values[i] =(V) ois.readObject();
                    }
                    else
                    {
                        serialized = readByteArray( ois );
                        
                        if ( serialized != null )
                        {
                            bpage.values[i] = (V)btree.valueSerializer.deserialize( serialized );
                        }
                    }
                }
            }
            catch ( ClassNotFoundException except )
            {
                throw new IOException( except.getLocalizedMessage() );
            }
        }
        else
        {
            bpage.children = new long[btree.pageSize];
            
            for ( int i = bpage.first; i < btree.pageSize; i++ )
            {
                bpage.children[i] = ois.readLong();
            }
        }
        
        ois.close();
        bais.close();

        return bpage;
    }


    /** 
     * Serialize the content of an object into a byte array.
     *
     * @param obj Object to serialize
     * @return a byte array representing the object's state
     *
     */
    @SuppressWarnings("unchecked") // The serialize signature requires an Object, so we have to cast
    public byte[] serialize( Object obj ) throws IOException
    {
        byte[] serialized;
        ByteArrayOutputStream baos;
        ObjectOutputStream oos;
        BPage<K, V> bpage;
        byte[] data;

        // note:  It is assumed that BPage instance doing the serialization is the parent
        // of the BPage object being serialized.
        bpage = ( BPage<K, V> ) obj;
        baos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream( baos );

        oos.writeBoolean( bpage.isLeaf );
        
        if ( bpage.isLeaf )
        {
            oos.writeLong( bpage.previous );
            oos.writeLong( bpage.next );
        }

        oos.writeInt( bpage.first );

        for ( int i = bpage.first; i < btree.pageSize; i++ )
        {
            if ( btree.keySerializer == null )
            {
                oos.writeObject( bpage.keys[i] );
            }
            else
            {
                if ( bpage.keys[i] != null )
                {
                    serialized = btree.keySerializer.serialize( bpage.keys[i] );
                    writeByteArray( oos, serialized );
                }
                else
                {
                    writeByteArray( oos, null );
                }
            }
        }

        if ( bpage.isLeaf )
        {
            for ( int i = bpage.first; i < btree.pageSize; i++ )
            {
                if ( btree.valueSerializer == null )
                {
                    oos.writeObject( bpage.values[i] );
                }
                else
                {
                    if ( bpage.values[i] != null )
                    {
                        serialized = btree.valueSerializer.serialize( bpage.values[i] );
                        writeByteArray( oos, serialized );
                    }
                    else
                    {
                        writeByteArray( oos, null );
                    }
                }
            }
        }
        else
        {
            for ( int i = bpage.first; i < btree.pageSize; i++ )
            {
                oos.writeLong( bpage.children[i] );
            }
        }

        oos.flush();
        data = baos.toByteArray();
        oos.close();
        baos.close();
        return data;
    }

    /** STATIC INNER CLASS
     *  Result from insert() method call. If the insertion has created
     *  a new page, it will be contained in the overflow field.
     *  If the inserted element already exist, then we will store
     *  the existing element.
     */
    static class InsertResult<K, V>
    {

        /**
         * Overflow page.
         */
        BPage<K, V> overflow;

        /**
         * Existing value for the insertion key.
         */
        V existing;
    }

    /** STATIC INNER CLASS
     *  Result from remove() method call. If we had to removed a BPage,
     *  it will be stored into the underflow field.
     */
    static class RemoveResult<V>
    {
        /**
         * Set to true if underlying pages underflowed
         */
        boolean underflow;

        /**
         * Removed entry value
         */
        V value;
    }

    /** PRIVATE INNER CLASS
     * Browser to traverse leaf BPages.
     */
    class Browser extends TupleBrowser<K, V>
    {
        /** Current page. */
        private BPage<K, V> page;

        /**
         * Current index in the page.  The index positionned on the next
         * tuple to return.
         */
        private int index;


        /**
         * Create a browser.
         *
         * @param page Current page
         * @param index Position of the next tuple to return.
         */
        Browser( BPage<K, V> page, int index )
        {
            this.page = page;
            this.index = index;
        }


        /**
         * Get the next Tuple in the current BTree. We have 3 cases to deal with :
         * 1) we are at the end of the btree. We will return false, the tuple won't be set.
         * 2) we are in the middle of a page : grab the values and store them into the tuple
         * 3) we are at the end of the page : grab the next page, and get the tuple from it.
         * 
         * @return true if we have found a tumple, false otherwise.
         */
        public boolean getNext( Tuple<K, V> tuple ) throws IOException
        {
            // First, check that we are within a page
            if ( index < page.btree.pageSize )
            {
                // We are. Now check that we have a Tuple
                if ( page.keys[index] == null )
                {
                    // no : reached end of the tree.
                    return false;
                }
            }
            // all the tuple for this page has been read. Move to the 
            // next page, if we have one.
            else if ( page.next != 0 )
            {
                // move to next page
                page = page.loadBPage( page.next );
                index = page.first;
            }
            
            tuple.setKey( page.keys[index] );
            tuple.setValue( page.values[index] );
            index++;
            
            return true;
        }


        public boolean getPrevious( Tuple<K, V> tuple ) throws IOException
        {
            if ( index == page.first )
            {
                if ( page.previous != 0 )
                {
                    page = page.loadBPage( page.previous );
                    index = page.btree.pageSize;
                }
                else
                {
                    // reached beginning of the tree
                    return false;
                }
            }
            
            index--;
            tuple.setKey( page.keys[index] );
            tuple.setValue( page.values[index] );
            
            return true;
        }
    }
}
