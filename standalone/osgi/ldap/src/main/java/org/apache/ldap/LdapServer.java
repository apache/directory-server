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
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.server.protocol.LdapProtocolProvider;
import org.apache.mina.common.TransportType;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapServer
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( LdapServer.class );

    private ServiceRegistry registry;
    private LdapProtocolProvider provider;
    private Service tcpService;

    private LdapConfig config;

    private String name;
    private int port = -1;

    public LdapServer( LdapConfig config, ServiceRegistry registry, Hashtable env )
    {
        this.config = config;
        this.registry = registry;

        port = config.getPort();
        name = config.getName();

        try
        {
            log.debug( name + " starting on " + port );

            provider = new LdapProtocolProvider( (Hashtable) env.clone() );

            tcpService = new Service( name, TransportType.SOCKET, port );

            registry.bind( tcpService, provider.getHandler() );

            log.debug( name + " listening on port " + port );
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
        registry.unbind( tcpService );

        registry = null;
        provider = null;
        tcpService = null;

        log.debug( name + " has stopped listening on port " + port );
    }
}
