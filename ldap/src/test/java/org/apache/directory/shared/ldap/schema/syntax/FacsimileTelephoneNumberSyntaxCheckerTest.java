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

import org.apache.directory.shared.ldap.schema.syntaxes.FacsimileTelephoneNumberSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for FacsimileTelephoneNumberSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class FacsimileTelephoneNumberSyntaxCheckerTest
{
    FacsimileTelephoneNumberSyntaxChecker checker = new FacsimileTelephoneNumberSyntaxChecker();


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
    public void testOneCharString()
    {
        assertFalse( checker.isValidSyntax( "A" ) );
        assertFalse( checker.isValidSyntax( "+" ) );
    }
    
    
    @Test
    public void testWrongCase()
    {
        assertFalse( checker.isValidSyntax( "123 456 f" ) );
        assertFalse( checker.isValidSyntax( "+ ()" ) );
        assertFalse( checker.isValidSyntax( " +2 (123) 456-789 +" ) );
    }
    
    
    @Test
    public void testCorrectTelephoneNumber()
    {
        assertTrue( checker.isValidSyntax( "1" ) );
        assertTrue( checker.isValidSyntax( "1111" ) );
        assertTrue( checker.isValidSyntax( "1 (2) 3" ) );
        assertTrue( checker.isValidSyntax( "+ 123 ( 456 )7891   12345" ) );
        assertTrue( checker.isValidSyntax( " 12 34 56 78 90 " ) );
    }
    
    @Test
    public void testWithNewMandatoryRegexp()
    {
        // Adding french telephone number regexp
        checker.setDefaultRegexp( " *0[1-8](( *|[-/.]{1})\\d\\d){4} *" );
        
        assertFalse( checker.isValidSyntax( "+ 123 ( 456 )7891   12345" ) );
        assertTrue( checker.isValidSyntax( " 01 02 03 04 05 " ) );
        assertTrue( checker.isValidSyntax( " 0102 03 04 05 " ) );
        assertTrue( checker.isValidSyntax( " 01 02 03 04  05 " ) );
        assertTrue( checker.isValidSyntax( " 01/02/03/04/05 " ) );
        assertFalse( checker.isValidSyntax( " 01 / 02 .03 04--  05 " ) );
    }

    @Test
    public void testCorrectTelephoneNumberAndFaxParam()
    {
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$twoDimensional" ) );
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$fineResolution" ) );
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$unlimitedLength" ) );
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$b4Length" ) );
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$a3Width" ) );
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$b4Width" ) );
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$twoDimensional" ) );
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$uncompressed" ) );
    }
    
    @Test
    public void testCorrectTelephoneNumberAndFaxParams()
    {
        assertTrue( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$twoDimensional$fineResolution$a3Width" ) );
    }

    @Test
    public void testCorrectTelephoneNumberBadFaxParams()
    {
        assertFalse( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$" ) );
        assertFalse( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$$" ) );
        assertFalse( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$twoDimensionnal" ) );
        assertFalse( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$ twoDimensional" ) );
        assertFalse( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$twoDimensional$" ) );
        assertFalse( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$twoDimensional$twoDimensional" ) );
        assertFalse( checker.isValidSyntax( "+ 33 1 (456) 7891   12345$b4Width$ $a3Width" ) );
    }
}
