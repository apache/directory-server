/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.EncKrbPrivPartContainer;
import org.apache.directory.shared.kerberos.components.EncKrbPrivPart;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;

/**
 * Test cases for EncKrbPrivPart codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncKrbPrivPartDecoderTest
{
    @Test
    public void testDecodeEncKrbPrivPart() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x49;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x7C, 0x47,
            0x30, 0x45,
              (byte)0xA0, 0x4,        // user-data
                     0x04, 0x02, 0x00, 0x01,
              (byte)0xA1, 0x11,       // timestamp
                     0x18, 0xF, '2', '0', '1', '0', '1', '1', '1', '9', '0', '8', '0', '0', '4', '3', 'Z',
              (byte)0xA2, 0x03,       // usec
                     0x02, 0x01, 0x01,
              (byte)0xA3, 0x03,       // seq-number
                     0x02, 0x01, 0x01,
              (byte)0xA4, 0xF,        // s-address
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1,
              (byte)0xA5, 0xF,        // r-adress
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1
        } );

        String decoded = Strings.dumpBytes(stream.array());
        stream.flip();
        
        EncKrbPrivPartContainer container = new EncKrbPrivPartContainer( stream );
        
        try
        {
            decoder.decode( stream, container );
        }
        catch( DecoderException e )
        {
            fail();
        }
        
        EncKrbPrivPart encKrbPrivPart = container.getEncKrbPrivPart();
        
        String time = "20101119080043Z";
        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );
        
        assertTrue( Arrays.equals( new byte[]{0,1}, encKrbPrivPart.getUserData() ) );
        assertEquals( time, encKrbPrivPart.getTimestamp().getDate() );
        assertEquals( 1, encKrbPrivPart.getUsec() );
        assertEquals( 1, encKrbPrivPart.getSeqNumber() );
        assertEquals( ad, encKrbPrivPart.getSenderAddress() );
        assertEquals( ad, encKrbPrivPart.getRecipientAddress() );

        int computedLen = encKrbPrivPart.computeLength();
        
        assertEquals( streamLen, computedLen );
        
        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );
            
            encKrbPrivPart.encode( bb );
            
            String encoded = Strings.dumpBytes(bb.array());
            assertEquals( decoded, encoded );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }
    
    
    @Test
    public void testDecodeEncKrbPrivPartWithoutTimestamp() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x36;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x7C, 0x34,
            0x30, 0x32,
              (byte)0xA0, 0x4,        // user-data
                     0x04, 0x02, 0x00, 0x01,
                     // NO timestamp
              (byte)0xA2, 0x03,       // usec
                     0x02, 0x01, 0x01,
              (byte)0xA3, 0x03,       // seq-number
                     0x02, 0x01, 0x01,
              (byte)0xA4, 0xF,        // s-address
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1,
              (byte)0xA5, 0xF,        // r-adress
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1
        } );

        String decoded = Strings.dumpBytes(stream.array());
        stream.flip();
        
        EncKrbPrivPartContainer container = new EncKrbPrivPartContainer( stream );
        
        try
        {
            decoder.decode( stream, container );
        }
        catch( DecoderException e )
        {
            fail();
        }
        
        EncKrbPrivPart enKrbPrivPart = container.getEncKrbPrivPart();
        
        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );
        
        assertTrue( Arrays.equals( new byte[]{0,1}, enKrbPrivPart.getUserData() ) );
        assertNull( enKrbPrivPart.getTimestamp() );
        assertEquals( 1, enKrbPrivPart.getUsec() );
        assertEquals( 1, enKrbPrivPart.getSeqNumber() );
        assertEquals( ad, enKrbPrivPart.getSenderAddress() );
        assertEquals( ad, enKrbPrivPart.getRecipientAddress() );

        int computedLen = enKrbPrivPart.computeLength();
        
        assertEquals( streamLen, computedLen );
        
        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );
            
            enKrbPrivPart.encode( bb );
            
            String encoded = Strings.dumpBytes(bb.array());
            assertEquals( decoded, encoded );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }

    
    @Test
    public void testDecodeEncKrbPrivPartWithoutTimestampAndUsec() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x31;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x7C, 0x2F,
            0x30, 0x2D,
              (byte)0xA0, 0x4,        // user-data
                     0x04, 0x02, 0x00, 0x01,
                     // NO timestamp and usec
              (byte)0xA3, 0x03,       // seq-number
                     0x02, 0x01, 0x01,
              (byte)0xA4, 0xF,        // s-address
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1,
              (byte)0xA5, 0xF,        // r-adress
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1
        } );

        String decoded = Strings.dumpBytes(stream.array());
        stream.flip();
        
        EncKrbPrivPartContainer container = new EncKrbPrivPartContainer( stream );
        
        try
        {
            decoder.decode( stream, container );
        }
        catch( DecoderException e )
        {
            fail();
        }
        
        EncKrbPrivPart encKrbPrivPart = container.getEncKrbPrivPart();
        
        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );
        
        assertTrue( Arrays.equals( new byte[]{0,1}, encKrbPrivPart.getUserData() ) );
        assertNull( encKrbPrivPart.getTimestamp() );
        assertEquals( 0, encKrbPrivPart.getUsec() );
        assertEquals( 1, encKrbPrivPart.getSeqNumber() );
        assertEquals( ad, encKrbPrivPart.getSenderAddress() );
        assertEquals( ad, encKrbPrivPart.getRecipientAddress() );

        int computedLen = encKrbPrivPart.computeLength();
        
        assertEquals( streamLen, computedLen );
        
        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );
            
            encKrbPrivPart.encode( bb );
            
            String encoded = Strings.dumpBytes(bb.array());
            assertEquals( decoded, encoded );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }

    
    @Test
    public void testDecodeEncKrbPrivPartWithoutTimestampUsecAndSeqNumber() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x2C;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x7C, 0x2A,
            0x30, 0x28,
              (byte)0xA0, 0x4,        // user-data
                     0x04, 0x02, 0x00, 0x01,
                     // NO timestamp, usec and seq-number
              (byte)0xA4, 0xF,        // s-address
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1,
              (byte)0xA5, 0xF,        // r-adress
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1
        } );

        String decoded = Strings.dumpBytes(stream.array());
        stream.flip();
        
        EncKrbPrivPartContainer container = new EncKrbPrivPartContainer( stream );
        
        try
        {
            decoder.decode( stream, container );
        }
        catch( DecoderException e )
        {
            e.printStackTrace();
            fail();
        }
        
        EncKrbPrivPart encKrbPrivPart = container.getEncKrbPrivPart();
        
        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );
        
        assertTrue( Arrays.equals( new byte[]{0,1}, encKrbPrivPart.getUserData() ) );
        assertNull( encKrbPrivPart.getTimestamp() );
        assertEquals( 0, encKrbPrivPart.getUsec() );
        assertEquals( 0, encKrbPrivPart.getSeqNumber() );
        assertEquals( ad, encKrbPrivPart.getSenderAddress() );
        assertEquals( ad, encKrbPrivPart.getRecipientAddress() );

        int computedLen = encKrbPrivPart.computeLength();
        
        assertEquals( streamLen, computedLen );
        
        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );
            
            encKrbPrivPart.encode( bb );
            
            String encoded = Strings.dumpBytes(bb.array());
            assertEquals( decoded, encoded );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }

    
    @Test
    public void testDecodeEncKrbPrivPartWithoutSequenceNumber() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x44;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x7C, 0x42,
            0x30, 0x40,
              (byte)0xA0, 0x4,        // user-data
                     0x04, 0x02, 0x00, 0x01,
              (byte)0xA1, 0x11,       // timestamp
                     0x18, 0xF, '2', '0', '1', '0', '1', '1', '1', '9', '0', '8', '0', '0', '4', '3', 'Z',
              (byte)0xA2, 0x03,       // usec
                     0x02, 0x01, 0x01,
                     // NO seq-number
              (byte)0xA4, 0xF,        // s-address
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1,
              (byte)0xA5, 0xF,        // r-adress
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1
        } );

        String decoded = Strings.dumpBytes(stream.array());
        stream.flip();
        
        EncKrbPrivPartContainer container = new EncKrbPrivPartContainer( stream );
        
        try
        {
            decoder.decode( stream, container );
        }
        catch( DecoderException e )
        {
            fail();
        }
        
        EncKrbPrivPart encKrbPrivPart = container.getEncKrbPrivPart();
        
        String time = "20101119080043Z";
        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );
        
        assertTrue( Arrays.equals( new byte[]{0,1}, encKrbPrivPart.getUserData() ) );
        assertEquals( time, encKrbPrivPart.getTimestamp().getDate() );
        assertEquals( 1, encKrbPrivPart.getUsec() );
        assertEquals( 0, encKrbPrivPart.getSeqNumber() );
        assertEquals( ad, encKrbPrivPart.getSenderAddress() );
        assertEquals( ad, encKrbPrivPart.getRecipientAddress() );

        int computedLen = encKrbPrivPart.computeLength();
        
        assertEquals( streamLen, computedLen );
        
        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );
            
            encKrbPrivPart.encode( bb );
            
            String encoded = Strings.dumpBytes(bb.array());
            assertEquals( decoded, encoded );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }

    
    @Test
    public void testDecodeEncKrbPrivPartWithoutOptionalValues() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x1B;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x7C, 0x19,
            0x30, 0x17,
              (byte)0xA0, 0x4,        // user-data
                     0x04, 0x02, 0x00, 0x01,
                     // NO timestamp, usec and seq-number
              (byte)0xA4, 0xF,        // s-address
                     0x30, 0x0D,
                      (byte)0xA0, 0x03,
                             0x02, 0x01, 0x02,
                       (byte)0xA1, 0x06,
                              0x04, 0x04, 127, 0, 0, 1,
                    // NO r-address
        } );

        String decoded = Strings.dumpBytes(stream.array());
        stream.flip();
        
        EncKrbPrivPartContainer container = new EncKrbPrivPartContainer( stream );
        
        try
        {
            decoder.decode( stream, container );
        }
        catch( DecoderException e )
        {
            e.printStackTrace();
            fail();
        }
        
        EncKrbPrivPart encKrbPrivPart = container.getEncKrbPrivPart();
        
        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );
        
        assertTrue( Arrays.equals( new byte[]{0,1}, encKrbPrivPart.getUserData() ) );
        assertNull( encKrbPrivPart.getTimestamp() );
        assertEquals( 0, encKrbPrivPart.getUsec() );
        assertEquals( 0, encKrbPrivPart.getSeqNumber() );
        assertEquals( ad, encKrbPrivPart.getSenderAddress() );
        assertNull( encKrbPrivPart.getRecipientAddress() );

        int computedLen = encKrbPrivPart.computeLength();
        
        assertEquals( streamLen, computedLen );
        
        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );
            
            encKrbPrivPart.encode( bb );
            
            String encoded = Strings.dumpBytes(bb.array());
            assertEquals( decoded, encoded );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }

}
