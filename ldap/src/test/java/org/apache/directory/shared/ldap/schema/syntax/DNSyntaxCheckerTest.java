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
 * Test cases for DNSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DNSyntaxCheckerTest extends TestCase
{
    DNSyntaxChecker checker = new DNSyntaxChecker();


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
        assertFalse( checker.isValidSyntax( "0" ) );
        assertFalse( checker.isValidSyntax( "'" ) );
        assertFalse( checker.isValidSyntax( "1" ) );
        assertFalse( checker.isValidSyntax( "B" ) );
    }
    
    
    public void testWrongDN()
    {
        assertFalse( checker.isValidSyntax( "a=b," ) );
        assertFalse( checker.isValidSyntax( "a=#0101'B" ) );
        assertFalse( checker.isValidSyntax( "a=b+" ) );
        assertFalse( checker.isValidSyntax( "a=b,c=d," ) );
    }
    
    
    public void testCorrectDN()
    {
        assertTrue( checker.isValidSyntax( "a=b" ) );
        assertTrue( checker.isValidSyntax( "a = b" ) );
        assertTrue( checker.isValidSyntax( "a=b + c=d" ) );
        assertTrue( checker.isValidSyntax( "a=b,c=d" ) );
        assertTrue( checker.isValidSyntax( "a=b\\,c = d, e=f" ) );
    }
}
