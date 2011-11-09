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
package org.apache.directory.server.core.api.partition.index;

import java.util.UUID;

import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * Abstract class managing the object for index entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @param <V> The value stored in the Tuple, associated key for the object
 * @param <ID> The ID of the object
 */
public abstract class AbstractIndexEntry<V> implements IndexEntry<V>
{
    /** The referenced Entry if loaded from the store */
    private Entry entry;

    /**
     * Creates an instance of AbstractIndexEntry
     * 
     * @param object The interned Entry
     */
    protected AbstractIndexEntry( Entry entry )
    {
        this.entry = entry;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public abstract V getValue();

    
    /**
     * {@inheritDoc}
     */
    public abstract void setValue( V value );

    
    /**
     * {@inheritDoc}
     */
    public abstract UUID getId();

    
    /**
     * {@inheritDoc}
     */
    public abstract void setId( UUID id );

    
    /**
     * {@inheritDoc}
     */
    public Entry getEntry()
    {
        return entry;
    }

    
    /**
     * {@inheritDoc}
     */
    public abstract Tuple<?, ?> getTuple();

    
    /**
     * {@inheritDoc}
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }

    
    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        entry = null;
    }

    
    /**
     * {@inheritDoc}
     */
    public void copy( IndexEntry<V> entry )
    {
        this.entry = entry.getEntry();
    }
}
