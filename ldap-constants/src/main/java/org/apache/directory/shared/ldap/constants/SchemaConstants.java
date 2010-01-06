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
    // SchemaEntity names
    String ATTRIBUTE_TYPE                       = "AttributeType";
    String COMPARATOR                           = "Comparator";
    String DIT_CONTENT_RULE                     = "DitContentRule";
    String DIT_STRUCTURE_RULE                   = "DitStructureRule";
    String MATCHING_RULE                        = "MatchingRule";
    String MATCHING_RULE_USE                    = "MatchingRuleUse";
    String NAME_FORM                            = "NameForm";
    String NORMALIZER                           = "Normalizer";
    String OBJECT_CLASS                         = "ObjectCLass";
    String SYNTAX                               = "Syntax";
    String SYNTAX_CHECKER                       = "SyntaxChecker";
    
    // SchemaEntity paths
    String ATTRIBUTES_TYPE_PATH                 = "ou=attributetypes";
    String COMPARATORS_PATH                     = "ou=comparators";
    String DIT_CONTENT_RULES_PATH               = "ou=ditcontentrules";
    String DIT_STRUCTURE_RULES_PATH             = "ou=ditstructurerules";
    String MATCHING_RULES_PATH                  = "ou=matchingrules";
    String MATCHING_RULE_USE_PATH               = "ou=matchingruleuse";
    String NAME_FORMS_PATH                      = "ou=nameforms";
    String NORMALIZERS_PATH                     = "ou=normalizers";
    String OBJECT_CLASSES_PATH                  = "ou=objectclasses";
    String SYNTAXES_PATH                        = "ou=syntaxes";
    String SYNTAX_CHECKERS_PATH                 = "ou=syntaxcheckers";
    
    // Schema root
    String OU_SCHEMA                            = "ou=schema";
    
    // The DN for the schema modifications
    String SCHEMA_MODIFICATIONS_DN              = "cn=schemaModifications,ou=schema";
    
    
    // Special attributes 1.1 , * and + for search operations
    String NO_ATTRIBUTE                         = "1.1";
    String[] NO_ATTRIBUTE_ARRAY                 = new String[]{ NO_ATTRIBUTE };
    
    String ALL_USER_ATTRIBUTES                  = "*";
    String[] ALL_USER_ATTRIBUTES_ARRAY          = new String[]{ ALL_USER_ATTRIBUTES };
    
    String ALL_OPERATIONAL_ATTRIBUTES           = "+";
    String[] ALL_OPERATIONAL_ATTRIBUTES_ARRAY   = new String[]{ ALL_OPERATIONAL_ATTRIBUTES };
    
    // ---- ObjectClasses -----------------------------------------------------
    // Krb5Principal
    String KRB5_PRINCIPAL_OC                    = "krb5Principal";
    String KRB5_PRINCIPAL_OC_OID                = "1.3.6.1.4.1.5322.10.2.1";
    
    // Top
    String TOP_OC                               = "top";
    String TOP_OC_OID                           = "2.5.6.0";
    
    // Alias
    String ALIAS_OC                             = "alias";
    String ALIAS_OC_OID                         = "2.5.6.1";

    // Country
    String COUNTRY_OC                           = "country";
    String COUNTRY_OC_OID                       = "2.5.6.2";

    // Domain
    String DOMAIN_OC                            = "domain";
    String DOMAIN_OC_OID                        = "0.9.2342.19200300.100.4.13";

    // DcObject
    String DC_OBJECT_OC                         = "dcObject";
    String DC_OBJECT_OC_OID                     = "1.3.6.1.4.1.1466.344";

    // Locality
    String LOCALITY_OC                          = "locality";
    String LOCALITY_OC_OID                      = "2.5.6.3";

    // Organization
    String ORGANIZATION_OC                      = "organization";
    String ORGANIZATION_OC_OID                  = "2.5.6.4";

    // OrganizationalUnit
    String ORGANIZATIONAL_UNIT_OC               = "organizationalUnit";
    String ORGANIZATIONAL_UNIT_OC_OID           = "2.5.6.5";

    // Person
    String PERSON_OC                            = "person";
    String PERSON_OC_OID                        = "2.5.6.6";

    // PosixAccount
    String POSIX_ACCOUNT_OC                     = "posicAccount";
    String POSIX_ACCOUNT_OC_OID                 = "1.3.6.1.1.1.2.0";

    // PosixGroup
    String POSIX_GROUP_OC                       = "posixGroup";
    String POSIX_GROUP_OC_OID                   = "1.3.6.1.1.1.2.2";
    
    // OrganizationalPerson
    String ORGANIZATIONAL_PERSON_OC             = "organizationalPerson";
    String ORGANIZATIONAL_PERSON_OC_OID         = "2.5.6.7";

    // OrganizationalRole
    String ORGANIZATIONAL_ROLE_OC               = "organizationalRole";
    String ORGANIZATIONAL_ROLE_OC_OID           = "2.5.6.8";

    // GroupOfNames
    String GROUP_OF_NAMES_OC                    = "groupOfNames";
    String GROUP_OF_NAMES_OC_OID                = "2.5.6.9";
    
    // ResidentialPerson
    String RESIDENTIAL_PERSON_OC                = "residentialPerson";
    String RESIDENTIAL_PERSON_OC_OID            = "2.5.6.10";
    
    // GroupOfUniqueNames
    String GROUP_OF_UNIQUE_NAMES_OC             = "groupOfUniqueNames";
    String GROUP_OF_UNIQUE_NAMES_OC_OID         = "2.5.6.17";
    
    // Subentry
    String SUBENTRY_OC                          = "subentry";
    String SUBENTRY_OC_OID                      = "2.5.17.0";
    
    // AccessControlSubentry
    String ACCESS_CONTROL_SUBENTRY_OC           = "accessControlSubentry";
    String ACCESS_CONTROL_SUBENTRY_OC_OID       = "2.5.17.1";
    
    // CollectiveAttributeSubentry
    String COLLECTIVE_ATTRIBUTE_SUBENTRY_OC     = "collectiveAttributeSubentry";
    String COLLECTIVE_ATTRIBUTE_SUBENTRY_OC_OID = "2.5.17.2";

    // Subschema
    String SUBSCHEMA_OC                         = "subschema";
    String SUBSCHEMA_OC_OID                     = "2.5.20.1";
    
    // InetOrgPerson
    String INET_ORG_PERSON_OC                   = "inetOrgPerson";
    String INET_ORG_PERSON_OC_OID               = "2.16.840.1.113730.3.2.2";
    
    // Referral
    String REFERRAL_OC                          = "referral";
    String REFERRAL_OC_OID                      = "2.16.840.1.113730.3.2.6";

    // ExtensibleObject
    String EXTENSIBLE_OBJECT_OC                 = "extensibleObject";
    String EXTENSIBLE_OBJECT_OC_OID             = "1.3.6.1.4.1.1466.101.120.111";

    // Apache Meta Schema
    String META_TOP_OC                          = "metaTop";
    String META_TOP_OC_OID                      = "1.3.6.1.4.1.18060.0.4.0.3.1";
    String META_OBJECT_CLASS_OC                 = "metaObjectClass";
    String META_OBJECT_CLASS_OC_OID             = "1.3.6.1.4.1.18060.0.4.0.3.2";
    String META_ATTRIBUTE_TYPE_OC               = "metaAttributeType";
    String META_ATTRIBUTE_TYPE_OC_OID           = "1.3.6.1.4.1.18060.0.4.0.3.3";
    String META_SYNTAX_OC                       = "metaSyntax";
    String META_SYNTAX_OC_OID                   = "1.3.6.1.4.1.18060.0.4.0.3.4";
    String META_MATCHING_RULE_OC                = "metaMatchingRule";
    String META_MATCHING_RULE_OC_OID            = "1.3.6.1.4.1.18060.0.4.0.3.5";
    String META_DIT_STRUCTURE_RULE_OC           = "metaDITStructureRule";
    String META_DIT_STRUCTURE_RULE_OC_OID       = "1.3.6.1.4.1.18060.0.4.0.3.6";
    String META_NAME_FORM_OC                    = "metaNameForm";
    String META_NAME_FORM_OC_OID                = "1.3.6.1.4.1.18060.0.4.0.3.7";
    String META_MATCHING_RULE_USE_OC            = "metaMatchingRuleUse";
    String META_MATCHING_RULE_USE_OC_OID        = "1.3.6.1.4.1.18060.0.4.0.3.8";
    String META_DIT_CONTENT_RULE_OC             = "metaDITContentRule";
    String META_DIT_CONTENT_RULE_OC_OID         = "1.3.6.1.4.1.18060.0.4.0.3.9";
    String META_SYNTAX_CHECKER_OC               = "metaSyntaxChecker";
    String META_SYNTAX_CHECKER_OC_OID           = "1.3.6.1.4.1.18060.0.4.0.3.10";
    String META_SCHEMA_OC                       = "metaSchema";
    String META_SCHEMA_OC_OID                   = "1.3.6.1.4.1.18060.0.4.0.3.11";
    String META_NORMALIZER_OC                   = "metaNormalizer";
    String META_NORMALIZER_OC_OID               = "1.3.6.1.4.1.18060.0.4.0.3.12";
    String META_COMPARATOR_OC                   = "metaComparator";
    String META_COMPARATOR_OC_OID               = "1.3.6.1.4.1.18060.0.4.0.3.13";
    
    
    // ---- AttributeTypes ----------------------------------------------------
    // ObjectClass
    String OBJECT_CLASS_AT                          = "objectClass";
    String OBJECT_CLASS_AT_OID                      = "2.5.4.0";
    
    // AliasedObjectName
    String ALIASED_OBJECT_NAME_AT                   = "aliasedObjectName";
    String ALIASED_OBJECT_NAME_AT_OID               = "2.5.4.1";

    // Name
    String NAME_AT                                  = "name";
    String NAME_AT_OID                              = "2.5.4.41";

    // Cn
    String CN_AT                                    = "cn";
    String COMMON_NAME_AT                           = "commonName";
    String CN_AT_OID                                = "2.5.4.3";
    
    // Sn
    String SN_AT                                    = "sn";
    String SURNAME_AT                               = "surname";
    String SN_AT_OID                                = "2.5.4.4";

    // St
    String ST_AT = "st";
    String STATEORPROVINCE_NAME_AT                  = "stateOrProvinceName";
    String ST_AT_OID                                = "2.5.4.8";

    // Street
    String STREET_AT                                = "street";
    String STREET_ADDRESS_AT                        = "streetAddress";
    String STREET_AT_OID                            = "2.5.4.9";

    // PostalCode
    String POSTALCODE_AT                            = "postalCode";
    String POSTALCODE_AT_OID                        = "2.5.4.17";

    // PostalCode
    String C_POSTALCODE_AT                          = "c-postalCode";
    String C_POSTALCODE_AT_OID                      = "2.5.4.17.1";

    // PostOfficeBox
    String POSTOFFICEBOX_AT                         = "postOfficeBox";
    String POSTOFFICEBOX_AT_OID                     = "2.5.4.18";

    // SearchGuide
    String SEARCHGUIDE_AT                           = "searchguide";
    String SEARCHGUIDE_AT_OID                       = "2.5.4.14";

    // O
    String O_AT                                     = "o";
    String ORGANIZATION_NAME_AT                     = "organizationName";
    String O_AT_OID                                 = "2.5.4.10";

    // Ou
    String OU_AT = "ou";
    String ORGANIZATIONAL_UNIT_NAME_AT              = "organizationalUnitName";
    String OU_AT_OID                                = "2.5.4.11";

    // Member
    String MEMBER_AT                                = "member";
    String MEMBER_AT_OID                            = "2.5.4.31";

    // UserPassword
    String USER_PASSWORD_AT                         = "userPassword";
    String USER_PASSWORD_AT_OID                     = "2.5.4.35";

    // UniqueMember
    String UNIQUE_MEMBER_AT                         = "uniqueMember";
    String UNIQUE_MEMBER_AT_OID                     = "2.5.4.50";

    // ExcludeAllColectiveAttributes
    String EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT     = "excludeAllCollectiveAttributes";
    String EXCLUDE_ALL_COLLECTIVE_ATTRIBUTES_AT_OID = "2.5.18.0";
    
        // CreateTimestamp
    String CREATE_TIMESTAMP_AT                      = "createTimestamp";
    String CREATE_TIMESTAMP_AT_OID                  = "2.5.18.1";
    
    // ModifyTimestamp
    String MODIFY_TIMESTAMP_AT                      = "modifyTimestamp";
    String MODIFY_TIMESTAMP_AT_OID                  = "2.5.18.2";
    
    // CreatorsName
    String CREATORS_NAME_AT                         = "creatorsName";
    String CREATORS_NAME_AT_OID                     = "2.5.18.3";
    
    // ModifiersName
    String MODIFIERS_NAME_AT                        = "modifiersName";
    String MODIFIERS_NAME_AT_OID                    = "2.5.18.4";
    
    // SubtreeSpecification
    String SUBTREE_SPECIFICATION_AT                 = "subtreeSpecification";
    String SUBTREE_SPECIFICATION_AT_OID             = "2.5.18.6";

    // CollectiveExclusions
    String COLLECTIVE_EXCLUSIONS_AT                 = "collectiveExclusions";
    String COLLECTIVE_EXCLUSIONS_AT_OID             = "2.5.18.7";

    // SubschemaSubentry
    String SUBSCHEMA_SUBENTRY_AT                    = "subschemaSubentry";
    String SUBSCHEMA_SUBENTRY_AT_OID                = "2.5.18.10";

    // CollectiveAttributeSubentries
    String COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT       = "collectiveAttributeSubentries";
    String COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT_OID   = "2.5.18.12";

    // DitStructureRules
    String DIT_STRUCTURE_RULES_AT                   = "ditStructureRules";
    String DIT_STRUCTURE_RULES_AT_OID               = "2.5.21.1";
    
    // DitContentRules
    String DIT_CONTENT_RULES_AT                     = "ditContentRules";
    String DIT_CONTENT_RULES_AT_OID                 = "2.5.21.2";
    
    // MatchingRules
    String MATCHING_RULES_AT                        = "matchingRules";
    String MATCHING_RULES_AT_OID                    = "2.5.21.4";
    
    // AttributeTypes
    String ATTRIBUTE_TYPES_AT                       = "attributeTypes";
    String ATTRIBUTE_TYPES_AT_OID                   = "2.5.21.5";
    
    // ObjectClasses
    String OBJECT_CLASSES_AT                        = "objectClasses";
    String OBJECT_CLASSES_AT_OID                    = "2.5.21.6";

    // NameForms
    String NAME_FORMS_AT                            = "nameForms";
    String NAME_FORMS_AT_OID                        = "2.5.21.7";

    // MatchingRuleUse
    String MATCHING_RULE_USE_AT                     = "matchingRuleUse";
    String MATCHING_RULE_USE_AT_OID                 = "2.5.21.8";
    
    // StructuralObjectClass
    String STRUCTURAL_OBJECT_CLASS_AT               = "structuralObjectClass";
    String STRUCTURAL_OBJECT_CLASS_AT_OID           = "2.5.21.9";
    
    // AccessControlScheme
    String ACCESS_CONTROL_SCHEME_AT                 = "accessControlScheme";
    String ACCESS_CONTROL_SCHEME_OID                = "2.5.24.1";
    
    // PrescriptiveACI
    String PRESCRIPTIVE_ACI_AT                      = "prescriptiveACI";
    String PRESCRIPTIVE_ACI_AT_OID                  = "2.5.24.4";
    
    // EntryACI
    String ENTRY_ACI_AT                             = "entryACI";
    String ENTRY_ACI_AT_OID                         = "2.5.24.5";
    
    // SubentryACI
    String SUBENTRY_ACI_AT                          = "subentryACI";
    String SUBENTRY_ACI_AT_OID                      = "2.5.24.6";
    
    // Uid
    String UID_AT                                   = "uid";
    String USER_ID_AT                               = "userid";
    String UID_AT_OID                               = "0.9.2342.19200300.100.1.1";

    // UidObject
    String UID_OBJECT_AT                            = "uidObject";
    String UID_OBJECT_AT_OID                        = "1.3.6.1.1.3.1";
    
    // VendorName
    String VENDOR_NAME_AT                           = "vendorName";
    String VENDOR_NAME_AT_OID                       = "1.3.6.1.1.4";
    
    // VendorVersion
    String VENDOR_VERSION_AT                        = "vendorVersion";
    String VENDOR_VERSION_AT_OID                    = "1.3.6.1.1.5";
    
    // NamingContexts
    String NAMING_CONTEXTS_AT                       = "namingContexts";
    String NAMING_CONTEXTS_AT_OID                   = "1.3.6.1.4.1.1466.101.120.5";
    
    // ChangeLogContext
    String CHANGELOG_CONTEXT_AT                     = "changeLogContext";
    String CHANGELOG_CONTEXT_AT_OID                 = "1.3.6.1.4.1.18060.0.4.1.2.49";
    
    // SupportedExtension
    String SUPPORTED_EXTENSION_AT                   = "supportedExtension";
    String SUPPORTED_EXTENSION_AT_OID               = "1.3.6.1.4.1.1466.101.120.7";

    // supportedSASLMechanisms
    String SUPPORTED_SASL_MECHANISMS_AT             = "supportedSASLMechanisms";
    String SUPPORTED_SASL_MECHANISMS_AT_OID         = "1.3.6.1.4.1.1466.101.120.14";
    
    // supportedControl
    String SUPPORTED_CONTROL_AT                     = "supportedControl";
    String SUPPORTED_CONTROL_AT_OID                 = "1.3.6.1.4.1.1466.101.120.13";
    
    // SupportedLdapVersion
    String SUPPORTED_LDAP_VERSION_AT                = "supportedLDAPVersion";
    String SUPPORTED_LDAP_VERSION_AT_OID            = "1.3.6.1.4.1.1466.101.120.15";

    // LdapSyntaxes
    String LDAP_SYNTAXES_AT                         = "ldapSyntaxes";
    String LDAP_SYNTAXES_AT_OID                     = "1.3.6.1.4.1.1466.101.120.16";
    
    // SupportedFeatures
    String SUPPORTED_FEATURES_AT                    = "supportedFeatures";
    String SUPPORTED_FEATURES_AT_OID                = "1.3.6.1.4.1.4203.1.3.5";
    
    // AccessControlSubentries
    String ACCESS_CONTROL_SUBENTRIES_AT             = "accessControlSubentries";
    String ACCESS_CONTROL_SUBENTRIES_AT_OID         = "1.3.6.1.4.1.18060.0.4.1.2.11";

    // TriggerExecutionSubentries
    String TRIGGER_EXECUTION_SUBENTRIES_AT          = "triggerExecutionSubentries";
    String TRIGGER_EXECUTION_SUBENTRIES_AT_OID      = "1.3.6.1.4.1.18060.0.4.1.2.27";

    // Comparators
    String COMPARATORS_AT                           = "comparators";
    String COMPARATORS_AT_OID                       = "1.3.6.1.4.1.18060.0.4.1.2.32";
    
    // Normalizers
    String NORMALIZERS_AT                           = "normalizers";
    String NORMALIZERS_AT_OID                       = "1.3.6.1.4.1.18060.0.4.1.2.33";

    // SyntaxCheckers
    String SYNTAX_CHECKERS_AT                       = "syntaxCheckers";
    String SYNTAX_CHECKERS_AT_OID                   = "1.3.6.1.4.1.18060.0.4.1.2.34";

    // Ref
    String REF_AT                                   = "ref";
    String REF_AT_OID                               = "2.16.840.1.113730.3.1.34";

    // DisplayName
    String DISPLAY_NAME_AT                          = "displayName";
    String DISPLAY_NAME_AT_OID                      = "2.16.840.1.113730.3.1.241";

    // governingStructureRule
    String GOVERNING_STRUCTURE_RULE_AT              = "governingStructureRule";
    String GOVERNING_STRUCTURE_RULE_AT_OID          = "2.5.21.10";
    
    // entryUUID
    String ENTRY_UUID_AT                            = "entryUUID";
    String ENTRY_UUID_AT_OID                        = "1.3.6.1.1.16.4";
    
    // entryCSN
    String ENTRY_CSN_AT                             = "entryCSN";
    String ENTRY_CSN_AT_OID                         = "1.3.6.1.4.1.4203.666.1.7";
    
    // contextCSN
    String CONTEXT_CSN_AT                           = "contextCSN";
    String CONTEXT_CSN_AT_OID                       = "1.3.6.1.4.1.4203.666.1.25";
    
    // entryDN
    String ENTRY_DN_AT                              = "entryDN";
    String ENTRY_DN_AT_OID                          = "1.3.6.1.1.20";

    // hasSubordinates
    String HAS_SUBORDINATES_AT                      = "hasSubordinates";
    String HAS_SUBORDINATES_AT_OID                  = "2.5.18.9";

    // numSubordinates, by Sun
    String NUM_SUBORDINATES_AT                      = "numSubordinates";
    // no official OID in RFCs

    // subordinateCount, by Novell
    String SUBORDINATE_COUNT_AT                     = "subordinateCount";
    // no official OID in RFCs
    
    // DomainComponent
    String DC_AT = "dc";
    String DOMAIN_COMPONENT_AT                      = "domainComponent";
    String DOMAIN_COMPONENT_AT_OID                  = "0.9.2342.19200300.100.1.25";
    
    //-------------------------------------------------------------------------
    // ---- Syntaxes ----------------------------------------------------------
    //-------------------------------------------------------------------------
    String NAME_OR_NUMERIC_ID_SYNTAX                      = "1.3.6.1.4.1.18060.0.4.0.0.0";
    
    String OBJECT_CLASS_TYPE_SYNTAX                       = "1.3.6.1.4.1.18060.0.4.0.0.1";
    
    String NUMERIC_OID_SYNTAX                             = "1.3.6.1.4.1.18060.0.4.0.0.2";
    
    String ATTRIBUTE_TYPE_USAGE_SYNTAX                    = "1.3.6.1.4.1.18060.0.4.0.0.3";
        
    // RFC 4517, par. 3.3.23
    String NUMBER_SYNTAX                                  = "1.3.6.1.4.1.18060.0.4.0.0.4";
    
    String OID_LEN_SYNTAX                                 = "1.3.6.1.4.1.18060.0.4.0.0.5";
    
    String OBJECT_NAME_SYNTAX                             = "1.3.6.1.4.1.18060.0.4.0.0.6";

    // RFC 2252, removed in RFC 4517
    String ACI_ITEM_SYNTAX                                = "1.3.6.1.4.1.1466.115.121.1.1";

    // RFC 2252, removed in RFC 4517
    String ACCESS_POINT_SYNTAX                            = "1.3.6.1.4.1.1466.115.121.1.2";
    
    // RFC 4517, chap 3.3.1
    String ATTRIBUTE_TYPE_DESCRIPTION_SYNTAX              = "1.3.6.1.4.1.1466.115.121.1.3";

    // RFC 2252, removed in RFC 4517
    String AUDIO_SYNTAX                                   = "1.3.6.1.4.1.1466.115.121.1.4";

    // RFC 2252, removed in RFC 4517
    String BINARY_SYNTAX                                  = "1.3.6.1.4.1.1466.115.121.1.5";
    
    // RFC 4517, chap 3.3.2
    String BIT_STRING_SYNTAX                              = "1.3.6.1.4.1.1466.115.121.1.6";
    
    // RFC 4517, chap 3.3.3
    String BOOLEAN_SYNTAX                                 = "1.3.6.1.4.1.1466.115.121.1.7";
    
    // RFC 2252, removed in RFC 4517, reintroduced in RFC 4523, chap. 2.1 
    String CERTIFICATE_SYNTAX                             = "1.3.6.1.4.1.1466.115.121.1.8";
    
    // RFC 2252, removed in RFC 4517, reintroduced in RFC 4523, chap. 2.2 
    String CERTIFICATE_LIST_SYNTAX                        = "1.3.6.1.4.1.1466.115.121.1.9";

    // RFC 2252, removed in RFC 4517, reintroduced in RFC 4523, chap. 2.3 
    String CERTIFICATE_PAIR_SYNTAX                        = "1.3.6.1.4.1.1466.115.121.1.10";
    
    // RFC 4517, chap 3.3.4
    String COUNTRY_STRING_SYNTAX                          = "1.3.6.1.4.1.1466.115.121.1.11";
    
    // RFC 4517, chap 3.3.9
    String DN_SYNTAX                                      = "1.3.6.1.4.1.1466.115.121.1.12";

    // RFC 2252, removed in RFC 4517
    String DATA_QUALITY_SYNTAX                            = "1.3.6.1.4.1.1466.115.121.1.13";
    
    // RFC 4517, chap 3.3.5
    String DELIVERY_METHOD_SYNTAX                         = "1.3.6.1.4.1.1466.115.121.1.14";
    
    // RFC 4517, chap 3.3.6
    String DIRECTORY_STRING_SYNTAX                        = "1.3.6.1.4.1.1466.115.121.1.15";
    
    // RFC 4517, chap 3.3.7
    String DIT_CONTENT_RULE_SYNTAX                        = "1.3.6.1.4.1.1466.115.121.1.16";
    
    // RFC 4517, chap 3.3.8
    String DIT_STRUCTURE_RULE_SYNTAX                      = "1.3.6.1.4.1.1466.115.121.1.17";
    
    // RFC 2252, removed in RFC 4517
    String DL_SUBMIT_PERMISSION_SYNTAX                    = "1.3.6.1.4.1.1466.115.121.1.18";

    // RFC 2252, removed in RFC 4517
    String DSA_QUALITY_SYNTAX                             = "1.3.6.1.4.1.1466.115.121.1.19";

    // RFC 2252, removed in RFC 4517
    String DSE_TYPE_SYNTAX                                = "1.3.6.1.4.1.1466.115.121.1.20";
    
    // RFC 4517, chap 3.3.10
    String ENHANCED_GUIDE_SYNTAX                          = "1.3.6.1.4.1.1466.115.121.1.21";
    
    // RFC 4517, chap 3.3.11
    String FACSIMILE_TELEPHONE_NUMBER_SYNTAX              = "1.3.6.1.4.1.1466.115.121.1.22";
    
    // RFC 4517, chap 3.3.12
    String FAX_SYNTAX                                     = "1.3.6.1.4.1.1466.115.121.1.23";
    
    // RFC 4517, chap 3.3.13
    String GENERALIZED_TIME_SYNTAX                        = "1.3.6.1.4.1.1466.115.121.1.24";
    
    // RFC 4517, chap 3.3.14
    String GUIDE_SYNTAX                                   = "1.3.6.1.4.1.1466.115.121.1.25";
    
    // RFC 4517, chap 3.3.15
    String IA5_STRING_SYNTAX                              = "1.3.6.1.4.1.1466.115.121.1.26";
    
    // RFC 4517, chap 3.3.16
    String INTEGER_SYNTAX                                 = "1.3.6.1.4.1.1466.115.121.1.27";
    
    // RFC 4517, chap 3.3.17
    String JPEG_SYNTAX                                    = "1.3.6.1.4.1.1466.115.121.1.28";
    
    // RFC 2252, removed in RFC 4517
    String MASTER_AND_SHADOW_ACCESS_POINTS_SYNTAX         = "1.3.6.1.4.1.1466.115.121.1.29";
    
    // RFC 4517, chap 3.3.19
    String MATCHING_RULE_DESCRIPTION_SYNTAX               = "1.3.6.1.4.1.1466.115.121.1.30";
    
    // RFC 4517, chap 3.3.20
    String MATCHING_RULE_USE_DESCRIPTION_SYNTAX           = "1.3.6.1.4.1.1466.115.121.1.31";
    
    // RFC 2252, removed in RFC 4517
    String MAIL_PREFERENCE_SYNTAX                         = "1.3.6.1.4.1.1466.115.121.1.32";
    
    // RFC 2252, removed in RFC 4517
    String MHS_OR_ADDRESS_SYNTAX                          = "1.3.6.1.4.1.1466.115.121.1.33"; 
    
    // RFC 4517, chap 3.3.21
    String NAME_AND_OPTIONAL_UID_SYNTAX                   = "1.3.6.1.4.1.1466.115.121.1.34";
    
    // RFC 4517, chap 3.3.22
    String NAME_FORM_DESCRIPTION_SYNTAX                   = "1.3.6.1.4.1.1466.115.121.1.35";
    
    // RFC 4517, chap 3.3.23
    String NUMERIC_STRING_SYNTAX                          = "1.3.6.1.4.1.1466.115.121.1.36";
    
    // RFC 4517, chap 3.3.24
    String OBJECT_CLASS_DESCRIPTION_SYNTAX                = "1.3.6.1.4.1.1466.115.121.1.37";
    
    // RFC 4517, chap 3.3.26
    String OID_SYNTAX                                     = "1.3.6.1.4.1.1466.115.121.1.38";
    
    // RFC 4517, chap 3.3.27
    String OTHER_MAILBOX_SYNTAX                           = "1.3.6.1.4.1.1466.115.121.1.39";
    
    // RFC 4517, chap 3.3.25
    String OCTET_STRING_SYNTAX                            = "1.3.6.1.4.1.1466.115.121.1.40";
    
    // RFC 4517, chap 3.3.28
    String POSTAL_ADDRESS_SYNTAX                          = "1.3.6.1.4.1.1466.115.121.1.41";
    
    // RFC 2252, removed in RFC 4517
    String PROTOCOL_INFORMATION_SYNTAX                    = "1.3.6.1.4.1.1466.115.121.1.42";
    
    // RFC 2252, removed in RFC 4517
    String PRESENTATION_ADDRESS_SYNTAX                    = "1.3.6.1.4.1.1466.115.121.1.43";
    
    // RFC 4517, chap 3.3.29
    String PRINTABLE_STRING_SYNTAX                        = "1.3.6.1.4.1.1466.115.121.1.44";
    
    // RFC 2252, removed in RFC 4517
    String SUBTREE_SPECIFICATION_SYNTAX                   = "1.3.6.1.4.1.1466.115.121.1.45";
    
    // RFC 2252, removed in RFC 4517
    String SUPPLIER_INFORMATION_SYNTAX                    = "1.3.6.1.4.1.1466.115.121.1.46";
    
    // RFC 2252, removed in RFC 4517
    String SUPPLIER_OR_CONSUMER_SYNTAX                    = "1.3.6.1.4.1.1466.115.121.1.47";
    
    // RFC 2252, removed in RFC 4517
    String SUPPLIER_AND_CONSUMER_SYNTAX                   = "1.3.6.1.4.1.1466.115.121.1.48";

    // RFC 2252, removed in RFC 4517, reintroduced in RFC 4523, chap. 2.4
    String SUPPORTED_ALGORITHM_SYNTAX                     = "1.3.6.1.4.1.1466.115.121.1.49";
    
    // RFC 4517, chap 3.3.31
    String TELEPHONE_NUMBER_SYNTAX                        = "1.3.6.1.4.1.1466.115.121.1.50";

    // RFC 4517, chap 3.3.32
    String TELETEX_TERMINAL_IDENTIFIER_SYNTAX             = "1.3.6.1.4.1.1466.115.121.1.51";
    
    // RFC 4517, chap 3.3.33
    String TELEX_NUMBER_SYNTAX                            = "1.3.6.1.4.1.1466.115.121.1.52"; 
    
    // RFC 4517, chap 3.3.34
    String UTC_TIME_SYNTAX                                = "1.3.6.1.4.1.1466.115.121.1.53";
    
    // RFC 4517, chap 3.3.18
    String LDAP_SYNTAX_DESCRIPTION_SYNTAX                 = "1.3.6.1.4.1.1466.115.121.1.54";
    
    // RFC 2252, removed in RFC 4517
    String MODIFY_RIGHTS_SYNTAX                           = "1.3.6.1.4.1.1466.115.121.1.55";
    
    // RFC 2252, removed in RFC 4517
    String LDAP_SCHEMA_DEFINITION_SYNTAX                  = "1.3.6.1.4.1.1466.115.121.1.56";
    
    // RFC 2252, removed in RFC 4517
    String LDAP_SCHEMA_DESCRIPTION_SYNTAX                 = "1.3.6.1.4.1.1466.115.121.1.57";
    
    // RFC 4517, chap 3.3.30
    String SUBSTRING_ASSERTION_SYNTAX                     = "1.3.6.1.4.1.1466.115.121.1.58";

    // From draft-ietf-pkix-ldap-v3-01.txt. Obsolete.
    String ATTRIBUTE_CERTIFICATE_ASSERTION_SYNTAX         = "1.3.6.1.4.1.1466.115.121.1.59";

    //From RFC 4530, chap. 2.1
    String UUID_SYNTAX                                    = "1.3.6.1.1.16.1";
    
    // From http://www.openldap.org/faq/data/cache/1145.html
    String CSN_SYNTAX                                     = "1.3.6.1.4.1.4203.666.11.2.1"; 
    
    // From http://www.openldap.org/faq/data/cache/1145.html
    String CSN_SID_SYNTAX                                 = "1.3.6.1.4.1.4203.666.11.2.4";

    // Apache DS
    String JAVA_BYTE_SYNTAX                               = "1.3.6.1.4.1.18060.0.4.1.0.0";
    String JAVA_CHAR_SYNTAX                               = "1.3.6.1.4.1.18060.0.4.1.0.1";
    String JAVA_SHORT_SYNTAX                              = "1.3.6.1.4.1.18060.0.4.1.0.2";
    String JAVA_LONG_SYNTAX                               = "1.3.6.1.4.1.18060.0.4.1.0.3";
    String JAVA_INT_SYNTAX                                = "1.3.6.1.4.1.18060.0.4.1.0.4";

    // Comparator syntax
    String COMPARATOR_SYNTAX                              = "1.3.6.1.4.1.18060.0.4.1.0.5";
    
    // Normalizer Syntax
    String NORMALIZER_SYNTAX                              = "1.3.6.1.4.1.18060.0.4.1.0.6";
    
    // SyntaxChecker Syntax
    String SYNTAX_CHECKER_SYNTAX                          = "1.3.6.1.4.1.18060.0.4.1.0.7";
    
    //-------------------------------------------------------------------------
    // ---- MatchingRules -----------------------------------------------------
    //-------------------------------------------------------------------------
    // caseExactIA5Match (RFC 4517, chap. 4.2.3)
    String CASE_EXACT_IA5_MATCH_MR                        = "caseExactIA5Match";
    String CASE_EXACT_IA5_MATCH_MR_OID                    = "1.3.6.1.4.1.1466.109.114.1";
    
    // caseIgnoreIA5Match (RFC 4517, chap. 4.2.7)
    String CASE_IGNORE_IA5_MATCH_MR                       = "caseIgnoreIA5Match";
    String CASE_IGNORE_IA5_MATCH_MR_OID                   = "1.3.6.1.4.1.1466.109.114.2";
    
    // caseIgnoreIA5SubstringsMatch (RFC 4517, chap. 4.2.8)
    String CASE_IGNORE_IA5_SUBSTRINGS_MATCH_MR            = "caseIgnoreIA5SubstringsMatch";
    String CASE_IGNORE_IA5_SUBSTRINGS_MATCH_MR_OID        = "1.3.6.1.4.1.1466.109.114.3";
    
    // objectIdentifierMatch (RFC 4517, chap. 4.2.26)
    String OBJECT_IDENTIFIER_MATCH_MR                     = "objectIdentifierMatch";
    String OBJECT_IDENTIFIER_MATCH_MR_OID                 = "2.5.13.0";
    
    // distinguishedNameMatch (RFC 4517, chap. 4.2.15)
    String DISTINGUISHED_NAME_MATCH_MR                    = "distinguishedNameMatch";
    String DISTINGUISHED_NAME_MATCH_MR_OID                = "2.5.13.1";
    
    // caseIgnoreMatch (RFC 4517, chap. 3.3.19)
    String CASE_IGNORE_MATCH_MR                           = "caseIgnoreMatch";
    String CASE_IGNORE_MATCH_MR_OID                       = "2.5.13.2";
    
    // caseIgnoreOrderingMatch (RFC 4517, chap. 4.2.12)
    String CASE_IGNORE_ORDERING_MATCH_MR                  = "caseIgnoreOrderingMatch";
    String CASE_IGNORE_ORDERING_MATCH_MR_OID              = "2.5.13.3";
    
    // caseIgnoreSubstringsMatch (RFC 4517, chap. 4.2.13)
    String CASE_IGNORE_SUBSTRING_MATCH_MR                 = "caseIgnoreSubstringsMatch";
    String CASE_IGNORE_SUBSTRING_MATCH_MR_OID             = "2.5.13.4";
    
    // caseExactMatch (RFC 4517, chap. 4.2.4)
    String CASE_EXACT_MATCH_MR                            = "caseExactMatch";
    String CASE_EXACT_MATCH_MR_OID                        = "2.5.13.5";
    
    // caseExactOrderingMatch (RFC 4517, chap. 4.2.5)
    String CASE_EXACT_ORDERING_MATCH_MR                   = "caseExactOrderingMatch";
    String CASE_EXACT_ORDERING_MATCH_MR_OID               = "2.5.13.6";
    
    // caseExactSubstringsMatch (RFC 4517, chap. 4.2.6)
    String CASE_EXACT_SUBSTRING_MATCH_MR                  = "caseExactSubstringsMatch";
    String CASE_EXACT_SUBSTRING_MATCH_MR_OID              = "2.5.13.7";
    
    // numericStringMatch (RFC 4517, chap. 4.2.22)
    String NUMERIC_STRING_MATCH_MR                        = "numericStringMatch";
    String NUMERIC_STRING_MATCH_MR_OID                    = "2.5.13.8";
    
    // numericStringOrderingMatch (RFC 4517, chap. 4.2.23)
    String NUMERIC_STRING_ORDERING_MATCH_MR               = "numericStringOrderingMatch";
    String NUMERIC_STRING_ORDERING_MATCH_MR_OID           = "2.5.13.9";
    
    // numericStringSubstringsMatch (RFC 4517, chap. 4.2.24)
    String NUMERIC_STRING_SUBSTRINGS_MATCH_MR             = "numericStringSubstringsMatch";
    String NUMERIC_STRING_SUBSTRINGS_MATCH_MR_OID         = "2.5.13.10";
    
    // caseIgnoreListMatch (RFC 4517, chap. 4.2.9)
    String CASE_IGNORE_LIST_MATCH_MR                      = "caseIgnoreListMatch";
    String CASE_IGNORE_LIST_MATCH_MR_OID                  = "2.5.13.11";
    
    // caseIgnoreListSubstringsMatch (RFC 4517, chap. 4.2.10)
    String CASE_IGNORE_LIST_SUBSTRINGS_MATCH_MR           = "caseIgnoreListSubstringsMatch";
    String CASE_IGNORE_LIST_SUBSTRINGS_MATCH_MR_OID       = "2.5.13.12";
    
    // booleanMatch (RFC 4517, chap. 4.2.2)
    String BOOLEAN_MATCH_MR                               = "booleanMatch";
    String BOOLEAN_MATCH_MR_OID                           = "2.5.13.13";
    
    // integerMatch (RFC 4517, chap. 4.2.19)
    String INTEGER_MATCH_MR                               = "integerMatch";
    String INTEGER_MATCH_MR_OID                           = "2.5.13.14";
    
    // integerOrderingMatch (RFC 4517, chap. 4.2.20)
    String INTEGER_ORDERING_MATCH_MR                      = "integerOrderingMatch";
    String INTEGER_ORDERING_MATCH_MR_OID                  = "2.5.13.15";

    // bitStringMatch (RFC 4517, chap. 4.2.1)
    String BIT_STRING_MATCH_MR                            = "bitStringMatch";
    String BIT_STRING_MATCH_MR_OID                        = "2.5.13.16";
    
    // octetStringMatch (RFC 4517, chap. 4.2.27)
    String OCTET_STRING_MATCH_MR                          = "octetStringMatch";
    String OCTET_STRING_MATCH_MR_OID                      = "2.5.13.17";
    
    // octetStringMatch (RFC 4517, chap. 4.2.28)
    String OCTET_STRING_ORDERING_MATCH_MR                 = "octetStringOrderingMatch";
    String OCTET_STRING_ORDERING_MATCH_MR_OID             = "2.5.13.18";
    
    // octetStringSubstringsMatch
    String OCTET_STRING_SUBSTRINGS_MATCH_MR               = "octetStringSubstringsMatch";
    String OCTET_STRING_SUBSTRINGS_MATCH_MR_OID           = "2.5.13.19";
    
    // telephoneNumberMatch (RFC 4517, chap. 4.2.29)
    String TELEPHONE_NUMBER_MATCH_MR                      = "telephoneNumberMatch";
    String TELEPHONE_NUMBER_MATCH_MR_OID                  = "2.5.13.20";
    
    // telephoneNumberMatch (RFC 4517, chap. 4.2.30)
    String TELEPHONE_NUMBER_SUBSTRINGS_MATCH_MR           = "telephoneNumberSubstringsMatch";
    String TELEPHONE_NUMBER_SUBSTRINGS_MATCH_MR_OID       = "2.5.13.21";
    
    // presentationAddressMatch Removed in RFC 4517
    String PRESENTATION_ADDRESS_MATCH_MATCH_MR            = "presentationAddressMatch";
    String PRESENTATION_ADDRESS_MATCH_MATCH_MR_OID        = "2.5.13.22";
    
    // uniqueMemberMatch (RFC 4517, chap. 4.2.31)
    String UNIQUE_MEMBER_MATCH_MR                         = "uniqueMemberMatch";
    String UNIQUE_MEMBER_MATCH_MR_OID                     = "2.5.13.23";
    
    // protocolInformationMatch Removed in RFC 4517
    String PROTOCOL_INFORMATION_MATCH_MR                  = "protocolInformationMatch";
    String PROTOCOL_INFORMATION_MATCH_MR_OID              = "2.5.13.24";
    
    // "2.5.13.25" is not used ...
    // "2.5.13.26" is not used ...
    
    // generalizedTimeMatch (RFC 4517, chap. 4.2.16)
    String GENERALIZED_TIME_MATCH_MR                      = "generalizedTimeMatch";
    String GENERALIZED_TIME_MATCH_MR_OID                  = "2.5.13.27";
    
    // generalizedTimeOrderingMatch (RFC 4517, chap. 4.2.17)
    String GENERALIZED_TIME_ORDERING_MATCH_MR             = "generalizedTimeOrderingMatch";
    String GENERALIZED_TIME_ORDERING_MATCH_MR_OID         = "2.5.13.28";

    // integerFirstComponentMatch (RFC 4517, chap. 4.2.18)
    String INTEGER_FIRST_COMPONENT_MATCH_MR               = "integerFirstComponentMatch";
    String INTEGER_FIRST_COMPONENT_MATCH_MR_OID           = "2.5.13.29";
    
    // objectIdentifierFirstComponentMatch (RFC 4517, chap. 4.2.25)
    String OBJECT_IDENTIFIER_FIRST_COMPONENT_MATCH_MR     = "objectIdentifierFirstComponentMatch";
    String OBJECT_IDENTIFIER_FIRST_COMPONENT_MATCH_MR_OID = "2.5.13.30";

    // directoryStringFirstComponentMatch (RFC 4517, chap. 4.2.14)
    String DIRECTORY_STRING_FIRST_COMPONENT_MATCH_MR      = "directoryStringFirstComponentMatch";
    String DIRECTORY_STRING_FIRST_COMPONENT_MATCH_MR_OID  = "2.5.13.31";

    // wordMatch (RFC 4517, chap. 4.2.32)
    String WORD_MATCH_MR                                  = "wordMatch";
    String WORD_MATCH_MR_OID                              = "2.5.13.32";

    // keywordMatch (RFC 4517, chap. 4.2.21)
    String KEYWORD_MATCH_MR                               = "keywordMatch";
    String KEYWORD_MATCH_MR_OID                           = "2.5.13.33";

    // uuidMatch
    String UUID_MATCH_MR                                  = "uuidMatch";
    String UUID_MATCH_MR_OID                              = "1.3.6.1.1.16.2";
    
    // uuidOrderingMatch
    String UUID_ORDERING_MATCH_MR                         = "uuidOrderingMatch";
    String UUID_ORDERING_MATCH_MR_OID                     = "1.3.6.1.1.16.3";
    
    // csnMatch 
    String CSN_MATCH_MR                                   = "csnMatch"; 
    String CSN_MATCH_MR_OID                               = "1.3.6.1.4.1.4203.666.11.2.2"; 
    
    // csnOrderingMatch
    String CSN_ORDERING_MATCH_MR                          = "csnOrderingMatch"; 
    String CSN_ORDERING_MATCH_MR_OID                      = "1.3.6.1.4.1.4203.666.11.2.3"; 
    
    // csnSidMatch
    String CSN_SID_MATCH_MR                               = "csnSidMatch"; 
    String CSN_SID_MATCH_MR_OID                           = "1.3.6.1.4.1.4203.666.11.2.5"; 
    
    // nameOrNumericIdMatch 
    String NAME_OR_NUMERIC_ID_MATCH                       = "nameOrNumericIdMatch";
    String NAME_OR_NUMERIC_ID_MATCH_OID                   = "1.3.6.1.4.1.18060.0.4.0.1.0";
    
    // objectClassTypeMatch 
    String OBJECT_CLASS_TYPE_MATCH                        = "objectClassTypeMatch";
    String OBJECT_CLASS_TYPE_MATCH_OID                    = "1.3.6.1.4.1.18060.0.4.0.1.1";
    
    // numericOidMatch 
    String NUMERIC_OID_MATCH                              = "numericOidMatch";
    String NUMERIC_OID_MATCH_OID                          = "1.3.6.1.4.1.18060.0.4.0.1.2";
    
    // supDITStructureRuleMatch 
    String SUP_DIT_STRUCTURE_RULE_MATCH                   = "supDITStructureRuleMatch";
    String SUP_DIT_STRUCTURE_RULE_MATCH_OID               = "1.3.6.1.4.1.18060.0.4.0.1.3";
    
    // ruleIDMatch 
    String RULE_ID_MATCH                                  = "ruleIDMatch";
    String RULE_ID_MATCH_OID                              = "1.3.6.1.4.1.18060.0.4.0.1.4";
    
    // ExactDnAsStringMatch
    String EXACT_DN_AS_STRING_MATCH_MR                    = "exactDnAsStringMatch"; 
    String EXACT_DN_AS_STRING_MATCH_MR_OID                = "1.3.6.1.4.1.18060.0.4.1.1.1"; 
    
    // BigIntegerMatch
    String BIG_INTEGER_MATCH_MR                           = "bigIntegerMatch"; 
    String BIG_INTEGER_MATCH_MR_OID                       = "1.3.6.1.4.1.18060.0.4.1.1.2"; 
    
    // JdbmStringMatch
    String JDBM_STRING_MATCH_MR                           = "jdbmStringMatch"; 
    String JDBM_STRING_MATCH_MR_OID                       = "1.3.6.1.4.1.18060.0.4.1.1.3"; 
    
    // ComparatorMatch
    String COMPARATOR_MATCH_MR                            = "comparatorMatch"; 
    String COMPARATOR_MATCH_MR_OID                        = "1.3.6.1.4.1.18060.0.4.1.1.5";
    
    // NormalizerMatch
    String NORMALIZER_MATCH_MR                            = "normalizerMatch"; 
    String NORMALIZER_MATCH_MR_OID                        = "1.3.6.1.4.1.18060.0.4.1.1.6"; 
    
    // SyntaxCheckerMatch
    String SYNTAX_CHECKER_MATCH_MR                        = "syntaxCheckerMatch"; 
    String SYNTAX_CHECKER_MATCH_MR_OID                    = "1.3.6.1.4.1.18060.0.4.1.1.7"; 
    
    // ---- Features ----------------------------------------------------------
    String FEATURE_ALL_OPERATIONAL_ATTRIBUTES             = "1.3.6.1.4.1.4203.1.5.1";
}
