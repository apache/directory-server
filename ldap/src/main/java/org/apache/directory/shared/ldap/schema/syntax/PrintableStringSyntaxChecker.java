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


import javax.naming.NamingException;


import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is a Printable String according to RFC 4517.
 * 
 * From RFC 4517 :
 * 
 * PrintableString    = 1*PrintableCharacter
 * PrintableCharacter = ALPHA | DIGIT | SQUOTE | LPAREN | RPAREN |
 *                          PLUS | COMMA | HYPHEN | DOT | EQUALS |
 *                          SLASH | COLON | QUESTION | SPACE
 *                          
 * SLASH   = %x2F                ; forward slash ("/")
 * COLON   = %x3A                ; colon (":")
 * QUESTION= %x3F                ; question mark ("?")
 * 
 * From RFC 4512 :
 * ALPHA   = %x41-5A | %x61-7A   ; "A"-"Z" / "a"-"z"
 * DIGIT   = %x30 | LDIGIT       ; "0"-"9"
 * LDIGIT  = %x31-39             ; "1"-"9"
 * SQUOTE  = %x27                ; single quote ("'")
 * LPAREN  = %x28                ; left paren ("(")
 * RPAREN  = %x29                ; right paren (")")
 * PLUS    = %x2B                ; plus sign ("+")
 * COMMA   = %x2C                ; comma (",")
 * HYPHEN  = %x2D                ; hyphen ("-")
 * DOT     = %x2E                ; period (".")
 * EQUALS  = %x3D                ; equals sign ("=")
 * SPACE   = %x20                ; space (" ")
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PrintableStringSyntaxChecker implements SyntaxChecker
{
    /** The Syntax OID, according to RFC 4517, par. 3.3.29 */
    public static final String OID = "1.3.6.1.4.1.1466.115.121.1.44";
    
    /** A table containing booleans when the corresponding char is printable */
    private static final boolean[] IS_PRINTABLE_CHAR =
        {
        false, false, false, false, false, false, false, false, // ---, ---, ---, ---, ---, ---, ---, ---
        false, false, false, false, false, false, false, false, // ---, ---, ---, ---, ---, ---, ---, ---
        false, false, false, false, false, false, false, false, // ---, ---, ---, ---, ---, ---, ---, ---
        false, false, false, false, false, false, false, false, // ---, ---, ---, ---, ---, ---, ---, ---
        true,  false, false, false, false, false, false, true,  // ' ', ---, ---, ---, ---, ---, ---, "'" 
        true,  true,  false, true,  true,  true,  true,  true,  // '(', ')', ---, '+', ',', '-', '.', '/'
        true,  true,  true,  true,  true,  true,  true,  true,  // '0', '1', '2', '3', '4', '5', '6', '7',  
        true,  true,  true,  false, false, true,  false, true,  // '8', '9', ':', ---, ---, '=', ---, '?'
        false, true,  true,  true,  true,  true,  true,  true,  // ---, 'A', 'B', 'C', 'D', 'E', 'F', 'G', 
        true,  true,  true,  true,  true,  true,  true,  true,  // 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O'
        true,  true,  true,  true,  true,  true,  true,  true,  // 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W'
        true,  true,  true,  false, false, false, false, false, // 'X', 'Y', 'Z', ---, ---, ---, ---, ---
        false, true,  true,  true,  true,  true,  true,  true,  // ---, 'a', 'b', 'c', 'd', 'e', 'f', 'g' 
        true,  true,  true,  true,  true,  true,  true,  true,  // 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o'
        true,  true,  true,  true,  true,  true,  true,  true,  // 'p', 'q', 'r', 's', 't', 'u', 'v', 'w'
        true,  true,  true,  false, false, false, false, false  // 'x', 'y', 'z', ---, ---, ---, ---, ---
        };
    
    /**
     * 
     * Creates a new instance of PrintableStringSyntaxChecker.
     *
     */
    public PrintableStringSyntaxChecker()
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#assertSyntax(java.lang.Object)
     */
    public void assertSyntax( Object value ) throws NamingException
    {
        if ( ! isValidSyntax( value ) )
        {
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#getSyntaxOid()
     */
    public String getSyntaxOid()
    {
        return OID;
    }
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
     */
    public boolean isValidSyntax( Object value )
    {
        String strValue;

        if ( value == null )
        {
            return false;
        }
        
        if ( value instanceof String )
        {
            strValue = ( String ) value;
        }
        else if ( value instanceof byte[] )
        {
            strValue = StringTools.utf8ToString( ( byte[] ) value ); 
        }
        else
        {
            strValue = value.toString();
        }

        if ( strValue.length() == 0 )
        {
            return false;
        }
        
        // We must have at least one char
        if ( strValue.length() == 0 )
        {
            return false;
        }
        
        for ( int i = 0; i < strValue.length(); i++ )
        {
            char c = strValue.charAt( i );
            
            if ( ( c > 127 ) || !IS_PRINTABLE_CHAR[ c ] )
            {
                return false;
            }
        }
        
        return true;
    }
}
