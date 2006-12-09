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


import org.apache.directory.shared.ldap.schema.syntax.NumericOidSyntaxChecker;

import junit.framework.TestCase;


/**
 * A test case for the NameOrNumericId test.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameOrNumericIdSyntaxCheckerTest extends TestCase
{
    NumericOidSyntaxChecker checker = new NumericOidSyntaxChecker( "1.1" );


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
        assertFalse( checker.isValidSyntax( "." ) );
        assertTrue( checker.isValidSyntax( "a" ) );
        assertFalse( checker.isValidSyntax( "-" ) );
    }
    
    
    public void testNumericIds()
    {
        assertFalse( checker.isValidSyntax( "111" ) );
        assertFalse( checker.isValidSyntax( "11.a" ) );
        assertFalse( checker.isValidSyntax( "11.1a" ) );
        assertTrue( checker.isValidSyntax( "1.1" ) );
        assertTrue( checker.isValidSyntax( "1.3.6.1.2.67.3.2" ) );
    }
    
    
    public void testNames()
    {
        assertFalse( checker.isValidSyntax( "asdf$" ) );
        assertTrue( checker.isValidSyntax( "asdf-asdf" ) );
        assertFalse( checker.isValidSyntax( "-asdf-asdf" ) );
        assertTrue( checker.isValidSyntax( "A-asdf-asdf" ) );
        assertFalse( checker.isValidSyntax( "0-asdf-asdf" ) );
        assertTrue( checker.isValidSyntax( "A-asdf0a234sdf" ) );
    }
}
