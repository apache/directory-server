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

import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescriptionSchemaParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Tests the NormalizerDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NormalizerDescriptionSchemaParserTest
{
    /** the parser instance */
    private NormalizerDescriptionSchemaParser parser;


    @Before
    public void setUp() throws Exception
    {
        parser = new NormalizerDescriptionSchemaParser();
    }


    @After
    public void tearDown() throws Exception
    {
        parser = null;
    }


    @Test
    public void testNumericOid() throws ParseException
    {
        SchemaParserTestUtils.testNumericOid( parser, "FQCN org.apache.directory.SimpleNormalizer" );
    }


    @Test
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser, "1.1", "FQCN org.apache.directory.SimpleNormalizer" );
    }


    @Test
    public void testFqcn() throws ParseException
    {
        String value = null;
        NormalizerDescription nd = null;

        // FQCN simple p
        value = "( 1.1 FQCN org.apache.directory.SimpleNormalizer )";
        nd = parser.parseNormalizerDescription( value );
        assertNotNull( nd.getFqcn() );
        assertEquals( "org.apache.directory.SimpleNormalizer", nd.getFqcn() );
    }


    @Test
    public void testBytecode() throws ParseException
    {
        String value = null;
        NormalizerDescription nd = null;

        // FQCN simple p
        value = "( 1.1 FQCN org.apache.directory.SimpleComparator BYTECODE ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        nd = parser.parseNormalizerDescription( value );
        assertNotNull( nd.getBytecode() );
        assertEquals( "ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789====", nd.getBytecode() );
    }


    @Test
    public void testExtensions() throws ParseException
    {
        SchemaParserTestUtils.testExtensions( parser, "1.1", "FQCN org.apache.directory.SimpleNormalizer" );
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
        SchemaParserTestUtils.testQuirksMode( parser, "FQCN org.apache.directory.SimpleNormalizer" );

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
            testRequiredElements();
            testMultiThreaded();
        }
        finally
        {
            parser.setQuirksMode( false );
        }
    }
}
