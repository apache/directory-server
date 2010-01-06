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

import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.parsers.NameFormDescriptionSchemaParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


/**
 * Tests the NameFormDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NameFormDescriptionSchemaParserTest
{
    /** the parser instance */
    private NameFormDescriptionSchemaParser parser;
    
    @Before
    public void setUp() throws Exception
    {
        parser = new NameFormDescriptionSchemaParser();
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
        SchemaParserTestUtils.testNumericOid( parser, "OC o MUST m" );
    }


    /**
     * Tests NAME and its values
     * 
     * @throws ParseException
     */
    @Test
    public void testNames() throws ParseException
    {
        SchemaParserTestUtils.testNames( parser, "1.1", "OC o MUST m" );
    }


    /**
     * Tests DESC
     * 
     * @throws ParseException
     */
    @Test
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "OC o MUST m" );
    }


    /**
     * Tests OBSOLETE
     * 
     * @throws ParseException
     */
    @Test
    public void testObsolete() throws ParseException
    {
        SchemaParserTestUtils.testObsolete( parser, "1.1", "OC o MUST m" );
    }


    /**
     * Test OC and its value.
     * 
     * @throws ParseException
     */
    @Test
    public void testOc() throws ParseException, NamingException
    {
        String value = null;
        NameForm nf = null;

        // numeric oid
        value = "( 1.1 MUST m OC 1.2.3.4.5.6.7.8.9.0 )";
        nf = parser.parseNameFormDescription( value );
        assertEquals( "1.2.3.4.5.6.7.8.9.0", nf.getStructuralObjectClassOid() );

        // numeric oid
        value = "(   1.1 MUST m   OC    123.4567.890    )";
        nf = parser.parseNameFormDescription( value );
        assertEquals( "123.4567.890", nf.getStructuralObjectClassOid() );

        // descr
        value = "( 1.1 MUST m OC abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789 )";
        nf = parser.parseNameFormDescription( value );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", nf
            .getStructuralObjectClassOid() );

        // quoted value
        value = "( 1.1 MUST m OC '1.2.3.4.5.6.7.8.9.0' )";
        nf = parser.parseNameFormDescription( value );
        assertEquals( "1.2.3.4.5.6.7.8.9.0", nf.getStructuralObjectClassOid() );

        // quoted value
        value = "( 1.1 MUST m OC 'test' )";
        nf = parser.parseNameFormDescription( value );
        assertEquals( "test", nf.getStructuralObjectClassOid() );

        // invalid character
        value = "( 1.1 MUST m OC 1.2.3.4.A )";
        try
        {
            nf = parser.parseNameFormDescription( value );
            fail( "Exception expected, invalid OC 1.2.3.4.A (invalid character)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no multi value allowed
        value = "( 1.1 MUST m OC ( test1 test2 ) )";
        try
        {
            nf = parser.parseNameFormDescription( value );
            fail( "Exception expected, OC must be single valued" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // OC must only appear once
        value = "( 1.1 MUST m OC test1 OC test2 )";
        try
        {
            nf = parser.parseNameFormDescription( value );
            fail( "Exception expected, OC appears twice" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        if ( !parser.isQuirksMode() )
        {
            // OC is required
            value = "( 1.1 MUST m )";
            try
            {
                nf = parser.parseNameFormDescription( value );
                fail( "Exception expected, OC is required" );
            }
            catch ( ParseException pe )
            {
                // expected
            }

            // invalid start
            value = "( 1.1 MUST m OC -test ) )";
            try
            {
                nf = parser.parseNameFormDescription( value );
                fail( "Exception expected, invalid OC '-test' (starts with hypen)" );
            }
            catch ( ParseException pe )
            {
                // expected
            }
        }
    }


    /**
     * Test MUST and its values.
     * 
     * @throws ParseException
     */
    @Test
    public void testMust() throws ParseException, NamingException
    {
        String value = null;
        NameForm nf = null;

        // MUST simple numericoid
        value = "( 1.1 OC o MUST 1.2.3 )";
        nf = parser.parseNameFormDescription( value );
        assertEquals( 1, nf.getMustAttributeTypeOids().size() );
        assertEquals( "1.2.3", nf.getMustAttributeTypeOids().get( 0 ) );

        // MUST mulitple
        value = "(1.1 OC o MUST (cn$sn       $11.22.33.44.55         $  objectClass   ))";
        nf = parser.parseNameFormDescription( value );
        assertEquals( 4, nf.getMustAttributeTypeOids().size() );
        assertEquals( "cn", nf.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( "sn", nf.getMustAttributeTypeOids().get( 1 ) );
        assertEquals( "11.22.33.44.55", nf.getMustAttributeTypeOids().get( 2 ) );
        assertEquals( "objectClass", nf.getMustAttributeTypeOids().get( 3 ) );

        // no MUST values
        value = "( 1.1 OC o MUST )";
        try
        {
            nf = parser.parseNameFormDescription( value );
            fail( "Exception expected, no MUST value" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // MUST must only appear once
        value = "( 1.1 OC o MUST test1 MUST test2 )";
        try
        {
            nf = parser.parseNameFormDescription( value );
            fail( "Exception expected, MUST appears twice" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        if ( !parser.isQuirksMode() )
        {
            // MUST is required
            value = "( 1.1 OC o )";
            try
            {
                nf = parser.parseNameFormDescription( value );
                fail( "Exception expected, MUST is required" );
            }
            catch ( ParseException pe )
            {
                // expected
            }

            // invalid value
            value = "( 1.1 OC o MUST ( c_n ) )";
            try
            {
                nf = parser.parseNameFormDescription( value );
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
     * 
     * @throws ParseException
     */
    @Test
    public void testMay() throws ParseException, NamingException
    {
        String value = null;
        NameForm nf = null;

        // no MAY
        value = "( 1.1 OC o MUST m )";
        nf = parser.parseNameFormDescription( value );
        assertEquals( 0, nf.getMayAttributeTypeOids().size() );

        // MAY simple numericoid
        value = "( 1.1 OC o MUST m MAY 1.2.3 )";
        nf = parser.parseNameFormDescription( value );
        assertEquals( 1, nf.getMayAttributeTypeOids().size() );
        assertEquals( "1.2.3", nf.getMayAttributeTypeOids().get( 0 ) );

        // MAY mulitple
        value = "(1.1 OC o MUST m MAY (cn$sn       $11.22.33.44.55         $  objectClass   ))";
        nf = parser.parseNameFormDescription( value );
        assertEquals( 4, nf.getMayAttributeTypeOids().size() );
        assertEquals( "cn", nf.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "sn", nf.getMayAttributeTypeOids().get( 1 ) );
        assertEquals( "11.22.33.44.55", nf.getMayAttributeTypeOids().get( 2 ) );
        assertEquals( "objectClass", nf.getMayAttributeTypeOids().get( 3 ) );

        // MAY must only appear once
        value = "( 1.1 OC o MUST m MAY test1 MAY test2 )";
        try
        {
            nf = parser.parseNameFormDescription( value );
            fail( "Exception expected, MAY appears twice" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        if ( !parser.isQuirksMode() )
        {
            // invalid value
            value = "( 1.1 OC o MUST m MAY ( c_n ) )";
            try
            {
                nf = parser.parseNameFormDescription( value );
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
        SchemaParserTestUtils.testExtensions( parser, "1.1", "OC o MUST m" );

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
        NameForm nf = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577' OBSOLETE OC bcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789a MUST ( 3.4.5.6.7.8.9.0.1.2 $ cdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789ab ) MAY ( 4.5.6.7.8.9.0.1.2.3 $ defghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789abc ) X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )";
        nf = parser.parseNameFormDescription( value );

        assertEquals( "1.2.3.4.5.6.7.8.9.0", nf.getOid() );
        assertEquals( 2, nf.getNames().size() );
        assertEquals( "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", nf.getNames().get( 0 ) );
        assertEquals( "test", nf.getNames().get( 1 ) );
        assertEquals( "Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577", nf.getDescription() );
        assertTrue( nf.isObsolete() );
        assertEquals( "bcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789a", nf
            .getStructuralObjectClassOid() );
        assertEquals( 2, nf.getMustAttributeTypeOids().size() );
        assertEquals( "3.4.5.6.7.8.9.0.1.2", nf.getMustAttributeTypeOids().get( 0 ) );
        assertEquals( "cdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789ab", nf.getMustAttributeTypeOids()
            .get( 1 ) );
        assertEquals( 2, nf.getMayAttributeTypeOids().size() );
        assertEquals( "4.5.6.7.8.9.0.1.2.3", nf.getMayAttributeTypeOids().get( 0 ) );
        assertEquals( "defghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789abc", nf.getMayAttributeTypeOids()
            .get( 1 ) );
        assertEquals( 2, nf.getExtensions().size() );
        assertNotNull( nf.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, nf.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", nf.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", nf.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( nf.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, nf.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", nf.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", nf.getExtensions().get( "X-TEST-b" ).get( 1 ) );
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
            { "( 1.1 OC o MUST m NAME 'test1' NAME 'test2' )", "( 1.1 OC o MUST m DESC 'test1' DESC 'test2' )",
                "( 1.1 OC o MUST m OBSOLETE OBSOLETE )", "( 1.1 OC o MUST m OC test1 OC test2 )",
                "( 1.1 OC o MUST m MUST test1 MUST test2 )", "( 1.1 OC o MUST m MAY test1 MAY test2 )",
                "( 1.1 OC o MUST m X-TEST 'test1' X-TEST 'test2' )" };
        SchemaParserTestUtils.testUnique( parser, testValues );
    }


    /**
     * Test required elements.
     * 
     * @throws ParseException
     */
    @Test
    public void testRequiredElements() throws ParseException, NamingException
    {
        String value = null;
        NameForm nf = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 OC o MUST m )";
        nf = parser.parseNameFormDescription( value );
        assertNotNull( nf.getStructuralObjectClassOid() );
        assertEquals( 1, nf.getMustAttributeTypeOids().size() );

        if ( !parser.isQuirksMode() )
        {
            value = "( 1.2.3.4.5.6.7.8.9.0 MUST m )";
            try
            {
                nf = parser.parseNameFormDescription( value );
                fail( "Exception expected, OC is required" );
            }
            catch ( ParseException pe )
            {
                // expected
            }

            value = "( 1.2.3.4.5.6.7.8.9.0 OC o )";
            try
            {
                nf = parser.parseNameFormDescription( value );
                fail( "Exception expected, MUST is required" );
            }
            catch ( ParseException pe )
            {
                // expected
            }
        }
    }


    //    /**
    //     * Test if MUST and MAY are disjoint.
    //     * 
    //     * Problem: What if MUST is a numeric oid and MAY is a name?
    //     * 
    //     * @throws ParseException
    //     */
    //    @Test
    //    public void testDisjoint() throws ParseException
    //    {
    //        String value = null;
    //        NameFormDescription nfd = null;
    //
    //        value = "( 1.2.3.4.5.6.7.8.9.0 OC o MUST test1 MAY test2 )";
    //        nfd = parser.parseNameFormDescription( value );
    //        assertNotNull( nfd.getStructuralObjectClassOid() );
    //        assertEquals( 1, nfd.getMustAttributeTypeOids().size() );
    //
    //        value = "( 1.2.3.4.5.6.7.8.9.0 OC o MUST test1 MAY test1 )";
    //        try
    //        {
    //            nfd = parser.parseNameFormDescription( value );
    //            fail( "Exception expected, MUST and MAY must be disjoint" );
    //        }
    //        catch ( ParseException pe )
    //        {
    //            // expected
    //        }
    //
    //        value = "( 1.2.3.4.5.6.7.8.9.0 OC o MUST ( test1 $ test2 ) MAY ( test4 $ test3 $ test2 ) )";
    //        try
    //        {
    //            nfd = parser.parseNameFormDescription( value );
    //            fail( "Exception expected, MUST and MAY must be disjoint" );
    //        }
    //        catch ( ParseException pe )
    //        {
    //            // expected
    //        }
    //
    //    }

    /**
     * Tests the multithreaded use of a single parser.
     */
    @Test
    public void testMultiThreaded() throws ParseException
    {
        String[] testValues = new String[]
            {
                "( 1.1 OC o MUST m )",
                "( 2.5.15.3 NAME 'orgNameForm' OC organization MUST o )",
                "( 2.5.15.3 NAME 'orgNameForm' OC organization MUST o )",
                "( 1.2.3.4.5.6.7.8.9.0 NAME ( 'abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789' 'test' ) DESC 'Descripton \u00E4\u00F6\u00FC\u00DF \u90E8\u9577' OBSOLETE OC bcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789a MUST ( 3.4.5.6.7.8.9.0.1.2 $ cdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789ab ) MAY ( 4.5.6.7.8.9.0.1.2.3 $ defghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789abc ) X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )" };
        SchemaParserTestUtils.testMultiThreaded( parser, testValues );

    }


    /**
     * Tests quirks mode.
     */
    @Test
    public void testQuirksMode() throws ParseException, NamingException
    {
        SchemaParserTestUtils.testQuirksMode( parser, "OC o MUST m" );

        try
        {
            parser.setQuirksMode( true );

            // ensure all other test pass in quirks mode
            testNumericOid();
            testNames();
            testDescription();
            testObsolete();
            testOc();
            testMust();
            testMay();
            testExtensions();
            testFull();
            testUniqueElements();
            testRequiredElements();
            testMultiThreaded();
        }
        finally
        {
            parser.setQuirksMode( false );
        }
    }

}
