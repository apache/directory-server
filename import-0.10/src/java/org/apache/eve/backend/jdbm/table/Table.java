/*
 * $Id: Table.java,v 1.2 2003/03/13 18:27:34 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.BackendException ;


/**
 * A backend friendly wrapper around a jdbm BTree that transparent enables
 * duplicates when the BTree does not support them.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
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
    public TupleComparator getComparator() ;

    /**
     * Gets the data renderer used by this Table to display or log records keys
     * and values.
     *
     * @return the renderer used
     */
    public TupleRenderer getRenderer() ;

    /**
     * Sets the data renderer to by used by this Table to display or log record
     * keys and values.
     *
     * @param a_renderer the DataRenderer instance to used as the renderer.
     */
    public void setRenderer(TupleRenderer a_renderer) ;

    /**
     * Gets the name of this Table.
     *
     * @return the name
     */
    public String getName() ;

    /**
     * Checks to see if this Table has enabled the use of duplicate keys.
     *
     * @return true if duplicate keys are enabled, false otherwise.
     */
	public boolean isDupsEnabled() ;

    /**
     * Checks to see if this Table has enabled sorting on the values of
     * duplicate keys.
     *
     * @return true if duplicate key values are sorted, false otherwise.
     */
	public boolean isSortedDupsEnabled() ;

    ///////////////////////////////////////
    // Simple Table Key/Value Assertions //
    ///////////////////////////////////////

    /**
     * Checks to see if this table has a key: same as a get call with a check to
     * see if the returned value is null or not.
     *
     * @param a_key the Object of the key to check for
     * @return true if the key exists, false otherwise.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public boolean has(Object a_key) throws BackendException ;

    /**
     * Checks to see if this table has a key with a specific value.
     *
     * @param a_key the key Object to check for
     * @param a_value the value Object to check for
     * @return true if a record with the key and value exists, false otherwise.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public boolean has(Object a_key, Object a_value) throws BackendException ;

    /**
     * Checks to see if this table has a record with a key greater/less than or
     * equal to the key argument.  The key argument need not exist for this
     * call to return true.  The underlying database must be a BTree because
     * this method depends on the use of sorted keys.
     *
     * @param a_key the key Object to compare keys to
     * @param isGreaterThan boolean for greater than or less then comparison
     * @return true if a record with a key greater/less than the key argument
     * exists, false otherwise
     * @throws BackendException if there is a failure to read the underlying Db,
     * or if the underlying Db is not a Btree.
     */
	public boolean has(Object a_key, boolean isGreaterThan)
        throws BackendException ;

    /**
     * Checks to see if this table has a record with a key equal to the
     * argument key with a value greater/less than or equal to the value
     * argument provided.  The key argument <strong>MUST</strong> exist for
     * this call to return true and the underlying Db must be a Btree that
     * allows for sorted duplicate values.  The entire basis to this method
     * depends on the fact that duplicate key values are sorted according to
     * a valid value comparator function.
     *
     * @param a_key the key Object
     * @param a_val the value Object to compare values to
     * @param isGreaterThan boolean for greater than or less then comparison
     * @return true if a record with a key greater/less than the key argument
     * exists, false otherwise
     * @throws BackendException if there is a failure to read the underlying Db
     * or if the underlying Db is not of the Btree type that allows sorted
     * duplicate values.
     */
	public boolean has(Object a_key, Object a_val, boolean isGreaterThan)
        throws BackendException ;

    ////////////////////////////////////
    // Table Value Accessors/Mutators //
    ////////////////////////////////////

    /**
     * Gets the value of a record by key if the key exists.  If this Table
     * allows duplicate keys then the first key will be returned.  If this
     * Table is also a Btree that first key will be the smallest key in the
     * Table as specificed by this Table's comparator or the default berkeley
     * bytewise lexical comparator.
     *
     * @param a_key the key of the record
     * @return the value of the record with a_key if a_key exists or null if
     * no such record exists.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public Object get(Object a_key) throws BackendException ;

    /**
     * Puts a record into this Table.
     *
     * @param a_key the key of the record
     * @param a_value the value of the record.
     * @return the last value present for a_key or null if this the key did not
     * exist before.
     * @throws BackendException if there is a failure to read or write to
     * the underlying Db
     */
    public Object put(Object a_key, Object a_value) throws BackendException ;

    /**
     * Removes all records with a_key from this Table.
     *
     * @param the key of the records to remove.
     * @throws BackendException if there is a failure to read or write to
     * the underlying Db
     */
    public Object remove(Object a_key) throws BackendException ;

    /**
     * Removes a single specific record with a_key and a_value from this Table.
     *
     * @param the key of the record to remove.
     * @param the value of the record to remove.
     * @throws BackendException if there is a failure to read or write to
     * the underlying Db
     */
    public Object remove(Object a_key, Object a_value) throws BackendException ;

    //////////////////////
    // Cursor Overloads //
    //////////////////////

    /**
     * Sets a cursor to the first record in the Table and enables single
     * next steps across all records.
     *
     * @throws BackendException if the underlying cursor could not be set.
     */
    public Cursor getCursor() throws BackendException ;

    /**
     * Sets a cursor to the first record in the Table with a key value of
     * a_key and enables single next steps across all duplicate records with
     * this key.  This cursor will only iterate over duplicates of the key.
     *
     * @param a_key the key to iterate over
     * @throws BackendException if the underlying cursor could not be set
     */
    public Cursor getCursor(Object a_key) throws BackendException ;

    /**
     * Sets a cursor to the first record in the Table with a key value
     * greater/less than or equal to a_key and enables single next steps across
     * all records with key values equal to or less/greater than a_key.
     *
     * @warning This cursor operation has no meaning for database table
     * types other than the btree type since it relies on sorted keys.
     * @param a_key the key to use to position this cursor to record with a key
     * greater/less than or equal to it.
     * @param isGreaterThan if true the cursor iterates up over ascending keys
     * greater than or equal to the a_key argument, but if false this cursor
     * iterates down over descending keys less than or equal to a_key argument.
     * @throws BackendException if the underlying cursor could not be set
     */
    public Cursor getCursor(Object a_key, boolean isGreaterThan)
        throws BackendException ;

    /**
     * Sets a cursor to the first record in the Table with a key equal to
     * the a_key argument whose value is greater/less than or equal to a_key and
     * enables single next steps across all records with key equal to a_key.
     * Hence this cursor will only iterate over duplicate keys where values are
     * less than or greater than or equal to a_val.
     *
     * @warning the underlying Db must have sorted duplicates enabled as in an
     * Index.
     * @param a_key the key to use to position this cursor to record with a key
     * equal to it.
     * @param a_val the value to use to position this cursor to record with a
     * value greater/less than or equal to it.
     * @param isGreaterThan if true the cursor iterates up over ascending values
     * greater than or equal to the a_val argument, but if false this cursor
     * iterates down over descending values less than or equal to a_val argument
     * starting from the largest value going down.
     * @throws BackendException if the underlying cursor could not be set or
     * this method is called over a cursor on a table that does not have sorted
     * duplicates enabled.
     */
    public Cursor getCursor(Object a_key, Object a_val, boolean isGreaterThan)
        throws BackendException ;

    ////////////////////////////////
    // Table Record Count Methods //
    ////////////////////////////////

    /**
     * Gets the count of the number of records in this Table.
     *
     * @return the number of records
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public int count() throws BackendException ;

    /**
     * Gets the count of the number of records in this Table with a specific
     * key: returns the number of duplicates for a key.
     *
     * @param a_key the Object key to count.
     * @return the number of duplicate records for a key.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public int count(Object a_key) throws BackendException ;

    /**
     * Returns the number of records greater than or less than a key value.  The
     * key need not exist for this call to return a non-zero value.
     *
     * @param a_key the Object key to count.
     * @param isGreaterThan boolean set to true to count for greater than and
     * equal to record keys, or false for less than or equal to keys.
     * @return the number of keys greater or less than a_key.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public int count(Object a_key, boolean isGreaterThan) throws BackendException ;

    /**
     * Closes the underlying Db of this Table.
     * 
     * @throws BackendException if there is a failure to close the handle to
     * the underlying Db file.
     */
    public void close() ;
}
