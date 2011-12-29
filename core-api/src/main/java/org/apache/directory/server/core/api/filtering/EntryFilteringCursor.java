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
package org.apache.directory.server.core.api.filtering;


import java.util.List;

import org.apache.directory.server.core.api.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.api.txn.TxnHandle;
import org.apache.directory.server.core.api.txn.TxnManager;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * 
 * TODO Add Javadoc !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface EntryFilteringCursor extends Cursor<Entry>
{
    /**
     * Gets whether or not this BaseEntryFilteringCursor has been abandoned.
     *
     * @return true if abandoned, false if not
     */
    boolean isAbandoned();


    /**
     * Sets whether this BaseEntryFilteringCursor has been abandoned.
     *
     * @param abandoned true if abandoned, false if not
     */
    void setAbandoned( boolean abandoned );


    /**
     * Adds an entry filter to this BaseEntryFilteringCursor at the very end of 
     * the filter list.  EntryFilters are applied in the order of addition.
     * 
     * @param filter a filter to apply to the entries
     * @return the result of {@link List#add(Object)}
     */
    boolean addEntryFilter( EntryFilter filter );


    /**
     * Removes an entry filter to this BaseEntryFilteringCursor at the very end of 
     * the filter list.  
     * 
     * @param filter a filter to remove from the filter list
     * @return the result of {@link List#remove(Object)}
     */
    boolean removeEntryFilter( EntryFilter filter );


    /**
     * Gets an unmodifiable list of EntryFilters applied.
     *
     * @return an unmodifiable list of EntryFilters applied
     */
    List<EntryFilter> getEntryFilters();


    /**
     * @return the operationContext
     */
    SearchingOperationContext getOperationContext();


    /**
     * Associate the transaction manager to this cursor
     * @param txnManager The associated TxnManager
     */
    void setTxnManager( TxnManager txnManager );


    /**
     * @return the associated transaction to this cursor
     */
    TxnHandle getTransaction();
}