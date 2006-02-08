/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.filter;


import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.io.IOException;
import java.text.ParseException;

import antlr.*;

import org.apache.directory.shared.ldap.filter.AntlrFilterLexer;
import org.apache.directory.shared.ldap.filter.AntlrFilterParser;
import org.apache.directory.shared.ldap.filter.AntlrFilterValueLexer;
import org.apache.directory.shared.ldap.filter.AntlrFilterValueParser;
import org.apache.directory.shared.ldap.util.StringTools;


public class FilterParserImpl implements FilterParser
{
    private AntlrFilterParser parser;

    private PipedOutputStream parserPipe;

    private AntlrFilterLexer lexer;

    private TokenStreamSelector selector;

    private LexerSharedInputState state;

    private AntlrFilterValueLexer valueLexer;

    private AntlrFilterValueParser valueParser;


    /**
     * Creates a filter parser implementation.
     */
    public FilterParserImpl()
    {
        init();
    }


    /**
     * Initializes the filter parser.
     */
    private synchronized void init()
    {
        // build the pipe used to feed the parser data and reusing it
        this.parserPipe = new PipedOutputStream();
        PipedInputStream pipeTail = new PipedInputStream();

        try
        {
            this.parserPipe.connect( pipeTail );
        }
        catch ( IOException e )
        {
            // this never blows chuncks and if it does we report!
            throw new InternalError();
        }

        this.state = new LexerSharedInputState( pipeTail );
        this.lexer = new AntlrFilterLexer( state );
        this.valueLexer = new AntlrFilterValueLexer( state );

        this.selector = new TokenStreamSelector();
        this.selector.addInputStream( this.lexer, AntlrFilterLexer.SELECTOR_KEY );
        this.selector.addInputStream( this.valueLexer, AntlrFilterValueLexer.SELECTOR_KEY );
        this.selector.select( this.lexer );

        this.parser = new AntlrFilterParser( this.selector );
        this.parser.setSelector( this.selector );
        this.parser.setValueLexer( this.valueLexer );
        this.parser.setValueParser( this.valueParser );

        this.valueParser = new AntlrFilterValueParser( this.selector );
        this.valueParser.setSelector( this.selector );
        this.valueParser.setLexer( this.lexer );

        this.parser.setValueParser( this.valueParser );
    }


    public synchronized ExprNode parse( String filter ) throws ParseException, IOException
    {
        ExprNode root = null;

        if ( filter == null || filter.trim().equals( "" ) )
        {
            return null;
        }

        if ( filter.indexOf( "**" ) > -1 )
        {
            filter = StringTools.trimConsecutiveToOne( filter, '*' );
        }

        this.parserPipe.write( filter.getBytes() );
        this.parserPipe.write( '\n' );
        this.parserPipe.flush();

        try
        {
            root = this.parser.filter();
            this.state.reset();
            this.selector.select( this.lexer );
        }
        catch ( RecognitionException e )
        {
            // @todo either use ExceptionUtils here or switch to throwing a
            // naming exception instead.
            String msg = "Parser failure on filter:\n\t" + filter;
            msg += "\nAntlr exception trace:\n" + e.getMessage();
            init();
            throw new ParseException( msg, e.getColumn() );
        }
        catch ( TokenStreamException e2 )
        {
            String msg = "Parser failure on filter:\n\t" + filter;
            msg += "\nAntlr exception trace:\n" + e2.getMessage();
            init();
            throw new ParseException( msg, 0 );
        }

        return root;
    }


    public void setFilterParserMonitor( FilterParserMonitor monitor )
    {
        this.parser.setFilterParserMonitor( monitor );
        this.valueParser.setFilterParserMonitor( monitor );
    }
}
