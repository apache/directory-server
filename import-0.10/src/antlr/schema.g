header {
    package org.apache.eve.schema;
    import java.util.* ;
}

// ===================================================
//   TOKENS FOR LDAP SCHEMA SYNTAX LEXER DEFINITIONS
//
// -- (c) Apache Software Foundation                                     --
// -- Please refer to the LICENSE.txt file in the root directory of      --
// -- any directory project for copyright and distribution information.  --
//
//
// ===================================================


class antlrSchemaSyntaxLexer extends Lexer ;

options {
    k = 4 ;
    exportVocab=antlrSchema ;
    charVocabulary = '\3'..'\377' ;
    caseSensitive = false ;
    testLiterals = false ;
}


//COMMENT       : '#' (~'\n')* '\n'   
//  ;

WS  :   (   '#' (~'\n')* '\n' { newline(); }
        |   ' '
        |   '\t'
        |   '\r' '\n' { newline(); }
        |   '\n'      { newline(); }
        |   '\r'      { newline(); }
        )
        {$setType(Token.SKIP);} //ignore this token
    ;

OPEN_PAREN      : '(' 
    ;

CLOSE_PAREN     : ')' 
    ;

OPEN_BRACKET    : '{' 
    ;

CLOSE_BRACKET   : '}' 
    ;

QUOTED_STRING   : '\'' ( ~'\'' )* '\'' 
    ;

AND             : '$'
    ;

OID             : 
        ( '0'..'9' )+ ( '.' ( '0'..'9' )+ )* 
            ( OPEN_BRACKET ('0' .. '9')+ CLOSE_BRACKET )?
    ;

IDENTIFIER options { testLiterals=true; }
    : 
        ( 'a' .. 'z') ( 'a' .. 'z' | '0' .. '9' | '-')*
    ;


class antlrSchemaParser extends Parser ;


tokens {
    ATTRIBUTE_TYPE          =   "attributetype"         ;
    NAME_KW                 =   "NAME"                  ;
    EQUALITY_KW             =   "EQUALITY"              ;
    ORDERING_KW             =   "ORDERING"              ;
    SYNTAX_KW               =   "SYNTAX"                ;
    SINGLEVAL_KW            =   "SINGLE-VALUE"          ;
    NOUSERMOD_KW            =   "NO-USER-MODIFICATION"  ;
    USAGE_KW                =   "USAGE"                 ;
    DESC_KW                 =   "DESC"                  ;
    MAY_KW                  =   "MAY"                   ;
    MUST_KW                 =   "MUST"                  ;
    SUBSTR_KW               =   "SUBSTR"                ;
    SUP_KW                  =   "SUP"                   ;
    AUXILIARY_KW            =   "AUXILIARY"             ;
    STRUCTURAL_KW           =   "STRUCTURAL"            ;
    ABSTRACT_KW             =   "ABSTRACT"              ;
}


schemafile [SchemaImpl a_schema]:
{
    AttributeSpec l_attribute = null ;
    ObjectClassSpec l_objectClass = null ;
}
    ( l_attribute=attributedef 
        {
            a_schema.addAttributeSpec(l_attribute) ;
        }
    | 
    l_objectClass=objectclassdef 
        {
            a_schema.addObjectClassSpec(l_objectClass) ;
        }
    )+ EOF ; 


attributedef returns [AttributeSpec l_attribute]
{
    l_attribute = new AttributeSpec() ;
    ArrayList l_nameList = null ;
}
    :
    ATTRIBUTE_TYPE OPEN_PAREN oid:OID l_nameList=namelist 
        {
            l_attribute.m_oid = oid.getText() ;
            l_attribute.m_nameList = l_nameList ;
        }
        (   
        ( "DESC" desc:QUOTED_STRING 
            {
                String quoted = desc.getText() ;
                l_attribute.m_desc = quoted.substring(1, quoted.length() - 1) ;
            }
        ) |
        

        ( "EQUALITY" equality:IDENTIFIER 
            {
                l_attribute.m_equality = equality.getText().toLowerCase() ;
            }
        ) |


        ( "SUBSTR" substr:IDENTIFIER
            {
                l_attribute.m_substr = substr.getText().toLowerCase() ;
            }
        ) |


        ( "ORDERING" ordering:IDENTIFIER
            {
                l_attribute.m_ordering = ordering.getText().toLowerCase() ;
            }
        ) |


        ( "SYNTAX" syntax:OID
            {
                l_attribute.m_syntax = syntax.getText() ;
            }
        ) |


        ( "SINGLE-VALUE" 
            {
                l_attribute.m_isSingleValue = true ;
            }
        ) |

        ( "NO-USER-MODIFICATION"
            {
                l_attribute.m_canUserModify = false ;
            }
        ) |

        ( "SUP" sup:IDENTIFIER 
            {
                l_attribute.m_superClass = sup.getText().toLowerCase() ;
            }
        ) |


        ( "USAGE" usage:IDENTIFIER
            {
                l_attribute.m_usage = usage.getText().toLowerCase() ;
            }
        )           
        )* CLOSE_PAREN ;


