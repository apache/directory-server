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
package org.apache.directory.shared.ldap.name;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.NameParser;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.entry.client.ClientBinaryValue;
import org.apache.directory.shared.ldap.schema.parsers.ParserMonitor;
import org.apache.directory.shared.ldap.util.StringTools;

}

/**
 * An antlr generated DN lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrDnLexer extends Lexer;

options    {
    k = 3 ;
    exportVocab=AntlrDn ;
    charVocabulary = '\u0000'..'\uFFFE';
    caseSensitive = false ;
    defaultErrorHandler = false ;
}

COMMA : ',' ;
EQUALS : '=' ;
PLUS : '+' ;
HYPHEN : '-' ;
DQUOTE : '"' ;
SEMI : ';' ;
LANGLE : '<' ;
RANGLE : '>' ;
SPACE : ' ' ;

NUMERICOID_OR_ALPHA_OR_DIGIT 
    : ( NUMERICOID ) => NUMERICOID { $setType(NUMERICOID); }
    | ( DIGIT ) => DIGIT { $setType(DIGIT); }
    | ( ALPHA ) => ALPHA { $setType(ALPHA); }
    ;
protected NUMERICOID : ( "oid." )? NUMBER ( DOT NUMBER )+ ;
protected DOT: '.' ;
protected NUMBER: DIGIT | ( LDIGIT ( DIGIT )+ ) ;
protected LDIGIT : '1'..'9' ;
protected DIGIT : '0'..'9' ;
protected ALPHA : 'a'..'z' ;

HEXPAIR_OR_ESCESC_ESCSHARP_OR_ESC 
    : (ESC HEX HEX) => HEXPAIR { $setType(HEXPAIR); }
    | ESCESC { $setType(ESCESC); }
    | ESCSHARP { $setType(ESCSHARP); }
    | ESC { $setType(ESC); }
    ;
protected HEXPAIR : ESC! HEX HEX ;
protected ESC : '\\';
protected ESCESC : ESC ESC;
protected ESCSHARP : ESC SHARP;
protected HEX: DIGIT | 'a'..'f' ;

HEXVALUE_OR_SHARP
    : (SHARP ( HEX HEX )+) => HEXVALUE { $setType(HEXVALUE); }
    | SHARP { $setType(SHARP); }
    ;
protected HEXVALUE : SHARP! ( HEX HEX )+ ;
protected SHARP: '#' ;

UTFMB : '\u0080'..'\uFFFE' ;

/**
 * RFC 4514, Section 3:
 * LUTF1 = %x01-1F / %x21 / %x24-2A / %x2D-3A /
 *    %x3D / %x3F-5B / %x5D-7F
 *
 * To avoid nondeterminism the following 
 * rules are excluded. These rules are 
 * explicitely added in the productions.
 *   EQUALS (0x3D) 
-*   HYPHEN (0x2D)  
 *   DIGIT (0x30-0x39)
 *   ALPHA (0x41-0x5A and 0x61-0x7A)
 */
LUTF1_REST : 
    '\u0001'..'\u001F' |
    '\u0021' |
    '\u0024'..'\u002A' |
    '\u002E'..'\u002F' |
    '\u003A' |
    '\u003F'..'\u0040' |
    '\u005B' |
    '\u005D'..'\u0060' | 
    '\u007B'..'\u007F' 
    ;


/**
 * An antlr generated DN parser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrDnParser extends Parser;
options    {
    k = 3 ;
    defaultErrorHandler = false ;
    //buildAST=true ;
}

{
    private ParserMonitor monitor = null;
    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor;
    }
    private void matchedProduction( String msg )
    {
        if ( null != monitor )
        {
            monitor.matchedProduction( msg );
        }
    }
    static class UpAndNormValue
    {
        String upValue = "";
        Object normValue = "";
        String trailingSpaces = "";
    }
}

    /**
     * Parses an DN string.
     *
     * RFC 4514, Section 3
     * distinguishedName = [ relativeDistinguishedName
     *     *( COMMA relativeDistinguishedName ) ]
     *
     * RFC 2253, Section 3
     * distinguishedName = [name] 
     * name       = name-component *("," name-component)
     *
     * RFC 1779, Section 2.3
     * <name> ::= <name-component> ( <spaced-separator> )
     *        | <name-component> <spaced-separator> <name>
     * <spaced-separator> ::= <optional-space>
     *             <separator>
     *             <optional-space>
     * <separator> ::=  "," | ";"
     * <optional-space> ::= ( <CR> ) *( " " )
     *
     */
