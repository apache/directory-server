/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server;


import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.directory.api.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of {@link X509TrustManager} which trusts the given certificates without verifying them.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NoVerificationTrustManager extends X509ExtendedTrustManager
{
    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger( NoVerificationTrustManager.class );
    
    
    /**
     * A public constructor
     */
    public NoVerificationTrustManager()
    {
        super();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted( X509Certificate[] x509Certificates, String authType, Socket socket )
        throws CertificateException 
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( I18n.msg( I18n.MSG_04168_CHECK_CLIENT_TRUSTED, x509Certificates[0] ) );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted( X509Certificate[] x509Certificates, String authType, SSLEngine engine )
        throws CertificateException 
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( I18n.msg( I18n.MSG_04168_CHECK_CLIENT_TRUSTED, x509Certificates[0] ) );
        }
    }
    
    
    public void checkServerTrusted( X509Certificate[] x509Certificates, String authType, Socket socket )
        throws CertificateException 
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( I18n.msg( I18n.MSG_04169_CHECK_SERVER_TRUSTED, x509Certificates[0] ) );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkServerTrusted( X509Certificate[] x509Certificates, String authType, SSLEngine engine )
        throws CertificateException 
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( I18n.msg( I18n.MSG_04169_CHECK_SERVER_TRUSTED, x509Certificates[0] ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted( X509Certificate[] x509Certificates, String s ) throws CertificateException
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( I18n.msg( I18n.MSG_04168_CHECK_CLIENT_TRUSTED, x509Certificates[0] ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void checkServerTrusted( X509Certificate[] x509Certificates, String s ) throws CertificateException
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( I18n.msg( I18n.MSG_04169_CHECK_SERVER_TRUSTED, x509Certificates[0] ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        return new X509Certificate[0];
    }
}
