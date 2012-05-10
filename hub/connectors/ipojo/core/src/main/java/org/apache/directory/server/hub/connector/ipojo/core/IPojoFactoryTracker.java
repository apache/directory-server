package org.apache.directory.server.hub.connector.ipojo.core;


import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.FactoryStateListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class IPojoFactoryTracker implements FactoryStateListener, ServiceTrackerCustomizer
{
    BundleContext context;
    ServiceTracker tracker;
    IPojoConnector connector;


    public IPojoFactoryTracker( IPojoConnector connector )
    {
        this.connector = connector;
        context = connector.getContext();

        tracker = new ServiceTracker( context, Factory.class.getName(), this );
    }


    public void open()
    {
        tracker.open();
    }


    public void close()
    {
        tracker.close();
    }


    @Override
    public void stateChanged( Factory factory, int state )
    {
        ComponentFactory componentFactory = ( ComponentFactory ) factory;

        if ( state == Factory.VALID )
        {
            connector.factoryActivated( componentFactory );
        }
        else if ( state == Factory.INVALID )
        {
            connector.factoryDeactivating( componentFactory );
        }

    }


    @Override
    public Object addingService( ServiceReference reference )
    {
        Factory factory = context.getService( ( ServiceReference<Factory> ) reference );

        ComponentFactory componentFactory = ( ComponentFactory ) factory;
        if ( componentFactory == null )
        {
            return null;
        }
        
        System.out.println(factory.getName()+"adding");

        componentFactory.addFactoryStateListener( this );

        if ( componentFactory.getState() == Factory.VALID )
        {
            connector.factoryActivated( componentFactory );
        }

        return reference;

    }


    @Override
    public void modifiedService( ServiceReference reference, Object service )
    {
    }


    @Override
    public void removedService( ServiceReference reference, Object service )
    {
    }

}
