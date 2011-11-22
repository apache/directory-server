/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.component.hub;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.ADSComponentCacheHandle;
import org.apache.directory.server.component.ADSConstants;
import org.apache.directory.server.component.instance.ADSComponentInstance;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.LdapLdifException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * Class to manage schema and instance cache for ADSComponents.
 */
public class ComponentCacheManager
{
    /*
     * Logger
     */
    private final Logger LOG = LoggerFactory.getLogger( ComponentCacheManager.class );

    /*
     * List to keep track of uncached ADSComponents which wants automatic caching.
     */
    private Set<ADSComponent> uncachedComponents;


    public ComponentCacheManager()
    {
        uncachedComponents = new HashSet<ADSComponent>();
    }


    /**
     * Adds component to watch list.
     *
     * @param component ADSComponent to watch its cache for.
     */
    public void addCacheWatch( ADSComponent component )
    {
        uncachedComponents.add( component );
    }


    /**
     * Removes component from watch list.
     * Also called by cacheComponent()
     *
     * @param component ADSComponent release watch.
     */
    public void removeCacheWatch( ADSComponent component )
    {
        uncachedComponents.remove( component );
    }


    /**
     * Caches all the ADSComponents in the watch list, which are registered but didn't cached.
     * It is called by ComponentHub when it is being invalidated.
     */
    public void cacheRemaningComponents()
    {
        for ( ADSComponent component : uncachedComponents )
        {
            cacheSchema( component );
            cacheInstanceConfigurations( component );
        }
    }


    /**
     * Returns the cache handle for specified component.
     *
     * @param component Component to get cache handle for.
     * @return cache handle.
     */
    public ADSComponentCacheHandle getCacheHandle( ADSComponent component )
    {
        String componentCachePath = getComponentCacheBaseDir( component );
        String schemaFile = componentCachePath + IOUtils.DIR_SEPARATOR + ADSConstants.ADS_CACHE_SCHEMA_FILE;
        String instancesDir = componentCachePath + IOUtils.DIR_SEPARATOR + ADSConstants.ADS_CACHE_INSTANCES_DIR;

        String version = getCacheVersion( component );

        return new ADSComponentCacheHandle( componentCachePath, schemaFile, instancesDir, version );

    }


    /**
     * Caches the schema, instance configurations and set the version for a component.
     *
     * @param component ADSComponent reference to cache.
     */
    public void cacheComponent( ADSComponent component )
    {
        cacheSchema( component );
        cacheInstanceConfigurations( component );
        cacheVersion( component );

        removeCacheWatch( component );
    }


    /**
     * Cache the schema for component.
     *
     * @param component ADSComponent reference to cache its schema.
     */
    public void cacheSchema( ADSComponent component )
    {
        List<LdifEntry> schema = component.getSchema().getSchemaElements();
        if ( schema == null )
        {
            LOG.info( "Component does not have custom schema elements:  "
                + component );
            return;
        }

        String componentPath = component.getCacheHandle().getCacheBaseDir();
        String filePath = component.getCacheHandle().getCachedSchemaLocation();

        File cacheDirectory = new File( componentPath );

        try
        {
            FileUtils.forceMkdir( cacheDirectory );
            BufferedWriter writer = new BufferedWriter( new FileWriter( filePath ) );

            for ( LdifEntry ldif : schema )
            {
                writer.write( LdifUtils.convertToLdif( ldif ) );
                writer.write( IOUtils.LINE_SEPARATOR + IOUtils.LINE_SEPARATOR );
            }

            writer.flush();
            writer.close();

            cacheVersion( component );
        }
        catch ( IOException e )
        {
            LOG.info( " I/O Error while caching schema for component" + component );
        }
        catch ( LdapException e )
        {
            LOG.info( "LdifConvert Error while caching schema for component " + component );
        }
    }


    /**
     * Caches the current configuration of instances of an ADSComponent.
     *
     * @param component ADSComponent reference to cache its instance configurations.
     */
    public void cacheInstanceConfigurations( ADSComponent component )
    {
        String instancesDirPath = component.getCacheHandle().getCachedInstanceConfigurationsLocation();
        File instancesDir = new File( instancesDirPath );
        try
        {
            if ( instancesDir.exists() )
            {
                //Delete existing configuration cache.
                FileUtils.deleteDirectory( instancesDir );
            }

            FileUtils.forceMkdir( instancesDir );

            int counter = 1;
            for ( ADSComponentInstance instance : component.getInstances() )
            {
                Properties props = instance.getInstanceConfiguration();

                String fileName = instancesDirPath + IOUtils.DIR_SEPARATOR + component.getComponentName() + "-"
                    + counter++ + ".config";

                props.store( new FileOutputStream( fileName ), "Instance Configuration for" + component );
            }
        }
        catch ( IOException exc )
        {
            LOG.info( "I/O error occured while caching instance configuration as Properties for component:"
                + component );
        }
    }


    /**
     * Set the cache version for the specified component
     *
     * @param component ADSComponent reference to set version on cache for.
     */
    public void cacheVersion( ADSComponent component )
    {
        String componentPath = component.getCacheHandle().getCacheBaseDir();
        String versionFilePath = componentPath + IOUtils.DIR_SEPARATOR + ADSConstants.ADS_CACHE_VERSION_FILE;

        File cacheDirectory = new File( componentPath );

        try
        {
            if ( cacheDirectory.exists() )
            {
                BufferedWriter writer = new BufferedWriter( new FileWriter( versionFilePath ) );
                String version = component.getComponentVersion();

                writer.write( version );

                writer.flush();
                writer.close();

                component.getCacheHandle().setCachedVersion( version );
            }
        }
        catch ( IOException e )
        {
            LOG.info( "  Error while setting version under cache for component:" + component );
        }
    }


