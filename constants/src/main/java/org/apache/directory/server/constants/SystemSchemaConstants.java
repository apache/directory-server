/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.constants;


/**
 * Constants for the System schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface SystemSchemaConstants
{
    String SCHEMA_NAME = "system";

    String CN_AT = "cn";
    String OBJECT_CLASS_AT = "objectClass";
    String CREATORS_NAME_AT = "creatorsName";
    String CREAT_TIMESTAMP_AT = "createTimestamp";
    String MODIFY_TIMESTAMP_AT = "modifyTimestamp";
    String MODIFIERS_NAME_AT = "modifiersName";

    String LDAP_SYNTAXES_AT = "ldapSyntaxes";
    String MATCHING_RULES_AT = "matchingRules";
    String ATTRIBUTE_TYPES_AT = "attributeTypes";
    String OBJECT_CLASSES_AT = "objectClasses";
    String MATCHING_RULE_USE_AT = "matchingRuleUse";
    String DIT_STRUCTURE_RULES_AT = "ditStructureRules";
    String DIT_CONTENT_RULES_AT = "ditContentRules";
    String NAME_FORMS_AT = "nameForms";
}
