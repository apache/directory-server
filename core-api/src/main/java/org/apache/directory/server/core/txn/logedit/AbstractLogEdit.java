
package org.apache.directory.server.core.txn.logedit;

import org.apache.directory.server.core.log.LogAnchor;

public abstract class AbstractLogEdit implements LogEdit
{
    /** position in the wal */
    private transient LogAnchor logAnchor = new LogAnchor();
    
    /**
     * {@inheritDoc}
     */
    public LogAnchor getLogAnchor()
    {
        return logAnchor;
    }
}
