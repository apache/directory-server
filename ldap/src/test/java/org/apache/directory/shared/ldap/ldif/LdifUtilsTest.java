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
package org.apache.directory.shared.ldap.ldif;

import junit.framework.TestCase;

public class LdifUtilsTest extends TestCase
{
	private String testString = "this is a test";
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char NUL (ASCII code 0)
	 */
	public void testIsLdifSafeStartingWithNUL()
    {
		char c = ( char ) 0;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char LF (ASCII code 10)
	 */
	public void testIsLdifSafeStartingWithLF()
    {
		char c = ( char ) 10;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }

	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char CR (ASCII code 13)
	 */
	public void testIsLdifSafeStartingWithCR()
    {
		char c = ( char ) 13;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }

	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char SPACE (ASCII code 32)
	 */
	public void testIsLdifSafeStartingWithSpace()
    {
		char c = ( char ) 32;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char COLON (:) (ASCII code 58)
	 */
	public void testIsLdifSafeStartingWithColon()
    {
		char c = ( char ) 58;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char LESS_THAN (<) (ASCII code 60)
	 */
	public void testIsLdifSafeStartingWithLessThan()
    {
		char c = ( char ) 60;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char with ASCII code 127
	 */
	public void testIsLdifSafeStartingWithCharGreaterThan127()
    {
		char c = ( char ) 127;
		
		assertTrue( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char with ASCII code greater than 127
	 */
	public void testIsLdifSafeStartingWithCharGreaterThan127Bis()
    {
		char c = ( char ) 222;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing the
	 * char NUL (ASCII code 0)
	 */
	public void testIsLdifSafeContainsNUL()
    {
		char c = ( char ) 0;
		
		assertFalse( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing the
	 * char LF (ASCII code 10)
	 */
	public void testIsLdifSafeContainsLF()
    {
		char c = ( char ) 10;
		
		assertFalse( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing the
	 * char CR (ASCII code 13)
	 */
	public void testIsLdifSafeContainsCR()
    {
		char c = ( char ) 13;
		
		assertFalse( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing the
	 * char with ASCII code 127
	 */
	public void testIsLdifSafeContainsCharGreaterThan127()
    {
		char c = ( char ) 127;
		
		assertTrue( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing a
	 * char with ASCII code greater than 127
	 */
	public void testIsLdifSafeContainsCharGreaterThan127Bis()
    {
		char c = ( char ) 328;
		
		assertFalse( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String ending with the
	 * char SPACE (ASCII code 32)
	 */
	public void testIsLdifSafeEndingWithSpace()
    {
		char c = ( char ) 32;
		
		assertFalse( LdifUtils.isLDIFSafe( testString  + c) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a correct String
	 */
	public void testIsLdifSafeCorrectString()
    {		
		assertTrue( LdifUtils.isLDIFSafe( testString ) );
    }
    
    public void testStripLineToNChars()
    {
        String line = "abc";
        
        try
        {
            LdifUtils.stripLineToNChars( line, 1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // This is correct
        }
        
        String res = LdifUtils.stripLineToNChars( line, 2 );
        assertEquals( "ab\n c", res );
        assertEquals( "abc", LdifUtils.stripLineToNChars( line, 3 ) );
    }

    public void testStripLineTo5Chars()
    {
        assertEquals( "a", LdifUtils.stripLineToNChars( "a", 5 ) );
        assertEquals( "ab", LdifUtils.stripLineToNChars( "ab", 5 ) );
        assertEquals( "abc", LdifUtils.stripLineToNChars( "abc", 5 ) );
        assertEquals( "abcd", LdifUtils.stripLineToNChars( "abcd", 5 ) );
        assertEquals( "abcde", LdifUtils.stripLineToNChars( "abcde", 5 ) );
        assertEquals( "abcde\n f", LdifUtils.stripLineToNChars( "abcdef", 5 ) );
        assertEquals( "abcde\n fg", LdifUtils.stripLineToNChars( "abcdefg", 5 ) );
        assertEquals( "abcde\n fgh", LdifUtils.stripLineToNChars( "abcdefgh", 5 ) );
        assertEquals( "abcde\n fghi", LdifUtils.stripLineToNChars( "abcdefghi", 5 ) );
        assertEquals( "abcde\n fghi\n j", LdifUtils.stripLineToNChars( "abcdefghij", 5 ) );
        assertEquals( "abcde\n fghi\n jk", LdifUtils.stripLineToNChars( "abcdefghijk", 5 ) );
        assertEquals( "abcde\n fghi\n jkl", LdifUtils.stripLineToNChars( "abcdefghijkl", 5 ) );
        assertEquals( "abcde\n fghi\n jklm", LdifUtils.stripLineToNChars( "abcdefghijklm", 5 ) );
        assertEquals( "abcde\n fghi\n jklm\n n", LdifUtils.stripLineToNChars( "abcdefghijklmn", 5 ) );
        assertEquals( "abcde\n fghi\n jklm\n no", LdifUtils.stripLineToNChars( "abcdefghijklmno", 5 ) );
        assertEquals( "abcde\n fghi\n jklm\n nop", LdifUtils.stripLineToNChars( "abcdefghijklmnop", 5 ) );
    }
}
