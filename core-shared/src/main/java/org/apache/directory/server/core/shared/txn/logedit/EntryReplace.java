
package org.apache.directory.server.core.shared.txn.logedit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.txn.logedit.AbstractDataChange;
import org.apache.directory.server.core.api.txn.logedit.EntryModification;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;

public class EntryReplace extends AbstractDataChange implements EntryModification
{
    /** New Entry */
    private Entry newEntry;
    
    /** Old enty */
    private Entry oldEntry;
    
    // For externalizable
    public EntryReplace()
    {
        
    }
    
    public EntryReplace( Entry newEntry, Entry oldEntry )
    {
        this.newEntry = newEntry;
        this.oldEntry = oldEntry;
    }
    
    
    public Entry getNewEntry()
    {
        return newEntry;
    }
    
    
    public Entry getOldEntry()
    {
        return oldEntry;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Entry applyModification( Partition partition, Entry curEntry, UUID entryId, long changeLsn, boolean recovery )
    {
        // Currently this log edit is only for existing exntries
        if ( curEntry == null )
        {
            if ( recovery == false )
            {
                throw new IllegalStateException(" Trying to replace a non existing entry:" + entryId + " entry:" + newEntry);
            }
            else
            {
                return null;
            }
        }
        
        // TODO check the version of the entry and see which entry is more recent
        return newEntry;
    }
    
    
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
     newEntry = new DefaultEntry();
     newEntry.readExternal( in );
     
     oldEntry = new DefaultEntry();
     oldEntry.readExternal( in );
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
       newEntry.writeExternal( out );
       oldEntry.writeExternal( out );
    }
}
