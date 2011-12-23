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
package org.apache.directory.server.core.shared.txn;


import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.server.core.api.log.UserLogRecord;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.ReverseIndexEntry;
import org.apache.directory.server.core.api.partition.index.UUIDComparator;
import org.apache.directory.server.core.api.txn.logedit.DataChange;
import org.apache.directory.server.core.api.txn.logedit.LogEdit;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;
import org.apache.directory.server.core.shared.txn.logedit.IndexChange;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/** Package protected */
class ReadWriteTxn extends AbstractTransaction
{
    /** list of log edits by the txn */
    private List<LogEdit> logEdits = new LinkedList<LogEdit>();

    /*
     * Number of txns that depend on this txn and previous committed
     * txns. This number is bumped up only after the txn is committed.
     * A txn can be flushed to partitions only after the txn itself is
     * committed and ref count becomes zero for all the previously
     * committed txns.
     */
    private AtomicInteger txnRefCount = new AtomicInteger( 0 );

    /** User record used to communicate data with log manager */
    private UserLogRecord logRecord = new UserLogRecord();

    /** A summary of forward index adds */
    private Map<Dn, Map<String, TreeSet<IndexEntry<Object>>>> forwardIndexAdds =
        new HashMap<Dn, Map<String, TreeSet<IndexEntry<Object>>>>();

    /** A summary of reverse index adds */
    private Map<Dn, Map<String, TreeSet<IndexEntry<Object>>>> reverseIndexAdds =
        new HashMap<Dn, Map<String, TreeSet<IndexEntry<Object>>>>();

    /** A summary of index deletes */
    private Map<Dn, Map<String, TreeSet<IndexEntry<Object>>>> indexDeletes =
        new HashMap<Dn, Map<String, TreeSet<IndexEntry<Object>>>>();

    /** List of Dn sets this txn depends */
    private List<DnSet> readDns = new LinkedList<DnSet>();

    /** List of Dn sets affected by the write operations of this txn */
    private List<DnSet> writeDns = new LinkedList<DnSet>();


    public AtomicInteger getRefCount()
    {
        return txnRefCount;
    }


    public UserLogRecord getUserLogRecord()
    {
        return logRecord;
    }


    public List<LogEdit> getEdits()
    {
        return logEdits;
    }


    /**
     * Logs the given log edit for this txn. If the edit contains index changes, it updates the summary
     * of changes for the changed index so that it is easier to provide a cursor view over the set of 
     * index changes.
     *
     * @param edit txn log edit to be logged
     */
    @SuppressWarnings("unchecked")
    public void addLogEdit( LogEdit edit )
    {
        logEdits.add( edit );

        /*
         * Update the in memory summary of the index changes
         */
        if ( edit instanceof DataChangeContainer )
        {
            DataChangeContainer dEdit = ( DataChangeContainer ) edit;
            List<DataChange> dataChanges = dEdit.getChanges();
            Dn partitionDn = dEdit.getPartitionDn();

            IndexChange indexChange;
            IndexChange.Type indexChangeType;
            ForwardIndexEntry<Object> indexEntry;
            ReverseIndexEntry<Object> reverseIndexEntry;

            Map<String, TreeSet<IndexEntry<Object>>> forwardIndices =
                forwardIndexAdds.get( partitionDn );

            Map<String, TreeSet<IndexEntry<Object>>> reverseIndices =
                reverseIndexAdds.get( partitionDn );

            if ( forwardIndices == null )
            {
                forwardIndices = new HashMap<String, TreeSet<IndexEntry<Object>>>();

                // Reverse index changes should be null too
                if ( reverseIndices != null )
                {
                    throw new IllegalStateException(
                        "Reverse Index changes for partition are not null while forward index changes are null"
                            + partitionDn );
                }

                reverseIndices = new HashMap<String, TreeSet<IndexEntry<Object>>>();

                forwardIndexAdds.put( partitionDn, forwardIndices );
                reverseIndexAdds.put( partitionDn, reverseIndices );
            }

            Map<String, TreeSet<IndexEntry<Object>>> deletedIndices =
                indexDeletes.get( partitionDn );

            if ( deletedIndices == null )
            {
                deletedIndices = new HashMap<String, TreeSet<IndexEntry<Object>>>();
                indexDeletes.put( partitionDn, deletedIndices );
            }

            for ( DataChange nextChange : dataChanges )
            {
                if ( nextChange instanceof IndexChange )
                {
                    indexChange = ( IndexChange ) nextChange;
                    indexChangeType = indexChange.getType();
                    Index<Object> index = ( Index<Object> ) indexChange.getIndex();

                    TreeSet<IndexEntry<Object>> forwardAdds =
                        forwardIndices.get( indexChange.getOID() );

                    TreeSet<IndexEntry<Object>> reverseAdds =
                        reverseIndices.get( indexChange.getOID() );

                    if ( forwardAdds == null )
                    {
                        forwardAdds =
                            new TreeSet<IndexEntry<Object>>( index.getForwardIndexEntryComparator() );

                        // Reverse index changes should be null too
                        if ( reverseAdds != null )
                        {
                            throw new IllegalStateException(
                                "Reverse Index changes for partition are not null while forward index changes are null"
                                    + partitionDn + indexChange.getOID() );
                        }

                        reverseAdds =
                            new TreeSet<IndexEntry<Object>>( index.getReverseIndexEntryComparator() );

                        forwardIndices.put( indexChange.getOID(), forwardAdds );
                        reverseIndices.put( indexChange.getOID(), reverseAdds );
                    }

                    TreeSet<IndexEntry<Object>> deletes = deletedIndices.get( indexChange.getOID() );

                    if ( deletes == null )
                    {
                        deletes = new TreeSet<IndexEntry<Object>>( index.getForwardIndexEntryComparator() );
                        deletedIndices.put( indexChange.getOID(), deletes );
                    }

                    indexEntry = new ForwardIndexEntry<Object>();
                    indexEntry.setValue( indexChange.getKey() );
                    indexEntry.setId( indexChange.getID() );

                    reverseIndexEntry = new ReverseIndexEntry<Object>();
                    reverseIndexEntry.setValue( indexChange.getKey() );
                    reverseIndexEntry.setId( indexChange.getID() );

                    if ( indexChangeType == IndexChange.Type.ADD )
                    {
                        deletes.remove( indexEntry );
                        forwardAdds.add( indexEntry );
                        reverseAdds.add( reverseIndexEntry );
                    }
                    else
                    {
                        deletes.add( indexEntry );
                        forwardAdds.remove( indexEntry );
                        reverseAdds.remove( reverseIndexEntry );
                    }
                }
            }
        }
    }


