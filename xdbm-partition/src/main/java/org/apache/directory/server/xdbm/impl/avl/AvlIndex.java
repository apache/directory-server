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
package org.apache.directory.server.xdbm.impl.avl;


import java.io.IOException;
import java.net.URI;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.LdapComparator;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndex;
import org.apache.directory.server.xdbm.IndexEntry;


/**
 * An Index backed by an AVL Tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlIndex<K> extends AbstractIndex<K, String>
{
    protected Normalizer normalizer;
    protected AvlTable<K, String> forward;
    protected AvlTable<String, K> reverse;


    public AvlIndex()
    {
        super( true );
    }


    public AvlIndex( String attributeId )
    {
        super( attributeId, true );
    }


    public AvlIndex( String attributeId, boolean withReverse )
    {
        super( attributeId, withReverse );
    }


    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws LdapException
    {
        this.attributeType = attributeType;

        MatchingRule mr = attributeType.getEquality();

        if ( mr == null )
        {
            mr = attributeType.getOrdering();
        }

        if ( mr == null )
        {
            mr = attributeType.getSubstring();
        }

        normalizer = mr.getNormalizer();

        if ( normalizer == null )
        {
            throw new LdapOtherException( I18n.err( I18n.ERR_49018_NO_NORMALIZER_FOR_ATTRIBUTE_TYPE, attributeType ) );
        }

        LdapComparator<K> comp = ( LdapComparator<K> ) mr.getLdapComparator();

        /*
         * The forward key/value map stores attribute values to master table
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */
        forward = new AvlTable<>( attributeType.getName(), comp, UuidComparator.INSTANCE, true );

        /*
         * Now the reverse map stores the primary key into the master table as
         * the key and the values of attributes as the value.  If an attribute
         * is single valued according to its specification based on a schema
         * then duplicate keys should not be allowed within the reverse table.
         */
        if ( withReverse )
        {
            if ( attributeType.isSingleValued() )
            {
                reverse = new AvlTable<>( attributeType.getName(), UuidComparator.INSTANCE, comp, false );
            }
            else
            {
                reverse = new AvlTable<>( attributeType.getName(), UuidComparator.INSTANCE, comp, true );
            }
        }
    }


    public void add( PartitionTxn partitionTxn, K attrVal, String id ) throws LdapException
    {
        forward.put( partitionTxn, attrVal, id );

        if ( withReverse )
        {
            reverse.put( partitionTxn, id, attrVal );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( PartitionTxn partitionTxn ) throws LdapException, IOException
    {
        if ( forward != null )
        {
            forward.close( partitionTxn );
        }

        if ( reverse != null )
        {
            reverse.close( partitionTxn );
        }
    }


    /**
     * {@inheritDoc}
     */
    public long count( PartitionTxn partitionTxn ) throws LdapException
    {
        return forward.count( partitionTxn );
    }


    /**
     * {@inheritDoc}
     */
    public long count( PartitionTxn partitionTxn, K attrVal ) throws LdapException
    {
        return forward.count( partitionTxn, attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        if ( withReverse )
        {
            if ( isDupsEnabled() )
            {
                Cursor<Tuple<String, K>> cursor = reverse.cursor( partitionTxn, id );

                try
                {
                    while ( cursor.next() )
                    {
                        Tuple<String, K> tuple = cursor.get();
                        forward.remove( partitionTxn, tuple.getValue(), id );
                    }
    
                    cursor.close();
                }
                catch ( CursorException | IOException e )
                {
                    throw new LdapOtherException( e.getMessage(), e );
                }
            }
            else
            {
                K key = reverse.get( partitionTxn, id );
                forward.remove( partitionTxn, key );
            }

            reverse.remove( partitionTxn, id );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void drop( PartitionTxn partitionTxn, K attrVal, String id ) throws LdapException
    {
        forward.remove( partitionTxn, attrVal, id );

        if ( withReverse )
        {
            reverse.remove( partitionTxn, id, attrVal );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( PartitionTxn partitionTxn, K attrVal ) throws LdapException
    {
        return forward.has( partitionTxn, attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( PartitionTxn partitionTxn, K attrVal, String id ) throws LdapException
    {
        return forward.has( partitionTxn, attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<IndexEntry<K, String>> forwardCursor( PartitionTxn partitionTxn ) throws LdapException
    {
        return new IndexCursorAdaptor( partitionTxn, forward.cursor(), true );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, String>> forwardCursor( PartitionTxn partitionTxn, K key ) throws LdapException
    {
        return new IndexCursorAdaptor( partitionTxn, forward.cursor( partitionTxn, key ), true );
    }


    /**
     * {@inheritDoc}
     */
    public String forwardLookup( PartitionTxn partitionTxn, K attrVal ) throws LdapException
    {
        return forward.get( partitionTxn, attrVal );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<String> forwardValueCursor( PartitionTxn partitionTxn, K key ) throws LdapException
    {
        return forward.valueCursor( partitionTxn, key );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long greaterThanCount( PartitionTxn partitionTxn, K attrVal ) throws LdapException
    {
        return forward.greaterThanCount( partitionTxn, attrVal );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long lessThanCount( PartitionTxn partitionTxn, K attrVal ) throws LdapException
    {
        return forward.lessThanCount( partitionTxn,  attrVal );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean reverse( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        if ( withReverse )
        {
            return reverse.has( partitionTxn, id );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean reverse( PartitionTxn partitionTxn, String id, K attrVal ) throws LdapException
    {
        if ( withReverse )
        {
            return reverse.has( partitionTxn, id, attrVal );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public K reverseLookup( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        if ( withReverse )
        {
            return reverse.get( partitionTxn, id );
        }
        else
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<K> reverseValueCursor( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        if ( withReverse )
        {
            return reverse.valueCursor( partitionTxn, id );
        }
        else
        {
            return new EmptyCursor<>();
        }
    }


    /**
     * throws UnsupportedOperationException cause it is a in-memory index
     */
    public void setWkDirPath( URI wkDirPath )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_49019_CANNOT_USE_IN_MEMORY_INDEX_TO_STORE_DATA ) );
    }


    /**
     * this method always returns null for AvlIndex cause this is a in-memory index.
     */
    public URI getWkDirPath()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDupsEnabled()
    {
        if ( withReverse )
        {
            return reverse.isDupsEnabled();
        }
        else
        {
            return false;
        }
    }
}
