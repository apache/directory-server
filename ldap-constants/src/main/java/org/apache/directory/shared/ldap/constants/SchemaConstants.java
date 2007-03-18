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
    // ---- ObjectClasses ----
    public static final String SUBENTRY_OC = "subentry";
    
    // top
    public static final String TOP_OC = "top";
    public static final String TOP_OC_OID = "2.5.6.0";
    
    // subschema
    public static final String SUBSCHEMA_OC = "subschema";
    public static final String SUBSCHEMA_OC_OID = "2.5.20.1";
    
    public static final String EXTENSIBLE_OBJECT_OC = "extensibleObject";
    public static final String ORGANIZATIONAL_UNIT_OC = "organizationalUnit";
    
    // ---- AttributeTypes ----
    // ObjectClass
    public static final String OBJECT_CLASS_AT = "objectClass";
    public static final String OBJECT_CLASS_AT_OID = "2.5.4.0";
    
    public static final String CREATORS_NAME_AT = "creatorsName";
    public static final String CREATE_TIMESTAMP_AT = "createTimestamp";
    
    // ObjectClasses
    public static final String OBJECT_CLASSES_AT = "objectClasses";
    public static final String OBJECT_CLASSES_AT_OID = "2.5.21.6";

    public static final String MODIFY_TIMESTAMP_AT = "modifyTimestamp";
    public static final String MODIFIERS_NAME_AT = "modifiersName";
    public static final String LDAP_SYNTAXES_AT = "ldapSyntaxes";
    public static final String MATCHING_RULES_AT = "matchingRules";
    public static final String ATTRIBUTE_TYPES_AT = "attributeTypes";
    public static final String MATCHING_RULE_USE_AT = "matchingRuleUse";
    public static final String DIT_STRUCTURE_RULES_AT = "ditStructureRules";
    public static final String DIT_CONTENT_RULES_AT = "ditContentRules";
    
    // NameForms
    public static final String NAME_FORMS_AT = "nameForms";
    public static final String NAME_FORMS_AT_OID = "2.5.21.7";
}
