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
package org.apache.directory.server.core.txn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.directory.server.core.log.UserLogRecord;
import org.apache.directory.server.core.log.Log;
import org.apache.directory.server.core.log.InvalidLogException;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexComparator;

import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;

import org.apache.directory.server.core.txn.logedit.LogEdit;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultTxnLogManager<ID> implements TxnLogManager<ID>
{
    /** Write ahead log */
    private Log wal;
    
    /** Txn Manager */
    private TxnManagerInternal<ID> txnManager;
    
    
    /**
     * Inits the the txn log manager
     * 
     * @param logger write ahead logger
     * @param txnManager txn Manager
     */
    public void init( Log logger, TxnManagerInternal<ID> txnManager )
    {
        wal = logger;
        this.txnManager = txnManager;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void log( LogEdit<ID> logEdit, boolean sync ) throws IOException
    {
        Transaction<ID> curTxn = txnManager.getCurTxn();
       
        if ( ( curTxn == null ) || ( ! ( curTxn instanceof ReadWriteTxn ) ) )
        {
            throw new IllegalStateException( "Trying to log logedit without ReadWriteTxn" );
        }
       
        ReadWriteTxn<ID> txn = (ReadWriteTxn<ID>)curTxn;
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
    public void log( UserLogRecord logRecord, boolean sync ) throws IOException
    {
        try
        {
            wal.log( logRecord, sync );
        }
        catch ( InvalidLogException e )
        {
            throw new IOException(e);
        }
    }
   
   
    /**
     * {@inheritDoc}
     */
    public Entry mergeUpdates(Dn partitionDn, ID entryID,  Entry entry )
    {
         Transaction<ID> curTxn = txnManager.getCurTxn();
         
         if ( ( curTxn == null ) )
         {
             throw new IllegalStateException( "Trying to merge with log wihout txn" );
         }
        
        return curTxn.mergeUpdates( partitionDn, entryID, entry );
    }
    
   
    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object, Entry, ID> wrap( Dn partitionDn, IndexCursor<Object, Entry, ID> wrappedCursor, IndexComparator<Object,ID> comparator, String attributeOid, boolean forwardIndex, Object onlyValueKey, ID onlyIDKey ) throws Exception
    {
        return new IndexCursorWrapper<ID>( partitionDn, wrappedCursor, comparator, attributeOid, forwardIndex, onlyValueKey, onlyIDKey );
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
        Transaction<ID> curTxn = txnManager.getCurTxn();

        if ( ( curTxn == null ) )
        {
            throw new IllegalStateException( "Trying to add dn set wihout txn" );
        }

        // No need to do anything for read only txns
        if ( !( curTxn instanceof ReadWriteTxn ) )
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