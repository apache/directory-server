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

import org.apache.directory.shared.ldap.schema.syntaxes.Ia5StringSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for Ia5StringSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class Ia5StringSyntaxCheckerTest
{
    Ia5StringSyntaxChecker checker = new Ia5StringSyntaxChecker();


    @Test
    public void testNullString()
    {
        assertTrue( checker.isValidSyntax( null ) );
    }


    @Test
    public void testEmptyString()
    {
        assertTrue( checker.isValidSyntax( "" ) );
    }


    @Test
    public void testWrongCase()
    {
        assertFalse( checker.isValidSyntax( "\u00E9" ) );
        assertFalse( checker.isValidSyntax( "\u00A7" ) );
        assertFalse( checker.isValidSyntax( "\u00E8" ) );
        assertFalse( checker.isValidSyntax( "\u00C7" ) );
        assertFalse( checker.isValidSyntax( "\u00E0" ) );
        assertFalse( checker.isValidSyntax( "\u00B0" ) );
        assertFalse( checker.isValidSyntax( "\u00F9" ) );
        assertFalse( checker.isValidSyntax( "\u00A3" ) );
        assertFalse( checker.isValidSyntax( "\u20AC" ) );
        assertFalse( checker.isValidSyntax( "\u00B4" ) );
        assertFalse( checker.isValidSyntax( "\u00B8" ) );
    }
    
    
    @Test
    public void testCorrectCase()
    {
        assertTrue( checker.isValidSyntax( "0123456789" ) );
        assertTrue( checker.isValidSyntax( "abcdefghijklmnopqrstuvwxyz" ) );
        assertTrue( checker.isValidSyntax( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" ) );
        assertTrue( checker.isValidSyntax( "'()+,-.=/:? " ) );
        
        byte[] bytes = new byte[128];
        
        for ( int i = 0; i < 128; i++ )
        {
            bytes[i] = (byte)i;
        }
        
        assertTrue( checker.isValidSyntax( new String( bytes ) ) );
    }
}
