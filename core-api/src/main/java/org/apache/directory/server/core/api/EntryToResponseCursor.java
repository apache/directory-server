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


import java.util.Iterator;

import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
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

    /** the underlying cursor */
    private Cursor<Entry> wrapped;

    /** a reference to hold the SearchResultDone response */
    private SearchResultDone searchDoneResp;

    private boolean done;

    private int messageId;


    public EntryToResponseCursor( int messageId, Cursor<Entry> wrapped )
    {
        LOG_CURSOR.debug( "Creating EntryToResponseCursor {}", this );
        this.wrapped = wrapped;
        this.messageId = messageId;
    }


    public Iterator<Response> iterator()
    {
        throw new UnsupportedOperationException();
    }


    public void after( Response resp ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void afterLast() throws Exception
    {
        wrapped.afterLast();
    }


    public boolean available()
    {
        return wrapped.available();
    }


    public void before( Response resp ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void beforeFirst() throws Exception
    {
        wrapped.beforeFirst();
    }


    public void close() throws Exception
    {
        LOG_CURSOR.debug( "Closing EntryToResponseCursor {}", this );
        wrapped.close();
    }


    public void close( Exception e ) throws Exception
    {
        LOG_CURSOR.debug( "Closing EntryToResponseCursor {}", this );
        wrapped.close( e );
    }


    public boolean first() throws Exception
    {
        return wrapped.first();
    }


    public Response get() throws Exception
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


    public boolean isClosed() throws Exception
    {
        return wrapped.isClosed();
    }


    public boolean last() throws Exception
    {
        return wrapped.last();
    }


    public boolean next() throws Exception
    {
        done = wrapped.next();

        if ( !done )
        {
            searchDoneResp = new SearchResultDoneImpl( messageId );
            searchDoneResp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
        }

        return done;
    }


    public boolean previous() throws Exception
    {
        return wrapped.previous();
    }


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
