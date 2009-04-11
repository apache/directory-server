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
package org.apache.directory.shared.ldap.util;


import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertEquals;



/**
 * Test the class DNUtils
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DNUtilsTest
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsFull()
    {
        // Full compare
        assertEquals( 6, StringTools.areEquals( "azerty".getBytes(), 0, "azerty" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsDiff()
    {
        // First character is !=
        assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "Azerty" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsEmpty()
    {
        // Compare to an empty string
        assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsFirstCharDiff()
    {
        // First character is !=
        assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "Azerty" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsMiddleCharDiff()
    {
        // First character is !=
        assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "azeRty" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsLastCharDiff()
    {
        // First character is !=
        assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "azertY" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsCharByChar()
    {
        // Index must be incremented after each comparison
        assertEquals( 1, StringTools.areEquals( "azerty".getBytes(), 0, "a" ) );
        assertEquals( 2, StringTools.areEquals( "azerty".getBytes(), 1, "z" ) );
        assertEquals( 3, StringTools.areEquals( "azerty".getBytes(), 2, "e" ) );
        assertEquals( 4, StringTools.areEquals( "azerty".getBytes(), 3, "r" ) );
        assertEquals( 5, StringTools.areEquals( "azerty".getBytes(), 4, "t" ) );
        assertEquals( 6, StringTools.areEquals( "azerty".getBytes(), 5, "y" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsTooShort()
    {
        // length too short
        assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "azertyiop" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsTooShortMiddle()
    {
        // length too short
        assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "ertyiop" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsLastChar()
    {
        // last character
        assertEquals( 6, StringTools.areEquals( "azerty".getBytes(), 5, "y" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    @Test
    public void testAreEqualsMiddle()
    {
        // In the middle
        assertEquals( 4, StringTools.areEquals( "azerty".getBytes(), 2, "er" ) );
    }
}
