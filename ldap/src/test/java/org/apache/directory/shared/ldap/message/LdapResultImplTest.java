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


import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.message.InternalLdapResult;
import org.apache.directory.shared.ldap.message.LdapResultImpl;
import org.apache.directory.shared.ldap.message.InternalReferral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Tests the methods of the LdapResultImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *         $Rev$
 */
public class LdapResultImplTest
{
    /**
     * Tests to make sure the two same objects are seen as equal.
     */
    @Test
    public void testEqualsSameObj()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        assertTrue( "same object should be equal", r0.equals( r0 ) );
    }


    /**
     * Tests to make sure a default LdapResultImpl equals another one just
     * created.
     */
    @Test
    public void testEqualsDefaultCopy()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        assertTrue( "default copy should be equal", r0.equals( r1 ) );
        assertTrue( "default copy should be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for equality when the lockable parent is not the same.
     */
    @Test
    public void testEqualsDiffLockableParent()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        assertTrue( "default copy with different lockable parents " + "should be equal", r0.equals( r1 ) );
        assertTrue( "default copy with different lockable parents " + "should be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for equality when the lockable parent is the same.
     */
    @Test
    public void testEqualsDiffImpl()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        InternalLdapResult r1 = new InternalLdapResult()
        {
            public ResultCodeEnum getResultCode()
            {
                return ResultCodeEnum.SUCCESS;
            }


            public void setResultCode( ResultCodeEnum a_resultCode )
            {
            }


            public LdapDN getMatchedDn()
            {
                return null;
            }


            public void setMatchedDn( LdapDN dn )
            {
            }


            public String getErrorMessage()
            {
                return null;
            }


            public void setErrorMessage( String a_errorMessage )
            {
            }


            public boolean isReferral()
            {
                return false;
            }


            public InternalReferral getReferral()
            {
                return null;
            }


            public void setReferral( InternalReferral a_referral )
            {
            }
        };

        assertTrue( "r0 equals should see other impl r1 as equal", r0.equals( r1 ) );
        assertFalse( "r1 impl uses Object.equals() so it should not see " + "r0 as the same object", r1.equals( r0 ) );
    }


    /**
     * Tests two non default carbon copies for equality.
     */
    @Test
    public void testEqualsCarbonCopy() throws InvalidNameException
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        r1.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );

        r0.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );

        InternalReferral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        InternalReferral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertTrue( "exact copy should be equal", r0.equals( r1 ) );
        assertTrue( "exact copy should be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for inequality when the error message is different.
     */
    @Test
    public void testNotEqualsDiffErrorMessage() throws InvalidNameException
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah" );

        r0.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        r1.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );

        r0.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );

        InternalReferral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        InternalReferral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( "results with different error messages should " + "not be equal", r0.equals( r1 ) );
        assertFalse( "results with different error messages should " + "not be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for inequality when the matchedDn properties are not the same.
     */
    @Test
    public void testNotEqualsDiffMatchedDn() throws InvalidNameException
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        r1.setMatchedDn( new LdapDN( "dc=apache,dc=org")  );

        r0.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );

        InternalReferral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        InternalReferral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( "results with different matchedDn properties " + "should not be equal", r0.equals( r1 ) );
        assertFalse( "results with different matchedDn properties " + "should not be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for inequality when the resultCode properties are not the same.
     */
    @Test
    public void testNotEqualsDiffResultCode() throws InvalidNameException
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        r1.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );

        r0.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );
        r1.setResultCode( ResultCodeEnum.SIZE_LIMIT_EXCEEDED );

        InternalReferral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        InternalReferral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( "results with different result codes should not be equal", r0.equals( r1 ) );
        assertFalse( "results with different result codes should not be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for inequality when the referrals are not the same.
     */
    @Test
    public void testNotEqualsDiffReferrals() throws InvalidNameException
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        r1.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );

        r0.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );

        InternalReferral refs0 = new ReferralImpl();
        r0.setReferral( refs0 );
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        InternalReferral refs1 = new ReferralImpl();
        r1.setReferral( refs1 );
        refs1.addLdapUrl( "ldap://abc.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( "results with different referrals should not be equal", r0.equals( r1 ) );
        assertFalse( "results with different referrals should not be equal", r1.equals( r0 ) );
    }
}
