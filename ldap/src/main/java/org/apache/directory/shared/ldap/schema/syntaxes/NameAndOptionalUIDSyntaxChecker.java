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
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AbstractSyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is a valid Name and Optional UID.
 * 
 * This element is a composition of two parts : a DN and an optional UID :
 * NameAndOptionalUID = distinguishedName [ SHARP BitString ]
 * 
 * Both part already have their syntax checkers, so we will just call them
 * after having splitted the element in two ( if necessary)
 * 
 * We just check that the DN is valid, we don't need to verify each of the RDN 
 * syntax.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameAndOptionalUIDSyntaxChecker extends AbstractSyntaxChecker
{
    /**
     * 
     * Creates a new instance of NameAndOptionalUIDSyntaxChecker.
     *
     */
    public NameAndOptionalUIDSyntaxChecker()
    {
        super( SchemaConstants.NAME_AND_OPTIONAL_UID_SYNTAX );
    }
    
    
    /**
     * 
     * Creates a new instance of NameAndOptionalUIDSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected NameAndOptionalUIDSyntaxChecker( String oid )
    {
        super( oid );
    }
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
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
        
        // Let's see if we have an UID part
        int sharpPos = strValue.lastIndexOf( '#' );
        
        if ( sharpPos != -1 )
        {
            // Now, check that we don't have another '#'
            if ( strValue.indexOf( '#' ) != sharpPos )
            {
                // Yes, we have one : this is not allowed, it should have been
                // escaped.
                return false;
            }
            
            // This is an UID if the '#' is immediatly
            // followed by a BitString, except if the '#' is
            // on the last position
            // We shoould not find a
            if ( BitStringSyntaxChecker.isValid( strValue.substring( sharpPos + 1 ) ) && 
                 ( sharpPos < strValue.length() ) )
            {
                // Ok, we have a BitString, now check the DN,
                // except if the '#' is in first position
                if ( sharpPos > 0 )
                {
                    return LdapDN.isValid( strValue.substring( 0, sharpPos ) );
                }
                else
                {
                    // The DN must not be null ?
                    return false;
                }
            }
            else
            {
                // We have found a '#' but no UID part.
                return false;
            }
        }
        else
        {
            // No UID, the strValue is a DN
            // Check that the value is a valid DN
            return LdapDN.isValid( strValue );
        }
    }
}
