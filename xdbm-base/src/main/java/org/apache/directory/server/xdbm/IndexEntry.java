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


/**
 * Interface for index entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
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
     * Gets the id of the object indexed.
     *
     * @return the id of the object indexed
     */
    ID getId();


    /**
     * Sets the id of the object indexed.
     *
     * @param id the id of the object indexed
     */
    void setId( ID id );


    /**
     * Gets the object indexed if resusitated.
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
     * Sets the object indexed.
     *
     * @param obj the object indexed
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
