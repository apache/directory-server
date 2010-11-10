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

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.codec.authorizationData.AuthorizationDataContainer;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
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

        ByteBuffer stream = ByteBuffer.allocate( 0x24 );
        
        stream.put( new byte[]
            { 
              0x30, 0x22,
                0x30, 0x0F,
                  (byte)0xA0, 0x03,                 // ad-type
                    0x02, 0x01, 0x02,
                  (byte)0xA1, 0x08,                 // ad-data
                    0x04, 0x06, 'a', 'b', 'c', 'd', 'e', 'f',
                0x30, 0x0F,
                  (byte)0xA0, 0x03,                 // ad-type
                    0x02, 0x01, 0x02,
                  (byte)0xA1, 0x08,                 // ad-data
                    0x04, 0x06, 'g', 'h', 'i', 'j', 'k', 'l'
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
        
        String[] expected = new String[]{ "abcdef", "ghijkl" };
        int i = 0;
        
        for ( AuthorizationData.AD ad : authData.getAuthorizationData() )
        {
            assertEquals( 2, ad.getAdType() );
            assertTrue( Arrays.equals( StringTools.getBytesUtf8( expected[i++] ), ad.getAdData() ) );
            
        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( authData.computeLength() );
        
        try
        {
            bb = authData.encode( bb );
    
            // Check the length
            assertEquals( 0x24, bb.limit() );
    
            String encodedPdu = StringTools.dumpBytes( bb.array() );
    
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            
            fail();
        }
    }
    
    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithoutType() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );
        
        stream.put( new byte[]
            { 0x30, 0x5,
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

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );
        
        stream.put( new byte[]
            { 0x30, 0x5,
                (byte)0xA0, 0x03,                 // ad-data
                  0x02, 0x01, 0x02
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithIncorrectPdu() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );
        
        stream.put( new byte[]
            { 0x30, 0x0 } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithEmptyData() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0xB );
        
        stream.put( new byte[]
            { 
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

    
    @Test( expected = DecoderException.class)
    public void testAuthorizationDataWithEmptyType() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0xB );
        
        stream.put( new byte[]
            { 
                0x30, 0x09,
                  (byte)0xA0, 0x02,                 // ad-type
                    0x02, 0x00,
                  (byte)0xA1, 0x03,                 // ad-data
                    0x04, 0x01, 0x02
            } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();
        
        kerberosDecoder.decode( stream, authDataContainer );
        fail();
    }

}
