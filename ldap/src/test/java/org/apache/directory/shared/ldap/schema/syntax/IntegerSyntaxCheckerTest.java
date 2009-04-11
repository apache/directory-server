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

import org.apache.directory.shared.ldap.schema.syntaxes.IntegerSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for IntegerSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IntegerSyntaxCheckerTest
{
    IntegerSyntaxChecker checker = new IntegerSyntaxChecker();


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
        assertFalse( checker.isValidSyntax( "-" ) );
    }
    
    
    @Test
    public void testWrongCase()
    {
        assertFalse( checker.isValidSyntax( "000" ) );
        assertFalse( checker.isValidSyntax( "-0" ) );
        assertFalse( checker.isValidSyntax( " 1" ) );
        assertFalse( checker.isValidSyntax( "1 " ) );
    }
    
    
    @Test
    public void testCorrectCase()
    {
        assertTrue( checker.isValidSyntax( "1" ) );
        assertTrue( checker.isValidSyntax( "10" ) );
        assertTrue( checker.isValidSyntax( "1111" ) );
        assertTrue( checker.isValidSyntax( "-1" ) );
        assertTrue( checker.isValidSyntax( "-1234567891" ) );
        assertTrue( checker.isValidSyntax( "123456789" ) );
    }
}
