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

import org.apache.directory.shared.ldap.schema.syntax.parser.NameFormDescriptionSchemaParser;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value follows the
 * name descripton syntax according to RFC 4512, par 4.2.7.2:
 * 
 * <pre>
 * NameFormDescription = LPAREN WSP
 *    numericoid                 ; object identifier
 *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *    [ SP "DESC" SP qdstring ]  ; description
 *    [ SP "OBSOLETE" ]          ; not active
 *    SP "OC" SP oid             ; structural object class
 *    SP "MUST" SP oids          ; attribute types
 *    [ SP "MAY" SP oids ]       ; attribute types
 *    extensions WSP RPAREN      ; extensions
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameFormDescriptionSyntaxChecker extends AbstractSyntaxChecker
{

    /** The Syntax OID, according to RFC 4517, par. 3.3.22 */
    private static final String SC_OID = "1.3.6.1.4.1.1466.115.121.1.35";

    /** The schema parser used to parse the DITContentRuleDescription Syntax */
    private NameFormDescriptionSchemaParser schemaParser = new NameFormDescriptionSchemaParser();


    /**
     * 
     * Creates a new instance of DITContentRuleDescriptionSyntaxChecker.
     *
     */
    public NameFormDescriptionSyntaxChecker()
    {
        super( SC_OID );
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
            schemaParser.parseNameFormDescription( strValue );
            return true;
        }
        catch ( ParseException pe )
        {
            return false;
        }
    }
}
