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


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.instance.CachedComponentInstance;
import org.apache.directory.server.component.instance.ADSComponentInstance;
import org.apache.directory.server.component.utilities.ADSComponentHelper;
import org.apache.directory.server.component.utilities.ADSSchemaConstants;
import org.apache.directory.server.component.utilities.LdifConfigHelper;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConfigurationManager
{
    /*
     * Logger
     */
    private final Logger LOG = LoggerFactory.getLogger( ConfigurationManager.class );

    /*
     * Config Partition reference.
     */
    private SingleFileLdifPartition configPartition;

    /*
     * ComponentSchemaManager utility class.
     */
    private ComponentSchemaManager componentSchemaManager;


    public ConfigurationManager( SingleFileLdifPartition configPartition, ComponentSchemaManager componentSchemaManager )
    {
        this.configPartition = configPartition;
        this.componentSchemaManager = componentSchemaManager;
    }


    /**
     * If a configuration exists for the component, it loads its schema data and instance data
     * in supplied ADSComponent reference. If it does not exits, then create new configuration
     * for it under config partition.
     *
     * @param component ADSComponent reference to pair with config partition.
     */
    public void pairWithComponent( ADSComponent component )
    {
        LdifEntry componentEntry = getComponentEntry( component );
        if ( componentEntry == null )
        {
            try
            {
                // Generate and install component's own schema elements first.
                componentSchemaManager.generateAndInstallSchema( component );
            }
            catch ( LdapException e )
            {
                LOG.info( "Schema installation failed for component:" + component );
                e.printStackTrace();
                return;
            }

            List<LdifEntry> componentEntries = generateComponentEntries( component );

            for ( LdifEntry le : componentEntries )
            {
                AddOperationContext ac = new AddOperationContext( null, le.getEntry() );
                try
                {
                    configPartition.add( ac );
                }
                catch ( LdapException e )
                {
                    LOG.info( "Error while injecting generated entries for component:" + component );
                }
            }

            return;
        }

        List<LdifEntry> instanceEntries = getCachedInstances( component );
        if ( instanceEntries == null )
        {
            return;
        }

        List<CachedComponentInstance> cachedInstances = new ArrayList<CachedComponentInstance>();
        for ( LdifEntry le : instanceEntries )
        {
            Properties conf = LdifConfigHelper.instanceEntryToConfiguration( le );
            cachedInstances.add( new CachedComponentInstance( le.getDn().getName(), conf ) );
        }

        component.setCachedInstances( cachedInstances );

        setActiveOnHouseKeeping( component );
    }


    /**
     * Injects an instance's configuration into config partition.
     *
     * @param instance ADSComponentInstance to set in config partition.
     */
    public void injectInstance( ADSComponentInstance instance )
    {
        LdifEntry instanceEntry = LdifConfigHelper.instanceToLdif( instance );

        AddOperationContext ac = new AddOperationContext( null );
        ac.setEntry( instanceEntry.getEntry() );

        try
        {
            configPartition.add( ac );
        }
        catch ( LdapException e )
        {
            LOG.info( "Error while injecting instance into config partition with Dn:" + instance.getDITHookDn() );
            e.printStackTrace();
        }
    }


    public void doHouseKeeping()
    {

    }


    private void setActiveOnHouseKeeping( ADSComponent component )
    {
        try
        {
            Attribute purgeAttrib = new DefaultAttribute( ADSSchemaConstants.ADS_COMPONENT_ATTRIB_PURGE, "0" );
            DefaultModification dm = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, purgeAttrib );

            String componentDn = ADSComponentHelper.getComponentDn( component );

            configPartition.modify( new Dn( componentDn ), dm );
        }
        catch ( Exception e )
        {
            LOG.info( "Error while modifying component entry for positive housekeeping :" + component );
            e.printStackTrace();
        }
    }


    /**
     * Reads the component entry from config partition.
     * Return null if none exists.
     *
     * @param component ADSComponent reference to get its entry
     * @return LdifEntry of component on config partition.
     */
    private LdifEntry getComponentEntry( ADSComponent component )
    {
        String componentDn = ADSComponentHelper.getComponentDn( component );
        LookupOperationContext luc = new LookupOperationContext( null );
        LdifEntry le = null;
        try
        {
            luc.setDn( new Dn( componentDn ) );
            Entry e = configPartition.lookup( luc );

            le = new LdifEntry( e );
        }
        catch ( LdapException e )
        {
            LOG.info( "Error while fetching component entry for component:" + component );
            e.printStackTrace();
        }

        return le;
    }


    /**
     * Generates LdifEntry references describing component.
     * First is actual component entry, the second is instances parent entry.
     *
     * @param component ADSComponent reference to generate entry for
     * @return Generated entries representing component.
     */
    private List<LdifEntry> generateComponentEntries( ADSComponent component )
    {
        List<LdifEntry> ldifs = new ArrayList<LdifEntry>();

        String componentDn = ADSComponentHelper.getComponentDn( component );
        String componentInstancesDn = ADSComponentHelper.getComponentInstancesDn( component );
        String componentName = component.getComponentName();
        String componentType = component.getComponentType();
        String componentOCName = ADSComponentHelper.getComponentObjectClass( component );

        try
        {
            ldifs.add( new LdifEntry( componentDn,
                "objectClass:organizationalUnit",
                "objectClass:top",
                "objectClass:" + ADSSchemaConstants.ADS_COMPONENT,
                "ou:" + componentName,
                ADSSchemaConstants.ADS_COMPONENT_ATTRIB_NAME + ":" + componentName,
                ADSSchemaConstants.ADS_COMPONENT_ATTRIB_TYPE + ":" + componentType,
                ADSSchemaConstants.ADS_COMPONENT_ATTRIB_OC + ":" + componentOCName,
                ADSSchemaConstants.ADS_COMPONENT_ATTRIB_PURGE + ":" + "0" ) );

            ldifs.add( new LdifEntry( componentInstancesDn,
                "objectClass:organizationalUnit",
                "objectClass:top",
                "ou:instances" ) );
        }
        catch ( LdapException e )
        {
            LOG.info( "Error while generating component entry for component:" + component );
            e.printStackTrace();

            return null;
        }

        return ldifs;

    }


    /**
     * Gets the entries of component's instances on config partition.
     *
     * @param component ADSComponent reference to get its cached instance entries
     * @return List of LdifEntry references representing cached instances.
     */
    private List<LdifEntry> getCachedInstances( ADSComponent component )
    {
        String componentInstancesDn = ADSComponentHelper.getComponentInstancesDn( component );
        List<LdifEntry> instances = new ArrayList<LdifEntry>();

        SearchEngine<Entry, Long> se = configPartition.getSearchEngine();
        SchemaManager schemaManager = configPartition.getSchemaManager();

        AttributeType adsdAt = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        EqualityNode<?> filter = new EqualityNode( adsdAt, new StringValue( "organizationalUnit" ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchScope.SUBTREE.ordinal() );
        IndexCursor<Long, Entry, Long> cursor = null;

        try
        {
            // Do the search
            cursor = se.cursor( new Dn( componentInstancesDn ), AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

            // First, check if we have some entries to process.
            if ( !cursor.next() )
            {
                LOG.error( "No instances found for component:" + component );
                return null;
            }

            // Skip the first Entry by invoking cursor.next() in while clause.
            while ( cursor.next() )
            {
                ForwardIndexEntry<Long, Long> forwardEntry = ( ForwardIndexEntry<Long, Long> ) cursor
                    .get();

                Entry entry = configPartition.lookup( forwardEntry.getId() );

                instances.add( new LdifEntry( entry ) );
            }
        }
        catch ( Exception e )
        {
            LOG.info( "Can not open a cursor on config partition for component:" + component );
            return null;
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
                    LOG.info( "Error while closing the cursor" );
                }
            }
        }

        if ( 0 == instances.size() )
        {
            return null;
        }

        return instances;

    }

}
