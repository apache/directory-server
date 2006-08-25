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


import junit.framework.TestCase;

import java.util.Collections;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.ldap.ExtendedResponse;

import org.apache.directory.shared.ldap.message.Control;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.ResultResponse;


/**
 * TestCase for the ExtendedRequestImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class ExtendedRequestImplTest extends TestCase
{
    /**
     * Tests the same object referrence for equality.
     */
    public void testEqualsSameObj()
    {
        ExtendedRequestImpl req = new ExtendedRequestImpl( 5 );
        assertTrue( req.equals( req ) );
    }


    /**
     * Tests for equality using exact copies.
     */
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
    public void testEqualsDiffImpl()
    {
        ExtendedRequest req0 = new ExtendedRequest()
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
                return MessageTypeEnum.EXTENDEDRESP;
            }


            public boolean hasResponse()
            {
                return true;
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.EXTENDEDREQ;
            }


            public Map getControls()
            {
                return Collections.EMPTY_MAP;
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


            public ResultResponse getResultResponse()
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
        };

        ExtendedRequestImpl req1 = new ExtendedRequestImpl( 5 );
        assertTrue( req1.equals( req0 ) );
        assertFalse( req0.equals( req1 ) );
    }
}
