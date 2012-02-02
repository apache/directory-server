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
package jdbm.recman;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Unit tests class LocationEntry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class LocationTest
{
    private static Location clonedServerEntryA;
    private static Location clonedServerEntryACopy;
    private static Location clonedServerEntryB;
    private static Location clonedServerEntryA1;
    private static Location clonedServerEntryACopy1;
    private static Location clonedServerEntryB1;
    private static Location clonedServerEntryC1;
    private static Location clonedServerEntryD1;


    /**
     * Initialize name instances
     */
    @BeforeClass
    public static void initNames() throws Exception
    {
        clonedServerEntryA = new Location( 1L );
        clonedServerEntryACopy = new Location( 1L );
        clonedServerEntryB = new Location( 1L );
        clonedServerEntryA1 = new Location( 1L, ( short ) 1 );
        clonedServerEntryACopy1 = new Location( 1L, ( short ) 1 );
        clonedServerEntryB1 = new Location( 1L, ( short ) 1 );
        clonedServerEntryC1 = new Location( 1L, ( short ) 2 );
        clonedServerEntryD1 = new Location( 2L, ( short ) 1 );
    }


    @Test
    public void testEqualsNull() throws Exception
    {
        assertFalse( clonedServerEntryA.equals( null ) );
        assertFalse( clonedServerEntryA1.equals( null ) );
    }


    @Test
    public void testEqualsReflexive() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryA );
        assertEquals( clonedServerEntryA1, clonedServerEntryA1 );
    }


    @Test
    public void testHashCodeReflexive() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryA.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryA1.hashCode() );
    }


    @Test
    public void testEqualsSymmetric() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryACopy );
        assertEquals( clonedServerEntryACopy, clonedServerEntryA );
        assertEquals( clonedServerEntryA1, clonedServerEntryACopy1 );
        assertEquals( clonedServerEntryACopy1, clonedServerEntryA1 );
    }


    @Test
    @Ignore
    public void testHashCodeSymmetric() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryACopy.hashCode() );
        assertEquals( clonedServerEntryACopy.hashCode(), clonedServerEntryA.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryACopy1.hashCode() );
        assertEquals( clonedServerEntryACopy1.hashCode(), clonedServerEntryA1.hashCode() );
    }


    @Test
    public void testEqualsTransitive() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryACopy );
        assertEquals( clonedServerEntryACopy, clonedServerEntryB );
        assertEquals( clonedServerEntryA, clonedServerEntryB );
        assertEquals( clonedServerEntryA1, clonedServerEntryACopy1 );
        assertEquals( clonedServerEntryACopy1, clonedServerEntryB1 );
        assertEquals( clonedServerEntryA1, clonedServerEntryB1 );
    }


    @Test
    @Ignore
    public void testHashCodeTransitive() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryACopy.hashCode() );
        assertEquals( clonedServerEntryACopy.hashCode(), clonedServerEntryB.hashCode() );
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryB.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryACopy1.hashCode() );
        assertEquals( clonedServerEntryACopy1.hashCode(), clonedServerEntryB1.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryB1.hashCode() );
    }


    @Test
    public void testNotEqualDiffValue() throws Exception
    {
        assertFalse( clonedServerEntryA1.equals( clonedServerEntryC1 ) );
        assertFalse( clonedServerEntryC1.equals( clonedServerEntryA1 ) );
        assertFalse( clonedServerEntryA1.equals( clonedServerEntryD1 ) );
        assertFalse( clonedServerEntryD1.equals( clonedServerEntryA1 ) );
    }
}
