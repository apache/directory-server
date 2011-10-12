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
package org.apache.directory.server.core.partition.index;


import org.apache.directory.shared.ldap.model.cursor.Cursor;


/**
 * A Cursor introducing new advance methods designed to reduce some
 * inefficiencies encountered when scanning over Tuples.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface IndexCursor<V, E, ID> extends Cursor<IndexEntry<V, ID>>
{
    /**
     * An alternative to calling before(IndexEntry) which often may require
     * wrapping an id and value in a newly created IndexEntry object that may
     * be an unnecessary object creation.  Some implementations may not
     * support this operation and may throw an UnsupportedOperationEception.
     *
     * @param id the id for the entry
     * @param indexValue the value to advance just before
     * @throws Exception if there are faults performing this operation
     */
    void beforeValue( ID id, V indexValue ) throws Exception;


    /**
     * An alternative to calling after(IndexEntry) which often may require
     * wrapping an id and value in a newly created IndexEntry object that may
     * be an unnecessary object creation.  Some implementations may not
     * support this operation and may throw an UnsupportedOperationEception.
     *
     * @param id the id for the entry
     * @param indexValue the value to advance just after the last value
     * @throws Exception if there are faults performing this operation
     */
    void afterValue( ID id, V indexValue ) throws Exception;
}