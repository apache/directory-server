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

package org.apache.directory.server.integration.http;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.bridge.http.HttpDirectoryService;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.security.TlsKeyGenerator;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.bouncycastle.jce.provider.X509CertParser;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to start the jetty http server
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HttpServer
{

    /** the jetty http server instance */
    private Server jetty;

    /** jetty config file */
    private String confFile;

    /** a collection to hold the configured web applications */
    private Set<WebApp> webApps;

    /** Transport for http */
    private TcpTransport httpTransport = null;

    /** Transport for https */
    private TcpTransport httpsTransport = null;

    /** protocol identifier for http */
    public static final String HTTP_TRANSPORT_ID = "http";

    /** protocol identifier for https */
    public static final String HTTPS_TRANSPORT_ID = "https";

    /** an internal flag to check the server configuration */
    private boolean configured = false;

    private static final Logger LOG = LoggerFactory.getLogger( HttpServer.class );

    private DirectoryService dirService;


    public HttpServer()
    {
    }


    /**
     * starts the jetty http server
     * 
     * @throws Exception
     */
    public void start( DirectoryService dirService ) throws Exception
    {
        this.dirService = dirService;

        XmlConfiguration jettyConf = null;

        if ( confFile != null )
        {
            jettyConf = new XmlConfiguration( new FileInputStream( confFile ) );

            LOG.info( "configuring jetty http server from the configuration file {}", confFile );

            try
            {
                jetty = new Server();
                jettyConf.configure( jetty );
                configured = true;
            }
            catch ( Exception e )
            {
                LOG.error( I18n.err( I18n.ERR_120 ) );
                throw e;
            }
        }
        else
        {
            LOG.info( "No configuration file set, looking for web apps" );
            configureServerThroughCode();
        }

        if ( configured )
        {
            Handler[] handlers = jetty.getHandlers();

            for ( Handler h : handlers )
            {
                if ( h instanceof ContextHandler )
                {
                    ContextHandler ch = ( ContextHandler ) h;
                    ch.setAttribute( HttpDirectoryService.KEY, new HttpDirectoryService( dirService ) );
                }
            }

            LOG.info( "starting jetty http server" );
            jetty.start();
        }
        else
        {
            jetty = null;
            LOG.warn( "Error while configuring the http server, skipping the http server startup" );
        }
    }


    /*
     * configure the jetty server programmatically without using any configuration file 
     */
    private void configureServerThroughCode()
    {
        try
        {
            jetty = new Server();

            if ( httpTransport != null )
            {
                ServerConnector httpConnector = new ServerConnector( jetty );
                httpConnector.setPort( httpTransport.getPort() );
                httpConnector.setHost( httpTransport.getAddress() );
                jetty.addConnector( httpConnector );
            }

            if ( httpsTransport != null )
            {
                // load the admin entry to get the private key and certificate
                Dn adminDn = dirService.getDnFactory().create( ServerDNConstants.ADMIN_SYSTEM_DN );
                Entry adminEntry = dirService.getAdminSession().lookup( adminDn, SchemaConstants.ALL_USER_ATTRIBUTES,
                    SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES );

                File confDir = dirService.getInstanceLayout().getConfDirectory();
                File ksFile = new File( confDir, "httpserver.generated.ks" );

                String password = UUID.randomUUID().toString();

                KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
                ks.load( null, null );

                X509CertParser parser = new X509CertParser();

                parser.engineInit( new ByteArrayInputStream( adminEntry.get( TlsKeyGenerator.USER_CERTIFICATE_AT )
                    .getBytes() ) );

                X509Certificate cert = ( X509Certificate ) parser.engineRead();

                ks.setCertificateEntry( "cert", cert );

                KeyPair keyPair = TlsKeyGenerator.getKeyPair( adminEntry );
                ks.setKeyEntry( "privatekey", keyPair.getPrivate(), password.toCharArray(), new Certificate[]
                    { cert } );

                try ( OutputStream stream = new FileOutputStream( ksFile ) )
                {
                    ks.store( stream, password.toCharArray() );
                }

                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStoreType( "JKS" );
                sslContextFactory.setKeyStorePath( ksFile.getAbsolutePath() );
                sslContextFactory.setKeyStorePassword( password );
                sslContextFactory.setKeyManagerPassword( password );

                HttpConfiguration httpsConfiguration = new HttpConfiguration();
                httpsConfiguration.setSecureScheme( "https" );
                httpsConfiguration.setSecurePort( httpsTransport.getPort() );
                httpsConfiguration.addCustomizer( new SecureRequestCustomizer() );

                ServerConnector httpsConnector = new ServerConnector( jetty, new SslConnectionFactory( sslContextFactory, "http/1.1" ), new HttpConnectionFactory( httpsConfiguration ) );
                httpsConnector.setPort( httpsTransport.getPort() );
                httpsConnector.setHost( httpsTransport.getAddress() );

                jetty.addConnector( httpsConnector );
            }

            HandlerList handlers = new HandlerList();
            for ( WebApp w : webApps )
            {
                WebAppContext webapp = new WebAppContext();
                webapp.setWar( w.getWarFile() );
                webapp.setContextPath( w.getContextPath() );
                handlers.addHandler( webapp );

                webapp.setParentLoaderPriority( true );
            }

            // add web apps from the webapps directory inside directory service's working directory
            // the exploded or archived wars
            File webAppDir = new File( dirService.getInstanceLayout().getInstanceDirectory(), "webapps" );

            FilenameFilter webAppFilter = new FilenameFilter()
            {

                public boolean accept( File dir, String name )
                {
                    return name.endsWith( ".war" );
                }
            };

            if ( webAppDir.exists() )
            {
                File[] appList = webAppDir.listFiles( webAppFilter );
                for ( File app : appList )
                {
                    WebAppContext webapp = new WebAppContext();
                    webapp.setWar( app.getAbsolutePath() );
                    String ctxName = app.getName();
                    int pos = ctxName.indexOf( '.' );
                    if ( pos > 0 )
                    {
                        ctxName = ctxName.substring( 0, pos );
                    }

                    webapp.setContextPath( "/" + ctxName );
                    handlers.addHandler( webapp );

                    webapp.setParentLoaderPriority( true );
                }
            }

            jetty.setHandler( handlers );

            configured = true;
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_121 ), e );
        }

    }


    /**
     * stops the jetty http server
     * 
     * @throws Exception
     */
    public void stop() throws Exception
    {
        if ( jetty != null && jetty.isStarted() )
        {
            LOG.info( "stopping jetty http server" );
            jetty.stop();
        }
    }


    public void setConfFile( String confFile )
    {
        this.confFile = confFile;
    }


    public Set<WebApp> getWebApps()
    {
        return webApps;
    }


    public void setWebApps( Set<WebApp> webapps )
    {
        this.webApps = webapps;
    }


    public TcpTransport getHttpTransport()
    {
        return httpTransport;
    }


    public void setHttpTransport( TcpTransport httpTransport )
    {
        this.httpTransport = httpTransport;
    }


    public TcpTransport getHttpsTransport()
    {
        return httpsTransport;
    }


    public void setHttpsTransport( TcpTransport httpsTransport )
    {
        this.httpsTransport = httpsTransport;
    }

}
