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
package org.apache.directory.shared.kerberos.codec;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.etypeInfo.ETypeInfoContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.ETypeInfo;
import org.apache.directory.shared.kerberos.components.ETypeInfoEntry;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the ETYPE-INFO decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class ETypeInfoDecoderTest
{
    /**
     * Test the decoding of a ETYPE-INFO
     */
    @Test
    public void testETypeInfo()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x20 );

        stream.put( new byte[]
            {
                0x30, 0x1E,
                0x30, 0x0D,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x06, // salt
                0x04,
                0x04,
                0x31,
                0x32,
                0x33,
                0x34,
                0x30,
                0x0D,
                ( byte ) 0xA0,
                0x03, // etype
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x06, // salt
                0x04,
                0x04,
                0x35,
                0x36,
                0x37,
                0x38
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a ETypeInfo Container
        Asn1Container etypeInfoContainer = new ETypeInfoContainer();
        etypeInfoContainer.setStream( stream );

        // Decode the ETypeInfo PDU
        try
        {
            Asn1Decoder.decode( stream, etypeInfoContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded ETypeInfo
        ETypeInfo etypeInfo = ( ( ETypeInfoContainer ) etypeInfoContainer ).getETypeInfo();

        assertEquals( 2, etypeInfo.getETypeInfoEntries().length );

        String[] expected = new String[]
            { "1234", "5678" };
        int i = 0;

        for ( ETypeInfoEntry etypeInfoEntry : etypeInfo.getETypeInfoEntries() )
        {
            assertEquals( EncryptionType.DES3_CBC_MD5, etypeInfoEntry.getEType() );
            assertTrue( Arrays.equals( Strings.getBytesUtf8( expected[i] ), etypeInfoEntry.getSalt() ) );
            i++;
        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( etypeInfo.computeLength() );

        try
        {
            bb = etypeInfo.encode( bb );

            // Check the length
            assertEquals( 0x20, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a ETypeInfo with nothing in it
     */
    @Test(expected = DecoderException.class)
    public void testETypeInfoEmpty() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a ETypeInfo Container
        Asn1Container etypeInfoContainer = new ETypeInfoContainer();

        // Decode the ETypeInfo PDU
        Asn1Decoder.decode( stream, etypeInfoContainer );
        fail();
    }


    /**
     * Test the decoding of a ETypeInfo with empty ETypeInfoEntry in it
     */
    @Test(expected = DecoderException.class)
    public void testETypeInfoNoETypeInfoEntry() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                ( byte ) 0x30, 0x00 // empty ETypeInfoEntry
        } );

        stream.flip();

        // Allocate a ETypeInfo Container
        Asn1Container etypeInfoContainer = new ETypeInfoContainer();

        // Decode the ETypeInfo PDU
        Asn1Decoder.decode( stream, etypeInfoContainer );
        fail();
    }
}
