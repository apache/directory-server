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
package org.apache.directory.shared.ldap.codec;


import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.bind.BindRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * A global Ldap Decoder test
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapDecoderTest
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Test the decoding of a full PDU
     */
    @Test
    public void testDecodeFull()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x35 );
        stream.put( new byte[]
            { 0x30, 0x33, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x60, 0x2E, // CHOICE { ..., bindRequest BindRequest, ...
                // BindRequest ::= APPLICATION[0] SEQUENCE {
                0x02, 0x01, 0x03, // version INTEGER (1..127),
                0x04, 0x1F, // name LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', ( byte ) 0x80, 0x08, // authentication
                                                                                            // AuthenticationChoice
                // AuthenticationChoice ::= CHOICE { simple [0] OCTET STRING,
                // ...
                'p', 'a', 's', 's', 'w', 'o', 'r', 'd' } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        // Check the decoded PDU
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        BindRequestCodec br = message.getBindRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( 3, br.getVersion() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", br.getName().toString() );
        assertEquals( true, ( br.getAuthentication() instanceof SimpleAuthentication ) );
        assertEquals( "password", StringTools.utf8ToString( ( ( SimpleAuthentication ) br.getAuthentication() )
            .getSimple() ) );
    }


    /**
     * Test the decoding of a partial PDU
     */
    @Test
    public void testDecodePartial()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 16 );
        stream.put( new byte[]
            { 0x30, 0x33, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x60, 0x2E, // CHOICE { ..., bindRequest BindRequest, ...
                // BindRequest ::= APPLICATION[0] SEQUENCE {
                0x02, 0x01, 0x03, // version INTEGER (1..127),
                0x04, 0x1F, // name LDAPDN,
                'u', 'i', 'd', '=' } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.VALUE_STATE_PENDING, ldapMessageContainer.getState() );

        // Check the decoded PDU
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        BindRequestCodec br = message.getBindRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( 3, br.getVersion() );
        assertEquals( null, br.getName() );
        assertEquals( false, ( br.getAuthentication() instanceof SimpleAuthentication ) );
    }


    /**
     * Test the decoding of a splitted PDU
     */
    @Test
    public void testDecodeSplittedPDU()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 16 );
        stream.put( new byte[]
            { 0x30, 0x33, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x60, 0x2E, // CHOICE { ..., bindRequest BindRequest, ...
                // BindRequest ::= APPLICATION[0] SEQUENCE {
                0x02, 0x01, 0x03, // version INTEGER (1..127),
                0x04, 0x1F, // name LDAPDN,
                'u', 'i', 'd', '=' } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU first block of data
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.VALUE_STATE_PENDING, ldapMessageContainer.getState() );

        // Second block of data
        stream = ByteBuffer.allocate( 37 );
        stream.put( new byte[]
            { 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a', 'm', 'p', 'l', 'e', ',',
                'd', 'c', '=', 'c', 'o', 'm', ( byte ) 0x80, 0x08, // authentication
                                                                    // AuthenticationChoice
                // AuthenticationChoice ::= CHOICE { simple [0] OCTET STRING,
                // ...
                'p', 'a', 's', 's', 'w', 'o', 'r', 'd' } );

        stream.flip();

        // Decode a BindRequest PDU second block of data
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        assertEquals( ldapMessageContainer.getState(), TLVStateEnum.PDU_DECODED );

        // Check the decoded PDU
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        BindRequestCodec br = message.getBindRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( 3, br.getVersion() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", br.getName().toString() );
        assertEquals( true, ( br.getAuthentication() instanceof SimpleAuthentication ) );
        assertEquals( "password", StringTools.utf8ToString( ( ( SimpleAuthentication ) br.getAuthentication() )
            .getSimple() ) );
    }


    /**
     * Test the decoding of a PDU with a bad Length. The first TLV has a length
     * of 0x32 when the PDU is 0x33 bytes long.
     */
    @Test
    public void testDecodeBadLengthTooSmall()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x35 );
        stream.put( new byte[]
            {
            // Length should be 0x33...
                0x30, 0x32, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x60, 0x2E, // CHOICE { ..., bindRequest BindRequest, ...
                // BindRequest ::= APPLICATION[0] SEQUENCE {
                0x02, 0x01, 0x03, // version INTEGER (1..127),
                0x04, 0x1F, // name LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', ( byte ) 0x80, 0x08, // authentication
                                                                                            // AuthenticationChoice
                // AuthenticationChoice ::= CHOICE { simple [0] OCTET STRING,
                // ...
                'p', 'a', 's', 's', 'w', 'o', 'r', 'd' } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertEquals( "The current Value length is above the expected length", de.getMessage() );
            return;
        }

        fail( "Should never reach this point.." );
    }


    /**
     * Test the decoding of a PDU with a bad primitive Length. The second TLV
     * has a length of 0x02 when the PDU is 0x01 bytes long.
     */
    @Test
    public void testDecodeBadPrimitiveLengthTooBig()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x35 );
        stream.put( new byte[]
            { 
            0x30, 0x33,                 // LDAPMessage ::=SEQUENCE {
                                        // Length should be 0x01...
              0x02, 0x02, 0x01,         // messageID MessageID
              0x60, 0x2E,               // CHOICE { ..., bindRequest BindRequest, ...
                                        // BindRequest ::= APPLICATION[0] SEQUENCE {
                0x02, 0x01, 0x03,       // version INTEGER (1..127),
                0x04, 0x1F,             // name LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                ( byte ) 0x80, 0x08,    // authentication AuthenticationChoice
                                        // AuthenticationChoice ::= CHOICE { simple [0] OCTET STRING,
                                        // ...
                  'p', 'a', 's', 's', 'w', 'o', 'r' } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertEquals( "Bad transition !", de.getMessage() );
            return;
        }

        fail( "Should never reach this point." );
    }


    /**
     * Test the decoding of a PDU with a bad primitive Length. The second TLV
     * has a length of 0x02 when the PDU is 0x01 bytes long.
     */
    @Test
    public void testDecodeBadTagTransition()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x35 );
        stream.put( new byte[]
            { 0x30, 0x33, // LDAPMessage ::=SEQUENCE {
                // Length should be 0x01...
                0x02, 0x01, 0x01, // messageID MessageID
                0x2D, 0x2D, // CHOICE { ..., bindRequest BindRequest, ...
                // BindRequest ::= APPLICATION[0] SEQUENCE {
                0x02, 0x01, 0x03, // version INTEGER (1..127),
                0x04, 0x1F, // name LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', ( byte ) 0x80, 0x08, // authentication
                                                                                            // AuthenticationChoice
                // AuthenticationChoice ::= CHOICE { simple [0] OCTET STRING,
                // ...
                'p', 'a', 's', 's', 'w', 'o', 'r', 'd' } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertEquals( "Bad transition !", de.getMessage() );
            return;
        }

        fail( "Should never reach this point." );
    }

    /**
     * Test the decoding of a splitted Length.
     * 
     * The length is 3 bytes long, but the PDU has been splitted 
     * just after the first byte 
     */
    @Test
    public void testDecodeSplittedLength()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 3 );
        stream.put( new byte[]
            { 
            0x30, (byte)0x82, 0x01,// LDAPMessage ::=SEQUENCE {
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU first block of data
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.LENGTH_STATE_PENDING, ldapMessageContainer.getState() );

        // Second block of data
        stream = ByteBuffer.allocate( 1 );
        stream.put( new byte[]
            { 
                (byte)0x80 // End of the length
            } );

        stream.flip();

        // Decode a BindRequest PDU second block of data
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.TAG_STATE_START, ldapMessageContainer.getState() );

        // Check the decoded length
        assertEquals( 384, ldapMessageContainer.getCurrentTLV().getLength() );
    }
}
