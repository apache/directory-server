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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.ModificationItem;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.modify.ModifyRequest;
import org.apache.ldap.common.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the ModifyRequest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyRequestTest extends TestCase {
    /**
     * Test the decoding of a ModifyRequest
     */
    public void testDecodeModifyRequest2AttrsSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x54 );
        
        stream.put(
            new byte[]
            {
                 
                
                0x30, 0x52, 		// LDAPMessage ::= SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				0x66, 0x4d, 		//     CHOICE { ..., modifyRequest   ModifyRequest, ...
                        			// ModifyRequest ::= [APPLICATION 6] SEQUENCE {
									//     object          LDAPDN,
				0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
                0x30, 0x29,         //     modification    SEQUENCE OF SEQUENCE {
                0x30, 0x11,
                0x0A, 0x01, 0x02,   //         operation       ENUMERATED {
                                    //             add     (0),
                                    //             delete  (1),
                                    //             replace (2) },
                                    //         modification    AttributeTypeAndValues } }
                0x30, 0x0c,         // AttributeTypeAndValues ::= SEQUENCE {
                0x04, 0x01, 'l',    //     type    AttributeDescription,
                0x31, 0x07,         //     vals    SET OF AttributeValue }
                0x04, 0x05, 'P', 'a', 'r', 'i', 's',

                0x30, 0x14,         //      modification    SEQUENCE OF *SEQUENCE* {
                0x0A, 0x01, 0x00,   //         operation       ENUMERATED {
                					//             add     (0),
                					//             delete  (1),
                					//             replace (2) },
                					//         modification    AttributeTypeAndValues } }
                0x30, 0x0f,         // AttributeTypeAndValues ::= SEQUENCE {
                0x04, 0x05, 'a', 't', 't', 'r', 's', //     type    AttributeDescription,
                0x31, 0x06,         //     vals    SET OF AttributeValue }
                0x04, 0x04, 't', 'e', 's', 't'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
    	
        // Check the decoded PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyRequest modifyRequest      = message.getModifyRequest();
        
        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( "cn=testModify,ou=users,ou=system", modifyRequest.getObject() );

        ArrayList modifications = modifyRequest.getModifications();
        
        Assert.assertEquals( 2, modifications.size() );
        
        HashSet expectedTypes = new HashSet();
        
        expectedTypes.add("l");
        expectedTypes.add("attrs");
        
        HashMap typesVals = new HashMap();
        
        HashSet lVals = new HashSet();
        lVals.add("Paris");
        typesVals.put("l", lVals);
        
        HashSet attrsVals = new HashSet();
        attrsVals.add("test");
        typesVals.put("attrs", attrsVals);
        
        ModificationItem modification = (ModificationItem)modifications.get( 1 );
        BasicAttribute attributeValue = (BasicAttribute)modification.getAttribute();
            
        Assert.assertTrue( expectedTypes.contains( attributeValue.getID().toLowerCase() ) );
            
        NamingEnumeration values = attributeValue.getAll();
        HashSet vals = (HashSet)typesVals.get( attributeValue.getID().toLowerCase() );

        while ( values.hasMore() )
        {
            Object value = values.next();
            
            if ( value instanceof String )
            {
                Assert.assertTrue( vals.contains( value ) );
            }
            else
            {
                Assert.assertTrue( vals.contains( StringTools.utf8ToString( (byte[])value ) ) );
            }
            
            vals.remove( value.toString() );
        }

        // Check the length
        Assert.assertEquals(0x54, message.computeLength());

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
     * Test the decoding of a ModifyRequest, with different operations
     */
    public void testDecodeModifyRequestManyOperations() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x18C );
        
        stream.put(
            new byte[]
            {
                      0x30, (byte)0x81, (byte)0x89, 
                        0x02, 0x01, 0x15, 
                        0x66, 0x67,         // ModifyRequest
                          0x04, 0x2B,       // object : cn=Tori Amos,ou=playground,dc=apache,dc=org
                            0x63, 0x6E, 0x3D, 0x54, 0x6F, 0x72, 0x69, 0x20,  
                            0x41, 0x6D, 0x6F, 0x73, 0x2C, 0x6F, 0x75, 0x3D, 
                            0x70, 0x6C, 0x61, 0x79, 0x67, 0x72, 0x6F, 0x75, 
                            0x6E, 0x64, 0x2C, 0x64, 0x63, 0x3D, 0x61, 0x70, 
                            0x61, 0x63, 0x68, 0x65, 0x2C, 0x64, 0x63, 0x3D, 
                            0x6F, 0x72, 0x67, 
                          0x30, 0x38,               // Modifications
                            0x30, 0x24,             // Modification
                              0x0A, 0x01, 0x00,     // Operation = ADD
                              0x30, 0x1F,           // type : telephoneNumber
                                0x04, 0x0F, 
                                  0x74, 0x65, 0x6C, 0x65, 0x70, 0x68, 0x6F, 0x6E,
                                  0x65, 0x4E, 0x75, 0x6D, 0x62, 0x65, 0x72, 
                              0x31, 0x0C,           // vals : 1234567890
                                0x04, 0x0A, 
                                  0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, // 1234567890
                                  0x39, 0x30, 
                            0x30, 0x10,             // Modification
                              0x0A, 0x01, 0x02,     // Operation = REPLACE
                              0x30, 0x0B,           // type : cn
                                0x04, 0x02, 
                                  0x63, 0x6E, 
                                0x31, 0x05,         // vals : XXX
                                  0x04, 0x03, 
                                    0x58, 0x58, 0x58, 
                        (byte)0xA0, 0x1B,           // Controls : 2.16.840.1.113730.3.4.2
                          0x30, 0x19, 
                            0x04, 0x17, 
                              0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 
                              0x2E, 0x31, 0x2E, 0x31, 0x31, 0x33, 0x37, 0x33, 
                              0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32                     
            } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        // Check the decoded PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyRequest modifyRequest      = message.getModifyRequest();
        
        Assert.assertEquals( 21, message.getMessageId() );
        Assert.assertEquals( "cn=Tori Amos,ou=playground,dc=apache,dc=org", modifyRequest.getObject() );

        ArrayList modifications = modifyRequest.getModifications();
        
        Assert.assertEquals( 2, modifications.size() );
        
        HashSet expectedTypes = new HashSet();
        
        expectedTypes.add("telephoneNumber");
        expectedTypes.add("cn");
        
        HashMap typesVals = new HashMap();
        
        HashSet telephoneVals = new HashSet();
        telephoneVals.add("1234567890");
        typesVals.put("telephoneNumber", telephoneVals);
        
        HashSet cnVals = new HashSet();
        cnVals.add("XXX");
        typesVals.put("cn", cnVals);
        
        ModificationItem modification = (ModificationItem)modifications.get( 1 );
        BasicAttribute attributeValue = (BasicAttribute)modification.getAttribute();
            
        Assert.assertTrue( expectedTypes.contains( attributeValue.getID().toLowerCase() ) );
            
        NamingEnumeration values = attributeValue.getAll();
        HashSet vals = (HashSet)typesVals.get( attributeValue.getID().toLowerCase() );

        while ( values.hasMore() )
        {
            Object value = values.next();
            
            if ( value instanceof String )
            {
                Assert.assertTrue( vals.contains( value ) );
            }
            else
            {
                Assert.assertTrue( vals.contains( StringTools.utf8ToString( (byte[])value ) ) );
            }
            
            vals.remove( value.toString() );
        }

        // Check the length
        Assert.assertEquals(0x8C, message.computeLength());

        // Check the encoding, by decoding and re-encoding the result
        try
        {
            ByteBuffer bb = message.encode( null );
            String decodedPdu1 = StringTools.dumpBytes( bb.array() );
            
            try
            {
                ldapDecoder.decode( bb, ldapMessageContainer );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                Assert.fail( de.getMessage() );
            }
            
            LdapMessage message2 = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
            
            
            ByteBuffer bb2 = message2.encode( null );
            String decodedPdu2 = StringTools.dumpBytes( bb2.array() );

            Assert.assertEquals( decodedPdu1, decodedPdu2 );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            Assert.fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a ModifyRequest, with different operations, take 2
     */
    public void testDecodeModifyRequestManyOperations2() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x18C );
        
        stream.put(
            new byte[]
            {
                    0x30, (byte)0x81, (byte)0xB6,           // LdapMessage
                    0x02, 0x01, 0x31,                       // Message ID : 49
                      0x66, (byte)0x81, (byte)0x93,         // ModifyRequest
                        0x04, 0x2B,                         // object : cn=Tori Amos,ou=playground,dc=apache,dc=org 
                          0x63, 0x6E, 0x3D, 0x54, 0x6F, 0x72, 0x69, 0x20, 
                          0x41, 0x6D, 0x6F, 0x73, 0x2C, 0x6F, 0x75, 0x3D, 
                          0x70, 0x6C, 0x61, 0x79, 0x67, 0x72, 0x6F, 0x75, 
                          0x6E, 0x64, 0x2C, 0x64, 0x63, 0x3D, 0x61, 0x70, 
                          0x61, 0x63, 0x68, 0x65, 0x2C, 0x64, 0x63, 0x3D, 
                          0x6F, 0x72, 0x67, 
                        0x30, 0x64,                         // Modifications
                          0x30, 0x14,                       // Modification  
                            0x0A, 0x01, 0x01,               // Operation : Delete
                            0x30, 0x0F,                     // type : description
                              0x04, 0x0B, 
                                0x64, 0x65, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 
                                0x69, 0x6F, 0x6E, 
                            0x31, 0x00,                     // Vals = null
                          0x30, 0x25,                       // Modification
                            0x0A, 0x01, 0x00,               // Operation : Add
                            0x30, 0x20,                     // type : telephoneNumber
                              0x04, 0x0F, 
                                0x74, 0x65, 0x6C, 0x65, 0x70, 0x68, 0x6F, 0x6E, 
                                0x65, 0x4E, 0x75, 0x6D, 0x62, 0x65, 0x72, 
                              0x31, 0x0D,                   // Vals : 01234567890
                                0x04, 0x0B, 
                                  0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 
                                  0x38, 0x39, 0x30, 
                          0x30, 0x25,                       // Modification
                            0x0A, 0x01, 0x00,               // Operation : Add
                            0x30, 0x20,                     // type : telephoneNumber
                              0x04, 0x0F, 
                                0x74, 0x65, 0x6C, 0x65, 0x70, 0x68, 0x6F, 0x6E, 
                                0x65, 0x4E, 0x75, 0x6D, 0x62, 0x65, 0x72, 
                            0x31, 0x0D,                     // Vals : 01234567890
                              0x04, 0x0B, 
                                0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 
                                0x38, 0x39, 0x30, 
                    (byte)0xA0, 0x1B,                       // Controls : 2.16.840.1.113730.3.4.2
                      0x30, 0x19, 
                      0x04, 0x17, 
                        0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 
                        0x2E, 0x31, 0x2E, 0x31, 0x31, 0x33, 0x37, 0x33, 
                        0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32             } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a ModifyRequest PDU
        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        // Check the decoded PDU
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        ModifyRequest modifyRequest      = message.getModifyRequest();
        
        Assert.assertEquals( 49, message.getMessageId() );
        Assert.assertEquals( "cn=Tori Amos,ou=playground,dc=apache,dc=org", modifyRequest.getObject() );

        ArrayList modifications = modifyRequest.getModifications();
        
        Assert.assertEquals( 3, modifications.size() );
        
        HashSet expectedTypes = new HashSet();
        
        expectedTypes.add("telephonenumber");
        expectedTypes.add("description");
        
        HashMap typesVals = new HashMap();
        
        HashSet telephoneVals = new HashSet();
        telephoneVals.add("01234567890");
        typesVals.put("telephonenumber", telephoneVals);
        
        ModificationItem modification = (ModificationItem)modifications.get( 1 );
        BasicAttribute attributeValue = (BasicAttribute)modification.getAttribute();
            
        Assert.assertTrue( expectedTypes.contains( attributeValue.getID().toLowerCase() ) );
            
        NamingEnumeration values = attributeValue.getAll();
        HashSet vals = (HashSet)typesVals.get( attributeValue.getID().toLowerCase() );

        while ( values.hasMore() )
        {
            Object value = values.next();
            
            if ( value instanceof String )
            {
                Assert.assertTrue( vals.contains( value ) );
            }
            else
            {
                Assert.assertTrue( vals.contains( StringTools.utf8ToString( (byte[])value ) ) );
            }
            
            vals.remove( value.toString() );
        }

        // Check the length
        Assert.assertEquals(0xB9, message.computeLength());

        // Check the encoding, by decoding and re-encoding the result
        try
        {
            ByteBuffer bb = message.encode( null );
            String decodedPdu1 = StringTools.dumpBytes( bb.array() );
            
            try
            {
                ldapDecoder.decode( bb, ldapMessageContainer );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                Assert.fail( de.getMessage() );
            }
            
            LdapMessage message2 = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
            
            
            ByteBuffer bb2 = message2.encode( null );
            String decodedPdu2 = StringTools.dumpBytes( bb2.array() );

            Assert.assertEquals( decodedPdu1, decodedPdu2 );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            Assert.fail( ee.getMessage() );
        }
    }
}
