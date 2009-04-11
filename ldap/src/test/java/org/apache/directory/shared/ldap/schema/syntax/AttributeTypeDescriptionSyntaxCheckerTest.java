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


import org.apache.directory.shared.ldap.schema.syntaxes.AttributeTypeDescriptionSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test cases for AttributeTypeDescriptionSyntaxChecker.
 * 
 * There are also many test cases in SchemaParserAttributeTypeDescriptionTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypeDescriptionSyntaxCheckerTest
{
    private AttributeTypeDescriptionSyntaxChecker checker = new AttributeTypeDescriptionSyntaxChecker();

    @Test
    public void testValid()
    {
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15  )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name  )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch  )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch  )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME cn SUP name )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 name ( 'cn' 'commonName' )  sup name )" ) );

        // spaces
        assertTrue( checker.isValidSyntax( "(2.5.4.3 SUP name)" ) );
        assertTrue( checker.isValidSyntax( "(      2.5.4.3      NAME ('cn'   'commonName')     SYNTAX       1.3.6.1.4.1.1466.115.121.1.15   )" ) );
        
        // COLLECTIVE requires usage userApplications
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name COLLECTIVE )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name COLLECTIVE USAGE userApplications )" ) );

        // NO-USER-MODIFICATION requires an operational usage.
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name NO-USER-MODIFICATION USAGE dSAOperation )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name NO-USER-MODIFICATION USAGE directoryOperation )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name NO-USER-MODIFICATION USAGE distributedOperation )" ) );
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
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name" ) );

        // SYNTAX or SUP must be contained
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by' )" ) );
        
        // COLLECTIVE requires usage userApplications
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name COLLECTIVE USAGE dSAOperation)" ) );
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name COLLECTIVE USAGE directoryOperation )" ) );
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name COLLECTIVE USAGE distributedOperation )" ) );
        
        // NO-USER-MODIFICATION requires an operational usage.
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name NO-USER-MODIFICATION )" ) );
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME 'cn' SUP name NO-USER-MODIFICATION USAGE userApplications )" ) );

        // invalid characters
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn#' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications )" ) );
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'common_name' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications )" ) );
        assertFalse( checker.isValidSyntax( "( 2.5.4.3 NAME ( 'cn' 'commonName' ) DESC 'RFC2256: common name(s) for which the entity is known by'  SUP na=me EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications )" ) );
    }

}
