package org.apache.directory.server.core.shared.txn;

import org.apache.directory.server.core.api.log.Log;
import org.apache.directory.server.core.api.txn.TxnLogManager;

interface TxnLogManagerInternal extends TxnLogManager
{
	/**
	 * 
	 * @return return the wal log manager used by the txnlogmanager
	 */
	Log getWAL();
}
