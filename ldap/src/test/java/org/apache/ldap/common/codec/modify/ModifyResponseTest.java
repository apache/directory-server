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
package org.apache.ldap.common.codec.modify;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.Control;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.modify.ModifyResponse;
import org.apache.ldap.common.util.StringTools;

import junit.framework.TestCase;

/**
 * Test the ModifyResponse codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyResponseTest extends TestCase {
    /**
     * Test the decoding of a ModifyResponse
     */
    public void testDecodeModifyResponseSuccess()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0E );
        
        stream.put(
            new byte[]
            {
                0x30, 0x0C, 		// LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	//         messageID MessageID
				0x67, 0x07, 		//        CHOICE { ..., modifyResponse ModifyResponse, ...
                        			// ModifyResponse ::= [APPLICATION 7] LDAPResult
				0x0A, 0x01, 0x00, 	//   LDAPResult ::= SEQUENCE {
									//		resultCode ENUMERATED {
									//			success (0), ...
				 					//      },
				0x04, 0x00,			//		matchedDN    LDAPDN,
				0x04, 0x00  		//      errorMessage LDAPString,
									//		referral     [3] Referral OPTIONAL }
									// }
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyResponse PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
    	
        // Check the decoded ModifyResponse PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyResponse modifyResponse      = message.getModifyResponse();

        assertEquals( 1, message.getMessageId() );
        assertEquals( 0, modifyResponse.getLdapResult().getResultCode() );
        assertEquals( "", modifyResponse.getLdapResult().getMatchedDN() );
        assertEquals( "", modifyResponse.getLdapResult().getErrorMessage() );
        
        // Check the length
        assertEquals(0x0E, message.computeLength());

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
     * Test the decoding of a ModifyResponse with controls
     */
    public void testDecodeModifyResponseSuccessWithControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x2B );
        
        stream.put(
            new byte[]
            {
                0x30, 0x29,           // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,   //         messageID MessageID
                  0x67, 0x07,         //        CHOICE { ..., modifyResponse ModifyResponse, ...
                                      // ModifyResponse ::= [APPLICATION 7] LDAPResult
                    0x0A, 0x01, 0x00, //   LDAPResult ::= SEQUENCE {
                                      //      resultCode ENUMERATED {
                                      //          success (0), ...
                                      //      },
                    0x04, 0x00,       //      matchedDN    LDAPDN,
                    0x04, 0x00,       //      errorMessage LDAPString,
                                      //      referral     [3] Referral OPTIONAL }
                                      // }
                  (byte)0xA0, 0x1B,   // A control 
                    0x30, 0x19, 
                      0x04, 0x17, 
                        0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 
                        0x2E, 0x31, 0x2E, 0x31, 0x31, 0x33, 0x37, 0x33, 
                        0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyResponse PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        
        // Check the decoded ModifyResponse PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyResponse modifyResponse      = message.getModifyResponse();

        assertEquals( 1, message.getMessageId() );
        assertEquals( 0, modifyResponse.getLdapResult().getResultCode() );
        assertEquals( "", modifyResponse.getLdapResult().getMatchedDN() );
        assertEquals( "", modifyResponse.getLdapResult().getErrorMessage() );
        
        // Check the Control
        List controls = message.getControls();
        
        assertEquals( 1, controls.size() );
        
        Control control = message.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( (byte[])control.getControlValue() ) );

        // Check the length
        assertEquals(0x2B, message.computeLength());

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
     * Test the decoding of a ModifyResponse with no LdapResult
     */
    public void testDecodeModifyResponseEmptyResult()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x07 );
        
        stream.put(
            new byte[]
            {
                    0x30, 0x05,           // LDAPMessage ::=SEQUENCE {
                      0x02, 0x01, 0x01,   //         messageID MessageID
                      0x67, 0x00,         //        CHOICE { ..., modifyResponse ModifyResponse, ...
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyResponse message
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
