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
 * Test cases for OtherMailboxSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OtherMailboxSyntaxCheckerTest extends TestCase
{
    OtherMailboxSyntaxChecker checker = new OtherMailboxSyntaxChecker();


    public void testNullString()
    {
        assertFalse( checker.isValidSyntax( null ) );
    }


    public void testEmptyString()
    {
        assertFalse( checker.isValidSyntax( "" ) );
    }


    public void testWrongCase() throws Exception
    {
        assertFalse( checker.isValidSyntax( "mailType" ) );
        assertFalse( checker.isValidSyntax( "%$test" ) );
        assertFalse( checker.isValidSyntax( "test$test$test" ) );
        assertFalse( checker.isValidSyntax( "test$test$" ) );
        assertFalse( checker.isValidSyntax( new String( new byte[]{ 'm', 'a', 'i', 'l', '$', 0x00, 0x7F, (byte)0x80 }, "UTF-8" ) ) );
    }
    
    
    public void testCorrectCase()
    {
        assertTrue( checker.isValidSyntax( "SMTP$dev@directory.apache.org" ) );
    }
}
