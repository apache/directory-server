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
package org.apache.eve.tools.schema;

import java.util.* ;
import org.apache.ldap.common.schema.*;
import org.apache.eve.schema.*;

}


class antlrOpenLdapSchemaLexer extends Lexer ;

options    {
    k = 4 ;
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



class antlrOpenLdapSchemaParser extends Parser ;

options    {
    k = 5 ;
    defaultErrorHandler = false ;
}


{
    public static final String[] EMPTY = new String[0];
    private ParserMonitor monitor = null;
    OidRegistry registry = null;


    private final String resolve( String name )
    {
        String oid = null;

        try
        {
            oid = registry.getOid( name );
        }
        catch( Exception e )
        {
            e.printStackTrace();
            throw new RuntimeException( "could not find the oid: " + e.getMessage() );
        }

        return oid;
    }


    public final void matchedProduction( String msg )
    {
        if ( null != monitor )
        {
            monitor.matchedProduction( msg );
        }
    }
    

    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor;
    }


    public void setOidRegistry( OidRegistry registry )
    {
        this.registry = registry;
    }


    private static class MutableAttributeType extends BaseAttributeType
    {
        public MutableAttributeType( String oid )
        {
            super( oid );
        }

        public void setSuperior( AttributeType superior )
        {
            super.setSuperior( superior );
        }

        public void setAllNames( String[] nameArray )
        {
            super.setAllNames( nameArray );
        }

        public void setEquality( MatchingRule equality )
        {
            super.setEquality( equality );
        }

        public void setSubstr( MatchingRule substr )
        {
            super.setSubstr( substr );
        }

        public void setOrdering( MatchingRule ordering )
        {
            super.setOrdering( ordering );
        }

        public void setSyntax( Syntax syntax )
        {
            super.setSyntax( syntax );
        }

        public void setSingleValue( boolean singleValue )
        {
            super.setSingleValue( singleValue );
        }

        public void setDescription( String description )
        {
            super.setDescription( description );
        }

        public void setCollective( boolean collective )
        {
            super.setCollective( collective );
        }

        public void setCanUserModify( boolean canUserModify )
        {
            super.setCanUserModify( canUserModify );
        }

        public void setObsolete( boolean obsolete )
        {
            super.setObsolete( obsolete );
        }

        public void setUsage( UsageEnum usage )
        {
            super.setUsage( usage );
        }

        public void setLength( int length )
        {
            super.setLength( length );
        }

        public String getSuperiorOid()
        {
            return super.getSuperior() != null ? super.getSuperior().getOid() : null;
        }

        public String getSubstrOid()
        {
            return super.getSubstr() != null ? super.getSubstr().getOid() : null;
        }

        public String getOrderingOid()
        {
            return super.getOrdering() != null ? super.getOrdering().getOid() : null;
        }

        public String getEqualityOid()
        {
            return super.getEquality() != null ? super.getEquality().getOid() : null;
        }

        public String getSyntaxOid()
        {
            return super.getSyntax() != null ? super.getSyntax().getOid() : null;
        }
    }


    private static class MutableMatchingRule extends BaseMatchingRule
    {
        public MutableMatchingRule( String oid )
        {
            super( oid ) ;
        }
    }


    private static class MutableSyntax extends BaseSyntax
    {
        public MutableSyntax( String oid )
        {
            super( oid ) ;
        }
    }
}


attributeType returns [MutableAttributeType type]
{
    matchedProduction( "attributeType()" ) ;
    type = null ;
    UsageEnum usageEnum;
}
    :
    "attributetype"
    OPEN_PAREN oid:NUMERICOID
    {
        type = new MutableAttributeType( oid.getText() );
    }
        ( names[type] )?
        ( "DESC" QUOTE desc:IDENTIFIER { type.setDescription( desc.getText() ); } QUOTE )?
        ( "OBSOLETE" { type.setObsolete( true ); } )?
        ( superior[type] )?
        ( equality[type] )?
        ( ordering[type] )?
        ( substr[type] )?
        ( syntax[type] )?
        ( "SINGLE-VALUE" { type.setSingleValue( true ); } )?
        ( "COLLECTIVE" { type.setCollective( true ); } )?
        ( "NO-USER-MODIFICATION" { type.setCanUserModify( true ); } )?
        ( usage[type] )?

    CLOSE_PAREN ;


superior [MutableAttributeType type]
{
    matchedProduction( "superior()" ) ;
}
    : "SUP"
    (
        oid:NUMERICOID
        {
            type.setSuperior( new MutableAttributeType( oid.getText() ) );
        }
        |
        id:IDENTIFIER
        {
            String soid = resolve( id.getText() );
            type.setSuperior( new MutableAttributeType( soid ) );
        }
    );


equality [MutableAttributeType type]
{
    matchedProduction( "equality()" ) ;
}
    : "EQUALITY"
    (
        oid:NUMERICOID
        {
            type.setEquality( new MutableMatchingRule( oid.getText() ) );
        }
        |
        id:IDENTIFIER
        {
            String soid = resolve( id.getText() );
            type.setEquality( new MutableMatchingRule( soid ) );
        }
    );


substr [MutableAttributeType type]
{
    matchedProduction( "substr()" ) ;
}
    : "SUBSTR"
    (
        oid:NUMERICOID
        {
            type.setSubstr( new MutableMatchingRule( oid.getText() ) );
        }
        |
        id:IDENTIFIER
        {
            String soid = resolve( id.getText() );
            type.setSubstr( new MutableMatchingRule( soid ) );
        }
    );


ordering [MutableAttributeType type]
{
    matchedProduction( "ordering()" ) ;
}
    : "ORDERING"
    (
        oid:NUMERICOID
        {
            type.setOrdering( new MutableMatchingRule( oid.getText() ) );
        }
        |
        id:IDENTIFIER
        {
            String soid = resolve( id.getText() );
            type.setOrdering( new MutableMatchingRule( soid ) );
        }
    );


names [MutableAttributeType type]
{
    matchedProduction( "names()" ) ;
    ArrayList list = new ArrayList();
}
    :
    (
        "NAME" QUOTE id0:IDENTIFIER QUOTE
        {
            list.add( id0.getText() );
            registry.register( id0.getText(), type.getOid() );
        }
        |
        ( OPEN_PAREN QUOTE id1:IDENTIFIER
        {
            list.add( id1.getText() );
            registry.register( id1.getText(), type.getOid() );
        } QUOTE
        ( QUOTE id2:IDENTIFIER QUOTE
        {
            list.add( id2.getText() );
            registry.register( id2.getText(), type.getOid() );
        } )* CLOSE_PAREN )
    )
    {
        type.setAllNames( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


syntax [MutableAttributeType type]
{
    matchedProduction( "syntax()" ) ;
}
    : "SYNTAX"
    (
        oid:NUMERICOID
        {
            type.setSyntax( new MutableSyntax( oid.getText() ) );
        }
        ( OPEN_BRACKET length:NUMERIC_STRING
        {
            type.setLength( Integer.parseInt( length.getText() ) );
        } OPEN_BRACKET )?
    );

usage [MutableAttributeType type]
{
    matchedProduction( "usage()" ) ;
}
    :
    "USAGE"
    (
        "userApplications" { type.setUsage( UsageEnum.USERAPPLICATIONS ); } |
        "directoryOperation" { type.setUsage( UsageEnum.DIRECTORYOPERATION ); } |
        "distributedOperation" { type.setUsage( UsageEnum.DISTRIBUTEDOPERATION ); } |
        "dSAOperation" { type.setUsage( UsageEnum.DSAOPERATION ); }
    );
