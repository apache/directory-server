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
public class JdbmIndexBean<K, E> extends IndexBean
{
    /** The default cache size */
    private static final int DEFAULT_INDEX_CACHE_SIZE = 100;
    
    /** default duplicate limit before duplicate keys switch to using a btree for values */
    private static final int DEFAULT_DUPLICATE_LIMIT = 512;

    /** the size (number of index entries) for the cache */
    private int indexcachesize = DEFAULT_INDEX_CACHE_SIZE;

    /** duplicate limit before duplicate keys switch to using a btree for values */
    private int indexnumduplimit = DEFAULT_DUPLICATE_LIMIT;
    
    /** The index file name */
    private String indexfilename;
    
    /** The index working directory */
    private String indexworkingdir;

    /**
     * Create a new JdbmIndexBean instance
     */
    public JdbmIndexBean()
    {
    }


    /**
     * Gets the size of the index cache in terms of the number of index entries to be cached.
     *
     * @return the size of the index cache
     */
    public int getIndexCacheSize()
    {
        return indexcachesize;
    }


    /**
     * Sets the size of the index cache in terms of the number of index entries to be cached.
     *
     * @param IndexCacheSize the size of the index cache
     */
    public void setIndexCacheSize( int indexCacheSize )
    {
        this.indexcachesize = indexCacheSize;
    }


    /**
     * Gets the threshold at which point duplicate keys use btree indirection to store
     * their values.
     *
     * @return the threshold for storing a keys values in another btree
     */
    public int getIndexNumDupLimit()
    {
        return indexnumduplimit;
    }


    /**
     * Sets the threshold at which point duplicate keys use btree indirection to store
     * their values.
     *
     * @param indexNumDupLimit the threshold for storing a keys values in another btree
     */
    public void setIndexNumDupLimit( int indexNumDupLimit )
    {
        this.indexnumduplimit = indexNumDupLimit;
    }


    /**
     * @return the indexFileName
     */
    public String getIndexFileName()
    {
        return indexfilename;
    }


    /**
     * @param indexFileName the indexFileName to set
     */
    public void setIndexFileName( String indexFileName )
    {
        this.indexfilename = indexFileName;
    }


    /**
     * @return the indexWorkingDir
     */
    public String getIndexWorkingDir()
    {
        return indexworkingdir;
    }


    /**
     * @param indexWorkingDir the indexWorkingDir to set
     */
    public void setIndexWorkingDir( String indexWorkingDir )
    {
        this.indexworkingdir = indexWorkingDir;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "JdbmIndexBean :\n" );
        sb.append( super.toString( tabs + "  " ) );
        sb.append( tabs ).append( "  index file name : " ).append( indexfilename );
        sb.append( tabs ).append( "  index working directory : " ).append( indexworkingdir );
        sb.append( tabs ).append( "  index cache size : " ).append( indexcachesize );
        sb.append( tabs ).append( "  index num dup limit : " ).append( indexnumduplimit );
        
        return sb.toString();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
