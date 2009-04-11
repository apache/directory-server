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
package org.apache.directory.shared.ldap.codec.extended;


import java.nio.ByteBuffer;
import java.util.List;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequestCodec;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;


/**
 * Test the ExtendedRequest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtendedRequestTest
{
    /**
     * Test the decoding of a full ExtendedRequest
     */
    @Test
    public void testDecodeExtendedRequestSuccess()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x1D );

        stream.put( new byte[]
            { 
              0x30, 0x1B,               // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01,       // messageID MessageID
                                        // CHOICE { ..., extendedReq ExtendedRequest, ...
                0x77, 0x16,             // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
                                        // requestName [0] LDAPOID,
                  ( byte ) 0x80, 0x0D, '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '2',
                                        // requestValue [1] OCTET STRING OPTIONAL }
                  ( byte ) 0x81, 0x05, 'v', 'a', 'l', 'u', 'e' 
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        // Check the decoded ExtendedRequest PDU
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedRequestCodec extendedRequest = message.getExtendedRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "1.3.6.1.5.5.2", extendedRequest.getRequestName() );
        assertEquals( "value", StringTools.utf8ToString( extendedRequest.getRequestValue() ) );

        // Check the length
        assertEquals( 0x1D, message.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );

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
     * Test the decoding of a full ExtendedRequest with controls
     */
    @Test
    public void testDecodeExtendedRequestWithControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x3A );

        stream.put( new byte[]
            { 
            0x30, 0x38,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
                                        // CHOICE { ..., extendedReq ExtendedRequest, ...
              0x77, 0x16,               // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
                                        // requestName [0] LDAPOID,
                ( byte ) 0x80, 0x0D, '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '2',
                                        // requestValue [1] OCTET STRING OPTIONAL }
                ( byte ) 0x81, 0x05, 'v', 'a', 'l', 'u', 'e', 
              ( byte ) 0xA0, 0x1B,      // A control
                0x30, 0x19, 
                  0x04, 0x17,
                    '2', '.', '1', '6', '.', '8', '4', '0', '.', '1', '.', '1', '1', '3', '7', '3', 
                    '0', '.', '3', '.', '4', '.', '2'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        // Check the decoded ExtendedRequest PDU
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedRequestCodec extendedRequest = message.getExtendedRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "1.3.6.1.5.5.2", extendedRequest.getRequestName() );
        assertEquals( "value", StringTools.utf8ToString( extendedRequest.getRequestValue() ) );

        // Check the Control
        List<ControlCodec> controls = message.getControls();

        assertEquals( 1, controls.size() );

        ControlCodec control = message.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( ( byte[] ) control.getControlValue() ) );

        // Check the length
        assertEquals( 0x3A, message.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );

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
     * Test the decoding of a full ExtendedRequest with no value and with
     * controls
     */
    @Test
    public void testDecodeExtendedRequestNoValueWithControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x33 );

        stream.put( new byte[]
            { 
            0x30, 0x31,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
                                        // CHOICE { ..., extendedReq ExtendedRequest, ...
              0x77, 0x0F,               // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
                                        // requestName [0] LDAPOID,
                ( byte ) 0x80, 0x0D, 
                  '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '2',
                                        // requestValue [1] OCTET STRING OPTIONAL }
                ( byte ) 0xA0, 0x1B,    // A control
                  0x30, 0x19, 
                    0x04, 0x17, 
                      '2', '.', '1', '6', '.', '8', '4', '0', '.', '1', '.', '1', '1', '3', '7', '3', 
                      '0', '.', '3', '.', '4', '.', '2'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        // Check the decoded ExtendedRequest PDU
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedRequestCodec extendedRequest = message.getExtendedRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "1.3.6.1.5.5.2", extendedRequest.getRequestName() );
        assertEquals( "", StringTools.utf8ToString( extendedRequest.getRequestValue() ) );

        // Check the Control
        List<ControlCodec> controls = message.getControls();

        assertEquals( 1, controls.size() );

        ControlCodec control = message.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( ( byte[] ) control.getControlValue() ) );

        // Check the length
        assertEquals( 0x33, message.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );

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
     * Test the decoding of an empty ExtendedRequest
     */
    @Test
    public void testDecodeExtendedRequestEmpty()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            { 
            0x30, 0x05,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
                                        // CHOICE { ..., extendedReq ExtendedRequest, ...
              0x77, 0x00,               // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
            fail( "We should never reach this point !!!" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the decoding of an empty OID
     */
    @Test
    public void testDecodeEmptyOID()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            { 
            0x30, 0x07,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
                                        // CHOICE { ..., extendedReq ExtendedRequest, ...
              0x77, 0x02,               // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
                ( byte ) 0x80, 0x00 
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
            fail( "We should never reach this point !!!" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }

    /**
     * Test the decoding of a bad name 
     */
    @Test
    public void testDecodeExtendedBadRequestName()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x16 );

        stream.put( new byte[]
            { 
            0x30, 0x14,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
                                        // CHOICE { ..., extendedReq ExtendedRequest, ...
              0x77, 0x0F,               // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
                                        // requestName [0] LDAPOID,
                ( byte ) 0x80, 0x0D, 
                  '1', '-', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '2', 
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
            fail( "We should never reach this point !!!" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }    

    /**
     * Test the decoding of a name only ExtendedRequest
     */
    @Test
    public void testDecodeExtendedRequestName()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x16 );

        stream.put( new byte[]
            { 
            0x30, 0x14,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
                                        // CHOICE { ..., extendedReq ExtendedRequest, ...
              0x77, 0x0F,               // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
                                        // requestName [0] LDAPOID,
                ( byte ) 0x80, 0x0D, 
                  '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '2', 
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        // Check the decoded ExtendedRequest PDU
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedRequestCodec extendedRequest = message.getExtendedRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "1.3.6.1.5.5.2", extendedRequest.getRequestName() );

        // Check the length
        assertEquals( 0x16, message.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );

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
     * Test the decoding of an empty name ExtendedRequest
     */
    @Test
    public void testDecodeExtendedRequestEmptyName()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x18 );

        stream.put( new byte[]
            { 
            0x30, 0x16,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
                                        // CHOICE { ..., extendedReq ExtendedRequest, ...
              0x77, 0x11,               // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
                                        // requestName [0] LDAPOID,
                ( byte ) 0x80, 0x0D, 
                  '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '2', 
                ( byte ) 0x81, 0x00 
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        // Check the decoded ExtendedRequest PDU
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedRequestCodec extendedRequest = message.getExtendedRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "1.3.6.1.5.5.2", extendedRequest.getRequestName() );
        assertEquals( "", StringTools.utf8ToString( extendedRequest.getRequestValue() ) );

        // Check the length
        assertEquals( 0x18, message.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }
}
