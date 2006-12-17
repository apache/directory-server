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
package org.apache.directory.shared.ldap.schema.syntax;


import java.text.ParseException;

import junit.framework.TestCase;

import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.syntax.parser.AttributeTypeDescriptionSchemaParser;


/**
 * Tests the AttributeTypeDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaParserAttributeTypeDescriptionTest extends TestCase
{
    /** the parser instance */
    private AttributeTypeDescriptionSchemaParser parser;


    protected void setUp() throws Exception
    {
        parser = new AttributeTypeDescriptionSchemaParser();
    }


    protected void tearDown() throws Exception
    {
        parser = null;
    }


    /**
     * Test numericoid
     * 
     * @throws ParseException
     */
    public void testNumericOid() throws ParseException
    {
        SchemaParserTestUtils.testNumericOid( parser, "SYNTAX 1.1" );
    }


    /**
     * Tests NAME and its values
     * 
     * @throws ParseException
     */
    public void testNames() throws ParseException
    {
        SchemaParserTestUtils.testNames( parser, "1.1", "SYNTAX 1.1" );
    }


    /**
     * Tests DESC
     * 
     * @throws ParseException
     */
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "SYNTAX 1.1" );
    }


    /**
     * Tests OBSOLETE
     * 
     * @throws ParseException
     */
    public void testObsolete() throws ParseException
    {
        SchemaParserTestUtils.testObsolete( parser, "1.1", "SYNTAX 1.1" );
    }


    /**
     * Test SUP and its value.
     * 
     * @throws ParseException
     */
    public void testSuperType() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // no SUP
        value = "( 1.1 SYNTAX 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getSuperType() );

        // SUP numericoid
        value = "( 1.1 SYNTAX 1.1 SUP 1.2.3.4.5.6.7.8.9.0 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4.5.6.7.8.9.0", atd.getSuperType() );

        // SUP descr
        value = "( 1.1 SYNTAX 1.1 SUP abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", atd.getSuperType() );

        // no quote allowed
        value = "( 1.1 SYNTAX 1.1 SUP 'name' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SUP 'name' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no quote allowed
        value = "( 1.1 SYNTAX 1.1 SUP '1.2.3.4' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SUP '1.2.3.4' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // invalid character
        value = "( 1.1 SYNTAX 1.1 SUP 1.2.3.4.A )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SUP '1.2.3.4.A' (invalid character)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // only single SUP allowed
        value = "( 1.1 SYNTAX 1.1 SUP ( name1 $ name2 ) )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, only single SUP allowed" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // empty sup
        value = "( 1.1 SYNTAX 1.1 SUP )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, no SUP value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests EQUALITY and its values.
     * Very similar to SUP, so here are less test cases. 
     * 
     * @throws ParseException
     */
    public void testEquality() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // no EQUALITY
        value = "( 1.1 SYNTAX 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getEqualityMatchingRule() );

        // EQUALITY numericoid
        value = "( 1.1 SYNTAX 1.1 EQUALITY 1.2.3.4567.8.9.0 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", atd.getEqualityMatchingRule() );

        // EQUALITY descr
        value = "( 1.1 SYNTAX 1.1 EQUALITY abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", atd.getEqualityMatchingRule() );

        // no quote allowed
        value = "( 1.1 SYNTAX 1.1 EQUALITY 'caseExcactMatch' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid EQUALITY 'caseExcactMatch' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests ORDERING and its values.
     * Very similar to SUP, so here are less test cases. 
     * 
     * @throws ParseException
     */
    public void testOrdering() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // no EQUALITY
        value = "( 1.1 SYNTAX 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getOrderingMatchingRule() );

        // EQUALITY numericoid
        value = "( 1.1 SYNTAX 1.1 ORDERING 1.2.3.4567.8.9.0 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", atd.getOrderingMatchingRule() );

        // EQUALITY descr
        value = "( 1.1 SYNTAX 1.1 ORDERING abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", atd.getOrderingMatchingRule() );

        // no quote allowed
        value = "( 1.1 SYNTAX 1.1 ORDERING 'generalizedTimeOrderingMatch' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid ORDERING 'generalizedTimeOrderingMatch' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests SUBSTRING and its values.
     * Very similar to SUP, so here are less test cases. 
     * 
     * @throws ParseException
     */
    public void testSubstring() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // no EQUALITY
        value = "( 1.1 SYNTAX 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getSubstringsMatchingRule() );

        // EQUALITY numericoid
        value = "( 1.1 SYNTAX 1.1 SUBSTR 1.2.3.4567.8.9.0 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", atd.getSubstringsMatchingRule() );

        // EQUALITY descr
        value = "( 1.1 SYNTAX 1.1 SUBSTR abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", atd
            .getSubstringsMatchingRule() );

        // no quote allowed
        value = "( 1.1 SYNTAX 1.1 SUBSTR 'caseIgnoreSubstringsMatch' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SUBSTR 'caseIgnoreSubstringsMatch' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests SYNTAX
     * 
     * @throws ParseException
     */
    public void testSyntax() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // no SYNTAX
        value = "( 1.1 SUP 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getSyntax() );
        assertEquals( 0, atd.getSyntaxLength() );

        // SYNTAX numericoid
        value = "( 1.1 SYNTAX 1.2.3.4567.8.9.0 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", atd.getSyntax() );
        assertEquals( 0, atd.getSyntaxLength() );

        // SYNTAX numericoid and length
        value = "( 1.1 SYNTAX 1.2.3.4567.8.9.0{1234567890} )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", atd.getSyntax() );
        assertEquals( 1234567890, atd.getSyntaxLength() );

        // SYNTAX numericoid and zero length
        value = "( 1.1 SYNTAX 1.2.3{0} )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", atd.getSyntax() );
        assertEquals( 0, atd.getSyntaxLength() );

        // no quote allowed
        value = "( 1.1 SYNTAX '1.2.3{32}' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SYNTAX '1.2.3{32}' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // empty syntax
        value = "( 1.1 SYNTAX 1.2.3.4{} )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SYNTAX 1.2.3.4{} (empty length)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // leading zero in length
        value = "( 1.1 SYNTAX 1.2.3.4{01} )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SYNTAX 1.2.3.4{01} (leading zero in length)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // invalid syntax
        value = "( 1.1 SYNTAX 1.2.3.4{X} )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SYNTAX 1.2.3.4{X} (invalid length)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no syntax
        value = "( 1.1 SYNTAX {32} )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SYNTAX {32} (no syntax)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // length overflow
        value = "( 1.1 SYNTAX 1.2.3.4{123456789012} )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SYNTAX 1.2.3.4{123456789012} (length overflow)" );
        }
        catch ( NumberFormatException nfe )
        {
            // expected
        }

    }


    /**
     * Tests SINGLE-VALUE
     * 
     * @throws ParseException
     */
    public void testSingleValue() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // not single-value
        value = "( 1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isSingleValued() );

        // single-value
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' SINGLE-VALUE)";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isSingleValued() );

        // single-value 
        value = "(1.1 SYNTAX 1.1 SINGLE-VALUE)";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isSingleValued() );

        // ivalid
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' SINGLE-VALU )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SINGLE-VALUE value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests COLLECTIVE
     * 
     * @throws ParseException
     */
    public void testCollective() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // not collective
        value = "( 1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isCollective() );

        // single-value
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' COLLECTIVE )";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isCollective() );

        // single-value 
        value = "(1.1 SYNTAX 1.1 COLLECTIVE)";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isCollective() );

        // ivalid
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' COLLECTIV )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid COLLECTIVE value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests NO-USER-MODIFICATION
     * 
     * @throws ParseException
     */
    public void testNoUserModification() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // not NO-USER-MODIFICATION
        value = "( 1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isUserModifiable() );

        // NO-USER-MODIFICATION
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' NO-USER-MODIFICATION USAGE directoryOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isUserModifiable() );

        // NO-USER-MODIFICATION 
        value = "(1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE directoryOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isUserModifiable() );

        // ivalid
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' NO-USER-MODIFICATIO USAGE directoryOperation )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NO-USER-MODIFICATION value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests usage 
     * 
     * @throws ParseException
     */
    public void testUsage() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // DEFAULT is userApplications
        value = "( 1.1 SYNTAX 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.USER_APPLICATIONS, atd.getUsage() );

        // userApplications
        value = "( 1.1 SYNTAX 1.1 USAGE userApplications )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.USER_APPLICATIONS, atd.getUsage() );

        // directoryOperation
        value = "( 1.1 SYNTAX 1.1 USAGE directoryOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DIRECTORY_OPERATION, atd.getUsage() );

        // AUXILIARY
        value = "( 1.1 SYNTAX 1.1 USAGE distributedOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DISTRIBUTED_OPERATION, atd.getUsage() );

        // STRUCTURAL
        value = "( 1.1 SYNTAX 1.1 USAGE dSAOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DSA_OPERATION, atd.getUsage() );

        // TODO: case insensitive?

        // ivalid
        value = "( 1.1 SYNTAX 1.1 USAGE abc )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid USAGE value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Test extensions.
     * 
     * @throws ParseException
     */
    public void testExtensions() throws ParseException
    {
        SchemaParserTestUtils.testExtensions( parser, "1.1", "SYNTAX 1.1" );
    }


    /**
     * Test full attribute type description.
     * 
     * @throws ParseException
     */
    public void testFull() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton äöüß 部長' OBSOLETE SUP abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 EQUALITY 2.3.4.5.6.7.8.9.0.1 ORDERING 3.4.5.6.7.8.9.0.1.2 SUBSTR 4.5.6.7.8.9.0.1.2.3 SYNTAX 5.6.7.8.9.0.1.2.3.4{1234567890} SINGLE-VALUE NO-USER-MODIFICATION USAGE dSAOperation X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )";
        atd = parser.parseAttributeTypeDescription( value );

        assertEquals( "1.2.3.4.5.6.7.8.9.0", atd.getNumericOid() );
        assertEquals( 2, atd.getNames().size() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", atd.getNames().get( 0 ) );
        assertEquals( "test", atd.getNames().get( 1 ) );
        assertEquals( "Descripton äöüß 部長", atd.getDescription() );
        assertTrue( atd.isObsolete() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", atd.getSuperType() );
        assertEquals( "2.3.4.5.6.7.8.9.0.1", atd.getEqualityMatchingRule() );
        assertEquals( "3.4.5.6.7.8.9.0.1.2", atd.getOrderingMatchingRule() );
        assertEquals( "4.5.6.7.8.9.0.1.2.3", atd.getSubstringsMatchingRule() );
        assertEquals( "5.6.7.8.9.0.1.2.3.4", atd.getSyntax() );
        assertEquals( 1234567890, atd.getSyntaxLength() );

        assertTrue( atd.isSingleValued() );
        assertFalse( atd.isCollective() );
        assertFalse( atd.isUserModifiable() );
        assertEquals( UsageEnum.DSA_OPERATION, atd.getUsage() );

        assertEquals( 2, atd.getExtensions().size() );
        assertNotNull( atd.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, atd.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", atd.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", atd.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( atd.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, atd.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", atd.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", atd.getExtensions().get( "X-TEST-b" ).get( 1 ) );
    }

    
    /**
     * Test unique elements.
     * 
     * @throws ParseException
     */
    public void testUniqueElements() throws ParseException
    {
        String[] testValues = new String[]
            { 
                "( 1.1 SYNTAX 1.1 NAME 'test1' NAME 'test2' )", 
                "( 1.1 SYNTAX 1.1 DESC 'test1' DESC 'test2' )",
                "( 1.1 SYNTAX 1.1 OBSOLETE OBSOLETE )", 
                "( 1.1 SYNTAX 1.1 SUP test1 SUP test2 )",
                "( 1.1 SYNTAX 1.1 EQUALITY test1 EQUALITY test2 )",
                "( 1.1 SYNTAX 1.1 ORDERING test1 ORDERING test2 )",
                "( 1.1 SYNTAX 1.1 SUBSTR test1 SUBSTR test2 )",
                "( 1.1 SYNTAX 1.1 SYNTAX 2.2 SYNTAX 3.3 )",
                "( 1.1 SYNTAX 1.1 SINGLE-VALUE SINGLE-VALUE )",
                "( 1.1 SYNTAX 1.1 COLLECTIVE COLLECTIVE )", 
                "( 1.1 SYNTAX 1.1 USAGE directoryOperation NO-USER-MODIFICATION NO-USER-MODIFICATION )", 
                "( 1.1 SYNTAX 1.1 USAGE directoryOperation USAGE userApplications )", 
                "( 1.1 SYNTAX 1.1 X-TEST 'test1' X-TEST 'test2' )" 
            };
        SchemaParserTestUtils.testUnique( parser, testValues );
    }
    
    
    /**
     * Test required elements.
     * 
     * @throws ParseException
     */
    public void testRequiredElements() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 SYNTAX 1.1 SUP 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNotNull( atd.getSyntax() );
        assertNotNull( atd.getSuperType() );
        
        value = "( 1.2.3.4.5.6.7.8.9.0 SYNTAX 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNotNull( atd.getSyntax() );
        assertNull( atd.getSuperType() );
        
        value = "( 1.2.3.4.5.6.7.8.9.0 SUP 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getSyntax() );
        assertNotNull( atd.getSuperType() );

        value = "( 1.2.3.4.5.6.7.8.9.0 )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, SYNTAX or SUP is required" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

    }
    
    /**
     * Test collective constraint:
     * COLLECTIVE requires USAGE userApplications
     * 
     * @throws ParseException
     */
    public void testCollecitveConstraint() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        value = "( 1.1 SYNTAX 1.1 COLLECTIVE )";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isCollective() );
        assertEquals( UsageEnum.USER_APPLICATIONS , atd.getUsage() );
        
        value = "( 1.1 SYNTAX 1.1 COLLECTIVE USAGE userApplications )";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isCollective() );
        assertEquals( UsageEnum.USER_APPLICATIONS , atd.getUsage() );
        
        value = "( 1.1 SYNTAX 1.1 COLLECTIVE USAGE directoryOperation )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, COLLECTIVE requires USAGE userApplications" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        
        value = "( 1.1 SYNTAX 1.1 COLLECTIVE USAGE dSAOperation )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, COLLECTIVE requires USAGE userApplications" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        
        value = "( 1.1 SYNTAX 1.1 COLLECTIVE USAGE distributedOperation )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, COLLECTIVE requires USAGE userApplications" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        
    }
    
    
    /**
     * Test no-user-modification constraint:
     * NO-USER-MODIFICATION requires an operational USAGE
     * 
     * @throws ParseException
     */
    public void testNoUserModificatonConstraint() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;
        
        value = "( 1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE directoryOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isUserModifiable() );
        assertEquals( UsageEnum.DIRECTORY_OPERATION , atd.getUsage() );
        
        value = "( 1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE dSAOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isUserModifiable() );
        assertEquals( UsageEnum.DSA_OPERATION , atd.getUsage() );
        
        value = "( 1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE distributedOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isUserModifiable() );
        assertEquals( UsageEnum.DISTRIBUTED_OPERATION , atd.getUsage() );
        
        value = "( 1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE userApplications )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, NO-USER-MODIFICATION requires an operational USAGE" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        
        value = "( 1.1 SYNTAX 1.1 NO-USER-MODIFICATION )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, NO-USER-MODIFICATION requires an operational USAGE" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }
    
    
    /**
     * Ensure that element order is ignored
     * 
     * @throws ParseException
     */
    public void testIgnoreElementOrder() throws ParseException
    {
        String value = "( 2.5.4.3 SUP name SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications DESC 'RFC2256: common name(s) for which the entity is known by'  EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch NAME ( 'cn' 'commonName' )  )";
        AttributeTypeDescription atd = parser.parseAttributeTypeDescription( value );

        assertEquals( "2.5.4.3", atd.getNumericOid() );
        assertEquals( 2, atd.getNames().size() );
        assertEquals( "cn", atd.getNames().get( 0 ) );
        assertEquals( "commonName", atd.getNames().get( 1 ) );
        assertEquals( "RFC2256: common name(s) for which the entity is known by", atd.getDescription() );
        assertEquals( "name", atd.getSuperType() );
        assertEquals( "caseIgnoreMatch", atd.getEqualityMatchingRule() );
        assertEquals( "caseIgnoreSubstringsMatch", atd.getSubstringsMatchingRule() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", atd.getSyntax() );
        assertEquals( UsageEnum.USER_APPLICATIONS, atd.getUsage() );
        assertEquals( 0, atd.getExtensions().size() );
    }


    ////////////////////////////////////////////////////////////////
    //         Some real-world attribute type definitions         //
    ////////////////////////////////////////////////////////////////

    public void testRfcUid() throws ParseException
    {
        String value = "( 0.9.2342.19200300.100.1.1 NAME ( 'uid' 'userid' ) DESC 'RFC1274: user identifier' EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{256} USAGE userApplications )";
        AttributeTypeDescription atd = parser.parseAttributeTypeDescription( value );

        assertEquals( "0.9.2342.19200300.100.1.1", atd.getNumericOid() );
        assertEquals( 2, atd.getNames().size() );
        assertEquals( "uid", atd.getNames().get( 0 ) );
        assertEquals( "userid", atd.getNames().get( 1 ) );
        assertEquals( "RFC1274: user identifier", atd.getDescription() );
        assertNull( atd.getSuperType() );

        assertEquals( "caseIgnoreMatch", atd.getEqualityMatchingRule() );
        assertEquals( "caseIgnoreSubstringsMatch", atd.getSubstringsMatchingRule() );
        assertNull( atd.getOrderingMatchingRule() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", atd.getSyntax() );
        assertEquals( 256, atd.getSyntaxLength() );
        assertEquals( UsageEnum.USER_APPLICATIONS, atd.getUsage() );

        assertFalse( atd.isObsolete() );
        assertFalse( atd.isCollective() );
        assertFalse( atd.isSingleValued() );
        assertTrue( atd.isUserModifiable() );

        assertEquals( 0, atd.getExtensions().size() );
    }


    /**
     * Tests the multithreaded use of a single parser.
     */
    public void testMultiThreaded() throws Exception
    {
        String[] testValues = new String[]
            {
                "( 1.1 SYNTAX 1.1 )",
                "( 2.5.4.41 NAME 'name' DESC 'RFC2256: common supertype of name attributes'  EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} USAGE userApplications )",
                "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications )",
                "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton äöüß 部長' OBSOLETE SUP abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 EQUALITY 2.3.4.5.6.7.8.9.0.1 ORDERING 3.4.5.6.7.8.9.0.1.2 SUBSTR 4.5.6.7.8.9.0.1.2.3 SYNTAX 5.6.7.8.9.0.1.2.3.4{1234567890} SINGLE-VALUE NO-USER-MODIFICATION USAGE dSAOperation X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )" };
        SchemaParserTestUtils.testMultiThreaded( parser, testValues );
    }

}
