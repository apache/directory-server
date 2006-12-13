package org.apache.directory.shared.ldap.schema.syntax.parser;

import java.io.Reader;

import org.apache.directory.shared.ldap.schema.syntax.AntlrSchemaLexer;

import antlr.CharBuffer;
import antlr.LexerSharedInputState;

/**
 * A reusable lexer class extended from antlr generated lexer for an LDAP
 * schema as defined in RFC 4512. This class
 * enables the reuse of the antlr lexer without having to recreate the it every
 * time as stated in <a
 * href="http://www.antlr.org:8080/pipermail/antlr-interest/2003-April/003631.html">
 * a Antlr Interest Group mail</a> .
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
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