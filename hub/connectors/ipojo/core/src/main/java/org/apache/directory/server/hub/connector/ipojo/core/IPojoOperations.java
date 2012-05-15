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

package org.apache.directory.server.hub.connector.ipojo.core;


import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.directory.server.hub.api.component.DcProperty;
import org.apache.directory.server.hub.api.component.DcRuntime;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.ComponentInstantiationException;
import org.apache.directory.server.hub.api.exception.ComponentReconfigurationException;
import org.apache.directory.server.hub.api.meta.DcOperationsManager;
import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;


public class IPojoOperations implements DcOperationsManager
{

    private ComponentFactory factory;


    public IPojoOperations( ComponentFactory factory )
    {
        this.factory = factory;
    }


    @Override
    public void instantiateComponent( DirectoryComponent component ) throws ComponentInstantiationException
    {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        for ( DcProperty prop : component.getConfiguration() )
        {
            configuration.put( prop.getName(), prop.getObject() );
        }

        configuration.put( "instance.name", component.getComponentPID() );

        try
        {
            ComponentInstance instance = factory.createComponentInstance( configuration );

            InstanceManager manager = ( InstanceManager ) instance;
            Object pojo = manager.getPojoObject();

            component.setRuntimeInfo( new DcRuntime( manager, pojo ) );
        }
        catch ( UnacceptableConfiguration e )
        {
            throw new ComponentInstantiationException( "Configuration unacceptable for component"
                + component.getComponentPID(), e );
        }
        catch ( MissingHandlerException e )
        {
            throw new ComponentInstantiationException( "Missing handler for component:" + component.getComponentPID(),
                e );
        }
        catch ( ConfigurationException e )
        {
            throw new ComponentInstantiationException( "Configuration failed for component:"
                + component.getComponentPID(), e );
        }
    }


    @Override
    public void reconfigureComponent( DirectoryComponent component ) throws ComponentReconfigurationException
    {
        DcRuntime runtime = component.getRuntimeInfo();
        if ( runtime == null || runtime.getSpecialObject() == null )
        {
            throw new ComponentReconfigurationException( "Failed to reconfigure disposed component:"
                + component.getComponentPID() );
        }

        InstanceManager manager = ( InstanceManager ) runtime.getSpecialObject();

        if ( manager.getState() != InstanceManager.VALID )
        {
            throw new ComponentReconfigurationException( "Failed to reconfigure disactivated component:"
                + component.getComponentPID() );
        }

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        for ( DcProperty prop : component.getConfiguration() )
        {
            configuration.put( prop.getName(), prop.getObject() );
        }

        configuration.put( "instance.name", component.getComponentPID() );

        manager.reconfigure( configuration );

        if ( manager.getState() != InstanceManager.VALID )
        {
            throw new ComponentReconfigurationException( "Reconfiguration stopped the component:"
                + component.getComponentPID() );
        }
    }


    @Override
    public void disposeComponent( DirectoryComponent component )
    {
        DcRuntime runtime = component.getRuntimeInfo();
        if ( runtime == null || runtime.getSpecialObject() == null )
        {
            component.setRuntimeInfo( null );
            return;
        }

        InstanceManager manager = ( InstanceManager ) runtime.getSpecialObject();
        if ( manager.getState() != InstanceManager.DISPOSED )
        {
            manager.dispose();
        }

        component.setRuntimeInfo( null );
    }

}
