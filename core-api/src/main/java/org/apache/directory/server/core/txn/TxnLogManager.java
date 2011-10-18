
package org.apache.directory.server.core.txn;

import org.apache.directory.server.core.txn.logedit.LogEdit;

import org.apache.directory.server.core.log.UserLogRecord;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;

import java.io.IOException;

public interface TxnLogManager<ID>
{
    public void log( LogEdit<ID> logEdit, boolean sync ) throws IOException;
    
    public void log( UserLogRecord logRecord, boolean sync ) throws IOException;
    
    public Entry mergeUpdates(Dn partitionDN, ID entryID,  Entry entry );
}
