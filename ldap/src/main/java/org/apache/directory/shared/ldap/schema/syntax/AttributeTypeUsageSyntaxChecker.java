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
 * A syntax checker which checks to see if an attributeType's type is either: 
 * userApplications
 * directoryOperation
 * distributedOperation
 * dSAOperation
.*  The case is NOT ignored.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypeUsageSyntaxChecker extends AbstractSyntaxChecker
{
    /** The Apache OID for meta syntax checker */
    private static final String SC_OID = "1.3.6.1.4.1.18060.0.4.0.0.3";

    /**
     * 
     * Creates a new instance of AttributeTypeUsageSyntaxChecker.
     *
     */
    public AttributeTypeUsageSyntaxChecker()
    {
        super( SC_OID );
    }

    /**
     * 
     * Creates a new instance of AttributeTypeUsageSyntaxChecker.
     * 
     * @param the oid to associate with this new SyntaxChecker
     *
     */
    protected AttributeTypeUsageSyntaxChecker( String oid )
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

        if ( strValue.length() < "userApplications".length() || 
            strValue.length() > "userApplications".length() )
        {
            return false;
        }
        
        char ch = strValue.charAt( 0 );

        switch ( ch )
        {
            case( 'd' ):
                if ( "dSAOperation".equals( strValue ) ||
                     "directoryOperation".equals( strValue ) ||
                     "distributedOperation".equals( strValue ) )
                {
                    return true;
                }
                else
                {
                    return false;
                }
            
            case( 'u' ):
                return "userApplications".equals( strValue );
            
            default:
                return false;
        }
    }
}
