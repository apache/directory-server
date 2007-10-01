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
package org.apache.directory.server.jndi;


import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;


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
        Attributes attrs;
        Set<Index> indexedAttrs;
        Set<Partition> partitions = new HashSet<Partition>();

        // Add partition 'testing'
        JdbmPartition partition = new JdbmPartition();
        partition.setId( "testing" );
        partition.setSuffix( "ou=testing" );

        indexedAttrs = new HashSet<Index>();
        indexedAttrs.add( new JdbmIndex( "ou" ) );
        indexedAttrs.add( new JdbmIndex( "objectClass" ) );
        partition.setIndexedAttributes( indexedAttrs );

        attrs = new AttributesImpl( true );
        Attribute attr = new AttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "organizationalUnit" );
        attr.add( "extensibleObject" );
        attrs.put( attr );
        attr = new AttributeImpl( "ou" );
        attr.add( "testing" );
        attrs.put( attr );
        partition.setContextEntry( attrs );

        partitions.add( partition );

        // Add partition 'example'
        partition = new JdbmPartition();
        partition.setId( "example" );
        partition.setSuffix( "dc=example" );

        indexedAttrs = new HashSet<Index>();
        indexedAttrs.add( new JdbmIndex( "ou" ) );
        indexedAttrs.add( new JdbmIndex( "dc" ) );
        indexedAttrs.add( new JdbmIndex( "objectClass" ) );
        partition.setIndexedAttributes( indexedAttrs );

        attrs = new AttributesImpl( true );
        attr = new AttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attr.add( "extensibleObject" );
        attrs.put( attr );
        attr = new AttributeImpl( "dc" );
        attr.add( "example" );
        attrs.put( attr );
        partition.setContextEntry( attrs );

        partitions.add( partition );

        // Add partition 'MixedCase'
        partition = new JdbmPartition();
        partition.setId( "mixedcase" );
        partition.setSuffix( "dc=MixedCase" );

        indexedAttrs = new HashSet<Index>();
        indexedAttrs.add( new JdbmIndex( "dc" ) );
        indexedAttrs.add( new JdbmIndex( "objectClass" ) );
        partition.setIndexedAttributes( indexedAttrs );

        attrs = new AttributesImpl( true );
        attr = new AttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attr.add( "extensibleObject" );
        attrs.put( attr );
        attr = new AttributeImpl( "dc" );
        attr.add( "MixedCase" );
        attrs.put( attr );
        partition.setContextEntry( attrs );

        partitions.add( partition );

        configuration.setPartitions( partitions );

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
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.jndi.ServerContextFactory" );

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
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.jndi.ServerContextFactory" );

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
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.jndi.ServerContextFactory" );

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
