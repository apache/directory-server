
package org.apache.directory.server.core.txn;

public interface TxnManagerInternal<ID> extends TxnManager<ID>
{
    public Transaction<ID> getCurTxn();
}
