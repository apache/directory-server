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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.adKdcIssued.AdKdcIssuedContainer;
import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.AdKdcIssued;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.AuthorizationDataEntry;
import org.apache.directory.shared.kerberos.components.Checksum;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test cases for AD-KDCIssued decoder.
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdKdcIssuedDecoderTest
{
    /**
     * Test the decoding of a AD-KDCIssued message
     */
    @Test
    public void testDecodeAdKdcIssued()
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x60 );

        stream.put( new byte[]
            {
                0x30, 0x5E,
                ( byte ) 0xA0, 0x11,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03, // cksumtype
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x08, // checksum
                0x04,
                0x06,
                'c',
                'h',
                'k',
                's',
                'u',
                'm',
                ( byte ) 0xA1,
                0x0D, // realm
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA2,
                0x14, // sname
                0x30,
                0x12,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0B, // name-string
                0x30,
                0x09,
                0x1B,
                0x07,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                ( byte ) 0xA3,
                0x24, // enc-part
                0x30,
                0x22,
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

        // Allocate a AdKdcIssued Container
        Asn1Container adKdcIssuedContainer = new AdKdcIssuedContainer();
        adKdcIssuedContainer.setStream( stream );

        // Decode the AdKdcIssued PDU
        try
        {
            Asn1Decoder.decode( stream, adKdcIssuedContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded AdKdcIssued
        AdKdcIssued adKdcIssued = ( ( AdKdcIssuedContainer ) adKdcIssuedContainer ).getAdKdcIssued();

        Checksum checksum = adKdcIssued.getAdChecksum();

        assertEquals( ChecksumType.getTypeByValue( 2 ), checksum.getChecksumType() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "chksum" ), checksum.getChecksumValue() ) );

        assertEquals( "EXAMPLE.COM", adKdcIssued.getIRealm() );

        PrincipalName principalName = adKdcIssued.getISName();

        assertNotNull( principalName );
        assertEquals( PrincipalNameType.KRB_NT_PRINCIPAL, principalName.getNameType() );
        assertTrue( principalName.getNames().contains( "hnelson" ) );

        AuthorizationData authData = adKdcIssued.getElements();

        assertNotNull( authData.getAuthorizationData().size() );
        assertEquals( 2, authData.getAuthorizationData().size() );

        String[] expected = new String[]
            { "abcdef", "ghijkl" };
        int i = 0;

        for ( AuthorizationDataEntry ad : authData.getAuthorizationData() )
        {
            assertEquals( AuthorizationType.AD_INTENDED_FOR_SERVER, ad.getAdType() );
            assertTrue( Arrays.equals( Strings.getBytesUtf8( expected[i++] ), ad.getAdData() ) );

        }

        ByteBuffer bb = ByteBuffer.allocate( adKdcIssued.computeLength() );

        // Check the encoding
        try
        {
            bb = adKdcIssued.encode( bb );

            // Check the length
            assertEquals( 0x60, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a AD-KDCIssued message with no optional fields
     */
    @Test
    public void testDecodeAdKdcIssuedNoOptionalFields()
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x3B );

        stream.put( new byte[]
            {
                0x30, 0x39,
                ( byte ) 0xA0, 0x11,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03, // cksumtype
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x08, // checksum
                0x04,
                0x06,
                'c',
                'h',
                'k',
                's',
                'u',
                'm',
                ( byte ) 0xA3,
                0x24, // enc-part
                0x30,
                0x22,
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

        // Allocate a AdKdcIssued Container
        Asn1Container adKdcIssuedContainer = new AdKdcIssuedContainer();
        adKdcIssuedContainer.setStream( stream );

        // Decode the AdKdcIssued PDU
        try
        {
            Asn1Decoder.decode( stream, adKdcIssuedContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded AdKdcIssued
        AdKdcIssued adKdcIssued = ( ( AdKdcIssuedContainer ) adKdcIssuedContainer ).getAdKdcIssued();

        // The checksum
        Checksum checksum = adKdcIssued.getAdChecksum();

        assertEquals( ChecksumType.getTypeByValue( 2 ), checksum.getChecksumType() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "chksum" ), checksum.getChecksumValue() ) );

        // The realm
        assertNull( adKdcIssued.getIRealm() );

        // The sname
        assertNull( adKdcIssued.getISName() );

        // the elements
        AuthorizationData authData = adKdcIssued.getElements();

        assertNotNull( authData.getAuthorizationData().size() );
        assertEquals( 2, authData.getAuthorizationData().size() );

        String[] expected = new String[]
            { "abcdef", "ghijkl" };
        int i = 0;

        for ( AuthorizationDataEntry ad : authData.getAuthorizationData() )
        {
            assertEquals( AuthorizationType.AD_INTENDED_FOR_SERVER, ad.getAdType() );
            assertTrue( Arrays.equals( Strings.getBytesUtf8( expected[i++] ), ad.getAdData() ) );

        }

        ByteBuffer bb = ByteBuffer.allocate( adKdcIssued.computeLength() );

        // Check the encoding
        try
        {
            bb = adKdcIssued.encode( bb );

            // Check the length
            assertEquals( 0x3B, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of an empty AdKDCIssued message
     */
    @Test
    public void testDecodeTicketEmpty() throws Exception
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a AdKDCIssued Container
        Asn1Container adKdcIssuedContainer = new AdKdcIssuedContainer();
        adKdcIssuedContainer.setStream( stream );

        // Decode the AdKDCIssued PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, adKdcIssuedContainer);
        } );
    }
}
