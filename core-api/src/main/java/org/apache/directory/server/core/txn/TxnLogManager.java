
package org.apache.directory.server.core.txn;

import org.apache.directory.server.core.partition.index.IndexCursor;
import org.apache.directory.server.core.partition.index.IndexComparator;
import org.apache.directory.server.core.txn.logedit.LogEdit;

import org.apache.directory.server.core.log.UserLogRecord;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;

import java.io.IOException;
import java.util.Comparator;

public interface TxnLogManager<ID>
{
    public void log( LogEdit<ID> logEdit, boolean sync ) throws IOException;
    
    public void log( UserLogRecord logRecord, boolean sync ) throws IOException;
    
    public Entry mergeUpdates(Dn partitionDN, ID entryID,  Entry entry );
    
    public IndexCursor<Object, Entry, ID> wrap( Dn partitionDn, IndexCursor<Object, Entry, ID> wrappedCursor, IndexComparator<Object,ID> comparator, String attributeOid, boolean forwardIndex, Object onlyValueKey, ID onlyIDKey ) throws Exception;
}
