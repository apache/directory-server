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
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DCPropertyDescription;
import org.apache.directory.server.hub.core.connector.collection.CollectionConnector.CollectionType;
import org.osgi.framework.Version;


public class CollectionMetaDescriptorGenerator
{
    public static DCMetadataDescriptor generateMetadataDescriptor( CollectionType type )
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


    private static DCMetadataDescriptor generateForList()
    {
        String metaPid = DirectoryComponentConstants.DC_COLL_OC_LIST;
        Version metaVersion = new Version( "2.0.0" );
        String className = ArrayList.class.getName();
        String[] implemented = new String[]
            { List.class.getName(), Collection.class.getName() };
        String[] extended = new String[]
            { AbstractList.class.getName() };
        DCPropertyDescription[] properties = new DCPropertyDescription[]
            { new DCPropertyDescription( DirectoryComponentConstants.DC_LIST_PROP_TYPE,
                ComponentConstants.PRIMITIVE_STR, Object.class.getName(), "Specifies collection's container type",
                true, DirectoryComponentConstants.DC_VAL_NULL ) };

        return new DCMetadataDescriptor( metaPid, true, metaVersion, className, implemented, extended, properties );
    }


    private static DCMetadataDescriptor generateForSet()
    {
        String metaPid = DirectoryComponentConstants.DC_COLL_OC_SET;
        Version metaVersion = new Version( "2.0.0" );
        String className = HashSet.class.getName();
        String[] implemented = new String[]
            { Set.class.getName(), Collection.class.getName() };
        String[] extended = new String[]
            { AbstractSet.class.getName() };
        DCPropertyDescription[] properties = new DCPropertyDescription[]
            { new DCPropertyDescription( DirectoryComponentConstants.DC_SET_PROP_TYPE,
                ComponentConstants.PRIMITIVE_STR, Object.class.getName(), "Specifies collection's container type",
                true, DirectoryComponentConstants.DC_VAL_NULL ) };

        return new DCMetadataDescriptor( metaPid, true, metaVersion, className, implemented, extended, properties );
    }


    private static DCMetadataDescriptor generateForArray()
    {
        String metaPid = DirectoryComponentConstants.DC_COLL_OC_ARRAY;
        Version metaVersion = new Version( "2.0.0" );
        String className = Array.class.getName();
        String[] implemented = new String[0];
        String[] extended = new String[0];
        DCPropertyDescription[] properties = new DCPropertyDescription[]
            { new DCPropertyDescription( DirectoryComponentConstants.DC_ARRAY_PROP_TYPE,
                ComponentConstants.PRIMITIVE_STR, Object.class.getName(), "Specifies collection's container type",
                true, DirectoryComponentConstants.DC_VAL_NULL ) };

        return new DCMetadataDescriptor( metaPid, true, metaVersion, className, implemented, extended, properties );
    }

}
