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


import java.net.URI;

import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.core.partition.impl.btree.LongComparator;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndex;
import org.apache.directory.server.xdbm.EmptyIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.LdapComparator;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.Normalizer;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * An Index backed by an AVL Tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlIndex<K, O> extends AbstractIndex<K, O, Long>
{
    protected Normalizer normalizer;
    protected AvlTable<K, Long> forward;
    protected AvlTable<Long, K> reverse;


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


    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws Exception
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
            throw new Exception( I18n.err( I18n.ERR_212, attributeType ) );
        }

        LdapComparator<K> comp = ( LdapComparator<K> ) mr.getLdapComparator();

        /*
         * The forward key/value map stores attribute values to master table
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */
        forward = new AvlTable<K, Long>( attributeType.getName(), comp, LongComparator.INSTANCE, true );

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
                reverse = new AvlTable<Long, K>( attributeType.getName(), LongComparator.INSTANCE, comp, false );
            }
            else
            {
                reverse = new AvlTable<Long, K>( attributeType.getName(), LongComparator.INSTANCE, comp, true );
            }
        }
    }


    public void add( K attrVal, Long id ) throws Exception
    {
        forward.put( attrVal, id );

        if ( withReverse )
        {
            reverse.put( id, attrVal );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        if ( forward != null )
        {
            forward.close();
        }

        if ( reverse != null )
        {
            reverse.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public int count() throws Exception
    {
        return forward.count();
    }


    /**
     * {@inheritDoc}
     */
    public int count( K attrVal ) throws Exception
    {
        return forward.count( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( Long id ) throws Exception
    {
        if ( withReverse )
        {
            if ( isDupsEnabled() )
            {
                Cursor<Tuple<Long, K>> cursor = reverse.cursor( id );

                while ( cursor.next() )
                {
                    Tuple<Long, K> tuple = cursor.get();
                    forward.remove( tuple.getValue(), id );
                }

                cursor.close();

            }
            else
            {
                K key = reverse.get( id );
                forward.remove( key );
            }

            reverse.remove( id );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void drop( K attrVal, Long id ) throws Exception
    {
        forward.remove( attrVal, id );

        if ( withReverse )
        {
            reverse.remove( id, attrVal );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( K attrVal ) throws Exception
    {
        return forward.has( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( K attrVal, Long id ) throws Exception
    {
        return forward.has( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, Long>> forwardCursor() throws Exception
    {
        return new IndexCursorAdaptor( forward.cursor(), true );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, Long>> forwardCursor( K key ) throws Exception
    {
        return new IndexCursorAdaptor( forward.cursor( key ), true );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardGreaterOrEq( K attrVal ) throws Exception
    {
        return forward.hasGreaterOrEqual( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardGreaterOrEq( K attrVal, Long id ) throws Exception
    {
        return forward.hasGreaterOrEqual( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardLessOrEq( K attrVal ) throws Exception
    {
        return forward.hasLessOrEqual( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardLessOrEq( K attrVal, Long id ) throws Exception
    {
        return forward.hasLessOrEqual( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public Long forwardLookup( K attrVal ) throws Exception
    {
        return forward.get( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<Long> forwardValueCursor( K key ) throws Exception
    {
        return forward.valueCursor( key );
    }


    /**
     * {@inheritDoc}
     */
    public int greaterThanCount( K attrVal ) throws Exception
    {
        return forward.greaterThanCount( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public int lessThanCount( K attrVal ) throws Exception
    {
        return forward.lessThanCount( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverse( Long id ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.has( id );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverse( Long id, K attrVal ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.has( id, attrVal );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, Long>> reverseCursor() throws Exception
    {
        if ( withReverse )
        {
            return new IndexCursorAdaptor( reverse.cursor(), false );
        }
        else
        {
            return new EmptyIndexCursor<K, Long>();
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, Long>> reverseCursor( Long id ) throws Exception
    {
        if ( withReverse )
        {
            return new IndexCursorAdaptor( reverse.cursor( id ), false );
        }
        else
        {
            return new EmptyIndexCursor<K, Long>();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseGreaterOrEq( Long id ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.hasGreaterOrEqual( id );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseGreaterOrEq( Long id, K attrVal ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.hasGreaterOrEqual( id, attrVal );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseLessOrEq( Long id ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.hasLessOrEqual( id );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseLessOrEq( Long id, K attrVal ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.hasLessOrEqual( id, attrVal );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public K reverseLookup( Long id ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.get( id );
        }
        else
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<K> reverseValueCursor( Long id ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.valueCursor( id );
        }
        else
        {
            return new EmptyCursor<K>();
        }
    }


    /**
     * throws UnsupportedOperationException cause it is a in-memory index
     */
    public void setWkDirPath( URI wkDirPath )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_213 ) );
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


    /**
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
    }
}
