
package org.apache.directory.server.core.txn;

import java.util.List;
import java.util.LinkedList;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.server.core.txn.logedit.LogEdit;

import org.apache.directory.server.core.log.UserLogRecord;

public class ReadWriteTxn extends AbstractTransaction
{  
    /** list of log edits by the txn */
    List<LogEdit> logEdits = new LinkedList<LogEdit>();
    
    /*
     * Number of txns that depend on this txn and previous committed
     * txns. This number is bumped up only after the txn is committed.
     * A txn can be flushed to partitions only after the txn itself is
     * committed and ref count becomes zero for all the previously
     * committed txns.
     */
    AtomicInteger txnRefCount = new AtomicInteger( 0 );
    
    /** User record used to communicate data with log manager */
    UserLogRecord logRecord = new UserLogRecord();
    
    // TODO add a map of index changes 
   
    
      
    public AtomicInteger getRefCount()
    {
        return this.txnRefCount;
    }
    
    public UserLogRecord getUserLogRecord()
    {
        return this.getUserLogRecord();
    }
    
    public List<LogEdit> getEdits()
    {
        return logEdits;
    }
    
    
}
