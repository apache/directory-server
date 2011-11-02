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


import java.io.IOException;
import java.util.TreeSet;

import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;

import org.apache.directory.server.core.api.log.InvalidLogException;
import org.apache.directory.server.core.shared.txn.logedit.EntryChange;
import org.apache.directory.server.core.shared.txn.logedit.EntryAddDelete;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.partition.index.AbstractTable;
import org.apache.directory.server.core.api.txn.TxnLogManager;


public class EntryUpdateMergeTest
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
    private TxnManagerInternal<Long> txnManager;

    /** Txn log manager */
    private TxnLogManager<Long> txnLogManager;

    /** Entry to be merged */
    private Entry toUpdate;

    /** Entry Id */
    private Long updatedEntryId = new Long( 0 );

    /** Entry to be added by a txn */
    private Entry toAdd;

    /** Entry Id */
    private Long addedEntryId = new Long( 1 );

    /** Entry to be added by a txn */
    private Entry toDelete;

    /** Entry Id */
    private Long deletedEntryId = new Long( 2 );

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
        String file = folder.newFolder( LOG_SUFFIX ).getAbsolutePath();

        return file;
    }


    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws IOException, InvalidLogException
    {
        try
        {
            // Init the partition dn
            dn = new Dn( "ou=department" );

            schemaManager = new DefaultSchemaManager();
            SN_AT = schemaManager.getAttributeType( "sn" );
            GN_AT = schemaManager.getAttributeType( "gn" );

            // Init the txn manager
            TxnManagerFactory.<Long> init( LongComparator.INSTANCE, LongSerializer.INSTANCE, getLogFolder(),
                logBufferSize, logFileSize );
            txnManager = TxnManagerFactory.<Long> txnManagerInternalInstance();
            txnLogManager = TxnManagerFactory.<Long> txnLogManagerInstance();

            toUpdate = createEntry( updatedEntryId );
            toAdd = createEntry( addedEntryId );
            toDelete = createEntry( deletedEntryId );

            // Begin a txn and do some entry changes.
            DataChangeContainer<Long> changeContainer = new DataChangeContainer<Long>( dn );
            changeContainer.setEntryID( updatedEntryId );
            txnManager.beginTransaction( false );

            Attribute attribute = new DefaultAttribute( "sn", SN_AT );
            attribute.add( "test2" );

            Modification redo = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
            Modification undo = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attribute );
            EntryChange<Long> eChange = new EntryChange<Long>( redo, undo );

            changeContainer.getChanges().add( eChange );

            txnLogManager.log( changeContainer, false );
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );

            changeContainer = new DataChangeContainer<Long>( dn );
            changeContainer.setEntryID( updatedEntryId );
            attribute = new DefaultAttribute( "gn", GN_AT );
            attribute.add( "test3" );

            redo = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
            undo = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attribute );
            eChange = new EntryChange<Long>( redo, undo );

            changeContainer.getChanges().add( eChange );
            txnLogManager.log( changeContainer, false );

            changeContainer = new DataChangeContainer<Long>( dn );
            changeContainer.setEntryID( addedEntryId );
            EntryAddDelete<Long> eAdd = new EntryAddDelete<Long>( toAdd, EntryAddDelete.Type.ADD );

            changeContainer.getChanges().add( eAdd );
            txnLogManager.log( changeContainer, false );

            txnManager.commitTransaction();

            txnManager.beginTransaction( false );

            changeContainer = new DataChangeContainer<Long>( dn );
            changeContainer.setEntryID( deletedEntryId );
            EntryAddDelete<Long> eDelete = new EntryAddDelete<Long>( toDelete, EntryAddDelete.Type.DELETE );

            changeContainer.getChanges().add( eDelete );
            txnLogManager.log( changeContainer, false );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
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
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testMergeModification()
    {
        try
        {
            Entry updated = txnLogManager.mergeUpdates( dn, updatedEntryId, toUpdate );

            String value = updated.get( SN_AT ).getString();

            assertTrue( value.equals( "test2" ) );

            value = updated.get( GN_AT ).getString();

            assertTrue( value.equals( "test3" ) );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            fail();
        }
    }


    private Entry createEntry( Long id ) throws Exception
    {
        String user = id.toString();

        String dn = "cn=" + user + ",ou=department";

        DefaultEntry entry = new DefaultEntry( schemaManager, dn,
            "objectClass", "person",
            "cn", user );

        return entry;
    }
}
