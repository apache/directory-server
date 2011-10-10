
package org.apache.directory.server.core.txn;

public interface TxnManagerInternal extends TxnManager
{
    public Transaction getCurTxn();
}
