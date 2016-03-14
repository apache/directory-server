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
package org.apache.directory.server.protocol.shared.transport;


import org.apache.mina.core.service.IoAcceptor;


public interface Transport
{
    /**
     * Initialize the Acceptor if needed
     */
    void init();


    /**
     * @return The associated Address
     */
    String getAddress();


    /**
     * Set the InetAddress for this transport.
     * @param address The address to set
     */
    void setAddress( String address );


    /**
     * Gets the port for this service.
     *
     * @return the port for this service
     */
    int getPort();


    /**
     * Sets the port for this service.
     *
     * @param port the port for this service
     * @throws IllegalArgumentException if the port number is not within a valid range
     */
    void setPort( int port );


    /**
     * @return The associated IoAcceptor
     */
    IoAcceptor getAcceptor();


    /**
     * @return The number of processing threads for this acceptor
     */
    int getNbThreads();


    /**
     * Set the number of processing threads for the acceptor
     * @param nbThreads The number of threads to create in the acceptor
     */
    void setNbThreads( int nbThreads );


    /**
     * @return The number of messages stored into the backlog when the 
     * acceptor is being busy processing the current messages
     */
    int getBackLog();


    /**
     * Set the size of the messages queue waiting for the acceptor to
     * be ready.
     * @param backLog The queue size
     */
    void setBackLog( int backLog );


    /**
     * Enable or disable SSL
     * @param sslEnabled if <code>true</code>, SSL is enabled.
     */
    void setEnableSSL( boolean sslEnabled );


    /**
     * Enable or disable SSL
     * @param sslEnabled if <code>true</code>, SSL is enabled.
     */
    void enableSSL( boolean sslEnabled );


    /**
     * @return <code>true</code> if SSL is enabled for this transport
     */
    boolean isSSLEnabled();
}
