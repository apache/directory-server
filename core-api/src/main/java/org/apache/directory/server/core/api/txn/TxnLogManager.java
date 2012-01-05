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
package org.apache.directory.server.core.api.txn;


import java.io.IOException;
import java.util.Comparator;
import java.util.UUID;

import org.apache.directory.server.core.api.log.UserLogRecord;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.txn.logedit.LogEdit;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface TxnLogManager
{
    /**
     * Logs the given log edit for the txn associated with the current thread
     *
     * @param logEdit edit to be logged
     * @param sync log edit will be flushed to media if set to true
     * @throws IOException 
     */
    void log( LogEdit logEdit, boolean sync ) throws Exception;


    /**
     * Logs the given log record for the txn associated with the current thread
     *
     * @param logRecord log record to be logged
     * @param sync log edit will be flushed to media if set to true
     * @throws Exception 
     */
    void log( UserLogRecord logRecord, boolean sync ) throws Exception;


    /**
     * Provide a transactionally consistent view on the entry identified
     * by the partitionDn+entryID by applying the necessary updates from the txn log
     * to the entry. 
     *
     * @param partitionDN dn of the partition the entry lives in
     * @param entry current version of the entry the txn has
     * @return
     */
    Entry mergeUpdates(Dn partitionDN, UUID entryID,  Entry entry );
    


    /**
     * Checks all the updates done on the given index for the given key and returns 
     * the latest version of the coressponding id
     *
     * @param partitionDN dn of the partition the entry lives in
     * @param attributeOid oid of the indexed attribute
     * @param key key to do the lookup on 
     * @param valueComp value comparator
     * @return id corresponding to the key
     */
    UUID mergeForwardLookup( Dn partitionDN, String attributeOid, Object key, UUID curId,
        Comparator<Object> valueComparator );


    /**
     * Checks all the updates done on the given index for the given id and returns 
     * the latest version of the corressponding value
     *
     * @param partitionDN dn of the partition the entry lives in
     * @param attributeOid oid of the indexed attribute
     * @param id key to do the lookup on 
     * @return value corresponding to the id
     */
    Object mergeReversLookup( Dn partitionDN, String attributeOid, UUID id, Object curValue );


    /**
     * Checks all the updates on the given index entry and returns whether the it exists or not
     *
     * @param partitionDN dn of the partition the entry lives in
     * @param attributeOid oid of the indexed attribute
     * @param indexEntry entry to do the check for 
     * @param currentlyExists true if the index entry currently exists in the underlying partition
     * @return true if the given index entry exists
     */
    boolean mergeExistence( Dn partitionDN, String attributeOid, IndexEntry<?> indexEntry, boolean currentlyExists );


    /**
     * Returns a cursor which provides a transactionally consistent view of the wrapped cursor.
     *
     * @param partitionDn dn of the partition the index lives in
     * @param wrappedCursor cursor to be wrapped
     * @param comparator comparator that should be used to order index entries
     * @param attributeOid oid of the indexed attribute
     * @param forwardIndex true if the cursor is for the forward index and false if for reverse index
     * @param onlyValueKey If cursor is forward index cursor and locked down by a value, this parameter is set to that value
     * @param onlyIDKey If cursor is forward index cursor and locked down by a ID, this parameter is set to that ID
     * @return a cursor which provides a transactionally consistent view of the wrapped cursor 
     * @throws Exception
     */
    IndexCursor<?> wrap( Dn partitionDn, IndexCursor<Object> wrappedCursor, IndexComparator<Object> comparator,
        String attributeOid, boolean forwardIndex, Object onlyValueKey, UUID onlyIDKey ) throws Exception;


    /**
     * Returns an index which a provides a transactionally consistent view over the given index
     *
     * @param partitionDn dn of the partition the index lives in
     * @param wrappedIndex index to be wrapped
     * @return a transactionally consistent index
     * @throws Exception
     */
    Index<?> wrap( Dn partitionDn, Index<?> wrappedIndex ) throws Exception;


    /**
     * Returns a transactionally consistent view of the master table
     *
     * @param partitionDn
     * @param wrappedTable
     * @return
     * @throws Exception
     */
    MasterTable wrap( Dn partitionDn, MasterTable wrappedTable ) throws Exception;


    /**
     * Adds a dn and a scope on which the current txn depens
     *
     * @param baseDn base dn
     * @param scope scope of the dependency
     */
    void addRead( Dn baseDn, SearchScope scope );


    /**
     * Adds a dn and a scope which the current txn affected through
     * a modification
     *
     * @param baseDn base dn
     * @param scope scope of the dn set affected by the change.
     */
    void addWrite( Dn baseDn, SearchScope scope );
}
