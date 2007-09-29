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

    @Test
    public void testItemFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou~=people)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people", node.getValue() );
        assertTrue( node instanceof ApproximateNode );
    }

    
    @Test
    public void testAndFilter() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(&(ou~=people)(age>=30))" );
        assertEquals( 2, node.getChildren().size() );
        assertTrue( node instanceof AndNode );
    }

    
    @Test
    public void testAndFilterOneChildOnly() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(&(ou~=people))" );
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof AndNode );
    }


    @Test
    public void testOrFilter() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(|(ou~=people)(age>=30))" );
        assertEquals( 2, node.getChildren().size() );
        assertTrue( node instanceof OrNode );
    }


    @Test
    public void testOrFilterOneChildOnly() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(|(age>=30))" );
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof OrNode );
    }


    @Test
    public void testNotFilter() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(!(&(ou~= people)(age>=30)))" );
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof NotNode );
    }


    @Test
    public void testOptionAndEscapesFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou;lang-de>=\\23\\42asdl fkajsd)" );
        assertEquals( "ou;lang-de", node.getAttribute() );
        assertEquals( "\\23\\42asdl fkajsd", node.getValue() );
    }


    @Test
    public void testOptionsAndEscapesFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou;lang-de;version-124>=\\23\\42asdl fkajsd)" );
        assertEquals( "ou;lang-de;version-124", node.getAttribute() );
        assertEquals( "\\23\\42asdl fkajsd", node.getValue() );
    }


    @Test
    public void testNumericoidOptionsAndEscapesFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(1.3.4.2;lang-de;version-124>=\\23\\42asdl fkajsd)" );
        assertEquals( "1.3.4.2;lang-de;version-124", node.getAttribute() );
        assertEquals( "\\23\\42asdl fkajsd", node.getValue() );
    }


    @Test
    public void testPresentFilter() throws ParseException
    {
        PresenceNode node = ( PresenceNode ) FilterParser.parse( "(ou=*)" );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof PresenceNode );
    }


    @Test
    public void testNumericoidPresentFilter() throws ParseException
    {
        PresenceNode node = ( PresenceNode ) FilterParser.parse( "(1.2.3.4=*)" );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertTrue( node instanceof PresenceNode );
    }


    @Test
    public void testEqualsFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou=people)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people", node.getValue() );
        assertTrue( node instanceof EqualityNode );
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


    @Test
    public void testEqualsWithForwardSlashFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou=people/in/my/company)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people/in/my/company", node.getValue() );
        assertTrue( node instanceof EqualityNode );
    }


    @Test
    public void testExtensibleFilterForm1() throws ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(ou:dn:stupidMatch:=dummyAssertion\\23\\ac)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertTrue( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm1WithNumericOid() throws ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(1.2.3.4:dn:1.3434.23.2:=dummyAssertion\\23\\ac)" );
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertTrue( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm1NoDnAttr() throws ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(ou:stupidMatch:=dummyAssertion\\23\\ac)" );
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
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(ou:=dummyAssertion\\23\\ac)" );
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( null, node.getMatchingRuleId() );
        assertFalse( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm2() throws ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(:dn:stupidMatch:=dummyAssertion\\23\\ac)" );
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
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(:dn:1.3434.23.2:=dummyAssertion\\23\\ac)" );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertTrue( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm2NoDnAttr() throws ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(:stupidMatch:=dummyAssertion\\23\\ac)" );
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertFalse( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testExtensibleFilterForm2NoDnAttrWithNumericOidNoAttr() throws ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(:1.3434.23.2:=dummyAssertion\\23\\ac)" );
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
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=foo*)" );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( null, node.getFinal() );
    }


    @Test
    public void testSubstringNoAny() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=foo*bar)" );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    @Test
    public void testSubstringNoAnyNoIni() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=*bar)" );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 0, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertEquals( null, node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    @Test
    public void testSubstringOneAny() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=foo*guy*bar)" );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 1, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( "guy" ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    @Test
    public void testSubstringManyAny() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=a*b*c*d*e*f)" );
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
    }


    @Test
    public void testSubstringNoIniManyAny() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=*b*c*d*e*f)" );
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
    }


    @Test
    public void testSubstringManyAnyNoFinal() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=a*b*c*d*e*)" );
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
    }


    @Test
    public void testSubstringNoIniManyAnyNoFinal() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=*b*c*d*e*)" );
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
    }


    @Test
    public void testSubstringNoAnyDoubleSpaceStar() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=foo* *bar)" );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 1, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( " " ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    @Test
    public void testSubstringAnyDoubleSpaceStar() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=foo* a *bar)" );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 1, node.getAny().size() );
        assertFalse( node.getAny().contains( "" ) );
        assertTrue( node.getAny().contains( " a " ) );
        assertEquals( "foo", node.getInitial() );
        assertEquals( "bar", node.getFinal() );
    }


    /**
     * Enrique just found this bug with the filter parser when parsing substring
     * expressions like *any*. Here's the JIRA issue: <a
     * href="http://nagoya.apache.org/jira/browse/DIRLDAP-21">DIRLDAP-21</a>.
     */
    @Test
    public void testSubstringStarAnyStar() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=*foo*)" );
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );

        assertEquals( 1, node.getAny().size() );
        assertTrue( node.getAny().contains( "foo" ) );
        assertNull( node.getInitial() );
        assertNull( node.getFinal() );
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
