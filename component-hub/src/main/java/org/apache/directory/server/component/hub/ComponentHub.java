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
import org.apache.directory.server.component.ADSConstants;
import org.apache.directory.server.component.hub.listener.HubListener;
import org.apache.directory.server.component.schema.ComponentSchemaGenerator;
import org.apache.directory.server.component.schema.UserComponentSchemaGenerator;
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
     * Map to keep "component type" -> "schema generator" mapping.
     */
    private Dictionary<String, ComponentSchemaGenerator> schemaGenerators;

    /*
     * Map to keep "component type" -> "listeners" mapping
     */
    private Dictionary<String, List<HubListener>> listenersMap;

    /*
     * OSGI Logger
     */
    @Requires
    private LogService logger;


    public ComponentHub()
    {
        componentMap = new Hashtable<String, List<ADSComponent>>();
        components = new ArrayList<ADSComponent>();

        schemaGenerators = new Hashtable<String, ComponentSchemaGenerator>();
        schemaGenerators.put( ADSConstants.ADS_COMPONENT_TYPE_USER, new UserComponentSchemaGenerator() );

        listenersMap = new Hashtable<String, List<HubListener>>();
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
        logger.log( LogService.LOG_INFO, "ADSComponentHub validated." );
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

        try
        {
            String componentType = parseComponentType( arrivingFactory );

            List<HubListener> listenersByType = listenersMap.get( componentType );

            // Fire the 'factory arrived' event on listeners.
            if ( listenersByType != null )
            {
                for ( HubListener listener : listenersByType )
                {
                    listener.onFactoryArrival( arrivingFactory );
                }
            }

            //Actual ADSComponent creation
            ADSComponent component = generateADSComponent( componentType, arrivingFactory );

            // Iterate over listeners for 'component created' event. Apply the changes applied by them.
            if ( listenersByType != null )
            {
                for ( HubListener listener : listenersByType )
                {
                    ADSComponent _comp = listener.onComponentCreation( component );
                    if ( _comp != null )
                    {
                        component = _comp;
                    }
                }
            }

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
        catch ( ParseException e )
        {
            e.printStackTrace();
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

        try
        {
            String componentType = parseComponentType( leavingFactory );

            List<HubListener> listenersByType = listenersMap.get( componentType );

            // Fire the 'factory leaving' event on listeners.
            if ( listenersByType != null )
            {
                for ( HubListener listener : listenersByType )
                {
                    listener.onFactoryDeparture( leavingFactory );
                }
            }

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

            // Iterate over listeners for 'component deleting' event.
            if ( listenersByType != null )
            {
                for ( HubListener listener : listenersByType )
                {
                    listener.onComponentDeletion( associatedComp );
                }
            }

            // All clients are notified now cache and delete the ADSComponent existence on ApacheDS
            cacheAndReleaseADSComponent( associatedComp );

        }
        catch ( ParseException e )
        {
            e.printStackTrace();
        }
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
        List<String> handlers = factory.getRequiredHandlers();
        for ( String handlerName : handlers )
        {
            if ( handlerName.equals( ADSConstants.ADS_COMPONENT_HANDLER_FULLNAME ) )
            {
                return true;
            }
        }

        return false;
    }


    private String parseComponentType( Factory factory ) throws ParseException
    {
        Dictionary bundleHeaders = factory.getBundleContext().getBundle().getHeaders();
        String ipojoHeader = ( String ) bundleHeaders.get( ADSConstants.IPOJO_HEADER );

        if ( ipojoHeader == null )
        {
            throw new ParseException( "Null ipojo header returned for factory: " + factory.getName() );
        }

        ManifestMetadataParser parser = new ManifestMetadataParser();
        parser.parseHeader( ipojoHeader );

        Element[] componentMetas = parser.getComponentsMetadata();

        for ( Element componentMeta : componentMetas )
        {
            String compName = componentMeta.getAttribute( "name" );
            if ( compName.equals( factory.getName() ) )
            {
                Element[] adsElements = componentMeta.getElements(
                    ADSConstants.ADS_COMPONENT_HANDLER_NAME,
                    ADSConstants.ADS_COMPONENT_HANDLER_NS );

                if ( adsElements == null || adsElements.length == 0 )
                {
                    throw new ParseException( "ADSComponent element couldn't be found for factory: "
                        + factory.getName() );
                }

                return adsElements[0].getAttribute( "componentType" );
            }
        }

        return null;
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
        return null;
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
        List<HubListener> listenersForComp = listenersMap.get( componentType );
        if ( listenersForComp == null )
        {
            ArrayList<HubListener> list = new ArrayList<HubListener>();
            list.add( listener );
            listenersMap.put( componentType, list );
        }
        else
        {
            if ( !listenersForComp.contains( listener ) )
            {
                listenersForComp.add( listener );
            }
        }
    }


    /**
     * Removes the specified listener from the notification chain.
     *
     * @param listener HubListener implementation
     */
    public void removeListener( HubListener listener )
    {
        Enumeration<List<HubListener>> it = listenersMap.elements();
        while ( it.hasMoreElements() )
        {
            List<HubListener> list = it.nextElement();
            if ( list.contains( listener ) )
            {
                list.remove( listener );
            }
        }
    }
}
