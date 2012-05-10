package org.apache.directory.server.hub.api;

import org.apache.directory.server.hub.api.component.DCConfiguration;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DCOperationsManager;
import org.apache.directory.server.hub.api.registry.DCMetadataRegistry;
import org.apache.directory.server.hub.api.registry.DirectoryComponentRegistry;
import org.apache.directory.server.hub.api.registry.InjectionRegistry;
import org.apache.directory.server.hub.api.registry.PIDHandlerRegistry;




public interface ComponentHub
{

    public abstract void init() throws StoreNotValidException;


    public abstract void connectHandler( DCMetadataDescriptor metadata, DCOperationsManager operationsManager )
        throws HubAbortException;


    public abstract void disconnectHandler( String handlerPID );


    public abstract void updateComponentName( DirectoryComponent component, String newPID ) throws HubAbortException;


    public abstract void updateComponent( DirectoryComponent component, DCConfiguration newConfiguration )
        throws HubAbortException;


    public abstract void addComponent( DirectoryComponent component ) throws HubAbortException;


    public abstract void removeComponent( DirectoryComponent component ) throws HubAbortException;


    public abstract void addInjection( String injectionType, Object injection );


    public abstract void removeInjection( String injectionType );


    public abstract void registerClient( AbstractHubClient hubClient, String type );


    public abstract void unregisterClient( AbstractHubClient hubClient, String type );


    public abstract DirectoryComponentRegistry getDCRegistry();


    public abstract DCMetadataRegistry getMetaRegistry();


    public abstract InjectionRegistry getInjectionRegistry();


    public abstract PIDHandlerRegistry getPIDHandlerRegistry();

}