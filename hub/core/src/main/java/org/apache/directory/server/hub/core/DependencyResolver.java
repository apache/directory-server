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
