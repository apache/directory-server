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

package org.apache.directory.server.hub.api.component;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


public class DCConfiguration implements Iterable<DCProperty>
{
    private List<DCProperty> properties;
    private Hashtable<String, DCProperty> propertyMap;
    private Integer collectionIndex = null;

    private Properties constantProperties = new Properties();


    public DCConfiguration( List<DCProperty> properties )
    {
        this.properties = properties;
        propertyMap = new Hashtable<String, DCProperty>();

        for ( DCProperty property : properties )
        {
            propertyMap.put( property.getName(), property );
        }
    }


    public DCConfiguration( DCConfiguration configuration )
    {
        properties = new ArrayList<DCProperty>();
        propertyMap = new Hashtable<String, DCProperty>();

        for ( DCProperty prop : configuration )
        {
            addProperty( new DCProperty( prop.getName(), prop.getValue() ) );
        }

        collectionIndex = configuration.getCollectionIndex();
    }


    @Override
    public Iterator<DCProperty> iterator()
    {
        return properties.iterator();
    }


    public void addProperty( DCProperty property )
    {
        properties.add( property );
        propertyMap.put( property.getName(), property );
    }


    public void removeProperty( String propertyName )
    {
        DCProperty removing = propertyMap.remove( propertyName );
        if ( removing != null )
        {
            properties.remove( removing );
        }
    }


    public DCProperty getProperty( String propertyName )
    {
        return propertyMap.get( propertyName );
    }


    public Integer getCollectionIndex()
    {
        return collectionIndex;
    }


    public void setCollectionIndex( Integer collectionIndex )
    {
        this.collectionIndex = collectionIndex;
    }


    public void addConstant( String name, Object value )
    {
        constantProperties.put( name, value );
    }


    public Object getConstantProperty( String name )
    {
        return constantProperties.get( name );
    }

}
