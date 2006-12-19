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


import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is a numeric oid and a length
 * constraint according to RFC 4512.
 * 
 * From RFC 4512 :
 * 
 * noidlen    = numericoid [ LCURLY len RCURLY ]
 * numericoid = number 1*( DOT number )
 * len        = number
 * number     = DIGIT | ( LDIGIT 1*DIGIT )
 * DIGIT      = %x30 | LDIGIT                  ; "0"-"9"
 * LDIGIT     = %x31-39                        ; "1"-"9"
 * DOT        = %x2E                           ; period (".")
 * LCURLY  = %x7B                              ; left curly brace "{"
 * RCURLY  = %x7D                              ; right curly brace "}"
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OidLenSyntaxChecker extends AbstractSyntaxChecker
{
    /** The Syntax OID */
    private static final String SC_OID = "1.3.6.1.4.1.18060.0.4.0.0.5";
    
    /**
     * 
     * Creates a new instance of OidLenSyntaxChecker.
     *
     */
    public OidLenSyntaxChecker()
    {
        super( SC_OID );
    }
    
    /**
     * 
     * Creates a new instance of OidLenSyntaxChecker.
     * 
     * @param the oid to associate with this new SyntaxChecker
     *
     */
    protected OidLenSyntaxChecker( String oid )
    {
        super( oid );
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
        
        // We are looking at the first position of the len part
        int pos = strValue.indexOf( '{' );
            
        if ( pos < 0 )
        {
            // Not found ... but it may still be a valid OID
            return OID.isOID( strValue );
        }
        else
        {
            // we should have a len value. First check that the OID is valid
            String oid = strValue.substring( 0, pos );
            
            if ( !OID.isOID( oid ) )
            {
                return false;
            }
            
            String len = strValue.substring( pos );
            
            // We must have a lnumber and a '}' at the end
            if ( len.charAt( len.length() -1 ) != '}' )
            {
                // No final '}'
                return false;
            }
            
            for ( int i = 1; i < len.length() - 1; i++ )
            {
                switch ( len.charAt(i) )
                {
                    case '0': case '1': case '2' : case '3': case '4':
                    case '5': case '6': case '7' : case '8': case '9':
                        break;
                        
                    default: 
                        return false;
                }
            }
            
            if ( ( len.charAt( 1 ) == '0' ) && len.length() > 3 )
            {
                // A number can't start with a '0' unless it's the only
                // number
                return false;
            }
            
            return true;
        }
    }
}
