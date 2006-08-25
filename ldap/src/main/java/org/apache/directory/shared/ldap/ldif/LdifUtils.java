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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.util.Base64;


/**
 * Some LDIF useful methods
 *
 */
public class LdifUtils
{

	/** The array that will be used to match the first char.*/
    private static boolean[] LDIF_SAFE_STARTING_CHAR_ALPHABET = new boolean[128];
    
    /** The array that will be used to match the other chars.*/
    private static boolean[] LDIF_SAFE_OTHER_CHARS_ALPHABET = new boolean[128];
    
    static
    {
    	// Initialization of the array that will be used to match the first char.
    	for (int i = 0; i < 128; i++) {
    		LDIF_SAFE_STARTING_CHAR_ALPHABET[i] = true;
		}
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[0] = false; // 0 (NUL)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[10] = false; // 10 (LF)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[13] = false; // 13 (CR)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[32] = false; // 32 (SPACE)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[58] = false; // 58 (:)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[60] = false; // 60 (>)
    	
    	// Initialization of the array that will be used to match the other chars.
    	for (int i = 0; i < 128; i++) {
    		LDIF_SAFE_OTHER_CHARS_ALPHABET[i] = true;
		}
    	LDIF_SAFE_OTHER_CHARS_ALPHABET[0] = false; // 0 (NUL)
    	LDIF_SAFE_OTHER_CHARS_ALPHABET[10] = false; // 10 (LF)
    	LDIF_SAFE_OTHER_CHARS_ALPHABET[13] = false; // 13 (CR)
    }

    /**
     * Checks if the input String contains only safe values, that is, the data
     * does not need to be encoded for use with LDIF. The rules for checking safety
     * are based on the rules for LDIF (LDAP Data Interchange Format) per RFC 2849.
     * The data does not need to be encoded if all the following are true:
     * 
     * The data cannot start with the following char values:
     * 		00 (NUL)
     * 		10 (LF)
     * 		13 (CR)
     * 		32 (SPACE)
     * 		58 (:)
     * 		60 (<)
     * 		Any character with value greater than 127
     * 
     * The data cannot contain any of the following char values:
     * 		00 (NUL)
     * 		10 (LF)
     * 		13 (CR)
     * 		Any character with value greater than 127
     * 
     * The data cannot end with a space.
     * 
     * @param str the String to be checked
     * @return true if encoding not required for LDIF
     */
    public static boolean isLDIFSafe( String str )
    {
    	// Checking the first char
    	char currentChar = str.charAt(0);
    	if ( currentChar > 127 || !LDIF_SAFE_STARTING_CHAR_ALPHABET[currentChar] )
    	{
    		return false;
    	}
    	
    	// Checking the other chars
    	for (int i = 1; i < str.length(); i++)
    	{
        	currentChar = str.charAt(i);
        	
        	if ( currentChar > 127 || !LDIF_SAFE_OTHER_CHARS_ALPHABET[currentChar] )
        	{
        		return false;
        	}
		}
    	
    	// The String cannot end with a space
    	return ( currentChar != ' ' );
    }
    
    /**
     * Convert an Attributes as LDIF
     * @param attrs the Attributes to convert
     * @return the corresponding LDIF code as a String
     * @throws NamingException If a naming exception is encountered.
     */
    public static String convertToLdif( Attributes attrs ) throws NamingException
    {
		StringBuffer sb = new StringBuffer();
		
		NamingEnumeration ne = attrs.getAll();
		
		while ( ne.hasMore() )
		{
			Object attribute = ne.next();
			if (attribute instanceof Attribute) {
				sb.append( convertToLdif( (Attribute) attribute ) );
			}			
		}
		
		return sb.toString();
	}
    
    /**
     * Converts an Attribute as LDIF
     * @param attr the Attribute to convert
     * @return the corresponding LDIF code as a String
     * @throws NamingException If a naming exception is encountered.
     */
	private static String convertToLdif(Attribute attr) throws NamingException
	{
		StringBuffer sb = new StringBuffer();
		
		// iterating on the attribute's values
		for ( int i = 0; i < attr.size(); i++ )
        {
			StringBuffer lineBuffer = new StringBuffer();
			
			lineBuffer.append( attr.getID() );
			
			Object value = attr.get(i);
            
            // Checking if the value is binary
            if ( value instanceof byte[] )
            {
            	// It is binary, so we have to encode it using Base64 before adding it
            	char[] encoded = Base64.encode( ( byte[] ) value );
            	
            	lineBuffer.append( ":: " + new String( encoded ) );                        	
            }
            else if ( value instanceof String )
            {
            	// It's a String but, we have to check if encoding isn't required
            	String str = (String) value;
            	if ( !LdifUtils.isLDIFSafe( str ) )
            	{
            		char[] encoded = Base64.encode( ( ( String ) value ).getBytes() );
            		
            		lineBuffer.append( ":: " + new String( encoded ) );
            	}
            	else
            	{
            		lineBuffer.append( ": " + value );
            	}
            }
            
            lineBuffer.append( "\n" );
            sb.append( stripLineToNChars(lineBuffer.toString(), 80));
        }
		
		return sb.toString();
	}
	
	/**
	 * Strips the String every n specified characters
	 * @param str the string to strip
	 * @param nbChars the number of characters
	 * @return the stripped String
	 */
	private static String stripLineToNChars( String str, int nbChars)
	{
		if ( str.length() <= nbChars )
		{
			return str;
		}
		
		StringBuffer sb = new StringBuffer();
		String substr;
		int i = 0;
		boolean firstPass = true;
		
		while ( i < (str.length() - nbChars) ) {
			if ( firstPass )
			{
				substr = str.substring( i, i + nbChars);
				firstPass = false;
				// Since we add a space at the beginning of the next line,
				// we need to update nbChars
				nbChars--;
			}
			else
			{
				substr = str.substring( i, i + nbChars );
			}
			
			sb.append( substr + "\n " );
			i = i + nbChars;
		}
		// Adding the last characters
		sb.append( str.substring(i, str.length() ) );
		
		return sb.toString();
	}
}

