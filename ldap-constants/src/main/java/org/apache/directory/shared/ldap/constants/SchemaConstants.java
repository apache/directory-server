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
    
    // OrganizationalUnit
    public static final String ORGANIZATIONAL_UNIT_OC = "organizationalUnit";
    public static final String ORGANIZATIONAL_UNIT_OC_OID = "2.5.6.5";

    // Person
    public static final String PERSON_OC = "person";
    public static final String PERSON_OC_OID = "2.5.6.6";
    
    // OrganizationalPerson
    public static final String ORGANIZATIONAL_PERSON_OC = "organizationalPerson";
    public static final String ORGANIZATIONAL_PERSON_OC_OID = "2.5.6.7";

    // Subentry
    public static final String SUBENTRY_OC = "subentry";
    public static final String SUBENTRY_OC_OID = "2.5.17.0";
    
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
    
    // UserPassword
    public static final String USER_PASSWORD_AT = "userPassword";
    public static final String USER_PASSWORD_AT_OID = "2.5.4.35";

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

    // DisplayName
    public static final String DISPLAY_NAME_AT = "displayName";
    public static final String DISPLAY_NAME_AT_OID = "2.16.840.1.113730.3.1.241";
}
