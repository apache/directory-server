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
package org.apache.directory.shared.ldap.schema.syntax.parser;


import java.text.ParseException;

import org.apache.directory.shared.ldap.schema.parsers.MatchingRuleUseDescription;
import org.apache.directory.shared.ldap.schema.parsers.MatchingRuleUseDescriptionSchemaParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


/**
 * Tests the MatchingRuleUseDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MatchingRuleUseDescriptionSchemaParserTest
{
    /** the parser instance */
    private MatchingRuleUseDescriptionSchemaParser parser;


    @Before
    public void setUp() throws Exception
    {
        parser = new MatchingRuleUseDescriptionSchemaParser();
    }


    @After
    public void tearDown() throws Exception
    {
        parser = null;
    }


    @Test
    public void testNumericOid() throws ParseException
    {
        SchemaParserTestUtils.testNumericOid( parser, "APPLIES 1.1" );
    }


    @Test
    public void testNames() throws ParseException
    {
        SchemaParserTestUtils.testNames( parser, "1.1", "APPLIES 1.1" );
    }


    @Test
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "APPLIES 1.1" );
    }


    @Test
    public void testObsolete() throws ParseException
    {
        SchemaParserTestUtils.testObsolete( parser, "1.1", "APPLIES 1.1" );
    }


    @Test
    public void testApplies() throws ParseException
    {

        String value = null;
        MatchingRuleUseDescription mrud = null;

        // APPLIES simple numericoid
        value = "( 1.1 APPLIES 1.2.3.4.5.6.7.8.9.0 )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 1, mrud.getApplicableAttributes().size() );
        assertEquals( "1.2.3.4.5.6.7.8.9.0", mrud.getApplicableAttributes().get( 0 ) );

        // SUP simple descr
        value = "( 1.1 APPLIES abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 1, mrud.getApplicableAttributes().size() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", mrud
            .getApplicableAttributes().get( 0 ) );

        // APPLIES single numericoid
        value = "( 1.1 APPLIES ( 123.4567.890 ) )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 1, mrud.getApplicableAttributes().size() );
        assertEquals( "123.4567.890", mrud.getApplicableAttributes().get( 0 ) );

        // APPLIES single descr
        value = "(1.1 APPLIES(a-z-A-Z-0-9))";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 1, mrud.getApplicableAttributes().size() );
        assertEquals( "a-z-A-Z-0-9", mrud.getApplicableAttributes().get( 0 ) );

        // APPLIES multi numericoid
        value = "( 1.1 APPLIES ( 1.2.3 $ 4.5.6 $ 7.8.90 ) )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 3, mrud.getApplicableAttributes().size() );
        assertEquals( "1.2.3", mrud.getApplicableAttributes().get( 0 ) );
        assertEquals( "4.5.6", mrud.getApplicableAttributes().get( 1 ) );
        assertEquals( "7.8.90", mrud.getApplicableAttributes().get( 2 ) );

        // APPLIES multi descr
        value = "( 1.1 APPLIES ( test1 $ test2 ) )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 2, mrud.getApplicableAttributes().size() );
        assertEquals( "test1", mrud.getApplicableAttributes().get( 0 ) );
        assertEquals( "test2", mrud.getApplicableAttributes().get( 1 ) );

        // APPLIES multi mixed, tabs
        value = "\t(\t1.1\tAPPLIES\t(\ttest1\t$\t1.2.3.4\t$\ttest2\t)\t)\t";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 3, mrud.getApplicableAttributes().size() );
        assertEquals( "test1", mrud.getApplicableAttributes().get( 0 ) );
        assertEquals( "1.2.3.4", mrud.getApplicableAttributes().get( 1 ) );
        assertEquals( "test2", mrud.getApplicableAttributes().get( 2 ) );

        // APPLIES multi mixed no space
        value = "(1.1 APPLIES(TEST-1$1.2.3.4$TEST-2))";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 3, mrud.getApplicableAttributes().size() );
        assertEquals( "TEST-1", mrud.getApplicableAttributes().get( 0 ) );
        assertEquals( "1.2.3.4", mrud.getApplicableAttributes().get( 1 ) );
        assertEquals( "TEST-2", mrud.getApplicableAttributes().get( 2 ) );

        // APPLIES multi mixed many spaces
        value = "(          1.1          APPLIES          (          test1          $          1.2.3.4$test2          )          )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 3, mrud.getApplicableAttributes().size() );
        assertEquals( "test1", mrud.getApplicableAttributes().get( 0 ) );
        assertEquals( "1.2.3.4", mrud.getApplicableAttributes().get( 1 ) );
        assertEquals( "test2", mrud.getApplicableAttributes().get( 2 ) );

        // quoted value
        value = "( 1.1 APPLIES 'test' )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 1, mrud.getApplicableAttributes().size() );
        assertEquals( "test", mrud.getApplicableAttributes().get( 0 ) );

        // quoted value
        value = "( 1.1 APPLIES '1.2.3.4' )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 1, mrud.getApplicableAttributes().size() );
        assertEquals( "1.2.3.4", mrud.getApplicableAttributes().get( 0 ) );

        // no $ separator
        value = "( 1.1 APPLIES ( test1 test2 ) )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 2, mrud.getApplicableAttributes().size() );
        assertEquals( "test1", mrud.getApplicableAttributes().get( 0 ) );
        assertEquals( "test2", mrud.getApplicableAttributes().get( 1 ) );

        // invalid character
        value = "( 1.1 APPLIES 1.2.3.4.A )";
        try
        {
            mrud = parser.parseMatchingRuleUseDescription( value );
            fail( "Exception expected, invalid APPLIES '1.2.3.4.A' (invalid character)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // empty APPLIES
        value = "( 1.1 APPLIES )";
        try
        {
            mrud = parser.parseMatchingRuleUseDescription( value );
            fail( "Exception expected, no APPLIES value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // APPLIES must only appear once
        value = "( 1.1 APPLIES test1 APPLIES test2 )";
        try
        {
            mrud = parser.parseMatchingRuleUseDescription( value );
            fail( "Exception expected, APPLIES appears twice" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        if ( !parser.isQuirksMode() )
        {
            // APPLIES is required
            value = "( 1.1 )";
            try
            {
                mrud = parser.parseMatchingRuleUseDescription( value );
                fail( "Exception expected, APPLIES is required" );
            }
            catch ( ParseException pe )
            {
                // expected
            }

            // invalid start
            value = "( 1.1 APPLIES ( test1 $ -test2 ) )";
            try
            {
                mrud = parser.parseMatchingRuleUseDescription( value );
                fail( "Exception expected, invalid APPLIES '-test' (starts with hypen)" );
            }
            catch ( ParseException pe )
            {
                // expected
            }
        }
    }


    @Test
    public void testExtensions() throws ParseException
    {
        SchemaParserTestUtils.testExtensions( parser, "1.1", "APPLIES 1.1" );
    }


    @Test
    public void testFull() throws ParseException
    {
        String value = null;
        MatchingRuleUseDescription mrud = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577' OBSOLETE APPLIES ( 0.1.2.3.4.5.6.7.8.9 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )";
        mrud = parser.parseMatchingRuleUseDescription( value );

        assertEquals( "1.2.3.4.5.6.7.8.9.0", mrud.getNumericOid() );
        assertEquals( 2, mrud.getNames().size() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", mrud.getNames().get( 0 ) );
        assertEquals( "test", mrud.getNames().get( 1 ) );
        assertEquals( "Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577", mrud.getDescription() );
        assertTrue( mrud.isObsolete() );
        assertEquals( 2, mrud.getApplicableAttributes().size() );
        assertEquals( "0.1.2.3.4.5.6.7.8.9", mrud.getApplicableAttributes().get( 0 ) );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", mrud
            .getApplicableAttributes().get( 1 ) );
        assertEquals( 2, mrud.getExtensions().size() );
        assertNotNull( mrud.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, mrud.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", mrud.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", mrud.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( mrud.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, mrud.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", mrud.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", mrud.getExtensions().get( "X-TEST-b" ).get( 1 ) );
    }


    /**
     * Test unique elements.
     * 
     * @throws ParseException
     */
    @Test
    public void testUniqueElements()
    {
        String[] testValues = new String[]
            { "( 1.1 APPLIES 1.1 NAME 'test1' NAME 'test2' )", "( 1.1 APPLIES 1.1 DESC 'test1' DESC 'test2' )",
                "( 1.1 APPLIES 1.1 OBSOLETE OBSOLETE )", "( 1.1 APPLIES 1.1 APPLIES test1 APPLIES test2 )",
                "( 1.1 APPLIES 1.1 X-TEST 'test1' X-TEST 'test2' )" };
        SchemaParserTestUtils.testUnique( parser, testValues );
    }


    /**
     * Test required elements.
     * 
     * @throws ParseException
     */
    @Test
    public void testRequiredElements() throws ParseException
    {
        String value = null;
        MatchingRuleUseDescription mrud = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 APPLIES a )";
        mrud = parser.parseMatchingRuleUseDescription( value );
        assertEquals( 1, mrud.getApplicableAttributes().size() );

        if ( !parser.isQuirksMode() )
        {
            value = "( 1.2.3.4.5.6.7.8.9.0 )";
            try
            {
                parser.parseMatchingRuleUseDescription( value );
                fail( "Exception expected, APPLIES is required" );
            }
            catch ( ParseException pe )
            {
                // expected
            }
        }
    }


    ////////////////////////////////////////////////////////////////
    //       Some real-world matching rule use descriptons        //
    ////////////////////////////////////////////////////////////////

    @Test
    public void testOpenldap1() throws ParseException
    {
        String value = "( 2.5.13.17 NAME 'octetStringMatch' APPLIES ( javaSerializedData $ userPassword ) )";
        MatchingRuleUseDescription mrud = parser.parseMatchingRuleUseDescription( value );

        assertEquals( "2.5.13.17", mrud.getNumericOid() );
        assertEquals( 1, mrud.getNames().size() );
        assertEquals( "octetStringMatch", mrud.getNames().get( 0 ) );
        assertEquals( "", mrud.getDescription() );
        assertFalse( mrud.isObsolete() );
        assertEquals( 2, mrud.getApplicableAttributes().size() );
        assertEquals( "javaSerializedData", mrud.getApplicableAttributes().get( 0 ) );
        assertEquals( "userPassword", mrud.getApplicableAttributes().get( 1 ) );
        assertEquals( 0, mrud.getExtensions().size() );
    }


    /**
     * Tests the multithreaded use of a single parser.
     */
    @Test
    public void testMultiThreaded() throws ParseException
    {
        String[] testValues = new String[]
            {
                "( 1.1 APPLIES 1.1 )",
                "( 2.5.13.17 NAME 'octetStringMatch' APPLIES ( javaSerializedData $ userPassword ) )",
                "( 2.5.13.1 NAME 'distinguishedNameMatch' APPLIES ( memberOf $ dITRedirect $ associatedName $ secretary $ documentAuthor $ manager $ seeAlso $ roleOccupant $ owner $ member $ distinguishedName $ aliasedObjectName $ namingContexts $ subschemaSubentry $ modifiersName $ creatorsName ) )",
                "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577' OBSOLETE APPLIES ( 0.1.2.3.4.5.6.7.8.9 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )" };
        SchemaParserTestUtils.testMultiThreaded( parser, testValues );
    }


    /**
     * Tests quirks mode.
     */
    @Test
    public void testQuirksMode() throws ParseException
    {
        SchemaParserTestUtils.testQuirksMode( parser, "APPLIES 1.1" );

        try
        {
            parser.setQuirksMode( true );

            // ensure all other test pass in quirks mode
            testNumericOid();
            testNames();
            testDescription();
            testObsolete();
            testApplies();
            testExtensions();
            testFull();
            testUniqueElements();
            testRequiredElements();
            testOpenldap1();
            testMultiThreaded();
        }
        finally
        {
            parser.setQuirksMode( false );
        }
    }

}
