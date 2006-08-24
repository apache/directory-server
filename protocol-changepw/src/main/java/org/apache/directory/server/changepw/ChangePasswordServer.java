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

package org.apache.directory.server.changepw;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Dictionary;

import org.apache.directory.server.changepw.protocol.ChangePasswordProtocolHandler;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A wrapper encapsulating configuration, a MINA registry, and a PrincipalStore
 * to implement a complete Change Password server. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePasswordServer
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( ChangePasswordServer.class );

    private ChangePasswordConfiguration config;
    private IoAcceptor acceptor;
    private PrincipalStore store;

    private IoHandler handler;

    public ChangePasswordServer(ChangePasswordConfiguration config, IoAcceptor acceptor, PrincipalStore store)
    {
        this.config = config;
        this.acceptor = acceptor;
        this.store = store;

        String name = config.getName();
        int port = config.getPort();

        try
        {
            handler = new ChangePasswordProtocolHandler( config, this.store );

            acceptor.bind( new InetSocketAddress( port ), handler );

            log.debug( "{} listening on port {}", name, new Integer( port ) );
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
        handler = null;

        log.debug( "{} has stopped listening on port {}", config.getName(), new Integer( config.getPort() ) );
    }
}
