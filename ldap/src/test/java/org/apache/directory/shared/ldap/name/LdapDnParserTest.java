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
package org.apache.directory.shared.ldap.name;


import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.LdapDnParser;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Test the class LdapDN
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapDnParserTest extends TestCase
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
     * test an empty DN
     */
    public void testLdapDNEmpty() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();

        Assert.assertEquals( "", ( ( LdapDN ) dnParser.parse( "" ) ).getName() );
    }


    /**
     * test a simple DN : a = b
     */
    public void testLdapDNSimple() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();

        Assert.assertEquals( "a = b", ( ( LdapDN ) dnParser.parse( "a = b" ) ).getName() );
        Assert.assertEquals( "a=b", ( ( LdapDN ) dnParser.parse( "a = b" ) ).toString() );
    }


    /**
     * test a composite DN : a = b, d = e
     */
    public void testLdapDNComposite() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a = b, c = d" );
        Assert.assertEquals( "a=b,c=d", dn.toString() );
        Assert.assertEquals( "a = b, c = d", dn.getName() );
    }


    /**
     * test a composite DN with or without spaces: a=b, a =b, a= b, a = b, a = b
     */
    public void testLdapDNCompositeWithSpace() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a=b, a =b, a= b, a = b, a  =  b" );
        Assert.assertEquals( "a=b,a=b,a=b,a=b,a=b", dn.toString() );
        Assert.assertEquals( "a=b, a =b, a= b, a = b, a  =  b", dn.getName() );
    }


    /**
     * test a composite DN with differents separators : a=b;c=d,e=f It should
     * return a=b,c=d,e=f (the ';' is replaced by a ',')
     */
    public void testLdapDNCompositeSepators() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a=b;c=d,e=f" );
        Assert.assertEquals( "a=b,c=d,e=f", dn.toString() );
        Assert.assertEquals( "a=b;c=d,e=f", dn.getName() );
    }


    /**
     * test a simple DN with multiple NameComponents : a = b + c = d
     */
    public void testLdapDNSimpleMultivaluedAttribute() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a = b + c = d" );
        Assert.assertEquals( "a=b+c=d", dn.toString() );
        Assert.assertEquals( "a = b + c = d", dn.getName() );
    }


    /**
     * test a composite DN with multiple NC and separators : a=b+c=d, e=f + g=h +
     * i=j
     */
    public void testLdapDNCompositeMultivaluedAttribute() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a=b+c=d, e=f + g=h + i=j" );
        Assert.assertEquals( "a=b+c=d,e=f+g=h+i=j", dn.toString() );
        Assert.assertEquals( "a=b+c=d, e=f + g=h + i=j", dn.getName() );
    }


    /**
     * test a simple DN with an oid prefix (uppercase) : OID.12.34.56 = azerty
     */
    public void testLdapDNOidUpper() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "OID.12.34.56 = azerty" );
        Assert.assertEquals( "oid.12.34.56=azerty", dn.toString() );
        Assert.assertEquals( "OID.12.34.56 = azerty", dn.getName() );
    }


    /**
     * test a simple DN with an oid prefix (lowercase) : oid.12.34.56 = azerty
     */
    public void testLdapDNOidLower() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "oid.12.34.56 = azerty" );
        Assert.assertEquals( "oid.12.34.56=azerty", dn.toString() );
        Assert.assertEquals( "oid.12.34.56 = azerty", dn.getName() );
    }


    /**
     * test a simple DN with an oid attribut without oid prefix : 12.34.56 =
     * azerty
     */
    public void testLdapDNOidWithoutPrefix() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "12.34.56 = azerty" );
        Assert.assertEquals( "12.34.56=azerty", dn.toString() );
        Assert.assertEquals( "12.34.56 = azerty", dn.getName() );
    }


    /**
     * test a composite DN with an oid attribut wiithout oid prefix : 12.34.56 =
     * azerty; 7.8 = test
     */
    public void testLdapDNCompositeOidWithoutPrefix() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "12.34.56 = azerty; 7.8 = test" );
        Assert.assertEquals( "12.34.56=azerty,7.8=test", dn.toString() );
        Assert.assertEquals( "12.34.56 = azerty; 7.8 = test", dn.getName() );
    }


    /**
     * test a simple DN with pair char attribute value : a = \,\=\+\<\>\#\;\\\"\A0\00"
     */
    public void testLdapDNPairCharAttributeValue() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\A0\\00" );
        Assert.assertEquals( "a=\\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\A0\\00", dn.toString() );
        Assert.assertEquals( "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\A0\\00", dn.getName() );
    }


    /**
     * test a simple DN with hexString attribute value : a = #0010A0AAFF
     */
    public void testLdapDNHexStringAttributeValue() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a = #0010A0AAFF" );
        Assert.assertEquals( "a=#0010A0AAFF", dn.toString() );
        Assert.assertEquals( "a = #0010A0AAFF", dn.getName() );
    }


    /**
     * test a simple DN with quoted attribute value : a = "quoted \"value"
     */
    public void testLdapDNQuotedAttributeValue() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "a = quoted \\\"value" );
        Assert.assertEquals( "a=quoted \\\"value", dn.toString() );
        Assert.assertEquals( "a = quoted \\\"value", dn.getName() );
    }


    /**
     * Test the encoding of a LdanDN
     */
    public void testNameToBytes() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN dn = ( LdapDN ) dnParser.parse( "cn = John, ou = People, OU = Marketing" );

        byte[] bytes = LdapDN.getBytes( dn );

        Assert.assertEquals( 30, bytes.length );
        Assert.assertEquals( "cn=John,ou=People,ou=Marketing", StringTools.utf8ToString( bytes ) );
    }


    public void testStringParser() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN name = ( LdapDN ) dnParser.parse( "CN = Emmanuel  Lécharny" );

        Assert.assertEquals( "CN = Emmanuel  Lécharny", name.getName() );
        Assert.assertEquals( "cn=Emmanuel  Lécharny", name.toString() );
    }


    public void testVsldapExtras() throws NamingException
    {
        NameParser dnParser = LdapDnParser.getNameParser();
        LdapDN name = ( LdapDN ) dnParser
            .parse( "cn=Billy Bakers, OID.2.5.4.11=Corporate Tax, ou=Fin-Accounting, ou=Americas, ou=Search, o=IMC, c=US" );

        Assert.assertEquals(
            "cn=Billy Bakers, OID.2.5.4.11=Corporate Tax, ou=Fin-Accounting, ou=Americas, ou=Search, o=IMC, c=US", name
                .getName() );
        Assert.assertEquals(
            "cn=Billy Bakers,oid.2.5.4.11=Corporate Tax,ou=Fin-Accounting,ou=Americas,ou=Search,o=IMC,c=US", name
                .toString() );
    }


    // ~ Methods
    // ----------------------------------------------------------------

    /**
     * Class under test for void DnParser()
     * 
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testDnParser() throws NamingException
    {
        NameParser parser = LdapDnParser.getNameParser();

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
        NameParser parser = LdapDnParser.getNameParser();

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
        NameParser parser = LdapDnParser.getNameParser();

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
        NameParser parser = LdapDnParser.getNameParser();

        Name nameRFC1779_1 = parser
            .parse( "CN=Marshall T. Rose, O=Dover Beach Consulting, L=Santa Clara, ST=California, C=US" );

        assertEquals( "RFC1779_1 : ",
            "CN=Marshall T. Rose, O=Dover Beach Consulting, L=Santa Clara, ST=California, C=US",
            ( ( LdapDN ) nameRFC1779_1 ).toUpName() );
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
        NameParser parser = LdapDnParser.getNameParser();

        Name nameRFC2253_1 = parser.parse( "CN=Steve Kille,O=Isode limited,C=GB" );

        assertEquals( "RFC2253_1 : ", "CN=Steve Kille,O=Isode limited,C=GB", ( ( LdapDN ) nameRFC2253_1 ).toUpName() );
    }


    /**
     * Class under test for Name parse(String)
     * 
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_2() throws NamingException
    {
        NameParser parser = LdapDnParser.getNameParser();

        Name nameRFC2253_2 = parser.parse( "CN = Sales + CN =   J. Smith , O = Widget Inc. , C = US" );

        assertEquals( "RFC2253_2 : ", "CN = Sales + CN =   J. Smith , O = Widget Inc. , C = US",
            ( ( LdapDN ) nameRFC2253_2 ).toUpName() );
        assertEquals( "RFC2253_2 : ", "cn=J. Smith+cn=Sales,o=Widget Inc.,c=US", nameRFC2253_2.toString() );
    }


    /**
     * Class under test for Name parse(String)
     * 
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_3() throws NamingException
    {
        NameParser parser = LdapDnParser.getNameParser();

        Name nameRFC2253_3 = parser.parse( "CN=L. Eagle,   O=Sue\\, Grabbit and Runn, C=GB" );

        assertEquals( "RFC2253_3 : ", "CN=L. Eagle,   O=Sue\\, Grabbit and Runn, C=GB", ( ( LdapDN ) nameRFC2253_3 )
            .toUpName() );
        assertEquals( "RFC2253_3 : ", "cn=L. Eagle,o=Sue\\, Grabbit and Runn,c=GB", nameRFC2253_3.toString() );
    }


    /**
     * Class under test for Name parse(String)
     * 
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_4() throws NamingException
    {
        NameParser parser = LdapDnParser.getNameParser();

        Name nameRFC2253_4 = parser.parse( "CN=Before\\0DAfter,O=Test,C=GB" );
        assertEquals( "RFC2253_4 : ", "CN=Before\\0DAfter,O=Test,C=GB", ( ( LdapDN ) nameRFC2253_4 ).toUpName() );
    }


    /**
     * Class under test for Name parse(String)
     * 
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_5() throws NamingException
    {
        NameParser parser = LdapDnParser.getNameParser();

        Name nameRFC2253_5 = parser.parse( "1.3.6.1.4.1.1466.0=#04024869,O=Test,C=GB" );

        assertEquals( "RFC2253_5 : ", "1.3.6.1.4.1.1466.0=#04024869,O=Test,C=GB", ( ( LdapDN ) nameRFC2253_5 )
            .toUpName() );
    }


    /**
     * Class under test for Name parse(String)
     * 
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseStringRFC2253_6() throws NamingException
    {
        NameParser parser = LdapDnParser.getNameParser();

        Name nameRFC2253_6 = parser.parse( "SN=Lu\\C4\\8Di\\C4\\87" );

        assertEquals( "RFC2253_6 : ", "SN=Lu\\C4\\8Di\\C4\\87", ( ( LdapDN ) nameRFC2253_6 ).toUpName() );
    }


    /**
     * Class under test for Name parse(String)
     * 
     * @throws NamingException
     *             if anything goes wrong
     */
    public final void testParseInvalidString() throws NamingException
    {
        NameParser parser = LdapDnParser.getNameParser();

        try
        {
            parser.parse( "&#347;=&#347;rasulu,dc=example,dc=com" );
            fail( "the invalid name should never succeed in a parse" );
        }
        catch ( Exception e )
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
        NameParser parser = LdapDnParser.getNameParser();
        String input = "ou=some test\\,  something else";
        String result = parser.parse( input ).toString();
        assertEquals( input, result );
    }


    public void testWindowsFilePath() throws Exception
    {
        // '\' should be escaped as stated in RFC 2253
        String path = "windowsFilePath=C:\\\\cygwin";
        NameParser parser = LdapDnParser.getNameParser();
        Name result = parser.parse( path );
        assertEquals( path, ( ( LdapDN ) result ).toUpName() );
        assertEquals( "windowsfilepath=C:\\\\cygwin", result.toString() );
    }


    public void testNameFrenchChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', 0x4A, ( byte ) 0xC3, ( byte ) 0xA9, 0x72, ( byte ) 0xC3, ( byte ) 0xB4, 0x6D, 0x65 } );

        NameParser parser = LdapDnParser.getNameParser();
        String result = parser.parse( cn ).toString();

        assertEquals( cn, result.toString() );

    }


    public void testNameGermanChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC3, ( byte ) 0x84, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0x9C,
                ( byte ) 0xC3, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0xA4, ( byte ) 0xC3, ( byte ) 0xB6,
                ( byte ) 0xC3, ( byte ) 0xBC }, "UTF-8" );

        NameParser parser = LdapDnParser.getNameParser();
        String result = parser.parse( cn ).toString();

        assertEquals( cn, result.toString() );
    }


    public void testNameTurkishChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC4, ( byte ) 0xB0, ( byte ) 0xC4, ( byte ) 0xB1, ( byte ) 0xC5, ( byte ) 0x9E,
                ( byte ) 0xC5, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0xB6,
                ( byte ) 0xC3, ( byte ) 0x9C, ( byte ) 0xC3, ( byte ) 0xBC, ( byte ) 0xC4, ( byte ) 0x9E,
                ( byte ) 0xC4, ( byte ) 0x9F }, "UTF-8" );

        NameParser parser = LdapDnParser.getNameParser();
        String result = parser.parse( cn ).toString();

        assertEquals( cn, result.toString() );

    }
}
