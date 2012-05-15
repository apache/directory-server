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
import java.util.List;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.core.util.ParentLink;


public class ParentLinkRegistry
{
    private MultiMap parentLinks = new MultiValueMap();
    private MultiMap childLinks = new MultiValueMap();


    public void addParentLink( DirectoryComponent component, ParentLink link )
    {
        parentLinks.put( component, link );
        childLinks.put( link.getParent(), component );
    }


    public List<ParentLink> getParentLinks( DirectoryComponent component )
    {
        Collection<ParentLink> parents = ( Collection<ParentLink> ) parentLinks.get( component );
        if ( parents != null )
        {
            return new ArrayList<ParentLink>( parents );
        }
        else
        {
            return null;
        }
    }


    /**
     * Destroy any links pointing to given component
     *
     * @param component
     */
    public void destroyComponentLinks( DirectoryComponent component )
    {
        Collection<DirectoryComponent> childs = ( Collection ) childLinks.remove( component );

        if ( childs != null )
        {
            for ( DirectoryComponent child : childs )
            {
                Collection<ParentLink> links = new ArrayList<ParentLink>(
                    ( Collection<ParentLink> ) parentLinks.get( child ) );

                for ( ParentLink link : links )
                {
                    if ( link.getParent().equals( component ) )
                    {
                        parentLinks.remove( child, link );
                    }
                }
            }
        }
    }
}
