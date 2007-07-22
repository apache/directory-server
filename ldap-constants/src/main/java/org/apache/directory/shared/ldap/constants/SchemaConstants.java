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
public interface SchemaConstants
{
    // ---- ObjectClasses -----------------------------------------------------
    // Top
    String TOP_OC = "top";
    String TOP_OC_OID = "2.5.6.0";
    
    // Alias
    String ALIAS_OC = "alias";
    String ALIAS_OC_OID = "2.5.6.1";
    
    // Country
    String COUNTRY_OC = "country";
    String COUNTRY_OC_OID = "2.5.6.2";

    // Locality
    String LOCALITY_OC = "locality";
    String LOCALITY_OC_OID = "2.5.6.3";

    // Organization
    String ORGANIZATION_OC = "organization";
    String ORGANIZATION_OC_OID = "2.5.6.4";

    // OrganizationalUnit
    String ORGANIZATIONAL_UNIT_OC = "organizationalUnit";
    String ORGANIZATIONAL_UNIT_OC_OID = "2.5.6.5";

    // Person
    String PERSON_OC = "person";
    String PERSON_OC_OID = "2.5.6.6";
    
    // OrganizationalPerson
    String ORGANIZATIONAL_PERSON_OC = "organizationalPerson";
    String ORGANIZATIONAL_PERSON_OC_OID = "2.5.6.7";

    // OrganizationalRole
    String ORGANIZATIONAL_ROLE_OC = "organizationalRole";
    String ORGANIZATIONAL_ROLE_OC_OID = "2.5.6.8";

    // GroupOfNames
    String GROUP_OF_NAMES_OC = "groupOfNames";
    String GROUP_OF_NAMES_OC_OID = "2.5.6.9";
    
    // ResidentialPerson
    String RESIDENTIAL_PERSON_OC = "residentialPerson";
    String RESIDENTIAL_PERSON_OC_OID = "2.5.6.10";
    
    // GroupOfUniqueNames
    String GROUP_OF_UNIQUE_NAMES_OC = "groupOfUniqueNames";
    String GROUP_OF_UNIQUE_NAMES_OC_OID = "2.5.6.17";
    
    // Subentry
    String SUBENTRY_OC = "subentry";
    String SUBENTRY_OC_OID = "2.5.17.0";
    
    // AccessControlSubentry
    String ACCESS_CONTROL_SUBENTRY_OC = "accessControlSubentry";
    String ACCESS_CONTROL_SUBENTRY_OC_OID = "2.5.17.1";
    
    // Subschema
    String SUBSCHEMA_OC = "subschema";
    String SUBSCHEMA_OC_OID = "2.5.20.1";
    
    // InetOrgPerson
    String INET_ORG_PERSON_OC = "inetOrgPerson";
    String INET_ORG_PERSON_OC_OID = "2.16.840.1.113730.3.2.2";
    

    // ExtensibleObject
    String EXTENSIBLE_OBJECT_OC = "extensibleObject";
    String EXTENSIBLE_OBJECT_OC_OID = "1.3.6.1.4.1.1466.101.120.111";

    // ---- AttributeTypes ----------------------------------------------------
    // ObjectClass
    String OBJECT_CLASS_AT = "objectClass";
    String OBJECT_CLASS_AT_OID = "2.5.4.0";
    
    // AliasedObjectName
    String ALIASED_OBJECT_NAME_AT = "aliasedObjectName";
    String ALIASED_OBJECT_NAME_AT_OID = "2.5.4.1";
    
    // Cn
    String CN_AT = "cn";
    String COMMON_NAME_AT = "commonName";
    String CN_AT_OID = "2.5.4.3";
    
    // Sn
    String SN_AT = "sn";
    String SURNAME_AT = "surname";
    String SN_AT_OID = "2.5.4.4";
    
    // Ou
    String OU_AT = "ou";
    String ORGANIZATIONAL_UNIT_NAME_AT = "organizationalUnitName";
    String OU_AT_OID = "2.5.4.11";
    
    // Member
    String MEMBER_AT = "member";
    String MEMBER_AT_OID = "2.5.4.31";

    // UserPassword
    String USER_PASSWORD_AT = "userPassword";
    String USER_PASSWORD_AT_OID = "2.5.4.35";

    // UniqueMember
    String UNIQUE_MEMBER_AT = "uniqueMember";
    String UNIQUE_MEMBER_AT_OID = "2.5.4.50";

    // CreateTimestamp
    String CREATE_TIMESTAMP_AT = "createTimestamp";
    String CREATE_TIMESTAMP_AT_OID = "2.5.18.1";
    
    // ModifyTimestamp
    String MODIFY_TIMESTAMP_AT = "modifyTimestamp";
    String MODIFY_TIMESTAMP_AT_OID = "2.5.18.2";
    
    // CreatorsName
    String CREATORS_NAME_AT = "creatorsName";
    String CREATORS_NAME_AT_OID = "2.5.18.3";
    
    // ModifiersName
    String MODIFIERS_NAME_AT = "modifiersName";
    String MODIFIERS_NAME_AT_OID = "2.5.18.4";
    
    // SubtreeSpecification
    String SUBTREE_SPECIFICATION_AT = "subtreeSpecification";
    String SUBTREE_SPECIFICATION_AT_OID = "2.5.18.6";

    // SubschemaSubentry
    String SUBSCHEMA_SUBENTRY_AT = "subschemaSubentry";
    String SUBSCHEMA_SUBENTRY_AT_OID = "2.5.18.10";

    // CollectiveAttributeSubentries
    String COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT = "collectiveAttributeSubentries";
    String COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT_OID = "2.5.18.12";

