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

package org.apache.directory.server.hub.core.meta;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.hub.api.component.util.ComponentConstants;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DcPropertyDescription;
import org.apache.directory.server.hub.api.meta.DcPropertyType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public class DcMetadataNormalizer
{
    public static void normalizeDCMetadata( DcMetadataDescriptor metadata )
    {
        for ( DcPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( isPrimitive( pd.getType() ) )
            {
                pd.setPropertyContext( DcPropertyType.PRIMITIVE );
                if ( pd.getDefaultValue() == null )
                {
                    pd.setDefaultValue( "-1" );
                }
            }
            else if ( isInjection( pd.getType() ) )
            {
                pd.setPropertyContext( DcPropertyType.INJECTION );
            }
            else if ( isCollection( pd.getType() ) )
            {
                if ( pd.getContainerFor() == null || isPrimitive( pd.getContainerFor() ) )
                {
                    pd.setPropertyContext( DcPropertyType.PRIMITIVE_COLLECTION );
                    if ( pd.getDefaultValue() == null )
                    {
                        pd.setDefaultValue( "[]" );
                    }
                }
                else
                {
                    pd.setPropertyContext( DcPropertyType.COLLECTION );
                    if ( pd.getDefaultValue() == null )
                    {
                        pd.setDefaultValue( "null" );
                    }
                }
            }
            else
            {
                pd.setPropertyContext( DcPropertyType.REFERENCE );
                if ( pd.getDefaultValue() == null )
                {
                    pd.setDefaultValue( "null" );
                }
            }
        }
    }


    private static boolean isPrimitive( String type )
    {
        if ( type.equals( ComponentConstants.PRIMITIVE_STR )
            || type.equals( ComponentConstants.PRIMITIVE_INT )
            || type.equals( ComponentConstants.PRIMITIVE_FLOAT )
            || type.equals( ComponentConstants.PRIMITIVE_BOOL ) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    private static boolean isInjection( String type )
    {
        for ( String injectionType : injectionTypes )
        {
            if ( injectionType.equals( type ) )
            {
                return true;
            }
        }

        return false;
    }


    private static boolean isCollection( String type )
    {
        if ( type.equals( Collection.class.getName() )
            || type.equals( List.class.getName() )
            || type.equals( ArrayList.class.getName() )
            || type.equals( Set.class.getName() )
            || type.equals( HashSet.class.getName() )
            || type.equals( Array.class.getName() ) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private static String[] injectionTypes = new String[]
        {
            DirectoryService.class.getName(),
            SchemaManager.class.getName(),
            InstanceLayout.class.getName(),
            "org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer"
    };
}
