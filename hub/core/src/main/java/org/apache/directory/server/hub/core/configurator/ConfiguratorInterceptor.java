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

package org.apache.directory.server.hub.core.configurator;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubConnector;
import org.apache.directory.server.hub.api.component.DCConfiguration;
import org.apache.directory.server.hub.api.component.DCProperty;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.component.DirectoryComponentConstants;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.core.store.StoreDCBuilder;
import org.apache.directory.server.hub.core.store.StoreSchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;


public class ConfiguratorInterceptor extends BaseInterceptor implements HubConnector
{
    private ComponentHub hub;
    private StoreDCBuilder dcBuilder;


    public ConfiguratorInterceptor()
    {
        super( InterceptorEnum.CONFIGURATOR_INTERCEPTOR );
    }


    /**
     * Initialize the event interceptor. It creates a pool of executor which will be used
     * to call the listeners in separate threads.
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        dcBuilder = new StoreDCBuilder( directoryService.getSchemaManager() );
    }


    public void add( final AddOperationContext addContext ) throws LdapException
    {
        if ( !isConfigurationOperation( addContext ) || !isHubEntry( addContext.getEntry() ) )
        {
            next( addContext );
            return;
        }

        DirectoryComponent component = dcBuilder.buildComponentFromEntry( addContext.getEntry() );

        //Coming from store already.
        component.setDirty( false );

        try
        {
            hub.addComponent( component );

            // If newly added component is a component item, its collection must be updated.
            if ( component.getConfiguration().getCollectionIndex() != null )
            {
                Dn parentDn = new Dn( component.getConfigLocation() );
                parentDn = parentDn.getParent();

                DirectoryComponent parentComponent = hub.getDCRegistry().getComponentByLocation( parentDn.toString() );
                if ( parentComponent.getComponentManagerPID().startsWith( StoreSchemaConstants.HUB_OC_COLLECTION ) )
                {
                    DCConfiguration newConfiguration = new DCConfiguration( parentComponent.getConfiguration() );

                    String itemID = DirectoryComponentConstants.DC_PROP_ITEM_PREFIX
                        + component.getComponentPID();

                    newConfiguration.addProperty(
                        new DCProperty( itemID, component.getComponentPID() ) );

                    try
                    {
                        hub.updateComponent( parentComponent, newConfiguration );
                    }
                    catch ( HubAbortException e )
                    {
                        /*
                         * If reconfiguration caused hub abort, it shouldn't prevent item entry to be removed,
                         * because it has already been added to ComponentHub without error.
                         */
                    }

                }
            }
        }
        catch ( HubAbortException e )
        {
            throw new LdapException( e );
        }

        next( addContext );
    }


    public void delete( final DeleteOperationContext deleteContext ) throws LdapException
    {
        if ( !isConfigurationOperation( deleteContext ) || !isHubEntry( deleteContext.getEntry() ) )
        {
            next( deleteContext );
            return;
        }

        String componentLocation = deleteContext.getDn().getName();
        DirectoryComponent component = hub.getDCRegistry().getComponentByLocation( componentLocation );

        if ( component != null )
        {
            try
            {
                // It is already deleting from store.
                component.setDirty( false );

                hub.removeComponent( component );
            }
            catch ( HubAbortException e )
            {
                throw new LdapException( e );
            }
        }

        next( deleteContext );
    }


    public void modify( final ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( !isConfigurationOperation( modifyContext ) || !isHubEntry( modifyContext.getEntry() ) )
        {
            next( modifyContext );
            return;
        }

        String location = modifyContext.getDn().getName();
        DirectoryComponent component = hub.getDCRegistry().getComponentByLocation( location );

        if ( component == null )
        {
            next( modifyContext );
            return;
        }

        List<Modification> mods = new ArrayList<Modification>( modifyContext.getModItems() );

        // Detect name change request first.
        for ( Modification mod : mods )
        {
            if ( mod.getAttribute().getUpId().equals( StoreSchemaConstants.HUB_AT_COMPONENT_NAME ) )
            {
                mods.remove( mod );

                try
                {
                    hub.updateComponentName( component, mod.getAttribute().getString() );
                }
                catch ( HubAbortException e )
                {
                    throw new LdapException( e );
                }
            }
        }

        if ( mods.size() > 0 )
        {
            Entry modifiedEntry = EntryModifier.generateEntryWithMods( modifyContext.getEntry(),
                modifyContext.getModItems() );
            DirectoryComponent newComponent = dcBuilder.buildComponentFromEntry( modifiedEntry );

            DCConfiguration newConfiguration = newComponent.getConfiguration();

            try
            {
                hub.updateComponent( component, newConfiguration );
            }
            catch ( HubAbortException e )
            {
                throw new LdapException( e );
            }
        }

        next( modifyContext );
    }


    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        // TODO: implement
        /*if ( !isConfigurationOperation( moveContext ) || !isHubEntry( moveContext.getEntry() ) )
        {
            next( moveContext );
            return;
        }*/

        next( moveContext );
    }


    public void moveAndRename( final MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        // TODO: implement
        /*if ( !isConfigurationOperation( moveAndRenameContext ) || !isHubEntry( moveAndRenameContext.getEntry() ) )
        {
            next( moveAndRenameContext );
            return;
        }*/
        next( moveAndRenameContext );
    }


    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        if ( !isConfigurationOperation( renameContext ) || !isHubEntry( renameContext.getEntry() ) )
        {
            next( renameContext );
            return;
        }
        String oldLocation = renameContext.getDn().getName();
        DirectoryComponent component = hub.getDCRegistry().getComponentByLocation( oldLocation );
        if ( component != null )
        {
            if ( oldLocation.contains( StoreSchemaConstants.HUB_AT_COMPONENT_NAME ) )
            {
                /*
                 * Dn is constructed with ads-instance attribute,
                 * component's PID is also changing.
                 */

                String newName = renameContext.getNewDn().getRdn().getNormValue().getString();
                try
                {
                    hub.updateComponentName( component, newName );
                }
                catch ( HubAbortException e )
                {
                    throw new LdapException( e );
                }
            }

            String newLocation = renameContext.getNewDn().getName();
            hub.getDCRegistry().changeComponentLocation( component, newLocation );

        }

        next( renameContext );
    }


    public boolean isConfigurationOperation( OperationContext operation )
    {
        try
        {
            Dn targetDn = new Dn( operation.getDn().getName() );

            return targetDn.isDescendantOf( new Dn( "ou=config" ) );
        }
        catch ( LdapInvalidDnException e )
        {
            return false;
        }
    }


    public boolean isHubEntry( Entry entry )
    {
        Attribute ocAttrib = entry.get( schemaManager.getAttributeType( "objectclass" ) );
        for ( Value<?> val : ocAttrib )
        {
            String ocName = val.getString();
            if ( ocName.equals( StoreSchemaConstants.HUB_OC_COMPONENT ) )
            {
                return true;
            }
        }

        return false;
    }


    @Override
    public void init( ComponentHub hub )
    {
        this.hub = hub;
    }
}
