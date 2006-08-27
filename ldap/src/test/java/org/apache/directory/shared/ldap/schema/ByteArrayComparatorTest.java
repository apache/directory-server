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
package org.apache.directory.shared.ldap.schema;


import junit.framework.TestCase;


/**
 * Testcase to test the ByteArrayComparator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ByteArrayComparatorTest extends TestCase
{
    public void testBothNull()
    {
        assertEquals( 0, ByteArrayComparator.INSTANCE.compare( null, null ) );
    }

    
    public void testB2Null()
    {
        assertEquals( 1, ByteArrayComparator.INSTANCE.compare( new byte[0], null ) );
    }

    
    public void testB1Null()
    {
        assertEquals( -1, ByteArrayComparator.INSTANCE.compare( null, new byte[0] ) );
    }

    
    public void testBothEmpty()
    {
        assertEquals( 0, ByteArrayComparator.INSTANCE.compare( new byte[0], new byte[0] ) );
    }

    
    public void testBothEqualLengthOne()
    {
        assertEquals( 0, ByteArrayComparator.INSTANCE.compare( new byte[1], new byte[1] ) );
    }

    
    public void testBothEqualLengthTen()
    {
        assertEquals( 0, ByteArrayComparator.INSTANCE.compare( new byte[10], new byte[10] ) );
    }
    
    
    public void testB1PrefixOfB2()
    {
        byte[] b1 = new byte[] { 0, 1, 2 };
        byte[] b2 = new byte[] { 0, 1, 2, 3 };

        assertEquals( -1, ByteArrayComparator.INSTANCE.compare( b1, b2 ) );
    }
    
    
    public void testB2PrefixOfB1()
    {
        byte[] b1 = new byte[] { 0, 1, 2, 3 };
        byte[] b2 = new byte[] { 0, 1, 2 };

        assertEquals( 1, ByteArrayComparator.INSTANCE.compare( b1, b2 ) );
    }
    
    
    public void testB1GreaterThanB2() 
    {
        byte[] b1 = new byte[] { 0, 5 };
        byte[] b2 = new byte[] { 0, 1, 2 };

        assertEquals( 1, ByteArrayComparator.INSTANCE.compare( b1, b2 ) );
    }


    public void testB1GreaterThanB2SameLength() 
    {
        byte[] b1 = new byte[] { 0, 5 };
        byte[] b2 = new byte[] { 0, 1 };

        assertEquals( 1, ByteArrayComparator.INSTANCE.compare( b1, b2 ) );
    }


    public void testB2GreaterThanB1() 
    {
        byte[] b1 = new byte[] { 0, 1, 2 };
        byte[] b2 = new byte[] { 0, 5 };

        assertEquals( -1, ByteArrayComparator.INSTANCE.compare( b1, b2 ) );
    }


    public void testB2GreaterThanB1SameLength() 
    {
        byte[] b1 = new byte[] { 0, 1 };
        byte[] b2 = new byte[] { 0, 5 };

        assertEquals( -1, ByteArrayComparator.INSTANCE.compare( b1, b2 ) );
    }
}
