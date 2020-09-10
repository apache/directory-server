/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap.handlers.extended;


import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.directory.api.ldap.extras.extended.startTls.StartTlsRequest;
import org.apache.directory.api.ldap.extras.extended.startTls.StartTlsResponse;
import org.apache.directory.api.ldap.extras.extended.startTls.StartTlsResponseImpl;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.filter.ssl.SslFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handler for the StartTLS extended operation.
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2830.txt">RFC 2830</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StartTlsHandler implements ExtendedOperationHandler<ExtendedRequest, ExtendedResponse>
{
    public static final String EXTENSION_OID = StartTlsRequest.EXTENSION_OID;

    private static final Set<String> EXTENSION_OIDS;
    private static final Logger LOG = LoggerFactory.getLogger( StartTlsHandler.class );

    /** The SSL Context instance */
    private SSLContext sslContext;

    /** The list of enabled ciphers */
    private List<String> cipherSuite;

    /** The list of enabled protocols */
    private List<String> enabledProtocols;

    /** The 'needClientAuth' SSL flag */
    private boolean needClientAuth;

    /** The 'wantClientAuth' SSL flag */
    private boolean wantClientAuth;

    static
    {
        Set<String> set = new HashSet<>( 3 );
        set.add( EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    /**
     * {@inheritDoc}
     */
    public void handleExtendedOperation( LdapSession session, ExtendedRequest req ) throws Exception
    {
        LOG.info( "Handling StartTLS request." );

        IoFilterChain chain = session.getIoSession().getFilterChain();
        SslFilter sslFilter = ( SslFilter ) chain.get( "sslFilter" );

        if ( sslFilter == null )
        {
            sslFilter = new SslFilter( sslContext, false );

            // Set the cipher suite
            if ( ( cipherSuite != null ) && !cipherSuite.isEmpty() )
            {
                sslFilter.setEnabledCipherSuites( cipherSuite.toArray( new String[cipherSuite.size()] ) );
            }

            // Set the enabled protocols, default to no SSLV3
            if ( ( enabledProtocols != null ) && !enabledProtocols.isEmpty() )
            {
                sslFilter.setEnabledProtocols( enabledProtocols.toArray( new String[enabledProtocols.size()] ) );
            }
            else
            {
                // default to TLS only
                sslFilter.setEnabledProtocols( new String[]{ "TLSv1", "TLSv1.1", "TLSv1.2" } );
            }

            // Set the remaining SSL flags
            sslFilter.setNeedClientAuth( needClientAuth );
            sslFilter.setWantClientAuth( wantClientAuth );

            chain.addFirst( "sslFilter", sslFilter );
        }
        else
        {
            // Be sure we disable SSLV3
            sslFilter.setEnabledProtocols( new String[]
                { "TLSv1", "TLSv1.1", "TLSv1.2" } );
            sslFilter.startSsl( session.getIoSession() );
        }

        StartTlsResponse res = new StartTlsResponseImpl( req.getMessageId() );
        LdapResult result = res.getLdapResult();
        result.setResultCode( ResultCodeEnum.SUCCESS );
        res.setResponseName( EXTENSION_OID );

        // Send a response.
        session.getIoSession().setAttribute( SslFilter.DISABLE_ENCRYPTION_ONCE );
        session.getIoSession().write( res );
    }


    /**
     * {@inheritDoc}
     */
    public final Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    /**
     * {@inheritDoc}
     */
    public final String getOid()
    {
        return EXTENSION_OID;
    }


    /**
     * {@inheritDoc}
     */
    public void setLdapServer( LdapServer ldapServer )
    {
        LOG.debug( "Setting LDAP Service" );
        Provider provider = Security.getProvider( "SUN" );
        LOG.debug( "provider = {}", provider );

        try
        {
            sslContext = SSLContext.getInstance( "TLS" );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_681 ), e );
        }

        try
        {
            sslContext.init( ldapServer.getKeyManagerFactory().getKeyManagers(),
                    ldapServer.getTrustManagers(), new SecureRandom() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_682 ), e );
        }

        // Get the transport
        Transport[] transports = ldapServer.getTransports();

        // Check for any SSL parameter
        for ( Transport transport : transports )
        {
            if ( transport instanceof TcpTransport )
            {
                TcpTransport tcpTransport = ( TcpTransport ) transport;

                cipherSuite = tcpTransport.getCipherSuite();
                enabledProtocols = tcpTransport.getEnabledProtocols();
                needClientAuth = tcpTransport.isNeedClientAuth();
                wantClientAuth = tcpTransport.isWantClientAuth();

                break;
            }
        }
    }
}
