package org.apache.directory.server.hub.api;


import java.util.List;

import org.apache.directory.server.hub.api.component.DCConfiguration;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.HubStoreException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;



public interface HubStore
{

    public void init( ComponentHub hub ) throws StoreNotValidException;


    List<DCMetadataDescriptor> getMetadataDescriptors() throws HubStoreException;


    List<DirectoryComponent> getComponents() throws HubStoreException;


    void installMetadataDescriptor( DCMetadataDescriptor metadata ) throws HubStoreException;


    void updateMetadataDescriptor( DCMetadataDescriptor oldMetadata, DCMetadataDescriptor newMetadata )
        throws HubStoreException;


    void uninstallMetadataDescriptor( DCMetadataDescriptor metadata ) throws HubStoreException;


    void installComponent( DirectoryComponent component ) throws HubStoreException;


    void updateComponent( DirectoryComponent component, DCConfiguration newConfiguration ) throws HubStoreException;


    void uninstallComponent( DirectoryComponent component ) throws HubStoreException;
}
