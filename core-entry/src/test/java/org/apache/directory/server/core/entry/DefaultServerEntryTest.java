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
package org.apache.directory.server.core.entry;

import java.util.HashSet;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachemetaSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.registries.DefaultOidRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test the DefaultServerEntry class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultServerEntryTest
{
    private static BootstrapSchemaLoader loader;
    private static Registries registries;
    private static OidRegistry oidRegistry;
    
    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeClass
    public static void setup() throws NamingException
    {
        loader = new BootstrapSchemaLoader();
        oidRegistry = new DefaultOidRegistry();
        registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );
        
        // load essential bootstrap schemas 
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
    }


    /**
     * Test a conversion from a ServerEntry to an AttributesImpl
     */
    @Test public void testToAttributesImpl() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( dn, registries );
        
        ObjectClassAttribute oc = new ObjectClassAttribute( registries );
        oc.add( "top", "person", "inetOrgPerson" );
        
        entry.addObjectClass( oc );
        entry.put( "cn", registries.getAttributeTypeRegistry().lookup( "cn" ), "test" );
        
        Attributes attributes = ServerEntryUtils.toAttributesImpl( entry );
        
        assertNotNull( attributes );
        assertTrue( attributes instanceof AttributesImpl );
        
        Set<String> expected = new HashSet<String>();
        expected.add( "objectClass" );
        expected.add( "cn" );
     
        for ( NamingEnumeration<String> ids = attributes.getIDs(); ids.hasMoreElements();)
        {
            String id = ids.nextElement();
            
            assertTrue( expected.contains( id ) );
            expected.remove( id );
            
        }

        assertEquals( 0, expected.size() );
    }


    /**
     * Test a conversion from a ServerEntry to an BasicAttributes
     */
    @Test public void testToBasicAttributes() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( dn, registries );
        
        ObjectClassAttribute oc = new ObjectClassAttribute( registries );
        oc.add( "top", "person", "inetOrgPerson" );
        
        entry.addObjectClass( oc );
        entry.put( "cn", registries.getAttributeTypeRegistry().lookup( "cn" ), "test" );
        
        Attributes attributes = ServerEntryUtils.toBasicAttributes( entry );
        
        assertNotNull( attributes );
        assertTrue( attributes instanceof BasicAttributes );
        
        Set<String> expected = new HashSet<String>();
        expected.add( "objectClass" );
        expected.add( "cn" );
     
        for ( NamingEnumeration<String> ids = attributes.getIDs(); ids.hasMoreElements();)
        {
            String id = ids.nextElement();
            
            assertTrue( expected.contains( id ) );
            expected.remove( id );
            
        }

        assertEquals( 0, expected.size() );
    }
}
