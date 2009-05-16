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


import org.apache.directory.shared.ldap.schema.syntaxes.UuidSyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for UuidSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UuidSyntaxCheckerTest
{
    UuidSyntaxChecker checker = new UuidSyntaxChecker();


    @Test
    public void testNullUuid()
    {
        assertFalse( checker.isValidSyntax( null ) );
    }


    @Test
    public void testEmptyUuid()
    {
        assertFalse( checker.isValidSyntax( StringTools.EMPTY_BYTES ) );
    }


    @Test
    public void testStringUuid()
    {
        assertFalse( checker.isValidSyntax( "01234567788ABCDEF" ) );
    }

    
    @Test
    public void testCorrectUuid()
    {
        byte[] array = new byte[16];
        
        for ( int i = 0; i < 16; i++ )
        {
            array[ i ] = (byte)i;
        }

        assertTrue( checker.isValidSyntax( array ) );
    }


    @Test
    public void testWrongSizeUuid()
    {
        byte[] array = new byte[15];
        
        for ( int i = 0; i < 15; i++ )
        {
            array[ i ] = (byte)i;
        }

        assertFalse( checker.isValidSyntax( array ) );
    }
}
