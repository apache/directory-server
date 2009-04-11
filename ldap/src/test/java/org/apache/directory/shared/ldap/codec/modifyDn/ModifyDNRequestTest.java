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
package org.apache.directory.shared.ldap.codec.modifyDn;


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
import org.apache.directory.shared.ldap.codec.ResponseCarryingException;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestCodec;
import org.apache.directory.shared.ldap.message.InternalMessage;
import org.apache.directory.shared.ldap.message.ModifyDnResponseImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test the ModifyDNRequest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyDNRequestTest
{
    /**
     * Test the decoding of a full ModifyDNRequest
     */
    @Test
    public void testDecodeModifyDNRequestSuccess()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x48 );

        stream.put( new byte[]
            {
            0x30, 0x46,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x41,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                                        // ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
                                        // entry LDAPDN,
                0x04, 0x20, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                                        // newrdn RelativeLDAPDN,
                0x04, 0x0F, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y', 
                0x01, 0x01, 0x00, // deleteoldrdn BOOLEAN,
                                        // newSuperior [0] LDAPDN OPTIONAL }
                ( byte ) 0x80, 0x09, 
                  'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm' 
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyDNRequestCodec modifyDNRequest = message.getModifyDNRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "cn=testModify,ou=users,ou=system", modifyDNRequest.getEntry().toString() );
        assertEquals( false, modifyDNRequest.isDeleteOldRDN() );
        assertEquals( "cn=testDNModify", modifyDNRequest.getNewRDN().toString() );
        assertEquals( "ou=system", modifyDNRequest.getNewSuperior().toString() );

        // Check the length
        assertEquals( 0x48, message.computeLength() );

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
     * Test the decoding of a bad DN ModifyDNRequest
     */
    @Test
    public void testDecodeModifyDNRequestBadDN()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x48 );

        stream.put( new byte[]
            {
            0x30, 0x46,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x41,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                                        // ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
                                        // entry LDAPDN,
                0x04, 0x20, 
                  'c', 'n', ':', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                                        // newrdn RelativeLDAPDN,
                0x04, 0x0F, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y', 
                0x01, 0x01, 0x00, // deleteoldrdn BOOLEAN,
                                        // newSuperior [0] LDAPDN OPTIONAL }
                ( byte ) 0x80, 0x09, 
                  'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm' 
            } );

        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( de instanceof ResponseCarryingException );
            InternalMessage response = ((ResponseCarryingException)de).getResponse();
            assertTrue( response instanceof ModifyDnResponseImpl );
            assertEquals( ResultCodeEnum.INVALID_DN_SYNTAX, ((ModifyDnResponseImpl)response).getLdapResult().getResultCode() );
            return;
        }

        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a bad RDN ModifyDNRequest
     */
    @Test
    public void testDecodeModifyDNRequestBadRDN()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x48 );

        stream.put( new byte[]
            {
            0x30, 0x46,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x41,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                                        // ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
                                        // entry LDAPDN,
                0x04, 0x20, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                                        // newrdn RelativeLDAPDN,
                0x04, 0x0F, 
                  'c', 'n', ':', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y', 
                0x01, 0x01, 0x00,       // deleteoldrdn BOOLEAN,
                                        // newSuperior [0] LDAPDN OPTIONAL }
                ( byte ) 0x80, 0x09, 
                  'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm' 
            } );

        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( de instanceof ResponseCarryingException );
            InternalMessage response = ((ResponseCarryingException)de).getResponse();
            assertTrue( response instanceof ModifyDnResponseImpl );
            assertEquals( ResultCodeEnum.INVALID_DN_SYNTAX, ((ModifyDnResponseImpl)response).getLdapResult().getResultCode() );
            return;
        }

        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a bad RDN ModifyDNRequest
     */
    @Test
    public void testDecodeModifyDNRequestBadNewSuperior()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x48 );

        stream.put( new byte[]
            {
            0x30, 0x46,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x41,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                                        // ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
                                        // entry LDAPDN,
                0x04, 0x20, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                                        // newrdn RelativeLDAPDN,
                0x04, 0x0F, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y', 
                0x01, 0x01, 0x00,       // deleteoldrdn BOOLEAN,
                                        // newSuperior [0] LDAPDN OPTIONAL }
                ( byte ) 0x80, 0x09, 
                  'o', 'u', ':', 's', 'y', 's', 't', 'e', 'm' 
            } );

        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( de instanceof ResponseCarryingException );
            InternalMessage response = ((ResponseCarryingException)de).getResponse();
            assertTrue( response instanceof ModifyDnResponseImpl );
            assertEquals( ResultCodeEnum.INVALID_DN_SYNTAX, ((ModifyDnResponseImpl)response).getLdapResult().getResultCode() );
            return;
        }

        fail( "We should not reach this point" );
    }
    
    /**
     * Test the decoding of a full ModifyDNRequest with controls
     */
    @Test
    public void testDecodeModifyDNRequestSuccessWithControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x65 );

        stream.put( new byte[]
            {
            0x30, 0x63,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x41,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                                        // ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
                                        // entry LDAPDN,
                0x04, 0x20, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                                        // newrdn RelativeLDAPDN,
                0x04, 0x0F, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y', 
                0x01, 0x01, 0x00,       // deleteoldrdn BOOLEAN,
                                        // newSuperior [0] LDAPDN OPTIONAL }
                ( byte ) 0x80, 0x09, 
                  'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm', 
              ( byte ) 0xA0, 0x1B, // A control
                0x30, 0x19, 0x04, 0x17, 0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 0x2E, 0x31, 0x2E, 0x31, 0x31,
                0x33, 0x37, 0x33, 0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32 
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyDNRequestCodec modifyDNRequest = message.getModifyDNRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "cn=testModify,ou=users,ou=system", modifyDNRequest.getEntry().toString() );
        assertEquals( false, modifyDNRequest.isDeleteOldRDN() );
        assertEquals( "cn=testDNModify", modifyDNRequest.getNewRDN().toString() );
        assertEquals( "ou=system", modifyDNRequest.getNewSuperior().toString() );

        // Check the Control
        List<ControlCodec> controls = message.getControls();

        assertEquals( 1, controls.size() );

        ControlCodec control = message.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( ( byte[] ) control.getControlValue() ) );

        // Check the length
        assertEquals( 0x65, message.computeLength() );

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
     * Test the decoding of a ModifyDNRequest without a superior
     */
    @Test
    public void testDecodeModifyDNRequestWithoutSuperior()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x3D );

        stream.put( new byte[]
            {
            0x30, 0x3B,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x36,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                                        // ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
                                        // entry LDAPDN,
                0x04, 0x20, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                                        // newrdn RelativeLDAPDN,
                0x04, 0x0F, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y', 
                0x01, 0x01, 0x00        // deleteoldrdn BOOLEAN,
                                        // newSuperior [0] LDAPDN OPTIONAL }
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyDNRequestCodec modifyDNRequest = message.getModifyDNRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "cn=testModify,ou=users,ou=system", modifyDNRequest.getEntry().toString() );
        assertEquals( false, modifyDNRequest.isDeleteOldRDN() );
        assertEquals( "cn=testDNModify", modifyDNRequest.getNewRDN().toString() );

        // Check the length
        assertEquals( 0x3D, message.computeLength() );

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
     * Test the decoding of a ModifyDNRequest without a superior with controls
     */
    @Test
    public void testDecodeModifyDNRequestWithoutSuperiorWithControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x5A );

        stream.put( new byte[]
            {
            0x30, 0x58,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x36,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                                        // ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
                                        // entry LDAPDN,
                0x04, 0x20, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                                        // newrdn RelativeLDAPDN,
                0x04, 0x0F, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y', 
                0x01, 0x01, 0x00,       // deleteoldrdn BOOLEAN,
                                        // newSuperior [0] LDAPDN OPTIONAL }
              ( byte ) 0xA0, 0x1B,      // A control
                0x30, 0x19, 0x04, 0x17, 0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 0x2E, 0x31, 0x2E, 0x31, 0x31,
                0x33, 0x37, 0x33, 0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32 
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyDNRequestCodec modifyDNRequest = message.getModifyDNRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "cn=testModify,ou=users,ou=system", modifyDNRequest.getEntry().toString() );
        assertEquals( false, modifyDNRequest.isDeleteOldRDN() );
        assertEquals( "cn=testDNModify", modifyDNRequest.getNewRDN().toString() );

        // Check the Control
        List<ControlCodec> controls = message.getControls();

        assertEquals( 1, controls.size() );

        ControlCodec control = message.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( ( byte[] ) control.getControlValue() ) );

        // Check the length
        assertEquals( 0x5A, message.computeLength() );

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


    // Defensive tests

    /**
     * Test the decoding of a ModifyDNRequest with an empty body
     */
    @Test
    public void testDecodeModifyDNRequestEmptyBody()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            { 
            0x30, 0x05,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x00                // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyDNRequest PDU
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
     * Test the decoding of a ModifyDNRequest with an empty entry
     */
    @Test
    public void testDecodeModifyDNRequestEmptyEntry()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            { 
            0x30, 0x07,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x02,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                0x04, 0x00 } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyDNRequest PDU
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
     * Test the decoding of a ModifyDNRequest with an empty newRdn
     */
    @Test
    public void testDecodeModifyDNRequestEmptyNewRdn()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x2D );

        stream.put( new byte[]
            { 
            0x30, 0x2B,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x26,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                            // ...
                0x04, 0x20, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm', 
                0x04, 0x00 
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyDNRequest PDU
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
     * Test the decoding of a ModifyDNRequest with an empty deleteOldRdn
     */
    @Test
    public void testDecodeModifyDNRequestEmptyDeleteOldRdnn()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x3C );

        stream.put( new byte[]
            { 
            0x30, 0x3A,                 // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x6C, 0x35,               // CHOICE { ..., modifyDNRequest ModifyDNRequest,
                                        // ...
                0x04, 0x20, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                  's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm', 
                0x04, 0x0F, 
                  'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y', 
                0x01, 0x00              // deleteoldrdn BOOLEAN
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyDNRequest PDU
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
}
