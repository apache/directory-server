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
import java.util.List;
import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.txn.TxnHandle;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/** Package protected */
interface Transaction extends TxnHandle
{

    /**
     * Get the list of txns that this txn should check when mergin
     * its view from the partitions with the data in txn logs.
     *
     * @return list of txns this txn depends.
     */
    List<ReadWriteTxn> getTxnsToCheck();


    /**
     * Returns the start time of the txn
     *
     * @return start time of the txn
     */
    long getStartTime();


    /**
     * Called to notify the txn of the txn start. Txn state
     * is updated accordingly.
     *
     * @param startTime start time of the txn
     */
    //void startTxn( long startTime );

    void reuseTxn();


    /**
     * Called when txn commits. Txn state is updated
     * accordingly.
     *
     * @param commitTime commit time of the txn.
     */
    void commitTxn( long commitTime );


    /**
     * Gets the commit time of the txn
     *
     * @return commit time of the txn
     */
    long getCommitTime();


    /**
     * Called when txn aborts. Txn state is updated accordingly.
     *
     */
    void abortTxn();


    /**
     * Provides a transactionally consistent view of the entry.
     *
     * @param partitionDn dn of the partition the entry lives in.
     * @param entryID id of the entry
     * @param entry version of the entry the caller has.
     * @return
     */
    Entry mergeUpdates( Dn partitionDn, UUID entryID, Entry entry );


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
    Object mergeReverseLookup( Dn partitionDN, String attributeOid, UUID id, Object curValue );


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

}