distinguishedName [LdapDN dn]
    {
        matchedProduction( "distinguishedName()" );
        Rdn rdn = null;
    }
    :
    (
        rdn = relativeDistinguishedName[new Rdn()] { dn.add( rdn ); rdn=null; }
        (
            ( COMMA | SEMI )
            rdn = relativeDistinguishedName[new Rdn()] { dn.add( rdn ); rdn=null; }
        )*
        EOF
    )?
    ;


    /**
     * Parses an DN string.
     *
     * RFC 4514, Section 3
     * distinguishedName = [ relativeDistinguishedName
     *     *( COMMA relativeDistinguishedName ) ]
     *
     * RFC 2253, Section 3
     * distinguishedName = [name] 
     * name       = name-component *("," name-component)
     *
     * RFC 1779, Section 2.3
     * <name> ::= <name-component> ( <spaced-separator> )
     *        | <name-component> <spaced-separator> <name>
     * <spaced-separator> ::= <optional-space>
     *             <separator>
     *             <optional-space>
     * <separator> ::=  "," | ";"
     * <optional-space> ::= ( <CR> ) *( " " )
     *
     */
relativeDistinguishedNames [List<Rdn> rdns]
    {
        matchedProduction( "relativeDistinguishedNames()" );
        Rdn rdn = null;
    }
    :
    (
        rdn = relativeDistinguishedName[new Rdn()] { rdns.add( rdn ); }
        (
            ( COMMA | SEMI )
            rdn = relativeDistinguishedName[new Rdn()] { rdns.add( rdn ); }
        )*
        EOF
    )?
    ;

    /**
     * Parses an RDN string.
     *
     * RFC 4514, Section 3
     * relativeDistinguishedName = attributeTypeAndValue
     *     *( PLUS attributeTypeAndValue )
     *
     * RFC 2253, Section 3
     * name-component = attributeTypeAndValue *("+" attributeTypeAndValue)
     *
     * RFC 1779, Section 2.3
     * <name-component> ::= <attribute>
     *     | <attribute> <optional-space> "+"
     *       <optional-space> <name-component>
     *
     */
relativeDistinguishedName [Rdn initialRdn] returns [Rdn rdn]
    {
        matchedProduction( "relativeDistinguishedName()" );
        rdn = initialRdn;
        String tmp;
        String upName = "";
    }
    :
    (
        ( SPACE { upName += " "; } )*
        tmp = attributeTypeAndValue[rdn] 
        { 
            upName += tmp;
        }
        (
            PLUS { upName += "+"; }
            ( SPACE { upName += " "; } )*
            tmp = attributeTypeAndValue[rdn] 
            { 
                upName += tmp;
            }
        )*
    )
    {
        rdn.normalize();
        rdn.setUpName( upName );
    }
    ;
    

    /**
     * RFC 4514, Section 3
     * attributeTypeAndValue = attributeType EQUALS attributeValue
     *
     * RFC 2253, Section 3
     * attributeTypeAndValue = attributeType "=" attributeValue
     *
     */
