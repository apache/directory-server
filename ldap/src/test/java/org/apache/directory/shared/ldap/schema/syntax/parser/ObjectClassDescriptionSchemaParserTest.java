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

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.parsers.ObjectClassDescriptionSchemaParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * Tests the ObjectClassDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ObjectClassDescriptionSchemaParserTest
{
    /** the parser instance */
    private ObjectClassDescriptionSchemaParser parser;


    @Before
    public void setUp() throws Exception
    {
        parser = new ObjectClassDescriptionSchemaParser();
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
        SchemaParserTestUtils.testNumericOid( parser, "" );
    }


    /**
     * Tests NAME and its values
     * 
     * @throws ParseException
     */
    @Test
    public void testNames() throws ParseException
    {
        SchemaParserTestUtils.testNames( parser, "1.1", "" );
    }


    /**
     * Tests DESC
     * 
     * @throws ParseException
     */
    @Test
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "" );
    }


    /**
     * Tests OBSOLETE
     * 
     * @throws ParseException
     */
    @Test
    public void testObsolete() throws ParseException
    {
        SchemaParserTestUtils.testObsolete( parser, "1.1", "" );
    }


    /**
     * Test SUP and its values.
     * 
     * @throws ParseException
     */
    @Test
    public void testSuperior() throws ParseException, NamingException
    {
        String value = null;
        ObjectClass objectClass = null;

        // no SUP
        value = "( 1.1 )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 0, objectClass.getSuperiorOids().size() );

        // SUP simple numericoid
        value = "( 1.1 SUP 1.2.3 )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "1.2.3", objectClass.getSuperiorOids().get( 0 ) );

        // SUP simple descr
        value = "( 1.1 SUP top )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "top", objectClass.getSuperiorOids().get( 0 ) );

        // SUP single numericoid
        value = "( 1.1 SUP ( 1.2.3.4.5 ) )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "1.2.3.4.5", objectClass.getSuperiorOids().get( 0 ) );

        // SUP single descr
        value = "( 1.1 SUP ( A-Z-0-9 ) )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "A-Z-0-9", objectClass.getSuperiorOids().get( 0 ) );

        // SUP multi numericoid
        value = "( 1.1 SUP ( 1.2.3 $ 1.2.3.4.5 ) )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 2, objectClass.getSuperiorOids().size() );
        assertEquals( "1.2.3", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( "1.2.3.4.5", objectClass.getSuperiorOids().get( 1 ) );

        // SUP multi descr
        value = "( 1.1 SUP ( top1 $ top2 ) )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 2, objectClass.getSuperiorOids().size() );
        assertEquals( "top1", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( "top2", objectClass.getSuperiorOids().get( 1 ) );

        // SUP multi mixed, tabs
        value = "\t(\t1.1\tSUP\t(\ttop1\t$\t1.2.3.4\t$\ttop2\t)\t)\t";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 3, objectClass.getSuperiorOids().size() );
        assertEquals( "top1", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( "1.2.3.4", objectClass.getSuperiorOids().get( 1 ) );
        assertEquals( "top2", objectClass.getSuperiorOids().get( 2 ) );

        // SUP multi mixed, no space
        value = "(1.1 SUP(TOP-1$1.2.3.4$TOP-2))";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 3, objectClass.getSuperiorOids().size() );
        assertEquals( "TOP-1", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( "1.2.3.4", objectClass.getSuperiorOids().get( 1 ) );
        assertEquals( "TOP-2", objectClass.getSuperiorOids().get( 2 ) );

        // SUP multi mixed many spaces
        value = "(          1.1          SUP          (          top1          $          1.2.3.4$top2          )          )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 3, objectClass.getSuperiorOids().size() );
        assertEquals( "top1", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( "1.2.3.4", objectClass.getSuperiorOids().get( 1 ) );
        assertEquals( "top2", objectClass.getSuperiorOids().get( 2 ) );

        // quoted value
        value = "( 1.1 SUP 'top' )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "top", objectClass.getSuperiorOids().get( 0 ) );

        // quoted value
        value = "( 1.1 SUP '1.2.3.4' )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "1.2.3.4", objectClass.getSuperiorOids().get( 0 ) );

        // no $ separator
        value = "( 1.1 SUP ( top1 top2 ) )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 2, objectClass.getSuperiorOids().size() );
        assertEquals( "top1", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( "top2", objectClass.getSuperiorOids().get( 1 ) );

        // invalid character
        value = "( 1.1 SUP 1.2.3.4.A )";
        try
        {
            objectClass = parser.parseObjectClassDescription( value );
            fail( "Exception expected, invalid SUP '1.2.3.4.A' (invalid character)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // empty sup
        value = "( 1.1 SUP )";
        try
        {
            objectClass = parser.parseObjectClassDescription( value );
            fail( "Exception expected, no SUP value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        if ( !parser.isQuirksMode() )
        {
            // invalid start
            value = "( 1.1 SUP ( top1 $ -top2 ) )";
            try
            {
                objectClass = parser.parseObjectClassDescription( value );
                fail( "Exception expected, invalid SUP '-top' (starts with hypen)" );
            }
            catch ( ParseException pe )
            {
                // expected
            }
        }
    }


    /**
     * Tests kind (ABSTRACT, AUXILIARY, STRUCTURAL)
     * 
     * @throws ParseException
     */
    @Test
    public void testKind() throws ParseException
    {
        String value = null;
        ObjectClass objectClass = null;

        // DEFAULT is STRUCTURAL
        value = "( 1.1 )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );

        // ABSTRACT
        value = "( 1.1 ABSTRACT )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.ABSTRACT, objectClass.getType() );

        // AUXILIARY, tab
        value = "\t(\t1.1\tAUXILIARY\t)\t";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.AUXILIARY, objectClass.getType() );

        // STRUCTURAL, no space
        value = "(1.1 STRUCTURAL)";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );

        // STRUCTURAL, case-insensitive
        value = "(1.1 sTrUcTuRaL )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );

        // invalid
        value = "( 1.1 FOO )";
        try
        {
            objectClass = parser.parseObjectClassDescription( value );
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
    @Test
    public void testMust() throws ParseException
    {
        String value = null;
        ObjectClass objectClass = null;

        // no MUST
        value = "( 1.1 )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 0, objectClass.getMustAttributeTypeOids().size() );

        // MUST simple numericoid
        value = "( 1.1 MUST 1.2.3 )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 1, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "1.2.3", objectClass.getMustAttributeTypeOids().get( 0 ) );

        // MUST multiple
        value = "(1.1 MUST(cn$sn\r$11.22.33.44.55         $  objectClass   ))";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 4, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "cn", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( "sn", objectClass.getMustAttributeTypeOids().get( 1 ) );
        assertEquals( "11.22.33.44.55", objectClass.getMustAttributeTypeOids().get( 2 ) );
        assertEquals( "objectClass", objectClass.getMustAttributeTypeOids().get( 3 ) );

        // MUST multiple, no $ separator
        value = "(1.1 MUST(cn sn\t'11.22.33.44.55'\n'objectClass'))";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 4, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "cn", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( "sn", objectClass.getMustAttributeTypeOids().get( 1 ) );
        assertEquals( "11.22.33.44.55", objectClass.getMustAttributeTypeOids().get( 2 ) );
        assertEquals( "objectClass", objectClass.getMustAttributeTypeOids().get( 3 ) );

        // no MUST values
        value = "( 1.1 MUST )";
        try
        {
            objectClass = parser.parseObjectClassDescription( value );
            fail( "Exception expected, no MUST value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        if ( !parser.isQuirksMode() )
        {
            // invalid value
            value = "( 1.1 MUST ( c_n ) )";
            try
            {
                objectClass = parser.parseObjectClassDescription( value );
                fail( "Exception expected, invalid value c_n" );
            }
            catch ( ParseException pe )
            {
                // expected
            }
        }
    }


    /**
     * Test MAY and its values.
     * Very similar to SUP, so here are less test cases. 
     * 
     * @throws ParseException
     */
    @Test
    public void testMay() throws ParseException
    {
        String value = null;
        ObjectClass objectClass = null;

        // no MAY
        value = "( 1.1 )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 0, objectClass.getMayAttributeTypeOids().size() );

        // MAY simple numericoid
        value = "( 1.1 MAY 1.2.3 )";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 1, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "1.2.3", objectClass.getMayAttributeTypeOids().get( 0 ) );

        // MAY multiple
        value = "(1.1 MAY(cn$sn       $11.22.33.44.55\n$  objectClass   ))";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 4, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "cn", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "sn", objectClass.getMayAttributeTypeOids().get( 1 ) );
        assertEquals( "11.22.33.44.55", objectClass.getMayAttributeTypeOids().get( 2 ) );
        assertEquals( "objectClass", objectClass.getMayAttributeTypeOids().get( 3 ) );

        // MAY multiple, no $ separator, quoted
        value = "(1.1 MAY('cn' sn\t'11.22.33.44.55'\nobjectClass))";
        objectClass = parser.parseObjectClassDescription( value );
        assertEquals( 4, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "cn", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "sn", objectClass.getMayAttributeTypeOids().get( 1 ) );
        assertEquals( "11.22.33.44.55", objectClass.getMayAttributeTypeOids().get( 2 ) );
        assertEquals( "objectClass", objectClass.getMayAttributeTypeOids().get( 3 ) );

        if ( !parser.isQuirksMode() )
        {
            // invalid value
            value = "( 1.1 MAY ( c_n ) )";
            try
            {
                objectClass = parser.parseObjectClassDescription( value );
                fail( "Exception expected, invalid value c_n" );
            }
            catch ( ParseException pe )
            {
                // expected
            }
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
        SchemaParserTestUtils.testExtensions( parser, "1.1", "" );

    }


    /**
     * Test full object class description.
     * 
     * @throws ParseException
     */
    @Test
    public void testFull() throws ParseException, NamingException
    {
        String value = null;
        ObjectClass objectClass = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577' OBSOLETE SUP ( 2.3.4.5.6.7.8.9.0.1 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) STRUCTURAL MUST ( 3.4.5.6.7.8.9.0.1.2 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) MAY ( 4.5.6.7.8.9.0.1.2.3 $ abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 ) X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )";
        objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "1.2.3.4.5.6.7.8.9.0", objectClass.getOid() );
        assertEquals( 2, objectClass.getNames().size() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", objectClass.getNames().get( 0 ) );
        assertEquals( "test", objectClass.getNames().get( 1 ) );
        assertEquals( "Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577", objectClass.getDescription() );
        assertTrue( objectClass.isObsolete() );
        assertEquals( 2, objectClass.getSuperiorOids().size() );
        assertEquals( "2.3.4.5.6.7.8.9.0.1", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", objectClass
            .getSuperiorOids().get( 1 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );
        assertEquals( 2, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "3.4.5.6.7.8.9.0.1.2", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", objectClass.getMustAttributeTypeOids()
            .get( 1 ) );
        assertEquals( 2, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "4.5.6.7.8.9.0.1.2.3", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", objectClass.getMayAttributeTypeOids()
            .get( 1 ) );
        assertEquals( 2, objectClass.getExtensions().size() );
        assertNotNull( objectClass.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, objectClass.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", objectClass.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", objectClass.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( objectClass.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, objectClass.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", objectClass.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", objectClass.getExtensions().get( "X-TEST-b" ).get( 1 ) );
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
            { "( 1.1 NAME 'test1' NAME 'test2' )", "( 1.1 DESC 'test1' DESC 'test2' )", "( 1.1 OBSOLETE OBSOLETE )",
                "( 1.1 SUP test1 SUP test2 )", "( 1.1 STRUCTURAL STRUCTURAL )", "( 1.1 ABSTRACT ABSTRACT )",
                "( 1.1 AUXILIARY AUXILIARY )", "( 1.1 STRUCTURAL AUXILIARY AUXILIARY )",
                "( 1.1 MUST test1 MUST test2 )", "( 1.1 MAY test1 MAY test2 )", "( 1.1 X-TEST 'test1' X-TEST 'test2' )" };
        SchemaParserTestUtils.testUnique( parser, testValues );
    }


    /**
     * Ensure that element order is ignored
     * 
     * @throws ParseException
     */
    @Test
    public void testIgnoreElementOrder() throws ParseException, NamingException
    {
        String value = "( 2.5.6.6 STRUCTURAL MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) SUP top DESC 'RFC2256: a person' MUST ( sn $ cn ) NAME 'person' )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.6", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "person", objectClass.getNames().get( 0 ) );
        assertEquals( "RFC2256: a person", objectClass.getDescription() );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "top", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );
        assertEquals( 2, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "sn", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( "cn", objectClass.getMustAttributeTypeOids().get( 1 ) );
        assertEquals( 4, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "userPassword", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "telephoneNumber", objectClass.getMayAttributeTypeOids().get( 1 ) );
        assertEquals( "seeAlso", objectClass.getMayAttributeTypeOids().get( 2 ) );
        assertEquals( "description", objectClass.getMayAttributeTypeOids().get( 3 ) );
        assertEquals( 0, objectClass.getExtensions().size() );

    }


    ////////////////////////////////////////////////////////////////
    //          Some real-world object class definitions          //
    ////////////////////////////////////////////////////////////////

    @Test
    public void testRfcTop() throws ParseException, NamingException
    {
        String value = "( 2.5.6.0 NAME 'top' DESC 'top of the superclass chain' ABSTRACT MUST objectClass )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.0", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "top", objectClass.getNames().get( 0 ) );
        assertEquals( "top of the superclass chain", objectClass.getDescription() );
        assertEquals( 0, objectClass.getSuperiorOids().size() );
        assertEquals( ObjectClassTypeEnum.ABSTRACT, objectClass.getType() );
        assertEquals( 1, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "objectClass", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( 0, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( 0, objectClass.getExtensions().size() );
    }


    @Test
    public void testRfcPerson() throws ParseException, NamingException
    {
        String value = "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top STRUCTURAL MUST ( sn $ cn ) MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.6", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "person", objectClass.getNames().get( 0 ) );
        assertEquals( "RFC2256: a person", objectClass.getDescription() );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "top", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );
        assertEquals( 2, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "sn", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( "cn", objectClass.getMustAttributeTypeOids().get( 1 ) );
        assertEquals( 4, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "userPassword", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "telephoneNumber", objectClass.getMayAttributeTypeOids().get( 1 ) );
        assertEquals( "seeAlso", objectClass.getMayAttributeTypeOids().get( 2 ) );
        assertEquals( "description", objectClass.getMayAttributeTypeOids().get( 3 ) );
        assertEquals( 0, objectClass.getExtensions().size() );
    }


    @Test
    public void testRfcSimpleSecurityObject() throws ParseException, NamingException
    {
        String value = "( 0.9.2342.19200300.100.4.19 NAME 'simpleSecurityObject' DESC 'RFC1274: simple security object' SUP top AUXILIARY MUST userPassword )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "0.9.2342.19200300.100.4.19", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "simpleSecurityObject", objectClass.getNames().get( 0 ) );
        assertEquals( "RFC1274: simple security object", objectClass.getDescription() );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "top", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.AUXILIARY, objectClass.getType() );
        assertEquals( 1, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "userPassword", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( 0, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( 0, objectClass.getExtensions().size() );
    }


    @Test
    public void testSunAlias() throws ParseException, NamingException
    {
        String value = "( 2.5.6.1 NAME 'alias' DESC 'Standard LDAP objectclass' SUP top ABSTRACT MUST aliasedObjectName X-ORIGIN 'RFC 2256' )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.1", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "alias", objectClass.getNames().get( 0 ) );
        assertEquals( "Standard LDAP objectclass", objectClass.getDescription() );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "top", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.ABSTRACT, objectClass.getType() );
        assertEquals( 1, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "aliasedObjectName", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( 0, objectClass.getMayAttributeTypeOids().size() );

        assertEquals( 1, objectClass.getExtensions().size() );
        assertNotNull( objectClass.getExtensions().get( "X-ORIGIN" ) );
        assertEquals( 1, objectClass.getExtensions().get( "X-ORIGIN" ).size() );
        assertEquals( "RFC 2256", objectClass.getExtensions().get( "X-ORIGIN" ).get( 0 ) );
    }


    @Test
    public void testNovellDcObject() throws ParseException, NamingException
    {
        String value = "( 1.3.6.1.4.1.1466.344 NAME 'dcObject' AUXILIARY MUST dc X-NDS_NAMING 'dc' X-NDS_NOT_CONTAINER '1' X-NDS_NONREMOVABLE '1' )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "1.3.6.1.4.1.1466.344", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "dcObject", objectClass.getNames().get( 0 ) );
        assertNull( objectClass.getDescription() );
        assertEquals( 0, objectClass.getSuperiorOids().size() );
        assertEquals( ObjectClassTypeEnum.AUXILIARY, objectClass.getType() );
        assertEquals( 1, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "dc", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( 0, objectClass.getMayAttributeTypeOids().size() );

        assertEquals( 3, objectClass.getExtensions().size() );
        assertNotNull( objectClass.getExtensions().get( "X-NDS_NAMING" ) );
        assertEquals( 1, objectClass.getExtensions().get( "X-NDS_NAMING" ).size() );
        assertEquals( "dc", objectClass.getExtensions().get( "X-NDS_NAMING" ).get( 0 ) );
        assertNotNull( objectClass.getExtensions().get( "X-NDS_NOT_CONTAINER" ) );
        assertEquals( 1, objectClass.getExtensions().get( "X-NDS_NOT_CONTAINER" ).size() );
        assertEquals( "1", objectClass.getExtensions().get( "X-NDS_NOT_CONTAINER" ).get( 0 ) );
        assertNotNull( objectClass.getExtensions().get( "X-NDS_NONREMOVABLE" ) );
        assertEquals( 1, objectClass.getExtensions().get( "X-NDS_NONREMOVABLE" ).size() );
        assertEquals( "1", objectClass.getExtensions().get( "X-NDS_NONREMOVABLE" ).get( 0 ) );
    }


    @Test
    public void testNovellList() throws ParseException, NamingException
    {
        String value = "( 2.16.840.1.113719.1.1.6.1.30 NAME 'List' SUP Top STRUCTURAL MUST cn MAY ( description $ l $ member $ ou $ o $ eMailAddress $ mailboxLocation $ mailboxID $ owner $ seeAlso $ fullName ) X-NDS_NAMING 'cn' X-NDS_CONTAINMENT ( 'Organization' 'organizationalUnit' 'domain' ) X-NDS_NOT_CONTAINER '1' X-NDS_NONREMOVABLE '1' X-NDS_ACL_TEMPLATES '2#entry#[Root Template]#member' )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "2.16.840.1.113719.1.1.6.1.30", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "List", objectClass.getNames().get( 0 ) );
        assertNull( objectClass.getDescription() );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "Top", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );
        assertEquals( 1, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "cn", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( 11, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "description", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "fullName", objectClass.getMayAttributeTypeOids().get( 10 ) );

        assertEquals( 5, objectClass.getExtensions().size() );
        assertNotNull( objectClass.getExtensions().get( "X-NDS_NAMING" ) );
        assertEquals( 1, objectClass.getExtensions().get( "X-NDS_NAMING" ).size() );
        assertEquals( "cn", objectClass.getExtensions().get( "X-NDS_NAMING" ).get( 0 ) );

        assertNotNull( objectClass.getExtensions().get( "X-NDS_NOT_CONTAINER" ) );
        assertEquals( 1, objectClass.getExtensions().get( "X-NDS_NOT_CONTAINER" ).size() );
        assertEquals( "1", objectClass.getExtensions().get( "X-NDS_NOT_CONTAINER" ).get( 0 ) );

        assertNotNull( objectClass.getExtensions().get( "X-NDS_NONREMOVABLE" ) );
        assertEquals( 1, objectClass.getExtensions().get( "X-NDS_NONREMOVABLE" ).size() );
        assertEquals( "1", objectClass.getExtensions().get( "X-NDS_NONREMOVABLE" ).get( 0 ) );

        // X-NDS_CONTAINMENT ( 'Organization' 'organizationalUnit' 'domain' )
        assertNotNull( objectClass.getExtensions().get( "X-NDS_CONTAINMENT" ) );
        assertEquals( 3, objectClass.getExtensions().get( "X-NDS_CONTAINMENT" ).size() );
        assertEquals( "Organization", objectClass.getExtensions().get( "X-NDS_CONTAINMENT" ).get( 0 ) );
        assertEquals( "organizationalUnit", objectClass.getExtensions().get( "X-NDS_CONTAINMENT" ).get( 1 ) );
        assertEquals( "domain", objectClass.getExtensions().get( "X-NDS_CONTAINMENT" ).get( 2 ) );

        // X-NDS_ACL_TEMPLATES '2#entry#[Root Template]#member'
        assertNotNull( objectClass.getExtensions().get( "X-NDS_ACL_TEMPLATES" ) );
        assertEquals( 1, objectClass.getExtensions().get( "X-NDS_ACL_TEMPLATES" ).size() );
        assertEquals( "2#entry#[Root Template]#member", objectClass.getExtensions().get( "X-NDS_ACL_TEMPLATES" ).get( 0 ) );
    }


    @Test
    public void testMicrosoftAds2000Locality() throws ParseException, NamingException
    {
        String value = "( 2.5.6.3 NAME 'locality' SUP top STRUCTURAL MUST (l ) MAY (st $ street $ searchGuide $ seeAlso ) )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.6.3", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "locality", objectClass.getNames().get( 0 ) );
        assertNull( objectClass.getDescription() );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "top", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );
        assertEquals( 1, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( "l", objectClass.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( 4, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "st", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "street", objectClass.getMayAttributeTypeOids().get( 1 ) );
        assertEquals( "searchGuide", objectClass.getMayAttributeTypeOids().get( 2 ) );
        assertEquals( "seeAlso", objectClass.getMayAttributeTypeOids().get( 3 ) );
        assertEquals( 0, objectClass.getExtensions().size() );
    }


    @Test
    public void testMicrosoftAds2003Msieee() throws ParseException, NamingException
    {
        String value = "( 1.2.840.113556.1.5.240 NAME 'msieee80211-Policy' SUP top STRUCTURAL MAY (msieee80211-Data $ msieee80211-DataType $ msieee80211-ID ) )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "1.2.840.113556.1.5.240", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "msieee80211-Policy", objectClass.getNames().get( 0 ) );
        assertNull( objectClass.getDescription() );
        assertEquals( 1, objectClass.getSuperiorOids().size() );
        assertEquals( "top", objectClass.getSuperiorOids().get( 0 ) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );
        assertEquals( 0, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( 3, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "msieee80211-Data", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "msieee80211-DataType", objectClass.getMayAttributeTypeOids().get( 1 ) );
        assertEquals( "msieee80211-ID", objectClass.getMayAttributeTypeOids().get( 2 ) );
        assertEquals( 0, objectClass.getExtensions().size() );
    }


    @Test
    public void testSiemensDirxX500Subschema() throws ParseException, NamingException
    {
        String value = "( 2.5.20.1 NAME 'x500subSchema' AUXILIARY MAY (dITStructureRules $ nameForms $ dITContentRules $ x500objectClasses $ x500attributeTypes $ matchingRules $ matchingRuleUse) )";
        ObjectClass objectClass = parser.parseObjectClassDescription( value );

        assertEquals( "2.5.20.1", objectClass.getOid() );
        assertEquals( 1, objectClass.getNames().size() );
        assertEquals( "x500subSchema", objectClass.getNames().get( 0 ) );
        assertNull( objectClass.getDescription() );
        assertEquals( 0, objectClass.getSuperiorOids().size() );
        assertEquals( ObjectClassTypeEnum.AUXILIARY, objectClass.getType() );
        assertEquals( 0, objectClass.getMustAttributeTypeOids().size() );
        assertEquals( 7, objectClass.getMayAttributeTypeOids().size() );
        assertEquals( "dITStructureRules", objectClass.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "matchingRuleUse", objectClass.getMayAttributeTypeOids().get( 6 ) );
        assertEquals( 0, objectClass.getExtensions().size() );
    }


    /**
     * Tests the multi-threaded use of a single parser.
     */
    @Test
    public void testMultiThreaded() throws ParseException
    {
        String[] testValues = new String[]
            {
                "( 1.1 )",
                "( 2.5.6.0 NAME 'top' DESC 'top of the superclass chain' ABSTRACT MUST objectClass )",
                "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top STRUCTURAL MUST ( sn $ cn ) MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) )",
                "( 2.16.840.1.113719.1.1.6.1.30 NAME 'List' SUP Top STRUCTURAL MUST cn MAY ( description $ l $ member $ ou $ o $ eMailAddress $ mailboxLocation $ mailboxID $ owner $ seeAlso $ fullName ) X-NDS_NAMING 'cn' X-NDS_CONTAINMENT ( 'Organization' 'organizationalUnit' 'domain' ) X-NDS_NOT_CONTAINER '1' X-NDS_NONREMOVABLE '1' X-NDS_ACL_TEMPLATES '2#entry#[Root Template]#member' )" };
        SchemaParserTestUtils.testMultiThreaded( parser, testValues );
    }


    /**
     * Tests quirks mode.
     */
    @Test
    public void testQuirksMode() throws ParseException, NamingException
    {
        SchemaParserTestUtils.testQuirksMode( parser, "" );

        try
        {
            String value = null;
            ObjectClass objectClass = null;

            parser.setQuirksMode( true );

            // ensure all other test pass in quirks mode
            testNumericOid();
            testNames();
            testDescription();
            testObsolete();
            testSuperior();
            testKind();
            testMust();
            testMay();
            testExtensions();
            testFull();
            testUniqueElements();
            testIgnoreElementOrder();
            testRfcTop();
            testRfcSimpleSecurityObject();
            testSunAlias();
            testNovellDcObject();
            testNovellList();
            testMicrosoftAds2000Locality();
            testMicrosoftAds2003Msieee();
            testSiemensDirxX500Subschema();
            testMultiThreaded();

            // NAME with special chars
            value = "( 1.2.3 NAME 't-e_s.t;' )";
            objectClass = parser.parseObjectClassDescription( value );
            assertEquals( 1, objectClass.getNames().size() );
            assertEquals( "t-e_s.t;", objectClass.getNames().get( 0 ) );

            // SUP with underscore
            value = "( 1.1 SUP te_st )";
            objectClass = parser.parseObjectClassDescription( value );
            assertEquals( 1, objectClass.getSuperiorOids().size() );
            assertEquals( "te_st", objectClass.getSuperiorOids().get( 0 ) );

            // MAY with underscore
            value = "( 1.1 MAY te_st )";
            objectClass = parser.parseObjectClassDescription( value );
            assertEquals( 1, objectClass.getMayAttributeTypeOids().size() );
            assertEquals( "te_st", objectClass.getMayAttributeTypeOids().get( 0 ) );

            // MUST with underscore
            value = "( 1.1 MUST te_st )";
            objectClass = parser.parseObjectClassDescription( value );
            assertEquals( 1, objectClass.getMustAttributeTypeOids().size() );
            assertEquals( "te_st", objectClass.getMustAttributeTypeOids().get( 0 ) );

            // Netscape object class 
            value = "( nsAdminGroup-oid NAME 'nsAdminGroup' DESC 'Netscape defined objectclass' SUP top STRUCTURAL MUST cn MAY ( nsAdminGroupName $ description $ nsConfigRoot $ nsAdminSIEDN ) X-ORIGIN 'Netscape' )";
            objectClass = parser.parseObjectClassDescription( value );
            assertEquals( "nsAdminGroup-oid", objectClass.getOid() );
            assertEquals( 1, objectClass.getNames().size() );
            assertEquals( "nsAdminGroup", objectClass.getNames().get( 0 ) );
        }
        finally
        {
            parser.setQuirksMode( false );
        }
    }

}
