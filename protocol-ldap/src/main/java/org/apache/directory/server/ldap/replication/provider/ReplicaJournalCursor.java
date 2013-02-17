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

package org.apache.directory.server.ldap.replication.provider;


import java.io.IOException;
import java.util.Iterator;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.controls.ChangeType;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmTable;
import org.apache.directory.server.ldap.replication.ReplicaEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Define a cursor on top of a replication journal.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaJournalCursor extends AbstractCursor<ReplicaEventMessage>
{
    /** Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ReplicaJournalCursor.class );

    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** the underlying journal's cursor */
    private Cursor<Tuple<String, ReplicaEventMessage>> tupleCursor;

    /** the event log journal */
    private JdbmTable<String, ReplicaEventMessage> journal;

    /** the consumer's CSN based on which messages will be qualified for sending */
    private String consumerCsn;

    private ReplicaEventMessage qualifiedEvtMsg;

    /** used while cleaning up the log */
    private boolean skipQualifying;


    /**
     * Creates a cursor on top of the given journal
     * @param journal the log journal
     * @param consumerCsn the consumer's CSN taken from cookie
     * @throws Exception 
     */
    public ReplicaJournalCursor( JdbmTable<String, ReplicaEventMessage> journal, String consumerCsn ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating ReplicaJournalCursor {}", this );
        }

        this.journal = journal;
        this.tupleCursor = journal.cursor();
        this.consumerCsn = consumerCsn;
    }


    /**
     * {@inheritDoc}
     */
    public void after( ReplicaEventMessage arg0 ) throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return ( qualifiedEvtMsg != null );
    }


    /**
     * {@inheritDoc}
     */
    public void before( ReplicaEventMessage arg0 ) throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException, IOException
    {
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public ReplicaEventMessage get() throws CursorException
    {
        return qualifiedEvtMsg;
    }


    /**
     * selects the current queue entry if qualified for sending to the consumer
     * 
     * @throws Exception
     */
    private boolean isQualified( String csn, ReplicaEventMessage evtMsg ) throws LdapException
    {
        LOG.debug( "ReplicaEventMessage: {}", evtMsg );

        if ( evtMsg.isEventOlderThan( consumerCsn ) )
        {
            if ( LOG.isDebugEnabled() )
            {
                String evt = "MODDN"; // take this as default cause the event type for MODDN is null

                ChangeType changeType = evtMsg.getChangeType();

                if ( changeType != null )
                {
                    evt = changeType.name();
                }

                LOG.debug( "event {} for dn {} is not qualified for sending", evt, evtMsg.getEntry().getDn() );
            }

            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException, IOException
    {
        while ( tupleCursor.next() )
        {
            Tuple<String, ReplicaEventMessage> tuple = tupleCursor.get();

            String csn = tuple.getKey();
            ReplicaEventMessage message = tuple.getValue();

            if ( skipQualifying )
            {
                qualifiedEvtMsg = message;
                return true;
            }

            boolean qualified = isQualified( csn, message );

            if ( qualified )
            {
                qualifiedEvtMsg = message;
                return true;
            }
            else
            {
                journal.remove( csn );
            }
        }

        qualifiedEvtMsg = null;

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing ReplicaJournalCursor {}", this );
        }

        tupleCursor.close();
        super.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing ReplicaJournalCursor {}", this );
        }

        tupleCursor.close();
        super.close( cause );
    }


    /**
     * sets the flag to skip CSN based checking while traversing
     * used for internal log cleanup ONLY 
     */
    protected void skipQualifyingWhileFetching()
    {
        skipQualifying = true;
    }


    /**
     * delete the current message
     * used for internal log cleanup ONLY
     */
    protected void delete()
    {
        try
        {
            if ( qualifiedEvtMsg != null )
            {
                journal.remove( qualifiedEvtMsg.getEntry().get( SchemaConstants.ENTRY_CSN_AT ).getString() );
            }
        }
        catch ( Exception e )
        {

        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<ReplicaEventMessage> iterator()
    {
        throw new UnsupportedOperationException();
    }
}
