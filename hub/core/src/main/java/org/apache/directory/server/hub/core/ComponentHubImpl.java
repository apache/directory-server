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

package org.apache.directory.server.hub.core;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.hub.api.AbstractHubClient;
import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubStore;
import org.apache.directory.server.hub.api.component.DcConfiguration;
import org.apache.directory.server.hub.api.component.DcProperty;
import org.apache.directory.server.hub.api.component.DcRuntime;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.component.DirectoryComponentConstants;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.IPojoComponentConstants;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.directory.server.hub.api.exception.BadConfigurationException;
import org.apache.directory.server.hub.api.exception.ComponentInstantiationException;
import org.apache.directory.server.hub.api.exception.ComponentReconfigurationException;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.exception.HubStoreException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DcOperationsManager;
import org.apache.directory.server.hub.api.meta.DcPropertyDescription;
import org.apache.directory.server.hub.api.meta.DcPropertyType;
import org.apache.directory.server.hub.api.registry.DcMetadataRegistry;
import org.apache.directory.server.hub.api.registry.DirectoryComponentRegistry;
import org.apache.directory.server.hub.api.registry.InjectionRegistry;
import org.apache.directory.server.hub.api.registry.PidHandlerRegistry;
import org.apache.directory.server.hub.connector.ipojo.core.IPojoConnector;
import org.apache.directory.server.hub.core.configurator.ConfiguratorInterceptor;
import org.apache.directory.server.hub.core.connector.collection.CollectionConnector;
import org.apache.directory.server.hub.core.meta.DcMetadataNormalizer;
import org.apache.directory.server.hub.core.util.DCDependency;
import org.apache.directory.server.hub.core.util.ParentLink;
import org.apache.directory.server.hub.core.util.DCDependency.DCDependencyType;
import org.apache.felix.ipojo.IPojoContext;
import org.osgi.framework.Version;


public class ComponentHubImpl implements ComponentHub
{
    // Registries
    private DirectoryComponentRegistry componentsReg = new DirectoryComponentRegistry();
    private DcMetadataRegistry metadatasReg = new DcMetadataRegistry();
    private InjectionRegistry injectionsReg = new InjectionRegistry();
    private PidHandlerRegistry handlersReg = new PidHandlerRegistry();
    private ParentLinkRegistry parentLinksReg = new ParentLinkRegistry();

    private CollectionConnector collectionConnector;
    public IPojoConnector ipojoConnector;

    private HubStore store;

    private HubClientManager clientManager = new HubClientManager( this );

    private DependencyResolver dependencyResolver = new DependencyResolver();

    private ConfiguratorInterceptor configurator;


