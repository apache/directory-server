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


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubConnector;
import org.apache.directory.server.hub.api.component.util.ComponentConstants;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DcOperationsManager;
import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.osgi.framework.BundleContext;


public class IPojoConnector implements HubConnector
{
    public static BundleContext connectorContext;

    private ComponentHub hub;

    private IPojoFactoryTracker tracker;

    private Set<String> managedFactories = new HashSet<String>();


    @Override
    public void init( ComponentHub hub )
    {
        if ( connectorContext == null )
        {
            //TODO Error log.
            return;
        }

        this.hub = hub;

        tracker = new IPojoFactoryTracker( this );
        tracker.open();
    }


    public synchronized void factoryActivated( ComponentFactory factory )
    {
        if ( isDirectoryFactory( factory ) )
        {
            DcMetadataDescriptor metadata = DcMetadataBuilder.generateDCMetadata( factory );
            DcOperationsManager operationsManager = new IPojoOperations( factory );

            try
            {
                hub.connectHandler( metadata, operationsManager );
                managedFactories.add( factory.getName() );
            }
            catch ( HubAbortException e )
            {
            }
        }
    }


    public void factoryDeactivating( ComponentFactory factory )
    {
        if ( managedFactories.contains( factory.getName() ) )
        {
            hub.disconnectHandler( factory.getName() );
        }
    }


    private boolean isDirectoryFactory( ComponentFactory factory )
    {
        PropertyDescription[] properties = factory.getComponentDescription().getProperties();

        for ( PropertyDescription pd : properties )
        {
            if ( pd.getName().equals( ComponentConstants.DC_NATURE_INDICATOR ) )
            {
                if ( Boolean.parseBoolean( pd.getValue() ) )
                {
                    return true;
                }
            }
        }

        return false;
    }


    public BundleContext getContext()
    {
        return connectorContext;
    }
}
