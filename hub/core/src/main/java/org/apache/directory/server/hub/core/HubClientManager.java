package org.apache.directory.server.hub.core;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.directory.server.hub.api.AbstractHubClient;
import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;


public class HubClientManager
{
    private ComponentHub hub;

    private MultiMap typeRegistrations = new MultiValueMap();
    private MultiMap clientRegistrations = new MultiValueMap();


    public synchronized void registerHubClient( AbstractHubClient hubClient, String type )
    {
        typeRegistrations.put( type, hubClient );
        clientRegistrations.put( hubClient, type );

        List<DirectoryComponent> existing = hub.getDCRegistry().getComponents();
        if ( existing == null )
        {
            return;
        }

        for ( DirectoryComponent component : existing )
        {
            DCMetadataDescriptor metadata = hub.getMetaRegistry().getMetadataDescriptor(
                component.getComponentManagerPID() );
            if ( metadata.is( type ) )
            {
                if ( component.getRuntimeInfo() != null )
                {
                    hubClient.componentActivated( component );
                }
            }
        }
    }


    public void unregisterHubClient( AbstractHubClient hubClient, String type )
    {
        if ( type == null )
        {
            Collection<String> registeredTypes = ( Collection ) clientRegistrations.remove( hubClient );
            if ( registeredTypes != null )
            {
                for ( String reg : registeredTypes )
                {
                    typeRegistrations.remove( reg, hubClient );
                }
            }
        }
        else
        {
            typeRegistrations.remove( type, hubClient );
            clientRegistrations.remove( hubClient, type );
        }
    }


    public HubClientManager( ComponentHub hub )
    {
        this.hub = hub;
    }


    public synchronized void fireDCActivated( DirectoryComponent component )
    {
        List<AbstractHubClient> clients = getRegisteredClients( component );
        for ( AbstractHubClient client : clients )
        {
            client.componentActivated( component );
        }
    }


    public void fireDCDeactivating( DirectoryComponent component )
    {
        List<AbstractHubClient> clients = getRegisteredClients( component );
        for ( AbstractHubClient client : clients )
        {
            client.componentDeactivating( component );
        }
    }


    public void fireDCRemoving( DirectoryComponent component ) throws HubAbortException
    {
        List<AbstractHubClient> clients = getRegisteredClients( component );
        for ( AbstractHubClient client : clients )
        {
            client.componentRemoving( component );
        }
    }


    public void fireDCReconfigured( DirectoryComponent component )
    {
        List<AbstractHubClient> clients = getRegisteredClients( component );
        for ( AbstractHubClient client : clients )
        {
            client.componentReconfigured( component );
        }
    }


    public List<AbstractHubClient> getRegisteredClients( DirectoryComponent component )
    {
        List<AbstractHubClient> registeredClients = new ArrayList<AbstractHubClient>();

        DCMetadataDescriptor metadata = hub.getMetaRegistry()
            .getMetadataDescriptor( component.getComponentManagerPID() );

        Set<String> registeredTypes = typeRegistrations.keySet();
        for ( String type : registeredTypes )
        {
            if ( metadata.is( type ) )
            {
                registeredClients.addAll( ( Collection ) typeRegistrations.get( type ) );
            }
        }

        return registeredClients;
    }
}
