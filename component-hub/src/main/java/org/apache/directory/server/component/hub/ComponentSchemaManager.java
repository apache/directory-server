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


import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.schema.ADSComponentSchema;
import org.apache.directory.server.component.schema.ComponentOIDGenerator;
import org.apache.directory.server.component.schema.ComponentSchemaGenerator;
import org.apache.directory.server.component.schema.DefaultComponentSchemaGenerator;
import org.apache.directory.server.component.utilities.ADSSchemaConstants;
import org.apache.directory.server.component.utilities.EntryNormalizer;

import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Ava;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
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

    /*
     * Specify whether OID Generator is fed with largest component OID in schema.
     */
    private boolean OIDGeneratorFed = false;


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
    public void generateAndInstallSchema( ADSComponent component ) throws LdapException
    {
        if ( !schemaBaseExists() )
        {
            generateAndInstallBaseSchema();
        }

        if ( !OIDGeneratorFed )
        {
            feedOIDGenerator();
        }

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
        List<LdifEntry> schemaElements = schema.getSchemaElements();

        for ( LdifEntry le : schemaElements )
        {
            Entry normalizedEntry = EntryNormalizer.normalizeEntry( le.getEntry() );
            AddOperationContext addContext = new AddOperationContext( null, normalizedEntry );

            schemaPartition.add( addContext );
        }
    }


    /**
     * Checks if the base schema element is exist to hold provided schema's elements.
     *
     * @param schema ADSComponentSchema reference to check its parent schema
     * @return whether base schema exists or not
     */
    private boolean schemaBaseExists()
    {
        LookupOperationContext luc = new LookupOperationContext( null );
        try
        {
            luc.setDn( new Dn( "cn", ADSSchemaConstants.ADS_COMPONENT_BASE, SchemaConstants.OU_SCHEMA ) );
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
     * It install the base schema for the components.
     *  
     * @param schema ADSComponentSchema reference.
     * @throws LdapException
     */
    private void generateAndInstallBaseSchema() throws LdapException
    {
        try
        {
            LdifReader reader = new LdifReader( this.getClass().getResourceAsStream( "componenthub.ldif" ) );

            for ( LdifEntry le : reader )
            {

                Entry normalizedEntry = EntryNormalizer.normalizeEntry( le.getEntry() );
                
                AddOperationContext addContext = new AddOperationContext( null, normalizedEntry );

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


    /**
     * Searchs the schema partition for component-hub elements, and
     * sets the OIDGenerator's base to the largest component number's base.
     * 
     *
     */
    private void feedOIDGenerator()
    {
        try
        {
            Dn componentOCDn = new Dn( "m-oid", ADSSchemaConstants.ADS_COMPONENT, SchemaConstants.OBJECT_CLASSES_PATH,
                "cn",
                ADSSchemaConstants.ADS_COMPONENT_BASE, SchemaConstants.OU_SCHEMA );

            SearchOperationContext soc = new SearchOperationContext( null );
            soc.setDn( componentOCDn );
            soc.setScope( SearchScope.ONELEVEL );
            soc.setReturningAttributes( new String[]
                { "m-oid" } );

            EntryFilteringCursor cursor = schemaPartition.search( soc );

            int baseOIDLen = ADSSchemaConstants.ADS_COMPONENT_BASE.length();
            int maxComponentID = 0;

            while ( cursor.next() )
            {
                String oid = cursor.get().get( "m-oid" ).get().getString();
                String _componentID = oid.substring( baseOIDLen );
                _componentID = _componentID.substring( 0, _componentID.indexOf( '.' ) );

                int componentID = Integer.parseInt( _componentID );
                if ( componentID > maxComponentID )
                {
                    maxComponentID = componentID;
                }
            }

            ComponentOIDGenerator.feedGenerator( maxComponentID );
            OIDGeneratorFed = true;
        }
        catch ( Exception e )
        {
            LOG.info( "Cursor threw exception while searching for max component oid in schema partition" );
            e.printStackTrace();
        }
    }
}
