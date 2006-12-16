package org.apache.directory.shared.ldap.schema.syntax.parser;

import java.io.StringReader;
import java.text.ParseException;

import org.apache.directory.shared.ldap.schema.syntax.AbstractSchemaDescription;

public abstract class AbstractSchemaParser
{


    /** the antlr generated parser being wrapped */
    protected ReusableAntlrSchemaParser parser;

    /** the antlr generated lexer being wrapped */
    protected ReusableAntlrSchemaLexer lexer;
    
    
    protected AbstractSchemaParser() 
    {
        lexer = new ReusableAntlrSchemaLexer( new StringReader( "" ) );
        parser = new ReusableAntlrSchemaParser( lexer );
    }
    
    /**
     * Initializes the plumbing by creating a pipe and coupling the parser/lexer
     * pair with it. param spec the specification to be parsed
     */
    protected void reset( String spec )
    {
        StringReader in = new StringReader( spec );
        lexer.prepareNextInput( in );
        parser.resetState();
    }
    
    public abstract AbstractSchemaDescription parse( String schemaDescription ) throws ParseException;
    
}
