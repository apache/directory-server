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
package org.apache.directory.shared.asn1.ber.tlv;


import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class is used to test the Length class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LengthTest
{

    /**
     * Test the getNbBytes method
     */
    @Test
    public void testLengthGetNbBytes()
    {
        assertEquals( "1 expected", 1, TLV.getNbBytes( 0 ) );
        assertEquals( "1 expected", 1, TLV.getNbBytes( 1 ) );
        assertEquals( "1 expected", 1, TLV.getNbBytes( 127 ) );
        assertEquals( "2 expected", 2, TLV.getNbBytes( 128 ) );
        assertEquals( "2 expected", 2, TLV.getNbBytes( 255 ) );
        assertEquals( "3 expected", 3, TLV.getNbBytes( 256 ) );
        assertEquals( "3 expected", 3, TLV.getNbBytes( 65535 ) );
        assertEquals( "4 expected", 4, TLV.getNbBytes( 65536 ) );
        assertEquals( "4 expected", 4, TLV.getNbBytes( 16777215 ) );
        assertEquals( "5 expected", 5, TLV.getNbBytes( 16777216 ) );
        assertEquals( "5 expected", 5, TLV.getNbBytes( 0xFFFFFFFF ) );
    }


    /**
     * Test the getBytes method
     */
    @Test
    public void testLengthGetBytes()
    {
        assertTrue( Arrays.equals( new byte[]
            { 0x01 }, TLV.getBytes( 1 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { 0x7F }, TLV.getBytes( 127 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x81, ( byte ) 0x80 }, TLV.getBytes( 128 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x81, ( byte ) 0xFF }, TLV.getBytes( 255 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x82, 0x01, 0x00 }, TLV.getBytes( 256 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x82, ( byte ) 0xFF, ( byte ) 0xFF }, TLV.getBytes( 65535 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x83, 0x01, 0x00, 0x00 }, TLV.getBytes( 65536 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x83, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF }, TLV.getBytes( 16777215 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x84, 0x01, 0x00, 0x00, 0x00 }, TLV.getBytes( 16777216 ) ) );
        assertTrue( Arrays
            .equals( new byte[]
                { ( byte ) 0x84, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF }, TLV
                .getBytes( 0xFFFFFFFF ) ) );
    }
}
