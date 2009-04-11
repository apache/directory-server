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

import org.apache.directory.shared.ldap.schema.syntaxes.TeletexTerminalIdentifierSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for TeletexTerminalIdentifierSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class TeletexTerminalIdentifierSyntaxCheckerTest
{
    TeletexTerminalIdentifierSyntaxChecker checker = new TeletexTerminalIdentifierSyntaxChecker();


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
    public void testWrongCase() throws Exception
    {
        assertFalse( checker.isValidSyntax( "test$" ) );
        assertFalse( checker.isValidSyntax( new String( new byte[]{ 't', 'e', 's', 't', 0x00, 0x7F, (byte)0x80, '$', 't', 'e', 's', 't' }, "UTF-8" ) ) );
        assertFalse( checker.isValidSyntax( "test$$" ) );
        assertFalse( checker.isValidSyntax( "test$a:b" ) );
        assertFalse( checker.isValidSyntax( "test$misc" ) );
        assertFalse( checker.isValidSyntax( "test$misc:" ) );
        assertFalse( checker.isValidSyntax( "test$:" ) );
        assertFalse( checker.isValidSyntax( "test$:abc" ) );
        assertFalse( checker.isValidSyntax( "test$misc:a$b" ) );
        assertFalse( checker.isValidSyntax( "test$misc:a\\b" ) );
        assertFalse( checker.isValidSyntax( "test$misc:a\\2b" ) );
        assertFalse( checker.isValidSyntax( "test$misc:a\\5b" ) );
    }
    
    
    @Test
    public void testCorrectCase() throws Exception
    {
        assertTrue( checker.isValidSyntax( "test" ) );
        assertTrue( checker.isValidSyntax( "test$graphic:abc" ) );
        assertTrue( checker.isValidSyntax( "test$misc:abc" ) );
        assertTrue( checker.isValidSyntax( "test$control:abc" ) );
        assertTrue( checker.isValidSyntax( "test$page:abc" ) );
        assertTrue( checker.isValidSyntax( "test$private:abc" ) );
        assertTrue( checker.isValidSyntax( "test$private:abc$misc:def" ) );
        assertTrue( checker.isValidSyntax( "test$misc:" + new String( new byte[]{ 't', 'e', 's', 't', 0x00, 0x7F, (byte)0xFF}, "UTF-8" ) ) );
        assertTrue( checker.isValidSyntax( "test$misc:a\\5c" ) );
        assertTrue( checker.isValidSyntax( "test$misc:a\\5C" ) );
        assertTrue( checker.isValidSyntax( "test$misc:a\\24" ) );
    }
}
