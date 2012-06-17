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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.ReverseIndexEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
abstract class AbstractTransaction implements Transaction
{
    /** Logical time(LSN in the wal) when the txn began */
    private long startTime;

    /** logical commit time, set when txn commits */
    protected long commitTime;

    /** State of the transaction */
    private State txnState;

    /** List of txns that this txn depends on */
    private List<ReadWriteTxn> txnsToCheck = new ArrayList<ReadWriteTxn>();

    protected long id;

    private static AtomicLong counter = new AtomicLong( 0 );
    
    /** True optimistic lock is held by the txn */
    private boolean isOptimisticLockHeld = false;
    
    /** version of the logical data vseen by this txn */
    private long myLogicalDataVersion;

    
    public void setTxnId( long id )
    {
    	this.id = id;
    }
    
    public boolean isOptimisticLockHeld()
    {
        return isOptimisticLockHeld;
    }
    
    
    public void setOptimisticLockHeld()
    {
        isOptimisticLockHeld = true;
    }
    
    
    public void clearOptimisticLockHeld()
    {
        isOptimisticLockHeld = false;
    }
    
    
    public long getLogicalDataVersion()
    {
        return myLogicalDataVersion;
    }
    
    
    public void setLogicalDataVersion( long logicalDataVersion )
    {
        myLogicalDataVersion = logicalDataVersion;
    }


    /**
     * TODO : doco
     */
    public AbstractTransaction()
    {
        txnState = State.INITIAL;
        id = counter.getAndIncrement();
    }

    

    /**
     * {@inheritDoc}
     */
    public void startTxn( long startTime, long logicalDataVerion  )
    {
        this.startTime = startTime;
        txnState = State.READ;
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTime()
    {
        return startTime;
    }


    /**
     * {@inheritDoc}
     */
    public void commitTxn( long commitTime )
    {
        this.commitTime = commitTime;
        txnState = State.COMMIT;
    }


    /**
     * {@inheritDoc}
     */
    public long getCommitTime()
    {
        return commitTime;
    }


    /**
     * {@inheritDoc}
     */
    public void abortTxn()
    {
        txnState = State.ABORT;
    }


    /**
     * {@inheritDoc}
     */
    public List<ReadWriteTxn> getTxnsToCheck()
    {
        return txnsToCheck;
    }


    /**
     * {@inheritDoc}
     */
    public State getState()
    {
        return txnState;
    }


    /**
     * {@inheritDoc}
     */
    public Entry mergeUpdates( Dn partitionDn, UUID entryID, Entry entry )
    {
        Entry prevEntry = entry;
        Entry curEntry = entry;
        boolean cloneOnChange = true;

        for ( ReadWriteTxn curTxn : txnsToCheck )
        {
            curEntry = curTxn.applyUpdatesToEntry( partitionDn, entryID, curEntry, cloneOnChange );

            if ( curEntry != prevEntry )
            {
                cloneOnChange = false;
            }
        }

        return curEntry;
    }


    /**
     * {@inheritDoc}
     */
    public UUID mergeForwardLookup( Dn partitionDn, String attributeOid, Object key, UUID curId,
        Comparator<Object> valueComparator )
    {
        ForwardIndexEntry<Object> indexEntry = new ForwardIndexEntry<Object>();
        indexEntry.setId( curId );
        indexEntry.setValue( key );

        for ( ReadWriteTxn curTxn : txnsToCheck )
        {
            curTxn.updateForwardLookup( partitionDn, attributeOid, indexEntry, valueComparator );
        }

        return indexEntry.getId();
    }


    /**
     * {@inheritDoc}
     */
    public Object mergeReverseLookup( Dn partitionDn, String attributeOid, UUID id, Object curValue )
    {
        ReverseIndexEntry<Object> indexEntry = new ReverseIndexEntry<Object>();
        indexEntry.setId( id );
        indexEntry.setValue( curValue );

        for ( ReadWriteTxn curTxn : txnsToCheck )
        {
            curTxn.updateReverseLookup( partitionDn, attributeOid, indexEntry );
        }

        return indexEntry.getValue();
    }


    /**
     * {@inheritDoc}
     */
    public boolean mergeExistence( Dn partitionDN, String attributeOid, IndexEntry<?> indexEntry,
        boolean currentlyExists )
    {
        boolean forward = ( indexEntry instanceof ForwardIndexEntry );

        for ( ReadWriteTxn curTxn : txnsToCheck )
        {
            currentlyExists = curTxn.updateExistence( partitionDN, attributeOid, indexEntry, currentlyExists, forward );
        }

        return currentlyExists;
    }


    public long getId()
    {
        return id;
    }
}