attributeTypeAndValue [Rdn rdn] returns [String upName = ""]
    {
        matchedProduction( "attributeTypeAndValue()" );
        String type = null;
        UpAndNormValue value = new UpAndNormValue();
    }
    :
    (
        type = attributeType { upName += type; }
        ( SPACE { upName += " "; } )*
        EQUALS { upName += "="; }
        ( SPACE { upName += " "; } )*
        attributeValue[value] 
        {
            try
            {
                if ( value.normValue instanceof String )
                {
                    rdn.addAttributeTypeAndValue( type, type, 
                        new ClientStringValue( value.upValue ), 
                        new ClientStringValue( (String)value.normValue ) );
                }
                else
                {
                    rdn.addAttributeTypeAndValue( type, type, 
                        new ClientStringValue( value.upValue ), 
                        new ClientBinaryValue( (byte[])value.normValue ) );
                }
                { upName += value.upValue + value.trailingSpaces; }
            }
            catch ( InvalidNameException e )
            {
                throw new SemanticException( e.getMessage() );
            } 
        }
    )
    ;
    

    /**
     * RFC 4514 Section 3
     *
     * attributeType = descr / numericoid
     *
     */    
attributeType returns [String attributeType]
    {
        matchedProduction( "attributeType()" );
    }
    :
    (
        attributeType = descr
        |
        attributeType = numericoid
    )
    ;


    /**
     * RFC 4512 Section 1.4
     *
     * descr = keystring
     * keystring = leadkeychar *keychar
     * leadkeychar = ALPHA
     * keychar = ALPHA / DIGIT / HYPHEN
     *
     */    
descr returns [String descr]
    {
        matchedProduction( "descr()" );
    }
    :
    leadkeychar:ALPHA { descr = leadkeychar.getText(); }
    (
        alpha:ALPHA { descr += alpha.getText(); }
        |
        digit:DIGIT { descr += digit.getText(); }
        |
        hyphen:HYPHEN { descr += hyphen.getText(); }
    )*
    ;


    /**
     * RFC 4512 Section 1.4
     *
     * numericoid = number 1*( DOT number )
     * number  = DIGIT / ( LDIGIT 1*DIGIT )
     * DIGIT   = %x30 / LDIGIT       ; "0"-"9"
     * LDIGIT  = %x31-39             ; "1"-"9"
     *
     */   
numericoid returns [String numericoid = ""]
    {
        matchedProduction( "numericoid()" );
    }
    :
    noid:NUMERICOID { numericoid += noid.getText(); }
    ;


    /**
     * RFC 4514, Section 3
     * attributeValue = string / hexstring
     *
     * RFC 2253, Section 3
     * attributeValue = string
     * string     = *( stringchar / pair )
     *              / "#" hexstring
     *              / QUOTATION *( quotechar / pair ) QUOTATION ; only from v2
     * 
     */    
attributeValue [UpAndNormValue value]
    {
        matchedProduction( "attributeValue()" );
    }
    :
    (
        (
            quotestring [value]
            ( SPACE { value.trailingSpaces += " "; } )*
        )
        |
        string [value]
        |
        (
            hexstring [value]
            ( SPACE { value.trailingSpaces += " "; } )*
        )
    )?
    ;


    /**
     * RFC 2253, Section 3
     *              / QUOTATION *( quotechar / pair ) QUOTATION ; only from v2
     * quotechar     = <any character except "\" or QUOTATION >
     *
     */
quotestring [UpAndNormValue value] 
    {
        matchedProduction( "quotestring()" );
        org.apache.directory.shared.ldap.util.ByteBuffer bb = new org.apache.directory.shared.ldap.util.ByteBuffer();
        byte[] bytes;
    }
    :
    (
        dq1:DQUOTE { value.upValue += dq1.getText(); }
        (
            (
                s:~(DQUOTE|ESC|ESCESC|ESCSHARP|HEXPAIR) 
                {
                    value.upValue += s.getText();
                    bb.append( StringTools.getBytesUtf8( s.getText() ) ); 
                }
            )
            |
            bytes = pair[value] { bb.append( bytes ); }
        )*
        dq2:DQUOTE { value.upValue += dq2.getText(); }
    )
    {
        // TODO: pair / s
        String string = StringTools.utf8ToString( bb.copyOfUsedBytes() );
        string = string.replace("\\ ", " ");
        value.normValue = string;
    }
    ;


    /**
     * RFC 4514 Section 3
     *
     * hexstring = SHARP 1*hexpair
     *
     * If in <hexstring> form, a BER representation can be obtained from
     * converting each <hexpair> of the <hexstring> to the octet indicated
     * by the <hexpair>.
     *
     */ 
