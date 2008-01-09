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


import java.io.File;
import java.io.IOException;

import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.server.core.cursor.Cursor;


/**
 * An index into the master table which returns one or more entry's positions
 * in the master table for those entries which posses an attribute with the
 * specified value.  Cursors over indices can also be gotten to traverse the
 * values of the index.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Index<K>
{
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
    void setWkDirPath( File wkDirPath );


    /**
     * Gets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @return optional working directory path
     */
    File getWkDirPath();


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
     * Gets the normalized value for an attribute.
     *
     * @param attrVal the user provided value to normalize
     * @return the normalized value.
     * @throws IOException if something goes wrong.
     */
    K getNormalized( K attrVal ) throws IOException;


    /**
     * Gets the total scan count for this index.
     *
     * @return the number of key/value pairs in this index
     * @throws IOException on failure to access index db files
     */
    int count() throws IOException;


    /**
     * Gets the scan count for the occurance of a specific attribute value 
     * within the index.
     *
     * @param attrVal the value of the attribute to get a scan count for
     * @return the number of key/value pairs in this index with the value value
     * @throws IOException on failure to access index db files
     */
    int count( K attrVal ) throws IOException;


    int greaterThanCount( K attrVal ) throws IOException;


    int lessThanCount( K attrVal ) throws IOException;


    Long forwardLookup( K attrVal ) throws IOException;


    K reverseLookup( Long id ) throws IOException;


    void add( K attrVal, Long id ) throws IOException;


    void drop( Long id ) throws IOException;


    void drop( K attrVal, Long id ) throws IOException;


    Cursor<IndexRecord> reverseCursor() throws IOException;


    Cursor<IndexRecord> cursor() throws IOException;


    boolean hasValue( K attrVal, Long id ) throws IOException;


    boolean hasValue( K attrVal, Long id, boolean isGreaterThan ) throws IOException;


    void close() throws IOException;


    void sync() throws IOException;
}
