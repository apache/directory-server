
package org.apache.directory.server.core.txn.logedit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;

public class EntryChange<ID> extends AbstractDataChange<ID>
{
    /** redo change */
    private Modification redoChange;
    
    /** undo change */
    private Modification undoChange;
    
    //For externalizable
    public EntryChange()
    {
        
    }
    
    public EntryChange( Modification redo, Modification undo )
    {
        redoChange = redo;
        undoChange = undo;
    }
    
    public Modification getRedoChange()
    {
        return redoChange;
    }
    
    public Modification getUndoChange()
    {
        return undoChange;
    }
    
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        redoChange = new DefaultModification();
        
        redoChange.readExternal( in );
        
        undoChange = new DefaultModification();
        
        undoChange.readExternal( in );
        
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        redoChange.writeExternal( out );
        undoChange.writeExternal( out );
    }
    
}