hexstring [UpAndNormValue value]
    {
        matchedProduction( "hexstring()" );
    }
    :
    hexValue:HEXVALUE
    {
        // convert to byte[]
        value.upValue = "#" + hexValue.getText();
        value.normValue = StringTools.toByteArray( hexValue.getText() ); 
    }
    ;


    /**
     * RFC 4514 Section 3
     *
     * ; The following characters are to be escaped when they appear
     * ; in the value to be encoded: ESC, one of <escaped>, leading
     * ; SHARP or SPACE, trailing SPACE, and NULL.
     * string =   [ ( leadchar / pair ) [ *( stringchar / pair )
     *    ( trailchar / pair ) ] ]
     *
     * TODO: Trailing SPACE is manually removed. This needs review! 
     * TODO: ESC SPACE is replaced by a simgle SPACE. This needs review! 
     */ 
string [UpAndNormValue value]
    {
        matchedProduction( "string()" );
        org.apache.directory.shared.ldap.util.ByteBuffer bb = new org.apache.directory.shared.ldap.util.ByteBuffer();
        String tmp;
        byte[] bytes;
    }
    :
    (
        (
            tmp = lutf1 
            { 
                value.upValue += tmp;
                bb.append( StringTools.getBytesUtf8( tmp ) ); 
            }
            |
            tmp = utfmb 
            {
                value.upValue += tmp;
                bb.append( StringTools.getBytesUtf8( tmp ) );
            }
            |
            bytes = pair [value] { bb.append( bytes ); }
        )
        ( 
            tmp = sutf1
            {
                value.upValue += tmp;
                bb.append( StringTools.getBytesUtf8( tmp ) ); 
            }
            |
            tmp = utfmb 
            {
                value.upValue += tmp;
                bb.append( StringTools.getBytesUtf8( tmp ) ); 
            }
            |
            bytes = pair [value] { bb.append( bytes ); }
        )*
    )
    {
        String string = StringTools.utf8ToString( bb.copyOfUsedBytes() );
        
        // trim trailing space characters manually
        // don't know how to tell antlr that the last char mustn't be a space.
        while ( string.length() > 0 && value.upValue.length() > 1 
            && value.upValue.charAt( value.upValue.length() - 1 ) == ' ' 
            && value.upValue.charAt( value.upValue.length() - 2 ) != '\\' )
        {
            string = string.substring( 0, string.length() - 1 );
            value.upValue = value.upValue.substring( 0, value.upValue.length() - 1 );
            value.trailingSpaces += " ";
        }
        
        value.normValue = string;
    }
    ;


/**
 * RFC 4514, Section 3:
 * LUTF1 = %x01-1F / %x21 / %x24-2A / %x2D-3A /
 *    %x3D / %x3F-5B / %x5D-7F
 *
 * The rule LUTF1_REST doesn't contain the following charcters,
 * so we must check them additionally
 *   EQUALS (0x3D) 
-*   HYPHEN (0x2D)  
 *   DIGIT (0x30-0x39)
 *   ALPHA (0x41-0x5A and 0x61-0x7A)
 */
lutf1 returns [String lutf1=""]
    {
        matchedProduction( "lutf1()" );
    }
    :
    rest:LUTF1_REST { lutf1 = rest.getText(); }
    |
    equals:EQUALS { lutf1 = equals.getText(); }
    |
    hyphen:HYPHEN { lutf1 = hyphen.getText(); }
    |
    digit:DIGIT { lutf1 = digit.getText(); }
    |
    alpha:ALPHA { lutf1 = alpha.getText(); }
    ;
    
/**
 * RFC 4514, Section 3:
 * SUTF1 = %x01-21 / %x23-2A / %x2D-3A /
 *    %x3D / %x3F-5B / %x5D-7F
 *
 * The rule LUTF1_REST doesn't contain the following charcters,
 * so we must check them additionally
 *   EQUALS (0x3D) 
-*   HYPHEN (0x2D)  
 *   DIGIT (0x30-0x39)
 *   ALPHA (0x41-0x5A and 0x61-0x7A)
 *   SHARP
 *   SPACE
 */
