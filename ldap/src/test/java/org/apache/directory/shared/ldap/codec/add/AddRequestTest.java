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
package org.apache.directory.shared.ldap.codec.add;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.Control;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.add.AddRequest;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;


/**
 * Test the AddRequest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddRequestTest extends TestCase
{
    /**
     * Test the decoding of a AddRequest
     */
    public void testDecodeAddRequestSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x59 );

        stream.put( new byte[]
            { 0x30, 0x57, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x52, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                // attributes AttributeList }
                0x30, 0x2E, // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x0c, // attribute 1
                0x04, 0x01, 'l', // type AttributeDescription,
                0x31, 0x07, // vals SET OF AttributeValue }
                0x04, 0x05, 'P', 'a', 'r', 'i', 's',

                0x30, 0x1E, // attribute 2
                // type AttributeDescription,
                0x04, 0x05, 'a', 't', 't', 'r', 's', 0x31, 0x15, // vals SET
                                                                    // OF
                                                                    // AttributeValue
                                                                    // }
                0x04, 0x05, 't', 'e', 's', 't', '1', 0x04, 0x05, 't', 'e', 's', 't', '2', 0x04, 0x05, 't', 'e', 's',
                't', '3', } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        AddRequest addRequest = message.getAddRequest();

        // Check the decoded message
        assertEquals( 1, message.getMessageId() );
        assertEquals( "cn=testModify,ou=users,ou=system", addRequest.getEntry().toString() );

        Attributes attributes = addRequest.getAttributes();

        assertEquals( 2, attributes.size() );

        HashSet expectedTypes = new HashSet();

        expectedTypes.add( "l" );
        expectedTypes.add( "attrs" );

        HashMap typesVals = new HashMap();

        HashSet lVal1 = new HashSet();
        lVal1.add( "Paris" );
        typesVals.put( "l", lVal1 );

        HashSet lVal2 = new HashSet();
        lVal2.add( "test1" );
        lVal2.add( "test2" );
        lVal2.add( "test3" );
        typesVals.put( "attrs", lVal2 );

        BasicAttribute attributeValue = ( BasicAttribute ) attributes.get( "l" );

        assertTrue( expectedTypes.contains( attributeValue.getID().toLowerCase() ) );

        NamingEnumeration values = attributeValue.getAll();
        HashSet vals = ( HashSet ) typesVals.get( attributeValue.getID().toLowerCase() );

        while ( values.hasMore() )
        {
            Object value = values.next();

            assertTrue( vals.contains( value.toString() ) );

            vals.remove( value.toString() );
        }

        attributeValue = ( BasicAttribute ) attributes.get( "attrs" );

        assertTrue( expectedTypes.contains( attributeValue.getID().toLowerCase() ) );

        values = attributeValue.getAll();
        vals = ( HashSet ) typesVals.get( attributeValue.getID().toLowerCase() );

        while ( values.hasMore() )
        {
            Object value = values.next();

            assertTrue( vals.contains( value.toString() ) );

            vals.remove( value.toString() );
        }

        // Check the length
        assertEquals( 0x59, message.computeLength() );

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
     * Test the decoding of a AddRequest with a null body
     */
    public void testDecodeAddRequestNullBody() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            { 0x30, 0x05, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x00 // CHOICE { ..., addRequest AddRequest, ...
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
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


    /**
     * Test the decoding of a AddRequest with a null entry
     */
    public void testDecodeAddRequestNullEntry() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x39 );

        stream.put( new byte[]
            { 0x30, 0x37, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x26, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x00,
                // attributes AttributeList }
                0x30, 0x2E, // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x0c, // attribute 1
                0x04, 0x01, 'l', // type AttributeDescription,
                0x31, 0x07, // vals SET OF AttributeValue }
                0x04, 0x05, 'P', 'a', 'r', 'i', 's',

                0x30, 0x1E, // attribute 2
                // type AttributeDescription,
                0x04, 0x05, 'a', 't', 't', 'r', 's', 0x31, 0x15, // vals SET
                                                                    // OF
                                                                    // AttributeValue
                                                                    // }
                0x04, 0x05, 't', 'e', 's', 't', '1', 0x04, 0x05, 't', 'e', 's', 't', '2', 0x04, 0x05, 't', 'e', 's',
                't', '3', } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
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


    /**
     * Test the decoding of a AddRequest with a null attributeList
     */
    public void testDecodeAddRequestNullAttributes() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x2B );

        stream.put( new byte[]
            { 0x30, 0x29, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x24, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                // attributes AttributeList }
                0x30, 0x00, // AttributeList ::= SEQUENCE OF SEQUENCE {
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
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


    /**
     * Test the decoding of a AddRequest with a empty attributeList
     */
    public void testDecodeAddRequestNullAttributeList() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x2D );

        stream.put( new byte[]
            { 0x30, 0x2B, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x26, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                // attributes AttributeList }
                0x30, 0x02, // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x00, // AttributeList ::= SEQUENCE OF SEQUENCE {
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
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


    /**
     * Test the decoding of a AddRequest with a empty attributeList
     */
    public void testDecodeAddRequestNullType() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x2F );

        stream.put( new byte[]
            { 0x30, 0x2D, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x28, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                // attributes AttributeList }
                0x30, 0x04, // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x02, // attribute 1
                0x04, 0x00, // type AttributeDescription,
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
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


    /**
     * Test the decoding of a AddRequest with a empty attributeList
     */
    public void testDecodeAddRequestNoVals() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x30 );

        stream.put( new byte[]
            { 0x30, 0x2E, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x29, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                // attributes AttributeList }
                0x30, 0x05, // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x03, // attribute 1
                0x04, 0x01, 'A', // type AttributeDescription,
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
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


    /**
     * Test the decoding of a AddRequest with a empty attributeList
     */
    public void testDecodeAddRequestNullVals() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x32 );

        stream.put( new byte[]
            { 0x30, 0x30, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x2B, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                // attributes AttributeList }
                0x30, 0x07, // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x05, // attribute 1
                0x04, 0x01, 'A', // type AttributeDescription,
                0x31, 0x00 } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
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


    /**
     * Test the decoding of a AddRequest with a empty attributeList
     */
    public void testDecodeAddRequestEmptyAttributeValue() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x34 );

        stream.put( new byte[]
            { 0x30, 0x32, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x2D, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                // attributes AttributeList }
                0x30, 0x09, // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x07, // attribute 1
                0x04, 0x01, 'l', // type AttributeDescription,
                0x31, 0x02, 0x04, 0x00 } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        AddRequest addRequest = message.getAddRequest();

        // Check the decoded message
        assertEquals( 1, message.getMessageId() );
        assertEquals( "cn=testModify,ou=users,ou=system", addRequest.getEntry().toString() );

        Attributes attributes = addRequest.getAttributes();

        assertEquals( 1, attributes.size() );

        BasicAttribute attributeValue = ( BasicAttribute ) attributes.get( "l" );

        assertEquals( "l", attributeValue.getID().toLowerCase() );

        NamingEnumeration values = attributeValue.getAll();

        while ( values.hasMore() )
        {
            Object value = values.next();

            assertEquals( "", value.toString() );
        }

        // Check the length
        assertEquals( 0x34, message.computeLength() );

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
     * Test the decoding of a AddRequest with a empty attributeList and a
     * control
     */
    public void testDecodeAddRequestEmptyAttributeValueWithControl() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x51 );

        stream.put( new byte[]
            { 0x30, 0x4F, // LDAPMessage ::= SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x68, 0x2D, // CHOICE { ..., addRequest AddRequest, ...
                // AddRequest ::= [APPLICATION 8] SEQUENCE {
                // entry LDAPDN,
                0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u',
                's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                // attributes AttributeList }
                0x30, 0x09, // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x07, // attribute 1
                0x04, 0x01, 'l', // type AttributeDescription,
                0x31, 0x02, 0x04, 0x00, ( byte ) 0xA0, 0x1B, // A control
                0x30, 0x19, 0x04, 0x17, 0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 0x2E, 0x31, 0x2E, 0x31, 0x31,
                0x33, 0x37, 0x33, 0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32 } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        AddRequest addRequest = message.getAddRequest();

        // Check the decoded message
        assertEquals( 1, message.getMessageId() );
        assertEquals( "cn=testModify,ou=users,ou=system", addRequest.getEntry().toString() );

        Attributes attributes = addRequest.getAttributes();

        assertEquals( 1, attributes.size() );

        BasicAttribute attributeValue = ( BasicAttribute ) attributes.get( "l" );

        assertEquals( "l", attributeValue.getID().toLowerCase() );

        NamingEnumeration values = attributeValue.getAll();

        while ( values.hasMore() )
        {
            Object value = values.next();

            assertEquals( "", value.toString() );
        }

        // Check the length
        assertEquals( 0x51, message.computeLength() );

        // Check the Control
        List controls = message.getControls();

        assertEquals( 1, controls.size() );

        Control control = message.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( ( byte[] ) control.getControlValue() ) );

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
