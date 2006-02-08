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
package org.apache.directory.shared.ldap.filter;


import java.io.IOException;
import java.text.ParseException;

import org.apache.directory.shared.ldap.filter.AbstractExprNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;


/**
 * Tests the FilterParserImpl class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class FilterParserImplTest extends TestCase
{
    private FilterParserImpl parser;


    protected void setUp() throws Exception
    {
        parser = new FilterParserImpl();
    }


    protected void tearDown() throws Exception
    {
        parser = null;
    }


    public void testItemFilter() throws IOException, ParseException
    {
        SimpleNode node = ( SimpleNode ) parser.parse( "( ou ~= people )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people", node.getValue() );
        assertEquals( AbstractExprNode.APPROXIMATE, node.getAssertionType() );
    }


    public void testAndFilter() throws IOException, ParseException
    {
        BranchNode node = ( BranchNode ) parser.parse( "(& ( ou ~= people ) (age>=30) ) " );
        assertEquals( 2, node.getChildren().size() );
        assertEquals( AbstractExprNode.AND, node.getOperator() );
    }


    public void testOrFilter() throws IOException, ParseException
    {
        BranchNode node = ( BranchNode ) parser.parse( "(| ( ou ~= people ) (age>=30) ) " );
        assertEquals( 2, node.getChildren().size() );
        assertEquals( AbstractExprNode.OR, node.getOperator() );
    }


    public void testNotFilter() throws IOException, ParseException
    {
        BranchNode node = ( BranchNode ) parser.parse( "( ! (& ( ou ~= people ) (age>=30) ) )" );
        assertEquals( 1, node.getChildren().size() );
        assertEquals( AbstractExprNode.NOT, node.getOperator() );
    }


    public void testOptionAndEscapesFilter() throws IOException, ParseException
    {
        SimpleNode node = ( SimpleNode ) parser.parse( "( ou;lang-de >= \\23\\42asdl fkajsd )" );
        assertEquals( "ou;lang-de", node.getAttribute() );
        assertEquals( "\\23\\42asdl fkajsd", node.getValue() );
    }


    public void testOptionsAndEscapesFilter() throws IOException, ParseException
    {
        SimpleNode node = ( SimpleNode ) parser.parse( "( ou;lang-de;version-124 >= \\23\\42asdl fkajsd )" );
        assertEquals( "ou;lang-de;version-124", node.getAttribute() );
        assertEquals( "\\23\\42asdl fkajsd", node.getValue() );
    }


    public void testNumericoidOptionsAndEscapesFilter() throws IOException, ParseException
    {
        SimpleNode node = ( SimpleNode ) parser.parse( "( 1.3.4.2;lang-de;version-124 >= \\23\\42asdl fkajsd )" );
        assertEquals( "1.3.4.2;lang-de;version-124", node.getAttribute() );
        assertEquals( "\\23\\42asdl fkajsd", node.getValue() );
    }


    public void testPresentFilter() throws IOException, ParseException
    {
        PresenceNode node = ( PresenceNode ) parser.parse( "( ou =*)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "( ou =* )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "( ou =  * )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "(  ou = *)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "( ou =* ) " );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "( ou =*)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );
    }


    public void testNumericoidPresentFilter() throws IOException, ParseException
    {
        PresenceNode node = ( PresenceNode ) parser.parse( "( 1.2.3.4 = * )" );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "( 1.2.3.4 =  * )" );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "(  1.2.3.4 = *)" );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "( 1.2.3.4 =* ) " );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );

        node = ( PresenceNode ) parser.parse( "( 1.2.3.4 =*)" );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( AbstractExprNode.PRESENCE, node.getAssertionType() );
    }


    public void testEqualsFilter() throws IOException, ParseException
    {
        SimpleNode node = ( SimpleNode ) parser.parse( "( ou = people )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people", node.getValue() );
        assertEquals( AbstractExprNode.EQUALITY, node.getAssertionType() );
    }


    public void testEqualsWithForwardSlashFilter() throws IOException, ParseException
    {
        SimpleNode node = ( SimpleNode ) parser.parse( "( ou = people/in/my/company )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people/in/my/company", node.getValue() );
        assertEquals( AbstractExprNode.EQUALITY, node.getAssertionType() );
    }


    public void testExtensibleFilterForm1() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( ou :dn :stupidMatch := dummyAssertion\\23\\ac )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertTrue( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm1WithNumericOid() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( 1.2.3.4 :dn :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertTrue( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm1NoDnAttr() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( ou :stupidMatch := dummyAssertion\\23\\ac )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertFalse( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm1OptionOnRule() throws IOException, ParseException
    {
        try
        {
            parser.parse( "( ou :stupidMatch;lang-de := dummyAssertion\\23\\ac )" );
            fail( "we should never get here" );
        }
        catch ( IOException e )
        {
        }
        catch ( ParseException e )
        {
        }
    }


    public void testExtensibleFilterForm1NoAttrNoMatchingRule() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( ou := dummyAssertion\\23\\ac )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( null, node.getMatchingRuleId() );
        assertFalse( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm1NoDnAttrWithNumericOidNoAttr() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( 1.2.3.4 :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertFalse( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm2() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( :dn :stupidMatch := dummyAssertion\\23\\ac )" );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertTrue( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm2OptionOnRule() throws IOException, ParseException
    {
        try
        {
            parser.parse( "( :dn :stupidMatch;lang-en := dummyAssertion\\23\\ac )" );
            fail( "we should never get here" );
        }
        catch ( IOException e )
        {
        }
        catch ( ParseException e )
        {
        }
    }


    public void testExtensibleFilterForm2WithNumericOid() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( :dn :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertTrue( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm2NoDnAttr() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( :stupidMatch := dummyAssertion\\23\\ac )" );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertFalse( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm2NoDnAttrWithNumericOidNoAttr() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertFalse( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm3() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( := dummyAssertion )" );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( null, node.getMatchingRuleId() );
        assertFalse( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testExtensibleFilterForm3WithEscapes() throws IOException, ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) parser.parse( "( := dummyAssertion\\23\\ac )" );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( null, node.getMatchingRuleId() );
        assertFalse( node.dnAttributes() );
        assertEquals( AbstractExprNode.EXTENSIBLE, node.getAssertionType() );
    }


    public void testReuseParser() throws IOException, ParseException
    {
        parser.parse( "( ou ~= people )" );
        parser.parse( "(& ( ou ~= people ) (age>=30) ) " );
        parser.parse( "(| ( ou ~= people ) (age>=30) ) " );
        parser.parse( "( ! (& ( ou ~= people ) (age>=30) ) )" );
        parser.parse( "( ou;lang-de >= \\23\\42asdl fkajsd )" );
        parser.parse( "( ou;lang-de;version-124 >= \\23\\42asdl fkajsd )" );
        parser.parse( "( 1.3.4.2;lang-de;version-124 >= \\23\\42asdl fkajsd )" );
        parser.parse( "( ou =* )" );
        parser.parse( "( 1.2.3.4 = * )" );
        parser.parse( "( ou = people )" );
        parser.parse( "( ou = people/in/my/company )" );
        parser.parse( "( ou :dn :stupidMatch := dummyAssertion\\23\\ac )" );
        parser.parse( "( 1.2.3.4 :dn :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        parser.parse( "( ou :stupidMatch := dummyAssertion\\23\\ac )" );
        parser.parse( "( ou := dummyAssertion\\23\\ac )" );
        parser.parse( "( 1.2.3.4 :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        parser.parse( "( :dn :stupidMatch := dummyAssertion\\23\\ac )" );
        parser.parse( "( :dn :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        parser.parse( "( :stupidMatch := dummyAssertion\\23\\ac )" );
        parser.parse( "( :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        parser.parse( "( := dummyAssertion\\23\\ac )" );
    }


    public void testReuseParserAfterFailures() throws IOException, ParseException
    {
        parser.parse( "( ou ~= people )" );
        parser.parse( "(& ( ou ~= people ) (age>=30) ) " );
        parser.parse( "(| ( ou ~= people ) (age>=30) ) " );
        parser.parse( "( ! (& ( ou ~= people ) (age>=30) ) )" );
        parser.parse( "( ou;lang-de >= \\23\\42asdl fkajsd )" );
        parser.parse( "( ou;lang-de;version-124 >= \\23\\42asdl fkajsd )" );
        parser.parse( "( 1.3.4.2;lang-de;version-124 >= \\23\\42asdl fkajsd )" );
        try
        {
            parser.parse( "( ou :stupidMatch;lang-de := dummyAssertion\\23\\ac )" );
            fail( "we should never get here" );
        }
        catch ( IOException e )
        {
        }
        catch ( ParseException e )
        {
        }
        parser.parse( "( ou =* )" );
        parser.parse( "( 1.2.3.4 = * )" );
        parser.parse( "( ou = people )" );
        parser.parse( "( ou = people/in/my/company )" );
        parser.parse( "( ou :dn :stupidMatch := dummyAssertion\\23\\ac )" );
        parser.parse( "( 1.2.3.4 :dn :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        parser.parse( "( ou :stupidMatch := dummyAssertion\\23\\ac )" );
        try
        {
            parser.parse( "( :dn :stupidMatch;lang-en := dummyAssertion\\23\\ac )" );
            fail( "we should never get here" );
        }
        catch ( IOException e )
        {
        }
        catch ( ParseException e )
        {
        }
        parser.parse( "( ou := dummyAssertion\\23\\ac )" );
        parser.parse( "( 1.2.3.4 :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        parser.parse( "( :dn :stupidMatch := dummyAssertion\\23\\ac )" );
        parser.parse( "( :dn :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        parser.parse( "( :stupidMatch := dummyAssertion\\23\\ac )" );
        parser.parse( "( :1.3434.23.2 := dummyAssertion\\23\\ac )" );
        parser.parse( "( := dummyAssertion\\23\\ac )" );
    }


    public void testNullOrEmptyString() throws IOException, ParseException
    {
        assertNull( parser.parse( null ) );
        assertNull( parser.parse( "" ) );
    }


    public void testSubstringNoAnyNoFinal() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou = foo* )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( null, node.getFinal() );
    }


    public void testSubstringNoAny() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou = foo*bar )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    public void testSubstringNoAnyNoIni() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou = *bar )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( null, node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    public void testSubstringOneAny() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou = foo*guy*bar )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 1, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "guy" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    public void testSubstringManyAny() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou =a*b*c*d*e*f )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 4, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "b" ) );
        assertTrue( node.getAny().contains( "c" ) );
        assertTrue( node.getAny().contains( "d" ) );
        assertTrue( node.getAny().contains( "e" ) );
        assertEquals( "a", node.getInitial() );
        assertEquals( "f", node.getFinal() );
    }


    public void testSubstringNoIniManyAny() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou =*b*c*d*e*f )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 4, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "e" ) );
        assertTrue( node.getAny().contains( "b" ) );
        assertTrue( node.getAny().contains( "c" ) );
        assertTrue( node.getAny().contains( "d" ) );
        assertEquals( null, node.getInitial() );
        assertEquals( "f", node.getFinal() );
    }


    public void testSubstringManyAnyNoFinal() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou =a*b*c*d*e* )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 4, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "e" ) );
        assertTrue( node.getAny().contains( "b" ) );
        assertTrue( node.getAny().contains( "c" ) );
        assertTrue( node.getAny().contains( "d" ) );
        assertEquals( "a", node.getInitial() );
        assertEquals( null, node.getFinal() );
    }


    public void testSubstringNoIniManyAnyNoFinal() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou =*b*c*d*e* )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 4, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "e" ) );
        assertTrue( node.getAny().contains( "b" ) );
        assertTrue( node.getAny().contains( "c" ) );
        assertTrue( node.getAny().contains( "d" ) );
        assertEquals( null, node.getInitial() );
        assertEquals( null, node.getFinal() );
    }


    public void testSubstringNoAnyDoubleSpaceStar() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou = foo* *bar )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertFalse( node.getAny().contains( " " ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    public void testSubstringAnyDoubleSpaceStar() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou = foo* a *bar )" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 1, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "a" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    /**
     * Enrique just found this bug with the filter parser when parsing substring
     * expressions like *any*.  Here's the JIRA issue:
     * <a href="http://nagoya.apache.org/jira/browse/DIRLDAP-21">DIRLDAP-21</a>.
     */
    public void testSubstringStarAnyStar() throws IOException, ParseException
    {
        SubstringNode node = ( SubstringNode ) parser.parse( "( ou =*foo*)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( AbstractExprNode.SUBSTRING, node.getAssertionType() );

        assertEquals( 1, node.getAny().size() );
        assertTrue( node.getAny().contains( "foo" ) );
        assertNull( node.getInitial() );
        assertNull( node.getFinal() );
    }


    /* @todo look at custom error handlers for the parser */
    /////// Causes parser to hang rather than really bombing out.  Looks like
    /////// we may need to implement a custom error handler for this parser.
//    public void testSubstringNoAnyDoubleStar() throws IOException, ParseException
//    {
//        SubstringNode node = null;
//
//        try
//        {
//            node = ( SubstringNode ) parser.parse( "( ou = foo**bar )" );
//            fail("should not get here");
//        }
//        catch( Exception e )
//        {
//        }
//
//        assertNull( node );
//    }

}
