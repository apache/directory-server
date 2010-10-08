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


import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.activemq.util.ByteArrayInputStream;
import org.bouncycastle.jce.provider.X509CertParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO ReplicationTrustManager.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicationTrustManager implements X509TrustManager
{
    private static final Logger LOG = LoggerFactory.getLogger( ReplicationTrustManager.class );

    /** the internal trust manager used for verifying the certificates */
    private static X509TrustManager trustManager = null;

    /** the in-memory keystore in JKS format */
    private static KeyStore ks;

    /** flag used for marking the intialization phase status */
    private static boolean initialized;

    /** the singleton instance of this trust manager */
    private static ReplicationTrustManager INSTANCE = new ReplicationTrustManager();


    private ReplicationTrustManager()
    {
        try
        {
            ks = KeyStore.getInstance( "JKS" );
            ks.load( null, null ); // initiate with null stream and password, this keystore resides in-memory only
        }
        catch ( Exception e )
        {
            LOG.error( "failed to initiate the keystore", e );
            throw new RuntimeException( e );
        }
    }


    /**
     * loads the given map of [alias-name, certificate-data] entries into the keystore
     * to be used by the trust manager
     *
     * @param aliasCertMap the map of [alias-name, certificate-data] entries
     * @throws Exception in case of any issues related to certificate data parsing or finding SunX509 TrustManagerFactory implementation
     */
    public static void init( Map<String, byte[]> aliasCertMap ) throws Exception
    {
        if ( initialized )
        {
            LOG.warn( "ReplicationTrustManager was already initialized, ignoring call to init" );
            return;
        }

        X509CertParser parser = new X509CertParser();

        for ( Map.Entry<String, byte[]> entry : aliasCertMap.entrySet() )
        {
            try
            {
                parser.engineInit( new ByteArrayInputStream( entry.getValue() ) );

                X509Certificate cert = ( X509Certificate ) parser.engineRead();

                ks.setCertificateEntry( entry.getKey(), cert );
            }
            catch ( Exception ex )
            {
                LOG.warn( "failed to load the certificate associated with the alias {}", entry.getKey(), ex );
            }
        }

        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance( "SunX509" );
        tmFactory.init( ks );

        TrustManager trustManagers[] = tmFactory.getTrustManagers();

        for ( int i = 0; i < trustManagers.length; i++ )
        {
            if ( trustManagers[i] instanceof X509TrustManager )
            {
                trustManager = ( X509TrustManager ) trustManagers[i];
                LOG.debug( "found X509TrustManager {}", trustManager );
                break;
            }
        }

        if ( trustManager == null )
        {
            throw new Exception( "no X509TrustManagerS were found" );
        }

        initialized = true;
    }


    /**
     * returns the singleton instance of ReplicationTrustManager, note that this
     * return instance can only be used after calling the {@link #init(Map)} method 
     * 
     * @return the instance of the ReplicationTrustManager
     */
    public static ReplicationTrustManager getInstance()
    {
        return INSTANCE;
    }


    public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException
    {
        trustManager.checkClientTrusted( chain, authType );
    }


    public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException
    {
        trustManager.checkServerTrusted( chain, authType );
    }


    public X509Certificate[] getAcceptedIssuers()
    {
        return trustManager.getAcceptedIssuers();
    }
}
