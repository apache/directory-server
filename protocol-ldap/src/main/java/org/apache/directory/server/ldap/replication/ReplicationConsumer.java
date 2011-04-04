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

package org.apache.directory.server.ldap.replication;


import org.apache.directory.server.core.DirectoryService;


/**
 * An interface for providers of a service which receives the ldap entries as and when a 
 * event happens in the server. The data received might vary based on the internal configuration
 * used by implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ReplicationConsumer
{
    /**
     * sets the configuration of the consumer
     * 
     * @param config the configuration of the consumer
     */
    void setConfig( ReplicationConsumerConfig config );


    /**
     * @return get the configuration of the consumer
     */
    ReplicationConsumerConfig getConfig();


    /**
     * initializes the consumer
     * 
     * @param dirService the DirectoryService
     * @throws Exception
     */
    void init( DirectoryService dirService ) throws Exception;


    /**
     * starts the consumer
     */
    void start();


    /**
     * stops the consumer
     */
    void stop();


    /**
     * the identifier of this consumer instance
     * @return the identifier of the consumer instance
     */
    String getId();
}
