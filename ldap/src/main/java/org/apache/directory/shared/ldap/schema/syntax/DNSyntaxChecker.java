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


import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is a valid DN. We just check
 * that the DN is valid, we don't need to verify each of the RDN syntax.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DNSyntaxChecker extends AbstractSyntaxChecker
{
    /** The Syntax OID, according RFC 4517, par. 3.3.9 */
    private static final String SC_OID = "1.3.6.1.4.1.1466.115.121.1.12";
    
    /**
     * 
     * Creates a new instance of DNSyntaxChecker.
     *
     */
    public DNSyntaxChecker()
    {
        super( SC_OID );
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
        
        // Check that the value is a valid DN
        return LdapDN.isValid( strValue );
    }
}
