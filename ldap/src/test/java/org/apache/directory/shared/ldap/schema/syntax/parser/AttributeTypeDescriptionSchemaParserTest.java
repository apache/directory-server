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

import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.parser.ConsoleParserMonitor;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.parsers.AttributeTypeDescriptionSchemaParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * Tests the AttributeTypeDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AttributeTypeDescriptionSchemaParserTest
{
    /** the parser instance */
    private AttributeTypeDescriptionSchemaParser parser;


    @Before
    public void setUp() throws Exception
    {
        parser = new AttributeTypeDescriptionSchemaParser();
        parser.setParserMonitor( new ConsoleParserMonitor() );
    }


    @After
    public void tearDown() throws Exception
    {
        parser = null;
    }


    /**
     * Test numericoid
     * 
     * @throws ParseException
     */
    @Test
    public void testNumericOid() throws ParseException
    {
        SchemaParserTestUtils.testNumericOid( parser, "SYNTAX 1.1" );
    }


    /**
     * Tests NAME and its values
     * 
     * @throws ParseException
     */
    @Test
    public void testNames() throws ParseException
    {
        SchemaParserTestUtils.testNames( parser, "1.1", "SYNTAX 1.1" );
    }


    /**
     * Tests DESC
     * 
     * @throws ParseException
     */
    @Test
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "SYNTAX 1.1" );
    }


    /**
     * Tests OBSOLETE
     * 
     * @throws ParseException
     */
    @Test
    public void testObsolete() throws ParseException
    {
        SchemaParserTestUtils.testObsolete( parser, "1.1", "SYNTAX 1.1" );
    }


    /**
     * Test SUP and its value.
     * 
     * @throws ParseException
     */
    @Test
    public void testSuperType() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // no SUP
        value = "( 1.1 SYNTAX 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertNull( attributeType.getSuperiorOid() );

        // SUP numericoid
        value = "( 1.1 SYNTAX 1.1 SUP 1.2.3.4.5.6.7.8.9.0 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4.5.6.7.8.9.0", attributeType.getSuperiorOid() );

        // SUP descr, no space
        value = "(1.1 SYNTAX1.1 SUPabcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789)";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType.getSuperiorOid() );

        // SUP descr, newline
        value = "\t(\t1.1\tSYNTAX\t1.1\tSUP\tabcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789\t)\t";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType.getSuperiorOid() );

        // quoted SUP value
        value = "( 1.1 SYNTAX 1.1 SUP 'name' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "name", attributeType.getSuperiorOid() );

        // quoted SUP value
        value = "( 1.1 SYNTAX 1.1 SUP '1.2.3.4' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4", attributeType.getSuperiorOid() );

        // quoted SUP value
        value = "( 1.1 SYNTAX 1.1 SUP ('1.2.3.4') )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4", attributeType.getSuperiorOid() );

        // invalid character
        value = "( 1.1 SYNTAX 1.1 SUP 1.2.3.4.A )";
        try
        {
            attributeType = parser.parseAttributeTypeDescription( value );
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
            attributeType = parser.parseAttributeTypeDescription( value );
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
            attributeType = parser.parseAttributeTypeDescription( value );
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
    @Test
    public void testEquality() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // no EQUALITY
        value = "( 1.1 SYNTAX 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertNull( attributeType.getEqualityOid() );

        // EQUALITY numericoid
        value = "( 1.1 SYNTAX 1.1 EQUALITY 1.2.3.4567.8.9.0 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", attributeType.getEqualityOid() );

        // EQUALITY descr, no space
        value = "(1.1 SYNTAX1.1 EQUALITYabcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789)";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType.getEqualityOid() );

        // EQUALITY descr, newline
        value = "\n(\n1.1\nSYNTAX\n1.1\nEQUALITY\nabcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789\n)\n";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType.getEqualityOid() );

        // quoted value
        value = "( 1.1 SYNTAX 1.1 EQUALITY 'caseExcactMatch' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "caseExcactMatch", attributeType.getEqualityOid() );

        // quote value in parentheses 
        value = "( 1.1 SYNTAX 1.1 EQUALITY ('caseExcactMatch') )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "caseExcactMatch", attributeType.getEqualityOid() );
    }


    /**
     * Tests ORDERING and its values.
     * Very similar to SUP, so here are less test cases. 
     * 
     * @throws ParseException
     */
    @Test
    public void testOrdering() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // no ORDERING
        value = "( 1.1 SYNTAX 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertNull( attributeType.getOrderingOid() );

        // ORDERING numericoid
        value = "( 1.1 SYNTAX 1.1 ORDERING 1.2.3.4567.8.9.0 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", attributeType.getOrderingOid() );

        // ORDERING descr, no space
        value = "(1.1 SYNTAX1.1 ORDERINGabcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789)";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType.getOrderingOid() );

        // ORDERING descr, newline
        value = "\r(\r1.1\rSYNTAX\r1.1\rORDERING\rabcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789\r)\r";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType.getOrderingOid() );

        // quoted value
        value = "( 1.1 SYNTAX 1.1 ORDERING 'generalizedTimeOrderingMatch' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "generalizedTimeOrderingMatch", attributeType.getOrderingOid() );

        // quote value in parentheses
        value = "( 1.1 SYNTAX 1.1 ORDERING ('generalizedTimeOrderingMatch') )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "generalizedTimeOrderingMatch", attributeType.getOrderingOid() );
    }


    /**
     * Tests SUBSTRING and its values.
     * Very similar to SUP, so here are less test cases. 
     * 
     * @throws ParseException
     */
    @Test
    public void testSubstring() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // no SUBSTR
        value = "( 1.1 SYNTAX 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertNull( attributeType.getSubstringOid() );

        // SUBSTR numericoid
        value = "( 1.1 SYNTAX 1.1 SUBSTR 1.2.3.4567.8.9.0 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", attributeType.getSubstringOid() );

        // SUBSTR descr, no space
        value = "(1.1 SYNTAX1.1 SUBSTRabcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789)";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType
            .getSubstringOid() );

        // SUBSTR descr, newline
        value = "\r\n(\r\n1.1\r\nSYNTAX\r\n1.1\r\nSUBSTR\r\nabcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789\r\n)\r\n";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType
            .getSubstringOid() );

        // quoted value
        value = "( 1.1 SYNTAX 1.1 SUBSTR 'caseIgnoreSubstringsMatch' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "caseIgnoreSubstringsMatch", attributeType.getSubstringOid() );

        // quote value in parentheses
        value = "( 1.1 SYNTAX 1.1 SUBSTR ('caseIgnoreSubstringsMatch') )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "caseIgnoreSubstringsMatch", attributeType.getSubstringOid() );
    }


    /**
     * Tests SYNTAX
     * 
     * @throws ParseException
     */
    @Test
    public void testSyntax() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // no SYNTAX
        value = "( 1.1 SUP 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertNull( attributeType.getSyntaxOid() );
        assertEquals( 0, attributeType.getSyntaxLength() );

        // SYNTAX string
        value = "( 1.1 SYNTAX IA5String )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "IA5String", attributeType.getSyntaxOid() );
        assertEquals( 0, attributeType.getSyntaxLength() );

        // SYNTAX numericoid
        value = "( 1.1 SYNTAX 1.2.3.4567.8.9.0 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", attributeType.getSyntaxOid() );
        assertEquals( 0, attributeType.getSyntaxLength() );
        
        // quoted numericoid
        value = "( 1.1 SYNTAX '1.2.3.4567.8.9.0' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", attributeType.getSyntaxOid() );
        assertEquals( 0, attributeType.getSyntaxLength() );

        // quoted numericoid
        value = "( 1.1 SYNTAX ('1.2.3.4567.8.9.0') )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", attributeType.getSyntaxOid() );
        assertEquals( 0, attributeType.getSyntaxLength() );

        // SYNTAX numericoid and length, no spaces
        value = "(1.1 SYNTAX1.2.3.4567.8.9.0{1234567890})";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", attributeType.getSyntaxOid() );
        assertEquals( 1234567890, attributeType.getSyntaxLength() );

        // SYNTAX, with tabs
        value = "\t(\t1.1\tSYNTAX\t1.2.3.4567.8.9.0\t{1234567890}\t)\t";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4567.8.9.0", attributeType.getSyntaxOid() );
        assertEquals( 1234567890, attributeType.getSyntaxLength() );

        // SYNTAX numericoid and zero length
        value = "( 1.1 SYNTAX 1.2.3 {0} )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", attributeType.getSyntaxOid() );
        assertEquals( 0, attributeType.getSyntaxLength() );

        // quoted value
        value = "( 1.1 SYNTAX '1.2.3{32}' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", attributeType.getSyntaxOid() );
        assertEquals( 32, attributeType.getSyntaxLength() );

        // quote value in parentheses
        value = "( 1.1 SYNTAX ( '1.2.3{32}' ) )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", attributeType.getSyntaxOid() );
        assertEquals( 32, attributeType.getSyntaxLength() );

        // empty length
        value = "( 1.1 SYNTAX 1.2.3.4{} )";
        try
        {
            attributeType = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SYNTAX 1.2.3.4{} (empty length)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // leading zero in length
        value = "( 1.1 SYNTAX 1.2.3.4{01} )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3.4", attributeType.getSyntaxOid() );
        assertEquals( 1, attributeType.getSyntaxLength() );

        // invalid syntax length
        value = "( 1.1 SYNTAX 1.2.3.4{X} )";
        try
        {
            attributeType = parser.parseAttributeTypeDescription( value );
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
            attributeType = parser.parseAttributeTypeDescription( value );
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
            attributeType = parser.parseAttributeTypeDescription( value );
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
    @Test
    public void testSingleValue() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // not single-value
        value = "( 1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertFalse( attributeType.isSingleValued() );

        // single-value
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' SINGLE-VALUE)";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertTrue( attributeType.isSingleValued() );

        // single-value 
        value = "(1.1 SYNTAX 1.1 SINGLE-VALUE)";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertTrue( attributeType.isSingleValued() );

        // invalid
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' SINGLE-VALU )";
        try
        {
            attributeType = parser.parseAttributeTypeDescription( value );
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
    @Test
    public void testCollective() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // not collective
        value = "( 1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertFalse( attributeType.isCollective() );

        // single-value
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' COLLECTIVE )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertTrue( attributeType.isCollective() );

        // single-value 
        value = "(1.1 SYNTAX 1.1 COLLECTIVE)";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertTrue( attributeType.isCollective() );

        // ivalid
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' COLLECTIV )";
        try
        {
            attributeType = parser.parseAttributeTypeDescription( value );
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
    @Test
    public void testNoUserModification() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // not NO-USER-MODIFICATION
        value = "( 1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertTrue( attributeType.isUserModifiable() );

        // NO-USER-MODIFICATION
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' NO-USER-MODIFICATION USAGE directoryOperation )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertFalse( attributeType.isUserModifiable() );

        // NO-USER-MODIFICATION 
        value = "(1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE directoryOperation )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertFalse( attributeType.isUserModifiable() );

        // ivalid
        value = "(1.1 SYNTAX 1.1 NAME 'test' DESC 'Descripton' NO-USER-MODIFICATIO USAGE directoryOperation )";
        try
        {
            attributeType = parser.parseAttributeTypeDescription( value );
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
    @Test
    public void testUsage() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        // DEFAULT is userApplications
        value = "( 1.1 SYNTAX 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.USER_APPLICATIONS, attributeType.getUsage() );

        // userApplications
        value = "( 1.1 SYNTAX 1.1 USAGE userApplications )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.USER_APPLICATIONS, attributeType.getUsage() );

        // directoryOperation
        value = "( 1.1 SYNTAX 1.1 USAGE directoryOperation )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DIRECTORY_OPERATION, attributeType.getUsage() );

        // distributedOperation, tabs
        value = "\t(\t1.1\tSYNTAX\t1.1\tUSAGE\tdistributedOperation\t)\t";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DISTRIBUTED_OPERATION, attributeType.getUsage() );

        // dSAOperation, no space
        value = "(1.1 SYNTAX1.1 USAGEdSAOperation)";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DSA_OPERATION, attributeType.getUsage() );

        // directoryOperation, case insensitivity
        value = "( 1.1 SYNTAX 1.1 USAGE DiReCtOrYoPeRaTiOn )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DIRECTORY_OPERATION, attributeType.getUsage() );

        // ivalid
        value = "( 1.1 SYNTAX 1.1 USAGE abc )";
        try
        {
            attributeType = parser.parseAttributeTypeDescription( value );
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
    @Test
    public void testExtensions() throws ParseException
    {
        SchemaParserTestUtils.testExtensions( parser, "1.1", "SYNTAX 1.1" );
    }


    /**
     * Test full attribute type description.
     * 
     * @throws ParseException
     */
    @Test
    public void testFull() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577' OBSOLETE SUP abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 EQUALITY 2.3.4.5.6.7.8.9.0.1 ORDERING 3.4.5.6.7.8.9.0.1.2 SUBSTR 4.5.6.7.8.9.0.1.2.3 SYNTAX 5.6.7.8.9.0.1.2.3.4{1234567890} SINGLE-VALUE NO-USER-MODIFICATION USAGE dSAOperation X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )";
        attributeType = parser.parseAttributeTypeDescription( value );

        assertEquals( "1.2.3.4.5.6.7.8.9.0", attributeType.getOid() );
        assertEquals( 2, attributeType.getNames().size() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType.getNames().get( 0 ) );
        assertEquals( "test", attributeType.getNames().get( 1 ) );
        assertEquals( "Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577", attributeType.getDescription() );
        assertTrue( attributeType.isObsolete() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", attributeType.getSuperiorOid() );
        assertEquals( "2.3.4.5.6.7.8.9.0.1", attributeType.getEqualityOid() );
        assertEquals( "3.4.5.6.7.8.9.0.1.2", attributeType.getOrderingOid() );
        assertEquals( "4.5.6.7.8.9.0.1.2.3", attributeType.getSubstringOid() );
        assertEquals( "5.6.7.8.9.0.1.2.3.4", attributeType.getSyntaxOid() );
        assertEquals( 1234567890, attributeType.getSyntaxLength() );

        assertTrue( attributeType.isSingleValued() );
        assertFalse( attributeType.isCollective() );
        assertFalse( attributeType.isUserModifiable() );
        assertEquals( UsageEnum.DSA_OPERATION, attributeType.getUsage() );

        assertEquals( 2, attributeType.getExtensions().size() );
        assertNotNull( attributeType.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, attributeType.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", attributeType.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", attributeType.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( attributeType.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, attributeType.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", attributeType.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", attributeType.getExtensions().get( "X-TEST-b" ).get( 1 ) );
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
            { "( 1.1 SYNTAX 1.1 NAME 'test1' NAME 'test2' )", "( 1.1 SYNTAX 1.1 DESC 'test1' DESC 'test2' )",
                "( 1.1 SYNTAX 1.1 OBSOLETE OBSOLETE )", "( 1.1 SYNTAX 1.1 SUP test1 SUP test2 )",
                "( 1.1 SYNTAX 1.1 EQUALITY test1 EQUALITY test2 )", "( 1.1 SYNTAX 1.1 ORDERING test1 ORDERING test2 )",
                "( 1.1 SYNTAX 1.1 SUBSTR test1 SUBSTR test2 )", "( 1.1 SYNTAX 1.1 SYNTAX 2.2 SYNTAX 3.3 )",
                "( 1.1 SYNTAX 1.1 SINGLE-VALUE SINGLE-VALUE )", "( 1.1 SYNTAX 1.1 COLLECTIVE COLLECTIVE )",
                "( 1.1 SYNTAX 1.1 USAGE directoryOperation NO-USER-MODIFICATION NO-USER-MODIFICATION )",
                "( 1.1 SYNTAX 1.1 USAGE directoryOperation USAGE userApplications )",
                "( 1.1 SYNTAX 1.1 X-TEST 'test1' X-TEST 'test2' )" };
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
        AttributeType attributeType = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 SYNTAX 1.1 SUP 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertNotNull( attributeType.getSyntaxOid() );
        assertNotNull( attributeType.getSuperiorOid() );

        value = "( 1.2.3.4.5.6.7.8.9.0 SYNTAX 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertNotNull( attributeType.getSyntaxOid() );
        assertNull( attributeType.getSuperiorOid() );

        value = "( 1.2.3.4.5.6.7.8.9.0 SUP 1.1 )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertNull( attributeType.getSyntaxOid() );
        assertNotNull( attributeType.getSuperiorOid() );

        if ( !parser.isQuirksMode() )
        {
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
    }


    /**
     * Test collective constraint:
     * COLLECTIVE requires USAGE userApplications
     * 
     * @throws ParseException
     */
    @Test
    public void testCollecitveConstraint() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        value = "( 1.1 SYNTAX 1.1 COLLECTIVE )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertTrue( attributeType.isCollective() );
        assertEquals( UsageEnum.USER_APPLICATIONS, attributeType.getUsage() );

        value = "( 1.1 SYNTAX 1.1 COLLECTIVE USAGE userApplications )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertTrue( attributeType.isCollective() );
        assertEquals( UsageEnum.USER_APPLICATIONS, attributeType.getUsage() );

        if ( !parser.isQuirksMode() )
        {
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
    }


    /**
     * Test no-user-modification constraint:
     * NO-USER-MODIFICATION requires an operational USAGE
     * 
     * @throws ParseException
     */
    @Test
    public void testNoUserModificatonConstraint() throws ParseException
    {
        String value = null;
        AttributeType attributeType = null;

        value = "( 1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE directoryOperation )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertFalse( attributeType.isUserModifiable() );
        assertEquals( UsageEnum.DIRECTORY_OPERATION, attributeType.getUsage() );

        value = "( 1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE dSAOperation )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertFalse( attributeType.isUserModifiable() );
        assertEquals( UsageEnum.DSA_OPERATION, attributeType.getUsage() );

        value = "( 1.1 SYNTAX 1.1 NO-USER-MODIFICATION USAGE distributedOperation )";
        attributeType = parser.parseAttributeTypeDescription( value );
        assertFalse( attributeType.isUserModifiable() );
        assertEquals( UsageEnum.DISTRIBUTED_OPERATION, attributeType.getUsage() );

        if ( !parser.isQuirksMode() )
        {
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
    }


    /**
     * Ensure that element order is ignored
     * 
     * @throws ParseException
     */
    @Test
    public void testIgnoreElementOrder() throws ParseException
    {
        String value = "( 2.5.4.3 SUP name SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications DESC 'RFC2256: common name(s) for which the entity is known by'  EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch NAME ( 'cn' 'commonName' )  )";
        AttributeType attributeType = parser.parseAttributeTypeDescription( value );

        assertEquals( "2.5.4.3", attributeType.getOid() );
        assertEquals( 2, attributeType.getNames().size() );
        assertEquals( "cn", attributeType.getNames().get( 0 ) );
        assertEquals( "commonName", attributeType.getNames().get( 1 ) );
        assertEquals( "RFC2256: common name(s) for which the entity is known by", attributeType.getDescription() );
        assertEquals( "name", attributeType.getSuperiorOid() );
        assertEquals( "caseIgnoreMatch", attributeType.getEqualityOid() );
        assertEquals( "caseIgnoreSubstringsMatch", attributeType.getSubstringOid() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", attributeType.getSyntaxOid() );
        assertEquals( UsageEnum.USER_APPLICATIONS, attributeType.getUsage() );
        assertEquals( 0, attributeType.getExtensions().size() );
    }


    ////////////////////////////////////////////////////////////////
    //         Some real-world attribute type definitions         //
    ////////////////////////////////////////////////////////////////

    @Test
    public void testRfcUid() throws ParseException
    {
        String value = "( 0.9.2342.19200300.100.1.1 NAME ( 'uid' 'userid' ) DESC 'RFC1274: user identifier' EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{256} USAGE userApplications )";
        AttributeType attributeType = parser.parseAttributeTypeDescription( value );

        assertEquals( "0.9.2342.19200300.100.1.1", attributeType.getOid() );
        assertEquals( 2, attributeType.getNames().size() );
        assertEquals( "uid", attributeType.getNames().get( 0 ) );
        assertEquals( "userid", attributeType.getNames().get( 1 ) );
        assertEquals( "RFC1274: user identifier", attributeType.getDescription() );
        assertNull( attributeType.getSuperiorOid() );

        assertEquals( "caseIgnoreMatch", attributeType.getEqualityOid() );
        assertEquals( "caseIgnoreSubstringsMatch", attributeType.getSubstringOid() );
        assertNull( attributeType.getOrderingOid() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", attributeType.getSyntaxOid() );
        assertEquals( 256, attributeType.getSyntaxLength() );
        assertEquals( UsageEnum.USER_APPLICATIONS, attributeType.getUsage() );

        assertFalse( attributeType.isObsolete() );
        assertFalse( attributeType.isCollective() );
        assertFalse( attributeType.isSingleValued() );
        assertTrue( attributeType.isUserModifiable() );

        assertEquals( 0, attributeType.getExtensions().size() );
    }


    /**
     * Tests the parse of a simple AttributeType
     */
    @Test
    public void testAddAttributeType() throws ParseException
    {
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) "
            + "DESC 'bogus description' SUP name SINGLE-VALUE )";
        AttributeType desc = parser.parseAttributeTypeDescription( substrate );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", desc.getOid() );
        assertEquals( "bogus", desc.getNames().get( 0 ) );
        assertEquals( "bogusName", desc.getNames().get( 1 ) );
        assertEquals( "bogus description", desc.getDescription() );
        assertEquals( "name", desc.getSuperiorOid() );
        assertEquals( true, desc.isSingleValued() );
    }


    /**
     * Tests the parse of a simple AttributeType with the schema extension.
     */
    @Test
    public void testAttributeTypeWithSchemaExtension() throws ParseException
    {
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) "
            + "DESC 'bogus description' SUP name SINGLE-VALUE X-SCHEMA 'blah' )";
        AttributeType desc = parser.parseAttributeTypeDescription( substrate );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", desc.getOid() );
        assertEquals( "bogus", desc.getNames().get( 0 ) );
        assertEquals( "bogusName", desc.getNames().get( 1 ) );
        assertEquals( "bogus description", desc.getDescription() );
        assertEquals( "name", desc.getSuperiorOid() );
        assertEquals( true, desc.isSingleValued() );
        assertEquals( "blah", desc.getExtensions().get( "X-SCHEMA" ).get( 0 ) );
    }


    /**
     * Tests the multithreaded use of a single parser.
     */
    @Test
    public void testMultiThreaded() throws ParseException
    {
        String[] testValues = new String[]
            {
                "( 1.1 SYNTAX 1.1 )",
                "( 2.5.4.41 NAME 'name' DESC 'RFC2256: common supertype of name attributes'  EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} USAGE userApplications )",
                "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications )",
                "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577' OBSOLETE SUP abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 EQUALITY 2.3.4.5.6.7.8.9.0.1 ORDERING 3.4.5.6.7.8.9.0.1.2 SUBSTR 4.5.6.7.8.9.0.1.2.3 SYNTAX 5.6.7.8.9.0.1.2.3.4{1234567890} SINGLE-VALUE NO-USER-MODIFICATION USAGE dSAOperation X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )" };
        SchemaParserTestUtils.testMultiThreaded( parser, testValues );
    }


    /**
     * Tests quirks mode.
     */
    @Test
    public void testQuirksMode() throws ParseException
    {
        SchemaParserTestUtils.testQuirksMode( parser, "SYNTAX 1.1" );

        try
        {
            String value = null;
            AttributeType attributeType = null;

            parser.setQuirksMode( true );

            // ensure all other test pass in quirks mode
            testNumericOid();
            testNames();
            testDescription();
            testObsolete();
            testSuperType();
            testEquality();
            testOrdering();
            testSubstring();
            testSyntax();
            testSingleValue();
            testCollective();
            testNoUserModification();
            testUsage();
            testExtensions();
            testFull();
            testUniqueElements();
            testRequiredElements();
            testCollecitveConstraint();
            testNoUserModificatonConstraint();
            testIgnoreElementOrder();
            testRfcUid();
            testAddAttributeType();
            testMultiThreaded();

            // NAME with special chars
            value = "( 1.2.3 SYNTAX te_st NAME 't-e_s.t;' )";
            attributeType = parser.parseAttributeTypeDescription( value );
            assertEquals( 1, attributeType.getNames().size() );
            assertEquals( "t-e_s.t;", attributeType.getNames().get( 0 ) );

            // SYNTAX with underscore
            value = "( 1.1 SYNTAX te_st )";
            attributeType = parser.parseAttributeTypeDescription( value );
            assertEquals( "te_st", attributeType.getSyntaxOid() );

            // SUPERTYPE with underscore
            value = "( 1.1 SYNTAX 1.1 SUP te_st )";
            attributeType = parser.parseAttributeTypeDescription( value );
            assertEquals( "te_st", attributeType.getSuperiorOid() );

            // EQUALITY with underscore
            value = "( 1.1 SYNTAX 1.1 EQUALITY te_st )";
            attributeType = parser.parseAttributeTypeDescription( value );
            assertEquals( "te_st", attributeType.getEqualityOid() );

            // SUBSTR with underscore
            value = "( 1.1 SYNTAX 1.1 SUBSTR te_st )";
            attributeType = parser.parseAttributeTypeDescription( value );
            assertEquals( "te_st", attributeType.getSubstringOid() );

            // ORDERING with underscore
            value = "( 1.1 SYNTAX 1.1 ORDERING te_st )";
            attributeType = parser.parseAttributeTypeDescription( value );
            assertEquals( "te_st", attributeType.getOrderingOid() );

            // Netscape attribute 
            value = "( nsAdminGroupName-oid NAME 'nsAdminGroupName' DESC 'Netscape defined attribute type' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-ORIGIN 'Netscape' )";
            attributeType = parser.parseAttributeTypeDescription( value );
            assertEquals( "nsAdminGroupName-oid", attributeType.getOid() );
            assertEquals( 1, attributeType.getNames().size() );
            assertEquals( "nsAdminGroupName", attributeType.getNames().get( 0 ) );
        }
        finally
        {
            parser.setQuirksMode( false );
        }
    }

}
