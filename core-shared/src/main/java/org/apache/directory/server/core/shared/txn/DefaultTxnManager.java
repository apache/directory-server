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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.directory.server.core.api.log.Log;
import org.apache.directory.server.core.api.log.LogAnchor;
import org.apache.directory.server.core.api.log.LogScanner;
import org.apache.directory.server.core.api.log.UserLogRecord;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.api.txn.TxnConflictException;
import org.apache.directory.server.core.api.txn.TxnHandle;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.api.txn.logedit.LogEdit;
import org.apache.directory.server.core.api.txn.logedit.LogEdit.EditType;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;
import org.apache.directory.server.core.shared.txn.logedit.TxnStateChange;
import org.apache.directory.server.core.shared.txn.logedit.TxnStateChange.ChangeState;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/** Package protected */
class DefaultTxnManager implements TxnManagerInternal
{
    /** wal log manager */
    private TxnLogManager txnLogManager;

    /** Write ahead log */
    private Log wal;

    /** List of committed txns in commit LSN order */
    private ConcurrentLinkedQueue<ReadWriteTxn> committedQueue = new ConcurrentLinkedQueue<ReadWriteTxn>();

    /** Verify lock under which txn verification is done */
    private Lock verifyLock = new ReentrantLock();

    /** Used to assign start and commit version numbers to writeTxns */
    private Lock writeTxnsLock = new ReentrantLock();

    /** Used to order txns in case of conflicts */
    private ReentrantReadWriteLock optimisticLock = new ReentrantReadWriteLock();

    /** Latest committed txn on which read only txns can depend */
    private AtomicReference<ReadWriteTxn> latestCommittedTxn = new AtomicReference<ReadWriteTxn>();

    /** Latest verified write txn */
    private AtomicReference<ReadWriteTxn> latestVerifiedTxn = new AtomicReference<ReadWriteTxn>();

    /** Latest flushed txn's logical commit time */
    private AtomicLong latestFlushedTxnLSN = new AtomicLong( LogAnchor.UNKNOWN_LSN );

    /** Default flush interval in ms */
    private final static int DEFAULT_FLUSH_INTERVAL = 100;

    /** Flush interval */
    private int flushInterval;

    /** Flush lock */
    private Lock flushLock = new ReentrantLock();

    /** Number of flushed txns */
    private int numFlushedTxns;

    /** Number of flushed */
    private int numFlushes;

    /** Take a checkpoint every 1000 flushes ~100 secs */
    private final static int DEFAULT_FLUSH_ROUNDS = 1000;

    /** Flush Condition object */
    private Condition flushCondition = flushLock.newCondition();

    /** Whether flushing is failed */
    private boolean flushFailed;

    /** partitions to be synced after applying changes */
    private HashSet<Partition> flushedToPartitions = new HashSet<Partition>();

    /** Backgorund syncing thread */
    private LogSyncer syncer;

    /** Initial committed txn */
    private ReadWriteTxn dummyTxn = new ReadWriteTxn();

    private AtomicInteger pending = new AtomicInteger();

    /** Logical data version number */
    private long logicalDataVersion;

    /** Initial scan point into the logs */
    private LogAnchor initialScanPoint;

    /** Initial set of committed txns */
    private HashSet<Long> txnsToRecover = new HashSet<Long>();

    /** last flushed log anchor */
    private LogAnchor lastFlushedLogAnchor;

    /** Whether to avoid flushing */
    private boolean doNotFlush = false;

    /** Per thread txn context */
    static final ThreadLocal<Transaction> txnVar =
        new ThreadLocal<Transaction>()
        {
            @Override
            protected Transaction initialValue()
            {
                return null;
            }
        };


