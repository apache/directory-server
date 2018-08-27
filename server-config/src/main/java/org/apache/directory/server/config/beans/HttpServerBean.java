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
package org.apache.directory.server.config.beans;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the HttpServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HttpServerBean extends ServerBean
{
    /** The configuration file */
    @ConfigurationElement(attributeType = "ads-httpConfFile", isOptional = true)
    private String httpConfFile;

    /** The list of supported web apps */
    @ConfigurationElement(objectClass = "ads-httpWebApp", container = "httpWebApps")
    private List<HttpWebAppBean> httpWebApps = new ArrayList<>();


    /**
     * Create a new HttpServerBean instance
     */
    public HttpServerBean()
    {
        super();

        // Enabled by default
        setEnabled( true );
    }


    /**
     * @return the httpConfFile
     */
    public String getHttpConfFile()
    {
        return httpConfFile;
    }


    /**
     * @param httpConfFile the httpConfFile to set
     */
    public void setHttpConfFile( String httpConfFile )
    {
        this.httpConfFile = httpConfFile;
    }


    /**
     * @return the httpWebApps
     */
    public List<HttpWebAppBean> getHttpWebApps()
    {
        return httpWebApps;
    }


    /**
     * @param httpWebApps the httpWebApps to set
     */
    public void setHttpWebApps( List<HttpWebAppBean> httpWebApps )
    {
        this.httpWebApps = httpWebApps;
    }


    /**
     * @param httpWebApps the httpWebApps to add
     */
    public void addHttpWebApps( HttpWebAppBean... httpWebApps )
    {
        for ( HttpWebAppBean httpWebApp : httpWebApps )
        {
            this.httpWebApps.add( httpWebApp );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "HttpServer :\n" );
        sb.append( super.toString( tabs + "  " ) );
        sb.append( toString( tabs, "  http configuration file", httpConfFile ) );

        if ( ( httpWebApps != null ) && !httpWebApps.isEmpty() )
        {
            sb.append( tabs ).append( "  web applications :\n" );

            for ( HttpWebAppBean httpWebApp : httpWebApps )
            {
                sb.append( httpWebApp.toString( tabs + "    " ) );
            }
        }

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }
}
