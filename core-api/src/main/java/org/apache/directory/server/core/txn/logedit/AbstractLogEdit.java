
package org.apache.directory.server.core.txn.logedit;

import org.apache.directory.server.core.log.LogAnchor;

public abstract class AbstractLogEdit<ID> implements LogEdit<ID>
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
