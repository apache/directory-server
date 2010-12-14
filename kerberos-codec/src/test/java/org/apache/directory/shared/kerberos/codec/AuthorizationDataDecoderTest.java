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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.authorizationData.AuthorizationDataContainer;
import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.AuthorizationDataEntry;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;

/**
 * Test cases for AuthorizationData decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthorizationDataDecoderTest
{

    @Test
    public void testAuthorizationData()
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x21 );
        
        stream.put( new byte[]
            { 
              0x30, 0x1F,
                0x30, 0x0F,
                  (byte)0xA0, 0x03,                 // ad-type
                    0x02, 0x01, 0x02,
                  (byte)0xA1, 0x08,                 // ad-data
                    0x04, 0x06, 'a', 'b', 'c', 'd', 'e', 'f',
                0x30, 0x0C,
                  (byte)0xA0, 0x03,                 // ad-type
                    0x02, 0x01, 0x02,
                  (byte)0xA1, 0x05,                 // ad-data
                    0x04, 0x03, 'g', 'h', 'i'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        // Decode the AuthorizationData PDU
        try
        {
            kerberosDecoder.decode( stream, authDataContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        AuthorizationData authData = authDataContainer.getAuthorizationData();
        
        assertNotNull( authData.getAuthorizationData().size() );
        assertEquals( 2, authData.getAuthorizationData().size() );
        
        String[] expected = new String[]{ "abcdef", "ghi" };
        int i = 0;
        
        for ( AuthorizationDataEntry ad : authData.getAuthorizationData() )
        {
            assertEquals( AuthorizationType.AD_INTENDED_FOR_SERVER, ad.getAdType() );
            assertTrue( Arrays.equals( StringTools.getBytesUtf8( expected[i++] ), ad.getAdData() ) );
            
        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( authData.computeLength() );
        
        try
        {
            bb = authData.encode( bb );
    
            // Check the length
            assertEquals( 0x21, bb.limit() );
    
            String encodedPdu = StringTools.dumpBytes( bb.array() );
    
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataEmptyPdu() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );
        
        stream.put( new byte[]
            { 
              0x30, 0x00,
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithNoInnerData() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );
        
        stream.put( new byte[]
            { 
              0x30, 0x02,
               0x30, 0x00 
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataEmptyTypeTag() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );
        
        stream.put( new byte[]
            { 
              0x30, 0x04,
               0x30, 0x02,
                (byte)0xA0, 0x00                 // ad-data
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }


    @Test( expected = DecoderException.class)
    public void testAuthorizationDataEmptyTypeValue() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x08 );
        
        stream.put( new byte[]
            { 
              0x30, 0x06,
               0x30, 0x04,
                (byte)0xA0, 0x02,                 // ad-data
                  0x02, 0x00
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }
    
    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithoutType() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );
        
        stream.put( new byte[]
            { 
              0x30, 0x07,
               0x30, 0x05,
                (byte)0xA1, 0x03,                 // ad-data
                  0x04, 0x01, 'a'
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }
    
    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithoutData() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );
        
        stream.put( new byte[]
            { 
              0x30, 0x07,
               0x30, 0x05,
                (byte)0xA0, 0x03,                 // ad-data
                  0x02, 0x01, 0x02
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithEmptyDataTag() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0xB );
        
        stream.put( new byte[]
            { 
               0x30, 0x09,
                0x30, 0x07,
                  (byte)0xA0, 0x03,                 // ad-type
                    0x02, 0x01, 0x02,
                  (byte)0xA1, 0x00                  // ad-data
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithEmptyData() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0xD );
        
        stream.put( new byte[]
            { 
               0x30, 0x0B,
                0x30, 0x09,
                  (byte)0xA0, 0x03,                 // ad-type
                    0x02, 0x01, 0x02,
                  (byte)0xA1, 0x02,                 // ad-data
                    0x04, 0x00
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }
}
