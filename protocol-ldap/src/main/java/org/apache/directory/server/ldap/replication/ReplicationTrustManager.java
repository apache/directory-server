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

package org.apache.directory.server.ldap.replication;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jce.provider.X509CertParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A X509TrustManager implementation used by the replication subsystem.
 * It is seeded with the JVM default trust store (the 'javax.net.ssl.trustStore'
 * file, or the 'cacerts' file of the Java installation) so that certificates
 * signed by a trusted CA are accepted out of the box. Certificates of replica
 * peers can be added on top of that by parsing them with Bouncycastle's
 * X509CertParser and storing them in the in-memory KeyStore.
 * 
 * The SunX509 TrustManagerFactory is then initialized using this KeyStore and the
 * resulting X509TrustManager present in this factory's TrustManagers will be used
 * internally to perform the certificate verification 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ReplicationTrustManager implements X509TrustManager
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ReplicationTrustManager.class );

    /** the internal trust manager used for verifying the certificates */
    private static X509TrustManager trustManager = null;

    /** the in-memory keystore */
    private static KeyStore ks;

    /** the X509 certificate parser */
    private static X509CertParser parser = new X509CertParser();

    static
    {
        try
        {
            ks = KeyStore.getInstance( KeyStore.getDefaultType() );
        }
        catch ( Exception e )
        {
            LOG.error( "failed to initialize the keystore and X509 trustmanager", e );
            throw new RuntimeException( e );
        }
    }

    /** the singleton instance of this trust manager */
    private static final ReplicationTrustManager INSTANCE = new ReplicationTrustManager();

    /**
     * Creates a instance of ReplicationTrustManager
     */
    private ReplicationTrustManager()
    {
        try
        {
            loadDefaultTrustStore();

            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            tmFactory.init( ks );

            TrustManager[] trustManagers = tmFactory.getTrustManagers();

            for ( int i = 0; i < trustManagers.length; i++ )
            {
                if ( trustManagers[i] instanceof X509TrustManager )
                {
                    trustManager = ( X509TrustManager ) trustManagers[i];
                    LOG.debug( "found X509TrustManager {}", trustManager );
                    break;
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "failed to initialize the keystore and X509 trustmanager", e );
            throw new RuntimeException( e );
        }
    }


    /**
     * Initializes the in-memory keystore with the JVM default trust store:
     * either the file configured through the 'javax.net.ssl.trustStore' system
     * property, or the 'cacerts' file of the Java installation. If none of
     * them can be loaded, the keystore is left empty and certificates can
     * still be added via {@link #addCertificate(String, byte[])}.
     */
    private void loadDefaultTrustStore()
    {
        String trustStoreFile = System.getProperty( "javax.net.ssl.trustStore" );
        char[] trustStorePassword = null;

        String trustStorePasswordValue = System.getProperty( "javax.net.ssl.trustStorePassword" );

        if ( trustStorePasswordValue != null )
        {
            trustStorePassword = trustStorePasswordValue.toCharArray();
        }

        try
        {
            if ( trustStoreFile != null )
            {
                try ( InputStream in = new FileInputStream( trustStoreFile ) )
                {
                    ks.load( in, trustStorePassword );
                }
            }
            else
            {
                File cacertsFile = new File( System.getProperty( "java.home" ),
                    "lib" + File.separator + "security" + File.separator + "cacerts" );

                if ( cacertsFile.exists() )
                {
                    try ( InputStream in = new FileInputStream( cacertsFile ) )
                    {
                        ks.load( in, trustStorePassword );
                    }
                }
                else
                {
                    ks.load( null, null );
                }
            }
        }
        catch ( Exception e )
        {
            LOG.warn( "failed to load the default trust store, continuing with an empty keystore", e );

            try
            {
                ks = KeyStore.getInstance( KeyStore.getDefaultType() );
                ks.load( null, null );
            }
            catch ( Exception ke )
            {
                // cannot happen with a null stream and null password
                LOG.error( "failed to initialize an empty keystore", ke );
            }
        }
    }


    /**
     * loads the given map of [alias-name, certificate-data] entries into the keystore
     * to be used by the trust manager
     *
     * @param aliasCertMap the map of [alias-name, certificate-data] entries
     * @throws Exception in case of any issues related to certificate data parsing
     */
    public static void addCertificates( Map<String, byte[]> aliasCertMap ) throws Exception
    {
        for ( Map.Entry<String, byte[]> entry : aliasCertMap.entrySet() )
        {
            addCertificate( entry.getKey(), entry.getValue() );
        }
    }


    /**
     * stores the given certificate into the keystore with the given alias name
     * 
     * @param certAlias the alias name to be used for this certificate
     * @param certificate the X509 certificate data
     * @throws Exception in case of any issues related to certificate data parsing
     */
    public static void addCertificate( String certAlias, byte[] certificate ) throws Exception
    {
        try
        {
            parser.engineInit( new ByteArrayInputStream( certificate ) );

            X509Certificate cert = ( X509Certificate ) parser.engineRead();

            ks.setCertificateEntry( certAlias, cert );
        }
        catch ( Exception ex )
        {
            LOG.warn( "failed to load the certificate associated with the alias {}", certAlias, ex );
            throw ex;
        }
    }


    /**
     * returns the singleton instance of ReplicationTrustManager, note that this
     * return instance can only be used after calling the {@link #addCertificates(Map)} method 
     * 
     * @return the instance of the ReplicationTrustManager
     */
    public static ReplicationTrustManager getInstance()
    {
        return INSTANCE;
    }


    /**
     * {@inheritDoc}
     */
    public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException
    {
        trustManager.checkClientTrusted( chain, authType );
    }


    /**
     * {@inheritDoc}
     */
    public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException
    {
        trustManager.checkServerTrusted( chain, authType );
    }


    /**
     * {@inheritDoc}
     */
    public X509Certificate[] getAcceptedIssuers()
    {
        return trustManager.getAcceptedIssuers();
    }
}
