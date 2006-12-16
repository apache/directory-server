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


import java.text.ParseException;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.syntax.parser.MatchingRuleDescriptionSchemaParser;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value follows the
 * matching rule descripton syntax according to RFC 4512, par 4.2.3:
 * 
 *  <pre>
 * MatchingRuleDescription = LPAREN WSP
 *    numericoid                 ; object identifier
 *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *    [ SP "DESC" SP qdstring ]  ; description
 *    [ SP "OBSOLETE" ]          ; not active
 *    SP "SYNTAX" SP numericoid  ; assertion syntax
 *    extensions WSP RPAREN      ; extensions
 * 
 * extensions = *( SP xstring SP qdstrings )
 * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
 * 
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MatchingRuleDescriptionSyntaxChecker implements SyntaxChecker
{

    /** The Syntax OID, according to RFC 4517, par. 3.3.19 */
    public static final String OID = "1.3.6.1.4.1.1466.115.121.1.30";

	/** The schema parser used to parse the MatchingRuleDescription Syntax */
    private MatchingRuleDescriptionSchemaParser schemaParser = new MatchingRuleDescriptionSchemaParser();


    /**
     * 
     * Creates a new instance of MatchingRuleDescriptionSchemaParser.
     *
     */
    public MatchingRuleDescriptionSyntaxChecker()
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#getSyntaxOid()
     */
    public String getSyntaxOid()
    {
        return OID;
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

        try
        {
            MatchingRuleDescription mrd = schemaParser.parseMatchingRuleDescription( strValue );
            
            // SYNTAX must be present
            if ( ( mrd.getSyntax() == null ) ) 
            {
                return false;
            }
            
            return true;
        }
        catch ( ParseException pe )
        {
            return false;
        }
    }
    
}
