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

package org.apache.directory.server.ldap.replication.consumer;


import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.ldap.replication.ReplicationConsumerConfig;


/**
 * An interface for consumers of a service which receives the ldap entries as and when a 
 * event happens in the server. The data received might vary based on the internal configuration
 * used by implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ReplicationConsumer
{
    /** A flag we used when we want to connect without waiting */
    boolean NOW = true;
    
    /** A flag we used when we want to connect after a waiting delay */
    boolean DIFFERED = false;
    
    /**
     * Sets the configuration of the consumer
     * 
     * @param config the configuration of the consumer
     */
    void setConfig( ReplicationConsumerConfig config );


    /**
     * @return get the configuration of the consumer
     */
    ReplicationConsumerConfig getConfig();


    /**
     * Initializes the consumer
     * 
     * @param dirService the DirectoryService
     * @throws Exception If the initialization failed
     */
    void init( DirectoryService dirService ) throws Exception;


    /**
     * Connect the consumer, connection immediately or wait before reconnection
     * 
     * @param now A param that tells the consumer to connect immediately or not
     * @return true if the consumer is connected, false otherwise
     */
    boolean connect( boolean now );
    
    
    /**
     * Test the connection with the provider. It does connect to the provider, and
     * tries to bind on it using the consumer credentials.
     */
    void ping();


    /**
     * Stops the consumer
     */
    void stop();


    /**
     * @return the identifier of the consumer instance
     */
    String getId();

    
    /**
     * Starts the synchronization operation
     * 
     * @return The replication status
     */
    ReplicationStatusEnum startSync();
}
