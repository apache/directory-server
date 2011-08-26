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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import jdbm.RecordManager;
import jdbm.helper.Serializer;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import org.apache.directory.server.i18n.I18n;


/**
 * B+Tree persistent indexing data structure.  B+Trees are optimized for
 * block-based, random I/O storage because they store multiple keys on
 * one tree node (called <code>BPage</code>).  In addition, the leaf nodes
 * directly contain (inline) the values associated with the keys, allowing a
 * single (or sequential) disk read of all the values on the page.
 * <p>
 * B+Trees are n-airy, yeilding log(N) search cost.  They are self-balancing,
 * preventing search performance degradation when the size of the tree grows.
 * <p>
 * Keys and associated values must be <code>Serializable</code> objects. The
 * user is responsible to supply a serializable <code>Comparator</code> object
 * to be used for the ordering of entries, which are also called <code>Tuple</code>.
 * The B+Tree allows traversing the keys in forward and reverse order using a
 * TupleBrowser obtained from the browse() methods.
 * <p>
 * This implementation does not directly support duplicate keys, but it is
 * possible to handle duplicates by inlining or referencing an object collection
 * as a value.
 * <p>
 * There is no limit on key size or value size, but it is recommended to keep
 * both as small as possible to reduce disk I/O.   This is especially true for
 * the key size, which impacts all non-leaf <code>BPage</code> objects.
 *
 * @author <a href="mailto:boisvert@intalio.com">Alex Boisvert</a>
 */
public class BTree<K, V> implements Externalizable
{
    private static final boolean DEBUG = false;

    /** Version id for serialization. */
    final static long serialVersionUID = 1L;

    /** Default page size (number of entries per node) */
    public static final int DEFAULT_SIZE = 16;

    /** Page manager used to persist changes in BPages */
    protected transient RecordManager recordManager;

    /** This BTree's record ID in the PageManager. */
    private transient long recordId;

    /** Comparator used to index entries. */
    private Comparator<K> comparator;

    /** Serializer used to serialize index keys (optional) */
    protected Serializer keySerializer;

    /** Serializer used to serialize index values (optional) */
    protected Serializer valueSerializer;

    /**
     * Height of the B+Tree.  This is the number of BPages you have to traverse
     * to get to a leaf BPage, starting from the root.
     */
    private int bTreeHeight;

    /** Record id of the root BPage */
    private long rootId;

    /** Number of entries in each BPage. */
    protected int pageSize;

    /** Total number of entries in the BTree */
    protected AtomicInteger nbEntries;

    /** Serializer used for BPages of this tree */
    private transient BPage<K, V> bpageSerializer;


    /**
     * No-argument constructor used by serialization.
     */
    public BTree()
    {
        // empty
    }


    /**
     * Create a new persistent BTree, with 16 entries per node.
     *
     * @param recman Record manager used for persistence.
     * @param comparator Comparator used to order index entries
     */
    public BTree( RecordManager recman, Comparator<K> comparator ) throws IOException
    {
        createInstance( recman, comparator, null, null, DEFAULT_SIZE );
    }


    /**
     * Create a new persistent BTree, with 16 entries per node.
     *
     * @param recman Record manager used for persistence.
     * @param keySerializer Serializer used to serialize index keys (optional)
     * @param valueSerializer Serializer used to serialize index values (optional)
     * @param comparator Comparator used to order index entries
     */
    public BTree( RecordManager recman, Comparator<K> comparator, Serializer keySerializer,
        Serializer valueSerializer ) throws IOException
    {
        createInstance( recman, comparator, keySerializer, valueSerializer, DEFAULT_SIZE );
    }


