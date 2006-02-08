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


import org.apache.directory.shared.ldap.name.antlrValueLexer;

import antlr.LexerSharedInputState;


/**
 * A reusable lexer class extended from antlr generated antlrValueLexer
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReusableAntlrValueLexer extends antlrValueLexer
{
    private boolean savedCaseSensitive;

    private boolean savedCaseSensitiveLiterals;


    /**
     * Creates a ReusableAntlrValueLexer instance.
     * 
     * @param in
     *            the input to the lexer
     */
    public ReusableAntlrValueLexer(LexerSharedInputState inputState)
    {
        super( inputState );
        savedCaseSensitive = getCaseSensitive();
        savedCaseSensitiveLiterals = getCaseSensitiveLiterals();
    }


    /**
     * Resets the state of an antlr lexer and initializes it with new input.
     * 
     * @param in
     *            the input to the lexer
     */
    public void prepareNextInput( LexerSharedInputState inputState )
    {
        this.setInputState( inputState );

        this.setCaseSensitive( savedCaseSensitive );

        // no set method for this protected field.
        this.caseSensitiveLiterals = savedCaseSensitiveLiterals;
    }
}
