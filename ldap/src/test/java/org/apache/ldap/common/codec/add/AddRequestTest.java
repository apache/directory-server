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
package org.apache.ldap.common.codec.add;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.add.AddRequest;
import org.apache.ldap.common.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the AddRequest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddRequestTest extends TestCase {
    /**
     * Test the decoding of a AddRequest
     */
    public void testDecodeAddRequestSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x59 );
        
        stream.put(
            new byte[]
            {
                0x30, 0x57, 		// LDAPMessage ::= SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				0x68, 0x52, 		//     CHOICE { ..., addRequest   AddRequest, ...
                        			// AddRequest ::= [APPLICATION 8] SEQUENCE {
									//     entry           LDAPDN,
				0x04, 0x20, 'c', 'n', '=', 't', 'e', 's', 't', 'M', 'o', 'd', 'i', 'f', 'y', ',', 'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm',
									//     attributes      AttributeList }
                0x30, 0x2E,         // AttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x0c,         // attribute 1
                0x04, 0x01, 'l',    //     type    AttributeDescription,
                0x31, 0x07,         //     vals    SET OF AttributeValue }
                0x04, 0x05, 'P', 'a', 'r', 'i', 's',

                0x30, 0x1E,         // attribute 2
                					//     type    AttributeDescription,
                0x04, 0x05, 'a', 't', 't', 'r', 's', 
                0x31, 0x15,         //     vals    SET OF AttributeValue }
                0x04, 0x05, 't', 'e', 's', 't', '1',
                0x04, 0x05, 't', 'e', 's', 't', '2',
                0x04, 0x05, 't', 'e', 's', 't', '3',
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a AddRequest message
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
        AddRequest addRequest      = message.getAddRequest();

        // Check the decoded message
        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( "cn=testModify,ou=users,ou=system", addRequest.getEntry() );

        Attributes attributes = addRequest.getAttributes();
        
        Assert.assertEquals( 2, attributes.size() );
        
        HashSet expectedTypes = new HashSet();
        
        expectedTypes.add("l");
        expectedTypes.add("attrs");
        
        HashMap typesVals = new HashMap();
        
        HashSet lVal1 = new HashSet();
        lVal1.add("Paris");
        typesVals.put("l", lVal1);
        
        HashSet lVal2 = new HashSet();
        lVal2.add("test1");
        lVal2.add("test2");
        lVal2.add("test3");
        typesVals.put("attrs", lVal2);
        
        BasicAttribute attributeValue = (BasicAttribute)attributes.get( "l" );
            
        Assert.assertTrue( expectedTypes.contains( attributeValue.getID().toLowerCase() ) );
            
        NamingEnumeration values = attributeValue.getAll();
        HashSet vals = (HashSet)typesVals.get( attributeValue.getID().toLowerCase() );

        while ( values.hasMore() )
        {
            Object value = values.next();
            
            Assert.assertTrue( vals.contains( value.toString() ) );
            
            vals.remove( value.toString() );
        }

        attributeValue = (BasicAttribute)attributes.get( "attrs" );
        
	    Assert.assertTrue( expectedTypes.contains( attributeValue.getID().toLowerCase() ) );
	        
	    values = attributeValue.getAll();
	    vals = (HashSet)typesVals.get( attributeValue.getID().toLowerCase() );
	
	    while ( values.hasMore() )
	    {
	        Object value = values.next();
	        
	        Assert.assertTrue( vals.contains( value.toString() ) );
	        
	        vals.remove( value.toString() );
	    }
	    
        // Check the length
        Assert.assertEquals(0x59, message.computeLength());
	    
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
