
package org.apache.directory.server.core.txn.logedit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;

public class EntryAddDelete<ID> extends AbstractDataChange<ID>
{
    /** Added or deleted entry */
    private Entry entry;
    
    /** Type of change */
    Type type;
    
    // For externalizable
    public EntryAddDelete(  )
    {
        
    }
    
    public EntryAddDelete( Entry entry, Type type )
    {
        this.entry = entry;
        this.type = type;
    }
    
    public Entry getChangedEntry()
    {
        return entry;
    }
    
    public Type getType()
    {
        return type;
    }
    
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
     entry = new DefaultEntry();
     entry.readExternal( in );
     type = Type.values()[in.readInt()];
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
       entry.writeExternal( out );
       out.writeInt( type.ordinal() );
    }
    
    public enum Type
    {
        ADD,
        DELETE
    }
    
}
