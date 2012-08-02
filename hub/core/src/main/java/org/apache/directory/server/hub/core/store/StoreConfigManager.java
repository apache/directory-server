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
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.component.DcConfiguration;
import org.apache.directory.server.hub.api.component.DcProperty;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.component.DirectoryComponentConstants;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DcPropertyDescription;
import org.apache.directory.server.hub.api.meta.DcPropertyType;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.DateUtils;
import org.osgi.framework.Version;


public class StoreConfigManager
{
    private static final String CONFIG_BASE = "ou=meta,ou=config";
    private static final String CONFIG_MD_BASE = "ou=handler-descriptions," + CONFIG_BASE;
    private static final String CONFIG_PD_BASE = "ou=property-descriptions," + CONFIG_BASE;

    private ComponentHub hub;
    private SingleFileLdifPartition configPartition;
    private SchemaManager schemaManager;

    private StoreDcBuilder dcBuilder;


    public StoreConfigManager( ComponentHub hub )
    {
        this.hub = hub;
    }


    public void init( SingleFileLdifPartition configPartition ) throws StoreNotValidException
    {
        this.configPartition = configPartition;
        this.schemaManager = configPartition.getSchemaManager();
        dcBuilder = new StoreDcBuilder( schemaManager );
    }


    public void installMetadata( DcMetadataDescriptor metadata ) throws LdapException
    {

        for ( DcPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            installPropertyDescription( pd );
        }

        installMetaDescription( metadata );

    }


    public void uninstallMetadata( DcMetadataDescriptor metadata ) throws LdapException
    {
        for ( DcPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            uninstallPropertyDescription( pd.getName() );
        }

        uninstallMetaDescription( metadata.getMetadataPID() );
    }


    public void installPropertyDescription( DcPropertyDescription propertyDescription ) throws LdapException
    {
        Entry pdEntry = buildPropertyDescriptionEntry( propertyDescription );

        AddOperationContext addOp = new AddOperationContext( null, pdEntry.getDn(), pdEntry );

        configPartition.add( addOp );
    }


    public void uninstallPropertyDescription( String propertyName ) throws LdapException
    {
        Dn pdDn = new Dn( schemaManager, StoreSchemaConstants.HUB_AT_PD_NAME, propertyName, CONFIG_PD_BASE );
        Long entryId = configPartition.getEntryId( pdDn );
        if ( entryId != null )
        {
            configPartition.delete( entryId );
        }
    }


    public void installMetaDescription( DcMetadataDescriptor metadata ) throws LdapException
    {
        Entry mdEntry = buildComponentDescriptionEntry( metadata );

        AddOperationContext addOp = new AddOperationContext( null, mdEntry.getDn(), mdEntry );

        configPartition.add( addOp );
    }


    public void uninstallMetaDescription( String componentManagerPID ) throws LdapException
    {
        Dn pdDn = new Dn( schemaManager, StoreSchemaConstants.HUB_AT_MD_PID, componentManagerPID, CONFIG_MD_BASE );

        Long entryId = configPartition.getEntryId( pdDn );
        if ( entryId != null )
        {
            configPartition.delete( entryId );
        }
    }


