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

import junit.framework.TestCase;

import org.apache.directory.shared.ldap.schema.syntax.NormalizerDescription;


/**
 * Tests the NormalizerDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NormalizerDescriptionSchemaParserTest extends TestCase
{
    /** the parser instance */
    private NormalizerDescriptionSchemaParser parser;


    protected void setUp() throws Exception
    {
        parser = new NormalizerDescriptionSchemaParser();
    }


    protected void tearDown() throws Exception
    {
        parser = null;
    }


    public void testNumericOid() throws Exception
    {
        SchemaParserTestUtils.testNumericOid( parser, "FQCN org.apache.directory.SimpleComparator" );
    }


    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils
            .testDescription( parser, "1.1", "FQCN org.apache.directory.SimpleComparator" );
    }


    public void testFqcn() throws ParseException
    {

        String value = null;
        NormalizerDescription nd = null;

        // FQCN simple p
        value = "( 1.1 FQCN org.apache.directory.SimpleComparator )";
        nd = parser.parseNormalizerDescription( value );
        assertNotNull( nd.getFqcn() );
        assertEquals( "org.apache.directory.SimpleComparator", nd.getFqcn() );

    }


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


    public void testExtensions() throws ParseException
    {
        SchemaParserTestUtils.testExtensions( parser, "1.1", "FQCN org.apache.directory.SimpleComparator" );
    }


    public void testFull() throws ParseException
    {
    }


    /**
     * Test unique elements.
     * 
     * @throws ParseException
     */
    public void testUniqueElements()
    {
    }


    /**
     * Test required elements.
     * 
     * @throws ParseException
     */
    public void testRequiredElements() throws ParseException
    {
    }


    /**
     * Tests the multithreaded use of a single parser.
     */
    public void testMultiThreaded() throws Exception
    {
    }

}
