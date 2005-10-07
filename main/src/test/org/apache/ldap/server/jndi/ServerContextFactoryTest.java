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
package org.apache.ldap.server.jndi;


import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.ldap.server.AbstractAdminTestCase;
import org.apache.ldap.server.configuration.MutableDirectoryPartitionConfiguration;


/**
 * Tests to see if we can fire up the Eve directory server via JNDI.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerContextFactoryTest extends AbstractAdminTestCase
{
    public ServerContextFactoryTest()
    {
    }

    public void setUp() throws Exception
    {
        BasicAttributes attrs;
        Set indexedAttrs;
        Set pcfgs = new HashSet();

        MutableDirectoryPartitionConfiguration pcfg;
        
        // Add partition 'testing'
        pcfg = new MutableDirectoryPartitionConfiguration();
        pcfg.setName( "testing" );
        pcfg.setSuffix( "ou=testing" );
        
        indexedAttrs = new HashSet();
        indexedAttrs.add( "ou" );
        indexedAttrs.add( "objectClass" );
        pcfg.setIndexedAttributes( indexedAttrs );

        attrs = new BasicAttributes( true );
        BasicAttribute attr = new BasicAttribute( "objectClass" );
        attr.add( "top" );
        attr.add( "organizationalUnit" );
        attr.add( "extensibleObject" );
        attrs.put( attr );
        attr = new BasicAttribute( "ou" );
        attr.add( "testing" );
        attrs.put( attr );
        pcfg.setContextEntry( attrs );
        
        pcfgs.add( pcfg );
        
        // Add partition 'example'
        pcfg = new MutableDirectoryPartitionConfiguration();
        pcfg.setName( "example" );
        pcfg.setSuffix( "dc=example" );
        
        indexedAttrs = new HashSet();
        indexedAttrs.add( "ou" );
        indexedAttrs.add( "dc" );
        indexedAttrs.add( "objectClass" );
        pcfg.setIndexedAttributes( indexedAttrs );
        
        attrs = new BasicAttributes( true );
        attr = new BasicAttribute( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attr.add( "extensibleObject" );
        attrs.put( attr );
        attr = new BasicAttribute( "dc" );
        attr.add( "example" );
        attrs.put( attr );
        pcfg.setContextEntry( attrs );
        
        pcfgs.add( pcfg );

        // Add partition 'MixedCase'
        pcfg = new MutableDirectoryPartitionConfiguration();
        pcfg.setName( "mixedcase" );
        pcfg.setSuffix( "dc=MixedCase" );
        
        indexedAttrs = new HashSet();
        indexedAttrs.add( "dc" );
        indexedAttrs.add( "objectClass" );
        pcfg.setIndexedAttributes( indexedAttrs );

        attrs = new BasicAttributes( true );
        attr = new BasicAttribute( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attr.add( "extensibleObject" );
        attrs.put( attr );
        attr = new BasicAttribute( "dc" );
        attr.add( "MixedCase" );
        attrs.put( attr );
        pcfg.setContextEntry( attrs );
        
        pcfgs.add( pcfg );
        
        configuration.setContextPartitionConfigurations( pcfgs );

        super.setUp();
    }

    /**
     * Makes sure the system context has the right attributes and values.
     *
     * @throws NamingException if there are failures
     */
    public void testSystemContext() throws NamingException
    {
        assertNotNull( sysRoot );

        Attributes attributes = sysRoot.getAttributes( "" );

        assertNotNull( attributes );

        assertEquals( "system", attributes.get( "ou" ).get() );

        Attribute attribute = attributes.get( "objectClass" );

        assertNotNull( attribute );

        assertTrue( attribute.contains( "top" ) );

        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    /**
     * Tests to make sure tearDown is working correctly.
     *
     * @throws NamingException if there are failures
     */
    public void testSetupTeardown() throws NamingException
    {
        assertNotNull( sysRoot );

        Attributes attributes = sysRoot.getAttributes( "" );

        assertNotNull( attributes );

        assertEquals( "system", attributes.get( "ou" ).get() );

        Attribute attribute = attributes.get( "objectClass" );

        assertNotNull( attribute );

        assertTrue( attribute.contains( "top" ) );

        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    public void testAppPartitionExample() throws NamingException
    {
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );

        env.put( Context.PROVIDER_URL, "dc=example" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.ServerContextFactory" );

        InitialContext initialContext = new InitialContext( env );

        DirContext appRoot = ( DirContext ) initialContext.lookup( "" );

        assertNotNull( appRoot );

        Attributes attributes = appRoot.getAttributes( "" );

        assertNotNull( attributes );

        assertEquals( "example", attributes.get( "dc" ).get() );

        Attribute attribute = attributes.get( "objectClass" );

        assertNotNull( attribute );

        assertTrue( attribute.contains( "top" ) );

        assertTrue( attribute.contains( "domain" ) );
    }


    public void testAppPartitionTesting() throws NamingException
    {
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );

        env.put( Context.PROVIDER_URL, "ou=testing" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.ServerContextFactory" );

        InitialContext initialContext = new InitialContext( env );

        DirContext appRoot = ( DirContext ) initialContext.lookup( "" );

        assertNotNull( appRoot );

        Attributes attributes = appRoot.getAttributes( "" );

        assertNotNull( attributes );

        assertEquals( "testing", attributes.get( "ou" ).get() );

        Attribute attribute = attributes.get( "objectClass" );

        assertNotNull( attribute );

        assertTrue( attribute.contains( "top" ) );

        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    public void testAppPartitionMixedCase() throws NamingException
    {
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );

        env.put( Context.PROVIDER_URL, "dc=MixedCase" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.ServerContextFactory" );

        InitialContext initialContext = new InitialContext( env );

        DirContext appRoot = ( DirContext ) initialContext.lookup( "" );

        assertNotNull( appRoot );

        Attributes attributes = appRoot.getAttributes( "" );

        assertNotNull( attributes );

        assertEquals( "MixedCase", attributes.get( "dc" ).get() );

        Attribute attribute = attributes.get( "objectClass" );

        assertNotNull( attribute );

        assertTrue( attribute.contains( "top" ) );

        assertTrue( attribute.contains( "domain" ) );
    }
}
