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
import java.math.BigInteger;

import javax.naming.NamingException;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.shared.ldap.util.BigIntegerComparator;

import junit.framework.TestCase;


/**
 * Tests that the BTreeEnumeration functions as expected.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BTreeIteratorTest extends TestCase
{
    private final static byte[] EMPTY_BYTES = new byte[0];
    private File tempFile = null;
    private BTree tree = null;
    private RecordManager rm = null;
    
    
    public void setUp() throws Exception 
    {
        tempFile = File.createTempFile( "jdbm", "test" );
        rm = new BaseRecordManager( tempFile.getAbsolutePath() );
        tree = BTree.createInstance( rm, new BigIntegerComparator() );
    }
    

    public void testEmptyBTree() throws NamingException
    {
        BTreeIterator bte = new BTreeIterator( tree, true );
        assertFalse( "iterator on empty btree should not have elements", bte.hasNext() );
    }
    
    
    public void testOneElement() throws IOException, NamingException
    {
        BigInteger value = new BigInteger( "1" );
        tree.insert( value, EMPTY_BYTES, true );
        BTreeIterator bte = new BTreeIterator( tree, true );
        assertTrue( bte.hasNext() );
        assertEquals( value, bte.next() );
        assertFalse( "iterator consumed should not have elements", bte.hasNext() );
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
        BigInteger value = new BigInteger( "1" );
        tree.insert( value, EMPTY_BYTES, true );
        
        value = value.add( BigInteger.ONE );
        tree.insert( value, EMPTY_BYTES, true );

        value = value.add( BigInteger.ONE );
        value = value.add( BigInteger.ONE );
        tree.insert( value, EMPTY_BYTES, true );

        value = value.add( BigInteger.ONE );
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeIterator bte = new BTreeIterator( tree, true );

        assertTrue( bte.hasNext() );
        assertEquals( new BigInteger( "1" ), bte.next() );

        assertTrue( bte.hasNext() );
        assertEquals( new BigInteger( "2" ), bte.next() );

        assertTrue( bte.hasNext() );
        assertEquals( new BigInteger( "4" ), bte.next() );

        assertTrue( bte.hasNext() );
        assertEquals( new BigInteger( "5" ), bte.next() );

        assertFalse( "iterator consumed should not have elements", bte.hasNext() );
    }


    public void testManyElementsReversed() throws IOException, NamingException
    {
        /*
         * Adding the following values for this test
         * 1, -
         * 2, -
         * 4, -
         * 5, -
         */
        BigInteger value = new BigInteger( "1" );
        tree.insert( value, EMPTY_BYTES, true );
        
        value = value.add( BigInteger.ONE );
        tree.insert( value, EMPTY_BYTES, true );

        value = value.add( BigInteger.ONE );
        value = value.add( BigInteger.ONE );
        tree.insert( value, EMPTY_BYTES, true );

        value = value.add( BigInteger.ONE );
        tree.insert( value, EMPTY_BYTES, true );
        
        BTreeIterator bte = new BTreeIterator( tree, false );

        assertTrue( bte.hasNext() );
        assertEquals( new BigInteger( "5" ), bte.next() );

        assertTrue( bte.hasNext() );
        assertEquals( new BigInteger( "4" ), bte.next() );

        assertTrue( bte.hasNext() );
        assertEquals( new BigInteger( "2" ), bte.next() );

        assertTrue( bte.hasNext() );
        assertEquals( new BigInteger( "1" ), bte.next() );

        assertFalse( "iterator consumed should not have elements", bte.hasNext() );
    }
}
