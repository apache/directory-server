package org.apache.directory.server.core.api.partition;

import java.io.Closeable;
import java.io.IOException;

public interface PartitionTxn extends Closeable
{
    /**
     * Commit a write transaction. It will apply the changes on 
     * the database.Last, not least, a new version will be created.
     * If called by a Read transaction, it will simply close it.
     */
    void commit() throws IOException;
    
    
    /**
     * Abort a transaction. If it's a {@link PartitionReadTxn}, it will unlink this transaction
     * from the version it used. If it's a {@link PartitionWriteTxn}; it will drop all the pending
     * changes. The latest version will remain the same.
     */
    void abort() throws IOException;

    
    /**
     * Tells if the transaction has been committed/aborted or not.
     *  
     * @return <tt>true</tt> if the transaction has been completed.
     */
    boolean isClosed();
}
