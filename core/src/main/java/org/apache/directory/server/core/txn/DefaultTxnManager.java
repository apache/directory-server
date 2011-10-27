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

import org.apache.directory.server.core.api.partition.index.Serializer;
import org.apache.directory.server.core.txn.logedit.TxnStateChange;
import org.apache.directory.server.core.log.LogAnchor;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.directory.server.core.log.UserLogRecord;

import java.io.IOException;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultTxnManager<ID> implements  TxnManagerInternal<ID>
{
    /** wal log manager */
    private TxnLogManager<ID> txnLogManager;
    
    /** List of committed txns in commit LSN order */
    private ConcurrentLinkedQueue<ReadWriteTxn<ID>> committedQueue = new ConcurrentLinkedQueue<ReadWriteTxn<ID>>();
    
    /** Verify lock under which txn verification is done */
    private Lock verifyLock = new ReentrantLock();
    
    /** Used to assign start and commit version numbers to writeTxns */
    private Lock writeTxnsLock = new ReentrantLock();
    
    /** Latest committed txn on which read only txns can depend */
    private AtomicReference<ReadWriteTxn<ID>> latestCommittedTxn = new AtomicReference<ReadWriteTxn<ID>>();
    
    /** Latest verified write txn */
    private AtomicReference<ReadWriteTxn<ID>> latestVerifiedTxn = new AtomicReference<ReadWriteTxn<ID>>();
    
    /** Latest flushed txn's logical commit time */
    private AtomicLong latestFlushedTxnLSN = new AtomicLong( 0 );
    
    /** ID comparator */
    private Comparator<ID> idComparator;
    
    /** ID serializer */
    private Serializer idSerializer ;
    
    /** Per thread txn context */
    static final ThreadLocal < Transaction > txnVar = 
         new ThreadLocal < Transaction > () 
         {
             @Override 
             protected Transaction initialValue()
             {
                 return null;
             }
        };
    
    /**
     * TODO : doco
     * @param txnLogManager
     * @param idComparator
     * @param idSerializer
     */
    public void init( TxnLogManager<ID> txnLogManager, Comparator<ID> idComparator, Serializer idSerializer )
    {
        this.txnLogManager = txnLogManager;
        this.idComparator = idComparator;
        this.idSerializer = idSerializer;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Comparator<ID> getIDComparator()
    {
        return idComparator;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Serializer getIDSerializer()
    {
        return idSerializer;
    }
    
    
    /**
     * {@inheritDoc}
     */  
    public void beginTransaction( boolean readOnly ) throws IOException
    {
        Transaction<ID> curTxn = getCurTxn();
        
        if ( curTxn != null )
        {
            throw new IllegalStateException("Cannot begin a txn when txn is already running: " + 
                curTxn);
        }
        
        if ( readOnly )
        {
            beginReadOnlyTxn();
        }
        else
        {
            beginReadWriteTxn();
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void commitTransaction() throws IOException, TxnConflictException
    {
        Transaction<ID> txn = getCurTxn();
        
        if ( txn == null )
        {
            throw new IllegalStateException(" trying to commit non existent txn ");
        }
        
        prepareForEndingTxn( txn );
        
        if ( txn instanceof ReadOnlyTxn )
        {
            txn.commitTxn( txn.getStartTime() );
        }
        else
        {
            commitReadWriteTxn( (ReadWriteTxn<ID>)txn );
        }
        
        txnVar.set( null );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void abortTransaction() throws IOException
    {
        Transaction<ID> txn = getCurTxn();
        
        if ( txn == null )
        {
            // this is acceptable
            return;
        }
        
        prepareForEndingTxn( txn );
        
        if ( txn instanceof ReadWriteTxn )
        {
            abortReadWriteTxn( (ReadWriteTxn<ID>)txn );
        }
        
        txn.abortTxn();
        txnVar.set( null );
    }
    
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Transaction<ID> getCurTxn()
    {
       return (Transaction<ID>)txnVar.get(); 
    }
    
    
    private void beginReadOnlyTxn()
    {
        ReadOnlyTxn<ID> txn = new ReadOnlyTxn<ID>();
        ReadWriteTxn<ID> lastTxnToCheck = null;
        
        do
        {
            if ( lastTxnToCheck != null )
            {
                lastTxnToCheck.getRefCount().decrementAndGet();
            }
            
            lastTxnToCheck = latestCommittedTxn.get();
            lastTxnToCheck.getRefCount().getAndIncrement();
        } while ( lastTxnToCheck != latestCommittedTxn.get()  );
        
        // Determine start time
        long startTime;
        
        if ( lastTxnToCheck != null )
        {
            startTime = lastTxnToCheck.getCommitTime();
        }
        else
        {
            startTime = LogAnchor.UNKNOWN_LSN;
        }
        
        txn.startTxn( startTime );
        
        buildCheckList( txn, lastTxnToCheck );
        txnVar.set( txn );
    }
    
    
    private void beginReadWriteTxn() throws IOException
    {
        
        ReadWriteTxn<ID> txn = new ReadWriteTxn<ID>();
        UserLogRecord logRecord = txn.getUserLogRecord();
        
        TxnStateChange<ID> txnRecord = new TxnStateChange<ID>( LogAnchor.UNKNOWN_LSN, 
                TxnStateChange.State.TXN_BEGIN );
        ObjectOutputStream out = null;
        ByteArrayOutputStream bout = null;
        byte[] data;

        try
        {
            bout = new ByteArrayOutputStream();
            out = new ObjectOutputStream( bout );
            out.writeObject( txnRecord );
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
        
        logRecord.setData(  data, data.length );
        
        ReadWriteTxn<ID> lastTxnToCheck = null; 
        writeTxnsLock.lock();
        
        try
        {
            txnLogManager.log( logRecord, false );
            txn.startTxn( logRecord.getLogAnchor().getLogLSN() );
            
            do
            {
                if ( lastTxnToCheck != null )
                {
                    lastTxnToCheck.getRefCount().decrementAndGet();
                }
                
                lastTxnToCheck = latestVerifiedTxn.get();
                lastTxnToCheck.getRefCount().incrementAndGet();
            } while ( lastTxnToCheck != latestVerifiedTxn.get() );
            
        }
        finally
        {
            writeTxnsLock.unlock();
        }
        
        // Finally build the check list
        buildCheckList( txn, lastTxnToCheck );
        
        txnVar.set( txn );
    }
    
    
    private void buildCheckList( Transaction<ID> txn, ReadWriteTxn<ID> lastTxnToCheck )
    {
        if ( lastTxnToCheck != null )
        {
            long lastLSN = lastTxnToCheck.getCommitTime();
            ReadWriteTxn<ID> toAdd;

            List<ReadWriteTxn<ID>> toCheckList = txn.getTxnsToCheck();
            Iterator<ReadWriteTxn<ID>> it = committedQueue.iterator();
            
            while ( it.hasNext() )
            {
                toAdd = it.next();

                if ( toAdd.getCommitTime() > lastLSN )
                {
                    break;
                }

                toCheckList.add( toAdd );
            }

            /*
             * Get latest flushed lsn and eliminate already flushed txn from the check list.
             */
            long flushedLSN = latestFlushedTxnLSN.get();

            it = toCheckList.iterator();
            ReadWriteTxn<ID> toCheck;
            
            while ( it.hasNext() )
            {
                toCheck = it.next();
                
                if ( toCheck.commitTime <= flushedLSN )
                {
                    it.remove();
                }
            }
        }
    }
    
    
    private void prepareForEndingTxn( Transaction<ID> txn )
    {
        List<ReadWriteTxn<ID>> toCheck = txn.getTxnsToCheck();
        
        if ( toCheck.size() > 0 )
        {
            ReadWriteTxn<ID> lastTxnToCheck = toCheck.get( toCheck.size() - 1 );
            
            if ( lastTxnToCheck.commitTime != txn.getStartTime() )
            {
                throw new IllegalStateException( " prepareForEndingTxn: txn has unpexptected start time " + 
                    txn + " expected: " + lastTxnToCheck );
            }
            
            if ( lastTxnToCheck.getRefCount().get() <= 0 )
            {
                throw new IllegalStateException( " prepareForEndingTxn: lastTxnToCheck has unexpected ref cnt " + 
                    txn + " expected: " + lastTxnToCheck );
            }
            
            lastTxnToCheck.getRefCount().decrementAndGet();
        }
    }
    
    
    private void commitReadWriteTxn( ReadWriteTxn<ID> txn ) throws IOException, TxnConflictException
    {
        UserLogRecord logRecord = txn.getUserLogRecord();

        TxnStateChange<ID> txnRecord = new TxnStateChange<ID>( txn.getStartTime(),
            TxnStateChange.State.TXN_COMMIT );
        ObjectOutputStream out = null;
        ByteArrayOutputStream bout = null;
        byte[] data;

        try
        {
            bout = new ByteArrayOutputStream();
            out = new ObjectOutputStream( bout );
            out.writeObject( txnRecord );
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
        
        verifyLock.lock();
       
        //Verify txn and throw conflict exception if necessary
        Iterator<ReadWriteTxn<ID>> it = committedQueue.iterator();
        ReadWriteTxn toCheckTxn;
        long startTime = txn.getStartTime();
        
        while ( it.hasNext() )
        {
            toCheckTxn = it.next();

            // Check txns that committed after we started 
            if ( toCheckTxn.getCommitTime() < startTime )
            {
                continue;
            }

            if ( txn.hasConflict( toCheckTxn ) )
            {
                verifyLock.unlock();
                throw new TxnConflictException();
            }
        }
        
        writeTxnsLock.lock();
        
        try
        {
           // TODO sync of log can be done outside the locks. 
           txnLogManager.log( logRecord, true );
           txn.commitTxn( logRecord.getLogAnchor().getLogLSN() );
           
           latestVerifiedTxn.set( txn );
           committedQueue.offer( txn );
           
           // TODO when sync is done outside the locks, advance latest commit outside the locks
           latestCommittedTxn.set( txn );
        }
        finally
        {
            writeTxnsLock.unlock();
            verifyLock.unlock();
        }
    }
    
    
    private void abortReadWriteTxn( ReadWriteTxn<ID> txn ) throws IOException
    {
        UserLogRecord logRecord = txn.getUserLogRecord();

        TxnStateChange<ID> txnRecord = new TxnStateChange<ID>( txn.getStartTime(),
            TxnStateChange.State.TXN_ABORT );
        ObjectOutputStream out = null;
        ByteArrayOutputStream bout = null;
        byte[] data;

        try
        {
            bout = new ByteArrayOutputStream();
            out = new ObjectOutputStream( bout );
            out.writeObject( txnRecord );
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
        txnLogManager.log( logRecord, false );
    }
}
