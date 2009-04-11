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


import org.apache.directory.shared.ldap.schema.syntaxes.DITStructureRuleDescriptionSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test cases for DITStructureRuleDescriptionSyntaxChecker.
 * 
 * There are also many test cases in SchemaParserDITStructureRuleDescriptionTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DITStructureRuleDescriptionSyntaxCheckerTest
{
    private DITStructureRuleDescriptionSyntaxChecker checker = new DITStructureRuleDescriptionSyntaxChecker();


    @Test
    public void testValid()
    {
        assertTrue( checker.isValidSyntax( "( 2 FORM 2.5.15.3 )" ) );
        assertTrue( checker.isValidSyntax( "( 2 NAME 'organization' FORM 2.5.15.3 )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2 NAME 'organization' DESC 'organization structure rule' FORM 2.5.15.3 )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2 NAME 'organization' DESC 'organization structure rule' OBSOLETE FORM 2.5.15.3 )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2 NAME 'organization' DESC 'organization structure rule' OBSOLETE FORM 2.5.15.3 SUP 1 )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2 NAME 'organization' DESC 'organization structure rule' OBSOLETE FORM 2.5.15.3 SUP ( 1 ) )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2 NAME 'organization' DESC 'organization structure rule' OBSOLETE FORM 2.5.15.3 SUP ( 1 1234567890 5 ) )" ) );

        assertTrue( checker.isValidSyntax( "(2 FORM 2.5.15.3)" ) );
        assertTrue( checker.isValidSyntax( "(2 NAME organization FORM 2.5.15.3)" ) );
        assertTrue( checker
            .isValidSyntax( "(   2   NAME    'organization'    DESC    'organization structure rule'    OBSOLETE   FORM   2.5.15.3    SUP   (1 1234567890        5   ))" ) );

        // lowercase NAME, DESC, FORM
        assertTrue( checker
            .isValidSyntax( "( 2 name 'organization' desc 'organization structure rule' form 2.5.15.3 )" ) );
    }


    @Test
    public void testInvalid()
    {
        // null 
        assertFalse( checker.isValidSyntax( null ) );

        // empty 
        assertFalse( checker.isValidSyntax( "" ) );

        // missing/invalid ruleid
        assertFalse( checker.isValidSyntax( "()" ) );
        assertFalse( checker.isValidSyntax( "(  )" ) );
        assertFalse( checker.isValidSyntax( "( . )" ) );
        assertFalse( checker.isValidSyntax( "( 1 )" ) );
        assertFalse( checker.isValidSyntax( "( 1.2 )" ) );
        assertFalse( checker.isValidSyntax( "( A )" ) );
        assertFalse( checker.isValidSyntax( "( A.B )" ) );

        // missing right parenthesis
        assertFalse( checker.isValidSyntax( "( 2 FORM 2.5.15.3" ) );

        // missing FORM
        assertFalse( checker.isValidSyntax( "( 2 NAME 'organization' DESC 'organization structure rule' )" ) );
    }

}
