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


/**
 * A binary value (universal value acceptor) syntax checker.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BinarySyntaxChecker implements SyntaxChecker
{
    /** an instance so we don't have to create one every time */
    public static final SyntaxChecker INSTANCE = new BinarySyntaxChecker();

    /** the Apache assigned internal OID for this syntax checker */
    public static final String OID = "1.3.6.1.4.1.1466.115.121.1.5";


    /**
     * Gets the singleton instance for this class.
     * 
     * @return the singleton instance
     */
    public static SyntaxChecker getSingletonInstance()
    {
        return INSTANCE;
    }


    /**
     * Private default constructor to prevent unnecessary instantiation.
     */
    private BinarySyntaxChecker()
    {
        // so people are not creating this unnecesarily
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#assertSyntax(Object)
     */
    public void assertSyntax( Object value ) throws NamingException
    {
        // do nothing because everything is valid
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#getSyntaxOid()
     */
    public String getSyntaxOid()
    {
        return OID;
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(Object)
     */
    public boolean isValidSyntax( Object value )
    {
        return true;
    }
}
