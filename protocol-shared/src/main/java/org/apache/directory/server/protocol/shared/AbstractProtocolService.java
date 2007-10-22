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


import org.apache.directory.server.core.DirectoryService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * An abstract base class for a ProtocolService. The start/stop methods have
 * not been implemented.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractProtocolService implements ProtocolService
{
    private boolean started;
    private boolean enabled;
    private String serviceId;
    private String serviceName;
    private String ipAddress;
    private int ipPort = -1;
    private Set<TransportProtocol> transportProtocols;
    private DatagramAcceptor datagramAcceptor;
    private SocketAcceptor socketAcceptor;
    /** directory service core where protocol data is backed */
    private DirectoryService directoryService;


    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }


    public boolean isStarted()
    {
        return started;
    }


    protected void setStarted( boolean started )
    {
        this.started = started;
    }


    public boolean isEnabled()
    {
        return enabled;
    }


    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }


    public String getServiceId()
    {
        return serviceId;
    }


    public void setServiceId( String serviceId )
    {
        this.serviceId = serviceId;
    }


    public String getServiceName()
    {
        return serviceName;
    }


    public void setServiceName( String name )
    {
        this.serviceName = name;
    }


    public String getIpAddress()
    {
        return ipAddress;
    }


    public void setIpAddress( String ipAddress )
    {
        this.ipAddress = ipAddress;
    }


    public int getIpPort()
    {
        return ipPort;
    }


    public void setIpPort( int ipPort )
    {
        if ( ipPort < 0 || ipPort > 65535 )
        {
            throw new IllegalArgumentException( "Invalid port number: " + ipPort );
        }

        this.ipPort = ipPort;
    }


    public Set<TransportProtocol> getTransportProtocols()
    {
        return transportProtocols;
    }


    public void setTransportProtocols( Set<TransportProtocol> transportProtocols )
    {
        Set<TransportProtocol> copy = new HashSet<TransportProtocol>( transportProtocols.size() );
        copy.addAll( transportProtocols );
        this.transportProtocols = Collections.unmodifiableSet( copy );
    }


    public DatagramAcceptor getDatagramAcceptor()
    {
        return datagramAcceptor;
    }


    public void setDatagramAcceptor( DatagramAcceptor datagramAcceptor )
    {
        this.datagramAcceptor = datagramAcceptor;
    }


    public SocketAcceptor getSocketAcceptor()
    {
        return socketAcceptor;
    }


    public void setSocketAcceptor( SocketAcceptor socketAcceptor )
    {
        this.socketAcceptor = socketAcceptor;
    }
}
