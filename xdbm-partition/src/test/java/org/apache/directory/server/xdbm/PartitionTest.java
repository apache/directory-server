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
package org.apache.directory.server.xdbm;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.interceptor.context.ModDnAva;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.impl.avl.AvlPartitionTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the {@link AbstractStore} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class PartitionTest
{
    private static final Logger LOG = LoggerFactory.getLogger( PartitionTest.class );

    private static AvlPartition partition;
    private static SchemaManager schemaManager = null;
    private static DnFactory dnFactory;

    /** The OU AttributType instance */
    private static AttributeType OU_AT;

    /** The UID AttributType instance */
    private static AttributeType UID_AT;

    /** The CN AttributType instance */
    private static AttributeType CN_AT;

    @BeforeAll
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

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        OU_AT = schemaManager.getAttributeType( SchemaConstants.OU_AT );
        UID_AT = schemaManager.getAttributeType( SchemaConstants.UID_AT );
        CN_AT = schemaManager.getAttributeType( SchemaConstants.CN_AT );
    }


    @BeforeEach
    public void createStore() throws Exception
    {
        StoreUtils.createdExtraAttributes( schemaManager );

        // initialize the partition
        partition = new AvlPartition( schemaManager, dnFactory );
        partition.setId( "example" );
        partition.setSyncOnWrite( false );

        partition.addIndex( new AvlIndex<String>( SchemaConstants.OU_AT_OID ) );
        partition.addIndex( new AvlIndex<String>( SchemaConstants.UID_AT_OID ) );
        partition.addIndex( new AvlIndex<String>( SchemaConstants.CN_AT_OID ) );
        partition.setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );

        partition.initialize();

        StoreUtils.loadExampleData( partition, schemaManager );
        LOG.debug( "Created new partition" );
    }


    @AfterEach
    public void destroyStore() throws Exception
    {
        partition.destroy( null );
    }


    @Test
    public void testExampleDataIndices() throws Exception
    {
        PartitionTxn txn = partition.beginReadTransaction();
        
        assertEquals( 11, partition.getRdnIndex().count( txn ) );
        assertEquals( 3, partition.getAliasIndex().count( txn ) );
        assertEquals( 3, partition.getOneAliasIndex().count( txn ) );
        assertEquals( 3, partition.getSubAliasIndex().count( txn ) );
        assertEquals( 15, partition.getPresenceIndex().count( txn ) );
        assertEquals( 17, partition.getObjectClassIndex().count( txn ) );
        assertEquals( 11, partition.getEntryCsnIndex().count( txn ) );

        Iterator<String> userIndices = partition.getUserIndices();
        int count = 0;

        while ( userIndices.hasNext() )
        {
            userIndices.next();
            count++;
        }

        assertEquals( 3, count );
        assertEquals( 9, partition.getUserIndex( OU_AT ).count( txn ) );
        assertEquals( 0, partition.getUserIndex( UID_AT ).count( txn ) );
        assertEquals( 6, partition.getUserIndex( CN_AT ).count( txn ) );
    }


    /**
     * Adding an objectClass value should also add it to the objectClass index.
     */
    @Test
    public void testModifyAddObjectClass() throws Exception
    {
        PartitionTxn txn = partition.beginReadTransaction();
        
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OBJECT_CLASS_AT, schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT ) );

        String attribVal = "uidObject";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( txn, dn );
        Entry lookedup = partition.fetch( txn, entryId );

        // before modification: no "uidObject" tuple in objectClass index
        assertFalse( partition.getObjectClassIndex().forward( txn, "1.3.6.1.1.3.1", entryId ) );
        assertFalse( lookedup.get( "objectClass" ).contains( "uidObject" ) );

        lookedup = partition.modify( txn, dn, add );

        // after modification: expect "uidObject" tuple in objectClass index
        assertTrue( partition.getObjectClassIndex().forward( txn, "1.3.6.1.1.3.1", entryId ) );
        assertTrue( lookedup.get( "objectClass" ).contains( "uidObject" ) );
    }


    /**
     * Removing a value of an indexed attribute should also remove it from the index.
     */
    @Test
    public void testModifyRemoveIndexedAttribute() throws Exception
    {
        PartitionTxn txn = partition.beginReadTransaction();
        
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );

        String attribVal = "sales";
        attrib.add( attribVal );

        Modification remove = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( txn, dn );
        Entry lookedup = partition.fetch( txn, entryId );

        // before modification: expect "sales" tuple in ou index
        Index<String, String> ouIndex = ( Index<String, String> ) partition.getUserIndex( OU_AT );
        assertTrue( ouIndex.forward( txn, " sales ", entryId ) );
        assertTrue( lookedup.get( "ou" ).contains( "sales" ) );

        lookedup = partition.modify( txn, dn, remove );

        // after modification: no "sales" tuple in ou index
        assertFalse( ouIndex.forward( txn, " sales ", entryId ) );
        assertNull( lookedup.get( "ou" ) );
    }


    /**
     * Removing all values of an indexed attribute should not leave any tuples in the index,
     * nor in the presence index.
     */
    @Test
    public void testModifyRemoveAllIndexedAttribute() throws Exception
    {
        PartitionTxn txn = partition.beginReadTransaction();
        
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );

        Modification mod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( txn, dn );
        Entry lookedup = partition.fetch( txn, entryId );

        // before modification: expect "sales" tuple in ou index
        Index<String, String> ouIndex = ( Index<String, String> ) partition.getUserIndex( OU_AT );
        assertTrue( partition.getPresenceIndex().forward( txn, "2.5.4.11", entryId ) );
        assertTrue( ouIndex.forward( txn, " sales ", entryId ) );
        assertTrue( lookedup.get( "ou" ).contains( "sales" ) );

        lookedup = partition.modify( txn, dn, mod );

        // after modification: no "sales" tuple in ou index
        assertFalse( partition.getPresenceIndex().forward( txn, "2.5.4.11", entryId ) );
        assertFalse( ouIndex.reverse( txn, entryId ) );
        assertFalse( ouIndex.forward( txn, " sales ", entryId ) );
        assertNull( lookedup.get( "ou" ) );
    }


    /**
     * Removing an objectClass value should also remove it from the objectClass index.
     */
    @Test
    public void testModifyRemoveObjectClass() throws Exception
    {
        PartitionTxn txn = partition.beginReadTransaction();
        
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OBJECT_CLASS_AT, schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT ) );

        String attribVal = "person";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( txn, dn );
        Entry lookedup = partition.fetch( txn, entryId );

        // before modification: expect "person" tuple in objectClass index
        assertTrue( partition.getObjectClassIndex().forward( txn, "2.5.6.6", entryId ) );
        assertTrue( lookedup.get( "objectClass" ).contains( "person" ) );

        lookedup = partition.modify( txn, dn, add );

        // after modification: no "person" tuple in objectClass index
        assertFalse( partition.getObjectClassIndex().forward( txn, "2.5.6.6", entryId ) );
        assertFalse( lookedup.get( "objectClass" ).contains( "person" ) );
    }


    /**
     * Removing all values of the objectClass attribute should not leave any tuples in index.
     */
    @Test
    public void testModifyRemoveAllObjectClass() throws Exception
    {
        PartitionTxn txn = partition.beginReadTransaction();
        
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( "ObjectClass", schemaManager
            .lookupAttributeTypeRegistry( "ObjectClass" ) );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( txn, dn );
        Entry lookedup = partition.fetch( txn, entryId );

        // before modification: expect "person" tuple in objectClass index
        assertTrue( partition.getObjectClassIndex().forward( txn, "2.5.6.6", entryId ) );
        assertTrue( lookedup.get( "objectClass" ).contains( "person" ) );

        lookedup = partition.modify( txn, dn, add );

        // after modification: no tuple in objectClass index
        assertFalse( partition.getObjectClassIndex().forward( txn, "2.5.6.6", entryId ) );
        assertNull( lookedup.get( "objectClass" ) );
    }


    @Test
    public void testCheckCsnIndexUpdate() throws Exception
    {
        PartitionTxn txn = partition.beginReadTransaction();
        
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        AttributeType csnAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_CSN_AT );
        Attribute attrib = new DefaultAttribute( csnAt );

        CsnFactory csnF = new CsnFactory( 0 );
        String csn = csnF.newInstance().toString();
        attrib.add( csn );

        Modification add = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( txn, dn );
        Entry lookedup = partition.fetch( txn, entryId );

        assertNotSame( csn, lookedup.get( csnAt ).getString() );

        lookedup = partition.modify( txn, dn, add );

        String updateCsn = lookedup.get( csnAt ).getString();
        assertEquals( csn, updateCsn );

        csn = csnF.newInstance().toString();

        Entry modEntry = new DefaultEntry( schemaManager );
        modEntry.add( csnAt, csn );

        assertNotSame( csn, updateCsn );

        lookedup = partition
            .modify( txn, dn, new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, csnAt, csn ) );

        assertEquals( csn, lookedup.get( csnAt ).getString() );
    }


    @Test
    public void testEntryParentIdPresence() throws Exception
    {
        PartitionTxn txn = partition.beginReadTransaction();
        
        Dn dn = new Dn( schemaManager, "cn=user,ou=Sales,o=Good Times Co." );

        Entry entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "cn: user",
            "sn: user sn" );

        // add
        StoreUtils.injectEntryInStore( partition, entry, 12 );
        verifyParentId( txn, dn );

        // move
        Dn newSuperior = new Dn( schemaManager, "o=Good Times Co." );
        Dn newDn = new Dn( schemaManager, "cn=user,o=Good Times Co." );
        partition.move( txn, dn, newSuperior, newDn, null );
        entry = verifyParentId( txn, newDn );

        // move and rename
        Dn newParentDn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );
        Dn oldDn = newDn;
        Rdn newRdn = new Rdn( schemaManager, "cn=userMovedAndRenamed" );

        // The cn=userMovedAndRenamed RDN that will be added. We keep the 
        // cn=user attribute
        Map<String, List<ModDnAva>> modDnAvas = new HashMap<>();

        List<ModDnAva> modAvas = new ArrayList<>();
        modAvas.add( new ModDnAva( ModDnAva.ModDnType.ADD, newRdn.getAva()) );
        modDnAvas.put( SchemaConstants.CN_AT_OID, modAvas );

        partition.moveAndRename( txn, oldDn, newParentDn, newRdn, modDnAvas, entry );
        verifyParentId( txn, newParentDn.add( newRdn ) );
    }


    private Entry verifyParentId( PartitionTxn txn, Dn dn ) throws Exception
    {
        String entryId = partition.getEntryId( txn, dn );
        Entry entry = partition.fetch( txn, entryId );
        String parentId = partition.getParentId( txn, entryId );

        Attribute parentIdAt = entry.get( ApacheSchemaConstants.ENTRY_PARENT_ID_AT );
        assertNotNull( parentIdAt );
        //assertEquals( parentId.toString(), parentIdAt.getString() );

        return entry;
    }
}
