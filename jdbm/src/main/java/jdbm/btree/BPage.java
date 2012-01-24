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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import jdbm.helper.ActionContext;
import jdbm.helper.Serializer;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import org.apache.directory.server.i18n.I18n;


/**
 * Page of a Btree.
 * <p>
 * The page contains a number of key-value pairs. Keys are ordered to allow
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
 */
public class BPage<K, V> implements Serializer
{
    private static final boolean DEBUG = false;

    /** Version id for serialization. */
    final static long serialVersionUID = 1L;

    /** Parent B+Tree. */
    transient BTree<K, V> btree;

    /** This BPage's record ID in the PageManager. */
    protected transient long recordId;

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

    public static AtomicInteger outstandingBrowsers = new AtomicInteger( 0 );


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
    BPage( BTree<K, V> btree, BPage<K, V> root, BPage<K, V> overflow ) throws IOException
    {
        this.btree = btree;

        isLeaf = false;

        first = btree.pageSize - 2;

        keys = ( K[] ) new Object[btree.pageSize];
        keys[btree.pageSize - 2] = overflow.getLargestKey();
        keys[btree.pageSize - 1] = root.getLargestKey();

        children = new long[btree.pageSize];
        children[btree.pageSize - 2] = overflow.recordId;
        children[btree.pageSize - 1] = root.recordId;

        recordId = btree.recordManager.insert( this, this );
    }


    /**
     * Root page (first insert) constructor.
     */
    @SuppressWarnings("unchecked")
    // Cannot create an array of generic objects
    BPage( BTree<K, V> btree, K key, V value ) throws IOException
    {
        this.btree = btree;

        isLeaf = true;

        first = btree.pageSize - 2;

        keys = ( K[] ) new Object[btree.pageSize];
        keys[btree.pageSize - 2] = key;
        keys[btree.pageSize - 1] = null; // I am the root BPage for now

        values = ( V[] ) new Object[btree.pageSize];
        values[btree.pageSize - 2] = btree.copyValue( value );
        values[btree.pageSize - 1] = null; // I am the root BPage for now

        recordId = btree.recordManager.insert( this, this );
    }


    /**
     * Overflow page constructor.  Creates an empty BPage.
     */
    @SuppressWarnings("unchecked")
    // Cannot create an array of generic objects
    BPage( BTree btree, boolean isLeaf ) throws IOException
    {
        this.btree = btree;

        this.isLeaf = isLeaf;

        // page will initially be half-full
        first = btree.pageSize / 2;

        keys = ( K[] ) new Object[btree.pageSize];

        if ( isLeaf )
        {
            values = ( V[] ) new Object[btree.pageSize];
        }
        else
        {
            children = new long[btree.pageSize];
        }

        recordId = btree.recordManager.insert( this, this );
    }


