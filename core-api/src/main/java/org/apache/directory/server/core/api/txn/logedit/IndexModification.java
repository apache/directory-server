
package org.apache.directory.server.core.api.txn.logedit;

import org.apache.directory.server.core.api.partition.Partition;

public interface IndexModification extends DataChange
{
    void applyModification( Partition partition, boolean recovery ) throws Exception;
}
