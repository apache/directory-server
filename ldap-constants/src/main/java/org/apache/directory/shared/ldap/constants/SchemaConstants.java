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
    // Special attributes 1.1 , * and + for search operations
    String NO_ATTRIBUTE = "1.1";
    String[] NO_ATTRIBUTE_ARRAY = new String[]{ NO_ATTRIBUTE };
    
    String ALL_USER_ATTRIBUTES = "*";
    String[] ALL_USER_ATTRIBUTES_ARRAY = new String[]{ ALL_USER_ATTRIBUTES };
    
    String ALL_OPERATIONAL_ATTRIBUTES = "+";
    String[] ALL_OPERATIONAL_ATTRIBUTES_ARRAY = new String[]{ ALL_OPERATIONAL_ATTRIBUTES };
    
    // ---- ObjectClasses -----------------------------------------------------
    // Krb5Principal
    String KRB5_PRINCIPAL_OC = "krb5Principal";
    String KRB5_PRINCIPAL_OC_OID = "1.3.6.1.4.1.5322.10.2.1";
    
    // Top
    String TOP_OC = "top";
    String TOP_OC_OID = "2.5.6.0";
    
    // Alias
    String ALIAS_OC = "alias";
    String ALIAS_OC_OID = "2.5.6.1";

    // Country
    String COUNTRY_OC = "country";
    String COUNTRY_OC_OID = "2.5.6.2";

    // Domain
    String DOMAIN_OC = "domain";
    String DOMAIN_OC_OID = "0.9.2342.19200300.100.4.13";

    // DcObject
    String DC_OBJECT_OC = "dcObject";
    String DC_OBJECT_OC_OID = "1.3.6.1.4.1.1466.344";

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

    // PosixAccount
    String POSIX_ACCOUNT_OC = "posicAccount";
    String POSIX_ACCOUNT_OC_OID = "1.3.6.1.1.1.2.0";

    // PosixGroup
    String POSIX_GROUP_OC = "posixGroup";
    String POSIX_GROUP_OC_OID = "1.3.6.1.1.1.2.2";
    
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
    
    // CollectiveAttributeSubentry
    String COLLECTIVE_ATTRIBUTE_SUBENTRY_OC = "collectiveAttributeSubentry";
    String COLLECTIVE_ATTRIBUTE_SUBENTRY_OC_OID = "2.5.17.2";

    // Subschema
    String SUBSCHEMA_OC = "subschema";
    String SUBSCHEMA_OC_OID = "2.5.20.1";
    
    // InetOrgPerson
    String INET_ORG_PERSON_OC = "inetOrgPerson";
    String INET_ORG_PERSON_OC_OID = "2.16.840.1.113730.3.2.2";
    
    // Referral
    String REFERRAL_OC = "referral";
    String REFERRAL_OC_OID = "2.16.840.1.113730.3.2.6";

    // ExtensibleObject
    String EXTENSIBLE_OBJECT_OC = "extensibleObject";
    String EXTENSIBLE_OBJECT_OC_OID = "1.3.6.1.4.1.1466.101.120.111";

    // Apache Meta Schema
    String META_TOP_OC = "metaTop";
    String META_TOP_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.1";
    String META_OBJECT_CLASS_OC = "metaObjectClass";
    String META_OBJECT_CLASS_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.2";
    String META_ATTRIBUTE_TYPE_OC = "metaAttributeType";
    String META_ATTRIBUTE_TYPE_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.3";
    String META_SYNTAX_OC = "metaSyntax";
    String META_SYNTAX_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.4";
    String META_MATCHING_RULE_OC = "metaMatchingRule";
    String META_MATCHING_RULE_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.5";
    String META_DIT_STRUCTURE_RULE_OC = "metaDITStructureRule";
    String META_DIT_STRUCTURE_RULE_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.6";
    String META_NAME_FORM_OC = "metaNameForm";
    String META_NAME_FORM_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.7";
    String META_MATCHING_RULE_USE_OC = "metaMatchingRuleUse";
    String META_MATCHING_RULE_USE_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.8";
    String META_DIT_CONTENT_RULE_OC = "metaDITContentRule";
    String META_DIT_CONTENT_RULE_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.9";
    String META_SYNTAX_CHECKER_OC = "metaSyntaxChecker";
    String META_SYNTAX_CHECKER_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.10";
    String META_SCHEMA_OC = "metaSchema";
    String META_SCHEMA_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.11";
    String META_NORMALIZER_OC = "metaNormalizer";
    String META_NORMALIZER_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.12";
    String META_COMPARATOR_OC = "metaComparator";
    String META_COMPARATOR_OC_OID = "1.3.6.1.4.1.18060.0.4.0.3.13";
    
    
    // ---- AttributeTypes ----------------------------------------------------
    // ObjectClass
    String OBJECT_CLASS_AT = "objectClass";
    String OBJECT_CLASS_AT_OID = "2.5.4.0";
    
    // AliasedObjectName
    String ALIASED_OBJECT_NAME_AT = "aliasedObjectName";
    String ALIASED_OBJECT_NAME_AT_OID = "2.5.4.1";

    // Name
    String NAME_AT = "name";
    String NAME_AT_OID = "2.5.4.41";

    // Cn
    String CN_AT = "cn";
    String COMMON_NAME_AT = "commonName";
    String CN_AT_OID = "2.5.4.3";
    
    // Sn
    String SN_AT = "sn";
    String SURNAME_AT = "surname";
    String SN_AT_OID = "2.5.4.4";

    // St
    String ST_AT = "st";
    String STATEORPROVINCE_NAME_AT = "stateOrProvinceName";
    String ST_AT_OID = "2.5.4.8";

    // Street
    String STREET_AT = "street";
    String STREET_ADDRESS_AT = "streetAddress";
    String STREET_AT_OID = "2.5.4.9";

    // PostalCode
    String POSTALCODE_AT = "postalCode";
    String POSTALCODE_AT_OID = "2.5.4.17";

    // PostalCode
    String C_POSTALCODE_AT = "c-postalCode";
    String C_POSTALCODE_AT_OID = "2.5.4.17.1";

    // PostOfficeBox
    String POSTOFFICEBOX_AT = "postOfficeBox";
    String POSTOFFICEBOX_AT_OID = "2.5.4.18";

    // SearchGuide
    String SEARCHGUIDE_AT = "searchguide";
    String SEARCHGUIDE_AT_OID = "2.5.4.14";

    // Ou
    String O_AT = "o";
    String ORGANIZATION_NAME_AT = "organizationName";
    String O_AT_OID = "2.5.4.10";

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

    // ExcludeAllColectiveAttributes
    String EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT = "excludeAllCollectiveAttributes";
    String EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT_OID = "2.5.18.0";
    
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

    // CollectiveExclusions
    String COLLECTIVE_EXCLUSIONS_AT = "collectiveExclusions";
    String COLLECTIVE_EXCLUSIONS_AT_OID = "2.5.18.7";

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
    
    // VendorName
    String VENDOR_NAME_AT = "vendorName";
    String VENDOR_NAME_AT_OID = "1.3.6.1.1.4";
    
    // VendorVersion
    String VENDOR_VERSION_AT = "vendorVersion";
    String VENDOR_VERSION_AT_OID = "1.3.6.1.1.5";
    
    // NamingContexts
    String NAMING_CONTEXTS_AT = "namingContexts";
    String NAMING_CONTEXTS_AT_OID = "1.3.6.1.4.1.1466.101.120.5";
    
    // ChangeLogContext
    String CHANGELOG_CONTEXT_AT = "changeLogContext";
    String CHANGELOG_CONTEXT_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.49";
    
    // SupportedExtension
    String SUPPORTED_EXTENSION_AT = "supportedExtension";
    String SUPPORTED_EXTENSION_AT_OID = "1.3.6.1.4.1.1466.101.120.7";

    // supportedSASLMechanisms
    String SUPPORTED_SASL_MECHANISMS_AT = "supportedSASLMechanisms";
    String SUPPORTED_SASL_MECHANISMS_AT_OID = "1.3.6.1.4.1.1466.101.120.14";
    
    // supportedControl
    String SUPPORTED_CONTROL_AT = "supportedControl";
    String SUPPORTED_CONTROL_AT_OID = "1.3.6.1.4.1.1466.101.120.13";
    
    // SupportedLdapVersion
    String SUPPORTED_LDAP_VERSION_AT = "supportedLDAPVersion";
    String SUPPORTED_LDAP_VERSION_AT_OID = "1.3.6.1.4.1.1466.101.120.15";

    // LdapSyntaxes
    String LDAP_SYNTAXES_AT = "ldapSyntaxes";
    String LDAP_SYNTAXES_AT_OID = "1.3.6.1.4.1.1466.101.120.16";
    
    // SupportedFeatures
    String SUPPORTED_FEATURES_AT = "supportedFeatures";
    String SUPPORTED_FEATURES_AT_OID = "1.3.6.1.4.1.4203.1.3.5";
    
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

    // Ref
    String REF_AT = "ref";
    String REF_AT_OID = "2.16.840.1.113730.3.1.34";

    // DisplayName
    String DISPLAY_NAME_AT = "displayName";
    String DISPLAY_NAME_AT_OID = "2.16.840.1.113730.3.1.241";

    // governingStructureRule
    String GOVERNING_STRUCTURE_RULE_AT = "governingStructureRule";
    String GOVERNING_STRUCTURE_RULE_AT_OID = "2.5.21.10";

    // UUID
    String UUID_AT = "UUID";
    String UUID_AT_OID = "1.3.6.1.1.16.1";

    // entryUUID
    String ENTRY_UUID_AT = "entryUUID";
    String ENTRY_UUID_AT_OID = "1.3.6.1.1.16.4";

    // CSN
    String CSN_AT = "CSN";
    String CSN_AT_OID = "1.3.6.1.4.1.4203.666.2.1";

    // CSNSID
    String CSN_SID_AT = "CSN";
    String CSN_SID_AT_OID = "1.3.6.1.4.1.4203.666.2.4";

    // entryCSN
    String ENTRY_CSN_AT = "entryCSN";
    String ENTRY_CSN_AT_OID = "1.3.6.1.4.1.4203.666.1.7";

    // contextCSN
    String CONTEXT_CSN_AT = "contextCSN";
    String CONTEXT_CSN_AT_OID = "1.3.6.1.4.1.4203.666.1.25";

    // entryDN
    String ENTRY_DN_AT = "entryDN";
    String ENTRY_DN_AT_OID = "1.3.6.1.1.20";

    // hasSubordinates
    String HAS_SUBORDINATES_AT = "hasSubordinates";
    String HAS_SUBORDINATES_AT_OID = "2.5.18.9";

    // numSubordinates, by Sun
    String NUM_SUBORDINATES_AT = "numSubordinates";
    // no official OID in RFCs

    // subordinateCount, by Novell
    String SUBORDINATE_COUNT_AT = "subordinateCount";
    // no official OID in RFCs
    
    // ---- Syntaxes -----------------------------------------------------

    String BINARY_SYNTAX                    = "1.3.6.1.4.1.1466.115.121.1.5";
    String BIT_STRING_SYNTAX                = "1.3.6.1.4.1.1466.115.121.1.6";
    String BOOLEAN_SYNTAX                   = "1.3.6.1.4.1.1466.115.121.1.7";
    String GENERALIZED_TIME_SYNTAX          = "1.3.6.1.4.1.1466.115.121.1.24";
    String INTEGER_SYNTAX                   = "1.3.6.1.4.1.1466.115.121.1.27";
    String UTC_TIME_SYNTAX                  = "1.3.6.1.4.1.1466.115.121.1.53";
    String DIRECTORY_STRING_SYNTAX          = "1.3.6.1.4.1.1466.115.121.1.15";
    String UUID_SYNTAX                      = "1.3.6.1.1.16.1";
    String CSN_SYNTAX                       = "1.3.6.1.4.1.4203.666.11.2.1";
    String CSN_SID_SYNTAX                   = "1.3.6.1.4.1.4203.666.11.2.4";

    String JAVA_BYTE_SYNTAX                 = "1.3.6.1.4.1.18060.0.4.1.0.0";
    String JAVA_CHAR_SYNTAX                 = "1.3.6.1.4.1.18060.0.4.1.0.1";
    String JAVA_SHORT_SYNTAX                = "1.3.6.1.4.1.18060.0.4.1.0.2";
    String JAVA_LONG_SYNTAX                 = "1.3.6.1.4.1.18060.0.4.1.0.3";
    String JAVA_INT_SYNTAX                  = "1.3.6.1.4.1.18060.0.4.1.0.4";

    // ---- MatchingRules -----------------------------------------------------

    String CASE_IGNORE_MR                   = "caseIgnoreMatch";
    String CASE_IGNORE_SUBSTRING_MR         = "caseIgnoreSubstringsMatch";
    String CASE_EXACT_MR                    = "caseExactMatch";
    String CASE_EXACT_SUBSTRING_MR          = "caseExactSubstringsMatch";
    String INTEGER_MR                       = "integerMatch";
    String INTEGER_ORDERING_MR              = "integerOrderingMatch";
    String BOOLEAN_MR                       = "booleanMatch";
    String BIT_STRING_MR                    = "bitStringMatch";
    String GENERALIZED_TIME_MR              = "generalizedTimeMatch";
    String GENERALIZED_TIME_ORDERING_MR     = "generalizedTimeOrderingMatch";
    String UUID_MATCH                       = "uuidMatch";
    String UUID_ORDERING_MATCH              = "uuidOrderingMatch";
    String CSN_MATCH                        = "csndMatch";
    String CSN_ORDERING_MATCH               = "csnOrderingMatch";
    String CSN_SID_MATCH                    = "csnSidMatch";
    
    // ---- Features ----------------------------------------------------------
    String FEATURE_ALL_OPERATIONAL_ATTRIBUTES = "1.3.6.1.4.1.4203.1.5.1";
}
