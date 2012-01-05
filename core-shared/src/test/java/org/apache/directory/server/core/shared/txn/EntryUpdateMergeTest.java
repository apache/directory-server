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
package org.apache.directory.server.core.shared.txn;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.log.InvalidLogException;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;
import org.apache.directory.server.core.shared.txn.logedit.EntryAddDelete;
import org.apache.directory.server.core.shared.txn.logedit.EntryChange;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * TODO Add header
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryUpdateMergeTest extends AbstractPartitionTest
{
    /** Test partition Dn */
    private Dn dn;

    /** Log buffer size : 4096 bytes */
    private int logBufferSize = 1 << 12;

    /** Log File Size : 8192 bytes */
    private long logFileSize = 1 << 13;

    /** log suffix */
    private static String LOG_SUFFIX = "log";

    /** Txn manager */
    private TxnManagerInternal txnManager;

    /** Txn log manager */
    private TxnLogManager txnLogManager;

    /** Entry to be merged */
    private Entry toUpdate;

    /** Entry Id */
    private UUID updatedEntryId = UUID.fromString( "00000000-0000-0000-0000-000000000001" );

    /** Entry to be added by a txn */
    private Entry toAdd;

    /** Entry Id */
    private UUID addedEntryId = UUID.fromString( "00000000-0000-0000-0000-000000000002" );

    /** Entry to be added by a txn */
    private Entry toDelete;

    /** Entry Id */
    private UUID deletedEntryId = UUID.fromString( "00000000-0000-0000-0000-000000000003" );

    /** Schema manager */
    private SchemaManager schemaManager;

    /** Sn attribute type */
    private AttributeType SN_AT;

    /** Gn attribute type */
    private AttributeType GN_AT;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    /**
     * Get the Log folder
     */
    private String getLogFolder() throws IOException
    {
        File newFolder = folder.newFolder( LOG_SUFFIX );
        String file = newFolder.getAbsolutePath();

        return file;
    }


    /**
     * Utility method to create a new entry
     */
    private Entry createEntry( UUID id ) throws Exception
    {
        String user = id.toString();

        String dn = "cn=" + user + ",ou=department";

        DefaultEntry entry = new DefaultEntry(
            schemaManager,
            dn,
            "objectClass: person",
            "cn", user,
            "sn", user );

        return entry;
    }


    @Before
    public void setup() throws IOException, InvalidLogException
    {
        try
        {
            schemaManager = new DefaultSchemaManager();

            // Init the partition dn
            dn = new Dn( schemaManager, "ou=department" );

            SN_AT = schemaManager.getAttributeType( "sn" );
            GN_AT = schemaManager.getAttributeType( "gn" );

            // Init the txn manager
            TxnManagerFactory txnManagerFactory = new TxnManagerFactory( getLogFolder(), logBufferSize, logFileSize );
            txnManager = txnManagerFactory.txnManagerInternalInstance();
            txnLogManager = txnManagerFactory.txnLogManagerInstance();

            toUpdate = createEntry( updatedEntryId );
            toAdd = createEntry( addedEntryId );
            toDelete = createEntry( deletedEntryId );

            super.setup( dn );

            // Inject the entries in the Master table (except the entry that we will add)
            partition.getMasterTable().put( updatedEntryId, toUpdate );
            partition.getMasterTable().put( deletedEntryId, toDelete );

            // Begin a txn and do some entry changes on the UpdatedEntry
            DataChangeContainer changeContainer = new DataChangeContainer( partition );
            changeContainer.setEntryID( updatedEntryId );

            {
                txnManager.beginTransaction( false );

                // Add a SN value on an existing SN attribute
                Attribute attribute = new DefaultAttribute( "sn", SN_AT );
                attribute.add( "test2" );

                Modification redo = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
                Modification undo = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attribute );
                EntryChange eChange = new EntryChange( redo, undo );

                changeContainer.getChanges().add( eChange );

                txnLogManager.log( changeContainer, false );
                txnManager.commitTransaction();
            }

            // Do a second change on the UpdatedEntry
            {
                txnManager.beginTransaction( false );

                changeContainer = new DataChangeContainer( partition );
                changeContainer.setEntryID( updatedEntryId );

                // Add a GN attribute
                Attribute attribute = new DefaultAttribute( "gn", GN_AT );
                attribute.add( "test3" );

                Modification redo = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
                Modification undo = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attribute );
                EntryChange eChange = new EntryChange( redo, undo );

                changeContainer.getChanges().add( eChange );
                txnLogManager.log( changeContainer, false );

                // Now, add a new entry (AddEntry)
                changeContainer = new DataChangeContainer( partition );
                changeContainer.setEntryID( addedEntryId );
                EntryAddDelete eAdd = new EntryAddDelete( toAdd, EntryAddDelete.Type.ADD );

                changeContainer.getChanges().add( eAdd );
                txnLogManager.log( changeContainer, false );

                txnManager.commitTransaction();
            }

            // Do a third change : delete an entry
            {
                txnManager.beginTransaction( false );

                changeContainer = new DataChangeContainer( partition );
                changeContainer.setEntryID( deletedEntryId );
                EntryAddDelete eDelete = new EntryAddDelete( toDelete, EntryAddDelete.Type.DELETE );

                changeContainer.getChanges().add( eDelete );
                txnLogManager.log( changeContainer, false );
            }

            // Note : the transaction remains open. It will be committed by the teardown method.

        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @After
    public void teardown() throws IOException
    {
        try
        {
            txnManager.commitTransaction();
        }
        catch ( Exception e )
        {
            fail();
        }

        FileUtils.deleteDirectory( new File( getLogFolder() ) );
    }


    @Test
    public void testMergeModification()
    {
        try
        {
            Entry updated = txnLogManager.mergeUpdates( dn, updatedEntryId, toUpdate );

            assertTrue( updated.contains( SN_AT, "test2", updatedEntryId.toString() ) );
            assertTrue( updated.contains( GN_AT, "test3" ) );
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testMergeAdd()
    {
        try
        {
            Entry added = txnLogManager.mergeUpdates( dn, addedEntryId, null );

            assertTrue( added == toAdd );
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testMergeDelete()
    {
        try
        {
            Entry deleted = txnLogManager.mergeUpdates( dn, deletedEntryId, toDelete );

            assertTrue( deleted == null );
        }
        catch ( Exception e )
        {
            fail();
        }
    }
}
