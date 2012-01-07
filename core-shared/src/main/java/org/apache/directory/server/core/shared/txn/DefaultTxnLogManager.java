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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.UUID;

import org.apache.directory.server.core.api.log.InvalidLogException;
import org.apache.directory.server.core.api.log.Log;
import org.apache.directory.server.core.api.log.UserLogRecord;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.api.txn.logedit.LogEdit;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultTxnLogManager implements TxnLogManager
{
    /** Write ahead log */
    private Log wal;

    /** Txn Manager */
    private TxnManagerInternal txnManager;

    /** Txn Manager Factory */
    private TxnManagerFactory txnManagerFactory;


    /**
     * Inits the the txn log manager
     * 
     * @param logger write ahead logger
     * @param txnManager txn Manager
     */
    public DefaultTxnLogManager( Log logger, TxnManagerFactory txnManagerFactory )
    {
        wal = logger;
        this.txnManager = txnManagerFactory.txnManagerInternalInstance();
        this.txnManagerFactory = txnManagerFactory;
    }


    public void shutdown()
    {
        // Do nothing
    }


    /**
     * {@inheritDoc}
     */
    public void log( LogEdit logEdit, boolean sync ) throws Exception
    {
        Transaction curTxn = txnManager.getCurTxn();

        if ( curTxn == null )
        {
            /*
             *  Txn is not initialized. This might happen if the change path does not use txn like during testing
             *  or bootstrap. In this case we should have some data change and we will apply them to the underyling
             *  partitions directly
             */
            logEdit.apply( false );

            return;
        }

        if ( !( curTxn instanceof ReadWriteTxn ) )
        {
            throw new IllegalStateException( "Trying to log logedit without ReadWriteTxn" );
        }

        ReadWriteTxn txn = ( ReadWriteTxn ) curTxn;
        UserLogRecord logRecord = txn.getUserLogRecord();

        ObjectOutputStream out = null;
        ByteArrayOutputStream bout = null;
        byte[] data;

        try
        {
            bout = new ByteArrayOutputStream();
            out = new ObjectOutputStream( bout );
            out.writeObject( logEdit );
            out.flush();
            data = bout.toByteArray();
        }
        finally
        {
            if ( bout != null )
            {
                bout.close();
            }

            if ( out != null )
            {
                out.close();
            }
        }

        logRecord.setData( data, data.length );

        log( logRecord, sync );

        logEdit.getLogAnchor().resetLogAnchor( logRecord.getLogAnchor() );
        txn.addLogEdit( logEdit );
    }


    /**
     * {@inheritDoc}
     */
    public void log( UserLogRecord logRecord, boolean sync ) throws Exception
    {
        try
        {
            wal.log( logRecord, sync );
        }
        catch ( InvalidLogException e )
        {
            throw new IOException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Entry mergeUpdates( Dn partitionDn, UUID entryID, Entry entry )
    {
        Transaction curTxn = txnManager.getCurTxn();

        if ( ( curTxn == null ) )
        {
            throw new IllegalStateException( "Trying to merge with log wihout txn" );
        }

        return curTxn.mergeUpdates( partitionDn, entryID, entry );
    }


    /**
     * {@inheritDoc}
     */
    public UUID mergeForwardLookup( Dn partitionDN, String attributeOid, Object key, UUID curId,
        Comparator<Object> valueComparator )
    {
        Transaction curTxn = txnManager.getCurTxn();

        if ( ( curTxn == null ) )
        {
            throw new IllegalStateException( "Trying to merge with log wihout txn" );
        }

        return curTxn.mergeForwardLookup( partitionDN, attributeOid, key, curId, valueComparator );
    }


    /**
     * {@inheritDoc}
     */
    public Object mergeReversLookup( Dn partitionDN, String attributeOid, UUID id, Object curValue )
    {
        Transaction curTxn = txnManager.getCurTxn();

        if ( ( curTxn == null ) )
        {
            throw new IllegalStateException( "Trying to merge with log wihout txn" );
        }

        return curTxn.mergeReverseLookup( partitionDN, attributeOid, id, curValue );
    }


    /**
     * {@inheritDoc}
     */
    public boolean mergeExistence( Dn partitionDN, String attributeOid, IndexEntry<?> indexEntry,
        boolean currentlyExists )
    {
        Transaction curTxn = txnManager.getCurTxn();

        if ( ( curTxn == null ) )
        {
            throw new IllegalStateException( "Trying to merge with log wihout txn" );
        }

        return curTxn.mergeExistence( partitionDN, attributeOid, indexEntry, currentlyExists );
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object> wrap( Dn partitionDn, IndexCursor<Object> wrappedCursor,
        IndexComparator<Object> comparator, String attributeOid, boolean forwardIndex, Object onlyValueKey,
        UUID onlyIDKey ) throws Exception
    {
        Transaction curTxn = txnManager.getCurTxn();

        if ( ( curTxn == null ) )
        {
            return wrappedCursor;
        }

        return new IndexCursorWrapper( txnManagerFactory, partitionDn, wrappedCursor, comparator, attributeOid,
            forwardIndex, onlyValueKey, onlyIDKey );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<Object> wrap( Dn partitionDn, Index<?> wrappedIndex ) throws Exception
    {
        Transaction curTxn = txnManager.getCurTxn();

        if ( ( curTxn == null ) )
        {
            return ( Index<Object> ) wrappedIndex;
        }

        return new IndexWrapper( txnManagerFactory, partitionDn, ( Index<Object> ) wrappedIndex );
    }


    /**
     * {@inheritDoc}
     */
    public MasterTable wrap( Dn partitionDn, MasterTable wrappedTable ) throws Exception
    {
        Transaction curTxn = txnManager.getCurTxn();

        if ( ( curTxn == null ) )
        {
            return wrappedTable;
        }

        return new MasterTableWrapper( txnManagerFactory, partitionDn, wrappedTable );
    }


    /**
     * {@inheritDoc}
     */
    public void addRead( Dn baseDn, SearchScope scope )
    {
        addDnSet( baseDn, scope, true );
    }


    /**
     * {@inheritDoc}
     */
    public void addWrite( Dn baseDn, SearchScope scope )
    {
        addDnSet( baseDn, scope, false );
    }


    private void addDnSet( Dn baseDn, SearchScope scope, boolean read )
    {
        Transaction curTxn = txnManager.getCurTxn();

        // No txn, or read only txn, return without doing anything.
        if ( ( curTxn == null ) || ( curTxn instanceof ReadOnlyTxn ) )
        {
            return;
        }

        DnSet dnSet = new DnSet( baseDn, scope );
        ReadWriteTxn txn = ( ReadWriteTxn ) curTxn;

        if ( read )
        {
            txn.addRead( dnSet );
        }
        else
        {
            txn.addWrite( dnSet );

            // Every written dn set is also read
            txn.addRead( dnSet );
        }
    }
}