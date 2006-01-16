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
package org.apache.ldap.common.codec;

import junit.framework.TestCase;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.add.AddResponse;
import org.apache.ldap.common.util.StringTools;

import java.nio.ByteBuffer;


/**
 * A test for LdapResults. We will use a AddResponse message to
 * test the LdapResult part
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapResultTest extends TestCase
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Test the decoding of a AddResponse with no LdapResult
     */
    public void testDecodeAddResponseEmptyResultCode()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x10 );
        
        stream.put(
            new byte[]
            {
                0x30, 0x0E, 		// LDAPMessage ::=SEQUENCE {
				  0x02, 0x01, 0x01, //         messageID MessageID
				  0x69, 0x02, 		//        CHOICE { ..., addResponse AddResponse, ...
				    0x0A, 0x00		// Empty resultCode
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddResponse message
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
        	System.out.println( de.getMessage() );
            assertTrue( true );
            return;
        }
    	
        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a AddResponse with no LdapResult
     */
    public void testDecodeAddResponseEmptyResultCodeAbove90()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x10 );
        
        stream.put(
            new byte[]
            {
                0x30, 0x0E, 		// LDAPMessage ::=SEQUENCE {
				  0x02, 0x01, 0x01, //         messageID MessageID
				  0x69, 0x02, 		//        CHOICE { ..., addResponse AddResponse, ...
				    0x0A, 0x03, 0x01, 0x01 // resultCode too high
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddResponse message
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
        	System.out.println( de.getMessage() );
            assertTrue( true );
            return;
        }
    	
        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a AddResponse with all the different 
     * result codes
     */
    public void testDecodeAddResponseEmptyResultCodesOK()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0E );
        
        byte[] buffer = new byte[]
        {
            0x30, 0x0C, 		// LDAPMessage ::=SEQUENCE {
			  0x02, 0x01, 0x01, //         messageID MessageID
			  0x69, 0x07, 		//        CHOICE { ..., addResponse AddResponse, ...
			    0x0A, 0x01, 0x00, // resultCode success
                0x04, 0x00,			//		matchedDN    LDAPDN,
                0x04, 0x00  		//      errorMessage LDAPString,
        };

        for ( int i = 0; i < 91; i++ )
        {
        	buffer[9] = (byte)i;
	        stream.put( buffer );
	        stream.flip();

	        // Allocate a LdapMessage Container
	        IAsn1Container ldapMessageContainer = new LdapMessageContainer();
	
	        // Decode a AddResponse PDU
	        try
	        {
	            ldapDecoder.decode( stream, ldapMessageContainer );
	        }
	        catch ( DecoderException de )
	        {
        		fail( "We should never reach this point" );
	        }
	        
	        stream.clear();
        }

        assertTrue( true );
    }

    /**
     * Test the decoding of a AddResponse with no matched DN 
     */
    public void testDecodeAddResponseEmptyResultCodeNoMatchedDN()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0A );
        
        stream.put(
	        new byte[]
	        {
	            0x30, 0x08, 		// LDAPMessage ::=SEQUENCE {
				  0x02, 0x01, 0x01, //         messageID MessageID
				  0x69, 0x03, 		//        CHOICE { ..., addResponse AddResponse, ...
				    0x0A, 0x01, 0x00, // resultCode success
	        });

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddResponse message
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
        	System.out.println( de.getMessage() );
            assertTrue( true );
            return;
        }
    	
        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a AddResponse with no error message 
     */
    public void testDecodeAddResponseEmptyResultCodeNoErrorMsg()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0C );
        
        stream.put(
	        new byte[]
	        {
	            0x30, 0x0A, 		// LDAPMessage ::=SEQUENCE {
				  0x02, 0x01, 0x01, //         messageID MessageID
				  0x69, 0x05, 		//        CHOICE { ..., addResponse AddResponse, ...
				    0x0A, 0x01, 0x00, // resultCode success
				    0x04, 0x00		// Empty matched DN	
	        });

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddResponse message
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
        	System.out.println( de.getMessage() );
            assertTrue( true );
            return;
        }
    	
        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a AddResponse with a valid LdapResult 
     */
    public void testDecodeAddResponseEmptyResultCodeOK()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0E );
        
        stream.put(
	        new byte[]
	        {
	            0x30, 0x0C, 		// LDAPMessage ::=SEQUENCE {
				  0x02, 0x01, 0x01, //         messageID MessageID
				  0x69, 0x07, 		//        CHOICE { ..., addResponse AddResponse, ...
				    0x0A, 0x01, 0x00, // resultCode success
				    0x04, 0x00,		// Empty matched DN	
				    0x04, 0x00		// Empty errorMessage	
	        });

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the AddResponse PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
    	
        // Check the decoded AddResponse
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        AddResponse addResponse      = message.getAddResponse();
        
        assertEquals( 1, message.getMessageId() );
        assertEquals( 0, addResponse.getLdapResult().getResultCode() );
        assertEquals( "", addResponse.getLdapResult().getMatchedDN() );
        assertEquals( "", addResponse.getLdapResult().getErrorMessage() );

        // Check the length
        assertEquals(0x0E, message.computeLength());
        
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
