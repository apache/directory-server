/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.ldap.replication;


import java.util.Iterator;

import org.apache.activemq.ActiveMQQueueBrowser;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.broker.region.Queue;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Define a cursor on top of a message queue.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class ReplicaEventLogCursor extends AbstractCursor<ReplicaEventMessage>
{
    /** Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ReplicaEventLogCursor.class );
    
    /** A browser on top of the queue */
    private ActiveMQQueueBrowser browser;

    /** The queue on top of which we will build the cursor */
    private Queue regionQueue;

    /** the consumer's CSN based on which messages will be qualified for sending */
    private String consumerCsn;

    private ReplicaEventMessage qualifiedEvtMsg;
    
    /**
     * Creates a cursor on top of the given queue
     * @param session The session
     * @param queue The queue
     * @param regionQueue ???
     * @param consumerCsn the consumer's CSN taken from cookie
     * @throws Exception If we can't create a browser on top of the queue
     */
    public ReplicaEventLogCursor( ActiveMQSession session, ActiveMQQueue queue, Queue regionQueue, String consumerCsn ) throws Exception
    {
        browser = ( ActiveMQQueueBrowser ) session.createBrowser( queue );
        
        this.consumerCsn = consumerCsn;        
        this.regionQueue = regionQueue;
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
        return browser.hasMoreElements();
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
        qualifiedEvtMsg = null;
        
        ActiveMQObjectMessage amqObj = ( ActiveMQObjectMessage ) browser.nextElement();
        LOG.debug( "ReplicaEventMessage: {}", amqObj );
        qualifiedEvtMsg = ( ReplicaEventMessage ) amqObj.getObject();
        
        if( qualifiedEvtMsg.isEventOlderThan( consumerCsn ) )
        {
            if( LOG.isDebugEnabled() )
            {
                String evt = "MODDN"; // take this as default cause the event type for MODDN is null
                
                EventType evtType = qualifiedEvtMsg.getEventType();
                if ( evtType != null )
                {
                    evt = evtType.name();
                }
                
                LOG.debug( "event {} for dn {} is not qualified for sending", evt, qualifiedEvtMsg.getEntry().getDn() );
            }
            
            regionQueue.removeMessage( amqObj.getJMSMessageID() );
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
        while( browser.hasMoreElements() )
        {
            selectQualified();
            
            if ( qualifiedEvtMsg != null )
            {
                return true;
            }
        }
        
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
        browser.close();
        super.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws Exception
    {
        browser.close();
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
