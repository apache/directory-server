/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import jdbm.RecordManager;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.IndexComparator;
import org.apache.directory.server.core.partition.impl.btree.IndexEnumeration;
import org.apache.directory.server.core.schema.SerializableComparator;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.SynchronizedLRUMap;


/**
 * A Jdbm based index implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmIndex implements Index
{
    /**  */
    public static final String FORWARD_BTREE = "_forward";
    /** */
    public static final String REVERSE_BTREE = "_reverse";

    /** */
    private AttributeType attribute;
    /** */
    private JdbmTable forward = null;
    /** */
    private JdbmTable reverse = null;
    /** */
    private RecordManager recMan = null;
    /** 
     * @todo I don't think the keyCache is required anymore since the normalizer
     * will cache values for us.
     */
    private SynchronizedLRUMap keyCache = null;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an Index using an existing record manager based on a file.  The
     * index table B+Tree are created and saved within this file rather than 
     * creating a new file.
     *
     * @param attribute the attribute specification to base this index on
     * @param recMan the record manager
     * @throws NamingException if we fail to create B+Trees using recMan
     */
    public JdbmIndex( AttributeType attribute, RecordManager recMan )
        throws NamingException
    {
        this.attribute = attribute;
        keyCache = new SynchronizedLRUMap( 1000 );
        this.recMan = recMan;
        initTables();
    }
    

    public JdbmIndex( AttributeType attribute, File wkDirPath )
        throws NamingException
    {
        File file = new File( wkDirPath.getPath() + File.separator 
            + attribute.getName() );
        this.attribute = attribute;
        keyCache = new SynchronizedLRUMap( 1000 );

        try 
        {
            String path = file.getAbsolutePath();
            BaseRecordManager base = new BaseRecordManager( path );
            base.disableTransactions();
            recMan = new CacheRecordManager( base , new MRU( 1000 ) );
        } 
        catch ( IOException e ) 
        {
            NamingException ne = new NamingException(
                "Could not initialize the record manager" );
            ne.setRootCause( e );
            throw ne;
        }

        initTables();
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
        forward = new JdbmTable( attribute.getName() + FORWARD_BTREE,
            true, recMan, new IndexComparator( comp, true ) );
        
        /*
         * Now the reverse map stores the primary key into the master table as
         * the key and the values of attributes as the value.  If an attribute
         * is single valued according to its specification based on a schema 
         * then duplicate keys should not be allowed within the reverse table.
         */
        reverse = new JdbmTable( attribute.getName() + REVERSE_BTREE,
            ! attribute.isSingleValue(), recMan, 
            new IndexComparator( comp, false ) );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#getAttribute()
     */
    public AttributeType getAttribute()
    {
        return attribute;
    }


    // ------------------------------------------------------------------------
    // Scan Count Methods
    // ------------------------------------------------------------------------


    /**
     * @see Index#count()
     */
    public int count()
        throws NamingException
    {
        return forward.count();
    }


    /**
     * @see Index#count(java.lang.Object)
     */
    public int count( Object attrVal )
        throws NamingException
    {
        return forward.count( getNormalized( attrVal ) );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#count(java.lang.Object, boolean)
     */
    public int count( Object attrVal, boolean isGreaterThan )
        throws NamingException
    {
        return forward.count( getNormalized( attrVal ), isGreaterThan );
    }


    // ------------------------------------------------------------------------
    // Forward and Reverse Lookups
    // ------------------------------------------------------------------------


    /**
     * @see Index#forwardLookup(java.lang.Object)
     */
    public BigInteger forwardLookup( Object attrVal )
        throws NamingException
    {
        return ( BigInteger ) forward.get( getNormalized( attrVal ) );
    }


    /**
     * @see Index#reverseLookup(java.math.BigInteger)
     */
    public Object reverseLookup( BigInteger id )
        throws NamingException
    {
        return reverse.get( id );
    }


    // ------------------------------------------------------------------------
    // Add/Drop Methods
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#add(java.lang.Object,
     * java.math.BigInteger)
     */
    public synchronized void add( Object attrVal, BigInteger id )
        throws NamingException
    {
        forward.put( getNormalized( attrVal ), id );
        reverse.put( id, getNormalized( attrVal ) );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#add(
     * javax.naming.directory.Attribute, java.math.BigInteger)
     */
    public synchronized void add( Attribute attr, BigInteger id ) 
        throws NamingException 
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
     * @see Index#add(
     * javax.naming.directory.Attributes, java.math.BigInteger)
     */
    public synchronized void add( Attributes attrs, BigInteger id ) 
        throws NamingException
    {
        add( attrs.get( attribute.getName() ), id );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#drop(java.lang.Object,
     * java.math.BigInteger)
     */
    public synchronized void drop( Object attrVal, BigInteger id )
        throws NamingException
    {
        forward.remove( getNormalized( attrVal ), id );
        reverse.remove( id, getNormalized( attrVal ) );
    }


    /**
     * @see Index#drop(java.math.BigInteger)
     */
    public void drop( BigInteger entryId ) 
        throws NamingException 
    {
        NamingEnumeration values = reverse.listValues( entryId );
        
        while ( values.hasMore() )
        {
            forward.remove( values.next(), entryId );
        }
        
        reverse.remove( entryId );
    }


    /**
     * @see Index#drop(
     * javax.naming.directory.Attribute, java.math.BigInteger)
     */
    public void drop( Attribute attr, BigInteger id )
        throws NamingException 
    {
        // Can efficiently batch remove from the reverse table 
        NamingEnumeration values = attr.getAll();
        
        // If their are no values in attr this is a request to drop all
        if ( ! values.hasMore() )
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
     * @see org.apache.directory.server.core.partition.impl.btree.Index#drop(
     * javax.naming.directory.Attributes, java.math.BigInteger)
     */
    public void drop( Attributes attrs, BigInteger id )
        throws NamingException 
    {
        drop( attrs.get( attribute.getName() ), id );
    }
        
    
    // ------------------------------------------------------------------------
    // Index Listing Operations
    // ------------------------------------------------------------------------


    /**
     * @see Index#listReverseIndices(BigInteger)
     */
    public IndexEnumeration listReverseIndices( BigInteger id )
        throws NamingException
    {
        return new IndexEnumeration( reverse.listTuples( id ), true );
    }


    /**
     * @see Index#listIndices()
     */
    public IndexEnumeration listIndices()
        throws NamingException
    {
        return new IndexEnumeration( forward.listTuples() );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#listIndices(java.lang.Object)
     */
    public IndexEnumeration listIndices( Object attrVal ) 
        throws NamingException
    {
        return new IndexEnumeration( forward.listTuples( 
            getNormalized( attrVal ) ) );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Index#listIndices(java.lang.Object,
     * boolean)
     */
    public IndexEnumeration listIndices( Object attrVal, 
        boolean isGreaterThan ) throws NamingException
    {
        return new IndexEnumeration( forward.listTuples( 
            getNormalized( attrVal ), isGreaterThan ) );
    }


    /**
     * @see Index#listIndices(org.apache.regexp.RE)
     */
    public IndexEnumeration listIndices( Pattern regex )
        throws NamingException
    {
        return new IndexEnumeration( forward.listTuples(), false, regex );
    }


    /**
     * @see Index#listIndices(org.apache.regexp.RE,
     * java.lang.String)
     */
    public IndexEnumeration listIndices( Pattern regex, String prefix )
        throws NamingException
    {
        return new IndexEnumeration( forward.listTuples(
            getNormalized( prefix ), true ), false, regex );
    }


    // ------------------------------------------------------------------------
    // Value Assertion (a.k.a Index Lookup) Methods //
    // ------------------------------------------------------------------------


    /**
     * @see Index#hasValue(java.lang.Object,
     * java.math.BigInteger)
     */
    public boolean hasValue( Object attrVal, BigInteger id )
        throws NamingException
    {
        return forward.has( getNormalized( attrVal ), id );
    }


    /**
     * @see Index#hasValue(java.lang.Object,
     * java.math.BigInteger, boolean)
     */
    public boolean hasValue( Object attrVal, BigInteger id,
        boolean isGreaterThan )
        throws NamingException
    {
        return forward.has( getNormalized( attrVal ), 
            id, isGreaterThan );
    }


    /**
     * @see Index#hasValue(org.apache.regexp.RE,
     * java.math.BigInteger)
     */
    public boolean hasValue( Pattern regex, BigInteger id )
        throws NamingException
    {
        IndexEnumeration list = new IndexEnumeration( 
            reverse.listTuples( id ), true, regex );
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
    public synchronized void close()
        throws NamingException
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
            NamingException ne = new NamingException( 
                "Exception while closing backend index file for attribute " 
                + attribute.getName() );
            ne.setRootCause( e );
            throw ne;
        }
    }


    /**
     * @see Index#sync()
     */
    public synchronized void sync()
        throws NamingException
    {
        try 
        {
            recMan.commit();
        } 
        catch ( IOException e ) 
        {
            NamingException ne = new NamingException( 
                "Exception while syncing backend index file for attribute " 
                + attribute.getName() );
            ne.setRootCause( e );
            throw ne;
        }
    }


    /**
     * TODO I don't think the keyCache is required anymore since the normalizer
     * will cache values for us.
     */
    public Object getNormalized( Object attrVal )
        throws NamingException
    {
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
