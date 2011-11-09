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
import java.util.Comparator;
import java.util.UUID;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.core.api.partition.index.AbstractIndex;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.ForwardIndexComparator;
import org.apache.directory.server.core.api.partition.index.ReverseIndexComparator;
import org.apache.directory.server.core.api.partition.index.Table;
import org.apache.directory.server.core.api.partition.index.UUIDComparator;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.LdapComparator;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.Normalizer;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;


/**
 * An Index backed by an AVL Tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlIndex<K> extends AbstractIndex<K>
{
    protected Normalizer normalizer;
    protected AvlTable<K, UUID> forward;
    protected AvlTable<UUID, K> reverse; 
    
    /** Forward index entry comparator */
    protected ForwardIndexComparator<K> fIndexEntryComparator;
    
    /** Reverse index entry comparator */
    protected ReverseIndexComparator<K> rIndexEntryComparator;
    


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

        Comparator comp = ( LdapComparator<K> ) mr.getLdapComparator();
        ( ( LdapComparator<K> )comp ).setSchemaManager( schemaManager );
        
        String attributeOid = attributeType.getOid();
        
        if ( attributeOid.equals( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID ) ||
            attributeOid.equals( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID ) ||
            attributeOid.equals( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ) ||
            attributeOid.equals( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ) )
        {
            comp = UUIDComparator.INSTANCE;
        }
                  
        /*
         * The forward key/value map stores attribute values to master table 
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */
        forward = new AvlTable<K, UUID>( attributeType.getName(), comp, UUIDComparator.INSTANCE, true );

        /*
         * Now the reverse map stores the primary key into the master table as
         * the key and the values of attributes as the value.  If an attribute
         * is single valued according to its specification based on a schema 
         * then duplicate keys should not be allowed within the reverse table.
         */
        if ( attributeType.isSingleValued() )
        {
            reverse = new AvlTable<UUID, K>( attributeType.getName(), UUIDComparator.INSTANCE, comp, false );
        }
        else
        {
            reverse = new AvlTable<UUID, K>( attributeType.getName(), UUIDComparator.INSTANCE, comp, true );
        }
        
        fIndexEntryComparator = new ForwardIndexComparator<K>( comp );
        rIndexEntryComparator = new ReverseIndexComparator<K>( comp );
    }


    public void add( K attrVal, UUID id ) throws Exception
    {
        forward.put( getNormalized( attrVal ), id );
        reverse.put( id, getNormalized( attrVal ) );
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
        return forward.count( getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( UUID id ) throws Exception
    {
        Cursor<Tuple<UUID, K>> cursor = reverse.cursor( id );

        while ( cursor.next() )
        {
            Tuple<UUID, K> tuple = cursor.get();
            forward.remove( tuple.getValue(), id );
        }

        reverse.remove( id );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( K attrVal, UUID id ) throws Exception
    {
        forward.remove( getNormalized( attrVal ), id );
        reverse.remove( id, getNormalized( attrVal ) );
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
    public boolean forward( K attrVal, UUID id ) throws Exception
    {
        return forward.has( getNormalized( attrVal ), id );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public IndexCursor<K> forwardCursor() throws Exception
    {
        return new IndexCursorAdaptor( forward.cursor(), true );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public IndexCursor<K> forwardCursor( K key ) throws Exception
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
    public boolean forwardGreaterOrEq( K attrVal, UUID id ) throws Exception
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
    public boolean forwardLessOrEq( K attrVal, UUID id ) throws Exception
    {
        return forward.hasLessOrEqual( getNormalized( attrVal ), id );
    }


    /**
     * {@inheritDoc}
     */
    public UUID forwardLookup( K attrVal ) throws Exception
    {
        return forward.get( getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<UUID> forwardValueCursor( K key ) throws Exception
    {
        return forward.valueCursor( key );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public K getNormalized( K attrVal ) throws Exception
    {
        if ( attrVal instanceof UUID )
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
     * {@inheritDoc}
     */
    public boolean reverse( UUID id ) throws Exception
    {
        return reverse.has( id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverse( UUID id, K attrVal ) throws Exception
    {
        return reverse.has( id, getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public IndexCursor<K> reverseCursor() throws Exception
    {
        return new IndexCursorAdaptor( reverse.cursor(), false );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public IndexCursor<K> reverseCursor( UUID id ) throws Exception
    {
        return new IndexCursorAdaptor( reverse.cursor( id ), false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseGreaterOrEq( UUID id ) throws Exception
    {
        return reverse.hasGreaterOrEqual( id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseGreaterOrEq( UUID id, K attrVal ) throws Exception
    {
        return reverse.hasGreaterOrEqual( id, getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseLessOrEq( UUID id ) throws Exception
    {
        return reverse.hasLessOrEqual( id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseLessOrEq( UUID id, K attrVal ) throws Exception
    {
        return reverse.hasLessOrEqual( id, getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public K reverseLookup( UUID id ) throws Exception
    {
        return reverse.get( id );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<K> reverseValueCursor( UUID id ) throws Exception
    {
        return reverse.valueCursor( id );
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
        return reverse.isDupsEnabled();
    }
    
    public ForwardIndexComparator<K> getForwardIndexEntryComparator()
    {
        return this.fIndexEntryComparator;
    }
    
    public ReverseIndexComparator<K> getReverseIndexEntryComparator()
    {
        return this.rIndexEntryComparator;
    }
   
    /**
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
    }
}
