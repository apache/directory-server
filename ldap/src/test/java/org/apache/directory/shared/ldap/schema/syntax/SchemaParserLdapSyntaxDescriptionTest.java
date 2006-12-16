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

import org.apache.directory.shared.ldap.schema.syntax.parser.LdapSyntaxDescriptionSchemaParser;


/**
 * Tests the LdapSyntaxDescriptionSchemaParser class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaParserLdapSyntaxDescriptionTest extends TestCase
{
    /** the parser instance */
    private LdapSyntaxDescriptionSchemaParser parser;


    protected void setUp() throws Exception
    {
        parser = new LdapSyntaxDescriptionSchemaParser();
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
        SchemaParserTestUtils.testNumericOid( parser );
    }


    /**
     * Tests DESC
     * 
     * @throws ParseException
     */
    public void testDescription() throws ParseException
    {
        SchemaParserTestUtils.testDescription( parser );
    }


    /**
     * Test extensions.
     * 
     * @throws ParseException
     */
    public void testExtensions() throws ParseException
    {
        SchemaParserTestUtils.testExtensions( parser );
    }


    /**
     * Test full sytax description.
     * 
     * @throws ParseException
     */
    public void testFull() throws ParseException
    {
        String value = null;
        LdapSyntaxDescription lsd = null;

        value = "( 1.2.3.4.5.6.7.8.9.0 DESC 'Descripton äöüß 部長' X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )";
        lsd = parser.parseLdapSyntaxDescription( value );

        assertEquals( "1.2.3.4.5.6.7.8.9.0", lsd.getNumericOid() );
        assertEquals( "Descripton äöüß 部長", lsd.getDescription() );
        assertEquals( 2, lsd.getExtensions().size() );
        assertNotNull( lsd.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, lsd.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", lsd.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", lsd.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( lsd.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, lsd.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", lsd.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", lsd.getExtensions().get( "X-TEST-b" ).get( 1 ) );

    }


    ////////////////////////////////////////////////////////////////
    //         Some real-world attribute type definitions         //
    ////////////////////////////////////////////////////////////////

    public void testRfcBinary() throws ParseException
    {
        String value = "( 1.3.6.1.4.1.1466.115.121.1.5 DESC 'Binary' X-NOT-HUMAN-READABLE 'TRUE' )";
        LdapSyntaxDescription lsd = parser.parseLdapSyntaxDescription( value );

        assertEquals( "1.3.6.1.4.1.1466.115.121.1.5", lsd.getNumericOid() );
        assertEquals( "Binary", lsd.getDescription() );
        assertEquals( 1, lsd.getExtensions().size() );
        assertNotNull( lsd.getExtensions().get( "X-NOT-HUMAN-READABLE" ) );
        assertEquals( 1, lsd.getExtensions().get( "X-NOT-HUMAN-READABLE" ).size() );
        assertEquals( "TRUE", lsd.getExtensions().get( "X-NOT-HUMAN-READABLE" ).get( 0 ) );
    }


    /**
     * Tests the multithreaded use of a single parser.
     */
    public void testMultiThreaded() throws Exception
    {
        String[] testValues = new String[]
            { 
                "( 1.1 )", 
                "( 1.3.6.1.4.1.1466.115.121.1.36 DESC 'Numeric String' )",
                "( 1.3.6.1.4.1.1466.115.121.1.5 DESC 'Binary' X-NOT-HUMAN-READABLE 'TRUE' )",
                "( 1.2.3.4.5.6.7.8.9.0 DESC 'Descripton äöüß 部長' X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2') )" };
        SchemaParserTestUtils.testMultiThreaded( parser, testValues );
    }

}
