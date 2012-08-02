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

package org.apache.directory.server.component.handler.ipojo.property;


import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Property;


/**
 * DirectoryProperty implementation for IPojo DirectoryComponent handler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirectoryProperty extends Property
{
    /** Container Type specification for generic collection types */
    private String m_containertype;

    /** Description of a DirectoryProperty */
    private String m_description;


    public DirectoryProperty( String name, String field, String method,
        int index, String value, String type, String description, String containertype, InstanceManager manager,
        Handler handler ) throws ConfigurationException
    {
        super( name, field, method, index, value, type, manager, handler );
        m_containertype = containertype;
        m_description = description;
    }


    /**
     * Gets the container type parameter of property.
     *
     * @return Container type as String
     */
    public String getContainerType()
    {
        return m_containertype;
    }


    /**
     * Gets the description parameter of property
     *
     * @return Property description
     */
    public String getDescription()
    {
        return m_description;
    }

}
