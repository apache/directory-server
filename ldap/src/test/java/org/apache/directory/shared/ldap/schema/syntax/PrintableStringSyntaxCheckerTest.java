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

import org.apache.directory.shared.ldap.schema.syntaxes.PrintableStringSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for PrintableStringSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PrintableStringSyntaxCheckerTest
{
    PrintableStringSyntaxChecker checker = new PrintableStringSyntaxChecker();


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


    /**
     * 
     * Check that non printable Strings are not accepted. We created Strings
     * which contains only one char which is not in the acceptable set of
     * printable chars.
     *
     */
    @Test
    public void testWrongStrings()
    {
        for ( int i = 0; i < 0x1F; i++ )
        {
            assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)i} ) ) );
        }

        for ( int i = 0x21; i < 0x26; i++ )
        {
            assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)i} ) ) );
        }

        for ( int i = 0x5B; i < 0x60; i++ )
        {
            assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)i} ) ) );
        }

        for ( int i = 0x7B; i < 0x7F; i++ )
        {
            assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)i} ) ) );
        }

        assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)0x2A} ) ) );
        assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)0x3B} ) ) );
        assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)0x3C} ) ) );
        assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)0x3E} ) ) );
        assertFalse( checker.isValidSyntax( new String( new byte[]{(byte)0x40} ) ) );
    }
    
    
    @Test
    public void testCorrectCase()
    {
        assertTrue( checker.isValidSyntax( "0123456789" ) );
        assertTrue( checker.isValidSyntax( "abcdefghijklmnopqrstuvwxyz" ) );
        assertTrue( checker.isValidSyntax( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" ) );
        assertTrue( checker.isValidSyntax( "'()+,-.=/:? " ) );
    }
}
