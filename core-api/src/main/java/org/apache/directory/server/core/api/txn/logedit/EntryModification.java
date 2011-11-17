
package org.apache.directory.server.core.api.txn.logedit;

import java.util.UUID;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.shared.ldap.model.entry.Entry;

public interface EntryModification extends DataChange
{
    Entry applyModification( Partition partition, Entry curEntry, UUID entrId, long changeLsn, boolean recovery );
}
