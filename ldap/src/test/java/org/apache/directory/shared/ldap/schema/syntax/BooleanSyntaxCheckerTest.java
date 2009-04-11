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

import org.apache.directory.shared.ldap.schema.syntaxes.BooleanSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for BooleanSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BooleanSyntaxCheckerTest
{
    BooleanSyntaxChecker checker = new BooleanSyntaxChecker();


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
        assertFalse( checker.isValidSyntax( "f" ) );
        assertFalse( checker.isValidSyntax( "F" ) );
        assertFalse( checker.isValidSyntax( "t" ) );
        assertFalse( checker.isValidSyntax( "T" ) );
    }
    
    
    @Test
    public void testWrongValue()
    {
        assertFalse( checker.isValidSyntax( "abc" ) );
        assertFalse( checker.isValidSyntax( "123" ) );
    }
    
    
    @Test
    public void testMixedCase()
    {
        assertTrue( checker.isValidSyntax( "fAlSe" ) );
        assertTrue( checker.isValidSyntax( "tRue" ) );
        assertTrue( checker.isValidSyntax( "false" ) );
        assertTrue( checker.isValidSyntax( "true" ) );
    }
    
    
    @Test
    public void testUpperCase()
    {
        assertTrue( checker.isValidSyntax( "FALSE" ) );
        assertTrue( checker.isValidSyntax( "TRUE" ) );
    }
}
