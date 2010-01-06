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

package org.apache.directory.shared.dsmlv2.batchResponse;


import org.apache.directory.shared.dsmlv2.AbstractResponseTest;
import org.apache.directory.shared.dsmlv2.Dsmlv2ResponseParser;
import org.apache.directory.shared.dsmlv2.reponse.BatchResponse;
import org.apache.directory.shared.dsmlv2.reponse.ErrorResponse;
import org.apache.directory.shared.dsmlv2.reponse.SearchResponse;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.codec.add.AddResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.BindResponseCodec;
import org.apache.directory.shared.ldap.codec.compare.CompareResponseCodec;
import org.apache.directory.shared.ldap.codec.del.DelResponseCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponseCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponseCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponseCodec;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for the Compare Response parsing
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BatchResponseTest extends AbstractResponseTest
{
    /**
     * Test parsing of a Response with the (optional) requestID attribute
     */
    @Test
    public void testResponseWithRequestId()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_requestID_attribute.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1234567890, batchResponse.getRequestID() );
    }


    /**
     * Test parsing of a Response with the (optional) requestID attribute equals 0
     */
    @Test
    public void testResponseWithRequestIdEquals0()
    {
        testParsingFail( BatchResponseTest.class, "response_with_requestID_equals_0.xml" );
    }


    /**
     * Test parsing of a Response with 0 Response
     */
    @Test
    public void testResponseWith0Reponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_0_response.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 0, batchResponse.getResponses().size() );
    }


    /**
     * Test parsing of a Response with the 1 AddResponse
     */
    @Test
    public void testResponseWith1AddResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_1_AddResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof AddResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 1 AuthResponse
     */
    @Test
    public void testResponseWith1AuthResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_1_AuthResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof BindResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 1 CompareResponse
     */
    @Test
    public void testResponseWith1CompareResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_1_CompareResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof CompareResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 1 DelResponse
     */
    @Test
    public void testResponseWith1DelResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_1_DelResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof DelResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 1 ErrorResponse
     */
    @Test
    public void testResponseWith1ErrorResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_1_ErrorResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof ErrorResponse )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 1 ExtendedResponse
     */
    @Test
    public void testResponseWith1ExtendedResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput(
                BatchResponseTest.class.getResource( "response_with_1_ExtendedResponse.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof ExtendedResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 1 ModDNResponse
     */
    @Test
    public void testResponseWith1ModDNResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_1_ModDNResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof ModifyDNResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 1 ModifyResponse
     */
    @Test
    public void testResponseWith1ModifyResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_1_ModifyResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof ModifyResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 1 SearchResponse
     */
    @Test
    public void testResponseWith1SearchResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_1_SearchResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 1, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof SearchResponse )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 AddResponse
     */
    @Test
    public void testResponseWith2AddResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_2_AddResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof AddResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 AuthResponse
     */
    @Test
    public void testResponseWith2AuthResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_2_AuthResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof BindResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 CompareResponse
     */
    @Test
    public void testResponseWith2CompareResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_2_CompareResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof CompareResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 DelResponse
     */
    @Test
    public void testResponseWith2DelResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_2_DelResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof DelResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 ErrorResponse
     */
    @Test
    public void testResponseWith2ErrorResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_2_ErrorResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof ErrorResponse )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 ExtendedResponse
     */
    @Test
    public void testResponseWith2ExtendedResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput(
                BatchResponseTest.class.getResource( "response_with_2_ExtendedResponse.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof ExtendedResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 ModDNResponse
     */
    @Test
    public void testResponseWith2ModDNResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_2_ModDNResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof ModifyDNResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 ModifyResponse
     */
    @Test
    public void testResponseWith2ModifyResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_2_ModifyResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof ModifyResponseCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Response with the 2 SearchResponse
     */
    @Test
    public void testResponseWith2SearchResponse()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( BatchResponseTest.class.getResource( "response_with_2_SearchResponse.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchResponse batchResponse = parser.getBatchResponse();

        assertEquals( 2, batchResponse.getResponses().size() );

        LdapResponseCodec response = batchResponse.getCurrentResponse();

        if ( response instanceof SearchResponse )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }
}
