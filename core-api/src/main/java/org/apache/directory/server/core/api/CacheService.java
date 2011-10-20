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

package org.apache.directory.server.core.api;


import java.io.File;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A ehcache based cache service to be used for various caching requirement in the server. 
 * 
 * If a cache config file with the name {@link #DIRECTORY_CACHESERVICE_XML} is present in
 * the "workdirectory" of the DirectoryService then that file will be used for configuring 
 * the {@link CacheManager}, if not a default cache configuration file bundled along with 
 * this class is used
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CacheService
{

    private static final String DIRECTORY_CACHESERVICE_XML = "directory-cacheservice.xml";

    private static final Logger LOG = LoggerFactory.getLogger( CacheService.class );

    /** the ehcache cache manager */
    private CacheManager cacheManager;


    public CacheService()
    {
    }


    public void initialize( DirectoryService dirService )
    {
        if ( ( cacheManager != null ) && ( cacheManager.getStatus() == Status.STATUS_ALIVE ) )
        {
            LOG.warn( "cache service was already initialized and is alive" );
            return;
        }

        File configFile = new File( dirService.getInstanceLayout().getConfDirectory(), DIRECTORY_CACHESERVICE_XML );

        if ( !configFile.exists() )
        {
            LOG.info( "no custom cache configuration was set, loading the default cache configuration" );

            cacheManager = new CacheManager( getClass().getClassLoader().getResource( DIRECTORY_CACHESERVICE_XML ) );
        }
        else
        {
            LOG.info( "loading cache configuration from the file {}", configFile );

            cacheManager = new CacheManager( configFile.getAbsolutePath() );
        }
    }


    public void destroy()
    {
        LOG.info( "clearing all the caches" );

        cacheManager.clearAll();
        cacheManager.shutdown();
    }


    public Cache getCache( String name )
    {
        LOG.info( "fetching the cache named {}", name );

        return cacheManager.getCache( name );
    }
}
