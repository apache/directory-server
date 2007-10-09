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


import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientProtocolHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationProtocolHandler;
import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.directory.server.core.DirectoryService;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;

import java.util.*;

/**
 * The default implementation of {@link ReplicationContext}
 * 
 * @author The Apache Directory Project Team
 */
public class DefaultReplicationContext implements ReplicationContext
{
    private static final Timer EXPIRATION_TIMER = new Timer( "ReplicationMessageExpirer" );

    private final ReplicationInterceptor interceptor;
    private final ReplicationConfiguration configuration;
    private final DirectoryService directoryService;
    private final IoSession session;
    private final Map<Integer,ExpirationTask> expirableMessages = new HashMap<Integer,ExpirationTask>();
    private int nextSequence;
    private Replica peer;
    private State state = State.INIT;


    public DefaultReplicationContext( ReplicationInterceptor interceptor, DirectoryService directoryService,
        ReplicationConfiguration configuration, IoSession session )
    {
        this.interceptor = interceptor;
        this.configuration = configuration;
        this.directoryService = directoryService;
        this.session = session;
    }


    public ReplicationInterceptor getService()
    {
        return interceptor;
    }


    public ReplicationConfiguration getConfiguration()
    {
        return configuration;
    }


    public DirectoryService getDirectoryService()
    {
        return directoryService;
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
            expirableMessages.put( bm.getSequence(), task );
        }

        EXPIRATION_TIMER.schedule( task, configuration.getResponseTimeout() * 1000L );
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
        
        ReplicationContextHandler contextHandler = handler.getContextHandler();
        return ( ( ReplicationClientContextHandler ) contextHandler ).beginReplication( this );
    }


    public void cancelAllExpirations()
    {
        synchronized ( expirableMessages )
        {
            for ( ExpirationTask expirationTask : expirableMessages.values() )
            {
                ( expirationTask ).cancel();
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
            task = expirableMessages.remove( sequence );
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
