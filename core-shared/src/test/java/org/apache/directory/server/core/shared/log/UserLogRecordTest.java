/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.shared.log;


import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.UUID;

import org.apache.directory.server.core.api.log.UserLogRecord;
import org.apache.directory.server.core.api.log.UserLogRecord.LogEditType;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.index.GenericIndex;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.txn.logedit.DataChange;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;
import org.apache.directory.server.core.shared.txn.logedit.EntryAddDelete;
import org.apache.directory.server.core.shared.txn.logedit.EntryChange;
import org.apache.directory.server.core.shared.txn.logedit.EntryReplace;
import org.apache.directory.server.core.shared.txn.logedit.IndexChange;
import org.apache.directory.server.core.shared.txn.logedit.IndexChange.Type;
import org.apache.directory.server.core.shared.txn.logedit.TxnStateChange;
import org.apache.directory.server.core.shared.txn.logedit.TxnStateChange.ChangeState;
import org.apache.directory.server.core.shared.txn.utils.MockPartition;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;


/**
 * A test class for the UserLogRecord class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UserLogRecordTest
{
    private ObjectInputStream buildStream( byte[] buffer ) throws IOException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( buffer );
        try
        {
            oIn = new ObjectInputStream( in );
            oIn.read();

            return oIn;
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
    }


    @Test
    public void testTxnStateChangeSerializationBegin() throws Exception
    {
        TxnStateChange txnStateChange = new TxnStateChange( 1L, ChangeState.TXN_BEGIN );

        UserLogRecord userLogRecord = new UserLogRecord();
        txnStateChange.injectData( userLogRecord, LogEditType.TXN );

        TxnStateChange txnDeser = new TxnStateChange();
        txnDeser.readExternal( buildStream( userLogRecord.getDataBuffer() ) );

        assertEquals( txnStateChange.getTxnID(), txnDeser.getTxnID() );
        assertEquals( txnStateChange.getTxnState(), txnDeser.getTxnState() );
    }


    @Test
    public void testTxnStateChangeSerializationCommit() throws Exception
    {
        TxnStateChange txnStateChange = new TxnStateChange( 1L, ChangeState.TXN_COMMIT );

        UserLogRecord userLogRecord = new UserLogRecord();
        txnStateChange.injectData( userLogRecord, LogEditType.TXN );

        TxnStateChange txnDeser = new TxnStateChange();
        txnDeser.readExternal( buildStream( userLogRecord.getDataBuffer() ) );

        assertEquals( txnStateChange.getTxnID(), txnDeser.getTxnID() );
        assertEquals( txnStateChange.getTxnState(), txnDeser.getTxnState() );
    }


    @Test
    public void testTxnStateChangeSerializationAbort() throws Exception
    {
        TxnStateChange txnStateChange = new TxnStateChange( 1L, ChangeState.TXN_ABORT );

        UserLogRecord userLogRecord = new UserLogRecord();
        txnStateChange.injectData( userLogRecord, LogEditType.TXN );

        TxnStateChange txnDeser = new TxnStateChange();
        txnDeser.readExternal( buildStream( userLogRecord.getDataBuffer() ) );

        assertEquals( txnStateChange.getTxnID(), txnDeser.getTxnID() );
        assertEquals( txnStateChange.getTxnState(), txnDeser.getTxnState() );
    }


    @Test
    public void testDataChangeContainerSerialization() throws Exception
    {
        Partition partition = new MockPartition( new Dn( "dc=test" ) );
        DataChangeContainer dataChangeContainer = new DataChangeContainer( partition );

        dataChangeContainer.setEntryID( UUID.randomUUID() );
        dataChangeContainer.setTxnID( 1L );

        Index<UUID> index = new GenericIndex<UUID>( "entryUUID" );

        DataChange indexChange1 = new IndexChange( index, UUID.randomUUID(), UUID.randomUUID(), Type.ADD, true );
        DataChange indexChange2 = new IndexChange( index, UUID.randomUUID(), UUID.randomUUID(), Type.ADD, true );
        Entry entry = new DefaultEntry(
            "dc=example,dc=com",
            "objectClass:top",
            "objectClass:person",
            "cn: test",
            "sn:test"
            );

        DataChange entryAddDelete = new EntryAddDelete( entry, EntryAddDelete.Type.DELETE );

        dataChangeContainer.addChange( indexChange1 );
        dataChangeContainer.addChange( indexChange2 );
        dataChangeContainer.addChange( entryAddDelete );

        UserLogRecord userLogRecord = new UserLogRecord();
        dataChangeContainer.injectData( userLogRecord, LogEditType.DATA );

        DataChangeContainer dataDeser = new DataChangeContainer();
        dataDeser.readExternal( buildStream( userLogRecord.getDataBuffer() ) );

        assertEquals( dataChangeContainer.getTxnID(), dataDeser.getTxnID() );
        assertEquals( dataChangeContainer.getEntryID(), dataDeser.getEntryID() );
        assertEquals( dataChangeContainer.getPartitionDn(), dataDeser.getPartitionDn() );
        assertEquals( dataChangeContainer.getChanges().size(), dataDeser.getChanges().size() );

        for ( int i = 0; i < dataChangeContainer.getChanges().size(); i++ )
        {
            DataChange originalChange = dataChangeContainer.getChanges().get( i );
            DataChange deserChange = dataDeser.getChanges().get( i );

            if ( originalChange instanceof IndexChange )
            {
                assertEquals( ( ( IndexChange ) originalChange ).getID(), ( ( IndexChange ) deserChange ).getID() );
                assertEquals( ( ( IndexChange ) originalChange ).getKey(), ( ( IndexChange ) deserChange ).getKey() );
                assertEquals( ( ( IndexChange ) originalChange ).getOID(), ( ( IndexChange ) deserChange ).getOID() );
                assertEquals( ( ( IndexChange ) originalChange ).getType(), ( ( IndexChange ) deserChange ).getType() );
            }
            else if ( originalChange instanceof EntryAddDelete )
            {
                assertEquals( ( ( EntryAddDelete ) originalChange ).getType(),
                    ( ( EntryAddDelete ) deserChange ).getType() );
                assertEquals( ( ( EntryAddDelete ) originalChange ).getChangedEntry(),
                    ( ( EntryAddDelete ) deserChange ).getChangedEntry() );
            }
            else if ( originalChange instanceof EntryChange )
            {
                assertEquals( ( ( EntryChange ) originalChange ).getRedoChange(),
                    ( ( EntryChange ) deserChange ).getRedoChange() );
                assertEquals( ( ( EntryChange ) originalChange ).getUndoChange(),
                    ( ( EntryChange ) deserChange ).getUndoChange() );
            }
            else if ( originalChange instanceof EntryReplace )
            {
                assertEquals( ( ( EntryReplace ) originalChange ).getNewEntry(),
                    ( ( EntryReplace ) deserChange ).getNewEntry() );
                assertEquals( ( ( EntryReplace ) originalChange ).getOldEntry(),
                    ( ( EntryReplace ) deserChange ).getOldEntry() );
            }
        }
    }
}
