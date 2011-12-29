/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.component.hub;


import java.util.List;
import java.util.Properties;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.instance.ADSComponentInstance;
import org.apache.directory.server.component.instance.CachedComponentInstance;
import org.apache.directory.server.component.utilities.ADSConstants;
import org.apache.directory.server.component.utilities.ADSSchemaConstants;
import org.apache.directory.server.component.utilities.LdifConfigHelper;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InstanceManager
{

    /*
     * Logger
     */
    private final Logger LOG = LoggerFactory.getLogger( InstanceManager.class );

    /*
     * Registry to access ADSComponent references by name.
     */
    private ComponentRegistry componentRegistry;

    /*
     * Event Manager to fire new configuration events.
     */
    private ComponentEventManager eventManager;

    /*
     * List of registered component names to be managed by InstaceManager
     */
    private List<String> registeredComponentNames;


    public InstanceManager( ComponentRegistry componentRegistry, ComponentEventManager eventManager )
    {
        this.componentRegistry = componentRegistry;
        this.eventManager = eventManager;
    }


    /**
     * Registeres component under InstanceManager for instance entry management.
     *
     * @param component ADSComponent reference to register in InstanceManager
     */
    public void registerComponent( ADSComponent component )
    {
        registeredComponentNames.add( component.getComponentName().toLowerCase() );
    }


    /**
     * Unregisteres component from InstanceManager for instance entry management.
     *
     * @param component ADSComponent reference to unregister from InstanceManager
     */
    public void unregisterComponent( ADSComponent component )
    {
        registeredComponentNames.remove( component.getComponentName().toLowerCase() );
    }


    /**
     * Register inner DirectoryListener class with DirectoryService
     * DirectoryService reference must have its EventService set by EventInterceptor initialization.
     *
     * @param ads DirectoryService reference to register listener.
     */
    public void registerWithDirectoryService( DirectoryService ads )
    {
        try
        {
            SearchRequest sr = new SearchRequestImpl()
                .setBase( new Dn( "ou=config" ) )
                .setScope( SearchScope.SUBTREE )
                .setFilter( "(objectClass=*)" );

            NotificationCriteria nfCriteria = new NotificationCriteria( sr );
            ads.getEventService().addListener( listener, nfCriteria );
        }
        catch ( LdapException e )
        {
            LOG.info( "Ldap exception while creating SearchRequest" );
            e.printStackTrace();
        }
        catch ( Exception e )
        {
            LOG.info( "Exception while registering with EventService" );
            e.printStackTrace();
        }

    }


    /**
     * Gets the component name in lower case from instance entr 
     * by looking its parent component entry.
     *
     * @param instanceEntry Instance entry to look for its parent component's name
     * @return instance's component name
     */
    private String getComponentName( Entry instanceEntry )
    {
        try
        {
            Rdn parentComponentRdn = instanceEntry.getDn().getParent().getParent().getRdn();
            String componentName = parentComponentRdn.getNormValue().getString();

            return componentName;
        }
        catch ( Exception e )
        {
            // Most probably the given Entry is not an instance entry.
            return null;
        }
    }


    /**
     * Check if given Entry is a component entry.
     *
     * @param entry Entry to check
     * @return true if it is a component entry
     */
    private boolean ifComponentEntry( Entry entry )
    {
        Attribute OCAttrib = entry.get( SchemaConstants.OBJECT_CLASS_AT );
        if ( OCAttrib.contains( ADSSchemaConstants.ADS_COMPONENT ) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * Check if given Entry is a instance entry
     *
     * @param entry Entry to check
     * @return true if it is a instance entry
     */
    private boolean ifInstanceEntry( Entry entry )
    {
        Dn instancesDn = entry.getDn().getParent();
        if ( instancesDn == null )
        {
            return false;
        }

        if ( !instancesDn.getRdn().getName().equals( ADSConstants.ADS_COMPONENT_INSTANCES_RDN ) )
        {
            return false;
        }

        return true;
    }

    /*
     * Internal DirectoryListener implementation to register with EventInterceptor
     */
    private DirectoryListener listener = new DirectoryListener()
    {

        @Override
        public void entryRenamed( RenameOperationContext renameContext )
        {
            // TODO Prevent instance entry renamings, thus IPojo instances can't be renamed. Reload the original

        }


        @Override
        public void entryMovedAndRenamed( MoveAndRenameOperationContext moveAndRenameContext )
        {
            // TODO Prevent moves of entries in any circumstances here. Reload the original.

        }


        @Override
        public void entryMoved( MoveOperationContext moveContext )
        {
            // TODO Prevent moves of entries in any circumstances here. Reload the original.

        }


        @Override
        public void entryModified( ModifyOperationContext modifyContext )
        {
            Entry modifiedEntry = modifyContext.getModifiedEntry();

            if ( ifInstanceEntry( modifiedEntry ) )
            {
                Properties instanceConfiguration = LdifConfigHelper.instanceEntryToConfiguration( modifiedEntry );
                String componentName = getComponentName( modifiedEntry );

                if ( registeredComponentNames.contains( componentName ) )
                {
                    ADSComponent component = componentRegistry.getComponentByName( componentName );

                    String instanceName = ( String ) instanceConfiguration
                        .remove( ADSConstants.ADS_COMPONENT_INSTANCE_PROP_NAME );

                    ADSComponentInstance backedInstance = component.getInstance( instanceName );
                    backedInstance.reconfigure( instanceConfiguration );
                }
            }
            else if ( ifComponentEntry( modifiedEntry ) )
            {
                //TODO revert it back
            }
        }


        @Override
        public void entryDeleted( DeleteOperationContext deleteContext )
        {
            Entry deletedEntry = deleteContext.getEntry();
            if ( ifInstanceEntry( deletedEntry ) )
            {
                String componentName = getComponentName( deletedEntry );
                Properties instanceConfiguration = LdifConfigHelper.instanceEntryToConfiguration( deletedEntry );

                if ( registeredComponentNames.contains( componentName ) )
                {
                    ADSComponent component = componentRegistry.getComponentByName( componentName );

                    String instanceName = ( String ) instanceConfiguration
                        .remove( ADSConstants.ADS_COMPONENT_INSTANCE_PROP_NAME );

                    ADSComponentInstance backedInstance = component.getInstance( instanceName );

                    backedInstance.stop();
                }
            }
        }


        @Override
        public void entryAdded( AddOperationContext addContext )
        {
            Entry addedEntry = addContext.getEntry();

            if ( ifInstanceEntry( addedEntry ) )
            {
                Properties conf = LdifConfigHelper.instanceEntryToConfiguration( addedEntry );
                String componentName = getComponentName( addedEntry );

                if ( registeredComponentNames.contains( componentName ) )
                {
                    ADSComponent component = componentRegistry.getComponentByName( componentName );
                    CachedComponentInstance createdConf = new CachedComponentInstance( addedEntry.getDn().getName(),
                        conf );

                    component.addCachedInstance( createdConf );

                    eventManager.fireConfigurationCreated( component, createdConf );
                }
            }
            else if ( ifComponentEntry( addedEntry ) )
            {
                //TODO Revert it back
            }

        }
    };
}
