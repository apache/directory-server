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
import javax.naming.directory.BasicAttribute;
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
 * The antlr generated ACIItem parser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrACIItemParser extends Parser;


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
    private static final Logger log = LoggerFactory.getLogger( AntlrACIItemParser.class );
    
    // subordinate parser instances
    private final FilterParserImpl filterParser = new FilterParserImpl();
    
    private boolean isNormalizing = false;
    NameComponentNormalizer normalizer;
    
    // nonshared global data needed to avoid extensive pass/return stuff
    // these are only used by three first order components
    private String m_identificationTag;
    private AuthenticationLevel m_authenticationLevel;
    private int m_aciPrecedence;
    
    private boolean isItemFirstACIItem;
    
    // shared global data needed to avoid extensive pass/return stuff
    private Set m_protectedItems;
    private Map m_protectedItemsMap;
    private Set m_userClasses;
    private Map m_userClassesMap;
    private Set m_itemPermissions;
    private int m_precedence;
    private Set m_grantsAndDenials;
    private Set m_userPermissions;
    private Map oidsMap;
    
    private Set chopBeforeExclusions;
    private Set chopAfterExclusions;
    private SubtreeSpecificationModifier ssModifier = null;
    
    private ComponentsMonitor mainACIItemComponentsMonitor;
    private ComponentsMonitor itemPermissionComponentsMonitor;
    private ComponentsMonitor userPermissionComponentsMonitor;
    private ComponentsMonitor subtreeSpecificationComponentsMonitor;
    
    
    /**
     * Creates a (normalizing) subordinate DnParser for parsing Names.
     * This method MUST be called for each instance while we cannot do
     * constructor overloading for this class.
     *
     * @return the DnParser to be used for parsing Names
     */
    public void init( Map oidsMap )
    {
    	this.oidsMap = oidsMap;
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

wrapperEntryPoint returns [ ACIItem l_ACIItem ]
{
    log.debug( "entered wrapperEntryPoint()" );
    l_ACIItem = null;
}
    :
    ( SP )* l_ACIItem=theACIItem ( SP )* EOF
    ;

theACIItem returns [ ACIItem l_ACIItem ]
{
    log.debug( "entered theACIItem()" );
    l_ACIItem = null;
    mainACIItemComponentsMonitor = new MandatoryComponentsMonitor( 
            new String [] { "identificationTag", "precedence", "authenticationLevel", "itemOrUserFirst" } );
}
    :
    OPEN_CURLY
        ( SP )* mainACIItemComponent ( SP )*
            ( SEP ( SP )* mainACIItemComponent ( SP )* )*
    CLOSE_CURLY
    {
        if ( !mainACIItemComponentsMonitor.finalStateValid() )
        {
            throw new RecognitionException( "Missing mandatory ACIItem components: " 
                    + mainACIItemComponentsMonitor.getRemainingComponents() );
        }
        
        if ( isItemFirstACIItem )
        {
            l_ACIItem = new ItemFirstACIItem(
                    m_identificationTag,
                    m_precedence,
                    m_authenticationLevel,
                    m_protectedItems,
                    m_itemPermissions );
        }
        else
        {
            l_ACIItem = new UserFirstACIItem(
                    m_identificationTag,
                    m_aciPrecedence,
                    m_authenticationLevel,
                    m_userClasses,
                    m_userPermissions );
        }
        
    }
    ;
    
mainACIItemComponent
{
    log.debug( "entered mainACIItemComponent()" );
}
    :
    aci_identificationTag
    {
        mainACIItemComponentsMonitor.useComponent( "identificationTag" );
    }
    | aci_precedence
    {
        mainACIItemComponentsMonitor.useComponent( "precedence" );
    }
    | aci_authenticationLevel
    {
        mainACIItemComponentsMonitor.useComponent( "authenticationLevel" );
    }
    | aci_itemOrUserFirst
    {
        mainACIItemComponentsMonitor.useComponent( "itemOrUserFirst" );
    }
    ;
    exception
    catch [IllegalArgumentException e]
    {
        throw new RecognitionException( e.getMessage() );
    }
    
aci_identificationTag
{
    log.debug( "entered aci_identificationTag()" );
}
    :
    ID_identificationTag ( SP )+ token:SAFEUTF8STRING
    {
        m_identificationTag = token.getText();
    }
    ;

aci_precedence
{
    log.debug( "entered aci_precedence()" );
}
    :
    precedence
    {
        m_aciPrecedence = m_precedence;
    }
    ;

precedence
{
    log.debug( "entered precedence()" );
}
    :
    ID_precedence ( SP )+ token:INTEGER
    {
        m_precedence = token2Integer( token );
        if( m_precedence < 0 || m_precedence > 255 )
        {
            throw new RecognitionException( "Expecting INTEGER token having an Integer value between 0 and 255, found " + m_precedence );
        }
    }
    ;

aci_authenticationLevel
{
    log.debug( "entered aci_authenticationLevel()" );
}
    :
    ID_authenticationLevel ( SP )+ authenticationLevel
    ;

authenticationLevel
{
    log.debug( "entered authenticationLevel()" );
}
    :
    ID_none
    {
        m_authenticationLevel = AuthenticationLevel.NONE;
    }
    |
    ID_simple
    {
        m_authenticationLevel = AuthenticationLevel.SIMPLE;
    }
    |
    ID_strong
    {
        m_authenticationLevel = AuthenticationLevel.STRONG;
    }
    ;

aci_itemOrUserFirst
{
    log.debug( "entered aci_itemOrUserFirst()" );
}
    :
    ID_itemOrUserFirst ( SP )+ itemOrUserFirst
    ;

itemOrUserFirst
{
    log.debug( "entered itemOrUserFirst()" );
}
    :
    itemFirst | userFirst
    ;

itemFirst
{
    log.debug( "entered itemFirst()" );
}
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
    {
        isItemFirstACIItem = true;
    }
    ;

userFirst
{
    log.debug( "entered userFirst()" );
}
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
    {
        isItemFirstACIItem = false;
    }
    ;

protectedItems
{
    log.debug( "entered protectedItems()" );
    m_protectedItemsMap = new NoDuplicateKeysMap();
}
    :
    ID_protectedItems ( SP )*
        OPEN_CURLY ( SP )*
            (
                protectedItem ( SP )*
                    ( SEP ( SP )* protectedItem ( SP )* )*
            )?
        CLOSE_CURLY
    {
        m_protectedItems = new HashSet( m_protectedItemsMap.values() );
    }
    ;
    exception
    catch [IllegalArgumentException e]
    {
        throw new RecognitionException( "Protected Items cannot be duplicated. " + e.getMessage() );
    }

protectedItem
{
    log.debug( "entered protectedItem()" );
}
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
{
    log.debug( "entered entry()" );  
}
    :
    ID_entry
    {
        m_protectedItemsMap.put( "entry", ProtectedItem.ENTRY );
    }
    ;

allUserAttributeTypes
{
    log.debug( "entered allUserAttributeTypes()" );
}
    :
    ID_allUserAttributeTypes
    {
        m_protectedItemsMap.put( "allUserAttributeTypes", ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );
    }
    ;

attributeType
{
    log.debug( "entered attributeType()" );
    Set l_attributeTypeSet = null;
}
    :
    ID_attributeType ( SP )+ l_attributeTypeSet=attributeTypeSet
    {
        m_protectedItemsMap.put( "attributeType", new ProtectedItem.AttributeType( l_attributeTypeSet ) );
    }
    ;

allAttributeValues
{
    log.debug( "entered allAttributeValues()" );
    Set l_attributeTypeSet = null;
}
    :
    ID_allAttributeValues ( SP )+ l_attributeTypeSet=attributeTypeSet
    {
        m_protectedItemsMap.put( "allAttributeValues", new ProtectedItem.AllAttributeValues( l_attributeTypeSet ) );
    }
    ;

allUserAttributeTypesAndValues
{
    log.debug( "entered allUserAttributeTypesAndValues()" );
}
    :
    ID_allUserAttributeTypesAndValues
    {
        m_protectedItemsMap.put( "allUserAttributeTypesAndValues", ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );
    }
    ;

attributeValue
{
    log.debug( "entered attributeValue()" );
    String attributeTypeAndValue = null;
    String attributeType = null;
    String attributeValue = null;
    Set attributeSet = new HashSet();
}
    :
    token:ATTRIBUTE_VALUE_CANDIDATE // ate the identifier for subordinate dn parser workaround
    {
        // A Dn can be considered as a set of attributeTypeAndValues
        // So, parse the set as a Dn and extract each attributeTypeAndValue
        LdapDN attributeTypeAndValueSetAsDn = new LdapDN( token.getText() );
        attributeTypeAndValueSetAsDn.normalize( oidsMap );
        Enumeration attributeTypeAndValueSet = attributeTypeAndValueSetAsDn.getAll();
        while ( attributeTypeAndValueSet.hasMoreElements() )
        {
            attributeTypeAndValue = ( String ) attributeTypeAndValueSet.nextElement();
            attributeType = NamespaceTools.getRdnAttribute( attributeTypeAndValue );
            attributeValue = NamespaceTools.getRdnValue( attributeTypeAndValue );
            attributeSet.add( new BasicAttribute( attributeType, attributeValue ) );
            log.debug( "An attributeTypeAndValue from the set: " + attributeType + "=" +  attributeValue);
        }
        m_protectedItemsMap.put( "attributeValue", new ProtectedItem.AttributeValue( attributeSet ) );
    }
    ;
    exception
    catch [Exception e]
    {
        throw new RecognitionException( "dnParser failed for " + token.getText() + " , " + e.getMessage() );
    }

selfValue
{
    log.debug( "entered selfValue()" );
    Set l_attributeTypeSet = null;
}
    :
    ID_selfValue ( SP )+ l_attributeTypeSet=attributeTypeSet
    {
        m_protectedItemsMap.put( "sefValue", new ProtectedItem.SelfValue( l_attributeTypeSet ) );
    }
    ;

rangeOfValues
{
    log.debug( "entered rangeOfValues()" );
}
    :
    token:RANGE_OF_VALUES_CANDIDATE
    {
        m_protectedItemsMap.put( "rangeOfValues",
                new ProtectedItem.RangeOfValues(
                        filterParser.parse( token.getText() ) ) );
        log.debug( "filterParser parsed " + token.getText() );
    }
    ;
    exception
    catch [Exception e]
    {
        throw new RecognitionException( "filterParser failed. " + e.getMessage() );
    }   

maxValueCount
{
    log.debug( "entered maxValueCount()" );
    ProtectedItem.MaxValueCountItem l_maxValueCount = null;
    Set maxValueCountSet = new HashSet();
}
    :
    ID_maxValueCount ( SP )+
    OPEN_CURLY ( SP )*
        l_maxValueCount=aMaxValueCount ( SP )*
        {
            maxValueCountSet.add( l_maxValueCount );
        }
            ( SEP ( SP )* l_maxValueCount=aMaxValueCount ( SP )*
            {
                maxValueCountSet.add( l_maxValueCount );
            }
            )*
    CLOSE_CURLY
    {
        m_protectedItemsMap.put( "maxValueCount", new ProtectedItem.MaxValueCount( maxValueCountSet ) );
    }
    ;

aMaxValueCount returns [ ProtectedItem.MaxValueCountItem l_maxValueCount ]
{
    log.debug( "entered aMaxValueCount()" );
    l_maxValueCount = null;
    String l_oid = null;
    Token token = null;
}
    :
    OPEN_CURLY ( SP )*
        (
          ID_type ( SP )+ l_oid=oid ( SP )* SEP ( SP )*
          ID_maxCount ( SP )+ token1:INTEGER
          { token = token1; }
        | // relaxing
          ID_maxCount ( SP )+ token2:INTEGER ( SP )* SEP ( SP )*
          ID_type ( SP )+ l_oid=oid
          { token = token2; }
        )
    ( SP )* CLOSE_CURLY
    {
        l_maxValueCount = new ProtectedItem.MaxValueCountItem( l_oid, token2Integer( token ) );
    }
    ;

maxImmSub
{
    log.debug( "entered maxImmSub()" );
}
    :
    ID_maxImmSub ( SP )+ token:INTEGER
    {
        
        m_protectedItemsMap.put( "maxImmSub",
                new ProtectedItem.MaxImmSub(
                        token2Integer( token ) ) );
    }
    ;

restrictedBy
{
    log.debug( "entered restrictedBy()" );
    ProtectedItem.RestrictedByItem l_restrictedValue = null;
    Set l_restrictedBy = new HashSet();
}
    :
    ID_restrictedBy ( SP )+
        OPEN_CURLY ( SP )*
            l_restrictedValue=restrictedValue ( SP )*
            {
                l_restrictedBy.add( l_restrictedValue );
            }
                    ( SEP ( SP )* l_restrictedValue=restrictedValue ( SP )*
                    {
                        l_restrictedBy.add( l_restrictedValue );
                    }
                    )*
        CLOSE_CURLY
    {
        m_protectedItemsMap.put( "restrictedBy", new ProtectedItem.RestrictedBy( l_restrictedBy ) );
    }
    ;

restrictedValue returns [ ProtectedItem.RestrictedByItem l_restrictedValue ]
{
    log.debug( "entered restrictedValue()" );
    String typeOid = null;
    String valuesInOid = null;
    l_restrictedValue = null;
}
    :
    OPEN_CURLY ( SP )*
        (
          ID_type ( SP )+ typeOid=oid ( SP )* SEP ( SP )*
          ID_valuesIn ( SP )+ valuesInOid=oid
        | // relaxing
          ID_valuesIn ( SP )+ valuesInOid=oid ( SP )* SEP ( SP )*
          ID_type ( SP )+ typeOid=oid
        )
    ( SP )* CLOSE_CURLY
    {
        l_restrictedValue = new ProtectedItem.RestrictedByItem( typeOid, valuesInOid );
    }
    ;

attributeTypeSet returns [ Set l_attributeTypeSet ]
{
    log.debug( "entered attributeTypeSet()" );
    String l_oid = null;
    l_attributeTypeSet = new HashSet();
}
    :
    OPEN_CURLY ( SP )*
        l_oid=oid ( SP )*
        {
            l_attributeTypeSet.add( l_oid );
        }
            ( SEP ( SP )* l_oid=oid ( SP )*
            {
                l_attributeTypeSet.add( l_oid );
            }
            )*
    CLOSE_CURLY
    ;

classes
{
    log.debug( "entered classes()" );
    ExprNode l_classes = null;
}
    :
    ID_classes ( SP )+ l_classes=refinement
    {
        m_protectedItemsMap.put( "classes", new ProtectedItem.Classes( l_classes ) );
    }
    ;

itemPermissions
{
    log.debug( "entered itemPermissions()" );
    m_itemPermissions = new HashSet();
    ItemPermission l_itemPermission = null;
}
    :
    ID_itemPermissions ( SP )+
        OPEN_CURLY ( SP )*
            ( l_itemPermission=itemPermission ( SP )*
              {
                  m_itemPermissions.add( l_itemPermission );
              }
                ( SEP ( SP )* l_itemPermission=itemPermission ( SP )*
                  {
                      m_itemPermissions.add( l_itemPermission );
                  }
                )*
            )?
        CLOSE_CURLY
    ;

itemPermission returns [ ItemPermission l_itemPermission ]
{
    log.debug( "entered itemPermission()" );
    l_itemPermission = null;
    itemPermissionComponentsMonitor = new MandatoryAndOptionalComponentsMonitor( 
            new String [] { "userClasses", "grantsAndDenials" }, new String [] { "precedence" } );
}
    :
    OPEN_CURLY ( SP )*
        anyItemPermission ( SP )*
            ( SEP ( SP )* anyItemPermission ( SP )* )*
    CLOSE_CURLY
    {
        if ( !itemPermissionComponentsMonitor.finalStateValid() )
        {
            throw new RecognitionException( "Missing mandatory itemPermission components: " 
                    + itemPermissionComponentsMonitor.getRemainingComponents() );
        }
        
        l_itemPermission = new ItemPermission( m_precedence, m_grantsAndDenials, m_userClasses );
    }
    ;

anyItemPermission
    :
    precedence
    {
        itemPermissionComponentsMonitor.useComponent( "precedence" );
    }
    | userClasses
    {
        itemPermissionComponentsMonitor.useComponent( "userClasses" );
    }
    | grantsAndDenials
    {
        itemPermissionComponentsMonitor.useComponent( "grantsAndDenials" );
    }
    ;
    exception
    catch [IllegalArgumentException e]
    {
        throw new RecognitionException( e.getMessage() );
    }

grantsAndDenials
{
    log.debug( "entered grantsAndDenials()" );
    m_grantsAndDenials = new HashSet();
    GrantAndDenial l_grantAndDenial = null;
}
    :
    ID_grantsAndDenials ( SP )+
    OPEN_CURLY ( SP )*
        ( l_grantAndDenial = grantAndDenial ( SP )*
          {
              if ( !m_grantsAndDenials.add( l_grantAndDenial ))
              {
                  throw new RecognitionException( "Duplicated GrantAndDenial bit: " + l_grantAndDenial );
              }
          }
            ( SEP ( SP )* l_grantAndDenial = grantAndDenial ( SP )*
              {
                  if ( !m_grantsAndDenials.add( l_grantAndDenial ))
                  {
                      throw new RecognitionException( "Duplicated GrantAndDenial bit: " + l_grantAndDenial );
                  }
              }
            )*
        )?
    CLOSE_CURLY
    ;

grantAndDenial returns [ GrantAndDenial l_grantAndDenial ]
{
    log.debug( "entered grantAndDenialsBit()" );
    l_grantAndDenial = null;
}
    :
    ID_grantAdd { l_grantAndDenial = GrantAndDenial.GRANT_ADD; }
    | ID_denyAdd { l_grantAndDenial = GrantAndDenial.DENY_ADD; }
    | ID_grantDiscloseOnError { l_grantAndDenial = GrantAndDenial.GRANT_DISCLOSE_ON_ERROR; }
    | ID_denyDiscloseOnError { l_grantAndDenial = GrantAndDenial.DENY_DISCLOSE_ON_ERROR; }
    | ID_grantRead { l_grantAndDenial = GrantAndDenial.GRANT_READ; }
    | ID_denyRead { l_grantAndDenial = GrantAndDenial.DENY_READ; }
    | ID_grantRemove { l_grantAndDenial = GrantAndDenial.GRANT_REMOVE; }
    | ID_denyRemove { l_grantAndDenial = GrantAndDenial.DENY_REMOVE; }
    //-- permissions that may be used only in conjunction
    //-- with the entry component
    | ID_grantBrowse { l_grantAndDenial = GrantAndDenial.GRANT_BROWSE; }
    | ID_denyBrowse { l_grantAndDenial = GrantAndDenial.DENY_BROWSE; }
    | ID_grantExport { l_grantAndDenial = GrantAndDenial.GRANT_EXPORT; }
    | ID_denyExport { l_grantAndDenial = GrantAndDenial.DENY_EXPORT; }
    | ID_grantImport { l_grantAndDenial = GrantAndDenial.GRANT_IMPORT; }
    | ID_denyImport { l_grantAndDenial = GrantAndDenial.DENY_IMPORT; }
    | ID_grantModify { l_grantAndDenial = GrantAndDenial.GRANT_MODIFY; }
    | ID_denyModify { l_grantAndDenial = GrantAndDenial.DENY_MODIFY; }
    | ID_grantRename { l_grantAndDenial = GrantAndDenial.GRANT_RENAME; }
    | ID_denyRename { l_grantAndDenial = GrantAndDenial.DENY_RENAME; }
    | ID_grantReturnDN { l_grantAndDenial = GrantAndDenial.GRANT_RETURN_DN; }
    | ID_denyReturnDN { l_grantAndDenial = GrantAndDenial.DENY_RETURN_DN; }
    //-- permissions that may be used in conjunction
    //-- with any component, except entry, of ProtectedItems
    | ID_grantCompare { l_grantAndDenial = GrantAndDenial.GRANT_COMPARE; }
    | ID_denyCompare { l_grantAndDenial = GrantAndDenial.DENY_COMPARE; }
    | ID_grantFilterMatch { l_grantAndDenial = GrantAndDenial.GRANT_FILTER_MATCH; }
    | ID_denyFilterMatch { l_grantAndDenial = GrantAndDenial.DENY_FILTER_MATCH; }
    | ID_grantInvoke { l_grantAndDenial = GrantAndDenial.GRANT_INVOKE; }
    | ID_denyInvoke { l_grantAndDenial = GrantAndDenial.DENY_INVOKE; }
    ;

userClasses
{
    log.debug( "entered userClasses()" );
    m_userClassesMap = new NoDuplicateKeysMap();
}
    :
    ID_userClasses ( SP )+
    OPEN_CURLY ( SP )*
        (
            userClass ( SP )*
                ( SEP ( SP )* userClass ( SP )* )*
        )?
    CLOSE_CURLY
    {
        m_userClasses  = new HashSet( m_userClassesMap.values() );
    }
    ;
    exception
    catch [IllegalArgumentException e]
    {
        throw new RecognitionException( "User Classes cannot be duplicated. " + e.getMessage() );
    }

userClass
{
    log.debug( "entered userClasses()" );
}
    :
    allUsers
    | thisEntry 
    | name
    | userGroup
    | subtree
    ;

allUsers
{
    log.debug( "entered allUsers()" );
}
    :
    ID_allUsers
    {
        m_userClassesMap.put( "allUsers", UserClass.ALL_USERS );
    }
    ;

thisEntry
{
    log.debug( "entered thisEntry()" );
}
    :
    ID_thisEntry
    {
        m_userClassesMap.put( "thisEntry", UserClass.THIS_ENTRY );
    }
    ;

name
{
    log.debug( "entered name()" );
    Set l_name = new HashSet();
    LdapDN l_distinguishedName = null;
}
    :
    ID_name ( SP )+ 
        OPEN_CURLY ( SP )*
            l_distinguishedName=distinguishedName ( SP )*
            {
                l_name.add( l_distinguishedName );
            }
                ( SEP ( SP )* l_distinguishedName=distinguishedName ( SP )*
                {
                    l_name.add( l_distinguishedName );
                } )*
        CLOSE_CURLY
    {
        m_userClassesMap.put( "name", new UserClass.Name( l_name ) );
    }
    ;

userGroup
{
    log.debug( "entered userGroup()" );
    Set l_userGroup = new HashSet();
    LdapDN l_distinguishedName = null;
}
    :
    ID_userGroup ( SP )+ 
        OPEN_CURLY ( SP )*
            l_distinguishedName=distinguishedName ( SP )*
            {
                l_userGroup.add( l_distinguishedName );
            }
                ( SEP ( SP )* l_distinguishedName=distinguishedName ( SP )*
                {
                    l_userGroup.add( l_distinguishedName );
                } )*
        CLOSE_CURLY
    {
        m_userClassesMap.put( "userGroup", new UserClass.UserGroup( l_userGroup ) );
    }
    ;

subtree
{
    log.debug( "entered subtree()" );
    Set l_subtree = new HashSet();
    SubtreeSpecification l_subtreeSpecification = null;    
}
    :
    ID_subtree ( SP )+
        OPEN_CURLY ( SP )*
            l_subtreeSpecification=subtreeSpecification ( SP )*
            {
                l_subtree.add( l_subtreeSpecification );
            }
                ( SEP ( SP )* l_subtreeSpecification=subtreeSpecification ( SP )*
                {
                    l_subtree.add( l_subtreeSpecification );
                } )*
        CLOSE_CURLY
    {
        m_userClassesMap.put( "subtree", new UserClass.Subtree( l_subtree ) );
    }
    ;

userPermissions
{
    log.debug( "entered userPermissions()" );
    m_userPermissions = new HashSet();
    UserPermission l_userPermission = null;
}
    :
    ID_userPermissions ( SP )+
        OPEN_CURLY ( SP )*
            ( l_userPermission=userPermission ( SP )*
              {
                  m_userPermissions.add( l_userPermission );
              }
                ( SEP ( SP )* l_userPermission=userPermission ( SP )*
                  {
                      m_userPermissions.add( l_userPermission );
                  }
                )*
            )?
        CLOSE_CURLY
    ;

userPermission returns [ UserPermission l_userPermission ]
{
    log.debug( "entered userPermission()" );
    l_userPermission = null;
    userPermissionComponentsMonitor = new MandatoryAndOptionalComponentsMonitor( 
             new String [] { "protectedItems", "grantsAndDenials" }, new String [] { "precedence" } );
}
     :
     OPEN_CURLY ( SP )*
         anyUserPermission ( SP )*
             ( SEP ( SP )* anyUserPermission ( SP )* )*
     CLOSE_CURLY
     {
         if ( !userPermissionComponentsMonitor.finalStateValid() )
         {
             throw new RecognitionException( "Missing mandatory userPermission components: " 
                     + userPermissionComponentsMonitor.getRemainingComponents() );
         }
         
         l_userPermission = new UserPermission( m_aciPrecedence, m_grantsAndDenials, m_protectedItems );
     }
     ;

anyUserPermission
    :
    precedence
    {
        userPermissionComponentsMonitor.useComponent( "precedence" );
    }
    | protectedItems
    {
        userPermissionComponentsMonitor.useComponent( "protectedItems" );
    }
    | grantsAndDenials
    {
        userPermissionComponentsMonitor.useComponent( "grantsAndDenials" );
    }
    ;
    exception
    catch [IllegalArgumentException e]
    {
        throw new RecognitionException( e.getMessage() );
    }

subtreeSpecification returns [SubtreeSpecification ss]
{
    log.debug( "entered subtreeSpecification()" );
    // clear out ss, ssModifier, chopBeforeExclusions and chopAfterExclusions
    // in case something is left from the last parse
    ss = null;
    ssModifier = new SubtreeSpecificationModifier();
    chopBeforeExclusions = new HashSet();
    chopAfterExclusions = new HashSet();
    subtreeSpecificationComponentsMonitor = new OptionalComponentsMonitor( 
            new String [] { "base", "specificExclusions", "minimum", "maximum" } );
}
    :
    OPEN_CURLY ( SP )*
        ( subtreeSpecificationComponent ( SP )*
            ( SEP ( SP )* subtreeSpecificationComponent ( SP )* )* )?
    CLOSE_CURLY
    {
        ss = ssModifier.getSubtreeSpecification();
    }
    ;

subtreeSpecificationComponent
{
    log.debug( "entered subtreeSpecification()" );
}
    :
    ss_base
    {
        subtreeSpecificationComponentsMonitor.useComponent( "base" );
    }
    | ss_specificExclusions
    {
        subtreeSpecificationComponentsMonitor.useComponent( "specificExclusions" );
    }
    | ss_minimum
    {
        subtreeSpecificationComponentsMonitor.useComponent( "minimum" );
    }
    | ss_maximum
    {
        subtreeSpecificationComponentsMonitor.useComponent( "maximum" );
    }
    ;
    exception
    catch [IllegalArgumentException e]
    {
        throw new RecognitionException( e.getMessage() );
    }

ss_base
{
    log.debug( "entered ss_base()" );
    LdapDN base = null;
}
    :
    ID_base ( SP )+ base=distinguishedName
    {
        ssModifier.setBase( base );
    }
    ;

ss_specificExclusions
{
    log.debug( "entered ss_specificExclusions()" );
}
    :
    ID_specificExclusions ( SP )+ specificExclusions
    {
        ssModifier.setChopBeforeExclusions( chopBeforeExclusions );
        ssModifier.setChopAfterExclusions( chopAfterExclusions );
    }
    ;

specificExclusions
{
    log.debug( "entered specificExclusions()" );
}
    :
    OPEN_CURLY ( SP )*
        ( specificExclusion ( SP )*
            ( SEP ( SP )* specificExclusion ( SP )* )*
        )?
    CLOSE_CURLY
    ;

specificExclusion
{
    log.debug( "entered specificExclusion()" );
}
    :
    chopBefore | chopAfter
    ;

chopBefore
{
    log.debug( "entered chopBefore()" );
    LdapDN chopBeforeExclusion = null;
}
    :
    ID_chopBefore ( SP )* COLON ( SP )* chopBeforeExclusion=distinguishedName
    {
        chopBeforeExclusions.add( chopBeforeExclusion );
    }
    ;

chopAfter
{
    log.debug( "entered chopAfter()" );
    LdapDN chopAfterExclusion = null;
}
    :
    ID_chopAfter ( SP )* COLON ( SP )* chopAfterExclusion=distinguishedName
    {
        chopAfterExclusions.add( chopAfterExclusion );
    }
    ;

ss_minimum
{
    log.debug( "entered ss_minimum()" );
    int minimum = 0;
}
    :
    ID_minimum ( SP )+ minimum=baseDistance
    {
        ssModifier.setMinBaseDistance( minimum );
    }
    ;

ss_maximum
{
    log.debug( "entered ss_maximum()" );
    int maximum = 0;
}
    :
    ID_maximum ( SP )+ maximum=baseDistance
    {
        ssModifier.setMaxBaseDistance( maximum );
    }
    ;

distinguishedName returns [ LdapDN name ] 
{
    log.debug( "entered distinguishedName()" );
    name = null;
}
    :
    token:SAFEUTF8STRING
    {
        name = new LdapDN( token.getText() );
        name.normalize( oidsMap );
        log.debug( "recognized a DistinguishedName: " + token.getText() );
    }
    ;
    exception
    catch [Exception e]
    {
        throw new RecognitionException( "dnParser failed for " + token.getText() + " " + e.getMessage() );
    }

baseDistance returns [ int distance ]
{
    log.debug( "entered baseDistance()" );
    distance = 0;
}
    :
    token:INTEGER
    {
        distance = token2Integer( token );
    }
    ;

oid returns [ String result ]
{
    log.debug( "entered oid()" );
    result = null;
    Token token = null;
}
    :
    { token = LT( 1 ); } // an interesting trick goes here ;-)
    ( DESCR | NUMERICOID )
    {
        result = token.getText();
        log.debug( "recognized an oid: " + result );
    }
    ;

refinement returns [ ExprNode node ]
{
    log.debug( "entered refinement()" );
    node = null;
}
    :
    node=item | node=and | node=or | node=not
    ;

item returns [ LeafNode node ]
{
    log.debug( "entered item()" );
    node = null;
    String l_oid = null;
}
    :
    ID_item ( SP )* COLON ( SP )* l_oid=oid
    {
        node = new SimpleNode( "objectClass" , l_oid , AssertionEnum.EQUALITY );
    }
    ;

and returns [ BranchNode node ]
{
    log.debug( "entered and()" );
    node = null;
    ArrayList children = null; 
}
    :
    ID_and ( SP )* COLON ( SP )* children=refinements
    {
        node = new BranchNode( AssertionEnum.AND , children );
    }
    ;

or returns [ BranchNode node ]
{
    log.debug( "entered or()" );
    node = null;
    ArrayList children = null; 
}
    :
    ID_or ( SP )* COLON ( SP )* children=refinements
    {
        node = new BranchNode( AssertionEnum.OR , children );
    }
    ;

not returns [ BranchNode node ]
{
    log.debug( "entered not()" );
    node = null;
    ArrayList children = null;
}
    :
    ID_not ( SP )* COLON ( SP )* children=refinements
    {
        node = new BranchNode( AssertionEnum.NOT , children );
    }
    ;

refinements returns [ ArrayList children ]
{
    log.debug( "entered refinements()" );
    children = null;
    ExprNode child = null;
    ArrayList tempChildren = new ArrayList();
}
    :
    OPEN_CURLY ( SP )*
    (
        child=refinement ( SP )*
        {
            tempChildren.add( child );
        }
        ( SEP ( SP )* child=refinement ( SP )*
        {
            tempChildren.add( child );
        } )*
    )? CLOSE_CURLY
    {
        children = tempChildren;
    }
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
class AntlrACIItemLexer extends Lexer;


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

{
    private static final Logger log = LoggerFactory.getLogger( AntlrACIItemLexer.class );
}


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

