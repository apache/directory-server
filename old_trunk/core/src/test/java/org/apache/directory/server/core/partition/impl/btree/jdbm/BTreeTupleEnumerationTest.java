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
import java.io.IOException;

import javax.naming.NamingException;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.shared.ldap.util.LongComparator;
//import org.apache.directory.shared.ldap.util.BigIntegerComparator;

import junit.framework.TestCase;


/**
 * Tests that the BTreeTupleEnumeration functions as expected.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BTreeTupleEnumerationTest extends TestCase
{
    private final static byte[] EMPTY_BYTES = new byte[0];
    private File tempFile = null;
    private BTree tree = null;
    private RecordManager rm = null;
    
    
    public void setUp() throws Exception 
    {
        tempFile = File.createTempFile( "jdbm", "test" );
        rm = new BaseRecordManager( tempFile.getAbsolutePath() );
        tree = BTree.createInstance( rm, new LongComparator() );
    }
    
    protected void tearDown() throws Exception
    {
        String tmp = tempFile.getAbsolutePath();
        new File( tmp ).delete();
        new File( tmp + ".db" ).delete();
        new File( tmp + ".lg" ).delete();
    }

    public void testEmptyBTree() throws NamingException
    {
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, 1L );
        assertFalse( "enumeration on empty btree should not have elements", bte.hasMore() );
    }
    
    
    public void testOneElement() throws IOException, NamingException
    {
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, 1L );
        assertTrue( bte.hasMore() );
        Tuple tuple = ( Tuple ) bte.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( value, tuple.getValue() );
        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElements() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, 1L );
        
        Tuple tuple = ( Tuple ) bte.next();
        assertTrue( bte.hasMore() );
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );

        bte.next();
        assertTrue( bte.hasMore() );
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 2L, tuple.getValue() );

        bte.next();
        assertTrue( bte.hasMore() );
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 4L, tuple.getValue() );

        bte.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 5L, tuple.getValue() );

        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElementsLessThanNonExistantValue() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, new LongComparator(), 
            1L, 3L, false );
        
        Tuple tuple = ( Tuple ) bte.next();
        assertTrue( bte.hasMore() );
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 2L, tuple.getValue() );

        bte.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );

        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElementsLessThanLowestValue() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, new LongComparator(), 
            1L, 0L, false );
        
        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElementsLessThanAtLowestValue() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, new LongComparator(), 
            1L, 1L, false );
        
        Tuple tuple = ( Tuple ) bte.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );

        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElementsGreaterThanNonExistantValue() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, new LongComparator(), 
            1L, 3L, true );
        
        Tuple tuple = ( Tuple ) bte.next();
        assertTrue( bte.hasMore() );
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 4L, tuple.getValue() );

        bte.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 5L, tuple.getValue() );

        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElementsGreaterThanLastValue() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, new LongComparator(), 
            1L, 6L, true );
        
        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElementsGreaterThanAtLastValue() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, new LongComparator(), 
            1L, 5L, true );
        
        Tuple tuple = ( Tuple ) bte.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 5L, tuple.getValue() );

        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElementsLessThanExistantValue() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, new LongComparator(), 
            1L, 4L, false );
        
        Tuple tuple = ( Tuple ) bte.next();
        assertTrue( bte.hasMore() );
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 4L, tuple.getValue() );

        bte.next();
        assertTrue( bte.hasMore() );
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 2L, tuple.getValue() );

        bte.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 1L, tuple.getValue() );

        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
    
    
    public void testManyElementsGreaterThanExistantValue() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        Long value = 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );

        value += 1L;
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeTupleEnumeration bte = new BTreeTupleEnumeration( tree, new LongComparator(), 
            1L, 4L, true );
        
        Tuple tuple = ( Tuple ) bte.next();
        assertTrue( bte.hasMore() );
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 4L, tuple.getValue() );

        bte.next();
        assertEquals( 1L, tuple.getKey() );
        assertEquals( 5L, tuple.getValue() );

        assertFalse( "enumeration consumed should not have elements", bte.hasMore() );
    }
}
