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
package org.apache.directory.server.core.schema;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Tests methods in SchemaInterceptor.
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class SchemaServiceTest
{
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void setUp() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaServiceTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        loaded = schemaManager.loadWithDeps( "nis" );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }
    }


    @Test
    public void testDescendants() throws Exception
    {
        Iterator<AttributeType> list = schemaManager.getAttributeTypeRegistry().descendants( "name" );
        Set<String> nameAttrs = new HashSet<String>();

        while ( list.hasNext() )
        {
            AttributeType type = list.next();
            nameAttrs.add( type.getName() );
        }

        // We should only have 19 AT
        String[] expectedNames = new String[]
            { "sn", "generationQualifier", "ou", "c", "o", "l", "c-st", "givenName", "title", "cn", "initials",
                "dmdName", "c-ou", "c-o", "apachePresence", "st", "c-l", "ads-serverId", "ads-indexAttributeId",
                "ads-transportId", "ads-directoryServiceId", "ads-Id", "ads-extendedOpId", "ads-pwdId",
                "ads-compositeElement", "ads-replConsumerId", "ads-journalId", "ads-changeLogId", "ads-replProviderId" };

        for ( String name : expectedNames )
        {
            if ( nameAttrs.contains( name ) )
            {
                nameAttrs.remove( name );
            }
        }

        assertEquals( 0, nameAttrs.size() );
    }
    /*
        public void testAlterObjectClassesBogusAttr() throws NamingException
        {
            Attribute attr = new AttributeImpl( "blah", "blah" );

            try
            {
                SchemaInterceptor.alterObjectClasses( attr, registries.getObjectClassRegistry() );
                fail( "should not get here" );
            }
            catch ( LdapNamingException e )
            {
                assertEquals( ResultCodeEnum.OPERATIONS_ERROR, e.getResultCode() );
            }

            attr = new AttributeImpl( "objectClass" );
            SchemaInterceptor.alterObjectClasses( attr );
            assertEquals( 0, attr.size() );
        }


        public void testAlterObjectClassesNoAttrValue() throws NamingException
        {
            Attribute attr = new AttributeImpl( "objectClass" );
            SchemaInterceptor.alterObjectClasses( attr );
            assertEquals( 0, attr.size() );
        }


        public void testAlterObjectClassesTopAttrValue() throws NamingException
        {
            Attribute attr = new AttributeImpl( "objectClass", "top" );
            SchemaInterceptor.alterObjectClasses( attr, registries.getObjectClassRegistry() );
            assertEquals( 0, attr.size() );
        }


        public void testAlterObjectClassesInetOrgPersonAttrValue() throws NamingException
        {
            Attribute attr = new AttributeImpl( "objectClass", "organizationalPerson" );
            SchemaInterceptor.alterObjectClasses( attr, registries.getObjectClassRegistry() );
            assertEquals( 2, attr.size() );
            assertTrue( attr.contains( "person" ) );
            assertTrue( attr.contains( "organizationalPerson" ) );
        }


        public void testAlterObjectClassesOverlapping() throws NamingException
        {
            Attribute attr = new AttributeImpl( "objectClass", "organizationalPerson" );
            attr.add( "residentialPerson" );
            SchemaInterceptor.alterObjectClasses( attr, registries.getObjectClassRegistry() );
            assertEquals( 3, attr.size() );
            assertTrue( attr.contains( "person" ) );
            assertTrue( attr.contains( "organizationalPerson" ) );
            assertTrue( attr.contains( "residentialPerson" ) );
        }


        public void testAlterObjectClassesOverlappingAndDsa() throws NamingException
        {
            Attribute attr = new AttributeImpl( "objectClass", "organizationalPerson" );
            attr.add( "residentialPerson" );
            attr.add( "dSA" );
            SchemaInterceptor.alterObjectClasses( attr, registries.getObjectClassRegistry() );
            assertEquals( 5, attr.size() );
            assertTrue( attr.contains( "person" ) );
            assertTrue( attr.contains( "organizationalPerson" ) );
            assertTrue( attr.contains( "residentialPerson" ) );
            assertTrue( attr.contains( "dSA" ) );
            assertTrue( attr.contains( "applicationEntity" ) );
        }
        */
}
