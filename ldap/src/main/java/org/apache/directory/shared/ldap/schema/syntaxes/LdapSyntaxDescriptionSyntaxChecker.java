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
import org.apache.directory.shared.ldap.schema.parsers.LdapSyntaxDescriptionSchemaParser;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value follows the
 * LDAP syntax descripton syntax according to RFC 4512, par 4.2.2:
 * 
 * <pre>
 * SyntaxDescription = LPAREN WSP
 *    numericoid                 ; object identifier
 *    [ SP "DESC" SP qdstring ]  ; description
 *    extensions WSP RPAREN      ; extensions
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapSyntaxDescriptionSyntaxChecker extends AbstractSyntaxChecker
{
    /** The schema parser used to parse the LdapSyntaxDescription Syntax */
    private LdapSyntaxDescriptionSchemaParser schemaParser = new LdapSyntaxDescriptionSchemaParser();


    /**
     * 
     * Creates a new instance of AttributeTypeDescriptionSchemaParser.
     *
     */
    public LdapSyntaxDescriptionSyntaxChecker()
    {
        super( SchemaConstants.LDAP_SYNTAX_DESCRIPTION_SYNTAX );
    }

    /**
     * 
     * Creates a new instance of LdapSyntaxDescriptionSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected LdapSyntaxDescriptionSyntaxChecker( String oid )
    {
        super( oid );
    }
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
     */
    public boolean isValidSyntax( Object value )
    {
        String strValue =null;

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
            schemaParser.parseLdapSyntaxDescription( strValue );
            return true;
        }
        catch ( ParseException pe )
        {
            return false;
        }
    }
}
