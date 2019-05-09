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

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;


/**
 * Factory to create a SSLContext providing a client certificate.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ClientCertificateSslSocketFactory extends SocketFactory
{
    private static SocketFactory customSSLSocketFactory;

    public static File ksFile = new File("target/clientkeystore.jks");;
    public static char[] ksPassword = "changeit".toCharArray();

    {
        try {
            KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );

            try( InputStream keyInput = new FileInputStream( ksFile.getAbsoluteFile() ) )
            {
                keyStore.load( keyInput, ksPassword );
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance( "SunX509" );
            keyManagerFactory.init( keyStore, ksPassword );

            SSLContext context = SSLContext.getInstance( "TLS" );
            context.init( keyManagerFactory.getKeyManagers(), BogusTrustManagerFactory.X509_MANAGERS, new SecureRandom() );

            customSSLSocketFactory = context.getSocketFactory();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Error initializing ClientCertificateSslSocketFactory ", e );
        }
    }


    /**
     * This method is needed. It is called by the LDAP Context to create the connection
     *
     * @see SocketFactory#getDefault()
     */
    @SuppressWarnings("unused")
    public static SocketFactory getDefault()
    {
        return new ClientCertificateSslSocketFactory();
    }

    /**
     * @see SocketFactory#createSocket(String, int)
     */
    public Socket createSocket(String arg0, int arg1) throws IOException
    {
        return customSSLSocketFactory.createSocket(arg0, arg1);
    }

    /**
     * @see SocketFactory#createSocket(InetAddress, int)
     */
    public Socket createSocket(InetAddress arg0, int arg1) throws IOException
    {
        return customSSLSocketFactory.createSocket(arg0, arg1);
    }

    /**
     * @see SocketFactory#createSocket(String, int, InetAddress, int)
     */
    public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException
    {
        return customSSLSocketFactory.createSocket(arg0, arg1, arg2, arg3);
    }

    /**
     * @see SocketFactory#createSocket(InetAddress, int, InetAddress, int)
     */
    public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
    {
        return customSSLSocketFactory.createSocket(arg0, arg1, arg2, arg3);
    }
}
