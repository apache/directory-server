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


import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.i18n.I18n;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to start the jetty http server
 * 
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HttpServer
{

    /** the jetty http server instance */
    private Server jetty;

    /** jetty config file */
    private String confFile;

    /** a collection to hold the configured web applications */
    private Set<WebApp> webApps;

    /** the default port to be used when no configuration file is provided */
    private int port = 8080;

    /** an internal flag to check the server configuration */
    private boolean configured = false;

    private static final Logger LOG = LoggerFactory.getLogger( HttpServer.class );


    public HttpServer()
    {
    }


    /**
     * starts the jetty http server
     * 
     * @throws Exception
     */
    public void start() throws Exception
    {

        if ( confFile == null && ( webApps == null || webApps.isEmpty() ) )
        {
            LOG.warn( "Neither configuration file nor web apps were configured for the http server, skipping initialization." );
            return;
        }

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

            Connector connector = new SelectChannelConnector();
            connector.setPort( port );
            jetty.setConnectors( new Connector[]{ connector } );

            List<Handler> handlers = new ArrayList<Handler>();
            for ( WebApp w : webApps )
            {
                WebAppContext webapp = new WebAppContext();
                webapp.setWar( w.getWarFile() );
                webapp.setContextPath( w.getContextPath() );
                handlers.add( webapp );
            }

            jetty.setHandlers( handlers.toArray( new Handler[ handlers.size() ] ) );
            
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


    public int getPort()
    {
        return port;
    }


    public void setPort( int port )
    {
        this.port = port;
    }

}
