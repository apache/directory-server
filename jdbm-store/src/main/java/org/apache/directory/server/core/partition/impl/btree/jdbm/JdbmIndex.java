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


import jdbm.RecordManager;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.IndexComparator;
import org.apache.directory.server.core.partition.impl.btree.IndexEnumeration;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.SynchronizedLRUMap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;


/** 
 * A Jdbm based index implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmIndex implements Index
{
    /** default cache size to use */
    public static final int DEFAULT_INDEX_CACHE_SIZE = 100;
    /**
     * default duplicate limit before duplicate keys switch to using a btree for values
     */
    public static final int DEFAULT_DUPLICATE_LIMIT = 512;

    /**  the key used for the forward btree name */
    public static final String FORWARD_BTREE = "_forward";
    /**  the key used for the reverse btree name */
    public static final String REVERSE_BTREE = "_reverse";


    /** the attribute type resolved for this JdbmIndex */
    private AttributeType attribute;
    /**
     * the forward btree where the btree key is the value of the indexed attribute and
     * the value of the btree is the entry id of the entry containing an attribute with
     * that value
     */
    private JdbmTable forward;
    /**
     * the reverse btree where the btree key is the entry id of the entry containing a
     * value for the indexed attribute, and the btree value is the value of the indexed
     * attribute
     */
    private JdbmTable reverse;
    /**
     * the JDBM record manager for the file containing this index
     */
    private RecordManager recMan;
    /**
     * the normalized value cache for this index
     * @todo I don't think the keyCache is required anymore since the normalizer
     * will cache values for us.
     */
    private SynchronizedLRUMap keyCache;
    /** the size (number of index entries) for the cache */
    private int cacheSize = DEFAULT_INDEX_CACHE_SIZE;
    /**
     * duplicate limit before duplicate keys switch to using a btree for values
     */
    private int numDupLimit = DEFAULT_DUPLICATE_LIMIT;
    /**
     * the attribute identifier set at configuration time for this index which may not
     * be the OID but an alias name for the attributeType associated with this Index
     */
    private String attributeId;
    /** whether or not this index has been initialized */
    private boolean initialized;
    /** a customm working directory path when specified in configuration */
    private File wkDirPath;


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
    // ------------------------------------------------------------------------


    public JdbmIndex()
    {
        initialized = false;
    }


    public JdbmIndex( String attributeId )
    {
        initialized = false;
        setAttributeId( attributeId );
    }


    public void init( AttributeType attributeType, File wkDirPath ) throws NamingException
    {
        this.keyCache = new SynchronizedLRUMap( cacheSize );
        this.attribute = attributeType;
        if ( this.wkDirPath ==  null )
        {
            this.wkDirPath = wkDirPath;
        }

        File file = new File( this.wkDirPath.getPath() + File.separator + attribute.getName() );


        try
        {
            String path = file.getAbsolutePath();
            BaseRecordManager base = new BaseRecordManager( path );
            base.disableTransactions();
            this.recMan = new CacheRecordManager( base, new MRU( cacheSize ) );
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException( "Could not initialize the record manager" );
            ne.setRootCause( e );
            throw ne;
        }

        initTables();
        initialized = true;
    }


    /**
     * Initializes the forward and reverse tables used by this Index.
     * 
     * @throws NamingException if we cannot initialize the forward and reverse 
     * tables
     */
    private void initTables() throws NamingException
    {
        SerializableComparator comp;
        comp = new SerializableComparator( attribute.getEquality().getOid() );

        /*
         * The forward key/value map stores attribute values to master table 
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */
        forward = new JdbmTable( 
            attribute.getName() + FORWARD_BTREE, 
            true,
            numDupLimit,
            recMan, 
            new IndexComparator( comp, true ),
            null, null );
            //LongSerializer.INSTANCE );

        /*
         * Now the reverse map stores the primary key into the master table as
         * the key and the values of attributes as the value.  If an attribute
         * is single valued according to its specification based on a schema 
         * then duplicate keys should not be allowed within the reverse table.
         */
        reverse = new JdbmTable( 
            attribute.getName() + REVERSE_BTREE, 
            !attribute.isSingleValue(),
            numDupLimit,
            recMan,
            new IndexComparator( comp, false ),
            null, //LongSerializer.INSTANCE,
            null);
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#getAttribute()
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
            throw new IllegalStateException( "The " + property
                    + " property for an index cannot be set after it has been initialized." );
        }
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
     * @see Index#count()
     */
    public int count() throws NamingException
    {
        return forward.count();
    }


    /**
     * @see Index#count(java.lang.Object)
     */
    public int count( Object attrVal ) throws NamingException
    {
        return forward.count( getNormalized( attrVal ) );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#count(java.lang.Object, boolean)
     */
    public int count( Object attrVal, boolean isGreaterThan ) throws NamingException
    {
        return forward.count( getNormalized( attrVal ), isGreaterThan );
    }


    // ------------------------------------------------------------------------
    // Forward and Reverse Lookups
    // ------------------------------------------------------------------------


    /**
     * @see Index#forwardLookup(java.lang.Object)
     */
    public Long forwardLookup( Object attrVal ) throws NamingException
    {
        return ( Long ) forward.get( getNormalized( attrVal ) );
    }


    /**
     * @see Index#reverseLookup(Object)
     */
    public Object reverseLookup( Object id ) throws NamingException
    {
        return reverse.get( id );
    }


    // ------------------------------------------------------------------------
    // Add/Drop Methods
    // ------------------------------------------------------------------------


    /**
     * @see Index#add(Object,Object)
     */
    public synchronized void add( Object attrVal, Object id ) throws NamingException
    {
        forward.put( getNormalized( attrVal ), id );
        reverse.put( id, getNormalized( attrVal ) );
    }


    /**
     * @see Index#add(Attribute, Object)
     */
    public synchronized void add( Attribute attr, Object id ) throws NamingException
    {
        // Can efficiently batch add to the reverse table 
        NamingEnumeration values = attr.getAll();
        reverse.put( id, values );

        // Have no choice but to add each value individually to forward table
        values = attr.getAll();
        while ( values.hasMore() )
        {
            forward.put( values.next(), id );
        }
    }


    /**
     * @see Index#add(Attributes, Object)
     */
    public synchronized void add( Attributes attrs, Object id ) throws NamingException
    {
        add( AttributeUtils.getAttribute( attrs, attribute ), id );
    }


    /**
     * @see Index#drop(Object,Object)
     */
    public synchronized void drop( Object attrVal, Object id ) throws NamingException
    {
        forward.remove( getNormalized( attrVal ), id );
        reverse.remove( id, getNormalized( attrVal ) );
    }


    /**
     * @see Index#drop(Object)
     */
    public void drop( Object entryId ) throws NamingException
    {
        NamingEnumeration values = reverse.listValues( entryId );

        while ( values.hasMore() )
        {
            forward.remove( values.next(), entryId );
        }

        reverse.remove( entryId );
    }


    /**
     * @see Index#drop(Attribute, Object)
     */
    public void drop( Attribute attr, Object id ) throws NamingException
    {
        // Can efficiently batch remove from the reverse table 
        NamingEnumeration values = attr.getAll();

        // If their are no values in attr this is a request to drop all
        if ( !values.hasMore() )
        {
            drop( id );
            return;
        }

        reverse.remove( id, values );

        // Have no choice but to remove values individually from forward table
        values = attr.getAll();
        while ( values.hasMore() )
        {
            forward.remove( values.next(), id );
        }
    }


    /**
     * @see Index#drop(Attributes, Object)
     */
    public void drop( Attributes attrs, Object id ) throws NamingException
    {
        drop( AttributeUtils.getAttribute( attrs, attribute ), id );
    }


    // ------------------------------------------------------------------------
    // Index Listing Operations
    // ------------------------------------------------------------------------


    /**
     * @see Index#listReverseIndices(Object)
     */
    public IndexEnumeration listReverseIndices( Object id ) throws NamingException
    {
        return new IndexEnumeration<Tuple>( reverse.listTuples( id ), true );
    }


    /**
     * @see Index#listIndices()
     */
    public IndexEnumeration listIndices() throws NamingException
    {
        return new IndexEnumeration<Tuple>( forward.listTuples() );
    }


    /**
     * @see Index#listIndices(Object)
     */
    public IndexEnumeration listIndices( Object attrVal ) throws NamingException
    {
        return new IndexEnumeration<Tuple>( forward.listTuples( getNormalized( attrVal ) ) );
    }


    /**
     * @see Index#listIndices(Object,boolean)
     */
    public IndexEnumeration<Tuple> listIndices( Object attrVal, boolean isGreaterThan ) throws NamingException
    {
        return new IndexEnumeration<Tuple>( forward.listTuples( getNormalized( attrVal ), isGreaterThan ) );
    }


    /**
     * @see Index#listIndices(Pattern)
     */
    public IndexEnumeration<Tuple> listIndices( Pattern regex ) throws NamingException
    {
        return new IndexEnumeration<Tuple>( forward.listTuples(), false, regex );
    }


    /**
     * @see Index#listIndices(Pattern,String)
     */
    public IndexEnumeration<Tuple> listIndices( Pattern regex, String prefix ) throws NamingException
    {
        return new IndexEnumeration<Tuple>( forward.listTuples( getNormalized( prefix ), true ), false, regex );
    }


    // ------------------------------------------------------------------------
    // Value Assertion (a.k.a Index Lookup) Methods //
    // ------------------------------------------------------------------------

    
    /**
     * @see Index#hasValue(java.lang.Object,
     * Object)
     */
    public boolean hasValue( Object attrVal, Object id ) throws NamingException
    {
        return forward.has( getNormalized( attrVal ), id );
    }


    /**
     * @see Index#hasValue(java.lang.Object,
     * Object, boolean)
     */
    public boolean hasValue( Object attrVal, Object id, boolean isGreaterThan ) throws NamingException
    {
        return forward.has( getNormalized( attrVal ), id, isGreaterThan );
    }


    /**
     * @see Index#hasValue(Pattern,Object)
     */
    public boolean hasValue( Pattern regex, Object id ) throws NamingException
    {
        IndexEnumeration<Tuple> list = new IndexEnumeration<Tuple>( reverse.listTuples( id ), true, regex );
        boolean hasValue = list.hasMore();
        list.close();
        return hasValue;
    }


    // ------------------------------------------------------------------------
    // Maintenance Methods 
    // ------------------------------------------------------------------------

    
    /**
     * @see Index#close()
     */
    public synchronized void close() throws NamingException
    {
        try
        {
            forward.close();
            reverse.close();
            recMan.commit();
            recMan.close();
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException( "Exception while closing backend index file for attribute "
                + attribute.getName() );
            ne.setRootCause( e );
            throw ne;
        }
    }


    /**
     * @see Index#sync()
     */
    public synchronized void sync() throws NamingException
    {
        try
        {
            recMan.commit();
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException( "Exception while syncing backend index file for attribute "
                + attribute.getName() );
            ne.setRootCause( e );
            throw ne;
        }
    }


    /**
     * TODO I don't think the keyCache is required anymore since the normalizer
     * will cache values for us.
     */
    public Object getNormalized( Object attrVal ) throws NamingException
    {
        if ( attrVal instanceof Long )
        {
            return attrVal;
        }
        
        Object normalized = keyCache.get( attrVal );

        if ( null == normalized )
        {
            normalized = attribute.getEquality().getNormalizer().normalize( attrVal );

            // Double map it so if we use an already normalized
            // value we can get back the same normalized value.
            // and not have to regenerate a second time.
            keyCache.put( attrVal, normalized );
            keyCache.put( normalized, normalized );
        }

        return normalized;
    }
}
