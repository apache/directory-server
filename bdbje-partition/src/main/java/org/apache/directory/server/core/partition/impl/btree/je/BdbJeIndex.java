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

package org.apache.directory.server.core.partition.impl.btree.je;


import java.io.IOException;
import java.net.URI;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.ByteArraySerializer;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.ComparatorSerializerMap;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.Serializer;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.StringSerializer;
import org.apache.directory.server.xdbm.AbstractIndex;
import org.apache.directory.server.xdbm.IndexEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Database;


/**
 * Index implementation backed by bdb je.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJeIndex<K> extends AbstractIndex<K, String>
{

    public static final String REVERSE_KEY = "_reverse";

    public static final String FORWARD_KEY = "_forward";

    /** the logger */
    private static final Logger LOG = LoggerFactory.getLogger( BdbJeIndex.class );

    /** flag to indicate if this index was initialized */
    protected boolean initialized;

    /** flag indicating if duplicate values are allowed in reverse database */
    protected boolean revDupsAllowed = true;

    /** flag indicating if duplicate values are allowed in forward datatbase */
    protected boolean fwdDupsAllowed = true;

    /** the forward database */
    protected BdbJeTable<K, String> forward;

    /** the reverse database */
    protected BdbJeTable<String, K> reverse;

    /** the forward database key serializer */
    protected Serializer<K> forwardKeySerializer;

    /** the forward database value serializer */
    protected Serializer<String> forwardValueSerializer;

    /** the reverse database key serializer */
    protected Serializer<String> reverseKeySerializer;

    /** the reverse database value serializer */
    protected Serializer<K> reverseValueSerializer;

    /** the indexed attribute type */
    protected AttributeType attribute;

    /** flag to indicate if the transactions are enabled */
    protected boolean txnEnabled;


    /**
     * Creates a new instance of BdbJeIndex.
     * @param attributeId the attribute's identifier
     */
    public BdbJeIndex( String attributeId, boolean withReverse )
    {
        super( attributeId, withReverse );
        initialized = false;
    }


    public void init( SchemaManager schemaManager, BdbJePartitionEnviroment environment ) throws LdapException
    {
        if ( initialized )
        {
            return;
        }

        attribute = schemaManager.lookupAttributeTypeRegistry( attributeId );

        if ( attribute.isSingleValued() )
        {
            revDupsAllowed = false;
        }

        SerializableComparator<K> atValueComparator = new SerializableComparator<K>( attribute.getEquality().getOid() );

        atValueComparator.setSchemaManager( schemaManager );

        forwardValueSerializer = new StringSerializer();

        if ( !attribute.getSyntax().isHumanReadable() )
        {
            forwardKeySerializer = ( Serializer ) ByteArraySerializer.INSTANCE;
        }
        else
        {
            forwardKeySerializer = ComparatorSerializerMap.getSerializer( schemaManager.lookupComparatorRegistry(
                attribute.getEquality().getOid() ).getClass() );
        }

        Database fdb = environment.createDb( attribute.getOid() + FORWARD_KEY, true );
        forward = new BdbJeTable<>( fdb, schemaManager, atValueComparator, UuidComparator.INSTANCE,
            forwardKeySerializer, new StringSerializer() );
        reverseKeySerializer = forwardValueSerializer;
        reverseValueSerializer = forwardKeySerializer;

        Database rdb = environment.createDb( attribute.getOid() + REVERSE_KEY, revDupsAllowed );
        reverse = new BdbJeTable<>( rdb, schemaManager, UuidComparator.INSTANCE, atValueComparator,
            reverseKeySerializer, reverseValueSerializer );

        txnEnabled = environment.isTransactional();

        initialized = true;
    }


    @Override
    public AttributeType getAttribute()
    {
        return attribute;
    }


    public void setFwdDupsAllowed( boolean fwdDupsAllowed )
    {
        if ( initialized )
        {
            throw new IllegalStateException( "cannot set the fwdDupsAllowed flag after initialization" );
        }

        this.fwdDupsAllowed = fwdDupsAllowed;
    }


    @Override
    public void setAttributeId( String attributeId )
    {
        if ( initialized )
        {
            throw new IllegalStateException( "cannot set the attributeId after initialization" );
        }

        this.attributeId = attributeId;
    }


    // ------------------------------------------------------------------------
    // Add/Drop Methods
    // ------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void add( PartitionTxn partitionTxn, K attrVal, String id ) throws LdapException
    {
        // The pair to be added must exists
        forward.put( partitionTxn, attrVal, id );

        if ( withReverse )
        {
            reverse.put( partitionTxn, id, attrVal );
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void drop( PartitionTxn partitionTxn, K attrVal, String id ) throws LdapException
    {
        // The pair to be removed must exists
        if ( forward.has( partitionTxn, attrVal, id ) )
        {
            forward.remove( partitionTxn, attrVal, id );

            if ( withReverse )
            {
                reverse.remove( partitionTxn, id, attrVal );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void drop( PartitionTxn partitionTxn, String entryId ) throws LdapException
    {
        if ( withReverse )
        {
            if ( isDupsEnabled() )
            {
                // Build a cursor to iterate on all the keys referencing
                // this entryId
                Cursor<Tuple<String, K>> values = reverse.cursor( partitionTxn, entryId );

                try
                {
                    while ( values.next() )
                    {
                        // Remove the Key -> entryId from the index
                        forward.remove( partitionTxn, values.get().getValue(), entryId );
                    }

                    values.close();
                }
                catch ( CursorException | IOException e )
                {
                    throw new LdapOtherException( e.getMessage(), e );
                }
            }
            else
            {
                K key = reverse.get( partitionTxn, entryId );

                forward.remove( partitionTxn, key );
            }

            // Remove the id -> key from the reverse index
            reverse.remove( partitionTxn, entryId );
        }
    }


    // ------------------------------------------------------------------------
    // Index Cursor Operations
    // ------------------------------------------------------------------------


    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, String>> forwardCursor( PartitionTxn partitionTxn ) throws LdapException
    {
        return new IndexCursorAdaptor<>( partitionTxn, ( Cursor ) forward.cursor( partitionTxn ), true );
    }


    public Cursor<IndexEntry<K, String>> forwardCursor( PartitionTxn partitionTxn, K key ) throws LdapException
    {
        return new IndexCursorAdaptor<>( partitionTxn, ( Cursor ) forward.cursor( partitionTxn, key ), true );
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


    public Cursor<String> forwardValueCursor( PartitionTxn partitionTxn, K key ) throws LdapException
    {
        return forward.valueCursor( partitionTxn, key );
    }


    // ------------------------------------------------------------------------
    // Value Assertion (a.k.a Index Lookup) Methods //
    // ------------------------------------------------------------------------
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
    public boolean reverse( PartitionTxn partitionTxn, String id, K attrVal ) throws LdapException
    {
        return forward.has( partitionTxn, attrVal, id );
    }


    // ------------------------------------------------------------------------
    // Maintenance Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close( PartitionTxn partitionTxn ) throws LdapException, IOException
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


    // ------------------------------------------------------------------------
    // Scan Count Methods
    // ------------------------------------------------------------------------
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
        return forward.lessThanCount( partitionTxn, attrVal );
    }


    // ------------------------------------------------------------------------
    // Forward and Reverse Lookups
    // ------------------------------------------------------------------------

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


    @Override
    public void setWkDirPath( URI wkDirPath )
    {
    }


    @Override
    public URI getWkDirPath()
    {
        return null;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Index<" + attributeId + ">";
    }
}
