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
package org.apache.directory.server.core.partition.index;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.apache.directory.server.core.api.partition.index.GenericIndex;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests the {@link GenericIndex} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GenericIndexTest
{

    GenericIndex<String, Long, Long> index;


    @Before
    public void setUp()
    {
        String tmpDir = System.getProperty( "java.io.tmpdir" );
        index = new GenericIndex<String, Long, Long>( "cn", 42, new File( tmpDir ).toURI() );
    }


    @Test
    public void testConstructor1()
    {
        index = new GenericIndex<String, Long, Long>( "cn" );
        assertEquals( "cn", index.getAttributeId() );
        assertEquals( GenericIndex.DEFAULT_INDEX_CACHE_SIZE, index.getCacheSize() );
        assertNull( index.getWkDirPath() );
    }


    @Test
    public void testConstructor2()
    {
        index = new GenericIndex<String, Long, Long>( "cn", 42 );
        assertEquals( "cn", index.getAttributeId() );
        assertEquals( 42, index.getCacheSize() );
        assertNull( index.getWkDirPath() );
    }


    @Test
    public void testConstructor3()
    {
        File tmpDir = new File(System.getProperty( "java.io.tmpdir" ));

        index = new GenericIndex<String, Long, Long>( "cn", 42, tmpDir.toURI() );
        assertEquals( "cn", index.getAttributeId() );
        assertEquals( 42, index.getCacheSize() );
        assertNotNull( index.getWkDirPath() );
        assertEquals( tmpDir.toURI().getPath(), index.getWkDirPath().getPath() );
    }


    @Test
    public void testSetGetAttributeId()
    {
        index.setAttributeId( "sn" );
        assertEquals( "sn", index.getAttributeId() );
        index.setAttributeId( null );
        assertNull( index.getAttributeId() );
    }


    @Test
    public void testSetGetCacheSize()
    {
        index.setCacheSize( 0 );
        assertEquals( 0, index.getCacheSize() );
        index.setCacheSize( Integer.MAX_VALUE );
        assertEquals( Integer.MAX_VALUE, index.getCacheSize() );
        index.setCacheSize( Integer.MIN_VALUE );
        assertEquals( Integer.MIN_VALUE, index.getCacheSize() );
    }


    @Test
    public void testSetGetWkDirPath()
    {
        File tmpDir = new File( System.getProperty( "java.io.tmpdir" ));
        File zzzDir = new File( tmpDir, "zzz"  );

        index.setWkDirPath( zzzDir.toURI() );
        assertNotNull( index.getWkDirPath() );
        assertEquals( zzzDir, new File( index.getWkDirPath() ) );
        index.setWkDirPath( null );
        assertNull( index.getWkDirPath() );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testAdd() throws Exception
    {
        index.add( "test", 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testClose() throws Exception
    {
        index.close();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testCount() throws Exception
    {
        index.count();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testCountK() throws Exception
    {
        index.count( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testDropID() throws Exception
    {
        index.drop( 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testDropKID() throws Exception
    {
        index.drop( "test", 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardCursor() throws Exception
    {
        index.forwardCursor();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardCursorK() throws Exception
    {
        index.forwardCursor( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardLookup() throws Exception
    {
        index.forwardLookup( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardValueCursor() throws Exception
    {
        index.forwardValueCursor( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardK() throws Exception
    {
        index.forward( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardKID() throws Exception
    {
        index.forward( "test", 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseID() throws Exception
    {
        index.reverse( 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseIDK() throws Exception
    {
        index.reverse( 5L, "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardGreaterOrEqK() throws Exception
    {
        index.forwardGreaterOrEq( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardGreaterOrEqKID() throws Exception
    {
        index.forwardGreaterOrEq( "test", 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseGreaterOrEqID() throws Exception
    {
        index.reverseGreaterOrEq( 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseGreaterOrEqIDK() throws Exception
    {
        index.reverseGreaterOrEq( 5L, "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardLessOrEqK() throws Exception
    {
        index.forwardLessOrEq( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testForwardLessOrEqKID() throws Exception
    {
        index.forwardLessOrEq( "test", 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseLessOrEqID() throws Exception
    {
        index.reverseLessOrEq( 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseLessOrEqIDK() throws Exception
    {
        index.reverseLessOrEq( 5L, "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testGetAttribute()
    {
        index.getAttribute();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testGetNormalized() throws Exception
    {
        index.getNormalized( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testGreaterThanCount() throws Exception
    {
        index.greaterThanCount( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testLessThanCount() throws Exception
    {
        index.lessThanCount( "test" );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseCursor() throws Exception
    {
        index.reverseCursor();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseCursorID() throws Exception
    {
        index.reverseCursor( 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseLookup() throws Exception
    {
        index.reverseLookup( 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testReverseValueCursor() throws Exception
    {
        index.reverseValueCursor( 5L );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testSync() throws Exception
    {
        index.sync();
    }


    @Test
    public void testIsDupsEnabled()
    {
        assertFalse( index.isDupsEnabled() );
    }

}
