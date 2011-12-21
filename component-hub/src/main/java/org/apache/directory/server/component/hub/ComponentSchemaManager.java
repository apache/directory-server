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


import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.schema.ADSComponentSchema;
import org.apache.directory.server.component.schema.ComponentSchemaGenerator;
import org.apache.directory.server.component.schema.DefaultComponentSchemaGenerator;
import org.apache.directory.server.component.utilities.ADSComponentHelper;

import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ComponentSchemaManager
{
    /*
     * Logger
     */
    private final Logger LOG = LoggerFactory.getLogger( ComponentSchemaManager.class );

    /*
     * Schema Partition reference
     */
    private SchemaPartition schemaPartition;

    /*
     * Schema generators
     */
    private Dictionary<String, ComponentSchemaGenerator> schemaGenerators;


    public ComponentSchemaManager( SchemaPartition schemaPartition )
    {
        this.schemaPartition = schemaPartition;

        schemaGenerators = new Hashtable<String, ComponentSchemaGenerator>();
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
     * Generates and install the schema elements for component to represent it and its instances.
     *
     * @param component ADSComponent reference
     * @throws LdapException
     */
    public void setComponentSchema( ADSComponent component ) throws LdapException
    {
        ADSComponentSchema schema = generateComponentSchema( component );
        injectSchemaElements( schema );
    }


    /**
     * Generates the schema elements for the component using its assigned SchemaGenerator
     *
     * @param component ADSComponent reference to generate schema elements for.
     * @return Generated schema elements as ADSComponentSchema reference.
     */
    private ADSComponentSchema generateComponentSchema( ADSComponent component )
    {
        String componentType = component.getComponentType();

        ComponentSchemaGenerator generator = schemaGenerators.get( componentType );
        if ( generator == null )
        {
            generator = new DefaultComponentSchemaGenerator();
        }

        ADSComponentSchema schema = generator.generateADSComponentSchema( component );

        return schema;
    }


    /**
     * Injects schema elements into SchemaPartition.
     *
     * @param schema ADSComponentSchema reference to inject into SchemaPartition
     * @throws LdapException
     */
    private void injectSchemaElements( ADSComponentSchema schema ) throws LdapException
    {
        if ( !schemaBaseExists( schema ) )
        {
            createSchemaBase( schema );
        }

        List<LdifEntry> schemaElements = schema.getSchemaElements();

        for ( LdifEntry le : schemaElements )
        {
            AddOperationContext addContext = new AddOperationContext( null, le.getEntry() );
            schemaPartition.add( addContext );
        }
    }


    /**
     * Checks if the base schema element is exist to hold provided schema's elements.
     *
     * @param schema ADSComponentSchema reference to check its parent schema
     * @return whether base schema exists or not
     */
    private boolean schemaBaseExists( ADSComponentSchema schema )
    {
        LookupOperationContext luc = new LookupOperationContext( null );
        try
        {
            luc.setDn( new Dn( schema.getParentSchemaDn() ) );
            Entry e = schemaPartition.lookup( luc );

            if ( e != null )
            {
                return true;
            }
        }
        catch ( LdapException e )
        {
            LOG.info( "Error while checking base schema element" );
            e.printStackTrace();
        }
        return false;
    }


    /**
     * It install the base schema for the component.
     * PS:(It is first considered to be unique for every component type,
     * but now it is merged under one schema. So that's why we're using one resource for all of them.)
     * 
     * TODO Fix base schema related references later. Will be more meaningfull when we merge component-hubs schemas and constants
     * into ApacheDS's own configuration.
     *  
     * @param schema ADSComponentSchema reference.
     * @throws LdapException
     */
    private void createSchemaBase( ADSComponentSchema schema ) throws LdapException
    {
        try
        {
            LdifReader reader = new LdifReader( this.getClass().getResourceAsStream( "componenthub.ldif" ) );

            for ( LdifEntry le : reader )
            {
                AddOperationContext addContext = new AddOperationContext( null, le.getEntry() );
                schemaPartition.add( addContext );
            }
        }
        catch ( LdapException e )
        {
            LOG.info( "Error while injecting base componenthub schema" );
            e.printStackTrace();

            throw e;
        }

    }
}
