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
package org.apache.directory.server.core.partition.impl.btree;


import java.io.IOException;

import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.PartitionSearchResult;
import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.CursorException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Adapts index cursors to return just Entry objects.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryCursorAdaptor extends AbstractCursor<Entry>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private final Cursor<IndexEntry<String, String>> indexCursor;
    private final Evaluator<? extends ExprNode> evaluator;


    public EntryCursorAdaptor( AbstractBTreePartition db, PartitionSearchResult searchResult )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating EntryCursorAdaptor {}", this );
        }
        
        indexCursor = searchResult.getResultSet();
        evaluator = searchResult.getEvaluator();
    }


    /**
     * {@inheritDoc}
     */
    public void after( Entry element ) throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException, IOException
    {
        this.indexCursor.afterLast();
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return indexCursor.available();
    }


    /**
     * {@inheritDoc}
     */
    public void before( Entry element ) throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException, IOException
    {
        indexCursor.beforeFirst();
    }


    /**
     * {@inheritDoc}
     */
    public final void setClosureMonitor( ClosureMonitor monitor )
    {
        indexCursor.setClosureMonitor( monitor );
    }


    /**
     * {@inheritDoc}}
     */
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing EntryCursorAdaptor {}", this );
        }
        
        indexCursor.close();
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing EntryCursorAdaptor {}", this );
        }
        
        indexCursor.close( cause );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException, IOException
    {
        return indexCursor.first();
    }


    /**
     * {@inheritDoc}
     */
    public Entry get() throws CursorException, IOException
    {
        IndexEntry<String, String> indexEntry = indexCursor.get();

        try
        {
            if ( evaluator.evaluate( indexEntry ) )
            {
                Entry entry = indexEntry.getEntry();
                indexEntry.setEntry( null );
                
                return entry;
            }
            else
            {
                indexEntry.setEntry( null );
            }
    
            return null;
        }
        catch ( Exception e )
        {
            throw new CursorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isClosed()
    {
        return indexCursor.isClosed();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException, IOException
    {
        return indexCursor.last();
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException, IOException
    {
        return indexCursor.next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException, IOException
    {
        return indexCursor.previous();
    }
    

    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "EntryCursorAdaptor\n" );
        
        if ( indexCursor != null )
        {
            sb.append( tabs ).append( "    " ).append( "IndexCursor : \n" );
            sb.append( indexCursor.toString( tabs + "        " ) );
        }
        
        if ( evaluator != null )
        {
            sb.append( tabs ).append( "    " ).append( "Evaluator : \n" );
            sb.append( evaluator.toString( tabs + "        " ) );
        }
        
        return sb.toString();
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
