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
package org.apache.directory.shared.ldap.csn;


import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * 
 * Test for the CSN class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CSNTest
{
    private SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMddHHmmss.123456'Z'" );

    @Test
    public void testCSN()
    {
        long ts = System.currentTimeMillis();

        CSN csn = new CSN( sdf.format( new Date( ts ) ) + "#123456#abc#654321" );

        assertEquals( ts/1000, csn.getTimestamp()/1000 );
        
        // ALl the value are converted from hex to int
        assertEquals( 1193046, csn.getChangeCount() );
        assertEquals( 6636321, csn.getOperationNumber() );
        assertEquals( 2748, csn.getReplicaId() );
    }


    @Test
    public void testCSNNull()
    {
        try
        {
            new CSN( (String)null );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNEmpty()
    {
        try
        {
            new CSN( "" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNTimestampOnly()
    {
        try
        {
            new CSN( sdf.format( new Date( System.currentTimeMillis() ) ) );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNInvalidTS()
    {
        try
        {
            // A missing 'Z'
            new CSN( "20010101000000.000000#000001#abc#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
        
        try
        {
            // Missing milliseconds
            new CSN( "20000101000000.Z#000001#abc#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }

        try
        {
            // Missing dot
            new CSN( "20010101000000000000Z#0x1#abc#0x1" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }

        try
        {
            // Missing dot and millis
            new CSN( "20010101000000Z#000001#abc#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }

        try
        {
            // Invalid date
            new CSN( "200A01010000Z#000001#abc#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNNoTimestamp()
    {
        try
        {
            new CSN( "#000001#abc#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNNoChangeCount()
    {
        try
        {
            new CSN( "20010101000000.000000Z##abc#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNInvalidChangeCount()
    {
        try
        {
            new CSN( "20010101000000.000000Z#00#abc#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }

        try
        {
            new CSN( "20010101000000.000000Z#00000G#abc#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }
    
    
    @Test
    public void testCSNNoReplica()
    {
        try
        {
            new CSN( "20010101000000.000000Z#000001##000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }
    
    
    @Test
    public void testCSNInvalidReplica()
    {
        try
        {
            new CSN( "20010101000000.000000Z#000001#a12-b3é#000001" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNNoOpNumber()
    {
        try
        {
            new CSN( "20010101000000.000000Z#000000#abc" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
        
        try
        {
            new CSN( "20010101000000.000000Z#000000#abc#  " );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNInvalidOpNumber()
    {
        try
        {
            new CSN( "20010101000000.000000Z#000000#abc#000zzz" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }

        try
        {
            new CSN( "20010101000000.000000Z#000000#abc#00000" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }

        try
        {
            new CSN( "20010101000000.000000Z#000000#abc#" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }

        try
        {
            new CSN( "20010101000000.000000Z#000000#abc#00000G" );
            fail();
        }
        catch ( InvalidCSNException ice )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCSNToBytes()
    {
        CSN csn = new CSN( "20010101000000.000000Z#000000#abc#000001" );

        byte[] bytes = csn.toBytes();

        byte[] expected = new byte[]
            { 
                '2', '0', '0', '1', '0', '1', '0', '1', 
                '0', '0', '0', '0', '0', '0', '.', '0',
                '0', '0', '0', '0', '0', 'Z', '#', '0', 
                '0', '0', '0', '0', '0', '#', 'a', 'b', 
                'c', '#', '0', '0', '0', '0', '0', '1' 
            };
        
        assertTrue( Arrays.equals( expected, bytes ) );

        CSN deserializedCSN = new CSN( bytes );
        assertEquals( csn, deserializedCSN );
    }
}
