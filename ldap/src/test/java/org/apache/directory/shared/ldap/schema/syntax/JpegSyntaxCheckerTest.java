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


import org.apache.directory.shared.ldap.schema.syntaxes.JpegSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for JpegSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JpegSyntaxCheckerTest
{
    JpegSyntaxChecker checker = new JpegSyntaxChecker();


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
        assertFalse( checker.isValidSyntax(  "this is not a jpeg file..." ) );
    }

    @Test
    public void testCorrectCase()
    {
        byte[] array = new byte[256];
        
        for ( int i = 0; i < 256; i++ )
        {
            array[ i ] = (byte)i;
        }
        
        array[0] = (byte)0xFF;
        array[1] = (byte)0xD8;
        array[2] = (byte)0xFF;
        array[3] = (byte)0xE0;
        array[4] = (byte)0x00;
        array[5] = (byte)0x10;
        array[6] = 'J';
        array[7] = 'F';
        array[8] = 'I';
        array[9] = 'F';
        array[10] = '\0';

        assertTrue( checker.isValidSyntax( array ) );
    }
}
