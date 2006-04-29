/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.trigger;


import java.io.StringReader;
import java.text.ParseException;

import org.apache.directory.shared.ldap.name.NameComponentNormalizer;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A reusable wrapper around the antlr generated parser for an ACIItem as
 * defined by X.501. This class enables the reuse of the antlr parser/lexer pair
 * without having to recreate them every time.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 379008 $
 */
public class TriggerSpecificationParser
{
    /** the antlr generated parser being wrapped */
    private ReusableAntlrTriggerSpecificationParser parser;

    /** the antlr generated lexer being wrapped */
    private ReusableAntlrTriggerSpecificationLexer lexer;

    private final boolean isNormalizing;


    /**
     * Creates a ACIItem parser.
     */
    public TriggerSpecificationParser()
    {
        this.lexer = new ReusableAntlrTriggerSpecificationLexer( new StringReader( "" ) );
        this.parser = new ReusableAntlrTriggerSpecificationParser( lexer );

        this.parser.init(); // this method MUST be called while we cannot do
        // constructor overloading for antlr generated parser
        this.isNormalizing = false;
    }


    /**
     * Creates a normalizing ACIItem parser.
     */
    public TriggerSpecificationParser(NameComponentNormalizer normalizer)
    {
        this.lexer = new ReusableAntlrTriggerSpecificationLexer( new StringReader( "" ) );
        this.parser = new ReusableAntlrTriggerSpecificationParser( lexer );

        this.parser.setNormalizer( normalizer );
        this.parser.init(); // this method MUST be called while we cannot do
        // constructor overloading for antlr generated parser
        this.isNormalizing = true;
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
     * Parses an ACIItem without exhausting the parser.
     * 
     * @param spec
     *            the specification to be parsed
     * @return the specification bean
     * @throws ParseException
     *             if there are any recognition errors (bad syntax)
     */
    public synchronized void parse( String spec ) throws ParseException
    {

        if ( spec == null || spec.trim().equals( "" ) )
        {
            return;
        }

        reset( spec ); // reset and initialize the parser / lexer pair

        try
        {
            this.parser.wrapperEntryPoint();
        }
        catch ( TokenStreamException e )
        {
            String msg = "TokenStreamException: Parser failure on ACIItem:\n\t" + spec;
            msg += "\nAntlr exception trace:\n";
            e.printStackTrace();
            throw new ParseException( msg, 0 );
        }
        catch ( RecognitionException e )
        {
            String msg = "RecognitionException: Parser failure on ACIItem:\n\t" + spec;
            msg += "\nAntlr exception trace:\n";
            e.printStackTrace();
            throw new ParseException( msg, e.getColumn() );
        }

    }


    /**
     * Tests to see if this parser is normalizing.
     * 
     * @return true if it normalizes false otherwise
     */
    public boolean isNormizing()
    {
        return this.isNormalizing;
    }
}