sutf1 returns [String sutf1=""]
    {
        matchedProduction( "sutf1()" );
    }
    :
    rest:LUTF1_REST { sutf1 = rest.getText(); }
    |
    equals:EQUALS { sutf1 = equals.getText(); }
    |
    hyphen:HYPHEN { sutf1 = hyphen.getText(); }
    |
    digit:DIGIT { sutf1 = digit.getText(); }
    |
    alpha:ALPHA { sutf1 = alpha.getText(); }
    |
    sharp:SHARP { sutf1 = sharp.getText(); }
    | 
    space:SPACE  { sutf1 = space.getText(); }
    ;    


utfmb returns [String utfmb]
    {
        matchedProduction( "utfmb()" );
    }
    :
    s:UTFMB { utfmb = s.getText(); }
    ;


    /**
     * RFC 4514, Section 3
     * pair = ESC ( ESC / special / hexpair )
     * special = escaped / SPACE / SHARP / EQUALS
     * escaped = DQUOTE / PLUS / COMMA / SEMI / LANGLE / RANGLE
     * hexpair = HEX HEX
     *
     * If in <string> form, a LDAP string representation asserted value can
     * be obtained by replacing (left to right, non-recursively) each <pair>
     * appearing in the <string> as follows:
     *   replace <ESC><ESC> with <ESC>;
     *   replace <ESC><special> with <special>;
     *   replace <ESC><hexpair> with the octet indicated by the <hexpair>.
     * 
     * RFC 2253, Section 3
     * pair       = "\" ( special / "\" / QUOTATION / hexpair )
     * special    = "," / "=" / "+" / "<" /  ">" / "#" / ";"
     * 
     * RFC 1779, Section 2.3
     * <pair> ::= "\" ( <special> | "\" | '"')
     * <special> ::= "," | "=" | <CR> | "+" | "<" |  ">"
     *           | "#" | ";"
     * 
     * TODO: The ESC is removed from the norm value. This needs review! 
     */ 
pair [UpAndNormValue value] returns [byte[] pair]
    {
        matchedProduction( "pair()" );
        String tmp;
    }
    :
    (
        ESCESC 
        { 
            value.upValue += "\\\\";
            pair = StringTools.getBytesUtf8( "\\" );
        } 
    )
    |
    (
        ESCSHARP 
        { 
            value.upValue += "\\#";
            pair = StringTools.getBytesUtf8( "#" );
        } 
    )
    |
    ( 
        ESC { value.upValue += "\\"; }
        tmp = special 
        { 
            value.upValue += tmp;
            pair = StringTools.getBytesUtf8( tmp ); 
        }
    )
    |
    ( 
        hexpair:HEXPAIR 
        { 
            value.upValue += "\\" + hexpair.getText(); 
            pair = StringTools.toByteArray( hexpair.getText() ); 
        } 
    )
    ;


    /**
     * RFC 4514 Section 3
     * 
     * special = escaped / SPACE / SHARP / EQUALS
     * escaped = DQUOTE / PLUS / COMMA / SEMI / LANGLE / RANGLE
     *
     * TODO: For space an ESC is manually added. This needs review! 
     */ 
special returns [String special]
    {
        matchedProduction( "special()" );
    }
    :
    (
        dquote:DQUOTE { special = dquote.getText(); }
        |
        plus:PLUS { special = plus.getText(); }
        |
        comma:COMMA { special = comma.getText(); }
        |
        semi:SEMI { special = semi.getText(); }
        |
        langle:LANGLE { special = langle.getText(); }
        |
        rangle:RANGLE { special = rangle.getText(); }
        |
        space:SPACE { special = space.getText(); }
        |
        sharp:SHARP { special = sharp.getText(); }
        |
        equals:EQUALS { special = equals.getText(); }
    )
    ;






    
    
    