    /**
     * Applies the updates made by this txn to the entry identified by the entryID and partition dn. 
     *
     * @param partitionDn dn of the partition of the entry
     * @param entryID id of the entry
     * @param curEntry entry to be merged
     * @param cloneOnChange true if entry should be cloned while applying a change.
     * @return entry after it is merged with the updates in the txn.
     */
    public Entry applyUpdatesToEntry( Dn partitionDn, UUID entryID, Entry curEntry, boolean cloneOnChange )
    {
        boolean needToCloneOnChange = cloneOnChange;
        DataChangeContainer container;

        for ( LogEdit edit : logEdits )
        {
            if ( edit instanceof DataChangeContainer )
            {
                container = ( DataChangeContainer ) edit;

                Entry nextEntry = container.mergeUpdates( partitionDn, entryID, curEntry, cloneOnChange );

                if ( nextEntry != curEntry )
                {
                    cloneOnChange = false;
                    curEntry = nextEntry;
                }
            }
        }

        return curEntry;
    }


    /**
     * Checks all the updates done on the given index for the given key and returns 
     * the latest version of the coressponding id
     *
     * @param partitionDn dn of the partition the entry lives in
     * @param attributeOid oid of the indexed attribute
     * @param indexEntry index entry to do the lookup on 
     * @param valueComp value comparator
     * @return current version of the index entry
     */
    public void updateForwardLookup( Dn partitionDn, String attributeOid, IndexEntry<Object> indexEntry,
        Comparator<Object> valueComp )
    {
        UUID id = indexEntry.getId();
        Object key = indexEntry.getValue();
        NavigableSet<IndexEntry<Object>> changes = getForwardIndexChanges( partitionDn, attributeOid );

        if ( changes == null )
        {
            // No add. If we have a value, check if it is deleted.
            if ( id != null )
            {
                // Check if index entry is deleted
                NavigableSet<IndexEntry<Object>> txnDeletes = getDeletesFor( partitionDn, attributeOid );

                if ( txnDeletes != null && txnDeletes.contains( indexEntry ) )
                {
                    // Index entry is deleted
                    id = null;
                }
                else
                {
                    // No update
                }
            }
        }
        else
        {
            boolean added = false;
            indexEntry.setId( null );
            changes = changes.tailSet( indexEntry, false );

            for ( IndexEntry<Object> lookedUpEntry : changes )
            {
                if ( valueComp.compare( key, lookedUpEntry.getValue() ) == 0 )
                {
                    id = lookedUpEntry.getId();
                    added = true;
                }
            }

            if ( added == false && id != null )
            {
                // Check if index entry is deleted
                indexEntry.setId( id ); // reset the id
                NavigableSet<IndexEntry<Object>> txnDeletes = getDeletesFor( partitionDn, attributeOid );

                if ( txnDeletes != null && txnDeletes.contains( indexEntry ) )
                {
                    // Index entry is deleted
                    id = null;
                }
                else
                {
                    // No update
                }
            }
        }

        // Set the current version of the id
        indexEntry.setId( id );
    }


