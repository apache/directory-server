package org.apache.directory.server.hub.core.meta;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.hub.api.component.util.ComponentConstants;
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DCPropertyDescription;
import org.apache.directory.server.hub.api.meta.DCPropertyType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public class DCMetadataNormalizer
{
    public static void normalizeDCMetadata( DCMetadataDescriptor metadata )
    {
        for ( DCPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( isPrimitive( pd.getType() ) )
            {
                pd.setPropertyContext( DCPropertyType.PRIMITIVE );
                if ( pd.getDefaultValue() == null )
                {
                    pd.setDefaultValue( "-1" );
                }
            }
            else if ( isInjection( pd.getType() ) )
            {
                pd.setPropertyContext( DCPropertyType.INJECTION );
            }
            else if ( isCollection( pd.getType() ) )
            {
                if ( isPrimitive( pd.getContainerFor() ) )
                {
                    pd.setPropertyContext( DCPropertyType.PRIMITIVE_COLLECTION );
                    if ( pd.getDefaultValue() == null )
                    {
                        pd.setDefaultValue( "[]" );
                    }
                }
                else
                {
                    pd.setPropertyContext( DCPropertyType.COLLECTION );
                    if ( pd.getDefaultValue() == null )
                    {
                        pd.setDefaultValue( "null" );
                    }
                }
            }
            else
            {
                pd.setPropertyContext( DCPropertyType.REFERENCE );
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
            SchemaManager.class.getName()
    };
}
