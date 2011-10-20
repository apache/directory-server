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
package org.apache.directory.server.core.api.partition.index;


/**
 * A master table used to store indexable entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface MasterTable<ID, E> extends Table<ID, E>
{
    /** the base name for the db file for this table */
    String DBF = "master";

    /** the sequence key - stores last sequence value in the admin table */
    String SEQPROP_KEY = "__sequence__";


    /**
     * Gets the next value from the sequence of this MasterTable.  This has
     * the side-effect of incrementing the sequence values permanently.
     *
     * @param entry the entry in case the id is derived from the entry.
     * @return the current value of this MasterTable's sequence incremented by one
     * @throws Exception on failure to update the id sequence
     */
    ID getNextId( E entry ) throws Exception;
    
    
    /**
     * Resets the root ID to 0, this method should be called after deleting the
     * context entry of the partition
     * 
     * @throws Exception in case of any failure while resetting the root id value
     */
    void resetCounter() throws Exception;
}