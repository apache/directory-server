/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.schema;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.server.core.schema.SchemaService;
import org.apache.directory.server.core.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.core.schema.bootstrap.BootstrapRegistries;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.core.schema.bootstrap.CoreSchema;
import org.apache.directory.server.core.schema.bootstrap.CosineSchema;
import org.apache.directory.server.core.schema.bootstrap.InetorgpersonSchema;
import org.apache.directory.server.core.schema.bootstrap.SystemSchema;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;

import junit.framework.TestCase;


/**
 * Tests methods in SchemaService.
 * 
 */
public class SchemaServiceTest extends TestCase
{
    BootstrapRegistries registries = new BootstrapRegistries();


    public void setUp() throws Exception
    {
        registries = new BootstrapRegistries();
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        loader.load( new SystemSchema(), registries );
        loader.load( new ApacheSchema(), registries );
        loader.load( new CoreSchema(), registries );
        loader.load( new CosineSchema(), registries );
        loader.load( new InetorgpersonSchema(), registries );
    }

    
    public void testDescendants() throws Exception
    {
        AttributeTypeRegistry attrRegistry = registries.getAttributeTypeRegistry();
        Iterator list = attrRegistry.descendants( "name" );
        Set nameAttrs = new HashSet();
        while ( list.hasNext() )
        {
            AttributeType type = ( AttributeType ) list.next();
            nameAttrs.add( type.getName() );
        }
        assertEquals( "size of attributes extending name", 13, nameAttrs.size() );
        assertTrue( nameAttrs.contains( "dmdName" ) );
        assertTrue( nameAttrs.contains( "o" ) );
        assertTrue( nameAttrs.contains( "c" ) );
        assertTrue( nameAttrs.contains( "initials" ) );
        assertTrue( nameAttrs.contains( "ou" ) );
        assertTrue( nameAttrs.contains( "sn" ) );
        assertTrue( nameAttrs.contains( "title" ) );
        assertTrue( nameAttrs.contains( "l" ) );
        assertTrue( nameAttrs.contains( "apacheExistance" ) );
        assertTrue( nameAttrs.contains( "cn" ) );
        assertTrue( nameAttrs.contains( "st" ) );
        assertTrue( nameAttrs.contains( "givenName" ) );
    }
    

    public void testAlterObjectClassesBogusAttr() throws NamingException
    {
        Attribute attr = new BasicAttribute( "blah", "blah" );

        try
        {
            SchemaService.alterObjectClasses( attr, registries.getObjectClassRegistry() );
            fail( "should not get here" );
        }
        catch ( LdapNamingException e )
        {
            assertEquals( ResultCodeEnum.OPERATIONSERROR, e.getResultCode() );
        }

        attr = new BasicAttribute( "objectClass" );
        SchemaService.alterObjectClasses( attr, registries.getObjectClassRegistry() );
        assertEquals( 0, attr.size() );
    }


    public void testAlterObjectClassesNoAttrValue() throws NamingException
    {
        Attribute attr = new BasicAttribute( "objectClass" );
        SchemaService.alterObjectClasses( attr, registries.getObjectClassRegistry() );
        assertEquals( 0, attr.size() );
    }


    public void testAlterObjectClassesTopAttrValue() throws NamingException
    {
        Attribute attr = new BasicAttribute( "objectClass", "top" );
        SchemaService.alterObjectClasses( attr, registries.getObjectClassRegistry() );
        assertEquals( 0, attr.size() );
    }


    public void testAlterObjectClassesInetOrgPersonAttrValue() throws NamingException
    {
        Attribute attr = new BasicAttribute( "objectClass", "inetOrgPerson" );
        SchemaService.alterObjectClasses( attr, registries.getObjectClassRegistry() );
        assertEquals( 3, attr.size() );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );
        assertTrue( attr.contains( "inetOrgPerson" ) );
    }


    public void testAlterObjectClassesOverlapping() throws NamingException
    {
        Attribute attr = new BasicAttribute( "objectClass", "inetOrgPerson" );
        attr.add( "residentialPerson" );
        SchemaService.alterObjectClasses( attr, registries.getObjectClassRegistry() );
        assertEquals( 4, attr.size() );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );
        assertTrue( attr.contains( "inetOrgPerson" ) );
        assertTrue( attr.contains( "residentialPerson" ) );
    }


    public void testAlterObjectClassesOverlappingAndDsa() throws NamingException
    {
        Attribute attr = new BasicAttribute( "objectClass", "inetOrgPerson" );
        attr.add( "residentialPerson" );
        attr.add( "dSA" );
        SchemaService.alterObjectClasses( attr, registries.getObjectClassRegistry() );
        assertEquals( 6, attr.size() );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );
        assertTrue( attr.contains( "inetOrgPerson" ) );
        assertTrue( attr.contains( "residentialPerson" ) );
        assertTrue( attr.contains( "dSA" ) );
        assertTrue( attr.contains( "applicationEntity" ) );
    }
}
