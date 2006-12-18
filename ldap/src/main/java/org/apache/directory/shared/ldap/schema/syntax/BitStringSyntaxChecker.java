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


import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is a Boolean according to RFC 4517.
 * 
 * From RFC 4512 & RFC 4517 :
 * 
 * BitString    = SQUOTE *binary-digit SQUOTE "B"
 * binary-digit = "0" / "1"
 * SQUOTE  = %x27                           ; hyphen ("'")
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BitStringSyntaxChecker extends AbstractSyntaxChecker
{
    /** The Syntax OID, according to RFC 4517, par. 3.3.2 */
    private static final String SC_OID = "1.3.6.1.4.1.1466.115.121.1.6";
    
    /**
     * 
     * Creates a new instance of BitStringSyntaxChecker.
     *
     */
    public BitStringSyntaxChecker()
    {
        super( SC_OID );
    }
    
    /**
     * A shared and static method used to check that the string is a BitString.
     * A BitString is a string of bits, between quots and followed by a 'B' :
     * 
     * '01010110'B for instance
     * 
     * @param strValue The string to check
     * @return <code>true</code> if teh string is a BitString
     */
    public static boolean isValid( String strValue )
    {
        if ( strValue.length() == 0 )
        {
            return false;
        }
        
        int pos = 0;
        
        // Check that the String respect the syntax : ' ([01]+) ' B
        if ( ! StringTools.isCharASCII( strValue, pos++, '\'' ) )
        {
            return false;
        }

        // We must have at least one bit
        if ( ! StringTools.isBit( strValue, pos++ ) )
        {
            return false;
        }
        
        while ( StringTools.isBit( strValue, pos ) )
        {
            // Loop until we get a char which is not a 0 or a 1
            pos++;
        }

        // Now, we must have a simple quote 
        if ( ! StringTools.isCharASCII( strValue, pos++, '\'' ) )
        {
            return false;
        }

        // followed by a 'B'
        if ( ! StringTools.isCharASCII( strValue, pos, 'B' ) )
        {
            return false;
        }

        return true;
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

        return isValid( strValue );
    }
}
