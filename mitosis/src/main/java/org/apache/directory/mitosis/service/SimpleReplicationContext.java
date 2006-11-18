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
package org.apache.directory.mitosis.service;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientProtocolHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationProtocolHandler;
import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;


public class SimpleReplicationContext implements ReplicationContext
{
    private static final Timer expirationTimer = new Timer( "ReplicationMessageExpirer" );

    private final ReplicationService service;
    private final ReplicationConfiguration configuration;
    private final DirectoryServiceConfiguration serviceConfiguration;
    private final IoSession session;
    private final Map<Integer,ExpirationTask> expirableMessages = new HashMap<Integer,ExpirationTask>();
    private int nextSequence;
    private Replica peer;
    private State state = State.INIT;


    public SimpleReplicationContext( ReplicationService service, DirectoryServiceConfiguration serviceCfg,
        ReplicationConfiguration configuration, IoSession session )
    {
        this.service = service;
        this.configuration = configuration;
        this.serviceConfiguration = serviceCfg;
        this.session = session;
    }


    public ReplicationService getService()
    {
        return service;
    }


    public ReplicationConfiguration getConfiguration()
    {
        return configuration;
    }


    public DirectoryServiceConfiguration getServiceConfiguration()
    {
        return serviceConfiguration;
    }


    public IoSession getSession()
    {
        return session;
    }


    public int getNextSequence()
    {
        return nextSequence++;
    }


    public Replica getPeer()
    {
        return peer;
    }


    public void setPeer( Replica peer )
    {
        assert peer != null;
        this.peer = peer;
    }


    public State getState()
    {
        return state;
    }


    public void setState( State state )
    {
        this.state = state;
    }


    public void scheduleExpiration( Object message )
    {
        BaseMessage bm = ( BaseMessage ) message;
        ExpirationTask task = new ExpirationTask( bm );
        synchronized ( expirableMessages )
        {
            expirableMessages.put( new Integer( bm.getSequence() ), task );
        }

        expirationTimer.schedule( task, configuration.getResponseTimeout() * 1000L );
    }


    public Object cancelExpiration( int sequence )
    {
        ExpirationTask task = removeTask( sequence );
        if ( task == null )
        {
            return null;
        }

        task.cancel();
        return task.message;
    }
    
    public boolean replicate()
    {
        ReplicationProtocolHandler handler =
            ( ReplicationProtocolHandler ) this.session.getHandler();
        if( !( handler instanceof ReplicationClientProtocolHandler ) )
        {
            throw new UnsupportedOperationException(
                    "Only clients can begin replication." );
        }
        
        ReplicationContextHandler contextHandler = ( ( ReplicationProtocolHandler ) handler ).getContextHandler();
        return ( ( ReplicationClientContextHandler ) contextHandler ).beginReplication( this );
    }


    public void cancelAllExpirations()
    {
        synchronized ( expirableMessages )
        {
            Iterator i = expirableMessages.values().iterator();
            while ( i.hasNext() )
            {
                ( ( ExpirationTask ) i.next() ).cancel();
            }
        }
    }


    public int getScheduledExpirations()
    {
        synchronized ( expirableMessages )
        {
            return expirableMessages.size();
        }
    }


    private ExpirationTask removeTask( int sequence )
    {
        ExpirationTask task;
        synchronized ( expirableMessages )
        {
            task = ( ExpirationTask ) expirableMessages.remove( new Integer( sequence ) );
        }
        return task;
    }

    private class ExpirationTask extends TimerTask
    {
        private final BaseMessage message;


        private ExpirationTask( Object message )
        {
            this.message = ( BaseMessage ) message;
        }


        public void run()
        {
            if ( removeTask( message.getSequence() ) == this )
            {
                SessionLog.warn( getSession(), "No response within " + configuration.getResponseTimeout()
                    + " second(s) for message #" + message.getSequence() );
                getSession().close();
            }
        }
    }
}
