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
import org.apache.directory.shared.kerberos.codec.methodData.MethodDataContainer;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.components.MethodData;
import org.apache.directory.shared.kerberos.components.PaData;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the METHOD-DATA decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class MethodDataDecoderTest
{
    /**
     * Test the decoding of a METHOD-DATA
     */
    @Test
    public void testMethodData()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x24 );

        stream.put( new byte[]
            {
                0x30, 0x22,
                0x30, 0x0F,
                ( byte ) 0xA1, 0x03, // padata-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA2,
                0x08, // padata-value
                0x04,
                0x06,
                'a',
                'b',
                'c',
                'd',
                'e',
                'f',
                0x30,
                0x0F,
                ( byte ) 0xA1,
                0x03, // padata-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA2,
                0x08, // padata-value
                0x04,
                0x06,
                'g',
                'h',
                'i',
                'j',
                'k',
                'l'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a METHOD-DATA Container
        Asn1Container methodDataContainer = new MethodDataContainer();
        methodDataContainer.setStream( stream );

        // Decode the MethodData PDU
        try
        {
            Asn1Decoder.decode( stream, methodDataContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded ETypeInfo
        MethodData methodData = ( ( MethodDataContainer ) methodDataContainer ).getMethodData();

        assertEquals( 2, methodData.getPaDatas().length );

        String[] expected = new String[]
            { "abcdef", "ghijkl" };
        int i = 0;

        for ( PaData paData : methodData.getPaDatas() )
        {
            assertEquals( PaDataType.PA_ENC_TIMESTAMP, paData.getPaDataType() );
            assertTrue( Arrays.equals( Strings.getBytesUtf8( expected[i] ), paData.getPaDataValue() ) );
            i++;
        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( methodData.computeLength() );

        try
        {
            bb = methodData.encode( bb );

            // Check the length
            assertEquals( 0x24, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a METHOD-DATA with nothing in it
     */
    @Test(expected = DecoderException.class)
    public void testETypeInfoEmpty() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a METHOD-DATA Container
        Asn1Container methodDataContainer = new MethodDataContainer();

        // Decode the METHOD-DATA PDU
        Asn1Decoder.decode( stream, methodDataContainer );
        fail();
    }


    /**
     * Test the decoding of a METHOD-DATA with empty PA-DATA in it
     */
    @Test(expected = DecoderException.class)
    public void testETypeInfoNoETypeInfoEntry() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                ( byte ) 0x30, 0x00 // empty PA-DATA
        } );

        stream.flip();

        // Allocate a METHOD-DATA Container
        Asn1Container methodDataContainer = new MethodDataContainer();

        // Decode the METHOD-DATA PDU
        Asn1Decoder.decode( stream, methodDataContainer );
        fail();
        fail();
    }
}
