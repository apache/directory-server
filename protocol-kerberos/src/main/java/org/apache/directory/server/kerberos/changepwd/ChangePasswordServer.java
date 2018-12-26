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
package org.apache.directory.server.kerberos.changepwd;


import java.io.IOException;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.kerberos.ChangePasswordConfig;
import org.apache.directory.server.kerberos.changepwd.protocol.ChangePasswordProtocolHandler;
import org.apache.directory.server.kerberos.kdc.DirectoryPrincipalStore;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.replay.ReplayCacheImpl;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Contains the configuration parameters for the Change Password protocol provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordServer extends DirectoryBackedService
{
    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ChangePasswordServer.class );

    /** The default change password port. */
    private static final int DEFAULT_IP_PORT = 464;

    /** The default change password password policy for password length. */
    public static final int DEFAULT_PASSWORD_LENGTH = 6;

    /** The default change password password policy for category count. */
    public static final int DEFAULT_CATEGORY_COUNT = 3;

    /** The default change password password policy for token size. */
    public static final int DEFAULT_TOKEN_SIZE = 3;

    private ChangePasswordConfig config;

    /** the cache used for storing change password requests */
    private ReplayCache replayCache;


    /**
     * Creates a new instance of ChangePasswordConfiguration.
     */
    public ChangePasswordServer()
    {
        this( new ChangePasswordConfig() );
    }


    public ChangePasswordServer( ChangePasswordConfig config )
    {
        this.config = config;
    }


    /**
     * @throws IOException if we cannot bind to the specified ports
     */
    public void start() throws IOException, LdapInvalidDnException
    {
        PrincipalStore store = new DirectoryPrincipalStore( getDirectoryService(), new Dn( this.getSearchBaseDn() ) );

        LOG.debug( "initializing the changepassword replay cache" );

        Cache< String, Object > cache = getDirectoryService().getCacheService().getCache( "changePwdReplayCache", String.class, Object.class );
        replayCache = new ReplayCacheImpl( cache );

        for ( Transport transport : transports )
        {
            IoAcceptor acceptor = transport.getAcceptor();

            // Disable the disconnection of the clients on unbind
            acceptor.setCloseOnDeactivation( false );

            if ( transport instanceof UdpTransport )
            {
                // Allow the port to be reused even if the socket is in TIME_WAIT state
                ( ( DatagramSessionConfig ) acceptor.getSessionConfig() ).setReuseAddress( true );
            }
            else
            {
                // Allow the port to be reused even if the socket is in TIME_WAIT state
                ( ( SocketAcceptor ) acceptor ).setReuseAddress( true );

                // No Nagle's algorithm
                ( ( SocketAcceptor ) acceptor ).getSessionConfig().setTcpNoDelay( true );
            }

            // Set the handler
            acceptor.setHandler( new ChangePasswordProtocolHandler( this, store ) );

            // Bind
            acceptor.bind();
        }

        LOG.info( "ChangePassword service started." );
        //System.out.println( "ChangePassword service started." );
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

        replayCache.clear();

        LOG.info( "ChangePassword service stopped." );
        //System.out.println( "ChangePassword service stopped." );
    }


    /**
     * @return the replayCache
     */
    public ReplayCache getReplayCache()
    {
        return replayCache;
    }


    public ChangePasswordConfig getConfig()
    {
        return config;
    }


    public int getTcpPort()
    {
        for ( Transport t : getTransports() )
        {
            if ( t instanceof TcpTransport )
            {
                return t.getPort();
            }
        }

        throw new IllegalStateException( "TCP transport is not enabled" );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ChangePasswordServer[" ).append( getServiceName() ).append( "], listening on :" ).append( '\n' );

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
