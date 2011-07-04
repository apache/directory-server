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


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.schema.AttributeType;


/**
 * A generic index implementation that is just used to hold the index configuration
 * parameters (attributeId, cacheSize, wkDirPath). All other methods are not working.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractIndex<K, O, ID> implements Index<K, O, ID>
{
    /** The attribute identifier for this index */ 
    protected String attributeId;
    
    /** the attribute type resolved for this JdbmIndex */
    protected AttributeType attributeType;

    /** the size (number of index entries) for the cache */
    protected int cacheSize = DEFAULT_INDEX_CACHE_SIZE;

    /** whether or not this index has been initialized */
    protected boolean initialized;

    /**
     * Creates a new instance of AbstractIndex.
     * 
     * @param attributeId the attribute ID
     */
    protected AbstractIndex()
    {
    }

    
    /**
     * Creates a new instance of AbstractIndex.
     * 
     * @param attributeId the attribute ID
     */
    protected AbstractIndex( String attributeId )
    {
        this.attributeId = attributeId;
    }


    public String getAttributeId()
    {
        return attributeId;
    }


    /**
     * {@inheritDoc}
     */
    public AttributeType getAttribute()
    {
        return attributeType;
    }


    public void setAttributeId( String attributeId )
    {
        protect( "attributeId" );
        this.attributeId = attributeId;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDupsEnabled()
    {
        return !attributeType.isSingleValued();
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
        protect( "cacheSize" );
        this.cacheSize = cacheSize;
    }
    
    
    /**
     * Protects configuration properties from being set after initialization.
     *
     * @param property the property to protect
     */
    protected void protect( String property )
    {
        if ( initialized )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_575, property ) );
        }
    }
}
