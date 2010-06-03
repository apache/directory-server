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

import jdbm.RecordManager;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.core.partition.impl.btree.LongComparator;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.Tuple;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.comparators.SerializableComparator;
import org.apache.directory.shared.ldap.util.SynchronizedLRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * A Jdbm based index implementation.
 *
 * @org.apache.xbean.XBean
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmIndex<K, O> implements Index<K, O, Long>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmIndex.class.getSimpleName() );

    /**
     * default duplicate limit before duplicate keys switch to using a btree for values
     */
    public static final int DEFAULT_DUPLICATE_LIMIT = 512;

    /**  the key used for the forward btree name */
    public static final String FORWARD_BTREE = "_forward";

    /**  the key used for the reverse btree name */
    public static final String REVERSE_BTREE = "_reverse";

    /** the attribute type resolved for this JdbmIndex */
    protected AttributeType attribute;

    /**
     * the forward btree where the btree key is the value of the indexed attribute and
     * the value of the btree is the entry id of the entry containing an attribute with
     * that value
     */
    protected JdbmTable<K, Long> forward;

    /**
     * the reverse btree where the btree key is the entry id of the entry containing a
     * value for the indexed attribute, and the btree value is the value of the indexed
     * attribute
     */
    protected JdbmTable<Long, K> reverse;

    /**
     * the JDBM record manager for the file containing this index
     */
    protected RecordManager recMan;

    /**
     * the normalized value cache for this index
     * @todo I don't think the keyCache is required anymore since the normalizer
     * will cache values for us.
     */
    protected SynchronizedLRUMap keyCache;

    /** the size (number of index entries) for the cache */
    protected int cacheSize = DEFAULT_INDEX_CACHE_SIZE;

    /**
     * duplicate limit before duplicate keys switch to using a btree for values
     */
    protected int numDupLimit = DEFAULT_DUPLICATE_LIMIT;

    /**
     * the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     */
    protected String attributeId;

    /** whether or not this index has been initialized */
    protected boolean initialized;

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

    public JdbmIndex()
    {
        initialized = false;
    }


    public JdbmIndex( String attributeId )
    {
        initialized = false;
        setAttributeId( attributeId );
    }


    public void init( SchemaManager schemaManager, AttributeType attributeType, File wkDirPath ) throws IOException
    {
        LOG.debug( "Initializing an Index for attribute '{}'", attributeType.getName() );

        keyCache = new SynchronizedLRUMap( cacheSize );
        attribute = attributeType;

        if ( attributeId == null )
        {
            setAttributeId( attribute.getName() );
        }

        if ( this.wkDirPath == null )
        {
            this.wkDirPath = wkDirPath;
        }

        File file = new File( this.wkDirPath.getPath() + File.separator + attribute.getOid() );
        String path = file.getAbsolutePath();
        BaseRecordManager base = new BaseRecordManager( path );
        base.disableTransactions();
        this.recMan = new CacheRecordManager( base, new MRU( cacheSize ) );

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
        FileWriter fw = new FileWriter( new File( this.wkDirPath.getPath() + File.separator + attribute.getOid() + "-" + attribute.getName() + ".txt" ) );
        // write the AttributeType description
        fw.write( attribute.toString() );
        fw.close();
        
        initialized = true;
    }


    /**
     * Initializes the forward and reverse tables used by this Index.
     * 
     * @throws IOException if we cannot initialize the forward and reverse
     * tables
     * @throws NamingException 
     */
    private void initTables( SchemaManager schemaManager ) throws IOException
    {
        SerializableComparator<K> comp;

        MatchingRule mr = attribute.getEquality();

        if ( mr == null )
        {
            throw new IOException( I18n.err( I18n.ERR_574, attribute.getName() ) );
        }

        comp = new SerializableComparator<K>( mr.getOid() );

        /*
         * The forward key/value map stores attribute values to master table 
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */
        LongComparator.INSTANCE.setSchemaManager( schemaManager );
        comp.setSchemaManager( schemaManager );

        forward = new JdbmTable<K, Long>( schemaManager, attribute.getOid() + FORWARD_BTREE, numDupLimit, recMan,
            comp, LongComparator.INSTANCE, null, LongSerializer.INSTANCE );

        /*
         * Now the reverse map stores the primary key into the master table as
         * the key and the values of attributes as the value.  If an attribute
         * is single valued according to its specification based on a schema 
         * then duplicate keys should not be allowed within the reverse table.
         */
        if ( attribute.isSingleValued() )
        {
            reverse = new JdbmTable<Long, K>( schemaManager, attribute.getOid() + REVERSE_BTREE, recMan,
                LongComparator.INSTANCE, LongSerializer.INSTANCE, null );
        }
        else
        {
            reverse = new JdbmTable<Long, K>( schemaManager, attribute.getOid() + REVERSE_BTREE, numDupLimit, recMan,
                LongComparator.INSTANCE, comp, LongSerializer.INSTANCE, null );
        }
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#getAttribute()
     */
    public AttributeType getAttribute()
    {
        return attribute;
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------

    /**
     * Protects configuration properties from being set after initialization.
     *
     * @param property the property to protect
     */
    private void protect( String property )
    {
        if ( initialized )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_575, property ) );
        }
    }


    public boolean isCountExact()
    {
        return false;
    }


    /**
     * Gets the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     *
     * @return configured attribute oid or alias name
     */
    public String getAttributeId()
    {
        return attributeId;
    }


    /**
     * Sets the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     *
     * @param attributeId configured attribute oid or alias name
     */
    public void setAttributeId( String attributeId )
    {
        protect( "attributeId" );
        this.attributeId = attributeId;
    }


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
     * Sets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @param wkDirPath optional working directory path
     */
    public void setWkDirPath( File wkDirPath )
    {
        protect( "wkDirPath" );
        this.wkDirPath = wkDirPath;
    }


    /**
     * Gets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @return optional working directory path 
     */
    public File getWkDirPath()
    {
        return wkDirPath;
    }


    // ------------------------------------------------------------------------
    // Scan Count Methods
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.server.xdbm.Index#count()
     */
    public int count() throws IOException
    {
        return forward.count();
    }


    /**
     * @see Index#count(java.lang.Object)
     */
    public int count( K attrVal ) throws Exception
    {
        return forward.count( getNormalized( attrVal ) );
    }


    public int greaterThanCount( K attrVal ) throws Exception
    {
        return forward.greaterThanCount( getNormalized( attrVal ) );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#lessThanCount(java.lang.Object)
     */
    public int lessThanCount( K attrVal ) throws Exception
    {
        return forward.lessThanCount( getNormalized( attrVal ) );
    }


    // ------------------------------------------------------------------------
    // Forward and Reverse Lookups
    // ------------------------------------------------------------------------

    /**
     * @see Index#forwardLookup(java.lang.Object)
     */
    public Long forwardLookup( K attrVal ) throws Exception
    {
        return forward.get( getNormalized( attrVal ) );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#reverseLookup(Long)
     */
    public K reverseLookup( Long id ) throws Exception
    {
        return reverse.get( id );
    }


    // ------------------------------------------------------------------------
    // Add/Drop Methods
    // ------------------------------------------------------------------------

    /**
     * @see Index#add(Object, Long)
     */
    public synchronized void add( K attrVal, Long id ) throws Exception
    {
        forward.put( getNormalized( attrVal ), id );
        reverse.put( id, getNormalized( attrVal ) );
    }


    /**
     * @see Index#drop(Object,Long)
     */
    public synchronized void drop( K attrVal, Long id ) throws Exception
    {
        forward.remove( getNormalized( attrVal ), id );
        reverse.remove( id, getNormalized( attrVal ) );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( Long entryId ) throws Exception
    {
        // Build a cursor to iterate on all the keys referencing
        // this entryId
        Cursor<Tuple<Long, K>> values = reverse.cursor( entryId );

        while ( values.next() )
        {
            // Remove the Key -> entryId from the index
            forward.remove( values.get().getValue(), entryId );
        }

        // Remove the id -> key from the reverse index
        reverse.remove( entryId );
    }


    // ------------------------------------------------------------------------
    // Index Cursor Operations
    // ------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public IndexCursor<K, O, Long> reverseCursor() throws Exception
    {
        return new IndexCursorAdaptor<K, O, Long>( ( Cursor ) reverse.cursor(), false );
    }


    @SuppressWarnings("unchecked")
    public IndexCursor<K, O, Long> forwardCursor() throws Exception
    {
        return new IndexCursorAdaptor<K, O, Long>( ( Cursor ) forward.cursor(), true );
    }


    @SuppressWarnings("unchecked")
    public IndexCursor<K, O, Long> reverseCursor( Long id ) throws Exception
    {
        return new IndexCursorAdaptor<K, O, Long>( ( Cursor ) reverse.cursor( id ), false );
    }


    @SuppressWarnings("unchecked")
    public IndexCursor<K, O, Long> forwardCursor( K key ) throws Exception
    {
        return new IndexCursorAdaptor<K, O, Long>( ( Cursor ) forward.cursor( key ), true );
    }


    public Cursor<K> reverseValueCursor( Long id ) throws Exception
    {
        return reverse.valueCursor( id );
    }


    public Cursor<Long> forwardValueCursor( K key ) throws Exception
    {
        return forward.valueCursor( key );
    }


    // ------------------------------------------------------------------------
    // Value Assertion (a.k.a Index Lookup) Methods //
    // ------------------------------------------------------------------------
    /**
     * @see Index#forward(Object)
     */
    public boolean forward( K attrVal ) throws Exception
    {
        return forward.has( getNormalized( attrVal ) );
    }


    /**
     * @see Index#forward(Object,Long)
     */
    public boolean forward( K attrVal, Long id ) throws Exception
    {
        return forward.has( getNormalized( attrVal ), id );
    }


    /**
     * @see Index#reverse(Long)
     */
    public boolean reverse( Long id ) throws Exception
    {
        return reverse.has( id );
    }


    /**
     * @see Index#reverse(Long,Object)
     */
    public boolean reverse( Long id, K attrVal ) throws Exception
    {
        return forward.has( getNormalized( attrVal ), id );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#forwardGreaterOrEq(Object)
     */
    public boolean forwardGreaterOrEq( K attrVal ) throws Exception
    {
        return forward.hasGreaterOrEqual( getNormalized( attrVal ) );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#forwardGreaterOrEq(Object, Long)
     */
    public boolean forwardGreaterOrEq( K attrVal, Long id ) throws Exception
    {
        return forward.hasGreaterOrEqual( getNormalized( attrVal ), id );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#forwardLessOrEq(Object)
     */
    public boolean forwardLessOrEq( K attrVal ) throws Exception
    {
        return forward.hasLessOrEqual( getNormalized( attrVal ) );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#forwardLessOrEq(Object, Long)
     */
    public boolean forwardLessOrEq( K attrVal, Long id ) throws Exception
    {
        return forward.hasLessOrEqual( getNormalized( attrVal ), id );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#reverseGreaterOrEq(Long)
     */
    public boolean reverseGreaterOrEq( Long id ) throws Exception
    {
        return reverse.hasGreaterOrEqual( id );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#reverseGreaterOrEq(Long,Object)
     */
    public boolean reverseGreaterOrEq( Long id, K attrVal ) throws Exception
    {
        return reverse.hasGreaterOrEqual( id, getNormalized( attrVal ) );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#reverseLessOrEq(Long)
     */
    public boolean reverseLessOrEq( Long id ) throws Exception
    {
        return reverse.hasLessOrEqual( id );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#reverseLessOrEq(Long,Object)
     */
    public boolean reverseLessOrEq( Long id, K attrVal ) throws Exception
    {
        return reverse.hasLessOrEqual( id, getNormalized( attrVal ) );
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

        recMan.commit();
        recMan.close();
    }


    /**
     * @see Index#sync()
     */
    public synchronized void sync() throws IOException
    {
        recMan.commit();
    }


    /**
     * TODO I don't think the keyCache is required anymore since the normalizer
     * will cache values for us.
     */
    @SuppressWarnings("unchecked")
    public K getNormalized( K attrVal ) throws Exception
    {
        if ( attrVal instanceof Long )
        {
            return attrVal;
        }

        K normalized = ( K ) keyCache.get( attrVal );

        if ( null == normalized )
        {
            if ( attrVal instanceof String )
            {
                normalized = ( K ) attribute.getEquality().getNormalizer().normalize( ( String ) attrVal );
            }
            else
            {
                normalized = ( K ) attribute.getEquality().getNormalizer().normalize(
                    new BinaryValue( ( byte[] ) attrVal ) ).get();
            }

            // Double map it so if we use an already normalized
            // value we can get back the same normalized value.
            // and not have to regenerate a second time.
            keyCache.put( attrVal, normalized );
            keyCache.put( normalized, normalized );
        }

        return normalized;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDupsEnabled()
    {
        return reverse.isDupsEnabled();
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Index<" + attributeId + ">";
    }
}
