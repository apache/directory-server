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
package org.apache.directory.server.core.partition.impl.btree;


import org.apache.directory.server.core.cursor.Cursor;

import java.io.IOException;


/**
 * A wrapper interface around BTree implementations used to abstract away
 * implementation details.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Table<K, V>
{
    /**
     * Gets the comparator pair used by this Table: may be null if this Table
     * was not initialized with one.
     *
     * @return the comparator pair or null if this Table was not created with
     * one.
     */
    TupleComparator getComparator();


    /**
     * Gets an optional data renderer associated with this Table to display or
     * log record keys and values.
     *
     * @return the renderer used
     */
    TupleRenderer getRenderer();


    /**
     * Sets the data renderer to by used by this Table to display or log record
     * keys and values.
     *
     * @param renderer the DataRenderer instance to used as the renderer.
     */
    void setRenderer( TupleRenderer renderer );


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


    /**
     * Checks to see if this Table has enabled sorting on the values of
     * duplicate keys.
     *
     * @return true if duplicate key values are sorted, false otherwise.
     */
    boolean isSortedDupsEnabled();


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
     * @throws IOException if there is a failure to read the underlying Db
     */
    boolean has( K key ) throws IOException;


    /**
     * Checks to see if this table has a key with a specific value.
     *
     * @param key the key to check for
     * @param value the value to check for
     * @return true if a record with the key and value exists, false otherwise
     * @throws IOException if there is a failure to read the underlying Db
     */
    boolean has( K key, V value ) throws IOException;


    /**
     * Checks to see if this table has a record with a key greater/less than or
     * equal to the key argument.  The key argument need not exist for this
     * call to return true.  The underlying database must sort keys based on a
     * key comparator because this method depends on key ordering.
     *
     * @param key the key to compare keys to
     * @param isGreaterThan boolean for greater than or less then comparison
     * @return true if a record with a key greater/less than the key argument
     * exists, false otherwise
     * @throws IOException if there is a failure to read the underlying Db
     */
    boolean has( K key, boolean isGreaterThan ) throws IOException;


    /**
     * Checks to see if this table has a record with a key equal to the
     * argument key with a value greater/less than or equal to the value
     * argument provided.  The key argument <strong>MUST</strong> exist for
     * this call to return true and the underlying Db must allows for sorted
     * duplicate values.  The entire basis to this method depends on the fact
     * that tuples of the same key have values sorted according to a valid
     * value comparator.
     *
     * If the table does not support duplicates then an
     * UnsupportedOperationException is thrown.
     *
     * @param key the key
     * @param val the value to compare values to
     * @param isGreaterThan boolean for greater than or less then comparison
     * @return true if a record with a key greater/less than the key argument
     * exists, false otherwise
     * @throws IOException if there is a failure to read the underlying Db
     * or if the underlying Db is not of the Btree type that allows sorted
     * duplicate values.
     */
    boolean has( K key, V val, boolean isGreaterThan ) throws IOException;


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
     * @throws IOException if there is a failure to read the underlying Db
     */
    V get( K key ) throws IOException;


    /**
     * Puts a record into this Table.
     *
     * @param key the key of the record
     * @param value the value of the record.
     * @return the last value present for the key or null if the key did not
     * exist before.
     * @throws IOException if there is a failure to read or write to
     * the underlying Db
     */
    V put( K key, V value ) throws IOException;


    /**
     * Puts a set of values into the Table for a specific key.  If the Table
     * does not support duplicate keys then only the first value found in the
     * Cursor is added.  If duplicate keys are not supported and there is more
     * than one element in the Cursor an IllegalStateException will be raised
     * without putting any values into this Table.
     *
     * @param key the key to use for the values
     * @param values the values supplied as an cursor
     * @return the replaced object or null if one did not exist
     * @throws IOException if something goes wrong
     */
    V put( K key, Cursor<V> values ) throws IOException;


    /**
     * Removes all records with a specified key from this Table.
     *
     * @param key the key of the records to remove
     * @return the removed object or null if one did not exist for the key
     * @throws IOException if there is a failure to read or write to
     * the underlying Db
     */
    V remove( K key ) throws IOException;


    /**
     * Removes a single key value pair with a specified key and value from
     * this Table.
     *
     * @param key the key of the record to remove
     * @param value the value of the record to remove
     * @return the removed value object or null if one did not exist
     * @throws IOException if there is a failure to read or write to
     * the underlying Db
     */
    V remove( K key, V value ) throws IOException;


    /**
     * Removes a set of values with the same key from this Table.  If this 
     * table does not allow duplicates the method will attempt to remove the 
     * first value in the Cursor if one exists.  If there is more than one
     * value within the Cursor after the first an IllegalStateException is
     * thrown.
     *
     * @param key the key of the records to remove
     * @param values the values supplied as an enumeration
     * @return the first value removed
     * @throws IOException if there is a failure to read or write to
     * the underlying Db
     */
    V remove( K key, Cursor<V> values ) throws IOException;


    /**
     * Creates a Cursor that traverses records in a Table.
     *
     * @return a Cursor over Tuples containing the key value pairs
     * @throws IOException if there are failures accessing underlying stores
     */
    Cursor<Tuple<K,V>> cursor() throws IOException;


    // ------------------------------------------------------------------------
    // Table Record Count Methods
    // ------------------------------------------------------------------------

    
    /**
     * Gets the count of the number of records in this Table.
     *
     * @return the number of records
     * @throws IOException if there is a failure to read the underlying Db
     */
    int count() throws IOException;


    /**
     * Gets the count of the number of records in this Table with a specific
     * key: returns the number of duplicates for a key.
     *
     * @param key the Object key to count.
     * @return the number of duplicate records for a key.
     * @throws IOException if there is a failure to read the underlying Db
     */
    int count( K key ) throws IOException;


    /**
     * Returns the number of records greater than or less than a key value.  The
     * key need not exist for this call to return a non-zero value.
     *
     * @param key the Object key to count.
     * @param isGreaterThan boolean set to true to count for greater than and
     * equal to record keys, or false for less than or equal to keys.
     * @return the number of keys greater or less than key.
     * @throws IOException if there is a failure to read the underlying Db
     */
    int count( K key, boolean isGreaterThan ) throws IOException;


    /**
     * Closes the underlying Db of this Table.
     *
     * @throws IOException on any failures
     */
    void close() throws IOException;
}
