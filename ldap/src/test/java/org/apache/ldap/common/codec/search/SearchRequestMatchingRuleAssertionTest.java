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
import java.util.HashMap;
import java.util.Map;

import javax.naming.directory.Attributes;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.name.DnOidContainer;
import org.apache.ldap.common.schema.DeepTrimToLowerNormalizer;
import org.apache.ldap.common.schema.OidNormalizer;
import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.search.SearchRequest;

import junit.framework.TestCase;

/**
 * A test case for SearchRequest messages
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestMatchingRuleAssertionTest extends TestCase {
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Map oids = new HashMap(); 
        oids.put( "dc", new OidNormalizer( "dc", new  DeepTrimToLowerNormalizer() ) );
        oids.put( "domaincomponent", new OidNormalizer( "dc", new  DeepTrimToLowerNormalizer() ) );
        oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new  DeepTrimToLowerNormalizer() ) );
        oids.put( "ou", new OidNormalizer( "ou", new  DeepTrimToLowerNormalizer() ) );
        oids.put( "organizationalUnitName", new OidNormalizer( "ou", new  DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.11", new OidNormalizer( "ou", new  DeepTrimToLowerNormalizer() ) );
        oids.put( "objectclass", new OidNormalizer( "objectclass", new  DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.0", new OidNormalizer( "objectclass", new  DeepTrimToLowerNormalizer() ) );
		
        DnOidContainer.setOids( oids );
	}
	
    /**
     * Tests an search request decode with a simple equality match filter.
     */
    public void testDecodeSearchRequestExtensibleMatch()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x63 );
        stream.put(
            new byte[]
            {
                    0x30, 0x61, 
                	0x02, 0x01, 0x01, 
                	0x63, 0x5C, // "dc=example,dc=com"
                		0x04, 0x11, 0x64, 0x63, 0x3D, 0x65, 0x78, 0x61, 0x6D, 0x70, 0x6C, 0x65, 0x2C, 0x64, 0x63, 0x3D, 0x63, 0x6F, 0x6D, 
                		0x0A, 0x01, 0x00, 
                		0x0A, 0x01, 0x02, 
                		0x02, 0x01, 0x02, 
                		0x02, 0x01, 0x03, 
                		0x01, 0x01, (byte)0xFF,
                		(byte)0xA9, 0x21,
                		  (byte)0x81, 0x02, 'c', 'n',
                		  (byte)0x82, 0x13, '1', '.', '2', '.', '8', '4', '0',   
                								  '.', '4', '8', '0', '1', '8', '.',  
                								  '1', '.', '2', '.', '2',
                	      (byte)0x83, 0x03, 'a', 'o', 'k',
                	      (byte)0x84, 0x01, (byte)0xFF,
                		0x30, 0x15,										// Attributes 
                			0x04, 0x05, 0x61, 0x74, 0x74, 0x72, 0x30, 	// attr0
                			0x04, 0x05, 0x61, 0x74, 0x74, 0x72, 0x31, 	// attr1
                			0x04, 0x05, 0x61, 0x74, 0x74, 0x72, 0x32	// attr2
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
            fail( de.getMessage() );
        }
    	
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr      = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_BASE_OBJECT, sr.getScope() );
        assertEquals( LdapConstants.DEREF_FINDING_BASE_OBJ, sr.getDerefAliases() );
        assertEquals( 2, sr.getSizeLimit() );
        assertEquals( 3, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x63, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x56 ), decodedPdu.substring( 0, 0x56 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with an empty extensible match
     */
    public void testDecodeSearchRequestEmptyExtensibleMatch() 
    {
        byte[] asn1BER = new byte[] 
        { 
                0x30, 0x3B, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x36, 
                    0x04, 0x1F,                   //    baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                      'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                    0x0A, 0x01, 0x01,
                    0x0A, 0x01, 0x03,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x01, 0x01, (byte)0xFF,
                    (byte)0xA9, 0x00,
                    0x30, 0x02,                     // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x00
        };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a SearchRequest message
        try 
        {
            ldapDecoder.decode(stream, ldapMessageContainer);
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
     * Test the decoding of a SearchRequest with an extensible match and
     * an empty matching rule
     */
    public void testDecodeSearchRequestExtensibleMatchEmptyMatchingRule() 
    {
        byte[] asn1BER = new byte[] 
        { 
                0x30, 0x3D, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x38, 
                    0x04, 0x1F,                   //    baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                      'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                    0x0A, 0x01, 0x01,
                    0x0A, 0x01, 0x03,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x01, 0x01, (byte)0xFF,
                    (byte)0xA9, 0x02,
                      (byte)0x81, 0x00,
                    0x30, 0x02,                     // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x00
        };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a SearchRequest message
        try 
        {
            ldapDecoder.decode(stream, ldapMessageContainer);
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
     * Test the decoding of a SearchRequest with an extensible match and
     * an empty type
     */
    public void testDecodeSearchRequestExtensibleMatchEmptyType() 
    {
        byte[] asn1BER = new byte[] 
        { 
                0x30, 0x3D, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x38, 
                    0x04, 0x1F,                   //    baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                      'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                    0x0A, 0x01, 0x01,
                    0x0A, 0x01, 0x03,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x01, 0x01, (byte)0xFF,
                    (byte)0xA9, 0x02,
                      (byte)0x82, 0x00,
                    0x30, 0x02,                     // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x00
        };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a SearchRequest message
        try 
        {
            ldapDecoder.decode(stream, ldapMessageContainer);
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
     * Test the decoding of a SearchRequest with an extensible match and
     * an empty matchValue
     */
    public void testDecodeSearchRequestExtensibleMatchEmptyMatchValue() 
    {
        byte[] asn1BER = new byte[] 
        { 
                0x30, 0x43, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x3E, 
                    0x04, 0x1F,                   //    baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                      'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                    0x0A, 0x01, 0x01,
                    0x0A, 0x01, 0x03,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x01, 0x01, (byte)0xFF,
                    (byte)0xA9, 0x08,
                        (byte)0x81, 0x04, 't', 'e', 's', 't',
                        (byte)0x83, 0x00,
                    0x30, 0x02,                     // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x00
        };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a SearchRequest message
        try 
        {
            ldapDecoder.decode(stream, ldapMessageContainer);
        } 
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr      = message.getSearchRequest();

        assertEquals( 4, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // Extended
        ExtensibleMatchFilter extensibleMatchFilter = (ExtensibleMatchFilter)sr.getFilter();
        assertNotNull( extensibleMatchFilter );
        
        assertEquals( "test", extensibleMatchFilter.getMatchingRule().toString() );
        assertNull( extensibleMatchFilter.getType() );
        assertEquals( "", extensibleMatchFilter.getMatchValue().toString());
        assertFalse( extensibleMatchFilter.isDnAttributes() );
        
        Attributes attributes = sr.getAttributes();
        
        assertEquals( 1, attributes.size() );
        
        for (int i = 0; i < attributes.size(); i++) 
        {
            assertNotNull( attributes.get( "*" ) );
        }
    }

    /**
     * Test the decoding of a SearchRequest with an extensible match and
     * an matching rule and an empty type
     */
    public void testDecodeSearchRequestExtensibleMatchMatchingRuleEmptyType() 
    {
        byte[] asn1BER = new byte[] 
        { 
                0x30, 0x43, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x3E, 
                    0x04, 0x1F,                   //    baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                      'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                    0x0A, 0x01, 0x01,
                    0x0A, 0x01, 0x03,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x01, 0x01, (byte)0xFF,
                    (byte)0xA9, 0x08,
                        (byte)0x81, 0x04, 't', 'e', 's', 't',
                        (byte)0x82, 0x00,
                    0x30, 0x02,                     // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x00
        };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a SearchRequest message
        try 
        {
            ldapDecoder.decode(stream, ldapMessageContainer);
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
     * Test the decoding of a SearchRequest with an extensible match and
     * a matching rule and nothing else
     */
    public void testDecodeSearchRequestExtensibleMatchMatchingRuleAlone() 
    {
        byte[] asn1BER = new byte[] 
        { 
                0x30, 0x41, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x3C, 
                    0x04, 0x1F,                   //    baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                      'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                    0x0A, 0x01, 0x01,
                    0x0A, 0x01, 0x03,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x01, 0x01, (byte)0xFF,
                    (byte)0xA9, 0x06,
                        (byte)0x81, 0x04, 't', 'e', 's', 't',
                    0x30, 0x02,                     // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x00
        };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a SearchRequest message
        try 
        {
            ldapDecoder.decode(stream, ldapMessageContainer);
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
     * Test the decoding of a SearchRequest with an extensible match and
     * a type and nothing else
     */
    public void testDecodeSearchRequestExtensibleMatchTypeAlone() 
    {
        byte[] asn1BER = new byte[] 
        { 
                0x30, 0x43, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x3E, 
                    0x04, 0x1F,                   //    baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                      'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                    0x0A, 0x01, 0x01,
                    0x0A, 0x01, 0x03,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x01, 0x01, (byte)0xFF,
                    (byte)0xA9, 0x06,
                        (byte)0x82, 0x04, 't', 'e', 's', 't',
                    0x30, 0x02,                     // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x00
        };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a SearchRequest message
        try 
        {
            ldapDecoder.decode(stream, ldapMessageContainer);
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
     * Test the decoding of a SearchRequest with an extensible match and
     * a match Value and nothing else
     */
    public void testDecodeSearchRequestExtensibleMatchMatchValueAlone() 
    {
        byte[] asn1BER = new byte[] 
        { 
                0x30, 0x43, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x3E, 
                    0x04, 0x1F,                   //    baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                      'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                    0x0A, 0x01, 0x01,
                    0x0A, 0x01, 0x03,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x01, 0x01, (byte)0xFF,
                    (byte)0xA9, 0x06,
                        (byte)0x83, 0x04, 't', 'e', 's', 't',
                    0x30, 0x02,                     // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x00
        };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode a SearchRequest message
        try 
        {
            ldapDecoder.decode(stream, ldapMessageContainer);
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
