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
package org.apache.directory.server.core.partition.impl.btree;


import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;


/**
 * A Cursor of Cursors performing a union on all underlying Cursors resulting
 * in the disjunction of expressions represented by the constituant child
 * Cursors. This cursor prefetches underlying Cursor values so that it can
 * comply with the defined Cursor semantics.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DisjunctionEnumeration implements NamingEnumeration
{
    /** The underlying child enumerations */
    private final NamingEnumeration[] children;
    /** LUT used to avoid returning duplicates */
    private final Map candidates = new HashMap();
    /** Index of current cursor used */
    private int index = 0;
    /** Candidate to return */
    private final IndexRecord candidate = new IndexRecord();
    /** Prefetched record returned */
    private final IndexRecord prefetched = new IndexRecord();
    /** Used to determine if this enumeration has been exhausted */
    private boolean hasMore = true;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a DisjunctionEnumeration over a set of child NamingEnumerations.
     * The returned result is the union of all underlying NamingEnumerations 
     * without duplicates.
     *
     * @param children array of child NamingInstances
     * @throws NamingException if something goes wrong
     */
    public DisjunctionEnumeration(NamingEnumeration[] children) throws NamingException
    {
        this.children = children;

        // Close this cursor if their are no children.
        if ( children.length <= 0 )
        {
            hasMore = false;
            return;
        }

        // Advance to the first cursor that has a candidate for us.
        while ( !children[index].hasMore() )
        {
            index++;

            // Close and return if we exhaust the cursors without finding a
            // valid candidate to return.
            if ( index >= children.length )
            {
                close();
                return;
            }
        }

        // Grab the next candidate and add it's id to the LUT/hash of candidates
        IndexRecord rec = ( IndexRecord ) children[index].next();
        prefetched.copy( rec );
        candidates.put( rec.getEntryId(), rec.getEntryId() );
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
            return next();
        }
        catch ( NamingException e )
        {
            throw new NoSuchElementException();
        }
    }


    /**
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return hasMore();
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
        // Store the last prefetched candidate to return in candidate
        candidate.copy( prefetched );

        do
        {
            // Advance to a Cursor that has the next valid candidate for us.
            while ( !children[index].hasMore() )
            {
                index++;

                /* Close and return existing prefetched candidate if we
                 * have exhausted the underlying Cursors without finding a
                 * valid candidate to return.
                 */
                if ( index >= children.length )
                {
                    close();
                    return candidate;
                }
            }

            // Grab next candidate!
            IndexRecord rec = ( IndexRecord ) children[index].next();
            prefetched.copy( rec );

            // Break through do/while if the candidate is seen for the first
            // time, meaning we have not returned it already.
        }
        while ( candidates.containsKey( prefetched.getEntryId() ) );

        // Add candidate to LUT of encountered candidates.
        candidates.put( candidate.getEntryId(), candidate.getEntryId() );

        // Return the original value saved before overwriting prefetched
        return candidate;
    }


    /**
     * Tests if a prefetched value exists and a call to advance will hence
     * succeed.
     *
     * @return true if a call to advance will succeed false otherwise.
     */
    public boolean hasMore()
    {
        return hasMore;
    }


    /**
     * Closes all the underlying Cursors and not fail fast.  All enumerations 
     * will have close attempts made on them.
     * 
     * @throws NamingException if we cannot close all enumerations
     */
    public void close() throws NamingException
    {
        Throwable throwable = null;
        hasMore = false;

        for ( int ii = 0; ii < children.length; ii++ )
        {
            try
            {
                // Close all children but don't fail fast meaning don't stop
                // closing all children if one fails to close for some reason.
                children[ii].close();
            }
            catch ( Throwable t )
            {
                throwable = t;
            }
        }

        if ( null != throwable && throwable instanceof NamingException )
        {
            throw ( NamingException ) throwable;
        }
        else if ( null != throwable )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( throwable );
            throw ne;
        }
    }
}
