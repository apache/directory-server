/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.jndi;


import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;


/**
 * Adapts a Cursor over entries into a NamingEnumeration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NamingEnumerationAdapter implements NamingEnumeration<ClonedServerEntry>
{
    private final EntryFilteringCursor cursor;
    private boolean available = false;
    
    
    public NamingEnumerationAdapter( EntryFilteringCursor cursor ) throws NamingException
    {
        this.cursor = cursor;
        try
        {
            if ( ! cursor.first() )
            {
                cursor.close();
                available = false;
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }
    
    
    /* 
     * @see NamingEnumeration#close()
     */
    public void close() throws NamingException
    {
        try
        {
            cursor.close();
            available = false;
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    /* 
     * @see NamingEnumeration#hasMore()
     */
    public boolean hasMore() throws NamingException
    {
        return available;
    }


    /* 
     * @see NamingEnumeration#next()
     */
    public ClonedServerEntry next() throws NamingException
    {
        ClonedServerEntry entry = null;
        
        try
        {
            entry = cursor.get();
            if ( available = cursor.next() )
            {
                cursor.close();
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
        
        return entry;
    }


    /* 
     * @see Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return available;
    }


    /* 
     * @see Enumeration#nextElement()
     */
    public ClonedServerEntry nextElement()
    {
        ClonedServerEntry entry = null;
        
        try
        {
            entry = cursor.get();
            if ( available = cursor.next() )
            {
                cursor.close();
            }
        }
        catch ( Exception e )
        {
            NoSuchElementException nsee = new NoSuchElementException( e.getMessage() );
            nsee.initCause( e );
            throw nsee;
        }
        
        return entry;
    }
}
