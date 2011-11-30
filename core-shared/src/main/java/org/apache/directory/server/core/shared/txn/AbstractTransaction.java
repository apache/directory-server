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

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

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
    long startTime;
    
    /** logical commit time, set when txn commits */
    long commitTime;
    
    /** State of the transaction */
    State txnState;
    
    /** List of txns that this txn depends */
    List<ReadWriteTxn> txnsToCheck = new ArrayList<ReadWriteTxn>();
 
    
    /**
     * TODO : doco
     */
    public AbstractTransaction( )
    {
        txnState = State.INITIAL;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void startTxn( long startTime )
    {
        this.startTime = startTime;
        setState( State.READ );
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
        setState( State.COMMIT );
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
       setState( State.ABORT ); 
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
    public void setState( State newState )
    {
        txnState = newState;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Entry mergeUpdates( Dn partitionDn, UUID entryID, Entry entry )
    {
        Entry prevEntry  = entry;
        Entry curEntry = entry;
        ReadWriteTxn curTxn;
        boolean cloneOnChange = true;
        
        Iterator<ReadWriteTxn> it = txnsToCheck.iterator();
        
        while ( it.hasNext() )
        {
            curTxn = it.next();
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
    public UUID mergeForwardLookup(Dn partitionDn, String attributeOid,  Object key, UUID curId, Comparator<Object> valueComparator )
    {
        ForwardIndexEntry<Object> indexEntry = new ForwardIndexEntry<Object>();
        indexEntry.setId( curId );
        indexEntry.setValue( key );
        
        ReadWriteTxn curTxn;
        Iterator<ReadWriteTxn> it = txnsToCheck.iterator();
        
        while ( it.hasNext() )
        {
            curTxn = it.next();
            curTxn.updateForwardLookup( partitionDn, attributeOid, indexEntry, valueComparator );   
        }
        
        return indexEntry.getId();
    }
   
    
    /**
     * {@inheritDoc}
     */
    public Object mergeReverseLookup(Dn partitionDn, String attributeOid,  UUID id, Object curValue )
    {
        ReverseIndexEntry<Object> indexEntry = new ReverseIndexEntry<Object>();
        indexEntry.setId( id );
        indexEntry.setValue( curValue );
        
        ReadWriteTxn curTxn;
        Iterator<ReadWriteTxn> it = txnsToCheck.iterator();
        
        while ( it.hasNext() )
        {
            curTxn = it.next();
            curTxn.updateReverseLookup( partitionDn, attributeOid, indexEntry );
        }
        
        return indexEntry.getValue();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean mergeExistence(Dn partitionDN, String attributeOid,  IndexEntry<?> indexEntry, boolean currentlyExists )
    {
        ReadWriteTxn curTxn;
        boolean forward = ( indexEntry instanceof ForwardIndexEntry );
        
        Iterator<ReadWriteTxn> it = txnsToCheck.iterator();
        
        while ( it.hasNext() )
        {
            curTxn = it.next();
            currentlyExists = curTxn.updateExistence( partitionDN, attributeOid, indexEntry, currentlyExists, forward );
          
        }
        
        return currentlyExists;
    }
}