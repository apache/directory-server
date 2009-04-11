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
package org.apache.directory.shared.ldap.message;


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedResponse;

import org.apache.directory.shared.ldap.message.InternalExtendedRequest;
import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.InternalResultResponse;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * TestCase for the ExtendedRequestImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class ExtendedRequestImplTest
{
    private static final Map<String, Control> EMPTY_CONTROL_MAP = new HashMap<String, Control>();

    /**
     * Tests the same object referrence for equality.
     */
    @Test
    public void testEqualsSameObj()
    {
        ExtendedRequestImpl req = new ExtendedRequestImpl( 5 );
        assertTrue( req.equals( req ) );
    }


    /**
     * Tests for equality using exact copies.
     */
    @Test
    public void testEqualsExactCopy()
    {
        ExtendedRequestImpl req0 = new ExtendedRequestImpl( 5 );
        req0.setOid( "1.1.1.1" );
        req0.setPayload( "Hello World!".getBytes() );

        ExtendedRequestImpl req1 = new ExtendedRequestImpl( 5 );
        req1.setOid( "1.1.1.1" );
        req1.setPayload( "Hello World!".getBytes() );

        assertTrue( req0.equals( req1 ) );
        assertTrue( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the IDs are different.
     */
    @Test
    public void testNotEqualDiffId()
    {
        ExtendedRequestImpl req0 = new ExtendedRequestImpl( 7 );
        ExtendedRequestImpl req1 = new ExtendedRequestImpl( 5 );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the OID is different.
     */
    @Test
    public void testNotEqualDiffOID()
    {
        ExtendedRequestImpl req0 = new ExtendedRequestImpl( 5 );
        req0.setOid( "1.1.1.1" );
        req0.setPayload( "Hello World!".getBytes() );

        ExtendedRequestImpl req1 = new ExtendedRequestImpl( 5 );
        req0.setOid( "1.2.2.1" );
        req0.setPayload( "Hello World!".getBytes() );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the Assertion values are different.
     */
    @Test
    public void testNotEqualDiffValue()
    {
        ExtendedRequestImpl req0 = new ExtendedRequestImpl( 5 );
        req0.setOid( "1.1.1.1" );
        req0.setPayload( "Hello ".getBytes() );

        ExtendedRequestImpl req1 = new ExtendedRequestImpl( 5 );
        req0.setOid( "1.1.1.1" );
        req0.setPayload( "World!".getBytes() );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Tests for equality even when another ExtendedRequest implementation is
     * used.
     */
    @Test
    public void testEqualsDiffImpl()
    {
        InternalExtendedRequest req0 = new InternalExtendedRequest()
        {
            private static final long serialVersionUID = 1L;


            public String getOid()
            {
                return null;
            }


            public void setOid( String oid )
            {
            }


            public byte[] getPayload()
            {
                return null;
            }


            public void setPayload( byte[] payload )
            {
            }


            public MessageTypeEnum getResponseType()
            {
                return MessageTypeEnum.EXTENDED_RESP;
            }


            public boolean hasResponse()
            {
                return true;
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.EXTENDED_REQ;
            }


            public Map<String,Control> getControls()
            {
                return EMPTY_CONTROL_MAP;
            }


            public void add( Control control ) throws MessageException
            {
            }


            public void remove( Control control ) throws MessageException
            {
            }


            public int getMessageId()
            {
                return 5;
            }


            public Object get( Object key )
            {
                return null;
            }


            public Object put( Object key, Object value )
            {
                return null;
            }


            public InternalResultResponse getResultResponse()
            {
                return null;
            }


            public String getID()
            {
                return null;
            }


            public byte[] getEncodedValue()
            {
                return null;
            }


            public ExtendedResponse createExtendedResponse( String id, byte[] berValue, int offset, int length )
                throws NamingException
            {
                return null;
            }


            public void addAll( Control[] controls ) throws MessageException
            {
            }


            public boolean hasControl( String oid )
            {
                return false;
            }
        };

        ExtendedRequestImpl req1 = new ExtendedRequestImpl( 5 );
        assertTrue( req1.equals( req0 ) );
        assertFalse( req0.equals( req1 ) );
    }
}
