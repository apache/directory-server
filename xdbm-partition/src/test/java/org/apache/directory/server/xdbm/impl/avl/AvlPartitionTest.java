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
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.UUID;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.core.api.partition.OperationExecutionManager;
import org.apache.directory.server.core.api.partition.index.GenericIndex;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexNotFoundException;
import org.apache.directory.server.xdbm.XdbmStoreUtils;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.Strings;
import org.apache.directory.shared.util.exception.Exceptions;
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
@SuppressWarnings("unchecked")
public class AvlPartitionTest
{
    private static final Logger LOG = LoggerFactory.getLogger( AvlPartitionTest.class.getSimpleName() );

    private static AvlPartition partition;
    private static SchemaManager schemaManager = null;
    private static Dn EXAMPLE_COM;

    /** The OU AttributeType instance */
    private static AttributeType OU_AT;

    /** The SN AttributeType instance */
    private static AttributeType SN_AT;

    /** The DC AttributeType instance */
    private static AttributeType DC_AT;
    
    /** The ApacheAlias AttributeType instance */
    private static AttributeType APACHE_ALIAS_AT;
    
    /** Operation execution manager */
    private static OperationExecutionManager executionManager;
    
    /** txn and operation execution manager factories */
    private static TxnManagerFactory txnManagerFactory;
    private static OperationExecutionManagerFactory executionManagerFactory;

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
        
