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


import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.message.BindResponseImpl;
import org.apache.directory.shared.ldap.message.LdapResultImpl;
import org.apache.directory.shared.ldap.message.Referral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;

import junit.framework.TestCase;


/**
 * Tests the methods of the BindResponseImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class BindResponseImplTest extends TestCase

{
    /**
     * Tests to make sure the same object returns true with equals().
     */
    public void testEqualsSameObj()
    {
        BindResponseImpl resp = new BindResponseImpl( 1 );
        assertTrue( "same object should be equal", resp.equals( resp ) );
    }


    /**
     * Tests to make sure newly created objects with same id are equal.
     */
    public void testEqualsNewWithSameId()
    {
        BindResponseImpl resp0 = new BindResponseImpl( 1 );
        BindResponseImpl resp1 = new BindResponseImpl( 1 );
        assertTrue( "default copy with same id should be equal", resp0.equals( resp1 ) );
        assertTrue( "default copy with same id should be equal", resp1.equals( resp0 ) );
    }


    /**
     * Tests to make sure newly created objects with same different id are not
     * equal.
     */
    public void testNotEqualsNewWithDiffId()
    {
        BindResponseImpl resp0 = new BindResponseImpl( 1 );
        BindResponseImpl resp1 = new BindResponseImpl( 2 );
        assertFalse( "different id objects should not be equal", resp0.equals( resp1 ) );
        assertFalse( "different id objects should not be equal", resp1.equals( resp0 ) );
    }


    /**
     * Tests to make sure newly created objects with same different saslCreds
     * are not equal.
     */
    public void testNotEqualsNewWithDiffSaslCreds()
    {
        BindResponseImpl resp0 = new BindResponseImpl( 1 );
        resp0.setServerSaslCreds( new byte[2] );
        BindResponseImpl resp1 = new BindResponseImpl( 1 );
        resp1.setServerSaslCreds( new byte[3] );
        assertFalse( "different serverSaslCreds objects should not be equal", resp0.equals( resp1 ) );
        assertFalse( "different serverSaslCreds objects should not be equal", resp1.equals( resp0 ) );
    }


    /**
     * Tests for equality of two fully loaded identical BindResponse PDUs.
     */
    public void testEqualsWithTheWorks() throws InvalidNameException
    {
        LdapResultImpl r0 = new LdapResultImpl();
        LdapResultImpl r1 = new LdapResultImpl();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        r1.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );

        r0.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIMELIMITEXCEEDED );

        Referral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        Referral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        BindResponseImpl resp0 = new BindResponseImpl( 1 );
        BindResponseImpl resp1 = new BindResponseImpl( 1 );

        resp0.setServerSaslCreds( "password".getBytes() );
        resp1.setServerSaslCreds( "password".getBytes() );

        assertTrue( "loaded carbon copies should be equal", resp0.equals( resp1 ) );
        assertTrue( "loaded carbon copies should be equal", resp1.equals( resp0 ) );
    }
}
