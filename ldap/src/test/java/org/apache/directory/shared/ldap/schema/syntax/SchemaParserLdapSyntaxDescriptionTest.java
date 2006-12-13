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

    /** holds multithreaded success value */
    boolean isSuccessMultithreaded = true;


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
        String value = null;
        LdapSyntaxDescription lsd = null;

        // null test
        value = null;
        try
        {
            parser.parseLdapSyntaxDescription( value );
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
            parser.parseLdapSyntaxDescription( value );
            fail( "Exception expected, no NUMERICOID" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // simple
        value = "( 1.1 )";
        lsd = parser.parseLdapSyntaxDescription( value );
        assertEquals( "1.1", lsd.getNumericOid() );

        // simple with spaces
        value = "(          1.1          )";
        lsd = parser.parseLdapSyntaxDescription( value );
        assertEquals( "1.1", lsd.getNumericOid() );

        // non-numeric not allowed
        value = "( cn )";
        try
        {
            parser.parseLdapSyntaxDescription( value );
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
            parser.parseLdapSyntaxDescription( value );
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
            parser.parseLdapSyntaxDescription( value );
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
            parser.parseLdapSyntaxDescription( value );
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
            parser.parseLdapSyntaxDescription( value );
            fail( "Exception expected, invalid NUMERICOID '1.1' (quoted)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // leading 0 not allowed
        value = "( 01.1 )";
        try
        {
            parser.parseLdapSyntaxDescription( value );
            fail( "Exception expected, invalid NUMERICOID 01.1 (leading zero)" );
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
        LdapSyntaxDescription lsd = null;

        // simple
        value = "(1.1 DESC 'Descripton')";
        lsd = parser.parseLdapSyntaxDescription( value );
        assertEquals( "Descripton", lsd.getDescription() );

        // unicode
        value = "( 1.1 DESC 'Descripton äöüß 部長' )";
        lsd = parser.parseLdapSyntaxDescription( value );
        assertEquals( "Descripton äöüß 部長", lsd.getDescription() );

        // lowercase
        value = "( 1.1 desc 'Descripton' )";
        try
        {
            parser.parseLdapSyntaxDescription( value );
            fail( "Exception expected, DESC is lowercase" );
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
        LdapSyntaxDescription lsd = null;

        // no extension
        value = "( 1.1 )";
        lsd = parser.parseLdapSyntaxDescription( value );
        assertEquals( 0, lsd.getExtensions().size() );

        // single extension with one value
        value = "( 1.1 X-TEST 'test' )";
        lsd = parser.parseLdapSyntaxDescription( value );
        assertEquals( 1, lsd.getExtensions().size() );
        assertNotNull( lsd.getExtensions().get( "X-TEST" ) );
        assertEquals( 1, lsd.getExtensions().get( "X-TEST" ).size() );
        assertEquals( "test", lsd.getExtensions().get( "X-TEST" ).get( 0 ) );

        // single extension with multiple values
        value = "( 1.1 X-TEST-ABC ('test1' 'test äöüß'       'test 部長' ) )";
        lsd = parser.parseLdapSyntaxDescription( value );
        assertEquals( 1, lsd.getExtensions().size() );
        assertNotNull( lsd.getExtensions().get( "X-TEST-ABC" ) );
        assertEquals( 3, lsd.getExtensions().get( "X-TEST-ABC" ).size() );
        assertEquals( "test1", lsd.getExtensions().get( "X-TEST-ABC" ).get( 0 ) );
        assertEquals( "test äöüß", lsd.getExtensions().get( "X-TEST-ABC" ).get( 1 ) );
        assertEquals( "test 部長", lsd.getExtensions().get( "X-TEST-ABC" ).get( 2 ) );

        // multiple extensions
        value = "(1.1 X-TEST-a ('test1-1' 'test1-2') X-TEST-b ('test2-1' 'test2-2'))";
        lsd = parser.parseLdapSyntaxDescription( value );
        assertEquals( 2, lsd.getExtensions().size() );
        assertNotNull( lsd.getExtensions().get( "X-TEST-a" ) );
        assertEquals( 2, lsd.getExtensions().get( "X-TEST-a" ).size() );
        assertEquals( "test1-1", lsd.getExtensions().get( "X-TEST-a" ).get( 0 ) );
        assertEquals( "test1-2", lsd.getExtensions().get( "X-TEST-a" ).get( 1 ) );
        assertNotNull( lsd.getExtensions().get( "X-TEST-b" ) );
        assertEquals( 2, lsd.getExtensions().get( "X-TEST-b" ).size() );
        assertEquals( "test2-1", lsd.getExtensions().get( "X-TEST-b" ).get( 0 ) );
        assertEquals( "test2-2", lsd.getExtensions().get( "X-TEST-b" ).get( 1 ) );

        // invalid extension, no number allowed
        value = "( 1.1 X-TEST1 'test' )";
        try
        {
            lsd = parser.parseLdapSyntaxDescription( value );
            fail( "Exception expected, invalid extension X-TEST1 (no number allowed)" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

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
        // start up and track all threads (40 threads)
        List<Thread> threads = new ArrayList<Thread>();
        for ( int ii = 0; ii < 10; ii++ )
        {
            Thread t0 = new Thread( new ParseSpecification( "( 1.1 )" ) );
            Thread t1 = new Thread( new ParseSpecification(
                "( 1.3.6.1.4.1.1466.115.121.1.5 DESC 'Binary' X-NOT-HUMAN-READABLE 'TRUE' )" ) );
            Thread t2 = new Thread( new ParseSpecification( "( 1.3.6.1.4.1.1466.115.121.1.7 DESC 'Boolean' )" ) );
            Thread t3 = new Thread( new ParseSpecification( "( 1.3.6.1.4.1.1466.115.121.1.36 DESC 'Numeric String' )" ) );
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
        private final String lsd;

        LdapSyntaxDescription result;


        public ParseSpecification( String lsd )
        {
            this.lsd = lsd;
        }


        public void run()
        {
            try
            {
                result = parser.parseLdapSyntaxDescription( lsd );
            }
            catch ( ParseException e )
            {
                e.printStackTrace();
            }

            isSuccessMultithreaded = isSuccessMultithreaded && ( result != null );
        }
    }

}
