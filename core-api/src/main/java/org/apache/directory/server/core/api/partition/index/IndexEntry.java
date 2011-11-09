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

import java.util.UUID;

import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * Interface for index entries. An index entry associate an Entry with 
 * a value (the key) and the Entry ID in the table where it's stored. The Entry
 * may be present in this instance once we read it from the table.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @param <V> The value stored in the Tuple, associated key for the object
 */
public interface IndexEntry<V>
{
    /**
     * Gets the value referred to by this IndexEntry.
     *
     * @return the value of the Entry referred to
     */
    V getValue();


    /**
     * Sets the value referred to by this IndexEntry.
     *
     * @param value the value of the Entry referred to
     */
    void setValue( V value );


    /**
     * Gets the id of the indexed Entry.
     *
     * @return the id of the indexed Entry
     */
    UUID getId();


    /**
     * Sets the id of the indexed.Entry
     *
     * @param id the id of the indexed Entry
     */
    void setId( UUID id );


    /**
     * Gets the Entry indexed if found.
     *
     * @return the indexed Entry
     */
    Entry getEntry();


    /**
     * Gets access to the underlying tuple.
     *
     * @return the underlying tuple
     */
    Tuple<?, ?> getTuple();


    /**
     * Sets the indexed Entry.
     *
     * @param entry the indexed Entry
     */
    void setEntry( Entry entry );


    /**
     * Clears the id, value and Entry in this IndexEntry.
     */
    void clear();


    /**
     * Copies the values of another IndexEntry into this IndexEntry.
     *
     * @param entry the entry to copy fields of
     */
    void copy( IndexEntry<V> entry );
}
