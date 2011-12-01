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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.hub.listener.HubListener;
import org.apache.directory.server.component.instance.ADSComponentInstanceGenerator;
import org.apache.directory.server.component.instance.DefaultComponentInstanceGenerator;
import org.apache.directory.server.component.schema.ADSComponentSchema;
import org.apache.directory.server.component.schema.ComponentSchemaGenerator;
import org.apache.directory.server.component.schema.DefaultComponentSchemaGenerator;
import org.apache.directory.server.component.utilities.ADSComponentHelper;
import org.apache.directory.server.component.utilities.ADSConstants;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;


/**
 * An IPojo component that listens for incoming factories and instances.
 * Creating or destroying corresponding ADSComponent from them.
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Component
@Whiteboards(whiteboards =
    {
        @Wbp(onArrival = "onFactoryArrival",
            onDeparture = "onFactoryDeparture",
            filter = "(objectClass=org.apache.felix.ipojo.Factory)"),
        @Wbp(onArrival = "onInstanceArrival",
            onDeparture = "onInstanceDeparture",
            filter = "(objectClass=org.apache.felix.ipojo.architecture.Architecture)")
})
public class ComponentHub
{
    /*
     * boolean value to check for deferred writes to schema partition
     */
    private boolean schemaReady = false;

    /*
     * boolean value to check for deferred writes to config partition
     */
    private boolean configReady = false;

    /*
     * Map to keep "component type" -> "components" mapping.
     */
    private Dictionary<String, List<ADSComponent>> componentMap;

    /*
     * List to keep all active ApacheDS components.
     */
    private List<ADSComponent> components;

    /*
     * Used to manage listeners.
     */
    private ComponentEventManager eventManager = new ComponentEventManager();

    /*
     * Used to manage component caches.
     */
    private ComponentCacheManager cacheManager = new ComponentCacheManager();

    /*
     * Used to manage instances' DIT hooks.
     */
    private ConfigurationManager configManager = new ConfigurationManager();

    /*
     * Used to manage components
     */
    private ComponentManager componentManager = new ComponentManager( cacheManager, configManager );

    /*
     * Allowed interfaces for components.
     */
    private String[] allowedInterfaces = new String[]
        { Interceptor.class.getName() };

    /*
     * OSGI Logger
     */
    @Requires
    private LogService logger;


    public ComponentHub()
    {
        componentMap = new Hashtable<String, List<ADSComponent>>();
        components = new ArrayList<ADSComponent>();
    }


    /**
     * Called when ADSComponentHub instance is validated by IPojo
     *
     */
    @Validate
    public void hubValidated()
    {
        logger.log( LogService.LOG_INFO, "ADSComponentHub validated." );
    }


    /**
     * Called when ADSComponentHub instance is invalidated by IPojo
     *
     */
    @Invalidate
    public void hubInvalidated()
    {
        logger.log( LogService.LOG_INFO, "ADSComponentHub being invalidated." );

        cacheManager.cacheRemaningComponents();
    }


    /**
     * Factory arrival callback, registered by whiteboard handler.
     *
     * @param ref Reference to IPojo Factory
     */
    public void onFactoryArrival( ServiceReference ref )
    {
        Factory arrivingFactory = ( Factory ) ref.getBundle().getBundleContext().getService( ref );
        if ( !checkIfADSComponent( arrivingFactory ) )
        {
            return;
        }

        String componentType = parseComponentType( arrivingFactory );

        //Actual ADSComponent creation
        ADSComponent component = generateADSComponent( componentType, arrivingFactory );

        eventManager.fireComponentCreated( component );

        //Keep the newly created ADSComponent reference.
        components.add( component );

        List<ADSComponent> componentsByType = componentMap.get( componentType );
        if ( componentsByType == null )
        {
            List<ADSComponent> newCompList = new ArrayList<ADSComponent>();
            newCompList.add( component );
            componentMap.put( componentType, newCompList );
        }
        else
        {
            componentsByType.add( component );
        }

    }


    /**
     * Factory departure callback, registered by whiteboard handler.
     *
     * @param ref Reference to IPojo Factory
     */
    public void onFactoryDeparture( ServiceReference ref )
    {
        Factory leavingFactory = ( Factory ) ref.getBundle().getBundleContext().getService( ref );
        if ( !checkIfADSComponent( leavingFactory ) )
        {
            return;
        }

        String componentType = parseComponentType( leavingFactory );

        ADSComponent associatedComp = null;
        for ( ADSComponent _comp : components )
        {
            if ( _comp.getFactory().getName().equals( leavingFactory.getName() ) )
            {
                associatedComp = _comp;
                break;
            }
        }

        if ( associatedComp == null )
        {
            logger.log( LogService.LOG_INFO, "Couldn't found an associated ADSComponent for factory:"
                + leavingFactory.getName() );
            return;
        }

        // All clients are notified now cache and delete the ADSComponent existence on ApacheDS
        cacheAndReleaseADSComponent( associatedComp );

        // Fire "Component Deleted" event
        eventManager.fireComponentDeleted( associatedComp );

    }


    /**
     * IPojo instance arrival callback, registered by whiteboard handler.
     *
     * @param ref Reference to IPojo instance
     */
    public void onInstanceArrival( ServiceReference ref )
    {

    }


    /**
     * IPojo instance departure callback, registered by whiteboard handler.
     *
     * @param ref Reference to IPojo instance
     */
    public void onInstanceDeparture( ServiceReference ref )
    {

    }


    /**
     * Check whether the argument is ADSComponent annotated.
     *
     * @param factory
     * @return
     */
    private boolean checkIfADSComponent( Factory factory )
    {
        String implementingIface = parseBaseInterface( factory );
        for ( String iface : allowedInterfaces )
        {
            if ( iface.equals( implementingIface ) )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Gets the component type by provided specification of a component.
     *
     * @param factory to get its component type
     * @return component type as interface name.
     */
    private String parseComponentType( Factory factory )
    {
        String baseInterface = parseBaseInterface( factory ).toLowerCase();

        if ( baseInterface.contains( "." ) )
        {
            return baseInterface.substring( baseInterface.lastIndexOf( '.' ) + 1 );
        }
        else
        {
            return baseInterface;
        }
    }


    private String parseBaseInterface( Factory factory )
    {
        String[] publishedInterfaces = factory.getComponentDescription().getprovidedServiceSpecification();
        if ( publishedInterfaces.length == 0 )
        {
            return null;
        }

        return publishedInterfaces[publishedInterfaces.length - 1];

    }


    /**
     * Generates a new ADSComponent with its schema and cache handle. 
     *
     * @param componentType Type of a component being created
     * @param factory a factory reference to create a ADSComponent for.
     * @return
     */
    private ADSComponent generateADSComponent( String componentType, Factory factory )
    {
        ADSComponent component = new ADSComponent( componentManager );

        component.setFactory( factory );
        component.setComponentType( componentType );
        component.setComponentName( ADSComponentHelper.getComponentName( component.getFactory() ) );
        component.setComponentVersion( ADSComponentHelper.getComponentVersion( component.getFactory() ) );
        component.setCacheHandle( cacheManager.getCacheHandle( component ) );

        return component;
    }


    /**
     * Cache the ADSComponent existence on ApacheDS with all of its DIT entries. And then release it from hub.
     *
     * @param leavingComp ADSComponent reference to cache and release.
     */
    private void cacheAndReleaseADSComponent( ADSComponent leavingComp )
    {

    }


    /**
     * Registers a HubListener for specified component type.
     *
     * @param componentType component type to get notifications for.
     * @param listener HubListener implementation
     */
    public void registerListener( String componentType, HubListener listener )
    {
        eventManager.registerListener( componentType, listener );
    }


    /**
     * Removes the specified listener from the notification chain.
     *
     * @param listener HubListener implementation
     */
    public void removeListener( HubListener listener )
    {
        eventManager.removeListener( listener );
    }
}
