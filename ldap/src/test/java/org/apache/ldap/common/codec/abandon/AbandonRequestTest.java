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
package org.apache.ldap.common.codec.abandon;

import java.nio.ByteBuffer;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.abandon.AbandonRequest;
import org.apache.ldap.common.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test an AbandonRequest
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AbandonRequestTest extends TestCase {
    /**
     * Test the decoding of a AbandonRequest with no controls
     */
    public void testDecodeAbandonRequestNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x08 );
        stream.put(
            new byte[]
            {
                0x30, 0x06, 		// LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x03, 	//        messageID MessageID
				0x50, 0x01, 0x02	//        CHOICE { ..., abandonRequest AbandonRequest,...
									// AbandonRequest ::= [APPLICATION 16] MessageID
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessageContainer Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
    	
        // Check that everything is OK
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        AbandonRequest abandonRequest = message.getAbandonRequest();

        Assert.assertEquals( 3, message.getMessageId() );
        Assert.assertEquals( 2, abandonRequest.getAbandonedMessageId() );
        
        // Check the length
        Assert.assertEquals(8, message.computeLength());
        
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
     * Test the decoding of a AbandonRequest with controls
     */
    public void testDecodeAbandonRequestWithControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x64 );
        stream.put(
            new byte[]
            {
                0x30, 0x62,         // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x03,   //        messageID MessageID
                0x50, 0x01, 0x02,    //        CHOICE { ..., abandonRequest AbandonRequest,...
                (byte)0xA0, 0x5A,   //    controls       [0] Controls OPTIONAL }
                0x30, 0x1A,         // Control ::= SEQUENCE {
                                    //    controlType             LDAPOID, 
                0x04, 0x0D, '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '1',
                0x01, 0x01, (byte)0xFF,   //    criticality             BOOLEAN DEFAULT FALSE,
                                    //    controlValue            OCTET STRING OPTIONAL }
                0x04, 0x06, 'a', 'b', 'c', 'd', 'e', 'f',
                0x30, 0x17,         // Control ::= SEQUENCE {
                                    //    controlType             LDAPOID, 
                0x04, 0x0D, '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '2',
                                    //    controlValue            OCTET STRING OPTIONAL }
                0x04, 0x06, 'g', 'h', 'i', 'j', 'k', 'l',
                0x30, 0x12,         // Control ::= SEQUENCE {
                                    //    controlType             LDAPOID, 
                0x04, 0x0D, '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '3',
                0x01, 0x01, (byte)0xFF,   //    criticality             BOOLEAN DEFAULT FALSE}
                0x30, 0x0F,         // Control ::= SEQUENCE {
                                    //    controlType             LDAPOID} 
                0x04, 0x0D, '1', '.', '3', '.', '6', '.', '1', '.', '5', '.', '5', '.', '4'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessageContainer Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        // Check that everything is OK
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        AbandonRequest abandonRequest = message.getAbandonRequest();

        Assert.assertEquals( 3, message.getMessageId() );
        Assert.assertEquals( 2, abandonRequest.getAbandonedMessageId() );
        
        // Check the length
        Assert.assertEquals(0x64, message.computeLength());
        
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
     * Test the decoding of a AbandonRequest with no controls
     */
    public void testDecodeAbandonRequestNoControlsHighMessageId()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0A );
        stream.put(
            new byte[]
            {
                0x30, 0x08,         // LDAPMessage ::=SEQUENCE {
                0x02, 0x03, 0x00, (byte)0x80, 0x13,  //        messageID MessageID
                0x50, 0x01, 0x02    //        CHOICE { ..., abandonRequest AbandonRequest,...
                                    // AbandonRequest ::= [APPLICATION 16] MessageID
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessageContainer Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        // Check that everything is OK
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        AbandonRequest abandonRequest = message.getAbandonRequest();

        Assert.assertEquals( 32787, message.getMessageId() );
        Assert.assertEquals( 2, abandonRequest.getAbandonedMessageId() );
        
        // Check the length
        Assert.assertEquals(10, message.computeLength());
        
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
     * Test the decoding of a AbandonRequest with no controls
     */
    public void testDecodeAbandonRequestNoMessageId()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x0A );
        stream.put(
            new byte[]
            {
                0x30, 0x08,         // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01,	//        messageID MessageID
                0x50, 0x00    		//        CHOICE { ..., abandonRequest AbandonRequest,...
                                    // AbandonRequest ::= [APPLICATION 16] MessageID
            } );

        stream.flip();

        // Allocate a LdapMessageContainer Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the PDU
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
