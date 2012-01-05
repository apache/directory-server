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
package org.apache.directory.server.core.api.partition.index;


import java.io.IOException;
import java.util.Comparator;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A Abstract Table implementation aggregating the methods comon with all the 
 * different Table implementation.
 *
 * @param <K> The key
 * @param <V> The stored value
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractTable<K, V> implements Table<K, V>
{
    /** the name of this table */
    protected final String name;

    /** The global SchemaManager */
    protected SchemaManager schemaManager;

    /** a key comparator for the keys in this Table */
    protected final Comparator<K> keyComparator;

    /** a value comparator for the values in this Table */
    protected final Comparator<V> valueComparator;

    /** the current count of entries in this Table */
    protected int count;


    /**
     * Create an instance of Table
     * 
     * @param schemaManager The server schemaManager
     * @param name the name of the table
     * @param keyComparator a key comparator
     * @param valueComparator a value comparator
     */
    protected AbstractTable( SchemaManager schemaManager, String name, Comparator<K> keyComparator,
        Comparator<V> valueComparator )
    {
        this.schemaManager = schemaManager;
        this.name = name;

        if ( keyComparator == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_591 ) );
        }
        else
        {
            this.keyComparator = keyComparator;
        }

        this.valueComparator = valueComparator;
    }


    /**
     * {@inheritDoc}
     */
    public Comparator<K> getKeyComparator()
    {
        return keyComparator;
    }


    /**
     * {@inheritDoc}
     */
    public Comparator<V> getValueComparator()
    {
        return valueComparator;
    }


    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }


    /**
     * {@inheritDoc}
     */
    public int count() throws IOException
    {
        return count;
    }
}
