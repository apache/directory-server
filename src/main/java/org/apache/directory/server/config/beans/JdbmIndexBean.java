/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.config.beans;


/**
 * A class used to store the JdbmIndex configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmIndexBean<K, E> extends IndexBean<K, E>
{
    private int DEFAULT_INDEX_CACHE_SIZE = 100;
    
    /** default duplicate limit before duplicate keys switch to using a btree for values */
    public static final int DEFAULT_DUPLICATE_LIMIT = 512;

    /**
     * the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     */
    private String attributeId;

    /** the size (number of index entries) for the cache */
    private int cacheSize = DEFAULT_INDEX_CACHE_SIZE;

    /**
     * duplicate limit before duplicate keys switch to using a btree for values
     */
    protected int numDupLimit = DEFAULT_DUPLICATE_LIMIT;

    /**
     * Create a new JdbmIndexBean instance
     */
    public JdbmIndexBean()
    {
    }


    /**
     * Gets the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     *
     * @return configured attribute oid or alias name
     */
    public String getAttributeId()
    {
        return attributeId;
    }


    /**
     * Sets the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     *
     * @param attributeId configured attribute oid or alias name
     */
    public void setAttributeId( String attributeId )
    {
        this.attributeId = attributeId;
    }


    /**
     * Gets the size of the index cache in terms of the number of index entries to be cached.
     *
     * @return the size of the index cache
     */
    public int getCacheSize()
    {
        return cacheSize;
    }


    /**
     * Sets the size of the index cache in terms of the number of index entries to be cached.
     *
     * @param cacheSize the size of the index cache
     */
    public void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    /**
     * Gets the threshold at which point duplicate keys use btree indirection to store
     * their values.
     *
     * @return the threshold for storing a keys values in another btree
     */
    public int getNumDupLimit()
    {
        return numDupLimit;
    }


    /**
     * Sets the threshold at which point duplicate keys use btree indirection to store
     * their values.
     *
     * @param numDupLimit the threshold for storing a keys values in another btree
     */
    public void setNumDupLimit( int numDupLimit )
    {
        this.numDupLimit = numDupLimit;
    }
}
