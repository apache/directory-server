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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.AttributeValueAssertion;
import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.ResponseCarryingException;
import org.apache.directory.shared.ldap.codec.search.AndFilter;
import org.apache.directory.shared.ldap.codec.search.AttributeValueAssertionFilter;
import org.apache.directory.shared.ldap.codec.search.NotFilter;
import org.apache.directory.shared.ldap.codec.search.OrFilter;
import org.apache.directory.shared.ldap.codec.search.PresentFilter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.controls.subEntry.SubEntryControlCodec;
import org.apache.directory.shared.ldap.message.InternalMessage;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.SearchResponseDoneImpl;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * A test case for SearchRequest messages
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestTest
{
    static Map<String, OidNormalizer> oids = new HashMap<String, OidNormalizer>(); 

    @Before
    public void setUp() throws Exception
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
     * Test the decoding of a SearchRequest with no controls. The search filter
     * is : (&(|(objectclass=top)(ou=contacts))(!(objectclass=ttt)))
     */
    @Test
    public void testDecodeSearchRequestGlobalNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x90 );
        stream.put( new byte[]
            { 
            0x30, ( byte ) 0x81, ( byte ) 0x8D,         // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,                         // messageID MessageID
              0x63, ( byte ) 0x81, ( byte ) 0x87,       // CHOICE { ...,
                                                        // searchRequest SearchRequest, ...
                                                        // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                             // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01,                       // scope ENUMERATED {
                                                        // baseObject (0),
                                                        // singleLevel (1),
                                                        // wholeSubtree (2) },
                0x0A, 0x01, 0x03,                       // derefAliases ENUMERATED {
                                                        // neverDerefAliases (0),
                                                        // derefInSearching (1),
                                                        // derefFindingBaseObj (2),
                                                        // derefAlways (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF,              // typesOnly  BOOLEAN, (TRUE)
                                                        // filter Filter,
                ( byte ) 0xA0, 0x3C,                    // Filter ::= CHOICE {
                                                        // and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x24,                  // or [1] SET of Filter,
                    ( byte ) 0xA3, 0x12,                // equalityMatch [3]
                                                        // AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 'o', 'p', 
                    ( byte ) 0xA3, 0x0E,                // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x02, 'o', 'u',             // attributeDesc AttributeDescription (LDAPString),
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's', 
                    ( byte ) 0xA2, 0x14,                // not [2] Filter,
                      ( byte ) 0xA3, 0x12,              // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 't', 't',
                                                        // attributes AttributeDescriptionList }
                0x30, 0x15,                             // AttributeDescriptionList ::= SEQUENCE OF
                                                        // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'     // AttributeDescription ::= LDAPString
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (& (...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();

        // (& (| (...
        assertEquals( 2, andFilters.size() );
        OrFilter orFilter = ( OrFilter ) andFilters.get( 0 );
        assertNotNull( orFilter );

        // (& (| (obectclass=top) (...
        List<Filter> orFilters = orFilter.getOrFilter();
        assertEquals( 2, orFilters.size() );
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectclass", assertion.getAttributeDesc() );
        assertEquals( "top", assertion.getAssertionValue().get() );

        // (& (| (objectclass=top) (ou=contacts) ) (...
        equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "ou", assertion.getAttributeDesc() );
        assertEquals( "contacts", assertion.getAssertionValue().get() );

        // (& (| (objectclass=top) (ou=contacts) ) (! ...
        NotFilter notFilter = ( NotFilter ) andFilters.get( 1 );
        assertNotNull( notFilter );

        // (& (| (objectclass=top) (ou=contacts) ) (! (objectclass=ttt) ) )
        equalityMatch = ( AttributeValueAssertionFilter ) notFilter.getNotFilter();
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectclass", assertion.getAttributeDesc() );
        assertEquals( "ttt", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x81 ), decodedPdu.substring( 0, 0x81 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with no controls. Test the various
     * types of filter : >=, <=, ~= The search filter is :
     * (&(|(objectclass~=top)(ou<=contacts))(!(objectclass>=ttt)))
     */
    @Test
    public void testDecodeSearchRequestCompareFiltersNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x90 );
        stream.put( new byte[]
            { 
            0x30, ( byte ) 0x81, ( byte ) 0x8D,             // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,                             //     messageID MessageID
              0x63, ( byte ) 0x81, ( byte ) 0x87,           //     CHOICE { ...,
                                                            //         searchRequest SearchRequest, ...
                                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                                 //     baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01,                           //     scope ENUMERATED {
                                                            //         baseObject   (0),
                                                            //         singleLevel  (1),
                                                            //         wholeSubtree (2) },
                0x0A, 0x01, 0x03,                           //     derefAliases ENUMERATED {
                                                            //         neverDerefAliases (0),
                                                            //         derefInSearching (1),
                                                            //         derefFindingBaseObj (2),
                                                            //         derefAlways (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8,            //     sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,            //     timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF,                  //     typesOnly BOOLEAN, (TRUE)
                                                            //     filter Filter,
                ( byte ) 0xA0, 0x3C,                        // Filter ::= CHOICE {
                                                            //      and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x24,                      //      or [1] SET of Filter,
                    ( byte ) 0xA8, 0x12,                    //      approxMatch [8]
                                                            // AttributeValueAssertion,
                                                            // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x0B,                           // attributeDesc AttributeDescription (LDAPString),
                        'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                      0x04, 0x03,                           // attributeDesc AttributeDescription (LDAPString), 
                        't', 'o', 'p', 
                    ( byte ) 0xA6, 0x0E,                    // lessOrEqual [3] AttributeValueAssertion,
                      0x04, 0x02,                           // AttributeValueAssertion ::= SEQUENCE {
                        'o', 'u',                           // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x08,                           // assertionValue AssertionValue (OCTET STRING) } 
                        'c', 'o', 'n', 't', 'a', 'c', 't', 's', 
                  ( byte ) 0xA2, 0x14,                      // not [2] Filter,
                    ( byte ) 0xA5, 0x12,                    // greaterOrEqual [5] AttributeValueAssertion,
                                                            // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x0B,                           // attributeDesc AttributeDescription (LDAPString), 
                        'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                      0x04, 0x03, 't', 't', 't',            // assertionValue AssertionValue (OCTET STRING) }
                                                            // attributes AttributeDescriptionList }
                0x30, 0x15,                                 // AttributeDescriptionList ::= SEQUENCE OF
                                                            // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0',        // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1',        // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'         // AttributeDescription ::= LDAPString
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (& (...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();

        // (& (| (...
        assertEquals( 2, andFilters.size() );
        OrFilter orFilter = ( OrFilter ) andFilters.get( 0 );
        assertNotNull( orFilter );

        // (& (| (objectclass~=top) (...
        List<Filter> orFilters = orFilter.getOrFilter();
        assertEquals( 2, orFilters.size() );
        AttributeValueAssertionFilter approxMatch = ( AttributeValueAssertionFilter ) orFilters.get( 0 );
        assertNotNull( approxMatch );

        AttributeValueAssertion assertion = approxMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectclass", assertion.getAttributeDesc() );
        assertEquals( "top", assertion.getAssertionValue().get() );

        // (& (| (objectclass~=top) (ou<=contacts) ) (...
        AttributeValueAssertionFilter lessOrEqual = ( AttributeValueAssertionFilter ) orFilters.get( 1 );
        assertNotNull( lessOrEqual );

        assertion = lessOrEqual.getAssertion();
        assertNotNull( assertion );

        assertEquals( "ou", assertion.getAttributeDesc() );
        assertEquals( "contacts", assertion.getAssertionValue().get() );

        // (& (| (objectclass~=top) (ou<=contacts) ) (! ...
        NotFilter notFilter = ( NotFilter ) andFilters.get( 1 );
        assertNotNull( notFilter );

        // (& (| (objectclass~=top) (ou<=contacts) ) (! (objectclass>=ttt) ) )
        AttributeValueAssertionFilter greaterOrEqual = ( AttributeValueAssertionFilter ) notFilter.getNotFilter();
        assertNotNull( greaterOrEqual );

        assertion = greaterOrEqual.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectclass", assertion.getAttributeDesc() );
        assertEquals( "ttt", assertion.getAssertionValue().get() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x90, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x81 ), decodedPdu.substring( 0, 0x81 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with no controls. Test the present
     * filter : =* The search filter is :
     * (&(|(objectclass=*)(ou=*))(!(objectclass>=ttt)))
     */
    @Test
    public void testDecodeSearchRequestPresentNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x7B );
        stream.put( new byte[]
            { 0x30, 0x79,         // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x74,       // CHOICE { ..., searchRequest SearchRequest, ...
                                  // SearchRequest ::= APPLICATION[3] SEQUENCE {
                  0x04, 0x1F,     // baseObject LDAPDN,
                    'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 
                    'a', 's', 'u', 'l', 'u', ',', 'd', 'c', 
                    '=', 'e', 'x', 'a', 'm', 'p', 'l', 'e', 
                    ',', 'd', 'c', '=', 'c', 'o', 'm', 
                  0x0A, 0x01, 0x01, // scope
                                    // ENUMERATED
                                    // {
                                    // baseObject (0),
                                    // singleLevel (1),
                                    // wholeSubtree (2) },
                  0x0A, 0x01, 0x03, // derefAliases ENUMERATED {
                                    // neverDerefAliases (0),
                                    // derefInSearching (1),
                                    // derefFindingBaseObj (2),
                                    // derefAlways (3) },
                                    // sizeLimit INTEGER (0 .. maxInt), (1000)
                  0x02, 0x02, 0x03, ( byte ) 0xE8,
                                    // timeLimit INTEGER (0 .. maxInt), (1000)
                  0x02, 0x02, 0x03, ( byte ) 0xE8, 
                  0x01, 0x01, ( byte ) 0xFF, // typesOnly
                                    // BOOLEAN,
                                    // (TRUE)
                                    // filter Filter,
                  ( byte ) 0xA0, 0x29, // Filter ::= CHOICE {
                                    // and [0] SET OF Filter,
                    ( byte ) 0xA1, 0x11, // or [1] SET of Filter,
                      ( byte ) 0x87, 0x0B, // present [7] AttributeDescription,
                                    // AttributeDescription ::= LDAPString
                        'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                    // assertionValue AssertionValue (OCTET STRING) }
                      ( byte ) 0x87, 0x02, 'o', 'u', // present [7]
                                    // AttributeDescription,
                                    // AttributeDescription ::= LDAPString
                    ( byte ) 0xA2, 0x14, // not [2] Filter,
                      ( byte ) 0xA5, 0x12, // greaterOrEqual [5]
                                    // AttributeValueAssertion,
                                    // AttributeValueAssertion ::= SEQUENCE {
                                    // attributeDesc AttributeDescription (LDAPString),
                        0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                    // assertionValue AssertionValue (OCTET STRING) }
                        0x04, 0x03, 't', 't', 't',
                                    // attributes AttributeDescriptionList }
                0x30, 0x15, // AttributeDescriptionList ::= SEQUENCE OF
                            // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription
                                                        // ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription
                                                        // ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2' // AttributeDescription ::=
                                                    // LDAPString
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (& (...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();

        // (& (| (...
        assertEquals( 2, andFilters.size() );
        OrFilter orFilter = ( OrFilter ) andFilters.get( 0 );
        assertNotNull( orFilter );

        // (& (| (objectclass=*) (...
        List<Filter> orFilters = orFilter.getOrFilter();
        assertEquals( 2, orFilters.size() );

        PresentFilter presentFilter = ( PresentFilter ) orFilters.get( 0 );
        assertNotNull( presentFilter );

        assertEquals( "objectclass", presentFilter.getAttributeDescription() );

        // (& (| (objectclass=*) (ou=*) ) (...
        presentFilter = ( PresentFilter ) orFilters.get( 1 );
        assertNotNull( presentFilter );

        assertEquals( "ou", presentFilter.getAttributeDescription() );

        // (& (| (objectclass=*) (ou=*) ) (! ...
        NotFilter notFilter = ( NotFilter ) andFilters.get( 1 );
        assertNotNull( notFilter );

        // (& (| (objectclass=*) (ou=*) ) (! (objectclass>=ttt) ) )
        AttributeValueAssertionFilter greaterOrEqual = ( AttributeValueAssertionFilter ) notFilter.getNotFilter();
        assertNotNull( greaterOrEqual );

        AttributeValueAssertion assertion = greaterOrEqual.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectclass", assertion.getAttributeDesc() );
        assertEquals( "ttt", assertion.getAssertionValue().get() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x7B, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x6C ), decodedPdu.substring( 0, 0x6C ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with no attributes. The search
     * filter is : (objectclass=*)
     */
    @Test
    public void testDecodeSearchRequestNoAttributes()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x40 );
        stream.put( new byte[]
            { 0x30, 0x37, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x03, // messageID MessageID
                0x63, 0x32, // CHOICE { ..., searchRequest SearchRequest, ...
                // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x12, // baseObject LDAPDN,
                'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm', 0x0A, 0x01,
                0x00, // scope ENUMERATED {
                // baseObject (0),
                // singleLevel (1),
                // wholeSubtree (2) },
                0x0A, 0x01, 0x03, // derefAliases ENUMERATED {
                // neverDerefAliases (0),
                // derefInSearching (1),
                // derefFindingBaseObj (2),
                // derefAlways (3) },
                // sizeLimit INTEGER (0 .. maxInt), (infinite)
                0x02, 0x01, 0x00,
                // timeLimit INTEGER (0 .. maxInt), (infinite)
                0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0x00, // typesOnly
                                                                // BOOLEAN,
                                                                // (FALSE)
                // filter Filter,
                // Filter ::= CHOICE {
                ( byte ) 0x87, 0x0B, // present [7] AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'C', 'l', 'a', 's', 's',
                // attributes AttributeDescriptionList }
                0x30, 0x00, // AttributeDescriptionList ::= SEQUENCE OF
                            // AttributeDescription
                0x00, 0x00, // Some trailing 00, useless.
                0x00, 0x00, 0x00, 0x00 } );

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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 3, message.getMessageId() );
        assertEquals( "ou=users,ou=system", sr.getBaseObject().toString() );
        assertEquals( SearchScope.OBJECT, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );

        // (objectClass = *)
        PresentFilter presentFilter = ( PresentFilter ) sr.getFilter();
        assertNotNull( presentFilter );
        assertEquals( "objectClass", presentFilter.getAttributeDescription() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        assertEquals( 0, attributes.size() );

        // Check the length
        assertEquals( 0x39, message.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu.substring( 0, decodedPdu.length() - 35 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with an empty attribute. The search
     * filter is : (objectclass=*)
     */
    @Test
    public void testDecodeSearchRequestOneEmptyAttribute()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x3F );
        stream.put( new byte[]
            { 0x30, 0x3D,                // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x03,        // messageID MessageID
                0x63, 0x38,              // CHOICE { ..., searchRequest SearchRequest, ...
                                         // SearchRequest ::= APPLICATION[3] SEQUENCE {
                  0x04, 0x12,            // baseObject LDAPDN,
                    'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm', 
                  0x0A, 0x01, 0x00,      // scope ENUMERATED {
                                         // baseObject (0),
                                         // singleLevel (1),
                                         // wholeSubtree (2) },
                  0x0A, 0x01, 0x03,      // derefAliases ENUMERATED {
                                         // neverDerefAliases (0),
                                         // derefInSearching (1),
                                         // derefFindingBaseObj (2),
                                         // derefAlways (3) },
                                         // sizeLimit INTEGER (0 .. maxInt), (infinite)
                  0x02, 0x01, 0x00,      // timeLimit INTEGER (0 .. maxInt), (infinite)
                  0x02, 0x01, 0x00, 
                  0x01, 0x01,0x00,       // typesOnly
                                         // BOOLEAN,
                                         // (FALSE)
                                         // filter Filter,
                                         // Filter ::= CHOICE {
                  ( byte ) 0x87, 0x0B,   // present [7] AttributeDescription,
                    'o', 'b', 'j', 'e', 'c', 't', 'C', 'l', 'a', 's', 's',
                                         // attributes AttributeDescriptionList }
                  0x30, 0x06,            // AttributeDescriptionList ::= SEQUENCE OF
                                         // AttributeDescription
                    0x04, 0x02,          // Request for sn
                      's', 'n', 
                    0x04, 0x00           // Empty attribute
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
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 3, message.getMessageId() );
        assertEquals( "ou=users,ou=system", sr.getBaseObject().toString() );
        assertEquals( SearchScope.OBJECT, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );

        // (objectClass = *)
        PresentFilter presentFilter = ( PresentFilter ) sr.getFilter();
        assertNotNull( presentFilter );
        assertEquals( "objectClass", presentFilter.getAttributeDescription() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        assertEquals( 1, attributes.size() );
    }

    /**
     * Test the decoding of a SearchRequest with a star and an attribute. The search
     * filter is : (objectclass=*)
     */
    @Test
    public void testDecodeSearchRequestWithStarAndAttr()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x40 );
        stream.put( new byte[]
            { 0x30, 0x3E,                // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x03,        // messageID MessageID
                0x63, 0x39,              // CHOICE { ..., searchRequest SearchRequest, ...
                                         // SearchRequest ::= APPLICATION[3] SEQUENCE {
                  0x04, 0x12,            // baseObject LDAPDN,
                    'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',', 'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm', 
                  0x0A, 0x01, 0x00,      // scope ENUMERATED {
                                         // baseObject (0),
                                         // singleLevel (1),
                                         // wholeSubtree (2) },
                  0x0A, 0x01, 0x03,      // derefAliases ENUMERATED {
                                         // neverDerefAliases (0),
                                         // derefInSearching (1),
                                         // derefFindingBaseObj (2),
                                         // derefAlways (3) },
                                         // sizeLimit INTEGER (0 .. maxInt), (infinite)
                  0x02, 0x01, 0x00,      // timeLimit INTEGER (0 .. maxInt), (infinite)
                  0x02, 0x01, 0x00, 
                  0x01, 0x01,0x00,       // typesOnly
                                         // BOOLEAN,
                                         // (FALSE)
                                         // filter Filter,
                                         // Filter ::= CHOICE {
                  ( byte ) 0x87, 0x0B,   // present [7] AttributeDescription,
                    'o', 'b', 'j', 'e', 'c', 't', 'C', 'l', 'a', 's', 's',
                                         // attributes AttributeDescriptionList }
                  0x30, 0x07,            // AttributeDescriptionList ::= SEQUENCE OF
                                         // AttributeDescription
                    0x04, 0x02,          // Request for sn
                      's', 'n', 
                    0x04, 0x01, '*'      // * attribute
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
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 3, message.getMessageId() );
        assertEquals( "ou=users,ou=system", sr.getBaseObject().toString() );
        assertEquals( SearchScope.OBJECT, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );

        // (objectClass = *)
        PresentFilter presentFilter = ( PresentFilter ) sr.getFilter();
        assertNotNull( presentFilter );
        assertEquals( "objectClass", presentFilter.getAttributeDescription() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        assertEquals( 2, attributes.size() );
        Set<String> expectedAttrs = new HashSet<String>();
        expectedAttrs.add( "sn" );
        expectedAttrs.add( "*" );
        
        for ( EntryAttribute attribute:attributes )
        {
            assertTrue( expectedAttrs.contains( attribute.getId() ) );
            expectedAttrs.remove( attribute.getId() );
        }
        
        assertEquals( 0, expectedAttrs.size() );
    }        


    /**
     * Tests an search request decode with a simple equality match filter.
     */
    @Test
    public void testDecodeSearchRequestOrFilters()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x96 );
        stream.put( new byte[]
            { 
                0x30, (byte)0x81, (byte)0x93, 
                0x02, 0x01, 0x21, 
                0x63, (byte)0x81, (byte)0x8D, // "dc=example,dc=com"
                  0x04, 0x11, 
                    'd', 'c', '=', 'e', 'x', 'a', 'm', 'p',
                    'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                  0x0A, 0x01, 0x00, 
                  0x0A, 0x01, 0x02, 
                  0x02, 0x01, 0x02, 
                  0x02, 0x01, 0x03, 
                  0x01, 0x01, (byte)0xFF, 
                  (byte)0xA1, 0x52, // ( |
                    (byte)0xA3, 0x10, // ( uid=akarasulu )
                      0x04, 0x03, 
                        'u', 'i', 'd',
                      0x04, 0x09,   
                        'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u',
                    (byte)0xA3, 0x09, // ( cn=aok )
                      0x04, 0x02, 
                        'c', 'n', 
                      0x04, 0x03, 
                        'a', 'o', 'k', 
                    (byte)0xA3, 0x15, // ( ou=Human Resources )
                      0x04, 0x02, 
                        'o', 'u', 
                      0x04, 0x0F, 
                        'H', 'u', 'm', 'a', 'n', ' ', 'R', 'e', 
                        's', 'o', 'u', 'r', 'c', 'e', 's',
                    (byte)0xA3, 0x10, 
                      0x04, 0x01, 
                        'l', // (l=Santa Clara )
                      0x04, 0x0B, 
                        'S', 'a', 'n', 't', 'a', ' ', 'C', 'l', 'a', 'r', 'a',
                    (byte)0xA3, 0x0A, // ( cn=abok ))
                      0x04, 0x02, 
                        'c', 'n', 
                      0x04, 0x04, 
                        'a', 'b', 'o', 'k', 
                  0x30, 0x15, // Attributes
                    0x04, 0x05, 
                      'a', 't', 't', 'r', '0',  // attr0
                    0x04, 0x05, 
                      'a', 't', 't', 'r', '1',  // attr1
                    0x04, 0x05, 
                      'a', 't', 't', 'r', '2'   // attr2
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 33, message.getMessageId() );
        assertEquals( "dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.OBJECT, sr.getScope() );
        assertEquals( LdapConstants.DEREF_FINDING_BASE_OBJ, sr.getDerefAliases() );
        assertEquals( 2, sr.getSizeLimit() );
        assertEquals( 3, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        OrFilter orFilter = ( OrFilter ) sr.getFilter();
        assertNotNull( orFilter );
        assertEquals( 5, orFilter.getFilterSet().size() );

        // uid=akarasulu
        AttributeValueAssertion assertion = ( ( AttributeValueAssertionFilter ) orFilter.getOrFilter().get( 0 ) )
            .getAssertion();

        assertEquals( "uid", assertion.getAttributeDesc() );
        assertEquals( "akarasulu", assertion.getAssertionValue().get() );

        // cn=aok
        assertion = ( ( AttributeValueAssertionFilter ) orFilter.getOrFilter().get( 1 ) ).getAssertion();

        assertEquals( "cn", assertion.getAttributeDesc() );
        assertEquals( "aok", assertion.getAssertionValue().get() );

        // ou = Human Resources
        assertion = ( ( AttributeValueAssertionFilter ) orFilter.getOrFilter().get( 2 ) ).getAssertion();

        assertEquals( "ou", assertion.getAttributeDesc() );
        assertEquals( "Human Resources", assertion.getAssertionValue().get() );

        // l=Santa Clara
        assertion = ( ( AttributeValueAssertionFilter ) orFilter.getOrFilter().get( 3 ) ).getAssertion();

        assertEquals( "l", assertion.getAttributeDesc() );
        assertEquals( "Santa Clara", assertion.getAssertionValue().get() );

        // cn=abok
        assertion = ( ( AttributeValueAssertionFilter ) orFilter.getOrFilter().get( 4 ) ).getAssertion();

        assertEquals( "cn", assertion.getAttributeDesc() );
        assertEquals( "abok", assertion.getAssertionValue().get() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x96, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x87 ), decodedPdu.substring( 0, 0x87 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with controls.
     */
    @Test
    public void testDecodeSearchRequestWithControls()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x7f, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x33, 
                0x04, 0x13,                     // baseObject
                  'd', 'c', '=', 'm', 'y', '-', 'd', 'o', 'm', 'a', 'i', 
                  'n', ',', 'd', 'c', '=', 'c', 'o', 'm',
                0x0a, 0x01, 0x02,               // scope: subtree
                0x0a, 0x01, 0x03,               // derefAliases: derefAlways
                0x02, 0x01, 0x00,               // sizeLimit: 0
                0x02, 0x01, 0x00,               // timeLimit: 0
                0x01, 0x01, 0x00,               // typesOnly: false
                ( byte ) 0x87, 0x0b,            // Present filter: (objectClass=*)
                  'o', 'b', 'j', 'e', 'c', 't', 'C', 'l', 'a', 's', 's',
                  0x30, 0x00,                   // Attributes = '*'
                ( byte ) 0xa0, 0x45,            // controls
                  0x30, 0x28, 
                    0x04, 0x16,                 // control
                      '1', '.', '2', '.', '8', '4', '0', '.', '1', '1', '3', 
                      '5', '5', '6', '.', '1', '.', '4', '.', '3', '1', '9',
                    0x01, 0x01, ( byte ) 0xff, // criticality: false
                    0x04, 0x0b, 
                      0x30, 0x09, 
                        0x02, 0x01, 0x02, 
                        0x04, 0x04, 0x47, 0x00, 0x00, 0x00, // value: pageSize=2
                  0x30, 0x19, 
                    0x04, 0x17, // control
                      '2', '.', '1', '6', '.', '8', '4', '0', '.', '1', '.', '1',
                      '1', '3', '7', '3', '0', '.', '3', '.', '4', '.', '2',
            };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        assertEquals( 4, message.getMessageId() );
        assertEquals( 2, message.getControls().size() );

        // this is a constant in Java 5 API
        String pagedResultsControlOID = "1.2.840.113556.1.4.319";
        ControlCodec pagedResultsControl = message.getControls( 0 );
        assertEquals( pagedResultsControlOID, pagedResultsControl.getControlType() );
        assertTrue( pagedResultsControl.getCriticality() );

        // this is a constant in Java 5 API
        String manageReferralControlOID = "2.16.840.1.113730.3.4.2";
        ControlCodec manageReferralControl = message.getControls( 1 );
        assertEquals( manageReferralControlOID, manageReferralControl.getControlType() );

        SearchRequestCodec sr = message.getSearchRequest();
        assertEquals( "dc=my-domain,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.SUBTREE, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );

        assertTrue( sr.getFilter() instanceof PresentFilter );
        assertEquals( "objectClass", ( ( PresentFilter ) sr.getFilter() ).getAttributeDescription() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            String encodedPdu = StringTools.dumpBytes( bb.array() );
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with no controls but with oid
     * attributes. The search filter is :
     * (&(|(objectclass=top)(2.5.4.11=contacts))(!(organizationalUnitName=ttt)))
     */
    @Test
    public void testDecodeSearchRequestGlobalNoControlsOidAndAlias()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0xA1 );
        stream.put( new byte[]
            { 0x30, ( byte ) 0x81, ( byte ) 0x9E, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, ( byte ) 0x81, ( byte ) 0x98, // CHOICE { ...,
                                                    // searchRequest
                                                    // SearchRequest, ...
                // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, // scope
                                                                                            // ENUMERATED
                                                                                            // {
                // baseObject (0),
                // singleLevel (1),
                // wholeSubtree (2) },
                0x0A, 0x01, 0x03, // derefAliases ENUMERATED {
                // neverDerefAliases (0),
                // derefInSearching (1),
                // derefFindingBaseObj (2),
                // derefAlways (3) },
                // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,
                // timeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8, 0x01, 0x01, ( byte ) 0xFF, // typesOnly
                                                                            // BOOLEAN,
                                                                            // (TRUE)
                // filter Filter,
                ( byte ) 0xA0, 0x4D, // Filter ::= CHOICE {
                // and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x2A, // or [1] SET of Filter,
                    ( byte ) 0xA3, 0x12, // equalityMatch [3]
                                        // AttributeValueAssertion,
                // AttributeValueAssertion ::= SEQUENCE {
                // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 'o', 'p', 
                    ( byte ) 0xA3, 0x14, // equalityMatch
                                                                // [3]
                                                                // AttributeValueAssertion,
                // AttributeValueAssertion ::= SEQUENCE {
                0x04, 0x08, '2', '.', '5', '.', '4', '.', '1', '1', // attributeDesc
                                                                    // AttributeDescription
                                                                    // (LDAPString),
                // assertionValue AssertionValue (OCTET STRING) }
                0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's', ( byte ) 0xA2, 0x1F, // not
                                                                                            // [2]
                                                                                            // Filter,
                ( byte ) 0xA3, 0x1D, // equalityMatch [3]
                                        // AttributeValueAssertion,
                // AttributeValueAssertion ::= SEQUENCE {
                // attributeDesc AttributeDescription (LDAPString),
                0x04, 0x16, 'o', 'r', 'g', 'a', 'n', 'i', 'z', 'a', 't', 'i', 'o', 'n', 'a', 'l', 'U', 'n', 'i', 't',
                'N', 'a', 'm', 'e',
                // assertionValue AssertionValue (OCTET STRING) }
                0x04, 0x03, 't', 't', 't',
                // attributes AttributeDescriptionList }
                0x30, 0x15, // AttributeDescriptionList ::= SEQUENCE OF
                            // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription
                                                        // ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription
                                                        // ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2' // AttributeDescription ::=
                                                    // LDAPString
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
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (& (...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();

        // (& (| (...
        assertEquals( 2, andFilters.size() );
        OrFilter orFilter = ( OrFilter ) andFilters.get( 0 );
        assertNotNull( orFilter );

        // (& (| (obectclass=top) (...
        List<Filter> orFilters = orFilter.getOrFilter();
        assertEquals( 2, orFilters.size() );
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectclass", assertion.getAttributeDesc() );
        assertEquals( "top", assertion.getAssertionValue().get() );

        // (& (| (objectclass=top) (ou=contacts) ) (...
        equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "2.5.4.11", assertion.getAttributeDesc() );
        assertEquals( "contacts", assertion.getAssertionValue().get() );

        // (& (| (objectclass=top) (ou=contacts) ) (! ...
        NotFilter notFilter = ( NotFilter ) andFilters.get( 1 );
        assertNotNull( notFilter );

        // (& (| (objectclass=top) (ou=contacts) ) (! (objectclass=ttt) ) )
        equalityMatch = ( AttributeValueAssertionFilter ) notFilter.getNotFilter();
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "organizationalUnitName", assertion.getAttributeDesc() );
        assertEquals( "ttt", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // We won't check the encoding, as it has changed because of
        // attributes transformations
    }


    /**
     * Test the decoding of a SearchRequest with SubEntry control.
     */
    @Test
    public void testDecodeSearchRequestSubEntryControl()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x5D, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x33, 
                0x04, 0x13,                     // baseObject: dc=my-domain,dc=com
                  'd', 'c', '=', 'm', 'y', '-', 'd', 'o', 'm', 'a', 
                  'i', 'n', ',', 'd', 'c', '=', 'c', 'o', 'm',
                0x0a, 0x01, 0x02,               // scope: subtree
                0x0a, 0x01, 0x03,               // derefAliases: derefAlways
                0x02, 0x01, 0x00,               // sizeLimit: 0
                0x02, 0x01, 0x00,               // timeLimit: 0
                0x01, 0x01, 0x00,               // typesOnly: false
                ( byte ) 0x87, 0x0b,            // filter: (objectClass=*)
                  'o', 'b', 'j', 'e', 'c', 't', 'C', 'l', 'a', 's', 's',
                0x30, 0x00, 
              ( byte ) 0xa0, 0x23,              // controls
                0x30, 0x21, 
                  0x04, 0x17, 
                    '1', '.', '3', '.', '6', '.', '1', '.', '4', '.', '1', '.', '4', '2', '0', '3',
                    '.', '1', '.', '1', '0', '.', '1', // SubEntry OID
                  0x01, 0x01, ( byte ) 0xFF,    // criticality: true
                  0x04, 0x03, 
                    0x01, 0x01, ( byte ) 0xFF   // SubEntry visibility
            };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        assertEquals( 4, message.getMessageId() );
        assertEquals( 1, message.getControls().size() );

        // SubEntry Control
        String subEntryControlOID = "1.3.6.1.4.1.4203.1.10.1";
        ControlCodec subEntryControl = message.getControls( 0 );
        assertEquals( subEntryControlOID, subEntryControl.getControlType() );
        assertTrue( subEntryControl.getCriticality() );
        assertTrue( ( ( SubEntryControlCodec ) subEntryControl.getControlValue() ).isVisible() );

        SearchRequestCodec sr = message.getSearchRequest();
        assertEquals( "dc=my-domain,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.SUBTREE, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );

        assertTrue( sr.getFilter() instanceof PresentFilter );
        assertEquals( "objectClass", ( ( PresentFilter ) sr.getFilter() ).getAttributeDescription() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            String encodedPdu = StringTools.dumpBytes( bb.array() );
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    // Defensive tests
    /**
     * Test the decoding of a SearchRequest with an empty body
     */
    @Test
    public void testDecodeSearchRequestEmptyBody()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x05, 0x02, 0x01, 0x04, // messageID
                0x63, 0x00 };

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
     * Test the decoding of a SearchRequest with an empty baseDN and nothing more
     */
    @Test
    public void testDecodeSearchRequestBaseDnOnly()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x07, 
              0x02, 0x01, 0x04, // messageID
              0x63, 0x02, 
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
     * Test the decoding of a SearchRequest with no controls. The search filter
     * is : (&(|(objectclass=top)(ou=contacts))(!(objectclass=ttt)))
     */
    @Test
    public void testDecodeSearchRequestEmptyBaseDnNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x6F );
        stream.put( new byte[]
            { 
            0x30, 0x6D,                                 // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,                         // messageID MessageID
              0x63, 0x68,                               // CHOICE { ...,
                                                        // searchRequest SearchRequest, ...
                                                        // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x00,                             // baseObject LDAPDN,
                0x0A, 0x01, 0x01,                       // scope ENUMERATED {
                                                        // baseObject (0),
                                                        // singleLevel (1),
                                                        // wholeSubtree (2) },
                0x0A, 0x01, 0x03,                       // derefAliases ENUMERATED {
                                                        // neverDerefAliases (0),
                                                        // derefInSearching (1),
                                                        // derefFindingBaseObj (2),
                                                        // derefAlways (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF,              // typesOnly  BOOLEAN, (TRUE)
                                                        // filter Filter,
                ( byte ) 0xA0, 0x3C,                    // Filter ::= CHOICE {
                                                        // and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x24,                  // or [1] SET of Filter,
                    ( byte ) 0xA3, 0x12,                // equalityMatch [3]
                                                        // AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 'o', 'p', 
                    ( byte ) 0xA3, 0x0E,                // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x02, 'o', 'u',             // attributeDesc AttributeDescription (LDAPString),
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's', 
                    ( byte ) 0xA2, 0x14,                // not [2] Filter,
                      ( byte ) 0xA3, 0x12,              // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 't', 't',
                                                        // attributes AttributeDescriptionList }
                0x30, 0x15,                             // AttributeDescriptionList ::= SEQUENCE OF
                                                        // AttributeDescription
                  0x04, 0x05, 'a', 't', 't', 'r', '0',  // AttributeDescription ::= LDAPString
                  0x04, 0x05, 'a', 't', 't', 'r', '1',  // AttributeDescription ::= LDAPString
                  0x04, 0x05, 'a', 't', 't', 'r', '2'   // AttributeDescription ::= LDAPString
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (& (...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();

        // (& (| (...
        assertEquals( 2, andFilters.size() );
        OrFilter orFilter = ( OrFilter ) andFilters.get( 0 );
        assertNotNull( orFilter );

        // (& (| (obectclass=top) (...
        List<Filter> orFilters = orFilter.getOrFilter();
        assertEquals( 2, orFilters.size() );
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectclass", assertion.getAttributeDesc() );
        assertEquals( "top", assertion.getAssertionValue().get() );

        // (& (| (objectclass=top) (ou=contacts) ) (...
        equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "ou", assertion.getAttributeDesc() );
        assertEquals( "contacts", assertion.getAssertionValue().get() );

        // (& (| (objectclass=top) (ou=contacts) ) (! ...
        NotFilter notFilter = ( NotFilter ) andFilters.get( 1 );
        assertNotNull( notFilter );

        // (& (| (objectclass=top) (ou=contacts) ) (! (objectclass=ttt) ) )
        equalityMatch = ( AttributeValueAssertionFilter ) notFilter.getNotFilter();
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectclass", assertion.getAttributeDesc() );
        assertEquals( "ttt", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x6F ), decodedPdu.substring( 0, 0x6F ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a bad objectBase
     */
    @Test
    public void testDecodeSearchRequestGlobalBadObjectBase()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x90 );
        stream.put( new byte[]
            { 
            0x30, ( byte ) 0x81, ( byte ) 0x8D,         // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,                         // messageID MessageID
              0x63, ( byte ) 0x81, ( byte ) 0x87,       // CHOICE { ...,
                                                        // searchRequest SearchRequest, ...
                                                        // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                             // baseObject LDAPDN,
                  'u', 'i', 'd', ':', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01,                       // scope ENUMERATED {
                                                        // baseObject (0),
                                                        // singleLevel (1),
                                                        // wholeSubtree (2) },
                0x0A, 0x01, 0x03,                       // derefAliases ENUMERATED {
                                                        // neverDerefAliases (0),
                                                        // derefInSearching (1),
                                                        // derefFindingBaseObj (2),
                                                        // derefAlways (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF,              // typesOnly  BOOLEAN, (TRUE)
                                                        // filter Filter,
                ( byte ) 0xA0, 0x3C,                    // Filter ::= CHOICE {
                                                        // and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x24,                  // or [1] SET of Filter,
                    ( byte ) 0xA3, 0x12,                // equalityMatch [3]
                                                        // AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 'o', 'p', 
                    ( byte ) 0xA3, 0x0E,                // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x02, 'o', 'u',             // attributeDesc AttributeDescription (LDAPString),
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's', 
                    ( byte ) 0xA2, 0x14,                // not [2] Filter,
                      ( byte ) 0xA3, 0x12,              // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 't', 't',
                                                        // attributes AttributeDescriptionList }
                0x30, 0x15,                             // AttributeDescriptionList ::= SEQUENCE OF
                                                        // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'     // AttributeDescription ::= LDAPString
            } );
    
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            assertTrue( de instanceof ResponseCarryingException );
            InternalMessage response = ((ResponseCarryingException)de).getResponse();
            assertTrue( response instanceof SearchResponseDoneImpl );
            assertEquals( ResultCodeEnum.INVALID_DN_SYNTAX, ((SearchResponseDoneImpl)response).getLdapResult().getResultCode() );
            return;
        }

        fail( "We should not reach this point" );
    }
        
    /**
     * Test the decoding of a SearchRequest with an empty scope
     */
    @Test
    public void testDecodeSearchRequestEmptyScope()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x28, 
              0x02, 0x01, 0x04, // messageID
              0x63, 0x23, 
                0x04, 0x1F, // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
                0x0A, 0x00 
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
     * Test the decoding of a SearchRequest with a bad scope
     */
    @Test
    public void testDecodeSearchRequestGlobalBadScope()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x90 );
        stream.put( new byte[]
            { 
            0x30, ( byte ) 0x81, ( byte ) 0x8D,         // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,                         // messageID MessageID
              0x63, ( byte ) 0x81, ( byte ) 0x87,       // CHOICE { ...,
                                                        // searchRequest SearchRequest, ...
                                                        // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                             // baseObject LDAPDN,
                  'u', 'i', 'd', ':', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x03,                       // scope ENUMERATED {
                                                        // baseObject (0),
                                                        // singleLevel (1),
                                                        // wholeSubtree (2) },
                0x0A, 0x01, 0x03,                       // derefAliases ENUMERATED {
                                                        // neverDerefAliases (0),
                                                        // derefInSearching (1),
                                                        // derefFindingBaseObj (2),
                                                        // derefAlways (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF,              // typesOnly  BOOLEAN, (TRUE)
                                                        // filter Filter,
                ( byte ) 0xA0, 0x3C,                    // Filter ::= CHOICE {
                                                        // and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x24,                  // or [1] SET of Filter,
                    ( byte ) 0xA3, 0x12,                // equalityMatch [3]
                                                        // AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 'o', 'p', 
                    ( byte ) 0xA3, 0x0E,                // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x02, 'o', 'u',             // attributeDesc AttributeDescription (LDAPString),
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's', 
                    ( byte ) 0xA2, 0x14,                // not [2] Filter,
                      ( byte ) 0xA3, 0x12,              // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 't', 't',
                                                        // attributes AttributeDescriptionList }
                0x30, 0x15,                             // AttributeDescriptionList ::= SEQUENCE OF
                                                        // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'     // AttributeDescription ::= LDAPString
            } );
    
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

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
     * Test the decoding of a SearchRequest with an empty derefAlias
     */
    @Test
    public void testDecodeSearchRequestEmptyDerefAlias()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x2B, 0x02, 0x01, 0x04, // messageID
                0x63, 0x26, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x00, 0x0A, 0x00 };

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
     * Test the decoding of a SearchRequest with a bad derefAlias
     */
    @Test
    public void testDecodeSearchRequestGlobalBadDerefAlias()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x90 );
        stream.put( new byte[]
            { 
            0x30, ( byte ) 0x81, ( byte ) 0x8D,         // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,                         // messageID MessageID
              0x63, ( byte ) 0x81, ( byte ) 0x87,       // CHOICE { ...,
                                                        // searchRequest SearchRequest, ...
                                                        // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                             // baseObject LDAPDN,
                  'u', 'i', 'd', ':', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01,                       // scope ENUMERATED {
                                                        // baseObject (0),
                                                        // singleLevel (1),
                                                        // wholeSubtree (2) },
                0x0A, 0x01, 0x04,                       // derefAliases ENUMERATED {
                                                        // neverDerefAliases (0),
                                                        // derefInSearching (1),
                                                        // derefFindingBaseObj (2),
                                                        // derefAlways (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF,              // typesOnly  BOOLEAN, (TRUE)
                                                        // filter Filter,
                ( byte ) 0xA0, 0x3C,                    // Filter ::= CHOICE {
                                                        // and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x24,                  // or [1] SET of Filter,
                    ( byte ) 0xA3, 0x12,                // equalityMatch [3]
                                                        // AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 'o', 'p', 
                    ( byte ) 0xA3, 0x0E,                // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x02, 'o', 'u',             // attributeDesc AttributeDescription (LDAPString),
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's', 
                    ( byte ) 0xA2, 0x14,                // not [2] Filter,
                      ( byte ) 0xA3, 0x12,              // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 't', 't',
                                                        // attributes AttributeDescriptionList }
                0x30, 0x15,                             // AttributeDescriptionList ::= SEQUENCE OF
                                                        // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'     // AttributeDescription ::= LDAPString
            } );
    
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

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
     * Test the decoding of a SearchRequest with an empty size limit
     */
    @Test
    public void testDecodeSearchRequestEmptySizeLimit()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x2E, 0x02, 0x01, 0x04, // messageID
                0x63, 0x29, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x00, 0x0A, 0x01, 0x00, 0x02, 0x00 };

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
     * Test the decoding of a SearchRequest with a bad sizeLimit
     */
    @Test
    public void testDecodeSearchRequestGlobalBadSizeLimit()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x8F );
        stream.put( new byte[]
            { 
            0x30, ( byte ) 0x81, ( byte ) 0x8C,         // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,                         // messageID MessageID
              0x63, ( byte ) 0x81, ( byte ) 0x86,       // CHOICE { ...,
                                                        // searchRequest SearchRequest, ...
                                                        // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                             // baseObject LDAPDN,
                  'u', 'i', 'd', ':', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01,                       // scope ENUMERATED {
                                                        // baseObject (0),
                                                        // singleLevel (1),
                                                        // wholeSubtree (2) },
                0x0A, 0x01, 0x03,                       // derefAliases ENUMERATED {
                                                        // neverDerefAliases (0),
                                                        // derefInSearching (1),
                                                        // derefFindingBaseObj (2),
                                                        // derefAlways (3) },
                0x02, 0x01, ( byte ) 0xFF,              // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF,              // typesOnly  BOOLEAN, (TRUE)
                                                        // filter Filter,
                ( byte ) 0xA0, 0x3C,                    // Filter ::= CHOICE {
                                                        // and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x24,                  // or [1] SET of Filter,
                    ( byte ) 0xA3, 0x12,                // equalityMatch [3]
                                                        // AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 'o', 'p', 
                    ( byte ) 0xA3, 0x0E,                // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x02, 'o', 'u',             // attributeDesc AttributeDescription (LDAPString),
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's', 
                    ( byte ) 0xA2, 0x14,                // not [2] Filter,
                      ( byte ) 0xA3, 0x12,              // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 't', 't',
                                                        // attributes AttributeDescriptionList }
                0x30, 0x15,                             // AttributeDescriptionList ::= SEQUENCE OF
                                                        // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'     // AttributeDescription ::= LDAPString
            } );
    
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

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
     * Test the decoding of a SearchRequest with an empty time limit
     */
    @Test
    public void testDecodeSearchRequestEmptyTimeLimit()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x31, 0x02, 0x01, 0x04, // messageID
                0x63, 0x2C, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x00, 0x0A, 0x01, 0x00, 0x02, 0x01,
                0x00, 0x02, 0x00 };

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
     * Test the decoding of a SearchRequest with a bad timeLimit
     */
    @Test
    public void testDecodeSearchRequestGlobalBadTimeLimit()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x8F );
        stream.put( new byte[]
            { 
            0x30, ( byte ) 0x81, ( byte ) 0x8C,         // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,                         // messageID MessageID
              0x63, ( byte ) 0x81, ( byte ) 0x86,       // CHOICE { ...,
                                                        // searchRequest SearchRequest, ...
                                                        // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                             // baseObject LDAPDN,
                  'u', 'i', 'd', ':', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01,                       // scope ENUMERATED {
                                                        // baseObject (0),
                                                        // singleLevel (1),
                                                        // wholeSubtree (2) },
                0x0A, 0x01, 0x03,                       // derefAliases ENUMERATED {
                                                        // neverDerefAliases (0),
                                                        // derefInSearching (1),
                                                        // derefFindingBaseObj (2),
                                                        // derefAlways (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8,        // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x01, ( byte ) 0xFF,              // timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF,              // typesOnly  BOOLEAN, (TRUE)
                                                        // filter Filter,
                ( byte ) 0xA0, 0x3C,                    // Filter ::= CHOICE {
                                                        // and [0] SET OF Filter,
                  ( byte ) 0xA1, 0x24,                  // or [1] SET of Filter,
                    ( byte ) 0xA3, 0x12,                // equalityMatch [3]
                                                        // AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 'o', 'p', 
                    ( byte ) 0xA3, 0x0E,                // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x02, 'o', 'u',             // attributeDesc AttributeDescription (LDAPString),
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's', 
                    ( byte ) 0xA2, 0x14,                // not [2] Filter,
                      ( byte ) 0xA3, 0x12,              // equalityMatch [3] AttributeValueAssertion,
                                                        // AttributeValueAssertion ::= SEQUENCE {
                                                        // attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
                                                        // assertionValue AssertionValue (OCTET STRING) }
                      0x04, 0x03, 't', 't', 't',
                                                        // attributes AttributeDescriptionList }
                0x30, 0x15,                             // AttributeDescriptionList ::= SEQUENCE OF
                                                        // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1',    // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'     // AttributeDescription ::= LDAPString
            } );
    
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

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
     * Test the decoding of a SearchRequest with an empty filter
     */
    @Test
    public void testDecodeSearchRequestEmptyTypeOnly()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x34, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x2F, 0x04, 0x1F,           // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
              0x0A, 0x01, 0x00, 
              0x0A, 0x01, 0x00, 
              0x02, 0x01, 0x00, 
              0x02, 0x01, 0x00, 
              0x01, 0x00 
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
     * Test the decoding of a SearchRequest with an empty filter
     */
    @Test
    public void testDecodeSearchRequestEmptyFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x37, 0x02, 0x01, 0x04, // messageID
                0x63, 0x32, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x00, 0x0A, 0x01, 0x00, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA0, 0x00 };

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
     * Test the decoding of a SearchRequest with an empty Present filter
     */
    @Test
    public void testDecodeSearchRequestEmptyPresentFilter()
    {
        byte[] asn1BER = new byte[]
            { 
                0x30, 0x37, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x32, 
                    0x04, 0x1F, // baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 
                      'a', 's', 'u', 'l', 'u', ',', 'd', 'c', 
                      '=', 'e', 'x', 'a', 'm', 'p', 'l', 'e', 
                      ',', 'd', 'c', '=', 'c', 'o', 'm', 
                    0x0A, 0x01, 0x00, 
                    0x0A, 0x01, 0x00, 
                    0x02, 0x01, 0x00, 
                    0x02, 0x01, 0x00, 
                    0x01, 0x01, ( byte ) 0xFF, 
                    ( byte ) 0x87, 0x00 };

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
     * Test the decoding of a SearchRequest with an empty equalityMatch filter
     */
    @Test
    public void testDecodeSearchRequestEmptyEqualityMatchFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x37, 0x02, 0x01, 0x04, // messageID
                0x63, 0x32, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x00, 0x0A, 0x01, 0x00, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA3, 0x00 };

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
     * Test the decoding of a SearchRequest with an empty greaterOrEqual filter
     */
    @Test
    public void testDecodeSearchRequestEmptyGreaterOrEqualFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x37, 0x02, 0x01, 0x04, // messageID
                0x63, 0x32, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x00, 0x0A, 0x01, 0x00, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA5, 0x00 };

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
     * Test the decoding of a SearchRequest with an empty lessOrEqual filter
     */
    @Test
    public void testDecodeSearchRequestEmptyLessOrEqualFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x37, 0x02, 0x01, 0x04, // messageID
                0x63, 0x32, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x00, 0x0A, 0x01, 0x00, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA6, 0x00 };

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
     * Test the decoding of a SearchRequest with an approxMatch filter
     */
    @Test
    public void testDecodeSearchRequestEmptyApproxMatchFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x37, 0x02, 0x01, 0x04, // messageID
                0x63, 0x32, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x00, 0x0A, 0x01, 0x00, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA8, 0x00 };

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
     * Test the decoding of a SearchRequest with a greaterOrEqual filter and an
     * empty attributeDesc
     */
    @Test
    public void testDecodeSearchRequestEmptyGreaterOrEqualEmptyAttrDesc()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x39, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x34, 0x04, 0x1F,           // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
              0x0A, 0x01, 0x00, 
              0x0A, 0x01, 0x00, 
              0x02, 0x01, 0x00, 
              0x02, 0x01, 0x00, 
              0x01, 0x01, ( byte ) 0xFF, 
              ( byte ) 0xA5, 0x02, 
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
     * Test the decoding of a SearchRequest with a greaterOrEqual filter and an
     * empty attributeValue, and an empty attribute List
     */
    @Test
    public void testDecodeSearchRequestEmptyGreaterOrEqualEmptyAttrValue()
    {
        byte[] asn1BER = new byte[]
            { 
                0x30, 0x41, 
                  0x02, 0x01, 0x04, // messageID
                  0x63, 0x3C, 
                    0x04, 0x1F, // baseObject LDAPDN,
                      'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 
                      'a', 's', 'u', 'l', 'u', ',', 'd', 'c', 
                      '=', 'e', 'x', 'a', 'm', 'p', 'l', 'e', 
                      ',', 'd', 'c', '=', 'c', 'o', 'm', 
                    0x0A, 0x01, 0x01, 
                    0x0A, 0x01, 0x03, 
                    0x02, 0x01, 0x00, 
                    0x02, 0x01, 0x00, 
                    0x01, 0x01, ( byte ) 0xFF, 
                    ( byte ) 0xA5, 0x08, 
                      0x04, 0x04, 't', 'e', 's', 't',
                      0x04, 0x00, 
                    0x30, 0x00 // AttributeDescriptionList ::= SEQUENCE
                                        // OF AttributeDescription
            };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        String decodedPdu = StringTools.dumpBytes( stream.array() );
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 4, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // >=
        AttributeValueAssertionFilter greaterThanFilter = ( AttributeValueAssertionFilter ) sr.getFilter();
        assertNotNull( greaterThanFilter );

        AttributeValueAssertion assertion = greaterThanFilter.getAssertion();

        assertEquals( "test", assertion.getAttributeDesc() );
        assertEquals( "", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();

        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a greaterOrEqual filter and an
     * empty attributeValue, and an '*' attribute List
     */
    @Test
    public void testDecodeSearchRequestEmptyGreaterOrEqualEmptyAttrValueStar()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x44, 0x02, 0x01, 0x04, // messageID
                0x63, 0x3F, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA5, 0x08, 0x04, 0x04, 't', 'e', 's', 't',
                0x04, 0x00, 0x30, 0x03, // AttributeDescriptionList ::= SEQUENCE
                                        // OF AttributeDescription
                0x04, 0x01, '*' };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        String decodedPdu = StringTools.dumpBytes( stream.array() );
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 4, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // >=
        AttributeValueAssertionFilter greaterThanFilter = ( AttributeValueAssertionFilter ) sr.getFilter();
        assertNotNull( greaterThanFilter );

        AttributeValueAssertion assertion = greaterThanFilter.getAssertion();

        assertEquals( "test", assertion.getAttributeDesc() );
        assertEquals( "", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();

        assertEquals( 1, attributes.size() );
        assertEquals( "*", attributes.get( 0 ).getId() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a greaterOrEqual filter and an
     * empty attributeValue, and an empty attribute List
     */
    @Test
    public void testDecodeSearchRequestEmptyGreaterOrEqualEmptyAttrValueEmpty()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x43, 
              0x02, 0x01, 0x04, // messageID
              0x63, 0x3E, 
                0x04, 0x1F,     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, (byte) 0xFF, 
                (byte) 0xA5, 0x08, 
                0x04, 0x04, 
                  't', 'e', 's', 't',
                0x04, 0x00, 
                0x30, 0x02,     // AttributeDescriptionList ::= SEQUENCE
                                // OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 4, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // >=
        AttributeValueAssertionFilter greaterThanFilter = ( AttributeValueAssertionFilter ) sr.getFilter();
        assertNotNull( greaterThanFilter );

        AttributeValueAssertion assertion = greaterThanFilter.getAssertion();

        assertEquals( "test", assertion.getAttributeDesc() );
        assertEquals( "", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();

        assertEquals( 0, attributes.size() );
    }


    /**
     * Test the decoding of a SearchRequest with an empty And filter
     */
    @Test
    public void testDecodeSearchRequestEmptyAndFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x3B, 0x02, 0x01, 0x04, // messageID
                0x63, 0x36, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA0, 0x00, 0x30, 0x02, // AttributeDescriptionList
                                                                                                    // ::=
                                                                                                    // SEQUENCE
                                                                                                    // OF
                                                                                                    // AttributeDescription
                0x04, 0x00 };

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
     * Test the decoding of a SearchRequest with an empty Or filter
     */
    @Test
    public void testDecodeSearchRequestEmptyOrFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x3B, 0x02, 0x01, 0x04, // messageID
                0x63, 0x36, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA1, 0x00, 0x30, 0x02, // AttributeDescriptionList
                                                                                                    // ::=
                                                                                                    // SEQUENCE
                                                                                                    // OF
                                                                                                    // AttributeDescription
                0x04, 0x00 };

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
     * Test the decoding of a SearchRequest with an empty Not filter
     */
    @Test
    public void testDecodeSearchRequestEmptyNotFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x3B, 0x02, 0x01, 0x04, // messageID
                0x63, 0x36, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA2, 0x00, 0x30, 0x02, // AttributeDescriptionList
                                                                                                    // ::=
                                                                                                    // SEQUENCE
                                                                                                    // OF
                                                                                                    // AttributeDescription
                0x04, 0x00 };

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
     * Test the decoding of a SearchRequest with a Not filter and an empty And
     * filter
     */
    @Test
    public void testDecodeSearchRequestNotFilterEmptyAndFilter()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x3D, 0x02, 0x01, 0x04, // messageID
                0x63, 0x38, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA2, 0x02, ( byte ) 0xA0, 0x00, 0x30,
                0x02, // AttributeDescriptionList ::= SEQUENCE OF
                        // AttributeDescription
                0x04, 0x00 };

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
     * Test the decoding of a SearchRequest with a greaterOrEqual filter and an
     * empty attributeValue, and an '*' attribute List
     */
    @Test
    public void testDecodeSearchRequestDIRSERVER_651()
    {
        byte[] asn1BER = new byte[]
            { 
                0x30, 0x60, 
                0x02, 0x01, 0x02, 
                0x63, 0x5b, 
                  0x04, 0x0a, 
                    'd', 'c', '=', 'p', 'g', 'p', 'k', 'e', 'y', 's',
                  0x0a, 01, 02, 
                  0x0a, 01, 00, 
                  0x02, 01, 00, 
                  0x02, 01, 00, 
                  0x01, 01, 00, 
                  (byte)0xa0, 0x3c, 
                    (byte)0xa4, 0x28, 
                      0x04, 0x09, 
                        'p', 'g', 'p', 'u', 's', 'e', 'r', 'i', 'd',
                      0x30, 0x1b, 
                        (byte)0x80, 0x19, 
                          'v', 'g', 'j', 'o', 'k', 'j', 'e', 'v', '@', 
                          'n', 'e', 't', 'c', 'e', 't', 'e', 'r', 'a', '.', 'c', 'o', 'm', '.', 'm', 'k',
                    (byte)0xa3, 0x10,
                      0x04, 0x0b, 
                        'p', 'g', 'p', 'd', 'i', 's', 'a', 'b', 'l', 'e', 'd',
                      0x04, 0x01, 
                        '0',
                  0x30, 0x00
            };

        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        String decodedPdu = StringTools.dumpBytes( stream.array() );
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 2, message.getMessageId() );
        assertEquals( "dc=pgpkeys", sr.getBaseObject().toString() );
        assertEquals( SearchScope.SUBTREE, sr.getScope() );
        assertEquals( LdapConstants.NEVER_DEREF_ALIASES, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );

        // And 
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        SubstringFilter substringFilter = ( SubstringFilter ) andFilters.get( 0 );
        assertNotNull( substringFilter );

        assertEquals( "pgpuserid", substringFilter.getType() );
        assertEquals( "vgjokjev@netcetera.com.mk", substringFilter.getInitialSubstrings() );
        assertEquals( 0, substringFilter.getAnySubstrings().size() );
        assertEquals( null, substringFilter.getFinalSubstrings() );

        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 1 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "pgpdisabled", assertion.getAttributeDesc() );
        assertEquals( "0", assertion.getAssertionValue().get() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest
     * (a=b)
     */
    @Test
    public void testDecodeSearchRequestEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x25 );
        stream.put( new byte[]
            { 
                0x30, 0x23,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x1E,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA3, 0x06,    // Filter ::= CHOICE {
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                      0x04, 0x01, 'a',      //      attributeDesc AttributeDescription (LDAPString),
                      0x04, 0x01, 'b',      //      assertionValue AssertionValue (OCTET STRING) } 
                                            // attributes AttributeDescriptionList }
                    0x30, 0x00,             // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (a=b)
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) sr.getFilter();
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x25 ), decodedPdu.substring( 0, 0x25 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest
     * (&(a=b))
     */
    @Test
    public void testDecodeSearchRequestAndEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x27 );
        stream.put( new byte[]
            { 
                0x30, 0x25,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x20,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x08,    // Filter ::= CHOICE {
                      ( byte ) 0xA3, 0x06,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                        0x04, 0x01, 'a',    //      attributeDesc AttributeDescription (LDAPString),
                        0x04, 0x01, 'b',    //      assertionValue AssertionValue (OCTET STRING) } 
                                            // attributes AttributeDescriptionList }
                    0x30, 0x00,             // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 1, andFilters.size() );
        
        // (&(a=b))
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x27 ), decodedPdu.substring( 0, 0x27 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest
     * (&(a=b)(c=d))
     */
    @Test
    public void testDecodeSearchRequestAndEqEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x2F );
        stream.put( new byte[]
            { 
                0x30, 0x2D,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x28,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x10,    // Filter ::= CHOICE {
                      ( byte ) 0xA3, 0x06,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                        0x04, 0x01, 'a',    //      attributeDesc AttributeDescription (LDAPString),
                        0x04, 0x01, 'b',    //      assertionValue AssertionValue (OCTET STRING) } 
                      ( byte ) 0xA3, 0x06,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                        0x04, 0x01, 'c',    //      attributeDesc AttributeDescription (LDAPString),
                        0x04, 0x01, 'd',    //      assertionValue AssertionValue (OCTET STRING) } 
                                            // attributes AttributeDescriptionList }
                    0x30, 0x00,             // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(a=b)...
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        // (&(a=b)(c=d))
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "c", assertion.getAttributeDesc() );
        assertEquals( "d", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x2F ), decodedPdu.substring( 0, 0x2F ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest
     * (&(&(a=b))
     */
    @Test
    public void testDecodeSearchRequestAndAndEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x29 );
        stream.put( new byte[]
            { 
                0x30, 0x27,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x22,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x0A,    // Filter ::= CHOICE { and             [0] SET OF Filter,
                      ( byte ) 0xA0, 0x08,  // Filter ::= CHOICE { and             [0] SET OF Filter,
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'a',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'b',  //      assertionValue AssertionValue (OCTET STRING) } 
                    0x30, 0x00,             // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 1, andFilters.size() );
        
        // (&(&(..
        AndFilter andFilter2 = ( AndFilter ) andFilters.get( 0 );
        assertNotNull( andFilter2 );

        List<Filter> andFilters2 = andFilter2.getAndFilter();
        assertEquals( 1, andFilters2.size() );

        // (&(&(a=b)))
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x29 ), decodedPdu.substring( 0, 0x29 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest
     * (&(&(a=b)(c=d))
     */
    @Test
    public void testDecodeSearchRequestAndAndEqEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x31 );
        stream.put( new byte[]
            { 
                0x30, 0x2F,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x2A,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x12,    // Filter ::= CHOICE { and             [0] SET OF Filter,
                      ( byte ) 0xA0, 0x10,  // Filter ::= CHOICE { and             [0] SET OF Filter,
                        ( byte ) 0xA3, 0x06,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'a',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'b',  //      assertionValue AssertionValue (OCTET STRING) } 
                        ( byte ) 0xA3, 0x06,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'c',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'd',  //      assertionValue AssertionValue (OCTET STRING) } 
                    0x30, 0x00,             // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 1, andFilters.size() );
        
        // (&(&(..
        AndFilter andFilter2 = ( AndFilter ) andFilters.get( 0 );
        assertNotNull( andFilter2 );

        List<Filter> andFilters2 = andFilter2.getAndFilter();
        assertEquals( 2, andFilters2.size() );

        // (&(&(a=b)...
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        // (&(&(a=b)(c=d)
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "c", assertion.getAttributeDesc() );
        assertEquals( "d", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x31 ), decodedPdu.substring( 0, 0x31 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest
     * (&(&(a=b))(c=d))
     */
    @Test
    public void testDecodeSearchRequestAnd_AndEq_Eq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x31 );
        stream.put( new byte[]
            { 
                0x30, 0x2F,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x2A,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x12,    // Filter ::= CHOICE { and             [0] SET OF Filter,
                      ( byte ) 0xA0, 0x08,  // Filter ::= CHOICE { and             [0] SET OF Filter,
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'a',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'b',  //      assertionValue AssertionValue (OCTET STRING) } 
                      ( byte ) 0xA3, 0x06,  //      equalityMatch [3] AttributeValueAssertion,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                        0x04, 0x01, 'c',    //      attributeDesc AttributeDescription (LDAPString),
                        0x04, 0x01, 'd',    //      assertionValue AssertionValue (OCTET STRING) } 
                    0x30, 0x00,             // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(&(..
        AndFilter andFilter2 = ( AndFilter ) andFilters.get( 0 );
        assertNotNull( andFilter2 );

        List<Filter> andFilters2 = andFilter2.getAndFilter();
        assertEquals( 1, andFilters2.size() );

        // (&(&(a=b))...
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        // (&(&(a=b))(c=d))
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "c", assertion.getAttributeDesc() );
        assertEquals( "d", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x31 ), decodedPdu.substring( 0, 0x31 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest
     * (&(&(a=b)(c=d))(e=f))
     */
    @Test
    public void testDecodeSearchRequestAnd_AndEqEq_Eq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x39 );
        stream.put( new byte[]
            { 
                0x30, 0x37,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x32,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x1A,    // Filter ::= CHOICE { and             [0] SET OF Filter,
                      ( byte ) 0xA0, 0x10,  // Filter ::= CHOICE { and             [0] SET OF Filter,
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'a',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'b',  //      assertionValue AssertionValue (OCTET STRING) } 
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'c',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'd',  //      assertionValue AssertionValue (OCTET STRING) } 
                      ( byte ) 0xA3, 0x06,  //      equalityMatch [3] AttributeValueAssertion,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                        0x04, 0x01, 'e',    //      attributeDesc AttributeDescription (LDAPString),
                        0x04, 0x01, 'f',    //      assertionValue AssertionValue (OCTET STRING) } 
                    0x30, 0x00,             // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(&(..
        AndFilter andFilter2 = ( AndFilter ) andFilters.get( 0 );
        assertNotNull( andFilter2 );

        List<Filter> andFilters2 = andFilter2.getAndFilter();
        assertEquals( 2, andFilters2.size() );

        // (&(&(a=b)...
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        // (&(&(a=b)(c=d)...
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "c", assertion.getAttributeDesc() );
        assertEquals( "d", assertion.getAssertionValue().get() );
        
        // (&(&(a=b)(c=d))(e=f))
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "e", assertion.getAttributeDesc() );
        assertEquals( "f", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x39 ), decodedPdu.substring( 0, 0x39 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }




    /**
     * Test the decoding of a SearchRequest
     * (&(a=b)(|(a=b)(c=d)))
     */
    @Test
    public void testDecodeSearchRequestAndEq_OrEqEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x39 );
        stream.put( new byte[]
            { 
                0x30, 0x37,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x32,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x1A,    // Filter ::= CHOICE { and             [0] SET OF Filter,
                      ( byte ) 0xA3, 0x06,  //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                        0x04, 0x01, 'a',  //      attributeDesc AttributeDescription (LDAPString),
                        0x04, 0x01, 'b',  //      assertionValue AssertionValue (OCTET STRING) } 
                      ( byte ) 0xA1, 0x10,  // Filter ::= CHOICE { or             [1] SET OF Filter,
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'c',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'd',  //      assertionValue AssertionValue (OCTET STRING) } 
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'e',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'f',  //      assertionValue AssertionValue (OCTET STRING) } 
                    0x30, 0x00,             // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(a=b)..
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        // (&(a=b)(|(...
        OrFilter orFilter = ( OrFilter ) andFilters.get( 1 );
        assertNotNull( orFilter );

        List<Filter> orFilters = orFilter.getOrFilter();
        assertEquals( 2, orFilters.size() );

        // (&(a=b)(|(c=d)...
        equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 0 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "c", assertion.getAttributeDesc() );
        assertEquals( "d", assertion.getAssertionValue().get() );

        // (&(a=b)(|(c=d)(e=f)))
        equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "e", assertion.getAttributeDesc() );
        assertEquals( "f", assertion.getAssertionValue().get() );
        
        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x39 ), decodedPdu.substring( 0, 0x39 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }
    
    
    /**
     * Test the decoding of a SearchRequest
     * (&(&(a=b))(&(c=d)))
     */
    @Test
    public void testDecodeSearchRequestAnd_AndEq_AndEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x33 );
        stream.put( new byte[]
            { 
                0x30, 0x31,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x2C,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x14,    // Filter ::= CHOICE { and             [0] SET OF Filter,
                      ( byte ) 0xA0, 0x08,  // Filter ::= CHOICE { and             [0] SET OF Filter,
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'a',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'b',  //      assertionValue AssertionValue (OCTET STRING) } 
                      ( byte ) 0xA0, 0x08,  // Filter ::= CHOICE { and             [0] SET OF Filter,
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'c',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'd',  //      assertionValue AssertionValue (OCTET STRING) } 
                    0x30, 0x00              // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(&(..
        AndFilter andFilter2 = ( AndFilter ) andFilters.get( 0 );
        assertNotNull( andFilter2 );

        List<Filter> andFilters2 = andFilter2.getAndFilter();
        assertEquals( 1, andFilters2.size() );

        // (&(&(a=b)...
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        // (&(&(a=b))(&...
        andFilter2 = ( AndFilter ) andFilters.get( 1 );
        assertNotNull( andFilter2 );

        andFilters2 = andFilter2.getAndFilter();
        assertEquals( 1, andFilters2.size() );

        // (&(&(a=b))(&(c=d)))
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 0 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "c", assertion.getAttributeDesc() );
        assertEquals( "d", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x33 ), decodedPdu.substring( 0, 0x33 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest
     * (&(&(a=b)(c=d))(&(e=f)))
     */
    @Test
    public void testDecodeSearchRequestAnd_AndEqEq_AndEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x3B );
        stream.put( new byte[]
            { 
                0x30, 0x39,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x34,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x1C,    // Filter ::= CHOICE { and             [0] SET OF Filter,
                      ( byte ) 0xA0, 0x10,  // Filter ::= CHOICE { and             [0] SET OF Filter,
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'a',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'b',  //      assertionValue AssertionValue (OCTET STRING) } 
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'c',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'd',  //      assertionValue AssertionValue (OCTET STRING) } 
                      ( byte ) 0xA0, 0x08,  // Filter ::= CHOICE { and             [0] SET OF Filter,
                        ( byte ) 0xA3, 0x06,//      equalityMatch [3] AttributeValueAssertion,
                                            //      equalityMatch [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'e',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'f',  //      assertionValue AssertionValue (OCTET STRING) } 
                    0x30, 0x00              // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(&(..
        AndFilter andFilter2 = ( AndFilter ) andFilters.get( 0 );
        assertNotNull( andFilter2 );

        List<Filter> andFilters2 = andFilter2.getAndFilter();
        assertEquals( 2, andFilters2.size() );

        // (&(&(a=b)...
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "a", assertion.getAttributeDesc() );
        assertEquals( "b", assertion.getAssertionValue().get() );

        // (&(&(a=b)(c=d))...
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "c", assertion.getAttributeDesc() );
        assertEquals( "d", assertion.getAssertionValue().get() );
        
        // (&(&(a=b)(c=d))(&...
        andFilter2 = ( AndFilter ) andFilters.get( 1 );
        assertNotNull( andFilter2 );

        andFilters2 = andFilter2.getAndFilter();
        assertEquals( 1, andFilters2.size() );

        // (&(&(a=b)(c=d))(&(e=f)))
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters2.get( 0 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "e", assertion.getAttributeDesc() );
        assertEquals( "f", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x3B ), decodedPdu.substring( 0, 0x3B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest
     * (&(|(abcdef=*)(ghijkl=*))(!(e>=f)))
     */
    @Test
    public void testDecodeSearchRequestAnd_OrPrPr_NotGEq()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x3B );
        stream.put( new byte[]
            { 
                0x30, 0x39,                 // LDAPMessage ::=SEQUENCE {
                  0x02, 0x01, 0x01,         // messageID MessageID
                  0x63, 0x34,               // CHOICE { ...,
                                            // searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                    0x04, 0x03,             // baseObject LDAPDN,
                      'a', '=', 'b', 
                    0x0A, 0x01, 0x01,       // scope ENUMERATED {
                                            //      baseObject (0),
                                            //      singleLevel (1),
                                            //      wholeSubtree (2) },
                    0x0A, 0x01, 0x03,       // derefAliases ENUMERATED {
                                            //      neverDerefAliases (0),
                                            //      derefInSearching (1),
                                            //      derefFindingBaseObj (2),
                                            //      derefAlways (3) },
                    0x02, 0x01, 0x00,       // sizeLimit INTEGER (0 .. maxInt), (0)
                    0x02, 0x01, 0x00,       // timeLimit INTEGER (0 .. maxInt), (1000) 
                    0x01, 0x01, ( byte ) 0xFF,// typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                    ( byte ) 0xA0, 0x1C,    // Filter ::= CHOICE { and             [0] SET OF Filter,
                      ( byte ) 0xA1, 0x10,  // Filter ::= CHOICE { or             [0] SET OF Filter,
                        ( byte ) 0x87, 0x06,// present [7] AttributeDescription,
                          'a', 'b', 'c',    // AttributeDescription ::= LDAPString
                          'd', 'e', 'f',     
                        ( byte ) 0x87, 0x06,// present [7] AttributeDescription,
                          'g', 'h', 'i',    // AttributeDescription ::= LDAPString
                          'j', 'k', 'l',     
                      ( byte ) 0xA2, 0x08,  // Filter ::= CHOICE { not             Filter,
                        ( byte ) 0xA5, 0x06,//      greaterOrEqual [3] AttributeValueAssertion,
                                            // AttributeValueAssertion ::= SEQUENCE {
                          0x04, 0x01, 'e',  //      attributeDesc AttributeDescription (LDAPString),
                          0x04, 0x01, 'f',  //      assertionValue AssertionValue (OCTET STRING) } 
                    0x30, 0x00              // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
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

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "a=b", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(|(..
        OrFilter orFilter = ( OrFilter ) andFilters.get( 0 );
        assertNotNull( orFilter );

        List<Filter> orFilters = orFilter.getOrFilter();
        assertEquals( 2, orFilters.size() );

        // (&(&(abcdef=*)...
        PresentFilter presentFilter = ( PresentFilter ) orFilters.get( 0 );
        assertNotNull( presentFilter );

        assertEquals( "abcdef", presentFilter.getAttributeDescription() );

        // (&(&(abcdef=*)(ghijkl=*))...
        presentFilter = ( PresentFilter ) orFilters.get( 1 );
        assertNotNull( presentFilter );

        assertEquals( "ghijkl", presentFilter.getAttributeDescription() );
        
        // (&(&(abcdef=*)(ghijkl=*))(&...
        NotFilter notFilter = ( NotFilter ) andFilters.get( 1 );
        assertNotNull( notFilter );

        // (&(&(abcdef=*)(ghijkl=*))(&(e=f)))
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) notFilter.getNotFilter();
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "e", assertion.getAttributeDesc() );
        assertEquals( "f", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x3B ), decodedPdu.substring( 0, 0x3B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }
    
    /**
     * Test the decoding of a SearchRequest
     * for rootDSE
     */
    @Test
    public void testDecodeSearchRequestRootDSE()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x33 );
        stream.put( new byte[]
            { 
                0x30, (byte)0x84, 0x00, 0x00, 0x00, 0x2D, 
                  0x02, 0x01, 0x01, 
                  0x63, (byte)0x84, 0x00, 0x00, 0x00, 0x24, 
                    0x04, 0x00, 
                    0x0A, 0x01, 0x00, 
                    0x0A, 0x01, 0x00, 
                    0x02, 0x01, 0x00, 
                    0x02, 0x01, 0x00, 
                    0x01, 0x01, 0x00, 
                    (byte)0x87, 0x0B, 
                      0x6F, 0x62, 0x6A, 0x65, 0x63, 0x74, 0x43, 0x6C, 0x61, 0x73, 0x73, 
                    0x30, (byte)0x84, 0x00, 0x00, 0x00, 0x00
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
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "", sr.getBaseObject().toString() );
        assertEquals( SearchScope.OBJECT, sr.getScope() );
        assertEquals( LdapConstants.SCOPE_BASE_OBJECT, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );
        
        PresentFilter presentFilter = ( PresentFilter ) sr.getFilter();
        assertNotNull( presentFilter );
        assertEquals( "objectClass", presentFilter.getAttributeDescription() );
        
        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );
    }
    
    /**
     * Test the decoding of a SearchRequest with special length (long form)
     * for rootDSE
     */
    @Test
    public void testDecodeSearchRequestDIRSERVER_810()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x6B );
        stream.put( new byte[]
            { 
                0x30, (byte)0x84, 0x00, 0x00, 0x00, 0x65,
                  0x02, 0x01, 0x03,
                  0x63, (byte)0x84, 0x00, 0x00, 0x00, 0x5c,
                    0x04, 0x12,
                      0x6f, 0x75, 0x3d, 0x75, 0x73, 0x65, 0x72, 0x73, 0x2c, 
                      0x6f, 0x75, 0x3d, 0x73, 0x79, 0x73, 0x74, 0x65, 0x6d, // 'ou=users,ou=system'
                    0x0a, 0x01, 0x01,
                    0x0a, 0x01, 0x00,
                    0x02, 0x01, 0x00,
                    0x02, 0x01, 0x1e,
                    0x01, 0x01, (byte)0xff,
                    (byte)0xa0, (byte)0x84, 0x00, 0x00, 0x00, 0x2d,
                      (byte)0xa3, (byte)0x84, 0x00, 0x00, 0x00, 0x0e,
                        0x04, 0x03,
                          0x75, 0x69, 0x64,
                        0x04, 0x07,
                          0x62, 0x75, 0x73, 0x74, 0x65, 0x72, 0x20, // 'buster ' (with a space at the end)
                      (byte)0xa3, (byte)0x84, 0x00, 0x00, 0x00, 0x13,
                        0x04, 0x0b,
                          0x73, 0x62, 0x41, 0x74, 0x74, 0x72, 0x69, 0x62, 0x75, 0x74, 0x65, // sbAttribute
                        0x04, 0x04,
                          0x42, 0x75, 0x79, 0x20, // 'Buy ' (with a space at the end)
                    0x30, (byte)0x84, 0x00, 0x00, 0x00, 0x00
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
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 3, message.getMessageId() );
        assertEquals( "ou=users,ou=system", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.SCOPE_BASE_OBJECT, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 30, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );
        
        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(uid=buster)...
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "uid", assertion.getAttributeDesc() );
        assertEquals( "buster ", assertion.getAssertionValue().get() );

        // (&(uid=buster)(sbAttribute=Buy))
        equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "sbAttribute", assertion.getAttributeDesc() );
        assertEquals( "Buy ", assertion.getAssertionValue().get() );

        List<EntryAttribute> attributes = sr.getAttributes();
        assertEquals( 0, attributes.size() );
    }
    
    
    /**
     * Test the decoding of a SearchRequest with a complex filter :
     * (&(objectClass=person)(|(cn=Tori*)(sn=Jagger)))
     */
    @Test
    public void testDecodeSearchRequestComplexFilter()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();
    
        ByteBuffer stream = ByteBuffer.allocate( 0x77 );
        stream.put( new byte[]
            {
               0x30, 0x75,                  // LdapMessage
                 0x02, 0x01, 0x06,          // message Id = 6
                 0x63, 0x53,                // SearchRequest
                   0x04, 0x09,              // BasDN 'ou=system'
                     0x6F, 0x75, 0x3D, 0x73, 0x79, 0x73, 0x74, 0x65, 0x6D, 
                   0x0A, 0x01, 0x02,        // scope = SUBTREE
                   0x0A, 0x01, 0x03,        // derefAlias = 3
                   0x02, 0x01, 0x00,        // sizeLimit = none
                   0x02, 0x01, 0x00,        // timeLimit = none
                   0x01, 0x01, 0x00,        // types only = false
                   (byte)0xA0, 0x35,        // AND
                     (byte)0xA3, 0x15,      // equals
                       0x04, 0x0B,          // 'objectclass'
                         0x6F, 0x62, 0x6A, 0x65, 0x63, 0x74, 0x43, 0x6C, 
                         0x61, 0x73, 0x73, 
                       0x04, 0x06,          // 'person'
                         0x70, 0x65, 0x72, 0x73, 0x6F, 0x6E, 
                     (byte)0xA1, 0x1C,      // OR
                       (byte)0xA4, 0x0C,    // substrings : 'cn=Tori*'
                         0x04, 0x02,        // 'cn'
                           0x63, 0x6E, 
                         0x30, 0x06,        // initial = 'Tori'
                           (byte)0x80, 0x04, 
                               0x54, 0x6F, 0x72, 0x69, 
                       (byte)0xA3, 0x0C,    // equals
                         0x04, 0x02,        // 'sn'
                           0x73, 0x6E,    
                         0x04, 0x06,        // 'Jagger'
                           0x4A, 0x61, 0x67, 0x67, 0x65, 0x72, 
                   0x30, 0x00,              // Control
                   (byte)0xA0, 0x1B, 
                     0x30, 0x19, 
                       0x04, 0x17, 
                         '2', '.', '1', '6', '.', '8', '4', '0', '.',
                         '1', '.', '1', '1', '3', '7', '3', '0', '.',
                         '3', '.', '4', '.', '2'
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
            fail( de.getMessage() );
        }

        assertEquals( TLVStateEnum.PDU_DECODED, ldapMessageContainer.getState() );
        
        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 6, message.getMessageId() );
        assertEquals( "ou=system", sr.getBaseObject().toString() );
        assertEquals( SearchScope.SUBTREE, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );

        // (&(...
        AndFilter andFilter = ( AndFilter ) sr.getFilter();
        assertNotNull( andFilter );

        List<Filter> andFilters = andFilter.getAndFilter();
        assertEquals( 2, andFilters.size() );
        
        // (&(objectClass=person)..
        AttributeValueAssertionFilter equalityMatch = ( AttributeValueAssertionFilter ) andFilters.get( 0 );
        assertNotNull( equalityMatch );

        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "objectClass", assertion.getAttributeDesc() );
        assertEquals( "person", assertion.getAssertionValue().get() );

        // (&(a=b)(|
        OrFilter orFilter = ( OrFilter ) andFilters.get( 1 );
        assertNotNull( orFilter );

        List<Filter> orFilters = orFilter.getOrFilter();
        assertEquals( 2, orFilters.size() );
        
        // (&(a=b)(|(cn=Tori*
        SubstringFilter substringFilter = ( SubstringFilter ) orFilters.get( 0 );
        assertNotNull( substringFilter );

        assertEquals( "cn", substringFilter.getType() );
        assertEquals( "Tori", substringFilter.getInitialSubstrings() );
        assertEquals( 0, substringFilter.getAnySubstrings().size() );
        assertEquals( null, substringFilter.getFinalSubstrings() );

        // (&(a=b)(|(cn=Tori*)(sn=Jagger)))
        equalityMatch = ( AttributeValueAssertionFilter ) orFilters.get( 1 );
        assertNotNull( equalityMatch );

        assertion = equalityMatch.getAssertion();
        assertNotNull( assertion );

        assertEquals( "sn", assertion.getAttributeDesc() );
        assertEquals( "Jagger", assertion.getAssertionValue().get() );
    }
}
