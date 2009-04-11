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

import org.apache.directory.shared.ldap.schema.syntaxes.SubtreeSpecificationSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for SubtreeSpecificationSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeSpecificationSyntaxCheckerTest
{
    SubtreeSpecificationSyntaxChecker checker = new SubtreeSpecificationSyntaxChecker();


    @Test
    public void testNullString()
    {
        assertFalse( checker.isValidSyntax( null ) );
    }


    @Test
    public void testEmptyString()
    {
        assertFalse( checker.isValidSyntax( "" ) );
    }

    @Test
    public void testOid()
    {
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.45", checker.getSyntaxOid() );
    }

    @Test
    public void testCorrectCase()
    {
    }
    
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
    
    /**
     * Tests the parser with a valid empty specification.
     */
    @Test
    public void testEmptySpec() throws Exception
    {
        assertTrue( checker.isValidSyntax( EMPTY_SPEC ) );
       
        // try a second time
        assertTrue( checker.isValidSyntax( EMPTY_SPEC ) );

        // try a third time
        assertTrue( checker.isValidSyntax( EMPTY_SPEC ) );
    }


    /**
     * Tests the parser with a valid specification with base set.
     */
    @Test
    public void testSpecWithBase() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_BASE ) );
    }


    /**
     * Tests the parser with an invalid specification with missing white spaces
     * and base set.
     */
    @Test
    public void testInvalidSpecWithBaseAndMissingWS() throws Exception
    {
        assertFalse( checker.isValidSyntax( INVALID_SPEC_WITH_BASE_AND_MISSING_WS ) );
    }


    /**
     * Tests the parser with a valid specification with some specific exclusions
     * set.
     */
    @Test
    public void testSpecWithSpecificExclusions() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_SPECIFICEXCLUSIONS ) );
    }


    /**
     * Tests the parser with a valid specification with an empty specific
     * exclusions set.
     */
    @Test
    public void testSpecWithEmptySpecificExclusions() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_EMPTY_SPECIFICEXCLUSIONS ) );
    }


    /**
     * Tests the parser with a valid specification with minimum and maximum set.
     */
    @Test
    public void testSpecWithMinimumAndMaximum() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_MINIMUM_AND_MAXIMUM ) );
    }


    /**
     * Tests the parser with a valid specification with base and minimum and
     * maximum set.
     */
    @Test
    public void testWithBaseAndMinimumAndMaximum() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_BASE_AND_MINIMUM_AND_MAXIMUM ) );
    }


    /**
     * Tests the parser with a valid specification with base and specific
     * exclusions and minimum and maximum set.
     */
    @Test
    public void testSpecWithBaseAndSpecificExclusionsAndMinimumAndMaximum() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_BASE_AND_SPECIFICEXCLUSIONS_AND_MINIMUM_AND_MAXIMUM ) );
    }


    /**
     * Tests the parser with a valid specification with refinement set.
     */
    @Test
    public void testSpecWithRefinement() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_REFINEMENT ) );
    }


    /**
     * Tests the parser with a valid specification with base and empty
     * refinement set.
     */
    @Test
    public void testSpecWithBaseAndEmptyRefinement() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_BASE_AND_EMPTY_REFINEMENT ) );
    }


    /**
     * Tests the parser with a valid specification with all components set.
     */
    @Test
    public void testSpecWithAllInOne() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_ALL_IN_ONE ) );

    }


    /**
     * Tests the parser with a valid specification with unordinary component
     * order.
     */
    @Test
    public void testSpecOrderOfComponentsDoesNotMatter() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_ORDER_OF_COMPONENTS_DOES_NOT_MATTER ) );
    }

    /**
     * Tests the parser with a valid specification with unordinary component
     * order.
     */
    @Test
    public void testBadAssertion() throws Exception
    {
        assertFalse( checker.isValidSyntax( INVALID_SILLY_THING ) );
    }
    
    
    /**
     * Tests the parser with a valid specification with refinement set.
     */
    @Test
    public void testSpecWithFilter() throws Exception
    {
        assertTrue( checker.isValidSyntax( SPEC_WITH_FILTER ) );
    }
}
