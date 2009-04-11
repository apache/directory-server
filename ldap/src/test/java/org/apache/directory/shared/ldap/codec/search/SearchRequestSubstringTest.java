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
import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SubstringFilter;
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
import static org.junit.Assert.assertNotNull;


/**
 * A test case for SearchRequest messages
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestSubstringTest
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
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=t*)
     */
    @Test
    public void testDecodeSearchRequestSubstringInitialAny()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x64 );
        stream.put( new byte[]
            { 
            0x30, 0x62,                     // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,             //      messageID MessageID
              0x63, 0x5D,                   //      CHOICE { ..., 
                                            //          searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                 //      baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01,           //      scope ENUMERATED {
                                            //          baseObject      (0),
                                            //          singleLevel     (1),
                                            //          wholeSubtree    (2) },
                0x0A, 0x01, 0x03,           //      derefAliases ENUMERATED {
                                            //          neverDerefAliases   (0),
                                            //          derefInSearching    (1),
                                            //          derefFindingBaseObj (2),
                                            //          derefAlways         (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8,    //      sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8,    // timeLimit INTEGER (0 .. maxInt), (1000) 
                0x01, 0x01, ( byte ) 0xFF, // typesOnly
                                                                            // BOOLEAN,
                                                                            // (TRUE)
                // filter Filter,
                ( byte ) 0xA4, 0x12, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                  0x04, 0x0B, // type AttributeDescription,
                    'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 
                  0x30, 0x03, 
                    ( byte ) 0x80, 0x01, 't', //
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( "t", substringFilter.getInitialSubstrings() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x64, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x53 ), decodedPdu.substring( 0, 0x53 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=t*) With controls
     */
    @Test
    public void testDecodeSearchRequestSubstringInitialAnyWithControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x0081 );
        stream.put( new byte[]
            { 0x30, 0x7F, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x5D, // CHOICE { ..., searchRequest SearchRequest, ...
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
                ( byte ) 0xA4, 0x12, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                0x04, 0x0B, // type AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 0x30, 0x03, ( byte ) 0x80, 0x01, 't', //
                0x30, 0x15, // AttributeDescriptionList ::= SEQUENCE OF
                            // AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription
                                                        // ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription
                                                        // ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2', // AttributeDescription
                                                        // ::= LDAPString
                ( byte ) 0xA0, 0x1B, // A control
                0x30, 0x19, 0x04, 0x17, 0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 0x2E, 0x31, 0x2E, 0x31, 0x31,
                0x33, 0x37, 0x33, 0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32 } );

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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( "t", substringFilter.getInitialSubstrings() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the Control
        List<ControlCodec> controls = message.getControls();

        assertEquals( 1, controls.size() );

        ControlCodec control = message.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( ( byte[] ) control.getControlValue() ) );

        // Check the length
        assertEquals( 0x0081, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x53 ), decodedPdu.substring( 0, 0x53 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * any filter : (objectclass=*t*)
     */
    @Test
    public void testDecodeSearchRequestSubstringAny()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x64 );
        stream.put( new byte[]
            { 0x30, 0x62, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x5D, // CHOICE { ..., searchRequest SearchRequest, ...
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
                ( byte ) 0xA4, 0x12, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                0x04, 0x0B, // type AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 0x30, 0x03, ( byte ) 0x81, 0x01, 't', //
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 0 ) );
        assertEquals( null, substringFilter.getFinalSubstrings() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x64, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x53 ), decodedPdu.substring( 0, 0x53 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=*t*t)
     */
    @Test
    public void testDecodeSearchRequestSubstringAnyFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x67 );
        stream.put( new byte[]
            { 0x30, 0x65, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x60, // CHOICE { ..., searchRequest SearchRequest, ...
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
                ( byte ) 0xA4, 0x15, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                0x04, 0x0B, // type AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 0x30, 0x06, ( byte ) 0x81, 0x01, 't', //
                ( byte ) 0x82, 0x01, 't', //
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 0 ) );
        assertEquals( "t", substringFilter.getFinalSubstrings() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x67, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x58 ), decodedPdu.substring( 0, 0x58 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=t*t*t)
     */
    @Test
    public void testDecodeSearchRequestSubstringInitialAnyFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x6A );
        stream.put( new byte[]
            { 0x30, 0x68, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x63, // CHOICE { ..., searchRequest SearchRequest, ...
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
                ( byte ) 0xA4, 0x18, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                0x04, 0x0B, // type AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 0x30, 0x09, ( byte ) 0x80, 0x01, 't', //
                ( byte ) 0x81, 0x01, 't', //
                ( byte ) 0x82, 0x01, 't', //
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( "t", substringFilter.getInitialSubstrings() );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 0 ) );
        assertEquals( "t", substringFilter.getFinalSubstrings() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x6A, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x5B ), decodedPdu.substring( 0, 0x5B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=t*t*)
     */
    @Test
    public void testDecodeSearchRequestSubstringInitialAnyAnyFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x67 );
        stream.put( new byte[]
            { 0x30, 0x65, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x60, // CHOICE { ..., searchRequest SearchRequest, ...
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
                ( byte ) 0xA4, 0x15, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                0x04, 0x0B, // type AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 0x30, 0x06, ( byte ) 0x80, 0x01, 't', //
                ( byte ) 0x81, 0x01, 't', //
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( "t", substringFilter.getInitialSubstrings() );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 0 ) );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x67, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x58 ), decodedPdu.substring( 0, 0x58 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=t*t*t)
     */
    @Test
    public void testDecodeSearchRequestSubstringAnyAnyFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x6A );
        stream.put( new byte[]
            { 
            0x30, 0x68,                     // LDAPMessage ::=SEQUENCE {
              0x02, 0x01, 0x01,             // messageID MessageID
              0x63, 0x63,                   // CHOICE { ..., searchRequest SearchRequest, ...
                                            // SearchRequest ::= APPLICATION[3] SEQUENCE {
                0x04, 0x1F,                 // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01,           // scope ENUMERATED {
                                            // baseObject (0),
                                            // singleLevel (1),
                                            // wholeSubtree (2) },
                0x0A, 0x01, 0x03,           // derefAliases ENUMERATED {
                                            // neverDerefAliases (0),
                                            // derefInSearching (1),
                                            // derefFindingBaseObj (2),
                                            // derefAlways (3) },
                0x02, 0x02, 0x03, ( byte ) 0xE8, // sizeLimit INTEGER (0 .. maxInt), (1000)
                0x02, 0x02, 0x03, ( byte ) 0xE8, // timeLimit INTEGER (0 .. maxInt), (1000)
                0x01, 0x01, ( byte ) 0xFF,  // typesOnly BOOLEAN, (TRUE)
                                            // filter Filter,
                ( byte ) 0xA4, 0x18,        // Filter ::= CHOICE {
                                            // substrings [4] SubstringFilter }
                                            // SubstringFilter ::= SEQUENCE {
                  0x04, 0x0B,               // type AttributeDescription,
                    'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 
                  0x30, 0x09, 
                    ( byte ) 0x81, 0x01, 't',
                    ( byte ) 0x81, 0x01, 't',
                    ( byte ) 0x82, 0x01, 't',
                0x30, 0x15,                 // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                  0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                  0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                  0x04, 0x05, 'a', 't', 't', 'r', '2' // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 0 ) );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 1 ) );
        assertEquals( "t", substringFilter.getFinalSubstrings() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x6A, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x5B ), decodedPdu.substring( 0, 0x5B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=t*)
     */
    @Test
    public void testDecodeSearchRequestSubstringInitialAnyAny()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x67 );
        stream.put( new byte[]
            { 0x30, 0x65, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x60, // CHOICE { ..., searchRequest SearchRequest, ...
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
                ( byte ) 0xA4, 0x15, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                0x04, 0x0B, // type AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 0x30, 0x06, ( byte ) 0x80, 0x01, 't', //
                ( byte ) 0x81, 0x01, '*', //
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( "t", substringFilter.getInitialSubstrings() );
        assertEquals( "*", substringFilter.getAnySubstrings().get( 0 ) );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x67, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x58 ), decodedPdu.substring( 0, 0x58 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=*t*t*t*)
     */
    @Test
    public void testDecodeSearchRequestSubstringAnyAnyAny()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x6A );
        stream.put( new byte[]
            { 0x30, 0x68, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x63, // CHOICE { ..., searchRequest SearchRequest, ...
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
                ( byte ) 0xA4, 0x18, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                0x04, 0x0B, // type AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 0x30, 0x09, ( byte ) 0x81, 0x01, 't', //
                ( byte ) 0x81, 0x01, 't', //
                ( byte ) 0x81, 0x01, 't', //
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 0 ) );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 1 ) );
        assertEquals( "t", substringFilter.getAnySubstrings().get( 2 ) );
        assertEquals( null, substringFilter.getFinalSubstrings() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x6A, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x5B ), decodedPdu.substring( 0, 0x5B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with a substring filter. Test the
     * initial filter : (objectclass=*t*t*t*)
     */
    @Test
    public void testDecodeSearchRequestSubstringFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x67 );
        stream.put( new byte[]
            { 0x30, 0x65, // LDAPMessage ::=SEQUENCE {
                0x02, 0x01, 0x01, // messageID MessageID
                0x63, 0x60, // CHOICE { ..., searchRequest SearchRequest, ...
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
                ( byte ) 0xA4, 0x15, // Filter ::= CHOICE {
                // substrings [4] SubstringFilter
                // }
                // SubstringFilter ::= SEQUENCE {
                0x04, 0x0B, // type AttributeDescription,
                'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's', 0x30, 0x06, ( byte ) 0x82, 0x04, 'A', 'm', 'o',
                's', //
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

        LdapMessageCodec message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequestCodec sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( SearchScope.ONELEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( 0, substringFilter.getAnySubstrings().size() );
        assertEquals( "Amos", substringFilter.getFinalSubstrings() );

        // The attributes
        List<EntryAttribute> attributes = sr.getAttributes();

        for ( EntryAttribute attribute:attributes )
        {
            assertNotNull( attribute );
        }

        // Check the length
        assertEquals( 0x67, message.computeLength() );

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu.substring( 0, 0x5B ), decodedPdu.substring( 0, 0x5B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SearchRequest with an empty Substring filter
     */
    @Test
    public void testDecodeSearchRequestEmptySubstringFilter()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x3B, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x36, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA4, 0x00, 
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
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterEmptyType()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x3D, 
              0x02, 0x01, 0x04, // messageID
              0x63, 0x38, 
                0x04, 0x1F, // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA4, 0x02, 
                  0x04, 0x00, 
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
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterNoSubstrings()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x41, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x3D, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                  0x0A, 0x01, 0x01, 
                  0x0A, 0x01, 0x03, 
                  0x02, 0x01, 0x00, 
                  0x02, 0x01, 0x00, 
                  0x01, 0x01, ( byte ) 0xFF, 
                  ( byte ) 0xA4, 0x06, 0x04, 
                    0x04, 't', 'e', 's', 't',
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
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterEmptySubstrings()
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
                ( byte ) 0xA4, 0x08, 
                  0x04, 0x04, 't', 'e', 's', 't',
                  0x30, 0x00, 
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
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring Initial
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterEmptyInitial()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x45, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x40, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA4, 0x0A, 
                  0x04, 0x04, 't', 'e', 's', 't',
                  0x30, 0x02, 
                    ( byte ) 0x80, 0x00, 
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
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring Any
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterEmptyAny()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x45, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x40, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA4, 0x0A, 
                  0x04, 0x04, 't', 'e', 's', 't',
                  0x30, 0x02, 
                    ( byte ) 0x81, 0x00, 
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
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring Initial
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterEmptyFinal()
    {
        byte[] asn1BER = new byte[]
            { 
            0x30, 0x45, 
              0x02, 0x01, 0x04,                 // messageID
              0x63, 0x40, 
                0x04, 0x1F,                     // baseObject LDAPDN,
                  'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                  'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                0x0A, 0x01, 0x01, 
                0x0A, 0x01, 0x03, 
                0x02, 0x01, 0x00, 
                0x02, 0x01, 0x00, 
                0x01, 0x01, ( byte ) 0xFF, 
                ( byte ) 0xA4, 0x0A, 
                  0x04, 0x04, 't', 'e', 's', 't',
                  0x30, 0x02, 
                    ( byte ) 0x82, 0x00, 
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
     * Test the decoding of a SearchRequest with a Substring filter Any before
     * initial
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterAnyInitial()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x49, 0x02, 0x01, 0x04, // messageID
                0x63, 0x44, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA4, 0x0E, 0x04, 0x04, 't', 'e', 's', 't',
                0x30, 0x06, ( byte ) 0x81, 0x01, 'a', ( byte ) 0x80, 0x01, 'b', 0x30, 0x02, // AttributeDescriptionList
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
     * Test the decoding of a SearchRequest with a Substring filter Final before
     * initial
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterFinalInitial()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x49, 0x02, 0x01, 0x04, // messageID
                0x63, 0x44, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA4, 0x0E, 0x04, 0x04, 't', 'e', 's', 't',
                0x30, 0x06, ( byte ) 0x82, 0x01, 'a', ( byte ) 0x80, 0x01, 'b', 0x30, 0x02, // AttributeDescriptionList
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
     * Test the decoding of a SearchRequest with a Substring filter Final before
     * any
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterFinalAny()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x49, 0x02, 0x01, 0x04, // messageID
                0x63, 0x44, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA4, 0x0E, 0x04, 0x04, 't', 'e', 's', 't',
                0x30, 0x06, ( byte ) 0x82, 0x01, 'a', ( byte ) 0x81, 0x01, 'b', 0x30, 0x02, // AttributeDescriptionList
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
     * Test the decoding of a SearchRequest with a Substring filter Two initials
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterTwoInitials()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x49, 0x02, 0x01, 0x04, // messageID
                0x63, 0x44, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA4, 0x0E, 0x04, 0x04, 't', 'e', 's', 't',
                0x30, 0x06, ( byte ) 0x80, 0x01, 'a', ( byte ) 0x80, 0x01, 'b', 0x30, 0x02, // AttributeDescriptionList
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
     * Test the decoding of a SearchRequest with a Substring filter Two finals
     */
    @Test
    public void testDecodeSearchRequestSubstringFilterTwoFinals()
    {
        byte[] asn1BER = new byte[]
            { 0x30, 0x49, 0x02, 0x01, 0x04, // messageID
                0x63, 0x44, 0x04, 0x1F, // baseObject LDAPDN,
                'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 0x0A, 0x01, 0x01, 0x0A, 0x01, 0x03, 0x02, 0x01,
                0x00, 0x02, 0x01, 0x00, 0x01, 0x01, ( byte ) 0xFF, ( byte ) 0xA4, 0x0E, 0x04, 0x04, 't', 'e', 's', 't',
                0x30, 0x06, ( byte ) 0x82, 0x01, 'a', ( byte ) 0x82, 0x01, 'b', 0x30, 0x02, // AttributeDescriptionList
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
}
