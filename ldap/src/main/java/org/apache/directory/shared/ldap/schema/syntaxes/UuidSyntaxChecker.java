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
package org.apache.directory.shared.ldap.schema.syntaxes;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.AbstractSyntaxChecker;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;


/**
 * An UUID syntax checker.
 * 
 * UUID ::= OCTET STRING (SIZE(16)) -- constrained to an UUID [RFC4122]
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 736240 $
 */
public class UuidSyntaxChecker extends AbstractSyntaxChecker
{
    /** the Apache assigned internal OID for this syntax checker */
    public static final SyntaxChecker INSTANCE = new UuidSyntaxChecker();


    /**
     * Creates a new instance of UUIDSyntaxChecker.
     */
    public UuidSyntaxChecker()
    {
        super( SchemaConstants.UUID_SYNTAX );
    }


    /**
     * 
     * Creates a new instance of UUIDSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected UuidSyntaxChecker( String oid )
    {
        super( oid );
    }

    
    /**
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(Object)
     * 
     * @param value the value of some attribute with the syntax
     * @return true if the value is in the valid syntax, false otherwise
     */
    public boolean isValidSyntax( Object value )
    {
        if ( value == null )
        {
            return false;
        }
        
        if ( ! ( value instanceof byte[] ) )
        {
            return false;
        }
        
        byte[] uuid = (byte[])value;
        
        // The UUID must have a length of 16 bytes
        if ( uuid.length != 16 )
        {
            return false;
        }
        
        // There is not that much we can check.
        return true;
    }
}
