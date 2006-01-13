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
package org.apache.ldap.common.codec.compare;

import java.nio.ByteBuffer;

import javax.naming.NamingException;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.compare.CompareRequest;
import org.apache.ldap.common.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the CompareRequest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CompareRequestTest extends TestCase {

    /**
     * Test the decoding of a full CompareRequest
     */
    public void testDecodeCompareRequestSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x38 );
        
        stream.put(
            new byte[]
            {
                0x30, 0x36, 		// LDAPMessage ::= SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				            		//     CHOICE { ..., compareRequest   CompareRequest, ...
				0x6E, 0x31,         // CompareRequest ::= [APPLICATION 14] SEQUENCE {
				                    //     entry           LDAPDN,
				0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
				                    //     ava             AttributeValueAssertion }
				0x30, 0x0D,         // AttributeValueAssertion ::= SEQUENCE {
                                    //     attributeDesc   AttributeDescription,
				0x04, 0x04, 't', 'e', 's', 't',
				                    //     assertionValue  AssertionValue }
				0x04, 0x05, 'v', 'a', 'l', 'u', 'e'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the CompareRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
    	
        // Ceck the decoded CompareRequest PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        CompareRequest compareRequest      = message.getCompareRequest();

        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( "cn=testModify,ou=users,ou=system", compareRequest.getEntry() );
        Assert.assertEquals( "test", compareRequest.getAttributeDesc() );
        Assert.assertEquals( "value", compareRequest.getAssertionValue().toString() );

        // Check the length
        Assert.assertEquals(0x38, message.computeLength());

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
     * Test the decoding of an empty entry CompareRequest
     */
    public void testDecodeCompareRequestEmptyEntry() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x18 );
        
        stream.put(
            new byte[]
            {
                    0x30, 0x16, 		// LDAPMessage ::= SEQUENCE {
    				0x02, 0x01, 0x01, 	//     messageID MessageID
    				            		//     CHOICE { ..., compareRequest   CompareRequest, ...
    				0x6E, 0x11,         // CompareRequest ::= [APPLICATION 14] SEQUENCE {
    				0x04, 0x00,         //     entry           LDAPDN,
    				                    //     ava             AttributeValueAssertion }
    				0x30, 0x0D,         // AttributeValueAssertion ::= SEQUENCE {
                                        //     attributeDesc   AttributeDescription,
    				0x04, 0x04, 't', 'e', 's', 't',
    				                    //     assertionValue  AssertionValue }
    				0x04, 0x05, 'v', 'a', 'l', 'u', 'e'
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the CompareRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
            Assert.fail("We should never reach this point !!!");
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }
    }
}
