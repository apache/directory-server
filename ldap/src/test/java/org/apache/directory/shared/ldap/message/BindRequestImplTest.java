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

import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.message.Control;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.ResultResponse;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * TestCases for the methods of the BindRequestImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class BindRequestImplTest extends TestCase
{
    /**
     * Tests the same object referrence for equality.
     */
    public void testEqualsSameObj()
    {
        BindRequestImpl req = new BindRequestImpl( 5 );
        assertTrue( req.equals( req ) );
    }


    /**
     * Tests for equality using exact copies.
     */
    public void testEqualsExactCopy() throws InvalidNameException
    {
        BindRequestImpl req0 = new BindRequestImpl( 5 );
        req0.setCredentials( "password".getBytes() );
        req0.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req0.setSimple( true );
        req0.setVersion3( true );

        BindRequestImpl req1 = new BindRequestImpl( 5 );
        req1.setCredentials( "password".getBytes() );
        req1.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req1.setSimple( true );
        req1.setVersion3( true );

        assertTrue( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the IDs are different.
     */
    public void testNotEqualDiffId() throws InvalidNameException
    {
        BindRequestImpl req0 = new BindRequestImpl( 7 );
        req0.setCredentials( "password".getBytes() );
        req0.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req0.setSimple( true );
        req0.setVersion3( true );

        BindRequestImpl req1 = new BindRequestImpl( 5 );
        req1.setCredentials( "password".getBytes() );
        req1.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req1.setSimple( true );
        req1.setVersion3( true );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the credentials are different.
     */
    public void testNotEqualDiffCreds() throws InvalidNameException
    {
        BindRequestImpl req0 = new BindRequestImpl( 5 );
        req0.setCredentials( "abcdefg".getBytes() );
        req0.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req0.setSimple( true );
        req0.setVersion3( true );

        BindRequestImpl req1 = new BindRequestImpl( 5 );
        req1.setCredentials( "password".getBytes() );
        req1.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req1.setSimple( true );
        req1.setVersion3( true );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the DN names are different.
     */
    public void testNotEqualDiffName() throws InvalidNameException
    {
        BindRequestImpl req0 = new BindRequestImpl( 5 );
        req0.setCredentials( "password".getBytes() );
        req0.setName( new LdapDN( "uid=akarasulu,dc=example,dc=com" ) );
        req0.setSimple( true );
        req0.setVersion3( true );

        BindRequestImpl req1 = new BindRequestImpl( 5 );
        req1.setCredentials( "password".getBytes() );
        req1.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req1.setSimple( true );
        req1.setVersion3( true );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the auth mechanisms are different.
     */
    public void testNotEqualDiffSimple() throws InvalidNameException
    {
        BindRequestImpl req0 = new BindRequestImpl( 5 );
        req0.setCredentials( "password".getBytes() );
        req0.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req0.setSimple( false );
        req0.setVersion3( true );

        BindRequestImpl req1 = new BindRequestImpl( 5 );
        req1.setCredentials( "password".getBytes() );
        req1.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req1.setSimple( true );
        req1.setVersion3( true );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the bind LDAP versions are different.
     */
    public void testNotEqualDiffVersion() throws InvalidNameException
    {
        BindRequestImpl req0 = new BindRequestImpl( 5 );
        req0.setCredentials( "password".getBytes() );
        req0.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req0.setSimple( true );
        req0.setVersion3( false );

        BindRequestImpl req1 = new BindRequestImpl( 5 );
        req1.setCredentials( "password".getBytes() );
        req1.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req1.setSimple( true );
        req1.setVersion3( true );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Tests for equality even when another BindRequest implementation is used.
     */
    public void testEqualsDiffImpl()
    {
        BindRequest req0 = new BindRequest()
        {
            public boolean isSimple()
            {
                return true;
            }


            public boolean getSimple()
            {
                return true;
            }


            public void setSimple( boolean a_isSimple )
            {
            }


            public byte[] getCredentials()
            {
                return null;
            }


            public void setCredentials( byte[] a_credentials )
            {
            }


            public LdapDN getName()
            {
                return null;
            }


            public void setName( LdapDN name )
            {
            }


            public boolean isVersion3()
            {
                return true;
            }


            public boolean getVersion3()
            {
                return true;
            }


            public void setVersion3( boolean a_isVersion3 )
            {
            }


            public MessageTypeEnum getResponseType()
            {
                return MessageTypeEnum.BIND_REQUEST;
            }


            public boolean hasResponse()
            {
                return true;
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.BIND_REQUEST;
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
                return 5;
            }


            public Object get( Object a_key )
            {
                return null;
            }


            public Object put( Object a_key, Object a_value )
            {
                return null;
            }


            public String getSaslMechanism()
            {
                return null;
            }


            public void setSaslMechanism( String saslMechanism )
            {
            }


            public ResultResponse getResultResponse()
            {
                return null;
            }
        };

        BindRequestImpl req1 = new BindRequestImpl( 5 );
        assertTrue( req1.equals( req0 ) );
    }
}
