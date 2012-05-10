package org.apache.directory.server.hub.core;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.core.util.DCDependency;


public class DependencyResolver
{
    private Hashtable<DirectoryComponent, DCDependency> dependencyMap = new Hashtable();
    private MultiMap componentMap = new MultiValueMap();


    public void addDependencyHook( DirectoryComponent component, DCDependency dependency )
    {
        dependencyMap.put( component, dependency );
        componentMap.put( dependency, component );
    }


    public void clearDependencyHooks( DirectoryComponent component )
    {
        DCDependency dep = dependencyMap.remove( component );
        componentMap.remove( dep, component );
    }


    public List<DirectoryComponent> getWaiting( DCDependency dependency )
    {
        Collection<DirectoryComponent> waiting = ( Collection<DirectoryComponent> ) componentMap.remove( dependency );

        if ( waiting != null )
        {
            return new ArrayList<DirectoryComponent>( waiting );
        }
        else
        {
            return null;
        }
    }
}
