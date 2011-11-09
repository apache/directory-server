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

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Comparator;

import org.apache.directory.server.core.api.txn.logedit.LogEdit;
import org.apache.directory.server.core.shared.txn.logedit.IndexChange;
import org.apache.directory.server.core.api.txn.logedit.DataChange;
import org.apache.directory.server.core.shared.txn.logedit.EntryAddDelete;
import org.apache.directory.server.core.shared.txn.logedit.EntryChange;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;

import org.apache.directory.server.core.api.log.UserLogRecord;

import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.ReverseIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.UUIDComparator;

import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.entry.AttributeUtils;
import org.apache.directory.shared.ldap.model.entry.Entry;

import org.apache.directory.shared.ldap.model.exception.LdapException;

import org.apache.directory.shared.ldap.model.message.SearchScope;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/** Package protected */ class ReadWriteTxn extends AbstractTransaction
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
    private Map<Dn, Map<String, TreeSet< IndexEntry<Object> >>> forwardIndexAdds  = 
        new HashMap<Dn,  Map<String, TreeSet< IndexEntry<Object> >>>();
    
    /** A summary of reverse index adds */
    private Map<Dn, Map<String, TreeSet< IndexEntry<Object> >>> reverseIndexAdds  = 
        new HashMap<Dn,  Map<String, TreeSet< IndexEntry<Object> >>>();
    
    /** A summary of index deletes */
    private Map<Dn, Map<String, TreeSet< IndexEntry<Object> >>> indexDeletes  = 
        new HashMap<Dn,  Map<String, TreeSet< IndexEntry<Object> >>>();
    
    
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
            Iterator<DataChange> it = dataChanges.iterator();
            Dn partitionDn = dEdit.getPartitionDn();

            DataChange nextChange;
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

            while ( it.hasNext() )
            {
                nextChange = it.next();

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
                        reverseIndices.put( indexChange.getOID(), forwardAdds );
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
        LogEdit edit;
        DataChangeContainer container;

        Iterator<LogEdit> it = logEdits.iterator();

        while ( it.hasNext() )
        {
            edit = it.next();

            if ( edit instanceof DataChangeContainer )
            {
                container = ( DataChangeContainer ) edit;

                /**
                 * Check if the container has changes for the entry
                 * and the version says we need to apply this change
                 */
                //TODO check version and id here. 
                UUID entryId = container.getEntryID();
                boolean applyChanges = false;

                if ( entryId != null )
                {
                    /*
                     * Container has changes for entry. Check if the entry change
                     * affects out entry by comparing id and partitionDn.
                     */

                    Comparator<UUID> idComp = UUIDComparator.INSTANCE;

                    if ( partitionDn.equals( container.getPartitionDn() )
                        && ( idComp.compare( entryID, container.getEntryID() ) == 0 ) )
                    {
                        applyChanges = true;
                    }

                }

                if ( applyChanges )
                {
                    List<DataChange> dataChanges = container.getChanges();
                    Iterator<DataChange> dit = dataChanges.iterator();
                    DataChange nextChange;

                    while ( dit.hasNext() )
                    {
                        nextChange = dit.next();

                        if ( ( nextChange instanceof EntryChange ) && ( curEntry != null ) )
                        {
                            EntryChange entryChange = ( EntryChange ) nextChange;

                            if ( needToCloneOnChange )
                            {
                                curEntry = curEntry.clone();
                                needToCloneOnChange = false;
                            }

                            try
                            {
                                AttributeUtils.applyModification( curEntry, entryChange.getRedoChange() );
                            }
                            catch ( LdapException e )
                            {
                                //TODO decide whether to throw IOException or an internal exception here
                            }
                        }
                        else if ( nextChange instanceof EntryAddDelete )
                        {
                            EntryAddDelete addDelete = ( EntryAddDelete ) nextChange;
                            needToCloneOnChange = false;

                            if ( addDelete.getType() == EntryAddDelete.Type.ADD )
                            {
                                curEntry = addDelete.getChangedEntry();
                            }
                            else
                            {
                                curEntry = null;
                            }
                        }
                    }

                }
            }
        }

        return curEntry;
    }
    

    /**
     * Returns true if this txn has deletes for the index identified by partitionDn + attributeOid
     *
     * @param partitionDn dn of the partition
     * @param attributeOid oid of the indexed attribute
     * @return
     */
    public boolean hasDeletesFor( Dn partitionDn, String attributeOid )
    {
        Map<String, TreeSet<IndexEntry<Object>>> deletedIndices =
            indexDeletes.get( partitionDn );

        if ( deletedIndices != null )
        {
            return ( deletedIndices.get( attributeOid ) != null );
        }

        return false;
    }


    /**
     * Returns a cursor over the changes made by this txn on the index identified by partitionDn+attributeOid. 
     *
     * @param partitionDn dn of the partition
     * @param attributeOid oid of the indexed attribute
     * @param forwardIndex true if forward index and reverse if reverse index
     * @param onlyValueKey set if the cursor should be locked down by a key ( should be non null only for forward indices )
     * @param onlyIDKey  set if the cursor should be locked down by a key ( should be non null only for reverse indices )
     * @param comparator comparator that will be used to order index entries.
     * @return
     */
    public IndexCursor<Object> getCursorFor( Dn partitionDn, String attributeOid, boolean forwardIndex,
        Object onlyValueKey, UUID onlyIDKey, IndexComparator<Object> comparator )
    {
        TxnIndexCursor txnIndexCursor = null;

        Map<String, TreeSet<IndexEntry<Object>>> forwardIndices =
            forwardIndexAdds.get( partitionDn );

        if ( forwardIndices != null )
        {
            TreeSet<IndexEntry<Object>> sortedSet = forwardIndices.get( attributeOid );

            if ( sortedSet != null )
            {
                txnIndexCursor = new TxnIndexCursor( sortedSet, forwardIndex, onlyValueKey, onlyIDKey, comparator );
            }
        }

        return txnIndexCursor;
    }


    /**
     * Returns true if the given index entry is deleted by this txn. partitionDn + attributeOid 
     * identifies the index. 
     *
     * @param partitionDn dn of the partition index belongs to 
     * @param attributeOid oid of the indexed attribute.
     * @param indexEntry value to be checked
     * @return true if the given value is deleted.
     */
    public boolean isIndexEntryDeleted( Dn partitionDn, String attributeOid, IndexEntry<Object> indexEntry )
    {
        Map<String, TreeSet<IndexEntry<Object>>> deletedIndices =
            indexDeletes.get( partitionDn );

        if ( deletedIndices == null )
        {
            return false;
        }

        TreeSet<IndexEntry<Object>> deletedEntries = deletedIndices.get( attributeOid );

        if ( deletedEntries == null )
        {
            return false;
        }

        boolean result = deletedEntries.contains( indexEntry );

        return result;
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
        Iterator<DnSet> writeIt = txnWriteDns.iterator();
        Iterator<DnSet> readIt = readDns.iterator();

        DnSet readDnSet;
        SearchScope readScope;
        DnSet writeDnSet;
        SearchScope writeScope;

        while ( readIt.hasNext() )
        {
            readDnSet = readIt.next();
            readScope = readDnSet.getScope();

            while ( writeIt.hasNext() )
            {
                writeDnSet = writeIt.next();
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
}