    @SuppressWarnings("unchecked")
    // Cannot create an array of generic objects
    BPage<K, V> copyOnWrite()
    {
        BPage<K, V> newPage = new BPage<K, V>();

        newPage.btree = this.btree;
        newPage.isLeaf = this.isLeaf;

        newPage.first = this.first;
        newPage.previous = this.previous;
        newPage.next = this.next;

        newPage.keys = ( K[] ) new Object[btree.pageSize];
        newPage.values = ( V[] ) new Object[btree.pageSize];
        newPage.children = new long[btree.pageSize];

        newPage.recordId = this.recordId;

        if ( this.children != null )
        {
            this.copyChildren( this, 0, newPage, 0, btree.pageSize ); // this copies keys as well
        }

        if ( this.values != null )
        {
            this.copyEntries( this, 0, newPage, 0, btree.pageSize ); // this copies keys as well
        }

        return newPage;
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
     * @return The record ID
     */
    public long getRecordId()
    {
        return recordId;
    }


    /**
     * Set the recordId
     *
     * @param recordId The recordId
     */
    public void setRecordId( long recordId )
    {
        this.recordId = recordId;
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
     * @param context action context in case of action capable record manager
     * @return TupleBrowser positionned just before the given key, or before
     *                      next greater key if key isn't found.
     */
    TupleBrowser<K, V> find( int height, K key, ActionContext context ) throws IOException
    {
        int index = this.findChildren( key );

        if ( index < 0 )
        {
            index = -( index + 1 );
        }

        BPage<K, V> child = this;

        while ( !child.isLeaf )
        {
            // non-leaf BPage
            child = child.loadBPage( child.children[index] );
            index = child.findChildren( key );

            if ( index < 0 )
            {
                index = -( index + 1 );
            }
        }

        return new Browser( child, index, context );
    }


    /**
     * Find first entry and return a browser positioned before it.
     *@param context Action Context in case of 
     * @return TupleBrowser positionned just before the first entry.
     */
    TupleBrowser<K, V> findFirst( ActionContext context ) throws IOException
    {
        if ( isLeaf )
        {
            return new Browser( this, first, context );
        }
        else
        {
            BPage<K, V> child = childBPage( first );

            return child.findFirst( context );
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
        boolean keyExists = index < 0;

        if ( index < 0 )
        {
            index = -( index + 1 );
        }

        height -= 1;

        BPage<K, V> pageNewCopy = null;

        if ( height == 0 )
        {
            pageNewCopy = btree.copyOnWrite( this );
            result = new InsertResult<K, V>();
            result.pageNewCopy = pageNewCopy;

            // inserting on a leaf BPage
            overflow = -1;

            if ( DEBUG )
            {
                System.out.println( "Bpage.insert() Insert on leaf Bpage key=" + key + " value=" + value + " index="
                    + index );
            }

            // This is to deal with the special case where the key already exists.
            // In this case, the index will contain the key's position, but as a 
            // negative number
            if ( keyExists )
            {
                // key already exists
                if ( DEBUG )
                {
                    System.out.println( "Bpage.insert() Key already exists." );
                }

                result.existing = values[index];

                if ( replace )
                {
                    pageNewCopy.values[index] = btree.copyValue( value );
                    btree.recordManager.update( recordId, pageNewCopy, this );
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

            if ( result.pageNewCopy != null )
            {
                child = result.pageNewCopy;
                result.pageNewCopy = null;
            }

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
            pageNewCopy = btree.copyOnWrite( this );
            result.pageNewCopy = pageNewCopy;

            if ( DEBUG )
            {
                System.out.println( "BPage.insert() Overflow page: " + result.overflow.recordId );
            }

            key = result.overflow.getLargestKey();
            overflow = result.overflow.recordId;

            // update child's largest key
            pageNewCopy.keys[index] = child.getLargestKey();

            // clean result so we can reuse it
            result.overflow = null;
        }

        // if we get here, we need to insert a new entry on the BPage
        // before children[ index ]
        if ( !pageNewCopy.isFull() )
        {
            if ( height == 0 )
            {
                insertEntry( pageNewCopy, index - 1, key, value );
            }
            else
            {
                insertChild( pageNewCopy, index - 1, key, overflow );
            }

            btree.recordManager.update( recordId, pageNewCopy, this );

            return result;
        }

        // page is full, we must divide the page
        int half = btree.pageSize >> 1;
        BPage<K, V> newPage = new BPage<K, V>( btree, pageNewCopy.isLeaf );

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
                copyEntries( pageNewCopy, 0, newPage, half, index );
                setEntry( newPage, half + index, key, value );
                copyEntries( pageNewCopy, index, newPage, half + index + 1, half - index - 1 );
            }
            else
            {
                copyChildren( pageNewCopy, 0, newPage, half, index );
                setChild( newPage, half + index, key, overflow );
                copyChildren( pageNewCopy, index, newPage, half + index + 1, half - index - 1 );
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
                copyEntries( pageNewCopy, 0, newPage, half, half );
                copyEntries( pageNewCopy, half, pageNewCopy, half - 1, index - half );
                setEntry( pageNewCopy, index - 1, key, value );
            }
            else
            {
                copyChildren( pageNewCopy, 0, newPage, half, half );
                copyChildren( pageNewCopy, half, pageNewCopy, half - 1, index - half );
                setChild( pageNewCopy, index - 1, key, overflow );
            }
        }

        pageNewCopy.first = half - 1;

        // nullify lower half of entries
        for ( int i = 0; i < pageNewCopy.first; i++ )
        {
            if ( height == 0 )
            {
                setEntry( pageNewCopy, i, null, null );
            }
            else
            {
                setChild( pageNewCopy, i, null, -1 );
            }
        }

        if ( pageNewCopy.isLeaf )
        {
            // link newly created BPage
            newPage.previous = pageNewCopy.previous;
            newPage.next = pageNewCopy.recordId;

            if ( pageNewCopy.previous != 0 )
            {
                BPage<K, V> previousBPage = loadBPage( pageNewCopy.previous );
                previousBPage = btree.copyOnWrite( previousBPage );
                previousBPage.next = newPage.recordId;
                btree.recordManager.update( pageNewCopy.previous, previousBPage, this );
            }

            pageNewCopy.previous = newPage.recordId;
        }

        btree.recordManager.update( recordId, pageNewCopy, this );
        btree.recordManager.update( newPage.recordId, newPage, this );

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
    RemoveResult<K, V> remove( int height, K key ) throws IOException
    {
        RemoveResult<K, V> result;

        int half = btree.pageSize / 2;
        int index = findChildren( key );
        boolean keyExists = index < 0;

        if ( index < 0 )
        {
            index = -( index + 1 );
        }

        height -= 1;

        BPage<K, V> pageNewCopy = btree.copyOnWrite( this );

        if ( height == 0 )
        {
            // remove leaf entry
            if ( !keyExists )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_514, key ) );
            }

            result = new RemoveResult<K, V>();
            result.value = pageNewCopy.values[index];
            removeEntry( pageNewCopy, index );

            // update this BPage
            btree.recordManager.update( recordId, pageNewCopy, this );
        }
        else
        {
            // recurse into Btree to remove entry on a children page
            BPage<K, V> child = childBPage( index );
            result = child.remove( height, key );

            if ( result.pageNewCopy != null )
            {
                child = result.pageNewCopy;
                result.pageNewCopy = null;
            }
            else
            {
                child = btree.copyOnWrite( child );
            }

            // update children
            pageNewCopy.keys[index] = child.getLargestKey();
            btree.recordManager.update( recordId, pageNewCopy, this );

            if ( result.underflow )
            {
                // underflow occured
                if ( child.first != half + 1 )
                {
                    throw new IllegalStateException( I18n.err( I18n.ERR_513, "1" ) );
                }

                if ( index < pageNewCopy.children.length - 1 )
                {
                    // exists greater brother page
                    BPage<K, V> brother = pageNewCopy.childBPage( index + 1 );
                    brother = btree.copyOnWrite( brother );
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
                        pageNewCopy.keys[index] = child.getLargestKey();

                        // no change in previous/next BPage

                        // update BPages
                        btree.recordManager.update( recordId, pageNewCopy, this );
                        btree.recordManager.update( brother.recordId, brother, this );
                        btree.recordManager.update( child.recordId, child, this );

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

                        btree.recordManager.update( brother.recordId, brother, this );

                        // remove "child" from current BPage
                        if ( pageNewCopy.isLeaf )
                        {
                            copyEntries( pageNewCopy, pageNewCopy.first, pageNewCopy, pageNewCopy.first + 1, index
                                - pageNewCopy.first );
                            setEntry( pageNewCopy, pageNewCopy.first, null, null );
                        }
                        else
                        {
                            copyChildren( pageNewCopy, pageNewCopy.first, pageNewCopy, pageNewCopy.first + 1, index
                                - pageNewCopy.first );
                            setChild( pageNewCopy, pageNewCopy.first, null, -1 );
                        }

                        pageNewCopy.first += 1;
                        btree.recordManager.update( recordId, pageNewCopy, this );

                        // re-link previous and next BPages
                        if ( child.previous != 0 )
                        {
                            BPage<K, V> prev = loadBPage( child.previous );
                            prev = btree.copyOnWrite( prev );
                            prev.next = child.next;
                            btree.recordManager.update( prev.recordId, prev, this );
                        }

                        if ( child.next != 0 )
                        {
                            BPage<K, V> next = loadBPage( child.next );
                            next = btree.copyOnWrite( next );
                            next.previous = child.previous;
                            btree.recordManager.update( next.recordId, next, this );
                        }

                        // delete "child" BPage
                        btree.recordManager.delete( child.recordId );
                    }
                }
                else
                {
                    // page "brother" is before "child"
                    BPage<K, V> brother = pageNewCopy.childBPage( index - 1 );
                    brother = btree.copyOnWrite( brother );
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
                        pageNewCopy.keys[index - 1] = brother.getLargestKey();

                        // no change in previous/next BPage

                        // update BPages
                        btree.recordManager.update( recordId, pageNewCopy, this );
                        btree.recordManager.update( brother.recordId, brother, this );
                        btree.recordManager.update( child.recordId, child, this );

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

                        btree.recordManager.update( child.recordId, child, this );

                        // remove "brother" from current BPage
                        if ( pageNewCopy.isLeaf )
                        {
                            copyEntries( pageNewCopy, pageNewCopy.first, pageNewCopy, pageNewCopy.first + 1, index - 1
                                - pageNewCopy.first );
                            setEntry( pageNewCopy, pageNewCopy.first, null, null );
                        }
                        else
                        {
                            copyChildren( pageNewCopy, pageNewCopy.first, pageNewCopy, pageNewCopy.first + 1, index - 1
                                - pageNewCopy.first );
                            setChild( pageNewCopy, pageNewCopy.first, null, -1 );
                        }

                        pageNewCopy.first += 1;
                        btree.recordManager.update( recordId, pageNewCopy, this );

                        // re-link previous and next BPages
                        if ( brother.previous != 0 )
                        {
                            BPage<K, V> prev = loadBPage( brother.previous );
                            prev = btree.copyOnWrite( prev );
                            prev.next = brother.next;
                            btree.recordManager.update( prev.recordId, prev, this );
                        }

                        if ( brother.next != 0 )
                        {
                            BPage<K, V> next = loadBPage( brother.next );
                            next = btree.copyOnWrite( next );
                            next.previous = brother.previous;
                            btree.recordManager.update( next.recordId, next, this );
                        }

                        // delete "brother" BPage
                        btree.recordManager.delete( brother.recordId );
                    }
                }
            }
        }

