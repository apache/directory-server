/*
 * $Id: JdbmTable.java,v 1.13 2003/03/13 18:27:31 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


import java.util.TreeSet ;
import java.util.SortedSet ;
import java.util.ArrayList ;
import java.util.Collections ;
import java.io.IOException ;

import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.EmptyCursor ;
import org.apache.eve.backend.SingletonCursor ;
import org.apache.eve.backend.BackendException ;

import jdbm.helper.MRU ;
import jdbm.btree.BTree ;
import jdbm.helper.Tuple ;
import jdbm.helper.Comparator ;
import jdbm.helper.ObjectCache ;
import jdbm.helper.TupleBrowser ;
import jdbm.recman.RecordManager ;

import org.apache.avalon.framework.ExceptionUtil ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;


/**
 * A jdbm Btree wrapper that enables duplicate sorted keys using collections.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.13 $
 */
public class JdbmTable
    extends AbstractLogEnabled implements Table
{
    private static final int CACHE_SIZE = 1000 ;
    private static final String SZSUFFIX = "_btree_sz" ;

    private final String m_name ;
    private final RecordManager m_recMan ;
    private final boolean allowsDuplicates ;
    private final TupleComparator m_comparator ;

    private int m_count = 0 ;
    private BTree m_bt ;
    private TupleRenderer m_renderer ;


    //////////////////
    // Constructors //
    //////////////////

    /**
     * A jdbm Btree backed table.
     */
    public JdbmTable(String a_name, boolean allowsDuplicates,
        RecordManager a_manager, TupleComparator a_comparator)
        throws BackendException
    {
        m_name = a_name ;
        m_recMan = a_manager ;
        m_comparator = a_comparator ;
        this.allowsDuplicates = allowsDuplicates ;

        try {
            ObjectCache m_cache =
                new ObjectCache(m_recMan, new MRU(CACHE_SIZE)) ;
    
            long l_recId = m_recMan.getNamedObject(m_name) ;
            if(l_recId != 0) {
                m_bt = BTree.load(m_recMan, m_cache, l_recId) ;
                l_recId = m_recMan.getNamedObject(m_name + SZSUFFIX) ;
                m_count = ((Integer) m_recMan.fetchObject(l_recId)).intValue() ;
            } else {
                m_bt = new BTree(m_recMan, m_cache,
                    (jdbm.helper.Comparator) a_comparator.getKeyComparator()) ;
                l_recId = m_bt.getRecid() ;
                m_recMan.setNamedObject(m_name, l_recId) ;

				l_recId = m_recMan.insert(new Integer(0)) ;
				m_recMan.setNamedObject(m_name + SZSUFFIX, l_recId) ;
            }
        } catch(Throwable e) {
            throw new BackendException("Failed to load/create Btree:\n"
                + ExceptionUtil.printStackTrace(e), e) ;
        }
    }


    /**
     * A jdbm Btree backed table.
     */
    public JdbmTable(String a_name, RecordManager a_manager,
        TableComparator a_keyComparator)
        throws BackendException
    {
        this(a_name, false, a_manager, new KeyOnlyComparator(a_keyComparator)) ;
    }


    /**
     * Gets the comparator used by this Table: may be null if this Table was
     * not initialized with one.
     *
     * @return the final comparator instance or null if this Table was not
     * created with one.
     */
    public TupleComparator getComparator()
    {
        return m_comparator ;
    }


    /**
     * Checks to see if this Table has enabled the use of duplicate keys.
     *
     * @return true if duplicate keys are enabled, false otherwise.
     */
	public boolean isDupsEnabled()
    {
        return allowsDuplicates;
    }


    /**
     * Gets the name of this Table.
     *
     * @return the name
     */
    public String getName()
    {
        return m_name ;
    }


    /**
     * Gets the data renderer used by this Table to display or log records keys
     * and values.
     *
     * @return the renderer used
     */
    public TupleRenderer getRenderer()
    {
        return m_renderer ;
    }


    /**
     * Sets the data renderer to by used by this Table to display or log record
     * keys and values.
     *
     * @param a_renderer the DataRenderer instance to used as the renderer.
     */
    public void setRenderer(TupleRenderer a_renderer)
    {
        m_renderer = a_renderer ;
    }


    /**
     * Checks to see if this Table has enabled sorting on the values of
     * duplicate keys.  This will always return true but may change after
     * this release.
     *
     * @return true if duplicate key values are sorted, false otherwise.
     */
	public boolean isSortedDupsEnabled()
    {
        // If duplicates are enabled than duplicates will be maintained in
        // sorted order.
        return allowsDuplicates ;
    }


    /**
     * This operation is not supported by this implementation.  Will always
     * throw a UnsupportedOperationException.
     *
     * @param a_key the Object key to count.
     * @param isGreaterThan boolean set to true to count for greater than and
     * equal to record keys, or false for less than or equal to keys.
     * @return the number of keys greater or less than a_key.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public int count(Object a_key, boolean isGreaterThan)
        throws BackendException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Gets the count of the number of records in this Table with a specific
     * key: returns the number of duplicates for a key.
     *
     * @param a_key the Object key to count.
     * @return the number of duplicate records for a key.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public int count(Object a_key)
        throws BackendException
    {
        if(!allowsDuplicates) {
            getLogger().warn("JdbmTable.count(Object):"
                + " Should not be calling this method for tables that"
                + " do not support duplicates.") ;

            if(null == getRaw(a_key)) {
                return 0 ;
            } else {
                return 1 ;
            }
        }

        TreeSet l_set = (TreeSet) getRaw(a_key) ;

        if(l_set != null) {
            return l_set.size() ;
        }

        return 0 ;
    }


    /**
     *
     * @return the number of keys
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public int count()
        throws BackendException
    {
        return m_count ;
    }


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
    public Object get(Object a_key)
        throws BackendException
    {
        if(allowsDuplicates) {
            TreeSet l_set = (TreeSet) getRaw(a_key) ;
            if(null == l_set || l_set.size() == 0) {
                return null ;
            } else {
                return l_set.first() ;
            }
        }

        return getRaw(a_key) ;
    }


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
     * or if the underlying Db does not allow sorted duplicate values.
     */
	public boolean has(Object a_key, Object a_val, boolean isGreaterThan)
        throws BackendException
    {
        if(!allowsDuplicates) {
            getLogger().warn("JdbmTable.has(Object, Object, boolean):"
                + " Should not be calling this method for tables that"
                + " do not support duplicates.") ;

            Object l_val = getRaw(a_key) ;

            // key does not exist so return nothing
            if(null == l_val) {
				return false ;
            }
            // l_val == a_val return tuple
            else if(a_val.equals(l_val)) {
                return true ;
            }
			// l_val >= a_val and test is for greater then return tuple
            else if(m_comparator.compareValue(l_val, a_val) >= 1 &&
                isGreaterThan)
            {
				return true ;
            }
			// l_val <= a_val and test is for lesser then return tuple
            else if(m_comparator.compareValue(l_val, a_val) <= 1 &&
                !isGreaterThan)
            {
				return true ;
        	}
            // key's value does not equal a_val and conditions not satisfied.
            else {
                return false ;
            }
        }

        TreeSet l_set = (TreeSet) getRaw(a_key) ;
        if(null == l_set || l_set.size() == 0) {
            return false ;
        }

        SortedSet l_subset = null ;
        if(isGreaterThan) {
            l_subset = l_set.tailSet(a_val) ;
        } else {
            l_subset = l_set.headSet(a_val) ;
        }

        if(l_subset.size() > 0 || l_set.contains(a_val)) {
            return true ;
        }

        return false ;
    }


    /**
     * Checks to see if this table has a record with a key greater/less than or
     * equal to the key argument.  The key argument need not exist for this
     * call to return true.
     *
     * @param a_key the key Object to compare keys to
     * @param isGreaterThan boolean for greater than or less then comparison
     * @return true if a record with a key greater/less than the key argument
     * exists, false otherwise
     * @throws BackendException if there is a failure to read the underlying Db
     */
	public boolean has(Object a_key, boolean isGreaterThan)
        throws BackendException
    {
        try {
            // See if we can find the border between keys greater than and less
            // than in the set of keys.  This will be the spot we search from.
            Tuple l_tuple = m_bt.findGreaterOrEqual(a_key) ;
    
            // Test for equality first since it satisfies both greater/less than
            if(null != l_tuple &&
                m_comparator.compareKey(l_tuple.getKey(), a_key) == 0)
            {
                return true ;
            }
    
            // Greater searches are easy and quick thanks to findGreaterOrEqual
            if(isGreaterThan) {
                // A null return above means there were no equal or greater keys
                if(null == l_tuple) {
                    return false ;
                }
    
                // Not Null! - we found a tuple with equal or greater key value
                return true ;
            }
    
            // Less than searches occur below and are not as efficient or easy.
            // We need to scan up from the begining if findGreaterOrEqual failed
            // or scan down if findGreaterOrEqual succeed.
            TupleBrowser l_browser = null ;
            if(null == l_tuple) {
                // findGreaterOrEqual failed so we create a tuple and scan from
                // the lowest values up via getNext comparing each key to a_key
                l_tuple = new Tuple() ;
                l_browser = m_bt.browse() ;
    
                // We should at most have to read one key.  If 1st key is not
                // less than or equal to a_key then all keys are > a_key
                // since the keys are assorted in ascending order based on the
                // comparator.
                while(l_browser.getNext(l_tuple)) {
                    if(m_comparator.compareKey(l_tuple.getKey(), a_key) <= 0) {
                        return true ;
                    } else { // Short the search to prevent wasted cycling
                        return false ;
                    }
                }
            } else {
                // findGreaterOrEqual succeeded so use the existing tuple and
                // scan the down from the highest key less than a_key via
                // getPrevious while comparing each key to a_key.
                l_browser = m_bt.browse(l_tuple.getKey()) ;
    
                // The above call positions the browser just before the given
                // key so we need to step forward once then back.  Remember this
                // key represents a key greater than or equal to a_key.
                if(m_comparator.compareKey(l_tuple.getKey(), a_key) <= 0) {
                    return true ;
                }
                l_browser.getNext(l_tuple) ;
    
                // We should at most have to read one key, but we don't short
                // the search as in the search above first because the chance of
                // unneccessarily looping is nil since values get smaller.
                while(l_browser.getPrevious(l_tuple)) {
                    if(m_comparator.compareKey(l_tuple.getKey(), a_key) <= 0) {
                        return true ;
                    }
                }
            }
        } catch(IOException e) {
            String l_msg = "Failed to lookup whether a key " ;
            if(isGreaterThan) {
                l_msg += "greater " ;
            } else {
                l_msg += "less " ;
            }
            l_msg += "than or equal to a key " + renderKey(a_key)
                + " exists:\n" + ExceptionUtil.printStackTrace(e) ;
            throw new BackendException(l_msg, e) ;
        }

        return false ;
    }


    /**
     * Checks to see if this table has a key with a specific value.
     *
     * @param a_key the key Object to check for
     * @param a_value the value Object to check for
     * @return true if a record with the key and value exists, false otherwise.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public boolean has(Object a_key, Object a_value)
        throws BackendException
    {
        if(allowsDuplicates) {
            TreeSet l_set = (TreeSet) getRaw(a_key) ;
            if(null == l_set) {
                return false ;
            }

            return l_set.contains(a_value) ;
        }

        Object l_obj = getRaw(a_key) ;
        if(null == l_obj) {
            return false ;
        }

        return l_obj.equals(a_value) ;
    }


    /**
     * Checks to see if this table has a key: same as a get call with a check to
     * see if the returned value is null or not.
     *
     * @param a_key the Object of the key to check for
     * @return true if the key exists, false otherwise.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    public boolean has(Object a_key)
        throws BackendException
    {
        return getRaw(a_key) != null ;
    }


    /**
     * Puts a record into this Table.
     *
     * @param a_key the key of the record
     * @param a_value the value of the record.
     * @return the last value present for a_key or null if this the key did not
     * exist before. For tables allowing duplicates the return value is null.
     * @throws BackendException if there is a failure to read or write to
     * the underlying Db
     */
    public Object put(Object a_key, Object a_value)
        throws BackendException
    {
        Object l_replaced = null ;

        if(allowsDuplicates) {
            TreeSet l_set = (TreeSet) getRaw(a_key) ;
            if(null == l_set) {
                l_set = new TreeSet(m_comparator.getValueComparator()) ;
            } else if(l_set.contains(a_value)) {
                return a_value ;
            }

            l_set.add(a_value) ;
            putRaw(a_key, l_set, true) ;
            m_count++ ;
            return null ;
        }

        l_replaced = putRaw(a_key, a_value, true) ;

        if(null == l_replaced) {
            m_count++ ;
        }

        return l_replaced ;
    }


    /**
     * Removes a single specific record with a_key and a_value from this Table.
     *
     * @param the key of the record to remove.
     * @param the value of the record to remove.
     * @return a_value if (a_key, a_value) exists to be removed else null
     * @throws BackendException if there is a failure to read or write to
     * the underlying Db
     */
    public Object remove(Object a_key, Object a_value)
        throws BackendException
    {
        if(allowsDuplicates) {
            TreeSet l_set = (TreeSet) getRaw(a_key) ;

            if(null == l_set) {
                return null ;
            }

            // If removal succeeds then remove if set is empty else replace it
            if(l_set.remove(a_value)) {
                if(l_set.isEmpty()) {
                    removeRaw(a_key) ;
                } else {
                    putRaw(a_key, l_set, true) ;
                }

                // Decrement counter if removal occurs.
                m_count-- ;
                return a_value ;
            }

            return null ;
        }

        Object l_removed = null ;

        // Remove the value only if it is the same as a_value.
        if(getRaw(a_key).equals(a_value)) {
            return removeRaw(a_key) ;
        }

        return null ;
    }


    /**
     * Removes all records with a_key from this Table.
     *
     * @param the key of the records to remove.
     * @return if a_key exists its value is returned.  The value will be a
     * TreeSet containing sorted duplicate values if this table allows
     * duplicates.
     * @throws BackendException if there is a failure to read or write to
     * the underlying Db
     */
    public Object remove(Object a_key)
        throws BackendException
    {
        Object l_returned = removeRaw(a_key) ;

        if(null == l_returned) {
            return null ;
        }

        if(allowsDuplicates) {
            TreeSet l_set = (TreeSet) l_returned ;
            this.m_count -= l_set.size() ;
            return l_set.first() ;
        }

        this.m_count-- ;
        return l_returned ;
    }


    //////////////////////
    // Cursor Overloads //
    //////////////////////


    /**
     * Sets a cursor to the first record in the Table and enables single
     * next steps across all records.
     *
     * @throws BackendException if the underlying cursor could not be set.
     */
    public Cursor getCursor()
        throws BackendException
    {
        Cursor l_cursor = null ;

        try {
            l_cursor = new NoDupsCursor(m_bt.browse(), true) ;
            l_cursor.enableLogging(getLogger()) ;
        } catch(IOException e) {
            throw new BackendException("Could not create cursor over table "
                + m_name + ":\n" + ExceptionUtil.printStackTrace(e), e) ;
        }

        if(allowsDuplicates) {
            l_cursor = new DupsCursor((NoDupsCursor) l_cursor) ;
            l_cursor.enableLogging(getLogger()) ;
            return l_cursor ;
        }

        return l_cursor ;
    }


    /**
     * Sets a cursor to the first record in the Table with a key value of
     * a_key and enables single next steps across all duplicate records with
     * this key.  This cursor will only iterate over duplicates of the key.
     *
     * @param a_key the key to iterate over
     * @throws BackendException if the underlying cursor could not be set
     */
    public Cursor getCursor(Object a_key)
        throws BackendException
    {
        if(!allowsDuplicates) {
            getLogger().warn("JdbmTable.getCursor(Object):"
                + " Should not be calling this method for tables that"
                + " do not support duplicates.") ;

            Object l_val = getRaw(a_key) ;
            if(null == l_val) {
                return new EmptyCursor() ;
            } else {
            	return new SingletonCursor(new Tuple(a_key, getRaw(a_key))) ;
            }
        }

        TreeSet l_set = (TreeSet) getRaw(a_key) ;
        if(l_set == null) {
            return new EmptyCursor() ;
        }

        Cursor l_cursor = new TupleIteratorCursor(a_key, l_set.iterator()) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


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
        throws BackendException
    {
        Cursor l_cursor = null ;

        try {
            if(isGreaterThan) {
	            l_cursor = new NoDupsCursor(m_bt.browse(a_key), isGreaterThan) ;
            } else {
                /* According to the jdbm docs a browser is positioned right
                 * before a key greater than or equal to a_key.  getNext() will
                 * return the next tuple with a key greater than or equal to
                 * a_key.  getPrevious() used in descending scans for less than
                 * for equal to comparisions will not.  We need to advance
                 * forward once and check if the returned Tuple key equals
                 * a_key.  If it does then we do nothing feeding in the browser
                 * to the NoDupsCursor.  If it does not we call getPrevious and
                 * pass it into the NoDupsCursor constructor.
                 */
				Tuple l_tuple = new Tuple() ;
				TupleBrowser l_browser = m_bt.browse(a_key) ;
                if(l_browser.getNext(l_tuple)) {
                    Object l_greaterKey = l_tuple.getKey() ;
                    if(0 != m_comparator.compareKey(a_key, l_greaterKey)) {
                        // Make sure we don't return l_greaterKey in cursor
                        l_browser.getPrevious(l_tuple) ;
                    }
                }

                // If l_greaterKey != a_key above then it will not be returned.
                l_cursor = new NoDupsCursor(l_browser, isGreaterThan) ;
            }
        } catch(IOException e) {
            throw new BackendException("Failed to get TupleBrowser on table "
                + m_name + " using key " + renderKey(a_key) + ":\n"
                + ExceptionUtil.printStackTrace(e), e) ;
        }

        l_cursor.enableLogging(getLogger()) ;
        if(allowsDuplicates) {
            l_cursor = new DupsCursor((NoDupsCursor) l_cursor) ;
            l_cursor.enableLogging(getLogger()) ;
        }

        return l_cursor ;
    }


    /**
     * Sets a cursor to the first record in the Table with a key equal to
     * the a_key argument whose value is greater/less than or equal to a_val and
     * enables single next steps across all records with key equal to a_key.
     * Hence this cursor will only iterate over duplicate keys where values are
     * less/greater than or equal to a_val.
     *
     * @warning the underlying Db must have sorted duplicates enabled as in an
     * Index.
     * @param a_key the key to use to position this cursor to record with a key
     * equal to it.
     * @param a_val the value to use to position this cursor to record with a
     * value greater/less than or equal to it.
     * @param isGreaterThan if true the cursor iterates up over ascending values
     * greater than or equal to the a_val argument, but if false this cursor
     * iterates down over values less than or equal to a_val argument
     * descending from the highest values going down.
     * @throws BackendException if the underlying cursor could not be set or
     * this method is called over a cursor on a table that does not have sorted
     * duplicates enabled.
     */
    public Cursor getCursor(Object a_key, Object a_val, boolean isGreaterThan)
        throws BackendException
    {
        if(!allowsDuplicates) {
            getLogger().warn("JdbmTable.getCursor(Object, Object, boolean):"
                + " Should not be calling this method for tables that"
                + " do not support duplicates.") ;

            Object l_val = getRaw(a_key) ;

            // key does not exist so return nothing
            if(null == l_val) {
				return new EmptyCursor() ;
            }
            // l_val == a_val return tuple
            else if(a_val.equals(l_val)) {
                return new SingletonCursor(new Tuple(a_key, a_val)) ;
            }
			// l_val >= a_val and test is for greater then return tuple
            else if(m_comparator.compareValue(l_val, a_val) >= 1 &&
                isGreaterThan)
            {
				return new SingletonCursor(new Tuple(a_key, a_val)) ;
            }
			// l_val <= a_val and test is for lesser then return tuple
            else if(m_comparator.compareValue(l_val, a_val) <= 1 &&
                !isGreaterThan)
            {
				return new SingletonCursor(new Tuple(a_key, a_val)) ;
        	}
            // key's value does not equal a_val and conditions not satisfied.
            else {
                return new EmptyCursor() ;
            }
        }

        TreeSet l_set = (TreeSet) getRaw(a_key) ;
        if(l_set == null) {
            return new EmptyCursor() ;
        }

        if(isGreaterThan) {
            return new TupleIteratorCursor(a_key,
                l_set.tailSet(a_val).iterator()) ;
        } else {
            // Get all values from the smallest upto a_val and put them into
            // a list.  They will be in ascending order so we need to reverse
            // the list after adding a_val which is not included in headSet.
            SortedSet l_headset = l_set.headSet(a_val) ;
            ArrayList l_list = new ArrayList(l_set.size() + 1) ;
            l_list.addAll(l_headset) ;

            // Add largest value (a_val) if it is in the set.  TreeSet.headSet
            // does not get a_val if a_val is in the set.  So we add it now to
            // the end of the list.  List is now ascending from smallest to
            // a_val
            if(l_set.contains(a_val)) {
                l_list.add(a_val) ;
            }

            // Reverse the list now we have descending values from a_val to the
            // smallest value that a_key has.  Return tuple cursor over list.
            Collections.reverse(l_list) ;
            return new TupleIteratorCursor(a_key, l_list.iterator()) ;
        }
    }


    ////////////////////////////
    // Maintenance Operations //
    ////////////////////////////


    /**
     * Saves the count of this records to the database file but does not really
     * close the file since other tables may still be open.  This is because for
     * Jdbm the RecordManager closes the file.  Many btrees can be stored in a
     * file.  So this operation mearly performs clean up for this btree.
     * 
     * @throws BackendException if there is a failure to save the store the
     * count data.
     */
    public synchronized void close()
    {
        try {
            sync() ;
        } catch(BackendException e) {
            getLogger().error("Faild to update and close database:\n", e) ;
        }
    }


    /**
     * Does nothing really.  This is because for Jdbm the RecordManager needs
     * to perform synchronization for the whole file.  This table may be one
     * btree in that file.  So sychronization is handled higher up.
     */
    public void sync() throws BackendException
    {
        try {
            long l_recId = m_recMan.getNamedObject(m_name + SZSUFFIX) ;
            if(0 == l_recId) {
				l_recId = m_recMan.insert(new Integer(m_count)) ;
            } else {
				m_recMan.update(l_recId, new Integer(m_count)) ;
            }
        } catch(IOException e) {
            throw new BackendException("Failed to update and close database:\n"
                + ExceptionUtil.printStackTrace(e), e) ;
        }
    }


    /////////////////////////////
    // Private Utility Methods //
    /////////////////////////////


    private String render(Object a_key, Object a_value)
    {
        StringBuffer l_buf = new StringBuffer() ;

        l_buf.append("('") ;
        if(null == m_renderer) {
            l_buf.append(a_key.toString()) ;
        } else {
            l_buf.append(m_renderer.getKeyString(a_key)) ;
        }
        l_buf.append("', '") ;
        if(null == m_renderer) {
            l_buf.append(a_value.toString()) ;
        } else {
            l_buf.append(m_renderer.getValueString(a_value)) ;
        }
        l_buf.append("')") ;

        return l_buf.toString() ;
    }


    private String renderKey(Object a_obj)
    {
        StringBuffer l_buf = new StringBuffer() ;

        l_buf.append('\'') ;
        if(null == m_renderer) {
            l_buf.append(a_obj.toString()) ;
        } else {
            l_buf.append(m_renderer.getKeyString(a_obj)) ;
        }
        l_buf.append('\'') ;

        return l_buf.toString() ;
    }


    private String renderValue(Object a_obj)
    {
        StringBuffer l_buf = new StringBuffer() ;

        l_buf.append('\'') ;
        if(null == m_renderer) {
            l_buf.append(a_obj.toString()) ;
        } else {
            l_buf.append(m_renderer.getValueString(a_obj)) ;
        }
        l_buf.append('\'') ;

        return l_buf.toString() ;
    }


    /**
     * Gets the value of a record by key if the key exists.  If this Table
     * allows duplicate keys then the first key will be returned.  If this
     * Table is also a Btree that first key will be the smallest key in the
     * Table as specificed by this Table's comparator or the default berkeley
     * bytewise lexical comparator.
     *
     * @param a_key the key of the record
     * @return the value of the Btree tuple which is a TreeSet if this Table
     * supports duplicates.
     * @throws BackendException if there is a failure to read the underlying Db
     */
    private Object getRaw(Object a_key)
        throws BackendException
    {
        Object l_value = null ;

        try {
            l_value = m_bt.find(a_key) ;
        } catch(IOException e) {
            throw new BackendException("Failed to get value for key "
                + renderKey(a_key) + ":\n"
                + ExceptionUtil.printStackTrace(e), e) ;
        }

        return l_value ;
    }


    private Object putRaw(Object a_key, Object a_value, boolean doReplace)
        throws BackendException
    {
        Object l_replaced = null ;

        try {
            l_replaced = m_bt.insert(a_key, a_value, doReplace) ;
        } catch(IOException e) {
            throw new BackendException("Failed to insert key value pair "
                + render(a_key, a_value) + " into table:\n"
                + ExceptionUtil.printStackTrace(e), e) ;
        }

        return l_replaced ;
    }


    private Object removeRaw(Object a_key)
        throws BackendException
    {
        Object l_removed = null ;

        try {
            l_removed = m_bt.remove(a_key) ;
        } catch(IOException e) {
            throw new BackendException(
                "Failed to remove record with key " + renderKey(a_key)
                + ":\n" + ExceptionUtil.printStackTrace(e), e) ;
        }

        return l_removed ;
    }
}