    /**
     * Checks all the updates done on the given index for the given id and returns 
     * the latest version of the corressponding value
     *
     * @param partitionDn dn of the partition the entry lives in
     * @param attributeOid oid of the indexed attribute
     * @param id key to do the lookup on 
     * @return value corresponding to the id
     */
    public void updateReverseLookup( Dn partitionDn, String attributeOid, IndexEntry<Object> indexEntry )
    {
        UUID id = indexEntry.getId();
        Object key = indexEntry.getValue();
        NavigableSet<IndexEntry<Object>> changes = getReverseIndexChanges( partitionDn, attributeOid );

        if ( changes == null )
        {
            // No add. If we have a value, check if it is deleted.
            if ( key != null )
            {
                // Check if index entry is deleted
                NavigableSet<IndexEntry<Object>> txnDeletes = getDeletesFor( partitionDn, attributeOid );

                if ( txnDeletes != null && txnDeletes.contains( indexEntry ) )
                {
                    // Index entry is deleted
                    key = null;
                }
                else
                {
                    // No update
                }
            }
        }
        else
        {
            boolean added = false;
            indexEntry.setValue( null );
            changes = changes.tailSet( indexEntry, false );
            Iterator<IndexEntry<Object>> it = changes.iterator();

            if ( it.hasNext() )
            {
                IndexEntry<Object> lookedUpEntry = it.next();

                if ( UUIDComparator.INSTANCE.compare( id, lookedUpEntry.getId() ) == 0 )
                {
                    key = lookedUpEntry.getValue();
                }
            }

            if ( added == false && key != null )
            {
                // Check if index entry is deleted
                indexEntry.setValue( key ); // reset the id
                NavigableSet<IndexEntry<Object>> txnDeletes = getDeletesFor( partitionDn, attributeOid );

                if ( txnDeletes != null && txnDeletes.contains( indexEntry ) )
                {
                    // Index entry is deleted
                    key = null;
                }
                else
                {
                    // No update
                }
            }
        }

        // Set the current version of the value
        indexEntry.setValue( key );
    }


    /**
     * Checks updates on the given index entry and returns whether the it exists or not
     *
     * @param partitionDn dn of the partition the entry lives in
     * @param attributeOid oid of the indexed attribute
     * @param indexEntry entry to do the check for 
     * @param currentlyExists true if the index entry currently exists
     * @param forward true if lookup is on forward index, false otherwise
     * @return updated version of the existence status
     */
    public boolean updateExistence( Dn partitionDn, String attributeOid, IndexEntry<?> indexEntry,
        boolean currentlyExists, boolean forward )
    {
        NavigableSet<IndexEntry<Object>> changes;

        if ( forward )
        {
            changes = getForwardIndexChanges( partitionDn, attributeOid );
        }
        else
        {
            changes = getReverseIndexChanges( partitionDn, attributeOid );
        }

        if ( changes == null )
        {
            // No adds, check if index entry is deleted
            NavigableSet<IndexEntry<Object>> txnDeletes = getDeletesFor( partitionDn, attributeOid );

            if ( txnDeletes != null && txnDeletes.contains( indexEntry ) )
            {
                // Index entry is delete
                return false;
            }
            else
            {
                // No update on existence status
                return currentlyExists;
            }
        }
        else
        {
            if ( changes.contains( indexEntry ) )
            {
                return true;
            }
            else
            {
                // No add, check if index entry is deleted
                NavigableSet<IndexEntry<Object>> txnDeletes = getDeletesFor( partitionDn, attributeOid );

                if ( txnDeletes != null && txnDeletes.contains( indexEntry ) )
                {
                    // Index entry is delete
                    return false;
                }
                else
                {
                    // No update on existence status
                    return currentlyExists;
                }
            }
        }
    }


    /**
     * Returns deletes his txn has for the index identified by partitionDn + attributeOid
     *
     * @param partitionDn dn of the partition
     * @param attributeOid oid of the indexed attribute
     * @return deletes for the given index
     */
    public NavigableSet<IndexEntry<Object>> getDeletesFor( Dn partitionDn, String attributeOid )
    {
        Map<String, TreeSet<IndexEntry<Object>>> deletedIndices =
            indexDeletes.get( partitionDn );

        if ( deletedIndices != null )
        {
            return deletedIndices.get( attributeOid );
        }

        return null;
    }


