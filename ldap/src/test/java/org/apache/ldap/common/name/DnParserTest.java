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
package org.apache.ldap.common.name;


import javax.naming.Name;
import javax.naming.NamingException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ldap.common.schema.DnNormalizer;


/**
 * Test case for DN Parser class
 *
 * @author Emmanuel Lécharny
 */
public class DnParserTest extends TestCase
{

    //~ Methods ----------------------------------------------------------------

    /**
     * Class under test for void DnParser()
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testDnParser() throws NamingException
    {
        DnParser parser = new DnParser();

        assertNotNull( parser );
    }


    /**
     * Class under test for void DnParser(NameComponentNormalizer)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testDnParserNameComponentNormalizer() throws NamingException
    {
        SimpleNameComponentNormalizer normalizer = new SimpleNameComponentNormalizer( new DnNormalizer() );

        DnParser parser = new DnParser( normalizer );

        assertNotNull( parser );
    }


    /**
     * Class under test for void isNormizing() : Control that the Normalize flag
     * is set correctly
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testIsNormizing() throws NamingException
    {
        DnParser parser = new DnParser();

        SimpleNameComponentNormalizer normalizer = new SimpleNameComponentNormalizer( new DnNormalizer() );

        DnParser parserNormalized = new DnParser( normalizer );

        assertEquals( parser.isNormizing(), false );

        assertEquals( parserNormalized.isNormizing(), true );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringEmpty() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameEmpty = parser.parse( "" );

        assertNotNull( nameEmpty );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringNull() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameNull = parser.parse( null );

        assertEquals( "Null DN are legal : ", "", nameNull.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringRFC1779_1() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameRFC1779_1 = parser.parse( "CN=Marshall T. Rose, O=Dover Beach Consulting, L=Santa Clara, ST=California, C=US" );

        assertEquals( "RFC1779_1 : ", "CN=Marshall T. Rose,O=Dover Beach Consulting,L=Santa Clara,ST=California,C=US", nameRFC1779_1.toString() );

    }

    public void testVsldapExtras() throws Exception
    {
    	DnParser dnParser = new DnParser();
        Name name = dnParser.parse( "cn=Billy Bakers, OID.2.5.4.11=Corporate Tax, ou=Fin-Accounting, ou=Americas, ou=Search, o=IMC, c=US" );
        
        Assert.assertEquals( "cn=Billy Bakers,2.5.4.11=Corporate Tax,ou=Fin-Accounting,ou=Americas,ou=Search,o=IMC,c=US", name.toString() );
    }

    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringRFC2253_1() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameRFC2253_1 = parser.parse( "CN=Steve Kille,O=Isode limited,C=GB" );

        assertEquals( "RFC2253_1 : ",
                "CN=Steve Kille,O=Isode limited,C=GB",
                nameRFC2253_1.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringRFC2253_2() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameRFC2253_2 = parser.parse( "CN = Sales + CN =   J. Smith , O = Widget Inc. , C = US" );

        assertEquals( "RFC2253_2 : ",
                "CN=Sales+CN=J. Smith,O=Widget Inc.,C=US",
                nameRFC2253_2.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringRFC2253_3() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameRFC2253_3 = parser.parse( "CN=L. Eagle,   O=Sue\\, Grabbit and Runn, C=GB" );

        assertEquals( "RFC2253_3 : ", "CN=L. Eagle,O=Sue\\, Grabbit and Runn,C=GB", nameRFC2253_3.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringRFC2253_4() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameRFC2253_4 = parser.parse( "CN=Before\\0DAfter,O=Test,C=GB" );
        assertEquals( "RFC2253_4 : ", "CN=Before\\0DAfter,O=Test,C=GB", nameRFC2253_4.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringRFC2253_5() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameRFC2253_5 = parser.parse( "1.3.6.1.4.1.1466.0=#04024869,O=Test,C=GB" );

        assertEquals( "RFC2253_5 : ",
                "1.3.6.1.4.1.1466.0=#04024869,O=Test,C=GB",
                nameRFC2253_5.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseStringRFC2253_6() throws NamingException
    {
        DnParser parser = new DnParser();

        Name nameRFC2253_6 = parser.parse( "SN=Lu\\C4\\8Di\\C4\\87" );

        assertEquals( "RFC2253_6 : ",
                "SN=Lu\\C4\\8Di\\C4\\87",
                nameRFC2253_6.toString() );
    }


    /**
     * Class under test for Name parse(String)
     *
     * @throws NamingException if anything goes wrong
     */
    public final void testParseInvalidString() throws NamingException
    {
        DnParser parser = new DnParser();

        try
        {
            parser.parse( "&#347;=&#347;rasulu,dc=example,dc=com" );
            fail( "the invalid name should never succeed in a parse" );
        }
        catch ( Exception e )
        {
            assertNotNull(e);
        }
    }


    /**
     * Tests to see if inner whitespace is preserved after an escaped ',' in
     * a value of a name component.  This test was added to try to reproduce
     * the bug encountered in DIREVE-179 <a href="http://issues.apache.org/jira/browse/DIREVE-179">
     * here</a>. 
     *
     * @throws NamingException if anything goes wrong on parse()
     */
    public final void testPreserveSpaceAfterEscape() throws NamingException
    {
        DnParser parser = new DnParser();
        String input = "ou=some test\\,  something else";
        String result = parser.parse( input ).toString();
        assertEquals( input, result );
    }
    
    public void testWindowsFilePath() throws Exception
    {
        // '\' should be escaped as stated in RFC 2253
        String path = "windowsFilePath=C:\\\\cygwin";
        DnParser parser = new DnParser();
        String result = parser.parse( path ).toString();
        assertEquals( path, result );
    }    
    
    public void testNameFrenchChars() throws Exception
    {
        String cn = new String(
                new byte[] { 'c', 'n', '=', 0x4A, (byte) 0xC3, (byte) 0xA9, 0x72, (byte) 0xC3, (byte) 0xB4, 0x6D, 0x65 });

        DnParser parser = new DnParser();
        String result = parser.parse( cn ).toString();
        
        assertEquals( cn, result.toString() );

    }

    public void testNameGermanChars() throws Exception
    {
        String cn = new String(new byte[]{'c', 'n', '=', (byte)0xC3, (byte)0x84, (byte)0xC3, (byte)0x96, (byte)0xC3, 
                    (byte)0x9C, (byte)0xC3, (byte)0x9F, (byte)0xC3, (byte)0xA4, (byte)0xC3, (byte)0xB6, (byte)0xC3, (byte)0xBC}, "UTF-8");
        

        DnParser parser = new DnParser();
        String result = parser.parse( cn ).toString();
        
        assertEquals( cn, result.toString() );
    }

    public void testNameTurkishChars() throws Exception
    {
        String cn = new String(new byte[]{'c', 'n', '=', (byte)0xC4, (byte)0xB0, (byte)0xC4, (byte)0xB1, 
                (byte)0xC5, (byte)0x9E, (byte)0xC5, (byte)0x9F, 
                (byte)0xC3, (byte)0x96, (byte)0xC3, (byte)0xB6, 
                (byte)0xC3, (byte)0x9C, (byte)0xC3, (byte)0xBC, 
                (byte)0xC4, (byte)0x9E, (byte)0xC4, (byte)0x9F }, "UTF-8");

        DnParser parser = new DnParser();
        String result = parser.parse( cn ).toString();
        
        assertEquals( cn, result.toString() );

    }

}  // end class DnParserTest
