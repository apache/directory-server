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
package org.apache.directory.shared.ldap.codec.util;


import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;


/**
 * Test the class LdapURL
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapUrlTest extends TestCase
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Setup the test
     */
    protected void setUp()
    {
    }


    /**
     * Test a null LdapURL
     */
    public void testLdapUrlNull() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap:///", new LdapURL().toString() );
    }


    /**
     * test an empty LdapURL
     */
    public void testLdapDNEmpty() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap:///", new LdapURL( "" ).toString() );
    }


    /**
     * test a simple LdapURL
     */
    public void testLdapDNSimple() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://directory.apache.org:80/", new LdapURL( "ldap://directory.apache.org:80/" )
            .toString() );
    }


    /**
     * test a LdapURL host 1
     */
    public void testLdapDNWithMinus() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://d-a.org:80/", new LdapURL( "ldap://d-a.org:80/" ).toString() );
    }


    /**
     * test a LdapURL with a bad port
     */
    public void testLdapDNBadPort() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://directory.apache.org:/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad port 2
     */
    public void testLdapDNBadPort2() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://directory.apache.org:-1/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad port 3
     */
    public void testLdapDNBadPort3() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://directory.apache.org:abc/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad port 4
     */
    public void testLdapDNBadPort4() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://directory.apache.org:65536/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with no host
     */
    public void testLdapDNBadHost1() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap:///", new LdapURL( "ldap:///" ).toString() );
    }


    /**
     * test a LdapURL with a bad host 2
     */
    public void testLdapDNBadHost2() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://./" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad host 3
     */
    public void testLdapDNBadHost3() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://a..b/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad host 4
     */
    public void testLdapDNBadHost4() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://-/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad host 5
     */
    public void testLdapDNBadHost5() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://a.b.c-/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad host 6
     */
    public void testLdapDNBadHost6() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://a.b.-c/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad host 7
     */
    public void testLdapDNBadHost7() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://a.-.c/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL IP host
     */
    public void testLdapDNIPHost() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://1.2.3.4/", new LdapURL( "ldap://1.2.3.4/" ).toString() );
    }


    /**
     * test a LdapURL IP host and port
     */
    public void testLdapDNIPHostPort() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://1.2.3.4:80/", new LdapURL( "ldap://1.2.3.4:80/" ).toString() );
    }


    /**
     * test a LdapURL with a bad IP host 1
     */
    public void testLdapDNBadHostIP1() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://1.1.1/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad IP host 2
     */
    public void testLdapDNBadHostIP2() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://1.1.1./" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad IP host 3
     */
    public void testLdapDNBadHostIP3() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://1.1.1.100000/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a bad IP host 4
     */
    public void testLdapDNBadHostIP4() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://1.1.1.1.1/" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }

        Assert.fail();
    }


    /**
     * test a LdapURL with a valid host hich is not an IP
     */
    public void testLdapDNNotAnIP() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://1.1.1.100000.a/", new LdapURL( "ldap://1.1.1.100000.a/" ).toString() );
    }


    /**
     * test a LdapURL with valid simpleDN
     */
    public void testLdapDNSimpleDN() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://directory.apache.org:389/dc=example,dc=org/", new LdapURL(
            "ldap://directory.apache.org:389/dc=example,dc=org/" ).toString() );
    }


    /**
     * test a LdapURL with valid simpleDN 2
     */
    public void testLdapDNSimpleDN2() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://directory.apache.org:389/dc=example", new LdapURL(
            "ldap://directory.apache.org:389/dc=example" ).toString() );
    }


    /**
     * test a LdapURL with a valid encoded DN
     */
    public void testLdapDNSimpleDNEncoded() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://directory.apache.org:389/dc=example%202,dc=org", new LdapURL(
            "ldap://directory.apache.org:389/dc=example%202,dc=org" ).toString() );
    }


    /**
     * test a LdapURL with an invalid DN
     */
    public void testLdapDNInvalidDN() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://directory.apache.org:389/dc=example%202,dc : org" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }
    }


    /**
     * test a LdapURL with an invalid DN 2
     */
    public void testLdapDNInvalidDN2() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://directory.apache.org:389/dc=example%202,dc = org," );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }
    }


    /**
     * test a LdapURL with valid unique attributes
     */
    public void testLdapDNUniqueAttribute() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://directory.apache.org:389/dc=example,dc=org?ou", new LdapURL(
            "ldap://directory.apache.org:389/dc=example,dc=org?ou" ).toString() );
    }


    /**
     * test a LdapURL with valid attributes
     */
    public void testLdapDNAttributes() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://directory.apache.org:389/dc=example,dc=org?ou,objectclass,dc", new LdapURL(
            "ldap://directory.apache.org:389/dc=example,dc=org?ou,objectclass,dc" ).toString() );
    }


    /**
     * test a LdapURL with valid duplicated attributes
     */
    public void testLdapDNDuplicatedAttributes() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://directory.apache.org:389/dc=example,dc=org?ou,dc", new LdapURL(
            "ldap://directory.apache.org:389/dc=example,dc=org?ou,dc,ou" ).toString() );
    }


    /**
     * test a LdapURL with invalid attributes
     */
    public void testLdapInvalideAttributes() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://directory.apache.org:389/dc=example,dc=org?ou=,dc" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }
    }


    /**
     * test a LdapURL with attributes but no DN
     */
    public void testLdapNoDNAttributes() throws LdapURLEncodingException
    {
        try
        {
            new LdapURL( "ldap://directory.apache.org:389/?ou,dc" );
        }
        catch ( LdapURLEncodingException luee )
        {
            Assert.assertTrue( true );
            return;
        }
    }


    /**
     * test 1 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_1() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap:///o=University%20of%20Michigan,c=US", new LdapURL(
            "ldap:///o=University%20of%20Michigan,c=US" ).toString() );
    }


    /**
     * test 2 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_2() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US", new LdapURL(
            "ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US" ).toString() );
    }


    /**
     * test 3 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_3() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US?postalAddress", new LdapURL(
            "ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US?postalAddress" ).toString() );
    }


    /**
     * test 4 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_4() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://host.com:6666/o=University%20of%20Michigan,c=US??sub?(cn=Babs%20Jensen)",
            new LdapURL( "ldap://host.com:6666/o=University%20of%20Michigan,c=US??sub?(cn=Babs%20Jensen)" ).toString() );
    }


    /**
     * test 5 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_5() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://ldap.itd.umich.edu/c=GB?objectClass?one", new LdapURL(
            "ldap://ldap.itd.umich.edu/c=GB?objectClass?one" ).toString() );
    }


    /**
     * test 6 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_6() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://ldap.question.com/o=Question%3f,c=US?mail", new LdapURL(
            "ldap://ldap.question.com/o=Question%3f,c=US?mail" ).toString() );
    }


    /**
     * test 7 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_7() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap://ldap.netscape.com/o=Babsco,c=US???(int=%5c00%5c00%5c00%5c04)", new LdapURL(
            "ldap://ldap.netscape.com/o=Babsco,c=US???(int=%5c00%5c00%5c00%5c04)" ).toString() );
    }


    /**
     * test 8 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_8() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap:///??sub??bindname=cn=Manager%2co=Foo", new LdapURL(
            "ldap:///??sub??bindname=cn=Manager%2co=Foo" ).toString() );
    }


    /**
     * test 9 from RFC 2255 LdapURL
     */
    public void testLdapRFC2255_9() throws LdapURLEncodingException
    {
        Assert.assertEquals( "ldap:///??sub??!bindname=cn=Manager%2co=Foo", new LdapURL(
            "ldap:///??sub??!bindname=cn=Manager%2co=Foo" ).toString() );
    }
}
