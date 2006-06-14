/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.message;


import junit.framework.TestCase;

import java.util.Collections;
import java.util.Map;

import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.message.Control;
import org.apache.directory.shared.ldap.message.ExtendedResponse;
import org.apache.directory.shared.ldap.message.ExtendedResponseImpl;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.LdapResultImpl;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * TestCase for the ExtendedResponseImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExtendedResponseImplTest extends TestCase
{
    /**
     * Creates and populates a ExtendedResponseImpl stub for testing purposes.
     * 
     * @return a populated ExtendedResponseImpl stub
     */
    private ExtendedResponseImpl createStub()
    {
        // Construct the Search response to test with results and referrals
        ExtendedResponseImpl response = new ExtendedResponseImpl( 45 );
        response.setResponse( "Hello World!".getBytes() );
        response.setResponseName( "1.1.1.1" );
        LdapResult result = response.getLdapResult();
        
        try
        {
            result.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        }
        catch ( InvalidNameException ine )
        {
            // Do nothing
        }
        
        result.setResultCode( ResultCodeEnum.SUCCESS );
        ReferralImpl refs = new ReferralImpl();
        refs.addLdapUrl( "ldap://someserver.com" );
        refs.addLdapUrl( "ldap://apache.org" );
        refs.addLdapUrl( "ldap://another.net" );
        result.setReferral( refs );
        return response;
    }


    /**
     * Tests for equality using the same object.
     */
    public void testEqualsSameObj()
    {
        ExtendedResponseImpl resp = createStub();
        assertTrue( resp.equals( resp ) );
    }


    /**
     * Tests for equality using an exact copy.
     */
    public void testEqualsExactCopy()
    {
        ExtendedResponseImpl resp0 = createStub();
        ExtendedResponseImpl resp1 = createStub();
        assertTrue( resp0.equals( resp1 ) );
        assertTrue( resp1.equals( resp0 ) );
    }


    /**
     * Tests for equality using different stub implementations.
     */
    public void testEqualsDiffImpl()
    {
        ExtendedResponseImpl resp0 = createStub();
        ExtendedResponse resp1 = new ExtendedResponse()
        {
            private static final long serialVersionUID = 5297000474419901408L;


            public String getResponseName()
            {
                return "1.1.1.1";
            }


            public void setResponseName( String a_oid )
            {
            }


            public byte[] getResponse()
            {
                return "Hello World!".getBytes();
            }


            public void setResponse( byte[] a_value )
            {
            }


            public LdapResult getLdapResult()
            {
                LdapResultImpl result = new LdapResultImpl();
                
                try 
                {
                    result.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
                }
                catch ( InvalidNameException ine ) 
                {
                    // do nothing
                }
                
                result.setResultCode( ResultCodeEnum.SUCCESS );
                ReferralImpl refs = new ReferralImpl();
                refs.addLdapUrl( "ldap://someserver.com" );
                refs.addLdapUrl( "ldap://apache.org" );
                refs.addLdapUrl( "ldap://another.net" );
                result.setReferral( refs );

                return result;
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.EXTENDEDRESP;
            }


            public Map getControls()
            {
                return Collections.EMPTY_MAP;
            }


            public void add( Control a_control ) throws MessageException
            {
            }


            public void remove( Control a_control ) throws MessageException
            {
            }


            public int getMessageId()
            {
                return 45;
            }


            public Object get( Object a_key )
            {
                return null;
            }


            public Object put( Object a_key, Object a_value )
            {
                return null;
            }


            public String getID()
            {
                return "1.1.1.1";
            }


            public byte[] getEncodedValue()
            {
                return getResponse();
            }
        };

        assertTrue( resp0.equals( resp1 ) );
        assertFalse( resp1.equals( resp0 ) );
    }


    /**
     * Tests inequality when messageIds are different.
     */
    public void testNotEqualsDiffIds()
    {
        ExtendedResponseImpl resp0 = new ExtendedResponseImpl( 3 );
        ExtendedResponseImpl resp1 = new ExtendedResponseImpl( 4 );

        assertFalse( resp0.equals( resp1 ) );
        assertFalse( resp1.equals( resp0 ) );
    }


    /**
     * Tests inequality when responseNames are different.
     */
    public void testNotEqualsDiffNames()
    {
        ExtendedResponseImpl resp0 = createStub();
        resp0.setResponseName( "1.2.3.4" );
        ExtendedResponseImpl resp1 = createStub();
        resp1.setResponseName( "1.2.3.4.5" );

        assertFalse( resp0.equals( resp1 ) );
        assertFalse( resp1.equals( resp0 ) );
    }


    /**
     * Tests inequality when responses are different.
     */
    public void testNotEqualsDiffResponses()
    {
        ExtendedResponseImpl resp0 = createStub();
        resp0.setResponse( "abc".getBytes() );
        ExtendedResponseImpl resp1 = createStub();
        resp1.setResponse( "123".getBytes() );

        assertFalse( resp0.equals( resp1 ) );
        assertFalse( resp1.equals( resp0 ) );
    }
}
