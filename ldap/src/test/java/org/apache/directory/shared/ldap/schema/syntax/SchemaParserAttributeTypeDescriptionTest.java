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
import java.util.ArrayList;
import java.util.List;

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

    /** holds multithreaded success value */
    boolean isSuccessMultithreaded = true;


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
        String value = null;
        AttributeTypeDescription atd = null;

        // null test
        value = null;
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, null" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no oid
        value = "( )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, no NUMERICOID" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // simple
        value = "( 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.1", atd.getNumericOid() );

        // simple with spaces
        value = "(          1.1          )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.1", atd.getNumericOid() );

        // non-numeric not allowed
        value = "( cn )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NUMERICOID top" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // to short
        value = "( 1 )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NUMERICOID 1" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // dot only
        value = "( . )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NUMERICOID ." );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // ends with dot
        value = "( 1.1. )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NUMERICOID 1.1." );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // quotes not allowed
        value = "( '1.1' )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NUMERICOID '1.1' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests NAME and its values
     * 
     * @throws ParseException
     */
    public void testNames() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // alpha
        value = "( 1.1 NAME 'test' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 1, atd.getNames().size() );
        assertEquals( "test", atd.getNames().get( 0 ) );

        // alpha-num-hypen
        value = "( 1.1 NAME 'a-z-0-9' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 1, atd.getNames().size() );
        assertEquals( "a-z-0-9", atd.getNames().get( 0 ) );

        // with parentheses
        value = "( 1.1 NAME ( 'a-z-0-9' ) )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 1, atd.getNames().size() );
        assertEquals( "a-z-0-9", atd.getNames().get( 0 ) );

        // with parentheses, without space
        value = "( 1.1 NAME ('a-z-0-9') )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 1, atd.getNames().size() );
        assertEquals( "a-z-0-9", atd.getNames().get( 0 ) );

        // multi with space
        value = "( 1.1 NAME ( 'test' 'a-z-0-9' ) )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 2, atd.getNames().size() );
        assertEquals( "test", atd.getNames().get( 0 ) );
        assertEquals( "a-z-0-9", atd.getNames().get( 1 ) );

        // multi without space
        value = "( 1.1 NAME ('test' 'a-z-0-9' 'givenName') )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 3, atd.getNames().size() );
        assertEquals( "test", atd.getNames().get( 0 ) );
        assertEquals( "a-z-0-9", atd.getNames().get( 1 ) );
        assertEquals( "givenName", atd.getNames().get( 2 ) );

        // multi with many spaces
        value = "(          1.1          NAME          (          'test'          'a-z-0-9'          'givenName'          )          )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 3, atd.getNames().size() );
        assertEquals( "test", atd.getNames().get( 0 ) );
        assertEquals( "a-z-0-9", atd.getNames().get( 1 ) );
        assertEquals( "givenName", atd.getNames().get( 2 ) );

        // lowercase
        value = "( 1.1 name 'test' )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, NAME is lowercase" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // unquoted
        value = "( 1.1 NAME test )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NAME test (unquoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // start with number
        value = "( 1.1 NAME '1test' )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NAME 1test (starts with number)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // start with hypen
        value = "( 1.1 NAME '-test' )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NAME -test (starts with hypen)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // invalid character
        value = "( 1.1 NAME 'te_st' )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NAME te_st (contains invalid character)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // NAM unknown
        value = "( 1.1 NAM 'test' )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid token NAM" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // one valid, one invalid
        value = "( 1.1 NAME ( 'test' 'te_st' ) )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NAME te_st (contains invalid character)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no space between values
        value = "( 1.1 NAME ( 'test''test2' ) )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid NAME values (no space between values)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests DESC
     * 
     * @throws ParseException
     */
    public void testDescription() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // simple
        value = "(1.1 NAME 'test' DESC 'Descripton')";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "Descripton", atd.getDescription() );

        // unicode
        value = "( 1.1 NAME 'test' DESC 'Descripton äöüß 部長' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "Descripton äöüß 部長", atd.getDescription() );

        // lowercase
        value = "( 1.1 desc 'Descripton' )";
        try
        {
            parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, DESC is lowercase" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests OBSOLETE
     * 
     * @throws ParseException
     */
    public void testObsolete() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // not obsolete
        value = "( 1.1 NAME 'test' DESC 'Descripton' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isObsolete() );

        // obsolete
        value = "(1.1 NAME 'test' DESC 'Descripton' OBSOLETE)";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isObsolete() );

        // obsolete 
        value = "(1.1 OBSOLETE)";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isObsolete() );

        // ivalid
        value = "(1.1 NAME 'test' DESC 'Descripton' OBSOLET )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid OBSOLETE value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Test SUP and its values.
     * 
     * @throws ParseException
     */
    public void testSuperType() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // no SUP
        value = "( 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getSuperType() );

        // SUP numericoid
        value = "( 1.1 SUP 1.2.3 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", atd.getSuperType() );

        // SUP descr
        value = "( 1.1 SUP name )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "name", atd.getSuperType() );

        // no quote allowed
        value = "( 1.1 SUP 'name' )";
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
        value = "( 1.1 SUP '1.2.3.4' )";
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
        value = "( 1.1 SUP 1.2.3.4.A )";
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
        value = "( 1.1 SUP ( name1 $ name2 ) )";
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
        value = "( 1.1 SUP )";
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
        value = "( 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getEqualityMatchingRule() );

        // EQUALITY numericoid
        value = "( 1.1 EQUALITY 1.2.3 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", atd.getEqualityMatchingRule() );

        // EQUALITY descr
        value = "( 1.1 EQUALITY caseExcactMatch )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "caseExcactMatch", atd.getEqualityMatchingRule() );

        // no quote allowed
        value = "( 1.1 EQUALITY 'caseExcactMatch' )";
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
        value = "( 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getOrderingMatchingRule() );

        // EQUALITY numericoid
        value = "( 1.1 ORDERING 1.2.3 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", atd.getOrderingMatchingRule() );

        // EQUALITY descr
        value = "( 1.1 ORDERING generalizedTimeOrderingMatch )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "generalizedTimeOrderingMatch", atd.getOrderingMatchingRule() );

        // no quote allowed
        value = "( 1.1 ORDERING 'generalizedTimeOrderingMatch' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid ORDERING 'caseExcactMatch' (quoted)" );
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
        value = "( 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getSubstringsMatchingRule() );

        // EQUALITY numericoid
        value = "( 1.1 SUBSTR 1.2.3 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", atd.getSubstringsMatchingRule() );

        // EQUALITY descr
        value = "( 1.1 SUBSTR caseIgnoreSubstringsMatch )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "caseIgnoreSubstringsMatch", atd.getSubstringsMatchingRule() );

        // no quote allowed
        value = "( 1.1 SUBSTR 'generalizedTimeOrderingMatch' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SUBSTR 'generalizedTimeOrderingMatch' (quoted)" );
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
        value = "( 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertNull( atd.getSyntax() );
        assertEquals( 0, atd.getSyntaxLength() );

        // SYNTAX numericoid
        value = "( 1.1 SYNTAX 1.2.3 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", atd.getSyntax() );
        assertEquals( 0, atd.getSyntaxLength() );

        // SYNTAX numericoid and length
        value = "( 1.1 SYNTAX 1.2.3{32} )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( "1.2.3", atd.getSyntax() );
        assertEquals( 32, atd.getSyntaxLength() );

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

        // zero syntax
        value = "( 1.1 SYNTAX 1.2.3.4{01} )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid SYNTAX 1.2.3.4{01} (leading zero length)" );
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
            fail( "Exception expected, invalid SYNTAX 1.2.3.4{X} (zero length)" );
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
        value = "( 1.1 NAME 'test' DESC 'Descripton' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isSingleValued() );

        // single-value
        value = "(1.1 NAME 'test' DESC 'Descripton' SINGLE-VALUE)";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isSingleValued() );

        // single-value 
        value = "(1.1 SINGLE-VALUE)";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isSingleValued() );

        // ivalid
        value = "(1.1 NAME 'test' DESC 'Descripton' SINGLE-VALU )";
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
        value = "( 1.1 NAME 'test' DESC 'Descripton' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isCollective() );

        // single-value
        value = "(1.1 NAME 'test' DESC 'Descripton' COLLECTIVE )";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isCollective() );

        // single-value 
        value = "(1.1 COLLECTIVE)";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isCollective() );

        // ivalid
        value = "(1.1 NAME 'test' DESC 'Descripton' COLLECTIV )";
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
        value = "( 1.1 NAME 'test' DESC 'Descripton' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertTrue( atd.isUserModifiable() );

        // NO-USER-MODIFICATION
        value = "(1.1 NAME 'test' DESC 'Descripton' NO-USER-MODIFICATION )";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isUserModifiable() );

        // NO-USER-MODIFICATION 
        value = "(1.1 NO-USER-MODIFICATION)";
        atd = parser.parseAttributeTypeDescription( value );
        assertFalse( atd.isUserModifiable() );

        // ivalid
        value = "(1.1 NAME 'test' DESC 'Descripton' NO-USER-MODIFICATIO )";
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
     * Tests usage 
     * 
     * @throws ParseException
     */
    public void testUsage() throws ParseException
    {
        String value = null;
        AttributeTypeDescription atd = null;

        // DEFAULT is userApplications
        value = "( 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.USER_APPLICATIONS, atd.getUsage() );

        // userApplications
        value = "( 1.1 USAGE userApplications )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.USER_APPLICATIONS, atd.getUsage() );

        // directoryOperation
        value = "( 1.1 USAGE directoryOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DIRECTORY_OPERATION, atd.getUsage() );

        // AUXILIARY
        value = "( 1.1 USAGE distributedOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DISTRIBUTED_OPERATION, atd.getUsage() );

        // STRUCTURAL
        value = "( 1.1 USAGE dSAOperation )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( UsageEnum.DSA_OPERATION, atd.getUsage() );

        // TODO: case insensitive?

        // ivalid
        value = "( 1.1 USAGE abc )";
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
        String value = null;
        AttributeTypeDescription atd = null;

        // no extension
        value = "( 1.1 )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 0, atd.getExtensions().size() );

        // single extension with one value
        value = "( 1.1 X-TEST 'test' )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 1, atd.getExtensions().size() );
        assertNotNull( atd.getExtensions().get( "X-TEST" ) );
        assertEquals( 1, atd.getExtensions().get( "X-TEST" ).size() );
        assertEquals( "test", atd.getExtensions().get( "X-TEST" ).get( 0 ) );

        // single extension with multiple values
        value = "( 1.1 X-TEST-ABC ('test1' 'test äöüß'       'test 部長' ) )";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 1, atd.getExtensions().size() );
        assertNotNull( atd.getExtensions().get( "X-TEST-ABC" ) );
        assertEquals( 3, atd.getExtensions().get( "X-TEST-ABC" ).size() );
        assertEquals( "test1", atd.getExtensions().get( "X-TEST-ABC" ).get( 0 ) );
        assertEquals( "test äöüß", atd.getExtensions().get( "X-TEST-ABC" ).get( 1 ) );
        assertEquals( "test 部長", atd.getExtensions().get( "X-TEST-ABC" ).get( 2 ) );

        // multiple extensions
        value = "(1.1 X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2'))";
        atd = parser.parseAttributeTypeDescription( value );
        assertEquals( 2, atd.getExtensions().size() );
        assertNotNull( atd.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, atd.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", atd.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", atd.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( atd.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, atd.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", atd.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", atd.getExtensions().get( "X-TEST-b" ).get( 1 ) );

        // invalid extension, no number allowed
        value = "( 1.1 X-TEST1 'test' )";
        try
        {
            atd = parser.parseAttributeTypeDescription( value );
            fail( "Exception expected, invalid extension X-TEST1 (no number allowed)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

    }


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
        // start up and track all threads (40 threads)
        List<Thread> threads = new ArrayList<Thread>();
        for ( int ii = 0; ii < 10; ii++ )
        {
            Thread t0 = new Thread( new ParseSpecification( "( 1.1 )" ) );
            Thread t1 = new Thread(
                new ParseSpecification(
                    "( 2.5.4.41 NAME 'name' DESC 'RFC2256: common supertype of name attributes'  EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} USAGE userApplications )" ) );
            Thread t2 = new Thread(
                new ParseSpecification(
                    "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications )" ) );
            Thread t3 = new Thread(
                new ParseSpecification(
                    "( 2.5.18.3 NAME 'creatorsName' DESC 'RFC2252: name of creator'  EQUALITY distinguishedNameMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 SINGLE-VALUE NO-USER-MODIFICATION USAGE directoryOperation )" ) );
            threads.add( t0 );
            threads.add( t1 );
            threads.add( t2 );
            threads.add( t3 );
            t0.start();
            t1.start();
            t2.start();
            t3.start();
        }

        // wait until all threads have died
        boolean hasLiveThreads = false;
        do
        {
            hasLiveThreads = false;

            for ( int ii = 0; ii < threads.size(); ii++ )
            {
                Thread t = ( Thread ) threads.get( ii );
                hasLiveThreads = hasLiveThreads || t.isAlive();
            }
        }
        while ( hasLiveThreads );

        // check that no one thread failed to parse and generate a SS object
        assertTrue( isSuccessMultithreaded );
    }

    /**
     * Used to test multithreaded use of a single parser.
     */
    class ParseSpecification implements Runnable
    {
        private final String atd;

        AttributeTypeDescription result;


        public ParseSpecification( String atd )
        {
            this.atd = atd;
        }


        public void run()
        {
            try
            {
                result = parser.parseAttributeTypeDescription( atd );
            }
            catch ( ParseException e )
            {
                e.printStackTrace();
            }

            isSuccessMultithreaded = isSuccessMultithreaded && ( result != null );
        }
    }

}
