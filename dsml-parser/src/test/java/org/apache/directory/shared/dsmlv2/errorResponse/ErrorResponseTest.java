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

package org.apache.directory.shared.dsmlv2.errorResponse;


import org.apache.directory.shared.dsmlv2.AbstractResponseTest;
import org.apache.directory.shared.dsmlv2.Dsmlv2ResponseParser;
import org.apache.directory.shared.dsmlv2.reponse.ErrorResponse;
import org.apache.directory.shared.dsmlv2.reponse.ErrorResponse.ErrorResponseType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;

/**
 * Tests for the Error Response parsing
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ErrorResponseTest extends AbstractResponseTest
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

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_requestID_attribute.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 456, errorResponse.getMessageId() );
    }


    /**
     * Test parsing of a Response with the (optional) requestID attribute equals 0
     */
    @Test
    public void testResponseWithRequestIdEquals0()
    {
        testParsingFail( ErrorResponseTest.class, "response_with_requestID_equals_0.xml" );
    }


    /**
     * Test parsing of a response without Type attribute
     */
    @Test
    public void testResponseWithoutType()
    {
        testParsingFail( ErrorResponseTest.class, "response_without_type.xml" );
    }


    /**
     * Test parsing of a response with type == notAttempted
     */
    @Test
    public void testResponseWithTypeNotAttempted()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_type_notAttempted.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( ErrorResponseType.NOT_ATTEMPTED, errorResponse.getType() );
    }


    /**
     * Test parsing of a response with type == couldNotConnect
     */
    @Test
    public void testResponseWithTypeCouldNotConnect()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_type_couldNotConnect.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( ErrorResponseType.COULD_NOT_CONNECT, errorResponse.getType() );
    }


    /**
     * Test parsing of a response with type == connectionClosed
     */
    @Test
    public void testResponseWithTypeConnectionClosed()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_type_connectionClosed.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( ErrorResponseType.CONNECTION_CLOSED, errorResponse.getType() );
    }


    /**
     * Test parsing of a response with type == malformedRequest
     */
    @Test
    public void testResponseWithTypeMalformedRequest()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_type_malformedRequest.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( ErrorResponseType.MALFORMED_REQUEST, errorResponse.getType() );
    }


    /**
     * Test parsing of a response with type == gatewayInternalError
     */
    @Test
    public void testResponseWithTypeGatewayInternalError()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_type_gatewayInternalError.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( ErrorResponseType.GATEWAY_INTERNAL_ERROR, errorResponse.getType() );
    }


    /**
     * Test parsing of a response with type == authenticationFailed
     */
    @Test
    public void testResponseWithTypeAuthenticationFailed()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_type_authenticationFailed.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( ErrorResponseType.AUTHENTICATION_FAILED, errorResponse.getType() );
    }


    /**
     * Test parsing of a response with type == unresolvableURI
     */
    @Test
    public void testResponseWithTypeUnresolvableURI()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_type_unresolvableURI.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( ErrorResponseType.UNRESOLVABLE_URI, errorResponse.getType() );
    }


    /**
     * Test parsing of a response with type == other
     */
    @Test
    public void testResponseWithTypeOther()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_type_other.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( ErrorResponseType.OTHER, errorResponse.getType() );
    }


    /**
     * Test parsing of a response with type in error
     */
    @Test
    public void testResponseWithTypeError()
    {
        testParsingFail( ErrorResponseTest.class, "response_with_type_inError.xml" );
    }


    /**
     * Test parsing of a response with Message
     */
    @Test
    public void testResponseWithMessage()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_message.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( "Connection refused", errorResponse.getMessage() );
    }


    /**
     * Test parsing of a response with empty Message
     */
    @Test
    public void testResponseWithEmptyMessage()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( ErrorResponseTest.class.getResource( "response_with_empty_message.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        ErrorResponse errorResponse = ( ErrorResponse ) parser.getBatchResponse().getCurrentResponse();

        assertNull( errorResponse.getMessage() );
    }
}
