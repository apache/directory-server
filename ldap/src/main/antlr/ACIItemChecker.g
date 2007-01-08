header
{
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


package org.apache.directory.shared.ldap.aci;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;

import javax.naming.directory.Attribute;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.filter.AssertionEnum;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationModifier;
import org.apache.directory.shared.ldap.util.ComponentsMonitor;
import org.apache.directory.shared.ldap.util.MandatoryAndOptionalComponentsMonitor;
import org.apache.directory.shared.ldap.util.MandatoryComponentsMonitor;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.apache.directory.shared.ldap.util.NoDuplicateKeysMap;
import org.apache.directory.shared.ldap.util.OptionalComponentsMonitor;
import org.apache.directory.shared.ldap.name.LdapDN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

}


// ----------------------------------------------------------------------------
// parser class definition
// ----------------------------------------------------------------------------

/**
 * The antlr generated ACIItem checker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrACIItemChecker extends Parser;


// ----------------------------------------------------------------------------
// parser options
// ----------------------------------------------------------------------------

options
{
    k = 1; // ;-)
    defaultErrorHandler = false;
}


// ----------------------------------------------------------------------------
// imaginary tokens
// ----------------------------------------------------------------------------

tokens
{
    ATTRIBUTE_VALUE_CANDIDATE;
    RANGE_OF_VALUES_CANDIDATE;
}


// ----------------------------------------------------------------------------
// parser initialization
// ----------------------------------------------------------------------------

{
    // subordinate parser instances
    private final FilterParserImpl filterParser = new FilterParserImpl();
    
    private boolean isNormalizing = false;
    NameComponentNormalizer normalizer;
    
    /**
     * Creates a (normalizing) subordinate DnParser for parsing Names.
     * This method MUST be called for each instance while we cannot do
     * constructor overloading for this class.
     *
     * @return the DnParser to be used for parsing Names
     */
    public void init()
    {
    }

    /**
     * Sets the NameComponentNormalizer for this parser's dnParser.
     */
    public void setNormalizer(NameComponentNormalizer normalizer)
    {
        this.normalizer = normalizer;
        this.isNormalizing = true;
    }

    private int token2Integer( Token token ) throws RecognitionException
    {
        int i = 0;
        
        try
        {
            i = Integer.parseInt( token.getText());
        }
        catch ( NumberFormatException e )
        {
            throw new RecognitionException( "Value of INTEGER token " +
                                            token.getText() +
                                            " cannot be converted to an Integer" );
        }
        
        return i;
    }
}


// ----------------------------------------------------------------------------
// parser productions
// ----------------------------------------------------------------------------

wrapperEntryPoint
    :
    ( SP )* theACIItem ( SP )* EOF
    ;

theACIItem
    :
    OPEN_CURLY
        ( SP )* mainACIItemComponent ( SP )*
            ( SEP ( SP )* mainACIItemComponent ( SP )* )*
    CLOSE_CURLY
    ;
    
mainACIItemComponent
    :
    aci_identificationTag
    | aci_precedence
    | aci_authenticationLevel
    | aci_itemOrUserFirst
    ;
    
aci_identificationTag
    :
    ID_identificationTag ( SP )+ SAFEUTF8STRING
    ;

aci_precedence
    :
    precedence
    ;

precedence
    :
    ID_precedence ( SP )+ INTEGER
    ;

aci_authenticationLevel
    :
    ID_authenticationLevel ( SP )+ authenticationLevel
    ;

authenticationLevel
    :
    ID_none
    |
    ID_simple
    |
    ID_strong
    ;

aci_itemOrUserFirst
    :
    ID_itemOrUserFirst ( SP )+ itemOrUserFirst
    ;

itemOrUserFirst
    :
    itemFirst | userFirst
    ;

itemFirst
    :
    ID_itemFirst ( SP )* COLON ( SP )*
        OPEN_CURLY ( SP )*
            ( 
              protectedItems ( SP )*
                SEP ( SP )* itemPermissions
            | // relaxing
              itemPermissions ( SP )*
                SEP ( SP )* protectedItems
            )
        ( SP )* CLOSE_CURLY
    ;

