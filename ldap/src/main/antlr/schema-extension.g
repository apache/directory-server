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

import java.io.* ;
import java.util.* ;

}


   
/**
 * An antlr generated schema lexer. This is a sub-lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaExtensionLexer extends Lexer;

options    {
    k = 2 ;
    exportVocab=AntlrSchemaExtension ;
    charVocabulary = '\u0000'..'\uFFFE'; 
    caseSensitive = true ;
    defaultErrorHandler = false ;
}

protected WHSP : (options{greedy=true;}: ' ' )+ {$setType(Token.SKIP);} ;
protected QUOTE : '\'' ;
//protected ESC : '\\' ;

XKEY : xstring:XSTRING { setText(xstring.getText().trim()); }; 
XVALUES : values:VALUES { setText(values.getText().trim()); };

protected XSTRING : ( "X-" ( 'a'..'z' | 'A'..'Z' | '-' | '_' )+ WHSP ) ; 
protected VALUES : ( VALUE | '('  VALUE ( ('$')? VALUE )* ')' ) ;
protected VALUE : (WHSP)? ( QUOTED_STRING ) (options {greedy=true;}: WHSP)? ;
protected QUOTED_STRING : ( QUOTE (~'\'')* QUOTE ) ;






/**
 * An antlr generated schema parser. This is a sub-parser used to parse
 * extensions according to RFC4512.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaExtensionParser extends Parser;
options    {
    k = 3 ;
    defaultErrorHandler = false ;
    //buildAST=true ;
}


    /**
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE )
     */
extension returns [AntlrSchemaParser.Extension extension = new AntlrSchemaParser.Extension()]
    :
    ( xkey:XKEY { extension.setKey(xkey.getText()); } )
    ( xvalues:XVALUES { extension.setValues(qdstrings(xvalues.getText())); } )
    ;
    
    
qdstrings [String s] returns [List<String> qdstrings]
    {
        try 
        {
    	    AntlrSchemaQdstringLexer lexer = new AntlrSchemaQdstringLexer(new StringReader(s));
            AntlrSchemaQdstringParser parser = new AntlrSchemaQdstringParser(lexer);
            qdstrings = parser.qdstrings();
        }
        catch (RecognitionException re) {
            re.printStackTrace();
            throw re;
        }
        catch (TokenStreamException tse) {
            tse.printStackTrace();
            throw tse;
        }
    }
    :
    ;

