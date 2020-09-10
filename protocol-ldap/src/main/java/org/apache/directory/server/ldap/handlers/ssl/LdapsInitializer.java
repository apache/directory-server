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
package org.apache.directory.server.ldap.handlers.ssl;


import java.security.SecureRandom;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.filter.ssl.SslFilter;


/**
 * Loads the certificate file for LDAPS support and creates the appropriate
 * MINA filter chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public final class LdapsInitializer
{
    private LdapsInitializer()
    {
    }


    /**
     * Initialize the LDAPS server.
     *
     * @param ldapServer The LDAP server instance
     * @param transport The TCP transport that contains the SSL configuration
     * @return A IoFilter chain
     * @throws LdapException If we had a pb
     */
    public static IoFilterChainBuilder init( LdapServer ldapServer, TcpTransport transport ) throws LdapException
    {
        SSLContext sslCtx;

        try
        {
            // Initialize the SSLContext to work with our key managers.
            sslCtx = SSLContext.getInstance( "TLS" );
            sslCtx.init( ldapServer.getKeyManagerFactory().getKeyManagers(),
                    ldapServer.getTrustManagers(), new SecureRandom() );
        }
        catch ( Exception e )
        {
            throw new LdapException( I18n.err( I18n.ERR_683 ), e );
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        SslFilter sslFilter = new SslFilter( sslCtx );

        // The ciphers
        List<String> cipherSuites = transport.getCipherSuite();

        if ( ( cipherSuites != null ) && !cipherSuites.isEmpty() )
        {
            sslFilter.setEnabledCipherSuites( cipherSuites.toArray( new String[cipherSuites.size()] ) );
        }

        // The protocols
        List<String> enabledProtocols = transport.getEnabledProtocols();

        if ( ( enabledProtocols != null ) && !enabledProtocols.isEmpty() )
        {
            sslFilter.setEnabledProtocols( enabledProtocols.toArray( new String[enabledProtocols.size()] ) );
        }
        else
        {
            // Be sure we disable SSLV3
            sslFilter.setEnabledProtocols( new String[]
                { "TLSv1", "TLSv1.1", "TLSv1.2" } );
        }

        // The remaining SSL parameters
        sslFilter.setNeedClientAuth( transport.isNeedClientAuth() );
        sslFilter.setWantClientAuth( transport.isWantClientAuth() );

        chain.addLast( "sslFilter", sslFilter );

        return chain;
    }
}
