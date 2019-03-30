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
package org.apache.directory.server.osgi.integ;


import static org.junit.Assert.assertNotNull;

import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.junit.Test;

import com.github.benmanes.caffeine.jcache.CacheManagerImpl;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import com.typesafe.config.ConfigFactory;


public class ServerCoreAnnotationsOsgiTest extends ServerOsgiTestBase
{

    @Override
    protected String getBundleName()
    {
        return "org.apache.directory.server.core.annotations";
    }


    @Override
    protected void useBundleClasses() throws Exception
    {
        DefaultDirectoryServiceFactory factory = new DefaultDirectoryServiceFactory();
        factory.init( "foo" );
        DirectoryService ds = factory.getDirectoryService();
        assertNotNull( ds );
    }
    
    @Test
    public void test123() {
        CachingProvider cachingProvider = Caching.getCachingProvider(
            "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider",
            CacheService.class.getClassLoader());
        Cache<String, Set> cache = cachingProvider.getCacheManager()
            .getCache("groupCache", String.class, Set.class);
        System.out.println( cache );
    }
    
    @Test
    public void test234() {
        CaffeineCachingProvider p = new CaffeineCachingProvider();
        CacheManager cacheManager = new CacheManagerImpl( p, p.getDefaultURI(), getClass().getClassLoader(),
            p.getDefaultProperties(),
            ConfigFactory.load( getClass().getClassLoader() ) );

        
        Cache<String, Set> cache = cacheManager
            .getCache("groupCache", String.class, Set.class);
        System.out.println( cache );
    }

}
