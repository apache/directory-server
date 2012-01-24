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
 * Copyright 2000 (C) Cees de Groot. All Rights Reserved.
 * Contributions are Copyright (C) 2000 by their associated contributors.
 *
 * $Id: MRU.java,v 1.8 2005/06/25 23:12:31 doomdark Exp $
 */

package jdbm.helper;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.i18n.I18n;


/**
 *  MRU - Most Recently Used cache policy.
 *
 *  Methods are *not* synchronized, so no concurrent access is allowed.
 *
 * @author <a href="mailto:boisvert@intalio.com">Alex Boisvert</a>
 */
public class MRU<K, V> implements CachePolicy<K, V>
{
    /** Cached object Map */
    Map<Object, CacheEntry> map = new HashMap<Object, CacheEntry>();

    /**
     * Maximum number of objects in the cache.
     */
    int max;

    /**
     * Beginning of linked-list of cache elements.  First entry is element
     * which has been used least recently.
     */
    CacheEntry first;

    /**
     * End of linked-list of cache elements.  Last entry is element
     * which has been used most recently.
     */
    CacheEntry last;

    /**
     * Cache eviction listeners
     */
    List<CachePolicyListener> listeners = new ArrayList<CachePolicyListener>();


    /**
     * Construct an MRU with a given maximum number of objects.
     */
    public MRU( int max )
    {
        if ( max <= 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_528 ) );
        }

        this.max = max;
    }


    /**
     * Place an object in the cache.
     */
    public void put( K key, V value ) throws CacheEvictionException
    {
        CacheEntry entry = map.get( key );

        if ( entry != null )
        {
            entry.setValue( value );
            touchEntry( entry );
        }
        else
        {

            if ( map.size() == max )
            {
                // purge and recycle entry
                entry = purgeEntry();
                entry.setKey( key );
                entry.setValue( value );
            }
            else
            {
                entry = new CacheEntry( key, value );
            }

            addEntry( entry );
            map.put( entry.getKey(), entry );
        }
    }


    /**
     * Obtain an object in the cache
     */
    public V get( K key )
    {
        CacheEntry entry = map.get( key );

        if ( entry != null )
        {
            touchEntry( entry );
            return ( V ) entry.getValue();
        }
        else
        {
            return null;
        }
    }


    /**
     * Remove an object from the cache
     */
    public void remove( K key )
    {
        CacheEntry entry = map.get( key );

        if ( entry != null )
        {
            removeEntry( entry );
            map.remove( entry.getKey() );
        }
    }


    /**
     * Remove all objects from the cache
     */
    public void removeAll()
    {
        map = new HashMap<Object, CacheEntry>();
        first = null;
        last = null;
    }


    /**
     * Enumerate elements' values in the cache
     */
    public Enumeration<V> elements()
    {
        return new MRUEnumeration<V>( map.values().iterator() );
    }


    /**
     * Add a listener to this cache policy
     *
     * @param listener Listener to add to this policy
     */
    public void addListener( CachePolicyListener listener )
    {
        if ( listener == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_539_BAD_BLOCK_ID ) );
        }

        if ( !listeners.contains( listener ) )
        {
            listeners.add( listener );
        }
    }


    /**
     * Remove a listener from this cache policy
     *
     * @param listener Listener to remove from this policy
     */
    public void removeListener( CachePolicyListener listener )
    {
        listeners.remove( listener );
    }


    /**
     * Add a CacheEntry.  Entry goes at the end of the list.
     */
    protected void addEntry( CacheEntry entry )
    {
        if ( first == null )
        {
            first = entry;
            last = entry;
        }
        else
        {
            last.setNext( entry );
            entry.setPrevious( last );
            last = entry;
        }
    }


    /**
     * Remove a CacheEntry from linked list, and relink the 
     * remaining element sin the list.
     */
    protected void removeEntry( CacheEntry entry )
    {
        if ( entry == first )
        {
            first = entry.getNext();

            if ( first != null )
            {
                first.setPrevious( null );
            }
        }
        else if ( last == entry )
        {
            last = entry.getPrevious();

            if ( last != null )
            {
                last.setNext( null );
            }
        }
        else
        {
            entry.getPrevious().setNext( entry.getNext() );
            entry.getNext().setPrevious( entry.getPrevious() );
        }
    }


    /**
     * Place entry at the end of linked list -- Most Recently Used
     */
    protected void touchEntry( CacheEntry entry )
    {
        if ( last == entry )
        {
            return;
        }

        removeEntry( entry );
        addEntry( entry );
    }


    /**
     * Purge least recently used object from the cache
     *
     * @return recyclable CacheEntry
     */
    protected CacheEntry purgeEntry() throws CacheEvictionException
    {
        CacheEntry entry = first;

        // Notify policy listeners first. if any of them throw an
        // eviction exception, then the internal data structure
        // remains untouched.
        CachePolicyListener listener;

        for ( int i = 0; i < listeners.size(); i++ )
        {
            listener = listeners.get( i );
            listener.cacheObjectEvicted( entry.getValue() );
        }

        removeEntry( entry );
        map.remove( entry.getKey() );
        entry.setValue( null );

        return entry;
    }

}

/**
 * State information for cache entries.
 */
class CacheEntry
{
    private Object key;
    private Object value;

    private CacheEntry previous;
    private CacheEntry next;


    CacheEntry( Object key, Object value )
    {
        this.key = key;
        this.value = value;
    }


    Object getKey()
    {
        return key;
    }


    void setKey( Object obj )
    {
        key = obj;
    }


    Object getValue()
    {
        return value;
    }


    void setValue( Object obj )
    {
        value = obj;
    }


    CacheEntry getPrevious()
    {
        return previous;
    }


    void setPrevious( CacheEntry entry )
    {
        previous = entry;
    }


    CacheEntry getNext()
    {
        return next;
    }


    void setNext( CacheEntry entry )
    {
        next = entry;
    }
}

/**
 * Enumeration wrapper to return actual user objects instead of
 * CacheEntries.
 */
class MRUEnumeration<V> implements Enumeration<V>
{
    Iterator<CacheEntry> elements;


    MRUEnumeration( Iterator<CacheEntry> elements )
    {
        this.elements = elements;
    }


    public boolean hasMoreElements()
    {
        return elements.hasNext();
    }


    public V nextElement()
    {
        CacheEntry entry = elements.next();

        return ( V ) entry.getValue();
    }
}
