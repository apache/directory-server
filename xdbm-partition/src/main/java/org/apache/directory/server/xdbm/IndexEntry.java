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
package org.apache.directory.server.xdbm;

import org.apache.directory.shared.ldap.model.cursor.Tuple;


/**
 * Interface for index entries. An index entry associate an Entry object with 
 * a value (the key) and the Object ID in the table where it's stored. The Object
 * may be present in this instance once we read it from the tabe.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @param <V> The value stored in the Tuple, associated key for the object
 * @param <ID> The ID of the object
 * @param <O> The associated object
 */
public interface IndexEntry<V, O, ID>
{
    /**
     * Gets the value referred to by this IndexEntry.
     *
     * @return the value of the object referred to
     */
    V getValue();


    /**
     * Sets the value referred to by this IndexEntry.
     *
     * @param value the value of the object referred to
     */
    void setValue( V value );


    /**
     * Gets the id of the indexed object.
     *
     * @return the id of the indexed object
     */
    ID getId();


    /**
     * Sets the id of the indexed.object
     *
     * @param id the id of the indexed object
     */
    void setId( ID id );


    /**
     * Gets the object indexed if resuscitated.
     *
     * @return the object indexed
     */
    O getObject();


    /**
     * Gets access to the underlying tuple.
     *
     * @return the underlying tuple
     */
    Tuple<?, ?> getTuple();


    /**
     * Sets the indexed object.
     *
     * @param obj the indexed object
     */
    void setObject( O obj );


    /**
     * Clears the id, value and object in this IndexEntry.
     */
    void clear();


    /**
     * Copies the values of another IndexEntry into this IndexEntry.
     *
     * @param entry the entry to copy fields of
     */
    void copy( IndexEntry<V, O, ID> entry );
}
