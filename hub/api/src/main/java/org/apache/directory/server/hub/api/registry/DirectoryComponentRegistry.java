package org.apache.directory.server.hub.api.registry;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.directory.server.hub.api.component.DirectoryComponent;


public class DirectoryComponentRegistry
{
    private Hashtable<String, DirectoryComponent> pidMap = new Hashtable<String, DirectoryComponent>();
    private MultiMap handlerPIDMap = new MultiValueMap();
    private Hashtable<String, DirectoryComponent> dnMap = new Hashtable<String, DirectoryComponent>();


    public void addDirectoryComponent( DirectoryComponent component )
    {
        pidMap.put( component.getComponentPID(), component );
        handlerPIDMap.put( component.getComponentManagerPID(), component );
        dnMap.put( component.getConfigLocation(), component );
    }


    public void addDirectoryComponent( List<DirectoryComponent> components )
    {
        for ( DirectoryComponent component : components )
        {
            addDirectoryComponent( component );
        }
    }


    public void removeDirectoryComponent( DirectoryComponent component )
    {
        pidMap.remove( component.getComponentPID() );
        dnMap.remove( component.getConfigLocation() );
        handlerPIDMap.remove( component.getComponentManagerPID(), component );
    }


    public DirectoryComponent getComponentByReference( String componentPID )
    {
        return pidMap.get( componentPID );
    }


    public DirectoryComponent getComponentByLocation( String location )
    {
        return dnMap.get( location );
    }


    public List<DirectoryComponent> getComponents( String handlerPID )
    {
        Collection<DirectoryComponent> components = ( Collection ) handlerPIDMap.get( handlerPID );
        if ( components != null )
        {
            return new ArrayList<DirectoryComponent>( components );
        }
        else
        {
            return null;
        }

    }


    public List<DirectoryComponent> getComponents()
    {
        Collection<DirectoryComponent> components = ( Collection ) pidMap.values();
        if ( components != null )
        {
            return new ArrayList<DirectoryComponent>( components );
        }
        else
        {
            return null;
        }

    }


    public void changeComponentReference( DirectoryComponent component, String newName )
    {
        removeDirectoryComponent( component );
        component.setComponentName( newName );
        addDirectoryComponent( component );
    }


    public void changeComponentLocation( DirectoryComponent component, String newLocation )
    {
        removeDirectoryComponent( component );
        component.setConfigLocation( newLocation );
        addDirectoryComponent( component );
    }
}
