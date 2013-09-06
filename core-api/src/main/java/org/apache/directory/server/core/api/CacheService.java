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
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.apache.commons.io.FileUtils;
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

    private boolean initialized;


    public CacheService()
    {
    }


    /**
     * Creates a new instance of CacheService with the given cache manager.
     *
     * @param cachemanager
     */
    public CacheService( CacheManager cachemanager )
    {
        this.cacheManager = cachemanager;
        if ( cachemanager != null )
        {
            initialized = true;
        }
    }


    public void initialize( InstanceLayout layout )
    {
        if ( initialized )
        {
            LOG.debug( "CacheService was already initialized, returning" );
            return;
        }

        if ( ( cacheManager != null ) && ( cacheManager.getStatus() == Status.STATUS_ALIVE ) )
        {
            LOG.warn( "cache service was already initialized and is alive" );
            initialized = true;
            return;
        }

        Configuration cc;
        String cachePath = null;

        if ( layout != null )
        {
            File configFile = new File( layout.getConfDirectory(), DIRECTORY_CACHESERVICE_XML );

            if ( !configFile.exists() )
            {
                LOG.info( "no custom cache configuration was set, loading the default cache configuration" );
                cc = ConfigurationFactory.parseConfiguration( getClass().getClassLoader().getResource(
                    DIRECTORY_CACHESERVICE_XML ) );
            }
            else
            {
                LOG.info( "loading cache configuration from the file {}", configFile );

                cc = ConfigurationFactory.parseConfiguration( configFile );
            }

            cachePath = layout.getCacheDirectory().getAbsolutePath();
        }
        else
        {
            LOG.info( "no custom cache configuration was set, loading the default cache configuration" );
            cc = ConfigurationFactory.parseConfiguration( getClass().getClassLoader().getResource(
                DIRECTORY_CACHESERVICE_XML ) );

            cachePath = FileUtils.getTempDirectoryPath();
        }

        cc.getDiskStoreConfiguration().setPath( cachePath );
        cacheManager = new CacheManager( cc );

        initialized = true;
    }


    public void destroy()
    {
        if ( !initialized )
        {
            return;
        }

        LOG.info( "clearing all the caches" );

        initialized = false;

        cacheManager.clearAll();
        cacheManager.shutdown();
    }


    public Cache getCache( String name )
    {
        if ( !initialized )
        {
            throw new IllegalStateException( "CacheService was not initialized" );
        }

        LOG.info( "fetching the cache named {}", name );

        Cache cache = cacheManager.getCache( name );

        if ( cache == null )
        {
            cacheManager.addCache( name );
            cache = cacheManager.getCache( name );
        }

        return cache;
    }


    public void remove( String name )
    {
        cacheManager.removeCache( name );
    }


    public void attach( Cache cache )
    {
        cacheManager.addCache( cache );
    }

}
