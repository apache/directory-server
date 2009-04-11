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
package org.apache.directory.shared.ldap.codec.extended.operations;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.extended.operations.gracefulDisconnect.GracefulDisconnect;
import org.apache.directory.shared.ldap.codec.extended.operations.gracefulDisconnect.GracefulDisconnectContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.gracefulDisconnect.GracefulDisconnectDecoder;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test the GracefulDisconnectTest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GracefulDisconnectTest
{
    /**
     * Test the decoding of a GracefulDisconnect
     */
    @Test
    public void testDecodeGracefulDisconnectSuccess()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer stream = ByteBuffer.allocate( 0x70 );
        stream.put( new byte[]
            { 0x30, 0x6E, // GracefulDisconnec ::= SEQUENCE {
                0x02, 0x01, 0x01, // timeOffline INTEGER (0..720) DEFAULT 0,
                ( byte ) 0x80, 0x01, 0x01, // delay INTEGER (0..86400) DEFAULT
                                            // 0
                // replicatedContexts Referral OPTIONAL
                0x30, 0x66, 0x04, 0x1F, 'l', 'd', 'a', 'p', ':', '/', '/', 'd', 'i', 'r', 'e', 'c', 't', 'o', 'r', 'y',
                '.', 'a', 'p', 'a', 'c', 'h', 'e', '.', 'o', 'r', 'g', ':', '8', '0', '/', 0x04, 0x43, 'l', 'd', 'a',
                'p', ':', '/', '/', 'l', 'd', 'a', 'p', '.', 'n', 'e', 't', 's', 'c', 'a', 'p', 'e', '.', 'c', 'o',
                'm', '/', 'o', '=', 'B', 'a', 'b', 's', 'c', 'o', ',', 'c', '=', 'U', 'S', '?', '?', '?', '(', 'i',
                'n', 't', '=', '%', '5', 'c', '0', '0', '%', '5', 'c', '0', '0', '%', '5', 'c', '0', '0', '%', '5',
                'c', '0', '4', ')'
            // }
            } );
        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 1, gracefulDisconnect.getTimeOffline() );
        assertEquals( 1, gracefulDisconnect.getDelay() );
        assertEquals( 2, gracefulDisconnect.getReplicatedContexts().size() );
        assertEquals( "ldap://directory.apache.org:80/", gracefulDisconnect.getReplicatedContexts().get( 0 ).toString() );
        assertEquals( "ldap://ldap.netscape.com/o=Babsco,c=US???(int=%5c00%5c00%5c00%5c04)", gracefulDisconnect
            .getReplicatedContexts().get( 1 ).toString() );

        // Check the length
        assertEquals( 0x70, gracefulDisconnect.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = gracefulDisconnect.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }
    

    /**
     * Test the decoding of a GracefulDisconnect with a timeOffline only
     */
    @Test
    public void testDecodeGracefulDisconnectTimeOffline()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 0x30, 0x03, // GracefulDisconnect ::= SEQUENCE {
                0x02, 0x01, 0x01 // timeOffline INTEGER (0..720) DEFAULT 0,
            } );

        String decodedPdu = StringTools.dumpBytes( bb.array() );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 1, gracefulDisconnect.getTimeOffline() );
        assertEquals( 0, gracefulDisconnect.getDelay() );
        assertEquals( 0, gracefulDisconnect.getReplicatedContexts().size() );

        // Check the length
        assertEquals( 0x05, gracefulDisconnect.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb1 = gracefulDisconnect.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb1.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a GracefulDisconnect with a delay only
     */
    @Test
    public void testDecodeGracefulDisconnectDelay()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 0x30, 0x03, // GracefulDisconnect ::= SEQUENCE {
                ( byte ) 0x80, 0x01, 0x01 // delay INTEGER (0..86400) DEFAULT
                                            // 0
            } );

        String decodedPdu = StringTools.dumpBytes( bb.array() );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 0, gracefulDisconnect.getTimeOffline() );
        assertEquals( 1, gracefulDisconnect.getDelay() );
        assertEquals( 0, gracefulDisconnect.getReplicatedContexts().size() );

        // Check the length
        assertEquals( 0x05, gracefulDisconnect.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb1 = gracefulDisconnect.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb1.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a GracefulDisconnect with a timeOffline and a delay
     */
    @Test
    public void testDecodeGracefulDisconnectTimeOfflineDelay()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x08 );
        bb.put( new byte[]
            { 0x30, 0x06, // GracefulDisconnect ::= SEQUENCE {
                0x02, 0x01, 0x01, // timeOffline INTEGER (0..720) DEFAULT 0,
                ( byte ) 0x80, 0x01, 0x01, // timeOffline INTEGER (0..720)
                                            // DEFAULT 0,
            } );

        String decodedPdu = StringTools.dumpBytes( bb.array() );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 1, gracefulDisconnect.getTimeOffline() );
        assertEquals( 1, gracefulDisconnect.getDelay() );
        assertEquals( 0, gracefulDisconnect.getReplicatedContexts().size() );

        // Check the length
        assertEquals( 0x08, gracefulDisconnect.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb2 = gracefulDisconnect.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb2.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a GracefulDisconnect with replicatedContexts only
     */
    @Test
    public void testDecodeGracefulDisconnectReplicatedContextsOnly()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer stream = ByteBuffer.allocate( 0x6A );
        stream.put( new byte[]
            { 
            0x30, 0x68,             // GracefulDisconnec ::= SEQUENCE {
              0x30, 0x66,           // replicatedContexts Referral OPTIONAL
                0x04, 0x1F, 
                  'l', 'd', 'a', 'p', ':', '/', '/', 'd', 'i', 'r', 'e', 'c', 't', 'o', 'r', 'y', '.', 'a',
                  'p', 'a', 'c', 'h', 'e', '.', 'o', 'r', 'g', ':', '8', '0', '/', 
                0x04, 0x43, 
                  'l', 'd', 'a', 'p', ':', '/', '/', 'l', 'd', 'a', 'p', '.', 'n', 'e', 't', 's', 'c', 'a', 
                  'p', 'e', '.', 'c', 'o', 'm', '/', 'o', '=', 'B', 'a', 'b', 's', 'c', 'o', ',', 'c', '=', 
                  'U', 'S', '?', '?', '?', '(', 'i', 'n', 't', '=', '%', '5', 'c', '0', '0', '%', '5', 'c', 
                  '0', '0', '%', '5', 'c', '0', '0', '%', '5', 'c', '0', '4', ')'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 0, gracefulDisconnect.getTimeOffline() );
        assertEquals( 0, gracefulDisconnect.getDelay() );
        assertEquals( 2, gracefulDisconnect.getReplicatedContexts().size() );
        assertEquals( "ldap://directory.apache.org:80/", gracefulDisconnect.getReplicatedContexts().get( 0 ).toString() );
        assertEquals( "ldap://ldap.netscape.com/o=Babsco,c=US???(int=%5c00%5c00%5c00%5c04)", gracefulDisconnect
            .getReplicatedContexts().get( 1 ).toString() );

        // Check the length
        assertEquals( 0x6A, gracefulDisconnect.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = gracefulDisconnect.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a empty GracefulDisconnect
     */
    @Test
    public void testDecodeGracefulDisconnectEmpty()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x02 );
        bb.put( new byte[]
            { 0x30, 0x00 // GracefulDisconnect ::= SEQUENCE {
            } );

        String decodedPdu = StringTools.dumpBytes( bb.array() );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 0, gracefulDisconnect.getTimeOffline() );
        assertEquals( 0, gracefulDisconnect.getDelay() );
        assertEquals( 0, gracefulDisconnect.getReplicatedContexts().size() );

        // Check the length
        assertEquals( 0x02, gracefulDisconnect.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb1 = gracefulDisconnect.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb1.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    // Defensive tests

    /**
     * Test the decoding of a GracefulDisconnect with a timeOffline off limit
     */
    @Test
    public void testDecodeGracefulDisconnectTimeOfflineOffLimit()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            { 0x30, 0x04, // GracefulDisconnect ::= SEQUENCE {
                0x02, 0x02, 0x03, ( byte ) 0xE8 // timeOffline INTEGER (0..720)
                                                // DEFAULT 0,
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a GracefulDisconnect with a delay off limit
     */
    @Test
    public void testDecodeGracefulDisconnectDelayOffLimit()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            { 0x30, 0x05, // GracefulDisconnect ::= SEQUENCE {
                ( byte ) 0x80, 0x03, 0x01, ( byte ) 0x86, ( byte ) 0xA0 // delay
                                                                        // INTEGER
                                                                        // (0..86400)
                                                                        // DEFAULT
                                                                        // 0
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a GracefulDisconnect with an empty TimeOffline
     */
    @Test
    public void testDecodeGracefulDisconnectTimeOfflineEmpty()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            { 0x30, 0x02, // GracefulDisconnect ::= SEQUENCE {
                0x02, 0x00 // timeOffline INTEGER (0..720) DEFAULT 0,
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a GracefulDisconnect with an empty delay
     */
    @Test
    public void testDecodeGracefulDisconnectDelayEmpty()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            { 0x30, 0x02, // GracefulDisconnect ::= SEQUENCE {
                ( byte ) 0x80, 0x00 // delay INTEGER (0..86400) DEFAULT 0
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a GracefulDisconnect with an empty replicated
     * contexts
     */
    @Test
    public void testDecodeGracefulDisconnectReplicatedContextsEmpty()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x04 );
        bb.put( new byte[]
            { 0x30, 0x02, // GracefulDisconnect ::= SEQUENCE {
                0x30, 0x00 // replicatedContexts Referral OPTIONAL
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a GracefulDisconnect with an invalid replicated
     * context
     */
    @Test
    public void testDecodeGracefulDisconnectReplicatedContextsInvalid()
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x06 );
        bb.put( new byte[]
            { 0x30, 0x04, // GracefulDisconnect ::= SEQUENCE {
                0x30, 0x02, // replicatedContexts Referral OPTIONAL
                0x04, 0x00 } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }
}
