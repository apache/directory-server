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
package org.apache.directory.server.xdbm;


import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;


/**
 * A Abstract Table implementation aggregating the methods common with all the 
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

    /** the current count of Tuples in this Table */
    protected long count;

    /** whether or not this table allows for duplicates */
    protected boolean allowsDuplicates;

    /** A counter used to differ the commit on disk after N operations */
    protected AtomicInteger commitNumber;


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

        commitNumber = new AtomicInteger( 0 );
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
    public long count( PartitionTxn transaction ) throws LdapException
    {
        return count;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long greaterThanCount( PartitionTxn transaction, K key ) throws LdapException
    {
        // take a best guess
        return Math.min( count, 10L );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long lessThanCount( PartitionTxn transaction, K key ) throws LdapException
    {
        // take a best guess
        return Math.min( count, 10L );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDupsEnabled()
    {
        return allowsDuplicates;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Name    : " ).append( name ).append( '\n' );
        sb.append( "NbElems : " ).append( count ).append( '\n' );
        sb.append( "Dups    : " ).append( allowsDuplicates ).append( '\n' );
        sb.append( "Key     : " ).append( keyComparator.getClass().getName() ).append( '\n' );
        sb.append( "Value   : " ).append( valueComparator.getClass().getName() ).append( '\n' );

        return sb.toString();
    }
}