    /**
     * Inits the txn manager. A dummy txn is put to the committed queue for read
     * only txns to bump the ref count so that we can always keep track of the
     * minimum existing txn.
     * 
     * @param txnLogManager
     * @param idComparator
     * @param idSerializer
     */
    public void init( TxnLogManagerInternal txnLogManager )
    {
        this.txnLogManager = txnLogManager;
        wal = txnLogManager.getWAL();
        flushInterval = DEFAULT_FLUSH_INTERVAL;

        committedQueue.clear();
        latestFlushedTxnLSN.set( LogAnchor.UNKNOWN_LSN );
        txnsToRecover.clear();
        logicalDataVersion = 0;
        lastFlushedLogAnchor = new LogAnchor();

        initialScanPoint = wal.getCheckPoint();
        //System.out.println("checkpoint " + initialScanPoint);

        lastFlushedLogAnchor.resetLogAnchor( initialScanPoint );

        dummyTxn.commitTxn( initialScanPoint.getLogLSN() );
        latestCommittedTxn.set( dummyTxn );
        latestVerifiedTxn.set( dummyTxn );
        committedQueue.offer( dummyTxn );

        getTxnsToReover();

        if ( syncer == null )
        {
            syncer = new LogSyncer();
            syncer.setDaemon( true );
            syncer.start();
        }
    }


    public void shutdown()
    {
        //System.out.println("in shutdown");
        syncer.interrupt();

        try
        {
            syncer.join();
        }
        catch ( InterruptedException e )
        {
            //Ignore
        }

        // Do a best effort last flush
        flushLock.lock();

        try
        {
            if ( !doNotFlush )
            {
                flushTxns( true );
            }

            advanceCheckPoint( lastFlushedLogAnchor );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            flushLock.unlock();
        }

        syncer = null;
    }


    /**
     * {@inheritDoc}
     */
    public long getLogicalDataVersion()
    {
        return logicalDataVersion;
    }


    /**
     * {@inheritDoc}
     */
    public void bumpLogicalDataVersion()
    {
        logicalDataVersion++;
    }


    /**
     * {@inheritDoc}
     */
    public TxnHandle retryTransaction() throws Exception
    {
        Transaction curTxn = getCurTxn();

        // Should have a rw txn
        if ( ( curTxn == null ) ||
            !( curTxn instanceof ReadWriteTxn ) )
        {
            // Cannot start a TXN when a RW txn is ongoing 
            throw new IllegalStateException( "Unexpected txn state when trying txn: " +
                curTxn );
        }

        // abort current txn and start a new read write txn

        abortReadWriteTxn( ( ReadWriteTxn ) curTxn );
        curTxn.abortTxn();
        return beginReadWriteTxn( true );
    }


