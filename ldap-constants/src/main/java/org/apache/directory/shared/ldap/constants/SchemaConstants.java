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
package org.apache.directory.shared.ldap.constants;


/**
 * A utility class where we declare all the schema objects being used by any
 * ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class SchemaConstants
{
    // ---- ObjectClasses -----------------------------------------------------
    // Top
    public static final String TOP_OC = "top";
    public static final String TOP_OC_OID = "2.5.6.0";
    
    // Alias
    public static final String ALIAS_OC = "alias";
    public static final String ALIAS_OC_OID = "2.5.6.1";
    
    // Country
    public static final String COUNTRY_OC = "country";
    public static final String COUNTRY_OC_OID = "2.5.6.2";

    // Locality
    public static final String LOCALITY_OC = "locality";
    public static final String LOCALITY_OC_OID = "2.5.6.3";

    // Organization
    public static final String ORGANIZATION_OC = "organization";
    public static final String ORGANIZATION_OC_OID = "2.5.6.4";

    // OrganizationalUnit
    public static final String ORGANIZATIONAL_UNIT_OC = "organizationalUnit";
    public static final String ORGANIZATIONAL_UNIT_OC_OID = "2.5.6.5";

    // Person
    public static final String PERSON_OC = "person";
    public static final String PERSON_OC_OID = "2.5.6.6";
    
    // OrganizationalPerson
    public static final String ORGANIZATIONAL_PERSON_OC = "organizationalPerson";
    public static final String ORGANIZATIONAL_PERSON_OC_OID = "2.5.6.7";

    // OrganizationalRole
    public static final String ORGANIZATIONAL_ROLE_OC = "organizationalRole";
    public static final String ORGANIZATIONAL_ROLE_OC_OID = "2.5.6.8";

    // GroupOfNames
    public static final String GROUP_OF_NAMES_OC = "groupOfNames";
    public static final String GROUP_OF_NAMES_OC_OID = "2.5.6.9";
    
    // ResidentialPerson
    public static final String RESIDENTIAL_PERSON_OC = "residentialPerson";
    public static final String RESIDENTIAL_PERSON_OC_OID = "2.5.6.10";
    
    // GroupOfUniqueNames
    public static final String GROUP_OF_UNIQUE_NAMES_OC = "groupOfUniqueNames";
    public static final String GROUP_OF_UNIQUE_NAMES_OC_OID = "2.5.6.17";
    
    // Subentry
    public static final String SUBENTRY_OC = "subentry";
    public static final String SUBENTRY_OC_OID = "2.5.17.0";
    
    // AccessControlSubentry
    public static final String ACCESS_CONTROL_SUBENTRY_OC = "accessControlSubentry";    
    public static final String ACCESS_CONTROL_SUBENTRY_OC_OID = "2.5.17.1";    
    
    // Subschema
    public static final String SUBSCHEMA_OC = "subschema";
    public static final String SUBSCHEMA_OC_OID = "2.5.20.1";
    
    // InetOrgPerson
    public static final String INET_ORG_PERSON_OC = "inetOrgPerson";
    public static final String INET_ORG_PERSON_OC_OID = "2.16.840.1.113730.3.2.2";
    

    // ExtensibleObject
    public static final String EXTENSIBLE_OBJECT_OC = "extensibleObject";
    public static final String EXTENSIBLE_OBJECT_OC_OID = "1.3.6.1.4.1.1466.101.120.111";

    // ---- AttributeTypes ----------------------------------------------------
    // ObjectClass
    public static final String OBJECT_CLASS_AT = "objectClass";
    public static final String OBJECT_CLASS_AT_OID = "2.5.4.0";
    
    // AliasedObjectName
    public static final String ALIASED_OBJECT_NAME_AT = "aliasedObjectName";
    public static final String ALIASED_OBJECT_NAME_AT_OID = "2.5.4.1";
    
    // Cn
    public static final String CN_AT = "cn";
    public static final String COMMON_NAME_AT = "commonName";
    public static final String CN_AT_OID = "2.5.4.3";
    
    // Sn
    public static final String SN_AT = "sn";
    public static final String SURNAME_AT = "surname";
    public static final String SN_AT_OID = "2.5.4.4";
    
    // Ou
    public static final String OU_AT = "ou";
    public static final String ORGANIZATIONAL_UNIT_NAME_AT = "organizationalUnitName";
    public static final String OU_AT_OID = "2.5.4.11";
    
    // Member
    public static final String MEMBER_AT = "member";
    public static final String MEMBER_AT_OID = "2.5.4.31";

    // UserPassword
    public static final String USER_PASSWORD_AT = "userPassword";
    public static final String USER_PASSWORD_AT_OID = "2.5.4.35";

    // UniqueMember
    public static final String UNIQUE_MEMBER_AT = "uniqueMember";
    public static final String UNIQUE_MEMBER_AT_OID = "2.5.4.50";

    // CreateTimestamp
    public static final String CREATE_TIMESTAMP_AT = "createTimestamp";
    public static final String CREATE_TIMESTAMP_AT_OID = "2.5.18.1";
    
    // ModifyTimestamp
    public static final String MODIFY_TIMESTAMP_AT = "modifyTimestamp";
    public static final String MODIFY_TIMESTAMP_AT_OID = "2.5.18.2";
    
    // CreatorsName
    public static final String CREATORS_NAME_AT = "creatorsName";
    public static final String CREATORS_NAME_AT_OID = "2.5.18.3";
    
    // ModifiersName
    public static final String MODIFIERS_NAME_AT = "modifiersName";
    public static final String MODIFIERS_NAME_AT_OID = "2.5.18.4";
    
    // SubtreeSpecification
    public static final String SUBTREE_SPECIFICATION_AT = "subtreeSpecification";
    public static final String SUBTREE_SPECIFICATION_AT_OID = "2.5.18.6";

    // SubschemaSubentry
    public static final String SUBSCHEMA_SUBENTRY_AT = "subschemaSubentry";
    public static final String SUBSCHEMA_SUBENTRY_AT_OID = "2.5.18.10";

    // CollectiveAttributeSubentries
    public static final String COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT = "collectiveAttributeSubentries";
    public static final String COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT_OID = "2.5.18.12";

    // DitStructureRules
    public static final String DIT_STRUCTURE_RULES_AT = "ditStructureRules";
    public static final String DIT_STRUCTURE_RULES_AT_OID = "2.5.21.1";
    
    // DitContentRules
    public static final String DIT_CONTENT_RULES_AT = "ditContentRules";
    public static final String DIT_CONTENT_RULES_AT_OID = "2.5.21.2";
    
    // MatchingRules
    public static final String MATCHING_RULES_AT = "matchingRules";
    public static final String MATCHING_RULES_AT_OID = "2.5.21.4";
    
    // AttributeTypes
    public static final String ATTRIBUTE_TYPES_AT = "attributeTypes";
    public static final String ATTRIBUTE_TYPES_AT_OID = "2.5.21.5";
    
    // ObjectClasses
    public static final String OBJECT_CLASSES_AT = "objectClasses";
    public static final String OBJECT_CLASSES_AT_OID = "2.5.21.6";

    // NameForms
    public static final String NAME_FORMS_AT = "nameForms";
    public static final String NAME_FORMS_AT_OID = "2.5.21.7";

    // MatchingRuleUse
    public static final String MATCHING_RULE_USE_AT = "matchingRuleUse";
    public static final String MATCHING_RULE_USE_AT_OID = "2.5.21.8";
    
    // StructuralObjectClass
    public static final String STRUCTURAL_OBJECT_CLASS_AT = "structuralObjectClass";
    public static final String STRUCTURAL_OBJECT_CLASS_AT_OID = "2.5.21.9";
    
    // AccessControlScheme
    public static final String ACCESS_CONTROL_SCHEME_AT = "accessControlScheme";
    public static final String ACCESS_CONTROL_SCHEME_OID = "2.5.24.1";
    
    // PrescriptiveACI
    public static final String PRESCRIPTIVE_ACI_AT = "prescriptiveACI";
    public static final String PRESCRIPTIVE_ACI_AT_OID = "2.5.24.4";
    
    // EntryACI
    public static final String ENTRY_ACI_AT = "entryACI";
    public static final String ENTRY_ACI_AT_OID = "2.5.24.5";
    
    // SubentryACI
    public static final String SUBENTRY_ACI_AT = "subentryACI";
    public static final String SUBENTRY_ACI_AT_OID = "2.5.24.6";
    
    // Uid
    public static final String UID_AT = "uid";
    public static final String USER_ID_AT = "userid";
    public static final String UID_AT_OID = "0.9.2342.19200300.100.1.1";

    // UidObject
    public static final String UID_OBJECT_AT = "uidObject";
    public static final String UID_OBJECT_AT_OID = "1.3.6.1.1.3.1";

    // LdapSyntaxes
    public static final String LDAP_SYNTAXES_AT = "ldapSyntaxes";
    public static final String LDAP_SYNTAXES_AT_OID = "1.3.6.1.4.1.1466.101.120.16";

    // AccessControlSubentries
    public static final String ACCESS_CONTROL_SUBENTRIES_AT = "accessControlSubentries";
    public static final String ACCESS_CONTROL_SUBENTRIES_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.11";

    // TriggerExecutionSubentries
    public static final String TRIGGER_EXECUTION_SUBENTRIES_AT = "triggerExecutionSubentries";
    public static final String TRIGGER_EXECUTION_SUBENTRIES_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.27";

    // Comparators
    public static final String COMPARATORS_AT = "comparators";
    public static final String COMPARATORS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.32";
    
    // Normalizers
    public static final String NORMALIZERS_AT = "normalizers";
    public static final String NORMALIZERS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.33";

    // SyntaxCheckers
    public static final String SYNTAX_CHECKERS_AT = "syntaxCheckers";
    public static final String SYNTAX_CHECKERS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.34";

    // DisplayName
    public static final String DISPLAY_NAME_AT = "displayName";
    public static final String DISPLAY_NAME_AT_OID = "2.16.840.1.113730.3.1.241";
}
