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

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.Control;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.search.SearchRequest;
import org.apache.directory.shared.ldap.codec.search.SubstringFilter;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;


/**
 * A test case for SearchRequest messages
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestSubstringTest extends TestCase
{
    static Map oids = new HashMap();

    protected void setUp() throws Exception
    {
        super.setUp();

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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType().toString() );
        assertEquals( "t", substringFilter.getInitialSubstrings().toString() );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType().toString() );
        assertEquals( "t", substringFilter.getInitialSubstrings().toString() );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
        }

        // Check the Control
        List controls = message.getControls();

        assertEquals( 1, controls.size() );

        Control control = message.getControls( 0 );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType().toString() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 0 ) ) );
        assertEquals( null, substringFilter.getFinalSubstrings() );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType().toString() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 0 ) ) );
        assertEquals( "t", substringFilter.getFinalSubstrings().toString() );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType().toString() );
        assertEquals( "t", substringFilter.getInitialSubstrings().toString() );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 0 ) ) );
        assertEquals( "t", substringFilter.getFinalSubstrings().toString() );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType().toString() );
        assertEquals( "t", substringFilter.getInitialSubstrings().toString() );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 0 ) ) );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType().toString() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 0 ) ) );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 1 ) ) );
        assertEquals( "t", substringFilter.getFinalSubstrings().toString() );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType().toString() );
        assertEquals( "t", substringFilter.getInitialSubstrings().toString() );
        assertEquals( "*", ( ( String ) substringFilter.getAnySubstrings().get( 0 ) ) );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );

        // (objectclass=t*)
        SubstringFilter substringFilter = ( SubstringFilter ) sr.getFilter();
        assertNotNull( substringFilter );

        assertEquals( "objectclass", substringFilter.getType() );
        assertEquals( null, substringFilter.getInitialSubstrings() );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 0 ) ) );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 1 ) ) );
        assertEquals( "t", ( ( String ) substringFilter.getAnySubstrings().get( 2 ) ) );
        assertEquals( null, substringFilter.getFinalSubstrings() );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
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
        assertEquals( "Amos", substringFilter.getFinalSubstrings().toString() );

        // The attributes
        Attributes attributes = sr.getAttributes();

        for ( int i = 0; i < attributes.size(); i++ )
        {
            assertNotNull( attributes.get( "attr" + i ) );
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring Initial
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring Any
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter and an empty
     * Substring Initial
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter Any before
     * initial
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter Final before
     * initial
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter Final before
     * any
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter Two initials
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }


    /**
     * Test the decoding of a SearchRequest with a Substring filter Two finals
     */
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
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        fail( "We should not reach this point" );
    }
}
