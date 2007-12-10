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
package org.apache.directory.server.core.jndi;


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import static org.apache.directory.server.core.integ.IntegrationUtils.getContext;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import java.util.HashSet;
import java.util.Set;


/**
 * Tests various operations against a partition whose suffix contains both upper and lower case letters.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@CleanupLevel ( Level.CLASS )
@Factory ( MixedCaseITest.MyFactory.class )
public class MixedCaseITest
{
    public static DirectoryService service;

    private static final String SUFFIX = "dc=Apache,dc=Org";


    public static class MyFactory implements DirectoryServiceFactory
    {
        public DirectoryService newInstance()
        {
            DirectoryService service = new DefaultDirectoryService();
            service.getChangeLog().setEnabled( true );

            JdbmPartition partition = new JdbmPartition();
            partition.setId( "apache" );
            partition.setSuffix( SUFFIX );

            HashSet<Index> indexedAttributes = new HashSet<Index>();
            indexedAttributes.add( new JdbmIndex( "objectClass" ) );
            indexedAttributes.add( new JdbmIndex( "ou" ) );
            indexedAttributes.add( new JdbmIndex( "uid" ) );
            partition.setIndexedAttributes( indexedAttributes );

            Attributes attrs = new AttributesImpl( true );
            Attribute objectClass = new AttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "domain" );
            objectClass.add( "extensibleObject" );
            attrs.put( objectClass );
            attrs.put( "dc", "Apache" );

            partition.setContextEntry( attrs );

            Set<Partition> partitions = new HashSet<Partition>();
            partitions.add( partition );

            service.setPartitions( partitions );
            return service;
        }
    }

    
    @Test
    public void testSearch() throws NamingException
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", service, SUFFIX );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.SUBTREE_SCOPE );

        NamingEnumeration ne = ctxRoot.search( "", "(objectClass=*)", sc );
        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = ( SearchResult ) ne.next();
        assertEquals( "The entry returned should be the root entry.", SUFFIX, sr.getName() );
        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }


    @Test
    public void testAdd() throws NamingException
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", service, SUFFIX );

        String dn = "ou=Test";

        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "Test" );

        DirContext ctx = ctxRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration ne = ctxRoot.search( dn, "(objectClass=*)", sc );
        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = ( SearchResult ) ne.next();
        assertEquals( "The entry returned should be the entry added earlier.", dn + "," + SUFFIX, sr.getName() );
        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }


    @Test
    public void testModify() throws NamingException
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", service, SUFFIX );

        String dn = "ou=Test";
        String description = "New Value";

        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "Test" );
        attributes.put( "description", "Old Value" );

        DirContext ctx = ctxRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl( "description", description ) );

        ctxRoot.modifyAttributes( dn, mods );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration ne = ctxRoot.search( dn, "(objectClass=*)", sc );
        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = ( SearchResult ) ne.next();
        assertEquals( "The entry returned should be the entry added earlier.", dn + "," + SUFFIX, sr.getName() );

        attributes = sr.getAttributes();
        attribute = attributes.get( "description" );

        assertEquals( "The description attribute should contain the new value.", description, attribute.get() );
        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }


    @Test
    public void testDelete() throws NamingException
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", service, SUFFIX );

        String dn = "ou=Test";

        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "Test" );

        DirContext ctx = ctxRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        ctxRoot.destroySubcontext( dn );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        try
        {
            ctxRoot.search( dn, "(objectClass=*)", sc );
            fail( "Search should throw exception." );
        }
        catch ( LdapNameNotFoundException e )
        {
            // ignore
        }
    }
}
