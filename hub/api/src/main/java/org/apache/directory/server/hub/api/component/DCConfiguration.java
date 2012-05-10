package org.apache.directory.server.hub.api.component;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;



public class DCConfiguration implements Iterable<DCProperty>
{
    private List<DCProperty> properties;
    private Hashtable<String, DCProperty> propertyMap;
    private Integer collectionIndex = null;


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

}