    public Entry buildPropertyDescriptionEntry( DcPropertyDescription propertyDescription ) throws LdapException
    {
        Dn pdDn = new Dn( schemaManager, StoreSchemaConstants.HUB_AT_PD_NAME, propertyDescription.getName(),
            CONFIG_PD_BASE );

        Entry pdEntry = new DefaultEntry( schemaManager, pdDn );

        pdEntry.add( schemaManager.getAttributeType( "objectclass" ), "ads-property-descriptor" );
        pdEntry.add( schemaManager.getAttributeType( "ads-pd-name" ), propertyDescription.getName() );
        pdEntry.add( schemaManager.getAttributeType( "ads-pd-type" ), propertyDescription.getType() );

        pdEntry.add( schemaManager.getAttributeType( "ads-pd-defaultvalue" ), propertyDescription.getDefaultValue() );
        pdEntry.add( schemaManager.getAttributeType( "ads-pd-description" ), propertyDescription.getDescription() );
        pdEntry.add( schemaManager.getAttributeType( "ads-pd-containerFor" ), propertyDescription.getContainerFor() );
        pdEntry.add( schemaManager.getAttributeType( "ads-pd-mandatory" ),
            ( propertyDescription.isMandatory() ) ? "TRUE" : "FALSE" );

        pdEntry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
        pdEntry.add( SchemaConstants.ENTRY_CSN_AT, ApacheDsConfigStore.csnFactory.newInstance().toString() );
        pdEntry.add( SchemaConstants.CREATORS_NAME_AT, StoreSchemaConstants.SYSTEM_ADMIN_DN );
        pdEntry.add( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        return pdEntry;
    }


    public Entry buildComponentDescriptionEntry( DcMetadataDescriptor metadata ) throws LdapException
    {
        Dn mdDn = new Dn( schemaManager, StoreSchemaConstants.HUB_AT_MD_PID, metadata.getMetadataPID(),
            CONFIG_MD_BASE );

        Entry mdEntry = new DefaultEntry( schemaManager, mdDn );

        mdEntry.add( schemaManager.getAttributeType( "objectclass" ), "ads-meta-descriptor" );
        mdEntry.add( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_PID ), metadata.getMetadataPID() );
        mdEntry.add( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_VERSION ), metadata
            .getMetaVersion().toString() );
        mdEntry.add( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_FACTORY ),
            ( metadata.isFactory() ) ? "TRUE" : "FALSE" );
        mdEntry.add( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_CLASSNAME ),
            metadata.getClassName() );

        for ( DcPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            mdEntry.add( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_PROP ), pd.getName() );
        }

        for ( String iface : metadata.getImplementedInterfaces() )
        {
            mdEntry.add( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_IMPLEMENTS ), iface );
        }

        for ( String sclass : metadata.getExtendedClasses() )
        {
            mdEntry.add( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_EXTENDS ), sclass );
        }

        mdEntry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
        mdEntry.add( SchemaConstants.ENTRY_CSN_AT, ApacheDsConfigStore.csnFactory.newInstance().toString() );
        mdEntry.add( SchemaConstants.CREATORS_NAME_AT, StoreSchemaConstants.SYSTEM_ADMIN_DN );
        mdEntry.add( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        return mdEntry;
    }


    public void installComponent( DirectoryComponent component ) throws LdapException
    {
        Entry cEntry = buildComponentEntry( component );

        AddOperationContext addOp = new AddOperationContext( null, cEntry.getDn(), cEntry );
        configPartition.add( addOp );
    }


    public Entry buildComponentEntry( DirectoryComponent component ) throws LdapException
    {
        Dn componentDn = new Dn( schemaManager, component.getConfigLocation() );

        Entry componentEntry = new DefaultEntry( schemaManager, componentDn );
        componentEntry.add( schemaManager.getAttributeType( "objectclass" ), component.getComponentManagerPID() );
        componentEntry.add( schemaManager.getAttributeType( component.getNamerAttribute() ),
            component.getComponentName() );

        Integer itemIndex = component.getConfiguration().getCollectionIndex();
        if ( itemIndex != null )
        {
            componentEntry.add( schemaManager.getAttributeType( "objectclass" ),
                StoreSchemaConstants.HUB_OC_COLLECTION_ITEM );
            componentEntry.add( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_COLL_ITEM_INDEX ),
                itemIndex.toString() );
        }

        DcMetadataDescriptor metadata = hub.getMetaRegistry()
            .getMetadataDescriptor( component.getComponentManagerPID() );
        for ( DcProperty prop : component.getConfiguration() )
        {
            DcPropertyDescription pd = metadata.getPropertyDescription( prop.getName() );
            /*
             * Which is the case when whether there is a error or 
             * entry is a collection entry with item references(which is against the design)
             */
            if ( pd == null )
            {
                continue;
            }

            if ( pd.getPropertyContext() == DcPropertyType.INJECTION )
            {
                continue;
            }

            componentEntry.add( schemaManager.getAttributeType( prop.getName() ), prop.getValue() );

        }

        return componentEntry;
    }


    public void uninstallComponent( DirectoryComponent component ) throws LdapException
    {
        Dn removalDn = new Dn( schemaManager, component.getConfigLocation() );

        Long entryID = configPartition.getEntryId( removalDn );
        if ( entryID != null )
        {
            configPartition.delete( entryID );
        }
    }


    // No meta change.
    public void updateComponent( DirectoryComponent component, DcConfiguration newConfiguration ) throws LdapException
    {
        for ( DcProperty oldProp : component.getConfiguration() )
        {
            DcProperty newVersion = newConfiguration.getProperty( oldProp.getName() );

            if ( newVersion == null )
            {
                dropPropertyFromEntry( component, oldProp.getName() );
            }
            else
            {
                if ( !( oldProp.getValue().equals( newVersion.getValue() ) ) )
                {
                    dropPropertyFromEntry( component, oldProp.getName() );
                    addPropertyToEntry( component, newVersion );
                }
            }
        }

        for ( DcProperty newProp : newConfiguration )
        {
            if ( component.getConfiguration().getProperty( newProp.getName() ) == null )
            {
                addPropertyToEntry( component, newProp );
            }
        }
    }


    public void dropPropertyFromEntry( DirectoryComponent component, String propertyName ) throws LdapException
    {
        Dn componentDn = new Dn( schemaManager, component.getConfigLocation() );

        ModifyOperationContext modOp = new ModifyOperationContext( null );
        modOp.setDn( componentDn );
        List<Modification> mods = new ArrayList<Modification>();
        Modification removeMod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, propertyName );
        mods.add( removeMod );
        modOp.setModItems( mods );

        configPartition.modify( modOp );
    }


    public void addPropertyToEntry( DirectoryComponent component, DcProperty property ) throws LdapException
    {
        Dn componentDn = new Dn( schemaManager, component.getConfigLocation() );

        ModifyOperationContext modOp = new ModifyOperationContext( null );
        modOp.setDn( componentDn );
        List<Modification> mods = new ArrayList<Modification>();
        Attribute attrib = new DefaultAttribute( schemaManager.getAttributeType( property.getName() ),
            property.getValue() );
        Modification addMod = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );
        mods.add( addMod );
        modOp.setModItems( mods );

        configPartition.modify( modOp );
    }


    public List<DirectoryComponent> getComponents( Dn baseDn, SearchScope scope )
    {
        SearchEngine<Entry, Long> se = configPartition.getSearchEngine();

        AttributeType adsInstanceAttrib = schemaManager
            .getAttributeType( StoreSchemaConstants.HUB_AT_COMPONENT_NAME );

        PresenceNode filter = new PresenceNode( adsInstanceAttrib );

        IndexCursor<Long, Entry, Long> cursor = null;

        List<DirectoryComponent> components = new ArrayList<DirectoryComponent>();

        try
        {
            cursor = se.cursor( baseDn, AliasDerefMode.NEVER_DEREF_ALIASES, filter, scope );
            while ( cursor.next() )
            {
                ForwardIndexEntry<Long, Long> forwardEntry = ( ForwardIndexEntry<Long, Long> ) cursor
                    .get();

                Entry entry = configPartition.lookup( forwardEntry.getId() );

                DirectoryComponent component = dcBuilder.buildComponentFromEntry( entry );

                if ( component == null )
                {
                    continue;
                }

                if ( component.getComponentManagerPID().equals( StoreSchemaConstants.HUB_OC_COLL_LIST )
                    || component.getComponentManagerPID().equals( StoreSchemaConstants.HUB_OC_COLL_SET )
                    || component.getComponentManagerPID().equals( StoreSchemaConstants.HUB_OC_COLL_ARRAY ) )
                {
                    List<DirectoryComponent> items = getComponents(
                        new Dn( schemaManager, component.getConfigLocation() ), SearchScope.ONELEVEL );

                    if ( items != null )
                    {
                        for ( DirectoryComponent item : items )
                        {
                            Integer itemIndex = item.getConfiguration().getCollectionIndex();
                            if ( itemIndex != null )
                            {
                                String itemID = DirectoryComponentConstants.DC_PROP_ITEM_PREFIX
                                    + item.getComponentPID();

                                component.getConfiguration().addProperty(
                                    new DcProperty( itemID, item.getComponentPID() ) );
                            }
                        }
                    }
                }

                components.add( component );
            }

        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return components;
        }
        finally
        {
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }

        return components;
    }


    public List<DcMetadataDescriptor> getMetadatas()
    {
        SearchEngine<Entry, Long> se = configPartition.getSearchEngine();

        PresenceNode filter = new PresenceNode( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_PD_NAME ) );

        IndexCursor<Long, Entry, Long> cursor = null;

        List<DcMetadataDescriptor> metadatas = new ArrayList<DcMetadataDescriptor>();
        Hashtable<String, DcPropertyDescription> pdMap = new Hashtable<String, DcPropertyDescription>();

        try
        {
            cursor = se.cursor( new Dn( schemaManager, CONFIG_PD_BASE ), AliasDerefMode.NEVER_DEREF_ALIASES, filter,
                SearchScope.SUBTREE );

            while ( cursor.next() )
            {
                ForwardIndexEntry<Long, Long> forwardEntry = ( ForwardIndexEntry<Long, Long> ) cursor
                    .get();

                Entry entry = configPartition.lookup( forwardEntry.getId() );

                DcPropertyDescription pd = buildPropertyDescription( entry );
                if ( pd != null )
                {
                    pdMap.put( pd.getName(), pd );
                }
            }

            cursor.close();

            filter = new PresenceNode( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_PID ) );
            cursor = se.cursor( new Dn( schemaManager, CONFIG_MD_BASE ), AliasDerefMode.NEVER_DEREF_ALIASES, filter,
                SearchScope.SUBTREE );

            while ( cursor.next() )
            {
                ForwardIndexEntry<Long, Long> forwardEntry = ( ForwardIndexEntry<Long, Long> ) cursor
                    .get();

                Entry entry = configPartition.lookup( forwardEntry.getId() );

                DcMetadataDescriptor md = buildMetaDescription( entry, pdMap );
                if ( md != null )
                {
                    metadatas.add( md );
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return metadatas;
        }
        finally
        {
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    return metadatas;
                }
            }
        }

        return metadatas;
    }


    private DcPropertyDescription buildPropertyDescription( Entry entry )
    {
        Attribute name = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_PD_NAME ) );
        Attribute type = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_PD_TYPE ) );
        Attribute defaultVal = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_PD_DEFAULTVAL ) );
        Attribute desc = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_PD_DESCRIPTION ) );
        Attribute mandatory = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_PD_MANDATORY ) );
        Attribute container = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_PD_CONTAINERFOR ) );

        try
        {
            String pdDefaultValue = null;
            if ( defaultVal != null )
            {
                pdDefaultValue = defaultVal.getString();
            }

            String containerFor = null;
            if ( container != null )
            {
                containerFor = container.getString();
            }

            String pdDescription = null;
            if ( desc != null )
            {
                pdDescription = desc.getString();
            }

            boolean isMandatory = Boolean.parseBoolean( mandatory.getString() );

            DcPropertyDescription pd = new DcPropertyDescription( name.getString(), type.getString(),
                pdDefaultValue, pdDescription, isMandatory, false, containerFor );

            return pd;
        }
        catch ( Exception e )
        {
            return null;
        }

    }


    private DcMetadataDescriptor buildMetaDescription( Entry entry, Hashtable<String, DcPropertyDescription> pdMap )
    {
        Attribute pid = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_PID ) );
        Attribute version = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_VERSION ) );
        Attribute classname = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_CLASSNAME ) );
        Attribute extended = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_EXTENDS ) );
        Attribute implemented = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_IMPLEMENTS ) );
        Attribute props = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_PROP ) );
        Attribute factory = entry.get( schemaManager.getAttributeType( StoreSchemaConstants.HUB_AT_MD_FACTORY ) );

        try
        {
            List<String> extendedList = new ArrayList<String>();
            if ( extended != null )
            {
                for ( Value<?> val : extended )
                {
                    extendedList.add( val.getString() );
                }
            }

            List<String> implementedList = new ArrayList<String>();
            if ( implemented != null )
            {
                for ( Value<?> val : implemented )
                {
                    implementedList.add( val.getString() );
                }
            }

            List<DcPropertyDescription> pds = new ArrayList<DcPropertyDescription>();
            if ( props != null )
            {
                for ( Value<?> val : props )
                {
                    DcPropertyDescription pd = pdMap.get( val.getString() );
                    if ( pd == null )
                    {
                        throw new Exception( "Non existing property description:" + val.getString() );
                    }
                    pds.add( pd );
                }
            }
            boolean isFactory = Boolean.parseBoolean( factory.getString() );

            return new DcMetadataDescriptor( pid.getString(), isFactory, new Version(
                version.getString() ), classname.getString(),
                implementedList.toArray( new String[0] ), extendedList.toArray( new String[0] ), null,
                pds.toArray( new DcPropertyDescription[0] ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return null;
        }

    }
}
