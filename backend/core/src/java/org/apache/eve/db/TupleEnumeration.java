package org.apache.eve.db;


import java.util.Iterator;

import javax.naming.NamingEnumeration;


/**
 * A NamingEnumeration that returns underlying Iterator values for a single key
 * as Tuples.
 * 
 * <p>
 * WARNING: The tuple returned is reused every time for efficiency and populated
 * a over and over again with the new value.  The key never changes.
 * </p>
 * 
 */
public class TupleEnumeration
    implements NamingEnumeration
{
    /** TODO */
    private final Object key;
    /** TODO */
    private final Iterator iterator;
    /** TODO */
    private final Tuple tuple = new Tuple();


    /**
     * Creates a cursor over an Iterator of single key's values
     * 
     * @param key the keys whose duplicate values are to be returned
     * @param iterator the underlying iterator this cursor uses
     */
    public TupleEnumeration( Object key, Iterator iterator )
    {
        this.key = key;
        tuple.setKey( key );
        this.iterator = iterator;
    }

    
    /**
     * Gets the next value as a Tuple.
     *
     * @see javax.naming.NamingEnumeration#next()
     */
    public Object next()
    {
        tuple.setKey( key );
        tuple.setValue( iterator.next() );
        return tuple;
    }


    /**
     * Gets the next value as a Tuple.
     *
     * @see javax.naming.NamingEnumeration#nextElement()
     */
    public Object nextElement()
    {
        tuple.setKey( key );
        tuple.setValue( iterator.next() );
        return tuple;
    }


    /**
     * Checks if another value is available.
     *
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore()
    {
        return iterator.hasNext();
    }


    /**
     * Checks if another value is available.
     *
     * @see javax.naming.NamingEnumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return iterator.hasNext();
    }


    /**
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close() 
    { 
    }
}
