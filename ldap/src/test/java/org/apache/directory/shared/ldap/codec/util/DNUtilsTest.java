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
package org.apache.directory.shared.ldap.codec.util;


import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Test the class DNUtils
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DNUtilsTest extends TestCase
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsFull()
    {
        // Full compare
        Assert.assertEquals( 6, StringTools.areEquals( "azerty".getBytes(), 0, "azerty" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsDiff()
    {
        // First character is !=
        Assert.assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "Azerty" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsEmpty()
    {
        // Compare to an empty string
        Assert.assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsFirstCharDiff()
    {
        // First character is !=
        Assert.assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "Azerty" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsMiddleCharDiff()
    {
        // First character is !=
        Assert.assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "azeRty" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsLastCharDiff()
    {
        // First character is !=
        Assert.assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "azertY" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsCharByChar()
    {
        // Index must be incremented after each comparison
        Assert.assertEquals( 1, StringTools.areEquals( "azerty".getBytes(), 0, "a" ) );
        Assert.assertEquals( 2, StringTools.areEquals( "azerty".getBytes(), 1, "z" ) );
        Assert.assertEquals( 3, StringTools.areEquals( "azerty".getBytes(), 2, "e" ) );
        Assert.assertEquals( 4, StringTools.areEquals( "azerty".getBytes(), 3, "r" ) );
        Assert.assertEquals( 5, StringTools.areEquals( "azerty".getBytes(), 4, "t" ) );
        Assert.assertEquals( 6, StringTools.areEquals( "azerty".getBytes(), 5, "y" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsTooShort()
    {
        // length too short
        Assert.assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "azertyiop" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsTooShortMiddle()
    {
        // length too short
        Assert.assertEquals( -1, StringTools.areEquals( "azerty".getBytes(), 0, "ertyiop" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsLastChar()
    {
        // last character
        Assert.assertEquals( 6, StringTools.areEquals( "azerty".getBytes(), 5, "y" ) );
    }


    /**
     * Test the DNUtils AreEquals method
     */
    public void testAreEqualsMiddle()
    {
        // In the middle
        Assert.assertEquals( 4, StringTools.areEquals( "azerty".getBytes(), 2, "er" ) );
    }
}
