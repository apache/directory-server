/*
 * $Id: DisjunctionCursor.java,v 1.5 2003/05/04 17:46:52 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.search ;


import java.util.Map ;
import java.util.HashMap ;
import javax.naming.NamingException ;

import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.AtomicBackend ;
import org.apache.eve.backend.BackendModule ;
import org.apache.eve.protocol.ProtocolModule ;
import org.apache.eve.backend.BackendException ;
import org.apache.eve.backend.jdbm.index.IndexRecord;


/**
 * A Cursor of Cursors performing a union on all underlying Cursors resulting
 * in the disjunction of expressions represented by the constituant child
 * Cursors. This cursor prefetches underlying Cursor values so that it can
 * comply with the defined Cursor semantics.
 *
 * @author <a href="mailto:aok123@bellsouth.net"> Alex Karasulu </a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.5 $
 */
public class DisjunctionCursor
    extends Cursor
{
    /** The underlying child cursors */
    private final Cursor [] m_children ;
    /** LUT used to avoid returning duplicates */
    private final Map m_candidates = new HashMap() ;
    /** Index of current cursor used */
    private int m_index = 0 ;
    /** Candidate to return */
    private final IndexRecord m_candidate = new IndexRecord() ;
    /** Prefetched record returned */
    private final IndexRecord m_prefetched = new IndexRecord() ;


    /**
     * Creates a DisjunctionCursor over a set of child Cursors.  The returned
     * result is the union of all underlying Cursors without duplicates.
     *
     * @param a_backend the owning backend instance
     * @param a_children array of child Cursors
     */
    public DisjunctionCursor(Cursor [] a_children)
        throws NamingException
    {
        m_children = a_children ;

        // Close this cursor if their are no children.
        if(a_children.length <= 0) {
            close() ;
            return ;
        }


        // Advance to the first cursor that has a candidate for us.
        while(!m_children[m_index].hasMore()) {
            m_index++ ;

            // Close and return if we exhaust the cursors without finding a
            // valid candidate to return.
            if(m_index >= m_children.length) {
                close() ;
                return ;
            }
        }

        // Grab the next candidate and add it's id to the LUT/hash of candidates
        IndexRecord l_rec = (IndexRecord) m_children[m_index].next() ;
        m_prefetched.setEntryId(l_rec.getEntryId()) ;
        m_prefetched.setIndexKey(l_rec.getIndexKey()) ;
        m_candidates.put(m_prefetched.getEntryId(), m_prefetched.getEntryId()) ;
    }


    /**
     * Advances this Cursor one position.  Duplicates are not returned so if
     * underlying cursors keep returning duplicates the child cursors will be
     * advanced until a unique candidate is found or all child cursors are
     * exhausted.
     *
     * @return a candidate element
     */
    public Object advance()
        throws BackendException, NamingException
    {
        // Store the last prefetched candidate to return in m_candidate
        m_candidate.setEntryId(m_prefetched.getEntryId()) ;
        m_candidate.setIndexKey(m_prefetched.getIndexKey()) ;

        do {
            // Advance to a Cursor that has the next valid candidate for us.
            while(!m_children[m_index].hasMore()) {
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("" //ProtocolModule.getMessageKey()
						+ " - DisjunctionCursor.advance(): child cursor "
						+ m_children[m_index] + " at index "
						+ m_index + " has been exhausted.") ;
				}

                m_index++ ;
        
                // Close and return existing prefetched candidate if we
                // have exhausted the underlying Cursors without finding a
                // valid candidate to return.
                if(m_index >= m_children.length) {
					if(getLogger().isDebugEnabled()) {
						getLogger().debug("" //ProtocolModule.getMessageKey()
							+ " - DisjunctionCursor.advance(): no more child "
							+ "indices left they have all been exhausted. "
							+ "Closing this cursor. Returning last prefetched "
                            + "candidate value of " + m_candidate) ;
					}

                    close() ;
                    return m_candidate ;
                }
            }

            // Grab next candidate!
            IndexRecord l_rec = (IndexRecord) m_children[m_index].next() ;
            m_prefetched.setIndexKey(l_rec.getIndexKey()) ;
            m_prefetched.setEntryId(l_rec.getEntryId()) ;

			if(getLogger().isDebugEnabled()) {
				getLogger().debug("" //ProtocolModule.getMessageKey()
					+ " - DisjunctionCursor.advance(): got next prefetched "
					+ "candidate value of " + m_prefetched.getEntryId()
					+ " Did we see it before? "
                    + m_candidates.containsKey(m_prefetched.getEntryId())) ;
			}

            // Break through do/while if the candidate is seen for the first
            // time, meaning we have not returned it already.
        } while(m_candidates.containsKey(m_prefetched.getEntryId())) ;

		if(getLogger().isDebugEnabled()) {
			getLogger().debug("" //ProtocolModule.getMessageKey()
				+ " - DisjunctionCursor.advance(): got next valid candidate "
				+ "with a value of " + m_prefetched.getEntryId()
                + ". Returning last "
                + "prefetched value of " + m_candidate.getEntryId()) ;
		}

        // Add candidate to LUT of encountered candidates.
        m_candidates.put(m_candidate.getEntryId(), m_candidate.getEntryId()) ;

        // Return the original prefetched value
        return m_candidate ;
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
     * Closes all the underlying Cursors.
     */
    public void freeResources()
    {
        for(int ii = 0; ii < m_children.length; ii++) {
            // Close all children but don't fail fast meaning don't stop
            // closing all children if one fails to close for some reason.
            try {
                m_children[ii].close() ;
            } catch(Throwable t) {
                getLogger().warn("Could not close underlying cursor " +
                    m_children[ii], t) ;
            }
        }
    }
}
