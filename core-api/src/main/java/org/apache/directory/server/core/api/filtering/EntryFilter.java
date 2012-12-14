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


import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * An entry filter is used to modify search results while they are being 
 * returned from Cursors over ServerEntry objects.  These filters are used in
 * conjunction with a {@link FilteringCursor}.  Multiple filters can be 
 * applied one after the other and hence they are stack-able and applied in
 * order.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface EntryFilter
{
    /**
     * Filters the contents of search entries on the way out the door to 
     * client callers.  These filters can and do produce side-effects on the 
     * entry results if need be.  These entries, their attributes and values
     * should be cloned when alterations are made to avoid altering cached
     * entries.
     *
     * @param result the result to accept or reject possibly modifying it
     * @param controls search controls associated with the invocation
     * @return true if the entry is to be returned, false if it is rejected
     * @throws Exception if there are failures during evaluation
     */
    boolean accept( SearchOperationContext operation, Entry result ) throws Exception;
    
    
    /**
     * The pretty-printer for this class
     * @param tabs The tabs to add before each line
     * @return The pretty-printed instance
     */
    String toString( String tabs );
}
