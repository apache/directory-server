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


/**
 * Manages component install, uninstall events for listeners.
 * Provides listeners an ability to get passed events by holding an event cache.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ComponentEventManager
{

    /*
     * Map to keep "component type" -> "listeners" mapping
     */
    private Dictionary<String, List<HubListener>> listenersMap;

    /*
     * Map to keep all passed events.
     */
    private Dictionary<String, List<ADSComponent>> eventCache;


    public ComponentEventManager()
    {
        listenersMap = new Hashtable<String, List<HubListener>>();
        eventCache = new Hashtable<String, List<ADSComponent>>();
    }


    /**
     * Registers a HubListener for specified component type.
     *
     * @param componentType component type to get notifications for.
     * @param listener HubListener implementation
     */
    public synchronized void registerListener( String componentType, HubListener listener )
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

        // Make listener receive the passed creation events.
        List<ADSComponent> passedCreations = eventCache.get( componentType );
        if ( passedCreations != null )
        {
            for ( ADSComponent comp : passedCreations )
            {
                listener.onComponentCreation( comp );
            }
        }
    }


    /**
     * Removes the specified listener from the notification chain.
     *
     * @param listener HubListener implementation
     */
    public synchronized void removeListener( HubListener listener )
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


    /**
     * Iterates through listeners for specified ADSComponent's component type.
     * Calls their onComponentCreation() method. 
     * 
     * Reference is passed without cloning. So individual listeners can change
     * the provided ADSComponent. It will affect the all component hub.
     *
     * @param component ADSComponent reference to be used for notification
     */
    public synchronized void fireComponentCreated( ADSComponent component )
    {
        List<HubListener> listenersByType = listenersMap.get( component.getComponentType() );

        // Iterate over listeners for 'component created' event. Apply the changes applied by them.
        if ( listenersByType != null )
        {
            for ( HubListener listener : listenersByType )
            {
                listener.onComponentCreation( component );
            }
        }

        cacheCreation( component );
    }


    /**
     * Iterates through listeners for specified ADSComponent's component type.
     * Calls their onComponentDeletion() method.
     * 
     * In the time callbacks are executed, the ADSComponent's existence on ApacheDS
     * is terminated. So use the reference just for statistical purposes.
     *
     * @param component ADSComponent reference to be used for notification
     */
    public synchronized void fireComponentDeleted( ADSComponent component )
    {
        List<HubListener> listenersByType = listenersMap.get( component.getComponentType() );

        // Iterate over listeners for 'component deleting' event.
        if ( listenersByType != null )
        {
            for ( HubListener listener : listenersByType )
            {
                listener.onComponentDeletion( component );
            }
        }

        unCacheCreation( component );
    }


    /**
     * Caches ADSComponent to notify late-coming listeners.
     *
     * @param comp ADSComponent reference to cache
     */
    private void cacheCreation( ADSComponent component )
    {
        String compType = component.getComponentType();
        List<ADSComponent> passedEvents = eventCache.get( compType );
        if ( passedEvents == null )
        {
            passedEvents = new ArrayList<ADSComponent>();
            passedEvents.add( component );
        }
        else
        {
            passedEvents.add( component );
        }
    }


    /**
     * Removes ADSComponent from the cache. So late-coming listeners
     * won't be notified.
     *
     * @param comp ADSComponent reference to cache
     */
    private void unCacheCreation( ADSComponent component )
    {
        String compType = component.getComponentType();
        List<ADSComponent> passedEvents = eventCache.get( compType );
        if ( passedEvents != null && passedEvents.contains( component ) )
        {
            passedEvents.remove( component );
        }
    }
}
