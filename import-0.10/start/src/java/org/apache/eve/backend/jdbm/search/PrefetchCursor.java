/*
 * $Id: PrefetchCursor.java,v 1.3 2003/03/13 18:27:29 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.search ;


import java.util.Map ;
import java.util.HashMap ;
import java.util.ArrayList ;

import java.math.BigInteger ;
import javax.naming.NamingException ;

import org.apache.eve.backend.Cursor ;
import org.apache.ldap.common.filter.ExprNode ;
import org.apache.eve.backend.jdbm.JdbmDatabase ;
import org.apache.eve.protocol.ProtocolModule ;
import org.apache.eve.backend.BackendException ;
import org.apache.eve.backend.jdbm.search.Assertion ;
import org.apache.eve.backend.jdbm.index.IndexRecord ;


/**
 * A LogicalCursor represents a Cursor over wither a AND/NOT/OR expression in a
 * filter. This cursor prefetches underlying Cursor values so that it can comply
 * with the defined Cursor semantics.
 * 
 * @author <a href="mailto:aok123@bellsouth.net"> Alex Karasulu </a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 */
public class PrefetchCursor
    extends Cursor
{
    /** The prefetched candidate */
    private final IndexRecord m_prefetched = new IndexRecord() ;
    /** The returned candidate */
    private final IndexRecord m_candidate = new IndexRecord() ;
    /** The iteration cursor */
    private final Cursor m_cursor ;
    /** LUT used to avoid returning duplicates */
    private final Map m_candidates ;
    private final Assertion m_assertion ;
    private final boolean checkDups ;


    /**
     * Cursor over a conjunction expression.  All children except the candidate
     * child to be used for iteration are provided as expressions. The child
     * cursor is the basis for the iteration.
     *
     * @param a_backend the owning backend instance
     * @param a_cursor underlying iteration cursor
     * @param a_assertions array of assertions minus the assertion expression
     * used to construct this cursor.
     */
    public PrefetchCursor(Cursor a_cursor, Assertion a_assertion)
        throws BackendException, NamingException
    {
        m_cursor = a_cursor ;
        m_candidates = null ;
        m_assertion = a_assertion ;
        checkDups = false ;
        prefetch() ;
    }


    public PrefetchCursor(Cursor a_cursor,
        Assertion a_assertion, boolean enableDupCheck)
        throws BackendException, NamingException
    {
        m_cursor = a_cursor ;
        m_candidates = new HashMap() ;
        m_assertion = a_assertion ;
        checkDups = true ;
        prefetch() ;
    }


    /**
     * Checks to see is a candidate is valid by evaluation all assertions
     * against the candidate.  Any assertion failure short circuts tests
     * returning false.  Success through all assertions returns true.
     *
     * @param a_candidate the candidate to assert
     * @return true if the candidate is valid, false otherwise
     * @throws ClassCastException if the candidate is not a BigInteger.
     */
    protected boolean assertCandidate(Object a_candidate)
        throws BackendException, NamingException
    {
        return m_assertion.assertCandidate(a_candidate) ;
    }


    /**
     * Advances this Cursor one position.  Underlying Cursor may be advanced
     * several possitions while trying to find the next prefetched candidate to
     * return.  If underlying Cursor elements are not valid meaning they do not
     * pass assertions then they are rejected for return and the next item is
     * tested.  If the underlying Cursor is consumed, then the last prefetched
     * value is returned and this Cursor is closed.
     *
     * @return a valid candidate element that passed all assertions.
     */
    public Object advance()
        throws BackendException, NamingException
    {
        m_candidate.setEntryId(m_prefetched.getEntryId()) ;
        m_candidate.setIndexKey(m_prefetched.getIndexKey()) ;
        prefetch() ;
        return m_candidate ;
    }


    private void prefetch()
        throws BackendException, NamingException
    {
        IndexRecord l_rec = null ;

        // Scan underlying Cursor until we arrive at the next valid candidate
        // if the cursor is exhuasted we clean up after completing the loop
        while(m_cursor.hasMore()) {
            l_rec = (IndexRecord) m_cursor.next() ;

            // If value is valid then we set it as the next candidate to return
            if(assertCandidate(l_rec)) {
                // dup checking is on but candidate is not in already seen LUT
                // so we need to set it as next to return and add it to the LUT
                if(checkDups && !m_candidates.containsKey(l_rec.getEntryId())) {
                    m_prefetched.setEntryId(l_rec.getEntryId()) ;
                    m_prefetched.setIndexKey(l_rec.getIndexKey()) ;
                    m_candidates.put(l_rec.getEntryId(), l_rec.getEntryId()) ;
                    return ;
                // dup checking is on and candidate has already been seen so we
                // need to skip it.
                } else if(checkDups &&
                    m_candidates.containsKey(l_rec.getEntryId()))
                {
                    continue ;
                }

                m_prefetched.setEntryId(l_rec.getEntryId()) ;
                m_prefetched.setIndexKey(l_rec.getIndexKey()) ;
                return ;
            }
        }

        // At this pt the underlying Cursor has been exhaused so we close up
        // and set the prefetched value to null so canAdvance returns false.
        close() ;
    }


    /**
     * Tests if a prefetched value exists and a call to advance will hence
     * succeed.
     *
     * @return true if a call to advance will succeed false otherwise.
     */
    public boolean canAdvance()
        throws BackendException, NamingException
    {
        return !isClosed() ;
    }


    /**
     * Closes the underlying Cursor.
     */
    public void freeResources()
    {
        try {
            m_cursor.close() ;
        } catch(NamingException e) {
            getLogger().warn("Could not close conjunction cursor child", e) ;
        }
    }
}
