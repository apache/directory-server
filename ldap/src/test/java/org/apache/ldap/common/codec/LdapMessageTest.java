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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessageContainer;

import java.nio.ByteBuffer;


/**
 * A global Ldap Decoder test
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapMessageTest extends TestCase
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Test the decoding of null length messageId
     */
    public void testDecodeMessageLengthNull()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x02 );
        stream.put(
            new byte[]
            {
                0x30, 0x00, 		// LDAPMessage ::=SEQUENCE {
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
        	assertTrue( true );
        	return;
        }

        Assert.fail( "We should not reach this point !" );
    }
    
    /**
     * Test the decoding of null length messageId
     */
    public void testDecodeMessageIdLengthNull()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x04 );
        stream.put(
            new byte[]
            {
                0x30, 0x02, 		// LDAPMessage ::=SEQUENCE {
                0x02, 0x00 		//         messageID MessageID
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
        	assertTrue( true );
        	return;
        }

        Assert.fail( "We should not reach this point !" );
    }

    /**
     * Test the decoding of null length messageId
     */
    public void testDecodeMessageIdMinusOne()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x05 );
        stream.put(
            new byte[]
            {
                0x30, 0x03, 		// LDAPMessage ::=SEQUENCE {
                0x02, 0x01, (byte)0xff//         messageID MessageID = -1
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
        	assertTrue( true );
        	return;
        }

        Assert.fail( "We should not reach this point !" );
    }

    /**
     * Test the decoding of null length messageId
     */
    public void testDecodeMessageIdMaxInt()
    {

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x08 );
        stream.put(
            new byte[]
            {
                0x30, 0x06, 		// LDAPMessage ::=SEQUENCE {
                0x02, 0x04, (byte)0x7f, (byte)0xff, (byte)0xff, (byte)0xff//         messageID MessageID = -1
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a BindRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
        	assertTrue( true );
        	return;
        }

        Assert.fail( "We should not reach this point !" );
    }
}