    // DitStructureRules
    String DIT_STRUCTURE_RULES_AT = "ditStructureRules";
    String DIT_STRUCTURE_RULES_AT_OID = "2.5.21.1";
    
    // DitContentRules
    String DIT_CONTENT_RULES_AT = "ditContentRules";
    String DIT_CONTENT_RULES_AT_OID = "2.5.21.2";
    
    // MatchingRules
    String MATCHING_RULES_AT = "matchingRules";
    String MATCHING_RULES_AT_OID = "2.5.21.4";
    
    // AttributeTypes
    String ATTRIBUTE_TYPES_AT = "attributeTypes";
    String ATTRIBUTE_TYPES_AT_OID = "2.5.21.5";
    
    // ObjectClasses
    String OBJECT_CLASSES_AT = "objectClasses";
    String OBJECT_CLASSES_AT_OID = "2.5.21.6";

    // NameForms
    String NAME_FORMS_AT = "nameForms";
    String NAME_FORMS_AT_OID = "2.5.21.7";

    // MatchingRuleUse
    String MATCHING_RULE_USE_AT = "matchingRuleUse";
    String MATCHING_RULE_USE_AT_OID = "2.5.21.8";
    
    // StructuralObjectClass
    String STRUCTURAL_OBJECT_CLASS_AT = "structuralObjectClass";
    String STRUCTURAL_OBJECT_CLASS_AT_OID = "2.5.21.9";
    
    // AccessControlScheme
    String ACCESS_CONTROL_SCHEME_AT = "accessControlScheme";
    String ACCESS_CONTROL_SCHEME_OID = "2.5.24.1";
    
    // PrescriptiveACI
    String PRESCRIPTIVE_ACI_AT = "prescriptiveACI";
    String PRESCRIPTIVE_ACI_AT_OID = "2.5.24.4";
    
    // EntryACI
    String ENTRY_ACI_AT = "entryACI";
    String ENTRY_ACI_AT_OID = "2.5.24.5";
    
    // SubentryACI
    String SUBENTRY_ACI_AT = "subentryACI";
    String SUBENTRY_ACI_AT_OID = "2.5.24.6";
    
    // Uid
    String UID_AT = "uid";
    String USER_ID_AT = "userid";
    String UID_AT_OID = "0.9.2342.19200300.100.1.1";

    // UidObject
    String UID_OBJECT_AT = "uidObject";
    String UID_OBJECT_AT_OID = "1.3.6.1.1.3.1";

    // LdapSyntaxes
    String LDAP_SYNTAXES_AT = "ldapSyntaxes";
    String LDAP_SYNTAXES_AT_OID = "1.3.6.1.4.1.1466.101.120.16";

    // AccessControlSubentries
    String ACCESS_CONTROL_SUBENTRIES_AT = "accessControlSubentries";
    String ACCESS_CONTROL_SUBENTRIES_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.11";

    // TriggerExecutionSubentries
    String TRIGGER_EXECUTION_SUBENTRIES_AT = "triggerExecutionSubentries";
    String TRIGGER_EXECUTION_SUBENTRIES_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.27";

    // Comparators
    String COMPARATORS_AT = "comparators";
    String COMPARATORS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.32";
    
    // Normalizers
    String NORMALIZERS_AT = "normalizers";
    String NORMALIZERS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.33";

    // SyntaxCheckers
    String SYNTAX_CHECKERS_AT = "syntaxCheckers";
    String SYNTAX_CHECKERS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.34";

    // DisplayName
    String DISPLAY_NAME_AT = "displayName";
    String DISPLAY_NAME_AT_OID = "2.16.840.1.113730.3.1.241";

    // ---- Syntaxes -----------------------------------------------------

    String BINARY_SYNTAX = "1.3.6.1.4.1.1466.115.121.1.5";
    String BIT_STRING_SYNTAX = "1.3.6.1.4.1.1466.115.121.1.6";
    String BOOLEAN_SYNTAX = "1.3.6.1.4.1.1466.115.121.1.7";
    String GENERALIZED_TIME_SYNTAX = "1.3.6.1.4.1.1466.115.121.1.24";
    String INTEGER_SYNTAX = "1.3.6.1.4.1.1466.115.121.1.27";
    String UTC_TIME_SYNTAX = "1.3.6.1.4.1.1466.115.121.1.53";
    String DIRECTORY_STRING_SYNTAX = "1.3.6.1.4.1.1466.115.121.1.15";

    String JAVA_BYTE_SYNTAX = "1.3.6.1.4.1.18060.0.4.1.0.0";
    String JAVA_CHAR_SYNTAX = "1.3.6.1.4.1.18060.0.4.1.0.1";
    String JAVA_SHORT_SYNTAX = "1.3.6.1.4.1.18060.0.4.1.0.2";
    String JAVA_LONG_SYNTAX = "1.3.6.1.4.1.18060.0.4.1.0.3";
    String JAVA_INT_SYNTAX = "1.3.6.1.4.1.18060.0.4.1.0.4";

    // ---- MatchingRules -----------------------------------------------------

    String CASE_IGNORE_MR = "caseIgnoreMatch";
    String CASE_IGNORE_SUBSTRING_MR = "caseIgnoreSubstringsMatch";
    String CASE_EXACT_MR = "caseExactMatch";
    String CASE_EXACT_SUBSTRING_MR = "caseExactSubstringsMatch";
    String INTEGER_MR = "integerMatch";
    String INTEGER_ORDERING_MR = "integerOrderingMatch";
    String BOOLEAN_MR = "booleanMatch";
    String BIT_STRING_MR = "bitStringMatch";
    String GENERALIZED_TIME_MR = "generalizedTimeMatch";
    String GENERALIZED_TIME_ORDERING_MR = "generalizedTimeOrderingMatch";
}
