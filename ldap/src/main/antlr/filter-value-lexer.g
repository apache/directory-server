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
package org.apache.directory.shared.ldap.filter;
}

// ----------------------------------------------------------------------------
// class definition
// ----------------------------------------------------------------------------

/**
 * A lexer for LDAP search filter value encodings as defined by the ldapbis 
 * draft describing the string representation of search filters.  This lexer is
 * rather complex supporting the complete encoding with unicode support.  We 
 * have separated it into its own lexer and intend to use it in a 
 * TokenStreamSelector to multiplex lexical states in a parser.
 * 
 * @see <a href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-filter-08.txt">String Representation of Search Filters</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class AntlrFilterValueLexer extends Lexer;


// ----------------------------------------------------------------------------
// lexer options
// ----------------------------------------------------------------------------

options
{
	k=1;
	
	// allow any unicode characters
	charVocabulary='\u0000'..'\uFFFE';
	exportVocab=FilterValueLexer;
}


// ----------------------------------------------------------------------------
// lexer class members
// ----------------------------------------------------------------------------

{
    /** the selector key used by this class of lexer */
    public static final String SELECTOR_KEY = "filterValueLexer";
}


// ----------------------------------------------------------------------------
// abnf-core lexer rules used
// ----------------------------------------------------------------------------

protected DIGIT: '0'..'9';

// A hexidecimal digit 
protected HEXDIG: DIGIT | 'A'..'F' | 'a'..'f';


// ----------------------------------------------------------------------------
// filter lexer rules
// ----------------------------------------------------------------------------

// the value encoding in all its possible glory
VALUEENCODING: ( NORMAL | ESCAPED )+;

// anything besides escapes, 0x00 (NUL), LPAREN, RPAREN, ASTERISK, and ESC
protected NORMAL: UTF1SUBSET | UTFMB;

// escaped out hex representations of bytes 
protected ESCAPED: ESC HEXDIG HEXDIG;

// UTF1SUBSET excludes 0x00 (NUL), LPAREN, RPAREN, ASTERISK, and ESC
// %x01-27 / %x2B-5B / %x5D-7F and we could defined it like so ==>
// protected UTF1SUBSET: '\u0001'..'\u0039' | '\u0043'..'\u0091' | '\u0093'..'\u0127';
// but we need to define it using an inverted caracter class to make 
// VALUEENCODING work without nondeterminism.
protected UTF1SUBSET: ~( '\\' | '\u0000' | '(' | ')' | '*' | '\u0128'..'\uFFFE' );

// %x21 ; exclamation mark ("!")
protected EXCLAMATION: '!';

// %x26 ; ampersand (or AND symbol) ("&")
protected AMPERSAND: "&" ;
      
// %x2A ; asterisk ("*")
ASTERISK: '*';

// %x28 ; left paren ("(")
LPAREN: '(';

// %x29 ; right paren (")")
RPAREN: ')';

// %x3A ; colon (":")
protected COLON: ':';
      
// %x7C ; vertical bar (or pipe) ("|")      
protected VERTBAR: '|';

// tilde ~
protected TILDE: '~';


// ----------------------------------------------------------------------------
// ldap-models lexer rules
// ----------------------------------------------------------------------------

protected ESC: '\\';

// recognizes any UTF-8 encoded Unicode character
protected UTF8: UTF1 | UTFMB;

// the mulitbyte characters
protected UTFMB: UTF2 | UTF3 | UTF4;

// %x80-BF
protected UTF0: '\u0128'..'\u0191';

// %x00-7F      
protected UTF1: '\u0000'..'\u0127';

// %xC2-DF UTF0
protected UTF2: '\u0194'..'\u0223' UTF0;

// %xE0 %xA0-BF UTF0 / %xE1-EC 2(UTF0) / xED %x80-9F UTF0 / %xEE-EF 2(UTF0)
protected UTF3:   ( '\u0224' '\u0160'..'\u0191' UTF0 ) 
				|          ( '\u0225'..'\u0236' UTF0 UTF0 ) 
				| ( '\u0237' '\u0128'..'\u0159' UTF0 ) 
				|          ( '\u0238'..'\u0239' UTF0 UTF0 );

// %xF0 %x90-BF 2(UTF0) / %xF1-F3 3(UTF0) / %xF4 %x80-8F 2(UTF0)
protected UTF4:   ( '\u0240' '\u0144'..'\u0191' UTF0 UTF0 ) 
                |          ( '\u0241'..'\u0243' UTF0 UTF0 UTF0 ) 
				| ( '\u0244' '\u0128'..'\u0143' UTF0 UTF0 );



