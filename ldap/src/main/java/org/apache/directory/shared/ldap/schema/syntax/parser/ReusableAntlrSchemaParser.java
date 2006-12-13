package org.apache.directory.shared.ldap.schema.syntax.parser;

import org.apache.directory.shared.ldap.schema.syntax.AntlrSchemaParser;

import antlr.TokenStream;

/**
 * A reusable parser class extended from antlr generated parser for an LDAP
 * schema as defined in RFC 4512. This class
 * enables the reuse of the antlr parser without having to recreate the it every
 * time as stated in <a
 * href="http://www.antlr.org:8080/pipermail/antlr-interest/2003-April/003631.html">
 * a Antlr Interest Group mail</a> .
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
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