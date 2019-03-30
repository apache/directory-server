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


import java.util.Iterator;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheManager;

import org.apache.directory.api.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.jcache.CacheManagerImpl;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import com.typesafe.config.ConfigFactory;


/**
 * A cache service to be used for various caching requirement in the server. 
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
    //private static final String DIRECTORY_CACHESERVICE_XML = "directory-cacheservice.xml";

    /** The associated logger */
    private static final Logger LOG = LoggerFactory.getLogger( CacheService.class );

    /** the cache manager */
    private CacheManager cacheManager = null;

    /**
     * Utility method to dump the cache contents to a StringBuffer.
     * This is needed because Cache objects only implements Iterable
     * 
     * @return a StringBuffer
     */
    public static final StringBuffer dumpCacheContentsToString( Cache< ?, ? > cache ) 
    {
        Iterator<?> it = cache.iterator();
        StringBuffer sb = new StringBuffer();
        
        while ( it.hasNext() )
        {
            Entry<?, ?> nextObj = ( Entry<?, ?> ) it.next();
            sb.append( '\t' )
            .append( nextObj.getKey().toString() )
            .append( " -> " )
            .append( nextObj.getValue().toString() )
            .append( '\n' );
        }
        
        return sb;
    }

    /**
     * Creates a new instance of CacheService.
     */
    public CacheService()
    {
    }


    /**
     * Creates a new instance of CacheService with the given cache manager.
     *
     * @param cachemanager The provided CacheManager instance
     */
    public CacheService( CacheManager cachemanager )
    {
        this.cacheManager = cachemanager;
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
        LOG.debug( "CacheService initialization, for instance {}", instanceId );

        if ( ( cacheManager != null ) && ( !cacheManager.isClosed() ) )
        {
            LOG.warn( "cache service was already initialized and is available" );

            return;
        }

        CaffeineCachingProvider p = new CaffeineCachingProvider();
        cacheManager = new CacheManagerImpl( p, p.getDefaultURI(), getClass().getClassLoader(),
            p.getDefaultProperties(),
            ConfigFactory.load( getClass().getClassLoader() ) );
        //CaffeineCachingProvider
//        CachingProvider cachingProvider = Caching.getCachingProvider(
//            "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider",
//            getClass().getClassLoader() );
//        cacheManager = cachingProvider.getCacheManager();
        System.out.println( "###### entryDn cache: " + cacheManager.getCache( "entryDn", String.class, Dn.class ) );

        // TODO: need to create caches?
        //cacheManager.createCache( cacheName, configuration )
    }


    /**
     * Clear the cache and shutdown it
     */
    public void destroy()
    {
        if ( cacheManager == null )
        {
            return;
        }

        LOG.info( "clearing all the caches" );

        cacheManager.close();
        cacheManager = null;
    }


    /**
     * Get a specific cache from its name, or create a new one
     *
     * @param name The Cache name we want to retreive
     * @return The found cache. If we don't find it, we create a new one.
     */
    public <K, V> Cache<K, V> getCache( String name, Class<K> keyClazz, Class<V> valueClazz )
    {
        if ( cacheManager == null )
        {
            LOG.error( "Cannot fetch the cache named {}, the CacheServcie is not initialized", name );
            throw new IllegalStateException( "CacheService was not initialized" );
        }

        LOG.info( "fetching the cache named {}", name );

        Cache<K, V> cache = cacheManager.getCache( name, keyClazz, valueClazz );

        return cache;
    }


    /**
     * Remove a cache if it exists.
     * 
     * @param name The Cache's name we want to remove
     */
    public void remove( String name )
    {
        LOG.info( "Removing the cache named {}", name );
        
        cacheManager.destroyCache( name );
    }
}
