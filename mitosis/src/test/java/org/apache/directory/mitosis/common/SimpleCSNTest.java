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
package org.apache.directory.mitosis.common;

import junit.framework.TestCase;

/**
 * 
 * Test for the SimpleCSN class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleCSNTest extends TestCase
{

    public void testCSN()
    {
        long ts = System.currentTimeMillis();
        
        CSN csn = new SimpleCSN( Long.toString( ts, 16 ) + ":abcdefghi0123:" + 1 );
        
        assertEquals( ts, csn.getTimestamp() );
        assertEquals( 1, csn.getOperationSequence() );
        assertEquals( "abcdefghi0123", csn.getReplicaId().toString() );
    }

    public void testCSNEmpty()
    {
        try
        {
            new SimpleCSN( "" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }

    public void testCSNTSOnly()
    {
        try
        {
            new SimpleCSN( "123" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }

    public void testCSNInvalidTS()
    {
        try
        {
            new SimpleCSN( "zzz:abc:1" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }

    public void testCSNNoTS()
    {
        try
        {
            new SimpleCSN( ":abc:1" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }

    public void testCSNInavlidReplica()
    {
        try
        {
            new SimpleCSN( "123:*:1" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }

    public void testCSNNoReplica()
    {
        try
        {
            new SimpleCSN( "123::1" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }

    public void testCSNInavlidOpSeq()
    {
        try
        {
            new SimpleCSN( "123:abc:zzz" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }

    public void testCSNEmptyOpSeq()
    {
        try
        {
            new SimpleCSN( "123:abc:" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }

    public void testCSNNoOpSeq()
    {
        try
        {
            new SimpleCSN( "123:abc" );
            fail();
        }
        catch ( AssertionError ae )
        {
            assertTrue( true );
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }
    
    public void testCSNToBytes()
    {
        CSN csn = new SimpleCSN( "0123456789abcdef:test:5678cdef" );
        
        byte[] bytes = csn.toBytes();
        
        assertEquals( 0x01, bytes[0] );
        assertEquals( 0x23, bytes[1] );
        assertEquals( 0x45, bytes[2] );
        assertEquals( 0x67, bytes[3] );
        assertEquals( (byte)0x89, bytes[4] );
        assertEquals( (byte)0xAB, bytes[5] );
        assertEquals( (byte)0xCD, bytes[6] );
        assertEquals( (byte)0xEF, bytes[7] );
        assertEquals( 0x56, bytes[8] );
        assertEquals( 0x78, bytes[9] );
        assertEquals( (byte)0xCD, bytes[10] );
        assertEquals( (byte)0xEF, bytes[11] );
        
        assertEquals( "test", new String( bytes, 12, bytes.length - 12 ) );
        
        CSN deserializedCSN = new SimpleCSN( bytes );
        assertEquals( csn, deserializedCSN );
    }
}
