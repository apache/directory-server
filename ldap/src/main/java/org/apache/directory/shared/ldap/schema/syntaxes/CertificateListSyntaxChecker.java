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


/**
 * A SyntaxChecker which verifies that a value is a certificateList according to RFC 4523 :
 * 
 * "Due to changes made to the definition of a CertificateList through time,
 *  no LDAP-specific encoding is defined for this syntax.  Values of this
 *  syntax SHOULD be encoded using Distinguished Encoding Rules (DER)
 *  [X.690] and MUST only be transferred using the ;binary transfer
 *  option"
 * 
 * It has been removed in RFC 4517
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 488616 $
 */
public class CertificateListSyntaxChecker extends BinarySyntaxChecker
{
    /**
     * Private default constructor to prevent unnecessary instantiation.
     */
    public CertificateListSyntaxChecker()
    {
        super( SchemaConstants.CERTIFICATE_LIST_SYNTAX );
    }

    /**
     * 
     * Creates a new instance of CertificateListSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected CertificateListSyntaxChecker( String oid )
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
        return true;
    }
}
