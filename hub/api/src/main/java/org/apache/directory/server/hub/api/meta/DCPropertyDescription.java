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

package org.apache.directory.server.hub.api.meta;


public class DCPropertyDescription
{
    private DCPropertyType propertyContext;
    private String name;
    private String type;
    private String defaultValue;
    private String description;
    private boolean mandatory;
    private boolean immutable;
    private String containerFor;


    public DCPropertyDescription( String name, String type, String defaultValue, String description, boolean mandatory,
        boolean immutable, String containerFor )
    {
        this( null, name, type, defaultValue, description, mandatory, immutable, containerFor );
    }


    public DCPropertyDescription( DCPropertyType propertyContext, String name, String type, String defaultValue,
        String description, boolean mandatory, boolean immutable, String containerFor )
    {
        this.propertyContext = propertyContext;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
        this.mandatory = mandatory;
        this.immutable = immutable;
        this.containerFor = containerFor;
    }


    public String getName()
    {
        return name;
    }


    public String getType()
    {
        return type;
    }


    public String getDefaultValue()
    {
        return defaultValue;
    }


    public void setDefaultValue( String value )
    {
        this.defaultValue = value;
    }


    public String getDescription()
    {
        return description;
    }


    public boolean isMandatory()
    {
        return mandatory;
    }


    public String getContainerFor()
    {
        return containerFor;
    }


    public DCPropertyType getPropertyContext()
    {
        return propertyContext;
    }


    public void setPropertyContext( DCPropertyType propertyContext )
    {
        this.propertyContext = propertyContext;
    }

}
