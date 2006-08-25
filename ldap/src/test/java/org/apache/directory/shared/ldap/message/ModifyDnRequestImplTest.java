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

import org.apache.directory.shared.ldap.message.AbandonListener;
import org.apache.directory.shared.ldap.message.Control;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyDnRequestImpl;
import org.apache.directory.shared.ldap.message.ResultResponse;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;


/**
 * TestCase for the ModifyDnRequestImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ModifyDnRequestImplTest extends TestCase
{
    /**
     * Constructs a ModifyDnrequest to test.
     * 
     * @return the request
     */
    public ModifyDnRequestImpl getRequest()
    {
        // Construct the ModifyDn request to test
        ModifyDnRequestImpl request = new ModifyDnRequestImpl( 45 );
        request.setDeleteOldRdn( true );
        
        try
        {
            request.setName( new LdapDN( "dc=admins,dc=apache,dc=org" ) );
            request.setNewRdn( new Rdn( "dc=administrators" ) );
            request.setNewSuperior( new LdapDN( "dc=groups,dc=apache,dc=org" ) );
        }
        catch ( InvalidNameException ine )
        {
            // do nothing
        }
        
        return request;
    }


    /**
     * Tests the same object referrence for equality.
     */
    public void testEqualsSameObj()
    {
        ModifyDnRequestImpl req = new ModifyDnRequestImpl( 5 );
        assertTrue( req.equals( req ) );
    }


    /**
     * Tests for equality using exact copies.
     */
    public void testEqualsExactCopy0()
    {
        ModifyDnRequestImpl req0 = getRequest();
        ModifyDnRequestImpl req1 = getRequest();

        assertTrue( req0.equals( req1 ) );
    }


    /**
     * Tests for equality using exact copies.
     */
    public void testEqualsExactCopy1()
    {
        ModifyDnRequestImpl req0 = getRequest();
        req0.setNewSuperior( null );
        ModifyDnRequestImpl req1 = getRequest();
        req1.setNewSuperior( null );

        assertTrue( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the IDs are different.
     */
    public void testNotEqualDiffId()
    {
        ModifyDnRequestImpl req0 = new ModifyDnRequestImpl( 4 );
        ModifyDnRequestImpl req1 = new ModifyDnRequestImpl( 5 );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the DN names are different.
     */
    public void testNotEqualDiffName() throws InvalidNameException
    {
        ModifyDnRequestImpl req0 = getRequest();
        req0.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );

        ModifyDnRequestImpl req1 = getRequest();
        req1.setName( new LdapDN( "cn=admin,dc=apache,dc=org" ) );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the newSuperior DNs are different.
     */
    public void testNotEqualDiffNewSuperior() throws InvalidNameException
    {
        ModifyDnRequestImpl req0 = getRequest();
        req0.setNewSuperior( new LdapDN( "cn=admin,dc=example,dc=com" ) );

        ModifyDnRequestImpl req1 = getRequest();
        req1.setNewSuperior( new LdapDN( "cn=admin,dc=apache,dc=org" ) );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the delete old Rdn properties is different.
     */
    public void testNotEqualDiffDeleteOldRdn()
    {
        ModifyDnRequestImpl req0 = getRequest();
        req0.setDeleteOldRdn( true );

        ModifyDnRequestImpl req1 = getRequest();
        req1.setDeleteOldRdn( false );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the new Rdn properties are different.
     */
    public void testNotEqualDiffNewRdn() throws InvalidNameException
    {
        ModifyDnRequestImpl req0 = getRequest();
        req0.setNewRdn( new Rdn( "cn=admin0" ) );

        ModifyDnRequestImpl req1 = getRequest();
        req1.setNewRdn( new Rdn( "cn=admin1" ) );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Tests for equality even when another BindRequest implementation is used.
     */
    public void testEqualsDiffImpl()
    {
        ModifyDnRequest req0 = new ModifyDnRequest()
        {
            public LdapDN getName()
            {
                try
                {
                    return new LdapDN( "dc=admins,dc=apache,dc=org" );
                }
                catch ( InvalidNameException ine )
                {
                    // do nothing
                    return null;
                }
            }


            public void setName( LdapDN name )
            {
            }


            public Rdn getNewRdn()
            {
                try
                {
                    return new Rdn( "dc=administrators" );
                }
                catch ( InvalidNameException ine )
                {
                    // do nothing
                    return null;
                }
            }


            public void setNewRdn( Rdn newRdn )
            {
            }


            public boolean getDeleteOldRdn()
            {
                return true;
            }


            public void setDeleteOldRdn( boolean deleteOldRdn )
            {
            }


            public LdapDN getNewSuperior()
            {
                try
                {
                    return new LdapDN( "dc=groups,dc=apache,dc=org" );
                }
                catch ( InvalidNameException ine )
                {
                    // do nothing
                    return null;
                }
            }


            public void setNewSuperior( LdapDN newSuperior )
            {
            }


            public boolean isMove()
            {
                return false;
            }


            public MessageTypeEnum getResponseType()
            {
                return MessageTypeEnum.MODDNRESPONSE;
            }


            public boolean hasResponse()
            {
                return true;
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.MODDNREQUEST;
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


            public void abandon()
            {
            }


            public boolean isAbandoned()
            {
                return false;
            }


            public void addAbandonListener( AbandonListener listener )
            {
            }


            public ResultResponse getResultResponse()
            {
                return null;
            }
        };

        ModifyDnRequestImpl req1 = getRequest();
        assertTrue( req1.equals( req0 ) );
    }
}
