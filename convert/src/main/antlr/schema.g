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
/*
 * Keep the semicolon right next to the package name or else there will be a
 * bug that comes into the foreground in the new antlr release.
 */
package org.apache.directory.shared.converter.schema;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Collections;
import java.io.Writer;
import java.io.IOException;

import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.converter.schema.ObjectClassHolder;
import org.apache.directory.shared.converter.schema.AttributeTypeHolder;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
}


class antlrSchemaLexer extends Lexer ;

options    {
    k = 7 ;
    exportVocab=antlrSchema ;
    charVocabulary = '\3'..'\377' ;
    caseSensitive = false ;
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

class antlrSchemaParser extends Parser ;

options    {
    k = 5 ;
    defaultErrorHandler = false ;
}


{
	
    public static final String[] EMPTY = new String[0];

    private List<AttributeTypeHolder> attributeTypes = new ArrayList<AttributeTypeHolder>();
    private List<ObjectClassHolder> objectClasses = new ArrayList<ObjectClassHolder>();
    private Writer schemaOut;


    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------
    public void setOutput( Writer out )
    {
        schemaOut = out;
    }


    public void clear()
    {
        attributeTypes.clear();
        objectClasses.clear();
    }


    public List<AttributeTypeHolder> getAttributeTypes()
    {
        return Collections.unmodifiableList( attributeTypes );
    }


    public List<ObjectClassHolder> getObjectClasses()
    {
        return Collections.unmodifiableList( objectClasses );
    }
}


// ----------------------------------------------------------------------------
// Main Entry Point Production
// ----------------------------------------------------------------------------


parseSchema
    :
    ( attributeType | objectClass )* "END"
    ;


// ----------------------------------------------------------------------------
// AttributeType Productions
// ----------------------------------------------------------------------------


objectClass
{
    ObjectClassHolder objectClass = null;
}
    :
    "objectclass"
    OPEN_PAREN oid:NUMERICOID
    {
        objectClass = new ObjectClassHolder( oid.getText() );
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
        objectClasses.add( objectClass );
    }
    ;


may [ObjectClassHolder objectClass]
{
    ArrayList list = null;
}
    : "MAY" list=woidlist
    {
        objectClass.setMay( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


must [ObjectClassHolder objectClass]
{
    ArrayList list = null;
}
    : "MUST" list=woidlist
    {
        objectClass.setMust( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


objectClassSuperiors [ObjectClassHolder objectClass]
{
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

objectClassDesc [ObjectClassHolder objectClass]
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


objectClassNames [ObjectClassHolder objectClass]
{
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
    AttributeTypeHolder type = null;
}
    :
    "attributetype"
    OPEN_PAREN oid:NUMERICOID
    {
        type = new AttributeTypeHolder( oid.getText() );
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
        attributeTypes.add( type );
    }
    ;


desc [AttributeTypeHolder type]
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


superior [AttributeTypeHolder type]
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


equality [AttributeTypeHolder type]
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


substr [AttributeTypeHolder type]
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


ordering [AttributeTypeHolder type]
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


names [AttributeTypeHolder type]
{
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


syntax [AttributeTypeHolder type]
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


usage [AttributeTypeHolder type]
    :
    "USAGE"
    (
        "userApplications" { type.setUsage( UsageEnum.USER_APPLICATIONS ); } |
        "directoryOperation" { type.setUsage( UsageEnum.DIRECTORY_OPERATION ); } |
        "distributedOperation" { type.setUsage( UsageEnum.DISTRIBUTED_OPERATION ); } |
        "dSAOperation" { type.setUsage( UsageEnum.DSA_OPERATION ); }
    );
