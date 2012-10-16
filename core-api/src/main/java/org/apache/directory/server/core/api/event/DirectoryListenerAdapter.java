
package org.apache.directory.server.core.api.event;

import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;

/**
 * A DirectoryListener adapter class for convenient subclassing
 * It doesn't handle any operation 
 */
public abstract class DirectoryListenerAdapter implements DirectoryListener
{

    @Override
    public void entryAdded( AddOperationContext addContext )
    {
    }

    @Override
    public void entryDeleted( DeleteOperationContext deleteContext )
    {
    }

    @Override
    public void entryModified( ModifyOperationContext modifyContext )
    {
    }

    @Override
    public void entryRenamed( RenameOperationContext renameContext )
    {
    }

    @Override
    public void entryMoved( MoveOperationContext moveContext )
    {
    }

    @Override
    public void entryMovedAndRenamed( MoveAndRenameOperationContext moveAndRenameContext )
    {
    }

    @Override
    public boolean isSynchronous()
    {
        return false;
    }
}
