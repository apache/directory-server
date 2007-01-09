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
package org.apache.directory.server.core.schema.bootstrap;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.registries.DefaultOidRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * A unit test case for the BootstrapSchemaLoader class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapSchemaLoaderTest extends TestCase
{
    BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
    DefaultRegistries registries;


    protected void setUp() throws Exception
    {
        super.setUp();
        registries = new DefaultRegistries( "bootstrap", loader, new DefaultOidRegistry() );
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
        registries = null;
    }


    public void testLoadAll() throws NamingException
    {
        Set<Schema> schemas = new HashSet<Schema>();
        schemas.add( new CoreSchema() );
        schemas.add( new ApacheSchema() );
        schemas.add( new SystemSchema() );

        loader.loadWithDependencies( schemas, registries );
        AttributeType type;

        // from core.schema
        type = registries.getAttributeTypeRegistry().lookup( "knowledgeInformation" );
        assertNotNull( type );

        // from apache.schema
        type = registries.getAttributeTypeRegistry().lookup( "apacheAlias" );
        assertNotNull( type );

        // from system.schema
        type = registries.getAttributeTypeRegistry().lookup( "distinguishedName" );
        assertNotNull( type );
    }


    public void testSystemSchemaLoad() throws NamingException
    {
        SystemSchema systemSchema = new SystemSchema();
        loader.load( systemSchema, registries );

        AttributeType type;
        type = registries.getAttributeTypeRegistry().lookup( "distinguishedName" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "objectClass" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "modifyTimestamp" );
        assertNotNull( type );
    }


    public void testApacheSchemaLoad() throws NamingException
    {
        testSystemSchemaLoad();
        ApacheSchema apacheSchema = new ApacheSchema();
        loader.load( apacheSchema, registries );

        AttributeType type;
        type = registries.getAttributeTypeRegistry().lookup( "apacheNdn" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "apacheAlias" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "apacheUpdn" );
        assertNotNull( type );
    }


    public void testEveDepsSchemaLoad() throws NamingException
    {
        Set<Schema> schemas = new HashSet<Schema>();
        schemas.add( new ApacheSchema() );
        schemas.add( new SystemSchema() );

        loader.loadWithDependencies( schemas, registries );
        AttributeType type;
        type = registries.getAttributeTypeRegistry().lookup( "apacheNdn" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "apacheAlias" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "apacheUpdn" );
        assertNotNull( type );
    }


    public void testCoreSchemaLoad() throws NamingException
    {
        testSystemSchemaLoad();
        CoreSchema coreSchema = new CoreSchema();
        loader.load( coreSchema, registries );

        AttributeType type;
        type = registries.getAttributeTypeRegistry().lookup( "knowledgeInformation" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "countryName" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "serialNumber" );
        assertNotNull( type );
    }


    public void testCoreDepsSchemaLoad() throws NamingException
    {
        Set<Schema> schemas = new HashSet<Schema>();
        schemas.add( new CoreSchema() );
        schemas.add( new SystemSchema() );

        loader.loadWithDependencies( schemas, registries );
        AttributeType type;
        type = registries.getAttributeTypeRegistry().lookup( "knowledgeInformation" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "countryName" );
        assertNotNull( type );

        type = registries.getAttributeTypeRegistry().lookup( "serialNumber" );
        assertNotNull( type );
    }


    /**
     * Attempts to resolve the dependent schema objects of all entities that
     * refer to other objects within the registries.
     *
     * @throws NamingException if there are problems.
     */
    public void testReferentialIntegrity() throws NamingException
    {
        if ( System.getProperties().containsKey( "ignore.ref.integ.test" ) )
        {
            System.err.println( "REFERENTIAL INTEGRITY TESTS BYPASSED!!!" );
            return;
        }

        testLoadAll();
        List errors = registries.checkRefInteg();
        assertNotNull( errors );

        StringBuffer buf = new StringBuffer();

        if ( !errors.isEmpty() )
        {
            buf.append( "expected empty erorrs but got " ).append( errors.size() ).append( " errors:\n" );
            for ( int ii = 0; ii < errors.size(); ii++ )
            {
                buf.append( '\t' ).append( errors.get( ii ).toString() ).append( '\n' );
            }

            StringWriter out = new StringWriter();
            Exception e = ( Exception ) errors.get( 0 );
            e.printStackTrace( new PrintWriter( out ) );
            buf.append( "\nfirst exception trace:\n" + out.getBuffer().toString() );
        }

        assertTrue( buf.toString(), errors.isEmpty() );
    }
}
