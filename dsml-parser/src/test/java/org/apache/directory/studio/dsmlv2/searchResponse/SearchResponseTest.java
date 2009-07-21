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

package org.apache.directory.studio.dsmlv2.searchResponse;


import org.apache.directory.studio.dsmlv2.AbstractResponseTest;
import org.apache.directory.studio.dsmlv2.Dsmlv2ResponseParser;
import org.apache.directory.studio.dsmlv2.reponse.SearchResponse;


/**
 * Tests for the Search Result Done Response parsing
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchResponseTest extends AbstractResponseTest
{
    /**
     * Test parsing of a Response with the (optional) requestID attribute
     */
    public void testResponseWithRequestId()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( SearchResponseTest.class.getResource( "response_with_requestID_attribute.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchResponse searchResponse = ( SearchResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 456, searchResponse.getMessageId() );
    }


    /**
     * Test parsing of a Response with the (optional) requestID attribute equals 0
     */
    public void testResponseWithRequestIdEquals0()
    {
        testParsingFail( SearchResponseTest.class, "response_with_requestID_equals_0.xml" );
    }


    /**
     * Test parsing of a Response with a Search Result Done
     */
    public void testResponseWithSRD()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( SearchResponseTest.class.getResource( "response_with_1_SRD.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchResponse searchResponse = ( SearchResponse ) parser.getBatchResponse().getCurrentResponse();

        assertNotNull( searchResponse.getSearchResultDone() );
    }


    /**
     * Test parsing of a Response with 1 Search Result Entry and a Search Result Done
     */
    public void testResponseWith1SRE1SRD()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( SearchResponseTest.class.getResource( "response_with_1_SRE_1_SRD.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchResponse searchResponse = ( SearchResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 1, searchResponse.getSearchResultEntryList().size() );

        assertNotNull( searchResponse.getSearchResultDone() );
    }


    /**
     * Test parsing of a Response with 1 Search Result Reference and a Search Result Done
     */
    public void testResponseWith1SRR1SRD()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( SearchResponseTest.class.getResource( "response_with_1_SRR_1_SRD.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchResponse searchResponse = ( SearchResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 1, searchResponse.getSearchResultReferenceList().size() );

        assertNotNull( searchResponse.getSearchResultDone() );
    }


    /**
     * Test parsing of a Response with 1 Search Result Entry, 1 Search Result Reference and a Search Result Done
     */
    public void testResponseWith1SRE1SRR1SRD()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput(
                SearchResponseTest.class.getResource( "response_with_1_SRE_1_SRR_1_SRD.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchResponse searchResponse = ( SearchResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 1, searchResponse.getSearchResultEntryList().size() );

        assertEquals( 1, searchResponse.getSearchResultReferenceList().size() );

        assertNotNull( searchResponse.getSearchResultDone() );
    }


    /**
     * Test parsing of a Response with 2 Search Result Entry and a Search Result Done
     */
    public void testResponseWith2SRE1SRD()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( SearchResponseTest.class.getResource( "response_with_2_SRE_1_SRD.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchResponse searchResponse = ( SearchResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 2, searchResponse.getSearchResultEntryList().size() );

        assertNotNull( searchResponse.getSearchResultDone() );
    }


    /**
     * Test parsing of a Response with 2 Search Result Reference and a Search Result Done
     */
    public void testResponseWith2SRR1SRD()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( SearchResponseTest.class.getResource( "response_with_2_SRR_1_SRD.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchResponse searchResponse = ( SearchResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 2, searchResponse.getSearchResultReferenceList().size() );

        assertNotNull( searchResponse.getSearchResultDone() );
    }


    /**
     * Test parsing of a Response with 2 Search Result Entry, 2 Search Result Reference and a Search Result Done
     */
    public void testResponseWith2SRE2SRR1SRD()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput(
                SearchResponseTest.class.getResource( "response_with_2_SRE_2_SRR_1_SRD.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        SearchResponse searchResponse = ( SearchResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 2, searchResponse.getSearchResultEntryList().size() );

        assertEquals( 2, searchResponse.getSearchResultReferenceList().size() );

        assertNotNull( searchResponse.getSearchResultDone() );
    }


    /**
     * Test parsing of a response with no Search Result Done
     */
    public void testResponseWith0SRD()
    {
        testParsingFail( SearchResponseTest.class, "response_with_0_SRD.xml" );
    }


    /**
     * Test parsing of a response with 1 Search Result Entry but no Search Result Done
     */
    public void testResponseWith1SRE0SRD()
    {
        testParsingFail( SearchResponseTest.class, "response_with_1_SRE_0_SRD.xml" );
    }


    /**
     * Test parsing of a response with 1 Search Result Reference but no Search Result Done
     */
    public void testResponseWith1SRR0SRD()
    {
        testParsingFail( SearchResponseTest.class, "response_with_1_SRR_0_SRD.xml" );
    }
}
