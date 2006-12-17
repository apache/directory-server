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

import org.apache.directory.shared.ldap.schema.syntax.parser.DITContentRuleDescriptionSchemaParser;


/**
 * Tests the DITContentRuleDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaParserDITContentRuleDescriptionTest extends TestCase
{
    /** the parser instance */
    private DITContentRuleDescriptionSchemaParser parser;


    protected void setUp() throws Exception
    {
        parser = new DITContentRuleDescriptionSchemaParser();
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
        SchemaParserTestUtils.testNumericOid( parser, "" );
    }


    /**
     * Tests NAME and its values
     * 
     * @throws ParseException
     */
    public void testNames() throws ParseException
    {
        SchemaParserTestUtils.testNames( parser, "1.1", "" );
    }


    /**
     * Tests DESC
     * 
     * @throws ParseException
     */
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "" );
    }


    /**
     * Tests OBSOLETE
     * 
     * @throws ParseException
     */
    public void testObsolete() throws ParseException
    {
        SchemaParserTestUtils.testObsolete( parser, "1.1", "" );
    }


    /**
     * Test AUX and its values.
     * 
     * @throws ParseException
     */
    public void testAux() throws ParseException
    {
        String value = null;
        DITContentRuleDescription dcrd = null;

        // no AUX
        value = "( 1.1 )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 0, dcrd.getAuxiliaryObjectClasses().size() );

        // AUX simple numericoid
        value = "( 1.1 AUX 1.2.3 )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 1, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "1.2.3", dcrd.getAuxiliaryObjectClasses().get( 0 ) );

        // AUX simple descr
        value = "( 1.1 AUX top )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 1, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "top", dcrd.getAuxiliaryObjectClasses().get( 0 ) );

        // AUX single numericoid
        value = "( 1.1 AUX ( 1.2.3.4.5 ) )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 1, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "1.2.3.4.5", dcrd.getAuxiliaryObjectClasses().get( 0 ) );

        // AUX single descr
        value = "( 1.1 AUX ( A-Z-0-9 ) )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 1, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "A-Z-0-9", dcrd.getAuxiliaryObjectClasses().get( 0 ) );

        // AUX multi numericoid
        value = "( 1.1 AUX ( 1.2.3 $ 1.2.3.4.5 ) )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 2, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "1.2.3", dcrd.getAuxiliaryObjectClasses().get( 0 ) );
        assertEquals( "1.2.3.4.5", dcrd.getAuxiliaryObjectClasses().get( 1 ) );

        // AUX multi descr
        value = "( 1.1 AUX ( top1 $ top2 ) )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 2, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "top1", dcrd.getAuxiliaryObjectClasses().get( 0 ) );
        assertEquals( "top2", dcrd.getAuxiliaryObjectClasses().get( 1 ) );

        // AUX multi mixed
        value = "( 1.1 AUX ( top1 $ 1.2.3.4 $ top2 ) )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 3, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "top1", dcrd.getAuxiliaryObjectClasses().get( 0 ) );
        assertEquals( "1.2.3.4", dcrd.getAuxiliaryObjectClasses().get( 1 ) );
        assertEquals( "top2", dcrd.getAuxiliaryObjectClasses().get( 2 ) );

        // AUX multi mixed no space
        value = "( 1.1 AUX (TOP-1$1.2.3.4$TOP-2) )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 3, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "TOP-1", dcrd.getAuxiliaryObjectClasses().get( 0 ) );
        assertEquals( "1.2.3.4", dcrd.getAuxiliaryObjectClasses().get( 1 ) );
        assertEquals( "TOP-2", dcrd.getAuxiliaryObjectClasses().get( 2 ) );

        // AUX multi mixed many spaces
        value = "(          1.1          AUX          (          top1          $          1.2.3.4$top2          )          )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 3, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "top1", dcrd.getAuxiliaryObjectClasses().get( 0 ) );
        assertEquals( "1.2.3.4", dcrd.getAuxiliaryObjectClasses().get( 1 ) );
        assertEquals( "top2", dcrd.getAuxiliaryObjectClasses().get( 2 ) );

        // no quote allowed
        value = "( 1.1 AUX 'top' )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, invalid AUX 'top' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no quote allowed
        value = "( 1.1 AUX '1.2.3.4' )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, invalid AUX '1.2.3.4' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // invalid character
        value = "( 1.1 AUX 1.2.3.4.A )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, invalid AUX '1.2.3.4.A' (invalid character)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // invalid start
        value = "( 1.1 AUX ( top1 $ -top2 ) )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, invalid AUX '-top' (starts with hypen)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // invalid separator
        value = "( 1.1 AUX ( top1 top2 ) )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, invalid separator (no DOLLAR)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // empty AUX
        value = "( 1.1 AUX )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, no AUX value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Test MUST and its values.
     * Very similar to AUX, so here are less test cases. 
     * 
     * @throws ParseException
     */
    public void testMust() throws ParseException
    {
        String value = null;
        DITContentRuleDescription dcrd = null;
        
        // no MUST
        value = "( 1.1 )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 0, dcrd.getMustAttributeTypes().size() );

        // MUST simple numericoid
        value = "( 1.1 MUST 1.2.3 )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 1, dcrd.getMustAttributeTypes().size() );
        assertEquals( "1.2.3", dcrd.getMustAttributeTypes().get( 0 ) );

        // MUST mulitple
        value = "(1.1 MUST (cn$sn       $11.22.33.44.55         $  objectClass   ))";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 4, dcrd.getMustAttributeTypes().size() );
        assertEquals( "cn", dcrd.getMustAttributeTypes().get( 0 ) );
        assertEquals( "sn", dcrd.getMustAttributeTypes().get( 1 ) );
        assertEquals( "11.22.33.44.55", dcrd.getMustAttributeTypes().get( 2 ) );
        assertEquals( "objectClass", dcrd.getMustAttributeTypes().get( 3 ) );

        // invalid value
        value = "( 1.1 MUST ( c_n ) )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, invalid value c_n" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no MUST values
        value = "( 1.1 MUST )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, no MUST value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Test MAY and its values.
     * Very similar to AUX, so here are less test cases. 
     * 
     * @throws ParseException
     */
    public void testMay() throws ParseException
    {
        String value = null;
        DITContentRuleDescription dcrd = null;

        // no MAY
        value = "( 1.1 )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 0, dcrd.getMayAttributeTypes().size() );

        // MAY simple numericoid
        value = "( 1.1 MAY 1.2.3 )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 1, dcrd.getMayAttributeTypes().size() );
        assertEquals( "1.2.3", dcrd.getMayAttributeTypes().get( 0 ) );

        // MAY mulitple
        value = "(1.1 MAY (cn$sn       $11.22.33.44.55         $  objectClass   ))";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 4, dcrd.getMayAttributeTypes().size() );
        assertEquals( "cn", dcrd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "sn", dcrd.getMayAttributeTypes().get( 1 ) );
        assertEquals( "11.22.33.44.55", dcrd.getMayAttributeTypes().get( 2 ) );
        assertEquals( "objectClass", dcrd.getMayAttributeTypes().get( 3 ) );

        // invalid value
        value = "( 1.1 MAY ( c_n ) )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, invalid value c_n" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }

    /**
     * Test NOT and its values.
     * Very similar to AUX, so here are less test cases. 
     * 
     * @throws ParseException
     */
    public void testNot() throws ParseException
    {
        String value = null;
        DITContentRuleDescription dcrd = null;

        // no MAY
        value = "( 1.1 )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 0, dcrd.getNotAttributeTypes().size() );

        // MAY simple numericoid
        value = "( 1.1 NOT 1.2.3 )";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 1, dcrd.getNotAttributeTypes().size() );
        assertEquals( "1.2.3", dcrd.getNotAttributeTypes().get( 0 ) );

        // MAY mulitple
        value = "(1.1 NOT (cn$sn       $11.22.33.44.55         $  objectClass   ))";
        dcrd = parser.parseDITContentRuleDescription( value );
        assertEquals( 4, dcrd.getNotAttributeTypes().size() );
        assertEquals( "cn", dcrd.getNotAttributeTypes().get( 0 ) );
        assertEquals( "sn", dcrd.getNotAttributeTypes().get( 1 ) );
        assertEquals( "11.22.33.44.55", dcrd.getNotAttributeTypes().get( 2 ) );
        assertEquals( "objectClass", dcrd.getNotAttributeTypes().get( 3 ) );

        // invalid value
        value = "( 1.1 NOT ( c_n ) )";
        try
        {
            dcrd = parser.parseDITContentRuleDescription( value );
            fail( "Exception expected, invalid value c_n" );
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
        SchemaParserTestUtils.testExtensions( parser, "1.1", "" );

    }


    /**
     * Test full object class description.
     * 
     * @throws ParseException
     */
    public void testFull() throws ParseException
    {
        String value = null;
        DITContentRuleDescription dcrd = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton äöüß 部長' OBSOLETE AUX ( 2.3.4.5.6.7.8.9.0.1 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) MUST ( 3.4.5.6.7.8.9.0.1.2 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) MAY ( 4.5.6.7.8.9.0.1.2.3 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) NOT ( 5.6.7.8.9.0.1.2.3.4 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )";
        dcrd = parser.parseDITContentRuleDescription( value );

        assertEquals( "1.2.3.4.5.6.7.8.9.0", dcrd.getNumericOid() );
        assertEquals( 2, dcrd.getNames().size() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", dcrd.getNames().get( 0 ) );
        assertEquals( "test", dcrd.getNames().get( 1 ) );
        assertEquals( "Descripton äöüß 部長", dcrd.getDescription() );
        assertTrue( dcrd.isObsolete() );
        assertEquals( 2, dcrd.getAuxiliaryObjectClasses().size() );
        assertEquals( "2.3.4.5.6.7.8.9.0.1", dcrd.getAuxiliaryObjectClasses().get( 0 ) );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", dcrd
            .getAuxiliaryObjectClasses().get( 1 ) );
        assertEquals( 2, dcrd.getMustAttributeTypes().size() );
        assertEquals( "3.4.5.6.7.8.9.0.1.2", dcrd.getMustAttributeTypes().get( 0 ) );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", dcrd.getMustAttributeTypes()
            .get( 1 ) );
        assertEquals( 2, dcrd.getMayAttributeTypes().size() );
        assertEquals( "4.5.6.7.8.9.0.1.2.3", dcrd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", dcrd.getMayAttributeTypes()
            .get( 1 ) );
        assertEquals( 2, dcrd.getNotAttributeTypes().size() );
        assertEquals( "5.6.7.8.9.0.1.2.3.4", dcrd.getNotAttributeTypes().get( 0 ) );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", dcrd.getNotAttributeTypes()
            .get( 1 ) );
        assertEquals( 2, dcrd.getExtensions().size() );
        assertNotNull( dcrd.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, dcrd.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", dcrd.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", dcrd.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( dcrd.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, dcrd.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", dcrd.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", dcrd.getExtensions().get( "X-TEST-b" ).get( 1 ) );
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
                "( 1.1 NAME 'test1' NAME 'test2' )", 
                "( 1.1 DESC 'test1' DESC 'test2' )",
                "( 1.1 OBSOLETE OBSOLETE )", 
                "( 1.1 AUX test1 AUX test2 )",
                "( 1.1 MUST test1 MUST test2 )",
                "( 1.1 MAY test1 MAY test2 )",
                "( 1.1 NOT test1 NOT test2 )",
                "( 1.1 X-TEST 'test1' X-TEST 'test2' )" 
            };
        SchemaParserTestUtils.testUnique( parser, testValues );
    }
    
    
    /**
     * Tests the multithreaded use of a single parser.
     */
    public void testMultiThreaded() throws Exception
    {
        String[] testValues = new String[]
            {
                "( 1.1 )",
                "( 2.5.6.4 DESC 'content rule for organization' NOT ( x121Address $ telexNumber ) )",
                "( 2.5.6.4 DESC 'content rule for organization' NOT ( x121Address $ telexNumber ) )",
                "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton äöüß 部長' OBSOLETE AUX ( 2.3.4.5.6.7.8.9.0.1 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) MUST ( 3.4.5.6.7.8.9.0.1.2 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) MAY ( 4.5.6.7.8.9.0.1.2.3 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) NOT ( 5.6.7.8.9.0.1.2.3.4 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )" };
        SchemaParserTestUtils.testMultiThreaded( parser, testValues );

    }

}
