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

package org.apache.directory.server.hub.core.connector.collection;


import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.hub.api.component.DirectoryComponentConstants;
import org.apache.directory.server.hub.api.component.util.ComponentConstants;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DcPropertyDescription;
import org.apache.directory.server.hub.core.connector.collection.CollectionConnector.CollectionType;
import org.osgi.framework.Version;


public class CollectionMetaDescriptorGenerator
{
    public static DcMetadataDescriptor generateMetadataDescriptor( CollectionType type )
    {
        switch ( type )
        {
            case LIST:
                return generateForList();
            case ARRAY:
                return generateForArray();
            case SET:
                return generateForSet();
            default:
                return null;
        }
    }


    private static DcMetadataDescriptor generateForList()
    {
        String metaPid = DirectoryComponentConstants.DC_COLL_OC_LIST;
        Version metaVersion = new Version( "2.0.0" );
        String className = ArrayList.class.getName();
        String[] implemented = new String[]
            { List.class.getName(), Collection.class.getName() };
        String[] extended = new String[]
            { AbstractList.class.getName() };
        DcPropertyDescription[] properties = new DcPropertyDescription[]
            { new DcPropertyDescription( DirectoryComponentConstants.DC_LIST_PROP_TYPE,
                ComponentConstants.PRIMITIVE_STR, Object.class.getName(), "Specifies collection's container type",
                true, false, DirectoryComponentConstants.DC_VAL_NULL ) };

        return new DcMetadataDescriptor( metaPid, true, metaVersion, className, implemented, extended, null,
            properties );
    }


    private static DcMetadataDescriptor generateForSet()
    {
        String metaPid = DirectoryComponentConstants.DC_COLL_OC_SET;
        Version metaVersion = new Version( "2.0.0" );
        String className = HashSet.class.getName();
        String[] implemented = new String[]
            { Set.class.getName(), Collection.class.getName() };
        String[] extended = new String[]
            { AbstractSet.class.getName() };
        DcPropertyDescription[] properties = new DcPropertyDescription[]
            { new DcPropertyDescription( DirectoryComponentConstants.DC_SET_PROP_TYPE,
                ComponentConstants.PRIMITIVE_STR, Object.class.getName(), "Specifies collection's container type",
                true, false, DirectoryComponentConstants.DC_VAL_NULL ) };

        return new DcMetadataDescriptor( metaPid, true, metaVersion, className, implemented, extended, null,
            properties );
    }


    private static DcMetadataDescriptor generateForArray()
    {
        String metaPid = DirectoryComponentConstants.DC_COLL_OC_ARRAY;
        Version metaVersion = new Version( "2.0.0" );
        String className = Array.class.getName();
        String[] implemented = new String[0];
        String[] extended = new String[0];
        DcPropertyDescription[] properties = new DcPropertyDescription[]
            { new DcPropertyDescription( DirectoryComponentConstants.DC_ARRAY_PROP_TYPE,
                ComponentConstants.PRIMITIVE_STR, Object.class.getName(), "Specifies collection's container type",
                true, false, DirectoryComponentConstants.DC_VAL_NULL ) };

        return new DcMetadataDescriptor( metaPid, true, metaVersion, className, implemented, extended, null,
            properties );
    }

}
