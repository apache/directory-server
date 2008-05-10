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

import javax.naming.NamingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.filter.SSLFilter;


/**
 * Loads the certificate file for LDAPS support and creates the appropriate
 * MINA filter chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 *
 */
public class LdapsInitializer
{
    public static IoFilterChainBuilder init( KeyStore ks ) throws NamingException
    {
        SSLContext sslCtx;
        try
        {
            // Set up key manager factory to use our key store
            String algorithm = Security.getProperty( "ssl.KeyManagerFactory.algorithm" );
            if ( algorithm == null )
            {
                algorithm = "SunX509";
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance( algorithm );
            kmf.init( ks, null );

            // Initialize the SSLContext to work with our key managers.
            sslCtx = SSLContext.getInstance( "TLS" );
            sslCtx.init( kmf.getKeyManagers(), new TrustManager[]
                { new ServerX509TrustManager() }, new SecureRandom() );
        }
        catch ( Exception e )
        {
            throw ( NamingException ) new NamingException( "Failed to create a SSL context." ).initCause( e );
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        chain.addLast( "SSL", new SSLFilter( sslCtx ) );
        return chain;
    }
}
