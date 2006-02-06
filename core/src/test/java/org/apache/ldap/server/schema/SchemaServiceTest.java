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
package org.apache.ldap.server.schema;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.server.schema.bootstrap.ApacheSchema;
import org.apache.ldap.server.schema.bootstrap.BootstrapRegistries;
import org.apache.ldap.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.ldap.server.schema.bootstrap.CoreSchema;
import org.apache.ldap.server.schema.bootstrap.CosineSchema;
import org.apache.ldap.server.schema.bootstrap.InetorgpersonSchema;
import org.apache.ldap.server.schema.bootstrap.SystemSchema;

import junit.framework.TestCase;


/**
 * Tests methods in SchemaService.
 * 
 */
public class SchemaServiceTest extends TestCase
{
    ObjectClassRegistry registry = null;
    
    
    public void setUp() throws Exception
    {
        if ( registry != null )
        {
            return;
        }
        
        BootstrapRegistries registries = new BootstrapRegistries();
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        loader.load( new ApacheSchema(), registries );
        loader.load( new CoreSchema(), registries );
        loader.load( new CosineSchema(), registries );
        loader.load( new InetorgpersonSchema(), registries );
        loader.load( new SystemSchema(), registries );
        registry = registries.getObjectClassRegistry();
    }
    
    
    public void testAlterObjectClassesBogusAttr() throws NamingException 
    {
        Attribute attr = new BasicAttribute( "blah", "blah" );
        
        try
        {
            SchemaService.alterObjectClasses( attr, registry );
            fail( "should not get here" );
        }
        catch ( LdapNamingException e )
        {
            assertEquals( ResultCodeEnum.OPERATIONSERROR, e.getResultCode() );
        }

        attr = new BasicAttribute( "objectClass" );
        SchemaService.alterObjectClasses( attr, registry );
        assertEquals( 0, attr.size() );
    }
    
    
    public void testAlterObjectClassesNoAttrValue() throws NamingException 
    {
        Attribute attr = new BasicAttribute( "objectClass" );
        SchemaService.alterObjectClasses( attr, registry );
        assertEquals( 0, attr.size() );
    }
    
    
    public void testAlterObjectClassesTopAttrValue() throws NamingException 
    {
        Attribute attr = new BasicAttribute( "objectClass", "top" );
        SchemaService.alterObjectClasses( attr, registry );
        assertEquals( 0, attr.size() );
    }
    
    
    public void testAlterObjectClassesInetOrgPersonAttrValue() throws NamingException 
    {
        Attribute attr = new BasicAttribute( "objectClass", "inetOrgPerson" );
        SchemaService.alterObjectClasses( attr, registry );
        assertEquals( 3, attr.size() );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );
        assertTrue( attr.contains( "inetOrgPerson" ) );
    }


    public void testAlterObjectClassesOverlapping() throws NamingException 
    {
        Attribute attr = new BasicAttribute( "objectClass", "inetOrgPerson" );
        attr.add( "residentialPerson" );
        SchemaService.alterObjectClasses( attr, registry );
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
        SchemaService.alterObjectClasses( attr, registry );
        assertEquals( 6, attr.size() );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );
        assertTrue( attr.contains( "inetOrgPerson" ) );
        assertTrue( attr.contains( "residentialPerson" ) );
        assertTrue( attr.contains( "dSA" ) );
        assertTrue( attr.contains( "applicationEntity" ) );
    }
}
