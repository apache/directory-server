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
package org.apache.directory.shared.ldap.schema.comparators;


import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * Testcase to test the ByteArrayComparator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ByteArrayComparatorTest
{
    @Test
    public void testBothNull()
    {
        assertEquals( 0, new ByteArrayComparator( null ).compare( null, null ) );
    }

    
    @Test
    public void testB2Null()
    {
        assertEquals( 1, new ByteArrayComparator( null ).compare( new byte[0], null ) );
    }

    
    @Test
    public void testB1Null()
    {
        assertEquals( -1, new ByteArrayComparator( null ).compare( null, new byte[0] ) );
    }

    
    @Test
    public void testBothEmpty()
    {
        assertEquals( 0, new ByteArrayComparator( null ).compare( new byte[0], new byte[0] ) );
    }

    
    @Test
    public void testBothEqualLengthOne()
    {
        assertEquals( 0, new ByteArrayComparator( null ).compare( new byte[1], new byte[1] ) );
    }

    
    @Test
    public void testBothEqualLengthTen()
    {
        assertEquals( 0, new ByteArrayComparator( null ).compare( new byte[10], new byte[10] ) );
    }
    
    
    @Test
    public void testB1PrefixOfB2()
    {
        byte[] b1 = new byte[] { 0, 1, 2 };
        byte[] b2 = new byte[] { 0, 1, 2, 3 };

        assertEquals( -1, new ByteArrayComparator( null ).compare( b1, b2 ) );
    }
    
    
    @Test
    public void testB2PrefixOfB1()
    {
        byte[] b1 = new byte[] { 0, 1, 2, 3 };
        byte[] b2 = new byte[] { 0, 1, 2 };

        assertEquals( 1, new ByteArrayComparator( null ).compare( b1, b2 ) );
    }
    
    
    @Test
    public void testB1GreaterThanB2() 
    {
        byte[] b1 = new byte[] { 0, 5 };
        byte[] b2 = new byte[] { 0, 1, 2 };

        assertEquals( 1, new ByteArrayComparator( null ).compare( b1, b2 ) );
    }


    @Test
    public void testB1GreaterThanB2SameLength() 
    {
        byte[] b1 = new byte[] { 0, 5 };
        byte[] b2 = new byte[] { 0, 1 };

        assertEquals( 1, new ByteArrayComparator( null ).compare( b1, b2 ) );
    }


    @Test
    public void testB2GreaterThanB1() 
    {
        byte[] b1 = new byte[] { 0, 1, 2 };
        byte[] b2 = new byte[] { 0, 5 };

        assertEquals( -1, new ByteArrayComparator( null ).compare( b1, b2 ) );
    }


    @Test
    public void testB2GreaterThanB1SameLength() 
    {
        byte[] b1 = new byte[] { 0, 1 };
        byte[] b2 = new byte[] { 0, 5 };

        assertEquals( -1, new ByteArrayComparator( null ).compare( b1, b2 ) );
    }
}