        File logDir = new File( workingDirectory + File.separatorChar + "txnlog" + File.separatorChar );
        logDir.mkdirs();
        txnManagerFactory = new TxnManagerFactory( logDir.getPath(), 1 << 13, 1 << 14 );
        executionManagerFactory = new OperationExecutionManagerFactory( txnManagerFactory );
        executionManager = executionManagerFactory.instance();

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );

        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
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
        partition = new AvlPartition( schemaManager, txnManagerFactory, executionManagerFactory );
        partition.setId( "example" );
        partition.setSyncOnWrite( false );

        partition.addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        partition.addIndex( new AvlIndex( SchemaConstants.UID_AT_OID ) );
        partition.setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );

        partition.initialize();

        XdbmStoreUtils.loadExampleData( partition, schemaManager, executionManagerFactory.instance() );
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
        AvlPartition avlPartition = new AvlPartition( schemaManager, txnManagerFactory, executionManagerFactory );
        avlPartition.setSyncOnWrite( true ); // for code coverage

        assertNull( avlPartition.getAliasIndex() );
        avlPartition.addIndex( new AvlIndex<String>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) );
        assertNotNull( avlPartition.getAliasIndex() );

        assertEquals( 0, avlPartition.getCacheSize() );

        assertNull( avlPartition.getPresenceIndex() );
        avlPartition.addIndex( new AvlIndex<String>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID ) );
        assertNotNull( avlPartition.getPresenceIndex() );

        assertNull( avlPartition.getOneLevelIndex() );
        avlPartition.addIndex( new AvlIndex<UUID>( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID ) );
        assertNotNull( avlPartition.getOneLevelIndex() );

        assertNull( avlPartition.getSubLevelIndex() );
        avlPartition.addIndex( new AvlIndex<UUID>( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID ) );
        assertNotNull( avlPartition.getSubLevelIndex() );

        assertNull( avlPartition.getId() );
        avlPartition.setId( "foo" );
        assertEquals( "foo", avlPartition.getId() );

        assertNull( avlPartition.getRdnIndex() );
        avlPartition.addIndex( new AvlRdnIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID ) );
        assertNotNull( avlPartition.getRdnIndex() );

        assertNull( avlPartition.getOneAliasIndex() );
        avlPartition.addIndex( new AvlIndex<UUID>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ) );
        assertNotNull( avlPartition.getOneAliasIndex() );

        assertNull( avlPartition.getSubAliasIndex() );
        avlPartition.addIndex( new AvlIndex<UUID>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ) );
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
            partition.addIndex( new AvlIndex<String>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) );
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

        assertNotNull( partition.getOneLevelIndex() );
        
        try
        {
            partition.addIndex( new AvlIndex<UUID>( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID ) );
            //fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getSubLevelIndex() );
        
        try
        {
            partition.addIndex( new AvlIndex<UUID>( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID ) );
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

        assertNotNull( partition.getEntryUuidIndex() );
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
            partition.addIndex( new AvlIndex<UUID>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ) );
            //fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getSubAliasIndex() );
        
        try
        {
            partition.addIndex( new AvlIndex<UUID>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ) );
            //fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getSuffixDn() );
        
        Iterator<String> systemIndices = partition.getSystemIndices();

        for ( int i = 0; i < 10; i++ )
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
        assertEquals( UUID.fromString( "00000000-0000-0000-0000-000000000001" ),  executionManager.getEntryId( partition, dn ) );
        assertEquals( 11, partition.count() );

        // note that the suffix entry returns 0 for it's parent which does not exist
        assertEquals( UUID.fromString( "00000000-0000-0000-0000-000000000000" ), executionManager.getParentId( partition, executionManager.getEntryId( partition, dn ) ) );
        assertNull( executionManager.getParentId( partition, UUID.fromString( "00000000-0000-0000-0000-000000000000" ) ) );

        // should be allowed
        executionManager.delete( partition, dn, UUID.fromString( "00000000-0000-0000-0000-000000000001" ) );
    }


    @Test
    public void testEntryOperations() throws Exception
    {
        assertEquals( 3, executionManager.getChildCount( partition, UUID.fromString( "00000000-0000-0000-0000-000000000001" ) ) );

        Cursor<IndexEntry<UUID>> cursor = executionManager.list( partition, UUID.fromString( "00000000-0000-0000-0000-000000000001" ) );
        assertNotNull( cursor );
        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUIDString( 2 ),  cursor.get().getId() );
        assertTrue( cursor.next() );
        assertEquals( 3, executionManager.getChildCount( partition, Strings.getUUIDString( 1 ) ) );

        executionManager.delete( partition, executionManager.buildEntryDn( partition, Strings.getUUIDString( 2 ) ), Strings.getUUIDString( 2 ) );

        assertEquals( 2, executionManager.getChildCount( partition, Strings.getUUIDString( 1 ) ) );
        assertEquals( 10, partition.count() );

        // add an alias and delete to test dropAliasIndices method
        Dn dn = new Dn( schemaManager, "commonName=Jack Daniels,ou=Apache,ou=Board of Directors,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
        entry.add( "ou", "Apache" );
        entry.add( "commonName", "Jack Daniels" );
        entry.add( "aliasedObjectName", "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", Strings.getUUIDString( 12 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );

        executionManager.delete( partition, dn, Strings.getUUIDString( 12 ) );
    }


    @Test
    public void testSubLevelIndex() throws Exception
    {
        Index idx = partition.getSubLevelIndex();

        assertEquals( 19, idx.count() );

        Cursor<IndexEntry<UUID>> cursor = idx.forwardCursor( Strings.getUUIDString( 2 ) );

        assertTrue( cursor.next() );
        assertEquals( Strings.getUUIDString( 2 ), cursor.get().getId() );

        assertTrue( cursor.next() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );

        assertTrue( cursor.next() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );

        assertFalse( cursor.next() );

        idx.drop( Strings.getUUIDString( 5 ) );

        cursor = idx.forwardCursor( Strings.getUUIDString( 2 ) );

        assertTrue( cursor.next() );
        assertEquals( Strings.getUUIDString( 2 ), cursor.get().getId() );

        assertTrue( cursor.next() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );

        assertFalse( cursor.next() );

        // dn id 12
        Dn martinDn = new Dn( schemaManager, "cn=Marting King,ou=Sales,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, martinDn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Sales" );
        entry.add( "cn", "Martin King" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", Strings.getUUIDString( 12 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );

        cursor = idx.forwardCursor( Strings.getUUIDString( 2 ) );
        cursor.afterLast();
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 12 ), cursor.get().getId() );

        Dn newParentDn = new Dn( schemaManager, "ou=Board of Directors,o=Good Times Co." );

        Dn newDn = newParentDn.add( martinDn.getRdn() );
        executionManager.move( partition, martinDn, newParentDn, newDn, entry.clone(), entry );

        cursor = idx.forwardCursor( Strings.getUUIDString( 3 ) );
        cursor.afterLast();
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 12 ), cursor.get().getId() );

        // dn id 13
        Dn marketingDn = new Dn( schemaManager, "ou=Marketing,ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, marketingDn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Marketing" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", Strings.getUUIDString( 13 ).toString() );

        addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );

        // dn id 14
        Dn jimmyDn = new Dn( schemaManager, "cn=Jimmy Wales,ou=Marketing, ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, jimmyDn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Marketing" );
        entry.add( "cn", "Jimmy Wales" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", Strings.getUUIDString( 14 ).toString() );

        addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );

        newDn = newParentDn.add( marketingDn.getRdn() );
        executionManager.move( partition, marketingDn, newParentDn, newDn, entry.clone(), entry );

        cursor = idx.forwardCursor( Strings.getUUIDString( 3 ) );
        cursor.afterLast();

        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 14 ), cursor.get().getId() );

        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 13 ), cursor.get().getId() );

        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 12 ), cursor.get().getId() );

        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );

        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );

        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 7 ), cursor.get().getId() );

        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 3 ), cursor.get().getId() );

        assertFalse( cursor.previous() );
    }


    @Test
    public void testConvertIndex() throws Exception
    {
        Index nonAvlIndex = new GenericIndex( "ou", 10, new File( "." ).toURI() );

        Method convertIndex = partition.getClass().getDeclaredMethod( "convertAndInit", Index.class );
        convertIndex.setAccessible( true );
        Object obj = convertIndex.invoke( partition, nonAvlIndex );

        assertNotNull( obj );
        assertEquals( AvlIndex.class, obj.getClass() );
    }


    @Test(expected = LdapNoSuchObjectException.class)
    public void testAddWithoutParentId() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Marting King,ou=Not Present,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Not Present" );
        entry.add( "cn", "Martin King" );
        entry.add( "entryUUID", Strings.getUUIDString( 15 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );
    }


    @Test(expected = LdapSchemaViolationException.class)
    public void testAddWithoutObjectClass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Martin King,ou=Sales,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "ou", "Sales" );
        entry.add( "cn", "Martin King" );
        entry.add( "entryUUID", Strings.getUUIDString( 16 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );
    }


    @Test
    public void testModifyAddOUAttrib() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );
        attrib.add( "Engineering" );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        executionManager.modify( partition, dn, add );
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
        entry.add( "entryUUID", Strings.getUUIDString( 17 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );

        Rdn rdn = new Rdn( "sn=James" );

        executionManager.rename( partition, dn, rdn, true, null, addContext.getModifiedEntry() );
    }


    @Test
    public void testRenameEscaped() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Pivate Ryan,ou=Engineering,o=Good Times Co." );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Engineering" );
        entry.add( "cn", "Private Ryan" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", Strings.getUUIDString( 18 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );

        Rdn rdn = new Rdn( "sn=Ja\\+es" );

        executionManager.rename( partition, dn, rdn, true, null, addContext.getModifiedEntry() );

        Dn dn2 = new Dn( schemaManager, "sn=Ja\\+es,ou=Engineering,o=Good Times Co." );
        UUID id = executionManager.getEntryId( partition, dn2 );
        assertNotNull( id );
        Entry entry2 = executionManager.lookup( partition, id );
        assertEquals( "ja+es", entry2.get( "sn" ).getString() );
    }


    @Test
    public void testMove() throws Exception
    {
        Dn childDn = new Dn( schemaManager, "cn=Pivate Ryan,ou=Engineering,o=Good Times Co." );
        DefaultEntry childEntry = new DefaultEntry( schemaManager, childDn );
        childEntry.add( "objectClass", "top", "person", "organizationalPerson" );
        childEntry.add( "ou", "Engineering" );
        childEntry.add( "cn", "Private Ryan" );
        childEntry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        childEntry.add( "entryUUID", Strings.getUUIDString( 19 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, childEntry );
        executionManager.add( partition, addContext );

        Dn parentDn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );

        Rdn rdn = new Rdn( "cn=Ryan" );

        childEntry = ( DefaultEntry )addContext.getModifiedEntry();
        Entry modifiedChildEntry = childEntry.clone();
        executionManager.moveAndRename( partition, childDn, parentDn, rdn, modifiedChildEntry, childEntry, true );

        // to drop the alias indices   
        childDn = new Dn( schemaManager, "commonName=Jim Bean,ou=Apache,ou=Board of Directors,o=Good Times Co." );

        parentDn = new Dn( schemaManager, "ou=Engineering,o=Good Times Co." );

        assertEquals( 3, partition.getSubAliasIndex().count() );

        Dn newDn = parentDn.add( childDn.getRdn() );
        executionManager.move( partition, childDn, parentDn, newDn, modifiedChildEntry.clone(), modifiedChildEntry );

        // Count should be two after one add and two removes
        assertEquals( 2, partition.getSubAliasIndex().count() );
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

        Entry lookedup = executionManager.lookup( partition, executionManager.getEntryId( partition, dn ) );

        executionManager.modify( partition, dn, add );
        assertTrue( lookedup.get( "sn" ).contains( attribVal ) );

        executionManager.modify( partition, dn, new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, 
            schemaManager.getAttributeType( "telephoneNumber" ), "+1974045779" ) );
        lookedup = executionManager.lookup( partition, executionManager.getEntryId( partition, dn ) );
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

        Entry lookedup = executionManager.lookup( partition, executionManager.getEntryId( partition, dn ) );

        assertEquals( "WAlkeR", lookedup.get( "sn" ).get().getString() ); // before replacing

        lookedup = executionManager.modify( partition, dn, add );
        assertEquals( attribVal, lookedup.get( "sn" ).get().getString() );

        lookedup = executionManager.modify( partition, dn, new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SN_AT, "JWalker" ) );
        assertEquals( "JWalker", lookedup.get( "sn" ).get().getString() );
    }


    @Test
    public void testModifyRemove() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.SN_AT, schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.SN_AT_OID ) );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        Entry lookedup = executionManager.lookup( partition, executionManager.getEntryId( partition, dn ) );

        assertNotNull( lookedup.get( "sn" ).get() );

        lookedup = executionManager.modify( partition, dn, add );
        assertNull( lookedup.get( "sn" ) );

        // add an entry for the sake of testing the remove operation
        lookedup = executionManager.modify( partition, dn, new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, SN_AT, "JWalker" ) );
        assertNotNull( lookedup.get( "sn" ) );

        lookedup = executionManager.modify( partition, dn, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, SN_AT ) );
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
        entry.add( "entryUUID", Strings.getUUIDString( 20 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        executionManager.add( partition, addContext );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );

        String attribVal = "Marketing";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );

        Entry lookedup = executionManager.lookup( partition, executionManager.getEntryId( partition, dn ) );

        assertNull( lookedup.get( "ou" ) ); // before replacing

        lookedup = executionManager.modify( partition, dn, add );
        assertEquals( attribVal, lookedup.get( "ou" ).get().getString() );
    }
}
