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
package org.apache.directory.server.core.txn;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Comparator;

import org.apache.directory.server.core.txn.logedit.LogEdit;
import org.apache.directory.server.core.txn.logedit.IndexChange;
import org.apache.directory.server.core.txn.logedit.DataChange;
import org.apache.directory.server.core.txn.logedit.EntryAddDelete;
import org.apache.directory.server.core.txn.logedit.EntryChange;
import org.apache.directory.server.core.txn.logedit.DataChangeContainer;

import org.apache.directory.server.core.log.UserLogRecord;

import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;

import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.entry.AttributeUtils;
import org.apache.directory.shared.ldap.model.entry.Entry;

import org.apache.directory.shared.ldap.model.exception.LdapException;

import org.apache.directory.shared.ldap.model.constants.SchemaConstants;

import org.apache.directory.shared.ldap.model.message.SearchScope;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/** Package protected */ class ReadWriteTxn<ID> extends AbstractTransaction<ID>
{  
    /** list of log edits by the txn */
    private List<LogEdit<ID>> logEdits = new LinkedList<LogEdit<ID>>();
    
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
    private Map<Dn, Map<String, TreeSet< IndexEntry<Object,ID> >>> forwardIndexAdds  = 
        new HashMap<Dn,  Map<String, TreeSet< IndexEntry<Object,ID> >>>();
    
    /** A summary of reverse index adds */
    private Map<Dn, Map<String, TreeSet< IndexEntry<Object,ID> >>> reverseIndexAdds  = 
        new HashMap<Dn,  Map<String, TreeSet< IndexEntry<Object,ID> >>>();
    
    /** A summary of index deletes */
    private Map<Dn, Map<String, TreeSet< IndexEntry<Object,ID> >>> indexDeletes  = 
        new HashMap<Dn,  Map<String, TreeSet< IndexEntry<Object,ID> >>>();
    
    
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
    
    
    public List<LogEdit<ID>> getEdits()
    {
        return logEdits;
    }
    
    
    @SuppressWarnings("unchecked")
    public void addLogEdit( LogEdit<ID> edit )
    {
        logEdits.add( edit );
        
        /*
         * Update the in memory summary of the index changes
         */
        if ( edit instanceof DataChangeContainer )
        {
            DataChangeContainer<ID> dEdit = (DataChangeContainer<ID>)edit; 
            List<DataChange<ID>> dataChanges =  dEdit.getChanges();
            Iterator<DataChange<ID>> it = dataChanges.iterator();
            Dn partitionDn = dEdit.getPartitionDn();
            
            DataChange<ID> nextChange;
            IndexChange<ID> indexChange;
            IndexChange.Type indexChangeType;
            ForwardIndexEntry<Object,ID> indexEntry;
            
            Map<String, TreeSet<IndexEntry<Object,ID>>> forwardIndices = 
                forwardIndexAdds.get( partitionDn );
            
            Map<String, TreeSet<IndexEntry<Object,ID>>> reverseIndices = 
                reverseIndexAdds.get( partitionDn );
            
            if ( forwardIndices == null )
            {
                forwardIndices = new HashMap<String, TreeSet<IndexEntry<Object,ID>>>();
                
                // Reverse index changes should be null too
                reverseIndices = new HashMap<String, TreeSet<IndexEntry<Object,ID>>>();
                
                forwardIndexAdds.put( partitionDn, forwardIndices );
                reverseIndexAdds.put( partitionDn, reverseIndices );
            }
            
            Map<String, TreeSet< IndexEntry<Object,ID>>> deletedIndices = 
                    indexDeletes.get( partitionDn ); 
            
            if ( deletedIndices == null )
            {
                deletedIndices = new HashMap<String, TreeSet< IndexEntry<Object,ID>>>();
                indexDeletes.put( partitionDn, deletedIndices );
            }
            
            while ( it.hasNext() )
            {
                nextChange = it.next();
                
                if ( nextChange instanceof IndexChange )
                {
                    indexChange = (IndexChange<ID>) nextChange;
                    indexChangeType = indexChange.getType();
                    Index<Object,?,ID> index = (Index<Object,?,ID>)indexChange.getIndex();
                    
                    TreeSet<IndexEntry<Object,ID>> forwardAdds = 
                        forwardIndices.get( indexChange.getOID() );
                    
                    TreeSet<IndexEntry<Object,ID>> reverseAdds = 
                        reverseIndices.get( indexChange.getOID() );
                    
                    if ( forwardAdds == null )
                    {
                        forwardAdds = 
                            new TreeSet<IndexEntry<Object, ID>>( index.getForwardIndexEntryComparator() );
                        reverseAdds = 
                            new TreeSet<IndexEntry<Object, ID>>( index.getReverseIndexEntryComparator() );
                        
                        forwardIndices.put( indexChange.getOID(), forwardAdds );
                        reverseIndices.put( indexChange.getOID(), forwardAdds );
                    }
                    
                    TreeSet<IndexEntry<Object,ID>> deletes = deletedIndices.get( indexChange.getOID() );
                    
                    if ( deletes == null )
                    {
                        deletes = new TreeSet<IndexEntry<Object,ID>>( index.getForwardIndexEntryComparator() );
                        deletedIndices.put( indexChange.getOID(), deletes );
                    }
                    
                    indexEntry = new ForwardIndexEntry<Object,ID>();
                    indexEntry.setValue( indexChange.getKey() );
                    indexEntry.setId( indexChange.getID() );
                    
                    if ( indexChangeType == IndexChange.Type.ADD )
                    {
                        deletes.remove( indexEntry );
                        forwardAdds.add( indexEntry );
                        reverseAdds.add( indexEntry );
                    }
                    else
                    {
                        deletes.add( indexEntry );
                        forwardAdds.remove( indexEntry );
                        reverseAdds.remove( indexEntry );
                    }
                }
            }
        } 
    }
    
    
    public Entry applyUpdatesToEntry( Dn partitionDn, ID entryID, Entry curEntry, boolean cloneOnChange )
    {
        boolean needToCloneOnChange = cloneOnChange;
        LogEdit<ID> edit;
        DataChangeContainer<ID> container;
        
        Iterator<LogEdit<ID>> it = logEdits.iterator();
        
        while ( it.hasNext() )
        {
            edit = it.next();
            
            if ( edit instanceof DataChangeContainer )
            {
                container = (DataChangeContainer<ID>)edit;
                
                /**
                 * Check if the container has changes for the entry
                 * and the version says we need to apply this change
                 */
                //TODO check version and id here. If uuid is not available,
                // then match partitionDn as well.
                String uuid = container.getUUID();
                boolean applyChanges = false; 
                
                if ( uuid != null )
                {
                    /*
                     * Container has changes for entry. Check if the entry change
                     * affects out entry by comparing uuid if entry is available.
                     * Otherwise compare partition dn and Id.
                     */
                    
                    if ( curEntry!= null )
                    {
                        String curUuid = null;  
                        
                        try
                        {
                            curUuid = curEntry.get( SchemaConstants.ENTRY_UUID_AT ).getString();
                            if ( curUuid.equals( uuid ) )
                            {
                                //TODO check the version here to see if the change should be applied
                            }
                        }
                        catch( LdapException e )
                        {
                            //TODO decide whether to throw IOException or an internal exception here
                        }
                    }
                    else
                    {
                        Comparator<ID> idComp = TxnManagerFactory.<ID>txnManagerInstance().getIDComparator();
                        
                        if ( partitionDn.equals( container.getPartitionDn() ) &&  ( idComp.compare( entryID, container.getEntryID() ) == 0 ))
                        {
                            applyChanges = true;
                        }
                    }
                }
                
                if ( applyChanges )
                {
                    List<DataChange<ID>> dataChanges =  container.getChanges();
                    Iterator<DataChange<ID>> dit = dataChanges.iterator();
                    DataChange<ID> nextChange;
                    
                    while ( dit.hasNext() )
                    {
                        nextChange = dit.next();
                        
                        if ( ( nextChange instanceof EntryChange ) && ( curEntry != null ) )
                        {
                            EntryChange<ID> entryChange = (EntryChange<ID>)nextChange;
                           
                            if ( needToCloneOnChange )
                            {
                                curEntry = curEntry.clone();
                                needToCloneOnChange = false;
                            }
                            
                            try
                            {
                                AttributeUtils.applyModification(curEntry, entryChange.getRedoChange());
                            }
                            catch( LdapException e )
                            {
                                //TODO decide whether to throw IOException or an internal exception here
                            }
                        }
                        else if ( nextChange instanceof EntryAddDelete )
                        {
                            EntryAddDelete<ID> addDelete = (EntryAddDelete<ID>)nextChange;
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
    
    
    public boolean hasDeletesFor( Dn partitionDn, String attributeOid )
    {
        Map<String, TreeSet< IndexEntry<Object,ID>>> deletedIndices = 
            indexDeletes.get( partitionDn ); 
        
        if ( deletedIndices != null )
        {
            return ( deletedIndices.get( attributeOid ) != null );
        }
       
        return false;
    }
    
    
    public IndexCursor<Object,Entry,ID> getCursorFor( Dn partitionDn, String attributeOid, boolean forwardIndex, Object onlyValueKey, ID onlyIDKey, IndexComparator<Object,ID> comparator )
    {
        TxnIndexCursor<ID> txnIndexCursor = null;
        
        Map<String, TreeSet<IndexEntry<Object,ID>>> forwardIndices = 
            forwardIndexAdds.get( partitionDn );
        
        if ( forwardIndices != null )
        {
            TreeSet<IndexEntry<Object, ID>> sortedSet = forwardIndices.get( attributeOid );
            
            if ( sortedSet != null )
            {
                txnIndexCursor = new TxnIndexCursor<ID>( sortedSet, forwardIndex, onlyValueKey, onlyIDKey, comparator );
            }
        }
        
        return txnIndexCursor;
    }
    
    public void addRead( DnSet readSet )
    {
        readDns.add( readSet );
    }
    
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
            readDnSet =  readIt.next();
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
                    else //one level or subtree scope for the write.
                    {
                        // Even if one level scope, conservatively check the whole subtree
                        if ( readDnSet.getBaseDn().isDescendantOf( writeDnSet.getBaseDn() ) )
                        {
                            result = true;
                            break;
                        }
                    }
                }
                else //one level or subtree scope for the read.
                {
                    if ( writeScope.equals( SearchScope.OBJECT ) )
                    {
                        if ( readDnSet.getBaseDn().isAncestorOf( writeDnSet.getBaseDn() ) )
                        {
                            result = true;
                            break;
                        }
                    }
                    else //one level or subtree scope for the write.
                    {
                        // Even if one level scope, conservatively check if any basedn is descendent of the other
                        if ( ( readDnSet.getBaseDn().isDescendantOf( writeDnSet.getBaseDn() ) ) || 
                              ( readDnSet.getBaseDn().isAncestorOf( writeDnSet.getBaseDn() ) )  )
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
