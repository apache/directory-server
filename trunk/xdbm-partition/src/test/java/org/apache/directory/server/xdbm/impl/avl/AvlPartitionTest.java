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
package org.apache.directory.server.xdbm.impl.avl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Iterator;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.StoreUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Unit test cases for AvlStore
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlPartitionTest
{
    private static final Logger LOG = LoggerFactory.getLogger( AvlPartitionTest.class );

    private static AvlPartition partition;
    private static SchemaManager schemaManager = null;
    private static DnFactory dnFactory;
    private static Dn EXAMPLE_COM;

    /** The OU AttributeType instance */
    private static AttributeType OU_AT;

    /** The SN AttributeType instance */
    private static AttributeType SN_AT;

    /** The DC AttributeType instance */
    private static AttributeType DC_AT;

    /** The ApacheAlias AttributeType instance */
    private static AttributeType APACHE_ALIAS_AT;

    private static CacheService cacheService;
    
    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = AvlPartitionTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );

        schemaManager = new DefaultSchemaManager( loader );
        cacheService = new CacheService();
        cacheService.initialize( null );
        dnFactory = new DefaultDnFactory( schemaManager, cacheService.getCache( "dnCache" ) );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        EXAMPLE_COM = new Dn( schemaManager, "dc=example,dc=com" );

        OU_AT = schemaManager.getAttributeType( "ou" );
        SN_AT = schemaManager.getAttributeType( "sn" );
        DC_AT = schemaManager.getAttributeType( "dc" );
        APACHE_ALIAS_AT = schemaManager.getAttributeType( "apacheAlias" );
    }


    @Before
    public void createStore() throws Exception
    {
        // initialize the partition
        partition = new AvlPartition( schemaManager, dnFactory );
        partition.setId( "example" );
        partition.setSyncOnWrite( false );

        partition.addIndex( new AvlIndex<String>( SchemaConstants.OU_AT_OID ) );
        partition.addIndex( new AvlIndex<String>( SchemaConstants.UID_AT_OID ) );
        partition.setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );

        partition.setCacheService( cacheService );
        partition.initialize();

        StoreUtils.loadExampleData( partition, schemaManager );
        LOG.debug( "Created new partition" );
    }


    @After
    public void destroyStore() throws Exception
    {
        partition.destroy();
    }


    @Test
    public void testSimplePropertiesUnlocked() throws Exception
    {
        AvlPartition avlPartition = new AvlPartition( schemaManager, dnFactory );
        avlPartition.setSyncOnWrite( true ); // for code coverage

        assertNull( avlPartition.getAliasIndex() );
        avlPartition.addIndex( new AvlIndex<Dn>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) );
        assertNotNull( avlPartition.getAliasIndex() );

        assertEquals( 0, avlPartition.getCacheSize() );

        assertNull( avlPartition.getPresenceIndex() );
        avlPartition.addIndex( new AvlIndex<String>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID ) );
        assertNotNull( avlPartition.getPresenceIndex() );

        assertNull( avlPartition.getId() );
        avlPartition.setId( "foo" );
        assertEquals( "foo", avlPartition.getId() );

        assertNull( avlPartition.getRdnIndex() );
        avlPartition.addIndex( new AvlRdnIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID ) );
        assertNotNull( avlPartition.getRdnIndex() );

        assertNull( avlPartition.getOneAliasIndex() );
        avlPartition.addIndex( new AvlIndex<Long>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ) );
        assertNotNull( avlPartition.getOneAliasIndex() );

        assertNull( avlPartition.getSubAliasIndex() );
        avlPartition.addIndex( new AvlIndex<Long>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ) );
        assertNotNull( avlPartition.getSubAliasIndex() );

        assertNull( avlPartition.getSuffixDn() );
        avlPartition.setSuffixDn( EXAMPLE_COM );
        assertEquals( "dc=example,dc=com", avlPartition.getSuffixDn().getName() );

        assertNotNull( avlPartition.getSuffixDn() );

        assertFalse( avlPartition.getUserIndices().hasNext() );
        avlPartition.addIndex( new AvlIndex<Object>( "2.5.4.3" ) );
        assertTrue( avlPartition.getUserIndices().hasNext() );

        assertNull( avlPartition.getPartitionPath() );
        avlPartition.setPartitionPath( new File( "." ).toURI() );
        assertNull( avlPartition.getPartitionPath() );

        assertFalse( avlPartition.isInitialized() );
        assertFalse( avlPartition.isSyncOnWrite() );
        avlPartition.setSyncOnWrite( false );
        assertFalse( avlPartition.isSyncOnWrite() );

        avlPartition.sync();
        avlPartition.destroy();
    }


    @Test
    public void testSimplePropertiesLocked() throws Exception
    {
        assertNotNull( partition.getAliasIndex() );

        try
        {
            partition.addIndex( new AvlIndex<Dn>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) );
            //fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertEquals( 0, partition.getCacheSize() );
        assertNotNull( partition.getPresenceIndex() );

        try
        {
            partition.addIndex( new AvlIndex<String>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID ) );
            //fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getId() );

        try
        {
            partition.setId( "foo" );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getRdnIndex() );

        try
        {
            partition.addIndex( new AvlRdnIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID ) );
            //fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getOneAliasIndex() );

        try
        {
            partition.addIndex( new AvlIndex<Long>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ) );
            //fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getSubAliasIndex() );

        try
        {
            partition.addIndex( new AvlIndex<Long>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ) );
            //fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getSuffixDn() );

        Iterator<String> systemIndices = partition.getSystemIndices();

        for ( int i = 0; i < 8; i++ )
        {
            assertTrue( systemIndices.hasNext() );
            assertNotNull( systemIndices.next() );
        }

        assertFalse( systemIndices.hasNext() );
        assertNotNull( partition.getSystemIndex( APACHE_ALIAS_AT ) );

        try
        {
            partition.getSystemIndex( SN_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }

        try
        {
            partition.getSystemIndex( DC_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }

        assertNotNull( partition.getSuffixDn() );

        Iterator<String> userIndices = partition.getUserIndices();
        int count = 0;

        while ( userIndices.hasNext() )
        {
            userIndices.next();
            count++;
        }

        assertEquals( 2, count );
        assertFalse( partition.hasUserIndexOn( DC_AT ) );
        assertTrue( partition.hasUserIndexOn( OU_AT ) );
        assertTrue( partition.hasSystemIndexOn( APACHE_ALIAS_AT ) );
        userIndices = partition.getUserIndices();
        assertTrue( userIndices.hasNext() );
        assertNotNull( userIndices.next() );
        assertTrue( userIndices.hasNext() );
        assertNotNull( userIndices.next() );
        assertFalse( userIndices.hasNext() );
        assertNotNull( partition.getUserIndex( OU_AT ) );

        try
        {
            partition.getUserIndex( SN_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }

        try
        {
            partition.getUserIndex( DC_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }

        assertNull( partition.getPartitionPath() );
        assertTrue( partition.isInitialized() );
        assertFalse( partition.isSyncOnWrite() );

        partition.sync();
    }


    @Test
    public void testFreshStore() throws Exception
    {
        Dn dn = new Dn( schemaManager, "o=Good Times Co." );
        assertEquals( Strings.getUUID( 1L ), partition.getEntryId( dn ) );
        assertEquals( 11, partition.count() );

        // note that the suffix entry returns 0 for it's parent which does not exist
        assertEquals( Strings.getUUID( 0L ), partition.getParentId( partition.getEntryId( dn ) ) );
        assertNull( partition.getParentId( Strings.getUUID( 0L ) ) );

        // should be allowed
        partition.delete( Strings.getUUID( 1L ) );
    }


    @Test(expected = LdapNoSuchObjectException.class)
    public void testAddWithoutParentId() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Marting King,ou=Not Present,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Not Present" );
        entry.add( "cn", "Martin King" );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        partition.add( addContext );
    }


    @Test(expected = LdapSchemaViolationException.class)
    public void testAddWithoutObjectClass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Martin King,ou=Sales,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "ou", "Sales" );
        entry.add( "cn", "Martin King" );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        partition.add( addContext );
    }


    @Test
    public void testModifyAddOUAttrib() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );
        attrib.add( "Engineering" );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        partition.modify( dn, add );
    }


    @Test
    public void testRename() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Pivate Ryan,ou=Engineering,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Engineering" );
        entry.add( "cn", "Private Ryan" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        partition.add( addContext );

        Rdn rdn = new Rdn( "sn=James" );

        partition.rename( dn, rdn, true, null );
    }


    @Test
    public void testRenameEscaped() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Private Ryan,ou=Engineering,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Engineering" );
        entry.add( "cn", "Private Ryan" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        partition.add( addContext );

        Rdn rdn = new Rdn( "sn=Ja\\+es" );

        partition.rename( dn, rdn, true, null );

        Dn dn2 = new Dn( schemaManager, "sn=Ja\\+es,ou=Engineering,o=Good Times Co." );
        String id = partition.getEntryId( dn2 );
        assertNotNull( id );
        Entry entry2 = partition.fetch( id );
        assertEquals( "Ja+es", entry2.get( "sn" ).getString() );
        assertEquals( "ja+es", entry2.get( "sn" ).get().getNormValue() );
    }


    @Test
    public void testMove() throws Exception
    {
        Dn childDn = new Dn( schemaManager, "cn=Private Ryan,ou=Engineering,o=Good Times Co." );
        DefaultEntry childEntry = new DefaultEntry( schemaManager, childDn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "ou: Engineering",
            "cn", "Private Ryan",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, childEntry );
        partition.add( addContext );

        Dn parentDn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );

        Rdn rdn = new Rdn( "cn=Ryan" );

        partition.moveAndRename( childDn, parentDn, rdn, new ClonedServerEntry( childEntry ), true );

        // to drop the alias indices
        childDn = new Dn( schemaManager, "commonName=Jim Bean,ou=Apache,ou=Board of Directors,o=Good Times Co." );

        parentDn = new Dn( schemaManager, "ou=Engineering,o=Good Times Co." );

        assertEquals( 3, partition.getSubAliasIndex().count() );

        Dn newDn = parentDn.add( childDn.getRdn() );
        partition.move( childDn, parentDn, newDn, null );

        assertEquals( 3, partition.getSubAliasIndex().count() );
    }


    @Test
    public void testModifyAdd() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.SURNAME_AT, schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.SURNAME_AT ) );

        String attribVal = "Walker";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        Entry lookedup = partition.fetch( partition.getEntryId( dn ) );

        partition.modify( dn, add );
        assertTrue( lookedup.get( "sn" ).contains( attribVal ) );

        partition.modify( dn, new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            schemaManager.getAttributeType( "telephoneNumber" ), "+1974045779" ) );
        lookedup = partition.fetch( partition.getEntryId( dn ) );
        assertTrue( lookedup.get( "telephoneNumber" ).contains( "+1974045779" ) );
    }


    @Test
    public void testModifyReplace() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.SN_AT, schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.SN_AT_OID ) );

        String attribVal = "Johnny";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );

        Entry lookedup = partition.fetch( partition.getEntryId( dn ) );

        assertEquals( "WAlkeR", lookedup.get( "sn" ).get().getString() ); // before replacing

        lookedup = partition.modify( dn, add );
        assertEquals( attribVal, lookedup.get( "sn" ).get().getString() );

        lookedup = partition.modify( dn, new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SN_AT,
            "JWalker" ) );
        assertEquals( "JWalker", lookedup.get( "sn" ).get().getString() );
    }


    @Test
    public void testModifyRemove() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.SN_AT, schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.SN_AT_OID ) );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        Entry lookedup = partition.fetch( partition.getEntryId( dn ) );

        assertNotNull( lookedup.get( "sn" ).get() );

        lookedup = partition.modify( dn, add );
        assertNull( lookedup.get( "sn" ) );

        // add an entry for the sake of testing the remove operation
        lookedup = partition.modify( dn,
            new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, SN_AT, "JWalker" ) );
        assertNotNull( lookedup.get( "sn" ) );

        lookedup = partition.modify( dn, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, SN_AT ) );
        assertNull( lookedup.get( "sn" ) );
    }


    @Test
    public void testModifyReplaceNonExistingIndexAttribute() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Tim B,ou=Sales,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "cn", "Tim B" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        partition.add( addContext );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );

        String attribVal = "Marketing";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );

        Entry lookedup = partition.fetch( partition.getEntryId( dn ) );

        assertNull( lookedup.get( "ou" ) ); // before replacing

        lookedup = partition.modify( dn, add );
        assertEquals( attribVal, lookedup.get( "ou" ).get().getString() );
    }
}