    public ComponentHubImpl( HubStore store )
    {
        this.store = store;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#init()
     */
    @Override
    public void init() throws StoreNotValidException
    {
        store.init( this );

        try
        {
            List<DcMetadataDescriptor> metadatas = store.getMetadataDescriptors();
            metadatasReg.addMetadataDescriptor( metadatas );

            for ( DcMetadataDescriptor metadata : metadatas )
            {
                DcMetadataNormalizer.normalizeDCMetadata( metadata );
            }

            List<DirectoryComponent> components = store.getComponents();
            for ( DirectoryComponent component : components )
            {
                setInjectionProperties( metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() ),
                    component.getConfiguration() );
                componentsReg.addDirectoryComponent( component );
            }
        }
        catch ( HubStoreException e )
        {
            throw new StoreNotValidException( "HubStore is corrupted" );
        }

        collectionConnector = new CollectionConnector();
        collectionConnector.init( this );

        ipojoConnector = new IPojoConnector();
        ipojoConnector.init( this );

        insertConfiguratorInterceptor();

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#connectHandler(org.apache.directory.server.hub.component.meta.DCMetadataDescriptor, org.apache.directory.server.hub.component.meta.DCOperationsManager)
     */
    @Override
    public void connectHandler( DcMetadataDescriptor metadata, DcOperationsManager operationsManager )
        throws HubAbortException
    {
        DcMetadataDescriptor existingMetadata = metadatasReg.getMetadataDescriptor( metadata.getMetadataPID() );

        DcMetadataNormalizer.normalizeDCMetadata( metadata );

        if ( existingMetadata == null )
        {

            try
            {
                store.installMetadataDescriptor( metadata );

                metadatasReg.addMetadataDescriptor( metadata );
                handlersReg.setPIDHandler( metadata.getMetadataPID(), operationsManager );

                return;
            }
            catch ( HubStoreException e )
            {
                throw new HubAbortException( "Store rejected metadata descriptor", e );
            }
        }

        if ( !existingMetadata.compatibleWith( metadata ) )
        {
            try
            {
                /* 
                 * Updating store with new metadata might cause existing components(Not instantiated yet!) to be updated.
                 * Updates will be propagated to ComponentHub from Store.
                 */
                store.updateMetadataDescriptor( existingMetadata, metadata );
            }
            catch ( HubStoreException e )
            {
                throw new HubAbortException( "Store raised an error while updating metadata:"
                    + metadata.getMetadataPID(), e );
            }

        }

        /*
         * This call will set metadata with constant properties.
         * Stored metadata is kept until this point for new component configurations.
         */
        metadatasReg.addMetadataDescriptor( metadata );

        handlersReg.setPIDHandler( metadata.getMetadataPID(), operationsManager );
        activateHandler( metadata.getMetadataPID() );

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#disconnectHandler(java.lang.String)
     */
    @Override
    public void disconnectHandler( String handlerPID )
    {
        DcMetadataDescriptor meta = metadatasReg.getMetadataDescriptor( handlerPID );
        if ( meta != null )
        {
            deactivateHandler( handlerPID );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#updateComponentName(org.apache.directory.server.hub.component.DirectoryComponent, java.lang.String)
     */
    @Override
    public void updateComponentName( DirectoryComponent component, String newPID ) throws HubAbortException
    {
        List<ParentLink> parentLinks = parentLinksReg.getParentLinks( component );
        if ( parentLinks != null )
        {
            throw new HubAbortException(
                "You can't change name of component which is being referenced by other component" );
        }

        // Which also sets DC's new Name
        componentsReg.changeComponentReference( component, newPID );

        List<DirectoryComponent> waitingComponents = dependencyResolver
            .getWaiting( new DCDependency( DCDependencyType.REFERENCE, component
                .getComponentPID() ) );

        if ( waitingComponents != null )
        {
            for ( DirectoryComponent dependent : waitingComponents )
            {
                instantiateComponent( dependent );
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#updateComponent(org.apache.directory.server.hub.component.DirectoryComponent, org.apache.directory.server.hub.component.DCConfiguration)
     */
    @Override
    public void updateComponent( DirectoryComponent component, DcConfiguration newConfiguration )
        throws HubAbortException
    {
        DcMetadataDescriptor metadata = metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() );

        setInjectionProperties( metadata, newConfiguration );

        if ( component.getRuntimeInfo() != null )
        {
            try
            {
                validateConfiguration( component, newConfiguration );
            }
            catch ( BadConfigurationException e )
            {
                throw new HubAbortException(
                    "Active DirectoryComponent can not be reconfigured with incorrect configuration", e );
            }
        }

        // Immutable property change handling
        if ( component.getRuntimeInfo() != null )
        {
            for ( DcProperty prop : newConfiguration )
            {
                DcPropertyDescription pd = metadata.getPropertyDescription( prop.getName() );
                if ( pd != null && pd.isImmutable() )
                {
                    DcProperty oldProp = component.getConfiguration().getProperty( prop.getName() );
                    if ( oldProp != null && !( oldProp.getValue().equals( prop.getValue() ) ) )
                    {
                        // We're changing immutable property of live component
                        boolean wasDirty = component.isDirty();
                        component.setDirty( false );

                        try
                        {
                            removeComponent( component );
                            component.setDirty( wasDirty );
                            break;
                        }
                        catch ( HubAbortException e )
                        {
                            throw new HubAbortException(
                                "Reconfiguration of immutable property led to re-instantiation, which has been rejected by hub",
                                e );
                        }
                    }
                }
            }
        }

        if ( component.isDirty() )
        {
            try
            {
                store.updateComponent( component, newConfiguration );
            }
            catch ( HubStoreException e )
            {
                throw new HubAbortException( "HubStore error raised while updating:" + component.getComponentPID(), e );
            }
        }

        component.setConfiguration( newConfiguration );

        if ( component.getRuntimeInfo() != null )
        {
            reconfigureComponent( component );
        }
        else
        {
            if ( component.instantiationFailed() )
            {
                instantiateComponent( component );
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#addComponent(org.apache.directory.server.hub.component.DirectoryComponent)
     */
    @Override
    public void addComponent( DirectoryComponent component ) throws HubAbortException
    {
        DirectoryComponent check = componentsReg.getComponentByReference( component.getComponentPID() );
        if ( check != null )
        {
            throw new HubAbortException( "You can not have two component with same ID:"
                + component.getComponentPID() );
        }

        if ( componentsReg.getComponents( component.getComponentManagerPID() ) != null )
        {
            DcMetadataDescriptor metadata = metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() );
            if ( !metadata.isFactory() )
            {
                throw new HubAbortException( metadata.getMetadataPID() + "can not have more than 1 instance" );
            }
        }

        setInjectionProperties( metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() ),
            component.getConfiguration() );

        if ( component.isDirty() )
        {
            try
            {
                store.installComponent( component );
            }
            catch ( HubStoreException e )
            {
                throw new HubAbortException( "Component couldn't be added to store, discarding...", e );
            }
        }

        componentsReg.addDirectoryComponent( component );

        instantiateComponent( component );

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#removeComponent(org.apache.directory.server.hub.component.DirectoryComponent)
     */
    @Override
    public void removeComponent( DirectoryComponent component ) throws HubAbortException
    {
        clientManager.fireDCRemoving( component );

        if ( component.isDirty() )
        {
            try
            {
                store.uninstallComponent( component );
            }
            catch ( HubStoreException e )
            {
                throw new HubAbortException( "Component couldn't be removed from store, it is still active.", e );
            }
        }

        handleComponentRemoval( component );

        componentsReg.removeDirectoryComponent( component );

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#addInjection(java.lang.String, java.lang.Object)
     */
    @Override
    public void addInjection( String injectionType, Object injection )
    {
        injectionsReg.addInjection( injectionType, injection );

        List<DirectoryComponent> waitingComponents = dependencyResolver.getWaiting( new DCDependency(
            DCDependencyType.INJECTION, injectionType ) );

        if ( waitingComponents != null )
        {
            for ( DirectoryComponent component : waitingComponents )
            {
                instantiateComponent( component );
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#removeInjection(java.lang.String)
     */
    @Override
    public void removeInjection( String injectionType )
    {
        injectionsReg.removeInjection( injectionType );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#registerClient(org.apache.directory.server.hub.client.AbstractHubClient, java.lang.String)
     */
    @Override
    public void registerClient( AbstractHubClient hubClient, String type )
    {
        clientManager.registerHubClient( hubClient, type );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#unregisterClient(org.apache.directory.server.hub.client.AbstractHubClient, java.lang.String)
     */
    @Override
    public void unregisterClient( AbstractHubClient hubClient, String type )
    {
        clientManager.unregisterHubClient( hubClient, type );
    }


    private void activateHandler( String pid )
    {
        Collection<DirectoryComponent> components = componentsReg.getComponents( pid );

        if ( components != null )
        {
            for ( DirectoryComponent component : components )
            {
                instantiateComponent( component );
            }
        }
    }


    private void deactivateHandler( String pid )
    {
        List<DirectoryComponent> attachedComponents = componentsReg.getComponents( pid );

        if ( attachedComponents != null )
        {
            for ( DirectoryComponent component : attachedComponents )
            {
                disposeComponent( component );
            }
        }
    }


    private void setInjectionProperties( DcMetadataDescriptor metadata, DcConfiguration configuration )
    {

        for ( DcPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( pd.getPropertyContext() == DcPropertyType.INJECTION )
            {
                if ( pd.isMandatory() )
                {
                    configuration.addProperty( new DcProperty( pd.getName(), null ) );
                }
            }
        }
    }


    private void validateConfiguration( DirectoryComponent component, DcConfiguration configuration )
        throws BadConfigurationException
    {
        DcMetadataDescriptor metadata = metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() );

        for ( DcProperty property : configuration )
        {
            String propertyName = property.getName();
            String propertyValue = property.getValue();

            DcPropertyDescription pd = metadata.getPropertyDescription( propertyName );

            if ( pd == null )
            {
                continue;
            }

            switch ( pd.getPropertyContext() )
            {
                case PRIMITIVE:
                case PRIMITIVE_COLLECTION:
                    break;

                case REFERENCE:
                case COLLECTION:

                    if ( propertyValue.equals( DirectoryComponentConstants.DC_VAL_NULL ) )
                    {
                        break;
                    }

                    DirectoryComponent reference = componentsReg.getComponentByReference( propertyValue );

                    if ( reference == null || reference.getRuntimeInfo() == null )
                    {
                        throw new BadConfigurationException( "Component:" + component.getComponentPID()
                            + " is lacking property:" + propertyName );
                    }

                    DcMetadataDescriptor referenceMetadata = metadatasReg.getMetadataDescriptor( reference
                        .getComponentManagerPID() );

                    // Means iterating property is a collection item, we should match type with container type.
                    if ( metadata.is( Collection.class.getName() ) )
                    {
                        DcProperty containerProp = null;

                        if ( metadata.is( List.class.getName() ) )
                        {
                            containerProp = component.getConfiguration().getProperty(
                                DirectoryComponentConstants.DC_LIST_PROP_TYPE );
                        }
                        else if ( metadata.is( Set.class.getName() ) )
                        {
                            containerProp = component.getConfiguration().getProperty(
                                DirectoryComponentConstants.DC_SET_PROP_TYPE );
                        }
                        else if ( metadata.is( Array.class.getName() ) )
                        {
                            containerProp = component.getConfiguration().getProperty(
                                DirectoryComponentConstants.DC_ARRAY_PROP_TYPE );
                        }
                        else
                        {
                            throw new BadConfigurationException( "Wrong collection metadata for :"
                                + metadata.getMetadataPID() );
                        }

                        if ( !referenceMetadata.is( containerProp.getValue() ) )
                        {
                            throw new BadConfigurationException( "Collection item:" + reference.getComponentPID()
                                + " is not compatible with collection" );
                        }
                    }
                    else
                    {
                        if ( !referenceMetadata.is( pd.getType() ) )
                        {
                            throw new BadConfigurationException( "Component property:"
                                + reference.getComponentPID()
                                + " is not compatible with declared property type" );
                        }
                    }

                    break;

                case INJECTION:

                    Object injection = injectionsReg.getInjection( pd.getType() );
                    if ( injection == null )
                    {
                        throw new BadConfigurationException( "Component:" + component.getComponentPID()
                            + " is lacking property:" + propertyName );
                    }
            }
        }
    }


    /**
     * Process the configuration supplied or component's own configuration.
     *
     * @param component
     * @param configuration
     */
    private void processConfiguration( DirectoryComponent component ) throws BadConfigurationException
    {
        parentLinksReg.destroyComponentLinks( component );
        DcMetadataDescriptor metadata = metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() );

        // Loading meta-constant properties into component
        Hashtable<String, String> constants = metadata.getConstants();
        if ( constants != null )
        {
            for ( String key : constants.keySet() )
            {
                component.getConfiguration().addConstant( key, constants.get( key ) );
            }
        }

        for ( DcProperty property : component.getConfiguration() )
        {
            String propertyName = property.getName();
            String propertyValue = property.getValue();

            DcPropertyDescription pd = metadata.getPropertyDescription( propertyName );

            if ( pd == null )
            {
                property.setObject( property.getValue() );
                continue;
            }

            switch ( pd.getPropertyContext() )
            {
                case PRIMITIVE:
                case PRIMITIVE_COLLECTION:

                    property.setObject( propertyValue );
                    break;

                case REFERENCE:
                case COLLECTION:

                    if ( propertyValue.equals( DirectoryComponentConstants.DC_VAL_NULL ) )
                    {
                        if ( pd.isMandatory() )
                        {
                            throw new BadConfigurationException( "Mandatory property can not be set to null"
                                + pd.getName() );
                        }

                        property.setObject( null );
                        break;
                    }

                    DirectoryComponent reference = componentsReg.getComponentByReference( propertyValue );

                    if ( reference == null || reference.getRuntimeInfo() == null )
                    {
                        dependencyResolver.addDependencyHook( component, new DCDependency( DCDependencyType.REFERENCE,
                            propertyValue ) );

                        throw new BadConfigurationException( "Component:" + component.getComponentPID()
                            + " is lacking property:" + propertyName );
                    }

                    DcMetadataDescriptor referenceMetadata = metadatasReg.getMetadataDescriptor( reference
                        .getComponentManagerPID() );

                    // Means iterating property is a collection item, we should match type with container type.
                    if ( metadata.is( Collection.class.getName() ) )
                    {
                        DcProperty containerProp = null;

                        if ( metadata.is( List.class.getName() ) )
                        {
                            containerProp = component.getConfiguration().getProperty(
                                DirectoryComponentConstants.DC_LIST_PROP_TYPE );
                        }
                        else if ( metadata.is( Set.class.getName() ) )
                        {
                            containerProp = component.getConfiguration().getProperty(
                                DirectoryComponentConstants.DC_SET_PROP_TYPE );
                        }
                        else if ( metadata.is( Array.class.getName() ) )
                        {
                            containerProp = component.getConfiguration().getProperty(
                                DirectoryComponentConstants.DC_ARRAY_PROP_TYPE );
                        }
                        else
                        {
                            throw new BadConfigurationException( "Wrong collection metadata for :"
                                + metadata.getMetadataPID() );
                        }

                        if ( !referenceMetadata.is( containerProp.getValue() ) )
                        {
                            throw new BadConfigurationException( "Collection item:" + reference.getComponentPID()
                                + " is not compatible with collection" );
                        }

                        property.setObject( reference );
                    }
                    else
                    {
                        if ( !referenceMetadata.is( pd.getType() ) )
                        {
                            throw new BadConfigurationException( "Component property:"
                                + reference.getComponentPID()
                                + " is not compatible with declared property type" );
                        }

                        property.setObject( reference.getRuntimeInfo().getPojo() );
                    }

                    parentLinksReg.addParentLink( reference, new ParentLink( component, propertyName ) );

                    break;

                case INJECTION:

                    Object injection = injectionsReg.getInjection( pd.getType() );
                    if ( injection == null )
                    {
                        dependencyResolver.addDependencyHook( component, new DCDependency( DCDependencyType.INJECTION,
                            pd.getType() ) );
                        throw new BadConfigurationException( "Component:" + component.getComponentPID()
                            + " is lacking property:" + propertyName );
                    }
                    property.setObject( injection );
            }
        }
    }


    private void instantiateComponent( DirectoryComponent component )
    {
        DcOperationsManager opManager = handlersReg.getPIDHandler( component.getComponentManagerPID() );
        if ( opManager == null )
        {
            return;
        }

        try
        {
            processConfiguration( component );
            opManager.instantiateComponent( component );

            component.setFailFlag( false );
            clientManager.fireDCActivated( component );

            List<DirectoryComponent> waitingComponents = dependencyResolver
                .getWaiting( new DCDependency( DCDependencyType.REFERENCE, component
                    .getComponentPID() ) );

            if ( waitingComponents != null )
            {
                for ( DirectoryComponent dependent : waitingComponents )
                {
                    instantiateComponent( dependent );
                }
            }

        }
        catch ( ComponentInstantiationException e )
        {
            component.setFailFlag( true );
        }
        catch ( BadConfigurationException e )
        {
            component.setFailFlag( true );
        }
    }


    private void reconfigureComponent( DirectoryComponent component )
    {
        DcOperationsManager opManager = handlersReg.getPIDHandler( component.getComponentManagerPID() );
        if ( opManager == null )
        {
            return;
        }

        try
        {
            processConfiguration( component );
            opManager.reconfigureComponent( component );

            component.setFailFlag( false );
            clientManager.fireDCReconfigured( component );

            List<ParentLink> parents = parentLinksReg.getParentLinks( component );
            if ( parents != null )
            {
                for ( ParentLink parentLink : parents )
                {
                    DirectoryComponent parent = parentLink.getParent();
                    parent.getConfiguration().addProperty(
                        new DcProperty( DirectoryComponentConstants.DC_PROP_INNER_RECONF_NAME, parentLink
                            .getLinkPoint() ) );

                    reconfigureComponent( parent );
                }
            }
        }
        catch ( ComponentReconfigurationException e )
        {
            component.setFailFlag( true );
        }
        catch ( BadConfigurationException e )
        {
            component.setFailFlag( true );
        }
    }


    private void disposeComponent( DirectoryComponent component )
    {
        clientManager.fireDCDeactivating( component );

        List<ParentLink> parents = parentLinksReg.getParentLinks( component );
        if ( parents != null )
        {
            for ( ParentLink parentLink : parents )
            {
                dependencyResolver.addDependencyHook( parentLink.getParent(),
                    new DCDependency( DCDependencyType.REFERENCE, component.getComponentPID() ) );
                disposeComponent( parentLink.getParent() );
            }
        }

        DcOperationsManager opManager = handlersReg.getPIDHandler( component.getComponentManagerPID() );
        if ( opManager != null )
        {
            opManager.disposeComponent( component );
        }

        component.setRuntimeInfo( null );
    }


    private void handleComponentRemoval( DirectoryComponent component )
    {
        List<ParentLink> parents = parentLinksReg.getParentLinks( component );
        if ( parents != null )
        {
            for ( ParentLink parentLink : parents )
            {
                DcProperty refProperty = parentLink.getParent().getConfiguration()
                    .getProperty( parentLink.getLinkPoint() );
                refProperty.setValue( "null" );

                reconfigureComponent( parentLink.getParent() );
            }
        }

        DcOperationsManager opManager = handlersReg.getPIDHandler( component.getComponentManagerPID() );
        if ( opManager != null )
        {
            opManager.disposeComponent( component );
        }

        component.setRuntimeInfo( null );
    }


    private void insertConfiguratorInterceptor()
    {
        configurator = new ConfiguratorInterceptor();
        configurator.init( this );

        DcConfiguration config = new DcConfiguration( new ArrayList<DcProperty>() );
        config.addConstant( IPojoComponentConstants.PROP_INTERCEPTION_POINT, InterceptionPoint.END.toString() );
        config.addConstant( IPojoComponentConstants.PROP_INTERCEPTOR_OPERATIONS,
            "[" +
                InterceptorOperation.ADD + "," +
                InterceptorOperation.DELETE + "," +
                InterceptorOperation.MODIFY + "," +
                InterceptorOperation.RENAME
                + "]" );

        DirectoryComponent component = new DirectoryComponent( "configuratorMeta", "configuratorInterceptor", config );
        component.setRuntimeInfo( new DcRuntime( null, configurator ) );
        component.setConfigLocation( "ads-instance=configuratorInterceptor,ou=config" );
        component.setDirty( false );

        DcMetadataDescriptor configuratorMeta =
            new DcMetadataDescriptor( "configuratorMeta", false, new Version( "2.0.0" ),
                ConfiguratorInterceptor.class.getName(), new String[]
                    { Interceptor.class.getName() }, new String[0], null, new DcPropertyDescription[0] );

        metadatasReg.addMetadataDescriptor( configuratorMeta );
        componentsReg.addDirectoryComponent( component );

        clientManager.fireDCActivated( component );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#getDCRegistry()
     */
    @Override
    public DirectoryComponentRegistry getDCRegistry()
    {
        return componentsReg;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#getMetaRegistry()
     */
    @Override
    public DcMetadataRegistry getMetaRegistry()
    {
        return metadatasReg;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#getInjectionRegistry()
     */
    @Override
    public InjectionRegistry getInjectionRegistry()
    {
        return injectionsReg;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.hub.ComponentHub#getPIDHandlerRegistry()
     */
    @Override
    public PidHandlerRegistry getPIDHandlerRegistry()
    {
        return handlersReg;
    }
}
