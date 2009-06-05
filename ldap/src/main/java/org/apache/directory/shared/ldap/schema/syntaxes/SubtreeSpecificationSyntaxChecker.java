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

import java.text.ParseException;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.AbstractSyntaxChecker;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationChecker;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is a subtree specification.
 * 
 * It has been removed in RFC 4517
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 488616 $
 */
public class SubtreeSpecificationSyntaxChecker extends AbstractSyntaxChecker
{
    private SubtreeSpecificationChecker checker = new SubtreeSpecificationChecker();

    /**
     * Private default constructor to prevent unnecessary instantiation.
     */
    public SubtreeSpecificationSyntaxChecker()
    {
        super( SchemaConstants.SUBTREE_SPECIFICATION_SYNTAX );
    }

    /**
     * 
     * Creates a new instance of SubtreeSpecificationSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected SubtreeSpecificationSyntaxChecker( String oid )
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
        String strValue = null;

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

        try
        {
            synchronized( checker )
            {
                checker.parse( strValue );
            }
            return true;
        }
        catch ( ParseException pe )
        {
            return false;
        }
    }
}