    /**
     * Gets the cached schema of the component.
     * Returning list is in the from it can loaded into ApacheDS without any sorting.
     *
     * @param component ADSComponent reference to search cache for its schema
     * @return The cached schema in the form of LdifEntry list.
     */
    public List<LdifEntry> getCachedSchema( ADSComponent component )
    {

        if ( !cacheVersionMatch( component ) )
        {
            LOG.info( "Version mismatch between the cache and the component:  "
                + component );

            if ( !validateCache( component ) )
            {
                return null;
            }
        }

        String schemaFilePath = component.getCacheHandle().getCachedSchemaLocation();
        List<LdifEntry> schema = null;

        try
        {
            File schemaFile = new File( schemaFilePath );

            if ( !schemaFile.exists() )
            {
                return null;
            }

            LdifReader reader = new LdifReader( schemaFile );
            schema = new ArrayList<LdifEntry>();

            for ( LdifEntry entry : reader )
            {
                schema.add( entry );
            }

        }
        catch ( LdapLdifException e )
        {
            LOG.info( "Error while readering cached schema file for component:  "
                + component );
            return null;
        }

        return schema;
    }


    /**
     * Gets the cached instance configurations from the cache as list.
     *
     * @param component ADSComponent reference to search cache for its instance configurations
     * @return List of Properties describing cached instance configurations.
     */
    public List<Properties> getCachedInstanceConfigurations( ADSComponent component )
    {
        String instancesDirPath = component.getCacheHandle().getCachedInstanceConfigurationsLocation();
        File instancesDir = new File( instancesDirPath );

        if ( !instancesDir.exists() )
        {
            return null;
        }

        File[] configFiles = instancesDir.listFiles( new FileFilter()
        {

            @Override
            public boolean accept( File pathname )
            {
                return pathname.toString().matches( "*-*.config" );
            }
        } );

        List<Properties> instanceConfigurations = new ArrayList<Properties>();

        for ( File configFile : configFiles )
        {
            try
            {
                Properties prop = new Properties();
                prop.load( new FileInputStream( configFile ) );

                instanceConfigurations.add( prop );
            }
            catch ( IOException exc )
            {
                LOG.info( "I/O Error while loading properties from file:" + configFile );
            }
        }

        if ( instanceConfigurations.size() == 0 )
        {
            return null;
        }

        return instanceConfigurations;

    }


    /**
     * Gets the version of cache for the component.
     *
     * @param component ADSComponent reference to get its cache version
     * @return version of the cache.
     */
    public String getCacheVersion( ADSComponent component )
    {
        String componentPath = component.getCacheHandle().getCacheBaseDir();
        String versionFilePath = componentPath + IOUtils.DIR_SEPARATOR + ADSConstants.ADS_CACHE_VERSION_FILE;
        String version = null;

        File versionFile = new File( versionFilePath );
        if ( !versionFile.exists() )
        {
            return null;
        }

        try
        {
            BufferedReader reader = new BufferedReader( new FileReader( versionFile ) );
            version = reader.readLine();
            reader.close();
        }
        catch ( IOException e )
        {
            LOG.info( "Error occured while reading version file of the cached component:  "
                + component );
        }

        return version;
    }


    /**
     * Checks whether specified component's version is compatible with the cached version.
     *
     * TODO It looks for exact match for now, change it to some elegant match policy.
     *
     * @param component ADSComponent reference to check cache against.
     * @return result of the test.
     */
    public boolean cacheVersionMatch( ADSComponent component )
    {
        String cachedVersion = component.getCacheHandle().getCachedVersion();
        String currentVersion = component.getComponentVersion();

        if ( !currentVersion.equals( cachedVersion ) && cachedVersion != null )
        {
            return false;
        }

        return true;
    }


    /**
     * It is used to validate the cache for the given component.
     * TODO validateCache will be implemented later.
     *
     * @param component ADSComponent to validate cache against.
     * @return whether validation is successfull or not.
     */
    public boolean validateCache( ADSComponent component )
    {
        return false;
    }


    /**
     * It removes all the cache for given component.
     *
     * @param component ADSComponent reference to purge its cache.
     */
    public void purgeCache( ADSComponent component )
    {
        File baseCacheDir = new File( component.getCacheHandle().getCacheBaseDir() );
        if ( baseCacheDir.exists() )
        {
            try
            {
                FileUtils.forceDelete( baseCacheDir );
            }
            catch ( IOException e )
            {
                LOG.info( "I/O Error occured while purging the cache for component" + component );
            }
        }
    }


    /**
     * Gets the base dir in cache for specified component.
     *
     * @param component ADSComponent to get cache location for.
     * @return cache location as String.
     */
    private String getComponentCacheBaseDir( ADSComponent component )
    {
        String componentType = component.getComponentType();
        String componentName = component.getComponentName();

        return ADSConstants.ADS_CACHE_BASE_DIR + IOUtils.DIR_SEPARATOR + componentType + IOUtils.DIR_SEPARATOR
            + componentName;
    }

}