userFirst
    :
    ID_userFirst ( SP )* COLON ( SP )*
        OPEN_CURLY ( SP )*
            (
              userClasses ( SP )*
                SEP ( SP )* userPermissions
            | // relaxing
              userPermissions ( SP )*
                SEP ( SP )* userClasses
            )
        ( SP )* CLOSE_CURLY
    ;

protectedItems
    :
    ID_protectedItems ( SP )*
        OPEN_CURLY ( SP )*
            (
                protectedItem ( SP )*
                    ( SEP ( SP )* protectedItem ( SP )* )*
            )?
        CLOSE_CURLY
    ;

protectedItem
    :
    entry
    | allUserAttributeTypes
    | attributeType
    | allAttributeValues 
    | allUserAttributeTypesAndValues
    | attributeValue
    | selfValue
    | rangeOfValues
    | maxValueCount
    | maxImmSub
    | restrictedBy
    | classes
    ;

entry
    :
    ID_entry
    ;

allUserAttributeTypes
    :
    ID_allUserAttributeTypes
    ;

attributeType
    :
    ID_attributeType ( SP )+ attributeTypeSet
    ;

allAttributeValues
    :
    ID_allAttributeValues ( SP )+ attributeTypeSet
    ;

allUserAttributeTypesAndValues
    :
    ID_allUserAttributeTypesAndValues
    ;

attributeValue
    :
    ATTRIBUTE_VALUE_CANDIDATE // ate the identifier for subordinate dn parser workaround
    ;

selfValue
    :
    ID_selfValue ( SP )+ attributeTypeSet
    ;

rangeOfValues
    :
    RANGE_OF_VALUES_CANDIDATE
    ;

maxValueCount
    :
    ID_maxValueCount ( SP )+
    OPEN_CURLY ( SP )*
        aMaxValueCount ( SP )*
            ( SEP ( SP )* aMaxValueCount ( SP )*
            )*
    CLOSE_CURLY
    ;

aMaxValueCount
    :
    OPEN_CURLY ( SP )*
        (
          ID_type ( SP )+ oid ( SP )* SEP ( SP )*
          ID_maxCount ( SP )+ INTEGER
        | // relaxing
          ID_maxCount ( SP )+ INTEGER ( SP )* SEP ( SP )*
          ID_type ( SP )+ oid
        )
    ( SP )* CLOSE_CURLY
    ;

maxImmSub
    :
    ID_maxImmSub ( SP )+ INTEGER
    ;

restrictedBy
    :
    ID_restrictedBy ( SP )+
        OPEN_CURLY ( SP )*
            restrictedValue ( SP )*
                    ( SEP ( SP )* restrictedValue ( SP )*
                    )*
        CLOSE_CURLY
    ;

restrictedValue
    :
    OPEN_CURLY ( SP )*
        (
          ID_type ( SP )+ oid ( SP )* SEP ( SP )*
          ID_valuesIn ( SP )+ oid
        | // relaxing
          ID_valuesIn ( SP )+ oid ( SP )* SEP ( SP )*
          ID_type ( SP )+ oid
        )
    ( SP )* CLOSE_CURLY
    ;

attributeTypeSet 
    :
    OPEN_CURLY ( SP )*
        oid ( SP )*
            ( SEP ( SP )* oid ( SP )*
            )*
    CLOSE_CURLY
    ;

classes
    :
    ID_classes ( SP )+ refinement
    ;

itemPermissions
    :
    ID_itemPermissions ( SP )+
        OPEN_CURLY ( SP )*
            ( itemPermission ( SP )*
                ( SEP ( SP )* itemPermission ( SP )*
                )*
            )?
        CLOSE_CURLY
    ;

itemPermission
    :
    OPEN_CURLY ( SP )*
        anyItemPermission ( SP )*
            ( SEP ( SP )* anyItemPermission ( SP )* )*
    CLOSE_CURLY
    ;

anyItemPermission
    :
    precedence
    | userClasses
    | grantsAndDenials
    ;

