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

import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * A wrapper on top of a Cursor used to filter entries we get from the backend.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface EntryFilteringCursor extends Cursor<Entry>
{
    /**
     * Adds an entry filter to this BaseEntryFilteringCursor at the very end of 
     * the filter list.  EntryFilters are applied in the order of addition.
     * 
     * @param filter a filter to apply to the entries
     * @return the result of {@link List#add(Object)}
     */
    boolean addEntryFilter( EntryFilter filter );


    /**
     * Gets an unmodifiable list of EntryFilters applied.
     *
     * @return an unmodifiable list of EntryFilters applied
     */
    List<EntryFilter> getEntryFilters();


    /**
     * @return the operationContext
     */
    SearchOperationContext getOperationContext();
}