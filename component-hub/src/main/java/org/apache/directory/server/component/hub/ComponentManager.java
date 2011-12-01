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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.instance.ADSComponentInstance;
import org.apache.directory.server.component.instance.ADSComponentInstanceGenerator;
import org.apache.directory.server.component.schema.ComponentSchemaGenerator;
import org.apache.directory.server.core.api.LdapCoreSessionConnection;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.AddRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides methods to create instances, deploy schemas and create instance entries on DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ComponentManager
{
    /*
     * Logger
     */
    private final Logger LOG = LoggerFactory.getLogger( ComponentManager.class );

    /*
     * Schema generators
     */
    private Dictionary<String, ComponentSchemaGenerator> schemaGenerators;

    /*
     * Instance Generators
     */
    private Dictionary<String, ADSComponentInstanceGenerator> instanceGenerators;

    /*
     * Cache Manager
     */
    private ComponentCacheManager cacheManager;

    /*
     * Configuration Manager
     */
    private InstanceManager instanceManager;

    /*
     * Ldif deferred writing queue.
     */
    private Queue<LdifEntry> ldifQueue = new LinkedBlockingQueue<LdifEntry>();

    /*
     * Connection reference to access ApacheDS
     */
    private LdapCoreSessionConnection ldapConn;


    public ComponentManager( ComponentCacheManager cacheManager, InstanceManager instanceManager )
    {
        schemaGenerators = new Hashtable<String, ComponentSchemaGenerator>();
        instanceGenerators = new Hashtable<String, ADSComponentInstanceGenerator>();

        this.cacheManager = cacheManager;
        this.instanceManager = instanceManager;
    }


    /**
     * Used to set internal connection. All write operations to the schema will be deferred
     * until reference is set.
     *
     * @param conn LdapCoreSessionConnection reference to set.
     */
    public synchronized void setConnection( LdapCoreSessionConnection conn )
    {
        ldapConn = conn;

        flushCache();
    }


    /**
     * Adds new schema generator for specified component type.
     * Keeps the first added generator as default.
     *
     * @param componentType component type to register schema generator
     * @param generator schema generator instance
     */
    public void addSchemaGenerator( String componentType, ComponentSchemaGenerator generator )
    {
        if ( schemaGenerators.get( componentType ) == null )
        {
            schemaGenerators.put( componentType, generator );
        }
    }


    /**
     * Adds new instance generator for specified component type.
     * Keeps the first added generator as default.
     *
     * @param componentType component type to register instance generator
     * @param generator instance generator instance
     */
    public void addInstanceGenerator( String componentType, ADSComponentInstanceGenerator generator )
    {
        if ( instanceGenerators.get( componentType ) == null )
        {
            instanceGenerators.put( componentType, generator );
        }
    }


    /**
     * Create and return the instance of the given component
     * using ADSComponentInstanceGenerator registered for its type.
     *
     * @param component ADSComponent reference to instantiate
     * @return created ADSComponentInstance reference
     */
    public ADSComponentInstance createInstance( ADSComponent component, Properties properties )
    {
        ADSComponentInstanceGenerator generator = instanceGenerators.get( component.getComponentType() );
        if ( generator != null )
        {
            ADSComponentInstance instance = generator.createInstance( component, properties );

            instance.setInstanceManager( instanceManager );

            if ( instance != null )
            {
                component.addInstance( instance );
            }

            return instance;
        }
        else
        {
            LOG.info( "No instance generator found for component:" + component );
            return null;
        }
    }


    /**
     * Loads the schema of the component into ApacheDS for
     * further instance entry mappings and configuration hooks.
     *
     * @param component ADSComponent reference for schema loading
     */
    public void loadComponentSchema( ADSComponent component )
    {
        try
        {
            checkAndGenerateBaseSchema( component );
            if ( component.getSchema().getSchemaElements() != null )
            {
                loadLdifs( component.getSchema().getSchemaElements() );
            }
        }
        catch ( LdapException exc )
        {
            LOG.info( "An error occured while loading schema for component: " + component );
        }
    }


    /**
     * Load LdifEntry list into ApacheDS.
     * Loads them into the cache if the connection is not set yet.
     *
     * @param ldifs LdifEntry list to load.
     * @throws LdapException
     */
    private synchronized void loadLdifs( List<LdifEntry> ldifs ) throws LdapException
    {
        if ( ldapConn == null )
        {
            for ( LdifEntry ldif : ldifs )
            {
                ldifQueue.add( ldif );
            }
        }
        else
        {
            for ( LdifEntry ldif : ldifs )
            {
                AddRequest addReq = new AddRequestImpl();
                addReq.setEntry( ldif.getEntry() );

                ldapConn.add( addReq );
            }
        }
    }


    /**
     * Loads all the cached entries into ApacheDS
     *
     */
    private void flushCache()
    {
        List<LdifEntry> cache = new ArrayList<LdifEntry>();

        for ( LdifEntry ldif : ldifQueue )
        {
            cache.add( ldif );
        }

        try
        {
            loadLdifs( cache );
        }
        catch ( LdapException exc )
        {
            LOG.info( "Error while flushing the Ldif cache." );
        }
    }


    /**
     * Checks for the base schema which will hold the schema elements for the
     * given component. 
     * 
     * Generates an empty one, if none is exist.
     *
     * @param component ADSComponent reference to check base schema against.
     * @throws LdapException
     */
    private void checkAndGenerateBaseSchema( ADSComponent component ) throws LdapException
    {
        String parentSchemaDn = "cn=" + component.getSchema().getParentSchemaName() + ",ou=schema";
        String attribsDn = "ou=attributeTypes," + parentSchemaDn;
        String ocsDn = "ou=objectClasses," + parentSchemaDn;

        boolean schemaExists = ldapConn.exists( parentSchemaDn );
        boolean attribsExists = ldapConn.exists( attribsDn );
        boolean ocsExists = ldapConn.exists( ocsDn );

        if ( schemaExists && attribsExists && ocsExists )
        {
            return;
        }

        List<LdifEntry> ldifs = new ArrayList<LdifEntry>();

        if ( !schemaExists )
        {
            ldifs.add( new LdifEntry( parentSchemaDn,
                "objectClass:metaSchema",
                "objectClass:top",
                "cn:" + component.getSchema().getParentSchemaName(),
                "m-dependencies: system",
                "m-dependencies: core" ) );
        }

        if ( !attribsExists )
        {
            ldifs.add( new LdifEntry( attribsDn,
                "objectclass:organizationalUnit",
                "objectClass:top",
                "ou:attributetypes" ) );
        }

        if ( ocsExists )
        {
            ldifs.add( new LdifEntry( ocsDn,
                "objectclass:organizationalUnit",
                "objectClass:top",
                "ou:objectClasses" ) );
        }

        loadLdifs( ldifs );
    }


    /**
     * Deletes the schema elements for the component.
     *
     * @param component ADSComponent reference to delete its schema elements from ApacheDS
     */
    public void deleteSchemaElements( ADSComponent component )
    {
        List<LdifEntry> schemaElements = component.getSchema().getSchemaElements();

        //We reverse the order here for deletion.
        //List was originally in add order.
        Collections.reverse( schemaElements );

        if ( ldapConn == null )
        {
            return;
        }
        try
        {
            for ( LdifEntry ldif : schemaElements )
            {
                ldapConn.delete( ldif.getDn() );
            }
        }
        catch ( LdapException exc )
        {
            LOG.info( "Error occured while deleting component's schema elements" );
        }
    }


    /**
     * Caches the component manually
     *
     * @param component ADSComponent to initiate caching
     */
    public void cacheComponent( ADSComponent component )
    {
        cacheManager.cacheComponent( component );
    }


    /**
     * Loads the cached instance configurations for component, and use
     * them to create cached instances.
     *
     * @param component ADSComponent reference to load its cached instances.
     * @return loaded instances.
     */
    public List<ADSComponentInstance> loadCachedInstances( ADSComponent component )
    {
        List<ADSComponentInstance> cachedInstances = new ArrayList<ADSComponentInstance>();

        List<Properties> cachedConfigurations = cacheManager.getCachedInstanceConfigurations( component );

        if ( cachedConfigurations == null )
        {
            return null;
        }

        for ( Properties props : cachedConfigurations )
        {
            ADSComponentInstance ins = createInstance( component, props );
            if ( ins != null )
            {
                cachedInstances.add( ins );
            }
        }

        return cachedInstances;
    }

}
