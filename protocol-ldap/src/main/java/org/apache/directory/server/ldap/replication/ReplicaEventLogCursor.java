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
import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class ReplicaEventLogCursor extends AbstractCursor<ReplicaEventMessage>
{

    private static final Logger LOG = LoggerFactory.getLogger( ReplicaEventLogCursor.class );
    
    private ActiveMQQueueBrowser browser;

    private Queue regionQueue;


    public ReplicaEventLogCursor( ActiveMQSession session, ActiveMQQueue queue, Queue regionQueue ) throws Exception
    {
        // commit before starting browser, to see the latest view of the Queue data
//        session.commit();
        
        browser = ( ActiveMQQueueBrowser ) session.createBrowser( queue );
        
        this.regionQueue = regionQueue;
    }


    public void after( ReplicaEventMessage arg0 ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void afterLast() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean available()
    {
        return browser.hasMoreElements();
    }


    public void before( ReplicaEventMessage arg0 ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void beforeFirst() throws Exception
    {
    }


    public boolean first() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public ReplicaEventMessage get() throws Exception
    {
        ActiveMQObjectMessage amqObj = ( ActiveMQObjectMessage ) browser.nextElement();
        LOG.debug( "ReplicaEventMessage: {}", amqObj );
        ReplicaEventMessage message = ( ReplicaEventMessage ) amqObj.getObject();
        regionQueue.removeMessage( amqObj.getJMSMessageID() );
        
        return message;
    }


    public boolean last() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean next() throws Exception
    {
        return browser.hasMoreElements();
    }


    public boolean previous() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void close() throws Exception
    {
        browser.close();
        super.close();
    }


    @Override
    public void close( Exception cause ) throws Exception
    {
        browser.close();
        super.close( cause );
    }


    @Override
    public Iterator<ReplicaEventMessage> iterator()
    {
        throw new UnsupportedOperationException();
    }

}
