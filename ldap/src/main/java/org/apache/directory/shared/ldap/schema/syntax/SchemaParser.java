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


import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;

import antlr.CharBuffer;
import antlr.LexerSharedInputState;
import antlr.RecognitionException;
import antlr.TokenStream;
import antlr.TokenStreamException;


/**
 * A parser for RFC 4512 schema objects
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaParser
{

    /** the antlr generated parser being wrapped */
    private ReusableAntlrSchemaParser parser;

    /** the antlr generated lexer being wrapped */
    private ReusableAntlrSchemaLexer lexer;


    /**
     * Creates a schema parser instance.
     */
    public SchemaParser()
    {
        this.lexer = new ReusableAntlrSchemaLexer( new StringReader( "" ) );
        this.parser = new ReusableAntlrSchemaParser( lexer );
    }


    /**
     * Initializes the plumbing by creating a pipe and coupling the parser/lexer
     * pair with it. param spec the specification to be parsed
     */
    private synchronized void reset( String spec )
    {
        StringReader in = new StringReader( spec );
        this.lexer.prepareNextInput( in );
        this.parser.resetState();
    }


    /**
     * Parses a object class definition according to RFC 4512:
     * 
     * <pre>
     * ObjectClassDescription = LPAREN WSP
     *     numericoid                 ; object identifier
     *     [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *     [ SP "DESC" SP qdstring ]  ; description
     *     [ SP "OBSOLETE" ]          ; not active
     *     [ SP "SUP" SP oids ]       ; superior object classes
     *     [ SP kind ]                ; kind of class
     *     [ SP "MUST" SP oids ]      ; attribute types
     *     [ SP "MAY" SP oids ]       ; attribute types
     *     extensions WSP RPAREN
     *
     * kind = "ABSTRACT" / "STRUCTURAL" / "AUXILIARY"
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
     * 
     * @param objectClassDescription the object class description to be parsed
     * @return the parsed ObjectClassDescription bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized ObjectClassDescription parseObjectClassDescription( String objectClassDescription )
        throws ParseException
    {

        if ( objectClassDescription == null )
        {
            throw new ParseException( "Null", 0 );
        }

        reset( objectClassDescription ); // reset and initialize the parser / lexer pair

        try
        {
            ObjectClassDescription ocd = parser.objectClassDescription();
            return ocd;
        }
        catch ( RecognitionException re )
        {
            String msg = "Parser failure on object class description:\n\t" + objectClassDescription;
            msg += "\nAntlr message: " + re.getMessage();
            msg += "\nAntlr column: " + re.getColumn();
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = "Parser failure on object class description:\n\t" + objectClassDescription;
            msg += "\nAntlr message: " + tse.getMessage();
            throw new ParseException( msg, 0 );
        }

    }

    /**
     * A reusable lexer class extended from antlr generated lexer for an LDAP
     * schema as defined in RFC 4512. This class
     * enables the reuse of the antlr lexer without having to recreate the it every
     * time as stated in <a
     * href="http://www.antlr.org:8080/pipermail/antlr-interest/2003-April/003631.html">
     * a Antlr Interest Group mail</a> .
     * 
     * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev: 437007 $
     */
    class ReusableAntlrSchemaLexer extends AntlrSchemaLexer
    {
        private boolean savedCaseSensitive;

        private boolean savedCaseSensitiveLiterals;


        /**
         * Creates a ReusableAntlrSchemaLexer instance.
         * 
         * @param in
         *            the input to the lexer
         */
        public ReusableAntlrSchemaLexer( Reader in )
        {
            super( in );
            savedCaseSensitive = getCaseSensitive();
            savedCaseSensitiveLiterals = getCaseSensitiveLiterals();
        }


        /**
         * Resets the state of an antlr lexer and initializes it with new input.
         * 
         * @param in
         *            the input to the lexer
         */
        public void prepareNextInput( Reader in )
        {
            CharBuffer buf = new CharBuffer( in );
            LexerSharedInputState state = new LexerSharedInputState( buf );
            this.setInputState( state );

            this.setCaseSensitive( savedCaseSensitive );

            // no set method for this protected field.
            this.caseSensitiveLiterals = savedCaseSensitiveLiterals;
        }
    }

    /**
     * A reusable parser class extended from antlr generated parser for an LDAP
     * schema as defined in RFC 4512. This class
     * enables the reuse of the antlr parser without having to recreate the it every
     * time as stated in <a
     * href="http://www.antlr.org:8080/pipermail/antlr-interest/2003-April/003631.html">
     * a Antlr Interest Group mail</a> .
     * 
     * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev: 437007 $
     */
    class ReusableAntlrSchemaParser extends AntlrSchemaParser
    {
        /**
         * Creates a ReusableAntlrSchemaParser instance.
         */
        public ReusableAntlrSchemaParser( TokenStream lexer )
        {
            super( lexer );
        }


        /**
         * Resets the state of an antlr parser.
         */
        public void resetState()
        {
            // no set method for this protected field.
            this.traceDepth = 0;

            this.getInputState().reset();
        }
    }
}
