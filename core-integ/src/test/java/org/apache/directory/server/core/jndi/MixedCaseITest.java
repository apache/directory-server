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


import static org.apache.directory.server.core.integ.IntegrationUtils.getContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.directory.shared.schema.loader.ldif.JarLdifSchemaLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests various operations against a partition whose suffix contains both upper and lower case letters.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith(CiRunner.class)
@CleanupLevel(Level.CLASS)
@Factory(MixedCaseITest.MyFactory.class)
public class MixedCaseITest
{
    public static DirectoryService service;

    private static final String SUFFIX_DN = "dc=Apache,dc=Org";

    public static class MyFactory implements DirectoryServiceFactory
    {
        public DirectoryService newInstance() throws Exception
        {
            String workingDirectory = System.getProperty( "workingDirectory" );

            if ( workingDirectory == null )
            {
                String path = DirectoryServiceFactory.class.getResource( "" ).getPath();
                int targetPos = path.indexOf( "target" );
                workingDirectory = path.substring( 0, targetPos + 6 ) + "/server-work";
            }

            service = new DefaultDirectoryService();
            service.setWorkingDirectory( new File( workingDirectory ) );

            return service;
        }


        public void init() throws Exception
        {
            SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();

            // Init the LdifPartition
            LdifPartition ldifPartition = new LdifPartition();

            String workingDirectory = service.getWorkingDirectory().getPath();

            ldifPartition.setWorkingDirectory( workingDirectory + "/schema" );

            // Extract the schema on disk (a brand new one) and load the registries
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );

            schemaPartition.setWrappedPartition( ldifPartition );

            JarLdifSchemaLoader loader = new JarLdifSchemaLoader();

            SchemaManager schemaManager = new DefaultSchemaManager( loader );
            service.setSchemaManager( schemaManager );

            schemaManager.loadAllEnabled();

            List<Throwable> errors = schemaManager.getErrors();

            if ( errors.size() != 0 )
            {
                fail( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
            }

            schemaPartition.setSchemaManager( schemaManager );

            extractor.extractOrCopy();

            service.getChangeLog().setEnabled( true );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            // Inject the System Partition
            Partition systemPartition = new JdbmPartition();
            systemPartition.setId( "system" );
            ( ( JdbmPartition ) systemPartition ).setCacheSize( 500 );
            systemPartition.setSuffix( ServerDNConstants.SYSTEM_DN );
            systemPartition.setSchemaManager( schemaManager );
            ( ( JdbmPartition ) systemPartition ).setPartitionDir( new File( workingDirectory, "system" ) );

            // Add objectClass attribute for the system partition
            Set<Index<?, ServerEntry>> indexedAttrs = new HashSet<Index<?, ServerEntry>>();
            indexedAttrs.add( new JdbmIndex<Object, ServerEntry>( SchemaConstants.OBJECT_CLASS_AT ) );
            ( ( JdbmPartition ) systemPartition ).setIndexedAttributes( indexedAttrs );

            service.setSystemPartition( systemPartition );

            JdbmPartition partition = new JdbmPartition();
            partition.setId( "apache" );
            partition.setSuffix( SUFFIX_DN );
            partition.setPartitionDir( new File( workingDirectory, "apache" ) );

            HashSet<Index<?, ServerEntry>> indexedAttributes = new HashSet<Index<?, ServerEntry>>();
            indexedAttributes.add( new JdbmIndex<String, ServerEntry>( "objectClass" ) );
            indexedAttributes.add( new JdbmIndex<String, ServerEntry>( "ou" ) );
            indexedAttributes.add( new JdbmIndex<String, ServerEntry>( "uid" ) );
            partition.setIndexedAttributes( indexedAttributes );

            service.addPartition( partition );
        }
    }


    @Before
    public void setUp() throws Exception
    {
        LdapDN dn = new LdapDN( "dc=Apache,dc=Org" );
        ServerEntry entry = service.newEntry( dn );
        entry.add( "objectClass", "top", "domain", "extensibleObject" );
        entry.add( "dc", "Apache" );
        service.getAdminSession().add( entry );
    }


    @Test
    public void testSearch() throws Exception
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", service, SUFFIX_DN );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.SUBTREE_SCOPE );

        NamingEnumeration<SearchResult> ne = ctxRoot.search( "", "(objectClass=*)", sc );
        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = ne.next();
        assertEquals( "The entry returned should be the root entry.", SUFFIX_DN, sr.getName() );
        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }


    @Test
    public void testAdd() throws Exception
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", service, SUFFIX_DN );

        String dn = "ou=Test";

        Attributes attributes = AttributeUtils.createAttributes( "objectClass: top", "objectClass: organizationalUnit",
            "ou: Test" );

        DirContext ctx = ctxRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration<SearchResult> ne = ctxRoot.search( dn, "(objectClass=*)", sc );
        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = ne.next();
        assertEquals( "The entry returned should be the entry added earlier.", dn + "," + SUFFIX_DN, sr.getName() );
        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }


    @Test
    public void testModify() throws Exception
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", service, SUFFIX_DN );

        String dn = "ou=Test";
        String description = "New Value";

        Attributes attributes = AttributeUtils.createAttributes( "objectClass: top", "objectClass: organizationalUnit",
            "ou: Test", "description: Old Value" );

        DirContext ctx = ctxRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        ModificationItem[] mods = new ModificationItem[1];
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new BasicAttribute( "description", description ) );

        ctxRoot.modifyAttributes( dn, mods );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration<SearchResult> ne = ctxRoot.search( dn, "(objectClass=*)", sc );
        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = ( SearchResult ) ne.next();
        assertEquals( "The entry returned should be the entry added earlier.", dn + "," + SUFFIX_DN, sr.getName() );

        attributes = sr.getAttributes();
        Attribute attribute = attributes.get( "description" );

        assertEquals( "The description attribute should contain the new value.", description, attribute.get() );
        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }


    @Test
    public void testDelete() throws Exception
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", service, SUFFIX_DN );

        String dn = "ou=Test";

        Attributes attributes = AttributeUtils.createAttributes( "objectClass: top", "objectClass: organizationalUnit",
            "ou: Test" );

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
