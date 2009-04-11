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


import org.apache.directory.shared.ldap.schema.syntaxes.NameFormDescriptionSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test cases for NameFormDescriptionSyntaxChecker.
 * 
 * There are also many test cases in SchemaParserNameFormDescriptionTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameFormDescriptionSyntaxCheckerTest
{
    private NameFormDescriptionSyntaxChecker checker = new NameFormDescriptionSyntaxChecker();

    @Test
    public void testValid()
    {
        assertTrue( checker.isValidSyntax( "( 2.5.15.3 OC o MUST m )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.15.3 OC o MUST m NAME 'orgNameForm' )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.15.3 OC o MUST m NAME 'orgNameForm' DESC 'orgNameForm' )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.15.3 MUST m NAME 'orgNameForm' DESC 'orgNameForm' OC organization )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.15.3 NAME 'orgNameForm' DESC 'orgNameForm' OC organization MUST o )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.15.3 NAME 'orgNameForm' DESC 'orgNameForm' OC organization MUST ( o ) MAY ( ou $ cn ) )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.15.3 NAME 'orgNameForm' DESC 'orgNameForm' OC organization MUST ( o ) MAY ( ou $ cn ) )" ) );
       

        assertTrue( checker.isValidSyntax( "(2.5.15.3 OC o MUST m)" ) );
        assertTrue( checker.isValidSyntax( "(   2.5.15.3   NAME   'orgNameForm'    DESC    'orgNameForm'   OC   organization   MUST   (o)   MAY   (ou$cn))" ) );
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
        assertFalse( checker.isValidSyntax( "( 2.5.15.3 NAME 'orgNameForm'" ) );

        // missing quotes
        assertFalse( checker.isValidSyntax( "( 2.5.15.3 NAME orgNameForm )" ) );

        // lowercase NAME, DESC, AUX
        assertFalse( checker.isValidSyntax( "( 2.5.15.3 name 'orgNameForm' desc 'orgNameForm' oc o )" ) );

        assertFalse( checker.isValidSyntax( "( 2.5.15.3 )" ) );
        assertFalse( checker.isValidSyntax( "( 2.5.15.3 NAME 'orgNameForm' )" ) );
    }

}
