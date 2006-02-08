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


import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.DnParser;


/**
 * A distinguished name syntax checker.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DnSyntaxChecker implements SyntaxChecker
{
    /** The oid of the checked syntax */
    private final String oid;

    /** The parser used to parse the DN */
    private NameParser parser;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a SyntaxChecker for a DN based Syntax that uses a distinguished
     * name parser.
     * 
     * @param oid
     *            the OID of the syntax
     */
    public DnSyntaxChecker(String oid) throws NamingException
    {
        this.oid = oid;

        /*
         * One may ask: why are we not taking the same measures here to
         * introduce a name component normalizer? The answer to this is well we
         * don't care what the value of an attribute assertion is we only care
         * that the DN containing it is properly formed.
         */
        parser = new DnParser();
    }


    // ------------------------------------------------------------------------
    // SyntaxChecker Methods
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#getSyntaxOid()
     */
    public String getSyntaxOid()
    {
        return oid;
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(Object)
     */
    public boolean isValidSyntax( Object value )
    {
        if ( value instanceof Name )
        {
            return true;
        }
        else if ( value instanceof String )
        {
            try
            {
                parser.parse( ( String ) value );
            }
            catch ( Exception e )
            {
                return false;
            }

            return true;
        }

        return false;
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#assertSyntax(Object)
     */
    public void assertSyntax( Object value ) throws NamingException
    {
        if ( value instanceof Name )
        {
            return;
        }
        else if ( value instanceof String )
        {
            parser.parse( ( String ) value );
        }

        throw new NamingException( "Do not know how syntax check instances of " + value.getClass() );
    }
}
