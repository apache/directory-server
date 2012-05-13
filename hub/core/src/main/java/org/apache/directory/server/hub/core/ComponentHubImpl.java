package org.apache.directory.server.hub.core;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.hub.api.AbstractHubClient;
import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubStore;
import org.apache.directory.server.hub.api.component.DCConfiguration;
import org.apache.directory.server.hub.api.component.DCProperty;
import org.apache.directory.server.hub.api.component.DCRuntime;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.component.DirectoryComponentConstants;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorConstants;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.directory.server.hub.api.exception.BadConfigurationException;
import org.apache.directory.server.hub.api.exception.ComponentInstantiationException;
import org.apache.directory.server.hub.api.exception.ComponentReconfigurationException;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.exception.HubStoreException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DCOperationsManager;
import org.apache.directory.server.hub.api.meta.DCPropertyDescription;
import org.apache.directory.server.hub.api.meta.DCPropertyType;
import org.apache.directory.server.hub.api.registry.DCMetadataRegistry;
import org.apache.directory.server.hub.api.registry.DirectoryComponentRegistry;
import org.apache.directory.server.hub.api.registry.InjectionRegistry;
import org.apache.directory.server.hub.api.registry.PIDHandlerRegistry;
import org.apache.directory.server.hub.connector.ipojo.core.IPojoConnector;
import org.apache.directory.server.hub.core.configurator.ConfiguratorInterceptor;
import org.apache.directory.server.hub.core.connector.collection.CollectionConnector;
import org.apache.directory.server.hub.core.meta.DCMetadataNormalizer;
import org.apache.directory.server.hub.core.util.DCDependency;
import org.apache.directory.server.hub.core.util.ParentLink;
import org.apache.directory.server.hub.core.util.DCDependency.DCDependencyType;
import org.apache.felix.ipojo.IPojoContext;
import org.osgi.framework.Version;


public class ComponentHubImpl implements ComponentHub
{
    // Registries
    private DirectoryComponentRegistry componentsReg = new DirectoryComponentRegistry();
    private DCMetadataRegistry metadatasReg = new DCMetadataRegistry();
    private InjectionRegistry injectionsReg = new InjectionRegistry();
    private PIDHandlerRegistry handlersReg = new PIDHandlerRegistry();
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
            List<DCMetadataDescriptor> metadatas = store.getMetadataDescriptors();
            metadatasReg.addMetadataDescriptor( metadatas );

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
    public void connectHandler( DCMetadataDescriptor metadata, DCOperationsManager operationsManager )
        throws HubAbortException
    {
        DCMetadataDescriptor existingMetadata = metadatasReg.getMetadataDescriptor( metadata.getMetadataPID() );

        DCMetadataNormalizer.normalizeDCMetadata( metadata );

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
        DCMetadataDescriptor meta = metadatasReg.getMetadataDescriptor( handlerPID );
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
    public void updateComponent( DirectoryComponent component, DCConfiguration newConfiguration )
        throws HubAbortException
    {
        setInjectionProperties( metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() ),
            newConfiguration );

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


    private void setInjectionProperties( DCMetadataDescriptor metadata, DCConfiguration configuration )
    {

        for ( DCPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( pd.getPropertyContext() == DCPropertyType.INJECTION )
            {
                if ( pd.isMandatory() )
                {
                    configuration.addProperty( new DCProperty( pd.getName(), null ) );
                }
            }
        }
    }


    private void validateConfiguration( DirectoryComponent component, DCConfiguration configuration )
        throws BadConfigurationException
    {
        DCMetadataDescriptor metadata = metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() );

        for ( DCProperty property : configuration )
        {
            String propertyName = property.getName();
            String propertyValue = property.getValue();

            DCPropertyDescription pd = metadata.getPropertyDescription( propertyName );

            if ( pd == null )
            {
                continue;
            }

            switch ( pd.getPropertyContext() )
            {
                case CONSTANT:
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

                    DCMetadataDescriptor referenceMetadata = metadatasReg.getMetadataDescriptor( reference
                        .getComponentManagerPID() );

                    // Means iterating property is a collection item, we should match type with container type.
                    if ( metadata.is( Collection.class.getName() ) )
                    {
                        DCProperty containerProp = null;

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
        DCMetadataDescriptor metadata = metadatasReg.getMetadataDescriptor( component.getComponentManagerPID() );

        // Loading meta-constant properties into component
        for ( DCPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( pd.getPropertyContext() == DCPropertyType.CONSTANT )
            {
                component.getConfiguration().addConstant( pd.getName(), pd.getDefaultValue() );
            }
        }

        for ( DCProperty property : component.getConfiguration() )
        {
            String propertyName = property.getName();
            String propertyValue = property.getValue();

            DCPropertyDescription pd = metadata.getPropertyDescription( propertyName );

            if ( pd == null )
            {
                property.setObject( property.getValue() );
                continue;
            }

            if ( pd.getPropertyContext() == DCPropertyType.CONSTANT )
            {
                property.setObject( propertyValue );
                continue;
            }

            switch ( pd.getPropertyContext() )
            {
                case CONSTANT:
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

                    DCMetadataDescriptor referenceMetadata = metadatasReg.getMetadataDescriptor( reference
                        .getComponentManagerPID() );

                    // Means iterating property is a collection item, we should match type with container type.
                    if ( metadata.is( Collection.class.getName() ) )
                    {
                        DCProperty containerProp = null;

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
        DCOperationsManager opManager = handlersReg.getPIDHandler( component.getComponentManagerPID() );
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
        DCOperationsManager opManager = handlersReg.getPIDHandler( component.getComponentManagerPID() );
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
                        new DCProperty( DirectoryComponentConstants.DC_PROP_INNER_RECONF_NAME, parentLink
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

        DCOperationsManager opManager = handlersReg.getPIDHandler( component.getComponentManagerPID() );
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
                DCProperty refProperty = parentLink.getParent().getConfiguration()
                    .getProperty( parentLink.getLinkPoint() );
                refProperty.setValue( "null" );

                reconfigureComponent( parentLink.getParent() );
            }
        }

        DCOperationsManager opManager = handlersReg.getPIDHandler( component.getComponentManagerPID() );
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

        DCConfiguration config = new DCConfiguration( new ArrayList<DCProperty>() );
        config.addConstant( InterceptorConstants.PROP_INTERCEPTION_POINT, InterceptionPoint.END.toString() );
        config.addConstant( InterceptorConstants.PROP_INTERCEPTOR_OPERATIONS,
            "[" +
                InterceptorOperation.ADD + "," +
                InterceptorOperation.DELETE + "," +
                InterceptorOperation.MODIFY + "," +
                InterceptorOperation.RENAME
                + "]" );

        DirectoryComponent component = new DirectoryComponent( "configuratorMeta", "configuratorInterceptor", config );
        component.setRuntimeInfo( new DCRuntime( null, configurator ) );
        component.setConfigLocation( "ads-instance=configuratorInterceptor,ou=config" );
        component.setDirty( false );

        DCMetadataDescriptor configuratorMeta =
            new DCMetadataDescriptor( "configuratorMeta", false, new Version( "2.0.0" ),
                ConfiguratorInterceptor.class.getName(), new String[]
                    { Interceptor.class.getName() }, new String[0], new DCPropertyDescription[0] );

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
    public DCMetadataRegistry getMetaRegistry()
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
    public PIDHandlerRegistry getPIDHandlerRegistry()
    {
        return handlersReg;
    }
}