grantsAndDenials
    :
    ID_grantsAndDenials ( SP )+
    OPEN_CURLY ( SP )*
        ( grantAndDenial ( SP )*
            ( SEP ( SP )* grantAndDenial ( SP )*
            )*
        )?
    CLOSE_CURLY
    ;

grantAndDenial
    :
    ID_grantAdd 
    | ID_denyAdd
    | ID_grantDiscloseOnError
    | ID_denyDiscloseOnError 
    | ID_grantRead
    | ID_denyRead
    | ID_grantRemove
    | ID_denyRemove 
    //-- permissions that may be used only in conjunction
    //-- with the entry component
    | ID_grantBrowse
    | ID_denyBrowse
    | ID_grantExport
    | ID_denyExport
    | ID_grantImport
    | ID_denyImport
    | ID_grantModify
    | ID_denyModify
    | ID_grantRename
    | ID_denyRename
    | ID_grantReturnDN
    | ID_denyReturnDN
    //-- permissions that may be used in conjunction
    //-- with any component, except entry, of ProtectedItems
    | ID_grantCompare
    | ID_denyCompare
    | ID_grantFilterMatch
    | ID_denyFilterMatch
    | ID_grantInvoke
    | ID_denyInvoke
    ;

userClasses
    :
    ID_userClasses ( SP )+
    OPEN_CURLY ( SP )*
        (
            userClass ( SP )*
                ( SEP ( SP )* userClass ( SP )* )*
        )?
    CLOSE_CURLY
    ;

userClass
    :
    allUsers
    | thisEntry 
    | name
    | userGroup
    | subtree
    ;

allUsers
    :
    ID_allUsers
    ;

thisEntry
    :
    ID_thisEntry
    ;

name
    :
    ID_name ( SP )+ 
        OPEN_CURLY ( SP )*
            distinguishedName ( SP )*
                ( SEP ( SP )* distinguishedName ( SP )*
			)*
        CLOSE_CURLY
    ;

userGroup
    :
    ID_userGroup ( SP )+ 
        OPEN_CURLY ( SP )*
            distinguishedName ( SP )*
                ( SEP ( SP )* distinguishedName ( SP )* )*
        CLOSE_CURLY
    ;

subtree
    :
    ID_subtree ( SP )+
        OPEN_CURLY ( SP )*
            subtreeSpecification ( SP )*
                ( SEP ( SP )* subtreeSpecification ( SP )* )*
        CLOSE_CURLY
    ;

userPermissions
    :
    ID_userPermissions ( SP )+
        OPEN_CURLY ( SP )*
            ( userPermission ( SP )*
                ( SEP ( SP )* userPermission ( SP )* )*
            )?
        CLOSE_CURLY
    ;

userPermission
     :
     OPEN_CURLY ( SP )*
         anyUserPermission ( SP )*
             ( SEP ( SP )* anyUserPermission ( SP )* )*
     CLOSE_CURLY
     ;

anyUserPermission
    :
    precedence
    | protectedItems
    | grantsAndDenials
    ;

subtreeSpecification
    :
    OPEN_CURLY ( SP )*
        ( subtreeSpecificationComponent ( SP )*
            ( SEP ( SP )* subtreeSpecificationComponent ( SP )* )* )?
    CLOSE_CURLY
    ;

subtreeSpecificationComponent
    :
    ss_base
    | ss_specificExclusions
    | ss_minimum
    | ss_maximum
    ;

ss_base
    :
    ID_base ( SP )+ distinguishedName
    ;

ss_specificExclusions
    :
    ID_specificExclusions ( SP )+ specificExclusions
    ;

specificExclusions
    :
    OPEN_CURLY ( SP )*
        ( specificExclusion ( SP )*
            ( SEP ( SP )* specificExclusion ( SP )* )*
        )?
    CLOSE_CURLY
    ;

specificExclusion
    :
    chopBefore | chopAfter
    ;

chopBefore
    :
    ID_chopBefore ( SP )* COLON ( SP )* distinguishedName
    ;

chopAfter
    :
    ID_chopAfter ( SP )* COLON ( SP )* distinguishedName
    ;

