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
package org.apache.ldap.common.codec.modifyDn;

import java.nio.ByteBuffer;

import javax.naming.NamingException;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.modifyDn.ModifyDNRequest;
import org.apache.ldap.common.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the ModifyDNRequest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyDNRequestTest extends TestCase {
    /**
     * Test the decoding of a full ModifyDNRequest
     */
    public void testDecodeModifyDNRequestSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x48 );
        
        stream.put(
            new byte[]
            {
                 
                
                0x30, 0x46, 		// LDAPMessage ::= SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				0x6C, 0x41, 		//     CHOICE { ..., modifyDNRequest   ModifyDNRequest, ...
                        			// ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
									//     entry           LDAPDN,
				0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
			                        //     newrdn          RelativeLDAPDN,
                0x04, 0x0F, 'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y',
                0x01, 0x01, 0x00,   //     deleteoldrdn    BOOLEAN,
                                    // newSuperior     [0] LDAPDN OPTIONAL }
                (byte)0x80, 0x09, 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
    	
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyDNRequest modifyDNRequest      = message.getModifyDNRequest();

        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( "cn=testModify,ou=users,ou=system", modifyDNRequest.getEntry() );
        Assert.assertEquals( false, modifyDNRequest.isDeleteOldRDN() );
        Assert.assertEquals( "cn=testDNModify", modifyDNRequest.getNewRDN() );
        Assert.assertEquals( "ou=system", modifyDNRequest.getNewSuperior() );

        // Check the length
        Assert.assertEquals(0x48, message.computeLength());
        
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
     * Test the decoding of a ModifyDNRequest without a superior
     */
    public void testDecodeModifyDNRequestWithoutSuperior() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x3D );
        
        stream.put(
            new byte[]
            {
                 
                
                0x30, 0x3B, 		// LDAPMessage ::= SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				0x6C, 0x36, 		//     CHOICE { ..., modifyDNRequest   ModifyDNRequest, ...
                        			// ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
									//     entry           LDAPDN,
				0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
			                        //     newrdn          RelativeLDAPDN,
                0x04, 0x0F, 'c', 'n', '=', 't', 'e', 's', 't', 'D', 'N', 'M', 'o', 'd', 'i', 'f', 'y',
                0x01, 0x01, 0x00   //     deleteoldrdn    BOOLEAN,
                                    // newSuperior     [0] LDAPDN OPTIONAL }
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a ModifyRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
    	
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyDNRequest modifyDNRequest      = message.getModifyDNRequest();

        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( "cn=testModify,ou=users,ou=system", modifyDNRequest.getEntry() );
        Assert.assertEquals( false, modifyDNRequest.isDeleteOldRDN() );
        Assert.assertEquals( "cn=testDNModify", modifyDNRequest.getNewRDN() );
        
        // Check the length
        Assert.assertEquals(0x3D, message.computeLength());
        
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
