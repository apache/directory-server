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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.ParseException;

import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescriptionSchemaParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests the SyntaxCheckerDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SyntaxCheckerDescriptionSchemaParserTest
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.2.10000";
    private static final String FQCN = "org.foo.Bar";
    private static final String DESC = "bogus desc";
    private static final String BYTECODE = "14561234";

    /** the parser instance */
    private SyntaxCheckerDescriptionSchemaParser parser;


    @Before
    public void setUp() throws Exception
    {
        parser = new SyntaxCheckerDescriptionSchemaParser();
    }


    @After
    public void tearDown() throws Exception
    {
        parser = null;
    }


    @Test
    public void testNumericOid() throws ParseException
    {
        SchemaParserTestUtils.testNumericOid( parser, "FQCN org.apache.directory.SimpleSyntaxChecker" );
    }


    @Test
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "FQCN org.apache.directory.SimpleSyntaxChecker" );
    }


    @Test
    public void testFqcn() throws ParseException
    {
        String value = null;
        SyntaxCheckerDescription syntaxCheckerDescription = null;

        // FQCN simple p
        value = "( 1.1 FQCN org.apache.directory.SimpleSyntaxChecker )";
        syntaxCheckerDescription = parser.parseSyntaxCheckerDescription( value );
        assertNotNull( syntaxCheckerDescription.getFqcn() );
        assertEquals( "org.apache.directory.SimpleSyntaxChecker", syntaxCheckerDescription.getFqcn() );
    }


    @Test
    public void testBytecode() throws ParseException
    {
        String value = null;
        SyntaxCheckerDescription syntaxCheckerDescription = null;

        // FQCN simple p
        value = "( 1.1 FQCN org.apache.directory.SimpleSyntaxChecker BYTECODE ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        syntaxCheckerDescription = parser.parseSyntaxCheckerDescription( value );
        assertNotNull( syntaxCheckerDescription.getBytecode() );
        assertEquals( "ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789====", 
        		syntaxCheckerDescription.getBytecode() );
    }


    @Test
    public void testExtensions() throws ParseException
    {
        SchemaParserTestUtils.testExtensions( parser, "1.1", "FQCN org.apache.directory.SimpleSyntaxChecker" );
    }


    @Test
    public void testFull()
    {
        // TODO
    }


    /**
     * Test unique elements.
     * 
     * @throws ParseException
     */
    @Test
    public void testUniqueElements()
    {
        // TODO
    }


    /**
     * Test required elements.
     * 
     * @throws ParseException
     */
    @Test
    public void testRequiredElements()
    {
        // TODO
    }


    @Test
    public void testSimpleSyntaxChecker() throws ParseException
    {
        String simple = "( " + OID + " FQCN " + FQCN + " )";
        SyntaxCheckerDescription syntaxCheckerDescription = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( syntaxCheckerDescription );
        assertEquals( OID, syntaxCheckerDescription.getOid() );
        assertEquals( FQCN, syntaxCheckerDescription.getFqcn() );
        assertNull( syntaxCheckerDescription.getBytecode() );
        assertNull( syntaxCheckerDescription.getDescription() );
    }


    @Test
    public void testSyntaxCheckerWithDesc() throws ParseException
    {
        String simple = "( " + OID + " DESC '" + DESC + "' FQCN " + FQCN + " )";
        SyntaxCheckerDescription syntaxCheckerDescription = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( syntaxCheckerDescription );
        assertEquals( OID, syntaxCheckerDescription.getOid() );
        assertEquals( FQCN, syntaxCheckerDescription.getFqcn() );
        assertNull( syntaxCheckerDescription.getBytecode() );
        assertEquals( DESC, syntaxCheckerDescription.getDescription() );
    }


    @Test
    public void testSyntaxCheckerWithDescAndByteCode() throws ParseException
    {
        String simple = "( " + OID + " DESC '" + DESC + "' FQCN " + FQCN + " BYTECODE " + BYTECODE + " )";
        SyntaxCheckerDescription syntaxCheckerDescription = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( syntaxCheckerDescription );
        assertEquals( OID, syntaxCheckerDescription.getOid() );
        assertEquals( FQCN, syntaxCheckerDescription.getFqcn() );
        assertEquals( BYTECODE, syntaxCheckerDescription.getBytecode() );
        assertEquals( DESC, syntaxCheckerDescription.getDescription() );
    }


    @Test
    public void testSyntaxCheckerExample() throws ParseException
    {
        String simple = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.OctetStringSyntaxChecker )";
        SyntaxCheckerDescription syntaxCheckerDescription = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( syntaxCheckerDescription );
    }


    @Test
    public void testRealByteCodeExample() throws ParseException
    {
        String simple = "( 1.3.6.1.4.1.18060.0.4.1.0.10002 DESC 'bogus desc' "
            + "FQCN DummySyntaxChecker BYTECODE yv66vgAAADEAHgoABAAYCQADABkHABoHABsHABwBAANvaWQBABJMam"
            + "F2YS9sYW5nL1N0cmluZzsBAAY8aW5pdD4BABUoTGphdmEvbGFuZy9TdHJpbmc7KVYBAARDb2RlAQAPTGluZU51b"
            + "WJlclRhYmxlAQADKClWAQAMc2V0U3ludGF4T2lkAQAMZ2V0U3ludGF4T2lkAQAUKClMamF2YS9sYW5nL1N0cmlu"
            + "ZzsBAA1pc1ZhbGlkU3ludGF4AQAVKExqYXZhL2xhbmcvT2JqZWN0OylaAQAMYXNzZXJ0U3ludGF4AQAVKExqYXZ"
            + "hL2xhbmcvT2JqZWN0OylWAQAKRXhjZXB0aW9ucwcAHQEAClNvdXJjZUZpbGUBABdEdW1teVN5bnRheENoZWNrZX"
            + "IuamF2YQwACAAMDAAGAAcBABJEdW1teVN5bnRheENoZWNrZXIBABBqYXZhL2xhbmcvT2JqZWN0AQA8b3JnL2FwY"
            + "WNoZS9kaXJlY3Rvcnkvc2hhcmVkL2xkYXAvc2NoZW1hL3N5bnRheC9TeW50YXhDaGVja2VyAQAcamF2YXgvbmFt"
            + "aW5nL05hbWluZ0V4Y2VwdGlvbgAhAAMABAABAAUAAQACAAYABwAAAAYAAQAIAAkAAQAKAAAAKgACAAIAAAAKKrc"
            + "AASortQACsQAAAAEACwAAAA4AAwAAAAsABAAMAAkADQABAAgADAABAAoAAAAhAAEAAQAAAAUqtwABsQAAAAEACw"
            + "AAAAoAAgAAABEABAASAAEADQAJAAEACgAAACIAAgACAAAABiortQACsQAAAAEACwAAAAoAAgAAABcABQAYAAEAD"
            + "gAPAAEACgAAAB0AAQABAAAABSq0AAKwAAAAAQALAAAABgABAAAAHQABABAAEQABAAoAAAAaAAEAAgAAAAIErAAA"
            + "AAEACwAAAAYAAQAAACMAAQASABMAAgAKAAAAGQAAAAIAAAABsQAAAAEACwAAAAYAAQAAACkAFAAAAAQAAQAVAAE"
            + "AFgAAAAIAFw== X-SCHEMA 'nis' )";
        SyntaxCheckerDescription syntaxCheckerDescription = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( syntaxCheckerDescription );
        assertEquals( "1.3.6.1.4.1.18060.0.4.1.0.10002", syntaxCheckerDescription.getOid() );
        assertEquals( "DummySyntaxChecker", syntaxCheckerDescription.getFqcn() );
        assertNotNull( syntaxCheckerDescription.getBytecode() );
        assertEquals( "bogus desc", syntaxCheckerDescription.getDescription() );
    }


    /**
     * Tests the multithreaded use of a single parser.
     */
    @Test
    public void testMultiThreaded() throws ParseException
    {
        // TODO
    }


    /**
     * Tests quirks mode.
     */
    @Test
    public void testQuirksMode() throws ParseException
    {
        SchemaParserTestUtils.testQuirksMode( parser, "FQCN org.apache.directory.SimpleComparator" );

        try
        {
            parser.setQuirksMode( true );

            // ensure all other test pass in quirks mode
            testNumericOid();
            testDescription();
            testFqcn();
            testBytecode();
            testExtensions();
            testFull();
            testUniqueElements();
            testSimpleSyntaxChecker();
            testSyntaxCheckerWithDesc();
            testSyntaxCheckerWithDescAndByteCode();
            testSyntaxCheckerExample();
            testRealByteCodeExample();
            testMultiThreaded();
        }
        finally
        {
            parser.setQuirksMode( false );
        }
    }

}