ss_minimum
    :
    ID_minimum ( SP )+ baseDistance
    ;

ss_maximum
    :
    ID_maximum ( SP )+ baseDistance
    ;

distinguishedName
    :
    SAFEUTF8STRING
    ;

baseDistance
    :
    INTEGER
    ;

oid
    :
    ( DESCR | NUMERICOID )
    ;

refinement
    :
    item | and | or | not
    ;

item
    :
    ID_item ( SP )* COLON ( SP )* oid
    ;

and
    :
    ID_and ( SP )* COLON ( SP )* refinements
    ;

or
    :
    ID_or ( SP )* COLON ( SP )* refinements
    ;

not
    :
    ID_not ( SP )* COLON ( SP )* refinements
    ;

refinements
    :
    OPEN_CURLY ( SP )*
    (
        refinement ( SP )*
        ( SEP ( SP )* refinement ( SP )* )*
    )? CLOSE_CURLY
    ;

    
//  ----------------------------------------------------------------------------
//  lexer class definition
//  ----------------------------------------------------------------------------

/**
  * The parser's primary lexer.
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$
  */
class AntlrACIItemCheckerLexer extends Lexer;


//  ----------------------------------------------------------------------------
//  lexer options
//  ----------------------------------------------------------------------------

options
{
    k = 2;
    charVocabulary = '\3'..'\377';
}


//----------------------------------------------------------------------------
// tokens
//----------------------------------------------------------------------------

tokens
{
    ID_identificationTag = "identificationTag";
    ID_precedence = "precedence";
    ID_FALSE = "FALSE";
    ID_TRUE = "TRUE";
    ID_none = "none";
    ID_simple = "simple";
    ID_strong = "strong";
    ID_level = "level";
    ID_basicLevels = "basicLevels";
    ID_localQualifier = "localQualifier";
    ID_signed = "signed";
    ID_authenticationLevel = "authenticationLevel";
    ID_itemOrUserFirst = "itemOrUserFirst";
    ID_itemFirst = "itemFirst";
    ID_userFirst = "userFirst";
    ID_protectedItems = "protectedItems";
    ID_classes = "classes";
    ID_entry = "entry";
    ID_allUserAttributeTypes = "allUserAttributeTypes";
    ID_attributeType = "attributeType";
    ID_allAttributeValues = "allAttributeValues";
    ID_allUserAttributeTypesAndValues = "allUserAttributeTypesAndValues";
    ID_selfValue = "selfValue";
    ID_item = "item";
    ID_and = "and";
    ID_or = "or";
    ID_not = "not";
    ID_rangeOfValues = "rangeOfValues";
    ID_maxValueCount = "maxValueCount";
    ID_type = "type";
    ID_maxCount = "maxCount";
    ID_maxImmSub = "maxImmSub";
    ID_restrictedBy = "restrictedBy";
    ID_valuesIn = "valuesIn";
    ID_userClasses = "userClasses";
    ID_base = "base";
    ID_specificExclusions = "specificExclusions";
    ID_chopBefore = "chopBefore";
    ID_chopAfter = "chopAfter";
    ID_minimum = "minimum";
    ID_maximum = "maximum";
    ID_specificationFilter = "specificationFilter";
    ID_grantsAndDenials = "grantsAndDenials";
    ID_itemPermissions = "itemPermissions";
    ID_userPermissions = "userPermissions";
    ID_allUsers = "allUsers";
    ID_thisEntry = "thisEntry";
    ID_subtree = "subtree";
    ID_name = "name";
    ID_userGroup = "userGroup";

    ID_grantAdd = "grantAdd"; // (0),
    ID_denyAdd = "denyAdd";  // (1),
    ID_grantDiscloseOnError = "grantDiscloseOnError";  // (2),
    ID_denyDiscloseOnError = "denyDiscloseOnError";  // (3),
    ID_grantRead = "grantRead";  // (4),
    ID_denyRead = "denyRead";  // (5),
    ID_grantRemove = "grantRemove";  // (6),
    ID_denyRemove = "denyRemove";  // (7),
    //-- permissions that may be used only in conjunction
    //-- with the entry component
    ID_grantBrowse = "grantBrowse";  // (8),
    ID_denyBrowse = "denyBrowse";  // (9),
    ID_grantExport = "grantExport";  // (10),
    ID_denyExport = "denyExport";  // (11),
    ID_grantImport = "grantImport";  // (12),
    ID_denyImport = "denyImport";  // (13),
    ID_grantModify = "grantModify";  // (14),
    ID_denyModify = "denyModify";  // (15),
    ID_grantRename = "grantRename";  // (16),
    ID_denyRename = "denyRename";  // (17),
    ID_grantReturnDN = "grantReturnDN";  // (18),
    ID_denyReturnDN = "denyReturnDN";  // (19),
    //-- permissions that may be used in conjunction
    //-- with any component, except entry, of ProtectedItems
    ID_grantCompare = "grantCompare";  // (20),
    ID_denyCompare = "denyCompare";  // (21),
    ID_grantFilterMatch = "grantFilterMatch";  // (22),
    ID_denyFilterMatch = "denyFilterMatch";  // (23),
    ID_grantInvoke = "grantInvoke";  // (24),
    ID_denyInvoke = "denyInvoke";  // (25)
}


