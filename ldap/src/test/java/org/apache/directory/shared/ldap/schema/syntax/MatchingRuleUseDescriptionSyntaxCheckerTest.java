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


import org.apache.directory.shared.ldap.schema.syntaxes.MatchingRuleUseDescriptionSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test cases for MatchingRuleUseDescriptionSyntaxChecker.
 * 
 * There are also many test cases in SchemaParserMatchingRuleUseDescriptionTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MatchingRuleUseDescriptionSyntaxCheckerTest
{
    private MatchingRuleUseDescriptionSyntaxChecker checker = new MatchingRuleUseDescriptionSyntaxChecker();


    @Test
    public void testValid()
    {
        assertTrue( checker.isValidSyntax( ( "( 2.5.13.17 APPLIES userPassword )" ) ) );
        assertTrue( checker.isValidSyntax( ( "( 2.5.13.17 APPLIES ( javaSerializedData $ userPassword ) )" ) ) );
        assertTrue( checker
            .isValidSyntax( ( "( 2.5.13.17 NAME 'octetStringMatch' APPLIES ( javaSerializedData $ userPassword ) )" ) ) );
        assertTrue( checker
            .isValidSyntax( ( "( 2.5.13.17 NAME 'octetStringMatch' DESC 'octetStringMatch' APPLIES ( javaSerializedData $ userPassword ) )" ) ) );
        assertTrue( checker
            .isValidSyntax( ( "( 2.5.13.17 NAME 'octetStringMatch' DESC 'octetStringMatch' APPLIES ( javaSerializedData $ userPassword ) X-ABC-DEF 'test' )" ) ) );

        // spaces
        assertTrue( checker.isValidSyntax( ( "(2.5.13.17 APPLIES userPassword)" ) ) );
        assertTrue( checker
            .isValidSyntax( ( "(   2.5.13.17   NAME   'octetStringMatch'   DESC   'octetStringMatch'   APPLIES   (javaSerializedData   $    userPassword)  X-ABC-DEF     'test'   )" ) ) );

        // lowercase DESC
        assertTrue( checker.isValidSyntax( "( 2.5.13.17 desc 'Directory String' APPLIES userPassword )" ) );
    }


    @Test
    public void testInvalid()
    {
        // null 
        assertFalse( checker.isValidSyntax( null ) );

        // empty 
        assertFalse( checker.isValidSyntax( "" ) );

        // missing/invalid OID
        assertFalse( checker.isValidSyntax( "()" ) );
        assertFalse( checker.isValidSyntax( "(  )" ) );
        assertFalse( checker.isValidSyntax( "( . )" ) );
        assertFalse( checker.isValidSyntax( "( 1 )" ) );
        assertFalse( checker.isValidSyntax( "( 1. )" ) );
        assertFalse( checker.isValidSyntax( "( 1.2. )" ) );
        assertFalse( checker.isValidSyntax( "( 1.A )" ) );
        assertFalse( checker.isValidSyntax( "( A.B )" ) );

        // missing right parenthesis
        assertFalse( checker.isValidSyntax( "( 2.5.13.17 APPLIES userPassword " ) );

        // missing quotes
        assertFalse( checker.isValidSyntax( "( 2.5.13.17 DESC Directory String APPLIES userPassword )" ) );

        // invalid extension
        assertFalse( checker.isValidSyntax( "( 2.5.13.17 APPLIES userPassword X-ABC-DEF )" ) );
        assertFalse( checker.isValidSyntax( "( 2.5.13.17 APPLIES userPassword X-ABC-123 'test' )" ) );

        // APPLIES is required
        assertFalse( checker.isValidSyntax( ( "( 2.5.13.17 NAME 'octetStringMatch' )" ) ) );
    }

}
