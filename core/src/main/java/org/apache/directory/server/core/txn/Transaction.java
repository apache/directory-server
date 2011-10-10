
package org.apache.directory.server.core.txn;

import java.util.List;


interface Transaction
{
    public List<ReadWriteTxn> getTxnsToCheck();
    
    public long getStartTime();
    
    public void startTxn( long startTime );
    
    public void commitTxn( long commitTime );
    
    public long getCommitTime();
    
    public void abortTxn();
    
    public State getState();    
    
    
    enum State
    {
        INITIAL,
        READ,
        COMMIT,
        ABORT   
    }

}
