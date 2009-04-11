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
package org.apache.directory.shared.ldap.codec.unbind;


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
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UnBindRequestTest
{
    /**
     * Test the decoding of a UnBindRequest with no controls
     */
    @Test
    public void testDecodeUnBindRequestNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );
        stream.put( new byte[]
            { 
            0x30, 0x05,                 // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x42, 0x00,               // CHOICE { ..., unbindRequest UnbindRequest,...
                                        // UnbindRequest ::= [APPLICATION 2] NULL
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a BindRequest Container
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

        LdapMessageCodec ldapMessage = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();

        assertEquals( 1, ldapMessage.getMessageId() );

        // Check the length
        assertEquals( 7, ldapMessage.computeLength() );

        try
        {
            ByteBuffer bb = ldapMessage.encode( null );

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
     * Test the decoding of a UnBindRequest with controls
     */
    @Test
    public void testDecodeUnBindRequestWithControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x24 );
        stream.put( new byte[]
            { 
            0x30, 0x22,                 // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x42, 0x00,               // CHOICE { ..., unbindRequest UnbindRequest,...
                                        // UnbindRequest ::= [APPLICATION 2] NULL
              ( byte ) 0xA0, 0x1B,      // A control
                0x30, 0x19, 
                  0x04, 0x17,
                    '2', '.', '1', '6', '.', '8', '4', '0', '.', '1', '.', '1', '1', '3', '7', 
                    '3', '0', '.', '3', '.', '4', '.', '2'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a BindRequest Container
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

        LdapMessageCodec ldapMessage = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();

        assertEquals( 1, ldapMessage.getMessageId() );

        // Check the Control
        List<ControlCodec> controls = ldapMessage.getControls();

        assertEquals( 1, controls.size() );

        ControlCodec control = ldapMessage.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( ( byte[] ) control.getControlValue() ) );

        // Check the length
        assertEquals( 0x24, ldapMessage.computeLength() );

        try
        {
            ByteBuffer bb = ldapMessage.encode( null );

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
     * Test the decoding of a UnBindRequest with a not null body
     */
    @Test
    public void testDecodeUnBindRequestNotNull()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );
        stream.put( new byte[]
            { 
            0x30, 0x07,                 // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,         // messageID MessageID
              0x42, 0x02,               // CHOICE { ..., unbindRequest UnbindRequest,...
                0x04, 0x00              // UnbindRequest ::= [APPLICATION 2] NULL

            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a UnbindRequest message
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }
}
