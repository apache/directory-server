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


import java.net.URI;

import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.schema.AttributeType;


/**
 * An index used to retrieve elements into the master table. Each stored element that is
 * indexed has a unique identifier (ID). We may have more than one element associated with
 * a value (K). We may cache the retrieved element (O). <br/>
 * Cursors over indices can also be gotten to traverse the
 * values of the index.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @param <K> The Indexed value type, used to retrieve an element
 * @param <O> The indexed element type, when retrieved
 * @param <ID> The unique identifier type in the master table
 */
public interface Index<K, O, ID>
{
    /** The default cache size (ie, the number of elements we stored in the cache) */
    int DEFAULT_INDEX_CACHE_SIZE = 100;


    // -----------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------

    /**
     * Gets the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     *
     * @return configured attribute oid or alias name
     */
    String getAttributeId();


    /**
     * Sets the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     *
     * @param attributeId configured attribute oid or alias name
     */
    void setAttributeId( String attributeId );


    /**
     * Gets the size of the index cache in terms of the number of index entries to be cached.
     *
     * @return the size of the index cache
     */
    int getCacheSize();


    /**
     * Sets the size of the index cache in terms of the number of index entries to be cached.
     *
     * @param cacheSize the size of the index cache
     */
    void setCacheSize( int cacheSize );


    /**
     * Sets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @param wkDirPath optional working directory path
     */
    void setWkDirPath( URI wkDirPath );


    /**
     * Gets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @return optional working directory path
     */
    URI getWkDirPath();


    // -----------------------------------------------------------------------
    // E N D   C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------

    /**
     * Gets the attribute this Index is built upon.
     *
     * @return the id of the Index's attribute
     */
    AttributeType getAttribute();


    /**
     * Gets the total scan count for this index.
     *
     * @return the number of key/value pairs in this index
     * @throws Exception on failure to access index db files
     */
    int count() throws Exception;


    /**
     * Gets the scan count for the occurrence of a specific attribute value
     * within the index.
     *
     * @param attrVal the value of the attribute to get a scan count for
     * @return the number of key/value pairs in this index with the value value
     * @throws Exception on failure to access index db files
     */
    int count( K attrVal ) throws Exception;


    int greaterThanCount( K attrVal ) throws Exception;


    int lessThanCount( K attrVal ) throws Exception;


    ID forwardLookup( K attrVal ) throws Exception;


    K reverseLookup( ID id ) throws Exception;


    /**
     * Add an entry into the index, associated with the element ID. The added
     * value is the key to retrieve the element having the given ID.
     * 
     * @param attrVal The added value
     * @param id The element ID pointed by the added value
     * @throws Exception If the addition can't be done
     */
    void add( K attrVal, ID id ) throws Exception;


    /**
     * Remove all the reference to an entry from the index.
     * <br/>
     * As an entry might be referenced more than once in the forward index,
     * depending on which index we are dealing with, we need to iterate
     * over all the values contained into the reverse index for this entryId.
     * <br/>
     * For instance, considering the ObjectClass index for an entry having
     * three ObjectClasses (top, person, inetOrgPerson), then the reverse
     * index will contain :
     * <pre>
     * [entryId, [top, person, inetOrgPerson]]
     * </pre>
     * and the forward index will contain many entries like :
     * <pre>
     * [top, [..., entryId, ...]]
     * [person,  [..., entryId, ...]]
     * [inetOrgPerson,  [..., entryId, ...]]
     * </pre>
     * So dropping the entryId means that we must first get all the values from
     * the reverse index (and we will get [top, person, inetOrgPerson]) then to
     * iterate through all those values to remove entryId from the associated
     * list of entryIds.
     * 
     * @param entryId The master table entry ID to remove
     * @throws Exception
     */
    void drop( ID entryId ) throws Exception;


    /**
     * Remove the pair <K,ID> from the index for the given value and id.
     * 
     * @param attrVal The value we want to remove from the index
     * @param id The associated ID
     * @throws Exception If the removal can't be done
     */
    void drop( K attrVal, ID id ) throws Exception;


    IndexCursor<K, ID> reverseCursor() throws Exception;


    IndexCursor<K, ID> forwardCursor() throws Exception;


    IndexCursor<K, ID> reverseCursor( ID id ) throws Exception;


    IndexCursor<K, ID> forwardCursor( K key ) throws Exception;


    Cursor<K> reverseValueCursor( ID id ) throws Exception;


    Cursor<ID> forwardValueCursor( K key ) throws Exception;


    boolean forward( K attrVal ) throws Exception;


    boolean forward( K attrVal, ID id ) throws Exception;


    boolean reverse( ID id ) throws Exception;


    boolean reverse( ID id, K attrVal ) throws Exception;


    boolean forwardGreaterOrEq( K attrVal ) throws Exception;


    boolean forwardGreaterOrEq( K attrVal, ID id ) throws Exception;


    boolean reverseGreaterOrEq( ID id ) throws Exception;


    boolean reverseGreaterOrEq( ID id, K attrVal ) throws Exception;


    boolean forwardLessOrEq( K attrVal ) throws Exception;


    boolean forwardLessOrEq( K attrVal, ID id ) throws Exception;


    boolean reverseLessOrEq( ID id ) throws Exception;


    boolean reverseLessOrEq( ID id, K attrVal ) throws Exception;


    void close() throws Exception;


    void sync() throws Exception;


    /**
     * tells whether the Index implementation supports storing duplicate keys
     *
     * @return true if duplicate keys are allowed false otherwise
     */
    boolean isDupsEnabled();


    /**
     * Tells if the index has a reverse table or not
     * @return true if the index has a reverse table
     */
    boolean hasReverse();
}
