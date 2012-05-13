package org.apache.directory.server.hub.core.store;


import java.util.ArrayList;
import java.util.List;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubStore;
import org.apache.directory.server.hub.api.component.DCConfiguration;
import org.apache.directory.server.hub.api.component.DCProperty;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.HubStoreException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DCPropertyDescription;
import org.apache.directory.server.hub.api.meta.DCPropertyType;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public class ApacheDSConfigStore implements HubStore
{
    private ComponentHub hub;

    private SchemaPartition schemaPartition;
    private SingleFileLdifPartition configPartition;

    private StoreConfigManager configStoreManager;
    private StoreSchemaManager schemaStoreManager;

    private SchemaManager schemaManager;

    public static CsnFactory csnFactory;


    public ApacheDSConfigStore( SchemaPartition schemaPartition, SingleFileLdifPartition configPartition, int replicaId )
    {
        this.schemaPartition = schemaPartition;
        this.configPartition = configPartition;
        this.schemaManager = schemaPartition.getSchemaManager();

        csnFactory = new CsnFactory( replicaId );
    }


    @Override
    public void init( ComponentHub hub ) throws StoreNotValidException
    {
        this.hub = hub;

        configStoreManager = new StoreConfigManager( hub );
        schemaStoreManager = new StoreSchemaManager( hub );

        configStoreManager.init( configPartition );
        schemaStoreManager.init( schemaPartition );
    }


    @Override
    public List<DCMetadataDescriptor> getMetadataDescriptors() throws HubStoreException
    {
        return configStoreManager.getMetadatas();
    }


    @Override
    public List<DirectoryComponent> getComponents() throws HubStoreException
    {
        try
        {
            return configStoreManager.getComponents( new Dn( schemaManager, "ou=config" ), SearchScope.SUBTREE );
        }
        catch ( LdapInvalidDnException e )
        {
            e.printStackTrace();

            return null;
        }
    }


    @Override
    public void installMetadataDescriptor( DCMetadataDescriptor metadata ) throws HubStoreException
    {
        try
        {
            schemaStoreManager.installMetadata( metadata );

            configStoreManager.installMetadata( metadata );

        }
        catch ( LdapException e )
        {
            throw new HubStoreException( "Error occured while installing metadata:" + metadata.getMetadataPID(), e );
        }
    }


    @Override
    public void updateMetadataDescriptor( DCMetadataDescriptor oldMetadata, DCMetadataDescriptor newMetadata )
        throws HubStoreException
    {
        List<DCPropertyDescription> oldConfigurables = extractConfigurableDCs( oldMetadata );
        List<DCPropertyDescription> newconfigurables = extractConfigurableDCs( newMetadata );

        List<DCPropertyDescription> dropped = new ArrayList<DCPropertyDescription>();
        List<DCPropertyDescription> added = new ArrayList<DCPropertyDescription>();

        for ( DCPropertyDescription pd : oldConfigurables )
        {
            DCPropertyDescription newDesc = newMetadata.getPropertyDescription( pd.getName() );
            if ( newDesc == null )
            {
                dropped.add( pd );
            }
            else
            {
                if ( !( pd.getType().equals( newDesc.getType() ) ) )
                {
                    dropped.add( pd );
                    added.add( newDesc );
                }
            }
        }

        for ( DCPropertyDescription pd : newconfigurables )
        {
            if ( oldMetadata.getPropertyDescription( pd.getName() ) == null )
            {
                added.add( pd );
            }
        }

        try
        {
            List<DirectoryComponent> attachedComponents = hub.getDCRegistry().getComponents(
                oldMetadata.getMetadataPID() );
            if ( attachedComponents != null )
            {
                for ( DCPropertyDescription dropping : dropped )
                {
                    for ( DirectoryComponent component : attachedComponents )
                    {
                        configStoreManager.dropPropertyFromEntry( component, dropping.getName() );
                    }
                }
            }

            schemaStoreManager.uninstallAttributes( dropped );
            schemaStoreManager.installAttributes( added );

            if ( dropped.size() != 0 && added.size() != 0 )
            {
                schemaStoreManager.updateOC( newMetadata );
            }

            if ( attachedComponents != null )
            {
                for ( DCPropertyDescription adding : added )
                {
                    for ( DirectoryComponent component : attachedComponents )
                    {
                        configStoreManager.addPropertyToEntry( component,
                            new DCProperty( adding.getName(), adding.getDefaultValue() ) );
                    }
                }
            }

            if ( dropped.size() != 0 && added.size() != 0 )
            {
                if ( oldMetadata.getPropertyDescriptons().length != newMetadata.getPropertyDescriptons().length )
                {
                    if ( !oldMetadata.getMetaVersion().equals( newMetadata.getMetaVersion() ) )
                    {
                        configStoreManager.uninstallMetadata( oldMetadata );
                        configStoreManager.installMetadata( newMetadata );
                    }
                }
            }
        }
        catch ( LdapException e )
        {
            throw new HubStoreException( "Error occured while updating store against new metadata"
                + newMetadata.getMetadataPID(), e );
        }
    }


    @Override
    public void uninstallMetadataDescriptor( DCMetadataDescriptor metadata ) throws HubStoreException
    {
        try
        {
            configStoreManager.uninstallMetadata( metadata );

            List<DCPropertyDescription> configurables = extractConfigurableDCs( metadata );

            schemaStoreManager.uninstallOC( metadata.getMetadataPID() );
            schemaStoreManager.uninstallAttributes( configurables );
        }
        catch ( LdapException e )
        {
            throw new HubStoreException( "Error while uninstalling metadata:" + metadata.getMetadataPID(), e );
        }

    }


    @Override
    public void installComponent( DirectoryComponent component ) throws HubStoreException
    {
        try
        {
            configStoreManager.installComponent( component );
        }
        catch ( LdapException e )
        {
            throw new HubStoreException( "Store threw excepton while adding component:" + component.getComponentPID(),
                e );
        }
    }


    @Override
    public void updateComponent( DirectoryComponent component, DCConfiguration newConfiguration )
        throws HubStoreException
    {
        try
        {
            configStoreManager.updateComponent( component, newConfiguration );
        }
        catch ( LdapException e )
        {
            throw new HubStoreException(
                "Store threw excepton while updating component:" + component.getComponentPID(),
                e );
        }
    }


    @Override
    public void uninstallComponent( DirectoryComponent component ) throws HubStoreException
    {
        try
        {
            configStoreManager.uninstallComponent( component );
        }
        catch ( LdapException e )
        {
            throw new HubStoreException(
                "Store threw excepton while deleting component:" + component.getComponentPID(),
                e );
        }
    }


    public List<DCPropertyDescription> extractConfigurableDCs( DCMetadataDescriptor metadata )
    {
        List<DCPropertyDescription> pds = new ArrayList<DCPropertyDescription>();

        for ( DCPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( pd.getPropertyContext() == DCPropertyType.INJECTION )
            {
                continue;
            }

            if ( pd.getPropertyContext() == DCPropertyType.CONSTANT )
            {
                continue;
            }

            pds.add( pd );
        }

        return pds;
    }

}
