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

import org.apache.directory.shared.ldap.schema.syntax.parser.MatchingRuleDescriptionSchemaParser;


/**
 * Tests the MatchingRuleDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaParserMatchingRuleDescriptionTest extends TestCase
{
    /** the parser instance */
    private MatchingRuleDescriptionSchemaParser parser;

    protected void setUp() throws Exception
    {
        parser = new MatchingRuleDescriptionSchemaParser();
    }


    protected void tearDown() throws Exception
    {
        parser = null;
    }


    public void testNumericOid() throws Exception
    {
        SchemaParserTestUtils.testNumericOid( parser, "SYNTAX 1.1" );
    }


    public void testNames() throws Exception
    {
        SchemaParserTestUtils.testNames( parser, "1.1", "SYNTAX 1.1" );
    }


    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "SYNTAX 1.1" );
    }


    public void testObsolete() throws ParseException
    {
        SchemaParserTestUtils.testObsolete( parser, "1.1", "SYNTAX 1.1" );
    }


    public void testSyntax() throws ParseException
    {

        String value = null;
        MatchingRuleDescription mrd = null;

        // simple
        value = "( 1.1 SYNTAX 0.1.2.3.4.5.6.7.8.9 )";
        mrd = parser.parseMatchingRuleDescription( value );
        assertEquals( "0.1.2.3.4.5.6.7.8.9", mrd.getSyntax() );

        // simple
        value = "( 1.1 SYNTAX 123.456.789.0 )";
        mrd = parser.parseMatchingRuleDescription( value );
        assertEquals( "123.456.789.0", mrd.getSyntax() );

        // simple with spaces
        value = "( 1.1    SYNTAX    0.1.2.3.4.5.6.7.8.9    )";
        mrd = parser.parseMatchingRuleDescription( value );
        assertEquals( "0.1.2.3.4.5.6.7.8.9", mrd.getSyntax() );

        // non-numeric not allowed
        value = "( test )";
        try
        {
            parser.parse( value );
            fail( "Exception expected, invalid SYNTAX test" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // SYNTAX is required
        value = "( 1.1 )";
        try
        {
            mrd = parser.parseMatchingRuleDescription( value );
            fail( "Exception expected, SYNTAX is required" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // OC must only appear once
        value = "( 1.1 SYNTAX 2.2 SYNTAX 3.3 )";
        try
        {
            mrd = parser.parseMatchingRuleDescription( value );
            fail( "Exception expected, SYNTAX appears twice" );
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }
        
    }


    public void testExtensions() throws ParseException
    {
        SchemaParserTestUtils.testExtensions( parser, "1.1", "SYNTAX 1.1" );
    }


    public void testFull() throws ParseException
    {
        String value = null;
        MatchingRuleDescription mrd = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton äöüß 部長' OBSOLETE SYNTAX 0.1.2.3.4.5.6.7.8.9 X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )";
        mrd = parser.parseMatchingRuleDescription( value );

        assertEquals( "1.2.3.4.5.6.7.8.9.0", mrd.getNumericOid() );
        assertEquals( 2, mrd.getNames().size() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", mrd.getNames().get( 0 ) );
        assertEquals( "test", mrd.getNames().get( 1 ) );
        assertEquals( "Descripton äöüß 部長", mrd.getDescription() );
        assertTrue( mrd.isObsolete() );
        assertEquals( "0.1.2.3.4.5.6.7.8.9", mrd.getSyntax() );
        assertEquals( 2, mrd.getExtensions().size() );
        assertNotNull( mrd.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, mrd.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", mrd.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", mrd.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( mrd.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, mrd.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", mrd.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", mrd.getExtensions().get( "X-TEST-b" ).get( 1 ) );
    }
    
    
    /**
     * Test unique elements.
     * 
     * @throws ParseException
     */
    public void testUniqueElements()
    {
        String[] testValues = new String[]
            { 
                "( 1.1 SYNTAX 1.1 NAME 'test1' NAME 'test2' )", 
                "( 1.1 SYNTAX 1.1 DESC 'test1' DESC 'test2' )",
                "( 1.1 SYNTAX 1.1 OBSOLETE OBSOLETE )", 
                "( 1.1 SYNTAX 1.1 SYNTAX 2.2 SYNTAX 3.3 )",
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
        MatchingRuleDescription mrd = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 SYNTAX 1.1 )";
        mrd = parser.parseMatchingRuleDescription( value );
        assertNotNull( mrd.getSyntax() );

        value = "( 1.2.3.4.5.6.7.8.9.0 )";
        try
        {
            parser.parseMatchingRuleDescription( value );
            fail( "Exception expected, SYNTAX is required" );
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }

    }
    
    
    ////////////////////////////////////////////////////////////////
    //         Some real-world matching rule descriptons          //
    ////////////////////////////////////////////////////////////////

    public void testRfc1() throws ParseException
    {
        String value = "( 2.5.13.5 NAME 'caseExactMatch' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )";
        MatchingRuleDescription mrd = parser.parseMatchingRuleDescription( value );

        assertEquals( "2.5.13.5", mrd.getNumericOid() );
        assertEquals( 1, mrd.getNames().size() );
        assertEquals( "caseExactMatch", mrd.getNames().get( 0 ) );
        assertEquals( "", mrd.getDescription() );
        assertFalse( mrd.isObsolete() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", mrd.getSyntax() );
        assertEquals( 0, mrd.getExtensions().size() );
    }


    public void testSun1() throws ParseException
    {
        String value = "( 2.5.13.5 NAME 'caseExactMatch' DESC 'Case Exact Matching on Directory String [defined in X.520]' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )";
        MatchingRuleDescription mrd = parser.parseMatchingRuleDescription( value );

        assertEquals( "2.5.13.5", mrd.getNumericOid() );
        assertEquals( 1, mrd.getNames().size() );
        assertEquals( "caseExactMatch", mrd.getNames().get( 0 ) );
        assertEquals( "Case Exact Matching on Directory String [defined in X.520]", mrd.getDescription() );
        assertFalse( mrd.isObsolete() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", mrd.getSyntax() );
        assertEquals( 0, mrd.getExtensions().size() );
    }


    /**
     * This is a real matching rule from Sun Directory 5.2. It has an invalid 
     * syntax, no DOTs allowed in NAME value. 
     */
    public void testSun2()
    {
        String value = "( 1.3.6.1.4.1.42.2.27.9.4.34.3.6 NAME 'caseExactSubstringMatch-2.16.840.1.113730.3.3.2.11.3' DESC 'en' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )";
        try
        {
            parser.parseMatchingRuleDescription( value );
            fail( "Exception expected, invalid NAME value 'caseExactSubstringMatch-2.16.840.1.113730.3.3.2.11.3' (contains DOTs)" );
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }
    }


    /**
     * Tests the multithreaded use of a single parser.
     */
    public void testMultiThreaded() throws Exception
    {
        String[] testValues = new String[]
            {
                "( 1.1 SYNTAX 1.1 )",
                "( 2.5.13.5 NAME 'caseExactMatch' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )",
                "( 2.5.13.5 NAME 'caseExactMatch' DESC 'Case Exact Matching on Directory String [defined in X.520]' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )",
                "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton äöüß 部長' OBSOLETE SYNTAX 0.1.2.3.4.5.6.7.8.9 X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )" };
        SchemaParserTestUtils.testMultiThreaded( parser, testValues );
    }

}
