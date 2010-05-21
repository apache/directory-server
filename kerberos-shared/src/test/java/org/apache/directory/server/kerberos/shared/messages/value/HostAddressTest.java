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
package org.apache.directory.server.kerberos.shared.messages.value;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.server.kerberos.shared.messages.value.types.HostAddrType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the HostAddress encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 542147 $, $Date: 2007-05-28 10:14:21 +0200 (Mon, 28 May 2007) $
 */
public class HostAddressTest
{
    HostAddress hostAddressA;
    HostAddress hostAddressACopy;
    HostAddress hostAddressB;
    HostAddress hostAddressC;
    HostAddress hostAddressD;

    
    /**
     * Initialize name instances
     */
    @Before
    public void initNames() throws Exception
    {
        hostAddressA = new HostAddress( HostAddrType.ADDRTYPE_INET, new byte[]
                                                                            { 0x01, 0x02, 0x03, 0x04 } );
        hostAddressACopy = new HostAddress( HostAddrType.ADDRTYPE_INET, new byte[]
                                                                                { 0x01, 0x02, 0x03, 0x04 } );
        hostAddressB = new HostAddress( HostAddrType.ADDRTYPE_INET, new byte[]
                                                                            { 0x01, 0x02, 0x03, 0x04 } );
        hostAddressC = new HostAddress( HostAddrType.ADDRTYPE_INET, new byte[]
                                                                            { 0x01, 0x02, 0x03, 0x05 } );
        hostAddressD = new HostAddress( HostAddrType.ADDRTYPE_CHAOS, new byte[]
                                                                            { 0x01, 0x02, 0x03, 0x04 } );

    }

    
    @Test
    public void testEncodingHostAddressIP() throws Exception
    {
        ByteBuffer encoded = ByteBuffer.allocate( hostAddressA.computeLength() );

        hostAddressA.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
                0x30, 0x0d, 
                  (byte)0xA0, 0x03, 
                    0x02, 0x01, 0x02, 
                  (byte)0xA1, 0x06, 
                    0x04, 0x04, 
                      0x01, 0x02, 0x03, 0x04 
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    @Test
    public void testEncodingHostAddressIPNullAddress() throws Exception
    {
        HostAddress ha = new HostAddress( HostAddrType.ADDRTYPE_INET, null );

        ByteBuffer encoded = ByteBuffer.allocate( ha.computeLength() );

        ha.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
              0x30, 0x09, 
                (byte)0xA0, 0x03, 
                  0x02, 0x01, 0x02, 
                (byte)0xA1, 0x02, 
                  0x04, 0x00 
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    @Test
    public void testEqualsNull() throws Exception
    {
        assertFalse( hostAddressA.equals( null ) );
    }


    @Test
    public void testEqualsReflexive() throws Exception
    {
        assertEquals( hostAddressA, hostAddressA );
    }


    @Test
    public void testHashCodeReflexive() throws Exception
    {
        assertEquals( hostAddressA.hashCode(), hostAddressA.hashCode() );
    }


    @Test
    public void testEqualsSymmetric() throws Exception
    {
        assertEquals( hostAddressA, hostAddressACopy );
        assertEquals( hostAddressACopy, hostAddressA );
    }


    @Test
    @Ignore
    public void testHashCodeSymmetric() throws Exception
    {
        assertEquals( hostAddressA.hashCode(), hostAddressACopy.hashCode() );
        assertEquals( hostAddressACopy.hashCode(), hostAddressA.hashCode() );
    }


    @Test
    public void testEqualsTransitive() throws Exception
    {
        assertEquals( hostAddressA, hostAddressACopy );
        assertEquals( hostAddressACopy, hostAddressB );
        assertEquals( hostAddressA, hostAddressB );
    }


    @Test
    @Ignore
    public void testHashCodeTransitive() throws Exception
    {
        assertEquals( hostAddressA.hashCode(), hostAddressACopy.hashCode() );
        assertEquals( hostAddressACopy.hashCode(), hostAddressB.hashCode() );
        assertEquals( hostAddressA.hashCode(), hostAddressB.hashCode() );
    }


    @Test
    public void testNotEqualDiffValue() throws Exception
    {
        assertFalse( hostAddressA.equals( hostAddressC ) );
        assertFalse( hostAddressC.equals( hostAddressA ) );
        assertFalse( hostAddressA.equals( hostAddressD ) );
        assertFalse( hostAddressD.equals( hostAddressA ) );
    }
}
