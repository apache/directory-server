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


package org.apache.directory.shared.ldap.name;


import java.io.Reader;

import org.apache.directory.shared.ldap.name.antlrTypeLexer;

import antlr.CharBuffer;
import antlr.LexerSharedInputState;


/**
 * A reusable lexer class extended from antlr generated antlrTypelexer
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReusableAntlrTypeLexer extends antlrTypeLexer
{
    private boolean savedCaseSensitive;
    private boolean savedCaseSensitiveLiterals;

    /**
     * Creates a ReusableAntlrValueLexer instance.
     *
     * @param in the input to the lexer
     */
    public ReusableAntlrTypeLexer( Reader in )
    {
        super( in );
        savedCaseSensitive = getCaseSensitive();
        savedCaseSensitiveLiterals = getCaseSensitiveLiterals();
    }


    /**
     * Resets the state of an antlr lexer and initializes it with new input.
     *
     * @param in the input to the lexer
     */
    public void prepareNextInput( Reader in )
    {
        CharBuffer buf = new CharBuffer( in );
        LexerSharedInputState state = new LexerSharedInputState( buf );
        this.setInputState(state);
        
        this.setCaseSensitive(savedCaseSensitive);
        
        // no set method for this protected field.
        this.caseSensitiveLiterals = savedCaseSensitiveLiterals;
    }
}
