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

package org.apache.directory.server.core.api;


import java.io.IOException;
import java.util.Iterator;

import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.CursorException;
import org.apache.directory.shared.ldap.model.cursor.SearchCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.IntermediateResponse;
import org.apache.directory.shared.ldap.model.message.Referral;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultDoneImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultEntryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A cursor to get SearchResponses after setting the underlying cursor's
 * ServerEntry object in SearchResultEnty object
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryToResponseCursor extends AbstractCursor<Response> implements SearchCursor
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** the underlying cursor */
    private Cursor<Entry> wrapped;

    /** a reference to hold the SearchResultDone response */
    private SearchResultDone searchDoneResp;

    /** The done flag */
    private boolean done;

    /** The messsage ID */
    private int messageId;


    public EntryToResponseCursor( int messageId, Cursor<Entry> wrapped )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating EntryToResponseCursor {}", this );
        }
        
        this.wrapped = wrapped;
        this.messageId = messageId;
    }


    public Iterator<Response> iterator()
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void after( Response resp ) throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException, IOException
    {
        wrapped.afterLast();
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return wrapped.available();
    }


    /**
     * {@inheritDoc}
     */
    public void before( Response resp ) throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException, IOException
    {
        wrapped.beforeFirst();
    }


    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing EntryToResponseCursor {}", this );
        }
        
        wrapped.close();
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception e )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing EntryToResponseCursor {}", this );
        }
        
        wrapped.close( e );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException, IOException
    {
        return wrapped.first();
    }


    /**
     * {@inheritDoc}
     */
    public Response get() throws CursorException, IOException
    {
        Entry entry = wrapped.get();
        SearchResultEntry se = new SearchResultEntryImpl( messageId );
        se.setEntry( entry );

        return se;
    }


    /**
     * gives the SearchResultDone message received at the end of search results
     *
     * @return the SearchResultDone message, null if the search operation fails for any reason
     */
    public SearchResultDone getSearchResultDone()
    {
        return searchDoneResp;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isClosed()
    {
        return wrapped.isClosed();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException, IOException
    {
        return wrapped.last();
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException, IOException
    {
        boolean next = wrapped.next();

        if ( !next )
        {
            searchDoneResp = new SearchResultDoneImpl( messageId );
            searchDoneResp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
            done = true;
        }

        return next;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException, IOException
    {
        return wrapped.previous();
    }


    /**
     * {@inheritDoc}
     */
    public void setClosureMonitor( ClosureMonitor monitor )
    {
        wrapped.setClosureMonitor( monitor );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDone()
    {
        return done;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isReferral()
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public Referral getReferral() throws LdapException
    {
        throw new LdapException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEntry()
    {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public Entry getEntry() throws LdapException
    {
        if ( !done && wrapped.available() )
        {
            try
            {
                return wrapped.get();
            }
            catch ( Exception e )
            {
                throw new LdapException( e );
            }
        }

        throw new LdapException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isIntermediate()
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public IntermediateResponse getIntermediate() throws LdapException
    {
        throw new LdapException();
    }
}
