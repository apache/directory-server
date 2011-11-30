
package org.apache.directory.server.core.partition.ldif;

import java.util.Comparator;
import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.UUIDComparator;
import org.apache.directory.server.xdbm.impl.avl.AvlMasterTable;
import org.apache.directory.shared.ldap.model.entry.Entry;

public class LdifMasterTable extends AvlMasterTable
{
    /** partition of the table */
    private LdifPartition partition;

    
    public LdifMasterTable( LdifPartition partition )
    {
        super( partition.getId(), UUIDComparator.INSTANCE, null, false );
        this.partition = partition;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void put( UUID key, Entry entry ) throws Exception
    {
        if ( key == null )
        {
            return;
        }
        
        Entry existingEntry = get( key );
        
        if ( existingEntry != null )
        {
            // Remove existing entry
            partition.deleteEntry( existingEntry);
        }
        
        // Write the new entry
        partition.writeEntry( entry );
        
        super.put( key, entry );
    }

    
    /**
     * {@inheritDoc}
     */
    public void remove( UUID key ) throws Exception
    {
        if ( key == null )
        {
            return;
        }
       
        Entry existingEntry = get( key );
        
        if ( existingEntry != null )
        {
            // Remove existing entry
            partition.deleteEntry( existingEntry);
        }
        
        super.remove( key );
    }

    
    /**
     * {@inheritDoc}
     */
    public void remove( UUID key, Entry entry ) throws Exception
    {
        // Remove existing entry
        partition.deleteEntry( entry);
        
        super.remove( key, entry );
    }
}
