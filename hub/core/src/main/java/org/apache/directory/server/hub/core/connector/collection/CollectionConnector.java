package org.apache.directory.server.hub.core.connector.collection;


import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubConnector;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.meta.DCPropertyDescription;
import org.apache.directory.server.hub.api.meta.DCPropertyType;


public class CollectionConnector implements HubConnector
{
    public enum CollectionType
    {
        LIST,
        ARRAY,
        SET
    }


    @Override
    public void init( ComponentHub hub )
    {
        try
        {
            hub.connectHandler(
                CollectionMetaDescriptorGenerator.generateMetadataDescriptor( CollectionType.LIST ),
                new CollectionOperations( CollectionType.LIST ) );

            hub.connectHandler(
                CollectionMetaDescriptorGenerator.generateMetadataDescriptor( CollectionType.ARRAY ),
                new CollectionOperations( CollectionType.ARRAY ) );

            hub.connectHandler(
                CollectionMetaDescriptorGenerator.generateMetadataDescriptor( CollectionType.SET ),
                new CollectionOperations( CollectionType.SET ) );
        }
        catch ( HubAbortException e )
        {
            //TODO Log error
        }

    }

}
