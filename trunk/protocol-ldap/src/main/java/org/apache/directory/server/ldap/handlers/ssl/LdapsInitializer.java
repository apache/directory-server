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


import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.util.Strings;
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
public class LdapsInitializer
{
    public static IoFilterChainBuilder init( KeyStore ks, String certificatePassord ) throws LdapException
    {
        SSLContext sslCtx;
        try
        {
            // Set up key manager factory to use our key store
            String algorithm = Security.getProperty( "ssl.KeyManagerFactory.algorithm" );

            if ( algorithm == null )
            {
                algorithm = KeyManagerFactory.getDefaultAlgorithm();
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance( algorithm );

            if ( Strings.isEmpty( certificatePassord ) )
            {
                kmf.init( ks, null );
            }
            else
            {
                kmf.init( ks, certificatePassord.toCharArray() );
            }

            // Initialize the SSLContext to work with our key managers.
            sslCtx = SSLContext.getInstance( "TLS" );
            sslCtx.init( kmf.getKeyManagers(), new TrustManager[]
                { new ServerX509TrustManager() }, new SecureRandom() );
        }
        catch ( Exception e )
        {
            throw new LdapException( I18n.err( I18n.ERR_683 ), e );
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        chain.addLast( "sslFilter", new SslFilter( sslCtx ) );
        return chain;
    }
}
