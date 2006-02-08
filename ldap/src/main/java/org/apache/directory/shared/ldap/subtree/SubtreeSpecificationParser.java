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

package org.apache.directory.shared.ldap.subtree;

import java.io.StringReader;
import java.text.ParseException;

import org.apache.directory.shared.ldap.name.NameComponentNormalizer;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A reusable wrapper around the antlr generated parser for an LDAP subtree
 * specification as defined by <a href="http://www.faqs.org/rfcs/rfc3672.html">
 * RFC 3672</a>.  This class enables the reuse of the antlr parser/lexer pair
 * without having to recreate the pair every time.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeSpecificationParser
{
    /** the antlr generated parser being wrapped */
    private ReusableAntlrSubtreeSpecificationParser parser;
    /** the antlr generated lexer being wrapped */
    private ReusableAntlrSubtreeSpecificationLexer lexer;

    private final boolean isNormalizing;

    /**
     * Creates a subtree specification parser.
     */
    public SubtreeSpecificationParser()
    {
        StringReader in = new StringReader(""); // place holder for the first input
        this.lexer = new ReusableAntlrSubtreeSpecificationLexer( in );
        this.parser = new ReusableAntlrSubtreeSpecificationParser( lexer );
        this.parser.init(); // this method MUST be called while we cannot do
                            // constructor overloading for antlr generated parser
        this.isNormalizing = false;
    }
    
    /**
     * Creates a normalizing subtree specification parser.
     */
    public SubtreeSpecificationParser(NameComponentNormalizer normalizer)
    {
        StringReader in = new StringReader(""); // place holder for the first input
        this.lexer = new ReusableAntlrSubtreeSpecificationLexer( in );
        this.parser = new ReusableAntlrSubtreeSpecificationParser( lexer );
        
        this.parser.setNormalizer( normalizer );
        this.parser.init(); // this method MUST be called while we cannot do
                            // constructor overloading for antlr generated parser
        this.isNormalizing = true;
    }


    /**
     * Initializes the plumbing by creating a pipe and coupling the parser/lexer
     * pair with it.
     * 
     * param spec the specification to be parsed
     */
    private synchronized void reset(String spec)
    {
        StringReader in = new StringReader( spec + "end" ); // append end of input token
        this.lexer.prepareNextInput(in);
        this.parser.resetState();
    }


    /**
     * Parses a subtree specification without exhausting the parser.
     *
     * @param spec the specification to be parsed
     * @return the specification bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized SubtreeSpecification parse( String spec ) throws ParseException
    {
        SubtreeSpecification ss = null;

        if ( spec == null || spec.trim().equals( "" ) )
        {
            return null;
        }
        
        reset(spec); // reset and initialize the parser / lexer pair

        try
        {
            ss = this.parser.wrapperEntryPoint();
        }
        catch ( TokenStreamException e )
        {
            String msg = "Parser failure on subtree specification:\n\t" + spec ;
            msg += "\nAntlr exception trace:\n" + e.getMessage();
            throw new ParseException( msg, 0 );
        }
        catch ( RecognitionException e )
        {
            String msg = "Parser failure on subtree specification:\n\t" + spec ;
            msg += "\nAntlr exception trace:\n" + e.getMessage();
            throw new ParseException( msg, e.getColumn() );
        }  
        catch ( Exception e )
        {
            String msg = "Parser failure on subtree specification:\n\t" + spec ;
            msg += "\nAntlr exception trace:\n" + e.getMessage();
            throw new ParseException( msg, 0 );
        }

        return ss;
    }
    
    /**
     * Tests to see if this parser is normalizing.
     *
     * @return true if it normalizes false otherwise
     */
    public boolean isNormizing()
    {
        return this.isNormalizing ;
    }
}