    /**
     * Create a new persistent BTree with the given number of entries per node.
     *
     * @param recman Record manager used for persistence.
     * @param comparator Comparator used to order index entries
     * @param keySerializer Serializer used to serialize index keys (optional)
     * @param valueSerializer Serializer used to serialize index values (optional)
     * @param pageSize Number of entries per page (must be even).
     */
    public BTree( RecordManager recman, Comparator<K> comparator, Serializer keySerializer,
        Serializer valueSerializer, int pageSize ) throws IOException
    {
        createInstance( recman, comparator, keySerializer, valueSerializer, pageSize );
    }
    
    
    /**
     * The real BTree constructor.
     */
    private void createInstance(RecordManager recordManager, Comparator<K> comparator, Serializer keySerializer,
        Serializer valueSerializer, int pageSize) throws IOException
    {
        if ( recordManager == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_517 ) );
        }

        if ( comparator == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_518 ) );
        }

        if ( !( comparator instanceof Serializable ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_519 ) );
        }

        // make sure there's an even number of entries per BPage
        if ( ( pageSize & 1 ) != 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_522 ) );
        }

        this.recordManager = recordManager;
        this.comparator = comparator;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.pageSize = pageSize;
        this.bpageSerializer = new BPage<K, V>();
        this.bpageSerializer.btree = this;
        this.nbEntries = new AtomicInteger( 0 );

        this.recordId = recordManager.insert( this );
    }
    
    
    public void setPageSize( int pageSize )
    {
        if ( ( pageSize & 0x0001 ) != 0 )
        {
            this.pageSize = DEFAULT_SIZE;
        }
        else
        {
            this.pageSize = pageSize;
        }
    }


    /**
     * Load a persistent BTree.
     *
     * @param recman RecordManager used to store the persistent btree
     * @param recid Record id of the BTree
     */
    public BTree<K, V> load( RecordManager recman, long recid ) throws IOException
    {
        BTree<K, V> btree = (BTree<K, V>) recman.fetch( recid );
        btree.recordId = recid;
        btree.recordManager = recman;
        btree.bpageSerializer = new BPage<K, V>();
        btree.bpageSerializer.btree = btree;
        
        return btree;
    }


    /**
     * Insert an entry in the BTree.
     * <p>
     * The BTree cannot store duplicate entries.  An existing entry can be
     * replaced using the <code>replace</code> flag.   If an entry with the
     * same key already exists in the BTree, its value is returned.
     *
     * @param key Insert key
     * @param value Insert value
     * @param replace Set to true to replace an existing key-value pair.
     * @return Existing value, if any.
     */
    public synchronized Object insert( K key, V value, boolean replace ) throws IOException
    {
        if ( key == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_523 ) );
        }
        
        if ( value == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_524 ) );
        }

        BPage<K, V> rootPage = getRoot();

        if ( rootPage == null )
        {
            // BTree is currently empty, create a new root BPage
            if ( DEBUG )
            {
                System.out.println( "BTree.insert() new root BPage" );
            }
            
            rootPage = new BPage<K, V>( this, key, value );
            rootId = rootPage.getRecordId();
            bTreeHeight = 1;
            nbEntries.set( 1 );
            recordManager.update( recordId, this );
            
            return null;
        }
        else
        {
            BPage.InsertResult<K, V> insert = rootPage.insert( bTreeHeight, key, value, replace );
            boolean dirty = false;
            
            if ( insert.overflow != null )
            {
                // current root page overflowed, we replace with a new root page
                if ( DEBUG )
                {
                    System.out.println( "BTree.insert() replace root BPage due to overflow" );
                }
                
                rootPage = new BPage<K, V>( this, rootPage, insert.overflow );
                rootId = rootPage.getRecordId();
                bTreeHeight += 1;
                dirty = true;
            }
            
            if ( insert.existing == null )
            {
                nbEntries.getAndIncrement();
                dirty = true;
            }
            
            if ( dirty )
            {
                recordManager.update( recordId, this );
            }
            
            // insert might have returned an existing value
            return insert.existing;
        }
    }


    /**
     * Remove an entry with the given key from the BTree.
     *
     * @param key Removal key
     * @return Value associated with the key, or null if no entry with given
     *         key existed in the BTree.
     */
    public synchronized V remove( K key ) throws IOException
    {
        if ( key == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_523 ) );
        }

        BPage<K, V> rootPage = getRoot();
        
        if ( rootPage == null )
        {
            return null;
        }
        
        boolean dirty = false;
        BPage.RemoveResult<V> remove = rootPage.remove( bTreeHeight, key );
        
        if ( remove.underflow && rootPage.isEmpty() )
        {
            bTreeHeight -= 1;
            dirty = true;

            recordManager.delete( rootId );
            
            if ( bTreeHeight == 0 )
            {
                rootId = 0;
            }
            else
            {
                rootId = rootPage.childBPage( pageSize - 1 ).getRecordId();
            }
        }
        
        if ( remove.value != null )
        {
            nbEntries.getAndDecrement();
            dirty = true;
        }
        
        if ( dirty )
        {
            recordManager.update( recordId, this );
        }
        
        return remove.value;
    }


    /**
     * Find the value associated with the given key.
     *
     * @param key Lookup key.
     * @return Value associated with the key, or null if not found.
     */
    public synchronized V find( K key ) throws IOException
    {
        if ( key == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_523 ) );
        }
        
        BPage<K, V> rootPage = getRoot();
        
        if ( rootPage == null )
        {
            return null;
        }

        Tuple<K, V> tuple = new Tuple<K, V>( null, null );
        TupleBrowser<K, V> browser = rootPage.find( bTreeHeight, key );

        if ( browser.getNext( tuple ) )
        {
            // find returns the matching key or the next ordered key, so we must
            // check if we have an exact match
            if ( comparator.compare( key, tuple.getKey() ) != 0 )
            {
                return null;
            }
            else
            {
                return tuple.getValue();
            }
        }
        else
        {
            return null;
        }
    }


    /**
     * Find the value associated with the given key, or the entry immediately
     * following this key in the ordered BTree.
     *
     * @param key Lookup key.
     * @return Value associated with the key, or a greater entry, or null if no
     *         greater entry was found.
     */
    public synchronized Tuple<K, V> findGreaterOrEqual( K key ) throws IOException
    {
        Tuple<K, V> tuple;
        TupleBrowser<K, V> browser;

        if ( key == null )
        {
            // there can't be a key greater than or equal to "null"
            // because null is considered an infinite key.
            return null;
        }

        tuple = new Tuple<K, V>( null, null );
        browser = browse( key );
        
        if ( browser.getNext( tuple ) )
        {
            return tuple;
        }
        else
        {
            return null;
        }
    }


    /**
     * Get a browser initially positioned at the beginning of the BTree.
     * <p><b>
     * WARNING: If you make structural modifications to the BTree during
     * browsing, you will get inconsistent browing results.
     * </b>
     *
     * @return Browser positionned at the beginning of the BTree.
     */
    public synchronized TupleBrowser<K, V> browse() throws IOException
    {
        BPage<K, V> rootPage = getRoot();
        
        if ( rootPage == null )
        {
            return new EmptyBrowser(){};
        }
        
        TupleBrowser<K, V> browser = rootPage.findFirst();
        
        return browser;
    }


    /**
     * Get a browser initially positioned just before the given key.
     * <p><b>
     * WARNING: If you make structural modifications to the BTree during
     * browsing, you will get inconsistent browsing results.
     * </b>
     *
     * @param key Key used to position the browser.  If null, the browser
     *            will be positioned after the last entry of the BTree.
     *            (Null is considered to be an "infinite" key)
     * @return Browser positioned just before the given key.
     */
    public synchronized TupleBrowser<K, V> browse( K key ) throws IOException
    {
        BPage<K, V> rootPage = getRoot();
        
        if ( rootPage == null )
        {
            return new EmptyBrowser(){};
        }
        
        TupleBrowser<K, V> browser = rootPage.find( bTreeHeight, key );
        
        return browser;
    }


    /**
     * Return the number of entries (size) of the BTree.
     */
    public int size()
    {
        return nbEntries.get();
    }


    /**
     * Return the persistent record identifier of the BTree.
     */
    public long getRecordId()
    {
        return recordId;
    }


    /**
     * Return the root BPage<Object, Object>, or null if it doesn't exist.
     */
    private BPage<K, V> getRoot() throws IOException
    {
        if ( rootId == 0 )
        {
            return null;
        }
        
        BPage<K, V> root = ( BPage<K, V> ) recordManager.fetch( rootId, bpageSerializer );
        root.setRecordId( rootId );
        root.btree = this;
        
        return root;
    }


    /**
     * Implement Externalizable interface.
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        comparator = ( Comparator<K> ) in.readObject();
        keySerializer = ( Serializer ) in.readObject();
        valueSerializer = ( Serializer ) in.readObject();
        bTreeHeight = in.readInt();
        rootId = in.readLong();
        pageSize = in.readInt();
        nbEntries = new AtomicInteger( in.readInt() );
    }


    /**
     * Implement Externalizable interface.
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeObject( comparator );
        out.writeObject( keySerializer );
        out.writeObject( valueSerializer );
        out.writeInt( bTreeHeight );
        out.writeLong( rootId );
        out.writeInt( pageSize );
        out.writeInt( nbEntries.get() );
    }


    public void setValueSerializer( Serializer valueSerializer )
    {
        this.valueSerializer = valueSerializer;
    }

    
    /** PRIVATE INNER CLASS
     *  Browser returning no element.
     */
    class EmptyBrowser extends TupleBrowser<K, V>
    {
        public boolean getNext( Tuple<K, V> tuple )
        {
            return false;
        }

        public boolean getPrevious( Tuple<K, V> tuple )
        {
            return false;
        }
    }


    /**
     * @return the comparator
     */
    public Comparator<K> getComparator()
    {
        return comparator;
    }
    
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "BTree" );
        sb.append( "(height:" ).append(bTreeHeight );
        sb.append( ", pageSize:" ).append( pageSize );
        sb.append( ", nbEntries:" ).append( nbEntries );
        sb.append( ", rootId:" ).append( rootId );
        sb.append( ", comparator:" );
        
        if ( comparator == null )
        {
            sb.append( "null" );
        }
        else
        {
            sb.append( comparator.getClass().getSimpleName() );
        }

        sb.append( ", keySerializer:" );
        
        if ( keySerializer == null )
        {
            sb.append( "null" );
        }
        else
        {
            sb.append( keySerializer.getClass().getSimpleName() );
        }

        sb.append( ", valueSerializer:" );

        if ( valueSerializer == null )
        {
            sb.append( "null" );
        }
        else
        {
            sb.append( valueSerializer.getClass().getSimpleName() );
        }
        
        sb.append( ")\n" );

        return sb.toString();
    }
}
