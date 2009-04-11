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
package org.apache.directory.shared.ldap.name;


import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Tests the fast DN parser.
 * 
 * The test cases are copied from LdapDnParserTest and adjusted when an
 * TooComplexException is expected.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 696620 $, $Date: 2008-09-18 12:09:30 +0200 (Do, 18 Sep 2008) $, 
 */
public class FastLdapDnParserTest extends TestCase
{

    /**
     * test an empty DN
     */
    public void testLdapDNEmpty() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        Assert.assertEquals( "", ( ( LdapDN ) dnParser.parse( "" ) ).getUpName() );
    }


    /**
     * Tests incomplete DNs, used to check that the parser does not 
     * run into infinite loops.
     */
    public void testLdapDNIncomplete() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();

        // empty DN is ok
        dnParser.parse( " " );

        // test DNs starting with an descr
        try
        {
            dnParser.parse( " a" );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            // expected
        }
        try
        {
            dnParser.parse( " a " );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            // expected
        }
        try
        {
            dnParser.parse( " a- " );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            // expected
        }
        dnParser.parse( " a =" );
        dnParser.parse( " a = " );
        dnParser.parse( " a = b" );

        // test DNs starting with an OID
        try
        {
            dnParser.parse( " 1 = b " );
            fail( "OID must contain at least on dot." );
        }
        catch ( InvalidNameException ine )
        {
            // expected
        }
        try
        {
            dnParser.parse( " 0" );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            // expected
        }
        try
        {
            dnParser.parse( " 0." );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            // expected
        }
        try
        {
            dnParser.parse( " 0.5" );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            // expected
        }
        try
        {
            dnParser.parse( " 0.5 " );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            // expected
        }
        dnParser.parse( " 0.5=" );
        dnParser.parse( " 0.5 = " );
        dnParser.parse( " 0.5 = b" );
    }


    /**
     * test a simple DN : a = b
     */
    public void testLdapDNSimple() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        Assert.assertEquals( "a = b", ( ( LdapDN ) dnParser.parse( "a = b" ) ).getUpName() );
        Assert.assertEquals( "a=b", ( ( LdapDN ) dnParser.parse( "a = b" ) ).toString() );
    }


    /**
     * test a composite DN : a = b, d = e
     */
    public void testLdapDNComposite() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a = b, c = d" );
        Assert.assertEquals( "a=b,c=d", dn.toString() );
        Assert.assertEquals( "a = b, c = d", dn.getUpName() );
    }


    /**
     * test a composite DN with or without spaces: a=b, a =b, a= b, a = b, a = b
     */
    public void testLdapDNCompositeWithSpace() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a=b, a =b, a= b, a = b, a  =  b" );
        Assert.assertEquals( "a=b,a=b,a=b,a=b,a=b", dn.toString() );
        Assert.assertEquals( "a=b, a =b, a= b, a = b, a  =  b", dn.getUpName() );
    }


    /**
     * test a composite DN with differents separators : a=b;c=d,e=f It should
     * return a=b,c=d,e=f (the ';' is replaced by a ',')
     */
    public void testLdapDNCompositeSepators() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a=b;c=d,e=f" );
        Assert.assertEquals( "a=b,c=d,e=f", dn.toString() );
        Assert.assertEquals( "a=b;c=d,e=f", dn.getUpName() );
    }


    /**
     * test a simple DN with multiple NameComponents : a = b + c = d
     */
    public void testLdapDNSimpleMultivaluedAttribute() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        try
        {
            dnParser.parse( "a = b + c = d" );
            fail( "Multivalued RDN not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * test a composite DN with multiple NC and separators : a=b+c=d, e=f + g=h +
     * i=j
     */
    public void testLdapDNCompositeMultivaluedAttribute() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        try
        {
            dnParser.parse( "a=b+c=d, e=f + g=h + i=j" );
            fail( "Multivalued RDN not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * test a simple DN with an oid prefix (uppercase) : OID.12.34.56 = azerty
     */
    public void testLdapDNOidUpper() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        try
        {
            dnParser.parse( "OID.12.34.56 = azerty" );
            fail( "OID prefix not supported by fast parser" );
        }
        catch ( Exception e )
        {
            // expected
        }
    }


    /**
     * test a simple DN with an oid prefix (lowercase) : oid.12.34.56 = azerty
     */
    public void testLdapDNOidLower() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        try
        {
            dnParser.parse( "oid.12.34.56 = azerty" );
            fail( "OID prefix not supported by fast parser" );
        }
        catch ( Exception e )
        {
            // expected
        }
    }


    /**
     * test a simple DN with an oid attribut without oid prefix : 12.34.56 =
     * azerty
     */
    public void testLdapDNOidWithoutPrefix() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "12.34.56 = azerty" );
        Assert.assertEquals( "12.34.56=azerty", dn.toString() );
        Assert.assertEquals( "12.34.56 = azerty", dn.getUpName() );
    }


    /**
     * test a composite DN with an oid attribut wiithout oid prefix : 12.34.56 =
     * azerty; 7.8 = test
     */
    public void testLdapDNCompositeOidWithoutPrefix() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "12.34.56 = azerty; 7.8 = test" );
        Assert.assertEquals( "12.34.56=azerty,7.8=test", dn.toString() );
        Assert.assertEquals( "12.34.56 = azerty; 7.8 = test", dn.getUpName() );
    }


    /**
     * test a simple DN with pair char attribute value : a = \,\=\+\<\>\#\;\\\"\C3\A9"
     */
    public void testLdapDNPairCharAttributeValue() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        try
        {
            dnParser.parse( "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\C3\\A9" );
            fail( "Complex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * test a simple DN with hexString attribute value : a = #0010A0AAFF
     */
    public void testLdapDNHexStringAttributeValue() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        try
        {
            dnParser.parse( "a = #0010A0AAFF" );
            fail( "Hex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * test exception from illegal hexString attribute value : a=#zz.
     */
    public void testBadLdapDNHexStringAttributeValue() throws NamingException
    {
        try
        {
            NameParser dnParser = FastLdapDnParser.getNameParser();
            dnParser.parse( "a=#zz" );
            fail( "Hex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * test a simple DN with quoted attribute value : a = "quoted \"value"
     */
    public void testLdapDNQuotedAttributeValue() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        try
        {
            dnParser.parse( "a = quoted \\\"value" );
            fail( "Quotes not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * Test the encoding of a LdanDN
     */
    public void testNameToBytes() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "cn = John, ou = People, OU = Marketing" );

        byte[] bytes = LdapDN.getBytes( dn );

        Assert.assertEquals( 30, bytes.length );
        Assert.assertEquals( "cn=John,ou=People,ou=Marketing", StringTools.utf8ToString( bytes ) );
    }


    public void testStringParser() throws NamingException
    {
        String dn = StringTools.utf8ToString( new byte[]
            { 'C', 'N', ' ', '=', ' ', 'E', 'm', 'm', 'a', 'n', 'u', 'e', 'l', ' ', ' ', 'L', ( byte ) 0xc3,
                ( byte ) 0xa9, 'c', 'h', 'a', 'r', 'n', 'y' } );

        NameParser dnParser = FastLdapDnParser.getNameParser();
        LdapDN name = ( LdapDN ) dnParser.parse( dn );

        Assert.assertEquals( dn, name.getUpName() );
        Assert.assertEquals( "cn=Emmanuel  L\\C3\\A9charny", name.toString() );
    }


    public void testStringParserShort() throws NamingException
    {
        String dn = StringTools.utf8ToString( new byte[]
            { 'C', '=', ' ', 'E', ( byte ) 0xc3, ( byte ) 0xa9, 'c' } );

        NameParser dnParser = FastLdapDnParser.getNameParser();
        LdapDN name = ( LdapDN ) dnParser.parse( dn );

        Assert.assertEquals( dn, name.getUpName() );
        Assert.assertEquals( "c=E\\C3\\A9c", name.toString() );
    }


    public void testVsldapExtras() throws NamingException
    {
        NameParser dnParser = FastLdapDnParser.getNameParser();
        try
        {
            dnParser
                .parse( "cn=Billy Bakers, OID.2.5.4.11=Corporate Tax, ou=Fin-Accounting, ou=Americas, ou=Search, o=IMC, c=US" );
            fail( "OID prefix not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * Class under test for void DnParser()
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testDnParser()
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        assertNotNull( parser );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringEmpty() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        Name nameEmpty = parser.parse( "" );

        assertNotNull( nameEmpty );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringNull() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        Name nameNull = parser.parse( null );

        assertEquals( "Null DN are legal : ", "", nameNull.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC1779_1() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        Name nameRFC1779_1 = parser
            .parse( "CN=Marshall T. Rose, O=Dover Beach Consulting, L=Santa Clara, ST=California, C=US" );

        assertEquals( "RFC1779_1 : ",
            "CN=Marshall T. Rose, O=Dover Beach Consulting, L=Santa Clara, ST=California, C=US",
            ( ( LdapDN ) nameRFC1779_1 ).getUpName() );
        assertEquals( "RFC1779_1 : ", "cn=Marshall T. Rose,o=Dover Beach Consulting,l=Santa Clara,st=California,c=US",
            nameRFC1779_1.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_1() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        Name nameRFC2253_1 = parser.parse( "CN=Steve Kille,O=Isode limited,C=GB" );

        assertEquals( "RFC2253_1 : ", "CN=Steve Kille,O=Isode limited,C=GB", ( ( LdapDN ) nameRFC2253_1 ).getUpName() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_2() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        try
        {
            parser.parse( "CN = Sales + CN =   J. Smith , O = Widget Inc. , C = US" );
            fail( "Multivalued RDN not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_3() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        try
        {
            parser.parse( "CN=L. Eagle,   O=Sue\\, Grabbit and Runn, C=GB" );
            fail( "Complex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_4() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        try
        {
            parser.parse( "CN=Before\\0DAfter,O=Test,C=GB" );
            fail( "Complex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_5() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        try
        {
            parser.parse( "1.3.6.1.4.1.1466.0=#04024869,O=Test,C=GB" );
            fail( "Hex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_6() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        try
        {
            parser.parse( "SN=Lu\\C4\\8Di\\C4\\87" );
            fail( "Complex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseInvalidString()
    {
        NameParser parser = FastLdapDnParser.getNameParser();

        try
        {
            parser.parse( "&#347;=&#347;rasulu,dc=example,dc=com" );
            fail( "the invalid name should never succeed in a parse" );
        }
        catch ( NamingException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Tests to see if inner whitespace is preserved after an escaped ',' in a
     * value of a name component. This test was added to try to reproduce the
     * bug encountered in DIREVE-179 <a
     * href="http://issues.apache.org/jira/browse/DIREVE-179"> here</a>.
     *
     * @throws NamingException
     *             if anything goes wrong on parse()
     */
    public final void testPreserveSpaceAfterEscape() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();
        String input = "ou=some test\\,  something else";
        try
        {
            parser.parse( input ).toString();
            fail( "Complex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    public void testWindowsFilePath() throws Exception
    {
        // '\' should be escaped as stated in RFC 2253
        String path = "windowsFilePath=C:\\\\cygwin";
        NameParser parser = FastLdapDnParser.getNameParser();
        try
        {
            parser.parse( path );
            fail( "Complex DNs not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    public void testNameFrenchChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', 0x4A, ( byte ) 0xC3, ( byte ) 0xA9, 0x72, ( byte ) 0xC3, ( byte ) 0xB4, 0x6D, 0x65 },
            "UTF-8" );

        NameParser parser = FastLdapDnParser.getNameParser();
        String result = parser.parse( cn ).toString();

        assertEquals( "cn=J\\C3\\A9r\\C3\\B4me", result );

    }


    public void testNameGermanChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC3, ( byte ) 0x84, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0x9C,
                ( byte ) 0xC3, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0xA4, ( byte ) 0xC3, ( byte ) 0xB6,
                ( byte ) 0xC3, ( byte ) 0xBC }, "UTF-8" );

        NameParser parser = FastLdapDnParser.getNameParser();
        String result = parser.parse( cn ).toString();

        assertEquals( "cn=\\C3\\84\\C3\\96\\C3\\9C\\C3\\9F\\C3\\A4\\C3\\B6\\C3\\BC", result );
    }


    public void testNameTurkishChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC4, ( byte ) 0xB0, ( byte ) 0xC4, ( byte ) 0xB1, ( byte ) 0xC5, ( byte ) 0x9E,
                ( byte ) 0xC5, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0xB6,
                ( byte ) 0xC3, ( byte ) 0x9C, ( byte ) 0xC3, ( byte ) 0xBC, ( byte ) 0xC4, ( byte ) 0x9E,
                ( byte ) 0xC4, ( byte ) 0x9F }, "UTF-8" );

        NameParser parser = FastLdapDnParser.getNameParser();
        String result = parser.parse( cn ).toString();

        assertEquals( "cn=\\C4\\B0\\C4\\B1\\C5\\9E\\C5\\9F\\C3\\96\\C3\\B6\\C3\\9C\\C3\\BC\\C4\\9E\\C4\\9F", result );

    }


    public void testAUmlautPlusBytes() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC3, ( byte ) 0x84, 0x5C, 0x32, 0x42 }, "UTF-8" );
        NameParser parser = FastLdapDnParser.getNameParser();
        try
        {
            parser.parse( cn ).toString();
            fail( "DNs with special characters not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    public void testAUmlautPlusChar() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC3, ( byte ) 0x84, '\\', '+' }, "UTF-8" );

        NameParser parser = FastLdapDnParser.getNameParser();
        try
        {
            parser.parse( cn ).toString();
            fail( "DNs with special characters not supported by fast parser" );
        }
        catch ( TooComplexException tce )
        {
            // expected
        }
    }


    /**
     * Test to check that even with a non escaped char, the DN is parsed ok
     * or at least an error is generated.
     *
     * @throws NamingException
     *             if anything goes wrong on parse()
     */
    public final void testNonEscapedChars() throws NamingException
    {
        NameParser parser = FastLdapDnParser.getNameParser();
        String input = "ou=ou+test";

        try
        {
            parser.parse( input ).toString();
            fail( "Should never reach this point" );
        }
        catch ( TooComplexException tce )
        {
            assertTrue( true );
            return;
        }
    }

}
