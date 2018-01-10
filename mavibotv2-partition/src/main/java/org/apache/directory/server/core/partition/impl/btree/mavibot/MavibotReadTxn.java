package org.apache.directory.server.core.partition.impl.btree.mavibot;

import org.apache.directory.mavibot.btree.Transaction;

public class MavibotReadTxn extends AbstractMavibotTxn
{
    public MavibotReadTxn( Transaction transaction )
    {
        super( transaction );
    }
}
