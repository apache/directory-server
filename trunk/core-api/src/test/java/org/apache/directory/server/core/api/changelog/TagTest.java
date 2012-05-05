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
package org.apache.directory.server.core.api.changelog;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.apache.directory.server.core.api.changelog.Tag;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Unit tests class Tag.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TagTest
{
    private static Tag tagA;
    private static Tag tagACopy;
    private static Tag tagANull;
    private static Tag tagB;
    private static Tag tagC;
    private static Tag tagD;


    /**
     * Initialize name instances
     */
    @BeforeClass
    public static void initNames() throws Exception
    {

        tagA = new Tag( 1L, "aa", 1L, 1L );
        tagACopy = new Tag( 1L, "aa", 1L, 1L );
        tagB = new Tag( 1L, "aa", 2L, 2L );
        tagC = new Tag( 2L, "aa", 1L, 1L );
        tagD = new Tag( 1L, "bb", 1L, 1L );
        tagANull = new Tag( 1L, null, 1L, 1L );
    }


    @Test
    public void testEqualsNull() throws Exception
    {
        assertFalse( tagA.equals( null ) );
        assertFalse( tagANull.equals( null ) );
    }


    @Test
    public void testEqualsReflexive() throws Exception
    {
        assertEquals( tagA, tagA );
        assertEquals( tagANull, tagANull );
    }


    @Test
    public void testHashCodeReflexive() throws Exception
    {
        assertEquals( tagA.hashCode(), tagA.hashCode() );
        assertEquals( tagANull.hashCode(), tagANull.hashCode() );
    }


    @Test
    public void testEqualsSymmetric() throws Exception
    {
        assertEquals( tagA, tagACopy );
        assertEquals( tagACopy, tagA );
    }


    @Test
    public void testHashCodeSymmetric() throws Exception
    {
        assertEquals( tagA.hashCode(), tagACopy.hashCode() );
        assertEquals( tagACopy.hashCode(), tagA.hashCode() );
    }


    @Test
    public void testEqualsTransitive() throws Exception
    {
        assertEquals( tagA, tagACopy );
        assertEquals( tagACopy, tagB );
        assertEquals( tagA, tagB );
    }


    @Test
    public void testHashCodeTransitive() throws Exception
    {
        assertEquals( tagA.hashCode(), tagACopy.hashCode() );
        assertEquals( tagACopy.hashCode(), tagB.hashCode() );
        assertEquals( tagA.hashCode(), tagB.hashCode() );
    }


    @Test
    public void testNotEqualDiffValue() throws Exception
    {
        assertFalse( tagA.equals( tagC ) );
        assertFalse( tagC.equals( tagA ) );
        assertFalse( tagA.equals( tagANull ) );
        assertFalse( tagANull.equals( tagA ) );
        assertFalse( tagD.equals( tagA ) );
    }
}
