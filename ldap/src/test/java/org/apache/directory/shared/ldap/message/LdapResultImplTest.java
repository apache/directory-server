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


import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.LdapResultImpl;
import org.apache.directory.shared.ldap.message.Referral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;

import junit.framework.TestCase;


/**
 * Tests the methods of the LdapResultImpl class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a> $Rev$
 */
public class LdapResultImplTest extends TestCase
{
    /**
     * Tests to make sure the two same objects are seen as equal.
     */
    public void testEqualsSameObj()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        assertTrue( "same object should be equal", r0.equals( r0 ) );
    }


    /**
     * Tests to make sure a default LdapResultImpl equals another one just
     * created.
     */
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
    public void testEqualsDiffLockableParent()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        assertTrue( "default copy with different lockable parents " +
                "should be equal", r0.equals( r1 ) );
        assertTrue( "default copy with different lockable parents " +
                "should be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for equality when the lockable parent is the same.
     */
    public void testEqualsDiffImpl()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResult r1 = new LdapResult()
        {
            public ResultCodeEnum getResultCode()
            {
                return ResultCodeEnum.SUCCESS;
            }

            public void setResultCode( ResultCodeEnum a_resultCode )
            {
            }

            public String getMatchedDn()
            {
                return null;
            }

            public void setMatchedDn( String a_dn )
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

            public Referral getReferral()
            {
                return null;
            }

            public void setReferral( Referral a_referral )
            {
            }
        };

        assertTrue( "r0 equals should see other impl r1 as equal",
                r0.equals( r1 ) );
        assertFalse( "r1 impl uses Object.equals() so it should not see " +
                "r0 as the same object", r1.equals( r0 ) );
    }


    /**
     * Tests two non default carbon copies for equality.
     */
    public void testEqualsCarbonCopy()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( "dc=example,dc=com" );
        r1.setMatchedDn( "dc=example,dc=com" );

        r0.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );

        Referral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        Referral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertTrue( "exact copy should be equal", r0.equals( r1 ) );
        assertTrue( "exact copy should be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for inequality when the error message is different.
     */
    public void testNotEqualsDiffErrorMessage()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah" );

        r0.setMatchedDn( "dc=example,dc=com" );
        r1.setMatchedDn( "dc=example,dc=com" );

        r0.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );

        Referral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        Referral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( "results with different error messages should " +
                "not be equal", r0.equals( r1 ) );
        assertFalse( "results with different error messages should " +
                "not be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for inequality when the matchedDn properties are not the same.
     */
    public void testNotEqualsDiffMatchedDn()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( "dc=example,dc=com" );
        r1.setMatchedDn( "dc=apache,dc=org" );

        r0.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );

        Referral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        Referral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( "results with different matchedDn properties " +
                "should not be equal", r0.equals( r1 ) );
        assertFalse( "results with different matchedDn properties " +
                "should not be equal", r1.equals( r0 ) );
    }


    /**
     * Tests for inequality when the resultCode properties are not the same.
     */
    public void testNotEqualsDiffResultCode()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( "dc=example,dc=com" );
        r1.setMatchedDn( "dc=example,dc=com" );

        r0.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );
        r1.setResultCode( ResultCodeEnum.SIZELIMITEXCEEDED );

        Referral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        Referral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( "results with different result codes should not be equal",
                r0.equals( r1 ) );
        assertFalse( "results with different result codes should not be equal",
                r1.equals( r0 ) );
    }


    /**
     * Tests for inequality when the referrals are not the same.
     */
    public void testNotEqualsDiffReferrals()
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( "dc=example,dc=com" );
        r1.setMatchedDn( "dc=example,dc=com" );

        r0.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );

        Referral refs0 = new ReferralImpl();
        r0.setReferral( refs0 );
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        Referral refs1 = new ReferralImpl();
        r1.setReferral( refs1 );
        refs1.addLdapUrl( "ldap://abc.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( "results with different referrals should not be equal",
                r0.equals( r1 ) );
        assertFalse( "results with different referrals should not be equal",
                r1.equals( r0 ) );
    }
}
