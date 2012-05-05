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
package org.apache.directory.server.xdbm;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.junit.Test;


/**
 * Tests the {@link Tuple} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TupleTest
{

    @Test
    public void testDefaultConstructor()
    {
        Tuple<String, Long> tuple = new Tuple<String, Long>();
        assertNull( tuple.getKey() );
        assertNull( tuple.getValue() );
    }


    @Test
    public void testParameterConstructor()
    {
        Tuple<String, Long> tuple = new Tuple<String, Long>( "test", 6L );
        assertEquals( "test", tuple.getKey() );
        assertEquals( Long.valueOf( 6L ), tuple.getValue() );
    }


    @Test
    public void testConstructorWithNullParameter()
    {
        Tuple<String, Long> tuple1 = new Tuple<String, Long>( null, null );
        assertNull( tuple1.getKey() );
        assertNull( tuple1.getValue() );

        Tuple<String, Long> tuple2 = new Tuple<String, Long>( "test", null );
        assertEquals( "test", tuple2.getKey() );
        assertNull( tuple2.getValue() );

        Tuple<String, Long> tuple3 = new Tuple<String, Long>( null, 6L );
        assertNull( tuple3.getKey() );
        assertEquals( Long.valueOf( 6L ), tuple3.getValue() );
    }


    @Test
    public void testSetGetKey()
    {
        Tuple<String, Long> tuple = new Tuple<String, Long>();
        assertNull( tuple.getKey() );

        tuple.setKey( "a" );
        assertEquals( "a", tuple.getKey() );

        tuple.setKey( "b" );
        assertEquals( "b", tuple.getKey() );

        tuple.setKey( null );
        assertNull( tuple.getKey() );
    }


    @Test
    public void testSetGetValue()
    {
        Tuple<String, Long> tuple = new Tuple<String, Long>();
        assertNull( tuple.getValue() );

        tuple.setValue( 1L );
        assertEquals( Long.valueOf( 1L ), tuple.getValue() );

        tuple.setValue( 2L );
        assertEquals( Long.valueOf( 2L ), tuple.getValue() );

        tuple.setValue( null );
        assertNull( tuple.getValue() );
    }


    @Test
    public void testSetGetBoth()
    {
        Tuple<String, Long> tuple = new Tuple<String, Long>();
        assertNull( tuple.getKey() );
        assertNull( tuple.getValue() );

        tuple.setBoth( "a", 1L );
        assertEquals( "a", tuple.getKey() );
        assertEquals( Long.valueOf( 1L ), tuple.getValue() );

        tuple.setBoth( "b", 2L );
        assertEquals( "b", tuple.getKey() );
        assertEquals( Long.valueOf( 2L ), tuple.getValue() );

        tuple.setBoth( null, null );
        assertNull( tuple.getKey() );
        assertNull( tuple.getValue() );
    }


    @Test
    public void testSetGetBothTuple()
    {
        Tuple<String, Long> tuple = new Tuple<String, Long>();
        assertNull( tuple.getKey() );
        assertNull( tuple.getValue() );

        tuple.setBoth( new Tuple<String, Long>( "a", 1L ) );
        assertEquals( "a", tuple.getKey() );
        assertEquals( Long.valueOf( 1L ), tuple.getValue() );

        tuple.setBoth( new Tuple<String, Long>( "b", 2L ) );
        assertEquals( "b", tuple.getKey() );
        assertEquals( Long.valueOf( 2L ), tuple.getValue() );

        tuple.setBoth( new Tuple<String, Long>() );
        assertNull( tuple.getKey() );
        assertNull( tuple.getValue() );
    }


    @Test
    public void testEquals()
    {
        Tuple<String, Long> tuple0 = new Tuple<String, Long>();
        Tuple<String, Long> tuple1 = new Tuple<String, Long>( "a", 1L );
        Tuple<String, Long> tuple2 = new Tuple<String, Long>( "b", 2L );
        Tuple<String, Long> tuple3 = new Tuple<String, Long>( "a", 2L );
        Tuple<String, Long> tuple4 = new Tuple<String, Long>( "a", 1L );
        Tuple<String, Long> tuple5 = new Tuple<String, Long>( "a", null );
        Tuple<String, Long> tuple6 = new Tuple<String, Long>( null, 1L );

        // test null and other instance
        assertFalse( tuple0.equals( null ) );
        assertFalse( tuple0.equals( new Object() ) );

        // test equal tuples
        assertTrue( tuple0.equals( new Tuple<String, Long>() ) );
        assertTrue( tuple0.equals( tuple0 ) );
        assertTrue( tuple1.equals( tuple4 ) );
        assertTrue( tuple4.equals( tuple1 ) );

        // test tuples with non-null key/value
        assertFalse( tuple1.equals( tuple2 ) );
        assertFalse( tuple1.equals( tuple3 ) );
        assertFalse( tuple2.equals( tuple3 ) );

        // test tuples with null key/value
        assertFalse( tuple1.equals( tuple0 ) );
        assertFalse( tuple0.equals( tuple1 ) );
        assertFalse( tuple1.equals( tuple5 ) );
        assertFalse( tuple5.equals( tuple1 ) );
        assertFalse( tuple1.equals( tuple6 ) );
        assertFalse( tuple6.equals( tuple1 ) );
        assertFalse( tuple5.equals( tuple6 ) );
        assertFalse( tuple6.equals( tuple5 ) );
    }


    @Test
    public void testHashCodeEquals()
    {
        Set<Tuple<String, Long>> tupleSet = new HashSet<Tuple<String, Long>>();

        Tuple<String, Long> tuple0 = new Tuple<String, Long>();
        Tuple<String, Long> tuple1 = new Tuple<String, Long>( "a", 1L );
        Tuple<String, Long> tuple2 = new Tuple<String, Long>( "b", 2L );
        Tuple<String, Long> tuple3 = new Tuple<String, Long>( "a", 2L );
        Tuple<String, Long> tuple4 = new Tuple<String, Long>( "a", 1L );

        tupleSet.add( tuple0 );
        tupleSet.add( tuple1 );
        tupleSet.add( tuple2 );
        tupleSet.add( tuple3 );
        assertEquals( 4, tupleSet.size() );

        tupleSet.add( tuple4 );
        assertEquals( 4, tupleSet.size() );
    }


    @Test
    public void testToString()
    {
        Tuple<String, Long> tuple0 = new Tuple<String, Long>();
        Tuple<String, Long> tuple1 = new Tuple<String, Long>( "a", 1L );
        Tuple<String, Long> tuple2 = new Tuple<String, Long>( "b", 2L );
        Tuple<String, Long> tuple3 = new Tuple<String, Long>( "a", 1L );

        assertNotNull( tuple0.toString() );
        assertNotNull( tuple1.toString() );
        assertNotNull( tuple2.toString() );
        assertNotNull( tuple3.toString() );
        assertFalse( tuple0.toString().equals( tuple1.toString() ) );
        assertFalse( tuple1.toString().equals( tuple2.toString() ) );
        assertTrue( tuple1.toString().equals( tuple3.toString() ) );
    }

}
