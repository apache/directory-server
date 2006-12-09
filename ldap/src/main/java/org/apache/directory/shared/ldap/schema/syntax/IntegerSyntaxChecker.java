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
 * A SyntaxChecker which verifies that a value is an Integer according to RFC 4517.
 * 
 * From RFC 4517 :
 * 
 * Integer = ( HYPHEN LDIGIT *DIGIT ) | number
 * 
 * From RFC 4512 :
 * number  = DIGIT | ( LDIGIT 1*DIGIT )
 * DIGIT   = %x30 | LDIGIT       ; "0"-"9"
 * LDIGIT  = %x31-39             ; "1"-"9"
 * HYPHEN  = %x2D                ; hyphen ("-")
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IntegerSyntaxChecker implements SyntaxChecker
{
    /** The Syntax OID, according to RFC 4517, par. 3.3.16 */
    public static final String OID = "1.3.6.1.4.1.1466.115.121.1.27";
    
    /**
     * 
     * Creates a new instance of IntegerSyntaxChecker.
     *
     */
    public IntegerSyntaxChecker()
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
        
        // The first char must be either a '-' or in [0..9].
        // If it's a '0', then there should be any other char after
        int pos = 0;
        char c = strValue.charAt( pos );
        
        if ( c == '-' )
        {
            pos = 1;
        }
        else if ( !StringTools.isDigit( c ) )
        {
            return false;
        }
        else if ( c == '0' )
        {
            if ( strValue.length() > 1 )
            {
                return false;
            }
            else
            {
                return true;
            }
        }
            
        // We must have at least a digit which is not '0'
        if ( !StringTools.isDigit( strValue, pos ) )
        {
            return false;
        }
        else if ( StringTools.isCharASCII( strValue, pos, '0' ) )
        {
            return false;
        }
        else
        {
            pos++;
        }
        
        while ( StringTools.isDigit( strValue, pos) )
        {
            pos++;
        }
        
        return ( pos == strValue.length() );
    }
}