        // underflow if page is more than half-empty
        result.underflow = pageNewCopy.first > half;
        result.pageNewCopy = pageNewCopy;

        return result;
    }


    /**
     * Find the first children node with a key equal or greater than the given
     * key.
     *
     * @return index of first children with equal or greater key. If the 
     * key already exists, the index value will be negative
     */
    private int findChildren( K key )
    {
        int left = first;
        int right = btree.pageSize - 1;

        // binary search
        while ( left < right )
        {
            int middle = ( left + right ) >>> 1;

            int comp = compare( keys[middle], key );

            if ( comp < 0 )
            {
                left = middle + 1;
            }
            else if ( comp > 0 )
            {
                right = middle;
            }
            else
            {
                // Special case : the key already exists,
                // we can return immediately
                return -middle - 1;
            }
        }

        if ( left == right )
        {
            // Special case : we don't know if the key is present
            if ( compare( keys[left], key ) == 0 )
            {
                return -right - 1;
            }
        }

        return right;
    }


    /**
     * Insert entry at given position.
     */
    private void insertEntry( BPage<K, V> page, int index, K key, V value ) throws IOException
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
        values[index] = btree.copyValue( value );
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
    private void setEntry( BPage<K, V> page, int index, K key, V value ) throws IOException
    {
        page.keys[index] = key;
        page.values[index] = btree.copyValue( value );
    }


    /**
     * Set the child BPage recordId at the given index.
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
    // False positive
    @SuppressWarnings("PMD.UnnecessaryFinalModifier")
    BPage<K, V> childBPage( int index ) throws IOException
    {
        return loadBPage( children[index] );
    }


    /**
     * Load the BPage at the given recordId.
     */
    @SuppressWarnings("unchecked")
    // The fetch method returns an Object
    private BPage<K, V> loadBPage( long recid ) throws IOException
    {
        BPage<K, V> child = ( BPage<K, V> ) btree.recordManager.fetch( recid, this );
        child.recordId = recid;
        child.btree = btree;

        return child;
    }


    private final int compare( K value1, K value2 )
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

        return btree.getComparator().compare( value1, value2 );
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
        StringBuffer prefix = new StringBuffer();

        for ( int i = 0; i < height; i++ )
        {
            prefix.append( "    " );
        }

        System.out.println( prefix + "-------------------------------------- BPage recordId=" + recordId );
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
     * Assert the ordering of the keys on the BPage. This is used for testing
     * purposes only.
     */
    private void assertConsistency()
    {
        for ( int i = first; i < btree.pageSize - 1; i++ )
        {
            if ( compare( keys[i], keys[i + 1] ) >= 0 )
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

                if ( compare( keys[i], child.getLargestKey() ) != 0 )
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
    @SuppressWarnings("unchecked")
    // Cannot create an array of generic objects
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

        bpage.keys = ( K[] ) new Object[btree.pageSize];

        try
        {
            for ( int i = bpage.first; i < btree.pageSize; i++ )
            {
                if ( btree.keySerializer == null )
                {
                    bpage.keys[i] = ( K ) ois.readObject();
                }
                else
                {
                    serialized = readByteArray( ois );

                    if ( serialized != null )
                    {
                        bpage.keys[i] = ( K ) btree.keySerializer.deserialize( serialized );
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
            bpage.values = ( V[] ) new Object[btree.pageSize];

            try
            {
                for ( int i = bpage.first; i < btree.pageSize; i++ )
                {
                    if ( btree.valueSerializer == null )
                    {
                        bpage.values[i] = ( V ) ois.readObject();
                    }
                    else
                    {
                        serialized = readByteArray( ois );

                        if ( serialized != null )
                        {
                            bpage.values[i] = ( V ) btree.valueSerializer.deserialize( serialized );
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
    @SuppressWarnings("unchecked")
    // The serialize signature requires an Object, so we have to cast
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

        /**
         * New version of the page doing the insert
         */
        BPage<K, V> pageNewCopy;
    }

    /** STATIC INNER CLASS
     *  Result from remove() method call. If we had to removed a BPage,
     *  it will be stored into the underflow field.
     */
    static class RemoveResult<K, V>
    {
        /**
         * Set to true if underlying pages underflowed
         */
        boolean underflow;

        /**
         * Removed entry value
         */
        V value;

        /**
         * New version of the page doing the remove
         */
        BPage<K, V> pageNewCopy;

    }

    /** PRIVATE INNER CLASS
     * Browser to traverse leaf BPages.
     */
    class Browser extends TupleBrowser<K, V>
    {
        /** Current page. */
        private BPage<K, V> page;

        /** context used to track browsing action */
        ActionContext context;

        /**
         * Current index in the page.  The index positionned on the next
         * tuple to return.
         */
        private int index;


        /**
         * Create a browser.
         *
         * @param page Current page
         * @param context context in case of action capable record manager 
         * @param index Position of the next tuple to return.
         */
        Browser( BPage<K, V> page, int index, ActionContext context )
        {
            this.page = page;
            this.index = index;
            this.context = context;

            outstandingBrowsers.incrementAndGet();
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
            btree.setAsCurrentAction( context );
            try
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
                tuple.setValue( btree.copyValue( page.values[index] ) );
                index++;
            }
            catch ( IOException e )
            {
                btree.abortAction( context );
                context = null;
                this.close();
                throw e;
            }
            finally
            {
                if ( context != null )
                {
                    btree.unsetAsCurrentAction( context );
                }
            }

            return true;
        }


        public boolean getPrevious( Tuple<K, V> tuple ) throws IOException
        {
            btree.setAsCurrentAction( context );

            try
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
                tuple.setValue( btree.copyValue( page.values[index] ) );
            }
            catch ( IOException e )
            {
                btree.abortAction( context );
                context = null;
                this.close();
                throw e;
            }
            finally
            {
                if ( context != null )
                {
                    btree.unsetAsCurrentAction( context );
                }
            }

            return true;
        }


        @Override
        public void close()
        {
            super.close();

            if ( context != null )
            {
                btree.setAsCurrentAction( context );
                btree.endAction( context );
                context = null;
            }

            int browserCount = outstandingBrowsers.decrementAndGet();

            if ( browserCount > 0 )
            {
                //System.out.println( "JDBM btree browsers are outstanding after close: " + browserCount );
            }
        }

    }


    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if ( isLeaf )
        {
            sb.append( "Leaf(" );
        }
        else
        {
            sb.append( "Node(" );
        }

        sb.append( keys.length );
        sb.append( ") : [" );

        if ( isLeaf )
        {
            boolean isFirst = true;
            int index = 0;

            for ( K key : keys )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( "<" );
                sb.append( String.valueOf( key ) );
                sb.append( "/" );
                sb.append( values[index] );
                sb.append( ">" );

                index++;
            }
        }
        else
        {
            boolean isFirst = true;

            for ( K key : keys )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( "<" );
                sb.append( key );
                sb.append( ">" );
            }
        }

        sb.append( "]\n" );
        return sb.toString();
    }
}
