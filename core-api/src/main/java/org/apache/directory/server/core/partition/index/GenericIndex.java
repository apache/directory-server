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
package org.apache.directory.server.core.partition.index;


import java.net.URI;

import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.schema.AttributeType;


/**
 * A generic index implementation that is just used to hold the index configuration
 * parameters (attributeId, cacheSize, wkDirPath). All other methods are not working.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GenericIndex<K, O, ID> extends AbstractIndex<K, O, ID>
{
    /** Index working directory */
    protected URI wkDirPath;


    /**
     * Creates a new instance of GenericIndex.
     * 
     * @param attributeId the attribute ID
     */
    public GenericIndex( String attributeId )
    {
        this( attributeId, DEFAULT_INDEX_CACHE_SIZE, null );
    }


    /**
     * Creates a new instance of GenericIndex.
     * 
     * @param attributeId the attribute ID
     * @param cacheSize the cache size
     */
    public GenericIndex( String attributeId, int cacheSize )
    {
        this( attributeId, cacheSize, null );
    }


    /**
     * Creates a new instance of GenericIndex.
     *
     * @param attributeId the attribute ID
     * @param cacheSize the cache size
     * @param wkDirPath the working directory
     */
    public GenericIndex( String attributeId, int cacheSize, URI wkDirPath )
    {
        super( attributeId );
        this.cacheSize = cacheSize;
        this.wkDirPath = wkDirPath;
    }


    public void add( K attrVal, ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void close() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public int count() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public int count( K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void drop( ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void drop( K attrVal, ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public IndexCursor<K, O, ID> forwardCursor() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public IndexCursor<K, O, ID> forwardCursor( K key ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public ID forwardLookup( K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public Cursor<ID> forwardValueCursor( K key ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean forward( K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean forward( K attrVal, ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean reverse( ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean reverse( ID id, K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean forwardGreaterOrEq( K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean forwardGreaterOrEq( K attrVal, ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean reverseGreaterOrEq( ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean reverseGreaterOrEq( ID id, K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean forwardLessOrEq( K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean forwardLessOrEq( K attrVal, ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean reverseLessOrEq( ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public boolean reverseLessOrEq( ID id, K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public AttributeType getAttribute()
    {
        throw new UnsupportedOperationException();
    }


    public K getNormalized( K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public URI getWkDirPath()
    {
        return wkDirPath;
    }


    public int greaterThanCount( K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public int lessThanCount( K attrVal ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public IndexCursor<K, O, ID> reverseCursor() throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public IndexCursor<K, O, ID> reverseCursor( ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public K reverseLookup( ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public Cursor<K> reverseValueCursor( ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public ForwardIndexComparator<K,ID> getForwardIndexEntryComparator()
    {
        throw new UnsupportedOperationException();
    }

    public ReverseIndexComparator<K,ID> getReverseIndexEntryComparator()
    {
        throw new UnsupportedOperationException();
    }

    
    public void setWkDirPath( URI wkDirPath )
    {
        this.wkDirPath = wkDirPath;
    }


    public void sync() throws Exception
    {
        throw new UnsupportedOperationException();
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isDupsEnabled()
    {
        return false;
    }
}
