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


import junit.framework.TestCase;


/**
 * Test cases for ObjectClassDescriptionSyntaxChecker.
 * 
 * There are also many test cases in SchemaParserObjectClassDescriptionTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ObjectClassDescriptionSyntaxCheckerTest extends TestCase
{
    ObjectClassDescriptionSyntaxChecker checker = new ObjectClassDescriptionSyntaxChecker();


    public void testNullString()
    {
        assertFalse( checker.isValidSyntax( null ) );
    }


    public void testEmptyString()
    {
        assertFalse( checker.isValidSyntax( "" ) );
    }


    public void testOneCharString()
    {
        assertFalse( checker.isValidSyntax( "A" ) );
        assertFalse( checker.isValidSyntax( "1" ) );
        assertFalse( checker.isValidSyntax( "-" ) );
        assertFalse( checker.isValidSyntax( "(" ) );
    }


    public void testValid()
    {
        assertTrue( checker.isValidSyntax( "( 2.5.6.6 )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.6.6 NAME 'person' )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top STRUCTURAL )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top STRUCTURAL MUST ( sn $ cn ) )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top STRUCTURAL MUST ( sn $ cn ) MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) )" ) );

        assertTrue( checker.isValidSyntax( "(2.5.6.6)" ) );
        assertTrue( checker.isValidSyntax( "(      2.5.6.6      NAME      'person'      )" ) );
    }


    public void testInvalid()
    {
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
        assertFalse( checker.isValidSyntax( "( 2.5.6.6 NAME 'person'" ) );

        // missing quotes
        assertFalse( checker.isValidSyntax( "( 2.5.6.6 NAME person )" ) );

        // lowercase NAME, DESC, SUP
        assertFalse( checker.isValidSyntax( "( 2.5.6.6 name 'person' desc 'RFC2256: a person' sup top " ) );

    }

}
