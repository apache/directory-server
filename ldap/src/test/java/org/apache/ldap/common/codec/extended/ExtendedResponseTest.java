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
import org.apache.ldap.common.codec.extended.ExtendedResponse;
import org.apache.ldap.common.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the ExtendedResponse codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtendedResponseTest extends TestCase {
    /**
     * Test the decoding of a full ExtendedResponse
     */
    public void testDecodeExtendedResponseSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x1D );
        
        stream.put(
            new byte[]
            {
                0x30, 0x1B, 		// LDAPMessage ::= SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				            		//     CHOICE { ..., extendedResp     ExtendedResponse, ...
				0x78, 0x16,         // ExtendedResponse ::= [APPLICATION 23] SEQUENCE {
				                    //     COMPONENTS OF LDAPResult,
				0x0A, 0x01, 0x00, 	//   LDAPResult ::= SEQUENCE {
				                    //		resultCode ENUMERATED {
				                    //			success (0), ...
					                //      },
				0x04, 0x00,			//		matchedDN    LDAPDN,
				0x04, 0x00,  		//      errorMessage LDAPString,
				                    //		referral     [3] Referral OPTIONAL }				                    //     requestName      [0] LDAPOID,
				                    //    responseName     [10] LDAPOID OPTIONAL,
				(byte)0x8A, 0x06, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02,
				                    //    response         [11] OCTET STRING OPTIONAL }
				(byte)0x8B, 0x05, 'v', 'a', 'l', 'u', 'e'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the ExtendedResponse PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
    	
        // Check the decoded ExtendedResponse PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedResponse extendedResponse      = message.getExtendedResponse();

        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( 0, extendedResponse.getLdapResult().getResultCode() );
        Assert.assertEquals( "", extendedResponse.getLdapResult().getMatchedDN() );
        Assert.assertEquals( "", extendedResponse.getLdapResult().getErrorMessage() );
        Assert.assertEquals( "1.3.6.1.5.5.2", extendedResponse.getResponseName() );
        Assert.assertEquals( "value", StringTools.utf8ToString( (byte[])extendedResponse.getResponse() ) );
        
        // Check the length
        Assert.assertEquals(0x1D, message.computeLength());
        
        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            Assert.assertEquals(encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            Assert.fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a ExtendedRequest with only a name
     */
    public void testDecodeExtendedRequestName() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0E );
        
        stream.put(
            new byte[]
            {
                    0x30, 0x0C, 		// LDAPMessage ::= SEQUENCE {
    				0x02, 0x01, 0x01, 	//     messageID MessageID
    				            		//     CHOICE { ..., extendedResp     Response, ...
    				0x78, 0x07,         // ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
    				                    //     COMPONENTS OF LDAPResult,
    				0x0A, 0x01, 0x00, 	//   LDAPResult ::= SEQUENCE {
    				                    //		resultCode ENUMERATED {
    				                    //			success (0), ...
    					                //      },
    				0x04, 0x00,			//		matchedDN    LDAPDN,
    				0x04, 0x00  		//      errorMessage LDAPString,
    				                    //		referral     [3] Referral OPTIONAL }				                    //     requestName      [0] LDAPOID,
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the ExtendedResponse PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
    	
        // Check the decoded ExtendedResponse PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ExtendedResponse extendedResponse      = message.getExtendedResponse();

        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( 0, extendedResponse.getLdapResult().getResultCode() );
        Assert.assertEquals( "", extendedResponse.getLdapResult().getMatchedDN() );
        Assert.assertEquals( "", extendedResponse.getLdapResult().getErrorMessage() );

        // Check the length
        Assert.assertEquals(0x0E, message.computeLength());

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            Assert.assertEquals(encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            Assert.fail( ee.getMessage() );
        }
    }
}
