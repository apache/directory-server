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
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;


/**
 * Document me.
 *
 * @todo put me into shared-ldap
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 485042 $
 */
public abstract class AbstractSyntaxChecker implements SyntaxChecker
{
    /** the oid of the syntax checker */
    private String oid = null;
    
    /**
     * 
     * Creates a new instance of AbstractSyntaxChecker.
     *
     * @param oid The inherited class OID
     */
    protected AbstractSyntaxChecker( String oid )
    {
        this.oid = oid;
    }

    /**
     * Gets the OID of this SyntaxChecker.
     *
     * @return the OID of this SyntaxChecker.
     */
    public String getSyntaxOid()
    {
        return oid;
    }
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#assertSyntax(java.lang.Object)
     */
    public void assertSyntax( Object value ) throws NamingException
    {
        if ( !isValidSyntax( value ) )
        {
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
        }
    }
}
