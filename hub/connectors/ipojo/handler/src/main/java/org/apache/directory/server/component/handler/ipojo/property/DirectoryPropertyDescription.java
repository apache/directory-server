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


import org.apache.felix.ipojo.architecture.PropertyDescription;


/**
 * TODO DirectoryPropertyDescription.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirectoryPropertyDescription extends PropertyDescription
{

    private String m_containertype;
    private String m_description;
    private boolean m_constant;


    public DirectoryPropertyDescription( String name, String type, String value, String desc, String spec,
        boolean immutable )
    {
        super( name, type, value, immutable );
        m_containertype = spec;
        m_description = desc;
    }


    public DirectoryPropertyDescription( DirectoryProperty prop )
    {
        super( prop );

        m_containertype = prop.getContainerType();
        m_description = prop.getDescription();
    }


    public DirectoryPropertyDescription( boolean constant, String name, String value )
    {
        super( name, String.class.getName(), value );
        m_constant = constant;
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
    public String getDesc()
    {
        return m_description;
    }


    /**
     * Gets whether the property is constant
     *
     * @return true if property is constant
     */
    public boolean isConstant()
    {
        return m_constant;
    }

}
