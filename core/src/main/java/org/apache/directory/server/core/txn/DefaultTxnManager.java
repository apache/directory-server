
package org.apache.directory.server.core.txn;

import org.apache.directory.server.core.txn.logedit.TxnStateChange;
import org.apache.directory.server.core.log.LogAnchor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Iterator;
import java.util.List;


import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.directory.server.core.log.UserLogRecord;
import org.apache.directory.server.core.log.LogAnchor;

import java.io.IOException;


public class DefaultTxnManager implements TxnManager, TxnManagerInternal
{
    /** wal log manager */
    TxnLogManager txnLogManager;
    
    /** List of committed txns in commit LSN order */
    ConcurrentLinkedQueue<ReadWriteTxn> committedQueue = new ConcurrentLinkedQueue<ReadWriteTxn>();
    
    /** Verify lock under which txn verification is done */
    Lock verifyLock = new ReentrantLock();
    
    /** Used to assign start and commit version numbers to writeTxns */
    Lock writeTxnsLock = new ReentrantLock();
    
    /** Latest committed txn on which read only txns can depend */
    AtomicReference<ReadWriteTxn> latestCommittedTxn = new AtomicReference<ReadWriteTxn>();
    
    /** Latest verified write txn */
    AtomicReference<ReadWriteTxn> latestVerifiedTxn = new AtomicReference<ReadWriteTxn>();
    
    /** Latest flushed txn's logical commit time */
    AtomicLong latestFlushedTxnLSN = new AtomicLong( 0 );
    
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
    
    public void init( TxnLogManager txnLogManager )
    {
        this.txnLogManager = txnLogManager;
    }
    
    /**
     * {@inheritDoc}
     */  
    public void beginTransaction( boolean readOnly ) throws IOException
    {
        Transaction curTxn = txnVar.get();
        
        if ( curTxn != null )
        {
            throw new IllegalStateException("Cannot begin a txn when txn is already running: " + 
                curTxn);
        }
        
        if ( readOnly )
        {
            this.beginReadOnlyTxn();
        }
        else
        {
            this.beginReadWriteTxn();
        }
        
    }
    
    /**
     * {@inheritDoc}
     */
    public void commitTransaction() throws IOException
    {
        Transaction txn = txnVar.get();
        
        if ( txn == null )
        {
            throw new IllegalStateException(" trying to commit non existent txn ");
        }
        
        this.prepareForEndingTxn( txn );
        
        if ( txn instanceof ReadOnlyTxn )
        {
            txn.commitTxn( txn.getStartTime() );
        }
        else
        {
            this.commitReadWriteTxn( (ReadWriteTxn)txn );
        }
        
        txnVar.set( null );
            
    }
    
    /**
     * {@inheritDoc}
     */
    public void abortTransaction() throws IOException
    {
        Transaction txn = txnVar.get();
        
        if ( txn == null )
        {
            // this is acceptable
            return;
        }
        
        this.prepareForEndingTxn( txn );
        
        if ( txn instanceof ReadWriteTxn )
        {
            this.abortReadWriteTxn( (ReadWriteTxn)txn );
        }
        
        txn.abortTxn();
        txnVar.set( null );
    }
    
    /**
     * {@inheritDoc}
     */
    public Transaction getCurTxn()
    {
       return txnVar.get(); 
    }
    
    private void beginReadOnlyTxn()
    {
        ReadOnlyTxn txn = new ReadOnlyTxn();
        ReadWriteTxn lastTxnToCheck = null;
        
        do
        {
            if ( lastTxnToCheck != null )
            {
                lastTxnToCheck.getRefCount().decrementAndGet();
            }
            
            lastTxnToCheck = latestCommittedTxn.get();
            lastTxnToCheck.getRefCount().getAndIncrement();
        }while ( lastTxnToCheck != latestCommittedTxn.get()  );
        
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
        
        this.buildCheckList( txn, lastTxnToCheck );
        txnVar.set( txn );
    }
    
    private void beginReadWriteTxn() throws IOException
    {
        long txnID;
        
        ReadWriteTxn txn = new ReadWriteTxn();
        UserLogRecord logRecord = txn.getUserLogRecord();
        
        TxnStateChange txnRecord = new TxnStateChange( LogAnchor.UNKNOWN_LSN, 
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
        
        ReadWriteTxn lastTxnToCheck = null; 
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
        this.buildCheckList( txn, lastTxnToCheck );
        
        txnVar.set( txn );
    }
    
    
    
    private void buildCheckList( Transaction txn, ReadWriteTxn lastTxnToCheck )
    {
        if ( lastTxnToCheck != null )
        {
            long lastLSN = lastTxnToCheck.getCommitTime();
            ReadWriteTxn toAdd;

            List<ReadWriteTxn> toCheckList = txn.getTxnsToCheck();
            Iterator<ReadWriteTxn> it = committedQueue.iterator();
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
            ReadWriteTxn toCheck;
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
    
    
    private void prepareForEndingTxn( Transaction txn )
    {
        List<ReadWriteTxn> toCheck = txn.getTxnsToCheck();
        
        if ( toCheck.size() > 0 )
        {
            ReadWriteTxn lastTxnToCheck = toCheck.get( toCheck.size() - 1 );
            
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
    
    private void commitReadWriteTxn( ReadWriteTxn txn ) throws IOException
    {
        UserLogRecord logRecord = txn.getUserLogRecord();

        TxnStateChange txnRecord = new TxnStateChange( txn.getStartTime(),
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
       
        // TODO verify txn here throw conflict exception if necessary

        
        writeTxnsLock.lock();
        try
        {
           // TODO sync of log can be done outside the locks. 
           txnLogManager.log( logRecord, true );
           txn.commitTxn( logRecord.getLogAnchor().getLogLSN() );
           
           latestVerifiedTxn.set( txn );
           
           // TODO when sync is done outside the locks, advance latest commit outside the locks
           latestCommittedTxn.set( txn );
        }
        finally
        {
            writeTxnsLock.unlock();
            verifyLock.unlock();
        }
    }
    
    
    private void abortReadWriteTxn( ReadWriteTxn txn ) throws IOException
    {
        UserLogRecord logRecord = txn.getUserLogRecord();

        TxnStateChange txnRecord = new TxnStateChange( txn.getStartTime(),
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
