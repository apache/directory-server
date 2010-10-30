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


import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.HttpDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.i18n.I18n;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.xml.XmlConfiguration;
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

    /** the default port to be used when no configuration file is provided */
    private int port = 8080;

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
            for( Handler h : handlers )
            {
                if( h instanceof ContextHandler )
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
                for( File app : appList )
                {
                    WebAppContext webapp = new WebAppContext();
                    webapp.setWar( app.getAbsolutePath() );
                    String ctxName = app.getName();
                    int pos = ctxName.indexOf( '.' );
                    if( pos > 0 )
                    {
                        ctxName = ctxName.substring( 0, pos );
                    }
                    
                    webapp.setContextPath( "/" + ctxName );
                    handlers.add( webapp );
                    
                    webapp.setParentLoaderPriority( true );
                }
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
