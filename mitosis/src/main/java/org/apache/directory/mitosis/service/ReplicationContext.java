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
import org.apache.directory.mitosis.service.protocol.handler.ReplicationContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationServerContextHandler;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.mina.common.IoSession;


/**
 * A <a href="http://www.corej2eepatterns.com/Patterns2ndEd/ContextObject.htm">context object</a>
 * that provides the functions required for a client or a server to implement
 * networking code related with replication communication.  This is provided
 * to and used by {@link ReplicationClientContextHandler} and
 * {@link ReplicationServerContextHandler}.
 *   
 * @author The Apache Directory Project Team
 */
public interface ReplicationContext
{
    /**
     * Returns <a href="http://mina.apache.org/">MINA</a> {@link IoSession}
     * instance that is associated with the current connection to
     * the remote {@link Replica}.
     */
    IoSession getSession();

    /**
     * Returns the current {@link ReplicationConfiguration} of the
     * {@link Replica} which is managing this context.
     */
    ReplicationConfiguration getConfiguration();

    /**
     * Returns the {@link ReplicationService} which is managing this
     * context.
     */
    ReplicationService getService();


    /**
     * Returns the {@link DirectoryServiceConfiguration} which owns the
     * {@link ReplicationService} which is managing this context.
     */
    DirectoryServiceConfiguration getServiceConfiguration();


    /**
     * Generates a new and unique sequence number of protocol message.
     * @return the new sequence number.
     */
    int getNextSequence();


    /**
     * Returns the remote peer {@link Replica} that this context is connected
     * to. 
     */
    Replica getPeer();


    /**
     * Sets the remote peer {@link Replica} that this context is connected
     * to.  A user has authenticate the remote peer first and call this method
     * manually to prevent unauthorized access.
     */
    void setPeer( Replica peer );

    /**
     * Returns the current state of the {@link Replica} this context is
     * managing.
     */
    State getState();

    /**
     * Sets the current state of the {@link Replica} this context is
     * managing.
     */
    void setState( State state );


    /**
     * Schedules an expiration of the specified <tt>message</tt>.  A user of
     * this context could call this method with the message it has written out
     * to the remote peer.  If {@link #cancelExpiration(int)} method is not
     * invoked within a certain timeout, an exception will be raised to
     * {@link ReplicationContextHandler#exceptionCaught(ReplicationContext, Throwable)}.
     */
    void scheduleExpiration( Object message );

    /**
     * Cancels the expiration scheduled by calling
     * {@link #scheduleExpiration(Object)}.  A user of this context could
     * call this method when the response message has been received to
     * stop the expiration for the message with the specified
     * <tt>sequence</tt> number.
     * 
     * @return the request message with the specified <tt>sequence</tt> number
     */
    Object cancelExpiration( int sequence );

    /**
     * Cancells all scheduled expirations.  A user of this context could
     * call this method when the current connection is closed.
     */
    void cancelAllExpirations();

    /**
     * Returns the number of the scheduled experations.  A user of this
     * contexst could check this value before sending a new message to the
     * remote peer to prevent {@link OutOfMemoryError} by limiting the number
     * of the messages which didn't get their responses.
     */
    int getScheduledExpirations();
    
    
    /**
     * Forces this context to send replication data to the peer replica immediately.
     * 
     * @return <tt>true</tt> if the replication has been started,
     *         <tt>false</tt> if the replication didn't start because
     *         the replication process is already in progress or
     *         the client is currently logging in to the server yet.
     */
    boolean replicate();

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
}
