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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.partition.impl.btree.TupleRenderer;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.shared.ldap.util.ArrayEnumeration;
import org.apache.directory.shared.ldap.util.LongComparator;

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
    private final LongComparator biComparator = new LongComparator();
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

        table.put( 1L, 1L );
        table.put( 2L, 1L );
        table.put( 4L, 1L );
        table.put( 5L, 1L );
    }

    protected void tearDown() throws Exception
    {
        String tmp = tempFile.getAbsolutePath();
        new File( tmp ).delete();
        new File( tmp + ".db" ).delete();
        new File( tmp + ".lg" ).delete();
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
        assertTrue( table.has( 1L ) );
        assertTrue( table.has( 2L ) );
        assertTrue( table.has( 4L ) );
        assertTrue( table.has( 5L ) );
        assertFalse( table.has( 3L ) );
        assertFalse( table.has( 0L ) );
        assertFalse( table.has( 999L ) );

        // test the has( Object, Object ) method
        assertTrue( table.has( 1L, 1L ) );
        assertTrue( table.has( 2L, 1L ) );
        assertTrue( table.has( 4L, 1L ) );
        assertTrue( table.has( 5L, 1L ) );
        assertFalse( table.has( 5L, 0L ) );
        assertFalse( table.has( 3L, 1L ) );
        assertFalse( table.has( 1L, 999L ) );
        assertFalse( table.has( 999L, 1L ) );

        // test the has( Object, boolean ) method
        assertFalse( table.has( Long.valueOf(0), false ) ); // we do not have a key less than or equal to 0
        assertTrue( table.has( Long.valueOf(1), false ) ); // we do have a key less than or equal to 1
        assertTrue( table.has( Long.valueOf(0), true ) ); // we do have a key greater than or equal to 0
        assertTrue( table.has( Long.valueOf(1), true ) ); // we do have a key greater than or equal to 1
        assertTrue( table.has( Long.valueOf(5), true ) ); // we do have a key greater than or equal to 5
        assertFalse( table.has( Long.valueOf(6), true ) ); // we do NOT have a key greater than or equal to 11
        assertFalse( table.has( Long.valueOf(999), true ) ); // we do NOT have a key greater than or equal to 12

        // test the has( Object, Object, boolean ) method
        try
        {
            table.has( 1L, 0L, true );
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
        assertEquals( 1, table.count( 1L ) );
        assertEquals( 0, table.count( 0L ) );
        assertEquals( 1, table.count( 2L ) );
        
        // test the count( Object, boolean ) method 
        // note for speed this count method returns the same as count()
        assertEquals( table.count(), table.count( 1L, true ) );
    }
    
    
    /**
     * Tests the get() method for correct behavoir.
     * 
     * @throws Exception
     */
    public void testGet() throws Exception
    {
        assertEquals( 1L, table.get( 1L ) );
        assertEquals( 1L, table.get( 2L ) );
        assertEquals( null, table.get( 3L ) );
        assertEquals( 1L, table.get( 4L ) );
        assertEquals( 1L, table.get( 5L ) );
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
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 2L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 4L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 5L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        
        assertFalse( tuples.hasMore() );

        // -------------------------------------------------------------------
        // test the listTuples(Object) method
        // -------------------------------------------------------------------

        tuples = table.listTuples( 0L );
        assertFalse( tuples.hasMore() );


        tuples = table.listTuples( 2L );
        assertTrue( tuples.hasMore() );
        tuple = ( Tuple ) tuples.next();
        assertEquals( 2L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        assertFalse( tuples.hasMore() );
        
        // -------------------------------------------------------------------
        // test the listTuples(Object, boolean) method
        // -------------------------------------------------------------------

        tuples = table.listTuples( 0L, false );
        assertFalse( tuples.hasMore() );


        tuples = table.listTuples( 1L, false );
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        assertFalse( tuples.hasMore() );


        tuples = table.listTuples( 2L, false );

        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 2L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );

        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        assertFalse( tuples.hasMore() );

        
        tuples = table.listTuples( 6L, true );
        assertFalse( tuples.hasMore() );

        
        tuples = table.listTuples( 5L, true );
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 5L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        assertFalse( tuples.hasMore() );

        
        tuples = table.listTuples( 4L, true );
        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 4L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );

        assertTrue( tuples.hasMore() ) ;
        tuple = ( Tuple ) tuples.next();
        assertEquals( 5L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );
        assertFalse( tuples.hasMore() );

        // -------------------------------------------------------------------
        // test the listTuples(Object,Object,boolean) method
        // -------------------------------------------------------------------

        try
        {
            tuples = table.listTuples( 0L, 0L, true );
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

        NamingEnumeration values = table.listValues( 0L );
        assertFalse( values.hasMore() );

        values = table.listValues( 2L );
        assertTrue( values.hasMore() );
        Object value = values.next();
        assertEquals( 1L, value );
        assertFalse( values.hasMore() );
        
        values = table.listValues( 1L );
        assertTrue( values.hasMore() ) ;
        value = values.next();
        assertEquals( 1L, value );
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
            3L,
            4L,
            5L,
            6L,
        } );

        try
        {
            table.put( 1L, values );
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
            table.remove( 0L );
            fail( "should not get here trying to remove non-existent key" );
        }
        catch ( IllegalArgumentException e )
        {
        }
        
        Object value = table.remove( 2L );
        assertEquals( 1L, value );
        assertEquals( 3, table.count() );
        
        value = table.remove( 1L );
        assertEquals( 1L, value );
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

        Object value = table.remove( 0L, 0L );
        assertNull( value );
        
        value = table.remove( 2L, 1L );
        assertEquals( 1L, value );
        assertEquals( 3, table.count() );
        
        value = table.remove( 1L, 2L );
        assertEquals( null, value ); 
        assertEquals( 3, table.count() );
    }
    
    
    /**
     * Tests the remove(Object,NamingEnumeration) for correct behavoir:
     */
    public void testRemoveObjectNamingEnumeration() throws Exception
    {
        NamingEnumeration values = new ArrayNE( new Object[] {
            1L,
            2L
        } );
        
        try
        {
            table.remove( 1L, values );
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

    