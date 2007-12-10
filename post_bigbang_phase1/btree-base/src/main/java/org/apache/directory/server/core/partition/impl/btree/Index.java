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


import java.util.regex.Pattern;
import java.io.File;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * Required interfaces for an index.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Index
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
     * @throws NamingException if something goes wrong.
     */
    Object getNormalized( Object attrVal ) throws NamingException;


    /**
     * Gets the total scan count for this index.
     *
     * @return the number of key/value pairs in this index
     * @throws NamingException if their is a failure accessing the index
     */
    int count() throws NamingException;


    /**
     * Gets the scan count for the occurance of a specific attribute value 
     * within the index.
     *
     * @param attrVal the value of the attribute to get a scan count for
     * @return the number of key/value pairs in this index with the value value
     * @throws NamingException if their is a failure accessing the index
     */
    int count( Object attrVal ) throws NamingException;


    int count( Object attrVal, boolean isGreaterThan ) throws NamingException;


    Object forwardLookup( Object attrVal ) throws NamingException;


    Object reverseLookup( Object id ) throws NamingException;


    void add( Object attrVal, Object id ) throws NamingException;


    void add( Attribute attr, Object id ) throws NamingException;


    void add( Attributes attrs, Object id ) throws NamingException;


    void drop( Object entryId ) throws NamingException;


    void drop( Object attrVal, Object id ) throws NamingException;


    /**
     * If the Attribute does not have any values then this reduces to a 
     * drop(BigInteger) call.
     */
    void drop( Attribute attr, Object id ) throws NamingException;


    /**
     * If the Attribute for this index within the Attributes does not have any 
     * values then this reduces to a drop(BigInteger) call.
     */
    void drop( Attributes attrs, Object id ) throws NamingException;


    IndexEnumeration listReverseIndices( Object id ) throws NamingException;


    IndexEnumeration listIndices() throws NamingException;


    IndexEnumeration listIndices( Object attrVal ) throws NamingException;


    IndexEnumeration listIndices( Object attrVal, boolean isGreaterThan ) throws NamingException;


    IndexEnumeration listIndices( Pattern regex ) throws NamingException;


    IndexEnumeration listIndices( Pattern regex, String prefix ) throws NamingException;


    boolean hasValue( Object attrVal, Object id ) throws NamingException;


    boolean hasValue( Object attrVal, Object id, boolean isGreaterThan ) throws NamingException;


    boolean hasValue( Pattern regex, Object id ) throws NamingException;


    void close() throws NamingException;


    void sync() throws NamingException;
}
