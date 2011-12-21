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


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.instance.CachedComponentInstance;
import org.apache.directory.server.component.instance.ComponentInstance;
import org.apache.directory.server.component.instance.ComponentInstanceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides methods to create instances, deploy schemas and create instance entries on DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ComponentManager
{
    /*
     * Logger
     */
    private final Logger LOG = LoggerFactory.getLogger( ComponentManager.class );

    /*
     * Instance Generators
     */
    private Dictionary<String, ComponentInstanceGenerator> instanceGenerators;

    /*
     * Configuration Manager
     */
    private ConfigurationManager configManager;


    public ComponentManager( ConfigurationManager configManager )
    {
        instanceGenerators = new Hashtable<String, ComponentInstanceGenerator>();

        this.configManager = configManager;
    }


    /**
     * Adds new instance generator for specified component type.
     * Keeps the first added generator as default.
     *
     * @param componentType component type to register instance generator
     * @param generator instance generator instance
     */
    public void addInstanceGenerator( String componentType, ComponentInstanceGenerator generator )
    {
        if ( instanceGenerators.get( componentType ) == null )
        {
            instanceGenerators.put( componentType, generator );
        }
    }


    /**
     * Creates the new instance based from its factory's default configuration.
     * And hook its instance with the DIT.
     *
     * @param component ADSComponent reference create new instance for.
     * @return created instance
     */
    public ComponentInstance createInstance( ADSComponent component )
    {
        if ( component.getDefaultConfiguration() == null )
        {
            ComponentInstanceGenerator generator = instanceGenerators.get( component.getComponentType() );
            if ( generator != null )
            {
                Properties defaultConf = generator.extractDefaultConfiguration( component );
                if ( defaultConf == null )
                {
                    LOG.info( "instance generator failed to extract default configuration for component:" + component );
                    return null;
                }
                component.setDefaultConfiguration( defaultConf );
            }
            else
            {
                LOG.info( "No instance generator found to extract default configuration for component:" + component );
                return null;
            }
        }

        ComponentInstance instance = generateInstance( component, component.getDefaultConfiguration() );

        component.addInstance( instance );
        configManager.injectInstance( instance );

        return instance;
    }


    /**
     * Generate and return the instance of the given component
     * using ComponentInstanceGenerator registered for its type.
     *
     * @param component ADSComponent reference to instantiate
     * @param properties Configuration to create instance under.
     * @return created ComponentInstance reference
     */
    public ComponentInstance generateInstance( ADSComponent component, Properties properties )
    {
        ComponentInstanceGenerator generator = instanceGenerators.get( component.getComponentType() );
        if ( generator != null )
        {
            ComponentInstance instance = generator.createInstance( component, properties );

            return instance;
        }
        else
        {
            LOG.info( "No instance generator found for component:" + component );
            return null;
        }
    }


    /**
     * Loads the cached instance configurations for component, and use
     * them to create cached instances.
     *
     * @param component ADSComponent reference to load its cached instances.
     * @return loaded instances.
     */
    public List<ComponentInstance> loadCachedInstances( ADSComponent component )
    {
        List<ComponentInstance> loadedInstances = new ArrayList<ComponentInstance>();

        for ( CachedComponentInstance cachedConf : component.getCachedInstances() )
        {
            ComponentInstance instance = loadInstance( component, cachedConf );
            if ( instance != null )
            {
                loadedInstances.add( instance );
            }
        }

        return loadedInstances;
    }


    /**
     * Loads the cached component instance.
     *
     * @param component ADSComponent reference owning cached instance
     * @param cachedInstance cached instance reference
     * @return ComponentInstance which is loaded from cache.
     */
    public ComponentInstance loadInstance( ADSComponent component, CachedComponentInstance cachedInstance )
    {
        ComponentInstance instance = generateInstance( component, cachedInstance.getCachedConfiguration() );

        if ( instance == null )
        {
            return null;
        }

        component.addInstance( instance );

        return instance;
    }

}
