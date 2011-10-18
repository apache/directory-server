
package org.apache.directory.server.core.txn.logedit;

import org.apache.directory.server.core.log.LogAnchor;

import java.io.Externalizable;

public interface LogEdit<ID> extends Externalizable 
{
    /**
     * Returns the position the edit is inserted in the wal.
     * Log anchor is initialized is set after the edit is serialized and inserted into
     * the wal so it should be transient.
     *
     * @return position of the log edit in the wal
     */
    public LogAnchor getLogAnchor();
}
