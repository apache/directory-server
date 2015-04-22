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
import java.util.UUID;

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
    /** The cache configuration file */
    private static final String DIRECTORY_CACHESERVICE_XML = "directory-cacheservice.xml";

    /** The associated logger */
    private static final Logger LOG = LoggerFactory.getLogger( CacheService.class );

    /** the ehcache cache manager */
    private CacheManager cacheManager;

    /** A flag telling if the cache Service has been intialized */
    private boolean initialized;


    /**
     * Creates a new instance of CacheService.
     */
    public CacheService()
    {
    }


    /**
     * Creates a new instance of CacheService with the given cache manager.
     *
     * @param cachemanager The provided CaxcheManager instance
     */
    public CacheService( CacheManager cachemanager )
    {
        this.cacheManager = cachemanager;

        if ( cachemanager != null )
        {
            initialized = true;
        }
    }


    /**
     * Initialize the CacheService
     *
     * @param layout The place on disk where the cache configuration will be stored
     */
    public void initialize( InstanceLayout layout )
    {
        initialize( layout, null );
    }


    /**
     * Initialize the CacheService
     *
     * @param layout The place on disk where the cache configuration will be stored
     * @param instanceId The Instance identifier
     */
    public void initialize( InstanceLayout layout, String instanceId )
    {
        if ( initialized )
        {
            LOG.debug( "CacheService was already initialized, returning" );
            return;
        }

        LOG.debug( "CacheService initialization, for instance {}", instanceId );

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

        String confName = UUID.randomUUID().toString();
        cc.setName( confName );

        if ( cachePath == null )
        {
            cachePath = FileUtils.getTempDirectoryPath();
        }

        cachePath += File.separator + confName;
        cc.getDiskStoreConfiguration().setPath( cachePath );

        cacheManager = new CacheManager( cc );

        initialized = true;
    }


    /**
     * Clear the cache and shutdown it
     */
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


    /**
     * Get a specific cache from its name, or create a new one
     *
     * @param name The Cache name we want to retreive
     * @return The found cache. If we don't find it, we create a new one.
     */
    public Cache getCache( String name )
    {
        if ( !initialized )
        {
            LOG.error( "Cannot fetch the cache named {}, the CacheServcie is not initialized", name );
            throw new IllegalStateException( "CacheService was not initialized" );
        }

        LOG.info( "fetching the cache named {}", name );

        Cache cache = cacheManager.getCache( name );

        if ( cache == null )
        {
            LOG.info( "No cache with name {} exists, creating one", name );
            cacheManager.addCache( name );
            cache = cacheManager.getCache( name );
        }

        return cache;
    }


    /**
     * Remove a cache if it exists.
     * 
     * @param name The Cache's name we want to remove
     */
    public void remove( String name )
    {
        if ( cacheManager.cacheExists( name ) )
        {
            LOG.info( "Removing the cache named {}", name );

            cacheManager.removeCache( name );
        }
        else
        {
            LOG.info( "Cannot removing the cache named {}, it does not exist", name );
        }
    }
}
