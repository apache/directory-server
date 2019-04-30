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

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.adMandatoryForKdc.AdMandatoryForKdcContainer;
import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;
import org.apache.directory.shared.kerberos.components.AdMandatoryForKdc;
import org.apache.directory.shared.kerberos.components.AuthorizationDataEntry;
import org.junit.Test;


/**
 * Test cases for AD-MANDATORY-FOR-KDC decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdMandatoryForKdcDecoderTest
{

    @Test
    public void testAdMandatoryForKdc()
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x24 );

        stream.put( new byte[]
            {
                0x30, 0x22,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03, // ad-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x08, // ad-data
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
                ( byte ) 0xA0,
                0x03, // ad-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x08, // ad-data
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

        AdMandatoryForKdcContainer adMandatoryForKdcContainer = new AdMandatoryForKdcContainer();

        // Decode the AdMandatoryForKdc PDU
        try
        {
            Asn1Decoder.decode( stream, adMandatoryForKdcContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        AdMandatoryForKdc adMandatoryForKdc = adMandatoryForKdcContainer.getAdMandatoryForKdc();

        assertNotNull( adMandatoryForKdc.getAuthorizationData().size() );
        assertEquals( 2, adMandatoryForKdc.getAuthorizationData().size() );

        String[] expected = new String[]
            { "abcdef", "ghijkl" };
        int i = 0;

        for ( AuthorizationDataEntry ad : adMandatoryForKdc.getAuthorizationData() )
        {
            assertEquals( AuthorizationType.AD_INTENDED_FOR_SERVER, ad.getAdType() );
            assertTrue( Arrays.equals( Strings.getBytesUtf8( expected[i++] ), ad.getAdData() ) );

        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( adMandatoryForKdc.computeLength() );

        try
        {
            bb = adMandatoryForKdc.encode( bb );

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
}
