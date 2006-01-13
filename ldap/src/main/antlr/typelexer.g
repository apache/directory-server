/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
header {
	/*
	 * Keep the semicolon right next to org.apache.ldap.common.name or else there
	 * will be a bug that comes into the foreground in the new antlr release.
	 */
    package org.apache.ldap.common.name;

    import antlr.TokenStreamSelector ;
}


/**
 * Lexer used to scan the attribute type character stream from the start of
 * an attribute type to the equal sign.  It implements the regular expressions
 * (a.k.a lexer rules) for accepting a 'attributeType' as specified in 
 * <a href="http://www.faqs.org/rfcs/rfc2253.html">RFC 2253</a>. This class
 * is generated from the antlr lexer definition file valuelexer.g.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
class antlrTypeLexer extends Lexer ;

options {
	k = 4 ;
    caseSensitive = false ;
	importVocab = DnCommon ;
	exportVocab = antlrType ;
}
{
    /** Lexer key for selector lookups */
    public static final String LEXER_KEY = "typeLexer" ;
    /** Selector this lexer belongs to */
    private TokenStreamSelector m_selector = null ;


    /**
     * Sets the selector for this lexer.
     *
     * @param a_selector the selector to use.
     */
    public void setSelector(TokenStreamSelector a_selector)
    {
        m_selector = a_selector ;
    }

    
    /**
     * Pushes onto the selector's stack the name of the value lexer.  This
     * method is called upon encountering an equals ('=') sign indicating the
     * end of an attribute type.
     */
    private void push() 
    {
        if(m_selector == null) {
            throw new NullPointerException(
            "The selector has not been set for the type lexer!\n"
            + "Call lexer.setSelector(TokenStreamSelector a_selector) "
            + "before using the lexer or its owning parser.") ;
        }

        m_selector.push(antlrValueLexer.LEXER_KEY) ;
    }
}



             ////////////////////////////////////////////
             // Protected Tokens Not Returned By Lexer //
             ////////////////////////////////////////////


protected
DIGIT                   : '0' .. '9'
    ;

protected
ALPHA                   : 'a' .. 'z'
    ;

protected
OPEN_BRACKET            : '{'
    ;

protected
CLOSE_BRACKET           : '}'
    ;



                /////////////////////////////////////
                // Public Tokens Returned By Lexer //
                /////////////////////////////////////


OIDDN                   : ( 'o' | 'O' ) ( 'i' | 'I' ) ( 'd' | 'D' ) '.' OID
    ;

OID                     : ( DIGIT )+ ( '.' ( DIGIT )+ )*
                          ( OPEN_BRACKET ( DIGIT )+ CLOSE_BRACKET )?
    ;

ATTRIBUTE               : ALPHA ( ALPHA | DIGIT | '-' )*
    ;

WS	                    :	(	' '
                            |	'\t'
                            |	'\f'
                            // handle newlines
                            |	(	"\r\n"  // Evil DOS
                                |	'\r'    // Macintosh
                                |	'\n'    // Unix (the right way)
                                )
                                { newline(); }
                            )
		            { $setType(Token.SKIP); }
	;


             ////////////////////////////////////////////
             // Public Common Tokens Returned By Lexer //
             ////////////////////////////////////////////


EQUAL                   : "=" 
        { 
            push() ;
        }
	;
