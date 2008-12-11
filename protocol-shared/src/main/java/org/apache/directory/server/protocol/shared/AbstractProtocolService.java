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
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.SocketAcceptor;

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
    
    /** The server IP address */
    private String ipAddress;
    
    /** The service's port, if there is only one (TCP or UDP) */
    private int ipPort = -1;
    
    /** The TCP port, if defined. */
    private int tcpPort = -1;
    
    /** The UDP port, if defined. */
    private int udpPort = -1;
    
    private Set<TransportProtocol> transportProtocols;
    
    /** The IoAcceptor used to accept UDP requests */
    private DatagramAcceptor datagramAcceptor;
    
    /** The IoAcceptor used to accept TCP requests */
    private SocketAcceptor socketAcceptor;
    
    /** The number of threads to use for the IoAcceptor executor */
    private int nbThreads;
    
    /** 
     * The number of threads to use for the TCP transport
     * protocol based IoAcceptor executor 
     **/
    private int nbTcpThreads;
    
    /** 
     * The number of threads to use for the UDP transport
     * protocol based IoAcceptor executor 
     **/
    private int nbUdpThreads;
    
    /** The backlog for all the transport services */
    private int ipBacklog;
    
    /** The backlog for the TCP transport services */
    private int tcpBacklog;
    
    /** The backlog for the UDP transport services */
    private int udpBacklog;
    
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


    /**
     * {@inheritDoc}
     */
    public int getIpPort()
    {
        return ipPort;
    }


    /**
     * {@inheritDoc}
     */
    public int getTcpPort()
    {
        return tcpPort;
    }


    /**
     * {@inheritDoc}
     */
    public int getUdpPort()
    {
        return udpPort;
    }


    /**
     * {@inheritDoc}
     */
    public void setIpPort( int ipPort )
    {
        if ( ( ipPort < 0 ) || ( ipPort > 65535 ) )
        {
            throw new IllegalArgumentException( "Invalid port number: " + ipPort );
        }

        this.ipPort = ipPort;
        
        // Now, substitute the existing values by the new one
         udpPort = ipPort;
         tcpPort = ipPort;
    }


    /**
     * {@inheritDoc}
     */
    public void setTcpPort( int tcpPort )
    {
        if ( ( tcpPort < 0 ) || ( tcpPort > 65535 ) )
        {
            throw new IllegalArgumentException( "Invalid port number: " + tcpPort );
        }

        this.tcpPort = tcpPort;
    }

    
    /**
     * {@inheritDoc}
     */
    public void setUdpPort( int udpPort )
    {
        if ( ( udpPort < 0 ) || ( udpPort > 65535 ) )
        {
            throw new IllegalArgumentException( "Invalid port number: " + udpPort );
        }

        this.udpPort = udpPort;
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


    /**
     * {@inheritDoc}
     */
    public DatagramAcceptor getDatagramAcceptor()
    {
        return datagramAcceptor;
    }


    /**
     * {@inheritDoc}
     */
    public void setDatagramAcceptor( DatagramAcceptor datagramAcceptor )
    {
        this.datagramAcceptor = datagramAcceptor;
    }


    /**
     * {@inheritDoc}
     */
    public SocketAcceptor getSocketAcceptor()
    {
        return socketAcceptor;
    }


    /**
     * {@inheritDoc}
     */
    public void setSocketAcceptor( SocketAcceptor socketAcceptor )
    {
        this.socketAcceptor = socketAcceptor;
    }

    
    /**
     * @return The number of thread used in the IoAcceptor executor. It is
     * used if no specific transport protocol is defined, and will be
     * overloaded by the specific NbUdpThreads or nbTcpThreads if those
     * transport protocols are defined.
     */
    public int getNbThreads() 
    {
        return nbThreads;
    }


    /**
     * @return The number of thread used in the IoAcceptor executor for
     * a TCP transport protocol based Acceptor.
     */
    public int getNbTcpThreads() 
    {
        return nbTcpThreads;
    }


    /**
     * @return The number of thread used in the IoAcceptor executor for
     * a UDP transport protocol based Acceptor.
     */
    public int getNbUdpThreads() 
    {
        return nbUdpThreads;
    }


    /**
     * @param nbThreads The number of thread to affect to the IoAcceptor
     * executor. This number will be injected into the UDP and TCP
     * nbThreads value.
     */
    public void setNbThreads(int nbThreads) 
    {
        this.nbThreads = nbThreads;
        this.nbTcpThreads = nbThreads;
        this.nbUdpThreads = nbThreads;
    }


    /**
     * @param nbThreads The number of thread to affect to the 
     * TCP transport protocol based IoAcceptor executor
     */
    public void setNbTcpThreads(int nbTcpThreads) 
    {
        this.nbTcpThreads = nbTcpThreads;
    }


    /**
     * @param nbThreads The number of thread to affect to the 
     * UDP transport protocol based IoAcceptor executor
     */
    public void setNbUdpThreads(int nbUdpThreads) 
    {
        this.nbUdpThreads = nbUdpThreads;
    }


    /**
     * @return the ipBacklog
     */
    public int getIpBacklog() {
        return ipBacklog;
    }


    /**
     * @param ipBacklog the ipBacklog to set
     */
    public void setIpBacklog(int ipBacklog) {
        if ( ipBacklog < 0  )
        {
            throw new IllegalArgumentException( "Invalid backlog number: " + ipBacklog );
        }

        this.ipBacklog = ipBacklog;
        
        // Now, substitute the existing values by the new one
        tcpBacklog = ipBacklog;
        udpBacklog = ipBacklog;
    }


    /**
     * @return the tcpBacklog
     */
    public int getTcpBacklog() {
        return tcpBacklog;
    }


    /**
     * @param tcpBacklog the tcpBacklog to set
     */
    public void setTcpBacklog(int tcpBacklog) {
        this.tcpBacklog = tcpBacklog;
    }


    /**
     * @return the udpBacklog
     */
    public int getUdpBacklog() {
        return udpBacklog;
    }


    /**
     * @param udpBacklog the udpBacklog to set
     */
    public void setUdpBacklog(int udpBacklog) {
        this.udpBacklog = udpBacklog;
    }
}
