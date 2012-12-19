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


import java.util.Comparator;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;


/**
 * A wrapper interface around BTree implementations used to abstract away
 * implementation details.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Table<K, V>
{
    /**
     * Gets the key comparator used by this Table: may be null if this Table
     * was not initialized with one.
     *
     * @return the key comparator or null if this Table was not created with
     * one.
     */
    Comparator<K> getKeyComparator();


    /**
     * Gets the value comparator used by this Table: may be null if this Table
     * was not initialized with one.
     *
     * @return the value comparator or null if this Table was not created with
     * one.
     */
    Comparator<V> getValueComparator();


    /**
     * Gets the name of this Table.
     *
     * @return the name
     */
    String getName();


    /**
     * Checks to see if this Table has allows for duplicate keys (a.k.a.
     * multiple values for the same key).
     *
     * @return true if duplicate keys are enabled, false otherwise
     */
    boolean isDupsEnabled();


    // ------------------------------------------------------------------------
    // Simple Table Key/Value Assertions 
    // ------------------------------------------------------------------------

    /**
     * Checks to see if this table has one or more tuples with a specific key:
     * this is exactly the same as a get call with a check to see if the
     * returned value is null or not.
     *
     * @param key the Object of the key to check for
     * @return true if the key exists, false otherwise
     * @throws Exception if there is a failure to read the underlying Db
     */
    boolean has( K key ) throws Exception;


    /**
     * Checks to see if this table has a key with a specific value.
     *
     * @param key the key to check for
     * @param value the value to check for
     * @return true if a record with the key and value exists, false otherwise
     * @throws Exception if there is a failure to read the underlying Db
     */
    boolean has( K key, V value ) throws Exception;


    /**
     * Checks to see if this table has a record with a key greater than or
     * equal to the key argument.  The key argument need not exist for this
     * call to return true.  The underlying database must sort keys based on a
     * key comparator because this method depends on key ordering.
     *
     * @param key the key to compare keys to
     * @return true if a Tuple with a key greater than or equal to the key
     * argument exists, false otherwise
     * @throws Exception if there is a failure to read the underlying Db
     */
    boolean hasGreaterOrEqual( K key ) throws Exception;


    /**
     * Checks to see if this table has a record with a key less than or
     * equal to the key argument.  The key argument need not exist for this
     * call to return true.  The underlying database must sort keys based on a
     * key comparator because this method depends on key ordering.
     *
     * @param key the key to compare keys to
     * @return true if a Tuple with a key less than or equal to the key
     * argument exists, false otherwise
     * @throws Exception if there is a failure to read the underlying Db
     */
    boolean hasLessOrEqual( K key ) throws Exception;


    /**
     * Checks to see if this table has a Tuple with a key equal to the key
     * argument, yet with a value greater than or equal to the value argument
     * provided.  The key argument <strong>MUST</strong> exist for this call
     * to return true and the underlying Db must allow for values of duplicate
     * keys to be sorted.  The entire basis to this method depends on the fact
     * that tuples of the same key have values sorted according to a valid
     * value comparator.
     *
     * If the table does not support duplicates then an
     * UnsupportedOperationException is thrown.
     *
     * @param key the key
     * @param val the value to compare values to
     * @return true if a Tuple with a key equal to the key argument and a
     * value greater than the value argument exists, false otherwise
     * @throws Exception if there is a failure to read the underlying Db
     * or if the underlying Db is not of the Btree type that allows sorted
     * duplicate values.
     */
    boolean hasGreaterOrEqual( K key, V val ) throws Exception;


    /**
     * Checks to see if this table has a Tuple with a key equal to the key
     * argument, yet with a value less than or equal to the value argument
     * provided.  The key argument <strong>MUST</strong> exist for this call
     * to return true and the underlying Db must allow for values of duplicate
     * keys to be sorted.  The entire basis to this method depends on the fact
     * that tuples of the same key have values sorted according to a valid
     * value comparator.
     *
     * If the table does not support duplicates then an
     * UnsupportedOperationException is thrown.
     *
     * @param key the key
     * @param val the value to compare values to
     * @return true if a Tuple with a key equal to the key argument and a
     * value less than the value argument exists, false otherwise
     * @throws Exception if there is a failure to read the underlying Db
     * or if the underlying Db is not of the Btree type that allows sorted
     * duplicate values.
     */
    boolean hasLessOrEqual( K key, V val ) throws Exception;


    // ------------------------------------------------------------------------
    // Table Value Accessors/Mutators
    // ------------------------------------------------------------------------

    /**
     * Gets the value of a record by key if the key exists.  If this Table
     * allows duplicate keys then the first key will be returned.  If this
     * Table sorts keys then the key will be the smallest key in the Table as
     * specificed by this Table's comparator or the default bytewise lexical
     * comparator.
     *
     * @param key the key of the record
     * @return the value of the record with the specified key if key exists or
     * null if no such key exists.
     * @throws Exception if there is a failure to read the underlying Db
     */
    V get( K key ) throws Exception;


    /**
     * Puts a record into this Table.  Null is not allowed for keys or values
     * and should result in an IllegalArgumentException.
     *
     * @param key the key of the record
     * @param value the value of the record.
     * @throws Exception if there is a failure to read or write to the
     * underlying Db
     * @throws IllegalArgumentException if a null key or value is used
     */
    void put( K key, V value ) throws Exception;


    /**
     * Removes all records with a specified key from this Table.
     *
     * @param key the key of the records to remove
     * @throws Exception if there is a failure to read or write to
     * the underlying Db
     */
    void remove( K key ) throws Exception;


    /**
     * Removes a single key value pair with a specified key and value from
     * this Table.
     *
     * @param key the key of the record to remove
     * @param value the value of the record to remove
     * @throws Exception if there is a failure to read or write to
     * the underlying Db
     */
    void remove( K key, V value ) throws Exception;


    /**
     * Creates a Cursor that traverses Tuples in a Table.
     *
     * @return a Cursor over Tuples containing the key value pairs
     * @throws Exception if there are failures accessing underlying stores
     */
    Cursor<Tuple<K, V>> cursor() throws Exception;


    /**
     * Creates a Cursor that traverses Table Tuples for the same key. Only
     * Tuples with the provided key will be returned if the key exists at
     * all.  If the key does not exist an empty Cursor is returned.  The
     * motivation behind this method is to minimize the need for callers to
     * actively constrain Cursor operations based on the Tuples they return
     * to a specific key.  This Cursor is naturally limited to return only
     * the tuples for the same key.
     *
     * @param key the duplicate key to return the Tuples of
     * @return a Cursor over Tuples containing the same key
     * @throws Exception if there are failures accessing underlying stores
     */
    Cursor<Tuple<K, V>> cursor( K key ) throws Exception;


    /**
     * Creates a Cursor that traverses Table values for the same key. Only
     * Tuples with the provided key will have their values returned if the key
     * exists at all.  If the key does not exist an empty Cursor is returned.
     * The motivation behind this method is to minimize the need for callers
     * to actively constrain Cursor operations to a specific key while
     * removing overheads in creating new Tuples or population one that is
     * reused to return key value pairs.  This Cursor is naturally limited to
     * return only the values for the same key.
     *
     * @param key the duplicate key to return the values of
     * @return a Cursor over values of a key
     * @throws Exception if there are failures accessing underlying stores
     */
    Cursor<V> valueCursor( K key ) throws Exception;


    // ------------------------------------------------------------------------
    // Table Record Count Methods
    // ------------------------------------------------------------------------

    /**
     * Gets the count of the number of records in this Table.
     *
     * @return the number of records
     * @throws Exception if there is a failure to read the underlying Db
     */
    int count() throws Exception;


    /**
     * Gets the count of the number of records in this Table with a specific
     * key: returns the number of duplicates for a key.
     *
     * @param key the Object key to count.
     * @return the number of duplicate records for a key.
     * @throws Exception if there is a failure to read the underlying Db
     */
    int count( K key ) throws Exception;


    /**
     * Gets the number of records greater than or equal to a key value.  The 
     * specific key argument provided need not exist for this call to return 
     * a non-zero value.
     *
     * @param key the key to use in comparisons
     * @return the number of keys greater than or equal to the key
     * @throws Exception if there is a failure to read the underlying db
     */
    int greaterThanCount( K key ) throws Exception;


    /**
     * Gets the number of records less than or equal to a key value.  The 
     * specific key argument provided need not exist for this call to return 
     * a non-zero value.
     *
     * @param key the key to use in comparisons
     * @return the number of keys less than or equal to the key
     * @throws Exception if there is a failure to read the underlying db
     */
    int lessThanCount( K key ) throws Exception;


    /**
     * Closes the underlying Db of this Table.
     *
     * @throws Exception on any failures
     */
    void close() throws Exception;
}
