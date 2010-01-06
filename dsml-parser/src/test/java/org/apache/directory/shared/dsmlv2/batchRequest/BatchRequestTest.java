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

package org.apache.directory.shared.dsmlv2.batchRequest;


import java.util.List;

import org.apache.directory.shared.dsmlv2.AbstractTest;
import org.apache.directory.shared.dsmlv2.Dsmlv2Parser;
import org.apache.directory.shared.dsmlv2.request.BatchRequest;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.BindRequestCodec;
import org.apache.directory.shared.ldap.codec.compare.CompareRequestCodec;
import org.apache.directory.shared.ldap.codec.del.DelRequestCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequestCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
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
public class BatchRequestTest extends AbstractTest
{
    /**
     * Test parsing of a Request with the (optional) requestID attribute
     */
    @Test
    public void testResponseWithRequestId()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_requestID_attribute.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1234567890, batchRequest.getRequestID() );
    }


    /**
     * Test parsing of a request with the (optional) requestID attribute equals to 0
     */
    @Test
    public void testRequestWithRequestIdEquals0()
    {
        testParsingFail( BatchRequestTest.class, "request_with_requestID_equals_0.xml" );
    }


    /**
     * Test parsing of a Request with the (optional) requestID attribute
     */
    @Test
    public void testResponseWith0Request()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_requestID_attribute.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 0, batchRequest.getRequests().size() );
    }


    /**
     * Test parsing of a Request with 1 AuthRequest
     */
    @Test
    public void testResponseWith1AuthRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_AuthRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof BindRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 AddRequest
     */
    @Test
    public void testResponseWith1AddRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_AddRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof AddRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 CompareRequest
     */
    @Test
    public void testResponseWith1CompareRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_CompareRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof CompareRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 AbandonRequest
     */
    @Test
    public void testResponseWith1AbandonRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_AbandonRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof AbandonRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 DelRequest
     */
    @Test
    public void testResponseWith1DelRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_DelRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof DelRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 ExtendedRequest
     */
    @Test
    public void testResponseWith1ExtendedRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_ExtendedRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof ExtendedRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 ModDNRequest
     */
    @Test
    public void testResponseWith1ModDNRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_ModDNRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof ModifyDNRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 ModifyRequest
     */
    @Test
    public void testResponseWith1ModifyRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_ModifyRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof ModifyRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 SearchRequest
     */
    @Test
    public void testResponseWith1SearchRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_SearchRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 1, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof SearchRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 2 AddRequest
     */
    @Test
    public void testResponseWith2AddRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_2_AddRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 2, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof AddRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 2 CompareRequest
     */
    @Test
    public void testResponseWith2CompareRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_2_CompareRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 2, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof CompareRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 2 AbandonRequest
     */
    @Test
    public void testResponseWith2AbandonRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_2_AbandonRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 2, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof AbandonRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 2 DelRequest
     */
    @Test
    public void testResponseWith2DelRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_2_DelRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 2, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof DelRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 2 ExtendedRequest
     */
    @Test
    public void testResponseWith2ExtendedRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_2_ExtendedRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 2, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof ExtendedRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 2 ModDNRequest
     */
    @Test
    public void testResponseWith2ModDNRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_2_ModDNRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 2, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof ModifyDNRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 2 ModifyRequest
     */
    @Test
    public void testResponseWith2ModifyRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_2_ModifyRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 2, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof ModifyRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 2 SearchRequest
     */
    @Test
    public void testResponseWith2SearchRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_2_SearchRequest.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        assertEquals( 2, batchRequest.getRequests().size() );

        LdapMessageCodec request = batchRequest.getCurrentRequest();

        if ( request instanceof SearchRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a Request with 1 AuthRequest and 1 AddRequest
     */
    @Test
    public void testResponseWith1AuthRequestAnd1AddRequest()
    {
        Dsmlv2Parser parser = null;
        try
        {
            parser = new Dsmlv2Parser();

            parser.setInput( BatchRequestTest.class.getResource( "request_with_1_AuthRequest_1_AddRequest.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BatchRequest batchRequest = parser.getBatchRequest();

        List requests = batchRequest.getRequests();

        assertEquals( 2, requests.size() );

        LdapMessageCodec request = ( LdapMessageCodec ) requests.get( 0 );

        if ( request instanceof BindRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }

        request = ( LdapMessageCodec ) requests.get( 1 );

        if ( request instanceof AddRequestCodec )
        {
            assertTrue( true );
        }
        else
        {
            fail();
        }
    }


    /**
     * Test parsing of a request with 1 wrong placed AuthRequest
     */
    @Test
    public void testRequestWithWrongPlacedAuthRequest()
    {
        testParsingFail( BatchRequestTest.class, "request_with_wrong_placed_AuthRequest.xml" );
    }
}
