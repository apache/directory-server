
package org.apache.directory.server.core.txn;

import java.util.List;

import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;


interface Transaction<ID>
{
    public List<ReadWriteTxn<ID>> getTxnsToCheck();
    
    public long getStartTime();
    
    public void startTxn( long startTime );
    
    public void commitTxn( long commitTime );
    
    public long getCommitTime();
    
    public void abortTxn();
    
    public State getState();
    
    public Entry mergeUpdates( Dn partitionDn, ID entryID, Entry entry );
    
    enum State
    {
        INITIAL,
        READ,
        COMMIT,
        ABORT   
    }

}
