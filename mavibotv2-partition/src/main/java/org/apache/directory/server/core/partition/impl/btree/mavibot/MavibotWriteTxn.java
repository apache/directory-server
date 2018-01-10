package org.apache.directory.server.core.partition.impl.btree.mavibot;

import org.apache.directory.mavibot.btree.Transaction;
import org.apache.directory.mavibot.btree.WriteTransaction;

public class MavibotWriteTxn extends AbstractMavibotTxn
{
    public MavibotWriteTxn( Transaction transaction )
    {
        super( transaction );
    }

    
    public WriteTransaction getWriteTransaction()
    {
        return ( WriteTransaction ) transaction;
    }
}
