/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.core.shared.txn.logedit;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.partition.index.UUIDComparator;
import org.apache.directory.server.core.api.txn.logedit.AbstractLogEdit;
import org.apache.directory.server.core.api.txn.logedit.DataChange;
import org.apache.directory.server.core.api.txn.logedit.EntryModification;
import org.apache.directory.server.core.api.txn.logedit.IndexModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A container for index and entry changes. If any entry change is contained, then they are 
 * for a single entry. All changes contained for an entry belong to a single version and 
 * can be atomically applied to the entry in question.
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DataChangeContainer extends AbstractLogEdit
{
    /** id of the entry if the container contains a change for an entry */
    private UUID entryID;

    /** Partition stored for fast access */
    private transient Partition partition;

    /** partition this change applies to */
    private Dn partitionDn;

    /** List of data changes */
    private List<DataChange> changes = new LinkedList<DataChange>();

    /** The type of change */
    private enum ChangeType
    {
        INDEX_MODIFICATION,
        ENTRY_ADD_DELETE,
        ENTRY_CHANGE,
        ENTRY_REPLACE
    }


    //For externalizable
    public DataChangeContainer()
    {
        super( Long.MIN_VALUE );
    }


    public DataChangeContainer( Partition partition )
    {
        super( Long.MIN_VALUE );

        this.partitionDn = partition.getSuffixDn();
        this.partition = partition;
    }


    public Dn getPartitionDn()
    {
        return partitionDn;
    }


    public Partition getPartition()
    {
        return partition;
    }
    
    public void setPartition( Partition partition )
    {
    	this.partition = partition;
    }


    public UUID getEntryID()
    {
        return entryID;
    }


    public void setEntryID( UUID id )
    {
        entryID = id;
    }


    public List<DataChange> getChanges()
    {
        return changes;
    }


    public void addChange( DataChange change )
    {
        changes.add( change );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void apply( boolean recovery ) throws Exception
    {
        long changeLsn = getLogAnchor().getLogLSN();
        Entry curEntry = null;
        boolean entryExisted = false;
        Entry originalEntry = null;

        if ( entryID != null )
        {
            MasterTable master = partition.getMasterTable();
            curEntry = master.get( entryID );

            if ( curEntry != null )
            {
                originalEntry = curEntry = curEntry.clone();
                entryExisted = true;
            }
        }

        for ( DataChange change : changes )
        {
            if ( ( change instanceof EntryModification ) )
            {
                EntryModification entryModification = ( EntryModification ) change;

                curEntry = entryModification.applyModification( partition, curEntry, entryID, changeLsn, recovery );
            }
            else
            {
                IndexModification indexModification = ( IndexModification ) change;
                indexModification.applyModification( partition, recovery );
            }
        }

        if ( curEntry != null )
        {
            MasterTable master = partition.getMasterTable();
            
            if ( !curEntry.isSchemaAware() )
            {
            	curEntry.apply( partition.getSchemaManager() );
            }
            master.put( entryID, curEntry );
        }
        else
        {
            if ( entryExisted )
            {
                MasterTable master = partition.getMasterTable();
                master.remove( entryID, originalEntry );
            }
        }
    }


    /**
     * Applies the updates made by this log edit to the entry identified by the entryID 
     * and partition dn. 
     *
     * @param entryPartitionDn dn of the partition of the entry
     * @param id id of the entry
     * @param curEntry entry to be merged
     * @param needToCloneOnChange true if entry should be cloned while applying a change.
     * @return entry after it is merged with the updates in the txn.
     */
    public Entry mergeUpdates( Dn entryPartitionDn, UUID id, Entry curEntry, boolean needToCloneOnChange )
    {
        /**
        * Check if the container has changes for the entry
        * and the version says we need to apply this change
        */
        boolean applyChanges = false;

        if ( entryID != null )
        {
            /*
             * Container has changes for an entry. Check if the entry change
             * affects out entry by comparing id and partitionDn.
             */

            Comparator<UUID> idComp = UUIDComparator.INSTANCE;

            if ( entryPartitionDn.equals( partitionDn )
                && ( idComp.compare( id, entryID ) == 0 ) )
            {
                applyChanges = true;
            }

        }

        if ( applyChanges )
        {
            long changeLsn = getLogAnchor().getLogLSN();

            for ( DataChange change : changes )
            {
                if ( change instanceof EntryModification )
                {
                    EntryModification entryModification = ( EntryModification ) change;

                    if ( needToCloneOnChange )
                    {
                        if ( curEntry != null )
                        {
                            curEntry = curEntry.clone();
                        }

                        needToCloneOnChange = false;
                    }

                    curEntry = entryModification.applyModification( partition, curEntry, id, changeLsn, true );
                }
            }

        }

        return curEntry;
    }


    /**
     * Read back a DataChange container from a stream. The format is : <br/>
     * <ul>
     * <li>entryId if we have stored an Entry change, null if it's an Index change</li>
     * <li>the transaction id</li>
     * <li>the partition DN</li>
     * <li>the changes if any</li>
     * </ul>
     */
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {

        boolean hasID = in.readBoolean();

        if ( hasID )
        {
            entryID = UUID.fromString( in.readUTF() );
        }
        else
        {
            entryID = null;
        }

        txnID = in.readLong();

        partitionDn = new Dn();
        partitionDn.readExternal( in );

        DataChange change = null;
        int numChanges = in.readInt();

        for ( int idx = 0; idx < numChanges; idx++ )
        {
            ChangeType changeType = ChangeType.values()[in.read()];

            switch ( changeType )
            {
                case ENTRY_ADD_DELETE:
                    change = new EntryAddDelete();
                    break;

                case ENTRY_CHANGE:
                    change = new EntryChange();
                    break;

                case ENTRY_REPLACE:
                    change = new EntryReplace();
                    break;

                case INDEX_MODIFICATION:
                    change = new IndexChange();
                    break;
            }

            change.readExternal( in );
            changes.add( change );
        }
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        DataChange change;

        out.write(EditType.DATA_CHANGE.ordinal());
        
        if ( entryID != null )
        {
            out.writeBoolean( true );
            out.writeUTF( entryID.toString() );
        }
        else
        {
            out.writeBoolean( false );
        }

        out.writeLong( txnID );

        partitionDn.writeExternal( out );

        out.writeInt( changes.size() );

        Iterator<DataChange> it = changes.iterator();

        while ( it.hasNext() )
        {
            change = it.next();

            if ( change instanceof IndexModification )
            {
                out.write( ChangeType.INDEX_MODIFICATION.ordinal() );
            }
            else if ( change instanceof EntryAddDelete )
            {
                out.write( ChangeType.ENTRY_ADD_DELETE.ordinal() );
            }
            else if ( change instanceof EntryChange )
            {
                out.write( ChangeType.ENTRY_CHANGE.ordinal() );
            }
            else if ( change instanceof EntryReplace )
            {
                out.write( ChangeType.ENTRY_REPLACE.ordinal() );
            }

            change.writeExternal( out );
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "DataChangeContainer(" );

        // The partition
        sb.append( partitionDn ).append( ", " );

        // The entry UUID
        sb.append( entryID ).append( ", " );

        // The txn ID
        sb.append( txnID ).append( ")\n{\n" );

        for ( DataChange change : changes )
        {
            sb.append( "    " );
            sb.append( change );
            sb.append( '\n' );
        }

        sb.append( "}" );

        return sb.toString();
    }
}
