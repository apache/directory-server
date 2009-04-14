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
package org.apache.directory.shared.ldap.filter;


import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;


/**
 * Tests the FilterParserImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 575783 $
 */
public class FilterParserTest
{
    private boolean checkWrongFilter( String filter )
    {
        try
        {
            return FilterParser.parse( filter ) == null;
        }
        catch ( ParseException pe )
        {
            return true;
        }
    }

    /**
     * Tests to avoid deadlocks for invalid filters. 
     * 
     */
    @Test
    public void testInvalidFilters() 
    {
        assertTrue( checkWrongFilter( "" ) );
        assertTrue( checkWrongFilter( "   " ) );
        assertTrue( checkWrongFilter( "(" ) );
        assertTrue( checkWrongFilter( "  (" ) );
        assertTrue( checkWrongFilter( "(  " ) );
        assertTrue( checkWrongFilter( ")" ) );
        assertTrue( checkWrongFilter( "  )" ) );
        assertTrue( checkWrongFilter( "()" ) );
        assertTrue( checkWrongFilter( "(  )" ) );
        assertTrue( checkWrongFilter( "  ()  " ) );
        assertTrue( checkWrongFilter( "(cn=test(" ) );
        assertTrue( checkWrongFilter( "(cn=aaaaa" ) );
        assertTrue( checkWrongFilter( "(&(cn=abc)" ) );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testItemFilter() throws ParseException
    {
        String str = "(ou~=people)";

        SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people", node.getValue().get() );
        assertTrue( node instanceof ApproximateNode );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }

    
    @Test
    public void testAndFilter() throws ParseException
    {
        String str = "(&(ou~=people)(age>=30))";
        BranchNode node = ( BranchNode ) FilterParser.parse( str );
        assertEquals( 2, node.getChildren().size() );
        assertTrue( node instanceof AndNode );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }

    
    @Test
    public void testAndFilterOneChildOnly() throws ParseException
    {
        String str = "(&(ou~=people))";
        BranchNode node = ( BranchNode ) FilterParser.parse( str );
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof AndNode );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testOrFilter() throws ParseException
    {
        String str = "(|(ou~=people)(age>=30))";
        BranchNode node = ( BranchNode ) FilterParser.parse( str );
        assertEquals( 2, node.getChildren().size() );
        assertTrue( node instanceof OrNode );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testOrFilterOneChildOnly() throws ParseException
    {
        String str = "(|(age>=30))";
        BranchNode node = ( BranchNode ) FilterParser.parse( str );
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof OrNode );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testNotFilter() throws ParseException
    {
        String str = "(!(&(ou~= people)(age>=30)))";
        BranchNode node = ( BranchNode ) FilterParser.parse( str );
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof NotNode );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testOptionAndEscapesFilter() throws ParseException
    {
        String str = "(ou;lang-de>=\\23\\42asdl fkajsd)";     // \23 = '#'
        SimpleNode node = ( SimpleNode ) FilterParser.parse( str );
        assertEquals( "ou;lang-de", node.getAttribute() );
        assertEquals( "#Basdl fkajsd", node.getValue().get() );
        String  str2 = node.toString();
        assertEquals( "(ou;lang-de>=#Basdl fkajsd)", str2 );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testOptionsAndEscapesFilter() throws ParseException
    {
        String str = "(ou;lang-de;version-124>=\\23\\42asdl fkajsd)";
        SimpleNode node = ( SimpleNode ) FilterParser.parse( str );
        assertEquals( "ou;lang-de;version-124", node.getAttribute() );
        assertEquals( "#Basdl fkajsd", node.getValue().get() );
        String  str2 = node.toString();
        assertEquals( "(ou;lang-de;version-124>=#Basdl fkajsd)", str2 );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testNumericoidOptionsAndEscapesFilter() throws ParseException
    {
        String str = "(1.3.4.2;lang-de;version-124>=\\23\\42afdl fkajsd)";
        SimpleNode node = ( SimpleNode ) FilterParser.parse( str );
        assertEquals( "1.3.4.2;lang-de;version-124", node.getAttribute() );
        assertEquals( "#Bafdl fkajsd", node.getValue().get() );
        String  str2 = node.toString();
        assertEquals( "(1.3.4.2;lang-de;version-124>=#Bafdl fkajsd)", str2 );
    }


    @Test
    public void testPresentFilter() throws ParseException
    {
        String str = "(ou=*)";
        PresenceNode node = ( PresenceNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof PresenceNode );
        String  str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testNumericoidPresentFilter() throws ParseException
    {
        String str = "(1.2.3.4=*)";
        PresenceNode node = ( PresenceNode ) FilterParser.parse( str );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertTrue( node instanceof PresenceNode );
        String  str2 = node.toString();
        assertEquals( str, str2 );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testEqualsFilter() throws ParseException
    {
        String str = "(ou=people)";
        SimpleNode node = ( SimpleNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people", node.getValue().get() );
        assertTrue( node instanceof EqualityNode );
        String  str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testBadEqualsFilter()
    {
        try
        {
            FilterParser.parse( "ou=people" );

            // The parsing should fail
            fail( "should fail with bad filter" );
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testEqualsWithForwardSlashFilter() throws ParseException
    {
        String str = "(ou=people/in/my/company)";
        SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people/in/my/company", node.getValue().get() );
        assertTrue( node instanceof EqualityNode );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testExtensibleFilterForm1() throws ParseException
    {
        String str = "(ou:dn:stupidMatch:=dummyAssertion\\23\\ac)";
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertTrue( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm1WithNumericOid() throws ParseException
    {
        String str = "(1.2.3.4:dn:1.3434.23.2:=dummyAssertion\\23\\ac)";
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( str );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertTrue( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm1NoDnAttr() throws ParseException
    {
        String str = "(ou:stupidMatch:=dummyAssertion\\23\\ac)";
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertFalse( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm1OptionOnRule()
    {
        try
        {
            FilterParser.parse( "(ou:stupidMatch;lang-de:=dummyAssertion\\23\\ac)" );
            fail( "we should never get here" );
        }
        catch ( ParseException e )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testExtensibleFilterForm1NoAttrNoMatchingRule() throws ParseException
    {
        String str = "(ou:=dummyAssertion\\23\\ac)";
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( null, node.getMatchingRuleId() );
        assertFalse( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm2() throws ParseException
    {
        String str = "(:dn:stupidMatch:=dummyAssertion\\23\\ac)";
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( str );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertTrue( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm2OptionOnRule()
    {
        try
        {
            FilterParser.parse( "(:dn:stupidMatch;lang-en:=dummyAssertion\\23\\ac)" );
            fail( "we should never get here" );
        }
        catch ( ParseException e )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testExtensibleFilterForm2WithNumericOid() throws ParseException
    {
        String str = "(:dn:1.3434.23.2:=dummyAssertion\\23\\ac)";
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( str );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertTrue( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm2NoDnAttr() throws ParseException
    {
        String str = "(:stupidMatch:=dummyAssertion\\23\\ac)";
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( str );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertFalse( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm2NoDnAttrWithNumericOidNoAttr() throws ParseException
    {
        String str = "(:1.3434.23.2:=dummyAssertion\\23\\ac)";
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( str );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertFalse( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm3() throws ParseException
    {
        try
        {
            FilterParser.parse( "(:=dummyAssertion)" );
            fail( "Should never reach this point" );
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testReuseParser() throws ParseException
    {
        FilterParser.parse( "(ou~=people)" );
        FilterParser.parse( "(&(ou~=people)(age>=30)) " );
        FilterParser.parse( "(|(ou~=people)(age>=30)) " );
        FilterParser.parse( "(!(&(ou~=people)(age>=30)))" );
        FilterParser.parse( "(ou;lang-de>=\\23\\42asdl fkajsd)" );
        FilterParser.parse( "(ou;lang-de;version-124>=\\23\\42asdl fkajsd)" );
        FilterParser.parse( "(1.3.4.2;lang-de;version-124>=\\23\\42asdl fkajsd)" );
        FilterParser.parse( "(ou=*)" );
        FilterParser.parse( "(1.2.3.4=*)" );
        FilterParser.parse( "(ou=people)" );
        FilterParser.parse( "(ou=people/in/my/company)" );
        FilterParser.parse( "(ou:dn:stupidMatch:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(1.2.3.4:dn:1.3434.23.2:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(ou:stupidMatch:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(ou:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(1.2.3.4:1.3434.23.2:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(:dn:stupidMatch:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(:dn:1.3434.23.2:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(:stupidMatch:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(:1.3434.23.2:=dummyAssertion\\23\\ac)" );
    }


    @Test
    public void testReuseParserAfterFailures() throws ParseException
    {
        FilterParser.parse( "(ou~=people)" );
        FilterParser.parse( "(&(ou~=people)(age>=30)) " );
        FilterParser.parse( "(|(ou~=people)(age>=30)) " );
        FilterParser.parse( "(!(&(ou~=people)(age>=30)))" );
        FilterParser.parse( "(ou;lang-de>=\\23\\42asdl fkajsd)" );
        FilterParser.parse( "(ou;lang-de;version-124>=\\23\\42asdl fkajsd)" );
        FilterParser.parse( "(1.3.4.2;lang-de;version-124>=\\23\\42asdl fkajsd)" );
        
        try
        {
            FilterParser.parse( "(ou:stupidMatch;lang-de:=dummyAssertion\\23\\ac)" );
            fail( "we should never get here" );
        }
        catch ( ParseException e )
        {
            assertTrue( true );
        }

        FilterParser.parse( "(ou=*)" );
        FilterParser.parse( "(1.2.3.4=*)" );
        FilterParser.parse( "(ou=people)" );
        FilterParser.parse( "(ou=people/in/my/company)" );
        FilterParser.parse( "(ou:dn:stupidMatch:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(1.2.3.4:dn:1.3434.23.2:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(ou:stupidMatch:=dummyAssertion\\23\\ac)" );

        try
        {
            FilterParser.parse( "(:dn:stupidMatch;lang-en:=dummyAssertion\\23\\ac)" );
            fail( "we should never get here" );
        }
        catch ( ParseException e )
        {
            assertTrue( true );
        }

        FilterParser.parse( "(ou:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(1.2.3.4:1.3434.23.2:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(:dn:stupidMatch:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(:dn:1.3434.23.2:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(:stupidMatch:=dummyAssertion\\23\\ac)" );
        FilterParser.parse( "(:1.3434.23.2:=dummyAssertion\\23\\ac)" );
    }


    @Test
    public void testNullOrEmptyString() throws ParseException
    {
        try
        {
            FilterParser.parse( null );
            fail( "Should not reach this point " );
        }
        catch( ParseException pe )
        {
            assertTrue( true );
        }

        try
        {
            FilterParser.parse( "" );
            fail( "Should not reach this point " );
        }
        catch( ParseException pe )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testSubstringNoAnyNoFinal() throws ParseException
    {
        String str = "(ou=foo*)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( null, node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringNoAny() throws ParseException
    {
        String str = "(ou=foo*bar)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringNoAnyNoIni() throws ParseException
    {
        String str = "(ou=*bar)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( null, node.getInitial() );
        assertEquals( "bar", node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringOneAny() throws ParseException
    {
        String str = "(ou=foo*guy*bar)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 1, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "guy" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringManyAny() throws ParseException
    {
        String str = "(ou=a*b*c*d*e*f)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 4, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "b" ) );
        assertTrue( node.getAny().contains( "c" ) );
        assertTrue( node.getAny().contains( "d" ) );
        assertTrue( node.getAny().contains( "e" ) );
        assertEquals( "a", node.getInitial() );
        assertEquals( "f", node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringNoIniManyAny() throws ParseException
    {
        String str = "(ou=*b*c*d*e*f)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 4, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "e" ) );
        assertTrue( node.getAny().contains( "b" ) );
        assertTrue( node.getAny().contains( "c" ) );
        assertTrue( node.getAny().contains( "d" ) );
        assertEquals( null, node.getInitial() );
        assertEquals( "f", node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringManyAnyNoFinal() throws ParseException
    {
        String str = "(ou=a*b*c*d*e*)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 4, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "e" ) );
        assertTrue( node.getAny().contains( "b" ) );
        assertTrue( node.getAny().contains( "c" ) );
        assertTrue( node.getAny().contains( "d" ) );
        assertEquals( "a", node.getInitial() );
        assertEquals( null, node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringNoIniManyAnyNoFinal() throws ParseException
    {
        String str = "(ou=*b*c*d*e*)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 4, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "e" ) );
        assertTrue( node.getAny().contains( "b" ) );
        assertTrue( node.getAny().contains( "c" ) );
        assertTrue( node.getAny().contains( "d" ) );
        assertEquals( null, node.getInitial() );
        assertEquals( null, node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringNoAnyDoubleSpaceStar() throws ParseException
    {
        String str = "(ou=foo* *bar)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 1, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( " " ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @Test
    public void testSubstringAnyDoubleSpaceStar() throws ParseException
    {
        String str = "(ou=foo* a *bar)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 1, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( " a " ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    /**
     * Enrique just found this bug with the filter parser when parsing substring
     * expressions like *any*. Here's the JIRA issue: <a
     * href="http://nagoya.apache.org/jira/browse/DIRLDAP-21">DIRLDAP-21</a>.
     */
    @Test
    public void testSubstringStarAnyStar() throws ParseException
    {
        String str = "(ou=*foo*)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 1, node.getAny().size() );
        assertTrue( node.getAny().contains( "foo" ) );
        assertNull( node.getInitial() );
        assertNull( node.getFinal() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testTwoByteUTF8Raw() throws ParseException
    {
        byte[] bytes =
            { ( byte ) 0xC2, ( byte ) 0xA2 }; // unicode U+00A2: cents sign

        try
        {
            String s = new String( bytes, "UTF-8" );
            String str = "(cn=" + s + ")";
            SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );

            assertEquals( "cn", node.getAttribute() );
            String val = node.getValue().get();
            assertEquals( "a2", Integer.toHexString( ( int ) val.charAt( 0 ) ) ); // char is U+00A2
            String str2 = node.toString();
            assertEquals( str, str2 );
        }
        catch ( UnsupportedEncodingException e )
        {
            fail();
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testTwoByteUTF8Escaped() throws ParseException
    {
        byte[] bytes =
            { ( byte ) 0xC2, ( byte ) 0xA2 }; // unicode U+00A2: cents sign

        try
        {
            String str = "(cn=\\c2\\a2)";
            String s = new String( bytes, "UTF-8" );
            String strOut = "(cn=" + s + ")";
            SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );

            assertEquals( "cn", node.getAttribute() );
            String val = ( String ) node.getValue().get();
            assertEquals( "a2", Integer.toHexString( ( int ) val.charAt( 0 ) ) ); // char is U+00A2
            String str2 = node.toString();
            assertEquals( strOut, str2 );
        }
        catch ( UnsupportedEncodingException e )
        {
            fail();
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testThreeByteUTF8Raw() throws ParseException
    {
        byte[] bytes =
            { ( byte ) 0xE2, ( byte ) 0x89, ( byte ) 0xA0 }; // unicode U+2260: "not equal to" sign in decimal signed bytes is -30, -119, -96

        try
        {
            String s = new String( bytes, "UTF-8" );
            String str = "(cn=" + s + ")";
            SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );

            assertEquals( "cn", node.getAttribute() );
            String val = node.getValue().get();
            assertEquals( "2260", Integer.toHexString( ( int ) val.charAt( 0 ) ) );
            String str2 = node.toString();
            assertEquals( str, str2 );
        }
        catch ( UnsupportedEncodingException e )
        {
            fail();
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testThreeByteUTF8Escaped() throws ParseException
    {
        byte[] bytes =
            { ( byte ) 0xE2, ( byte ) 0x89, ( byte ) 0xA0 }; // unicode U+2260: "not equal to" sign in decimal signed bytes is -30, -119, -96

        try
        {
            String str = "(cn=\\e2\\89\\a0aa)";
            String s = new String( bytes, "UTF-8" );
            String strOut = "(cn=" + s + "aa)";
            SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );

            assertEquals( "cn", node.getAttribute() );
            String val = ( String ) node.getValue().get();
            assertEquals( "2260", Integer.toHexString( ( int ) val.charAt( 0 ) ) );
            String str2 = node.toString();
            assertEquals( strOut, str2 );
        }
        catch ( UnsupportedEncodingException e )
        {
            fail();
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testThreeByteJapaneseUTF8Raw() throws ParseException
    {
        byte[] bytes =
            { ( byte ) 0xE3, ( byte ) 0x81, ( byte ) 0x99 }; // unicode U+3059: Japanese 'T' with squiggle on down-stroke.

        try
        {
            String s = new String( bytes, "UTF-8" );
            String str = "(cn=" + s + ")";
            SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );

            assertEquals( "cn", node.getAttribute() );
            String val = node.getValue().get();
            assertEquals( "3059", Integer.toHexString( ( int ) val.charAt( 0 ) ) );
            String str2 = node.toString();
            assertEquals( str, str2 );
        }
        catch ( UnsupportedEncodingException e )
        {
            fail();
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testThreeByteJapaneseUTF8Escaped() throws ParseException
    {
        byte[] bytes =
            { ( byte ) 0xE3, ( byte ) 0x81, ( byte ) 0x99 }; // unicode U+3059: Japanese 'T' with squiggle on down-stroke.

        try
        {
            String str = "(cn=\\e3\\81\\99)";
            String s = new String( bytes, "UTF-8" );
            String strOut = "(cn=" + s + ")";

            SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );
            assertEquals( "cn", node.getAttribute() );
            String val = node.getValue().get();
            assertEquals( "3059", Integer.toHexString( ( int ) val.charAt( 0 ) ) );
            String str2 = node.toString();
            assertEquals( strOut, str2 );
        }
        catch ( UnsupportedEncodingException e )
        {
            fail();
        }
    }


    /**
     * test a filter with a # in value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEqualsFilterWithPoundInValue() throws ParseException
    {
        String str = "(uid=#f1)";
        SimpleNode<String> node = ( SimpleNode<String> ) FilterParser.parse( str );
        assertEquals( "uid", node.getAttribute() );
        assertEquals( "#f1", node.getValue().get() );
        assertTrue( node instanceof EqualityNode );
        assertEquals( str, node.toString() );
    }


    /**
     * Test that special and non allowed chars into an assertionValue are not
     * accepted. ((cf DIRSERVER-1196)
     *
     */
    @Test
    public void testSpecialCharsInMemberOf()
    {
        try
        {
            FilterParser.parse( "(memberOf=1.2.840.113556.1.4.1301=$#@&*()==,2.5.4.11=local,2.5.4.11=users,2.5.4.11=readimanager)" );
            fail();
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test that filters like (&(a=b)(|(c=d)(e=f))) are correctly parsed
     */
    @Test
    public void testAndEqOr_EqEq()
    {
        try
        {
            BranchNode node = ( BranchNode ) FilterParser.parse("(&(objectClass=nisNetgroup)(|(nisNetGroupTriple=a*a)(nisNetGroupTriple=\\28*,acc1,*\\29)))");
            assertEquals( 2, node.getChildren().size() );
            
            assertTrue( node instanceof AndNode );
            
            // Check the (a=b) part
            ExprNode aEqb = node.getFirstChild();
            assertTrue( aEqb instanceof EqualityNode );
            assertEquals( "objectClass", ((EqualityNode<?>)aEqb).getAttribute() );
            assertEquals( "nisNetgroup", ((EqualityNode<?>)aEqb).getValue().get() );
            
            // Check the or node
            ExprNode orNode = node.getChildren().get( 1 );
            assertTrue( orNode instanceof OrNode );
            
            assertEquals( 2, ((OrNode)orNode).getChildren().size() );
            
            ExprNode leftNode = ((OrNode)orNode).getChildren().get( 0 );
            
            assertTrue( leftNode instanceof SubstringNode );
            assertEquals( "nisNetGroupTriple", ((SubstringNode)leftNode).getAttribute() );
            assertEquals( "a", ((SubstringNode)leftNode).getInitial() );
            assertEquals( 0, ((SubstringNode)leftNode).getAny().size() );
            assertEquals( "a", ((SubstringNode)leftNode).getFinal() );
            
            ExprNode rightNode = ((OrNode)orNode).getChildren().get( 1 );
            
            assertTrue( rightNode instanceof SubstringNode );
            assertEquals( "nisNetGroupTriple", ((SubstringNode)rightNode).getAttribute() );
            assertEquals( "(", ((SubstringNode)rightNode).getInitial() );
            assertEquals( 1, ((SubstringNode)rightNode).getAny().size() );
            assertEquals( ",acc1,", ((SubstringNode)rightNode).getAny().get( 0 ) );
            assertEquals( ")", ((SubstringNode)rightNode).getFinal() );
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testObjectClassAndFilterWithSpaces() throws ParseException
    {
        String str = "(&(objectClass=person)(objectClass=organizationalUnit))";
        BranchNode node = ( BranchNode ) FilterParser.parse( str );
        assertEquals( 2, node.getChildren().size() );
        assertTrue( node instanceof AndNode );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testQuotedChars() throws ParseException
    {
        String str = "(cn='~%\\28'$'\\5c)"; // note \28='(' and \5c='\'
        ExprNode node = FilterParser.parse( str );
        assertTrue( node instanceof EqualityNode );
        assertEquals( "'~%('$'\\", ( ( EqualityNode<String> ) node ).getValue().get() );
        String str2 = node.toString();
        assertEquals( str, str2 );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testQuotedCharsCase() throws ParseException
    {
        String str = "(cn='~%\\28'$'\\5Cac)"; // note \28='(' and \5c='\'
        ExprNode node = FilterParser.parse( str );
        assertTrue( node instanceof EqualityNode );
        assertEquals( "'~%('$'\\ac", ( ( EqualityNode<String> ) node ).getValue().get() );
        String str2 = node.toString();
        assertEquals( str.toUpperCase(), str2.toUpperCase() );
    }


    @Test
    public void testQuotedSubstringManyAny() throws ParseException
    {
        String str = "(ou=\\5C*\\00*\\3D*\\2Abb)";
        SubstringNode node = ( SubstringNode ) FilterParser.parse( str );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 2, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( "\\", node.getInitial() );
        assertTrue( node.getAny().contains( "\0" ) );
        assertTrue( node.getAny().contains( "=" ) );
        assertEquals( "*bb", node.getFinal() );
        String str2 = node.toString();
        assertEquals( "(ou=\\5c*\\00*=*\\2abb)", str2 );
    }


    /*
    @Test
    public void testPerf() throws ParseException
    {
        String filter = "(&(ou=abcdefg)(!(ou=hijkl))(&(a=bcd)(ew=fgh)))";
        FilterParser parser = new FilterParserImpl();
        
        long t0 = System.currentTimeMillis();
        
        for ( int i = 0; i < 1000000; i++ )
        {
            parser.parse( filter );
        }
        
        long t1 = System.currentTimeMillis();
        
        System.out.println( " Delta = " + (t1 - t0) );

        long t2 = System.currentTimeMillis();
        
        for ( int i = 0; i < 10000000; i++ )
        {
            FastFilterParserImpl.parse( filter );
        }
        
        long t3 = System.currentTimeMillis();
        
        System.out.println( " Delta = " + (t3 - t2) );
    }
    */
}
