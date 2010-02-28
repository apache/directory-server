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


import org.apache.directory.shared.ldap.cursor.Cursor;


/**
 * A Cursor introducing new advance methods designed to reduce some
 * inefficiencies encountered when scanning over Tuples.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public interface TupleCursor<K, V> extends Cursor<Tuple<K, V>>
{
    /**
     * An alternative to calling before(Tuple) which often may require
     * wrapping a key in a newly created Tuple object that may be unnecessary.
     * This method behaves just like before(Tuple) except it advances to just
     * before the first value of the key.
     *
     * @param key the key to advance just before
     * @throws Exception if there are faults peforming this operation
     */
    void beforeKey( K key ) throws Exception;


    /**
     * An alternative to calling after(Tuple) which often may require
     * wrapping a key in a newly created Tuple object that may be unnecessary.
     * This method behaves just like after(Tuple) except it advances to just
     * after the last value of the key.
     *
     * @param key the key to advance just after the last value
     * @throws Exception if there are faults peforming this operation
     */
    void afterKey( K key ) throws Exception;


    /**
     * An alternative to calling before(Tuple) which often may require
     * wrapping a key and a value in a newly created Tuple object that may be
     * unnecessary.  This method behaves just like before(Tuple) except it
     * advances to just before the value of the key which may still be of the
     * same key.  This method will not be supported if duplicate keys are not
     * supported.  In this case an UnsupportedOperationException will be
     * thrown.
     *
     * @param key the key of the value to advance just before
     * @param value the value to advance just before
     * @throws UnsupportedOperationException if duplicate keys not supporrted
     * @throws Exception if there are faults peforming this operation
     */
    void beforeValue( K key, V value ) throws Exception;


    /**
     * An alternative to calling after(Tuple) which often may require
     * wrapping a key and a value in a newly created Tuple object that may be
     * unnecessary.  This method behaves just like after(Tuple) except it
     * advances to just after the value with the specified key.  This method
     * will not be supported if duplicate keys are not supported.  In this
     * case an UnsupportedOperationException will be thrown.
     *
     * @param key the key of the value to advance just after
     * @param value the value to advance just after
     * @throws UnsupportedOperationException if duplicate keys not supporrted
     * @throws Exception if there are faults peforming this operation
     */
    void afterValue( K key, V value ) throws Exception;
}
