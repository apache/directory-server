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
package org.apache.directory.mitosis.service.protocol.handler;


import org.apache.directory.mitosis.service.ReplicationContext;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;

/**
 * An interface that provides handler methods for events which occurs
 * when a two replicas communicate with each other.  This interface is
 * very similar to MINA {@link IoHandler}, but there's a difference
 * in that this interface provide a {@link ReplicationContext} instead of
 * an {@link IoHandler}.  It's usually wrapped by
 * {@link ReplicationProtocolHandler} to work with MINA.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface ReplicationContextHandler
{
    /**
     * Invoked when a connection is established between two replicas.
     */
    void contextBegin( ReplicationContext ctx ) throws Exception;

    /**
     * Invoked when a connection is closed between two replicas.
     */
    void contextEnd( ReplicationContext ctx ) throws Exception;

    /**
     * Invoked when a message is received from a peer replica.
     */
    void messageReceived( ReplicationContext ctx, Object message ) throws Exception;

    /**
     * Invoked when a message is received from a peer replica.
     */
    void messageSent( ReplicationContext ctx, Object message ) throws Exception;

    /**
     * Invoked when an exception is raised during the communication or
     * executing replication logic.
     */
    void exceptionCaught( ReplicationContext ctx, Throwable cause ) throws Exception;

    /**
     * Invoked when two replicas are not exchanging any data for certain
     * amount of time.
     */
    void contextIdle( ReplicationContext ctx, IdleStatus status ) throws Exception;
}
