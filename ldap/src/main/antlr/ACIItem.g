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


import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;

import javax.naming.directory.Attribute;
import javax.naming.Name;

import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.message.AttributeImpl;
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
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.OidNormalizer;

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
    
    NameComponentNormalizer normalizer;
    
    // nonshared global data needed to avoid extensive pass/return stuff
    // these are only used by three first order components
    private String identificationTag;
    private AuthenticationLevel authenticationLevel;
    private int aciPrecedence;
    
    private boolean isItemFirstACIItem;
    
    // shared global data needed to avoid extensive pass/return stuff
    private Set<ProtectedItem> protectedItems;
    private Map<String, ProtectedItem> protectedItemsMap;
    private Set<UserClass> userClasses;
    private Map<String, UserClass> userClassesMap;
    private Set<ItemPermission> itemPermissions;
    private int precedence;
    private Set<GrantAndDenial> grantsAndDenials;
    private Set<UserPermission> userPermissions;
    private Map<String, OidNormalizer> oidsMap;
    
    private Set<Name> chopBeforeExclusions;
    private Set<Name> chopAfterExclusions;
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
    public void init( Map<String, OidNormalizer> oidsMap )
    {
    	this.oidsMap = oidsMap;
    }

    /**
     * Sets the NameComponentNormalizer for this parser's dnParser.
     */
    public void setNormalizer(NameComponentNormalizer normalizer)
    {
        this.normalizer = normalizer;
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

theACIItem returns [ ACIItem ACIItem ]
{
    log.debug( "entered theACIItem()" );
    ACIItem = null;
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
            ACIItem = new ItemFirstACIItem(
                    identificationTag,
                    precedence,
                    authenticationLevel,
                    protectedItems,
                    itemPermissions );
        }
        else
        {
            ACIItem = new UserFirstACIItem(
                    identificationTag,
                    aciPrecedence,
                    authenticationLevel,
                    userClasses,
                    userPermissions );
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
        identificationTag = token.getText();
    }
    ;

aci_precedence
{
    log.debug( "entered aci_precedence()" );
}
    :
    precedence
    {
        aciPrecedence = precedence;
    }
    ;

precedence
{
    log.debug( "entered precedence()" );
}
    :
    ID_precedence ( SP )+ token:INTEGER
    {
        precedence = token2Integer( token );
        
        if ( ( precedence < 0 ) || ( precedence > 255 ) )
        {
            throw new RecognitionException( "Expecting INTEGER token having an Integer value between 0 and 255, found " + precedence );
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
        authenticationLevel = AuthenticationLevel.NONE;
    }
    |
    ID_simple
    {
        authenticationLevel = AuthenticationLevel.SIMPLE;
    }
    |
    ID_strong
    {
        authenticationLevel = AuthenticationLevel.STRONG;
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
    protectedItemsMap = new NoDuplicateKeysMap();
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
        protectedItems = new HashSet<ProtectedItem>( protectedItemsMap.values() );
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
        protectedItemsMap.put( "entry", ProtectedItem.ENTRY );
    }
    ;

allUserAttributeTypes
{
    log.debug( "entered allUserAttributeTypes()" );
}
    :
    ID_allUserAttributeTypes
    {
        protectedItemsMap.put( "allUserAttributeTypes", ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );
    }
    ;

attributeType
{
    log.debug( "entered attributeType()" );
    Set<String> attributeTypeSet = null;
}
    :
    ID_attributeType ( SP )+ attributeTypeSet=attributeTypeSet
    {
        protectedItemsMap.put( "attributeType", new ProtectedItem.AttributeType(attributeTypeSet ) );
    }
    ;

allAttributeValues
{
    log.debug( "entered allAttributeValues()" );
    Set<String> attributeTypeSet = null;
}
    :
    ID_allAttributeValues ( SP )+ attributeTypeSet=attributeTypeSet
    {
        protectedItemsMap.put( "allAttributeValues", new ProtectedItem.AllAttributeValues( attributeTypeSet ) );
    }
    ;

allUserAttributeTypesAndValues
{
    log.debug( "entered allUserAttributeTypesAndValues()" );
}
    :
    ID_allUserAttributeTypesAndValues
    {
        protectedItemsMap.put( "allUserAttributeTypesAndValues", ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );
    }
    ;

attributeValue
{
    log.debug( "entered attributeValue()" );
    String attributeTypeAndValue = null;
    String attributeType = null;
    String attributeValue = null;
    Set<Attribute> attributeSet = new HashSet<Attribute>();
}
    :
    token:ATTRIBUTE_VALUE_CANDIDATE // ate the identifier for subordinate dn parser workaround
    {
        // A Dn can be considered as a set of attributeTypeAndValues
        // So, parse the set as a Dn and extract each attributeTypeAndValue
        LdapDN attributeTypeAndValueSetAsDn = new LdapDN( token.getText() );
        
        if ( oidsMap != null )
        {        
            attributeTypeAndValueSetAsDn.normalize( oidsMap );
        }
        
        Enumeration attributeTypeAndValueSet = attributeTypeAndValueSetAsDn.getAll();
        
        while ( attributeTypeAndValueSet.hasMoreElements() )
        {
            attributeTypeAndValue = ( String ) attributeTypeAndValueSet.nextElement();
            attributeType = NamespaceTools.getRdnAttribute( attributeTypeAndValue );
            attributeValue = NamespaceTools.getRdnValue( attributeTypeAndValue );
            attributeSet.add( new AttributeImpl( attributeType, attributeValue ) );
            log.debug( "An attributeTypeAndValue from the set: " + attributeType + "=" +  attributeValue);
        }
        
        protectedItemsMap.put( "attributeValue", new ProtectedItem.AttributeValue( attributeSet ) );
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
    Set<String> attributeTypeSet = null;
}
    :
    ID_selfValue ( SP )+ attributeTypeSet=attributeTypeSet
    {
        protectedItemsMap.put( "sefValue", new ProtectedItem.SelfValue( attributeTypeSet ) );
    }
    ;

rangeOfValues
{
    log.debug( "entered rangeOfValues()" );
}
    :
    token:RANGE_OF_VALUES_CANDIDATE
    {
        protectedItemsMap.put( "rangeOfValues",
                new ProtectedItem.RangeOfValues(
                        FilterParser.parse( token.getText() ) ) );
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
    ProtectedItem.MaxValueCountItem maxValueCount = null;
    Set<ProtectedItem.MaxValueCountItem> maxValueCountSet = new HashSet<ProtectedItem.MaxValueCountItem>();
}
    :
    ID_maxValueCount ( SP )+
    OPEN_CURLY ( SP )*
        maxValueCount=aMaxValueCount ( SP )*
        {
            maxValueCountSet.add( maxValueCount );
        }
            ( SEP ( SP )* maxValueCount=aMaxValueCount ( SP )*
            {
                maxValueCountSet.add( maxValueCount );
            }
            )*
    CLOSE_CURLY
    {
        protectedItemsMap.put( "maxValueCount", new ProtectedItem.MaxValueCount( maxValueCountSet ) );
    }
    ;

aMaxValueCount returns [ ProtectedItem.MaxValueCountItem maxValueCount ]
{
    log.debug( "entered aMaxValueCount()" );
    maxValueCount = null;
    String oid = null;
    Token token = null;
}
    :
    OPEN_CURLY ( SP )*
        (
          ID_type ( SP )+ oid=oid ( SP )* SEP ( SP )*
          ID_maxCount ( SP )+ token1:INTEGER
          { token = token1; }
        | // relaxing
          ID_maxCount ( SP )+ token2:INTEGER ( SP )* SEP ( SP )*
          ID_type ( SP )+ oid=oid
          { token = token2; }
        )
    ( SP )* CLOSE_CURLY
    {
        maxValueCount = new ProtectedItem.MaxValueCountItem( oid, token2Integer( token ) );
    }
    ;

maxImmSub
{
    log.debug( "entered maxImmSub()" );
}
    :
    ID_maxImmSub ( SP )+ token:INTEGER
    {
        
        protectedItemsMap.put( "maxImmSub",
                new ProtectedItem.MaxImmSub(
                        token2Integer( token ) ) );
    }
    ;

restrictedBy
{
    log.debug( "entered restrictedBy()" );
    ProtectedItem.RestrictedByItem restrictedValue = null;
    Set<ProtectedItem.RestrictedByItem> restrictedBy = new HashSet<ProtectedItem.RestrictedByItem>();
}
    :
    ID_restrictedBy ( SP )+
        OPEN_CURLY ( SP )*
            restrictedValue=restrictedValue ( SP )*
            {
                restrictedBy.add( restrictedValue );
            }
                    ( SEP ( SP )* restrictedValue=restrictedValue ( SP )*
                    {
                        restrictedBy.add( restrictedValue );
                    }
                    )*
        CLOSE_CURLY
    {
        protectedItemsMap.put( "restrictedBy", new ProtectedItem.RestrictedBy( restrictedBy ) );
    }
    ;

restrictedValue returns [ ProtectedItem.RestrictedByItem restrictedValue ]
{
    log.debug( "entered restrictedValue()" );
    String typeOid = null;
    String valuesInOid = null;
    restrictedValue = null;
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
        restrictedValue = new ProtectedItem.RestrictedByItem( typeOid, valuesInOid );
    }
    ;

attributeTypeSet returns [ Set<String> attributeTypeSet ]
{
    log.debug( "entered attributeTypeSet()" );
    String oid = null;
    attributeTypeSet = new HashSet<String>();
}
    :
    OPEN_CURLY ( SP )*
        oid=oid ( SP )*
        {
            attributeTypeSet.add( oid );
        }
            ( SEP ( SP )* oid=oid ( SP )*
            {
                attributeTypeSet.add( oid );
            }
            )*
    CLOSE_CURLY
    ;

classes
{
    log.debug( "entered classes()" );
    ExprNode classes = null;
}
    :
    ID_classes ( SP )+ classes=refinement
    {
        protectedItemsMap.put( "classes", new ProtectedItem.Classes( classes ) );
    }
    ;

itemPermissions
{
    log.debug( "entered itemPermissions()" );
    itemPermissions = new HashSet<ItemPermission>();
    ItemPermission itemPermission = null;
}
    :
    ID_itemPermissions ( SP )+
        OPEN_CURLY ( SP )*
            ( itemPermission=itemPermission ( SP )*
              {
                  itemPermissions.add( itemPermission );
              }
                ( SEP ( SP )* itemPermission=itemPermission ( SP )*
                  {
                      itemPermissions.add( itemPermission );
                  }
                )*
            )?
        CLOSE_CURLY
    ;

itemPermission returns [ ItemPermission itemPermission ]
{
    log.debug( "entered itemPermission()" );
    itemPermission = null;
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
        
        itemPermission = new ItemPermission( precedence, grantsAndDenials, userClasses );
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
    grantsAndDenials = new HashSet<GrantAndDenial>();
    GrantAndDenial grantAndDenial = null;
}
    :
    ID_grantsAndDenials ( SP )+
    OPEN_CURLY ( SP )*
        ( grantAndDenial = grantAndDenial ( SP )*
          {
              if ( !grantsAndDenials.add( grantAndDenial ))
              {
                  throw new RecognitionException( "Duplicated GrantAndDenial bit: " + grantAndDenial );
              }
          }
            ( SEP ( SP )* grantAndDenial = grantAndDenial ( SP )*
              {
                  if ( !grantsAndDenials.add( grantAndDenial ))
                  {
                      throw new RecognitionException( "Duplicated GrantAndDenial bit: " + grantAndDenial );
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
    userClassesMap = new NoDuplicateKeysMap();
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
        userClasses  = new HashSet<UserClass>( userClassesMap.values() );
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
        userClassesMap.put( "allUsers", UserClass.ALL_USERS );
    }
    ;

thisEntry
{
    log.debug( "entered thisEntry()" );
}
    :
    ID_thisEntry
    {
        userClassesMap.put( "thisEntry", UserClass.THIS_ENTRY );
    }
    ;

name
{
    log.debug( "entered name()" );
    Set<Name> names = new HashSet<Name>();
    LdapDN distinguishedName = null;
}
    :
    ID_name ( SP )+ 
        OPEN_CURLY ( SP )*
            distinguishedName=distinguishedName ( SP )*
            {
                names.add( distinguishedName );
            }
                ( SEP ( SP )* distinguishedName=distinguishedName ( SP )*
                {
                    names.add( distinguishedName );
                } )*
        CLOSE_CURLY
    {
        userClassesMap.put( "name", new UserClass.Name( names ) );
    }
    ;

userGroup
{
    log.debug( "entered userGroup()" );
    Set<Name> userGroup = new HashSet<Name>();
    LdapDN distinguishedName = null;
}
    :
    ID_userGroup ( SP )+ 
        OPEN_CURLY ( SP )*
            distinguishedName=distinguishedName ( SP )*
            {
                userGroup.add( distinguishedName );
            }
                ( SEP ( SP )* distinguishedName=distinguishedName ( SP )*
                {
                    userGroup.add( distinguishedName );
                } )*
        CLOSE_CURLY
    {
        userClassesMap.put( "userGroup", new UserClass.UserGroup( userGroup ) );
    }
    ;

subtree
{
    log.debug( "entered subtree()" );
    Set<SubtreeSpecification> subtrees = new HashSet<SubtreeSpecification>();
    SubtreeSpecification subtreeSpecification = null;    
}
    :
    ID_subtree ( SP )+
        OPEN_CURLY ( SP )*
            subtreeSpecification=subtreeSpecification ( SP )*
            {
                subtrees.add( subtreeSpecification );
            }
                ( SEP ( SP )* subtreeSpecification=subtreeSpecification ( SP )*
                {
                    subtrees.add( subtreeSpecification );
                } )*
        CLOSE_CURLY
    {
        userClassesMap.put( "subtree", new UserClass.Subtree( subtrees ) );
    }
    ;

userPermissions
{
    log.debug( "entered userPermissions()" );
    userPermissions = new HashSet<UserPermission>();
    UserPermission userPermission = null;
}
    :
    ID_userPermissions ( SP )+
        OPEN_CURLY ( SP )*
            ( userPermission=userPermission ( SP )*
              {
                  userPermissions.add( userPermission );
              }
                ( SEP ( SP )* userPermission=userPermission ( SP )*
                  {
                      userPermissions.add( userPermission );
                  }
                )*
            )?
        CLOSE_CURLY
    ;

userPermission returns [ UserPermission userPermission ]
{
    log.debug( "entered userPermission()" );
    userPermission = null;
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
         
         userPermission = new UserPermission( aciPrecedence, grantsAndDenials, protectedItems );
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
    chopBeforeExclusions = new HashSet<Name>();
    chopAfterExclusions = new HashSet<Name>();
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
        if ( oidsMap != null )
        {
            name.normalize( oidsMap );
        }
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
    String oid = null;
}
    :
    ID_item ( SP )* COLON ( SP )* oid=oid
    {
        node = new EqualityNode( SchemaConstants.OBJECT_CLASS_AT , oid );
    }
    ;

and returns [ BranchNode node ]
{
    log.debug( "entered and()" );
    node = null;
    List<ExprNode> children = null; 
}
    :
    ID_and ( SP )* COLON ( SP )* children=refinements
    {
        node = new AndNode( children );
    }
    ;

or returns [ BranchNode node ]
{
    log.debug( "entered or()" );
    node = null;
    List<ExprNode> children = null; 
}
    :
    ID_or ( SP )* COLON ( SP )* children=refinements
    {
        node = new OrNode( children );
    }
    ;

not returns [ BranchNode node ]
{
    log.debug( "entered not()" );
    node = null;
    List<ExprNode> children = null;
}
    :
    ID_not ( SP )* COLON ( SP )* children=refinements
    {
        node = new NotNode( children );
    }
    ;

refinements returns [ List<ExprNode> children ]
{
    log.debug( "entered refinements()" );
    children = null;
    ExprNode child = null;
    List<ExprNode> tempChildren = new ArrayList<ExprNode>();
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

