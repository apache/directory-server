/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.ldap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.ldap.LdapProtocolProvider;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.mina.common.IoAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapServer
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( LdapServer.class );

    private LdapConfig config;
    private IoAcceptor acceptor;
    private LdapProtocolProvider provider;

    public LdapServer( LdapConfig config, IoAcceptor acceptor, Hashtable env )
    {
        this.config = config;
        this.acceptor = acceptor;

        String name = config.getName();
        int port = config.getPort();

        try
        {
            provider = new LdapProtocolProvider( new StartupConfiguration(), (Hashtable) env.clone() );

            acceptor.bind( new InetSocketAddress( port ), provider.getHandler() );

            log.debug( "{} listening on port {}", name, new Integer( port ) );
        }
        catch ( LdapNamingException lne )
        {
            log.error( lne.getMessage(), lne );
        }
        catch ( IOException ioe )
        {
            log.error( ioe.getMessage(), ioe );
        }
    }

    public boolean isDifferent( Dictionary newConfig )
    {
        return config.isDifferent( newConfig );
    }

    public void destroy()
    {
        acceptor.unbind( new InetSocketAddress( config.getPort() ) );

        acceptor = null;
        provider = null;

        log.debug( "{} has stopped listening on port {}", config.getName(), new Integer( config.getPort() ) );
    }
}
