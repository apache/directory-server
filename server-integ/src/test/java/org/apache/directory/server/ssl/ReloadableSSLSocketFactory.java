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

package org.apache.directory.server.ssl;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


/**
 * A {@link SSLSocketFactory} that initializes the {@link TrustManager} each time it is used. 
 * The standard factory only loads the underlying key store and trust store files once, 
 * changes to the files are not reflected while the JVM is running. This implementation 
 * initializes the trust manager factory each time it is used.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReloadableSSLSocketFactory extends SSLSocketFactory
{

    private SSLSocketFactory delegate;


    public static SSLSocketFactory getDefault()
    {
        return new ReloadableSSLSocketFactory();
    }


    public ReloadableSSLSocketFactory()
    {
        try
        {
            // always load default trust managers
            TrustManagerFactory factory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            factory.init( ( KeyStore ) null );
            TrustManager[] trustManagers = factory.getTrustManagers();

            // create the real socket factory
            // TLS is not secure
            SSLContext sc = SSLContext.getInstance( "TLS" ); //$NON-NLS-1$
            sc.init( null, trustManagers, null );
            delegate = sc.getSocketFactory();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }


    private SSLSocketFactory getDelegate()
    {
        return delegate;
    }


    /**
     * {@inheritDoc}
     */
    public String[] getDefaultCipherSuites()
    {
        return getDelegate().getDefaultCipherSuites();
    }


    /**
     * {@inheritDoc}
     */
    public String[] getSupportedCipherSuites()
    {
        return getDelegate().getSupportedCipherSuites();
    }


    /**
     * {@inheritDoc}
     */
    public Socket createSocket( Socket s, String host, int port, boolean autoClose ) throws IOException
    {
        try
        {
            return getDelegate().createSocket( s, host, port, autoClose );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Socket createSocket( String host, int port ) throws IOException, UnknownHostException
    {
        try
        {
            return getDelegate().createSocket( host, port );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Socket createSocket( InetAddress host, int port ) throws IOException
    {
        try
        {
            return getDelegate().createSocket( host, port );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Socket createSocket( String host, int port, InetAddress localHost, int localPort ) throws IOException,
        UnknownHostException
    {
        try
        {
            return getDelegate().createSocket( host, port, localHost, localPort );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Socket createSocket( InetAddress address, int port, InetAddress localAddress, int localPort )
        throws IOException
    {
        try
        {
            return getDelegate().createSocket( address, port, localAddress, localPort );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }

}
