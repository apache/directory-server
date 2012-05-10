package org.apache.directory.server.hub.connector.ipojo.core;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubConnector;
import org.apache.directory.server.hub.api.component.util.ComponentConstants;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DCOperationsManager;
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


    public void factoryActivated( ComponentFactory factory )
    {
        if ( isDirectoryFactory( factory ) )
        {
            DCMetadataDescriptor metadata = DCMetadataBuilder.generateDCMetadata( factory );
            DCOperationsManager operationsManager = new IPojoOperations( factory );

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
        PropertyDescription[] properties = factory.getComponentTypeDescription().getProperties();

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
