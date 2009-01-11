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
import org.apache.directory.server.core.DirectoryService;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link ReplicationContext}
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev: $, $Date:  $
 */
public class ReplicationContext
{
    /** A logger for this class */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Timer EXPIRATION_TIMER = new Timer( "ReplicationMessageExpirer" );

    private final ReplicationInterceptor interceptor;
    private final ReplicationConfiguration configuration;
    private final DirectoryService directoryService;
    private final IoSession session;
    private final Map<Integer,ExpirationTask> expirableMessages = new HashMap<Integer,ExpirationTask>();
    private int nextSequence;
    private Replica peer;
    private State state = State.INIT;


    /**
     * Represents the state of the connection between two {@link Replica}s.
     * 
     * @author The Apache Directory Project Team
     */
    public enum State
    {
        /**
         * Connection is established.
         */
        INIT,

        /**
         * Client has logged in and is ready to exchange information.
         */
        READY,
        ;
    }

    
    public ReplicationContext( ReplicationInterceptor interceptor, DirectoryService directoryService,
        ReplicationConfiguration configuration, IoSession session )
    {
        this.interceptor = interceptor;
        this.configuration = configuration;
        this.directoryService = directoryService;
        this.session = session;
    }


    /**
     * Returns the {@link ReplicationInterceptor} which is managing this
     * context.
     */
    public ReplicationInterceptor getService()
    {
        return interceptor;
    }


    /**
     * Returns the current {@link ReplicationConfiguration} of the
     * {@link Replica} which is managing this context.
     */
    public ReplicationConfiguration getConfiguration()
    {
        return configuration;
    }


    /**
     * Returns the {@link DirectoryService} which owns the {@link ReplicationInterceptor}
     * which is managing this context.
     */
    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    /**
     * Returns <a href="http://mina.apache.org/">MINA</a> {@link IoSession}
     * instance that is associated with the current connection to
     * the remote {@link Replica}.
     */
    public IoSession getSession()
    {
        return session;
    }


    /**
     * Generates a new and unique sequence number of protocol message.
     * @return the new sequence number.
     */
    public int getNextSequence()
    {
        return nextSequence++;
    }


    /**
     * Returns the remote peer {@link Replica} that this context is connected
     * to. 
     */
    public Replica getPeer()
    {
        return peer;
    }


    /**
     * Sets the remote peer {@link Replica} that this context is connected
     * to.  A user has authenticate the remote peer first and call this method
     * manually to prevent unauthorized access.
     */
    public void setPeer( Replica peer )
    {
        assert peer != null;
        this.peer = peer;
    }


    /**
     * Returns the current state of the {@link Replica} this context is
     * managing.
     */
    public State getState()
    {
        return state;
    }


    /**
     * Sets the current state of the {@link Replica} this context is
     * managing.
     */
    public void setState( State state )
    {
        this.state = state;
    }


    /**
     * Schedules an expiration of the specified <tt>message</tt>.  A user of
     * this context could call this method with the message it has written out
     * to the remote peer.  If {@link #cancelExpiration(int)} method is not
     * invoked within a certain timeout, an exception will be raised to
     * {@link ReplicationContextHandler#exceptionCaught(ReplicationContext, Throwable)}.
     */
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


    /**
     * Cancels the expiration scheduled by calling
     * {@link #scheduleExpiration(Object)}.  A user of this context could
     * call this method when the response message has been received to
     * stop the expiration for the message with the specified
     * <tt>sequence</tt> number.
     * 
     * @return the request message with the specified <tt>sequence</tt> number
     */
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

    
    /**
     * Forces this context to send replication data to the peer replica immediately.
     * 
     * @return <tt>true</tt> if the replication has been started,
     *         <tt>false</tt> if the replication didn't start because
     *         the replication process is already in progress or
     *         the client is currently logging in to the server yet.
     */
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


    /**
     * Cancels all scheduled expirations.  A user of this context could
     * call this method when the current connection is closed.
     */
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


    /**
     * Returns the number of the scheduled experations.  A user of this
     * contexst could check this value before sending a new message to the
     * remote peer to prevent {@link OutOfMemoryError} by limiting the number
     * of the messages which didn't get their responses.
     */
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
                logger.warn( "No response within " + configuration.getResponseTimeout()
                    + " second(s) for message #" + message.getSequence() );
                getSession().close( true );
            }
        }
    }
}
