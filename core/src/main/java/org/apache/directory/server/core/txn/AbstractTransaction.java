
package org.apache.directory.server.core.txn;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;

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
        this.setState( State.READ );
    }
    
    /**
     * {@inheritDoc}
     */  
    public long getStartTime()
    {
        return this.startTime;
    }
    
    /**
     * {@inheritDoc}
     */
    public void commitTxn( long commitTime )
    {
        this.commitTime = commitTime;
        this.setState( State.COMMIT );
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
       this.setState( State.ABORT ); 
    }

    
    /**
     * {@inheritDoc}
     */  
    public List<ReadWriteTxn<ID>> getTxnsToCheck()
    {
        return this.txnsToCheck;
    }
    
    /**
     * {@inheritDoc}
     */  
    public State getState()
    {
        return this.txnState;
    }
    
    /**
     * {@inheritDoc}
     */  
    public void setState( State newState )
    {
        this.txnState = newState;
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