// ----------------------------------------------------------------------------
//  lexer initialization
// ----------------------------------------------------------------------------


// ----------------------------------------------------------------------------
// attribute description lexer rules from models
// ----------------------------------------------------------------------------

//  This is all messed up - could not figure out how to get antlr to represent
//  the safe UTF-8 character set from RFC 3642 for production SafeUTF8Character

protected SAFEUTF8CHAR :
    '\u0001'..'\u0021' |
    '\u0023'..'\u007F' |
    '\u00c0'..'\u00d6' |
    '\u00d8'..'\u00f6' |
    '\u00f8'..'\u00ff' |
    '\u0100'..'\u1fff' |
    '\u3040'..'\u318f' |
    '\u3300'..'\u337f' |
    '\u3400'..'\u3d2d' |
    '\u4e00'..'\u9fff' |
    '\uf900'..'\ufaff' ;

OPEN_CURLY : '{' ;

CLOSE_CURLY : '}' ;

SEP : ',' ;

SP : ' ' | '\t' | '\n' { newline(); } | '\r' ;

COLON : ':' ;

protected DIGIT : '0' | LDIGIT ;

protected LDIGIT : '1'..'9' ;

protected ALPHA : 'A'..'Z' | 'a'..'z' ;

protected INTEGER : DIGIT | ( LDIGIT ( DIGIT )+ ) ;

protected HYPHEN : '-' ;

protected NUMERICOID : INTEGER ( DOT INTEGER )+ ;

protected DOT : '.' ;

INTEGER_OR_NUMERICOID
    :
    ( INTEGER DOT ) => NUMERICOID
    {
        $setType( NUMERICOID );
    }
    |
    INTEGER
    {
        $setType( INTEGER );
    }
    ;

SAFEUTF8STRING : '"'! ( SAFEUTF8CHAR )* '"'! ;

DESCR // THIS RULE ALSO STANDS FOR AN IDENTIFIER
    :
    ( "attributeValue" ( SP! )+ '{' ) =>
      "attributeValue"! ( SP! )+ '{'! ( options { greedy = false; } : . )* '}'!
      { $setType( ATTRIBUTE_VALUE_CANDIDATE ); }
    | ( "rangeOfValues" ( SP! )+ '(' ) =>
      "rangeOfValues"! ( SP! )+ FILTER
      { $setType( RANGE_OF_VALUES_CANDIDATE ); }
    | ALPHA ( ALPHA | DIGIT | HYPHEN )*
    ;

protected FILTER : '(' ( ( '&' (SP)* (FILTER)+ ) | ( '|' (SP)* (FILTER)+ ) | ( '!' (SP)* FILTER ) | FILTER_VALUE ) ')' (SP)* ;

protected FILTER_VALUE : (options{greedy=true;}: ~( ')' | '(' | '&' | '|' | '!' ) ( ~(')') )* ) ;

    