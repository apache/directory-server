package org.apache.eve.db;


import java.util.NoSuchElementException;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.regexp.RE;


/**
 * A NamingEnumeration over an Index which returns IndexRecords.
 * 
 */
public class IndexEnumeration
    implements NamingEnumeration
{
    /** */
    private final RE re;
    /** */
    private final IndexRecord tmp = new IndexRecord();
    /** */
    private final IndexRecord returned = new IndexRecord();
    /** */
    private final IndexRecord prefetched = new IndexRecord();
    /** */
    private final boolean swapKeyVal;
    /** */
    private final NamingEnumeration underlying;

    /** */
    private boolean hasMore = true;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * TODO Domument me!
     *
     * @param list TODO
     * @throws NamingException TODO
     */
    IndexEnumeration( NamingEnumeration list ) throws NamingException
    {
        this( list, false, null );
    }


    /**
     * TODO Domument me!
     *
     * @param list TODO
     * @param swapKeyVal TODO
     * @throws NamingException TODO
     */
    IndexEnumeration( NamingEnumeration list, boolean swapKeyVal )
        throws NamingException
    {
        this( list, swapKeyVal, null );
    }


    /**
     * TODO Domument me!
     *
     * @param list TODO
     * @param swapKeyVal TODO
     * @param regex TODO
     * @throws NamingException TODO
     */
    IndexEnumeration( NamingEnumeration list, boolean swapKeyVal, 
        RE regex ) throws NamingException
    {
        re = regex;
        underlying = list;
        this.swapKeyVal = swapKeyVal;

        if ( ! underlying.hasMore() ) 
        {
            hasMore = false;
            return;
        }

        prefetch();
    }


    // ------------------------------------------------------------------------
    // NamingEnumeration Interface Methods 
    // ------------------------------------------------------------------------


    /**
     * @see javax.naming.NamingEnumeration#next()
     */
    public Object next()
        throws NamingException
    {
        returned.copy( prefetched );
        prefetch();
        return returned;
    }
    
    
    /**
     * @see java.util.Enumeration#nextElement()
     */
    public Object nextElement()
    {
        try
        {
            return next();
        }
        catch ( NamingException ne )
        {
            throw new NoSuchElementException();
        }
    }


    /**
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore()
    {
        return hasMore;
    }


    /**
     * @see javax.naming.NamingEnumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return hasMore;
    }


    /**
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close() throws NamingException
    {
        hasMore = false;
        underlying.close();
    }


    // ------------------------------------------------------------------------
    // Private Methods 
    // ------------------------------------------------------------------------


    /**
     * TODO Document me!
     *
     * @throws NamingException TODO
     */
    private void prefetch() throws NamingException
    {
        while ( underlying.hasMore() ) 
        {
            Tuple tuple = ( Tuple ) underlying.next();

            if ( swapKeyVal ) 
            {
                tmp.setSwapped( tuple, null );
            } 
            else 
            {
                tmp.setTuple( tuple, null );
            }

            // If regex is null just transfer into prefetched from tmp record
            // but if it is not then use it to match.  Successful match shorts
            // while loop.
            if ( null == re || re.match( ( String ) tmp.getIndexKey() ) ) 
            {
                prefetched.copy( tmp );
                return;
            }
        }

        // If we got down here then cursor has been consumed without a match!
        hasMore = false;
    }
}
