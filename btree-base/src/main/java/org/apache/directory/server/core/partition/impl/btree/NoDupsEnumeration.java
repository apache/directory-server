/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;


/**
 * A simple NamingEnumeration over a TupleBrowser on a table that does not allow
 * duplicates.
 * 
 * <p> WARNING: The Tuple returned by this listing is always the same instance 
 * object returned every time. It is reused to for the sake of efficency rather 
 * than creating a new tuple for each hasMore() call.
 * </p>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NoDupsEnumeration implements NamingEnumeration
{
    /** Temporary Tuple used to return results */
    private final Tuple returned = new Tuple();
    /** Temporary Tuple used to store prefetched values */
    private final Tuple prefetched = new Tuple();
    /** The JDBM TupleBrowser this NamingEnumeration wraps */
    private final TupleBrowser browser;
    /** The direction of this NamingEnumeration */
    private final boolean doAscendingScan;
    /** Whether or not this NamingEnumeration can advance */
    private boolean hasNext = true;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R
    // ------------------------------------------------------------------------

    /**
     * Creates a cursor over a TupleBrowser where duplicates are not expected.
     */
    public NoDupsEnumeration(TupleBrowser browser, boolean doAscendingScan) throws NamingException
    {
        this.browser = browser;
        this.doAscendingScan = doAscendingScan;
        prefetch();
    }


    // ------------------------------------------------------------------------
    // NamingEnumeration Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Returns the same Tuple every time but with different key/value pairs.
     * 
     * @see javax.naming.NamingEnumeration#next()
     */
    public Object next() throws NamingException
    {
        // Load values into the Tuple to return
        returned.setKey( prefetched.getKey() );
        returned.setValue( prefetched.getValue() );

        // Prefetch next set of values to return if and return last prefetched
        prefetch();
        return returned;
    }


    /**
     * Returns the same Tuple every time but with different key/value pairs.
     * 
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
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore()
    {
        return hasNext;
    }


    /**
     * Calls hasMore.
     *
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return hasNext;
    }


    /**
     * Sets hasNext to false.
     *
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close()
    {
        hasNext = false;
    }


    // ------------------------------------------------------------------------
    // Private/Package Friendly Methods
    // ------------------------------------------------------------------------

    /**
     * Gets the direction of this NamingEnumeration.
     *
     * @return true if this NamingEnumeration is ascending on keys, false 
     * otherwise.
     */
    public boolean doAscendingScan()
    {
        return doAscendingScan;
    }


    /**
     * Prefetches a value into prefetched over writing whatever values were 
     * contained in the Tuple.
     *
     * @throws NamingException if the TupleBrowser browser could not advance
     */
    private void prefetch() throws NamingException
    {
        // Prefetch into tuple!
        boolean isSuccess = false;

        if ( doAscendingScan )
        {
            isSuccess = browser.getNext( prefetched );
        }
        else
        {
            isSuccess = browser.getPrevious( prefetched );
        }

        hasNext = isSuccess;
    }
}
