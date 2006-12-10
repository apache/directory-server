header {
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.schema.syntax;

import java.util.* ;

}

   
/**
 * An antlr generated schema lexer. This is a sub-lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaQdstringLexer extends Lexer;

options    {
    k = 2 ;
    exportVocab=AntlrSchemaQdstring ;
    charVocabulary = '\u0000'..'\uFFFE'; 
    caseSensitive = true ;
    defaultErrorHandler = false ;
}

WHSP : ( ' ' ) {$setType(Token.SKIP);} ;
LPAR : '(' ;
RPAR : ')' ;
QUOTE : '\'' ;
QDSTRING : ( QUOTE (~'\'')* QUOTE ) ;





/**
 * An antlr generated schema parser. This is a sub-parser used to parse
 * qdstring and qdstrings according to RFC4512.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaQdstringParser extends Parser;
options    {
    k = 3 ;
    defaultErrorHandler = false ;
    //buildAST=true ;
}

    /**
     * qdstrings = qdstring / ( LPAREN WSP qdstringlist WSP RPAREN )
     * qdstringlist = [ qdstring *( SP qdstring ) ]
     */
qdstrings returns [List<String> qdstrings]
    {
    	qdstrings = new ArrayList<String>();
        String qdstring = null;
    }
    :
    (
        ( 
	        q:QDSTRING 
	        { 
	            qdstring = q.getText(); 
	            if(qdstring.startsWith("'")) {
	    			qdstring = qdstring.substring(1, qdstring.length());
	    		}
	    		if(qdstring.endsWith("'")) {
	    			qdstring = qdstring.substring(0, qdstring.length()-1);
	    		}
	    		qdstrings.add(qdstring);
	        } 
        )
    |
        ( LPAR qdstring=qdstring { qdstrings.add(qdstring); } ( qdstring=qdstring { qdstrings.add(qdstring); } )* RPAR )
    )
    ;

    /**
     * qdstring = SQUOTE dstring SQUOTE
     * dstring = 1*( QS / QQ / QUTF8 )   ; escaped UTF-8 string
     *
     * QQ =  ESC %x32 %x37 ; "\27"
     * QS =  ESC %x35 ( %x43 / %x63 ) ; "\5C" / "\5c"
     *
     * ; Any UTF-8 encoded Unicode character
     * ; except %x27 ("\'") and %x5C ("\")
     * QUTF8    = QUTF1 / UTFMB
     *
     * ; Any ASCII character except %x27 ("\'") and %x5C ("\")
     * QUTF1    = %x00-26 / %x28-5B / %x5D-7F
     */    
qdstring returns [String qdstring=null]
    : 
    ( 
        q:QDSTRING 
        { 
            qdstring = q.getText(); 
            if(qdstring.startsWith("'")) {
    			qdstring = qdstring.substring(1, qdstring.length());
    		}
    		if(qdstring.endsWith("'")) {
    			qdstring = qdstring.substring(0, qdstring.length()-1);
    		}
        } 
    )
    ; 

