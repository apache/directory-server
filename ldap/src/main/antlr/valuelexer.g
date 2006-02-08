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
    package org.apache.directory.shared.ldap.name;

    import antlr.TokenStreamSelector ;
}


/**
 * Lexer used to scan the attribute value character stream from an equal sign
 * in the name-component to the next comma, semicolon, or plus sign (if the
 * RDN is multivalued).  It implements the regular expressions (a.k.a lexer
 * rules) for accepting a 'value' as specified in 
 * <a href="http://www.faqs.org/rfcs/rfc2253.html">RFC 2253</a>. This class
 * is generated from the antlr lexer definition file valuelexer.g.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 */
class antlrValueLexer extends Lexer ;

options {
	k = 2 ;
    caseSensitive = false ;
	importVocab = DnCommon ;
	exportVocab = antlrValue ;
    charVocabulary = '\u0000'..'\uFFFE'; //'\0' .. '\377' ;
}
{
    /** Constant referenced by type lexer for pushes */
    public static final String LEXER_KEY = "valueLexer" ;
    /** Selector this lexer belongs to */
    private TokenStreamSelector m_selector = null ;

    
    /**
     * Sets the selector for this lexer.
     *
     * @param a_selector the selector to use.
     */
    public void setSelector( TokenStreamSelector a_selector )
    {
        m_selector = a_selector ;
    }

    
    /**
     * Pops the selector stack if it exists to switch back to
     * the invoking lexer.
     */
    private void pop() 
    {
        if ( m_selector == null ) 
        {
            throw new NullPointerException(
            	"The selector has not been set for the value lexer!\n"
            	+ "Call lexer.setSelector(TokenStreamSelector a_selector) "
            	+ "before using the lexer or its owning parser." ) ;
        }

        m_selector.pop() ;
    }
}


             ////////////////////////////////////////////
             // Protected Tokens Not Returned By Lexer //
             ////////////////////////////////////////////


protected
DIGIT                   : '0'..'9'
    ;

protected
ALPHA                   : 'a'..'z'
    ;

protected
HEXCHAR                 : DIGIT | ( 'a'..'f' )
    ;

protected
HEXPAIR                 : HEXCHAR HEXCHAR
    ;



                /////////////////////////////////////
                // Public Tokens Returned By Lexer //
                /////////////////////////////////////


ESCAPED_CHAR            : '\\'
                          ( ( ',' | '=' | '+' | '<' |  '>' | '#' | ';' ) 
                                     | '\\'
                                     | '"'
                                     | HEXPAIR )
    ;

HEX_STRING              : '#' ( HEXPAIR )+
    ;

QUOTED_STRING           : '"'
                          ( ~( '"' | '\\' ) | ESCAPED_CHAR )*
                          '"'
    ;

SIMPLE_STRING           : ( ~( ',' | '=' | '+' | '<'  |
                               '>' | '#' | ';' | '\\' | '"' ) )*
    ;



             ////////////////////////////////////////////
             // Public Common Tokens Returned By Lexer //
             ////////////////////////////////////////////


COMMA                   : ',' 
        {
            pop() ;
        }
	;

SEMI                    : ';'
        {
            pop() ;
        }
    ;

PLUS                    : '+'
        {
            pop() ;
        }
    ;

/**
 * Must pop the stack to revert back to the type lexer for the 
 * next distinguished name parse.
 */
DN_TERMINATOR           : '#'
        {
            pop() ;
        }
    ;


