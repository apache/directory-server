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


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;


/**
 * A backend friendly wrapper around a JDBM BTree that transparent enables
 * duplicates when the BTree does not support them.
 * 
 * TODO Need to rewrite the Javadocs in this interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Table
{
    /**
     * Gets the comparator used by this Table: may be null if this Table was
     * not initialized with one.
     *
     * @return the final comparator instance or null if this Table was not
     * created with one.
     */
    TupleComparator getComparator();


    /**
     * Gets the data renderer used by this Table to display or log records keys
     * and values.
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
     * Checks to see if this Table has enabled the use of duplicate keys.
     *
     * @return true if duplicate keys are enabled, false otherwise.
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
     * Checks to see if this table has a key: same as a get call with a check to
     * see if the returned value is null or not.
     *
     * @param key the Object of the key to check for
     * @return true if the key exists, false otherwise.
     * @throws NamingException if there is a failure to read the underlying Db
     */
    boolean has( Object key ) throws NamingException;


    /**
     * Checks to see if this table has a key with a specific value.
     *
     * @param key the key Object to check for
     * @param value the value Object to check for
     * @return true if a record with the key and value exists, false otherwise.
     * @throws NamingException if there is a failure to read the underlying Db
     */
    boolean has( Object key, Object value ) throws NamingException;


    /**
     * Checks to see if this table has a record with a key greater/less than or
     * equal to the key argument.  The key argument need not exist for this
     * call to return true.  The underlying database must be a BTree because
     * this method depends on the use of sorted keys.
     *
     * @param key the key Object to compare keys to
     * @param isGreaterThan boolean for greater than or less then comparison
     * @return true if a record with a key greater/less than the key argument
     * exists, false otherwise
     * @throws NamingException if there is a failure to read the underlying Db,
     * or if the underlying Db is not a Btree.
     */
    boolean has( Object key, boolean isGreaterThan ) throws NamingException;


    /**
     * Checks to see if this table has a record with a key equal to the
     * argument key with a value greater/less than or equal to the value
     * argument provided.  The key argument <strong>MUST</strong> exist for
     * this call to return true and the underlying Db must be a Btree that
     * allows for sorted duplicate values.  The entire basis to this method
     * depends on the fact that duplicate key values are sorted according to
     * a valid value comparator function.
     *
     * @param key the key Object
     * @param val the value Object to compare values to
     * @param isGreaterThan boolean for greater than or less then comparison
     * @return true if a record with a key greater/less than the key argument
     * exists, false otherwise
     * @throws NamingException if there is a failure to read the underlying Db
     * or if the underlying Db is not of the Btree type that allows sorted
     * duplicate values.
     */
    boolean has( Object key, Object val, boolean isGreaterThan ) throws NamingException;


    // ------------------------------------------------------------------------
    // Table Value Accessors/Mutators
    // ------------------------------------------------------------------------

    /**
     * Gets the value of a record by key if the key exists.  If this Table
     * allows duplicate keys then the first key will be returned.  If this
     * Table is also a Btree that first key will be the smallest key in the
     * Table as specificed by this Table's comparator or the default berkeley
     * bytewise lexical comparator.
     *
     * @param key the key of the record
     * @return the value of the record with key if key exists or null if
     * no such record exists.
     * @throws NamingException if there is a failure to read the underlying Db
     */
    Object get( Object key ) throws NamingException;


    /**
     * Puts a record into this Table.
     *
     * @param key the key of the record
     * @param value the value of the record.
     * @return the last value present for key or null if this the key did not
     * exist before.
     * @throws NamingException if there is a failure to read or write to
     * the underlying Db
     */
    Object put( Object key, Object value ) throws NamingException;


    /**
     * Efficiently puts a set of values into the Table.  If the Table does not 
     * support duplicate keys then only the first key within the enumeration is
     * added.  If there are more elements left after this single addition an
     * UnsupportedOperationException is thrown.  Nothing is added if the table
     * does not support duplicates and there is more than one element in the
     * enumeration.
     *
     * @param key the key to use for the values
     * @param values the values supplied as an enumeration
     * @throws NamingException if something goes wrong
     */
    Object put( Object key, NamingEnumeration values ) throws NamingException;


    /**
     * Removes all records with key from this Table.
     *
     * @param key the key of the records to remove
     * @throws NamingException if there is a failure to read or write to
     * the underlying Db
     */
    Object remove( Object key ) throws NamingException;


    /**
     * Removes a single specific record with key and value from this Table.
     *
     * @param key the key of the record to remove
     * @param value the value of the record to remove
     * @throws NamingException if there is a failure to read or write to
     * the underlying Db
     */
    Object remove( Object key, Object value ) throws NamingException;


    /**
     * Removes a set of values with the same key from this Table.  If this 
     * table does not allow duplicates the method will attempt to remove the 
     * first value in the enumeration if one exists.  If there is more than one 
     * value within the enumeration after the first drop an 
     * UnsupportedOperationException is thrown.  Nothing is removed if there is 
     * more than one element on the enumeration and the table does not support
     * duplicates.
     *
     * @param key the key of the records to remove
     * @throws NamingException if there is a failure to read or write to
     * the underlying Db
     */
    Object remove( Object key, NamingEnumeration values ) throws NamingException;


    /**
     * Sets a enumeration to the first record in the Table with a key value of
     * key and enables single next steps across all duplicate records with
     * this key.  This enumeration will only iterate over duplicates of the key.
     * Unlike listTuples(Object) which returns Tuples from the enumerations 
     * advances methods this call returns an enumeration with just the values
     * of the key.
     * 
     * @param key the key to iterate over
     * @throws NamingException if the underlying browser could not be set
     */
    NamingEnumeration listValues( Object key ) throws NamingException;


    // ------------------------------------------------------------------------
    // listTuple overloads
    // ------------------------------------------------------------------------

    /**
     * Sets a cursor to the first record in the Table and enables single
     * next steps across all records.
     *
     * @throws NamingException if the underlying cursor could not be set.
     */
    NamingEnumeration listTuples() throws NamingException;


    /**
     * Sets a cursor to the first record in the Table with a key value of
     * key and enables single next steps across all duplicate records with
     * this key.  This cursor will only iterate over duplicates of the key.
     *
     * @param key the key to iterate over
     * @throws NamingException if the underlying cursor could not be set
     */
    NamingEnumeration listTuples( Object key ) throws NamingException;


    /**
     * Sets a cursor to the first record in the Table with a key value
     * greater/less than or equal to key and enables single next steps across
     * all records with key values equal to or less/greater than key.
     *
     * @param key the key to use to position this cursor to record with a key
     * greater/less than or equal to it
     * @param isGreaterThan if true the cursor iterates up over ascending keys
     * greater than or equal to the key argument, but if false this cursor
     * iterates down over descending keys less than or equal to key argument
     * @throws NamingException if the underlying cursor could not be set
     */
    NamingEnumeration listTuples( Object key, boolean isGreaterThan ) throws NamingException;


    /**
     * Sets a cursor to the first record in the Table with a key equal to
     * the key argument whose value is greater/less than or equal to key and
     * enables single next steps across all records with key equal to key.
     * Hence this cursor will only iterate over duplicate keys where values are
     * less than or greater than or equal to val.
     *
     * @param key the key to use to position this cursor to record with a key
     * equal to it.
     * @param val the value to use to position this cursor to record with a
     * value greater/less than or equal to it.
     * @param isGreaterThan if true the cursor iterates up over ascending 
     * values greater than or equal to the val argument, but if false this 
     * cursor iterates down over descending values less than or equal to val 
     * argument starting from the largest value going down
     * @throws NamingException if the underlying cursor could not be set or
     * this method is called over a cursor on a table that does not have sorted
     * duplicates enabled.
     */
    NamingEnumeration listTuples( Object key, Object val, boolean isGreaterThan ) throws NamingException;


    // ------------------------------------------------------------------------
    // Table Record Count Methods
    // ------------------------------------------------------------------------

    /**
     * Gets the count of the number of records in this Table.
     *
     * @return the number of records
     * @throws NamingException if there is a failure to read the underlying Db
     */
    int count() throws NamingException;


    /**
     * Gets the count of the number of records in this Table with a specific
     * key: returns the number of duplicates for a key.
     *
     * @param key the Object key to count.
     * @return the number of duplicate records for a key.
     * @throws NamingException if there is a failure to read the underlying Db
     */
    int count( Object key ) throws NamingException;


    /**
     * Returns the number of records greater than or less than a key value.  The
     * key need not exist for this call to return a non-zero value.
     *
     * @param key the Object key to count.
     * @param isGreaterThan boolean set to true to count for greater than and
     * equal to record keys, or false for less than or equal to keys.
     * @return the number of keys greater or less than key.
     * @throws NamingException if there is a failure to read the underlying Db
     */
    int count( Object key, boolean isGreaterThan ) throws NamingException;


    /**
     * Closes the underlying Db of this Table.
     *
     * @throws NamingException on any failures
     */
    void close() throws NamingException;
}
