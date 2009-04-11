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

import org.apache.directory.shared.ldap.schema.syntaxes.DirectoryStringSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for DirectoryStringSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DirectoryStringSyntaxCheckerTest
{
    DirectoryStringSyntaxChecker checker = new DirectoryStringSyntaxChecker();


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
    public void testWrongCase()
    {
        assertFalse( checker.isValidSyntax( "" ) );
        
        byte[] bytes = new byte[2];
        
        bytes[0] = (byte)0x80;
        bytes[1] = (byte)0x00;
        
        assertFalse( checker.isValidSyntax( bytes ) );
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
