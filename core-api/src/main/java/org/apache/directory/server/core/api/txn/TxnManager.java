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


import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * The transaction manager interface.
 * 
 * @TODO Don't we have this interface available in the transaction API? why are
 *       we recreating this when we can reuse? 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface TxnManager
{
    /**
     * Starts a new txn and associates it with the current thread.
     *
     * @throws Exception
     */
    TxnHandle beginTransaction( boolean readOnly ) throws Exception;


    /**
     * Retries a txn. Retry is not necessary for read only transactions and
     * this method should only expect RW transactions
     * 
     */
    TxnHandle retryTransaction() throws Exception;


    /**
     * Tries to commit the current txn associated with the current thread. ReadWrite txns have to be verified against txns
     * that committed after they started for any conflicting change and conflicting
     * exception is thrown if verification fails.
     *
     * @throws Exception
     * @throws TxnConflictException
     */
    void commitTransaction() throws Exception, TxnConflictException;


    /**
     * Aborts the current txn associated with the current thread.
     *
     * @throws Exception
     */
    void abortTransaction() throws Exception;


    /**
     * Suspends the execution of the current txn and returns 
     * a handle to it.
     * 
     * @return handle for the current txn
     */
    TxnHandle suspendCurTxn();


    /**
     * Resumes the execution of the txn corresponding to the given
     * handle
     *
     * @param txnHandle handle for the txn to resume.
     */
    void resumeTxn( TxnHandle txnHandle );


    /**
     * returns a handle for the current txn
     *
     * @return handle for the current txn
     */
    TxnHandle getCurTxn();


    /**
     * Sets a handle as the current txn
     *
     * @param the new current txn
     */
    TxnHandle setCurTxn( TxnHandle txn );


    /**
     * Flushes the committed txns to partitions.
     */
    void applyPendingTxns();


    /**
     * Called when data derived from the underlying
     * data managed by the txn manager is about to be
     * changed. Ensures Every txn sees a consistent
     * version of the data. 
     *
     * @throws LdapException with a root cause as TxnConflictException
     */
    void startLogicalDataChange() throws LdapException;


    /**
     * Called when txn manager wont need data derived from  
     * data managed by the txn layer is not needed any more.  
     */
    void endLogicalDataRead();


    /**
     * Prepares the current txn for logical data reinit
     *
     * @return TRUE if txn needs to do logical data reinit
     */
    boolean prepareForLogicalDataReinit();


    /** 
     * Recovers the given partition
     */
    void recoverPartition( Partition partition );


    void setDoNotFlush();


    void unsetDoNotFlush();

}
