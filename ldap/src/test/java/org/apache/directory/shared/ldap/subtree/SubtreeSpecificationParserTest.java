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

package org.apache.directory.shared.ldap.subtree;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationParser;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


/**
 * Unit tests class for Subtree Specification parser (wrapper).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeSpecificationParserTest
{
    /** A valid empty specification with single white space between brackets */
    private static final String EMPTY_SPEC = "{ }";

    /** A valid specification only with base set */
    private static final String SPEC_WITH_BASE = "{ base \"ou=system\" }";

    /** An invalid specification with missing white space and base set */
    private static final String INVALID_SPEC_WITH_BASE_AND_MISSING_WS = "{ base\"ou=system\"}";

    /** A valid specification with some specific exclusions set */
    private static final String SPEC_WITH_SPECIFICEXCLUSIONS = "{ specificExclusions { chopAfter:\"ef=gh\", chopBefore:\"ab=cd\" } }";

    /** A valid specification with empty specific exclusions set */
    private static final String SPEC_WITH_EMPTY_SPECIFICEXCLUSIONS = "{ specificExclusions { } }";

    /** A valid specification with minimum and maximum set */
    private static final String SPEC_WITH_MINIMUM_AND_MAXIMUM = "{ minimum 1, maximum 2 }";

    /** A valid specification with base and minimum and maximum set */
    private static final String SPEC_WITH_BASE_AND_MINIMUM_AND_MAXIMUM = "{ base \"ou=ORGANIZATION UNIT\", minimum  1, maximum   2 }";

    /**
     * A valid specification with base and specific exclusions and minimum and
     * maximum set
     */
    private static final String SPEC_WITH_BASE_AND_SPECIFICEXCLUSIONS_AND_MINIMUM_AND_MAXIMUM = "{ base \"ou=people\", specificExclusions { chopBefore:\"x=y\""
        + ", chopAfter:\"k=l\", chopBefore:\"y=z\", chopAfter:\"l=m\" }, minimum   7, maximum 77 }";

    /** A valid specification with refinement set */
    private static final String SPEC_WITH_REFINEMENT = "{ base \"ou=system\", specificationFilter and:{ and:{ item:1.2.3"
        + ", or:{ item:4.5.6, item:person-7 } }, not:{ item:10.11.12 } } }";

    /** A valid specification with base and an empty refinement set */
    private static final String SPEC_WITH_BASE_AND_EMPTY_REFINEMENT = "{ base \"ou=system\", specificationFilter and:{ } }";

    /** A valid specification with ALL IN ONE */
    private static final String SPEC_WITH_ALL_IN_ONE = "{ base    \"ou=departments\""
        + ", specificExclusions { chopBefore:\"x=y\", chopAfter:\"k=l\", chopBefore:\"y=z\", chopAfter:\"l=m\" }"
        + ", minimum 7, maximum   77"
        + ", specificationFilter     and:{ and:{ item:1.2.3, or:{ item:4.5.6, item:7.8.9 } }, not:{ item:10.11.12 } } }";

    /** An valid specification with unordinary component order */
    private static final String SPEC_ORDER_OF_COMPONENTS_DOES_NOT_MATTER = "{ base \"ou=system\", minimum 3, specificExclusions { chopBefore:\"x=y\" } }";

    /** An invalid specification with completely unrelated content */
    private static final String INVALID_SILLY_THING = "How much wood would a wood chuck chuck if a wood chuck would chuck wood?";
    
    /** A valid specification with filter expression */
    private static final String SPEC_WITH_FILTER = "{ base \"ou=system\", specificationFilter (&(cn=test)(sn=test)) }";

    
    /** the ss parser wrapper */
    SubtreeSpecificationParser parser;

    /** holds multithreaded success value */
    boolean isSuccessMultithreaded = true;


    /**
     * Creates a SubtreeSpecificationParserTest instance.
     */
    public SubtreeSpecificationParserTest()
    {
        super();
        parser = new SubtreeSpecificationParser( null );
    }


    /**
     * Creates a SubtreeSpecificationParserTest instance.
     */
    public SubtreeSpecificationParserTest(String s)
    {
        parser = new SubtreeSpecificationParser( null );
    }


    /**
     * Tests the parser with a valid empty specification.
     */
    @Test
    public void testEmptySpec() throws Exception
    {
        SubtreeSpecification ss = parser.parse( EMPTY_SPEC );
        assertNotNull( ss );

        // try a second time
        ss = parser.parse( EMPTY_SPEC );
        assertNotNull( ss );

        // try a third time
        ss = parser.parse( EMPTY_SPEC );
        assertNotNull( ss );
    }


    /**
     * Tests the parser with a valid specification with base set.
     */
    @Test
    public void testSpecWithBase() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_BASE );
        assertNotNull( ss );

        assertEquals( "ou=system", ss.getBase().toString() );
    }


    /**
     * Tests the parser with an invalid specification with missing white spaces
     * and base set.
     */
    @Test
    public void testInvalidSpecWithBaseAndMissingWS() throws Exception
    {
        try
        {
            parser.parse( INVALID_SPEC_WITH_BASE_AND_MISSING_WS );
            fail( "testInvalidSpecWithBaseAndMissingWS() should never come here..." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Tests the parser with a valid specification with some specific exclusions
     * set.
     */
    @Test
    public void testSpecWithSpecificExclusions() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_SPECIFICEXCLUSIONS );
        assertFalse( ss.getChopBeforeExclusions().isEmpty() );
        assertFalse( ss.getChopAfterExclusions().isEmpty() );
        assertTrue( ss.getChopBeforeExclusions().contains( new LdapDN( "ab=cd" ) ) );
        assertTrue( ss.getChopAfterExclusions().contains( new LdapDN( "ef=gh" ) ) );

        // try a second time
        ss = parser.parse( SPEC_WITH_SPECIFICEXCLUSIONS );
        assertFalse( ss.getChopBeforeExclusions().isEmpty() );
        assertFalse( ss.getChopAfterExclusions().isEmpty() );
        assertTrue( ss.getChopBeforeExclusions().contains( new LdapDN( "ab=cd" ) ) );
        assertTrue( ss.getChopAfterExclusions().contains( new LdapDN( "ef=gh" ) ) );

        // try a third time
        ss = parser.parse( SPEC_WITH_SPECIFICEXCLUSIONS );
        assertFalse( ss.getChopBeforeExclusions().isEmpty() );
        assertFalse( ss.getChopAfterExclusions().isEmpty() );
        assertTrue( ss.getChopBeforeExclusions().contains( new LdapDN( "ab=cd" ) ) );
        assertTrue( ss.getChopAfterExclusions().contains( new LdapDN( "ef=gh" ) ) );
    }


    /**
     * Tests the parser with a valid specification with an empty specific
     * exclusions set.
     */
    @Test
    public void testSpecWithEmptySpecificExclusions() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_EMPTY_SPECIFICEXCLUSIONS );
        assertNotNull( ss );

        assertTrue( ss.getChopBeforeExclusions().isEmpty() );
    }


    /**
     * Tests the parser with a valid specification with minimum and maximum set.
     */
    @Test
    public void testSpecWithMinimumAndMaximum() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_MINIMUM_AND_MAXIMUM );
        assertEquals( 1, ss.getMinBaseDistance() );
        assertEquals( 2, ss.getMaxBaseDistance() );

        // try a second time
        ss = parser.parse( SPEC_WITH_MINIMUM_AND_MAXIMUM );
        assertEquals( 1, ss.getMinBaseDistance() );
        assertEquals( 2, ss.getMaxBaseDistance() );

        // try a third time
        ss = parser.parse( SPEC_WITH_MINIMUM_AND_MAXIMUM );
        assertEquals( 1, ss.getMinBaseDistance() );
        assertEquals( 2, ss.getMaxBaseDistance() );
    }


    /**
     * Tests the parser with a valid specification with base and minimum and
     * maximum set.
     */
    @Test
    public void testWithBaseAndMinimumAndMaximum() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_BASE_AND_MINIMUM_AND_MAXIMUM );

        assertEquals( new LdapDN( "ou=ORGANIZATION UNIT" ), ss.getBase() );
        assertEquals( 1, ss.getMinBaseDistance() );
        assertEquals( 2, ss.getMaxBaseDistance() );
    }


    /**
     * Tests the parser with a valid specification with base and specific
     * exclusions and minimum and maximum set.
     */
    @Test
    public void testSpecWithBaseAndSpecificExclusionsAndMinimumAndMaximum() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_BASE_AND_SPECIFICEXCLUSIONS_AND_MINIMUM_AND_MAXIMUM );
        assertNotNull( ss );

        assertEquals( "ou=people", ss.getBase().toString() );
        assertTrue( ss.getChopBeforeExclusions().contains( new LdapDN( "x=y" ) ) );
        assertTrue( ss.getChopBeforeExclusions().contains( new LdapDN( "y=z" ) ) );
        assertTrue( ss.getChopAfterExclusions().contains( new LdapDN( "k=l" ) ) );
        assertTrue( ss.getChopAfterExclusions().contains( new LdapDN( "l=m" ) ) );
        assertEquals( 7, ss.getMinBaseDistance() );
        assertEquals( 77, ss.getMaxBaseDistance() );
    }


    /**
     * Tests the parser with a valid specification with refinement set.
     */
    @Test
    public void testSpecWithRefinement() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_REFINEMENT );

        SimpleNode<String> n1 = new EqualityNode<String>( "objectClass", new ClientStringValue( "1.2.3" ) );
        SimpleNode<String> n2 = new EqualityNode<String>( "objectClass", new ClientStringValue( "4.5.6" ) );
        SimpleNode<String> n3 = new EqualityNode<String>( "objectClass", new ClientStringValue( "person-7" ) );
        BranchNode n4 = new OrNode();
        n4.addNode( n2 );
        n4.addNode( n3 );
        BranchNode n5 = new AndNode();
        n5.addNode( n1 );
        n5.addNode( n4 );
        SimpleNode<String> n6 = new EqualityNode<String>( "objectClass", new ClientStringValue( "10.11.12" ) );
        BranchNode n7 = new NotNode();
        n7.addNode( n6 );
        BranchNode n8 = new AndNode();
        n8.addNode( n5 );
        n8.addNode( n7 );

        assertEquals( n8, ss.getRefinement() );
    }


    /**
     * Tests the parser with a valid specification with base and empty
     * refinement set.
     */
    @Test
    public void testSpecWithBaseAndEmptyRefinement() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_BASE_AND_EMPTY_REFINEMENT );

        assertEquals( "ou=system", ss.getBase().toString() );
    }


    /**
     * Tests the parser with a valid specification with all components set.
     */
    @Test
    public void testSpecWithAllInOne() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_ALL_IN_ONE );
        assertNotNull( ss );
    }


    /**
     * Tests the parser with a valid specification with unordinary component
     * order.
     */
    @Test
    public void testSpecOrderOfComponentsDoesNotMatter() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_ORDER_OF_COMPONENTS_DOES_NOT_MATTER );
        assertNotNull( ss );
    }


    /**
     * Tests the parser with an invalid specification with silly things in.
     */
    @Test
    public void testInvalidSillyThing() throws Exception
    {
        try
        {
            parser.parse( INVALID_SILLY_THING );
            fail( "testInvalidSillyThing() should never come here..." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }
    
    
    /**
     * Tests the parser with a valid specification with refinement set.
     */
    @Test
    public void testSpecWithFilter() throws Exception
    {
        SubtreeSpecification ss = parser.parse( SPEC_WITH_FILTER );

        ExprNode filter = FilterParser.parse( "(&(cn=test)(sn=test))" );

        assertEquals( filter, ss.getRefinement() );
    }


    /**
     * Test reusability, especially if the state is resetted.
     */
    @Test
    public void testReusabiltiy() throws Exception
    {
        LdapDN firstDN = new LdapDN("k=l");
        String firstExclusion = "{ specificExclusions { chopAfter:\"k=l\" } }";
        SubtreeSpecification firstSpec = parser.parse( firstExclusion );
        assertEquals( 1, firstSpec.getChopAfterExclusions().size() );
        assertEquals( firstDN, firstSpec.getChopAfterExclusions().iterator().next() );

        LdapDN secondDN = new LdapDN("x=y");
        String secondExclusion = "{ specificExclusions { chopAfter:\"x=y\" } }";
        SubtreeSpecification secondSpec = parser.parse( secondExclusion );
        assertEquals( 1, secondSpec.getChopAfterExclusions().size() );
        assertEquals( secondDN, secondSpec.getChopAfterExclusions().iterator().next() );

    }


    /**
     * Tests the multithreaded use of a single parser.
     */
    @Test
    public void testMultiThreaded() throws Exception
    {
        // start up and track all threads (40 threads)
        List<Thread> threads = new ArrayList<Thread>();
        for ( int ii = 0; ii < 10; ii++ )
        {
            Thread t0 = new Thread( new ParseSpecification( EMPTY_SPEC ) );
            Thread t1 = new Thread( new ParseSpecification( SPEC_WITH_SPECIFICEXCLUSIONS ) );
            Thread t2 = new Thread( new ParseSpecification( SPEC_WITH_MINIMUM_AND_MAXIMUM ) );
            Thread t3 = new Thread( new ParseSpecification( SPEC_WITH_ALL_IN_ONE ) );
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
                Thread t = threads.get( ii );
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
        private final String specStr;

        SubtreeSpecification result;


        public ParseSpecification(String specStr)
        {
            this.specStr = specStr;
        }


        public void run()
        {
            try
            {
                result = parser.parse( specStr );
            }
            catch ( ParseException e )
            {
                e.printStackTrace();
            }

            isSuccessMultithreaded = isSuccessMultithreaded && ( result != null );
        }
    }
}
