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
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.directory.api.ldap.codec.api.ExtendedResponseDecorator;
import org.apache.directory.api.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;
import org.apache.directory.api.ldap.model.message.ExtendedResponseImpl;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
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
public class StartTlsHandler implements ExtendedOperationHandler<ExtendedRequest<ExtendedResponse>, ExtendedResponse>
{
    public static final String EXTENSION_OID = "1.3.6.1.4.1.1466.20037";

    private static final Set<String> EXTENSION_OIDS;
    private static final Logger LOG = LoggerFactory.getLogger( StartTlsHandler.class );

    private SSLContext sslContext;

    static
    {
        Set<String> set = new HashSet<String>( 3 );
        set.add( EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    public void handleExtendedOperation( LdapSession session, ExtendedRequest<ExtendedResponse> req ) throws Exception
    {
        LOG.info( "Handling StartTLS request." );

        IoFilterChain chain = session.getIoSession().getFilterChain();
        SslFilter sslFilter = ( SslFilter ) chain.get( "sslFilter" );
        if ( sslFilter == null )
        {
            sslFilter = new SslFilter( sslContext );
            chain.addFirst( "sslFilter", sslFilter );
        }
        else
        {
            sslFilter.startSsl( session.getIoSession() );
        }

        ExtendedResponseDecorator<ExtendedResponse> res = new ExtendedResponseDecorator<ExtendedResponse>( 
            LdapApiServiceFactory.getSingleton(), new ExtendedResponseImpl( req.getMessageId() ) );
        LdapResult result = res.getLdapResult();
        result.setResultCode( ResultCodeEnum.SUCCESS );
        res.setResponseName( EXTENSION_OID );
        res.setResponseValue( Strings.EMPTY_BYTES );

        // Send a response.
        session.getIoSession().setAttribute( SslFilter.DISABLE_ENCRYPTION_ONCE );
        session.getIoSession().write( res );
    }


    public final Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    public final String getOid()
    {
        return EXTENSION_OID;
    }


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
            sslContext.init( ldapServer.getKeyManagerFactory().getKeyManagers(), new TrustManager[]
                { new NoVerificationTrustManager() }, new SecureRandom() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_682 ), e );
        }
    }
}
