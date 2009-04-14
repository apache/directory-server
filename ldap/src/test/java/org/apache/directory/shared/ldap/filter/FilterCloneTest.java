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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;


/**
 * Tests the FilterParserImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 575783 $
 */
public class FilterCloneTest
{
    @SuppressWarnings("unchecked")
    @Test
    public void testItemFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou~=people)" );
        // just check that it doesnt throw for now
        node = (SimpleNode)node.clone();
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people", node.getValue().get() );
        assertTrue( node instanceof ApproximateNode );
    }

    
    @Test
    public void testAndFilter() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(&(ou~=people)(age>=30))" );
        // just check that it doesnt throw for now
        node = (BranchNode) node.clone();
        assertEquals( 2, node.getChildren().size() );
        assertTrue( node instanceof AndNode );
    }

    
    @Test
    public void testAndFilterOneChildOnly() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(&(ou~=people))" );
        // just check that it doesnt throw for now
        node = (BranchNode)node.clone();
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof AndNode );
    }


    @Test
    public void testOrFilter() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(|(ou~=people)(age>=30))" );
        // just check that it doesnt throw for now
        node = (BranchNode)node.clone();
        assertEquals( 2, node.getChildren().size() );
        assertTrue( node instanceof OrNode );
    }


    @Test
    public void testOrFilterOneChildOnly() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(|(age>=30))" );
        // just check that it doesnt throw for now
        node = (BranchNode) node.clone();
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof OrNode );
    }


    @Test
    public void testNotFilter() throws ParseException
    {
        BranchNode node = ( BranchNode ) FilterParser.parse( "(!(&(ou~= people)(age>=30)))" );
        // just check that it doesnt throw for now
        node = (BranchNode)node.clone();
        assertEquals( 1, node.getChildren().size() );
        assertTrue( node instanceof NotNode );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testOptionAndEscapesFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou;lang-de>=\\23\\42asdl fkajsd)" );
        // just check that it doesnt throw for now
        node = (SimpleNode)node.clone();
        assertEquals( "ou;lang-de", node.getAttribute() );
        assertEquals( "#Basdl fkajsd", node.getValue().get() );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testOptionsAndEscapesFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou;lang-de;version-124>=\\23\\42asdl fkajsd)" );
        // just check that it doesnt throw for now
        node = (SimpleNode)node.clone();
        assertEquals( "ou;lang-de;version-124", node.getAttribute() );
        assertEquals( "#Basdl fkajsd", node.getValue().get() );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testNumericoidOptionsAndEscapesFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(1.3.4.2;lang-de;version-124>=\\23\\42asdl fkajsd)" );
        // just check that it doesnt throw for now
        node = (SimpleNode)node.clone();
        assertEquals( "1.3.4.2;lang-de;version-124", node.getAttribute() );
        assertEquals( "#Basdl fkajsd", node.getValue().get() );
    }


    @Test
    public void testPresentFilter() throws ParseException
    {
        PresenceNode node = ( PresenceNode ) FilterParser.parse( "(ou=*)" );
        // just check that it doesnt throw for now
        node = (PresenceNode) node.clone();
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof PresenceNode );
    }


    @Test
    public void testNumericoidPresentFilter() throws ParseException
    {
        PresenceNode node = ( PresenceNode ) FilterParser.parse( "(1.2.3.4=*)" );
        // just check that it doesnt throw for now
        node = ( PresenceNode )node.clone();
        assertEquals( "1.2.3.4", node.getAttribute() );
        assertTrue( node instanceof PresenceNode );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testEqualsFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou=people)" );
        // just check that it doesnt throw for now
        node = ( SimpleNode) node.clone();
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people", node.getValue().get() );
        assertTrue( node instanceof EqualityNode );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testEqualsWithForwardSlashFilter() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou=people/in/my/company)" );
        // just check that it doesnt throw for now
        node = (SimpleNode) node.clone();
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "people/in/my/company", node.getValue().get() );
        assertTrue( node instanceof EqualityNode );
    }


    @Test
    public void testExtensibleFilterForm1() throws ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(ou:dn:stupidMatch:=dummyAssertion\\23\\ac)" );
        // just check that it doesnt throw for now
        node = ( ExtensibleNode ) node.clone();
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
        // just check that it doesnt throw for now
        node = ( ExtensibleNode )node.clone();
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
        // just check that it doesnt throw for now
        node = ( ExtensibleNode ) node.clone();
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertFalse( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


 
    @Test
    public void testExtensibleFilterForm1NoAttrNoMatchingRule() throws ParseException
    {
        ExtensibleNode node = ( ExtensibleNode ) FilterParser.parse( "(ou:=dummyAssertion\\23\\ac)" );
        // just check that it doesnt throw for now
        node = (ExtensibleNode) node.clone();
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
        // just check that it doesnt throw for now
        node = ( ExtensibleNode ) node.clone();
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "stupidMatch", node.getMatchingRuleId() );
        assertTrue( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
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
        ExtensibleNode node1 = ( ExtensibleNode ) FilterParser.parse( "(:stupidMatch:=dummyAssertion\\23\\ac)" );
        // just check that it doesnt throw for now
        ExtensibleNode node = ( ExtensibleNode )node1.clone();
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
        // just check that it doesnt throw for now
        node = ( ExtensibleNode) node.clone();
        assertEquals( null, node.getAttribute() );
        assertEquals( "dummyAssertion\\23\\ac", StringTools.utf8ToString( node.getValue() ) );
        assertEquals( "1.3434.23.2", node.getMatchingRuleId() );
        assertFalse( node.hasDnAttributes() );
        assertTrue( node instanceof ExtensibleNode );
    }


    @Test
    public void testSubstringNoAnyNoFinal() throws ParseException
    {
        SubstringNode node = ( SubstringNode ) FilterParser.parse( "(ou=foo*)" );
        // just check that it doesnt throw for now
        node = ( SubstringNode ) node.clone();
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
        // just check that it doesnt throw for now
        node = ( SubstringNode ) node.clone();
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
        // just check that it doesnt throw for now
        node = ( SubstringNode )node.clone();
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
        // just check that it doesnt throw for now
        node = ( SubstringNode )node.clone();
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
        // just check that it doesnt throw for now
        node = ( SubstringNode )node.clone();
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
        // just check that it doesnt throw for now
        node = ( SubstringNode )node.clone();
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
        // just check that it doesnt throw for now
        node = ( SubstringNode )node.clone();
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
        // just check that it doesnt throw for now
        node = ( SubstringNode ) node.clone();
        
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
        // just check that it doesnt throw for now
        node = ( SubstringNode )node.clone();
        
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
        // just check that it doesnt throw for now
        node = ( SubstringNode ) node.clone();
        
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
        // just check that it doesnt throw for now
        node = ( SubstringNode )node.clone();
        
        assertEquals( "ou", node.getAttribute() );
        assertTrue( node instanceof SubstringNode );
        assertEquals( 1, node.getAny().size() );
        assertTrue( node.getAny().contains( "foo" ) );
        assertNull( node.getInitial() );
        assertNull( node.getFinal() );
    }
    
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEqualsFilterNullValue() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(ou=)" );
        // just check that it doesnt throw for now
        node = ( SimpleNode )node.clone();
        
        assertEquals( "ou", node.getAttribute() );
        assertEquals( "", node.getValue().get() );
        assertTrue( node instanceof EqualityNode );
    }


    /**
     * test a filter with a # in value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEqualsFilterWithPoundInValue() throws ParseException
    {
        SimpleNode node = ( SimpleNode ) FilterParser.parse( "(uid=#f1)" );
        // just check that it doesnt throw for now
        node = ( SimpleNode ) node.clone();
        assertEquals( "uid", node.getAttribute() );
        assertEquals( "#f1", node.getValue().get() );
        assertTrue( node instanceof EqualityNode );
    }

    
    @Test
    public void testLargeBusyFilter() throws ParseException
    {
        ExprNode node1 = FilterParser.parse( "(&(|(2.5.4.3=h*)(2.5.4.4=h*)(2.16.840.1.113730.3.1.241=h*)(2.5.4.42=h*))(!(objectClass=computer))(|(objectClass=person)(objectClass=group)(objectClass=organizationalUnit)(objectClass=domain))(!(&(userAccountControl:1.2.840.113556.1.4.803:=2))))" );
        // just check that it doesnt throw for now
        ExprNode node = node1.clone();
        assertTrue(node instanceof AndNode);
        //TODO test full structure
    }
}
