/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.codec.search;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.search.ExtensibleMatchFilter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * A test case for SearchRequest messages
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestMatchingRuleAssertionTest
{
    static Map<String, OidNormalizer> oids = new HashMap<String, OidNormalizer>();

    @BeforeClass
    public static void setUp() throws Exception
    {
        oids.put( "dc", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "domaincomponent", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "objectclass", new OidNormalizer( "objectclass", new DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.0", new OidNormalizer( "objectclass", new DeepTrimToLowerNormalizer() ) );
    }


    /**
     * Tests an search request decode with a simple equality match filter.
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatch()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x63 );
        stream.put( new byte[]
            { 
            0x30, 0x61,                     // LDAPMessage ::= SEQUENCE {
              0x02, 0x01, 0x01,             //   messageID       MessageID,
              0x63, 0x5C,                   //   protocolOp      CHOICE {
                                            //     searchRequest   SearchRequest,
                                            //
                                            // SearchRequest ::= [APPLICATION 3] SEQUENCE {
                0x04, 0x11,                 //   baseObject      LDAPDN, (dc=example,dc=com)
                  'd', 'c', '=', 'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                                            //   scope           ENUMERATED {
                0x0A, 0x01, 0x00,           //      baseObject              (0), ...
                                            //   derefAliases    ENUMERATED {
                0x0A, 0x01, 0x02,           //     derefFindingBaseObj     (2),...
                0x02, 0x01, 0x02,           //   sizeLimit       INTEGER (0 .. maxInt), (2)
                0x02, 0x01, 0x03,           //   timeLimit       INTEGER (0 .. maxInt), (3)
                0x01, 0x01, ( byte ) 0xFF,  //   typesOnly       BOOLEAN, (true)
                ( byte ) 0xA9, 0x21,        //   filter          Filter,
                                            //
                                            // Filter ::= CHOICE {
                                            //   extensibleMatch [9] MatchingRuleAssertion }
                                            //
                                            // MatchingRuleAssertion ::= SEQUENCE {
                  ( byte ) 0x81, 0x02,      //   matchingRule    [1] MatchingRuleId OPTIONAL,
                    'c', 'n', 
                  ( byte ) 0x82, 0x13,      //    type            [2] AttributeDescription OPTIONAL,
                    '1', '.', '2', '.', '8', '4', '0', '.', '4', '8', '0', '1', '8', '.', '1', '.', '2', '.', '2', 
                  ( byte ) 0x83, 0x03,      //    matchValue      [3] AssertionValue,
                    'a', 'o', 'k', 
                                            //    dnAttributes    [4] BOOLEAN DEFAULT FALSE  }
                  ( byte ) 0x84, 0x01, ( byte ) 0xFF, 
                0x30, 0x15,                 // attributes      AttributeDescriptionList }
                  0x04, 0x05, 'a', 't', 't', 'r', '0',
                  0x04, 0x05, 'a', 't', 't', 'r', '1',
                  0x04, 0x05, 'a', 't', 't', 'r', '2'
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.OBJECT, sr.getScope() );
        assertEquals( LdapConstants.DEREF_FINDING_BASE_OBJ, sr.getDerefAliases() );
        assertEquals( 2, sr.getSizeLimit() );
        assertEquals( 3, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes  )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x63, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x56 ), decodedPdu.substring( 0, 0x56 ) );
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
    @Test
    public void testDecodeSearchRequestEmptyExtensibleMatch()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x3B, 
              0x02, 0x01, 0x04,                 // messageID
                0x63, 0x36,                     // baseObject LDAPDN,
                  0x04, 0x1F, 
                    'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                    'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                  0x0A, 0x01, 0x01, 
                  0x0A, 0x01, 0x03, 
                  0x02, 0x01, 0x00, 
                  0x02, 0x01, 0x00, 
                  0x01, 0x01, ( byte ) 0xFF, 
                  ( byte ) 0xA9, 0x00,             
                  0x30, 0x02, // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with an extensible match and an
     * empty matching rule
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatchEmptyMatchingRule()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x3D, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x38, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA9, 0x02, 
                  ( byte ) 0x81, 0x00,          // matchingRule    [1] MatchingRuleId OPTIONAL,
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with an extensible match and an
     * empty type
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatchEmptyType()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x3D, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x38, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA9, 0x02, 
                  ( byte ) 0x82, 0x00,          //    type            [2] AttributeDescription OPTIONAL
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with an extensible match and an
     * empty matchValue
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatchEmptyMatchValue()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x43, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x3E, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                  0x0A, 0x01, 0x01, 
                  0x0A, 0x01, 0x03, 
                  0x02, 0x01, 0x00, 
                  0x02, 0x01, 0x00, 
                  0x01, 0x01, ( byte ) 0xFF, 
                  ( byte ) 0xA9, 0x08, 
                    ( byte ) 0x81, 0x04, 't', 'e', 's', 't', 
                    ( byte ) 0x83, 0x00,        //    matchValue      [3] AssertionValue,
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 4, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // Extended
        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) sr.getFilter();
        assertNotNull( extensibleMatchFilter );

        assertEquals( "test", extensibleMatchFilter.getMatchingRule() );
        assertNull( extensibleMatchFilter.getType() );
        assertEquals( "", extensibleMatchFilter.getMatchValue().toString() );
        assertFalse( extensibleMatchFilter.isDnAttributes() );

        List<EntryAttribute> attributes = sr.getAttributes();

        assertEquals( 0, attributes.size() );
    }


    /**
     * Test the decoding of a SearchRequest with an extensible match and an
     * matching rule and an empty type
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatchMatchingRuleEmptyType()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x43, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x3E, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA9, 0x08, 
                  ( byte ) 0x81, 0x04, 't', 'e', 's', 't', 
                  ( byte ) 0x82, 0x00,          //    type            [2] AttributeDescription OPTIONAL,
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a SearchRequest with an extensible match and an
     * matching rule and an empty dnAttributes
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatchDnAttributesEmptyType()
    {
        byte[] asn1BER = new byte[]
            { 
             0x30, 0x60,                     // LDAPMessage ::= SEQUENCE {
               0x02, 0x01, 0x01,             //   messageID       MessageID,
               0x63, 0x5B,                   //   protocolOp      CHOICE {
                                             //     searchRequest   SearchRequest,
                                             //
                                             // SearchRequest ::= [APPLICATION 3] SEQUENCE {
                 0x04, 0x11,                 //   baseObject      LDAPDN, (dc=example,dc=com)
                   'd', 'c', '=', 'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                                             //   scope           ENUMERATED {
                 0x0A, 0x01, 0x00,           //      baseObject              (0), ...
                                             //   derefAliases    ENUMERATED {
                 0x0A, 0x01, 0x02,           //     derefFindingBaseObj     (2),...
                 0x02, 0x01, 0x02,           //   sizeLimit       INTEGER (0 .. maxInt), (2)
                 0x02, 0x01, 0x03,           //   timeLimit       INTEGER (0 .. maxInt), (3)
                 0x01, 0x01, ( byte ) 0xFF,  //   typesOnly       BOOLEAN, (true)
                 ( byte ) 0xA9, 0x20,        //   filter          Filter,
                                             //
                                             // Filter ::= CHOICE {
                                             //   extensibleMatch [9] MatchingRuleAssertion }
                                             //
                                             // MatchingRuleAssertion ::= SEQUENCE {
                   ( byte ) 0x81, 0x02,      //   matchingRule    [1] MatchingRuleId OPTIONAL,
                     'c', 'n', 
                   ( byte ) 0x82, 0x13,      //    type            [2] AttributeDescription OPTIONAL,
                     '1', '.', '2', '.', '8', '4', '0', '.', '4', '8', '0', '1', '8', '.', '1', '.', '2', '.', '2', 
                   ( byte ) 0x83, 0x03,      //    matchValue      [3] AssertionValue,
                     'a', 'o', 'k', 
                                             //    dnAttributes    [4] BOOLEAN DEFAULT FALSE  }
                   ( byte ) 0x84, 0x00, 
                 0x30, 0x15,                 // attributes      AttributeDescriptionList }
                   0x04, 0x05, 'a', 't', 't', 'r', '0',
                   0x04, 0x05, 'a', 't', 't', 'r', '1',
                   0x04, 0x05, 'a', 't', 't', 'r', '2'
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with an extensible match and a
     * matching rule and nothing else
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatchMatchingRuleAlone()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x41, 
              0x02, 0x01, 0x04, // messageID
              0x63, 0x3C, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
              0x0A, 0x01, 0x01, 
              0x0A, 0x01, 0x03, 
              0x02, 0x01, 0x00, 
              0x02, 0x01, 0x00, 
              0x01, 0x01, ( byte ) 0xFF, 
              ( byte ) 0xA9, 0x06, 
                ( byte ) 0x81, 0x04, 't', 'e', 's', 't', 
              0x30, 0x02, // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with an extensible match and a type
     * and nothing else
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatchTypeAlone()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x43, 
              0x02, 0x01, 0x04, // messageID
              0x63, 0x3E, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
              0x0A, 0x01, 0x01, 
              0x0A, 0x01, 0x03, 
              0x02, 0x01, 0x00, 
              0x02, 0x01, 0x00, 
              0x01, 0x01, ( byte ) 0xFF, 
              ( byte ) 0xA9, 0x06, 
                ( byte ) 0x82, 0x04, 't', 'e', 's', 't', 
              0x30, 0x02, // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
            return;
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with an extensible match and a match
     * Value and nothing else
     */
    @Test
    public void testDecodeSearchRequestExtensibleMatchMatchValueAlone()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x43, 
              0x02, 0x01, 0x04, // messageID
              0x63, 0x3E, 0x04, 
                0x1F, // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA9, 0x06, 
                  ( byte ) 0x83, 0x04, 't', 'e', 's', 't', 
                0x30, 0x02, // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 4, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // Extended
        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) sr.getFilter();
        assertNotNull( extensibleMatchFilter );

        assertNull( extensibleMatchFilter.getMatchingRule() );
        assertNull( extensibleMatchFilter.getType() );
        assertEquals( "test", extensibleMatchFilter.getMatchValue() );
        assertFalse( extensibleMatchFilter.isDnAttributes() );

        List<EntryAttribute> attributes = sr.getAttributes();

        assertEquals( 0, attributes.size() );
    }
}
