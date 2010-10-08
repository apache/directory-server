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
package org.apache.directory.server.protocol.shared;


import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.SocketAcceptor;


/**
 * Minimum functionality required by an ApacheDS protocol service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ProtocolService
{
    /**
     * Stops this ProtocolService which unbinds acceptors on the protocol port.
     *
     * @throws Exception if there are problems stopping this service
     */
    void stop() throws Exception;


    /**
     * Starts this ProtocolService which binds acceptors on the protocol port.
     *
     * @throws Exception if there are problems starting this service
     */
    void start() throws Exception;


    /**
     * Gets whether or not this service has been started.
     *
     * @return true if the service has started, false otherwise
     */
    boolean isStarted();


    /**
     * If this protocol service supports UDP transport then this gets the
     * non-null MINA DatagramAcceptor it uses.
     *
     * @return the MINA DatagramAcceptor used for UDP transports
     */
    DatagramAcceptor getDatagramAcceptor( Transport transport );


    /**
     * If this protocol service support TCP transport then this gets the
     * MINA SocketAcceptor it uses.
     *
     * @return the MINA SocketAcceptor used for TCP transport
     */
    SocketAcceptor getSocketAcceptor( Transport transport );


    /**
     * Services can be enabled or disabled. If enabled they will be started, if
     * not they will not.
     *
     * @return true if this service is to be started, false otherwise
     */
    boolean isEnabled();


    /**
     * Sets whether or not this ProtocolService is enabled.
     *
     * @param enabled true to enable, false to disable
     */
    void setEnabled( boolean enabled );


    /**
     * Gets the instance identifier for this ProtocolService.
     *
     * @return the identifier for the service instance
     */
    String getServiceId();


    /**
     * Sets the instance identifier for this ProtocolService.
     *
     * @param serviceId an identifier for the service instance
     */
    void setServiceId( String serviceId );


    /**
     * Gets a descriptive name for the kind of service this represents.
     * This name is constant across instances of this ProtocolService.
     *
     * @return a descriptive name for the kind of this service
     */
    String getServiceName();


    /**
     * Sets the descriptive name for the kind of service this represents.
     * This name is constant across instances of this ProtocolService.
     * 
     * @param name a descriptive name for the kind of this service
     */
    void setServiceName( String name );
}
