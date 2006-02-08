/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.schema;


import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;


/**
 * A SyntaxChecker implemented using Perl5 regular expressions to constrain 
 * values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RegexSyntaxChecker
    implements SyntaxChecker
{
    /** the oid of the syntax checked */
    private final String oid;
    /** the set of regular expressions */
    private final String [] expressions;


    /**
     * Creates a Syntax validator for a specific Syntax using Perl5 matching
     * rules for validation.
     * 
     * @param oid the oid of the Syntax values checked
     * @param matchExprArray the array of matching expressions
     */
    public RegexSyntaxChecker( String oid, String [] matchExprArray )
    {
        expressions = matchExprArray;
        this.oid = oid;
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#getSyntaxOid()
     */
    public String getSyntaxOid()
    {
        return oid;
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
     */
    public boolean isValidSyntax( Object value )
    {
        String str = null;
        boolean match = true;
        
        if ( value instanceof String )
        {
            str = ( String ) value;

	        for ( int i = 0; i < expressions.length; i++ )
	        {
	            match = match && str.matches( expressions[i] );
	            
	            if ( ! match )
	            {
	                break;
	            }
	        }
        }
        
        return match;
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#assertSyntax(java.lang.Object)
     */
    public void assertSyntax( Object value ) throws NamingException
    {
        if ( isValidSyntax( value ) )
        {
            return;
        }
        
        throw new InvalidAttributeValueException( value
                + " does not conform to the syntax specified by " 
                + oid );
    }
}