    /**
     * Returns add this txn has for the forward index identified by partitionDn + attributeOid
     *
     * @param partitionDn dn of the partition
     * @param attributeOid oid of the indexed attribute
     * @return adds for the given forward index 
     */
    public NavigableSet<IndexEntry<Object>> getForwardIndexChanges( Dn partitionDn, String attributeOid )
    {
        Map<String, TreeSet<IndexEntry<Object>>> forwardIndices =
            forwardIndexAdds.get( partitionDn );

        if ( forwardIndices != null )
        {
            return forwardIndices.get( attributeOid );
        }

        return null;
    }


    /**
     * Returns add this txn has for the reverse index identified by partitionDn + attributeOid
     *
     * @param partitionDn dn of the partition
     * @param attributeOid oid of the indexed attribute
     * @return adds for the given reverse index 
     */
    public NavigableSet<IndexEntry<Object>> getReverseIndexChanges( Dn partitionDn, String attributeOid )
    {
        Map<String, TreeSet<IndexEntry<Object>>> reverseIndices =
            reverseIndexAdds.get( partitionDn );

        if ( reverseIndices != null )
        {
            return reverseIndices.get( attributeOid );
        }

        return null;
    }


    /**
     * Adds the given Dn to the read set of the current txn
     *
     * @param readSet dn to add
     */
    public void addRead( DnSet readSet )
    {
        readDns.add( readSet );
    }


    /**
     * Adds the given Dn to the write and read set of the current txn.
     *
     * @param writeSet dn to add
     */
    public void addWrite( DnSet writeSet )
    {
        writeDns.add( writeSet );

        // Changing a dn means also read dependency
        readDns.add( writeSet );
    }


    public List<DnSet> getWriteSet()
    {
        return writeDns;
    }


    /**
     * Checks if this txn's read set conflicts with the write set
     * of the given txn.
     *
     * @param txn txn to verify this txn against
     * @return true if a conflict is detected.
     */
    public boolean hasConflict( ReadWriteTxn txn )
    {
        boolean result = false;

        List<DnSet> txnWriteDns = txn.getWriteSet();

        // If no write for any of the txns, then return

        if ( ( this.writeDns.size() == 0 ) || ( txnWriteDns.size() == 0 ) )
        {
            return false;
        }

        SearchScope readScope;
        SearchScope writeScope;

        for ( DnSet readDnSet : readDns )
        {
            readScope = readDnSet.getScope();

            for ( DnSet writeDnSet : txnWriteDns )
            {
                writeScope = writeDnSet.getScope();

                if ( readScope.equals( SearchScope.OBJECT ) )
                {
                    if ( writeScope.equals( SearchScope.OBJECT ) )
                    {
                        if ( readDnSet.getBaseDn().equals( writeDnSet.getBaseDn() ) )
                        {
                            result = true;
                            break;
                        }
                    }
                    else
                    //one level or subtree scope for the write.
                    {
                        // Even if one level scope, conservatively check the whole subtree
                        if ( readDnSet.getBaseDn().isDescendantOf( writeDnSet.getBaseDn() ) )
                        {
                            result = true;
                            break;
                        }
                    }
                }
                else
                //one level or subtree scope for the read.
                {
                    if ( writeScope.equals( SearchScope.OBJECT ) )
                    {
                        if ( readDnSet.getBaseDn().isAncestorOf( writeDnSet.getBaseDn() ) )
                        {
                            result = true;
                            break;
                        }
                    }
                    else
                    //one level or subtree scope for the write.
                    {
                        // Even if one level scope, conservatively check if any basedn is descendent of the other
                        if ( ( readDnSet.getBaseDn().isDescendantOf( writeDnSet.getBaseDn() ) ) ||
                            ( readDnSet.getBaseDn().isAncestorOf( writeDnSet.getBaseDn() ) ) )
                        {
                            result = true;
                            break;
                        }
                    }
                }
            } // end of inner while loop
        } // end of outer while loop

        return result;
    }


    public void flushLogEdits( Set<Partition> affectedPartitions ) throws Exception
    {
        for ( LogEdit edit : logEdits )
        {
            edit.apply( false );

            if ( edit instanceof DataChangeContainer )
            {
                affectedPartitions.add( ( ( DataChangeContainer ) edit ).getPartition() );
            }
        }
    }
}
