// ============================================================================
//
//
//                    OpenLDAP Schema Parser
//
//
// ============================================================================
// $Rev$
// ============================================================================


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
/*
 * Keep the semicolon right next to the package name or else there will be a
 * bug that comes into the foreground in the new antlr release.
 */
package org.apache.ldap.server.tools.schema;

import java.util.* ;
import org.apache.ldap.common.schema.*;

}


class antlrOpenLdapSchemaLexer extends Lexer ;

options    {
    k = 7 ;
    exportVocab=antlrOpenLdapSchema ;
    charVocabulary = '\3'..'\377' ;
    caseSensitive = false ;
    //testLiterals = true ;
    defaultErrorHandler = false ;
}


WS  :   (   '#' (~'\n')* '\n' { newline(); }
        |    ' '
        |   '\t'
        |   '\r' '\n' { newline(); }
        |   '\n'      { newline(); }
        |   '\r'      { newline(); }
        )
        {$setType(Token.SKIP);} //ignore this token
    ;

QUOTE              : '\''
    ;

DIGIT              : '0' .. '9'
    ;

DOLLAR             : '$'
    ;

OPEN_PAREN         : '('
    ;

CLOSE_PAREN        : ')'
    ;

OPEN_BRACKET       : '{'
    ;

CLOSE_BRACKET      : '}'
    ;

protected NUMERIC_STRING : ('0' .. '9')+
    ;

NUMERICOID         :
        NUMERIC_STRING ( '.' NUMERIC_STRING )+
    ;

IDENTIFIER options { testLiterals=true; }
    : 
        ( 'a' .. 'z') ( 'a' .. 'z' | '0' .. '9' | '-' | ';' )*
    ;

DESC
    :
        "desc" WS QUOTE ( ~'\'' )+ QUOTE
    ;

SYNTAX
    :
        "syntax" WS NUMERICOID ( OPEN_BRACKET ( DIGIT )+ CLOSE_BRACKET )?
    ;

class antlrOpenLdapSchemaParser extends Parser ;

options    {
    k = 5 ;
    defaultErrorHandler = false ;
}


{
    public static final String[] EMPTY = new String[0];

    private Map attributeTypes = new HashMap();
    private Map objectClasses = new HashMap();
    private ParserMonitor monitor = null;


    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------


    public void clear()
    {
        attributeTypes.clear();
        objectClasses.clear();
    }


    public Map getAttributeTypes()
    {
        return Collections.unmodifiableMap( attributeTypes );
    }


    public Map getObjectClasses()
    {
        return Collections.unmodifiableMap( objectClasses );
    }


    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------


    private void matchedProduction( String msg )
    {
        if ( null != monitor )
        {
            monitor.matchedProduction( msg );
        }
    }
}


// ----------------------------------------------------------------------------
// Main Entry Point Production
// ----------------------------------------------------------------------------


parseSchema
{
}
    :
    ( attributeType | objectClass )* "END"
    ;


// ----------------------------------------------------------------------------
// AttributeType Productions
// ----------------------------------------------------------------------------


objectClass
{
    matchedProduction( "objectClass()" );
    ObjectClassLiteral objectClass = null;
}
    :
    "objectclass"
    OPEN_PAREN oid:NUMERICOID
    {
        objectClass = new ObjectClassLiteral( oid.getText() );
    }
    ( objectClassNames[objectClass] )?
    ( objectClassDesc[objectClass] )?
    ( "OBSOLETE" { objectClass.setObsolete( true ); } )?
    ( objectClassSuperiors[objectClass] )?
    ( 
        "ABSTRACT"   { objectClass.setClassType( ObjectClassTypeEnum.ABSTRACT ); } |
        "STRUCTURAL" { objectClass.setClassType( ObjectClassTypeEnum.STRUCTURAL ); } |
        "AUXILIARY"  { objectClass.setClassType( ObjectClassTypeEnum.AUXILIARY ); }
    )?
    ( must[objectClass] )?
    ( may[objectClass] )?
    CLOSE_PAREN
    {
        objectClasses.put( objectClass.getOid(), objectClass );
    }
    ;


