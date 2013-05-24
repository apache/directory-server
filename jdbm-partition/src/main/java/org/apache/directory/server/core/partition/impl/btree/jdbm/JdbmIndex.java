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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import jdbm.RecordManager;
import jdbm.helper.ByteArraySerializer;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;
import jdbm.recman.TransactionManager;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndex;
import org.apache.directory.server.xdbm.EmptyIndexCursor;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Jdbm based index implementation. It creates an Index for a give AttributeType.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmIndex<K, V> extends AbstractIndex<K, V, String>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmIndex.class );

    /**
     * default duplicate limit before duplicate keys switch to using a btree for values
     */
    public static final int DEFAULT_DUPLICATE_LIMIT = 512;

    /**  the key used for the forward btree name */
    public static final String FORWARD_BTREE = "_forward";

    /**  the key used for the reverse btree name */
    public static final String REVERSE_BTREE = "_reverse";

    /**
     * the forward btree where the btree key is the value of the indexed attribute and
     * the value of the btree is the entry id of the entry containing an attribute with
     * that value
     */
    protected JdbmTable<K, String> forward;

    /**
     * the reverse btree where the btree key is the entry id of the entry containing a
     * value for the indexed attribute, and the btree value is the value of the indexed
     * attribute
     */
    protected JdbmTable<String, K> reverse;

    /**
     * the JDBM record manager for the file containing this index
     */
    protected RecordManager recMan;

    /**
     * duplicate limit before duplicate keys switch to using a btree for values
     */
    protected int numDupLimit = DEFAULT_DUPLICATE_LIMIT;

    /** a custom working directory path when specified in configuration */
    protected File wkDirPath;


    /*
     * NOTE: Duplicate Key Limit
     *
     * Jdbm cannot store duplicate keys: meaning it cannot have more than one value
     * for the same key in the btree.  Thus as a workaround we stuff values for the
     * same key into a TreeSet.  This is only effective up to some threshold after
     * which we run into problems with serialization on and off disk.  A threshold
     * is used to determine when to switch from using a TreeSet to start using another
     * btree in the same index file just for the values.  This value only btree just
     * has keys populated without a value for it's btree entries. When the switch
     * occurs the value for the key in the index btree contains a pointer to the
     * btree containing it's values.
     *
     * This numDupLimit is the threshold at which we switch from using in memory
     * containers for values of the same key to using a btree for those values
     * instead with indirection.
     */

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ----------------------------------------------------------------------
    /**
     * Creates a JdbmIndex instance for a give AttributeId
     */
    public JdbmIndex( String attributeId, boolean withReverse )
    {
        super( attributeId, withReverse );

        initialized = false;
    }


    /**
     * Initialize the index for an Attribute, with a specific working directory (may be null).
     * 
     * @param schemaManager The schemaManager to use to get back the Attribute
     * @param attributeType The attributeType this index is created for
     * @throws IOException If the initialization failed
     */
    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws IOException
    {
        LOG.debug( "Initializing an Index for attribute '{}'", attributeType.getName() );

        this.attributeType = attributeType;

        if ( attributeId == null )
        {
            setAttributeId( attributeType.getName() );
        }

        if ( this.wkDirPath == null )
        {
            NullPointerException e = new NullPointerException( "The index working directory has not be set" );

            throw e;
        }

        String path = new File( this.wkDirPath, attributeType.getOid() ).getAbsolutePath();

        BaseRecordManager base = new BaseRecordManager( path );
        TransactionManager transactionManager = base.getTransactionManager();
        transactionManager.setMaximumTransactionsInLog( 2000 );

        recMan = new CacheRecordManager( base, new MRU( DEFAULT_INDEX_CACHE_SIZE ) );

        try
        {
            initTables( schemaManager );
        }
        catch ( IOException e )
        {
            // clean up
            close();
            throw e;
        }

        // finally write a text file in the format <OID>-<attribute-name>.txt
        FileWriter fw = new FileWriter( new File( path + "-" + attributeType.getName() + ".txt" ) );
        // write the AttributeType description
        fw.write( attributeType.toString() );
        fw.close();

        initialized = true;
    }


    /**
     * Initializes the forward and reverse tables used by this Index.
     * 
     * @param schemaManager The server schemaManager
     * @throws IOException if we cannot initialize the forward and reverse
     * tables
     */
    private void initTables( SchemaManager schemaManager ) throws IOException
    {
        SerializableComparator<K> comp;

        MatchingRule mr = attributeType.getEquality();

        if ( mr == null )
        {
            throw new IOException( I18n.err( I18n.ERR_574, attributeType.getName() ) );
        }

        comp = new SerializableComparator<K>( mr.getOid() );

        /*
         * The forward key/value map stores attribute values to master table
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */
        UuidComparator.INSTANCE.setSchemaManager( schemaManager );
        comp.setSchemaManager( schemaManager );

        if ( mr.getSyntax().isHumanReadable() )
        {
            forward = new JdbmTable<K, String>( schemaManager, attributeType.getOid() + FORWARD_BTREE, numDupLimit,
                recMan,
                comp, UuidComparator.INSTANCE, StringSerializer.INSTANCE, UuidSerializer.INSTANCE );
        }
        else
        {
            forward = new JdbmTable<K, String>( schemaManager, attributeType.getOid() + FORWARD_BTREE, numDupLimit,
                recMan,
                comp, UuidComparator.INSTANCE, new ByteArraySerializer(), UuidSerializer.INSTANCE );
        }

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
                reverse = new JdbmTable<String, K>( schemaManager, attributeType.getOid() + REVERSE_BTREE, recMan,
                    UuidComparator.INSTANCE, UuidSerializer.INSTANCE, null );
            }
            else
            {
                reverse = new JdbmTable<String, K>( schemaManager, attributeType.getOid() + REVERSE_BTREE, numDupLimit,
                    recMan,
                    UuidComparator.INSTANCE, comp, UuidSerializer.INSTANCE, null );
            }
        }
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------
    /**
     * Gets the threshold at which point duplicate keys use btree indirection to store
     * their values.
     *
     * @return the threshold for storing a keys values in another btree
     */
    public int getNumDupLimit()
    {
        return numDupLimit;
    }


    /**
     * Sets the threshold at which point duplicate keys use btree indirection to store
     * their values.
     *
     * @param numDupLimit the threshold for storing a keys values in another btree
     */
    public void setNumDupLimit( int numDupLimit )
    {
        protect( "numDupLimit" );
        this.numDupLimit = numDupLimit;
    }


    /**
     * Sets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @param wkDirPath optional working directory path
     */
    public void setWkDirPath( URI wkDirPath )
    {
        //.out.println( "IDX Defining a WorkingDir : " + wkDirPath );
        protect( "wkDirPath" );
        this.wkDirPath = new File( wkDirPath );
    }


    /**
     * Gets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @return optional working directory path
     */
    public URI getWkDirPath()
    {
        return wkDirPath != null ? wkDirPath.toURI() : null;
    }


    // ------------------------------------------------------------------------
    // Scan Count Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public int count() throws IOException
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


    public int greaterThanCount( K attrVal ) throws Exception
    {
        return forward.greaterThanCount( attrVal );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#lessThanCount(java.lang.Object)
     */
    public int lessThanCount( K attrVal ) throws Exception
    {
        return forward.lessThanCount( attrVal );
    }


    // ------------------------------------------------------------------------
    // Forward and Reverse Lookups
    // ------------------------------------------------------------------------

    /**
     * @see Index#forwardLookup(java.lang.Object)
     */
    public String forwardLookup( K attrVal ) throws Exception
    {
        return forward.get( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public K reverseLookup( String id ) throws LdapException
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


    // ------------------------------------------------------------------------
    // Add/Drop Methods
    // ------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void add( K attrVal, String id ) throws Exception
    {
        // The pair to be removed must exists
        forward.put( attrVal, id );

        if ( withReverse )
        {
            reverse.put( id, attrVal );
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void drop( K attrVal, String id ) throws Exception
    {
        // The pair to be removed must exists
        if ( forward.has( attrVal, id ) )
        {
            forward.remove( attrVal, id );

            if ( withReverse )
            {
                reverse.remove( id, attrVal );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void drop( String entryId ) throws Exception
    {
        if ( withReverse )
        {
            if ( isDupsEnabled() )
            {
                // Build a cursor to iterate on all the keys referencing
                // this entryId
                Cursor<Tuple<String, K>> values = reverse.cursor( entryId );

                while ( values.next() )
                {
                    // Remove the Key -> entryId from the index
                    forward.remove( values.get().getValue(), entryId );
                }

                values.close();
            }
            else
            {
                K key = reverse.get( entryId );

                forward.remove( key );
            }

            // Remove the id -> key from the reverse index
            reverse.remove( entryId );
        }
    }


    // ------------------------------------------------------------------------
    // Index Cursor Operations
    // ------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, String>> reverseCursor() throws Exception
    {
        if ( withReverse )
        {
            return new IndexCursorAdaptor<K>( ( Cursor ) reverse.cursor(), false );
        }
        else
        {
            return new EmptyIndexCursor<K>();
        }
    }


    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, String>> forwardCursor() throws LdapException
    {
        return new IndexCursorAdaptor<K>( ( Cursor ) forward.cursor(), true );
    }


    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, String>> reverseCursor( String id ) throws Exception
    {
        if ( withReverse )
        {
            return new IndexCursorAdaptor<K>( ( Cursor ) reverse.cursor( id ), false );
        }
        else
        {
            return new EmptyIndexCursor<K>();
        }
    }


    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, String>> forwardCursor( K key ) throws Exception
    {
        return new IndexCursorAdaptor<K>( ( Cursor ) forward.cursor( key ), true );
    }


    public Cursor<K> reverseValueCursor( String id ) throws Exception
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


    public Cursor<String> forwardValueCursor( K key ) throws Exception
    {
        return forward.valueCursor( key );
    }


    // ------------------------------------------------------------------------
    // Value Assertion (a.k.a Index Lookup) Methods //
    // ------------------------------------------------------------------------
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
    public boolean forward( K attrVal, String id ) throws LdapException
    {
        return forward.has( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverse( String id ) throws Exception
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
    public boolean reverse( String id, K attrVal ) throws Exception
    {
        return forward.has( attrVal, id );
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
    public boolean forwardGreaterOrEq( K attrVal, String id ) throws Exception
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
    public boolean forwardLessOrEq( K attrVal, String id ) throws Exception
    {
        return forward.hasLessOrEqual( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseGreaterOrEq( String id ) throws Exception
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
    public boolean reverseGreaterOrEq( String id, K attrVal ) throws LdapException
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
    public boolean reverseLessOrEq( String id ) throws Exception
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
    public boolean reverseLessOrEq( String id, K attrVal ) throws Exception
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


    // ------------------------------------------------------------------------
    // Maintenance Methods
    // ------------------------------------------------------------------------
    /**
     * @see org.apache.directory.server.xdbm.Index#close()
     */
    public synchronized void close() throws IOException
    {
        if ( forward != null )
        {
            forward.close();
        }

        if ( reverse != null )
        {
            reverse.close();
        }

        commit( recMan );
        recMan.close();
    }


    /**
     * @see Index#sync()
     */
    public synchronized void sync() throws IOException
    {
        // Commit
        recMan.commit();

        // And flush the journal
        if ( ( commitNumber.get() % 4000 ) == 0 )
        {
            BaseRecordManager baseRecordManager = null;

            if ( recMan instanceof CacheRecordManager )
            {
                baseRecordManager = ( ( BaseRecordManager ) ( ( CacheRecordManager ) recMan ).getRecordManager() );
            }
            else
            {
                baseRecordManager = ( ( BaseRecordManager ) recMan );
            }

            baseRecordManager.getTransactionManager().synchronizeLog();
        }
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
     * Commit the modification on disk
     * 
     * @param recordManager The recordManager used for the commit
     */
    private void commit( RecordManager recordManager ) throws IOException
    {
        if ( commitNumber.incrementAndGet() % 2000 == 0 )
        {
            sync();
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Index<" + attributeId + ">";
    }
}
