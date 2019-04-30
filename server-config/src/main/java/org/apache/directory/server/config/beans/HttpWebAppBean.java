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


import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the HttpWebApp configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HttpWebAppBean extends AdsBaseBean
{
    /** The server identifier */
    @ConfigurationElement(attributeType = "ads-id", isRdn = true)
    private String id;

    /** The context path */
    @ConfigurationElement(attributeType = "ads-httpAppCtxPath")
    private String httpAppCtxPath;

    /** The war file */
    @ConfigurationElement(attributeType = "ads-httpWarFile")
    private String httpWarFile;


    /**
     * Create a new HttpWebAppBean instance
     */
    public HttpWebAppBean()
    {
        super();

        // Enabled by default
        setEnabled( true );
    }


    /**
     * @return the id
     */
    public String getId()
    {
        return id;
    }


    /**
     * @param id the id to set
     */
    public void setId( String id )
    {
        this.id = id;
    }


    /**
     * @return the httpAppCtxPath
     */
    public String getHttpAppCtxPath()
    {
        return httpAppCtxPath;
    }


    /**
     * @param httpAppCtxPath the httpAppCtxPath to set
     */
    public void setHttpAppCtxPath( String httpAppCtxPath )
    {
        this.httpAppCtxPath = httpAppCtxPath;
    }


    /**
     * @return the httpWarFile
     */
    public String getHttpWarFile()
    {
        return httpWarFile;
    }


    /**
     * @param httpWarFile the httpWarFile to set
     */
    public void setHttpWarFile( String httpWarFile )
    {
        this.httpWarFile = httpWarFile;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "HttpWebApp :\n" );
        sb.append( super.toString( tabs + "  " ) );
        sb.append( tabs ).append( "  id : " ).append( id ).append( '\n' );
        sb.append( tabs ).append( "  war file : " ).append( httpWarFile ).append( '\n' );
        sb.append( toString( tabs, "  application context path", httpAppCtxPath ) );

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
