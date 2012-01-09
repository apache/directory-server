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

import org.apache.directory.server.component.ADSComponent;
import org.apache.felix.ipojo.Factory;


/*
 * Class to store and retrieve ADSComponent references under different mappings.
 */
public class ComponentRegistry
{
    /*
     * Used to map component name to ADSComponent
     */
    private Dictionary<String, ADSComponent> nameToComponentMap;

    /*
     * Used to map IPojo factory to ADSComponent
     */
    private Dictionary<Factory, ADSComponent> factoryToComponentMap;

    /*
     * Used to map component type to all ADSComponents of that type
     */
    private Dictionary<String, List<ADSComponent>> typeToComponentsMap;

    /*
     * Used to keep all components
     */
    private List<ADSComponent> components;


    public ComponentRegistry()
    {
        nameToComponentMap = new Hashtable<String, ADSComponent>();
        factoryToComponentMap = new Hashtable<Factory, ADSComponent>();
        typeToComponentsMap = new Hashtable<String, List<ADSComponent>>();
        components = new ArrayList<ADSComponent>();
    }


    /**
     * Adds the component to registries
     *
     * @param component ADSComponent reference to keep in registers.
     */
    public void addComponent( ADSComponent component )
    {
        components.add( component );

        String componentType = component.getComponentType().toLowerCase();
        List<ADSComponent> componentsByType = typeToComponentsMap.get( componentType );

        if ( componentsByType == null )
        {
            List<ADSComponent> newCompList = new ArrayList<ADSComponent>();
            newCompList.add( component );
            typeToComponentsMap.put( componentType, newCompList );
        }

        nameToComponentMap.put( component.getComponentName().toLowerCase(), component );
        factoryToComponentMap.put( component.getFactory(), component );
    }


    /**
     * Deletes a component from registries
     *
     * @param component ADSComponent reference to remove from registries
     */
    public void removeComponent( ADSComponent component )
    {
        components.remove( component );

        String componentType = component.getComponentType().toLowerCase();
        List<ADSComponent> componentsByType = typeToComponentsMap.get( componentType );
        if ( componentsByType != null )
        {
            componentsByType.remove( component );
        }

        nameToComponentMap.remove( component.getComponentName().toLowerCase() );
        factoryToComponentMap.remove( component.getFactory() );
    }


    /**
     * Used to retrieve all component references.
     *
     * @return All ADSComponents in regitries as List
     */
    public List<ADSComponent> getAllComponents()
    {
        return components;
    }


    /**
     * Retrieve the component by its name.
     *
     * @param componentName Component name to retrieve its ADSComponent reference
     * @return ADSComponent reference 
     */
    public ADSComponent getComponentByName( String componentName )
    {
        return nameToComponentMap.get( componentName.toLowerCase() );
    }


    /**
     * Retrieve the component by its IPojo Factory
     *
     * @param componentFactory IPojo component factory to fetch its component.
     * @return ADSComponent reference.
     */
    public ADSComponent getCompoentByFactory( Factory componentFactory )
    {
        return factoryToComponentMap.get( componentFactory );
    }


    /**
     * Gets all the components under specified type.
     *
     * @param componentType Component type to look for
     * @return All ADSComponent references for given type
     */
    public List<ADSComponent> getComponentsByType( String componentType )
    {
        return typeToComponentsMap.get( componentType.toLowerCase() );
    }
}
