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

import java.util.Iterator;

import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmTable;
import org.apache.directory.server.ldap.replication.ReplicaEventMessage;
import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.message.controls.ChangeType;
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
    
    /** the underlying journal's cursor */
    private Cursor<Tuple<String, ReplicaEventMessage>> tupleCursor;
    
    /** the event log journal */
    private JdbmTable<String, ReplicaEventMessage> journal;

    /** the consumer's CSN based on which messages will be qualified for sending */
    private String consumerCsn;

    private ReplicaEventMessage qualifiedEvtMsg;
    
    /**
     * Creates a cursor on top of the given journal
     * @param journal the log journal
     * @param consumerCsn the consumer's CSN taken from cookie
     * @throws Exception 
     */
    public ReplicaJournalCursor( JdbmTable<String, ReplicaEventMessage> journal, String consumerCsn ) throws Exception
    {
        this.journal = journal;
        this.tupleCursor = journal.cursor();
        this.consumerCsn = consumerCsn;
    }


    /**
     * {@inheritDoc}
     */
    public void after( ReplicaEventMessage arg0 ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
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
    public void before( ReplicaEventMessage arg0 ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public ReplicaEventMessage get() throws Exception
    {
        return qualifiedEvtMsg;
    }

    
    /**
     * selects the current queue entry if qualified for sending to the consumer
     * 
     * @throws Exception
     */
    private void selectQualified() throws Exception
    {
        Tuple<String, ReplicaEventMessage> t = tupleCursor.get();
        
        qualifiedEvtMsg = t.getValue();
        
        LOG.debug( "ReplicaEventMessage: {}", qualifiedEvtMsg );
        
        if ( qualifiedEvtMsg.isEventOlderThan( consumerCsn ) )
        {
            if( LOG.isDebugEnabled() )
            {
                String evt = "MODDN"; // take this as default cause the event type for MODDN is null
                
                ChangeType changeType = qualifiedEvtMsg.getChangeType();
                
                if ( changeType != null )
                {
                    evt = changeType.name();
                }
                
                LOG.debug( "event {} for dn {} is not qualified for sending", evt, qualifiedEvtMsg.getEntry().getDn() );
            }
            
            // TODO need to be checked if this causes issues in JDBM
            journal.remove( t.getKey() );
            qualifiedEvtMsg = null;
        }
    }
    

    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        while( tupleCursor.next() )
        {
            selectQualified();
            
            if ( qualifiedEvtMsg != null )
            {
                return true;
            }
        }
        
        qualifiedEvtMsg = null;
        
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception
    {
        tupleCursor.close();
        super.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws Exception
    {
        tupleCursor.close();
        super.close( cause );
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
