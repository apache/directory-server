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
package org.apache.directory.server.kerberos.kdc;


import java.io.IOException;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.changepwd.ChangePasswordServer;
import org.apache.directory.server.kerberos.protocol.KerberosProtocolHandler;
import org.apache.directory.server.kerberos.protocol.codec.KerberosProtocolCodecFactory;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.replay.ReplayCacheImpl;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Contains the configuration parameters for the Kerberos protocol provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcServer extends DirectoryBackedService
{
    private static final long serialVersionUID = 522567370475574165L;

    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( KdcServer.class );

    /** The default kdc service name */
    private static final String SERVICE_NAME = "Keydap Kerberos Service";

    /** the cache used for storing AS and TGS requests */
    private ReplayCache replayCache;

    private KerberosConfig config;

    private ChangePasswordServer changePwdServer;


    /**
     * Creates a new instance of KdcServer with the default configuration.
     */
    public KdcServer()
    {
        this( new KerberosConfig() );
    }


    /**
     * 
     * Creates a new instance of KdcServer with the given config.
     *
     * @param config the kerberos server configuration
     */
    public KdcServer( KerberosConfig config )
    {
        this.config = config;
        super.setServiceName( SERVICE_NAME );
        super.setSearchBaseDn( config.getSearchBaseDn() );
    }


    /**
     * @return the replayCache
     */
    public ReplayCache getReplayCache()
    {
        return replayCache;
    }


    /**
     * @throws IOException if we cannot bind to the sockets
     */
    public void start() throws IOException, LdapInvalidDnException
    {
        PrincipalStore store;

        store = new DirectoryPrincipalStore( getDirectoryService(), new Dn( this.getSearchBaseDn() ) );

        LOG.debug( "initializing the kerberos replay cache" );

        Cache< String, Object > cache = getDirectoryService().getCacheService().
            getCache( "kdcReplayCache", String.class, Object.class );
        replayCache = new ReplayCacheImpl( cache, config.getAllowableClockSkew() );

        // Kerberos can use UDP or TCP
        for ( Transport transport : transports )
        {
            IoAcceptor acceptor = transport.getAcceptor();

            // Now, configure the acceptor
            // Inject the chain
            IoFilterChainBuilder chainBuilder = new DefaultIoFilterChainBuilder();

            if ( transport instanceof TcpTransport )
            {
                // Now, configure the acceptor
                // Disable the disconnection of the clients on unbind
                acceptor.setCloseOnDeactivation( false );

                // No Nagle's algorithm
                ( ( NioSocketAcceptor ) acceptor ).getSessionConfig().setTcpNoDelay( true );

                // Allow the port to be reused even if the socket is in TIME_WAIT state
                ( ( NioSocketAcceptor ) acceptor ).setReuseAddress( true );
            }

            // Inject the codec
            ( ( DefaultIoFilterChainBuilder ) chainBuilder ).addFirst( "codec",
                new ProtocolCodecFilter(
                    KerberosProtocolCodecFactory.getInstance() ) );

            acceptor.setFilterChainBuilder( chainBuilder );

            // Inject the protocol handler
            acceptor.setHandler( new KerberosProtocolHandler( this, store ) );

            // Bind to the configured address
            acceptor.bind();
        }

        LOG.info( "Kerberos service started." );

        if ( changePwdServer != null )
        {
            changePwdServer.setSearchBaseDn( this.getSearchBaseDn() );
            changePwdServer.start();
        }
    }


    public void stop()
    {
        for ( Transport transport : getTransports() )
        {
            IoAcceptor acceptor = transport.getAcceptor();

            if ( acceptor != null )
            {
                acceptor.dispose();
            }
        }

        if ( replayCache != null )
        {
            replayCache.clear();
        }

        LOG.info( "Kerberos service stopped." );

        if ( changePwdServer != null )
        {
            changePwdServer.stop();
        }
    }


    /**
     * gets the port number on which TCP transport is running
     * @return the port number if TCP transport is enabled, -1 otherwise 
     */
    public int getTcpPort()
    {
        for ( Transport t : transports )
        {
            if ( t instanceof TcpTransport )
            {
                return t.getPort();
            }
        }

        return -1;
    }


    /**
     * @return the KDC server configuration
     */
    public KerberosConfig getConfig()
    {
        return config;
    }


    public ChangePasswordServer getChangePwdServer()
    {
        return changePwdServer;
    }


    public void setChangePwdServer( ChangePasswordServer changePwdServer )
    {
        this.changePwdServer = changePwdServer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "KDCServer[" ).append( getServiceName() ).append( "], listening on :" ).append( '\n' );

        if ( getTransports() != null )
        {
            for ( Transport transport : getTransports() )
            {
                sb.append( "    " ).append( transport ).append( '\n' );
            }
        }

        return sb.toString();
    }
}
