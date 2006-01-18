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
import java.util.ArrayList;
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
import org.apache.ldap.common.codec.AttributeValueAssertion;
import org.apache.ldap.common.codec.Control;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.search.AndFilter;
import org.apache.ldap.common.codec.search.AttributeValueAssertionFilter;
import org.apache.ldap.common.codec.search.NotFilter;
import org.apache.ldap.common.codec.search.OrFilter;
import org.apache.ldap.common.codec.search.PresentFilter;
import org.apache.ldap.common.codec.search.SearchRequest;
import org.apache.ldap.common.codec.search.SubstringFilter;
import org.apache.ldap.common.codec.search.controls.SubEntryControl;
import org.apache.ldap.common.codec.util.LdapString;

import junit.framework.TestCase;

/**
 * A test case for SearchRequest messages
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestTest extends TestCase {
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
     * Test the decoding of a SearchRequest with no controls.
     * The search filter is : 
     * (&(|(objectclass=top)(ou=contacts))(!(objectclass=ttt)))
     */
    public void testDecodeSearchRequestGlobalNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x90 );
        stream.put(
            new byte[]
            {
                0x30, (byte)0x81, (byte)0x8D, // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, (byte)0x81, (byte)0x87,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,   	  //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA0, 0x3C,        	  // Filter ::= CHOICE {
				                         	  //    and             [0] SET OF Filter,
				(byte)0xA1, 0x24,        	  //    or              [1] SET of Filter,
				(byte)0xA3, 0x12,        	  //    equalityMatch   [3] AttributeValueAssertion,
									     	  // AttributeValueAssertion ::= SEQUENCE {
								 	          //    attributeDesc   AttributeDescription (LDAPString),
				0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
									          //    assertionValue  AssertionValue (OCTET STRING) }
				0x04, 0x03, 't', 'o', 'p',
				(byte)0xA3, 0x0E,             //    equalityMatch   [3] AttributeValueAssertion,
				                              // AttributeValueAssertion ::= SEQUENCE {
				0x04, 0x02, 'o', 'u',         //    attributeDesc   AttributeDescription (LDAPString),
			                                  //    assertionValue  AssertionValue (OCTET STRING) }
				0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's',
				(byte)0xA2, 0x14,             //    not             [2] Filter,
				(byte)0xA3, 0x12,             //    equalityMatch   [3] AttributeValueAssertion,
			                                  // AttributeValueAssertion ::= SEQUENCE {
		 	                                  //    attributeDesc   AttributeDescription (LDAPString),
                0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
			                                  //    assertionValue  AssertionValue (OCTET STRING) }
                0x04, 0x03, 't', 't', 't',
              						          //    attributes      AttributeDescriptionList }
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (& (...
        AndFilter andFilter = (AndFilter)sr.getFilter();
        assertNotNull(andFilter);
        
        ArrayList andFilters = andFilter.getAndFilter();
        
        // (& (| (...
        assertEquals(2, andFilters.size());
        OrFilter orFilter = (OrFilter)andFilters.get(0);
        assertNotNull(orFilter);
        
        // (& (| (obectclass=top) (...
        ArrayList orFilters = orFilter.getOrFilter();
        assertEquals(2, orFilters.size());
        AttributeValueAssertionFilter equalityMatch = (AttributeValueAssertionFilter)orFilters.get(0);  
        assertNotNull(equalityMatch);
        
        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("objectclass", assertion.getAttributeDesc().toString());
        assertEquals("top", assertion.getAssertionValue().toString());
        
        // (& (| (objectclass=top) (ou=contacts) ) (...
        equalityMatch = (AttributeValueAssertionFilter)orFilters.get(1);  
        assertNotNull(equalityMatch);
        
        assertion = equalityMatch.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("ou", assertion.getAttributeDesc().toString());
        assertEquals("contacts", assertion.getAssertionValue().toString());
        
        // (& (| (objectclass=top) (ou=contacts) ) (! ...
        NotFilter notFilter = ( NotFilter )andFilters.get(1);
        assertNotNull(notFilter);
        
        // (& (| (objectclass=top) (ou=contacts) ) (! (objectclass=ttt) ) )
        equalityMatch = (AttributeValueAssertionFilter)notFilter.getNotFilter();  
        assertNotNull(equalityMatch);
        
        assertion = equalityMatch.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("objectclass", assertion.getAttributeDesc().toString());
        assertEquals("ttt", assertion.getAssertionValue().toString());
        
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x81 ), decodedPdu.substring( 0, 0x81 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with no controls.
     * Test the various types of filter : >=, <=, ~=
     * The search filter is : 
     * (&(|(objectclass~=top)(ou<=contacts))(!(objectclass>=ttt)))
     */
    public void testDecodeSearchRequestCompareFiltersNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x90 );
        stream.put(
            new byte[]
            {
                0x30, (byte)0x81, (byte)0x8D, // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, (byte)0x81, (byte)0x87,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA0, 0x3C,        	  // Filter ::= CHOICE {
				                         	  //    and             [0] SET OF Filter,
				(byte)0xA1, 0x24,        	  //    or              [1] SET of Filter,
				(byte)0xA8, 0x12,        	  //    approxMatch     [8] AttributeValueAssertion,
									     	  // AttributeValueAssertion ::= SEQUENCE {
								 	          //    attributeDesc   AttributeDescription (LDAPString),
				0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
									          //    assertionValue  AssertionValue (OCTET STRING) }
				0x04, 0x03, 't', 'o', 'p',
				(byte)0xA6, 0x0E,             //    lessOrEqual     [3] AttributeValueAssertion,
				                              // AttributeValueAssertion ::= SEQUENCE {
				0x04, 0x02, 'o', 'u',         //    attributeDesc   AttributeDescription (LDAPString),
			                                  //    assertionValue  AssertionValue (OCTET STRING) }
				0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's',
				(byte)0xA2, 0x14,             //    not             [2] Filter,
				(byte)0xA5, 0x12,             //    greaterOrEqual  [5] AttributeValueAssertion,
			                                  // AttributeValueAssertion ::= SEQUENCE {
		 	                                  //    attributeDesc   AttributeDescription (LDAPString),
                0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
			                                  //    assertionValue  AssertionValue (OCTET STRING) }
                0x04, 0x03, 't', 't', 't',
              						          //    attributes      AttributeDescriptionList }
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (& (...
        AndFilter andFilter = (AndFilter)sr.getFilter();
        assertNotNull(andFilter);
        
        ArrayList andFilters = andFilter.getAndFilter();
        
        // (& (| (...
        assertEquals(2, andFilters.size());
        OrFilter orFilter = (OrFilter)andFilters.get(0);
        assertNotNull(orFilter);
        
        // (& (| (objectclass~=top) (...
        ArrayList orFilters = orFilter.getOrFilter();
        assertEquals(2, orFilters.size());
        AttributeValueAssertionFilter approxMatch = (AttributeValueAssertionFilter)orFilters.get(0);  
        assertNotNull(approxMatch);
        
        AttributeValueAssertion assertion = approxMatch.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("objectclass", assertion.getAttributeDesc().toString());
        assertEquals("top", assertion.getAssertionValue().toString());
        
        // (& (| (objectclass~=top) (ou<=contacts) ) (...
        AttributeValueAssertionFilter lessOrEqual = (AttributeValueAssertionFilter)orFilters.get(1);  
        assertNotNull(lessOrEqual);
        
        assertion = lessOrEqual.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("ou", assertion.getAttributeDesc().toString());
        assertEquals("contacts", assertion.getAssertionValue().toString());
        
        // (& (| (objectclass~=top) (ou<=contacts) ) (! ...
        NotFilter notFilter = (NotFilter)andFilters.get(1);
        assertNotNull(notFilter);
        
        // (& (| (objectclass~=top) (ou<=contacts) ) (! (objectclass>=ttt) ) )
        AttributeValueAssertionFilter greaterOrEqual = (AttributeValueAssertionFilter)notFilter.getNotFilter();  
        assertNotNull(greaterOrEqual);
        
        assertion = greaterOrEqual.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("objectclass", assertion.getAttributeDesc().toString());
        assertEquals("ttt", assertion.getAssertionValue().toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x90, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x81 ), decodedPdu.substring( 0, 0x81 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with no controls.
     * Test the present filter : =*
     * The search filter is : 
     * (&(|(objectclass=*)(ou=*))(!(objectclass>=ttt)))
     */
    public void testDecodeSearchRequestPresentNoControls()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x7B );
        stream.put(
            new byte[]
            {
                0x30, 0x79,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, 0x74,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA0, 0x29,        	  // Filter ::= CHOICE {
				                         	  //    and             [0] SET OF Filter,
				(byte)0xA1, 0x11,        	  //    or              [1] SET of Filter,
				(byte)0x87, 0x0B,        	  //    present         [7] AttributeDescription,
									     	  // AttributeDescription ::= LDAPString
				'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
									          //    assertionValue  AssertionValue (OCTET STRING) }
				(byte)0x87, 0x02, 'o', 'u',   //    present         [7] AttributeDescription,
				                              // AttributeDescription ::= LDAPString
				(byte)0xA2, 0x14,             //    not             [2] Filter,
				(byte)0xA5, 0x12,             //    greaterOrEqual  [5] AttributeValueAssertion,
			                                  // AttributeValueAssertion ::= SEQUENCE {
		 	                                  //    attributeDesc   AttributeDescription (LDAPString),
                0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
			                                  //    assertionValue  AssertionValue (OCTET STRING) }
                0x04, 0x03, 't', 't', 't',
              						          //    attributes      AttributeDescriptionList }
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (& (...
        AndFilter andFilter = (AndFilter)sr.getFilter();
        assertNotNull(andFilter);
        
        ArrayList andFilters = andFilter.getAndFilter();
        
        // (& (| (...
        assertEquals(2, andFilters.size());
        OrFilter orFilter = (OrFilter)andFilters.get(0);
        assertNotNull(orFilter);
        
        // (& (| (objectclass=*) (...
        ArrayList orFilters = orFilter.getOrFilter();
        assertEquals(2, orFilters.size());

        PresentFilter presentFilter = (PresentFilter)orFilters.get(0);  
        assertNotNull(presentFilter);
        
        assertEquals("objectclass", presentFilter.getAttributeDescription().toString());
        
        // (& (| (objectclass=*) (ou=*) ) (...
        presentFilter = (PresentFilter)orFilters.get(1);  
        assertNotNull(presentFilter);
        
        assertEquals("ou", presentFilter.getAttributeDescription().toString());
        
        // (& (| (objectclass=*) (ou=*) ) (! ...
        NotFilter notFilter = (NotFilter)andFilters.get(1);
        assertNotNull(notFilter);
        
        // (& (| (objectclass=*) (ou=*) ) (! (objectclass>=ttt) ) )
        AttributeValueAssertionFilter greaterOrEqual = (AttributeValueAssertionFilter)notFilter.getNotFilter();  
        assertNotNull(greaterOrEqual);
        
        AttributeValueAssertion assertion = greaterOrEqual.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("objectclass", assertion.getAttributeDesc().toString());
        assertEquals("ttt", assertion.getAssertionValue().toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x7B, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x6C ), decodedPdu.substring( 0, 0x6C ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with no attributes.
     * The search filter is : 
     * (objectclass=*)
     */
    public void testDecodeSearchRequestNoAttributes()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x40 );
        stream.put(
            new byte[]
            {
                0x30, 0x37,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x03, 	          //        messageID MessageID
				0x63, 0x32,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x12, 		     	  //    baseObject LDAPDN,
				'o', 'u', '=', 'u', 's', 'e', 'r', 's', ',',  
				'o', 'u', '=', 's', 'y', 's', 't', 'e', 'm', 
				0x0A, 0x01, 0x00,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (infinite)
				0x02, 0x01, 0x00, 
                					     	  //    timeLimit INTEGER (0 .. maxInt), (infinite)
				0x02, 0x01, 0x00,
				0x01, 0x01, (byte)0x00,       //    typesOnly BOOLEAN, (FALSE)
				                         	  //    filter    Filter,
											  // Filter ::= CHOICE {
				(byte)0x87, 0x0B,             //    present         [7] AttributeDescription,
				'o', 'b', 'j', 'e', 'c', 't', 'C', 'l', 'a', 's', 's',
              						          //    attributes      AttributeDescriptionList }
                0x30, 0x00,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x00, 0x00,					  // Some trailing 00, useless.
                0x00, 0x00,
                0x00, 0x00
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

        assertEquals( 3, message.getMessageId() );
        assertEquals( "ou=users,ou=system", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_BASE_OBJECT, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );
        
        // (objectClass = *)
        PresentFilter presentFilter = (PresentFilter)sr.getFilter();
        assertNotNull(presentFilter);
        assertEquals("objectClass", presentFilter.getAttributeDescription().toString());
        
        // The attributes
        Attributes attributes = sr.getAttributes();
        
       	assertNull( attributes );

        // Check the length
        assertEquals(0x39, message.computeLength());

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu, decodedPdu.substring( 0, decodedPdu.length() - 35) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a substring filter.
     * Test the initial filter : 
     * (objectclass=t*)
     */
    public void testDecodeSearchRequestSubstringInitialAny()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x64 );
        stream.put(
            new byte[]
            {
                0x30, 0x62,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, 0x5D,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA4, 0x12,        	  // Filter ::= CHOICE {
				                         	  //    substrings      [4] SubstringFilter
											  // }
									     	  // SubstringFilter ::= SEQUENCE {
				0x04, 0x0B,                   //     type            AttributeDescription,
				'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
				0x30, 0x03,
				(byte)0x80, 0x01, 't',        //
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        SubstringFilter substringFilter = (SubstringFilter)sr.getFilter();
        assertNotNull(substringFilter);
        
        assertEquals("objectclass", substringFilter.getType().toString());
        assertEquals("t", substringFilter.getInitialSubstrings().toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }

        // Check the length
        assertEquals(0x64, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x53 ), decodedPdu.substring( 0, 0x53 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a substring filter.
     * Test the initial filter : 
     * (objectclass=t*)
     */
    public void testDecodeSearchRequestSubstringAny()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x64 );
        stream.put(
            new byte[]
            {
                0x30, 0x62,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, 0x5D,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA4, 0x12,        	  // Filter ::= CHOICE {
				                         	  //    substrings      [4] SubstringFilter
											  // }
									     	  // SubstringFilter ::= SEQUENCE {
				0x04, 0x0B,                   //     type            AttributeDescription,
				'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
				0x30, 0x03,
				(byte)0x81, 0x01, 't',        //
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        SubstringFilter substringFilter = (SubstringFilter)sr.getFilter();
        assertNotNull(substringFilter);
        
        assertEquals("objectclass", substringFilter.getType().toString());
        assertEquals(null, substringFilter.getInitialSubstrings());
        assertEquals("t", ((LdapString)substringFilter.getAnySubstrings().get(0)).toString());
        assertEquals(null, substringFilter.getFinalSubstrings());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }

        // Check the length
        assertEquals(0x64, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x53 ), decodedPdu.substring( 0, 0x53 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a substring filter.
     * Test the initial filter : 
     * (objectclass=t*)
     */
    public void testDecodeSearchRequestSubstringAnyFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x67 );
        stream.put(
            new byte[]
            {
                0x30, 0x65,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, 0x60,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA4, 0x15,        	  // Filter ::= CHOICE {
				                         	  //    substrings      [4] SubstringFilter
											  // }
									     	  // SubstringFilter ::= SEQUENCE {
				0x04, 0x0B,                   //     type            AttributeDescription,
				'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
				0x30, 0x06,
				(byte)0x81, 0x01, 't',        //
				(byte)0x82, 0x01, 't',        //
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        SubstringFilter substringFilter = (SubstringFilter)sr.getFilter();
        assertNotNull(substringFilter);
        
        assertEquals("objectclass", substringFilter.getType().toString());
        assertEquals(null, substringFilter.getInitialSubstrings());
        assertEquals("t", ((LdapString)substringFilter.getAnySubstrings().get(0)).toString());
        assertEquals("t", substringFilter.getFinalSubstrings().toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x67, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x58 ), decodedPdu.substring( 0, 0x58 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a substring filter.
     * Test the initial filter : 
     * (objectclass=t*)
     */
    public void testDecodeSearchRequestSubstringInitialAnyFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x6A );
        stream.put(
            new byte[]
            {
                0x30, 0x68,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, 0x63,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA4, 0x18,        	  // Filter ::= CHOICE {
				                         	  //    substrings      [4] SubstringFilter
											  // }
									     	  // SubstringFilter ::= SEQUENCE {
				0x04, 0x0B,                   //     type            AttributeDescription,
				'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
				0x30, 0x09,
				(byte)0x80, 0x01, 't',        //
				(byte)0x81, 0x01, 't',        //
				(byte)0x82, 0x01, 't',        //
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        SubstringFilter substringFilter = (SubstringFilter)sr.getFilter();
        assertNotNull(substringFilter);
        
        assertEquals("objectclass", substringFilter.getType().toString());
        assertEquals("t", substringFilter.getInitialSubstrings().toString());
        assertEquals("t", ((LdapString)substringFilter.getAnySubstrings().get(0)).toString());
        assertEquals("t", substringFilter.getFinalSubstrings().toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x6A, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x5B ), decodedPdu.substring( 0, 0x5B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a substring filter.
     * Test the initial filter : 
     * (objectclass=t*t*)
     */
    public void testDecodeSearchRequestSubstringInitialAnyAnyFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x67 );
        stream.put(
            new byte[]
            {
                0x30, 0x65,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, 0x60,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA4, 0x15,        	  // Filter ::= CHOICE {
				                         	  //    substrings      [4] SubstringFilter
											  // }
									     	  // SubstringFilter ::= SEQUENCE {
				0x04, 0x0B,                   //     type            AttributeDescription,
				'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
				0x30, 0x06,
				(byte)0x80, 0x01, 't',        //
				(byte)0x81, 0x01, 't',        //
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        SubstringFilter substringFilter = (SubstringFilter)sr.getFilter();
        assertNotNull(substringFilter);
        
        assertEquals("objectclass", substringFilter.getType().toString());
        assertEquals("t", substringFilter.getInitialSubstrings().toString());
        assertEquals("t", ((LdapString)substringFilter.getAnySubstrings().get(0)).toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x67, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x58 ), decodedPdu.substring( 0, 0x58 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a substring filter.
     * Test the initial filter : 
     * (objectclass=t*)
     */
    public void testDecodeSearchRequestSubstringAnyAnyFinal()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x6A );
        stream.put(
            new byte[]
            {
                0x30, 0x68,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, 0x63,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA4, 0x18,        	  // Filter ::= CHOICE {
				                         	  //    substrings      [4] SubstringFilter
											  // }
									     	  // SubstringFilter ::= SEQUENCE {
				0x04, 0x0B,                   //     type            AttributeDescription,
				'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
				0x30, 0x09,
				(byte)0x81, 0x01, 't',        //
				(byte)0x81, 0x01, 't',        //
				(byte)0x82, 0x01, 't',        //
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        SubstringFilter substringFilter = (SubstringFilter)sr.getFilter();
        assertNotNull(substringFilter);
        
        assertEquals("objectclass", substringFilter.getType().toString());
        assertEquals(null, substringFilter.getInitialSubstrings());
        assertEquals("t", ((LdapString)substringFilter.getAnySubstrings().get(0)).toString());
        assertEquals("t", ((LdapString)substringFilter.getAnySubstrings().get(1)).toString());
        assertEquals("t", substringFilter.getFinalSubstrings().toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x6A, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x5B ), decodedPdu.substring( 0, 0x5B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a substring filter.
     * Test the initial filter : 
     * (objectclass=t*)
     */
    public void testDecodeSearchRequestSubstringInitialAnyAny()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x67 );
        stream.put(
            new byte[]
            {
                0x30, 0x65,                   // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, 0x60,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,       //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA4, 0x15,        	  // Filter ::= CHOICE {
				                         	  //    substrings      [4] SubstringFilter
											  // }
									     	  // SubstringFilter ::= SEQUENCE {
				0x04, 0x0B,                   //     type            AttributeDescription,
				'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
				0x30, 0x06,
				(byte)0x80, 0x01, 't',        //
				(byte)0x81, 0x01, '*',        //
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        SubstringFilter substringFilter = (SubstringFilter)sr.getFilter();
        assertNotNull(substringFilter);
        
        assertEquals("objectclass", substringFilter.getType().toString());
        assertEquals("t", substringFilter.getInitialSubstrings().toString());
        assertEquals("*", ((LdapString)substringFilter.getAnySubstrings().get(0)).toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x67, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x58 ), decodedPdu.substring( 0, 0x58 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Test the decoding of a SearchRequest with a substring filter.
     * Test the initial filter : 
     * (objectclass=*t*t*t*)
     */
    public void testDecodeSearchRequestSubstringAnyAnyAny()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x6A );
        stream.put(
            new byte[]
            {
                0x30, 0x68,                   // LDAPMessage ::=SEQUENCE {
				  0x02, 0x01, 0x01, 	          //        messageID MessageID
				  0x63, 0x63,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				    0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				    'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                    'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				    0x0A, 0x01, 0x01,         //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				    0x0A, 0x01, 0x03,         //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				    0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				    0x02, 0x02, 0x03, (byte)0xE8,
				    0x01, 0x01, (byte)0xFF,   //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				    (byte)0xA4, 0x18,         // Filter ::= CHOICE {
				                         	  //    substrings      [4] SubstringFilter
											  // }
									     	  // SubstringFilter ::= SEQUENCE {
				      0x04, 0x0B,             //     type            AttributeDescription,
				      'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
				      0x30, 0x09,
				        (byte)0x81, 0x01, 't',    //
				        (byte)0x81, 0x01, 't',    //
				        (byte)0x81, 0x01, 't',    //
                    0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                      0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                      0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                      0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        SubstringFilter substringFilter = (SubstringFilter)sr.getFilter();
        assertNotNull(substringFilter);
        
        assertEquals("objectclass", substringFilter.getType().getString());
        assertEquals(null, substringFilter.getInitialSubstrings());
        assertEquals("t", ((LdapString)substringFilter.getAnySubstrings().get(0)).getString());
        assertEquals("t", ((LdapString)substringFilter.getAnySubstrings().get(1)).getString());
        assertEquals("t", (( LdapString )substringFilter.getAnySubstrings().get(2)).getString());
        assertEquals(null, substringFilter.getFinalSubstrings());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x6A, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x5B ), decodedPdu.substring( 0, 0x5B ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Tests an search request decode with a simple equality match filter.
     */
    public void testDecodeSearchRequestOrFilters()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x96 );
        stream.put(
            new byte[]
            {
                    0x30, 0xFFFFFF81, 0xFFFFFF93, 
                	0x02, 0x01, 0x21, 
                	0x63, 0xFFFFFF81, 0xFFFFFF8D, // "dc=example,dc=com"
                		0x04, 0x11, 0x64, 0x63, 0x3D, 0x65, 0x78, 0x61, 0x6D, 0x70, 0x6C, 0x65, 0x2C, 0x64, 0x63, 0x3D, 0x63, 0x6F, 0x6D, 
                		0x0A, 0x01, 0x00, 
                		0x0A, 0x01, 0x02, 
                		0x02, 0x01, 0x02, 
                		0x02, 0x01, 0x03, 
                		0x01, 0x01, 0xFFFFFFFF, 
                		0xFFFFFFA1, 0x52, 								// ( | 
                			0xFFFFFFA3, 0x10, 							// ( uid=akarasulu )
                				0x04, 0x03, 0x75, 0x69, 0x64, 			
                				0x04, 0x09, 0x61, 0x6B, 0x61, 0x72, 0x61, 0x73, 0x75, 0x6C, 0x75, 
                			0xFFFFFFA3, 0x09, 							// ( cn=aok )
                				0x04, 0x02, 0x63, 0x6E, 
                				0x04, 0x03, 0x61, 0x6F, 0x6B, 
                			0xFFFFFFA3, 0x15, 							// ( ou = Human Resources )
                				0x04, 0x02, 0x6F, 0x75, 
                				0x04, 0x0F, 0x48, 0x75, 0x6D, 0x61, 0x6E, 0x20, 0x52, 0x65, 0x73, 0x6F, 0x75, 0x72, 0x63, 0x65, 0x73, 
                			0xFFFFFFA3, 0x10, 
                				0x04, 0x01, 0x6C, 						// ( l=Santa Clara )
                				0x04, 0x0B, 0x53, 0x61, 0x6E, 0x74, 0x61, 0x20, 0x43, 0x6C, 0x61, 0x72, 0x61, 
                			0xFFFFFFA3, 0x0A, 							// ( cn=abok )
                				0x04, 0x02, 0x63, 0x6E, 
                				0x04, 0x04, 0x61, 0x62, 0x6F, 0x6B, 
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

        assertEquals( 33, message.getMessageId() );
        assertEquals( "dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_BASE_OBJECT, sr.getScope() );
        assertEquals( LdapConstants.DEREF_FINDING_BASE_OBJ, sr.getDerefAliases() );
        assertEquals( 2, sr.getSizeLimit() );
        assertEquals( 3, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (objectclass=t*)
        OrFilter orFilter = (OrFilter)sr.getFilter();
        assertNotNull(orFilter);

        // uid=akarasulu
        AttributeValueAssertion assertion = ((AttributeValueAssertionFilter)orFilter.getOrFilter().get(0)).getAssertion();
        
        assertEquals("uid", assertion.getAttributeDesc().toString());
        assertEquals("akarasulu", assertion.getAssertionValue().toString());

        // cn=aok
        assertion = ((AttributeValueAssertionFilter)orFilter.getOrFilter().get(1)).getAssertion();
        
        assertEquals("cn", assertion.getAttributeDesc().toString());
        assertEquals("aok", assertion.getAssertionValue().toString());

        // ou = Human Resources
        assertion = ((AttributeValueAssertionFilter)orFilter.getOrFilter().get(2)).getAssertion();
        
        assertEquals("ou", assertion.getAttributeDesc().toString());
        assertEquals("Human Resources", assertion.getAssertionValue().toString());

        // l=Santa Clara
        assertion = ((AttributeValueAssertionFilter)orFilter.getOrFilter().get(3)).getAssertion();
        
        assertEquals("l", assertion.getAttributeDesc().toString());
        assertEquals("Santa Clara", assertion.getAssertionValue().toString());

        // cn=abok
        assertion = (( AttributeValueAssertionFilter )orFilter.getOrFilter().get(4)).getAssertion();
        
        assertEquals("cn", assertion.getAttributeDesc().toString());
        assertEquals("abok", assertion.getAssertionValue().toString());

        // The attributes
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }
        
        // Check the length
        assertEquals(0x96, message.computeLength());

        // Check the encoding
        // We won't check the whole PDU, as it may differs because
        // attributes may have been reordered
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            assertEquals(encodedPdu.substring( 0, 0x87 ), decodedPdu.substring( 0, 0x87 ) );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    /**
     * Tests an search request decode with a simple equality match filter.
     */
    public void testDecodeSearchRequestExtensibleMatch()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x65 );
        stream.put(
            new byte[]
            {
                    0x30, 0x63, 
                	0x02, 0x01, 0x01, 
                	0x63, 0x5E, // "dc=example,dc=com"
                		0x04, 0x11, 0x64, 0x63, 0x3D, 0x65, 0x78, 0x61, 0x6D, 0x70, 0x6C, 0x65, 0x2C, 0x64, 0x63, 0x3D, 0x63, 0x6F, 0x6D, 
                		0x0A, 0x01, 0x00, 
                		0x0A, 0x01, 0x02, 
                		0x02, 0x01, 0x02, 
                		0x02, 0x01, 0x03, 
                		0x01, 0x01, (byte)0xFF,
                		(byte)0xA9, 0x23,
                			0x30, 0x21,
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
        assertEquals(0x65, message.computeLength());

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
    * Test the decoding of a SearchRequest with controls.
    */
    public void testDecodeSearchRequestWithControls()
    {
       byte[] asn1BER = new byte[]
       {
           0x30, 0x7f,
               0x02, 0x01, 0x04, // messageID
               0x63, 0x33,
                   0x04, 0x13,
                       0x64, 0x63, 0x3d, 0x6d, 0x79, 0x2d, 0x64, 0x6f, 0x6d, 0x61,
                               0x69, 0x6e, 0x2c, 0x64, 0x63, 0x3d, 0x63, 0x6f, 0x6d, // baseObject: dc=my-domain,dc=com
                   0x0a, 0x01, 0x02, // scope: subtree
                   0x0a, 0x01, 0x03, // derefAliases: derefAlways
                   0x02, 0x01, 0x00, // sizeLimit: 0
                   0x02, 0x01, 0x00, // timeLimit: 0
                   0x01, 0x01, 0x00, // typesOnly: false
                   (byte)0x87, 0x0b,
                       0x6f, 0x62, 0x6a, 0x65, 0x63, 0x74, 0x43, 0x6c, 0x61, 0x73, 0x73, // filter: (objectClass=*)
                   0x30, 0x00,
               (byte)0xa0, 0x45, // controls
                   0x30, 0x28,
                       0x04, 0x16,
                           0x31, 0x2e, 0x32, 0x2e, 0x38, 0x34, 0x30, 0x2e, 0x31, 0x31,
                                   0x33, 0x35, 0x35, 0x36, 0x2e, 0x31, 0x2e, 0x34, 0x2e, 0x33,
                                   0x31, 0x39, // control oid: 1.2.840.113556.1.4.319
                       0x01, 0x01, (byte)0xff, // criticality: false
                       0x04, 0x0b,
                           0x30, 0x09, 0x02, 0x01, 0x02, 0x04, 0x04, 0x47, 0x00, 0x00, 0x00, // value: pageSize=2
                   0x30, 0x19,
                       0x04, 0x17,
                           0x32, 0x2e, 0x31, 0x36, 0x2e, 0x38, 0x34, 0x30, 0x2e, 0x31,
                                   0x2e, 0x31, 0x31, 0x33, 0x37, 0x33, 0x30, 0x2e, 0x33, 0x2e,
                                   0x34, 0x2e, 0x32 // control oid: 2.16.840.1.113730.3.4.2
       };
    
       Asn1Decoder ldapDecoder = new LdapDecoder();
    
       ByteBuffer  stream      = ByteBuffer.allocate( asn1BER.length );
       stream.put( asn1BER );
       String decodedPdu       = StringTools.dumpBytes( stream.array() );
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
    
       LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
       assertEquals( 4, message.getMessageId() );
       assertEquals( 2, message.getControls().size() );
    
       // this is a constant in Java 5 API
       String pagedResultsControlOID = "1.2.840.113556.1.4.319";
       Control pagedResultsControl = message.getControls( 0 );
       assertEquals( pagedResultsControlOID, pagedResultsControl.getControlType() );
       assertTrue( pagedResultsControl.getCriticality() );
    
       // this is a constant in Java 5 API
       String manageReferralControlOID = "2.16.840.1.113730.3.4.2";
       Control manageReferralControl = message.getControls( 1 );
       assertEquals( manageReferralControlOID, manageReferralControl.getControlType() );
    
       SearchRequest sr    = message.getSearchRequest();
       assertEquals( "dc=my-domain,dc=com", sr.getBaseObject() );
       assertEquals( LdapConstants.SCOPE_WHOLE_SUBTREE, sr.getScope() );
       assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
       assertEquals( 0, sr.getSizeLimit() );
       assertEquals( 0, sr.getTimeLimit() );
       assertEquals( false, sr.isTypesOnly() );
    
       assertTrue( sr.getFilter() instanceof PresentFilter );
       assertEquals ( "objectClass",
           ( (PresentFilter) sr.getFilter() ).getAttributeDescription().getString());
    
       // Check the encoding
       try
       {
           ByteBuffer bb = message.encode( null );
           String encodedPdu = StringTools.dumpBytes( bb.array() );
           assertEquals(encodedPdu, decodedPdu );
       }
       catch ( EncoderException ee )
       {
           ee.printStackTrace();
           fail( ee.getMessage() );
       }
    }    

    /**
     * Test the decoding of a SearchRequest with no controls but with
     * oid attributes.
     * The search filter is : 
     * (&(|(objectclass=top)(2.5.4.11=contacts))(!(organizationalUnitName=ttt)))
     */
    public void testDecodeSearchRequestGlobalNoControlsOidAndAlias()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0xA1 );
        stream.put(
            new byte[]
            {
                0x30, (byte)0x81, (byte)0x9E, // LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 	          //        messageID MessageID
				0x63, (byte)0x81, (byte)0x98,                   //	      CHOICE { ..., searchRequest SearchRequest, ...
                        			     	  // SearchRequest ::= APPLICATION[3] SEQUENCE {
				0x04, 0x1F, 		     	  //    baseObject LDAPDN,
				'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=',
                'e', 'x', 'a', 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm',
				0x0A, 0x01, 0x01,        	  //    scope           ENUMERATED {
                					     	  //        baseObject              (0),
				                         	  //        singleLevel             (1),
				                         	  //        wholeSubtree            (2) },
				0x0A, 0x01, 0x03,        	  //    derefAliases    ENUMERATED {
									     	  //        neverDerefAliases       (0),
									     	  //        derefInSearching        (1),
									     	  //        derefFindingBaseObj     (2),
									     	  //        derefAlways             (3) },
				                         	  //    sizeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
                					     	  //    timeLimit INTEGER (0 .. maxInt), (1000)
				0x02, 0x02, 0x03, (byte)0xE8,
				0x01, 0x01, (byte)0xFF,   	  //    typesOnly BOOLEAN, (TRUE)
				                         	  //    filter    Filter,
				(byte)0xA0, 0x4D,        	  // Filter ::= CHOICE {
				                         	  //    and             [0] SET OF Filter,
				(byte)0xA1, 0x2A,        	  //    or              [1] SET of Filter,
				(byte)0xA3, 0x12,        	  //    equalityMatch   [3] AttributeValueAssertion,
									     	  // AttributeValueAssertion ::= SEQUENCE {
								 	          //    attributeDesc   AttributeDescription (LDAPString),
				0x04, 0x0B, 'o', 'b', 'j', 'e', 'c', 't', 'c', 'l', 'a', 's', 's',
									          //    assertionValue  AssertionValue (OCTET STRING) }
				0x04, 0x03, 't', 'o', 'p',
				(byte)0xA3, 0x14,             //    equalityMatch   [3] AttributeValueAssertion,
				                              // AttributeValueAssertion ::= SEQUENCE {
				0x04, 0x08, '2', '.', '5', '.', '4', '.', '1', '1',         //    attributeDesc   AttributeDescription (LDAPString),
			                                  //    assertionValue  AssertionValue (OCTET STRING) }
				0x04, 0x08, 'c', 'o', 'n', 't', 'a', 'c', 't', 's',
				(byte)0xA2, 0x1F,             //    not             [2] Filter,
				(byte)0xA3, 0x1D,             //    equalityMatch   [3] AttributeValueAssertion,
			                                  // AttributeValueAssertion ::= SEQUENCE {
		 	                                  //    attributeDesc   AttributeDescription (LDAPString),
                0x04, 0x16, 'o', 'r', 'g', 'a', 'n', 'i', 'z', 'a', 't', 'i', 'o', 'n', 'a', 'l', 'U', 'n', 'i', 't', 'N', 'a', 'm', 'e',
			                                  //    assertionValue  AssertionValue (OCTET STRING) }
                0x04, 0x03, 't', 't', 't',
              						          //    attributes      AttributeDescriptionList }
                0x30, 0x15,				      // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription
                0x04, 0x05, 'a', 't', 't', 'r', '0', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '1', // AttributeDescription ::= LDAPString
                0x04, 0x05, 'a', 't', 't', 'r', '2'  // AttributeDescription ::= LDAPString
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
    	
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchRequest sr      = message.getSearchRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", sr.getBaseObject().toString() );
        assertEquals( LdapConstants.SCOPE_SINGLE_LEVEL, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 1000, sr.getSizeLimit() );
        assertEquals( 1000, sr.getTimeLimit() );
        assertEquals( true, sr.isTypesOnly() );
        
        // (& (...
        AndFilter andFilter = (AndFilter)sr.getFilter();
        assertNotNull(andFilter);
        
        ArrayList andFilters = andFilter.getAndFilter();
        
        // (& (| (...
        assertEquals(2, andFilters.size());
        OrFilter orFilter = (OrFilter)andFilters.get(0);
        assertNotNull(orFilter);
        
        // (& (| (obectclass=top) (...
        ArrayList orFilters = orFilter.getOrFilter();
        assertEquals(2, orFilters.size());
        AttributeValueAssertionFilter equalityMatch = (AttributeValueAssertionFilter)orFilters.get(0);  
        assertNotNull(equalityMatch);
        
        AttributeValueAssertion assertion = equalityMatch.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("objectclass", assertion.getAttributeDesc().toString());
        assertEquals("top", assertion.getAssertionValue().toString());
        
        // (& (| (objectclass=top) (ou=contacts) ) (...
        equalityMatch = (AttributeValueAssertionFilter)orFilters.get(1);  
        assertNotNull(equalityMatch);
        
        assertion = equalityMatch.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("ou", assertion.getAttributeDesc().toString());
        assertEquals("contacts", assertion.getAssertionValue().toString());
        
        // (& (| (objectclass=top) (ou=contacts) ) (! ...
        NotFilter notFilter = ( NotFilter )andFilters.get(1);
        assertNotNull(notFilter);
        
        // (& (| (objectclass=top) (ou=contacts) ) (! (objectclass=ttt) ) )
        equalityMatch = (AttributeValueAssertionFilter)notFilter.getNotFilter();  
        assertNotNull(equalityMatch);
        
        assertion = equalityMatch.getAssertion();
        assertNotNull(assertion);
        
        assertEquals("ou", assertion.getAttributeDesc().toString());
        assertEquals("ttt", assertion.getAssertionValue().toString());
        
        Attributes attributes = sr.getAttributes();
        
        for (int i = 0; i < attributes.size(); i++) 
        {
        	assertNotNull( attributes.get( "attr" + i ) );
        }

        // We won't check the encoding, as it has changed because of 
        // attributes transformations
    }

    /**
     * Test the decoding of a SearchRequest with SubEntry control.
     */
     public void testDecodeSearchRequestSubEntryControl()
     {
        byte[] asn1BER = new byte[]
        {
            0x30, 0x5D,
                0x02, 0x01, 0x04, // messageID
                0x63, 0x33,
                    0x04, 0x13,
                        0x64, 0x63, 0x3d, 0x6d, 0x79, 0x2d, 0x64, 0x6f, 0x6d, 0x61,
                                0x69, 0x6e, 0x2c, 0x64, 0x63, 0x3d, 0x63, 0x6f, 0x6d, // baseObject: dc=my-domain,dc=com
                    0x0a, 0x01, 0x02, // scope: subtree
                    0x0a, 0x01, 0x03, // derefAliases: derefAlways
                    0x02, 0x01, 0x00, // sizeLimit: 0
                    0x02, 0x01, 0x00, // timeLimit: 0
                    0x01, 0x01, 0x00, // typesOnly: false
                    (byte)0x87, 0x0b,
                        0x6f, 0x62, 0x6a, 0x65, 0x63, 0x74, 0x43, 0x6c, 0x61, 0x73, 0x73, // filter: (objectClass=*)
                    0x30, 0x00,
                (byte)0xa0, 0x23, // controls
                    0x30, 0x21,
                        0x04, 0x17, 
                          '1', '.', '3', '.', '6', '.', '1', '.',  
                          '4', '.', '1', '.', '4', '2', '0', '3',  
                          '.', '1', '.', '1', '0', '.', '1', // SubEntry OID
                        0x01, 0x01, (byte)0xFF, // criticality: true
                        0x04, 0x03,
                            0x01, 0x01, (byte)0xFF // SubEntry visibility
        };
     
        Asn1Decoder ldapDecoder = new LdapDecoder();
     
        ByteBuffer  stream      = ByteBuffer.allocate( asn1BER.length );
        stream.put( asn1BER );
        String decodedPdu       = StringTools.dumpBytes( stream.array() );
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
     
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        assertEquals( 4, message.getMessageId() );
        assertEquals( 1, message.getControls().size() );
     
        // SubEntry Control
        String subEntryControlOID = "1.3.6.1.4.1.4203.1.10.1";
        Control subEntryControl = message.getControls( 0 );
        assertEquals( subEntryControlOID, subEntryControl.getControlType() );
        assertTrue( subEntryControl.getCriticality() );
        assertTrue( ( (SubEntryControl)subEntryControl.getControlValue() ).isVisible() );
     
        SearchRequest sr    = message.getSearchRequest();
        assertEquals( "dc=my-domain,dc=com", sr.getBaseObject() );
        assertEquals( LdapConstants.SCOPE_WHOLE_SUBTREE, sr.getScope() );
        assertEquals( LdapConstants.DEREF_ALWAYS, sr.getDerefAliases() );
        assertEquals( 0, sr.getSizeLimit() );
        assertEquals( 0, sr.getTimeLimit() );
        assertEquals( false, sr.isTypesOnly() );
     
        assertTrue( sr.getFilter() instanceof PresentFilter );
        assertEquals ( "objectClass",
            ( (PresentFilter) sr.getFilter() ).getAttributeDescription().getString());
     
        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            String encodedPdu = StringTools.dumpBytes( bb.array() );
            assertEquals(encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
     }    
}
