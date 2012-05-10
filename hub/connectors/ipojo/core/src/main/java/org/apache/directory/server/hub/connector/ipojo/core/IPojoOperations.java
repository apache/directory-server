package org.apache.directory.server.hub.connector.ipojo.core;


import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.directory.server.hub.api.component.DCProperty;
import org.apache.directory.server.hub.api.component.DCRuntime;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.ComponentInstantiationException;
import org.apache.directory.server.hub.api.exception.ComponentReconfigurationException;
import org.apache.directory.server.hub.api.meta.DCOperationsManager;
import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;


public class IPojoOperations implements DCOperationsManager
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
        for ( DCProperty prop : component.getConfiguration() )
        {
            configuration.put( prop.getName(), prop.getObject() );
        }

        configuration.put( "instance.name", component.getComponentPID() );

        try
        {
            ComponentInstance instance = factory.createComponentInstance( configuration );

            InstanceManager manager = ( InstanceManager ) instance;
            Object pojo = manager.getPojoObject();

            component.setRuntimeInfo( new DCRuntime( manager, pojo ) );
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
        DCRuntime runtime = component.getRuntimeInfo();
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
        for ( DCProperty prop : component.getConfiguration() )
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
        DCRuntime runtime = component.getRuntimeInfo();
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
