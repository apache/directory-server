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

package org.apache.directory.server.component.handler.ipojo;


import java.util.List;

import org.apache.directory.server.component.handler.ipojo.property.DirectoryProperty;
import org.apache.directory.server.component.handler.ipojo.property.DirectoryPropertyDescription;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Property;


/**
 * TODO DirectoryComponentHandlerDescription.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirectoryComponentHandlerDescription extends HandlerDescription
{

    /**
     * The property descriptions.
     */
    private DirectoryPropertyDescription[] m_properties;


    /**
     * Creates the description object for the configuration handler description.
     * @param handler the configuration handler.
     * @param props the list of properties.
     * @param pid the managed service pid or <code>null</code> if not set.
     */
    public DirectoryComponentHandlerDescription( Handler handler, List props )
    {
        super( handler );
        m_properties = new DirectoryPropertyDescription[props.size()];
        for ( int i = 0; i < props.size(); i++ )
        {
            m_properties[i] = new DirectoryPropertyDescription( ( DirectoryProperty ) props.get( i ) );
        }
    }


    /**
     * The handler information.
     * @return the handler description.
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo()
    {
        Element elem = super.getHandlerInfo();

        for ( int i = 0; i < m_properties.length; i++ )
        {
            String name = m_properties[i].getName();
            Object value = m_properties[i].getValue();
            Element property = new Element( "directoryproperty", "" );
            elem.addElement( property );
            if ( name != null )
            {
                property.addAttribute( new Attribute( "name", name ) );
            }
            if ( value != null )
            {
                if ( value == Property.NO_VALUE )
                {
                    property.addAttribute( new Attribute( "value", "NO_VALUE" ) );
                }
                else
                {
                    property.addAttribute( new Attribute( "value", value.toString() ) );
                }
            }
        }
        return elem;
    }


    /**
     * Gets the directory properties.
     * @return the directory property set.
     */
    public DirectoryPropertyDescription[] getProperties()
    {
        return m_properties;
    }
}
