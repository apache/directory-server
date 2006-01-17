/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.common.codec.extended;

import java.nio.ByteBuffer;

import javax.naming.NamingException;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.extended.ExtendedRequest;
import org.apache.ldap.common.util.StringTools;

import junit.framework.TestCase;

/**
 * Test the ExtendedRequest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtendedRequestTest extends TestCase {
    /**
     * Test the decoding of a full ExtendedRequest
     */
    public void testDecodeExtendedRequestSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x16 );
        
        stream.put(
            new byte[]
            {
                0x30, 0x14, 		// LDAPMessage ::= SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				            		//     CHOICE { ..., extendedReq     ExtendedRequest, ...
				0x77, 0x0F,         // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
				                    //     requestName      [0] LDAPOID,
				(byte)0x80, 0x06, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02,
				                    //     requestValue     [1] OCTET STRING OPTIONAL }
				(byte)0x81, 0x05, 'v', 'a', 'l', 'u', 'e'
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
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedRequest extendedRequest      = message.getExtendedRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "1.3.6.1.5.5.2", extendedRequest.getRequestName() );
        assertEquals( "value", StringTools.utf8ToString( extendedRequest.getRequestValue() ) );
        
        // Check the length
        assertEquals(0x16, message.computeLength());

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu, decodedPdu );
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
    public void testDecodeExtendedRequestEmpty() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x07 );
        
        stream.put(
            new byte[]
            {
                    0x30, 0x05, 		// LDAPMessage ::= SEQUENCE {
    				  0x02, 0x01, 0x01, 	//     messageID MessageID
    				            		//     CHOICE { ..., extendedReq     ExtendedRequest, ...
    				  0x77, 0x00,         // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
            fail("We should never reach this point !!!");
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }

    /**
     * Test the decoding of an empty OID
     */
    public void testDecodeEmptyOID() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x09 );
        
        stream.put(
            new byte[]
            {
                    0x30, 0x07, 		// LDAPMessage ::= SEQUENCE {
    				  0x02, 0x01, 0x01, 	//     messageID MessageID
    				            		//     CHOICE { ..., extendedReq     ExtendedRequest, ...
    				  0x77, 0x02,         // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
    				    (byte)0x80, 0x00
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ExtendedRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
            fail("We should never reach this point !!!");
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }

    /**
     * Test the decoding of a name only ExtendedRequest
     */
    public void testDecodeExtendedRequestName() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0F );
        
        stream.put(
            new byte[]
            {
                0x30, 0x0D, 		// LDAPMessage ::= SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				            		//     CHOICE { ..., extendedReq     ExtendedRequest, ...
				0x77, 0x08,         // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
				                    //     requestName      [0] LDAPOID,
				(byte)0x80, 0x06, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02,
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
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedRequest extendedRequest      = message.getExtendedRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "1.3.6.1.5.5.2", extendedRequest.getRequestName() );
        
        // Check the length
        assertEquals(0x0F, message.computeLength());

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu, decodedPdu );
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
    public void testDecodeExtendedRequestEmptyName() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x11 );
        
        stream.put(
            new byte[]
            {
                0x30, 0x0F, 		// LDAPMessage ::= SEQUENCE {
				  0x02, 0x01, 0x01, 	//     messageID MessageID
				            		//     CHOICE { ..., extendedReq     ExtendedRequest, ...
				  0x77, 0x0A,         // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
				                    //     requestName      [0] LDAPOID,
				    (byte)0x80, 0x06, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02,
				    (byte)0x81, 0x00
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
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedRequest extendedRequest      = message.getExtendedRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "1.3.6.1.5.5.2", extendedRequest.getRequestName() );
        assertEquals( "", StringTools.utf8ToString( extendedRequest.getRequestValue() ) );
        
        // Check the length
        assertEquals(0x11, message.computeLength());

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }
}
