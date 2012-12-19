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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Iterator;

import net.sf.ehcache.store.AbstractStore;

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
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.impl.avl.AvlPartitionTest;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the {@link AbstractStore} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionTest
{
    private static final Logger LOG = LoggerFactory.getLogger( PartitionTest.class );

    private static AvlPartition partition;
    private static SchemaManager schemaManager = null;

    /** The OU AttributType instance */
    private static AttributeType OU_AT;

    /** The UID AttributType instance */
    private static AttributeType UID_AT;

    /** The CN AttributType instance */
    private static AttributeType CN_AT;


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

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        OU_AT = schemaManager.getAttributeType( SchemaConstants.OU_AT );
        UID_AT = schemaManager.getAttributeType( SchemaConstants.UID_AT );
        CN_AT = schemaManager.getAttributeType( SchemaConstants.CN_AT );
    }


    @Before
    public void createStore() throws Exception
    {

        // initialize the partition
        partition = new AvlPartition( schemaManager );
        partition.setId( "example" );
        partition.setSyncOnWrite( false );

        partition.addIndex( new AvlIndex<String, Entry>( SchemaConstants.OU_AT_OID ) );
        partition.addIndex( new AvlIndex<String, Entry>( SchemaConstants.UID_AT_OID ) );
        partition.addIndex( new AvlIndex<String, Entry>( SchemaConstants.CN_AT_OID ) );
        partition.setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );

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
    public void testExampleDataIndices() throws Exception
    {
        assertEquals( 11, partition.getRdnIndex().count() );
        assertEquals( 3, partition.getAliasIndex().count() );
        assertEquals( 3, partition.getOneAliasIndex().count() );
        assertEquals( 3, partition.getSubAliasIndex().count() );
        assertEquals( 15, partition.getPresenceIndex().count() );
        assertEquals( 27, partition.getObjectClassIndex().count() );
        assertEquals( 11, partition.getEntryCsnIndex().count() );

        Iterator<String> userIndices = partition.getUserIndices();
        int count = 0;

        while ( userIndices.hasNext() )
        {
            userIndices.next();
            count++;
        }

        assertEquals( 3, count );
        assertEquals( 9, partition.getUserIndex( OU_AT ).count() );
        assertEquals( 0, partition.getUserIndex( UID_AT ).count() );
        assertEquals( 6, partition.getUserIndex( CN_AT ).count() );
    }


    /**
     * Adding an objectClass value should also add it to the objectClass index.
     */
    @Test
    public void testModifyAddObjectClass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OBJECT_CLASS_AT, schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT ) );

        String attribVal = "uidObject";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( dn );
        Entry lookedup = partition.fetch( entryId );

        // before modification: no "uidObject" tuple in objectClass index
        assertFalse( partition.getObjectClassIndex().forward( "uidObject", entryId ) );
        assertFalse( lookedup.get( "objectClass" ).contains( "uidObject" ) );

        lookedup = partition.modify( dn, add );

        // after modification: expect "uidObject" tuple in objectClass index
        assertTrue( partition.getObjectClassIndex().forward( "uidObject", entryId ) );
        assertTrue( lookedup.get( "objectClass" ).contains( "uidObject" ) );
    }


    /**
     * Removing a value of an indexed attribute should also remove it from the index.
     */
    @Test
    public void testModifyRemoveIndexedAttribute() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );

        String attribVal = "sales";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( dn );
        Entry lookedup = partition.fetch( entryId );

        // before modification: expect "sales" tuple in ou index
        Index<String, Entry, String> ouIndex = ( Index<String, Entry, String> ) partition.getUserIndex( OU_AT );
        assertTrue( ouIndex.forward( "sales", entryId ) );
        assertTrue( lookedup.get( "ou" ).contains( "sales" ) );

        lookedup = partition.modify( dn, add );

        // after modification: no "sales" tuple in ou index
        assertFalse( ouIndex.forward( "sales", entryId ) );
        assertNull( lookedup.get( "ou" ) );
    }


    /**
     * Removing all values of an indexed attribute should not leave any tuples in the index,
     * nor in the presence index.
     */
    @Test
    public void testModifyRemoveAllIndexedAttribute() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( dn );
        Entry lookedup = partition.fetch( entryId );

        // before modification: expect "sales" tuple in ou index
        Index<String, Entry, String> ouIndex = ( Index<String, Entry, String> ) partition.getUserIndex( OU_AT );
        assertTrue( partition.getPresenceIndex().forward( SchemaConstants.OU_AT_OID, entryId ) );
        assertTrue( ouIndex.forward( "sales", entryId ) );
        assertTrue( lookedup.get( "ou" ).contains( "sales" ) );

        lookedup = partition.modify( dn, add );

        // after modification: no "sales" tuple in ou index
        assertFalse( partition.getPresenceIndex().forward( SchemaConstants.OU_AT_OID, entryId ) );
        assertFalse( ouIndex.reverse( entryId ) );
        assertFalse( ouIndex.forward( "sales", entryId ) );
        assertNull( lookedup.get( "ou" ) );
    }


    /**
     * Removing an objectClass value should also remove it from the objectClass index.
     */
    @Test
    public void testModifyRemoveObjectClass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OBJECT_CLASS_AT, schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT ) );

        String attribVal = "person";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( dn );
        Entry lookedup = partition.fetch( entryId );

        // before modification: expect "person" tuple in objectClass index
        assertTrue( partition.getObjectClassIndex().forward( "person", entryId ) );
        assertTrue( lookedup.get( "objectClass" ).contains( "person" ) );

        lookedup = partition.modify( dn, add );

        // after modification: no "person" tuple in objectClass index
        assertFalse( partition.getObjectClassIndex().forward( "person", entryId ) );
        assertFalse( lookedup.get( "objectClass" ).contains( "person" ) );
    }


    /**
     * Removing all values of the objectClass attribute should not leave any tuples in index.
     */
    @Test
    public void testModifyRemoveAllObjectClass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( "ObjectClass", schemaManager
            .lookupAttributeTypeRegistry( "ObjectClass" ) );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( dn );
        Entry lookedup = partition.fetch( entryId );

        // before modification: expect "person" tuple in objectClass index
        assertTrue( partition.getObjectClassIndex().forward( "person", entryId ) );
        assertTrue( lookedup.get( "objectClass" ).contains( "person" ) );

        lookedup = partition.modify( dn, add );

        // after modification: no tuple in objectClass index
        assertFalse( partition.getObjectClassIndex().forward( "person", entryId ) );
        assertNull( lookedup.get( "objectClass" ) );
    }


    @Test
    public void testCheckCsnIndexUpdate() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        AttributeType csnAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_CSN_AT );
        Attribute attrib = new DefaultAttribute( csnAt );

        CsnFactory csnF = new CsnFactory( 0 );
        String csn = csnF.newInstance().toString();
        attrib.add( csn );

        Modification add = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );

        String entryId = partition.getEntryId( dn );
        Entry lookedup = partition.fetch( entryId );

        assertNotSame( csn, lookedup.get( csnAt ).getString() );

        lookedup = partition.modify( dn, add );

        String updateCsn = lookedup.get( csnAt ).getString();
        assertEquals( csn, updateCsn );

        csn = csnF.newInstance().toString();

        Entry modEntry = new DefaultEntry( schemaManager );
        modEntry.add( csnAt, csn );

        assertNotSame( csn, updateCsn );

        lookedup = partition
            .modify( dn, new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, csnAt, csn ) );

        assertEquals( csn, lookedup.get( csnAt ).getString() );
    }


    @Test
    public void testEntryParentIdPresence() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=user,ou=Sales,o=Good Times Co." );

        Entry entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "cn: user",
            "sn: user sn" );

        // add
        StoreUtils.injectEntryInStore( partition, entry, 12 );
        verifyParentId( dn );

        // move
        Dn newSuperior = new Dn( schemaManager, "o=Good Times Co." );
        Dn newDn = new Dn( schemaManager, "cn=user,o=Good Times Co." );
        partition.move( dn, newSuperior, newDn, null );
        entry = verifyParentId( newDn );

        // move and rename
        Dn newParentDn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );
        Dn oldDn = newDn;
        Rdn newRdn = new Rdn( schemaManager, "cn=userMovedAndRenamed" );

        partition.moveAndRename( oldDn, newParentDn, newRdn, entry, false );
        verifyParentId( newParentDn.add( newRdn ) );
    }


    private Entry verifyParentId( Dn dn ) throws Exception
    {
        String entryId = partition.getEntryId( dn );
        Entry entry = partition.fetch( entryId );
        String parentId = partition.getParentId( entryId );

        Attribute parentIdAt = entry.get( SchemaConstants.ENTRY_PARENT_ID_AT );
        assertNotNull( parentIdAt );
        //assertEquals( parentId.toString(), parentIdAt.getString() );

        return entry;
    }
}
