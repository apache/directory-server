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

package org.apache.directory.shared.dsmlv2.searchRequest;


import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.shared.dsmlv2.AbstractTest;
import org.apache.directory.shared.dsmlv2.Dsmlv2Parser;
import org.apache.directory.shared.ldap.codec.AttributeValueAssertion;
import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.search.AndFilter;
import org.apache.directory.shared.ldap.codec.search.AttributeValueAssertionFilter;
import org.apache.directory.shared.ldap.codec.search.ExtensibleMatchFilter;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.NotFilter;
import org.apache.directory.shared.ldap.codec.search.OrFilter;
import org.apache.directory.shared.ldap.codec.search.PresentFilter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SubstringFilter;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Tests for the Del Request parsing
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchRequestTest extends AbstractTest
{
    /**
     * Test parsing of a request without the dn attribute
     */
    @Test
    public void testRequestWithoutDn()
    {
        testParsingFail( SearchRequestTest.class, "request_without_dn_attribute.xml" );
    }


    /**
     * Test parsing of a request with the dn attribute
     */
    @Test
    public void testRequestWithDn()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_dn_attribute.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( "ou=marketing,dc=microsoft,dc=com", searchRequest.getBaseObject().toString() );
    }


    /**
     * Test parsing of a request with the (optional) requestID attribute
     */
    @Test
    public void testRequestWithRequestId()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput(
                SearchRequestTest.class.getResource( "request_with_requestID_attribute.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( 456, searchRequest.getMessageId() );
    }


    /**
     * Test parsing of a request with the (optional) requestID attribute equals to 0
     */
    @Test
    public void testRequestWithRequestIdEquals0()
    {
        testParsingFail( SearchRequestTest.class, "request_with_requestID_equals_0.xml" );
    }


    /**
     * Test parsing of a request with a (optional) Control element
     */
    @Test
    public void testRequestWith1Control()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_1_control.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();
        ControlCodec control = searchRequest.getCurrentControl();

        assertEquals( 1, searchRequest.getControls().size() );
        assertTrue( control.getCriticality() );
        assertEquals( "1.2.840.113556.1.4.643", control.getControlType() );
        assertEquals( "Some text", StringTools.utf8ToString( ( byte[] ) control.getControlValue() ) );
    }


    /**
     * Test parsing of a request with a (optional) Control element with Base64 value
     */
    @Test
    public void testRequestWith1ControlBase64Value()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_1_control_base64_value.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();
        ControlCodec control = searchRequest.getCurrentControl();

        assertEquals( 1, searchRequest.getControls().size() );
        assertTrue( control.getCriticality() );
        assertEquals( "1.2.840.113556.1.4.643", control.getControlType() );
        assertEquals( "DSMLv2.0 rocks!!", StringTools.utf8ToString( ( byte[] ) control.getControlValue() ) );
    }


    /**
     * Test parsing of a request with a (optional) Control element with empty value
     */
    @Test
    public void testRequestWith1ControlEmptyValue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_1_control_empty_value.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();
        ControlCodec control = searchRequest.getCurrentControl();

        assertEquals( 1, searchRequest.getControls().size() );
        assertTrue( control.getCriticality() );
        assertEquals( "1.2.840.113556.1.4.643", control.getControlType() );
        assertEquals( StringTools.EMPTY_BYTES, control.getControlValue() );
    }


    /**
     * Test parsing of a request with 2 (optional) Control elements
     */
    @Test
    public void testRequestWith2Controls()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser
                .setInput( SearchRequestTest.class.getResource( "request_with_2_controls.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();
        ControlCodec control = searchRequest.getCurrentControl();

        assertEquals( 2, searchRequest.getControls().size() );
        assertFalse( control.getCriticality() );
        assertEquals( "1.2.840.113556.1.4.789", control.getControlType() );
        assertEquals( "Some other text", StringTools.utf8ToString( ( byte[] ) control.getControlValue() ) );
    }


    /**
     * Test parsing of a request with 3 (optional) Control elements without value
     */
    @Test
    public void testRequestWith3ControlsWithoutValue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_3_controls_without_value.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();
        ControlCodec control = searchRequest.getCurrentControl();

        assertEquals( 3, searchRequest.getControls().size() );
        assertTrue( control.getCriticality() );
        assertEquals( "1.2.840.113556.1.4.456", control.getControlType() );
        assertEquals( StringTools.EMPTY_BYTES, control.getControlValue() );
    }


    /**
     * Test parsing of a request without the Filter element
     */
    @Test
    public void testRequestWithoutFilter()
    {
        testParsingFail( SearchRequestTest.class, "request_without_filter.xml" );
    }


    /**
     * Test parsing of a request without scope attribute
     */
    @Test
    public void testRequestWithoutScopeAttribute()
    {
        testParsingFail( SearchRequestTest.class, "request_without_scope_attribute.xml" );
    }


    /**
     * Test parsing of a request with scope attribute to BaseObject value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithScopeBaseObject()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_scope_baseObject.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( SearchScope.OBJECT, searchRequest.getScope() );
    }


    /**
     * Test parsing of a request with scope attribute to SingleLevel value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithScopeSingleLevel()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_scope_singleLevel.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( SearchScope.ONELEVEL, searchRequest.getScope() );
    }


    /**
     * Test parsing of a request with scope attribute to WholeSubtree value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithScopeWholeSubtree()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_scope_wholeSubtree.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( SearchScope.SUBTREE, searchRequest.getScope() );
    }


    /**
     * Test parsing of a request with scope attribute to Error value
     */
    @Test
    public void testRequestWithScopeError()
    {
        testParsingFail( SearchRequestTest.class, "request_with_scope_error.xml" );
    }


    /**
     * Test parsing of a request without derefAliases attribute
     */
    @Test
    public void testRequestWithoutDerefAliasesAttribute()
    {
        testParsingFail( SearchRequestTest.class, "request_without_derefAliases_attribute.xml" );
    }


    /**
     * Test parsing of a request with derefAliases attribute to derefAlways value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithDerefAliasesDerefAlways()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_derefAliases_derefAlways.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( LdapConstants.DEREF_ALWAYS, searchRequest.getDerefAliases() );
    }


    /**
     * Test parsing of a request with derefAliases attribute to derefFindingBaseObj value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithDerefAliasesDerefFindingBaseObj()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_derefAliases_derefFindingBaseObj.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( LdapConstants.DEREF_FINDING_BASE_OBJ, searchRequest.getDerefAliases() );
    }


    /**
     * Test parsing of a request with derefAliases attribute to derefinSearching value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithDerefAliasesDerefinSearching()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_derefAliases_derefInSearching.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( LdapConstants.DEREF_IN_SEARCHING, searchRequest.getDerefAliases() );
    }


    /**
     * Test parsing of a request with derefAliases attribute to neverDerefAliases value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithDerefAliasesNeverDerefAliases()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_derefAliases_neverDerefAliases.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( LdapConstants.NEVER_DEREF_ALIASES, searchRequest.getDerefAliases() );
    }


    /**
     * Test parsing of a request with derefAliases attribute to Error value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithDerefAliasesError()
    {
        testParsingFail( SearchRequestTest.class, "request_with_derefAliases_error.xml" );
    }


    /**
     * Test parsing of a request with the sizeLimit (optional) attribute
     * @throws NamingException 
     */
    @Test
    public void testRequestWithSizeLimitAttribute()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput(
                SearchRequestTest.class.getResource( "request_with_sizeLimit_attribute.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( 1000, searchRequest.getSizeLimit() );
    }


    /**
     * Test parsing of a request with sizeLimit attribute to Error value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithSizeLimitError()
    {
        testParsingFail( SearchRequestTest.class, "request_with_sizeLimit_error.xml" );
    }


    /**
     * Test parsing of a request with the timeLimit (optional) attribute
     * @throws NamingException 
     */
    @Test
    public void testRequestWithTimeLimitAttribute()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput(
                SearchRequestTest.class.getResource( "request_with_timeLimit_attribute.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertEquals( 60, searchRequest.getTimeLimit() );
    }


    /**
     * Test parsing of a request with timeLimit attribute to Error value
     * @throws NamingException 
     */
    @Test
    public void testRequestWithTimeLimitError()
    {
        testParsingFail( SearchRequestTest.class, "request_with_timeLimit_error.xml" );
    }


    /**
     * Test parsing of a request with typesOnly to true
     */
    @Test
    public void testRequestWithTypesOnlyTrue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_typesOnly_true.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertTrue( searchRequest.isTypesOnly() );
    }


    /**
     * Test parsing of a request with typesOnly to 1
     */
    @Test
    public void testRequestWithTypesOnly1()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_typesOnly_1.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertTrue( searchRequest.isTypesOnly() );
    }


    /**
     * Test parsing of a request with typesOnly to false
     */
    @Test
    public void testRequestWithTypesOnlyFalse()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_typesOnly_false.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertFalse( searchRequest.isTypesOnly() );
    }


    /**
     * Test parsing of a request with typesOnly to 0
     */
    @Test
    public void testRequestWithTypesOnlyRdn0()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_typesOnly_0.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        assertFalse( searchRequest.isTypesOnly() );
    }


    /**
     * Test parsing of a request with typesOnly to an error value
     */
    @Test
    public void testRequestWithTypesOnlyError()
    {
        testParsingFail( SearchRequestTest.class, "request_with_typesOnly_error.xml" );
    }


    /**
     * Test parsing of a request with 2 Filter elements
     */
    @Test
    public void testRequestWith2Filters()
    {
        testParsingFail( SearchRequestTest.class, "request_with_2_filters.xml" );
    }


    /**
     * Test parsing of a request with Attibutes Element but not any Attribute element
     */
    @Test
    public void testRequestWithAttributesButNoAttribute()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_attributes_but_no_attribute.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        assertTrue( true );
    }


    /**
     * Test parsing of a request with 2 Attributes elements
     */
    @Test
    public void testRequestWith2AttributesElements()
    {
        testParsingFail( SearchRequestTest.class, "request_with_2_attributes_elements.xml" );
    }


    /**
     * Test parsing of a request with an Attributes element with 1 Attribute element
     * @throws NamingException 
     */
    @Test
    public void testRequestWithAttributes1Attribute() throws NamingException
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_attributes_1_attribute.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        List<EntryAttribute> attributes = searchRequest.getAttributes();
        assertEquals( 1, attributes.size() );

        EntryAttribute attribute = attributes.get( 0 );
        assertEquals( "sn", attribute.getUpId() );
    }


    /**
     * Test parsing of a request with an Attributes element with 2 Attribute elements
     * @throws NamingException 
     */
    @Test
    public void testRequestWithAttributes2Attribute() throws NamingException
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "request_with_attributes_2_attribute.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        List<EntryAttribute> attributes = searchRequest.getAttributes();
        assertEquals( 2, attributes.size() );

        EntryAttribute attribute1 = attributes.get( 0 );
        assertEquals( "sn", attribute1.getUpId() );

        EntryAttribute attribute2 = attributes.get( 1 );
        assertEquals( "givenName", attribute2.getUpId() );
    }


    /**
     * Test parsing of a request with 1 Attribute without name attribute
     */
    @Test
    public void testRequestWithAttributeWithoutNameAttribute()
    {
        testParsingFail( SearchRequestTest.class, "request_with_attribute_without_name_attribute.xml" );
    }


    /**
     * Test parsing of a request with empty Filter element
     */
    @Test
    public void testRequestWithEmptyFilter()
    {
        testParsingFail( SearchRequestTest.class, "request_with_empty_filter.xml" );
    }


    /**
     * Test parsing of a request with an And Filter
     */
    @Test
    public void testRequestWithAndFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_and.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AndFilter );
    }


    /**
     * Test parsing of a request with an Or Filter
     */
    @Test
    public void testRequestWithOrFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser
                .setInput( SearchRequestTest.class.getResource( "filters/request_with_or.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof OrFilter );
    }


    /**
     * Test parsing of a request with an Or Filter
     */
    @Test
    public void testRequestWithNotFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_not.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof NotFilter );
    }


    /**
     * Test parsing of a request with empty Filter element
     */
    @Test
    public void testRequestWithNotFilterWith2Children()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_not_with_2_children.xml" );
    }


    /**
     * Test parsing of a request with an approxMatch Filter
     */
    @Test
    public void testRequestWithApproxMatchFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput(
                SearchRequestTest.class.getResource( "filters/request_with_approxMatch.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.APPROX_MATCH_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertEquals( "foobar", assertion.getAssertionValue().getString() );
    }


    /**
     * Test parsing of a request with an approxMatch Filter with base64 value
     */
    @Test
    public void testRequestWithApproxMatchFilterBase64Value()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_approxMatch_base64_value.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.APPROX_MATCH_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertEquals( "DSMLv2.0 rocks!!", assertion.getAssertionValue().getString() );
    }


    /**
     * Test parsing of a request with an approxMatch Filter with empty value
     */
    @Test
    public void testRequestWithApproxMatchFilterEmptyValue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_approxMatch_with_empty_value.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.APPROX_MATCH_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertNull( assertion.getAssertionValue() );
    }


    /**
     * Test parsing of a request with approxMatch Filter but no name attribute
     */
    @Test
    public void testRequestWithApproxMatchFilterWithoutName()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_approxMatch_without_name.xml" );
    }


    /**
     * Test parsing of a request with approxMatch Filter but no value element
     */
    @Test
    public void testRequestWithApproxMatchFilterWithoutValue()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_approxMatch_without_value.xml" );
    }


    /**
     * Test parsing of a request with approxMatch Filter with 2 Value elements
     */
    @Test
    public void testRequestWithApproxMatchFilterWith2Values()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_approxMatch_with_2_values.xml" );
    }


    /**
     * Test parsing of a request with an greaterOrEqual Filter
     */
    @Test
    public void testRequestWithGreaterOrEqualFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_greaterOrEqual.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.GREATER_OR_EQUAL_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertEquals( "foobar", assertion.getAssertionValue().getString() );
    }


    /**
     * Test parsing of a request with an greaterOrEqual Filter with base64 value
     */
    @Test
    public void testRequestWithGreaterOrEqualFilterBase64Value()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_greaterOrEqual_base64_value.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.GREATER_OR_EQUAL_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertEquals( "DSMLv2.0 rocks!!", assertion.getAssertionValue().getString() );
    }


    /**
     * Test parsing of a request with an greaterOrEqual Filter with an empty value
     */
    @Test
    public void testRequestWithGreaterOrEqualFilterEmptyValue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_greaterOrEqual_with_empty_value.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.GREATER_OR_EQUAL_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertNull( assertion.getAssertionValue() );
    }


    /**
     * Test parsing of a request with greaterOrEqual Filter but no name attribute
     */
    @Test
    public void testRequestWithGreaterOrEqualFilterWithoutName()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_greaterOrEqual_without_name.xml" );
    }


    /**
     * Test parsing of a request with greaterOrEqual Filter but no value element
     */
    @Test
    public void testRequestWithGreaterOrEqualFilterWithoutValue()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_greaterOrEqual_without_value.xml" );
    }


    /**
     * Test parsing of a request with greaterOrEqual Filter with 2 Value elements
     */
    @Test
    public void testRequestWithGreaterOrEqualFilterWith2Values()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_greaterOrEqual_with_2_values.xml" );
    }


    /**
     * Test parsing of a request with an lessOrEqual Filter
     */
    @Test
    public void testRequestWithLessOrEqualFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput(
                SearchRequestTest.class.getResource( "filters/request_with_lessOrEqual.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.LESS_OR_EQUAL_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertEquals( "foobar", assertion.getAssertionValue().getString() );
    }


    /**
     * Test parsing of a request with an lessOrEqual Filter with Base64 value
     */
    @Test
    public void testRequestWithLessOrEqualFilterBase64Value()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_lessOrEqual_base64_value.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.LESS_OR_EQUAL_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertEquals( "DSMLv2.0 rocks!!", assertion.getAssertionValue().getString() );
    }


    /**
     * Test parsing of a request with an lessOrEqual Filter
     */
    @Test
    public void testRequestWithLessOrEqualFilterEmptyValue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_lessOrEqual_with_empty_value.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.LESS_OR_EQUAL_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertNull( assertion.getAssertionValue() );
    }


    /**
     * Test parsing of a request with lessOrEqual Filter but no name attribute
     */
    @Test
    public void testRequestWithLessOrEqualFilterWithoutName()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_lessOrEqual_without_name.xml" );
    }


    /**
     * Test parsing of a request with lessOrEqual Filter but no value element
     */
    @Test
    public void testRequestWithLessOrEqualFilterWithoutValue()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_lessOrEqual_without_value.xml" );
    }


    /**
     * Test parsing of a request with lessOrEqual Filter with 2 Value elements
     */
    @Test
    public void testRequestWithLessOrEqualFilterWith2Values()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_lessOrEqual_with_2_values.xml" );
    }


    /**
     * Test parsing of a request with an Equality Filter
     */
    @Test
    public void testRequestWithEqualityMatchFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_equalityMatch.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.EQUALITY_MATCH_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertEquals( "foobar", assertion.getAssertionValue().getString() );
    }


    /**
     * Test parsing of a request with an Equality Filter with base64 value
     */
    @Test
    public void testRequestWithEqualityMatchFilterBase64Value()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class
                .getResource( "filters/request_with_equalityMatch_base64_value.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.EQUALITY_MATCH_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertEquals( "DSMLv2.0 rocks!!", assertion.getAssertionValue().getString() );
    }


    /**
     * Test parsing of a request with an Equality Filter with an empty value
     */
    @Test
    public void testRequestWithEqualityMatchFilterWithEmptyValue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_equalityMatch_with_empty_value.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof AttributeValueAssertionFilter );

        AttributeValueAssertionFilter approxMatchFilter = ( AttributeValueAssertionFilter ) filter;

        assertEquals( LdapConstants.EQUALITY_MATCH_FILTER, approxMatchFilter.getFilterType() );

        AttributeValueAssertion assertion = approxMatchFilter.getAssertion();

        assertEquals( "sn", assertion.getAttributeDesc() );

        assertNull( assertion.getAssertionValue() );
    }


    /**
     * Test parsing of a request with EqualityMatch Filter but no name attribute
     */
    @Test
    public void testRequestWithEqualityMatchFilterWithoutName()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_equalityMatch_without_name.xml" );
    }


    /**
     * Test parsing of a request with EqualityMatch Filter but no value element
     */
    @Test
    public void testRequestWithEqualityMatchFilterWithoutValue()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_equalityMatch_without_value.xml" );
    }


    /**
     * Test parsing of a request with EqualityMatch Filter with 2 Value elements
     */
    @Test
    public void testRequestWithEqualityMatchFilterWith2Values()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_equalityMatch_with_2_values.xml" );
    }


    /**
     * Test parsing of a request with an Present Filter
     */
    @Test
    public void testRequestWithPresentFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_present.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof PresentFilter );

        PresentFilter presentFilter = ( PresentFilter ) filter;

        assertEquals( "givenName", presentFilter.getAttributeDescription().toString() );
    }


    /**
     * Test parsing of a request with Present Filter without name attribute
     */
    @Test
    public void testRequestWithPresentWithoutName()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_present_without_name.xml" );
    }


    /**
     * Test parsing of a request with an ExtensibleMatch Filter
     */
    @Test
    public void testRequestWithExtensibleMatchFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_extensibleMatch.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertEquals( "A Value", extensibleMatchFilter.getMatchValue().getString() );

        assertEquals( false, extensibleMatchFilter.isDnAttributes() );
    }


    /**
     * Test parsing of a request with an ExtensibleMatch Filter
     */
    @Test
    public void testRequestWithExtensibleMatchFilterBase64Value()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_extensibleMatch_base64_value.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertEquals( "DSMLv2.0 rocks!!", extensibleMatchFilter.getMatchValue().getString() );

        assertEquals( false, extensibleMatchFilter.isDnAttributes() );
    }


    /**
     * Test parsing of a request with an ExtensibleMatch Filter with empty value
     */
    @Test
    public void testRequestWithExtensibleMatchWithEmptyValue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_extensibleMatch_with_empty_value.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertNull( extensibleMatchFilter.getMatchValue() );

        assertEquals( false, extensibleMatchFilter.isDnAttributes() );
    }


    /**
     * Test parsing of a request with ExtensibleMatch Filter without Value element
     */
    @Test
    public void testRequestWithExtensibleMatchWithoutValue()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_extensibleMatch_without_value.xml" );
    }


    /**
     * Test parsing of a request with ExtensibleMatch Filter with 2 Value elements
     */
    @Test
    public void testRequestWithExtensibleMatchWith2Values()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_extensibleMatch_with_2_values.xml" );
    }


    /**
     * Test parsing of a request with typesOnly to true
     */
    @Test
    public void testRequestWithExtensibleMatchWithDnAttributesTrue()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_extensibleMatch_with_dnAttributes_true.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertTrue( extensibleMatchFilter.isDnAttributes() );
    }


    /**
     * Test parsing of a request with typesOnly to 1
     */
    @Test
    public void testRequestWithExtensibleMatchWithDnAttributes1()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_extensibleMatch_with_dnAttributes_1.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertTrue( extensibleMatchFilter.isDnAttributes() );
    }


    /**
     * Test parsing of a request with typesOnly to false
     */
    @Test
    public void testRequestWithExtensibleMatchWithDnAttributesFalse()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_extensibleMatch_with_dnAttributes_false.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertFalse( extensibleMatchFilter.isDnAttributes() );
    }


    /**
     * Test parsing of a request with typesOnly to 0
     */
    @Test
    public void testRequestWithExtensibleMatchWithDnAttributes0()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_extensibleMatch_with_dnAttributes_0.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertFalse( extensibleMatchFilter.isDnAttributes() );
    }


    /**
     * Test parsing of a request with typesOnly to an error value
     */
    @Test
    public void testRequestWithExtensibleMatchWithDnAttributesError()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_extensibleMatch_with_dnAttributes_error.xml" );
    }


    /**
     * Test parsing of a request with a matchingRule attribute
     */
    @Test
    public void testRequestWithExtensibleMatchWithMatchingRule()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_extensibleMatch_with_matchingRule.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertEquals( "AMatchingRuleName", extensibleMatchFilter.getMatchingRule().toString() );
    }


    /**
     * Test parsing of a request with a name attribute
     */
    @Test
    public void testRequestWithExtensibleMatchWithName()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_extensibleMatch_with_name.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof ExtensibleMatchFilter );

        ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

        assertEquals( "givenName", extensibleMatchFilter.getType().toString() );
    }


    /**
     * Test parsing of a request with an Substrings Filter
     */
    @Test
    public void testRequestWithSubstringsFilter()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertEquals( "sn", substringFilter.getType().toString() );
    }


    /**
     * Test parsing of a request with Substrings Filter without name
     */
    @Test
    public void testRequestWithSubstringsWithoutName()
    {
        testParsingFail( SearchRequestTest.class, "filters/request_with_substrings_without_name.xml" );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Initial element
     */
    @Test
    public void testRequestWithSubstrings1Initial()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_1_initial.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertEquals( "jack", substringFilter.getInitialSubstrings().toString() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Initial element with Base64 value
     */
    @Test
    public void testRequestWithSubstrings1Base64Initial()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_substrings_1_base64_initial.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertEquals( "DSMLv2.0 rocks!!", substringFilter.getInitialSubstrings().toString() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 emptyInitial element
     */
    @Test
    public void testRequestWithSubstrings1EmptyInitial()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class
                .getResource( "filters/request_with_substrings_1_empty_initial.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertNull( substringFilter.getInitialSubstrings() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Initial and 1 Any elements
     */
    @Test
    public void testRequestWithSubstrings1Initial1Any()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class
                .getResource( "filters/request_with_substrings_1_initial_1_any.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertEquals( "jack", substringFilter.getInitialSubstrings() );

        List<String> initials = substringFilter.getAnySubstrings();

        assertEquals( 1, initials.size() );

        assertEquals( "kate", initials.get( 0 ).toString() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Initial and 1 Final elements
     */
    @Test
    public void testRequestWithSubstrings1Initial1Final()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource(
                "filters/request_with_substrings_1_initial_1_final.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertEquals( "jack", substringFilter.getInitialSubstrings() );

        assertEquals( "john", substringFilter.getFinalSubstrings() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Any element
     */
    @Test
    public void testRequestWithSubstrings1Any()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_1_any.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        List<String> initials = substringFilter.getAnySubstrings();

        assertEquals( 1, initials.size() );
        assertEquals( "kate", initials.get( 0 ) );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Any element
     */
    @Test
    public void testRequestWithSubstrings1Base64Any()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_1_base64_any.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        List<String> initials = substringFilter.getAnySubstrings();

        assertEquals( 1, initials.size() );
        assertEquals( "DSMLv2.0 rocks!!", initials.get( 0 ) );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 empty Any element
     */
    @Test
    public void testRequestWithSubstrings1EmptyAny()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_1_empty_any.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        List<String> initials = substringFilter.getAnySubstrings();

        assertEquals( 0, initials.size() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Any element
     */
    @Test
    public void testRequestWithSubstrings2Any()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_2_any.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        List<String> initials = substringFilter.getAnySubstrings();

        assertEquals( 2, initials.size() );

        assertEquals( "kate", initials.get( 0 ) );

        assertEquals( "sawyer", initials.get( 1 ) );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Any and 1 Final elements
     */
    @Test
    public void testRequestWithSubstrings1Any1Final()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_1_any_1_final.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        List<String> initials = substringFilter.getAnySubstrings();

        assertEquals( 1, initials.size() );

        assertEquals( "kate", initials.get( 0 ) );

        assertEquals( "john", substringFilter.getFinalSubstrings() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Final element
     */
    @Test
    public void testRequestWithSubstrings1Final()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_1_final.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertEquals( "john", substringFilter.getFinalSubstrings().toString() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 Final element
     */
    @Test
    public void testRequestWithSubstrings1Base64Final()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_1_base64_final.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertEquals( "DSMLv2.0 rocks!!", substringFilter.getFinalSubstrings().toString() );
    }


    /**
     * Test parsing of a request with a Substrings Filter with 1 empty Final element
     */
    @Test
    public void testRequestWithSubstrings1EmptyFinal()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( SearchRequestTest.class.getResource( "filters/request_with_substrings_1_empty_final.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchRequestCodec searchRequest = ( SearchRequestCodec ) parser.getBatchRequest().getCurrentRequest();

        Filter filter = searchRequest.getFilter();

        assertTrue( filter instanceof SubstringFilter );

        SubstringFilter substringFilter = ( SubstringFilter ) filter;

        assertNull( substringFilter.getFinalSubstrings() );
    }


    /**
     * Test parsing of a request with a needed requestID attribute
     * 
     * DIRSTUDIO-1
     */
    @Test
    public void testRequestWithNeededRequestId()
    {
        testParsingFail( SearchRequestTest.class, "request_with_needed_requestID.xml" );
    }
}
