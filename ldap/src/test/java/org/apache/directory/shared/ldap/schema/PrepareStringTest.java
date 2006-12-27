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
package org.apache.directory.shared.ldap.schema;

import org.apache.directory.shared.ldap.util.unicode.InvalidCharacterException;

import junit.framework.TestCase;

/**
 * 
 * Test the PrepareString class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PrepareStringTest extends TestCase
{
    public void testInsignifiantSpacesStringNull() throws InvalidCharacterException
    {
        assertEquals( "  ", PrepareString.insignifiantSpacesString( (String)null ) );
    }
    
    public void testInsignifiantSpacesStringEmpty() throws InvalidCharacterException
    {
        assertEquals( "  ", PrepareString.insignifiantSpacesString( "" ) );
    }
    
    public void testInsignifiantSpacesStringOneSpace() throws InvalidCharacterException
    {
        assertEquals( "  ", PrepareString.insignifiantSpacesString( " " ) );
    }
    
    public void testInsignifiantSpacesStringTwoSpaces() throws InvalidCharacterException
    {
        assertEquals( "  ", PrepareString.insignifiantSpacesString( "  " ) );
    }
    
    public void testInsignifiantSpacesStringNSpaces() throws InvalidCharacterException
    {
        assertEquals( "  ", PrepareString.insignifiantSpacesString( "      " ) );
    }

    public void testInsignifiantSpacesStringOneChar() throws InvalidCharacterException
    {
        assertEquals( " a ", PrepareString.insignifiantSpacesString( "a" ) );
    }

    public void testInsignifiantSpacesStringTwoChars() throws InvalidCharacterException
    {
        assertEquals( " aa ", PrepareString.insignifiantSpacesString( "aa" ) );
    }

    public void testInsignifiantSpacesStringNChars() throws InvalidCharacterException
    {
        assertEquals( " aaaaa ", PrepareString.insignifiantSpacesString( "aaaaa" ) );
    }

    public void testInsignifiantSpacesStringOneCombining() throws InvalidCharacterException
    {
        char[] chars = new char[]{ ' ', 0x0310 };
        char[] expected = new char[]{ ' ', ' ', 0x0310, ' ' };
        assertEquals( new String( expected ), PrepareString.insignifiantSpacesString( new String( chars ) ) );
    }

    public void testInsignifiantSpacesStringNCombining() throws InvalidCharacterException
    {
        char[] chars = new char[]{ ' ', 0x0310, ' ', 0x0311, ' ', 0x0312 };
        char[] expected = new char[]{ ' ', ' ', 0x0310, ' ', 0x0311, ' ', 0x0312, ' ' };
        assertEquals( new String( expected ), PrepareString.insignifiantSpacesString( new String( chars ) ) );
    }
    
    public void testInsignifiantSpacesStringCharsSpaces() throws InvalidCharacterException
    {
        assertEquals( " a ", PrepareString.insignifiantSpacesString( " a" ) );
        assertEquals( " a ", PrepareString.insignifiantSpacesString( "a " ) );
        assertEquals( " a ", PrepareString.insignifiantSpacesString( " a " ) );
        assertEquals( " a a ", PrepareString.insignifiantSpacesString( "a a" ) );
        assertEquals( " a a ", PrepareString.insignifiantSpacesString( " a a" ) );
        assertEquals( " a a ", PrepareString.insignifiantSpacesString( "a a " ) );
        assertEquals( " a a ", PrepareString.insignifiantSpacesString( "a  a" ) );
        assertEquals( " a a ", PrepareString.insignifiantSpacesString( " a   a " ) );
        assertEquals( " aaa aaa aaa ", PrepareString.insignifiantSpacesString( "  aaa   aaa   aaa  " ) );
    }

    public void testInsignifiantSpacesStringCharsCombiningSpaces() throws InvalidCharacterException
    {
        char[] chars = new char[]{ ' ', 0x0310, 'a', 'a', ' ', ' ',  0x0311, ' ', ' ', 'a', 0x0311, 0x0312 };
        char[] expected = new char[]{ ' ', ' ', 0x0310, 'a', 'a', ' ', ' ',  0x0311, ' ', 'a', 0x0311, 0x0312, ' ' };
        assertEquals( new String( expected ), PrepareString.insignifiantSpacesString( new String( chars ) ) );
    }

    public void testMapNull()
    {
        assertNull( PrepareString.map( (String)null ) );
    }

    public void testMapEmpty()
    {
        assertEquals( "", PrepareString.map( "" ) );
    }

    public void testMapString()
    {
        assertEquals( "abcd", PrepareString.map( "abcd" ) );
    }

    public void testMapToLower()
    {
        assertEquals( "abcdefghijklmnopqrstuvwxyz", PrepareString.map( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" ) );
    }

    public void testMapToSpace()
    {
        char[] chars = new char[]{ 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0085, 0x00A0, 0x1680, 
            0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A,
            0x2028, 0x2029, 0x202F, 0x205F };
        assertEquals( "                       ", PrepareString.map( new String( chars ) ) );
    }

    public void testMapToIgnore()
    {
        char[] chars = new char[58];
            
        int pos = 0;
        
        for ( char c = 0x0000; c < 0x0008; c++ )
        {
            chars[pos++] = c;
        }

        for ( char c = 0x000E; c < 0x001F; c++ )
        {
            chars[pos++] = c;
        }
        
        for ( char c = 0x007F; c < 0x0084; c++ )
        {
            chars[pos++] = c;
        }
        
        for ( char c = 0x0086; c < 0x009F; c++ )
        {
            chars[pos++] = c;
        }
        
        chars[pos++] = 0x00AD;

        assertEquals( "", PrepareString.map( new String( chars ) ) );
    }
    
    public void testInsignifiantSpacesHandleNumericStringNull()
    {
        assertNull( PrepareString.insignifiantCharNumericString( (String )null ) );
    }

    public void testInsignifiantSpacesHandleNumericStringEmpty()
    {
        assertEquals( "", PrepareString.insignifiantCharNumericString( "" ) );
    }

    public void testInsignifiantSpacesHandleNumericStringSpacesOnly()
    {
        assertEquals( "", PrepareString.insignifiantCharNumericString( " " ) );
        assertEquals( "", PrepareString.insignifiantCharNumericString( "   " ) );
        assertEquals( "", PrepareString.insignifiantCharNumericString( "    " ) );
        assertEquals( "", PrepareString.insignifiantCharNumericString( "      " ) );
    }

    public void testInsignifiantSpacesHandleNumericString()
    {
        assertEquals( "1", PrepareString.insignifiantCharNumericString( "1" ) );
        assertEquals( "123456789", PrepareString.insignifiantCharNumericString( "123456789" ) );
        assertEquals( "111", PrepareString.insignifiantCharNumericString( "   111" ) );
        assertEquals( "111", PrepareString.insignifiantCharNumericString( "111  " ) );
        assertEquals( "11", PrepareString.insignifiantCharNumericString( "1 1" ) );
        assertEquals( "111", PrepareString.insignifiantCharNumericString( "1 1 1" ) );
    }
}
