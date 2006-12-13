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

import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.syntax.ObjectClassDescription;
import org.apache.directory.shared.ldap.schema.syntax.parser.ObjectClassDescriptionSchemaParser;


/**
 * Tests the ObjectClassDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaParserObjectClassDescriptionTest extends TestCase
{
    /** the parser instance */
    private ObjectClassDescriptionSchemaParser parser;

    /** holds multithreaded success value */
    boolean isSuccessMultithreaded = true;


    protected void setUp() throws Exception
    {
        parser = new ObjectClassDescriptionSchemaParser();
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
        ObjectClassDescription ocd = null;

        // null test
        value = null;
        try
        {
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
            fail( "Exception expected, no NUMERICOID" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // simple
        value = "( 1.1 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( "1.1", ocd.getNumericOid() );

        // simple
        value = "( 0.0 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( "0.0", ocd.getNumericOid() );
        
        // simple with spaces
        value = "(          1.1          )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( "1.1", ocd.getNumericOid() );

        // non-numeric not allowed
        value = "( top )";
        try
        {
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid NUMERICOID '1.1' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        
        // leading 0
        value = "( 01.1 )";
        try
        {
            parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid NUMERICOID 01.1." );
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
        ObjectClassDescription ocd = null;

        // alpha
        value = "( 1.1 NAME 'test' )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "test", ocd.getNames().get( 0 ) );

        // alpha-num-hypen
        value = "( 1.1 NAME 'a-z-0-9' )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "a-z-0-9", ocd.getNames().get( 0 ) );

        // with parentheses
        value = "( 1.1 NAME ( 'a-z-0-9' ) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "a-z-0-9", ocd.getNames().get( 0 ) );

        // with parentheses, without space
        value = "( 1.1 NAME ('a-z-0-9') )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "a-z-0-9", ocd.getNames().get( 0 ) );

        // multi with space
        value = "( 1.1 NAME ( 'test' 'a-z-0-9' ) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 2, ocd.getNames().size() );
        assertEquals( "test", ocd.getNames().get( 0 ) );
        assertEquals( "a-z-0-9", ocd.getNames().get( 1 ) );

        // multi without space
        value = "( 1.1 NAME ('test' 'a-z-0-9' 'top') )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 3, ocd.getNames().size() );
        assertEquals( "test", ocd.getNames().get( 0 ) );
        assertEquals( "a-z-0-9", ocd.getNames().get( 1 ) );
        assertEquals( "top", ocd.getNames().get( 2 ) );

        // multi with many spaces
        value = "(          1.1          NAME          (          'test'          'a-z-0-9'          'top'          )          )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 3, ocd.getNames().size() );
        assertEquals( "test", ocd.getNames().get( 0 ) );
        assertEquals( "a-z-0-9", ocd.getNames().get( 1 ) );
        assertEquals( "top", ocd.getNames().get( 2 ) );

        // lowercase
        value = "( 1.1 name 'test' )";
        try
        {
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            parser.parseObjectClassDescription( value );
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
            ocd = parser.parseObjectClassDescription( value );
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
        ObjectClassDescription ocd = null;

        // simple
        value = "(1.1 NAME 'test' DESC 'Descripton')";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( "Descripton", ocd.getDescription() );

        // unicode
        value = "( 1.1 NAME 'test' DESC 'Descripton äöüß 部長' )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( "Descripton äöüß 部長", ocd.getDescription() );

        // lowercase
        value = "( 1.1 desc 'Descripton' )";
        try
        {
            parser.parseObjectClassDescription( value );
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
        ObjectClassDescription ocd = null;

        // not obsolete
        value = "( 1.1 NAME 'test' DESC 'Descripton' )";
        ocd = parser.parseObjectClassDescription( value );
        assertFalse( ocd.isObsolete() );

        // obsolete
        value = "(1.1 NAME 'test' DESC 'Descripton' OBSOLETE)";
        ocd = parser.parseObjectClassDescription( value );
        assertTrue( ocd.isObsolete() );

        // ivalid
        value = "(1.1 NAME 'test' DESC 'Descripton' OBSOLET )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
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
    public void testSuperior() throws ParseException
    {
        String value = null;
        ObjectClassDescription ocd = null;

        // no SUP
        value = "( 1.1 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 0, ocd.getSuperiorObjectClasses().size() );

        // SUP simple numericoid
        value = "( 1.1 SUP 1.2.3 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "1.2.3", ocd.getSuperiorObjectClasses().get( 0 ) );

        // SUP simple descr
        value = "( 1.1 SUP top )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top", ocd.getSuperiorObjectClasses().get( 0 ) );

        // SUP single numericoid
        value = "( 1.1 SUP ( 1.2.3.4.5 ) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "1.2.3.4.5", ocd.getSuperiorObjectClasses().get( 0 ) );

        // SUP single descr
        value = "( 1.1 SUP ( A-Z-0-9 ) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "A-Z-0-9", ocd.getSuperiorObjectClasses().get( 0 ) );

        // SUP multi numericoid
        value = "( 1.1 SUP ( 1.2.3 $ 1.2.3.4.5 ) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 2, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "1.2.3", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( "1.2.3.4.5", ocd.getSuperiorObjectClasses().get( 1 ) );

        // SUP multi descr
        value = "( 1.1 SUP ( top1 $ top2 ) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 2, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top1", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( "top2", ocd.getSuperiorObjectClasses().get( 1 ) );

        // SUP multi mixed
        value = "( 1.1 SUP ( top1 $ 1.2.3.4 $ top2 ) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 3, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top1", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( "1.2.3.4", ocd.getSuperiorObjectClasses().get( 1 ) );
        assertEquals( "top2", ocd.getSuperiorObjectClasses().get( 2 ) );

        // SUP multi mixed no space
        value = "( 1.1 SUP (TOP-1$1.2.3.4$TOP-2) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 3, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "TOP-1", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( "1.2.3.4", ocd.getSuperiorObjectClasses().get( 1 ) );
        assertEquals( "TOP-2", ocd.getSuperiorObjectClasses().get( 2 ) );

        // SUP multi mixed many spaces
        value = "(          1.1          SUP          (          top1          $          1.2.3.4$top2          )          )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 3, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top1", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( "1.2.3.4", ocd.getSuperiorObjectClasses().get( 1 ) );
        assertEquals( "top2", ocd.getSuperiorObjectClasses().get( 2 ) );

        // no quote allowed
        value = "( 1.1 SUP 'top' )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid SUP 'top' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no quote allowed
        value = "( 1.1 SUP '1.2.3.4' )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
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
            ocd = parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid SUP '1.2.3.4.A' (invalid character)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // invalid start
        value = "( 1.1 SUP ( top1 $ -top2 ) )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid SUP '-top' (starts with hypen)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // invalid separator
        value = "( 1.1 SUP ( top1 top2 ) )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid separator (no DOLLAR)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // empty sup
        value = "( 1.1 SUP )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
            fail( "Exception expected, no SUP value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests kind (ABSTRACT, AUXILIARY, STRUCTURAL)
     * 
     * @throws ParseException
     */
    public void testKind() throws ParseException
    {
        String value = null;
        ObjectClassDescription ocd = null;

        // DEFAULT is STRUCTURAL
        value = "( 1.1 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, ocd.getKind() );

        // ABSTRACT
        value = "( 1.1 ABSTRACT )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.ABSTRACT, ocd.getKind() );

        // AUXILIARY
        value = "( 1.1 AUXILIARY )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.AUXILIARY, ocd.getKind() );

        // STRUCTURAL
        value = "( 1.1 STRUCTURAL )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, ocd.getKind() );

        // ivalid
        value = "( 1.1 FOO )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid KIND value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Test MUST and its values.
     * Very similar to SUP, so here are less test cases. 
     * 
     * @throws ParseException
     */
    public void testMust() throws ParseException
    {
        String value = null;
        ObjectClassDescription ocd = null;

        // no MUST
        value = "( 1.1 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 0, ocd.getMustAttributeTypes().size() );

        // MUST simple numericoid
        value = "( 1.1 MUST 1.2.3 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getMustAttributeTypes().size() );
        assertEquals( "1.2.3", ocd.getMustAttributeTypes().get( 0 ) );

        // MUST mulitple
        value = "(1.1 MUST (cn$sn       $11.22.33.44.55         $  objectClass   ))";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 4, ocd.getMustAttributeTypes().size() );
        assertEquals( "cn", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( "sn", ocd.getMustAttributeTypes().get( 1 ) );
        assertEquals( "11.22.33.44.55", ocd.getMustAttributeTypes().get( 2 ) );
        assertEquals( "objectClass", ocd.getMustAttributeTypes().get( 3 ) );

        // invalid value
        value = "( 1.1 MUST ( c_n ) )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
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
            ocd = parser.parseObjectClassDescription( value );
            fail( "Exception expected, no MUST value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Test MAY and its values.
     * Very similar to SUP, so here are less test cases. 
     * 
     * @throws ParseException
     */
    public void testMay() throws ParseException
    {
        String value = null;
        ObjectClassDescription ocd = null;

        // no MAY
        value = "( 1.1 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 0, ocd.getMayAttributeTypes().size() );

        // MAY simple numericoid
        value = "( 1.1 MAY 1.2.3 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getMayAttributeTypes().size() );
        assertEquals( "1.2.3", ocd.getMayAttributeTypes().get( 0 ) );

        // MAY mulitple
        value = "(1.1 MAY (cn$sn       $11.22.33.44.55         $  objectClass   ))";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 4, ocd.getMayAttributeTypes().size() );
        assertEquals( "cn", ocd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "sn", ocd.getMayAttributeTypes().get( 1 ) );
        assertEquals( "11.22.33.44.55", ocd.getMayAttributeTypes().get( 2 ) );
        assertEquals( "objectClass", ocd.getMayAttributeTypes().get( 3 ) );

        // invalid value
        value = "( 1.1 MAY ( c_n ) )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
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
        String value = null;
        ObjectClassDescription ocd = null;

        // no extension
        value = "( 1.1 )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 0, ocd.getExtensions().size() );

        // single extension with one value
        value = "( 1.1 X-TEST 'test' )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getExtensions().size() );
        assertNotNull( ocd.getExtensions().get( "X-TEST" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-TEST" ).size() );
        assertEquals( "test", ocd.getExtensions().get( "X-TEST" ).get( 0 ) );

        // single extension with multiple values
        value = "( 1.1 X-TEST-ABC ('test1' 'test äöüß'       'test 部長' ) )";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 1, ocd.getExtensions().size() );
        assertNotNull( ocd.getExtensions().get( "X-TEST-ABC" ) );
        assertEquals( 3, ocd.getExtensions().get( "X-TEST-ABC" ).size() );
        assertEquals( "test1", ocd.getExtensions().get( "X-TEST-ABC" ).get( 0 ) );
        assertEquals( "test äöüß", ocd.getExtensions().get( "X-TEST-ABC" ).get( 1 ) );
        assertEquals( "test 部長", ocd.getExtensions().get( "X-TEST-ABC" ).get( 2 ) );

        // multiple extensions
        value = "(1.1 X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2'))";
        ocd = parser.parseObjectClassDescription( value );
        assertEquals( 2, ocd.getExtensions().size() );
        assertNotNull( ocd.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, ocd.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", ocd.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", ocd.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( ocd.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, ocd.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", ocd.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", ocd.getExtensions().get( "X-TEST-b" ).get( 1 ) );

        // invalid extension, no number allowed
        value = "( 1.1 X-TEST1 'test' )";
        try
        {
            ocd = parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid extension X-TEST1 (no number allowed)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

    }


    public void testIgnoreElementOrder() throws ParseException
    {
        String value = "( 2.5.6.6 STRUCTURAL MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) SUP top DESC 'RFC2256: a person' MUST ( sn $ cn ) NAME 'person' )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.6", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "person", ocd.getNames().get( 0 ) );
        assertEquals( "RFC2256: a person", ocd.getDescription() );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, ocd.getKind() );
        assertEquals( 2, ocd.getMustAttributeTypes().size() );
        assertEquals( "sn", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( "cn", ocd.getMustAttributeTypes().get( 1 ) );
        assertEquals( 4, ocd.getMayAttributeTypes().size() );
        assertEquals( "userPassword", ocd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "telephoneNumber", ocd.getMayAttributeTypes().get( 1 ) );
        assertEquals( "seeAlso", ocd.getMayAttributeTypes().get( 2 ) );
        assertEquals( "description", ocd.getMayAttributeTypes().get( 3 ) );
        assertEquals( 0, ocd.getExtensions().size() );

    }


    ////////////////////////////////////////////////////////////////
    //          Some real-world object class definitions          //
    ////////////////////////////////////////////////////////////////

    public void testRfcTop() throws ParseException
    {
        String value = "( 2.5.6.0 NAME 'top' DESC 'top of the superclass chain' ABSTRACT MUST objectClass )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.0", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "top", ocd.getNames().get( 0 ) );
        assertEquals( "top of the superclass chain", ocd.getDescription() );
        assertEquals( 0, ocd.getSuperiorObjectClasses().size() );
        assertEquals( ObjectClassTypeEnum.ABSTRACT, ocd.getKind() );
        assertEquals( 1, ocd.getMustAttributeTypes().size() );
        assertEquals( "objectClass", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( 0, ocd.getMayAttributeTypes().size() );
        assertEquals( 0, ocd.getExtensions().size() );
    }


    public void testRfcPerson() throws ParseException
    {
        String value = "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top STRUCTURAL MUST ( sn $ cn ) MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.6", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "person", ocd.getNames().get( 0 ) );
        assertEquals( "RFC2256: a person", ocd.getDescription() );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, ocd.getKind() );
        assertEquals( 2, ocd.getMustAttributeTypes().size() );
        assertEquals( "sn", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( "cn", ocd.getMustAttributeTypes().get( 1 ) );
        assertEquals( 4, ocd.getMayAttributeTypes().size() );
        assertEquals( "userPassword", ocd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "telephoneNumber", ocd.getMayAttributeTypes().get( 1 ) );
        assertEquals( "seeAlso", ocd.getMayAttributeTypes().get( 2 ) );
        assertEquals( "description", ocd.getMayAttributeTypes().get( 3 ) );
        assertEquals( 0, ocd.getExtensions().size() );
    }


    public void testRfcSimpleSecurityObject() throws ParseException
    {
        String value = "( 0.9.2342.19200300.100.4.19 NAME 'simpleSecurityObject' DESC 'RFC1274: simple security object' SUP top AUXILIARY MUST userPassword )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "0.9.2342.19200300.100.4.19", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "simpleSecurityObject", ocd.getNames().get( 0 ) );
        assertEquals( "RFC1274: simple security object", ocd.getDescription() );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.AUXILIARY, ocd.getKind() );
        assertEquals( 1, ocd.getMustAttributeTypes().size() );
        assertEquals( "userPassword", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( 0, ocd.getMayAttributeTypes().size() );
        assertEquals( 0, ocd.getExtensions().size() );
    }


    public void testSunAlias() throws ParseException
    {
        String value = "( 2.5.6.1 NAME 'alias' DESC 'Standard LDAP objectclass' SUP top ABSTRACT MUST aliasedObjectName X-ORIGIN 'RFC 2256' )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.1", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "alias", ocd.getNames().get( 0 ) );
        assertEquals( "Standard LDAP objectclass", ocd.getDescription() );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.ABSTRACT, ocd.getKind() );
        assertEquals( 1, ocd.getMustAttributeTypes().size() );
        assertEquals( "aliasedObjectName", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( 0, ocd.getMayAttributeTypes().size() );

        assertEquals( 1, ocd.getExtensions().size() );
        assertNotNull( ocd.getExtensions().get( "X-ORIGIN" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-ORIGIN" ).size() );
        assertEquals( "RFC 2256", ocd.getExtensions().get( "X-ORIGIN" ).get( 0 ) );
    }


    public void testNovellDcObject() throws ParseException
    {
        String value = "( 1.3.6.1.4.1.1466.344 NAME 'dcObject' AUXILIARY MUST dc X-NDS_NAMING 'dc' X-NDS_NOT_CONTAINER '1' X-NDS_NONREMOVABLE '1' )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "1.3.6.1.4.1.1466.344", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "dcObject", ocd.getNames().get( 0 ) );
        assertEquals( "", ocd.getDescription() );
        assertEquals( 0, ocd.getSuperiorObjectClasses().size() );
        assertEquals( ObjectClassTypeEnum.AUXILIARY, ocd.getKind() );
        assertEquals( 1, ocd.getMustAttributeTypes().size() );
        assertEquals( "dc", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( 0, ocd.getMayAttributeTypes().size() );

        assertEquals( 3, ocd.getExtensions().size() );
        assertNotNull( ocd.getExtensions().get( "X-NDS_NAMING" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-NDS_NAMING" ).size() );
        assertEquals( "dc", ocd.getExtensions().get( "X-NDS_NAMING" ).get( 0 ) );
        assertNotNull( ocd.getExtensions().get( "X-NDS_NOT_CONTAINER" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-NDS_NOT_CONTAINER" ).size() );
        assertEquals( "1", ocd.getExtensions().get( "X-NDS_NOT_CONTAINER" ).get( 0 ) );
        assertNotNull( ocd.getExtensions().get( "X-NDS_NONREMOVABLE" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-NDS_NONREMOVABLE" ).size() );
        assertEquals( "1", ocd.getExtensions().get( "X-NDS_NONREMOVABLE" ).get( 0 ) );
    }


    public void testNovellList() throws ParseException
    {
        String value = "( 2.16.840.1.113719.1.1.6.1.30 NAME 'List' SUP Top STRUCTURAL MUST cn MAY ( description $ l $ member $ ou $ o $ eMailAddress $ mailboxLocation $ mailboxID $ owner $ seeAlso $ fullName ) X-NDS_NAMING 'cn' X-NDS_CONTAINMENT ( 'Organization' 'organizationalUnit' 'domain' ) X-NDS_NOT_CONTAINER '1' X-NDS_NONREMOVABLE '1' X-NDS_ACL_TEMPLATES '2#entry#[Root Template]#member' )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "2.16.840.1.113719.1.1.6.1.30", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "List", ocd.getNames().get( 0 ) );
        assertEquals( "", ocd.getDescription() );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "Top", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, ocd.getKind() );
        assertEquals( 1, ocd.getMustAttributeTypes().size() );
        assertEquals( "cn", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( 11, ocd.getMayAttributeTypes().size() );
        assertEquals( "description", ocd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "fullName", ocd.getMayAttributeTypes().get( 10 ) );

        assertEquals( 5, ocd.getExtensions().size() );
        assertNotNull( ocd.getExtensions().get( "X-NDS_NAMING" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-NDS_NAMING" ).size() );
        assertEquals( "cn", ocd.getExtensions().get( "X-NDS_NAMING" ).get( 0 ) );

        assertNotNull( ocd.getExtensions().get( "X-NDS_NOT_CONTAINER" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-NDS_NOT_CONTAINER" ).size() );
        assertEquals( "1", ocd.getExtensions().get( "X-NDS_NOT_CONTAINER" ).get( 0 ) );

        assertNotNull( ocd.getExtensions().get( "X-NDS_NONREMOVABLE" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-NDS_NONREMOVABLE" ).size() );
        assertEquals( "1", ocd.getExtensions().get( "X-NDS_NONREMOVABLE" ).get( 0 ) );

        // X-NDS_CONTAINMENT ( 'Organization' 'organizationalUnit' 'domain' )
        assertNotNull( ocd.getExtensions().get( "X-NDS_CONTAINMENT" ) );
        assertEquals( 3, ocd.getExtensions().get( "X-NDS_CONTAINMENT" ).size() );
        assertEquals( "Organization", ocd.getExtensions().get( "X-NDS_CONTAINMENT" ).get( 0 ) );
        assertEquals( "organizationalUnit", ocd.getExtensions().get( "X-NDS_CONTAINMENT" ).get( 1 ) );
        assertEquals( "domain", ocd.getExtensions().get( "X-NDS_CONTAINMENT" ).get( 2 ) );

        // X-NDS_ACL_TEMPLATES '2#entry#[Root Template]#member'
        assertNotNull( ocd.getExtensions().get( "X-NDS_ACL_TEMPLATES" ) );
        assertEquals( 1, ocd.getExtensions().get( "X-NDS_ACL_TEMPLATES" ).size() );
        assertEquals( "2#entry#[Root Template]#member", ocd.getExtensions().get( "X-NDS_ACL_TEMPLATES" ).get( 0 ) );
    }


    public void testMicrosoftAds2000Locality() throws ParseException
    {
        String value = "( 2.5.6.3 NAME 'locality' SUP top STRUCTURAL MUST (l ) MAY (st $ street $ searchGuide $ seeAlso ) )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.3", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "locality", ocd.getNames().get( 0 ) );
        assertEquals( "", ocd.getDescription() );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, ocd.getKind() );
        assertEquals( 1, ocd.getMustAttributeTypes().size() );
        assertEquals( "l", ocd.getMustAttributeTypes().get( 0 ) );
        assertEquals( 4, ocd.getMayAttributeTypes().size() );
        assertEquals( "st", ocd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "street", ocd.getMayAttributeTypes().get( 1 ) );
        assertEquals( "searchGuide", ocd.getMayAttributeTypes().get( 2 ) );
        assertEquals( "seeAlso", ocd.getMayAttributeTypes().get( 3 ) );
        assertEquals( 0, ocd.getExtensions().size() );
    }


    public void testMicrosoftAds2003Msieee() throws ParseException
    {
        String value = "( 1.2.840.113556.1.5.240 NAME 'msieee80211-Policy' SUP top STRUCTURAL MAY (msieee80211-Data $ msieee80211-DataType $ msieee80211-ID ) )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "1.2.840.113556.1.5.240", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "msieee80211-Policy", ocd.getNames().get( 0 ) );
        assertEquals( "", ocd.getDescription() );
        assertEquals( 1, ocd.getSuperiorObjectClasses().size() );
        assertEquals( "top", ocd.getSuperiorObjectClasses().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, ocd.getKind() );
        assertEquals( 0, ocd.getMustAttributeTypes().size() );
        assertEquals( 3, ocd.getMayAttributeTypes().size() );
        assertEquals( "msieee80211-Data", ocd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "msieee80211-DataType", ocd.getMayAttributeTypes().get( 1 ) );
        assertEquals( "msieee80211-ID", ocd.getMayAttributeTypes().get( 2 ) );
        assertEquals( 0, ocd.getExtensions().size() );
    }


    public void testSiemensDirxX500Subschema() throws ParseException
    {
        String value = "( 2.5.20.1 NAME 'x500subSchema' AUXILIARY MAY (dITStructureRules $ nameForms $ dITContentRules $ x500objectClasses $ x500attributeTypes $ matchingRules $ matchingRuleUse) )";
        ObjectClassDescription ocd = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.20.1", ocd.getNumericOid() );
        assertEquals( 1, ocd.getNames().size() );
        assertEquals( "x500subSchema", ocd.getNames().get( 0 ) );
        assertEquals( "", ocd.getDescription() );
        assertEquals( 0, ocd.getSuperiorObjectClasses().size() );
        assertEquals( ObjectClassTypeEnum.AUXILIARY, ocd.getKind() );
        assertEquals( 0, ocd.getMustAttributeTypes().size() );
        assertEquals( 7, ocd.getMayAttributeTypes().size() );
        assertEquals( "dITStructureRules", ocd.getMayAttributeTypes().get( 0 ) );
        assertEquals( "matchingRuleUse", ocd.getMayAttributeTypes().get( 6 ) );
        assertEquals( 0, ocd.getExtensions().size() );
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
            Thread t1 = new Thread( new ParseSpecification(
                "( 2.5.6.0 NAME 'top' DESC 'top of the superclass chain' ABSTRACT MUST objectClass )" ) );
            Thread t2 = new Thread(
                new ParseSpecification(
                    "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top STRUCTURAL MUST ( sn $ cn ) MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) )" ) );
            Thread t3 = new Thread(
                new ParseSpecification(
                    "( 2.16.840.1.113719.1.1.6.1.30 NAME 'List' SUP Top STRUCTURAL MUST cn MAY ( description $ l $ member $ ou $ o $ eMailAddress $ mailboxLocation $ mailboxID $ owner $ seeAlso $ fullName ) X-NDS_NAMING 'cn' X-NDS_CONTAINMENT ( 'Organization' 'organizationalUnit' 'domain' ) X-NDS_NOT_CONTAINER '1' X-NDS_NONREMOVABLE '1' X-NDS_ACL_TEMPLATES '2#entry#[Root Template]#member' )" ) );
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
        private final String ocd;

        ObjectClassDescription result;


        public ParseSpecification( String ocd )
        {
            this.ocd = ocd;
        }


        public void run()
        {
            try
            {
                result = parser.parseObjectClassDescription( ocd );
            }
            catch ( ParseException e )
            {
                e.printStackTrace();
            }

            isSuccessMultithreaded = isSuccessMultithreaded && ( result != null );
        }
    }

}
