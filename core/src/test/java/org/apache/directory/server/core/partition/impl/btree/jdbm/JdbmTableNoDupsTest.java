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
import java.io.Serializable;
import java.math.BigInteger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.partition.impl.btree.TupleRenderer;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.shared.ldap.util.ArrayEnumeration;
import org.apache.directory.shared.ldap.util.BigIntegerComparator;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;

import junit.framework.TestCase;


/**
 * Tests for JdbmTable.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmTableNoDupsTest extends TestCase implements Serializable
{
    private static final long serialVersionUID = 1L;
    private transient File tempFile = null;
    private transient RecordManager rm = null;
    private final BigIntegerComparator biComparator = new BigIntegerComparator();
    private final SerializableComparator serializableComparator = new SerializableComparator( "integerMatchingRule" )
    {
        private static final long serialVersionUID = 1L;

        public int compare( Object o1, Object o2 )
        {
            return biComparator.compare( o1, o2 );
        }
    };


    transient JdbmTable table;


    /**
     * Here's what the table looks like:
     * <pre>
     * .-.-.
     * |1|1|
     * |2|1|
     * |4|1|
     * |5|1|
     * .-.-. 
     * </pre>
     */
    public void setUp() throws Exception
    {
        tempFile = File.createTempFile( "jdbm", "test" );
        rm = new BaseRecordManager( tempFile.getAbsolutePath() );

        // make sure the table does not use duplicates
        table = new JdbmTable( "test", rm, serializableComparator );

        table.put( new BigInteger( "1" ), BigInteger.ONE );
        table.put( new BigInteger( "2" ), BigInteger.ONE );
        table.put( new BigInteger( "4" ), BigInteger.ONE );
        table.put( new BigInteger( "5" ), BigInteger.ONE );
    }

    
    public void testCatchAll() throws Exception
    {
        assertFalse( table.isDupsEnabled() );
        assertFalse( table.isSortedDupsEnabled() );
        assertEquals( "test", table.getName() );
        assertNotNull( table.getComparator() );
        assertNull( table.getRenderer() );
        table.setRenderer( new TupleRenderer() {
            public String getKeyString( Object key )
            {
                return null;
            }
            public String getValueString( Object value )
            {
                return null;
            }} );
        assertNotNull( table.getRenderer() );
        table.sync();
        table.close();

        table = new JdbmTable( "test", rm, serializableComparator );
    }
    

    /**
     * Tests the has() methods for correct behavoir:
     * <ul>
     *   <li>has(Object)</li>
     *   <li>has(Object, boolean)</li>
     *   <li>has(Object, Object)</li>
     *   <li>has(Object, Object, boolean)</li>
     * </ul>
     *
     * @throws NamingException
     */
    public void testHas() throws Exception
    {
        // test the has( Object ) method
        assertTrue( table.has( BigInteger.ONE ) );
        assertTrue( table.has( new BigInteger("2") ) );
        assertTrue( table.has( new BigInteger("4") ) );
        assertTrue( table.has( new BigInteger("5") ) );
        assertFalse( table.has( new BigInteger("3") ) );
        assertFalse( table.has( BigInteger.ZERO ) );
        assertFalse( table.has( new BigInteger( "999" ) ) );

        // test the has( Object, Object ) method
        assertTrue( table.has( BigInteger.ONE, BigInteger.ONE ) );
        assertTrue( table.has( new BigInteger("2"), BigInteger.ONE ) );
        assertTrue( table.has( new BigInteger("4"), BigInteger.ONE ) );
        assertTrue( table.has( new BigInteger("5"), BigInteger.ONE ) );
        assertFalse( table.has( new BigInteger("5"), BigInteger.ZERO ) );
        assertFalse( table.has( new BigInteger("3"), BigInteger.ONE ) );
        assertFalse( table.has( BigInteger.ONE, new BigInteger("999") ) );
        assertFalse( table.has( new BigInteger( "999" ), BigInteger.ONE ) );

        // test the has( Object, boolean ) method
        assertFalse( table.has( BigInteger.ZERO, false ) ); // we do not have a key less than or equal to 0
        assertTrue( table.has( BigInteger.ONE, false ) ); // we do have a key less than or equal to 1
        assertTrue( table.has( BigInteger.ZERO, true ) ); // we do have a key greater than or equal to 0
        assertTrue( table.has( BigInteger.ONE, true ) ); // we do have a key greater than or equal to 1
        assertTrue( table.has( new BigInteger( "5" ), true ) ); // we do have a key greater than or equal to 5
        assertFalse( table.has( new BigInteger( "6" ), true ) ); // we do NOT have a key greater than or equal to 11
        assertFalse( table.has( new BigInteger( "999" ), true ) ); // we do NOT have a key greater than or equal to 12

        // test the has( Object, Object, boolean ) method
        try
        {
            table.has( BigInteger.ONE, BigInteger.ZERO, true );
        }
        catch ( UnsupportedOperationException usoe )
        {
            
        }
    }
    
    
    /**
     * Tests the count() methods for correct behavoir:
     * <ul>
     *   <li>count()</li>
     *   <li>count(Object)</li>
     *   <li>count(Object, boolean)</li>
     * </ul>
     * 
     * @throws Exception
     */
    public void testCount() throws Exception
    {
        // test the count() method
        assertEquals( 4, table.count() );
        
        // test the count(Object) method
        assertEquals( 1, table.count( BigInteger.ONE ) );
        assertEquals( 0, table.count( BigInteger.ZERO ) );
        assertEquals( 1, table.count( new BigInteger( "2" ) ) );
        
        // test the count( Object, boolean ) method 
        // note for speed this count method returns the same as count()
        assertEquals( table.count(), table.count( BigInteger.ONE, true ) );
    }
    
    
    /**
     * Tests the get() method for correct behavoir.
     * 
     * @throws Exception
     */
    public void testGet() throws Exception
    {
        assertEquals( BigInteger.ONE, table.get( BigInteger.ONE ) );
        assertEquals( BigInteger.ONE, table.get( new BigInteger( "2" ) ) );
        assertEquals( null, table.get( new BigInteger( "3" ) ) );
        assertEquals( BigInteger.ONE, table.get( new BigInteger( "4" ) ) );
        assertEquals( BigInteger.ONE, table.get( new BigInteger( "5" ) ) );
    }
    
    
    /**
     * Tests the listTuples() methods for correct behavoir:
     * <ul>
     *   <li>listTuples()</li>
     *   <li>listTuples(Object)</li>
     *   <li>listTuples(Object,boolean)</li>
     *   <li>listTuples(Object,Object,boolean)</li>
     * </ul>
     * 
     * @throws Exception
     */
    public void testListTuples() throws Exception
    {
        Tuple tuple;

        // -------------------------------------------------------------------
        // test the listTuples() method
        // -------------------------------------------------------------------

        NamingEnumeration tuples = table.listTuples();
        
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( BigInteger.ONE, tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );
        
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( new BigInteger( "2" ), tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );
        
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( new BigInteger( "4" ), tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );
        
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( new BigInteger( "5" ), tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );
        
        assertFalse( tuples.hasMore() );

        // -------------------------------------------------------------------
        // test the listTuples(Object) method
        // -------------------------------------------------------------------

        tuples = table.listTuples( BigInteger.ZERO );
        assertFalse( tuples.hasMore() );


        tuples = table.listTuples( new BigInteger( "2" ) );
        assertTrue( tuples.hasMore() );
        tuple = ( Tuple ) tuples.next();
        assertEquals( new BigInteger( "2" ), tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );
        assertFalse( tuples.hasMore() );
        
        // -------------------------------------------------------------------
        // test the listTuples(Object, boolean) method
        // -------------------------------------------------------------------

        tuples = table.listTuples( BigInteger.ZERO, false );
        assertFalse( tuples.hasMore() );


        tuples = table.listTuples( BigInteger.ONE, false );
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( BigInteger.ONE, tuple.getKey() );
        assertEquals( new BigInteger( "1" ), tuple.getValue() );
        assertFalse( tuples.hasMore() );


        tuples = table.listTuples( new BigInteger( "2" ), false );

        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( new BigInteger( "2" ), tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );

        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( BigInteger.ONE, tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );
        assertFalse( tuples.hasMore() );

        
        tuples = table.listTuples( new BigInteger( "6" ), true );
        assertFalse( tuples.hasMore() );

        
        tuples = table.listTuples( new BigInteger( "5" ), true );
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( new BigInteger( "5" ), tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );
        assertFalse( tuples.hasMore() );

        
        tuples = table.listTuples( new BigInteger( "4" ), true );
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( new BigInteger( "4" ), tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );

        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( new BigInteger( "5" ), tuple.getKey() );
        assertEquals( BigInteger.ONE, tuple.getValue() );
        assertFalse( tuples.hasMore() );

        // -------------------------------------------------------------------
        // test the listTuples(Object,Object,boolean) method
        // -------------------------------------------------------------------

        try
        {
            tuples = table.listTuples( BigInteger.ZERO, BigInteger.ZERO, true );
        }
        catch( UnsupportedOperationException e )
        {
            
        }
    }

    
    /**
     * Tests the listValues() method for correct behavoir.
     */
    public void testListValues() throws Exception
    {
        // -------------------------------------------------------------------
        // test the listValues(Object) method
        // -------------------------------------------------------------------

        NamingEnumeration values = table.listValues( BigInteger.ZERO );
        assertFalse( values.hasMore() );

        values = table.listValues( new BigInteger( "2" ) );
        assertTrue( values.hasMore() );
        Object value = values.next();
        assertEquals( BigInteger.ONE, value );
        assertFalse( values.hasMore() );
        
        values = table.listValues( BigInteger.ONE );
        assertTrue( values.hasMore() ) ;
        value = values.next();
        assertEquals( BigInteger.ONE, value );
        assertFalse( values.hasMore() );
    }
    
    
    /**
     * Tests the put() methods for correct behavior:
     * <ul>
     *   <li>put(Object, Object)</li>
     *   <li>put(Object, NamingEnumeration)</li>
     * </ul>
     */
    public void testPut() throws Exception
    {
        // put(Object,Object) already tested in setUp() tests the 
        // this instead tests the NamingEnumeration overload
        
        NamingEnumeration values = new ArrayNE( new Object[] {
            new BigInteger( "3" ),
            new BigInteger( "4" ),
            new BigInteger( "5" ),
            new BigInteger( "6" ),
        } );

        try
        {
            table.put( BigInteger.ONE, values );
        }
        catch( UnsupportedOperationException e )
        {
        }
    }
    
    
    /**
     * Tests the remove(Object) for correct behavoir:
     */
    public void testRemoveObject() throws Exception
    {
        // -------------------------------------------------------------------
        // tests the remove(Object) method
        // -------------------------------------------------------------------

        try
        {
            table.remove( BigInteger.ZERO );
            fail( "should not get here trying to remove non-existent key" );
        }
        catch ( IllegalArgumentException e )
        {
        }
        
        Object value = table.remove( new BigInteger( "2" ) );
        assertEquals( BigInteger.ONE, value );
        assertEquals( 3, table.count() );
        
        value = table.remove( BigInteger.ONE );
        assertEquals( BigInteger.ONE, value );
        assertEquals( 2, table.count() );
    }
    
    
    /**
     * Tests the remove(Object,Object) for correct behavoir:
     */
    public void testRemoveObjectObject() throws Exception
    {
        // -------------------------------------------------------------------
        // tests the remove(Object) method
        // -------------------------------------------------------------------

        Object value = table.remove( BigInteger.ZERO, BigInteger.ZERO );
        assertNull( value );
        
        value = table.remove( new BigInteger( "2" ), BigInteger.ONE );
        assertEquals( BigInteger.ONE, value );
        assertEquals( 3, table.count() );
        
        value = table.remove( BigInteger.ONE, new BigInteger( "2" ) );
        assertEquals( null, value ); 
        assertEquals( 3, table.count() );
    }
    
    
    /**
     * Tests the remove(Object,NamingEnumeration) for correct behavoir:
     */
    public void testRemoveObjectNamingEnumeration() throws Exception
    {
        NamingEnumeration values = new ArrayNE( new Object[] {
            new BigInteger( "1" ),
            new BigInteger( "2" )
        } );
        
        try
        {
            table.remove( BigInteger.ONE, values );
        }
        catch( UnsupportedOperationException e )
        {
            
        }

        values.close();
    }
    
    
    class ArrayNE extends ArrayEnumeration implements NamingEnumeration
    {
        public ArrayNE( Object[] array )
        {
            super( array );
        }

        public void close() throws NamingException
        {
        }

        public boolean hasMore() throws NamingException
        {
            return hasMoreElements();
        }

        public Object next() throws NamingException
        {
            return nextElement();
        }
    }
}

    