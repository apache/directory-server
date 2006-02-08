/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.codec.util;


import java.io.UnsupportedEncodingException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.codec.util.LdapStringEncodingException;


/**
 * Test the class LdapString
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapStringTest extends TestCase
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Setup the test
     */
    protected void setUp()
    {
    }


    // CONSTRUCTOR functions --------------------------------------------------

    /**
     * Test a null LdapString
     */
    public void testLdapStringNull() throws LdapStringEncodingException
    {
        LdapString ls = new LdapString();

        Assert.assertEquals( "", ls.getString() );
    }


    /**
     * Test an empty LdapString
     */
    public void testLdapStringEmpty() throws LdapStringEncodingException, UnsupportedEncodingException
    {
        LdapString ls = new LdapString( "".getBytes( "UTF-8" ) );

        Assert.assertEquals( "", ls.getString() );
    }


    /**
     * Test an ASCII LdapString
     */
    public void testLdapStringASCII() throws LdapStringEncodingException, UnsupportedEncodingException
    {
        LdapString ls = new LdapString( "azerty".getBytes( "UTF-8" ) );

        Assert.assertEquals( "azerty", ls.getString() );
    }


    /**
     * Test a european LdapString
     */
    public void testLdapStringEuropean() throws LdapStringEncodingException, UnsupportedEncodingException
    {
        LdapString ls = new LdapString( "J\u00e9r\u00f4me".getBytes( "UTF-8" ) );

        Assert.assertEquals( "J\u00e9r\u00f4me", ls.getString() );
        Assert.assertEquals( 8, ls.getNbBytes() );
        Assert.assertEquals( "J\u00e9r\u00f4me", new String( ls.getBytes(), "UTF-8" ) );
    }


    /**
     * Test the bytes LdapString constructor
     */
    public void testLdapStringBytes() throws LdapStringEncodingException, UnsupportedEncodingException
    {
        LdapString ls = new LdapString( new byte[]
            { 'J', ( byte ) 0xc3, ( byte ) 0xa9, 'r', ( byte ) 0xc3, ( byte ) 0xb4, 'm', 'e' } );

        Assert.assertEquals( "J\u00e9r\u00f4me", ls.getString() );
        Assert.assertEquals( 8, ls.getNbBytes() );
        Assert.assertEquals( "J\u00e9r\u00f4me", new String( ls.getBytes(), "UTF-8" ) );
    }


    /**
     * Test a wrong LdapString
     */
    public void testWrongLdapStringBytes() throws LdapStringEncodingException, UnsupportedEncodingException
    {
        LdapString ls = new LdapString( new byte[]
            { 'J', ( byte ) 0xe3, ( byte ) 0xc9, 'r', ( byte ) 0xc3, ( byte ) 0xb4, 'm', 'e' } );
        Assert.assertEquals( "J\ufffd\ufffdr\u00f4me", ls.getString() );
    }
}
