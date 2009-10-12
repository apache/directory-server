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
import java.util.List;
import java.util.Set;

import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.directory.shared.schema.loader.ldif.LdifSchemaLoader;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests methods in SchemaInterceptor.
 */
public class SchemaServiceTest
{
    private static Registries registries;
    
    
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
        SchemaLdifExtractor extractor = new SchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        registries = new Registries();

        List<Throwable> errors = loader.loadAllEnabled( registries, true );
        
        if ( errors.size() != 0 )
        {
            fail( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
        }
        
        errors = loader.loadWithDependencies( loader.getSchema( "nis" ), registries, true );
        
        if ( errors.size() != 0 )
        {
            fail( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
        }
    }

    
    @Test
    public void testDescendants() throws Exception
    {
        AttributeTypeRegistry attrRegistry = registries.getAttributeTypeRegistry();
        Iterator<AttributeType> list = attrRegistry.descendants( "name" );
        Set<String> nameAttrs = new HashSet<String>();
        
        while ( list.hasNext() )
        {
            AttributeType type = list.next();
            nameAttrs.add( type.getName() );
        }
        
        // We should only have 13 AT
        String[] expectedNames = new String[]
        {
            "sn", 
            "generationQualifier", 
            "ou", 
            "c", 
            "o", 
            "l", 
            "c-st", 
            "givenName", 
            "title", 
            "cn", 
            "initials", 
            "dmdName", 
            "c-ou", 
            "c-o", 
            "apacheExistence", 
            "st", 
            "c-l"
        };
        
        for ( String name : expectedNames )
        {
            if ( nameAttrs.contains( name) )
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
