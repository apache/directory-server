// ----------------------------------------------------------------------------
// file header
// ----------------------------------------------------------------------------

header {
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
package org.apache.ldap.common.filter;
}

// ----------------------------------------------------------------------------
// class definition
// ----------------------------------------------------------------------------

/**
 * The filter parser's primary lexer.
 * 
 * @see <a href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-filter-08.txt">String Representation of Search Filters</a>
 * @see <a href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-models-12.txt">LDAP: Directory Information Models</a>
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 */
class AntlrFilterLexer extends Lexer;


// ----------------------------------------------------------------------------
// lexer options
// ----------------------------------------------------------------------------

options
{
	k=2;
	charVocabulary='\u0001'..'\u0127';
	exportVocab = FilterLexer;
	importVocab = FilterValueLexer;
}


// ----------------------------------------------------------------------------
// lexer class members
// ----------------------------------------------------------------------------

{
    /** the selector key used by this class of lexer */
    public static final String SELECTOR_KEY = "filterLexer";
}


// ----------------------------------------------------------------------------
// attribute description lexer rules from models
// ----------------------------------------------------------------------------


WS  :   (   ' '
        |   '\t'
        |   '\r' '\n' { newline(); }
        |   '\n'      { newline(); }
        |   '\r'      { newline(); }
        )
        {$setType(Token.SKIP);} //ignore this token
    ;

protected RANGLE: '>';

protected LANGLE: '<';

protected TILDE: '~';

COLON: ':';

ASTERISK: '*';

EXCLAMATION: '!';

EQUALS: '=';

LPAREN: '(';

RPAREN: ')';

VERTBAR: '|';

AMPERSTAND: '&';

DN: ":dn";

COLONEQUALS: ":=";

APPROX: TILDE EQUALS;

GREATEROREQUAL: RANGLE EQUALS;

LESSOREQUAL: LANGLE EQUALS;

protected DIGIT: '0' | LDIGIT;

protected LDIGIT: '1'..'9';

protected ALPHA: 'A'..'Z' | 'a'..'z';

protected NUMBER: DIGIT | ( LDIGIT ( DIGIT )+ );

protected NUMERICOID: NUMBER ( '.' NUMBER )+;

protected DESCR: ALPHA ( ALPHA | DIGIT | '-' )*;

protected OID: DESCR | NUMERICOID;

protected OPTION: ( ALPHA | DIGIT | '-' )+;

protected OPTIONS: ( ';' OPTION )*;

ATTRIBUTEDESCRIPTION: OID OPTIONS;
