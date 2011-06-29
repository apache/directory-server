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
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
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


    public AvlIndex()
    {
        super();
    }


    public AvlIndex( String attributeId )
    {
        super( attributeId );
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
    }


    public void add( K attrVal, Long id ) throws Exception
    {
        forward.put( getNormalized( attrVal ), id );
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
        return forward.count( getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( K attrVal, Long id ) throws Exception
    {
        forward.remove( getNormalized( attrVal ), id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( K attrVal ) throws Exception
    {
        return forward.has( getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( K attrVal, Long id ) throws Exception
    {
        return forward.has( getNormalized( attrVal ), id );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public IndexCursor<K, O, Long> forwardCursor() throws Exception
    {
        return new IndexCursorAdaptor( forward.cursor(), true );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public IndexCursor<K, O, Long> forwardCursor( K key ) throws Exception
    {
        return new IndexCursorAdaptor( forward.cursor( key ), true );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardGreaterOrEq( K attrVal ) throws Exception
    {
        return forward.hasGreaterOrEqual( getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardGreaterOrEq( K attrVal, Long id ) throws Exception
    {
        return forward.hasGreaterOrEqual( getNormalized( attrVal ), id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardLessOrEq( K attrVal ) throws Exception
    {
        return forward.hasLessOrEqual( getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardLessOrEq( K attrVal, Long id ) throws Exception
    {
        return forward.hasLessOrEqual( getNormalized( attrVal ), id );
    }


    /**
     * {@inheritDoc}
     */
    public Long forwardLookup( K attrVal ) throws Exception
    {
        return forward.get( getNormalized( attrVal ) );
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
    @SuppressWarnings("unchecked")
    public K getNormalized( K attrVal ) throws Exception
    {
        if ( attrVal instanceof Long )
        {
            return attrVal;
        }

        if ( attrVal instanceof String )
        {
            return ( K ) normalizer.normalize( ( String ) attrVal );
        }
        else
        {
            return ( K ) normalizer.normalize( new BinaryValue( ( byte[] ) attrVal ) ).getValue();
        }
    }


    /**
     * {@inheritDoc}
     */
    public int greaterThanCount( K attrVal ) throws Exception
    {
        return forward.greaterThanCount( getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public int lessThanCount( K attrVal ) throws Exception
    {
        return forward.lessThanCount( getNormalized( attrVal ) );
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
    public void sync() throws Exception
    {
    }
}