may [ObjectClassLiteral objectClass]
{
    matchedProduction( "may(ObjectClassLiteral)" ) ;
    ArrayList list = null;
}
    : "MAY" list=woidlist
    {
        objectClass.setMay( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


must [ObjectClassLiteral objectClass]
{
    matchedProduction( "must(ObjectClassLiteral)" ) ;
    ArrayList list = null;
}
    : "MUST" list=woidlist
    {
        objectClass.setMust( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


objectClassSuperiors [ObjectClassLiteral objectClass]
{
    matchedProduction( "objectClassSuperiors(ObjectClassLiteral)" ) ;
    ArrayList list = null;
}
    : "SUP" list=woidlist
    {
        objectClass.setSuperiors( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


woid returns [String oid]
{
    oid = null;
    matchedProduction( "woid()" ) ;
}
    :
    (
        opt1:NUMERICOID
        {
            oid = opt1.getText();
        }
        |
        opt2:IDENTIFIER
        {
            oid = opt2.getText();
        }
    )
    ;


woidlist returns [ArrayList list]
{
    matchedProduction( "woidlist()" ) ;
    list = new ArrayList( 2 );
    String oid = null;
}
    :
    (
        oid=woid { list.add( oid ); } |
        (
            OPEN_PAREN
            oid=woid { list.add( oid ); } ( DOLLAR oid=woid { list.add( oid ); } )*
            CLOSE_PAREN
        )
    )
    ;

objectClassDesc [ObjectClassLiteral objectClass]
{
    matchedProduction( "desc(ObjectClassLiteral)" ) ;
}
    : d:DESC
    {
        String desc = d.getText().split( "'" )[1];
        String[] quoted = desc.split( "\"" );

        if ( quoted.length == 1 )
        {
            objectClass.setDescription( desc );
        }
        else
        {
            StringBuffer buf = new StringBuffer();
            for ( int ii = 0; ii < quoted.length; ii++ )
            {
                if ( ii < quoted.length - 1 )
                {
                    buf.append( quoted[ii] ).append( "\\" ).append( "\"" );
                }
                else
                {
                    buf.append( quoted[ii] );
                }
            }

            objectClass.setDescription( buf.toString() );
        }
    }
    ;


objectClassNames [ObjectClassLiteral objectClass]
{
    matchedProduction( "names(ObjectClassLiteral)" ) ;
    ArrayList list = new ArrayList();
}
    :
    (
        "NAME"
        ( QUOTE id0:IDENTIFIER QUOTE
        {
            list.add( id0.getText() );
        }
        |
        ( OPEN_PAREN QUOTE id1:IDENTIFIER
        {
            list.add( id1.getText() );
        } QUOTE
        ( QUOTE id2:IDENTIFIER QUOTE
        {
            list.add( id2.getText() );
        } )* CLOSE_PAREN )
        )
    )
    {
        objectClass.setNames( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


// ----------------------------------------------------------------------------
// AttributeType Productions
// ----------------------------------------------------------------------------


attributeType
{
    matchedProduction( "attributeType()" );
    AttributeTypeLiteral type = null;
}
    :
    "attributetype"
    OPEN_PAREN oid:NUMERICOID
    {
        type = new AttributeTypeLiteral( oid.getText() );
    }
        ( names[type] )?
        ( desc[type] )?
        ( "OBSOLETE" { type.setObsolete( true ); } )?
        ( superior[type] )?
        ( equality[type] )?
        ( ordering[type] )?
        ( substr[type] )?
        ( syntax[type] )?
        ( "SINGLE-VALUE" { type.setSingleValue( true ); } )?
        ( "COLLECTIVE" { type.setCollective( true ); } )?
        ( "NO-USER-MODIFICATION" { type.setNoUserModification( true ); } )?
        ( usage[type] )?

    CLOSE_PAREN
    {
        attributeTypes.put( type.getOid(), type );
    }
    ;


desc [AttributeTypeLiteral type]
{
    matchedProduction( "desc(AttributeTypeLiteral)" ) ;
}
    : d:DESC
    {
        String desc = d.getText().split( "'" )[1];
        String[] quoted = desc.split( "\"" );

        if ( quoted.length == 1 )
        {
            type.setDescription( desc );
        }
        else
        {
            StringBuffer buf = new StringBuffer();
            for ( int ii = 0; ii < quoted.length; ii++ )
            {
                if ( ii < quoted.length - 1 )
                {
                    buf.append( quoted[ii] ).append( "\\" ).append( "\"" );
                }
                else
                {
                    buf.append( quoted[ii] );
                }
            }

            type.setDescription( buf.toString() );
        }
    }
    ;


superior [AttributeTypeLiteral type]
{
    matchedProduction( "superior(AttributeTypeLiteral)" ) ;
}
    : "SUP"
    (
        oid:NUMERICOID
        {
            type.setSuperior( oid.getText() );
        }
        |
        id:IDENTIFIER
        {
            type.setSuperior( id.getText() );
        }
    );


equality [AttributeTypeLiteral type]
{
    matchedProduction( "equality(AttributeTypeLiteral)" ) ;
}
    : "EQUALITY"
    (
        oid:NUMERICOID
        {
            type.setEquality( oid.getText() );
        }
        |
        id:IDENTIFIER
        {
            type.setEquality( id.getText() );
        }
    );


substr [AttributeTypeLiteral type]
{
    matchedProduction( "substr(AttributeTypeLiteral)" ) ;
}
    : "SUBSTR"
    (
        oid:NUMERICOID
        {
            type.setSubstr( oid.getText() );
        }
        |
        id:IDENTIFIER
        {
            type.setSubstr( id.getText() );
        }
    );


ordering [AttributeTypeLiteral type]
{
    matchedProduction( "ordering(AttributeTypeLiteral)" ) ;
}
    : "ORDERING"
    (
        oid:NUMERICOID
        {
            type.setOrdering( oid.getText() );
        }
        |
        id:IDENTIFIER
        {
            type.setOrdering( id.getText() );
        }
    );


names [AttributeTypeLiteral type]
{
    matchedProduction( "names(AttributeTypeLiteral)" ) ;
    ArrayList list = new ArrayList();
}
    :
        "NAME"
    (
        QUOTE id0:IDENTIFIER QUOTE { list.add( id0.getText() ); } |
        ( OPEN_PAREN
            ( QUOTE id1:IDENTIFIER
                {
                    list.add( id1.getText() );
                }
              QUOTE
            )+
        CLOSE_PAREN )
    )
    {
        type.setNames( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


syntax [AttributeTypeLiteral type]
{
    matchedProduction( "syntax(AttributeTypeLiteral)" ) ;
}
    : token:SYNTAX
    {
        String[] comps = token.getText().split( " " );

        int index = comps[1].indexOf( "{" );
        if ( index == -1 )
        {
            type.setSyntax( comps[1] );
            return;
        }

        String oid = comps[1].substring( 0, index );
        String length = comps[1].substring( index + 1, comps[1].length() - 1 );

        type.setSyntax( oid );
        type.setLength( Integer.parseInt( length ) );
    }
    ;


usage [AttributeTypeLiteral type]
{
    matchedProduction( "usage(AttributeTypeLiteral)" ) ;
}
    :
    "USAGE"
    (
        "userApplications" { type.setUsage( UsageEnum.USERAPPLICATIONS ); } |
        "directoryOperation" { type.setUsage( UsageEnum.DIRECTORYOPERATION ); } |
        "distributedOperation" { type.setUsage( UsageEnum.DISTRIBUTEDOPERATION ); } |
        "dSAOperation" { type.setUsage( UsageEnum.DSAOPERATION ); }
    );
