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
package org.apache.ldap.common.codec.search;

import java.nio.ByteBuffer;
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
import org.apache.ldap.common.codec.search.SearchResultEntry;
import org.apache.ldap.common.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the SearchResultEntry codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchResultEntryTest extends TestCase {
    /**
     * Test the decoding of a SearchResultEntry
     */
    public void testDecodeSearchResultEntrySuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x50 );
        
        stream.put(
            new byte[]
            {
                 
                
                0x30, 0x4e, 		// LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				0x64, 0x49, 		//     CHOICE { ..., searchResEntry  SearchResultEntry, ...
                        			// SearchResultEntry ::= [APPLICATION 4] SEQUENCE {
									//     objectName      LDAPDN,
				0x04, 0x1b, 'o', 'u', '=', 'c', 'o', 'n', 't', 'a', 'c', 't', 's', ',', 'd', 'c', '=', 'i', 'k', 't', 'e', 'k', ',', 'd', 'c', '=', 'c', 'o', 'm',
									//     attributes      PartialAttributeList }
									// PartialAttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x2a, 
                0x30, 0x28, 
                					//     type    AttributeDescription,
                0x04, 0x0b, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                					//     vals    SET OF AttributeValue }
                0x31, 0x19, 
                					// AttributeValue ::= OCTET STRING
                0x04, 0x03, 't', 'o', 'p', 
									// AttributeValue ::= OCTET STRING
                0x04, 0x12, 'o', 'r', 'g', 'a', 'n', 'i', 'z', 'a', 't', 'i', 'o', 'n', 'a', 'l', 'U', 'n', 'i', 't'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a BindRequest Container
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
        SearchResultEntry searchResultEntry      = message.getSearchResultEntry();

        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( "ou=contacts,dc=iktek,dc=com", searchResultEntry.getObjectName() );

        Attributes partialAttributesList = searchResultEntry.getPartialAttributeList();
        
        Assert.assertEquals( 1, partialAttributesList.size() );
        
        for ( int i = 0; i < partialAttributesList.size(); i++ )
        {
            BasicAttribute attributeValue = (BasicAttribute)partialAttributesList.get( "objectclass" );
            
            Assert.assertEquals( "objectClass".toLowerCase(), attributeValue.getID().toLowerCase() );
            
            NamingEnumeration values = attributeValue.getAll();
            
            HashSet expectedValues = new HashSet();
            
            expectedValues.add( "top" );
            expectedValues.add( "organizationalUnit" ); 
            
            while ( values.hasMore() )
            {
                Object value = values.next();
                
                Assert.assertTrue( expectedValues.contains( value.toString() ) );
                
                expectedValues.remove( value.toString() );
            }
        }
        
        // Check the length
        Assert.assertEquals(0x50, message.computeLength());

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
     * Test the decoding of a SearchResultEntry
     */
    public void testDecodeSearchResultEntry2AttrsSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x7b );
        
        stream.put(
            new byte[]
            {
                0x30, 0x79, 		// LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	//     messageID MessageID
				0x64, 0x74, 		//     CHOICE { ..., searchResEntry  SearchResultEntry, ...
                        			// SearchResultEntry ::= [APPLICATION 4] SEQUENCE {
									//     objectName      LDAPDN,
				0x04, 0x1b, 'o', 'u', '=', 'c', 'o', 'n', 't', 'a', 'c', 't', 's', ',', 'd', 'c', '=', 'i', 'k', 't', 'e', 'k', ',', 'd', 'c', '=', 'c', 'o', 'm',
									//     attributes      PartialAttributeList }
									// PartialAttributeList ::= SEQUENCE OF SEQUENCE {
                0x30, 0x55, 
                0x30, 0x28, 
                					//     type    AttributeDescription,
                0x04, 0x0b, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                					//     vals    SET OF AttributeValue }
                0x31, 0x19, 
                					// AttributeValue ::= OCTET STRING
                0x04, 0x03, 't', 'o', 'p', 
									// AttributeValue ::= OCTET STRING
                0x04, 0x12, 'o', 'r', 'g', 'a', 'n', 'i', 'z', 'a', 't', 'i', 'o', 'n', 'a', 'l', 'U', 'n', 'i', 't',
                0x30, 0x29, 
				//     type    AttributeDescription,
				0x04, 0x0c, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', '2',
								//     vals    SET OF AttributeValue }
				0x31, 0x19, 
								// AttributeValue ::= OCTET STRING
				0x04, 0x03, 't', 'o', 'p', 
								// AttributeValue ::= OCTET STRING
				0x04, 0x12, 'o', 'r', 'g', 'a', 'n', 'i', 'z', 'a', 't', 'i', 'o', 'n', 'a', 'l', 'U', 'n', 'i', 't'
            } );

        stream.flip();

        // Allocate a BindRequest Container
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
        SearchResultEntry searchResultEntry      = message.getSearchResultEntry();

        Assert.assertEquals( 1, message.getMessageId() );
        Assert.assertEquals( "ou=contacts,dc=iktek,dc=com", searchResultEntry.getObjectName() );

        Attributes partialAttributesList = searchResultEntry.getPartialAttributeList();
        
        Assert.assertEquals( 2, partialAttributesList.size() );
        
        String[] expectedAttributes = new String[]{"objectClass", "objectClass2"}; 
        
        for ( int i = 0; i < expectedAttributes.length; i++ )
        {
            BasicAttribute attributeValue = (BasicAttribute)partialAttributesList.get( expectedAttributes[i] );
            
            Assert.assertEquals( expectedAttributes[i].toLowerCase(), attributeValue.getID().toLowerCase() );
            
            NamingEnumeration values = attributeValue.getAll();
            
            HashSet expectedValues = new HashSet();
            
            expectedValues.add( "top" );
            expectedValues.add( "organizationalUnit" ); 
            
            while ( values.hasMore() )
            {
                Object value = values.next();
                
                Assert.assertTrue( expectedValues.contains( value.toString() ) );
                
                expectedValues.remove( value.toString() );
            }
        }
        
        // Check the length
        Assert.assertEquals(0x7b, message.computeLength());

        // Check the encoding
        try
        {
            message.encode( null );
            
            // We cant compare the encodings, the order of the attributes has changed
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            Assert.fail( ee.getMessage() );
        }
    }
    
    /**
     * Test the decoding of a SearchResultEntry with more bytes
     * to be decoded at the end
     */
    public void testDecodeSearchResultEntrySuccessWithFollowingMessage() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x66 );
        
        stream.put(
            new byte[]
            {
                  0x30, 0x5F, 		// LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x02, //     messageID MessageID
                  0x64, 0x5A,       //     CHOICE { ..., searchResEntry  SearchResultEntry, ...
      								// SearchResultEntry ::= [APPLICATION 4] SEQUENCE {
									//     objectName      LDAPDN,
                    0x04, 0x13, 'u', 'i', 'd', '=', 'a', 'd', 'm', 'i', 'n', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm', 
									//     attributes      PartialAttributeList }
                    0x30, 0x43, 	// PartialAttributeList ::= SEQUENCE OF SEQUENCE {
                      0x30, 0x41, 
                      				//     type    AttributeDescription,
                        0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 
                        0x31, 0x32, //     vals    SET OF AttributeValue }
                        			// AttributeValue ::= OCTET STRING
                          0x04, 0x0D, 'i', 'n', 'e', 't', 'O', 'r', 'g', 'P', 'e', 'r', 's', 'o', 'n',  
              						// AttributeValue ::= OCTET STRING
                          0x04, 0x14, 'o', 'r', 'g', 'a', 'n', 'i', 'z', 'a', 't', 'i', 'o', 'n', 'a', 'l', 'P', 'e', 'r', 's', 'o', 'n', 
              						// AttributeValue ::= OCTET STRING
                          0x04, 0x06, 'p', 'e', 'r', 's', 'o', 'n', 
              						// AttributeValue ::= OCTET STRING
                          0x04, 0x03, 't', 'o', 'p', 
                  0x30, 0x45, 		// Start of the next message
                  0x02, 0x01, 0x02  // messageID MessageID ...
            } );
                
        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a BindRequest Container
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
        SearchResultEntry searchResultEntry      = message.getSearchResultEntry();

        Assert.assertEquals( 2, message.getMessageId() );
        Assert.assertEquals( "uid=admin,ou=system", searchResultEntry.getObjectName() );

        Attributes partialAttributesList = searchResultEntry.getPartialAttributeList();
        
        Assert.assertEquals( 1, partialAttributesList.size() );
        
        for ( int i = 0; i < partialAttributesList.size(); i++ )
        {
            BasicAttribute attributeValue = (BasicAttribute)partialAttributesList.get( "objectclass" );
            
            Assert.assertEquals( "objectClass".toLowerCase(), attributeValue.getID().toLowerCase() );
            
            NamingEnumeration values = attributeValue.getAll();
            
            HashSet expectedValues = new HashSet();
            
            expectedValues.add( "top" );
            expectedValues.add( "person" );
            expectedValues.add( "organizationalPerson" ); 
            expectedValues.add( "inetOrgPerson" ); 
            
            while ( values.hasMore() )
            {
                Object value = values.next();
                
                Assert.assertTrue( expectedValues.contains( value.toString() ) );
                
                expectedValues.remove( value.toString() );
            }
        }
        
        // Check the length
        Assert.assertEquals(0x61, message.computeLength());
        
        // Check that the next bytes is the first of the next PDU
        Assert.assertEquals(0x30, stream.get(stream.position()));
        Assert.assertEquals(0x45, stream.get(stream.position() + 1));
        Assert.assertEquals(0x02, stream.get(stream.position() + 2));
        Assert.assertEquals(0x01, stream.get(stream.position() + 3));
        Assert.assertEquals(0x02, stream.get(stream.position() + 4));

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            // We have to supress the last 5 chars from the decodedPDU, as they
            // belongs to the next message.
            Assert.assertEquals(encodedPdu, decodedPdu.substring(0, 0x61 * 5) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            Assert.fail( ee.getMessage() );
        }
    }
}
