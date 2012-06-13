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

package org.apache.directory.server.hub.connector.ipojo.core;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.directory.server.component.handler.ipojo.DcHandlerConstants;
import org.apache.directory.server.component.handler.ipojo.property.DirectoryPropertyDescription;
import org.apache.directory.server.hub.api.component.util.ComponentConstants;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DcPropertyDescription;
import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.Version;


public class DcMetadataBuilder
{
    public static DcMetadataDescriptor generateDCMetadata( ComponentFactory factory )
    {
        String metadataPID = factory.getName();
        Version metaVersion = factory.getBundleContext().getBundle().getVersion();
        boolean isFactory = true;

        boolean isExclusive = false;
        boolean isThreadSafe = true;

        List<DcPropertyDescription> properties = new ArrayList<DcPropertyDescription>();

        Hashtable<String, String> attributes = new Hashtable<String, String>();

        for ( PropertyDescription property : factory.getComponentDescription().getProperties() )
        {
            String name = property.getName();
            String defaultValue = property.getValue();
            boolean mandatory = property.isMandatory();
            boolean immutable = property.isImmutable();
            boolean constant = false;
            String type = normalizeType( property.getType() );
            String description = "";
            String containerFor = "";

            DirectoryPropertyDescription dpd = null;
            if ( property instanceof DirectoryPropertyDescription )
            {
                dpd = ( DirectoryPropertyDescription ) property;
            }

            if ( dpd != null )
            {
                description = dpd.getDesc();
                containerFor = normalizeType( dpd.getContainerType() );
                constant = dpd.isConstant();
            }

            if ( immutable )
            {
                // Immutable components must be mandatory too.
                mandatory = true;
            }

            if ( constant )
            {
                if ( name.equals( DcHandlerConstants.META_IS_FACTORY ) )
                {
                    isFactory = Boolean.parseBoolean( defaultValue );
                }
                else if ( name.equals( DcHandlerConstants.META_IS_EXCLUSIVE ) )
                {
                    isExclusive = Boolean.parseBoolean( defaultValue );
                }
                else if ( name.equals( DcHandlerConstants.META_IS_THREAD_SAFE ) )
                {
                    isThreadSafe = Boolean.parseBoolean( defaultValue );
                }
                else
                {
                    attributes.put( name, defaultValue );
                }
            }
            else
            {
                properties.add( new DcPropertyDescription( name, type,
                    defaultValue, description, mandatory, immutable, containerFor ) );
            }
        }

        ComponentTypeDescription typeDescription = factory.getComponentTypeDescription();
        Element desc = typeDescription.getDescription();

        String className = desc.getAttribute( "Implementation-Class" );

        Element inheritance = desc.getElements( "Inherited" )[0];

        String interfaces = inheritance.getAttribute( "Interfaces" );
        String sclasses = inheritance.getAttribute( "SuperClasses" );

        String[] implemented = parseArray( interfaces );
        String[] extended = parseArray( sclasses );

        DcMetadataDescriptor metadata = new DcMetadataDescriptor( metadataPID, isFactory, metaVersion,
            className,
            implemented, extended, attributes, properties.toArray( new DcPropertyDescription[0] ) );

        metadata.setExclusive( isExclusive );
        metadata.setThreadSafe( isThreadSafe );

        return metadata;
    }


    private static String[] parseArray( String array )
    {
        if ( !( array.contains( "[" ) && array.contains( "]" ) ) )
        {
            return new String[0];
        }

        array = array.substring( 1, array.length() - 1 );

        String[] splitted = array.split( "," );

        for ( int i = 0; i < splitted.length; i++ )
        {
            splitted[i] = splitted[i].trim();
        }

        return splitted;
    }


    private static String normalizeType( String type )
    {
        if ( type == null )
        {
            return "";
        }

        if ( type.endsWith( "[]" ) )
        {
            return Array.class.getName();
        }
        else
        {
            if ( "string".equals( type ) || "String".equals( type ) || String.class.getName().equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_STR;
            }
            else if ( "boolean".equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_BOOL;
            }
            else if ( "byte".equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_INT;
            }
            else if ( "short".equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_INT;
            }
            else if ( "int".equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_INT;
            }
            else if ( "long".equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_INT;
            }
            else if ( "float".equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_FLOAT;
            }
            else if ( "double".equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_FLOAT;
            }
            else if ( "char".equals( type ) )
            {
                return ComponentConstants.PRIMITIVE_INT;
            }
            else
                return type;
        }
    }
}