objectclassdef returns [ObjectClassSpec l_objectClass]
{
    l_objectClass = new ObjectClassSpec() ;
    ArrayList l_mayList = null ;
    ArrayList l_mustList = null ;
    ArrayList l_superClasses = null ;
    ArrayList l_nameList = null ;
    
}
    :
    "objectclass" OPEN_PAREN oid:OID l_nameList=namelist
        {
            l_objectClass.oid = oid.getText() ;
            l_objectClass.nameList = l_nameList ;
        }
        (
        ( "DESC" desc:QUOTED_STRING 
            {
                String tmp = desc.getText() ;
                l_objectClass.desc = tmp.substring(1, tmp.length() - 1) ;
            }
        ) |
        ( l_superClasses=superclasslist 
            {
                l_objectClass.superClasses = l_superClasses ;
            }
        ) |
        ( "ABSTRACT" 
            { l_objectClass.type = ObjectClassSpec.ABSTRACT   ; }
            | "STRUCTURAL" 
            { l_objectClass.type = ObjectClassSpec.STRUCTURAL ; }
            | "AUXILIARY" 
            { l_objectClass.type = ObjectClassSpec.AUXILIARY  ; }

        ) |
        ( l_mustList=mustlist 
            {
                l_objectClass.mustList = l_mustList ;
            }
        ) |
        ( l_mayList=maylist 
            {
                l_objectClass.mayList = l_mayList ;
            }
        ) 
        )* CLOSE_PAREN ;

        
superclasslist returns [ArrayList l_supList]
{
    l_supList = new ArrayList() ; 
}
    :
    "SUP"  ( id:IDENTIFIER 
        {
            l_supList.add(id.getText().toLowerCase()) ;
        }

            | OPEN_PAREN id2:IDENTIFIER 
        {
            l_supList.add(id2.getText().toLowerCase()) ;
        }
        
            ( AND id3:IDENTIFIER 

        {
            l_supList.add(id3.getText().toLowerCase()) ;
        }
        
        )* CLOSE_PAREN ) ;


namelist returns [ArrayList l_nameList]
{ 
    l_nameList = new ArrayList() ; 
}
    : 
    "NAME" ( name:QUOTED_STRING 
        {
            String tmp = name.getText() ;
            tmp = tmp.substring(1, tmp.length() - 1) ;
            l_nameList.add(tmp) ;
        }

        | OPEN_PAREN (name2:QUOTED_STRING

        {
            String tmp = name2.getText() ;
            tmp = tmp.substring(1, tmp.length() - 1) ;
            l_nameList.add(tmp) ;
        }

            )+ CLOSE_PAREN ) ;



maylist returns [ArrayList l_mayList]
{ 
    l_mayList = null ; 
}
    : "MAY" ( IDENTIFIER | l_mayList=attributelist ) ;



mustlist returns [ArrayList l_mustList]
{ 
    l_mustList = null ; 
}
    : 
    "MUST" ( IDENTIFIER | l_mustList=attributelist ) ;


attributelist returns [ArrayList l_list]
{ 
    l_list = new ArrayList() ; 
}
    :
    OPEN_PAREN id:IDENTIFIER 
        { l_list.add(id.getText().toLowerCase()) ; }
        ( AND id2:IDENTIFIER { l_list.add(id2.getText().toLowerCase()) ; })* 
    CLOSE_PAREN ;