    /**
     * {@inheritDoc}
     */
    public Transaction beginTransaction( boolean readOnly ) throws Exception
    {
        Transaction curTxn = getCurTxn();

        // Deal with an existing TXN
        if ( curTxn != null )
        {
            // Cannot start a TXN when a RW txn is ongoing 
            throw new IllegalStateException( "Cannot begin a txn when txn is already running: " +
                curTxn );
        }

        // Normal situation : starts a brand new txn
        if ( readOnly )
        {
            return beginReadOnlyTxn();
        }
        else
        {
            return beginReadWriteTxn( false );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void commitTransaction() throws Exception, TxnConflictException
    {
        Transaction txn = getCurTxn();

        if ( txn == null )
        {
            throw new IllegalStateException( " trying to commit non existent txn " );
        }

        try
        {
            if ( flushFailed )
            {
                throw new IOException( "Flushing of txns failed" );
            }

            prepareForEndingTxn( txn );

            if ( txn instanceof ReadOnlyTxn )
            {
                txn.commitTxn( txn.getStartTime() );
            }
            else
            {
                commitReadWriteTxn( ( ReadWriteTxn ) txn );
            }
        }
        finally
        {
            // Release optimistic lock if it is held
            releaseOptimisticLock();
        }

        setCurTxn( null );

        //System.out.println( "TRAN: Committed " + txn );
    }


    /**
     * {@inheritDoc}
     */
    public void abortTransaction() throws Exception
    {
        Transaction txn = getCurTxn();

        if ( txn == null )
        {
            throw new IllegalStateException( "Trying to abort while there is not txn " );
        }

        try
        {
            prepareForEndingTxn( txn );

            if ( txn instanceof ReadWriteTxn )
            {
                abortReadWriteTxn( ( ReadWriteTxn ) txn );
            }

            txn.abortTxn();
        }
        finally
        {
            // Release optimistic lock if it is held
            releaseOptimisticLock();
            setCurTxn( null );
        }

        //System.out.println( "TRAN: Aborted " + txn );

    }


    /**
     * {@inheritDoc}
     */
    public TxnHandle suspendCurTxn()
    {
        Transaction curTxn = txnVar.get();

        setCurTxn( null );

        return curTxn;
    }


    /**
     * {@inheritDoc}
     */
    public void resumeTxn( TxnHandle txnHandle )
    {
        if ( txnHandle == null )
        {
            throw new IllegalArgumentException( "Cannot accept a null handle when resuming a txn " );
        }

        Transaction curTxn = txnVar.get();

        if ( curTxn != null )
        {
            throw new IllegalStateException( " Trying to resume txn" + txnHandle
                + " while there is already a txn running:" + curTxn );
        }

        setCurTxn( ( Transaction ) txnHandle );
    }


    /**
     * {@inheritDoc}
     */
    public Transaction getCurTxn()
    {
        return ( Transaction ) txnVar.get();
    }


    /**
     * {@inheritDoc}
     */
    public Transaction setCurTxn( TxnHandle transaction )
    {
        Transaction previousTransaction = ( Transaction ) txnVar.get();

        txnVar.set( ( Transaction ) transaction );

        return previousTransaction;
    }


    /**
     * {@inheritDoc}
     */
    public void applyPendingTxns()
    {
        flushLock.lock();

        try
        {
            flushTxns( false );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // Ignore
        }
        finally
        {
            flushLock.unlock();
        }

    }


    /**
     * {@inheritDoc}
     */
    public void startLogicalDataChange() throws LdapException
    {
        Transaction curTxn = getCurTxn();

        if ( curTxn == null )
        {
            return;
        }
        // Should have a rw txn
        if ( !( curTxn instanceof ReadWriteTxn ) )
        {
            // Cannot start a TXN when a RW txn is ongoing 
            throw new IllegalStateException( "Unexpected txn state when starting logical data change txn: " +
                curTxn );
        }

        // If txn is already exclusive then it can start logical data change immediately

        if ( !curTxn.isOptimisticLockHeld() )
        {
            throw new IllegalStateException( "Unexpected txn state when starting logical data change txn: " +
                " txn is not holding optimistic lock:" +
                curTxn );
        }

        // If lock is already held exclusively by the txn, then return
        if ( optimisticLock.isWriteLockedByCurrentThread() )
        {
            return;
        }

        long txnLogicalDataVersion = curTxn.getLogicalDataVersion();

        // Get operations lock in exclusive mode
        optimisticLock.readLock().unlock();
        optimisticLock.writeLock().lock();

        // If somebody raced and changed logical data, then bail out
        if ( getLogicalDataVersion() != txnLogicalDataVersion )
        {

            TxnConflictException e = new TxnConflictException();
            throw new LdapException( e );
        }

        // Finally bump of logical data version number
        bumpLogicalDataVersion();
    }


    /**
     * {@inheritDoc}
     */
    public void endLogicalDataRead()
    {
        // Should only be called for read only txns
        Transaction curTxn = getCurTxn();

        if ( curTxn == null || !( curTxn instanceof ReadOnlyTxn ) )
        {
            throw new IllegalStateException( "Unexpected txn state when ending logical data read:" + curTxn );
        }

        if ( !curTxn.isOptimisticLockHeld() )
        {
            throw new IllegalStateException(
                "Unexpected txn state when ending logical data read, optimistic lock not held:" +
                    curTxn );
        }

        releaseOptimisticLock();
    }


    /**
     * {@inheritDoc}
     */
    public boolean prepareForLogicalDataReinit()
    {
        Transaction curTxn = getCurTxn();

        if ( curTxn == null || !( curTxn instanceof ReadWriteTxn ) )
        {
            throw new IllegalStateException( "Unexpected txn state when preparing for logical data reinit:" + curTxn );
        }

        if ( optimisticLock.isWriteLockedByCurrentThread() )
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * If the thread holds optimistic lock, release it
     */
    private void releaseOptimisticLock()
    {
        Transaction curTxn = getCurTxn();

        if ( curTxn.isOptimisticLockHeld() )
        {
            if ( optimisticLock.isWriteLockedByCurrentThread() )
            {
                optimisticLock.writeLock().unlock();
            }
            else
            {
                optimisticLock.readLock().unlock();
            }

            curTxn.clearOptimisticLockHeld();
        }
    }


    /**
     * Begins a read only txn. A read only txn does not put any log edits
     * to the txn log.Its start time is the latest committed txn's commit time. 
     */
    private Transaction beginReadOnlyTxn()
    {
        ReadOnlyTxn txn = new ReadOnlyTxn();
        ReadWriteTxn lastTxnToCheck = null;

        optimisticLock.readLock().lock();
        txn.setOptimisticLockHeld();
        txn.setLogicalDataVersion( logicalDataVersion );

        /*
         * Set the start time as the latest committed txn's commit time. We need to make sure that
         * any change after our start time is not flushed to the partitions. Say we have txn1 as the
         * lastest committed txn. There is a small window where we get ref to txn1, txn2 commits and
         * becomes the latest committed txn, txn1's ref count becomes zero before we bump its ref
         * count and changes to txn2 are flushed to partitions. Below we loop until we make sure
         * that the txn for which we bumped up the ref count is indeed the latest committed txn.
         */
        do
        {
            if ( lastTxnToCheck != null )
            {
                lastTxnToCheck.getRefCount().decrementAndGet();
            }

            lastTxnToCheck = latestCommittedTxn.get();
            lastTxnToCheck.getRefCount().getAndIncrement();
        }
        while ( lastTxnToCheck != latestCommittedTxn.get() );

        // Determine start time
        long startTime;

        startTime = lastTxnToCheck.getCommitTime();
        txn.startTxn( startTime, logicalDataVersion );

        buildCheckList( txn, lastTxnToCheck );

        setCurTxn( txn );

        //System.out.println( "TRAN: Started " + txn );

        return txn;
    }


    /**
     * Begins a read write txn. A start txn marker is inserted
     * into the txn log and the lsn of that log record is the
     * start time.
     */
    private Transaction beginReadWriteTxn( boolean retry ) throws Exception
    {

        ReadWriteTxn txn = new ReadWriteTxn();
        UserLogRecord logRecord = txn.getUserLogRecord();

        LogEdit logEdit = new TxnStateChange( LogAnchor.UNKNOWN_LSN,
            TxnStateChange.ChangeState.TXN_BEGIN );

        logEdit.injectData( logRecord, UserLogRecord.LogEditType.TXN );

        if ( retry == false )
        {
            optimisticLock.readLock().lock();
        }
        else
        {
            optimisticLock.writeLock().lock();
        }

        txn.setOptimisticLockHeld();
        txn.setLogicalDataVersion( logicalDataVersion );

        /*
         * Get the start time and last txn to depend on
         * when mergin data under te writeTxnLock.
         */
        ReadWriteTxn lastTxnToCheck = null;

        writeTxnsLock.lock();

        try
        {
            txnLogManager.log( logRecord, false );
            txn.startTxn( logRecord.getLogAnchor().getLogLSN(), logicalDataVersion );
            txn.setTxnId( logRecord.getLogAnchor().getLogLSN() );

            do
            {
                if ( lastTxnToCheck != null )
                {
                    lastTxnToCheck.getRefCount().decrementAndGet();
                }

                lastTxnToCheck = latestVerifiedTxn.get();

                lastTxnToCheck.getRefCount().incrementAndGet();
            }
            while ( lastTxnToCheck != latestVerifiedTxn.get() );
        }
        catch ( Exception e )
        {
            // Release optimistic lock if held
            setCurTxn( txn );
            releaseOptimisticLock();

        }
        finally
        {
            writeTxnsLock.unlock();
        }

        // Finally build the check list
        buildCheckList( txn, lastTxnToCheck );

        setCurTxn( txn );

        //System.out.println( "TRAN: Started " + txn );

        return txn;
    }


    /**
     * Builds the list of txns which the given txn should check while mergin what it read from
     * the partitions with the changes in the txn log. These are the txns that committed before
     * the start of the give txn and for which the changes are not flushed to the partitions yet.
     * Note that, for some of these txns, flush to partitions could go on in parallel.
     *
     * @param txn txn for which we will build the check list
     * @param lastTxnToCheck latest txn to check
     */
    private void buildCheckList( Transaction txn, ReadWriteTxn lastTxnToCheck )
    {
        long lastLSN = lastTxnToCheck.getCommitTime();

        List<ReadWriteTxn> toCheckList = txn.getTxnsToCheck();
        long flushedLSN = latestFlushedTxnLSN.get();

        // Get all the txns that has been committed before the new txn
        for ( ReadWriteTxn toAdd : committedQueue )
        {
            long commitTime = toAdd.getCommitTime();

            if ( commitTime > lastLSN )
            {
                break;
            }

            /*
             * Get latest flushed lsn and eliminate already flushed txn from the check list.
             */
            if ( ( commitTime <= flushedLSN ) && ( toAdd != lastTxnToCheck ) )
            {
                continue;
            }

            toCheckList.add( toAdd );
        }

        // A read write txn, always has to check its changes
        if ( txn instanceof ReadWriteTxn )
        {
            txn.getTxnsToCheck().add( ( ReadWriteTxn ) txn );
        }
    }


    /**
     * Called before ending a txn. Txn for which this txn bumped 
     * up the ref count is gotten and its ref count is decreased.
     *
     * @param txn txn which is about to commit or abort
     */
    private void prepareForEndingTxn( Transaction txn )
    {
        List<ReadWriteTxn> toCheck = txn.getTxnsToCheck();

        // A read write txn, always has to check its changes
        if ( txn instanceof ReadWriteTxn )
        {

            if ( toCheck.size() <= 0 )
            {
                throw new IllegalStateException(
                    " prepareForEndingTxn: a read write txn should at least depend on itself:" + txn );
            }

            txn.getTxnsToCheck().remove( ( ReadWriteTxn ) txn );
        }

        if ( toCheck.size() > 0 )
        {
            ReadWriteTxn lastTxnToCheck = toCheck.get( toCheck.size() - 1 );

            if ( lastTxnToCheck.commitTime > txn.getStartTime() )
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

            //            if ( txn instanceof ReadOnlyTxn )
            //            {
            //                System.out.println(" txn end " + txn.getStartTime() + " " + 
            //                    lastTxnToCheck.getRefCount() 
            //                    + " pending " + pending.decrementAndGet() );
            //            }
        }
    }


    /**
     * Tries to commit the given read write txn. Before a read write txn can commit, it is
     * verified against the txns that committed after this txn started. If a conflicting change is
     * found, a conflict exception is thrown. 
     * 
     * If a txn can commit, a commit record is inserted into the txn log. The lsn of the commit record
     * is the commit time of the txn.
     * 
     * Note that, a txn is not committed until its commit record is synced to the underlying media. Say we haveread write txns rw1 and 
     * rw2 and that rw1 and rw2 is verified and their commit record are in the log but not synced to underlying media yet. A new read 
     * write txn rw3 and a read only txn r1 comes along. Since rw1 and rw2 wont be acked until they commit, r1 should not depend on rw1 and rw2 and can have a view 
     * as of a commit time before rw1 and rw2's commit time. If r1 depended rw1 or rw2 and we crashed before sycning rw1 and rw2's to the underlying media, 
     * r1 would have depended on a change that actually doesnt exist in the database. However, rw3 either has to depend on rw1 and rw2) or has to verify 
     * its changeset against rw1 and rw2 when it tries to commit. Whether the first thing(depending on rw1, rw2 and merging its changeset) or the second
     * thing ( verifiying its change set against rw1 and rw2) is determined by the order of the lsns of the commit record of rw1 and rw2  and start record of rw3.
     * Lets say we have this order in the txn log:
     *              commit record rw1, start record rw3, commit record rw2
     * then rw3 will merge its changes with that of rw1 and will verify its changes against rw2. When rw3 is merging its changeset with that of rw1, rw1 might not have
     * committed yet as its commit record might not have made it to the underlying media but this is OK as rw3 cannot commit before rw1 because of the log.
     *
     * @param txn txn to commit.
     * @throws Exception
     * @throws TxnConflictException
     */
    private void commitReadWriteTxn( ReadWriteTxn txn ) throws Exception, TxnConflictException
    {
        UserLogRecord logRecord = txn.getUserLogRecord();

        LogEdit logEdit = new TxnStateChange( txn.getStartTime(),
            TxnStateChange.ChangeState.TXN_COMMIT );

        logEdit.injectData( logRecord, UserLogRecord.LogEditType.TXN );

        verifyLock.lock();

        //Verify txn and throw conflict exception if necessary
        long startTime = txn.getStartTime();

        for ( ReadWriteTxn toCheckTxn : committedQueue )
        {
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


    /**
     * Aborts a read write txn. An abort record is inserted into the txn log.
     *
     * @param txn txn to abort
     * @throws IOException
     */
    private void abortReadWriteTxn( ReadWriteTxn txn ) throws Exception
    {
        UserLogRecord logRecord = txn.getUserLogRecord();

        LogEdit logEdit = new TxnStateChange( txn.getStartTime(),
            TxnStateChange.ChangeState.TXN_ABORT );

        logEdit.injectData( logRecord, UserLogRecord.LogEditType.TXN );

        txnLogManager.log( logRecord, false );
    }


    /**
     *  Flush the changes of the txns in the committed queue. A txn is flushed
     *  only if flushing it will not cause a pending txn to see changes beyond its
     *  start time.
     *  throws Exception thrown if anything goes wrong during flush.
     *  
     *  @param shutdown is TxnManager is shutting down
     *
     */
    private void flushTxns( boolean shutdown ) throws Exception
    {
        UserLogRecord lastLogRecord = null;

        // If flushing failed already, dont do anything anymore
        if ( flushFailed )
        {
            return;
        }

        /*
         * First get the latest committed txn ref and then the iterator.
         * Order is important.
         */
        ReadWriteTxn latestCommitted = latestCommittedTxn.get();
        long latestFlushedLsn = latestFlushedTxnLSN.get();
        flushedToPartitions.clear();

        Iterator<ReadWriteTxn> it = committedQueue.iterator();
        ReadWriteTxn txnToFlush = null;

        while ( it.hasNext() )
        {
            txnToFlush = it.next();

            if ( txnToFlush.getCommitTime() > latestFlushedLsn )
            {
                // Apply changes
                txnToFlush.flushLogEdits( flushedToPartitions );

                latestFlushedTxnLSN.set( txnToFlush.getCommitTime() );

                lastLogRecord = txnToFlush.getUserLogRecord();
            }

            if ( txnToFlush == latestCommitted )
            {
                // leave latest committed txn in queue and dont go beyond it.
                break;
            }

            numFlushedTxns++;

            //           if (  numFlushes % 100 == 0 )
            //           {
            //           System.out.println( "lastFlushed lsn: " + latestFlushedTxnLSN  + " " + committedQueue.size() );
            //           
            //           System.out.println( " last commit txn: " + latestCommitted.getCommitTime() );
            //           System.out.println( "txnToFlush: " + txnToFlush.getRefCount() + " " + txnToFlush.getCommitTime() ); 
            //           }

            /*
             *  If the latest flushed txn has ref count > 0, then
             *  following txns wont be flushed yet.
             */

            if ( txnToFlush.getRefCount().get() > 0 )
            {
                //     System.out.println( "breaking out: " + txnToFlush.getCommitTime()  + " " + committedQueue.size() );
                break;
            }

            // Remove from the queue
            it.remove();
        }

        // Sync each flushed to partition
        Iterator<Partition> partitionIt = flushedToPartitions.iterator();

        while ( partitionIt.hasNext() )
        {
            partitionIt.next().sync();
        }

        // Sync WAL to the last flushed LSN
        wal.sync( latestFlushedLsn );

        numFlushes++;

        if ( lastLogRecord != null )
        {
            lastFlushedLogAnchor.resetLogAnchor( lastLogRecord.getLogAnchor() );
        }

        if ( shutdown )
        {
            advanceCheckPoint( lastFlushedLogAnchor );
        }
        else if ( numFlushes % DEFAULT_FLUSH_ROUNDS == 0 )
        {
            advanceCheckPoint( lastFlushedLogAnchor );
        }

    }


    private void advanceCheckPoint( LogAnchor checkPoint )
    {
        wal.advanceCheckPoint( checkPoint );
    }


    private void getTxnsToReover()
    {
        LogScanner logScanner = wal.beginScan( initialScanPoint );
        UserLogRecord logRecord = new UserLogRecord();
        byte userRecord[];

        //System.out.println(" Get txns to recover " + initialScanPoint.getLogLSN() );

        try
        {
            while ( logScanner.getNextRecord( logRecord ) )
            {
                userRecord = logRecord.getDataBuffer();
                ObjectInputStream in = buildStream( userRecord );

                EditType editType = EditType.values()[in.read()];

                if ( editType == EditType.TXN_MARKER )
                {
                    TxnStateChange stateChange = new TxnStateChange();
                    stateChange.readExternal( in );

                    if ( stateChange.getTxnState() == ChangeState.TXN_COMMIT )
                    {
                        //System.out.println("Adding txn " + stateChange.getTxnID() + " to the tobe recovered txns");
                        txnsToRecover.add( new Long( stateChange.getTxnID() ) );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // Ignore
        }
    }


    // Walk over the txn log records from the latest checkpoint and apply the
    // log records to the partition
    public void recoverPartition( Partition partition )
    {
        Dn partitionSuffix = partition.getSuffixDn();

        //System.out.println("Recover partition " + partitionSuffix);

        LogScanner logScanner = wal.beginScan( initialScanPoint );
        UserLogRecord logRecord = new UserLogRecord();
        byte userRecord[];

        boolean recoveredChanges = false;

        try
        {
            while ( logScanner.getNextRecord( logRecord ) )
            {
                userRecord = logRecord.getDataBuffer();
                ObjectInputStream in = buildStream( userRecord );

                EditType editType = EditType.values()[in.read()];

                if ( editType == EditType.DATA_CHANGE )
                {
                    DataChangeContainer dataChangeContainer = new DataChangeContainer();
                    dataChangeContainer.readExternal( in );

                    //System.out.println("Data change container for " + dataChangeContainer.getPartitionDn() + 
                    //	" txn id " + dataChangeContainer.getTxnID() );

                    // If this change is for the partition we are tyring to recover 
                    // and belongs to a txn that committed, then 
                    Long txnID = new Long( dataChangeContainer.getTxnID() );

                    if ( txnsToRecover.contains( txnID ) )
                    {
                        if ( dataChangeContainer.getPartitionDn().equals( partitionSuffix ) )
                        {
                            //System.out.println("Apply change to partition " + partitionSuffix);
                            dataChangeContainer.setPartition( partition );
                            dataChangeContainer.apply( true );
                            recoveredChanges = true;
                        }
                    }
                }
            }

            if ( recoveredChanges && partition instanceof SchemaPartition )
            {
                ( ( SchemaPartition ) partition ).getSchemaManager().reloadAllEnabled();
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // Ignore for now
        }
    }


    public void setDoNotFlush()
    {
        doNotFlush = true;
    }


    public void unsetDoNotFlush()
    {
        doNotFlush = false;
    }


    private ObjectInputStream buildStream( byte[] buffer ) throws IOException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( buffer );

        try
        {
            oIn = new ObjectInputStream( in );

            return oIn;
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
    }

    class LogSyncer extends Thread
    {
        @Override
        public void run()
        {
            flushLock.lock();

            try
            {
                while ( true )
                {
                    flushCondition.await( flushInterval, TimeUnit.MILLISECONDS );

                    if ( !doNotFlush )
                    {
                        flushTxns( false );
                    }
                }
            }
            catch ( InterruptedException e )
            {
                // Bail out
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                flushFailed = true;
            }
            finally
            {
                flushLock.unlock();
            }
        }
    }
}
