package org.apache.directory.server.core.partition.impl.btree.mavibot;

import org.apache.directory.mavibot.btree.Transaction;
import org.apache.directory.server.core.api.partition.PartitionTxn;

public interface MavibotTxn extends PartitionTxn, Transaction
{
    Transaction getTransaction();
}
