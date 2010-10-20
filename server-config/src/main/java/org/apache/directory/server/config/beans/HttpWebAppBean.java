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


/**
 * A class used to store the HttpWebApp configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HttpWebAppBean extends AdsBaseBean
{
    /** The server identifier */
    private String id;
    
    /** The context path */
    private String httpappctxpath;
    
    /** The war file */
    private String httpwarfile;

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
        return httpappctxpath;
    }

    
    /**
     * @param httpAppCtxPath the httpAppCtxPath to set
     */
    public void setHttpAppCtxPath( String httpAppCtxPath )
    {
        this.httpappctxpath = httpAppCtxPath;
    }

    
    /**
     * @return the httpWarFile
     */
    public String getHttpWarFile()
    {
        return httpwarfile;
    }

    
    /**
     * @param httpWarFile the httpWarFile to set
     */
    public void setHttpWarFile( String httpWarFile )
    {
        this.httpwarfile = httpWarFile;
    }
}
