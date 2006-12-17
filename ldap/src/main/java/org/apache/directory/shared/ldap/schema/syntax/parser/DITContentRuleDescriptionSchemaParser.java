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
package org.apache.directory.shared.ldap.schema.syntax.parser;


import java.text.ParseException;

import org.apache.directory.shared.ldap.schema.syntax.AbstractSchemaDescription;
import org.apache.directory.shared.ldap.schema.syntax.DITContentRuleDescription;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A parser for RFC 4512 DIT content rule descriptons
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DITContentRuleDescriptionSchemaParser extends AbstractSchemaParser
{

    /**
     * Creates a schema parser instance.
     */
    public DITContentRuleDescriptionSchemaParser()
    {
        super();
    }


    /**
     * Parses a DIT content rule description according to RFC 4512:
     * 
     * <pre>
     * DITContentRuleDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    [ SP "AUX" SP oids ]       ; auxiliary object classes
     *    [ SP "MUST" SP oids ]      ; attribute types
     *    [ SP "MAY" SP oids ]       ; attribute types
     *    [ SP "NOT" SP oids ]       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
     * 
     * @param ditContentRuleDescription the DIT content rule description to be parsed
     * @return the parsed DITContentRuleDescription bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized DITContentRuleDescription parseDITContentRuleDescription( String ditContentRuleDescription )
        throws ParseException
    {

        if ( ditContentRuleDescription == null )
        {
            throw new ParseException( "Null", 0 );
        }

        reset( ditContentRuleDescription ); // reset and initialize the parser / lexer pair

        try
        {
            DITContentRuleDescription dcrd = parser.ditContentRuleDescription();
            return dcrd;
        }
        catch ( RecognitionException re )
        {
            String msg = "Parser failure on DIT content rule description:\n\t" + ditContentRuleDescription;
            msg += "\nAntlr message: " + re.getMessage();
            msg += "\nAntlr column: " + re.getColumn();
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = "Parser failure on DIT content rule description:\n\t" + ditContentRuleDescription;
            msg += "\nAntlr message: " + tse.getMessage();
            throw new ParseException( msg, 0 );
        }

    }


    public AbstractSchemaDescription parse( String schemaDescription ) throws ParseException
    {
        return parseDITContentRuleDescription( schemaDescription );
    }

}
