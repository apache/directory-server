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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
abstract class AbstractTransaction<ID> implements Transaction<ID>
{
    /** Logical time(LSN in the wal) when the txn began */ 
    long startTime;
    
    /** logical commit time, set when txn commits */
    long commitTime;
    
    /** State of the transaction */
    State txnState;
    
    /** List of txns that this txn depends */
    List<ReadWriteTxn<ID>> txnsToCheck = new ArrayList<ReadWriteTxn<ID>>();
 
    
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
    public List<ReadWriteTxn<ID>> getTxnsToCheck()
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
    
    
    public Entry mergeUpdates( Dn partitionDn, ID entryID, Entry entry )
    {
        Entry prevEntry  = entry;
        Entry curEntry = entry;
        ReadWriteTxn<ID> curTxn;
        boolean cloneOnChange = true;
        
        Iterator<ReadWriteTxn<ID>> it = txnsToCheck.iterator();
        
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
}