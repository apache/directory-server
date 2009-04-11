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

import javax.naming.InvalidNameException;
import javax.naming.ldap.Control;

import org.apache.directory.shared.ldap.message.InternalBindRequest;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.InternalResultResponse;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * TestCases for the methods of the BindRequestImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class BindRequestImplTest
{
    private static final Map<String, Control> EMPTY_CONTROL_MAP = new HashMap<String, Control>();

    /**
     * Tests the same object referrence for equality.
     */
    @Test
    public void testEqualsSameObj()
    {
        BindRequestImpl req = new BindRequestImpl( 5 );
        assertTrue( req.equals( req ) );
    }


    /**
     * Tests for equality using exact copies.
     */
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
    public void testEqualsDiffImpl()
    {
        InternalBindRequest req0 = new InternalBindRequest()
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


            public Map<String, Control> getControls()
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


            public InternalResultResponse getResultResponse()
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


            public void abandon()
            {
            }


            public void addAbandonListener( AbandonListener listener )
            {
            }


            public boolean isAbandoned()
            {
                return false;
            }
        };

        BindRequestImpl req1 = new BindRequestImpl( 5 );
        assertTrue( req1.equals( req0 ) );
    }
}
