
package org.apache.directory.server.core.txn;

import java.util.List;
import java.util.ArrayList;

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
    public List<ReadWriteTxn> getTxnsToCheck()
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
    
    
}