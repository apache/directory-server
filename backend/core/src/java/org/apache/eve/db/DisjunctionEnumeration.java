package org.apache.eve.db;


import java.util.Map ;
import java.util.HashMap ;
import java.util.NoSuchElementException ;

import javax.naming.NamingException ;
import javax.naming.NamingEnumeration ;


/**
 * A Cursor of Cursors performing a union on all underlying Cursors resulting
 * in the disjunction of expressions represented by the constituant child
 * Cursors. This cursor prefetches underlying Cursor values so that it can
 * comply with the defined Cursor semantics.
 *
 */
public class DisjunctionEnumeration implements NamingEnumeration
{
    /** The underlying child enumerations */
    private final NamingEnumeration [] m_children ;
    /** LUT used to avoid returning duplicates */
    private final Map m_candidates = new HashMap() ;
    /** Index of current cursor used */
    private int m_index = 0 ;
    /** Candidate to return */
    private final IndexRecord m_candidate = new IndexRecord() ;
    /** Prefetched record returned */
    private final IndexRecord m_prefetched = new IndexRecord() ;
    /** Used to determine if this enumeration has been exhausted */
    private boolean m_hasMore = true ;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    

    /**
     * Creates a DisjunctionEnumeration over a set of child NamingEnumerations.
     * The returned result is the union of all underlying NamingEnumerations 
     * without duplicates.
     *
     * @param a_children array of child NamingInstances
     * @throws NamingException if something goes wrong
     */
    public DisjunctionEnumeration( NamingEnumeration [] a_children )
        throws NamingException
    {
        m_children = a_children ;

        // Close this cursor if their are no children.
        if ( a_children.length <= 0 ) 
        {
            m_hasMore = false ;
            return ;
        }

        // Advance to the first cursor that has a candidate for us.
        while ( ! m_children[m_index].hasMore() ) 
        {
            m_index++ ;

            // Close and return if we exhaust the cursors without finding a
            // valid candidate to return.
            if ( m_index >= m_children.length ) 
            {
                close() ;
                return ;
            }
        }

        // Grab the next candidate and add it's id to the LUT/hash of candidates
        IndexRecord l_rec = ( IndexRecord ) m_children[m_index].next() ;
        m_prefetched.copy( l_rec ) ;
        m_candidates.put( l_rec.getEntryId(), l_rec.getEntryId() ) ;
    }


    // ------------------------------------------------------------------------
    // java.util.Enumeration Implementation Methods 
    // ------------------------------------------------------------------------
    

    /**
     * @see java.util.Enumeration#nextElement()
     */
    public Object nextElement()
    {
        try
        {
            return next() ;
        }
        catch ( NamingException e )
        {
            throw new NoSuchElementException() ;
        }
    }
    
    
    /**
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return hasMore() ;
    }
    

    // ------------------------------------------------------------------------
    // NamingEnumeration Method Implementations
    // ------------------------------------------------------------------------
    

    /**
     * Advances this Cursor one position.  Duplicates are not returned so if
     * underlying cursors keep returning duplicates the child cursors will be
     * advanced until a unique candidate is found or all child cursors are
     * exhausted.
     *
     * @return a candidate element
     * @throws NamingException if an error occurs
     */
    public Object next() throws NamingException
    {
        // Store the last prefetched candidate to return in m_candidate
        m_candidate.copy( m_prefetched ) ;

        do 
        {
            // Advance to a Cursor that has the next valid candidate for us.
            while ( ! m_children[m_index].hasMore() ) 
            {
                m_index++ ;
        
                /* Close and return existing prefetched candidate if we
                 * have exhausted the underlying Cursors without finding a
                 * valid candidate to return.
                 */
                if ( m_index >= m_children.length ) 
                {
                    close() ;
                    return m_candidate ;
                }
            }

            // Grab next candidate!
            IndexRecord l_rec = ( IndexRecord ) m_children[m_index].next() ;
            m_prefetched.copy( l_rec ) ;

            // Break through do/while if the candidate is seen for the first
            // time, meaning we have not returned it already.
        } while ( m_candidates.containsKey( m_prefetched.getEntryId() ) ) ;

        // Add candidate to LUT of encountered candidates.
        m_candidates.put( m_candidate.getEntryId(), m_candidate.getEntryId() ) ;

        // Return the original value saved before overwriting prefetched
        return m_candidate ;
    }


    /**
     * Tests if a prefetched value exists and a call to advance will hence
     * succeed.
     *
     * @return true if a call to advance will succeed false otherwise.
     */
    public boolean hasMore()
    {
        return m_hasMore ;
    }


    /**
     * Closes all the underlying Cursors and not fail fast.  All enumerations 
     * will have close attempts made on them.
     * 
     * @throws NamingException if we cannot close all enumerations
     */
    public void close() throws NamingException
    {
        Throwable l_throwable = null ;
        m_hasMore = false ;
        
        for ( int ii = 0; ii < m_children.length; ii++ ) 
        {
            try
            {
                // Close all children but don't fail fast meaning don't stop
                // closing all children if one fails to close for some reason.
                m_children[ii].close() ;
            }
            catch ( Throwable a_throwable )
            {
                l_throwable = a_throwable ;
            }
        }
        
        if ( null != l_throwable && l_throwable instanceof NamingException )
        {
            throw ( NamingException ) l_throwable ;
        }
        else if ( null != l_throwable )
        {
            NamingException l_ne = new NamingException() ;
            l_ne.setRootCause( l_throwable ) ;
            throw l_ne ;
        }
    }
}
