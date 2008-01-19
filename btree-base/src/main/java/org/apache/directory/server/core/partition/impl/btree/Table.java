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
    TupleComparator<K,V> getComparator();


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
     * Checks whether or not calls to count the number of keys greater than or
     * less than the key are exact.
     * 
     * Checking to see the number of values greater than or less than some key
     * may be excessively costly.  Since this is not a critical function but 
     * one that assists in optimizing searches some implementations can just 
     * return a worst case (maximum) guess.  
     *
     * @return true if the count is an exact value or a worst case guess 
     */
    boolean isCountExact();
    

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
     * Checks to see if this table has a record with a key greater/less than or
     * equal to the key argument.  The key argument need not exist for this
     * call to return true.  The underlying database must sort keys based on a
     * key comparator because this method depends on key ordering.
     *
     * @param key the key to compare keys to
     * @param isGreaterThan boolean for greater than or less then comparison
     * @return true if a record with a key greater/less than the key argument
     * exists, false otherwise
     * @throws Exception if there is a failure to read the underlying Db
     */
    boolean has( K key, boolean isGreaterThan ) throws Exception;


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
     * @throws Exception if there is a failure to read the underlying Db
     * or if the underlying Db is not of the Btree type that allows sorted
     * duplicate values.
     */
    boolean has( K key, V val, boolean isGreaterThan ) throws Exception;


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
     * @return the last value present for the key or null if the key did not
     * exist before.
     * @throws Exception if there is a failure to read or write to the
     * underlying Db
     * @throws IllegalArgumentException if a null key or value is used
     */
    V put( K key, V value ) throws Exception;


    /**
     * Removes all records with a specified key from this Table.
     *
     * @param key the key of the records to remove
     * @return the removed object or null if one did not exist for the key
     * @throws Exception if there is a failure to read or write to
     * the underlying Db
     */
    V remove( K key ) throws Exception;


    /**
     * Removes a single key value pair with a specified key and value from
     * this Table.
     *
     * @param key the key of the record to remove
     * @param value the value of the record to remove
     * @return the removed value object or null if one did not exist
     * @throws Exception if there is a failure to read or write to
     * the underlying Db
     */
    V remove( K key, V value ) throws Exception;


    /**
     * Creates a Cursor that traverses records in a Table.
     *
     * @return a Cursor over Tuples containing the key value pairs
     * @throws Exception if there are failures accessing underlying stores
     */
    Cursor<Tuple<K,V>> cursor() throws Exception;


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
