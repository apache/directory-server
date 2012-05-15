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

package org.apache.directory.server.hub.core.store;


import java.util.ArrayList;
import java.util.List;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubStore;
import org.apache.directory.server.hub.api.component.DcConfiguration;
import org.apache.directory.server.hub.api.component.DcProperty;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.HubStoreException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DcPropertyDescription;
import org.apache.directory.server.hub.api.meta.DcPropertyType;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public class ApacheDsConfigStore implements HubStore
{
    private ComponentHub hub;

    private SchemaPartition schemaPartition;
    private SingleFileLdifPartition configPartition;

    private StoreConfigManager configStoreManager;
    private StoreSchemaManager schemaStoreManager;

    private SchemaManager schemaManager;

    public static CsnFactory csnFactory;


    public ApacheDsConfigStore( SchemaPartition schemaPartition, SingleFileLdifPartition configPartition, int replicaId )
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
    public List<DcMetadataDescriptor> getMetadataDescriptors() throws HubStoreException
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
    public void installMetadataDescriptor( DcMetadataDescriptor metadata ) throws HubStoreException
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
    public void updateMetadataDescriptor( DcMetadataDescriptor oldMetadata, DcMetadataDescriptor newMetadata )
        throws HubStoreException
    {
        List<DcPropertyDescription> oldConfigurables = extractConfigurableDCs( oldMetadata );
        List<DcPropertyDescription> newconfigurables = extractConfigurableDCs( newMetadata );

        List<DcPropertyDescription> dropped = new ArrayList<DcPropertyDescription>();
        List<DcPropertyDescription> added = new ArrayList<DcPropertyDescription>();

        for ( DcPropertyDescription pd : oldConfigurables )
        {
            DcPropertyDescription newDesc = newMetadata.getPropertyDescription( pd.getName() );
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

        for ( DcPropertyDescription pd : newconfigurables )
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
                for ( DcPropertyDescription dropping : dropped )
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
                for ( DcPropertyDescription adding : added )
                {
                    for ( DirectoryComponent component : attachedComponents )
                    {
                        configStoreManager.addPropertyToEntry( component,
                            new DcProperty( adding.getName(), adding.getDefaultValue() ) );
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
    public void uninstallMetadataDescriptor( DcMetadataDescriptor metadata ) throws HubStoreException
    {
        try
        {
            configStoreManager.uninstallMetadata( metadata );

            List<DcPropertyDescription> configurables = extractConfigurableDCs( metadata );

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
    public void updateComponent( DirectoryComponent component, DcConfiguration newConfiguration )
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


    public List<DcPropertyDescription> extractConfigurableDCs( DcMetadataDescriptor metadata )
    {
        List<DcPropertyDescription> pds = new ArrayList<DcPropertyDescription>();

        for ( DcPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( pd.getPropertyContext() == DcPropertyType.INJECTION )
            {
                continue;
            }

            pds.add( pd );
        }

        return pds;
    }

}
