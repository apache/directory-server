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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.asn1.ber.tlv.Length;


/**
 * This class is used to test the Length class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LengthTest extends TestCase
{

    /**
     * Test the getNbBytes method
     */
    public void testLengthGetNbBytes()
    {
        Assert.assertEquals( 1, Length.getNbBytes( 0 ) );
        Assert.assertEquals( 1, Length.getNbBytes( 1 ) );
        Assert.assertEquals( 1, Length.getNbBytes( 127 ) );
        Assert.assertEquals( 2, Length.getNbBytes( 128 ) );
        Assert.assertEquals( 2, Length.getNbBytes( 255 ) );
        Assert.assertEquals( 3, Length.getNbBytes( 256 ) );
        Assert.assertEquals( 3, Length.getNbBytes( 65535 ) );
        Assert.assertEquals( 4, Length.getNbBytes( 65536 ) );
        Assert.assertEquals( 4, Length.getNbBytes( 16777215 ) );
        Assert.assertEquals( 5, Length.getNbBytes( 16777216 ) );
        Assert.assertEquals( 5, Length.getNbBytes( 0xFFFFFFFF ) );
    }


    /**
     * Test the getBytes method
     */
    public void testLengthGetBytes()
    {
        assertTrue( Arrays.equals( new byte[]
            { 0x01 }, Length.getBytes( 1 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { 0x7F }, Length.getBytes( 127 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x81, ( byte ) 0x80 }, Length.getBytes( 128 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x81, ( byte ) 0xFF }, Length.getBytes( 255 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x82, 0x01, 0x00 }, Length.getBytes( 256 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x82, ( byte ) 0xFF, ( byte ) 0xFF }, Length.getBytes( 65535 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x83, 0x01, 0x00, 0x00 }, Length.getBytes( 65536 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x83, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF }, Length.getBytes( 16777215 ) ) );
        assertTrue( Arrays.equals( new byte[]
            { ( byte ) 0x84, 0x01, 0x00, 0x00, 0x00 }, Length.getBytes( 16777216 ) ) );
        assertTrue( Arrays
            .equals( new byte[]
                { ( byte ) 0x84, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF }, Length
                .getBytes( 0xFFFFFFFF ) ) );
    }
}
