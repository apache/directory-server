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
package org.apache.ldap.common.codec.del;

import java.nio.ByteBuffer;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.del.DelResponse;
import org.apache.ldap.common.util.StringTools;

import junit.framework.TestCase;

/**
 * Test the DelResponse codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DelResponseTest extends TestCase {
    /**
     * Test the decoding of a DelResponse
     */
    public void testDecodeDelResponseSuccess()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x2D );
        
        stream.put(
            new byte[]
            {
                0x30, 0x2B, 		// LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	//         messageID MessageID
				0x6B, 0x26, 		//        CHOICE { ..., delResponse DelResponse, ...
                        			// DelResponse ::= [APPLICATION 11] LDAPResult
				0x0A, 0x01, 0x21, 	//   LDAPResult ::= SEQUENCE {
									//		resultCode ENUMERATED {
									//			success (0), ...
				 					//      },
				0x04, 0x1F,			//		matchedDN    LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x04, 0x00  		//      errorMessage LDAPString,
									//		referral     [3] Referral OPTIONAL }
									// }
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the DelResponse PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
    	
        // Check the decoded DelResponse PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        DelResponse delResponse      = message.getDelResponse();

        assertEquals( 1, message.getMessageId() );
        assertEquals( 33, delResponse.getLdapResult().getResultCode() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", delResponse.getLdapResult().getMatchedDN() );
        assertEquals( "", delResponse.getLdapResult().getErrorMessage() );

        // Check the length
        assertEquals(0x2D, message.computeLength());
        
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
     * Test the decoding of a DelResponse with no LdapResult
     */
    public void testDecodeDelResponseEmptyResult()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x07 );
        
        stream.put(
            new byte[]
            {
                    0x30, 0x05, 		// LDAPMessage ::=SEQUENCE {
    				  0x02, 0x01, 0x01, //         messageID MessageID
    				  0x6B, 0x00, 		//        CHOICE { ..., delResponse DelResponse, ...
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a DelResponse message
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
}
