
package org.apache.directory.server.core.txn;

import org.apache.directory.server.core.txn.logedit.LogEdit;

import org.apache.directory.server.core.log.UserLogRecord;

import java.io.IOException;

public interface TxnLogManager
{
    public void log( LogEdit logEdit, boolean sync ) throws IOException;
    
    public void log( UserLogRecord logRecord, boolean sync ) throws IOException;
}
